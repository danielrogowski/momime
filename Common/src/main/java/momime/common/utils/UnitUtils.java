package momime.common.utils;

import java.util.List;
import java.util.Map;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
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
	public UnitEx initializeUnitSkills (final AvailableUnit unit, final Integer startingExperience, final CommonDatabase db) throws RecordNotFoundException;
	
	/**
	 * @param unitSkillID Unit skill we want to check for
	 * @param ourSkillValues List of skills the unit has
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param db Lookup lists built over the XML database
	 * @return Whether the skill is negated or not
	 * @throws RecordNotFoundException If we can't find the skill definition
	 * @throws MomException If the skill definition has an unknown negatedByUnitID value
	 */
	public boolean isSkillNegated (final String unitSkillID, final Map<String, ? extends Object> ourSkillValues, final List<ExpandedUnitDetails> enemyUnits,
		final CommonDatabase db) throws RecordNotFoundException, MomException;
	
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
	 * @param units Unit stack
	 * @return Comma delimited list of their unit URNs, for debug messages
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	public String listUnitURNs (@SuppressWarnings ("rawtypes") final List units) throws MomException;

	/**
	 * Will find units even if they're invisible
	 * 
	 * @param units List of units to check (usually movingPlayer's memory)
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to test if there are units *at all* at this location)
	 * @return First unit we find at the requested location who belongs to someone other than the specified player
	 */
	public MemoryUnit findFirstAliveEnemyAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID);

	/**
	 * @param ourPlayerID Our player ID
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to test if there are units *at all* at this location)
	 * @param db Lookup lists built over the XML database
	 * @return First unit we find at the requested location who belongs to someone other than the specified player
	 */
	public MemoryUnit findFirstAliveEnemyWeCanSeeAtLocation (final int ourPlayerID, final FogOfWarMemory mem, final int x, final int y, final int plane,
		final int exceptPlayerID, final CommonDatabase db);
	
	/**
	 * Lists the enemy units at a specified location on the overland map, regardless of whether they might be invisible or not.
	 * 
	 * @param units List of units to check
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to count *all* units at this location)
	 * @return Number of units that we find at the requested location who belongs to someone other than the specified player
	 */
	public List<MemoryUnit> listAliveEnemiesAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID);

	/**
	 * Counts the number of enemy units at a specified location on the overland map, regardless of whether they might be invisible or not.
	 * 
	 * @param units List of units to check
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to count *all* units at this location)
	 * @return Number of units that we find at the requested location who belongs to someone other than the specified player
	 */
	public int countAliveEnemiesAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID);
	
	/**
	 * @param units List of units to check
	 * @param combatLocation Location on overland map where the combat is taking place
	 * @param combatPosition Position within the combat map to look at
	 * @param db Lookup lists built over the XML database
	 * @param allowTargetingVortexes Normally magic vortexes cannot be targeted in any way, but allow it if this is set to true
	 * @return Unit at this position, or null if there isn't one
	 */
	public MemoryUnit findAliveUnitInCombatAt (final List<MemoryUnit> units, final MapCoordinates3DEx combatLocation,
		final MapCoordinates2DEx combatPosition, final CommonDatabase db, final boolean allowTargetingVortexes);
	
	/**
	 * findAliveUnitInCombatAt will still return units we cannot see because they're invisible.  This adds that check.  So for example if we have a unit
	 * adjacent to an invisible unit, we can still "see" it and this method will return it.
	 * 
	 * @param combatLocation Location on overland map where the combat is taking place
	 * @param combatPosition Position within the combat map to look at
	 * @param ourPlayerID Our player ID
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param allowTargetingVortexes Normally magic vortexes cannot be targeted in any way, but allow it if this is set to true
	 * @return Unit at this position, or null if there isn't one, or if there is one but we can't see it
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public ExpandedUnitDetails findAliveUnitInCombatWeCanSeeAt (final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final int ourPlayerID, final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db,
		final CoordinateSystem combatMapCoordinateSystem, final boolean allowTargetingVortexes)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;
	
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

	/**
	 * @param xu Unit to test
	 * @param unitSpellEffects List of unit skills to test
	 * @param db Lookup lists built over the XML database
	 * @return True if the unit is immune to all listed effects, false if we find at least one it isn't immune to
	 * @throws RecordNotFoundException If we can't find definition for one of the skills
	 */
	public boolean isUnitImmuneToSpellEffects (final ExpandedUnitDetails xu, final List<UnitSpellEffect> unitSpellEffects, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Used to stop casting Heroism on units that already have 120 exp naturally
	 * 
	 * @param xu Unit to test
	 * @param unitSpellEffects List of unit skills to test
	 * @param db Lookup lists built over the XML database
	 * @return True if the unit skills give a boost to experience, but the unit already has that much experience naturally
	 * @throws RecordNotFoundException If we can't find definition for one of the skills
	 * @throws MomException If the unit doesn't have an experience value (but checks for this, so should never happen)
	 */
	public boolean isExperienceBonusAndWeAlreadyHaveTooMuch (final ExpandedUnitDetails xu, final List<UnitSpellEffect> unitSpellEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
}