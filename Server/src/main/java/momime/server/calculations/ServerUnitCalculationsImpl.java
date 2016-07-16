package momime.server.calculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates2D;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.calculations.UnitStack;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RangedAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;
import momime.server.database.PlaneSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.TileTypeSvr;
import momime.server.database.UnitSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.KillUnitActionID;

/**
 * Server only calculations pertaining to units, e.g. calculations relating to fog of war
 */
public final class ServerUnitCalculationsImpl implements ServerUnitCalculations
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ServerUnitCalculationsImpl.class);
	
	/** Marks locations in the doubleMovementDistances array that we haven't checked yet */
	private static final int MOVEMENT_DISTANCE_NOT_YET_CHECKED = -1;

	/** Marks locations in the doubleMovementDistances array that we've proved that we cannot move to */
	private static final int MOVEMENT_DISTANCE_CANNOT_MOVE_HERE = -2;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Random utils */
	private RandomUtils randomUtils; 
	
	/**
	 * @param unit The unit to check
	 * @param players Pre-locked players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How many squares this unit can see; by default = 1, flying units automatically get 2, and the Scouting unit skill can push this even higher
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateUnitScoutingRange (final MemoryUnit unit, final List<PlayerServerDetails> players,
		final FogOfWarMemory mem, final ServerDatabaseEx db) throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateUnitScoutingRange: Unit URN " + unit.getUnitURN () + ", " + unit.getUnitID ());

		int scoutingRange = 1;

		// Make sure we only bother to do this once
		final UnitHasSkillMergedList mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (mem.getMaintainedSpell (), unit, db);

		// Actual scouting skill
		scoutingRange = Math.max (scoutingRange, getUnitSkillUtils ().getModifiedSkillValue
			(unit, mergedSkills, ServerDatabaseValues.UNIT_SKILL_ID_SCOUTING, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db));

		// Scouting range granted by other skills (i.e. flight skills)
		for (final UnitSkillAndValue thisSkill : mergedSkills)
		{
			final Integer unitSkillScoutingRange = db.findUnitSkill (thisSkill.getUnitSkillID (), "calculateUnitScoutingRange").getUnitSkillScoutingRange ();
			if (unitSkillScoutingRange != null)
				scoutingRange = Math.max (scoutingRange, unitSkillScoutingRange);
		}

		log.trace ("Exiting calculateUnitScoutingRange = " + scoutingRange);
		return scoutingRange;
	}

	/**
	 * @param playerID Player whose units to count
	 * @param units Player's knowledge of all units
	 * @param sys Overland map coordinate system
	 * @return Count how many of that player's units are in every cell on the map
	 */
	final int [] [] [] countOurAliveUnitsAtEveryLocation (final int playerID, final List<MemoryUnit> units, final CoordinateSystem sys)
	{
		log.trace ("Entering countOurAliveUnitsAtEveryLocation: Player ID " + playerID);

		final int [] [] [] count = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getOwningPlayerID () == playerID) && (thisUnit.getStatus () == UnitStatusID.ALIVE) & (thisUnit.getUnitLocation () != null))
				count [thisUnit.getUnitLocation ().getZ ()] [thisUnit.getUnitLocation ().getY ()] [thisUnit.getUnitLocation ().getX ()]++;

		log.trace ("Exiting countOurAliveUnitsAtEveryLocation");
		return count;
	}

	/**
	 * @param x X coordinate of the location we want to check
	 * @param y Y coordinate of the location we want to check
	 * @param plane Plane we want to check
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge of the terrain
	 * @param units The player who is trying to move here's knowledge of units
	 * @param db Lookup lists built over the XML database
	 * @return Whether moving here will result in an attack or not
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	@Override
	public final boolean willMovingHereResultInAnAttack (final int x, final int y, final int plane, final int movingPlayerID,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units,
		final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering willMovingHereResultInAnAttack: Player ID " + movingPlayerID +
			", (" + x + ", " + y + ", " + plane + ")");

		// Work out what plane to look for units on
		final MemoryGridCell mc = map.getPlane ().get (plane).getRow ().get (y).getCell ().get (x);
		final int towerPlane;
		if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ()))
			towerPlane = 0;
		else
			towerPlane = plane;

		// The easiest one to check for is an enemy city - even if there's no units there, it still counts as an attack so we can decide whether to raze or capture it
		final boolean resultsInAttack;
		if ((mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () != movingPlayerID))
			resultsInAttack = true;

		// Lastly check for enemy units
		else if (getUnitUtils ().findFirstAliveEnemyAtLocation (units, x, y, towerPlane, movingPlayerID) != null)
			resultsInAttack = true;
		else
			resultsInAttack = false;

		log.trace ("Exiting willMovingHereResultInAnAttack = " + resultsInAttack);
		return resultsInAttack;
	}

	/**
	 * @param unitStack Unit stack we are moving
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	final Map<String, Integer> calculateDoubleMovementRatesForUnitStack (final List<MemoryUnit> unitStack,
		final List<MemoryMaintainedSpell> spells, final ServerDatabaseEx db) throws RecordNotFoundException, MomException
	{
		log.trace ("Entering calculateDoubleMovementRatesForUnitStack: " + getUnitUtils ().listUnitURNs (unitStack));

		// Get list of all the skills that any unit in the stack has, in case any of them have path finding, wind walking, etc.
		final List<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack, spells, db);

		// Go through each tile type
		final Map<String, Integer> movementRates = new HashMap<String, Integer> ();
		for (final TileTypeSvr tileType : db.getTileTypes ())
			if (!tileType.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR_HAVE_SEEN))
			{
				Integer worstMovementRate = 0;

				// Check every unit - stop if we've found that terrain is impassable to someone
				final Iterator<MemoryUnit> unitIter = unitStack.iterator ();
				while ((worstMovementRate != null) && (unitIter.hasNext ()))
				{
					final MemoryUnit thisUnit = unitIter.next ();

					final Integer thisMovementRate = getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills, tileType.getTileTypeID (), spells, db);
					if (thisMovementRate == null)
						worstMovementRate = null;
					else if (thisMovementRate > worstMovementRate)
						worstMovementRate = thisMovementRate;
				}

				// No point putting it into the map if it is impassable - HashMap.get returns null for keys not in the map anyway
				if (worstMovementRate != null)
					movementRates.put (tileType.getTileTypeID (), worstMovementRate);
			}

		log.trace ("Exiting calculateDoubleMovementRatesForUnitStack");
		return movementRates;
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
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	private final void calculateOverlandMovementDistances_Cell (final int cellX, final int cellY, final int cellPlane, final int movingPlayerID,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units,
		final int doubleMovementRemaining, final int [] [] [] doubleMovementDistances, final int [] [] [] movementDirections,
		final boolean [] [] [] canMoveToInOneTurn, final boolean [] [] [] movingHereResultsInAttack, final Integer [] [] [] doubleMovementToEnterTile,
		final List<MapCoordinates2D> cellsLeftToCheck, final CoordinateSystem sys, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering calculateOverlandMovementDistances_Cell: Player ID " + movingPlayerID +
			", (" + cellX + ", " + cellY + ", " + cellPlane + ")");

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
							movingHereResultsInAttack [cellPlane] [coords.getY ()] [coords.getX ()] = willMovingHereResultInAnAttack
								(coords.getX (), coords.getY (), cellPlane, movingPlayerID, map, units, db);

							// Log that we need to check every location branching off from here
							if (!movingHereResultsInAttack [cellPlane] [coords.getY ()] [coords.getX ()])
								cellsLeftToCheck.add (coords);
						}
					}
				}
			}
		}

		log.trace ("Exiting calculateOverlandMovementDistances_Cell");
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
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	private final void calculateOverlandMovementDistances_Plane (final int startX, final int startY, final int startPlane, final int movingPlayerID,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units,
		final int doubleMovementRemaining, final int [] [] [] doubleMovementDistances, final int [] [] [] movementDirections,
		final boolean [] [] [] canMoveToInOneTurn, final boolean [] [] [] movingHereResultsInAttack, final Integer [] [] [] doubleMovementToEnterTile,
		final CoordinateSystem sys, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering calculateOverlandMovementDistances_Plane: Player ID " + movingPlayerID + ", (" + startX + ", " + startY + ", " + startPlane + ")");

		// We can move to where we start from for free
		doubleMovementDistances [startPlane] [startY] [startX] = 0;
		canMoveToInOneTurn [startPlane] [startY] [startX] = true;

		// Rather than iterating out distances from the centre, process rings around each location before proceeding to the next location
		// This is to prevent the situation in the original MoM where you are on Enchanced Road, hit 'Up' and the game decides to move you up-left and then right to get there
		final List<MapCoordinates2D> cellsLeftToCheck = new ArrayList<MapCoordinates2D> ();
		calculateOverlandMovementDistances_Cell (startX, startY, startPlane, movingPlayerID, map, units,
			doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
			doubleMovementToEnterTile, cellsLeftToCheck, sys, db);

		// Keep going until there's nowhere left to check
		while (cellsLeftToCheck.size () > 0)
		{
			calculateOverlandMovementDistances_Cell (cellsLeftToCheck.get (0).getX (), cellsLeftToCheck.get (0).getY (), startPlane, movingPlayerID, map, units,
				doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn,
				movingHereResultsInAttack, doubleMovementToEnterTile, cellsLeftToCheck, sys, db);

			cellsLeftToCheck.remove (0);
		}

		log.trace ("Exiting calculateOverlandMovementDistances_Plane");
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
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public final void calculateOverlandMovementDistances (final int startX, final int startY, final int startPlane, final int movingPlayerID,
		final FogOfWarMemory map, final UnitStack unitStack, final int doubleMovementRemaining,
		final int [] [] [] doubleMovementDistances, final int [] [] [] movementDirections, final boolean [] [] [] canMoveToInOneTurn,
		final boolean [] [] [] movingHereResultsInAttack,
		final MomSessionDescription sd, final ServerDatabaseEx db) throws RecordNotFoundException, MomException
	{
		log.trace ("Entering calculateOverlandMovementDistances: (" + startX + ", " + startY + ", " + startPlane + ")");

		final List<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack.getUnits (), map.getMaintainedSpell (), db);
		
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
			cellTransportCapacity = new int [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()]; 
			for (final MemoryUnit thisUnit : map.getUnit ())
				if ((thisUnit.getOwningPlayerID () == movingPlayerID) && (thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getUnitLocation () != null))
				{
					final int x = thisUnit.getUnitLocation ().getX ();
					final int y = thisUnit.getUnitLocation ().getY ();
					final int z = thisUnit.getUnitLocation ().getZ ();
					
					final OverlandMapTerrainData terrainData = map.getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getTerrainData ();
					
					// Count space granted by transports
					final Integer unitTransportCapacity = db.findUnit (thisUnit.getUnitID (), "calculateOverlandMovementDistances").getTransportCapacity ();
					if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
					{
						if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
						{
							for (final PlaneSvr plane : db.getPlanes ())
								cellTransportCapacity [plane.getPlaneNumber ()] [y] [x] = cellTransportCapacity [plane.getPlaneNumber ()] [y] [x] + unitTransportCapacity;
						}
						else
							cellTransportCapacity [z] [y] [x] = cellTransportCapacity [z] [y] [x] + unitTransportCapacity;
					}
					
					// Count space taken up by units already in transports
					else if (getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills, getMemoryGridCellUtils ().convertNullTileTypeToFOW
						(terrainData), map.getMaintainedSpell (), db) == null)
						
						if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
						{
							for (final PlaneSvr plane : db.getPlanes ())
								cellTransportCapacity [plane.getPlaneNumber ()] [y] [x]--;
						}
						else
							cellTransportCapacity [z] [y] [x]--;
				}			
		}
		
		// Work out all the movement rates over all tile types of the unit stack
		// If a transporting move, only the movement speed of the transports matters 
		final Map<String, Integer> doubleMovementRates = calculateDoubleMovementRatesForUnitStack
			((unitStack.getTransports ().size () > 0) ? unitStack.getTransports () : unitStack.getUnits (), map.getMaintainedSpell (), db);

		// Count how many of OUR units are in every cell on the map - enemy units are fine, we'll just attack them :-)
		final int [] [] [] ourUnitCountAtLocation = countOurAliveUnitsAtEveryLocation (movingPlayerID, map.getUnit (), sd.getOverlandMapSize ());

		// Now we can work out the movement cost of entering every tile, taking into account the tiles we can't enter because we'll have too many units there
		final Integer [] [] [] doubleMovementToEnterTile = new Integer [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
		for (final PlaneSvr plane : db.getPlanes ())
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				{
					// If cell will be full, leave it as null = impassable
					if (ourUnitCountAtLocation [plane.getPlaneNumber ()] [y] [x] + unitStack.getTransports ().size () + unitStack.getUnits ().size () <= sd.getUnitSetting ().getUnitsPerMapCell ())
					{
						final OverlandMapTerrainData terrainData = map.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ();
						Integer movementRate = doubleMovementRates.get (getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData));
						
						// If the cell is otherwise impassable to us (i.e. land units trying to walk onto water) but there's enough space in a transport there, then allow it
						if ((movementRate == null) && (cellTransportCapacity != null) && (cellTransportCapacity [plane.getPlaneNumber ()] [y] [x] > 0))
						{
							// Work out how many spaces we -need-
							// Can't do this up front because it varies depending on whether the terrain being moved to is impassable to each kind of unit in the stack
							int spaceRequired = 0;
							boolean impassableToTransport = false;
							for (final MemoryUnit thisUnit : unitStack.getUnits ())
							{															
								final boolean impassable = (getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills,
									getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData), map.getMaintainedSpell (), db) == null);
								
								// Count space granted by transports
								final Integer unitTransportCapacity = db.findUnit (thisUnit.getUnitID (), "calculateOverlandMovementDistances").getTransportCapacity ();
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
							if ((!impassableToTransport) && (cellTransportCapacity [plane.getPlaneNumber ()] [y] [x] >= spaceRequired))
								movementRate = 2;
						}
						
						doubleMovementToEnterTile [plane.getPlaneNumber ()] [y] [x] = movementRate;
					}

					// Initialize all the map areas
					doubleMovementDistances	[plane.getPlaneNumber ()] [y] [x] = MOVEMENT_DISTANCE_NOT_YET_CHECKED;
					movementDirections			[plane.getPlaneNumber ()] [y] [x] = 0;
					canMoveToInOneTurn			[plane.getPlaneNumber ()] [y] [x] = false;
					movingHereResultsInAttack	[plane.getPlaneNumber ()] [y] [x] = false;
				}

		// If at a tower of wizardry, we can move on all planes
		final OverlandMapTerrainData terrainData = map.getMap ().getPlane ().get (startPlane).getRow ().get (startY).getCell ().get (startX).getTerrainData ();
		if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
		{
			for (final PlaneSvr plane : db.getPlanes ())
				calculateOverlandMovementDistances_Plane (startX, startY, plane.getPlaneNumber (), movingPlayerID, map.getMap (), map.getUnit (),
					doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
					doubleMovementToEnterTile, sd.getOverlandMapSize (), db);
		}
		else
			calculateOverlandMovementDistances_Plane (startX, startY, startPlane, movingPlayerID, map.getMap (), map.getUnit (),
				doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
				doubleMovementToEnterTile, sd.getOverlandMapSize (), db);

		log.trace ("Exiting calculateOverlandMovementDistances");
	}
	
	/**
	 * Rechecks that transports have sufficient space to hold all units for whom the terrain is impassable.
	 * This is used after naval combats where some of the transports may have died, to kill off any surviving units who now have no transport,
	 * or perhaps a unit had Flight cast on it which was dispelled during combat.
	 * 
	 * @param combatLocation The combatLocation where the units need to be rechecked
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void recheckTransportCapacity (final MapCoordinates3DEx combatLocation, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final FogOfWarSetting fogOfWarSettings, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.trace ("Entering recheckTransportCapacity: " + combatLocation);
		
		// First get a list of the map coordinates and players to check; this could be two cells if the defender won - they'll have units at the combatLocation and the
		// attackers' transports may have been wiped out but the transported units are still sat at the point they attacked from.
		final List<MapCoordinates3DEx> mapLocations = new ArrayList<MapCoordinates3DEx> ();
		final List<Integer> playerIDs = new ArrayList<Integer> ();
		for (final MemoryUnit tu : trueMap.getUnit ())
			if ((tu.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (tu.getCombatLocation ())))
			{
				if (!mapLocations.contains (tu.getUnitLocation ()))
					mapLocations.add ((MapCoordinates3DEx) tu.getUnitLocation ());
				
				if (!playerIDs.contains (tu.getOwningPlayerID ()))
					playerIDs.add (tu.getOwningPlayerID ());
			}
		
		// Now check all locations and all players
		for (final MapCoordinates3DEx mapLocation : mapLocations)
			for (final Integer playerID : playerIDs)
			{
				log.debug ("recheckTransportCapacity checking location " + mapLocation + " for units owned by player ID " + playerID);

				final OverlandMapTerrainData terrainData = trueMap.getMap ().getPlane ().get
					(mapLocation.getZ ()).getRow ().get (mapLocation.getY ()).getCell ().get (mapLocation.getX ()).getTerrainData ();
				
				// List all the units at this location owned by this player
				final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
				for (final MemoryUnit tu : trueMap.getUnit ())
					if ((tu.getStatus () == UnitStatusID.ALIVE) && (mapLocation.equals (tu.getUnitLocation ())) && (playerID == tu.getOwningPlayerID ()))
						unitStack.add (tu);
				
				// Get a list of the unit stack skills
				final List<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack, trueMap.getMaintainedSpell (), db);
				
				// Now check each unit in the stack
				final List<MemoryUnit> impassableUnits = new ArrayList<MemoryUnit> ();
				int spaceRequired = 0;
				for (final MemoryUnit tu : unitStack)
				{
					final boolean impassable = (getUnitCalculations ().calculateDoubleMovementToEnterTileType (tu, unitStackSkills,
						getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData), trueMap.getMaintainedSpell (), db) == null);
						
					// Count space granted by transports
					final Integer unitTransportCapacity = db.findUnit (tu.getUnitID (), "recheckTransportCapacity").getTransportCapacity ();
					if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
					{
						// Transports on impassable terrain just get killed (maybe a ship had its flight spell dispelled during an overland combat)
						if (impassable)
						{
							log.debug ("Killing Unit URN " + tu.getUnitURN () + " (transport on impassable terrain)");
							getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (tu, KillUnitActionID.HEALABLE_OVERLAND_DAMAGE, trueMap, players, fogOfWarSettings, db);
						}
						else
							spaceRequired = spaceRequired - unitTransportCapacity;
					}
					else if (impassable)
					{
						spaceRequired++;
						impassableUnits.add (tu);
					}
				}
				
				// Need to kill off any units?
				while ((spaceRequired > 0) && (impassableUnits.size () > 0))
				{
					final MemoryUnit killUnit = impassableUnits.get (getRandomUtils ().nextInt (impassableUnits.size ()));
					log.debug ("Killing Unit URN " + killUnit.getUnitURN () + " (unit on impassable terrain)");
					
					getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (killUnit, KillUnitActionID.HEALABLE_OVERLAND_DAMAGE, trueMap, players, fogOfWarSettings, db);
					
					spaceRequired--;
					impassableUnits.remove (killUnit);
				}
			}
		
		log.trace ("Exiting recheckTransportCapacity");
	}

	/**
	 * Non-magical ranged attack incurr a -10% to hit penalty for each 3 tiles distance between the attacking and defending unit on the combat map.
	 * This is loosely explained in the manual and strategy guide, but the info on the MoM wiki is clearer.
	 * 
	 * @param attacker Unit firing the ranged attack
	 * @param defender Unit being shot
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return To hit penalty incurred from the distance between the attacker and defender, NB. this is not capped in any way so may get very high values here
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateRangedAttackDistancePenalty (final MemoryUnit attacker, final MemoryUnit defender,
		final CombatMapSize combatMapCoordinateSystem, final List<PlayerServerDetails> players,
		final FogOfWarMemory mem, final ServerDatabaseEx db) throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateRangedAttackDistancePenalty: Attacker Unit URN " + attacker.getUnitURN () + ", Defender Unit URN " + defender.getUnitURN ());

		final UnitSvr unitDef = db.findUnit (attacker.getUnitID (), "calculateRangedAttackDistancePenalty");
		final RangedAttackType rat = db.findRangedAttackType (unitDef.getRangedAttackType (), "calculateRangedAttackDistancePenalty");
		
		// Magic attacks suffer no penalty
		int penalty;
		if (rat.getMagicRealmID () != null)
			penalty = 0;
		else
		{
			final double distance = getCoordinateSystemUtils ().determineReal2DDistanceBetween
				(combatMapCoordinateSystem, (MapCoordinates2DEx) attacker.getCombatPosition (), (MapCoordinates2DEx) defender.getCombatPosition ());
			
			penalty = (int) (distance / 3);
			
			// Long range skill?
			if (penalty > 1)
			{
				final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
				defenders.add (defender);
				
				if (getUnitSkillUtils ().getModifiedSkillValue (attacker, attacker.getUnitHasSkill (), ServerDatabaseValues.UNIT_SKILL_ID_LONG_RANGE, defenders,
					UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db) >= 0)
				
					penalty = 1;
			}
		}
		
		log.trace ("Exiting calculateRangedAttackDistancePenalty = " + penalty);
		return penalty;
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
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
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
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Random utils
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random utils
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
}