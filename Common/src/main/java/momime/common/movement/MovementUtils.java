package momime.common.movement;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.utils.ExpandedUnitDetails;

/**
 * There's a lot of methods involved in calculating movement.  All the component methods are here, then the main front end methods are in UnitMovementImpl
 * so that they are kept independant of each other for unit tests.
 */
public interface MovementUtils
{
	/**
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge
	 * @param players List of players in this session
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Count of the number of free transport spaces at every map cell
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	public int [] [] [] calculateCellTransportCapacity (final UnitStack unitStack, final Set<String> unitStackSkills, final int movingPlayerID, final FogOfWarMemory map,
		final List<? extends PlayerPublicDetails> players, final CoordinateSystem sys, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;
	
	/**
	 * @param unitStack Unit stack we are moving
	 * @param db Lookup lists built over the XML database
	 * @return Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	public Map<String, Integer> calculateDoubleMovementRatesForUnitStack (final List<ExpandedUnitDetails> unitStack,
		final CommonDatabase db) throws RecordNotFoundException, MomException;
	
	/**
	 * @param playerID Player whose units to count
	 * @param units Player's knowledge of all units
	 * @param sys Overland map coordinate system
	 * @return Count how many of that player's units are in every cell on the map
	 */
	public int [] [] [] countOurAliveUnitsAtEveryLocation (final int playerID, final List<MemoryUnit> units, final CoordinateSystem sys);

	/**
	 * Finds all places on the overland map our unit stack cannot go because either:
	 * 1) We already have units there, and number of units there + number of units in stack will be > 9
	 * 2) Its a Tower of Wizardry, and Planar Seal is cast (even our own)
	 * 3) Spell Ward, and we have summoned creatures of the corresponding type (even our own)
	 * 4) Someone else's Flying Fortress, and we have non-flying units
	 * 
	 * These are all pretty special circumstances that won't list out many coordinates.  What this doesn't include is locations
	 * where the terrain itself is impassable, or we'd end up listing every known water tile if its a stack of land units.
	 * 
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param movingPlayerID The player who is trying to move here
	 * @param ourUnitCountAtLocation Count how many of our units are in every cell on the map
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param knownWizards Details we have learned about wizards we have met
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * @param db Lookup lists built over the XML database
	 * @return Set of all overland map locations this unit stack is blocked from entering for one of the above reasons
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws PlayerNotFoundException If we can't find the player who is moving
	 */
	public Set<MapCoordinates3DEx> determineBlockedLocations (final UnitStack unitStack, final int movingPlayerID,
		final int [] [] [] ourUnitCountAtLocation, final CoordinateSystem overlandMapCoordinateSystem,
		final List<KnownWizardDetails> knownWizards, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException;

	/**
	 * @param movingPlayerID The player who is trying to move
	 * @param spells Known spells
	 * @return All locations where we have an Earth Gate cast on a city
	 */
	public Set<MapCoordinates3DEx> findEarthGates (final int movingPlayerID, final List<MemoryMaintainedSpell> spells);
	
	/**
	 * @param movingPlayerID The player who is trying to move
	 * @param spells Known spells
	 * @return All locations where we have an Astral Gate cast on a city, unless Planar Seal is cast in which case we always get an empty set
	 */
	public Set<MapCoordinates2DEx> findAstralGates (final int movingPlayerID, final List<MemoryMaintainedSpell> spells);

	/**
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param coords Where we are trying to move to
	 * @param cellTransportCapacity Number of free spaces on transports at each map cell
	 * @param doubleMovementRates Movement to enter each kind of overland map tile
	 * @param terrain Player knowledge of terrain
	 * @param db Lookup lists built over the XML database
	 * @return Double number of movement points it costs to enter this tile; null if the tile is impassable
	 */
	public Integer calculateDoubleMovementToEnterTile (final UnitStack unitStack, final Set<String> unitStackSkills, final MapCoordinates3DEx coords,
		final int [] [] [] cellTransportCapacity, final Map<String, Integer> doubleMovementRates,
		final MapVolumeOfMemoryGridCells terrain, final CommonDatabase db);

	/**
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param moveFrom Coordinates of the cell we are moving from
	 * @param movingPlayerID The player who is trying to move here
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param blockedLocations Set of all overland map locations this unit stack is blocked from entering for any reasons other than impassable terrain
	 * @param earthGates Earth gates we can use
	 * @param astralGates Astral gates we can use
	 * @param cellTransportCapacity Count of the number of free transport spaces at every map cell
	 * @param doubleMovementRates Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param cellsLeftToCheck List of cells that still need to be checked (we add adjacent cells to the end of this list)
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param mem The player who is trying to move here's knowledge
	 * @param db Lookup lists built over the XML database
	 */
	public void processOverlandMovementCell (final UnitStack unitStack, final Set<String> unitStackSkills,
		final MapCoordinates3DEx moveFrom, final int movingPlayerID, final int doubleMovementRemaining,
		final Set<MapCoordinates3DEx> blockedLocations, final Set<MapCoordinates3DEx> earthGates, final Set<MapCoordinates2DEx> astralGates,
		final int [] [] [] cellTransportCapacity, final Map<String, Integer> doubleMovementRates,
		final OverlandMovementCell [] [] [] moves, final List<MapCoordinates3DEx> cellsLeftToCheck,
		final CoordinateSystem overlandMapCoordinateSystem, final FogOfWarMemory mem, final CommonDatabase db);

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
	 * Adds all directions from the given location to the list of cells left to check for combat movement
	 * 
	 * @param moveFrom Combat tile we're moving from
	 * @param unitBeingMoved The unit moving in combat
	 * @param ignoresCombatTerrain True if the unit has a skill with the "ignoreCombatTerrain" flag
	 * @param cellsLeftToCheck List of combat tiles we still need to check movement from
	 * @param doubleMovementDistances Double the number of movement points it takes to move here, 0=free (enchanted road), negative=cannot reach
	 * @param movementDirections Trace of unit directions taken to reach here
	 * @param movementTypes Type of move (or lack of) for every location on the combat map (these correspond exactly to the X, move, attack, icons displayed in the client)
	 * @param ourUnits Array marking location of all of our units in the combat
	 * @param enemyUnits Array marking location of all enemy units in the combat; each element in the array is their combatActionID so we know which ones are flying
	 * @param borderTargetIDs List of tile borders that we can attack besides being able to target units; null if there are none
	 * @param combatMap The details of the combat terrain
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void processCombatMovementCell (final MapCoordinates2DEx moveFrom, final ExpandedUnitDetails unitBeingMoved, final boolean ignoresCombatTerrain,
		final List<MapCoordinates2DEx> cellsLeftToCheck, final int [] [] doubleMovementDistances, final int [] [] movementDirections,
		final CombatMovementType [] [] movementTypes, final boolean [] [] ourUnits, final String [] [] enemyUnits, final List<String> borderTargetIDs,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
}