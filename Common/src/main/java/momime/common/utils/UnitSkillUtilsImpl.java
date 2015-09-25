package momime.common.utils;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.AddsToSkill;
import momime.common.database.CombatAreaEffectSkillBonus;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.ExperienceSkillBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitHasSkill;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.WeaponGradeSkillBonus;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;

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
	 * @param value Value of 1 skill component
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @return value, if positive/negative as desired, or 0 if not wanted
	 */
	final int addToSkillValue (final int value, final UnitSkillPositiveNegative positiveNegative)
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
				throw new UnsupportedOperationException ("addToSkillValue doesn't know how to handle " + positiveNegative);
		}
		return result;
	}
	
	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param unitSkillID Unique identifier for this skill
	 * @param component Which component(s) making up this attribute to calculate
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Value of the specified skill - base value can be improved by weapon grades, experience or CAEs (e.g. Node Auras or Prayer), or can be reduced by curses or enemy CAEs (e.g. Black Prayer);
	 * 	Returns -1 if the unit doesn't have the skill
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit; or a bonus applies that we cannot determine the amount of
	 */
	@Override
	public final int getModifiedSkillValue (final AvailableUnit unit, final List<UnitHasSkill> skills, final String unitSkillID,
		final UnitSkillComponent component, final UnitSkillPositiveNegative positiveNegative, final List<? extends PlayerPublicDetails> players,
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
		
		// If we don't have the skill at all, then return -1 regardless of which components were asked for.
		// Also if it is a value-less skill like a movement skill, then no bonuses or penalities can apply to it so we just return 0, regardless of which components were asked for.
		int total;
		if (basicValue <= 0)
			total = basicValue;
		else
		{
			// Include basic value in total?
			total = 0;
			if ((component == UnitSkillComponent.BASIC) || (component == UnitSkillComponent.ALL))
				total = total + addToSkillValue (basicValue, positiveNegative);
			
			// Exclude experience, otherwise we get a repetitive loop as the call to expLvl = getExperienceLevel () lower down calls getSkillValue!
			if (!unitSkillID.equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE))
			{
				// Any bonuses due to weapon grades?
				if ((unit.getWeaponGrade () != null) &&
					((component == UnitSkillComponent.WEAPON_GRADE) || (component == UnitSkillComponent.ALL)))
				{
					// Only certain types of ranged attack get bonuses from Mithril and Adamantium weapons - e.g. bows do, magical blasts do not
					final boolean weaponGradeBonusApplies;
					if (unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
					{
						final Unit unitDefinition = db.findUnit (unit.getUnitID (), "getModifiedSkillValue");
						if (unitDefinition.getRangedAttackType () == null)
							weaponGradeBonusApplies = false;
						else
							weaponGradeBonusApplies = db.findRangedAttackType (unitDefinition.getRangedAttackType (), "getModifiedSkillValue").isMithrilAndAdamantiumVersions ();
					}
					else
						weaponGradeBonusApplies = true;
					
					if (weaponGradeBonusApplies)					
						for (final WeaponGradeSkillBonus bonus : db.findWeaponGrade (unit.getWeaponGrade (), "getModifiedSkillValue").getWeaponGradeSkillBonus ())
							if (bonus.getUnitSkillID ().equals (unitSkillID))
								total = total + addToSkillValue (bonus.getBonusValue (), positiveNegative);
				}
	
				// Any bonuses due to experience?
				final ExperienceLevel expLvl = getUnitUtils ().getExperienceLevel (unit, true, players, combatAreaEffects, db);
				if ((expLvl != null) &&
					((component == UnitSkillComponent.EXPERIENCE) || (component == UnitSkillComponent.ALL)))
					
					for (final ExperienceSkillBonus bonus : expLvl.getExperienceSkillBonus ())
						if (bonus.getUnitSkillID ().equals (unitSkillID))
							total = total + addToSkillValue (bonus.getBonusValue (), positiveNegative);
				
				// Any bonuses from skills that add to another skill, either hero skills or spell effects?
				if ((component == UnitSkillComponent.HERO_SKILLS) || (component == UnitSkillComponent.SPELL_EFFECTS) || (component == UnitSkillComponent.ALL))
					
					// Read down all the skills defined in the database looking for skills that grant a bonus to the attribute we're calculating
					for (final UnitSkill skillDef : db.getUnitSkills ())
						for (final AddsToSkill addsToSkill : skillDef.getAddsToSkill ())
							if (unitSkillID.equals (addsToSkill.getAddsToSkillID ()))
							{
								// Now see if the unit has that skill
								int multiplier = getModifiedSkillValue (unit, mergedSkills, skillDef.getUnitSkillID (),
									UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db);
								if (multiplier >= 0)
								{
									// Defining both isn't valid
									if ((addsToSkill.getAddsToSkillDivisor () != null) && (addsToSkill.getAddsToSkillFixed () != null))
										throw new MomException ("Unit skill " + skillDef.getUnitSkillID () + " adds to skill " + addsToSkill.getAddsToSkillID () +
											" but specifies both a level divisor and a fixed amount");
									
									// Any bonuses from hero skills? (Might gives +melee, Constitution gives +hit points, Agility gives +defence, and so on)
									else if (addsToSkill.getAddsToSkillDivisor () != null)
									{
										if ((expLvl != null) && (multiplier > 0) &&
											((component == UnitSkillComponent.HERO_SKILLS) || (component == UnitSkillComponent.ALL)))
										{
											// Multiplier will either equal 1 or 2, indicating whether we have the regular or super version of the skill - change this to be 2 for regular or 3 for super
											multiplier++;
											
											// Some skills take more than 1 level to gain 1 attribute point, so get this value
											final int divisor = (addsToSkill.getAddsToSkillDivisor () == null) ? 1 : addsToSkill.getAddsToSkillDivisor ();
											
											// Now can do the calculation
											final int bonus = ((expLvl.getLevelNumber () + 1) * multiplier) / (divisor*2);
											total = total + addToSkillValue (bonus, positiveNegative);
										}
									}
									
									// Any fixed bonuses from one skill to another?  e.g. Holy Armour gives +2 to defence
									else if (addsToSkill.getAddsToSkillFixed () != null)
									{
										if ((component == UnitSkillComponent.SPELL_EFFECTS) || (component == UnitSkillComponent.ALL))
											total = total + addToSkillValue (addsToSkill.getAddsToSkillFixed (), positiveNegative);
									}
									
									// Neither divisor nor fixed value specified, so the value must come from the skill itself
									else if ((multiplier > 0) && ((component == UnitSkillComponent.SPELL_EFFECTS) || (component == UnitSkillComponent.ALL)))
										total = total + multiplier;
								}
							}
				
				// Any bonuses from CAEs?
				if ((component == UnitSkillComponent.COMBAT_AREA_EFFECTS) || (component == UnitSkillComponent.ALL))
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
		
									total = total + addToSkillValue (caeBonusCache.getBonusValue (), positiveNegative);
						}
			}
		}

		log.trace ("Exiting getModifiedSkillValue = " + total);
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