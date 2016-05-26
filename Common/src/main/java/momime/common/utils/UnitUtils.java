package momime.common.utils;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CommonDatabase;
import momime.common.database.ExperienceLevel;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitDamage;

/**
 * Simple unit lookups of basic skill and upkeep values
 */
public interface UnitUtils
{
	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @return Unit with requested URN, or null if not found
	 */
	public MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units);

	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @param caller The routine that was looking for the value
	 * @return Unit with requested URN
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	public MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units, final String caller)
		throws RecordNotFoundException;

	/**
	 * @param unitURN Unit URN to remove
	 * @param units List of units to search through
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	public void removeUnitURN (final int unitURN, final List<MemoryUnit> units)
		throws RecordNotFoundException;

	/**
	 * Populates a unit's list of skills after creation - this is the equivalent of the TMomAvailableUnit.CreateAvailableUnit constructor in Delphi.
	 * The client will never use this on real units - the server always sends them will all info already populated; but the client does
	 * need this for initializing skills of sample units, e.g. when drawing units on the change construction screen.
	 * 
	 * @param unit Unit that has just been created
	 * @param startingExperience Initial experience; if -1 or null then experience won't be added into skill list, which is used when server sends units to client since they already have exp skill in list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 * @return Unit definition
	 */
	public Unit initializeUnitSkills (final AvailableUnit unit, final Integer startingExperience, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param unit Unit to test
	 * @return Number of figures in this unit before it takes any damage
	 */
	public int getFullFigureCount (final Unit unit);

	/**
	 * @param skills List of unit skills to check; this can either be the unmodified list read straight from unit.getUnitHasSkill () or UnitHasSkillMergedList
	 * @param unitSkillID Unique identifier for this skill
	 * @return Basic value of the specified skill (defined in the XML or heroes rolled randomly); whether skills granted from spells are included depends on whether we pass in a UnitHasSkillMergedList or not; -1 if we do not have the skill
	 */
	public int getBasicSkillValue (final List<UnitSkillAndValue> skills, final String unitSkillID);

	/**
	 * @param unit Unit whose skills to modify (note we pass in the unit rather than the skills list to force using the live list and not a UnitHasSkillMergedList)
	 * @param unitSkillID Unique identifier for this skill
	 * @param skillValue New basic value of the specified skill
	 * @throws MomException If this unit didn't previously have the specified skill (this method only modifies existing skills, not adds new ones)
	 */
	public void setBasicSkillValue (final AvailableUnit unit, final String unitSkillID, final int skillValue)
		throws MomException;

	/**
	 * @param spells List of known maintained spells
	 * @param unit Unit whose skill list this is
	 * @param db Lookup lists built over the XML database
	 * @return List of all skills this unit has, with skills gained from spells (both enchantments such as Holy Weapon and curses such as Vertigo) merged into the list
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 */
	public UnitHasSkillMergedList mergeSpellEffectsIntoSkillList (final List<MemoryMaintainedSpell> spells, final MemoryUnit unit, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Calculates and stores all derived skill, upkeep and other values for a particular unit and stores them for easy and quick lookup.
	 * Note the calculated values depend in part on which unit(s) we're in combat with and if we're calculating stats for purposes of defending an incoming attack.
	 * e.g. the +2 defence from Long Range will only be included vs incoming ranged attacks; the +3 bonus from Resist Elements will only be included vs incoming Chaos+Nature based attacks
	 * So must bear this in mind that we need to recalculate unit stats again if the circumstances we are calculating them for change, even if the unit itself has not changed.
	 * 
	 * @param unit Unit to expand skill list for
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of all skills this unit has, with skills granted from other skills and skills granted from spells merged into the list
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public ExpandedUnitDetails expandUnitDetails (final AvailableUnit unit,
		final List<ExpandedUnitDetails> enemyUnits, final String attackFromSkillID, final String attackFromMagicRealmID,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * @param unit Unit to get value for
	 * @param includeBonuses Whether to include level increases from Warlord+Crusade
	 * @param players Players list
	 * @param combatAreaEffects List of combat area effects known to us (we may not be the owner of the unit)
	 * @param db Lookup lists built over the XML database
	 * @return Experience level of this unit (0-5 for regular units, 0-8 for heroes); for units that don't gain experience (e.g. summoned), returns null
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type, magic realm or so on
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public ExperienceLevel getExperienceLevel (final AvailableUnit unit, final boolean includeBonuses, final List<? extends PlayerPublicDetails> players,
		final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Since Available Units cannot be in combat, this is quite a bit simpler than the MomUnit version
	 *
	 * The unit has to:
	 * 1) Be in the right location (or it be a global CAE)
	 * 2) Belong to the right player (either the CAE applies to all players, or just the caster)
	 *
	 * CAEs with player code B or O can never apply to Available Units, since these require the unit to be in combat
	 * The only way CAEs with player code C can apply is if they're global (e.g. Holy Arms)
	 *
	 * @param unit Unit to test
	 * @param effect The combat area effect to test
	 * @param db Lookup lists built over the XML database
	 * @return True if this combat area effect affects this unit
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	public boolean doesCombatAreaEffectApplyToUnit (final AvailableUnit unit, final MemoryCombatAreaEffect effect, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return True magic realm/lifeform type ID of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead)
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 * @throws MomException If no matching merger record exists when multiple lifeform type modifications apply
	 */
	public String getModifiedUnitMagicRealmLifeformTypeID (final AvailableUnit unit, final List<UnitSkillAndValue> skills,
		final List<MemoryMaintainedSpell> spells, final CommonDatabase db)
		throws RecordNotFoundException, MomException;

	/**
	 * @param unit Unit to look up the base upkeep for
	 * @param productionTypeID Production type we want to look up the base upkeep for
	 * @param db Lookup lists built over the XML database
	 * @return Base upkeep value, before any reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public int getBasicUpkeepValue (final AvailableUnit unit, final String productionTypeID, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * @param units Unit stack
	 * @return Comma delimited list of their unit URNs, for debug messages
	 */
	public String listUnitURNs (final List<MemoryUnit> units);

	/**
	 * @param units List of units to check (usually movingPlayer's memory)
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to test if there are units *at all* at this location)
	 * @return First unit we find at the requested location who belongs to someone other than the specified player
	 */
	public MemoryUnit findFirstAliveEnemyAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID);

	/**
	 * @param units List of units to check
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to count *all* units at this location)
	 * @return Number of units that we find at the requested location who belongs to someone other than the specified player
	 */
	public int countAliveEnemiesAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID);
	
	/**
	 * Clears up any references to the specified unit from under the FogOfWarMemory structure, because the unit has just been killed
	 * This is used even if the unit is not actually being freed, e.g. could be dismissing a hero or just setting a unit in combat to 'dead' but not actually freeing the unit
	 * 
	 * @param mem Fog of war memory structure to remove references from; can be player's memory or the true map on the server
	 * @param unitURN Unit about to be killed
	 */
	public void beforeKillingUnit (final FogOfWarMemory mem, final int unitURN);
	
	/**
	 * @param units List of units to check
	 * @param combatLocation Location on overland map where the combat is taking place
	 * @param combatPosition Position within the combat map to look at
	 * @return Unit at this position, or null if there isn't one
	 */
	public MemoryUnit findAliveUnitInCombatAt (final List<MemoryUnit> units, final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition);
	
	/**
	 * Performs a deep copy (i.e. creates copies of every sub object rather than copying the references) of every field value from one unit to another
	 * @param source Unit to copy values from
	 * @param dest Unit to copy values to
	 * @param includeMovementFields Only the player who owns a unit can see its movement remaining and special orders
	 */
	public void copyUnitValues (final MemoryUnit source, final MemoryUnit dest, final boolean includeMovementFields);

	/**
	 * @param damages List of types of unit damage
	 * @return Total damage taken across all types
	 */
	public int getTotalDamageTaken (final List<UnitDamage> damages);

	/**
	 * @param damages List of types of unit damage
	 * @return Total damage taken across all types, excluding PERMANENT
	 */
	public int getHealableDamageTaken (final List<UnitDamage> damages);
}