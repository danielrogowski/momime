package momime.server.ai;

import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.movement.OverlandMovementCell;

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
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param underdefendedLocations Locations which are either ours (final AIUnitsAndRatings units, cities/towers) but lack enough defence, or not ours but can be freely captured (final AIUnitsAndRatings units, empty lairs/cities/etc)
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_Reinforce (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<AIDefenceLocation> underdefendedLocations, final CoordinateSystem sys);

	/**
	 * AI tries to move units to attack defended stationary locations (final AIUnitsAndRatings units, nodes/lairs/towers/cities) where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param isMonsters Whether it is the rampaging monsters player; ramping monsters attack the nearest target recklessly whether they stand a chance of winning or not
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @param players Players list
	 * @param wizards True wizard details list
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 * @throws PlayerNotFoundException If the player owning a unit stack can't be found
	 */
	public AIMovementDecision considerUnitMovement_AttackStationary (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders, final boolean isMonsters,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db, final List<PlayerServerDetails> players, final List<KnownWizardDetails> wizards)
		throws RecordNotFoundException, PlayerNotFoundException;
	
	/**
	 * AI tries to move units to attack enemy unit stacks wandering around the map where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isMonsters Whether it is the rampaging monsters player; ramping monsters attack the nearest target recklessly whether they stand a chance of winning or not
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @param players Players list
	 * @param wizards True wizard details list
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 * @throws PlayerNotFoundException If the player owning a unit stack can't be found
	 */
	public AIMovementDecision considerUnitMovement_AttackWandering (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isMonsters,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db, final List<PlayerServerDetails> players, final List<KnownWizardDetails> wizards)
		throws RecordNotFoundException, PlayerNotFoundException;

	/**
	 * AI tries to move units to scout any unknown terrain that is adjacent to at least one tile that we know to be land.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @param playerID Player who is moving
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	public AIMovementDecision considerUnitMovement_ScoutLand (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db, final int playerID) throws RecordNotFoundException;
	
	/**
	 * AI tries to move units to scout any unknown terrain.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param playerID Player who is moving
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_ScoutAll (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final int playerID);

	/**
	 * AI looks to see if any defended locations (final AIUnitsAndRatings units, nodes/lairs/towers/cities) are too well defended to attack at the moment,
	 * and if it can see any then will look to merge together our units into a bigger stack.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param ourUnitsInSameCategory List of all our mobile unit stacks in the same category as the ones we are moving
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	public AIMovementDecision considerUnitMovement_JoinStack (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<AIUnitsAndRatings> ourUnitsInSameCategory, final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * AI looks for a transport to get in (final AIUnitsAndRatings units, or stay where we are if we are already in one).
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_GetInTransport (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys);

	/**
	 * AI looks for any of our locations (final AIUnitsAndRatings units, nodes/cities/towers) that we can reach, regardless of if they already have plenty of defence.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	public AIMovementDecision considerUnitMovement_Overdefend (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * AI looks for a good place for settlers to build a city
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param desiredCityLocations Locations where we want to put cities
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_BuildCity (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<MapCoordinates3DEx> desiredCityLocations, final CoordinateSystem sys);

	/**
	 * AI looks for a good place for engineers to build a road
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param desiredRoadLocations Locations where we want to put cities
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_BuildRoad (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<MapCoordinates3DEx> desiredRoadLocations, final CoordinateSystem sys);

	/**
	 * AI looks for any corrupted land that priests need to purify
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param desiredPurifyLocations Corrupted locations near our cities that need to be purified
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_Purify (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<MapCoordinates3DEx> desiredPurifyLocations, final CoordinateSystem sys);

	/**
	 * AI looks for a node that a magic/guardian spirit can meld with
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param nodeCaptureLocations Locations where we have guarded nodes ready to capture
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_MeldWithNode (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<MapCoordinates3DEx> nodeCaptureLocations, final CoordinateSystem sys);
	
	/**
	 * AI transports look for a suitable island to carry units to, if we are holding any.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_CarryUnits (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys);
	
	/**
	 * AI transports that are empty head for any islands where any unit stacks went on OVERDEFEND.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_LoadUnits (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys);
	
	/**
	 * If we are on the same plane as our Wizards' Fortress, then head the island that it is on.
	 * (final AIUnitsAndRatings units, This is intended for transport ships that have nothing better to do, so we're assuming we can't actually get *onto* the island).
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_FortressIsland (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys);
}