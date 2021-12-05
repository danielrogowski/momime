package momime.common.movement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;

/**
 * Methods dealing with unit movement
 */
public final class UnitMovementImpl implements UnitMovement
{
	/** Marks locations in the doubleMovementDistances array that we've proved that we cannot move to */
	final static int MOVEMENT_DISTANCE_CANNOT_MOVE_HERE = -2;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Movement utils */
	private MovementUtils movementUtils;
	
	/**
	 * Given a cell we can move from and to, checks if the destination cell is actually passable,
	 * if its a quicker route to it than we may have already found. 
	 * 
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param moveFrom Coordinates of the cell we are moving from
	 * @param movementType How we are moving between the two cells
	 * @param direction Direction from the from to the to cell, only if movementType is ADJACENT
	 * @param moveTo Coordinates of the cell we can move to
	 * @param movingPlayerID The player who is trying to move here
	 * @param doubleDistanceToHere Amount of movement it took to reach moveFrom
	 * @param doubleMovementRemainingToHere Amount of movement we have remaining after moveFrom
	 * @param cellTransportCapacity Count of the number of free transport spaces at every map cell
	 * @param doubleMovementRates Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param cellsLeftToCheck List of cells that still need to be checked (we add adjacent cells to the end of this list)
	 * @param mem The player who is trying to move here's knowledge
	 * @param db Lookup lists built over the XML database
	 */
	@Override
	public final void considerPossibleMove (final UnitStack unitStack, final Set<String> unitStackSkills,
		final MapCoordinates3DEx moveFrom, final OverlandMovementType movementType, final int direction,
		final MapCoordinates3DEx moveTo, final int movingPlayerID, final int doubleDistanceToHere, final int doubleMovementRemainingToHere,
		final int [] [] [] cellTransportCapacity, final Map<String, Integer> doubleMovementRates,
		final OverlandMovementCell [] [] [] moves, final List<MapCoordinates3DEx> cellsLeftToCheck, final FogOfWarMemory mem, final CommonDatabase db)
	{
		// Don't bother rechecking if we can already move here for free since we know we can't improve on that
		OverlandMovementCell cell = moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()];
		if ((cell == null) || (cell.getDoubleMovementDistance () > 0))
		{
			// This is a valid location on the map that we've either not visited before or that we've already found another
			// path to (in which case we still need to check it - we might have found a quicker path now)
			// Check if our type of unit(s) can move here
			final Integer doubleMovementRateForThisTileType;
			if (cell != null)
				doubleMovementRateForThisTileType = cell.getDoubleMovementToEnterTile ();
			else
				doubleMovementRateForThisTileType = getMovementUtils ().calculateDoubleMovementToEnterTile
					(unitStack, unitStackSkills, moveTo, cellTransportCapacity, doubleMovementRates, mem.getMap (), db);
			
			if (doubleMovementRateForThisTileType == null)
			{
				// Can't move here
				final OverlandMovementCell impassable = new OverlandMovementCell ();
				impassable.setDoubleMovementToEnterTile (MOVEMENT_DISTANCE_CANNOT_MOVE_HERE);
				impassable.setDoubleMovementDistance (MOVEMENT_DISTANCE_CANNOT_MOVE_HERE);
				
				moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] = impassable;
			}
			else
			{
				// How much movement (total) will it cost us to get here
				final int doubleNewDistanceToNewCoords = doubleDistanceToHere + doubleMovementRateForThisTileType;

				// Is this better than the current value for this cell?
				if ((cell == null) || (doubleNewDistanceToNewCoords < cell.getDoubleMovementDistance ()))
				{
					// Record the new distance
					if (cell == null)
					{
						cell = new OverlandMovementCell ();
						moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] = cell;
					}
					
					cell.setMovementType (movementType);
					cell.setMovedFrom (moveFrom);
					cell.setDirection (direction);
					cell.setDoubleMovementToEnterTile (doubleMovementRateForThisTileType);
					cell.setDoubleMovementDistance (doubleNewDistanceToNewCoords);
					cell.setMoveToInOneTurn (doubleMovementRemainingToHere > 0);

					// Is this a square we have to stop at, i.e. one which contains enemy units?
					final boolean movingHereResultsInAttack = getUnitCalculations ().willMovingHereResultInAnAttackThatWeKnowAbout
						(moveTo.getX (), moveTo.getY (), moveTo.getZ (), movingPlayerID, mem, db);

					// Log that we need to check every location branching off from here
					if (!movingHereResultsInAttack)
						cellsLeftToCheck.add (moveTo);
				}
			}
		}
	}

	/**
	 * @param start Where the unit stack is standing and starting their move from
	 * @param movingPlayerID Owner of the unit stack
	 * @param unitStack Unit stack to move
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param players List of players in this session
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param mem The player who is trying to move here's knowledge
	 * @param db Lookup lists built over the XML database
	 * @return Array listing all cells we can reach and the paths to get there
	 * @throws PlayerNotFoundException If an expected player can't be found
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If there is a problem with any of the calculations
	 */
	@Override
	public final OverlandMovementCell [] [] [] calculateOverlandMovementDistances (final MapCoordinates3DEx start, final int movingPlayerID,
		final UnitStack unitStack, final int doubleMovementRemaining,
		final List<? extends PlayerPublicDetails> players, final CoordinateSystem overlandMapCoordinateSystem, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack.getUnits ());
		
		// Count of the number of free transport spaces at every map cell; these can make otherwise impassable terrain passable
		final int [] [] [] cellTransportCapacity = getMovementUtils ().calculateCellTransportCapacity
			(unitStack, unitStackSkills, movingPlayerID, mem, players, overlandMapCoordinateSystem, db);
		
		// Work out all the movement rates over all tile types of the unit stack.
		// If a transporting move, only the movement speed of the transports matters.
		
		// Not sure this is necessarily correct - see example testCreateUnitStack_TransportOnly - if there are flying/swimming units
		// moving alongside the transports but not inside them, then their movement rates should be considered as well?
		final Map<String, Integer> doubleMovementRates = getMovementUtils ().calculateDoubleMovementRatesForUnitStack
			((unitStack.getTransports ().size () > 0) ? unitStack.getTransports () : unitStack.getUnits (), db);

		// Count how many of OUR units are in every cell on the map - enemy units are fine, we'll just attack them :-)
		final int [] [] [] ourUnitCountAtLocation = getMovementUtils ().countOurAliveUnitsAtEveryLocation (movingPlayerID, mem.getUnit (), overlandMapCoordinateSystem);
		
		// Determine all the places we are blocked from entering for all reasons other than impassable terrain
		final Set<MapCoordinates3DEx> blockedLocations = getMovementUtils ().determineBlockedLocations
			(unitStack, movingPlayerID, ourUnitCountAtLocation, overlandMapCoordinateSystem, mem.getMaintainedSpell (), mem.getMap (), db);
	
		// Find usable Earth Gates
		final Set<MapCoordinates3DEx> earthGates = getMovementUtils ().findEarthGates (movingPlayerID, mem.getMaintainedSpell ());
		
		// Find usable Astral Gates
		final Set<MapCoordinates2DEx> astralGates = getMovementUtils ().findAstralGates (movingPlayerID, mem.getMaintainedSpell ());
		
		// Initialize the map area
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [overlandMapCoordinateSystem.getDepth ()] [overlandMapCoordinateSystem.getHeight ()] [overlandMapCoordinateSystem.getWidth ()];
		
		// We can move to where we start from for free
		final OverlandMovementCell startCell = new OverlandMovementCell ();
		startCell.setMovementType (OverlandMovementType.START);
		startCell.setMoveToInOneTurn (true);
		moves [start.getZ ()] [start.getY ()] [start.getX ()] = startCell;
	
		// Rather than iterating out distances from the centre, process rings around each location before proceeding to the next location
		// This is to prevent the situation in the original MoM where you are on Enchanced Road, hit 'Up' and the game decides to move you up-left and then right to get there
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		getMovementUtils ().processOverlandMovementCell (unitStack, unitStackSkills, start,
			movingPlayerID, doubleMovementRemaining, blockedLocations, earthGates, astralGates,
			cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, overlandMapCoordinateSystem, mem, db);

		// Keep going until there's nowhere left to check
		while (cellsLeftToCheck.size () > 0)
		{
			getMovementUtils ().processOverlandMovementCell (unitStack, unitStackSkills, cellsLeftToCheck.get (0),
				movingPlayerID, doubleMovementRemaining, blockedLocations, earthGates, astralGates,
				cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, overlandMapCoordinateSystem, mem, db);

			cellsLeftToCheck.remove (0);
		}
		
		// Before we return the array, null out any cells that were impassable
		for (int z = 0; z < overlandMapCoordinateSystem.getDepth (); z++)
			for (int y = 0; y < overlandMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < overlandMapCoordinateSystem.getWidth (); x++)
				{
					final OverlandMovementCell move = moves [z] [y] [x];
					if ((move != null) && (move.getDoubleMovementDistance () == MOVEMENT_DISTANCE_CANNOT_MOVE_HERE))
						moves [z] [y] [x] = null;
				}
		
		return moves;
	}
	
	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
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

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}

	/**
	 * @return Movement utils
	 */
	public final MovementUtils getMovementUtils ()
	{
		return movementUtils;
	}

	/**
	 * @param u Movement utils
	 */
	public final void setMovementUtils (final MovementUtils u)
	{
		movementUtils = u;
	}
}