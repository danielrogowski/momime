package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CombatAreaEffectAttributeBonus;
import momime.common.database.CombatAreaEffectSkillBonus;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceAttributeBonus;
import momime.common.database.ExperienceLevel;
import momime.common.database.ExperienceSkillBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitAttributePositiveNegative;
import momime.common.database.UnitHasAttributeValue;
import momime.common.database.UnitHasSkill;
import momime.common.database.UnitSkill;
import momime.common.database.WeaponGradeAttributeBonus;
import momime.common.database.WeaponGradeSkillBonus;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Calculates modified values over and above basic skill, attribute and upkeep values
 */
public final class UnitSkillUtilsImpl implements UnitSkillUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitSkillUtilsImpl.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param unitSkillID Unique identifier for this skill
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Value of the specified skill - base value can be improved by weapon grades, experience or CAEs (e.g. Node Auras or Prayer), or can be reduced by curses or enemy CAEs (e.g. Black Prayer); skills granted from spells currently always return zero but this is likely to change
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int getModifiedSkillValue (final AvailableUnit unit, final List<UnitHasSkill> skills, final String unitSkillID, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering getModifiedSkillValue: " + unit.getUnitID () + ", " + unitSkillID);

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitHasSkill> mergedSkills;
		if ((unit instanceof MemoryUnit) && (!(skills instanceof UnitHasSkillMergedList)))
			mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit, db);
		else
			mergedSkills = skills;

		// Get unit magic realm ID
		final String storeMagicRealmLifeformTypeID = getUnitUtils ().getModifiedUnitMagicRealmLifeformTypeID (unit, mergedSkills, spells, db);

		// Get basic skill value
		final int basicValue = getUnitUtils ().getBasicSkillValue (mergedSkills, unitSkillID);
		int modifiedValue = basicValue;

		// Bonuses only apply if we already have the skill, and its a skill that has a numeric value such as Flame Breath
		// (skills which we have, but have no numeric value, will have value zero so are excluded here)

		// Exclude experience, otherwise we get a repetitive loop as the call to expLvl = getExperienceLevel () lower down calls getSkillValue!
		if ((modifiedValue > 0) && (!unitSkillID.equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)))
		{
			// Any bonuses due to weapon grades?
			if (unit.getWeaponGrade () != null)
				for (final WeaponGradeSkillBonus bonus : db.findWeaponGrade (unit.getWeaponGrade (), "getModifiedSkillValue").getWeaponGradeSkillBonus ())
					if (bonus.getUnitSkillID ().equals (unitSkillID))
						modifiedValue = modifiedValue + bonus.getBonusValue ();

			// Any bonuses due to experience?
			final ExperienceLevel expLvl = getUnitUtils ().getExperienceLevel (unit, true, players, combatAreaEffects, db);
			if (expLvl != null)
				for (final ExperienceSkillBonus bonus : expLvl.getExperienceSkillBonus ())
					if (bonus.getUnitSkillID ().equals (unitSkillID))
						modifiedValue = modifiedValue + bonus.getBonusValue ();

			// Any bonuses from CAEs?
			for (final MemoryCombatAreaEffect thisCAE : combatAreaEffects)
				if (getUnitUtils ().doesCombatAreaEffectApplyToUnit (unit, thisCAE, db))
				{
					// Found a combat area effect whose location matches this unit, as well as any player or other pre-requisites
					// So this means all the skill bonuses apply, except we still need to do the magic realm
					// check since some effects have different components which apply to different lifeform types, e.g. True Light and Darkness
					for (final CombatAreaEffectSkillBonus caeBonusCache : db.findCombatAreaEffect (thisCAE.getCombatAreaEffectID (), "getModifiedSkillValue").getCombatAreaEffectSkillBonus ())

						// Magic realm/lifeform type can be blank for effects that apply to all types of unit (e.g. Prayer)
						if ((caeBonusCache.getUnitSkillID ().equals (unitSkillID)) &&
							((caeBonusCache.getEffectMagicRealm () == null) || (caeBonusCache.getEffectMagicRealm ().equals (storeMagicRealmLifeformTypeID))))

							modifiedValue = modifiedValue + caeBonusCache.getBonusValue ();
				}
		}

		log.trace ("Exiting getModifiedSkillValue = " + modifiedValue);
		return modifiedValue;
	}
	
	/**
	 * @param value Value of 1 attribute component
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @return value, if positive/negative as desired, or 0 if not wanted
	 */
	final int addToAttributeValue (final int value, final UnitAttributePositiveNegative positiveNegative)
	{
		final int result;
		switch (positiveNegative)
		{
			case BOTH:
				result = value;
				break;
				
			case POSITIVE:
				result = (value > 0) ? value : 0;
				break;
				
			case NEGATIVE:
				result = (value < 0) ? value : 0;
				break;
				
			default:
				throw new UnsupportedOperationException ("addToAttributeValue doesn't know how to handle " + positiveNegative);
		}
		return result;
	}
	
	/**
	 * NB. The reason there is no getBasicAttributeValue method is because this can be achieved by passing in UnitAttributeComponent.BASIC
	 * 
	 * @param unit Unit to calculate attribute value for
	 * @param unitAttributeID Unique identifier for this attribute
	 * @param component Which component(s) making up this attribute to calculate
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Calculated unit attribute (e.g. swords/shields/hearts); 0 if attribute is N/A for this unit (unlike skills, which return -1)
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final int getModifiedAttributeValue (final AvailableUnit unit, final String unitAttributeID, final UnitAttributeComponent component,
		final UnitAttributePositiveNegative positiveNegative, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering getModifiedAttributeValue: " + unit.getUnitID () + ", " + unitAttributeID);

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitHasSkill> mergedSkills;
		if (unit instanceof MemoryUnit)
			mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit, db);
		else
			mergedSkills = unit.getUnitHasSkill ();
		
		// First get basic attribute value from the DB - we need this regardless of whether it was actually asked for
		final Unit unitDefinition = db.findUnit (unit.getUnitID (), "getModifiedAttributeValue");
		int basicValue = 0;
		boolean found = false;
		final Iterator<UnitHasAttributeValue> iter = unitDefinition.getUnitAttributeValue ().iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final UnitHasAttributeValue thisAttr = iter.next ();
			if (unitAttributeID.equals (thisAttr.getUnitAttributeID ()))
			{
				found = true;
				basicValue = thisAttr.getAttributeValue ();
			}
		}
		
		// Include basic value in total?
		int total = 0;
		if ((component == UnitAttributeComponent.BASIC) || (component == UnitAttributeComponent.ALL))
			total = total + addToAttributeValue (basicValue, positiveNegative);
		
		// Any bonuses due to weapon grades?
		// If this is the Ranged Attack skill, only grant bonuses if the unit had a ranged attack to begin with.
		// Otherwise, the weapon grade entries in the database have child nodes underneath them stating which attributes gain how much bonus -
		// this is how we know that e.g. adamantium gives +2 defense but not +2 resistance.
		if ((unit.getWeaponGrade () != null) &&
			((component == UnitAttributeComponent.WEAPON_GRADE) || (component == UnitAttributeComponent.ALL)) &&
			((!unitAttributeID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) || (basicValue > 0)))
		{
			// Only certain types of ranged attack get bonuses from Mithril and Adamantium weapons - e.g. bows do, magical blasts do not
			final boolean weaponGradeBonusApplies;
			if (unitAttributeID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
			{
				if (unitDefinition.getRangedAttackType () == null)
					weaponGradeBonusApplies = false;
				else
					weaponGradeBonusApplies = db.findRangedAttackType (unitDefinition.getRangedAttackType (), "getModifiedAttributeValue").isMithrilAndAdamantiumVersions ();
			}
			else
				weaponGradeBonusApplies = true;
			
			if (weaponGradeBonusApplies)
				for (final WeaponGradeAttributeBonus bonus : db.findWeaponGrade (unit.getWeaponGrade (), "getModifiedAttributeValue").getWeaponGradeAttributeBonus ())
					if (bonus.getUnitAttributeID ().equals (unitAttributeID))
						total = total + addToAttributeValue (bonus.getBonusValue (), positiveNegative);
		}
		
		// Any bonuses due to experience?
		// If this is the Ranged Attack skill, only grant bonuses if the unit had a ranged attack to begin with
		final ExperienceLevel expLevel = getUnitUtils ().getExperienceLevel (unit, true, players, combatAreaEffects, db);
		if ((expLevel != null) &&
			((component == UnitAttributeComponent.EXPERIENCE) || (component == UnitAttributeComponent.ALL)) &&
			((!unitAttributeID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) || (basicValue > 0)))
		{
			for (final ExperienceAttributeBonus bonus : expLevel.getExperienceAttributeBonus ())
				if (bonus.getUnitAttributeID ().equals (unitAttributeID))
					total = total + addToAttributeValue (bonus.getBonusValue (), positiveNegative);
		}
		
		// Any bonuses from hero skills? (Might gives +melee, Constitution gives +hit points, Agility gives +defence, and so on)
		if ((expLevel != null) &&
			((component == UnitAttributeComponent.HERO_SKILLS) || (component == UnitAttributeComponent.ALL)))
			
			// Read down all the skills defined in the database looking for skills that grant a bonus to the attribute we're calculating
			for (final UnitSkill skillDef : db.getUnitSkills ())
				if (unitAttributeID.equals (skillDef.getAddsToAttributeID ()))
				{
					// Now see if the unit has that skill
					int multiplier = getModifiedSkillValue (unit, mergedSkills, skillDef.getUnitSkillID (), players, spells, combatAreaEffects, db);
					if (multiplier > 0)
					{
						// Multiplier will either equal 1 or 2, indicating whether we have the regular or super version of the skill - change this to be 2 for regular or 3 for super
						multiplier++;
						
						// Some skills take more than 1 level to gain 1 attribute point, so get this value
						final int divisor = (skillDef.getAddsToAttributeDivisor () == null) ? 1 : skillDef.getAddsToAttributeDivisor ();
						
						// Now can do the calculation
						final int bonus = ((expLevel.getLevelNumber () + 1) * multiplier) / (divisor*2);
						total = total + addToAttributeValue (bonus, positiveNegative);
					}
				}
		
		// Any bonuses due to spells/special effects in the location the unit is currently in?
		// Ditto, ranged attack bonuses only apply if we had a ranged attack to begin with
		if (((component == UnitAttributeComponent.COMBAT_AREA_EFFECTS) || (component == UnitAttributeComponent.ALL)) &&
			((!unitAttributeID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) || (basicValue > 0)))
		{
			final String storeMagicRealmLifeformTypeID = getUnitUtils ().getModifiedUnitMagicRealmLifeformTypeID (unit, mergedSkills, spells, db);
			for (final MemoryCombatAreaEffect effect : combatAreaEffects)
				if (getUnitUtils ().doesCombatAreaEffectApplyToUnit (unit, effect, db))
				{
					// Found a combat area effect whose location matches this unit, as well as any player or other pre-requisites.
					// So this means all the attribute bonuses apply, except we still need to do the magic realm check
					// since some effects have different components which apply to different lifeform types e.g. True Light and Darkness
					for (final CombatAreaEffectAttributeBonus bonus : db.findCombatAreaEffect (effect.getCombatAreaEffectID (), "getModifiedAttributeValue").getCombatAreaEffectAttributeBonus ())
						if (bonus.getUnitAttributeID ().equals (unitAttributeID))
						{
							// Magic realm/lifeform type can be blank for effects that apply to all types of unit (e.g. prayer)
							if ((bonus.getEffectMagicRealm () == null) || (bonus.getEffectMagicRealm ().equals (storeMagicRealmLifeformTypeID)))
								total = total + addToAttributeValue (bonus.getBonusValue (), positiveNegative);
						}
				}
		}
		
		log.trace ("Exiting getModifiedAttributeValue = " + total);
		return total;
	}

	/**
	 * @param unit Unit to look up the base upkeep for
	 * @param productionTypeID Production type we want to look up the modified upkeep for
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @return Upkeep value, modified by reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public final int getModifiedUpkeepValue (final AvailableUnit unit, final String productionTypeID, final List<? extends PlayerPublicDetails> players,
		final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException
	{
		log.trace ("Entering getModifiedUpkeepValue: " + unit.getUnitID () + ", " + productionTypeID);

		// Get base value
		final int baseUpkeepValue = getUnitUtils ().getBasicUpkeepValue (unit, productionTypeID, db);

		// Reduce upkeep for Summoner retort?
		final int upkeepValue;
		if (baseUpkeepValue <= 0)
			upkeepValue = baseUpkeepValue;
		else
		{
			// Get reduction as a percentage
			// Note we use the special "unit upkeep" production type, not "Mana"
			final PlayerPublicDetails owningPlayer = getMultiplayerSessionUtils ().findPlayerWithID (players, unit.getOwningPlayerID (), "getModifiedUpkeepValue");
			final List<PlayerPick> picks = ((MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ()).getPick ();

			final String unitMagicRealmID = db.findUnit (unit.getUnitID (), "getModifiedUpkeepValue").getUnitMagicRealm ();
			final String unitTypeID = db.findUnitMagicRealm (unitMagicRealmID, "getModifiedUpkeepValue").getUnitTypeID ();

			final int percentageReduction = getPlayerPickUtils ().totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, unitTypeID, picks, db);

			// Calculate actual amount of reduction, rounding down
			final int amountReduction = (baseUpkeepValue * percentageReduction) / 100;

			upkeepValue = baseUpkeepValue - amountReduction;
		}

		log.trace ("Exiting getModifiedUpkeepValue = " + upkeepValue);
		return upkeepValue;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
	
	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}
}