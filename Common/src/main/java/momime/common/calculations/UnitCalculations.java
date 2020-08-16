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
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public void resetUnitCombatMovement (final int playerID, final MapCoordinates3DEx combatLocation,
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
	 * @param tile Combat tile being entered
	 * @param db Lookup lists built over the XML database
	 * @return 2x movement points required to enter this tile; negative value indicates impassable; will never return zero
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	public int calculateDoubleMovementToEnterCombatTile (final MomCombatTile tile, final CommonDatabase db)
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
	 * 
	 * @param unit Unit to calculate for
	 * @return Whether the unit can make a melee attack in combat
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public boolean canMakeMeleeAttack (final ExpandedUnitDetails unit) throws MomException;
	
	/**
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
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public void calculateCombatMovementDistances (final int [] [] doubleMovementDistances, final int [] [] movementDirections,
		final CombatMoveType [] [] movementTypes, final ExpandedUnitDetails unitBeingMoved, final FogOfWarMemory fogOfWarMemory,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException;
}