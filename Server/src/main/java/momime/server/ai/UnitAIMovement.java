package momime.server.ai;

import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.server.database.ServerDatabaseEx;

/**
 * Provides a method for processing each movement code that the AI uses to decide where to send units overland.
 * 
 * Each of these methods must be able to operate in "test" mode, so we just test to see if the unit has something
 * to do using that code, without actually doing it.
 */
public interface UnitAIMovement
{
	/**
	 * AI tries to move units to any location that lacks defence or can be captured without a fight.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param underdefendedLocations Locations which are either ours (cities/towers) but lack enough defence, or not ours but can be freely captured (empty lairs/cities/etc)
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_Reinforce (final int [] [] [] doubleMovementDistances,
		final List<AIDefenceLocation> underdefendedLocations);

	/**
	 * AI tries to move units to attack defended stationary locations (nodes/lairs/towers/cities) where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	public AIMovementDecision considerUnitMovement_AttackStationary (final AIUnitsAndRatings units, final int [] [] [] doubleMovementDistances,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException;
	
	/**
	 * AI tries to move units to attack enemy unit stacks wandering around the map where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	public AIMovementDecision considerUnitMovement_AttackWandering (final AIUnitsAndRatings units, final int [] [] [] doubleMovementDistances,
		final AIUnitsAndRatings [] [] [] enemyUnits, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException;

	/**
	 * AI tries to move units to scout any unknown terrain that is adjacent to at least one tile that we know to be land.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @param playerID Player who is moving
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	public AIMovementDecision considerUnitMovement_ScoutLand (final int [] [] [] doubleMovementDistances,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db, final int playerID) throws RecordNotFoundException;
	
	/**
	 * AI tries to move units to scout any unknown terrain.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param playerID Player who is moving
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_ScoutAll (final int [] [] [] doubleMovementDistances,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final int playerID);

	/**
	 * AI looks to see if any defended locations (nodes/lairs/towers/cities) are too well defended to attack at the moment,
	 * and if it can see any then will look to merge together our units into a bigger stack.
	 * 
	 * @param units The units to move
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param ourUnitsInSameCategory List of all our mobile unit stacks in the same category as the ones we are moving
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	public AIMovementDecision considerUnitMovement_JoinStack (final AIUnitsAndRatings units, final int [] [] [] doubleMovementDistances,
		final List<AIUnitsAndRatings> ourUnitsInSameCategory, final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException;

	/**
	 * AI looks for a tower garissoned by our units, and imagines that we are stood there and rechecks preceeding movement codes.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_PlaneShift (final int [] [] [] doubleMovementDistances);
	
	/**
	 * AI looks for a transport to get in (or stay where we are if we are already in one).
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_GetInTransport (final int [] [] [] doubleMovementDistances);

	/**
	 * AI looks for any of our locations (nodes/cities/towers) that we can reach, regardless of if they already have plenty of defence.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	public AIMovementDecision considerUnitMovement_Overdefend (final int [] [] [] doubleMovementDistances,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException;

	/**
	 * AI looks for a good place for settlers to build a city
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param currentLocation Current location of settler unit
	 * @param desiredCityLocations Locations where we want to put cities
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_BuildCity (final int [] [] [] doubleMovementDistances, final MapCoordinates3DEx currentLocation, final List<MapCoordinates3DEx> desiredCityLocations);

	/**
	 * AI looks for a good place for engineers to build a road
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param currentLocation Current location of engineer unit
	 * @param desiredRoadLocations Locations where we want to put cities
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_BuildRoad (final int [] [] [] doubleMovementDistances, final MapCoordinates3DEx currentLocation, final List<MapCoordinates3DEx> desiredRoadLocations);

	/**
	 * AI looks for any corrupted land that priests need to purify
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_Purify (final int [] [] [] doubleMovementDistances);

	/**
	 * AI looks for a node that a magic/guardian spirit can meld with
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_MeldWithNode (final int [] [] [] doubleMovementDistances);
	
	/**
	 * AI transports look for a suitable island to carry units to, if we are holding any.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_CarryUnits (final int [] [] [] doubleMovementDistances);
	
	/**
	 * AI transports that are empty head for any islands where any unit stacks went on OVERDEFEND.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_LoadUnits (final int [] [] [] doubleMovementDistances);
	
	/**
	 * If we are on the same plane as our Wizards' Fortress, then head the island that it is on.
	 * (This is intended for transport ships that have nothing better to do, so we're assuming we can't actually get *onto* the island).
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_FortressIsland (final int [] [] [] doubleMovementDistances);
}