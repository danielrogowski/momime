package momime.common.utils;

import java.util.List;
import java.util.Map;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkillValueType;
import momime.common.database.CommonDatabase;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnitHeroItemSlot;

/**
 * Sections broken out from the big expandUnitDetails method to make it more manageable
 */
public interface ExpandUnitDetailsUtils
{
	/**
	 * Searches for other units we might get unit stack bonuses like Pathfinding or Holy Bonus from.
	 * If we're in combat, then the unit stack means all the other units who are in combat with us.
	 * If we aren't in combat, then the unit stack means the other units in the same overland map square as us.  
	 * 
	 * @param unit Unit we are calculating stats for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of minimal computed details for all units stacked with the unit whose stats we're calculating
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public List<MinimalUnitDetails> buildUnitStackMinimalDetails (final AvailableUnit unit,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * @param unitStack All units in the unit stack
	 * @return List of Unit URNs of the stacked units that are MemoryUnits
	 * @throws MomException If a unit claims to be a MemoryUnit but then won't provide its UnitURN
	 */
	public List<Integer> buildUnitStackUnitURNs (final List<MinimalUnitDetails> unitStack) throws MomException;
	
	/**
	 * @param unitStack All units in the unit stack
	 * @return Map of all valued skills in the stack, picking out the highest value of each skill
	 * @throws MomException If we try to get the value of a skill the unit does not have
	 */
	public Map<String, Integer> getHighestSkillsInUnitStack (final List<MinimalUnitDetails> unitStack) throws MomException;
	
	/**
	 * @param mu Unit we are calculating stats for
	 * @param spells List of known spells
	 * @param unitStackUnitURNs List of unit URNs in the stack
	 * @param basicSkillValues Map of all skills the unit we are calculating has; will be added to
	 * @param unitStackSkills Map of all valued skills in the stack, picking out the highest value of each skill; will be added to
	 * @param db Lookup lists built over the XML database
	 * @return Map of unitSkillIDs granted by spells, keyed by skill ID with the value being the ID of the player who cast it on the unit
	 * @throws RecordNotFoundException If we encounter a spell we can't find the definition for
	 * @throws MomException If a unit claims to be a MemoryUnit but then won't provide its UnitURN
	 */
	public Map<String, Integer> addSkillsFromSpells (final MinimalUnitDetails mu, final List<MemoryMaintainedSpell> spells, final List<Integer> unitStackUnitURNs,
		final Map<String, Integer> basicSkillValues, final Map<String, Integer> unitStackSkills, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Searches for valueless skills granted either from hero basic item types (Shields grant Large Shield skill) or from bonuses added to the hero item
	 * 
	 * @param slots The item slots on the unit, which may have items in them
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param basicSkillValues Map of all skills the unit we are calculating has; will be added to
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a hero item type or bonus not found in the database
	 */
	public void addValuelessSkillsFromHeroItems (final List<MemoryUnitHeroItemSlot> slots, final String attackFromSkillID,
		final Map<String, Integer> basicSkillValues, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * Checks over all skills we already have to see if any of those skills grant others, and in turn if those skills grant more, and so on.
	 * This is for things like Invulnerability granting Weapon Immunity, or Undead granting a whole pile of immunities.
	 * 
	 * @param basicSkillValues Map of all skills the unit we are calculating has; will be added to
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a skill not found in the database
	 */
	public void addSkillsGrantedFromOtherSkills (final Map<String, Integer> basicSkillValues, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * @param basicSkillValues Map of all skills the unit we are calculating has
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param db Lookup lists built over the XML database
	 * @return Copy of basicSkillValues, but with negated skills removed
	 * @throws RecordNotFoundException If we can't find a skill definition
	 * @throws MomException If a skill definition has an unknown negatedByUnitID value
	 */
	public Map<String, Integer> copySkillValuesRemovingNegatedSkills (final Map<String, Integer> basicSkillValues,
		final List<ExpandedUnitDetails> enemyUnits, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Skills like Undead and Chaos Channels can modify a unit's magic realm/lifeform type.  So this
	 * method works out what the unit's modified magic realm/lifeform type is.
	 * 
	 * @param defaultMagicRealmID The default magic realm/lifeform type that will be output if no overrides are found
	 * @param basicSkillValuesWithNegatedSkillsRemoved Map of all skills the unit we are calculating has, with negated skills already removed
	 * @param db Lookup lists built over the XML database
	 * @return Modified magic realm/lifeform
	 * @throws RecordNotFoundException If we can't find one of the unit skills or picks involved
	 * @throws MomException If we find override(s), but no magic realm/lifeform is defined for that combination of override(s)
	 */
	public Pick determineModifiedMagicRealmLifeformType (final String defaultMagicRealmID,
		final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * @param basicSkillValuesWithNegatedSkillsRemoved Map of all skills the unit we are calculating has, with negated skills already removed
	 * @return Initial skill breakdown map, with basic components added
	 */
	public Map<String, UnitSkillValueBreakdown> buildInitialBreakdownFromBasicSkills
		(final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved);

	/**
	 * Boats sail 50% faster if we have Wind Mastery cast, or 50% slower if someone else does.
	 * 
	 * @param basicSkillValues Map of all skills the unit we are calculating has
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param unitOwnerPlayerID Player who owns 
	 * @param spells List of known spells
	 */
	public void adjustMovementSpeedForWindMastery (final Map<String, Integer> basicSkillValues,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final int unitOwnerPlayerID, final List<MemoryMaintainedSpell> spells);
	
	/**
	 * @param mu Unit we are calculating stats for
	 * @param weaponGrade Weapon grade to add bonuses from
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param unitStackSkills Map of all valued skills in the stack, picking out the highest value of each skill
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public void addBonusesFromWeaponGrade (final MinimalUnitDetails mu, final WeaponGrade weaponGrade,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final Map<String, Integer> unitStackSkills,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID)
		throws MomException;

	/**
	 * @param expLvl Experience level to add bonuses from
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 */
	public void addBonusesFromExperienceLevel (final ExperienceLevel expLvl, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues);

	/**
	 * Adds skills granted by CAEs.  Note this are only ever valueless skills.  CAEs can't directly add value bonuses, but do so by
	 * granting a unit skill and then that unit skill defines the bonuses.
	 * 
	 * @param unit Unit we are calculating stats for
	 * @param combatAreaEffects List of combat area effects to add bonuses from
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @param db Lookup lists built over the XML database
	 * @return List of skills added that we didn't have before
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If a skill definition has an unknown negatedByUnitID value
	 */
	public List<String> addSkillsFromCombatAreaEffects (final AvailableUnit unit, final List<MemoryCombatAreaEffect> combatAreaEffects,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final List<ExpandedUnitDetails> enemyUnits,
		final String magicRealmLifeformTypeID, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Even though above method checks for skills granted from CAEs being negated before adding them, if combat has both Prayer and High Prayer
	 * cast, Prayer will be added first (at which point it isn't negated) and High Prayer after.  So now we need to go over the list again rechecking them.
	 * 
	 * @param skillsGrantedFromCombatAreaEffects List of skills granted from combat area effects
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If a skill definition has an unknown negatedByUnitID value
	 */
	public void removeNegatedSkillsAddedFromCombatAreaEffects (final List<String> skillsGrantedFromCombatAreaEffects,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final List<ExpandedUnitDetails> enemyUnits, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Adds bonuses from skills which grant bonuses to other skills, for example Flame Blade granting +2 to melee attack.
	 * 
	 * @param mu Unit we are calculating stats for
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param unitStackSkills Map of all valued skills in the stack, picking out the highest value of each skill
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @param addsToSkillValueTypes List of types of skill value modification to apply
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public void addBonusesFromOtherSkills (final MinimalUnitDetails mu, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues,
		final Map<String, Integer> unitStackSkills, final List<ExpandedUnitDetails> enemyUnits,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID, final CommonDatabase db,
		final List<AddsToSkillValueType> addsToSkillValueTypes)
		throws MomException;

	/**
	 * Searches for bonuses granted either from hero basic item types (Plate Mail grants +2 defence) or from bonuses added to the hero item
	 * 
	 * @param slots The item slots on the unit, which may have items in them
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	public void addBonusesFromHeroItems (final List<MemoryUnitHeroItemSlot> slots,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final String attackFromSkillID, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * For now this is somewhat hard coded and just for the Chaos attribute, which halves the strength of attacks appropriate for the item type.
	 * 
	 * @param mu Unit we are calculating stats for
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public void addPenaltiesFromHeroItems (final MinimalUnitDetails mu, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Adds penalties from skills which lock/divide/multiply other skills late in the calculation, for example Shatter locking melee attack at 1
	 * or Warp Creature dividing melee attack by 2.
	 * 
	 * @param mu Unit we are calculating stats for
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param unitStackSkills Map of all valued skills in the stack, picking out the highest value of each skill
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public void addPenaltiesFromOtherSkills (final MinimalUnitDetails mu, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues,
		final Map<String, Integer> unitStackSkills, final List<ExpandedUnitDetails> enemyUnits,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID, final CommonDatabase db)
		throws MomException;
	
	/**
	 * Modifies basic upkeep values for the unit according to skills like Noble and retorts like Summoner which may reduce it, or Undead which may increase or zero it.
	 * 
	 * @param mu Unit we are calculating stats for
	 * @param basicUpkeepValues Basic upkeep values, copied directly from the unit definition in the XML
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param db Lookup lists built over the XML database
	 * @return Modified upkeep values
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public Map<String, Integer> buildModifiedUpkeepValues (final MinimalUnitDetails mu, final Map<String, Integer> basicUpkeepValues,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * @param mu Unit we are calculating stats for
	 * @param skillsFromSpellsCastOnThisUnit Map of unitSkillIDs granted by spells, keyed by skill ID with the value being the ID of the player who cast it on the unit
	 * @return Which player controls this unit in combat this round
	 * @throws MomException If a unit claims to be a MemoryUnit but then fails to typecast itself
	 */
	public int determineControllingPlayerID (final MinimalUnitDetails mu, final Map<String, Integer> skillsFromSpellsCastOnThisUnit)
		throws MomException;
}