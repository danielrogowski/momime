package momime.common.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.AddsToSkillValueType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;

/**
 * Methods for working out minimal unit details
 */
public final class UnitDetailsUtilsImpl implements UnitDetailsUtils
{
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * Calculates minimal unit details that can be derived quickly without examining the whole unit stack.
	 * 
	 * @param unit Unit to expand skill list for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of all skills this unit has, with skills granted from other skills and skills granted from spells merged into the list
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final MinimalUnitDetails expandMinimalUnitDetails (final AvailableUnit unit,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// STEP 1 - First just copy the skills from the unit into a map
		// NB. can't just use Collectors.toMap () because this throws an exception if you have any null values (which we often will)
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		unit.getUnitHasSkill ().forEach (s -> basicSkillValues.put (s.getUnitSkillID (), s.getUnitSkillValue ()));
		
		// STEP 2 - Do simple lookups
		// Need the player picks to look for Crusade, and need the unit type to get their experience levels
		final UnitEx unitDef = db.findUnit (unit.getUnitID (), "expandMinimalUnitDetails");
		final PlayerPublicDetails owningPlayer = (unit.getOwningPlayerID () == 0) ? null : getMultiplayerSessionUtils ().findPlayerWithID (players, unit.getOwningPlayerID (), "expandMinimalUnitDetails");
		final List<PlayerPick> picks = (owningPlayer == null) ? null : ((MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ()).getPick ();
		
		final String unitTypeID = db.findPick (unitDef.getUnitMagicRealm (), "expandMinimalUnitDetails").getUnitTypeID ();
		final UnitType unitType = db.findUnitType (unitTypeID, "expandMinimalUnitDetails");
		
		// STEP 3 - Find the unit's experience level
		// Experience can never be increased by spells, combat area effects, weapon grades, etc. etc. therefore safe to do this from the basic skill value on the unmerged list
		Integer experienceSkillValue = basicSkillValues.get (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		final ExperienceLevel basicExpLvl;
		final ExperienceLevel modifiedExpLvl;
		if (experienceSkillValue == null)
		{
			basicExpLvl = null;		// This type of unit doesn't gain experience (e.g. summoned)
			modifiedExpLvl = null;
		}
		else
		{
			// Check to see if the unit has heroism cast on it
			// This is a special case from all the other "adds to skill" modifiers as it has to happen way earlier
			if (unit instanceof MemoryUnit)
			{
				final int unitURN = ((MemoryUnit) unit).getUnitURN ();
				for (final MemoryMaintainedSpell thisSpell : mem.getMaintainedSpell ())
					if ((thisSpell.getUnitURN () != null) && (thisSpell.getUnitURN () == unitURN) && (thisSpell.getUnitSkillID () != null))
					{
						final UnitSkillEx unitSkill = db.findUnitSkill (thisSpell.getUnitSkillID (), "expandMinimalUnitDetails");
						for (final AddsToSkill addsToSkill : unitSkill.getAddsToSkill ())
							if (addsToSkill.getAddsToSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE))
								switch (addsToSkill.getAddsToSkillValueType ())
								{
									case ADD_FIXED:
										experienceSkillValue = experienceSkillValue + addsToSkill.getAddsToSkillValue ();
										break;
										
									case LOCK:
										experienceSkillValue = addsToSkill.getAddsToSkillValue ();
										break;
										
									default:
										// Either kind of divide makes no sense for this
								}
					}
			}
			
			// Check all experience levels defined under the unit type
			// This checks them all so we aren't relying on them being defined in the correct orer
			ExperienceLevel levelFromExperience = null;
			for (final ExperienceLevel experienceLevel : unitType.getExperienceLevel ())

				// Careful - getExperienceRequired () can be null, for normal units' Ultra-Elite and Champion statuses
				// Levels that actually require 0 experience must state a 0 in the XML rather than omit the field
				if ((experienceLevel.getExperienceRequired () != null) && (experienceSkillValue >= experienceLevel.getExperienceRequired ()) &&
					((levelFromExperience == null) || (levelFromExperience.getLevelNumber () < experienceLevel.getLevelNumber ())))
						levelFromExperience = experienceLevel;

			// Check we got one
			if (levelFromExperience == null)
				throw new MomException ("Unit " + unit.getUnitID () + " of type " + unitTypeID + " with " + experienceSkillValue + " experience cannot find any appropriate experience level");

			// Now we've found the level that we're at due to actual experience, see if we get any level bonuses, i.e. Warlord retort or Crusade spell
			basicExpLvl = levelFromExperience;
			int levelIncludingBonuses = levelFromExperience.getLevelNumber ();

			// Does the player have the Warlord retort?
			if ((picks != null) && (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_WARLORD) > 0))
				levelIncludingBonuses++;

			// Does the player have the Crusade CAE?
			if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (mem.getCombatAreaEffect (), null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, unit.getOwningPlayerID ()) != null)
				levelIncludingBonuses++;

			// Now we have to ensure that the level we've attained actually exists, this is fine for units but a hero might reach Demi-God naturally,
			// then giving them +1 level on top of that will move them to an undefined level
			do
			{
				levelFromExperience = UnitTypeUtils.findExperienceLevel (unitType, levelIncludingBonuses);
				levelIncludingBonuses--;
			} while (levelFromExperience == null);

			modifiedExpLvl = levelFromExperience;
		}
		
		// Finally can build the unit object
		final MinimalUnitDetailsImpl container = new MinimalUnitDetailsImpl (unit, unitDef, unitType, owningPlayer, basicExpLvl, modifiedExpLvl, basicSkillValues);
		return container;
	}
	
	/**
	 * Adds on a bonus to a skill value, if it passes suitable checks.  Assumes the main check, that this bonus applies as a whole, has already passed.
	 * So for example if this bonus is from another skill, e.g. +2 defence from Holy Armour, that we have already checked that the unit does actually have Holy Armour;
	 * or if it this bonus is from a weapon grade, that we have checked that the unit does actually have that weapon grade. 
	 * 
	 * @param mu Minimal details for the unit calculated so far
	 * @param unitSkillID If this bonus is being added because one skill gives a bonus to another skill, this is the skill the bonus is being granted FROM
	 * 	null if this bonus is being granted from something other than a skill, e.g. a weapon grade 
	 * @param addsToSkill The details of the skill the bonus is applied TO and any associated conditions
	 * @param overrideComponent Component to add these bonuses as; null means work it out based on whether its a + or - and whether it affects whole stack or not
	 * @param modifiedSkillValues Map of skill values calculated for the unit so far
	 * @param unitStackSkills List of all skills that any unit stacked with the unit we are calculating has; if a numeric skill then indicates the highest value from the stack
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Calculated lifeform type for this unit, e.g. regular unit, or it has been chaos channeled
	 * @return Value describing whether the bonus was added or not, and if not, why not - just used for unit test
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final AddSkillBonusResult addSkillBonus (final MinimalUnitDetails mu, final String unitSkillID, final AddsToSkill addsToSkill, final UnitSkillComponent overrideComponent,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final Map<String, Integer> unitStackSkills,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID)
		throws MomException
	{
		final AddSkillBonusResult result;
		
		final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (addsToSkill.getAddsToSkillID ());
		if (breakdown == null)
			result = AddSkillBonusResult.UNIT_DOES_NOT_HAVE_SKILL;
		
		else if ((addsToSkill.getAddsToSkillValueType () != AddsToSkillValueType.ADD_FIXED) &&
			(addsToSkill.getAddsToSkillValueType () != AddsToSkillValueType.ADD_DIVISOR))
			result = AddSkillBonusResult.INCORRECT_TYPE_OF_ADJUSTMENT;
		
		else
		{
			final boolean isInCombat = mu.isMemoryUnit () ? (mu.getCombatLocation () != null) : false;
			
			// If we have no info about the kind of attack being made, or this isn't in reference to an attack at all, then discount the bonus
			// if it has any restrictions that depend on the kind of incoming attack, even if we match those restrictions.
			// This is to stop the bonus from Large Shield showing on the unit info screen.
			if ((attackFromSkillID == null) && (attackFromMagicRealmID == null) &&
				((addsToSkill.getOnlyVersusAttacksFromSkillID () != null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID () != null)))
			{
				// Ignore
				result = AddSkillBonusResult.NO_INFO_ABOUT_INCOMING_ATTACK;
			}
			
			// If the bonus only applies in combat, and we aren't in combat, then ignore it.
			else if ((addsToSkill.isOnlyInCombat () != null) && (addsToSkill.isOnlyInCombat ()) && (!isInCombat))
			{
				// Ignore
				result = AddSkillBonusResult.NOT_IN_COMBAT;
			}
			
			// If the bonus only applies to specific magic realms then ignore it if doesn't match
			else if ((addsToSkill.getOnlyAppliesToMagicRealmID () != null) && (!addsToSkill.getOnlyAppliesToMagicRealmID ().equals (magicRealmLifeformTypeID)))
			{
				// Ignore
				result = AddSkillBonusResult.WRONG_MAGIC_REALM_LIFEFORM_TYPE_ID;
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
				
				// Does the skill only apply to particular ranged attack types, and incoming skill IDs or incoming attacks of only particular magic realms?
				// (This deals with all conditional bonuses, such as Resist Elements, Large Shield or Flame Blade)
				if ((addsToSkill.getRangedAttackTypeID () != null) && (!addsToSkill.getRangedAttackTypeID ().equals (mu.getUnitDefinition ().getRangedAttackType ())))
					result = AddSkillBonusResult.WRONG_RANGED_ATTACK_TYPE;
				
				else if ((addsToSkill.getOnlyVersusAttacksFromSkillID () != null) && (!onlyVersusAttacksFromSkillIDCheckPasses))
					result = AddSkillBonusResult.WRONG_ATTACK_SKILL;

				else if ((addsToSkill.getOnlyVersusAttacksFromMagicRealmID () != null) && (!addsToSkill.getOnlyVersusAttacksFromMagicRealmID ().equals (attackFromMagicRealmID)))
					result = AddSkillBonusResult.WRONG_ATTACK_MAGIC_REALM;
				
				else
				{
					result = AddSkillBonusResult.APPLIES;
					
					// How is the bonus calculated - fixed value, value from the skill, etc
					UnitSkillComponent component;
					if (overrideComponent != null)
						component = overrideComponent;
					else if ((addsToSkill.isPenaltyToEnemy () != null) && (addsToSkill.isPenaltyToEnemy ()))
						component = UnitSkillComponent.PENALTIES;
					else
						component = addsToSkill.isAffectsEntireStack () ? UnitSkillComponent.STACK : UnitSkillComponent.SPELL_EFFECTS;
					
					// Any fixed bonuses from one skill to another?  e.g. Holy Armour gives +2 to defence
					final int bonus;
					if ((addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.ADD_FIXED) && (addsToSkill.getAddsToSkillValue () != null))
						bonus = addsToSkill.getAddsToSkillValue ();
					
					// Divisors are only applicable when adding a value based on another skill
					else if (unitSkillID == null)
						bonus = 0;
					
					else
					{
						// For other kinds, we must have a value for the skill - e.g. Constitution 1 or Holy Bonus 2
						int multiplier;
						if (addsToSkill.isAffectsEntireStack ())
							multiplier = unitStackSkills.get (unitSkillID);
						else
						{
							multiplier = 0;
							final UnitSkillValueBreakdown totalComponents = modifiedSkillValues.get (unitSkillID);
							if (totalComponents != null)
								for (final Entry<UnitSkillComponent, Integer> c : totalComponents.getComponents ().entrySet ())
									if (c.getValue () == null)
										throw new MomException ("expandUnitDetails on " + mu.getUnitID () + " trying to sum addsFromSkill ID " + unitSkillID + " for bonuses but the " + c.getKey () + " component is null");
									else
										multiplier = multiplier + c.getValue ();
						}
						
						if (multiplier <= 0)
							bonus = 0;
						
						// Use already calculated highest value from the stack
						else if ((addsToSkill.isAffectsEntireStack ()) && (addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.ADD_DIVISOR) &&
							(addsToSkill.getAddsToSkillValue () != null))
						{
							component = UnitSkillComponent.STACK;
							
							// Multiplier has already been set to highest (level + 1) * (normal=2 or super=3) for highest value in the stack, see getBasicOrHeroSkillValue
							bonus = multiplier / (addsToSkill.getAddsToSkillValue () * 2);
						}
						
						// Any bonuses from hero skills? (Might gives +melee, Constitution gives +hit points, Agility gives +defence, and so on)
						else if ((addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.ADD_DIVISOR) && (addsToSkill.getAddsToSkillValue () != null))
						{
							component = UnitSkillComponent.HERO_SKILLS;
							if (mu.getModifiedExperienceLevel () != null)
							{
								// Multiplier will either equal 1 or 2, indicating whether we have the regular or super version of the skill - change this to be 2 for regular or 3 for super
								multiplier++;
								
								// Now can do the calculation
								bonus = ((mu.getModifiedExperienceLevel ().getLevelNumber () + 1) * multiplier) / (addsToSkill.getAddsToSkillValue () * 2);
							}
							else
								bonus = 0;
						}
						
						
						// Neither divisor nor fixed value specified, so the value must come from the skill itself
						else
							bonus = multiplier;
					}
					
					if (bonus != 0)
					{
						Integer bonusValue = breakdown.getComponents ().get (component);
						bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonus;
						breakdown.getComponents ().put (component, bonusValue);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Adds on a penalty to a skill value, if it passes suitable checks.  Assumes the main check, that this penalty applies as a whole, has already passed.
	 * Strictly speaking, these are not so much split in terms of bonus+penalty as much as the type of modification done.  This is really just handling
	 * adjustments that must be done late in the calculation, so for example if one modification adds +2 to a value but another then locks it at 1,
	 * the locking must be done late otherwise we might end up with 3.  The addSkillBonus method is also used to add penalties like ADD_FIXED with value -2,
	 * they are just a different kind of penalty.
	 * 
	 * @param mu Minimal details for the unit calculated so far
	 * @param addsToSkill The details of the skill the bonus is applied TO and any associated conditions
	 * @param modifiedSkillValues Map of skill values calculated for the unit so far
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Calculated lifeform type for this unit, e.g. regular unit, or it has been chaos channeled
	 * @return Value describing whether the penalty was added or not, and if not, why not - just used for unit test
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final AddSkillBonusResult addSkillPenalty (final MinimalUnitDetails mu, final AddsToSkill addsToSkill, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID)
		throws MomException
	{
		final AddSkillBonusResult result;
		
		final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (addsToSkill.getAddsToSkillID ());
		if (breakdown == null)
			result = AddSkillBonusResult.UNIT_DOES_NOT_HAVE_SKILL;
		
		// Note the value == null check is here, isn't in the bonuses method above, because we assume we won't get skills like "shatter 1" vs "shatter 2"
		// which set the level of penalty, whereas bonuses can have the value of the bonus sourced from places other than the AddsToSkill defintion,
		// like Holy Bonus where the value of the bonus comes from the skill value
		else if ((addsToSkill.getAddsToSkillValue () == null) || ((addsToSkill.getAddsToSkillValueType () != AddsToSkillValueType.LOCK) &&
			(addsToSkill.getAddsToSkillValueType () != AddsToSkillValueType.DIVIDE) && (addsToSkill.getAddsToSkillValueType () != AddsToSkillValueType.MULTIPLY)))
			result = AddSkillBonusResult.INCORRECT_TYPE_OF_ADJUSTMENT;
		
		else
		{
			final boolean isInCombat = mu.isMemoryUnit () ? (mu.getCombatLocation () != null) : false;
			
			// If we have no info about the kind of attack being made, or this isn't in reference to an attack at all, then discount the bonus
			// if it has any restrictions that depend on the kind of incoming attack, even if we match those restrictions.
			// This is to stop the bonus from Large Shield showing on the unit info screen.
			if ((attackFromSkillID == null) && (attackFromMagicRealmID == null) &&
				((addsToSkill.getOnlyVersusAttacksFromSkillID () != null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID () != null)))
			{
				// Ignore
				result = AddSkillBonusResult.NO_INFO_ABOUT_INCOMING_ATTACK;
			}
			
			// If the bonus only applies in combat, and we aren't in combat, then ignore it.
			else if ((addsToSkill.isOnlyInCombat () != null) && (addsToSkill.isOnlyInCombat ()) && (!isInCombat))
			{
				// Ignore
				result = AddSkillBonusResult.NOT_IN_COMBAT;
			}
			
			// If the bonus only applies to specific magic realms then ignore it if doesn't match
			else if ((addsToSkill.getOnlyAppliesToMagicRealmID () != null) && (!addsToSkill.getOnlyAppliesToMagicRealmID ().equals (magicRealmLifeformTypeID)))
			{
				// Ignore
				result = AddSkillBonusResult.WRONG_MAGIC_REALM_LIFEFORM_TYPE_ID;
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
				
				// Does the skill only apply to particular ranged attack types, and incoming skill IDs or incoming attacks of only particular magic realms?
				// (This deals with all conditional bonuses, such as Resist Elements, Large Shield or Flame Blade)
				if ((addsToSkill.getRangedAttackTypeID () != null) && (!addsToSkill.getRangedAttackTypeID ().equals (mu.getUnitDefinition ().getRangedAttackType ())))
					result = AddSkillBonusResult.WRONG_RANGED_ATTACK_TYPE;
				
				else if ((addsToSkill.getOnlyVersusAttacksFromSkillID () != null) && (!onlyVersusAttacksFromSkillIDCheckPasses))
					result = AddSkillBonusResult.WRONG_ATTACK_SKILL;

				else if ((addsToSkill.getOnlyVersusAttacksFromMagicRealmID () != null) && (!addsToSkill.getOnlyVersusAttacksFromMagicRealmID ().equals (attackFromMagicRealmID)))
					result = AddSkillBonusResult.WRONG_ATTACK_MAGIC_REALM;
				
				else
				{
					result = AddSkillBonusResult.APPLIES;
					
					// How is the bonus calculated - fixed value, value from the skill, etc
					UnitSkillComponent component;
					if ((addsToSkill.isPenaltyToEnemy () != null) && (addsToSkill.isPenaltyToEnemy ()))
						component = UnitSkillComponent.PENALTIES;
					else
						component = addsToSkill.isAffectsEntireStack () ? UnitSkillComponent.STACK : UnitSkillComponent.SPELL_EFFECTS;
					
					// Set to a fixed value?
					int currentSkillValue = 0;
					for (final Entry<UnitSkillComponent, Integer> c : breakdown.getComponents ().entrySet ())
						if (c.getValue () == null)
							throw new MomException ("expandUnitDetails on " + mu.getUnitID () + " trying to sum addsFromSkill ID " + addsToSkill.getAddsToSkillID () + " for penalties but the " + c.getKey () + " component is null");
						else
							currentSkillValue = currentSkillValue + c.getValue ();
					
					final int newValue;
					if (addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.LOCK)
						newValue = addsToSkill.getAddsToSkillValue ();
					
					// Divide by a value?
					else if (addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.DIVIDE)
						newValue = currentSkillValue / addsToSkill.getAddsToSkillValue ();
					
					// Multiply by a value?
					else
						newValue = currentSkillValue * addsToSkill.getAddsToSkillValue ();

					// LOCK and DIVIDE are used as penalties, so never allow these to improve an already bad stat
					final int bonus;
					if ((currentSkillValue < newValue) && (addsToSkill.getAddsToSkillValueType () != AddsToSkillValueType.MULTIPLY))
						bonus = 0;
					else
						bonus = newValue - currentSkillValue;
					
					if (bonus != 0)
					{
						Integer bonusValue = breakdown.getComponents ().get (component);
						bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonus;
						breakdown.getComponents ().put (component, bonusValue);
					}
				}
			}
		}
		
		return result;
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
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
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