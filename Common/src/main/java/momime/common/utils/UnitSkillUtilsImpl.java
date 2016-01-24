package momime.common.utils;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
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
import momime.common.database.HeroItemType;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.HeroItemTypeAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;

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
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value of the specified skill - base value can be improved by weapon grades, experience or CAEs (e.g. Node Auras or Prayer), or can be reduced by curses or enemy CAEs (e.g. Black Prayer);
	 * 	Returns -1 if the unit doesn't have the skill
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit; or a bonus applies that we cannot determine the amount of
	 */
	@Override
	public final int getModifiedSkillValue (final AvailableUnit unit, final List<UnitSkillAndValue> skills, final String unitSkillID,
		final UnitSkillComponent component, final UnitSkillPositiveNegative positiveNegative, final String attackFromSkillID, final String attackFromMagicRealmID,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering getModifiedSkillValue: " + unit.getUnitID () + ", " + unitSkillID + ", " + attackFromSkillID + ", " + attackFromMagicRealmID);

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitSkillAndValue> mergedSkills;
		if ((unit instanceof MemoryUnit) && (!(skills instanceof UnitHasSkillMergedList)))
			mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (mem.getMaintainedSpell (), (MemoryUnit) unit, db);
		else
			mergedSkills = skills;

		// Get unit magic realm ID
		final String storeMagicRealmLifeformTypeID = getUnitUtils ().getModifiedUnitMagicRealmLifeformTypeID (unit, mergedSkills, mem.getMaintainedSpell (), db);

		// Get basic skill value
		final int basicValue = getUnitUtils ().getBasicSkillValue (mergedSkills, unitSkillID);
		
		// The majority of skills, if we don't have the skill at all, then bonuses don't apply to it.
		// Also if it is a value-less skill like a movement skill, then no bonuses or penalities can apply to it so we just return 0, regardless of which components were asked for.
		// e.g. Settlers have no melee attack - just because they might gain 20 exp doesn't mean they start attacking with their pitchforks.
		// e.g. Units with no ranged attack don't suddenly gain one.
		// e.g. Phantom Warriors have no defence, but this is in the nature of the type of unit, and I think it makes sense to not allow them to gain a defence thru bonuses.
		// Movement speed, HP and Resistance are N/A here, because all units MUST define a value for those (see ServerDatabaseExImpl.consistencyChecks ())
		// So the two that are left, that we must treat differently, are + to hit and + to block.  Most units don't have those values defined, but bonuses definitely still apply.
		int total;
		if ((basicValue <= 0) && (!unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT)) &&
			(!unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK)))
				
			total = basicValue;
		else
		{
			// Include basic value in total?
			total = 0;
			if ((basicValue > 0) && ((component == UnitSkillComponent.BASIC) || (component == UnitSkillComponent.ALL)))
				total = total + addToSkillValue (basicValue, positiveNegative);
			
			// Exclude experience, otherwise we get a repetitive loop as the call to expLvl = getExperienceLevel () lower down calls getSkillValue!
			if (!unitSkillID.equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE))
			{
				final Unit unitDefinition = db.findUnit (unit.getUnitID (), "getModifiedSkillValue");
				
				// Any bonuses due to weapon grades?
				if ((unit.getWeaponGrade () != null) &&
					((component == UnitSkillComponent.WEAPON_GRADE) || (component == UnitSkillComponent.ALL)))
				{
					// Only certain types of ranged attack get bonuses from Mithril and Adamantium weapons - e.g. bows do, magical blasts do not
					final boolean weaponGradeBonusApplies;
					if (unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
					{
						if (unitDefinition.getRangedAttackType () == null)
							weaponGradeBonusApplies = false;
						else
							weaponGradeBonusApplies = db.findRangedAttackType (unitDefinition.getRangedAttackType (), "getModifiedSkillValue").isMithrilAndAdamantiumVersions ();
					}
					else
						weaponGradeBonusApplies = true;
					
					if (weaponGradeBonusApplies)					
						for (final UnitSkillAndValue bonus : db.findWeaponGrade (unit.getWeaponGrade (), "getModifiedSkillValue").getWeaponGradeSkillBonus ())
							if ((bonus.getUnitSkillID ().equals (unitSkillID)) && (bonus.getUnitSkillValue () != null))
								total = total + addToSkillValue (bonus.getUnitSkillValue (), positiveNegative);
				}
	
				// Any bonuses due to experience?
				final ExperienceLevel expLvl = getUnitUtils ().getExperienceLevel (unit, true, players, mem.getCombatAreaEffect (), db);
				if ((expLvl != null) &&
					((component == UnitSkillComponent.EXPERIENCE) || (component == UnitSkillComponent.ALL)))
					
					for (final UnitSkillAndValue bonus : expLvl.getExperienceSkillBonus ())
						if ((bonus.getUnitSkillID ().equals (unitSkillID)) && (bonus.getUnitSkillValue () != null))
							total = total + addToSkillValue (bonus.getUnitSkillValue (), positiveNegative);
				
				// Any bonuses from skills that add to another skill, either hero skills or spell effects?
				if ((component == UnitSkillComponent.HERO_SKILLS) || (component == UnitSkillComponent.SPELL_EFFECTS) ||
					(component == UnitSkillComponent.SPELL_EFFECTS_STACK) || (component == UnitSkillComponent.ALL))
				{
					final MapCoordinates3DEx unitCombatLocation = (unit instanceof MemoryUnit) ? (MapCoordinates3DEx) ((MemoryUnit) unit).getCombatLocation () : null;
					
					// Read down all the skills defined in the database looking for skills that grant a bonus to the attribute we're calculating
					for (final UnitSkill skillDef : db.getUnitSkills ())
						for (final AddsToSkill addsToSkill : skillDef.getAddsToSkill ())
							
							// If we have no info about the kind of attack being made, or this isn't in reference to an attack at all, then discount the bonus
							// if it has any restrictions that depend on the kind of incoming attack, even if we match those restrictions.
							// This is to stop the bonus from Large Shield showing on the unit info screen.
							if ((attackFromSkillID == null) && (attackFromMagicRealmID == null) &&
								(addsToSkill.getOnlyVersusAttacksFromSkillID () != null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID () != null))
							{
								// Ignore
							}
							else
							{
								// Deal with negative checks first, so the "if" below doesn't get too complicated; the value here is irrelevant if onlyVersusAttacksFromSkillID is null
								// NB. attackFromSkillID of "null" (i.e. spell based attacks) are considered as not matching the required skill ID
								// which is what we want - so Large Shield DOES give its bonus against incoming spell attacks such as Fire Bolt
								boolean onlyVersusAttacksFromSkillIDCheckPasses = (addsToSkill.getOnlyVersusAttacksFromSkillID () != null) &&
									(addsToSkill.getOnlyVersusAttacksFromSkillID ().equals (attackFromSkillID));
								if ((addsToSkill.isNegateOnlyVersusAttacksFromSkillID () != null) && (addsToSkill.isNegateOnlyVersusAttacksFromSkillID ()))
									onlyVersusAttacksFromSkillIDCheckPasses = !onlyVersusAttacksFromSkillIDCheckPasses;
								
								// Does this skill add to the skill we're calculating?  Also filter out skills that aren't the type (breakdown component) that we're looking for
								// Also does the skill only apply to particular ranged attack types, and incoming skill IDs or incoming attacks of only particular magic realms?
								// (This deals with all conditional bonuses, such as Resist Elements, Large Shield or Flame Blade)
								if ((unitSkillID.equals (addsToSkill.getAddsToSkillID ())) &&
									((addsToSkill.getRangedAttackTypeID () == null) || (addsToSkill.getRangedAttackTypeID ().equals (unitDefinition.getRangedAttackType ()))) &&
									((addsToSkill.getOnlyVersusAttacksFromSkillID () == null) || (onlyVersusAttacksFromSkillIDCheckPasses)) &&
									((addsToSkill.getOnlyVersusAttacksFromMagicRealmID () == null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID ().equals (attackFromMagicRealmID))) &&
									((component == UnitSkillComponent.ALL) ||
									(addsToSkill.isAffectsEntireStack () && (component == UnitSkillComponent.SPELL_EFFECTS_STACK)) ||
									(!addsToSkill.isAffectsEntireStack () && ((component == UnitSkillComponent.SPELL_EFFECTS) || (component == UnitSkillComponent.HERO_SKILLS)))))
								{
									// Now see if the unit has that skill; or any unit in the stack, as appropriate
									int multiplier;
									if (addsToSkill.isAffectsEntireStack ())
										multiplier = getHighestModifiedSkillValue ((MapCoordinates3DEx) unit.getUnitLocation (), unitCombatLocation, unit.getOwningPlayerID (),
											skillDef.getUnitSkillID (), players, mem, db);
									else
										multiplier = getModifiedSkillValue (unit, mergedSkills, skillDef.getUnitSkillID (), UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
											attackFromSkillID, attackFromMagicRealmID, players, mem, db);
									
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
											if ((component == UnitSkillComponent.ALL) ||
												(addsToSkill.isAffectsEntireStack () && (component == UnitSkillComponent.SPELL_EFFECTS_STACK)) ||
												(!addsToSkill.isAffectsEntireStack () && (component == UnitSkillComponent.SPELL_EFFECTS)))
														
												total = total + addToSkillValue (addsToSkill.getAddsToSkillFixed (), positiveNegative);
										}
										
										// Neither divisor nor fixed value specified, so the value must come from the skill itself
										else if ((multiplier > 0) && ((component == UnitSkillComponent.ALL) ||
											(addsToSkill.isAffectsEntireStack () && (component == UnitSkillComponent.SPELL_EFFECTS_STACK)) ||
											(!addsToSkill.isAffectsEntireStack () && (component == UnitSkillComponent.SPELL_EFFECTS))))
											
											total = total + multiplier;
									}
								}
							}
				}
				
				// Any bonuses from CAEs?
				if ((component == UnitSkillComponent.COMBAT_AREA_EFFECTS) || (component == UnitSkillComponent.ALL))
					for (final MemoryCombatAreaEffect thisCAE : mem.getCombatAreaEffect ())
						if (getUnitUtils ().doesCombatAreaEffectApplyToUnit (unit, thisCAE, db))
						{
							// Found a combat area effect whose location matches this unit, as well as any player or other pre-requisites
							// So this means all the skill bonuses apply, except we still need to do the magic realm
							// check since some effects have different components which apply to different lifeform types, e.g. True Light and Darkness
							for (final CombatAreaEffectSkillBonus caeBonusCache : db.findCombatAreaEffect (thisCAE.getCombatAreaEffectID (), "getModifiedSkillValue").getCombatAreaEffectSkillBonus ())
		
								// Magic realm/lifeform type can be blank for effects that apply to all types of unit (e.g. Prayer)
								if ((caeBonusCache.getUnitSkillID ().equals (unitSkillID)) && (caeBonusCache.getUnitSkillValue () != null) &&
									((caeBonusCache.getEffectMagicRealm () == null) || (caeBonusCache.getEffectMagicRealm ().equals (storeMagicRealmLifeformTypeID))))
		
									total = total + addToSkillValue (caeBonusCache.getUnitSkillValue (), positiveNegative);
						}
				
				// Any bonuses from hero items?
				if ((unit instanceof MemoryUnit) && ((component == UnitSkillComponent.HERO_ITEMS) || (component == UnitSkillComponent.ALL)))
					for (final MemoryUnitHeroItemSlot slot : ((MemoryUnit) unit).getHeroItemSlot ())
						if (slot.getHeroItem () != null)
						{
							// Natural effects of the item type, e.g. +2 defence from plate mail
							final HeroItemType heroItemType = db.findHeroItemType (slot.getHeroItem ().getHeroItemTypeID (), "getModifiedSkillValue");
							for (final UnitSkillAndValue basicStat : heroItemType.getHeroItemTypeBasicStat ())
								if (basicStat.getUnitSkillID ().equals (unitSkillID))
									total = total + addToSkillValue (basicStat.getUnitSkillValue (), positiveNegative);
							
							// Bonuses imbued on the item
							for (final HeroItemTypeAllowedBonus bonus : slot.getHeroItem ().getHeroItemChosenBonus ())
								for (final UnitSkillAndValue bonusStat : db.findHeroItemBonus (bonus.getHeroItemBonusID (), "getModifiedSkillValue").getHeroItemBonusStat ())
									
									// +Attack bonus - these apply to one or more specific skills, depending on the type of the item.
									// This deals with that e.g. swords don't add to "Thrown Weapons" skill but axes do.
									if (bonusStat.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_ATTACK_APPROPRIATE_FOR_TYPE_OF_HERO_ITEM))
									{
										for (final HeroItemTypeAttackType attackSkill : heroItemType.getHeroItemTypeAttackType ())
											if (attackSkill.getUnitSkillID ().equals (unitSkillID))
												total = total + addToSkillValue (bonusStat.getUnitSkillValue (), positiveNegative);
									}
				
									// Regular bonus
									else if (bonusStat.getUnitSkillID ().equals (unitSkillID))
										total = total + addToSkillValue (bonusStat.getUnitSkillValue (), positiveNegative);
						}
			}
			
			// If we were searching for a + to hit or + to block bonus and never found one, and the unit didn't have the skill to begin with, then revert to returning -1
			if ((total == 0) && (basicValue < 0))
				total = basicValue;
		}

		log.trace ("Exiting getModifiedSkillValue = " + total);
		return total;
	}
	
	/**
	 * @param unitLocation Location where the unit stack is
	 * @param unitCombatLocation The combat the unit stack is in, if any
	 * @param owningPlayerID The player who owns the unit stack
	 * @param unitSkillID Unique identifier for this skill
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Highest value of the specified skill from any unit in the stack; or -1 if no unit in the stack has the skill
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit; or a bonus applies that we cannot determine the amount of
	 */
	final int getHighestModifiedSkillValue (final MapCoordinates3DEx unitLocation, final MapCoordinates3DEx unitCombatLocation, final int owningPlayerID,
		final String unitSkillID, final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering getHighestModifiedSkillValue: " + unitLocation + ", " + unitCombatLocation + ", " + owningPlayerID + ", " + unitSkillID);
		
		int highest = -1;
		if (unitLocation != null)
			for (final MemoryUnit thisUnit : mem.getUnit ())
				if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () == owningPlayerID) && (unitLocation.equals (thisUnit.getUnitLocation ())) &&
					(((unitCombatLocation == null) && (thisUnit.getCombatLocation () == null)) ||
					((unitCombatLocation != null) && (unitCombatLocation.equals (thisUnit.getCombatLocation ())))))
					
					// This is used for "Resistance to All" and "Holy Bonus" rather than bonuses that depends on the kind of incoming attack
					// so its good enough to just pass nulls in for the attack values
					highest = Math.max (highest, getModifiedSkillValue (thisUnit, thisUnit.getUnitHasSkill (), unitSkillID,
						UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db));

		log.trace ("Exiting getHighestModifiedSkillValue = " + highest);
		return highest;
	}
	
	/**
	 * @param unit Unit to look up the base upkeep for
	 * @param productionTypeID Production type we want to look up the modified upkeep for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Upkeep value, modified by reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 * @throws MomException If we cannot find any appropriate experience level for this unit; or a bonus applies that we cannot determine the amount of
	 */
	@Override
	public final int getModifiedUpkeepValue (final AvailableUnit unit, final String productionTypeID, final List<? extends PlayerPublicDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		log.trace ("Entering getModifiedUpkeepValue: " + unit.getUnitID () + ", " + productionTypeID);

		// Get base value
		int baseUpkeepValue = getUnitUtils ().getBasicUpkeepValue (unit, productionTypeID, db);
		
		// Upkeep for undead is zeroed for normal units and adds +50% for summoned creatures
		if ((baseUpkeepValue > 0) && (getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_UNDEAD,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db) >= 0))
		{
			final String unitMagicRealmID = db.findUnit (unit.getUnitID (), "getModifiedUpkeepValue").getUnitMagicRealm ();
			final String unitTypeID = db.findPick (unitMagicRealmID, "getModifiedUpkeepValue").getUnitTypeID ();
			final UnitType unitType = db.findUnitType (unitTypeID, "getModifiedUpkeepValue");
			
			baseUpkeepValue = (baseUpkeepValue * unitType.getUndeadUpkeepPercentage ()) / 100; 
		}

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
			final String unitTypeID = db.findPick (unitMagicRealmID, "getModifiedUpkeepValue").getUnitTypeID ();

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