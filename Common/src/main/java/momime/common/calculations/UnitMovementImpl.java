package momime.common.calculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Plane;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileTypeEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;

/**
 * Methods dealing with unit movement
 */
public final class UnitMovementImpl implements UnitMovement
{
	/** Marks locations in the doubleMovementDistances array that we haven't checked yet */
	private final static int MOVEMENT_DISTANCE_NOT_YET_CHECKED = -1;

	/** Marks locations in the doubleMovementDistances array that we've proved that we cannot move to */
	private final static int MOVEMENT_DISTANCE_CANNOT_MOVE_HERE = -2;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/**
	 * @param playerID Player whose units to count
	 * @param units Player's knowledge of all units
	 * @param sys Overland map coordinate system
	 * @return Count how many of that player's units are in every cell on the map
	 */
	final int [] [] [] countOurAliveUnitsAtEveryLocation (final int playerID, final List<MemoryUnit> units, final CoordinateSystem sys)
	{
		final int [] [] [] count = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getOwningPlayerID () == playerID) && (thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getUnitLocation () != null))
				count [thisUnit.getUnitLocation ().getZ ()] [thisUnit.getUnitLocation ().getY ()] [thisUnit.getUnitLocation ().getX ()]++;

		return count;
	}
	
	/**
	 * @param unitStack Unit stack we are moving
	 * @param db Lookup lists built over the XML database
	 * @return Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	final Map<String, Integer> calculateDoubleMovementRatesForUnitStack (final List<ExpandedUnitDetails> unitStack,
		final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		// Get list of all the skills that any unit in the stack has, in case any of them have path finding, wind walking, etc.
		final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack);

		// Go through each tile type
		final Map<String, Integer> movementRates = new HashMap<String, Integer> ();
		for (final TileTypeEx tileType : db.getTileTypes ())
			if (!tileType.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR_HAVE_SEEN))
			{
				Integer worstMovementRate = 0;

				// Check every unit - stop if we've found that terrain is impassable to someone
				final Iterator<ExpandedUnitDetails> unitIter = unitStack.iterator ();
				while ((worstMovementRate != null) && (unitIter.hasNext ()))
				{
					final ExpandedUnitDetails thisUnit = unitIter.next ();

					final Integer thisMovementRate = getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills, tileType.getTileTypeID (), db);
					if (thisMovementRate == null)
						worstMovementRate = null;
					else if (thisMovementRate > worstMovementRate)
						worstMovementRate = thisMovementRate;
				}

				// No point putting it into the map if it is impassable - HashMap.get returns null for keys not in the map anyway
				if (worstMovementRate != null)
					movementRates.put (tileType.getTileTypeID (), worstMovementRate);
			}

		return movementRates;
	}

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
	final int [] [] [] calculateCellTransportCapacity (final UnitStack unitStack, final Set<String> unitStackSkills, final int movingPlayerID, final FogOfWarMemory map,
		final List<? extends PlayerPublicDetails> players, final CoordinateSystem sys, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException 
	{
		// If its a transported movement, then by defintion the stack can move onto another transport if it wants to,
		// so don't need to make any special considerations for moving units onto a transport
		final int [] [] [] cellTransportCapacity;
		if (unitStack.getTransports ().size () > 0)
			cellTransportCapacity = null;
		else
		{
			// Find how much spare transport capacity we have on every cell of the map.
			// We add +capacity for every transport found, and -1 capacity for every unit that is on terrain impassable to itself (therefore must be in a transport).
			// Then any spaces left with 1 or higher value have spare space units could be loaded into.
			// Any units standing in towers have their values counted on both planes.
			cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
			for (final MemoryUnit thisUnit : map.getUnit ())
				if ((thisUnit.getOwningPlayerID () == movingPlayerID) && (thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getUnitLocation () != null))
				{
					final int x = thisUnit.getUnitLocation ().getX ();
					final int y = thisUnit.getUnitLocation ().getY ();
					final int z = thisUnit.getUnitLocation ().getZ ();
					
					final OverlandMapTerrainData terrainData = map.getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getTerrainData ();
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null, players, map, db);
					
					// Count space granted by transports
					final Integer unitTransportCapacity = xu.getUnitDefinition ().getTransportCapacity ();
					if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
					{
						if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
						{
							for (int plane = 0; plane < sys.getDepth (); plane++)
								cellTransportCapacity [plane] [y] [x] = cellTransportCapacity [plane] [y] [x] + unitTransportCapacity;
						}
						else
							cellTransportCapacity [z] [y] [x] = cellTransportCapacity [z] [y] [x] + unitTransportCapacity;
					}
					
					// Count space taken up by units already in transports
					else
					{
						final String tileTypeID = getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, false);
						if (getUnitCalculations ().calculateDoubleMovementToEnterTileType (xu, unitStackSkills, tileTypeID, db) == null)
							if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
							{
								for (int plane = 0; plane < sys.getDepth (); plane++)
									cellTransportCapacity [plane] [y] [x]--;
							}
							else
								cellTransportCapacity [z] [y] [x]--;
					}
				}			
		}
		
		return cellTransportCapacity;
	}
	
	/**
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param terrain Player knowledge of terrain
	 * @param cellTransportCapacity Count of the number of free transport spaces at every map cell
	 * @param ourUnitCountAtLocation Count how many of our units are in every cell on the map
	 * @param doubleMovementRates Movement rate calculated for this unit stack to enter every possible tile type
	 * @param unitsPerMapCell Maximum number of units allowed to fit in a map cell
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Movement cost for the unit stack to enter every map cell
	 */
	final Integer [] [] [] calculateDoubleMovementToEnterTile (final UnitStack unitStack, final Set<String> unitStackSkills, final MapVolumeOfMemoryGridCells terrain,
		final int [] [] [] cellTransportCapacity, final int [] [] [] ourUnitCountAtLocation, final Map<String, Integer> doubleMovementRates,
		final int unitsPerMapCell, final CoordinateSystem sys, final CommonDatabase db)
	{
		final Integer [] [] [] doubleMovementToEnterTile = new Integer [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					// If cell will be full, leave it as null = impassable
					if (ourUnitCountAtLocation [z] [y] [x] + unitStack.getTransports ().size () + unitStack.getUnits ().size () <= unitsPerMapCell)
					{
						final OverlandMapTerrainData terrainData = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x).getTerrainData ();
						Integer movementRate = doubleMovementRates.get (getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, true));
						
						// If the cell is otherwise impassable to us (i.e. land units trying to walk onto water) but there's enough space in a transport there, then allow it
						if ((movementRate == null) && (cellTransportCapacity != null) && (cellTransportCapacity [z] [y] [x] > 0))
						{
							// Work out how many spaces we -need-
							// Can't do this up front because it varies depending on whether the terrain being moved to is impassable to each kind of unit in the stack
							int spaceRequired = 0;
							boolean impassableToTransport = false;
							for (final ExpandedUnitDetails thisUnit : unitStack.getUnits ())
							{															
								final boolean impassable = (getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills,
									getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, false), db) == null);
								
								// Count space granted by transports
								final Integer unitTransportCapacity = thisUnit.getUnitDefinition ().getTransportCapacity ();
								if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
								{
									if (impassable)
										impassableToTransport = true;
									else
										spaceRequired = spaceRequired - unitTransportCapacity;
								}
								
								// Count space taken up by units already in transports
								else if (impassable)									
									spaceRequired++;
							}							
							
							// If the cell is impassable to one of our transports then the free space is irrelevant, we just can't go there
							if ((!impassableToTransport) && (cellTransportCapacity [z] [y] [x] >= spaceRequired))
								movementRate = 2;
						}
						
						doubleMovementToEnterTile [z] [y] [x] = movementRate;
					}
				}
		
		return doubleMovementToEnterTile;
	}
	
	/**
	 * Processes where we can move to from one cell, marking further adjacent cells that we can also reach from here
	 *
	 * @param cellX X location to move from
	 * @param cellY Y location to move from
	 * @param cellPlane Plane we are moving over
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge of the terrain
	 * @param units The player who is trying to move here's knowledge of units
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param movementDirections The direction that we moved to get here, e.g. the tile directly above startX, startY will have value 1
	 * @param canMoveToInOneTurn Indicates the locations that we can reach in a single turn (see the forester example above)
	 * @param movingHereResultsInAttack Indicates whether we know that moving here will result in attacking an enemy unit stack
	 * @param doubleMovementToEnterTile Double the movement points required to enter every tile on both planes; null = impassable
	 * @param cellsLeftToCheck List of cells that still need to be checked (we add adjacent cells to the end of this list)
	 * @param sys Overland map coordinate system
	 */
	private final void calculateOverlandMovementDistances_Cell (final int cellX, final int cellY, final int cellPlane, final int movingPlayerID,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units,
		final int doubleMovementRemaining, final int [] [] [] doubleMovementDistances, final int [] [] [] movementDirections,
		final boolean [] [] [] canMoveToInOneTurn, final boolean [] [] [] movingHereResultsInAttack, final Integer [] [] [] doubleMovementToEnterTile,
		final List<MapCoordinates2DEx> cellsLeftToCheck, final CoordinateSystem sys)
	{
		final int doubleDistanceToHere = doubleMovementDistances [cellPlane] [cellY] [cellX];
		final int doubleMovementRemainingToHere = doubleMovementRemaining - doubleDistanceToHere;

		// Try each direction
		for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (sys.getCoordinateSystemType ()); d++)
		{
			final MapCoordinates2DEx coords = new MapCoordinates2DEx (cellX, cellY);
			if (getCoordinateSystemUtils ().move2DCoordinates (sys, coords, d))
			{
				// Don't bother rechecking if we can already move here for free since we know we can't improve on that
				final int doubleCurrentDistanceToNewCoords = doubleMovementDistances [cellPlane] [coords.getY ()] [coords.getX ()];
				if ((doubleCurrentDistanceToNewCoords == MOVEMENT_DISTANCE_NOT_YET_CHECKED) || (doubleCurrentDistanceToNewCoords > 0))
				{
					// This is a valid location on the map that we've either not visited before or that we've already found another
					// path to (in which case we still need to check it - we might have found a quicker path now)
					// Check if our type of unit(s) can move here
					final Integer doubleMovementRateForThisTileType = doubleMovementToEnterTile [cellPlane] [coords.getY ()] [coords.getX ()];
					if (doubleMovementRateForThisTileType == null)

						// Can't move here
						doubleMovementDistances [cellPlane] [coords.getY ()] [coords.getX ()] = MOVEMENT_DISTANCE_CANNOT_MOVE_HERE;
					else
					{
						// How much movement (total) will it cost us to get here
						final int doubleNewDistanceToNewCoords = doubleDistanceToHere + doubleMovementRateForThisTileType;

						// Is this better than the current value for this cell?
						if ((doubleCurrentDistanceToNewCoords == MOVEMENT_DISTANCE_NOT_YET_CHECKED) || (doubleNewDistanceToNewCoords < doubleCurrentDistanceToNewCoords))
						{
							// Record the new distance
							doubleMovementDistances [cellPlane] [coords.getY ()] [coords.getX ()] = doubleNewDistanceToNewCoords;
							movementDirections [cellPlane] [coords.getY ()] [coords.getX ()] = d;
							canMoveToInOneTurn [cellPlane] [coords.getY ()] [coords.getX ()] = (doubleMovementRemainingToHere > 0);

							// Is this a square we have to stop at, i.e. one which contains enemy units?
							movingHereResultsInAttack [cellPlane] [coords.getY ()] [coords.getX ()] = getUnitCalculations ().willMovingHereResultInAnAttack
								(coords.getX (), coords.getY (), cellPlane, movingPlayerID, map, units);

							// Log that we need to check every location branching off from here
							if (!movingHereResultsInAttack [cellPlane] [coords.getY ()] [coords.getX ()])
								cellsLeftToCheck.add (coords);
						}
					}
				}
			}
		}
	}

	/**
	 * Works out everywhere we can move on the specified plane
	 *
	 * @param startX X location to start from
	 * @param startY Y location to start from
	 * @param startPlane Plane we are moving over
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge of the terrain
	 * @param units The player who is trying to move here's knowledge of units
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param movementDirections The direction that we moved to get here, e.g. the tile directly above startX, startY will have value 1
	 * @param canMoveToInOneTurn Indicates the locations that we can reach in a single turn (see the forester example above)
	 * @param movingHereResultsInAttack Indicates whether we know that moving here will result in attacking an enemy unit stack
	 * @param doubleMovementToEnterTile Double the movement points required to enter every tile on both planes; null = impassable
	 * @param sys Overland map coordinate system
	 */
	private final void calculateOverlandMovementDistances_Plane (final int startX, final int startY, final int startPlane, final int movingPlayerID,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units,
		final int doubleMovementRemaining, final int [] [] [] doubleMovementDistances, final int [] [] [] movementDirections,
		final boolean [] [] [] canMoveToInOneTurn, final boolean [] [] [] movingHereResultsInAttack, final Integer [] [] [] doubleMovementToEnterTile,
		final CoordinateSystem sys)
	{
		// We can move to where we start from for free
		doubleMovementDistances [startPlane] [startY] [startX] = 0;
		canMoveToInOneTurn [startPlane] [startY] [startX] = true;

		// Rather than iterating out distances from the centre, process rings around each location before proceeding to the next location
		// This is to prevent the situation in the original MoM where you are on Enchanced Road, hit 'Up' and the game decides to move you up-left and then right to get there
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		calculateOverlandMovementDistances_Cell (startX, startY, startPlane, movingPlayerID, map, units,
			doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
			doubleMovementToEnterTile, cellsLeftToCheck, sys);

		// Keep going until there's nowhere left to check
		while (cellsLeftToCheck.size () > 0)
		{
			calculateOverlandMovementDistances_Cell (cellsLeftToCheck.get (0).getX (), cellsLeftToCheck.get (0).getY (), startPlane, movingPlayerID, map, units,
				doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn,
				movingHereResultsInAttack, doubleMovementToEnterTile, cellsLeftToCheck, sys);

			cellsLeftToCheck.remove (0);
		}
	}

	/**
	 * Calculates how many (doubled) movement points it will take to move from x, y to ever other location on the map plus whether we can move there or not
	 * MoM is a little weird with how movement works - providing you have even 1/2 move left, you can move anywhere, even somewhere which takes 3 movement to get to
	 * e.g. a unit on a road with 1 movement can walk onto the road (1/2 mp), and then into mountains (3 mp) for a total move of 3 1/2 mp even thought they only had 1 movement!
	 * Therefore knowing distances to each location is not enough - we need a separate boolean array to mark whether we can or cannot reach each location
	 *
	 * @param startX X coordinate of location to move from
	 * @param startY Y coordinate of location to move from
	 * @param startPlane Plane to move from
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param movementDirections The direction that we moved to get here, e.g. the tile directly above startX, startY will have value 1
	 * @param canMoveToInOneTurn Indicates the locations that we can reach in a single turn (see the forester example above)
	 * @param movingHereResultsInAttack Indicates whether we know that moving here will result in attacking an enemy unit stack
	 * @param players List of players in this session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public final void calculateOverlandMovementDistances (final int startX, final int startY, final int startPlane, final int movingPlayerID,
		final FogOfWarMemory map, final UnitStack unitStack, final int doubleMovementRemaining,
		final int [] [] [] doubleMovementDistances, final int [] [] [] movementDirections, final boolean [] [] [] canMoveToInOneTurn,
		final boolean [] [] [] movingHereResultsInAttack, final List<? extends PlayerPublicDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack.getUnits ());
		
		// Count of the number of free transport spaces at every map cell; these can make otherwise impassable terrain passable
		final int [] [] [] cellTransportCapacity = calculateCellTransportCapacity (unitStack, unitStackSkills, movingPlayerID, map, players, sd.getOverlandMapSize (), db);
		
		// Work out all the movement rates over all tile types of the unit stack.
		// If a transporting move, only the movement speed of the transports matters.
		
		// Not sure this is necessarily correct - see example testCreateUnitStack_TransportOnly - if there are flying/swimming units
		// moving alongside the transports but not inside them, then their movement rates should be considered as well?
		final Map<String, Integer> doubleMovementRates = calculateDoubleMovementRatesForUnitStack
			((unitStack.getTransports ().size () > 0) ? unitStack.getTransports () : unitStack.getUnits (), db);

		// Count how many of OUR units are in every cell on the map - enemy units are fine, we'll just attack them :-)
		final int [] [] [] ourUnitCountAtLocation = countOurAliveUnitsAtEveryLocation (movingPlayerID, map.getUnit (), sd.getOverlandMapSize ());

		// Now we can work out the movement cost of entering every tile, taking into account the tiles we can't enter because we'll have too many units there
		final Integer [] [] [] doubleMovementToEnterTile = calculateDoubleMovementToEnterTile (unitStack, unitStackSkills,
			map.getMap (), cellTransportCapacity, ourUnitCountAtLocation, doubleMovementRates, sd.getUnitSetting ().getUnitsPerMapCell (), sd.getOverlandMapSize (), db);
		
		// Initialize all the map areas
		for (int z = 0; z < sd.getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				{
					doubleMovementDistances	[z] [y] [x] = MOVEMENT_DISTANCE_NOT_YET_CHECKED;
					movementDirections			[z] [y] [x] = 0;
					canMoveToInOneTurn			[z] [y] [x] = false;
					movingHereResultsInAttack	[z] [y] [x] = false;
				}

		// If at a tower of wizardry, we can move on all planes
		final OverlandMapTerrainData terrainData = map.getMap ().getPlane ().get (startPlane).getRow ().get (startY).getCell ().get (startX).getTerrainData ();
		if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
		{
			for (final Plane plane : db.getPlane ())
				calculateOverlandMovementDistances_Plane (startX, startY, plane.getPlaneNumber (), movingPlayerID, map.getMap (), map.getUnit (),
					doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
					doubleMovementToEnterTile, sd.getOverlandMapSize ());
		}
		else
			calculateOverlandMovementDistances_Plane (startX, startY, startPlane, movingPlayerID, map.getMap (), map.getUnit (),
				doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
				doubleMovementToEnterTile, sd.getOverlandMapSize ());
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}
}