package momime.common.calculations;

import java.util.List;
import java.util.Set;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.PlayerPick;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Common calculations pertaining to units
 */
public interface UnitCalculations
{
	/**
	 * Gives all units full movement back again for their combat turn
	 *
	 * @param playerID Player whose units to update 
	 * @param combatLocation Where the combat is taking place
	 * @param terrifiedUnitURNs List of units who failed their resistance roll against Terror spell and so cannot move this turn
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of units that didn't get any movement allocated because they're stuck in a web
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public List<ExpandedUnitDetails> resetUnitCombatMovement (final int playerID, final MapCoordinates3DEx combatLocation, final List<Integer> terrifiedUnitURNs,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * @param map Our knowledge of the surrounding terrain
	 * @param buildings Pre-locked buildings list
	 * @param cityLocation Location of the city the unit is being constructed at
	 * @param picks Picks of the player who owns the city
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return Weapon grade that the unit that we build here will have
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 */
	public int calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
		(final List<MemoryBuilding> buildings, final MapVolumeOfMemoryGridCells map, final MapCoordinates3DEx cityLocation,
		final List<PlayerPick> picks, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * Flying units obviously ignore this although they still can't enter impassable terrain
	 * @param xu The unit that is moving; if passed in as null then can't do this check on specific movement skills
	 * @param tile Combat tile being entered
	 * @param db Lookup lists built over the XML database
	 * @return 2x movement points required to enter this tile; negative value indicates impassable; will never return zero
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	public int calculateDoubleMovementToEnterCombatTile (final ExpandedUnitDetails xu, final MomCombatTile tile, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * Initializes any values on the unit at the start of a combat
	 * NB. Available units can never expend ranged attack ammo or use mana, but storing these values keeps avoids the need for the
	 * methods to use the Fog of War memory to look for spell effects that might increase ammo or mana
	 * 
	 * @param unit Unit we want to give ammo+mana to
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public void giveUnitFullRangedAmmoAndMana (final ExpandedUnitDetails unit) throws MomException;
	
	/**
	 * Decreases amount of ranged ammo remaining for this unit when it fires a ranged attack
	 * @param unit Unit making the ranged attack
	 */
	public void decreaseRangedAttackAmmo (final MemoryUnit unit);

	/**
	 * This isn't as straightforward as it sounds, we either need dedicated ranged attack ammo (which can be phys or magic ranged attacks)
	 * or caster units can spend mana to fire ranged attacks, but only magical ranged attacks
	 * 
	 * @param unit Unit to calculate for
	 * @return Whether the unit can make a ranged attack in combat and has ammo to do so
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public boolean canMakeRangedAttack (final ExpandedUnitDetails unit) throws MomException;

	/**
	 * This is much simpler than canMakeRangedAttack, as we don't need ammo to fire with.
	 * This is really here to stop settlers with 0 attack trying to attack other units.
	 * But also have to stop grounded units attacking flying units, unless they have a thrown/gaze/breath attack.
	 * 
	 * @param enemyCombatActionID Standing combat action of the unit we want to attack, so we know whether it is flying
	 * @param unit Unit doing the attacking
	 * @param db Lookup lists built over the XML database
	 * @return Whether the unit can make a melee attack in combat
	 * @throws RecordNotFoundException If the enemyCombatActionID cannot be found
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public boolean canMakeMeleeAttack (final String enemyCombatActionID, final ExpandedUnitDetails unit, final CommonDatabase db)
		throws MomException, RecordNotFoundException;
	
	/**
	 * Will return true if we blunder onto a tile containing invisible units we couldn't see
	 * 
	 * @param x X coordinate of the location we want to check
	 * @param y Y coordinate of the location we want to check
	 * @param plane Plane we want to check
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge of the terrain
	 * @param units The player who is trying to move here's knowledge of units
	 * @return Whether moving here will result in an attack or not
	 */
	public boolean willMovingHereResultInAnAttack (final int x, final int y, final int plane, final int movingPlayerID,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units);
	
	/**
	 * Will only return true if we can see units in the target tile; if there's invisible enemies there will return false
	 * 
	 * @param x X coordinate of the location we want to check
	 * @param y Y coordinate of the location we want to check
	 * @param plane Plane we want to check
	 * @param movingPlayerID The player who is trying to move here
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Whether moving here will result in an attack or not
	 */
	public boolean willMovingHereResultInAnAttackThatWeKnowAbout (final int x, final int y, final int plane, final int movingPlayerID,
		final FogOfWarMemory mem, final CommonDatabase db);
	
	/**
	 * @param unitStack Unit stack to check
	 * @return Merged list of every skill that at least one unit in the stack has, including skills granted from spells
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	public Set<String> listAllSkillsInUnitStack (final List<ExpandedUnitDetails> unitStack) throws MomException;

	/**
	 * @param unit Unit that we want to move
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param tileTypeID Type of tile we are moving onto
	 * @param db Lookup lists built over the XML database
	 * @return Double the number of movement points we will use to walk onto that tile; null = impassable
	 */
	public Integer calculateDoubleMovementToEnterTileType (final ExpandedUnitDetails unit, final Set<String> unitStackSkills, final String tileTypeID, final CommonDatabase db);
	
	/**
	 * @param unit Unit that we want to move
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param db Lookup lists built over the XML database
	 * @return Whether this unit can pass over every type of possible terrain on the map; i.e. true for swimming units like Lizardmen, any flying unit, or any unit stacked with a Wind Walking unit
	 */
	public boolean areAllTerrainTypesPassable (final ExpandedUnitDetails unit, final Set<String> unitStackSkills, final CommonDatabase db);
	
	/**
	 * Checks whether selectedUnits includes any transports, and if so whether the other units fit inside them, and whether any others in the same map cell should be added to the stack.
	 * See the UnitStack object for a lot more comments on the rules by which this needs to work.
	 * 
	 * @param selectedUnits Units selected by the player to move
	 * @param players Players list
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on 
	 * @param db Lookup lists built over the XML database
	 * @return UnitStack object
	 * @throws PlayerNotFoundException If we cannot find the player who a unit in the same location as our transports
	 * @throws RecordNotFoundException If we can't find the definitions for any of the units at the location
	 * @throws MomException If selectedUnits is empty, all the units aren't at the same location, or all the units don't have the same owner 
	 */
	public UnitStack createUnitStack (final List<ExpandedUnitDetails> selectedUnits,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory fogOfWarMemory, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;
	
	
	/**
	 * Calculates how many (doubled) movement points it will take to move from x, y to ever other location in the combat map whether we can move there or not.
	 * 
	 * MoM is a little weird with how movement works - providing you have even 1/2 move left, you can move anywhere, even somewhere
	 * which takes 3 movement to get to - this can happen in combat as well, especially combats in cities when units can walk on the roads.
	 * 
	 * Therefore knowing distances to each location is not enough - we need a separate boolean array
	 * to mark whether we can or cannot reach each location - this is set in MovementTypes.
	 * 
	 * @param doubleMovementDistances Double the number of movement points it takes to move here, 0=free (enchanted road), negative=cannot reach
	 * @param movementDirections Trace of unit directions taken to reach here
	 * @param movementTypes Type of move (or lack of) for every location on the combat map (these correspond exactly to the X, move, attack, icons displayed in the client)
	 * @param unitBeingMoved The unit moving in combat
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on
	 * @param combatMap The details of the combat terrain
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public void calculateCombatMovementDistances (final int [] [] doubleMovementDistances, final int [] [] movementDirections,
		final CombatMoveType [] [] movementTypes, final ExpandedUnitDetails unitBeingMoved, final FogOfWarMemory fogOfWarMemory,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem,
		final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * Chooses the preferred method of movement for this unit, i.e. the one with the lowest preference number (no. 1 is chosen first, then no. 2, etc.)
	 * 
	 * This ensures that e.g. Flying units (whether natural flight, spell-cast Flight or Chaos Channels Flight) all show the
	 * correct flight icon, and units with Swimming/Sailing show the wave icon
	 * 
	 * @param unit Unit to determine the movement graphics for
	 * @param db Lookup lists built over the XML database
	 * @return Movement graphics node
	 * @throws MomException If this unit has no skills which have movement graphics, or we can't find its experience level
	 */
	public UnitSkillEx findPreferredMovementSkillGraphics (final ExpandedUnitDetails unit, final CommonDatabase db) throws MomException;

	/**
	 * combatActionIDs are MELEE when attacking melee, RANGED when attacking ranged, and generated by
	 * this routine when units are not attacking.  It looks up the combatActionIDs depending on what movement
	 * skills the unit has in such a way that we avoid having to hard code combatActionIDs.
	 * 
	 * e.g. a regular unit of swordsmen shows the STAND image while not moving, but if we cast
	 * Flight on them then we need to show the FLY animation instead.
	 *
	 * In the animations as directly converted from the original MoM graphics, WALK and FLY look the same - they
	 * resolve to the same animation, named e.g. UN100_D4_WALKFLY.  However the intention in the long term is
	 * to separate these and show flying units significantly raised up off the ground, so you can actually see flying
	 * units coming down to ground level when they have web cast on them, or swordsmen high up in the
	 * air when they have flight cast on them.
	 * 
	 * @param unit Unit to determine the combat action ID for
	 * @param isMoving Whether the unit is standing still or moving
	 * @param db Lookup lists built over the XML database
	 * @return Action ID for a unit standing still or moving
	 * @throws MomException If this unit has no skills which have movement graphics, we can't find its experience level, or a movement skill doesn't specify an action ID
	 */
	public String determineCombatActionID (final ExpandedUnitDetails unit, final boolean isMoving, final CommonDatabase db) throws MomException;
}