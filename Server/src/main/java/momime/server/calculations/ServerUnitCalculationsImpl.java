package momime.server.calculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitHasSkill;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;
import momime.server.database.MovementRateRuleSvr;
import momime.server.database.PlaneSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.TileTypeSvr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates2D;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

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
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/**
	 * @param unit The unit to check
	 * @param players Pre-locked players list
	 * @param spells Known spells (flight spell might increase scouting range)
	 * @param combatAreaEffects Known combat area effects (because theoretically, you could define a CAE which bumped up the scouting skill...)
	 * @param db Lookup lists built over the XML database
	 * @return How many squares this unit can see; by default = 1, flying units automatically get 2, and the Scouting unit skill can push this even higher
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateUnitScoutingRange (final MemoryUnit unit, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateUnitScoutingRange: Unit URN " + unit.getUnitURN () + ", " + unit.getUnitID ());

		int scoutingRange = 1;

		// Make sure we only bother to do this once
		final UnitHasSkillMergedList mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (spells, unit);

		// Actual scouting skill
		scoutingRange = Math.max (scoutingRange, getUnitUtils ().getModifiedSkillValue
			(unit, mergedSkills, ServerDatabaseValues.UNIT_SKILL_ID_SCOUTING, players, spells, combatAreaEffects, db));

		// Scouting range granted by other skills (i.e. flight skills)
		for (final UnitHasSkill thisSkill : mergedSkills)
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
		if ((mc.getCityData () != null) && (mc.getCityData ().getCityPopulation () != null) && (mc.getCityData ().getCityOwnerID () != null) &&
			(mc.getCityData ().getCityPopulation () > 0) && (mc.getCityData ().getCityOwnerID () != movingPlayerID))

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
	 * @param unitStack Unit stack to check
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Merged list of every skill that at least one unit in the stack has, including skills granted from spells
	 */
	@Override
	public final List<String> listAllSkillsInUnitStack (final List<MemoryUnit> unitStack,
		final List<MemoryMaintainedSpell> spells, final ServerDatabaseEx db)
	{
		log.trace ("Entering listAllSkillsInUnitStack: " + getUnitUtils ().listUnitURNs (unitStack));

		final List<String> list = new ArrayList<String> ();
		String debugList = "";

		if (unitStack != null)
			for (final MemoryUnit thisUnit : unitStack)
				for (final UnitHasSkill thisSkill : getUnitUtils ().mergeSpellEffectsIntoSkillList (spells, thisUnit))
					if (!list.contains (thisSkill.getUnitSkillID ()))
					{
						list.add (thisSkill.getUnitSkillID ());

						if (!debugList.equals (""))
							debugList = debugList + ", ";

						debugList = debugList + thisSkill.getUnitSkillID ();
					}

		log.trace ("Exiting listAllSkillsInUnitStack = " + debugList);
		return list;
	}

	/**
	 * @param unit Unit that we want to move
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param tileTypeID Type of tile we are moving onto
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Double the number of movement points we will use to walk onto that tile; null = impassable
	 */
	@Override
	public final Integer calculateDoubleMovementToEnterTileType (final AvailableUnit unit, final List<String> unitStackSkills, final String tileTypeID,
		final List<MemoryMaintainedSpell> spells, final ServerDatabaseEx db)
	{
		log.trace ("Entering calculateDoubleMovementToEnterTileType: " + unit.getUnitID () + ", Player ID " + unit.getOwningPlayerID () + ", " + tileTypeID);

		// Only merge the units list of skills once
		final List<UnitHasSkill> unitHasSkills;
		if (unit instanceof MemoryUnit)
			unitHasSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit);
		else
			unitHasSkills = unit.getUnitHasSkill ();

		// Turn it into a list of strings so we can search it more quickly
		final List<String> unitSkills = new ArrayList<String> ();
		for (final UnitHasSkill thisSkill : unitHasSkills)
			unitSkills.add (thisSkill.getUnitSkillID ());

		// We basically run down the movement rate rules and stop as soon as we find the first applicable one
		// Terrain is impassable if we check every movement rule and none of them are applicable
		Integer doubleMovement = null;
		final Iterator<MovementRateRuleSvr> rules = db.getMovementRateRules ().iterator ();
		while ((doubleMovement == null) && (rules.hasNext ()))
		{
			final MovementRateRuleSvr thisRule = rules.next ();

			// All 3 parts are optional
			if (((thisRule.getTileTypeID () == null) || (thisRule.getTileTypeID ().equals (tileTypeID))) &&
				((thisRule.getUnitSkillID () == null) || (unitSkills.contains (thisRule.getUnitSkillID ()))) &&
				((thisRule.getUnitStackSkillID () == null) || (unitStackSkills.contains (thisRule.getUnitStackSkillID ()))))

				doubleMovement = thisRule.getDoubleMovement ();
		}

		log.trace ("Exiting calculateDoubleMovementToEnterTileType = " + doubleMovement);
		return doubleMovement;
	}

	/**
	 * @param unitStack Unit stack we are moving
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 */
	final Map<String, Integer> calculateDoubleMovementRatesForUnitStack (final List<MemoryUnit> unitStack,
		final List<MemoryMaintainedSpell> spells, final ServerDatabaseEx db)
	{
		log.trace ("Entering calculateDoubleMovementRatesForUnitStack: " + getUnitUtils ().listUnitURNs (unitStack));

		// Get list of all the skills that any unit in the stack has, in case any of them have path finding, wind walking, etc.
		final List<String> unitStackSkills = listAllSkillsInUnitStack (unitStack, spells, db);

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

					final Integer thisMovementRate = calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills, tileType.getTileTypeID (), spells, db);
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
	 */
	@Override
	public final void calculateOverlandMovementDistances (final int startX, final int startY, final int startPlane, final int movingPlayerID,
		final FogOfWarMemory map, final List<MemoryUnit> unitStack, final int doubleMovementRemaining,
		final int [] [] [] doubleMovementDistances, final int [] [] [] movementDirections, final boolean [] [] [] canMoveToInOneTurn,
		final boolean [] [] [] movingHereResultsInAttack,
		final MomSessionDescription sd, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering calculateOverlandMovementDistances: (" + startX + ", " + startY + ", " + startPlane + ")");

		// Work out all the movement rates over all tile types of the unit stack
		final Map<String, Integer> doubleMovementRates = calculateDoubleMovementRatesForUnitStack (unitStack, map.getMaintainedSpell (), db);

		// Count how many of OUR units are in every cell on the map - enemy units are fine, we'll just attack them :-)
		final int [] [] [] ourUnitCountAtLocation = countOurAliveUnitsAtEveryLocation (movingPlayerID, map.getUnit (), sd.getMapSize ());

		// Now we can work out the movement cost of entering every tile, taking into account the tiles we can't enter because we'll have too many units there
		final Integer [] [] [] doubleMovementToEnterTile = new Integer [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		for (final PlaneSvr plane : db.getPlanes ())
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				{
					// If cell will be full, leave it as null = impassable
					if (ourUnitCountAtLocation [plane.getPlaneNumber ()] [y] [x] + unitStack.size () <= sd.getUnitSetting ().getUnitsPerMapCell ())
						doubleMovementToEnterTile [plane.getPlaneNumber ()] [y] [x] = doubleMovementRates.get (getMemoryGridCellUtils ().convertNullTileTypeToFOW
							(map.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ()));

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
					doubleMovementToEnterTile, sd.getMapSize (), db);
		}
		else
			calculateOverlandMovementDistances_Plane (startX, startY, startPlane, movingPlayerID, map.getMap (), map.getUnit (),
				doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
				doubleMovementToEnterTile, sd.getMapSize (), db);

		log.trace ("Exiting calculateOverlandMovementDistances");
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
}