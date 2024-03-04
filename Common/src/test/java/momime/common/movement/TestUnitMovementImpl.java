package momime.common.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.Spell;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitVisibilityUtils;

/**
 * Tests the UnitMovementImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitMovementImpl
{
	/**
	 * Tests the considerPossibleOverlandMove method where no previous route to the target cell was found
	 */
	@Test	
	public final void testConsiderPossibleOverlandMove_NoPreviousRoute ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();

		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		// Cost to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterTile
			(unitStack, unitStackSkills, new MapCoordinates3DEx (21, 11, 1), cellTransportCapacity, doubleMovementRates, mem.getMap (), db)).thenReturn (2);
		
		// Enemies in tile?
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.willMovingHereResultInAnAttackThatWeKnowAbout (21, 11, 1, 1, mem, db)).thenReturn (false);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		
		// Run method
		utils.considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1), OverlandMovementType.ADJACENT, 8,
			new MapCoordinates3DEx (21, 11, 1), 1, 2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		// Check cell generated
		final OverlandMovementCell move = moves [1] [11] [21];
		assertNotNull (move);
		assertEquals (new MapCoordinates3DEx (20, 10, 1), move.getMovedFrom ());
		assertEquals (OverlandMovementType.ADJACENT, move.getMovementType ());
		assertEquals (8, move.getDirection ());
		assertEquals (4, move.getDoubleMovementDistance ());
		assertEquals (2, move.getDoubleMovementToEnterTile ());
		
		// Check route from cell will be checked
		assertEquals (1, cellsLeftToCheck.size ());
		assertEquals (new MapCoordinates3DEx (21, 11, 1), cellsLeftToCheck.get (0));
		
		verifyNoMoreInteractions (db, movementUtils, unitCalculations);
	}

	/**
	 * Tests the considerPossibleOverlandMove method where we can reach the cell, but it will start an attack so we can't move past the cell
	 */
	@Test	
	public final void testConsiderPossibleOverlandMove_Attack ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();

		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		// Cost to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterTile
			(unitStack, unitStackSkills, new MapCoordinates3DEx (21, 11, 1), cellTransportCapacity, doubleMovementRates, mem.getMap (), db)).thenReturn (2);
		
		// Enemies in tile?
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.willMovingHereResultInAnAttackThatWeKnowAbout (21, 11, 1, 1, mem, db)).thenReturn (true);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		
		// Run method
		utils.considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1), OverlandMovementType.ADJACENT, 8,
			new MapCoordinates3DEx (21, 11, 1), 1, 2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		// Check cell generated
		final OverlandMovementCell move = moves [1] [11] [21];
		assertNotNull (move);
		assertEquals (new MapCoordinates3DEx (20, 10, 1), move.getMovedFrom ());
		assertEquals (OverlandMovementType.ADJACENT, move.getMovementType ());
		assertEquals (8, move.getDirection ());
		assertEquals (4, move.getDoubleMovementDistance ());
		assertEquals (2, move.getDoubleMovementToEnterTile ());
		
		// Check route from cell will be checked
		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (db, movementUtils, unitCalculations);
	}
	
	/**
	 * Tests the considerPossibleOverlandMove method where the target cell is impassable
	 */
	@Test	
	public final void testConsiderPossibleOverlandMove_Impassable ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();

		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		// Tile is impassable
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterTile
			(unitStack, unitStackSkills, new MapCoordinates3DEx (21, 11, 1), cellTransportCapacity, doubleMovementRates, mem.getMap (), db)).thenReturn (null);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		
		// Run method
		utils.considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1), OverlandMovementType.ADJACENT, 8,
			new MapCoordinates3DEx (21, 11, 1), 1, 2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		// Check cell generated
		final OverlandMovementCell move = moves [1] [11] [21];
		assertNotNull (move);
		assertNull (move.getMovedFrom ());
		assertNull (move.getMovementType ());
		assertEquals (0, move.getDirection ());
		assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE, move.getDoubleMovementDistance ());
		assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE, move.getDoubleMovementToEnterTile ());
		
		// Check route from cell will be checked
		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (db, movementUtils);
	}
	
	/**
	 * Tests the considerPossibleOverlandMove method where we already know the target cell is impassable so there's no point rechecking it
	 */
	@Test	
	public final void testConsiderPossibleOverlandMove_KnownImpassable ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();

		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];

		final OverlandMovementCell impassable = new OverlandMovementCell ();
		impassable.setDoubleMovementToEnterTile (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE);
		impassable.setDoubleMovementDistance (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE);
		moves [1] [11] [21] = impassable;

		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		
		// Run method
		utils.considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1), OverlandMovementType.ADJACENT, 8,
			new MapCoordinates3DEx (21, 11, 1), 1, 2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		// Check cell was unaltered
		final OverlandMovementCell move = moves [1] [11] [21];
		assertSame (impassable, move);
		assertNotNull (move);
		assertNull (move.getMovedFrom ());
		assertNull (move.getMovementType ());
		assertEquals (0, move.getDirection ());
		assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE, move.getDoubleMovementDistance ());
		assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE, move.getDoubleMovementToEnterTile ());
		
		// Check route from cell will be checked
		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (db);
	}
	
	/**
	 * Tests the calculateOverlandMovementDistances method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateOverlandMovementDistances () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Known terrain
		final FogOfWarMemory mem = new FogOfWarMemory (); 
		
		// Units moving
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.listAllSkillsInUnitStack (unitStack.getUnits ())).thenReturn (unitStackSkills);
		
		// Our transports at each map location
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateCellTransportCapacity (unitStack, unitStackSkills, 2, mem, players, sys, db)).thenReturn (cellTransportCapacity);
		
		// Movement rates
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		when (movementUtils.calculateDoubleMovementRatesForUnitStack (unitStack.getUnits (), db)).thenReturn (doubleMovementRates);
		
		// Our units at each map location
		final int [] [] [] ourUnitCountAtLocation = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		when (movementUtils.countOurAliveUnitsAtEveryLocation (2, mem.getUnit (), sys)).thenReturn (ourUnitCountAtLocation);
		
		// Blocked locations, e.g. full map cells or Spell Ward
		final Set<MapCoordinates3DEx> blockedLocations = new HashSet<MapCoordinates3DEx> ();
		when (movementUtils.determineBlockedLocations (unitStack, 2, ourUnitCountAtLocation, sys, mem, db)).thenReturn (blockedLocations);
		
		// Gates
		final Set<MapCoordinates3DEx> earthGates = new HashSet<MapCoordinates3DEx> ();
		final Set<MapCoordinates2DEx> astralGates = new HashSet<MapCoordinates2DEx> ();
		
		when (movementUtils.findEarthGates (2, mem.getMaintainedSpell ())).thenReturn (earthGates);
		when (movementUtils.findAstralGates (2, mem.getMaintainedSpell ())).thenReturn (astralGates);
		
		// Have the initial call on the start tile add 2 more cells to check
		@SuppressWarnings ("unchecked")
		final ArgumentCaptor<List<MapCoordinates3DEx>> cellsLeftToCheck = ArgumentCaptor.forClass (List.class);
		final ArgumentCaptor<OverlandMovementCell [] [] []> distancesCapture = ArgumentCaptor.forClass (OverlandMovementCell [] [] [].class);
		doAnswer ((i) ->
		{
			cellsLeftToCheck.getValue ().add (new MapCoordinates3DEx (19, 10, 1));
			cellsLeftToCheck.getValue ().add (new MapCoordinates3DEx (21, 10, 1));
			
			// Also set some other map cells, to test the nulling out of "cannot move here"
			final OverlandMovementCell left = new OverlandMovementCell ();
			final OverlandMovementCell right = new OverlandMovementCell ();
			
			left.setDoubleMovementDistance (1);
			right.setDoubleMovementDistance (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE);
			
			distancesCapture.getValue () [1] [10] [19] = left;
			distancesCapture.getValue () [1] [10] [21] = right;
			
			return null;
		}).when (movementUtils).processOverlandMovementCell (eq (unitStack), eq (unitStackSkills), eq (new MapCoordinates3DEx (20, 10, 1)), eq (2), eq (4),
			eq (blockedLocations), eq (earthGates), eq (astralGates), eq (cellTransportCapacity), eq (doubleMovementRates), distancesCapture.capture (),
			cellsLeftToCheck.capture (), eq (sys), eq (mem), eq (db));
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setUnitCalculations (unitCalculations);
		utils.setMovementUtils (movementUtils);
		
		// Run method
		final OverlandMovementCell [] [] [] distances = utils.calculateOverlandMovementDistances (new MapCoordinates3DEx (20, 10, 1), 2, unitStack, 4, players, sys, mem, db);
		
		// Can stay where we are for free
		final OverlandMovementCell startCell = distances [1] [10] [20];
		assertNotNull (startCell);
		assertEquals (OverlandMovementType.START, startCell.getMovementType ());
		assertTrue (startCell.isMoveToInOneTurn ());
		assertEquals (0, startCell.getDirection ());
		assertEquals (0, startCell.getDoubleMovementDistance ());
		assertEquals (0, startCell.getDoubleMovementToEnterTile ());
		
		// Verify "cannot move here" got blanked out, but the other cell set in a similar fashion did not
		assertEquals (1, distances [1] [10] [19].getDoubleMovementDistance ());
		assertNull (distances [1] [10] [21]);
		
		// Verify the additional calls took place
		verify (movementUtils).processOverlandMovementCell (eq (unitStack), eq (unitStackSkills), eq (new MapCoordinates3DEx (19, 10, 1)), eq (2), eq (4),
			eq (blockedLocations), eq (earthGates), eq (astralGates), eq (cellTransportCapacity), eq (doubleMovementRates), any (OverlandMovementCell [] [] [].class),
			anyList (), eq (sys), eq (mem), eq (db));

		verify (movementUtils).processOverlandMovementCell (eq (unitStack), eq (unitStackSkills), eq (new MapCoordinates3DEx (21, 10, 1)), eq (2), eq (4),
			eq (blockedLocations), eq (earthGates), eq (astralGates), eq (cellTransportCapacity), eq (doubleMovementRates), any (OverlandMovementCell [] [] [].class),
			anyList (), eq (sys), eq (mem), eq (db));
		
		verifyNoMoreInteractions (movementUtils, db, unitCalculations);
	}
	
	/**
	 * Tests the considerPossibleCombatMove method when we've already proved that the cell is impassable
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_KnownImpassable () throws Exception
	{
		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE;
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		
		// Run method
		utils.considerPossibleCombatMove (null, new MapCoordinates2DEx (5, 10), 3, 4, 5, null, false, null, doubleMovementDistances, null, null, null, null, null, null, sys, null);
	}

	/**
	 * Tests the considerPossibleCombatMove method when we can move there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_Move () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile moveToTile = combatMap.getRow ().get (10).getCell ().get (5);
		
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED;
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		
		// Movement to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db)).thenReturn (1);
		
		// Positions of other units in combat
		final boolean [] [] ourUnits = new boolean [sys.getHeight ()] [sys.getWidth ()];
		final String [] [] enemyUnits = new String [sys.getHeight ()] [sys.getWidth ()];
		
		// Border we are trying to cross
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 4, 10, 3, db)).thenReturn (true);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 5, 10, 7, db)).thenReturn (true);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		
		utils.considerPossibleCombatMove (new MapCoordinates2DEx (4, 10), new MapCoordinates2DEx (5, 10), 3, 4, 5,
			unitBeingMoved, false, cellsLeftToCheck, doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, null, combatMap, sys, db);
		
		// Check results
		assertEquals (4 + 1, doubleMovementDistances [10] [5]);
		assertEquals (3, movementDirections [10] [5]);
		assertEquals (CombatMovementType.MOVE, movementTypes [10] [5]);

		assertEquals (1, cellsLeftToCheck.size ());
		assertEquals (new MapCoordinates2DEx (5, 10), cellsLeftToCheck.get (0));
		
		verifyNoMoreInteractions (movementUtils, db, unitBeingMoved, unitCalculations);
	}

	/**
	 * Tests the considerPossibleCombatMove method when we can't move there because one of our own units is already there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_Move_OurUnitInTheWay () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile moveToTile = combatMap.getRow ().get (10).getCell ().get (5);
		
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED;
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		
		// Movement to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db)).thenReturn (1);
		
		// Positions of other units in combat
		final boolean [] [] ourUnits = new boolean [sys.getHeight ()] [sys.getWidth ()];
		final String [] [] enemyUnits = new String [sys.getHeight ()] [sys.getWidth ()];
		
		ourUnits [10] [5] = true;
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		
		utils.considerPossibleCombatMove (new MapCoordinates2DEx (4, 10), new MapCoordinates2DEx (5, 10), 3, 4, 5,
			unitBeingMoved, false, cellsLeftToCheck, doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, null, combatMap, sys, db);
		
		// Check results
		assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE, doubleMovementDistances [10] [5]);

		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (movementUtils, db, unitBeingMoved);
	}

	/**
	 * Tests the considerPossibleCombatMove method when we can attack an enemy there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_CanAttackEnemy () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile moveToTile = combatMap.getRow ().get (10).getCell ().get (5);
		
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED;
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		
		// Movement to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db)).thenReturn (1);
		
		// Positions of other units in combat
		final boolean [] [] ourUnits = new boolean [sys.getHeight ()] [sys.getWidth ()];
		final String [] [] enemyUnits = new String [sys.getHeight ()] [sys.getWidth ()];
		
		enemyUnits [10] [5] = "X";
		
		// Border we are trying to cross
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 4, 10, 3, db)).thenReturn (true);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 5, 10, 7, db)).thenReturn (true);
		
		// Can attack
		when (unitCalculations.canMakeMeleeAttack ("X", unitBeingMoved, db)).thenReturn (true);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		
		utils.considerPossibleCombatMove (new MapCoordinates2DEx (4, 10), new MapCoordinates2DEx (5, 10), 3, 4, 5,
			unitBeingMoved, false, cellsLeftToCheck, doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, null, combatMap, sys, db);
		
		// Check results
		assertEquals (4 + 1, doubleMovementDistances [10] [5]);
		assertEquals (3, movementDirections [10] [5]);
		assertEquals (CombatMovementType.MELEE_UNIT, movementTypes [10] [5]);

		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (movementUtils, db, unitBeingMoved, unitCalculations);
	}

	/**
	 * Tests the considerPossibleCombatMove method when we can't attack an enemy there, e.g. they're flying, and we're a spearman
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_CantAttackEnemy () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile moveToTile = combatMap.getRow ().get (10).getCell ().get (5);
		
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED;
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		
		// Movement to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db)).thenReturn (1);
		
		// Positions of other units in combat
		final boolean [] [] ourUnits = new boolean [sys.getHeight ()] [sys.getWidth ()];
		final String [] [] enemyUnits = new String [sys.getHeight ()] [sys.getWidth ()];
		
		enemyUnits [10] [5] = "X";
		
		// Border we are trying to cross
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 4, 10, 3, db)).thenReturn (true);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 5, 10, 7, db)).thenReturn (true);
		
		// Can attack
		when (unitCalculations.canMakeMeleeAttack ("X", unitBeingMoved, db)).thenReturn (false);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		
		utils.considerPossibleCombatMove (new MapCoordinates2DEx (4, 10), new MapCoordinates2DEx (5, 10), 3, 4, 5,
			unitBeingMoved, false, cellsLeftToCheck, doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, null, combatMap, sys, db);
		
		// Check results
		assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE, doubleMovementDistances [10] [5]);

		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (movementUtils, db, unitBeingMoved, unitCalculations);
	}

	/**
	 * Tests the considerPossibleCombatMove method when we can attack the tile itself, e.g. engineers attacking walls
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_CanAttackTile () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile moveToTile = combatMap.getRow ().get (10).getCell ().get (5);
		
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED;
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		
		// Movement to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db)).thenReturn (1);
		
		// Positions of other units in combat
		final boolean [] [] ourUnits = new boolean [sys.getHeight ()] [sys.getWidth ()];
		final String [] [] enemyUnits = new String [sys.getHeight ()] [sys.getWidth ()];
		
		// We can't cross a wall
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 4, 10, 3, db)).thenReturn (false);
		
		// But we can attack it
		moveToTile.setBorderDirections ("1");
		moveToTile.getBorderID ().add ("CTB01");
		
		when (unitCalculations.canMakeMeleeAttack (null, unitBeingMoved, db)).thenReturn (true);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		
		utils.considerPossibleCombatMove (new MapCoordinates2DEx (4, 10), new MapCoordinates2DEx (5, 10), 3, 4, 5,
			unitBeingMoved, false, cellsLeftToCheck, doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, Arrays.asList ("CTB01"), combatMap, sys, db);
		
		// Check results
		assertEquals (4 + 1, doubleMovementDistances [10] [5]);
		assertEquals (3, movementDirections [10] [5]);
		assertEquals (CombatMovementType.MELEE_WALL, movementTypes [10] [5]);

		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (movementUtils, db, unitBeingMoved, unitCalculations);
	}

	/**
	 * Tests the considerPossibleCombatMove method when we can attack the tile itself AND the unit standing behind it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_CanAttackUnitAndTile () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile moveToTile = combatMap.getRow ().get (10).getCell ().get (5);
		
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED;
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		
		// Movement to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db)).thenReturn (1);
		
		// Positions of other units in combat
		final boolean [] [] ourUnits = new boolean [sys.getHeight ()] [sys.getWidth ()];
		final String [] [] enemyUnits = new String [sys.getHeight ()] [sys.getWidth ()];
		
		enemyUnits [10] [5] = "X";
		
		// To hit the unit behind the wall, we have to be able to cross the wall, i.e. we're flying
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 4, 10, 3, db)).thenReturn (true);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 5, 10, 7, db)).thenReturn (true);
		
		// But we can attack it
		moveToTile.setBorderDirections ("1");
		moveToTile.getBorderID ().add ("CTB01");
		
		when (unitCalculations.canMakeMeleeAttack ("X", unitBeingMoved, db)).thenReturn (true);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		
		utils.considerPossibleCombatMove (new MapCoordinates2DEx (4, 10), new MapCoordinates2DEx (5, 10), 3, 4, 5,
			unitBeingMoved, false, cellsLeftToCheck, doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, Arrays.asList ("CTB01"), combatMap, sys, db);
		
		// Check results
		assertEquals (4 + 1, doubleMovementDistances [10] [5]);
		assertEquals (3, movementDirections [10] [5]);
		assertEquals (CombatMovementType.MELEE_UNIT_AND_WALL, movementTypes [10] [5]);

		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (movementUtils, db, unitBeingMoved, unitCalculations);
	}

	/**
	 * Tests the considerPossibleCombatMove method when we can't move there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_Impassable () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile moveToTile = combatMap.getRow ().get (10).getCell ().get (5);
		
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED;
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		
		// Movement to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db)).thenReturn (-1);
		
		// Positions of other units in combat
		final boolean [] [] ourUnits = new boolean [sys.getHeight ()] [sys.getWidth ()];
		final String [] [] enemyUnits = new String [sys.getHeight ()] [sys.getWidth ()];
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		
		utils.considerPossibleCombatMove (new MapCoordinates2DEx (4, 10), new MapCoordinates2DEx (5, 10), 3, 4, 5,
			unitBeingMoved, false, cellsLeftToCheck, doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, null, combatMap, sys, db);
		
		// Check results
		assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_CANNOT_MOVE_HERE, doubleMovementDistances [10] [5]);

		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (movementUtils, db, unitBeingMoved);
	}

	/**
	 * Tests the considerPossibleCombatMove method when we can already proved we can get there via a shorter route than this one
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConsiderPossibleCombatMove_AlreadyShorterRouteFound () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile moveToTile = combatMap.getRow ().get (10).getCell ().get (5);
		
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		doubleMovementDistances [10] [5] = 3;

		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		movementDirections [10] [5] = 7;
		
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];
		movementTypes [10] [5] = CombatMovementType.TELEPORT;		// Put something weird here to prove it doesn't get overridden
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		
		// Movement to enter tile
		final MovementUtils movementUtils = mock (MovementUtils.class);
		when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db)).thenReturn (1);
		
		// Positions of other units in combat
		final boolean [] [] ourUnits = new boolean [sys.getHeight ()] [sys.getWidth ()];
		final String [] [] enemyUnits = new String [sys.getHeight ()] [sys.getWidth ()];
		
		// Border we are trying to cross
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 4, 10, 3, db)).thenReturn (true);
		when (unitCalculations.okToCrossCombatTileBorder (combatMap, CoordinateSystemType.DIAMOND, 5, 10, 7, db)).thenReturn (true);
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		
		utils.considerPossibleCombatMove (new MapCoordinates2DEx (4, 10), new MapCoordinates2DEx (5, 10), 3, 4, 5,
			unitBeingMoved, false, cellsLeftToCheck, doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, null, combatMap, sys, db);
		
		// Check results
		assertEquals (3, doubleMovementDistances [10] [5]);		// Same as what we started with
		assertEquals (7, movementDirections [10] [5]);
		assertEquals (CombatMovementType.TELEPORT, movementTypes [10] [5]);

		assertEquals (0, cellsLeftToCheck.size ());
		
		verifyNoMoreInteractions (movementUtils, db, unitBeingMoved, unitCalculations);
	}
	
	/**
	 * Tests the calculateCombatMovementDistances method on a unit that can only walk and make melee attacks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCombatMovementDistances_Melee () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN002"));

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		when (unitBeingMoved.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (unitBeingMoved.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 7));
		when (unitBeingMoved.unitIgnoresCombatTerrain (db)).thenReturn (false);
		when (unitBeingMoved.getUnitID ()).thenReturn ("UN001");
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WALL_CRUSHER)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TELEPORT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MERGING)).thenReturn (false);
		when (unitBeingMoved.getControllingPlayerID ()).thenReturn (1);
		when (unitBeingMoved.getMemoryUnit ()).thenReturn (new MemoryUnit ());
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.canMakeRangedAttack (unitBeingMoved)).thenReturn (false);
		
		final List<ExpandedUnitDetails> unitsBeingMoved = new ArrayList<ExpandedUnitDetails> ();
		unitsBeingMoved.add (unitBeingMoved);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Add 1 friendly and 1 enemy unit who are in the combat and visible
		// 1 of each who are visible to someone else on our side, but not to the unit moving
		// 1 of each who are not visible at all
		// 1 of each who aren't in the combat
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		final List<ExpandedUnitDetails> xus = new ArrayList<ExpandedUnitDetails> ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		for (int n = 0; n < 8; n++)
		{
			final MemoryUnit mu = new MemoryUnit ();
			mu.setCombatLocation (new MapCoordinates3DEx (20, 10, (n < 6) ? 1 : 0));
			mu.setStatus (UnitStatusID.ALIVE);
			mu.setCombatPosition (new MapCoordinates2DEx (n, 8));
			mu.setCombatSide (((n % 2) == 0) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			mu.setCombatHeading (n + 1);
			mem.getUnit ().add (mu);
			
			if (n < 6)
			{
				final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
				when (xu.getOwningPlayerID ()).thenReturn (((n % 2) == 0) ? 1 : 2);
				when (expandUnitDetails.expandUnitDetails (mu, unitsBeingMoved, null, null, players, mem, db)).thenReturn (xu);
				xus.add (xu);
				
				if ((n % 2) == 1)		// enemy unit
				{
					when (unitVisibilityUtils.canSeeUnitInCombat (xu, 1, players, mem, db, sys)).thenReturn ((n == 1) || (n == 3));
					
					if ((n == 1) || (n == 3))		// enemy unit we can see
						when (unitCalculations.determineCombatActionID (xu, false, db)).thenReturn ("X");
					
					if (n == 1)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
					else if (n == 3)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
				}
			}
		}
		
		// Have the initial call on the start tile add 2 more cells to check
		final MovementUtils movementUtils = mock (MovementUtils.class);
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];

		@SuppressWarnings ("unchecked")
		final ArgumentCaptor<List<MapCoordinates2DEx>> cellsLeftToCheck = ArgumentCaptor.forClass (List.class);
		final ArgumentCaptor<boolean [] []> ourUnits = ArgumentCaptor.forClass (boolean [] [].class);
		final ArgumentCaptor<String [] []> enemyUnits = ArgumentCaptor.forClass (String [] [].class);
		
		doAnswer ((i) ->
		{
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (3, 7));
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (5, 7));
			
			return null;
		}).when (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (4, 7)), eq (unitBeingMoved), eq (false), cellsLeftToCheck.capture (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), ourUnits.capture (), enemyUnits.capture (), isNull (), eq (combatMap), eq (sys), eq (db));
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		
		// Run method
		utils.calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unitBeingMoved, mem, combatMap, sys, players, db);

		// Check unit grids
		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
			{
				// Can stay where we are for free
				if ((x == 4) && (y == 7))
				{
					assertEquals (0, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 0");
					assertEquals (CombatMovementType.MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be MOVE");
				}
				else
				{
					assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be NOT_YET_CHECKED");
					assertEquals (CombatMovementType.CANNOT_MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be CANNOT_MOVE");
				}

				// Our unit placed above
				if (((x == 0) || (x == 2) || (x == 4)) && (y == 8))
					assertTrue (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be true");
				else
					assertFalse (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be false");
				
				// Enemy unit placed above
				if (((x == 1) || (x == 3)) && (y == 8))
					assertEquals ("X", enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be a combatActionID");
				else
					assertNull (enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be null");
			}
		
		// Verify the additional calls took place
		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (3, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));

		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (5, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));
		
		verifyNoMoreInteractions (db, unitBeingMoved, unitCalculations, movementUtils, expandUnitDetails, unitVisibilityUtils);
		
		int index = 0;
		for (final ExpandedUnitDetails xu : xus)
		{
			System.out.println ("Checking no more interactions on unit index " + index);
			verifyNoMoreInteractions (xu);
			index++;
		}
	}

	/**
	 * Tests the calculateCombatMovementDistances method on a unit that can make ranged attacks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCombatMovementDistances_Ranged () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN002"));

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		when (unitBeingMoved.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (unitBeingMoved.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 7));
		when (unitBeingMoved.unitIgnoresCombatTerrain (db)).thenReturn (false);
		when (unitBeingMoved.getUnitID ()).thenReturn ("UN001");
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WALL_CRUSHER)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TELEPORT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MERGING)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT_FROM_SPELL)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT_FROM_HERO_ITEM)).thenReturn (false);
		when (unitBeingMoved.getControllingPlayerID ()).thenReturn (1);
		when (unitBeingMoved.getMemoryUnit ()).thenReturn (new MemoryUnit ());
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.canMakeRangedAttack (unitBeingMoved)).thenReturn (true);
		
		final List<ExpandedUnitDetails> unitsBeingMoved = new ArrayList<ExpandedUnitDetails> ();
		unitsBeingMoved.add (unitBeingMoved);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Add 1 friendly and 1 enemy unit who are in the combat and visible
		// 1 of each who are visible to someone else on our side, but not to the unit moving
		// 1 of each who are not visible at all
		// 1 of each who aren't in the combat
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		final List<ExpandedUnitDetails> xus = new ArrayList<ExpandedUnitDetails> ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		for (int n = 0; n < 8; n++)
		{
			final MemoryUnit mu = new MemoryUnit ();
			mu.setCombatLocation (new MapCoordinates3DEx (20, 10, (n < 6) ? 1 : 0));
			mu.setStatus (UnitStatusID.ALIVE);
			mu.setCombatPosition (new MapCoordinates2DEx (n, 8));
			mu.setCombatSide (((n % 2) == 0) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			mu.setCombatHeading (n + 1);
			mem.getUnit ().add (mu);
			
			if (n < 6)
			{
				final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
				when (xu.getOwningPlayerID ()).thenReturn (((n % 2) == 0) ? 1 : 2);
				when (expandUnitDetails.expandUnitDetails (mu, unitsBeingMoved, null, null, players, mem, db)).thenReturn (xu);
				xus.add (xu);
				
				if ((n % 2) == 1)		// enemy unit
				{
					when (unitVisibilityUtils.canSeeUnitInCombat (xu, 1, players, mem, db, sys)).thenReturn ((n == 1) || (n == 3));
					
					if ((n == 1) || (n == 3))		// enemy unit we can see
						when (unitCalculations.determineCombatActionID (xu, false, db)).thenReturn ("X");
					
					if (n == 1)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
						when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (n, 8));
					}
					else if (n == 3)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
				}
			}
		}
		
		// Wall of darkness
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (4, 7), combatMap, mem.getMaintainedSpell (), db)).thenReturn (false);		// us
		when (combatMapUtils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (1, 8), combatMap, mem.getMaintainedSpell (), db)).thenReturn (false);		// enemy visible at range

		// Have the initial call on the start tile add 2 more cells to check
		final MovementUtils movementUtils = mock (MovementUtils.class);
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];

		@SuppressWarnings ("unchecked")
		final ArgumentCaptor<List<MapCoordinates2DEx>> cellsLeftToCheck = ArgumentCaptor.forClass (List.class);
		final ArgumentCaptor<boolean [] []> ourUnits = ArgumentCaptor.forClass (boolean [] [].class);
		final ArgumentCaptor<String [] []> enemyUnits = ArgumentCaptor.forClass (String [] [].class);
		
		doAnswer ((i) ->
		{
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (3, 7));
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (5, 7));
			
			return null;
		}).when (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (4, 7)), eq (unitBeingMoved), eq (false), cellsLeftToCheck.capture (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), ourUnits.capture (), enemyUnits.capture (), isNull (), eq (combatMap), eq (sys), eq (db));
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		utils.setCombatMapUtils (combatMapUtils);
		
		// Run method
		utils.calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unitBeingMoved, mem, combatMap, sys, players, db);

		// Check unit grids
		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
			{
				// Can stay where we are for free
				if ((x == 4) && (y == 7))
				{
					assertEquals (0, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 0");
					assertEquals (CombatMovementType.MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be MOVE");
				}
				else if ((x == 1) && (y == 8))	// The one enemy unit we can see clearly enough to hit it with a ranged attack
				{
					assertEquals (999, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 999");
					assertEquals (CombatMovementType.RANGED_UNIT, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be RANGED_UNIT");
				}
				else
				{
					assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be NOT_YET_CHECKED");
					assertEquals (CombatMovementType.CANNOT_MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be CANNOT_MOVE");
				}

				// Our unit placed above
				if (((x == 0) || (x == 2) || (x == 4)) && (y == 8))
					assertTrue (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be true");
				else
					assertFalse (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be false");
				
				// Enemy unit placed above
				if (((x == 1) || (x == 3)) && (y == 8))
					assertEquals ("X", enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be a combatActionID");
				else
					assertNull (enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be null");
			}
		
		// Verify the additional calls took place
		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (3, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));

		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (5, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));
		
		verifyNoMoreInteractions (db, unitBeingMoved, unitCalculations, movementUtils, expandUnitDetails, unitVisibilityUtils, combatMapUtils);
		
		int index = 0;
		for (final ExpandedUnitDetails xu : xus)
		{
			System.out.println ("Checking no more interactions on unit index " + index);
			verifyNoMoreInteractions (xu);
			index++;
		}
	}

	/**
	 * Tests the calculateCombatMovementDistances method on a unit that can make ranged attacks, but the enemy is inside a Wall of Darkness
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCombatMovementDistances_Ranged_WallOfDarkness () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN002"));

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		when (unitBeingMoved.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (unitBeingMoved.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 7));
		when (unitBeingMoved.unitIgnoresCombatTerrain (db)).thenReturn (false);
		when (unitBeingMoved.getUnitID ()).thenReturn ("UN001");
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WALL_CRUSHER)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TELEPORT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MERGING)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT_FROM_SPELL)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT_FROM_HERO_ITEM)).thenReturn (false);
		when (unitBeingMoved.getControllingPlayerID ()).thenReturn (1);
		when (unitBeingMoved.getMemoryUnit ()).thenReturn (new MemoryUnit ());
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.canMakeRangedAttack (unitBeingMoved)).thenReturn (true);
		
		final List<ExpandedUnitDetails> unitsBeingMoved = new ArrayList<ExpandedUnitDetails> ();
		unitsBeingMoved.add (unitBeingMoved);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Add 1 friendly and 1 enemy unit who are in the combat and visible
		// 1 of each who are visible to someone else on our side, but not to the unit moving
		// 1 of each who are not visible at all
		// 1 of each who aren't in the combat
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		final List<ExpandedUnitDetails> xus = new ArrayList<ExpandedUnitDetails> ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		for (int n = 0; n < 8; n++)
		{
			final MemoryUnit mu = new MemoryUnit ();
			mu.setCombatLocation (new MapCoordinates3DEx (20, 10, (n < 6) ? 1 : 0));
			mu.setStatus (UnitStatusID.ALIVE);
			mu.setCombatPosition (new MapCoordinates2DEx (n, 8));
			mu.setCombatSide (((n % 2) == 0) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			mu.setCombatHeading (n + 1);
			mem.getUnit ().add (mu);
			
			if (n < 6)
			{
				final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
				when (xu.getOwningPlayerID ()).thenReturn (((n % 2) == 0) ? 1 : 2);
				when (expandUnitDetails.expandUnitDetails (mu, unitsBeingMoved, null, null, players, mem, db)).thenReturn (xu);
				xus.add (xu);
				
				if ((n % 2) == 1)		// enemy unit
				{
					when (unitVisibilityUtils.canSeeUnitInCombat (xu, 1, players, mem, db, sys)).thenReturn ((n == 1) || (n == 3));
					
					if ((n == 1) || (n == 3))		// enemy unit we can see
						when (unitCalculations.determineCombatActionID (xu, false, db)).thenReturn ("X");
					
					if (n == 1)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
						when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (n, 8));
					}
					else if (n == 3)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
				}
			}
		}
		
		// Wall of darkness
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (4, 7), combatMap, mem.getMaintainedSpell (), db)).thenReturn (false);		// us
		when (combatMapUtils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (1, 8), combatMap, mem.getMaintainedSpell (), db)).thenReturn (true);		// enemy visible at range

		// Have the initial call on the start tile add 2 more cells to check
		final MovementUtils movementUtils = mock (MovementUtils.class);
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];

		@SuppressWarnings ("unchecked")
		final ArgumentCaptor<List<MapCoordinates2DEx>> cellsLeftToCheck = ArgumentCaptor.forClass (List.class);
		final ArgumentCaptor<boolean [] []> ourUnits = ArgumentCaptor.forClass (boolean [] [].class);
		final ArgumentCaptor<String [] []> enemyUnits = ArgumentCaptor.forClass (String [] [].class);
		
		doAnswer ((i) ->
		{
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (3, 7));
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (5, 7));
			
			return null;
		}).when (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (4, 7)), eq (unitBeingMoved), eq (false), cellsLeftToCheck.capture (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), ourUnits.capture (), enemyUnits.capture (), isNull (), eq (combatMap), eq (sys), eq (db));
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		utils.setCombatMapUtils (combatMapUtils);
		
		// Run method
		utils.calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unitBeingMoved, mem, combatMap, sys, players, db);

		// Check unit grids
		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
			{
				// Can stay where we are for free
				if ((x == 4) && (y == 7))
				{
					assertEquals (0, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 0");
					assertEquals (CombatMovementType.MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be MOVE");
				}
				else
				{
					assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be NOT_YET_CHECKED");
					assertEquals (CombatMovementType.CANNOT_MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be CANNOT_MOVE");
				}

				// Our unit placed above
				if (((x == 0) || (x == 2) || (x == 4)) && (y == 8))
					assertTrue (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be true");
				else
					assertFalse (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be false");
				
				// Enemy unit placed above
				if (((x == 1) || (x == 3)) && (y == 8))
					assertEquals ("X", enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be a combatActionID");
				else
					assertNull (enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be null");
			}
		
		// Verify the additional calls took place
		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (3, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));

		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (5, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));
		
		verifyNoMoreInteractions (db, unitBeingMoved, unitCalculations, movementUtils, expandUnitDetails, unitVisibilityUtils, combatMapUtils);
		
		int index = 0;
		for (final ExpandedUnitDetails xu : xus)
		{
			System.out.println ("Checking no more interactions on unit index " + index);
			verifyNoMoreInteractions (xu);
			index++;
		}
	}

	/**
	 * Tests the calculateCombatMovementDistances method on a unit that can make ranged attacks, the enemy is behind of Wall of Darkness, but we have True Sight
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCombatMovementDistances_Ranged_WallOfDarkness_TrueSight () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN002"));

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		when (unitBeingMoved.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (unitBeingMoved.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 7));
		when (unitBeingMoved.unitIgnoresCombatTerrain (db)).thenReturn (false);
		when (unitBeingMoved.getUnitID ()).thenReturn ("UN001");
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WALL_CRUSHER)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TELEPORT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MERGING)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT)).thenReturn (true);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT_FROM_SPELL)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT_FROM_HERO_ITEM)).thenReturn (false);
		when (unitBeingMoved.getControllingPlayerID ()).thenReturn (1);
		when (unitBeingMoved.getMemoryUnit ()).thenReturn (new MemoryUnit ());
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.canMakeRangedAttack (unitBeingMoved)).thenReturn (true);
		
		final List<ExpandedUnitDetails> unitsBeingMoved = new ArrayList<ExpandedUnitDetails> ();
		unitsBeingMoved.add (unitBeingMoved);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Add 1 friendly and 1 enemy unit who are in the combat and visible
		// 1 of each who are visible to someone else on our side, but not to the unit moving
		// 1 of each who are not visible at all
		// 1 of each who aren't in the combat
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		final List<ExpandedUnitDetails> xus = new ArrayList<ExpandedUnitDetails> ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		for (int n = 0; n < 8; n++)
		{
			final MemoryUnit mu = new MemoryUnit ();
			mu.setCombatLocation (new MapCoordinates3DEx (20, 10, (n < 6) ? 1 : 0));
			mu.setStatus (UnitStatusID.ALIVE);
			mu.setCombatPosition (new MapCoordinates2DEx (n, 8));
			mu.setCombatSide (((n % 2) == 0) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			mu.setCombatHeading (n + 1);
			mem.getUnit ().add (mu);
			
			if (n < 6)
			{
				final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
				when (xu.getOwningPlayerID ()).thenReturn (((n % 2) == 0) ? 1 : 2);
				when (expandUnitDetails.expandUnitDetails (mu, unitsBeingMoved, null, null, players, mem, db)).thenReturn (xu);
				xus.add (xu);
				
				if ((n % 2) == 1)		// enemy unit
				{
					when (unitVisibilityUtils.canSeeUnitInCombat (xu, 1, players, mem, db, sys)).thenReturn ((n == 1) || (n == 3));
					
					if ((n == 1) || (n == 3))		// enemy unit we can see
						when (unitCalculations.determineCombatActionID (xu, false, db)).thenReturn ("X");
					
					if (n == 1)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
						when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (n, 8));
					}
					else if (n == 3)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
				}
			}
		}
		
		// Wall of darkness
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (4, 7), combatMap, mem.getMaintainedSpell (), db)).thenReturn (false);		// us

		// Have the initial call on the start tile add 2 more cells to check
		final MovementUtils movementUtils = mock (MovementUtils.class);
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];

		@SuppressWarnings ("unchecked")
		final ArgumentCaptor<List<MapCoordinates2DEx>> cellsLeftToCheck = ArgumentCaptor.forClass (List.class);
		final ArgumentCaptor<boolean [] []> ourUnits = ArgumentCaptor.forClass (boolean [] [].class);
		final ArgumentCaptor<String [] []> enemyUnits = ArgumentCaptor.forClass (String [] [].class);
		
		doAnswer ((i) ->
		{
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (3, 7));
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (5, 7));
			
			return null;
		}).when (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (4, 7)), eq (unitBeingMoved), eq (false), cellsLeftToCheck.capture (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), ourUnits.capture (), enemyUnits.capture (), isNull (), eq (combatMap), eq (sys), eq (db));
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		utils.setCombatMapUtils (combatMapUtils);
		
		// Run method
		utils.calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unitBeingMoved, mem, combatMap, sys, players, db);

		// Check unit grids
		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
			{
				// Can stay where we are for free
				if ((x == 4) && (y == 7))
				{
					assertEquals (0, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 0");
					assertEquals (CombatMovementType.MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be MOVE");
				}
				else if ((x == 1) && (y == 8))	// The one enemy unit we can see clearly enough to hit it with a ranged attack
				{
					assertEquals (999, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 999");
					assertEquals (CombatMovementType.RANGED_UNIT, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be RANGED_UNIT");
				}
				else
				{
					assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be NOT_YET_CHECKED");
					assertEquals (CombatMovementType.CANNOT_MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be CANNOT_MOVE");
				}

				// Our unit placed above
				if (((x == 0) || (x == 2) || (x == 4)) && (y == 8))
					assertTrue (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be true");
				else
					assertFalse (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be false");
				
				// Enemy unit placed above
				if (((x == 1) || (x == 3)) && (y == 8))
					assertEquals ("X", enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be a combatActionID");
				else
					assertNull (enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be null");
			}
		
		// Verify the additional calls took place
		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (3, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));

		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (5, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));
		
		verifyNoMoreInteractions (db, unitBeingMoved, unitCalculations, movementUtils, expandUnitDetails, unitVisibilityUtils, combatMapUtils);
		
		int index = 0;
		for (final ExpandedUnitDetails xu : xus)
		{
			System.out.println ("Checking no more interactions on unit index " + index);
			verifyNoMoreInteractions (xu);
			index++;
		}
	}

	/**
	 * Tests the calculateCombatMovementDistances method on a unit that can melee attack walls (engineers) 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCombatMovementDistances_Melee_AttackWalls () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN002"));
		
		final Spell disruptWall = new Spell ();
		disruptWall.getSpellValidBorderTarget ().add ("CTB01");
		when (db.findSpell (CommonDatabaseConstants.SPELL_ID_DISRUPT_WALL, "calculateCombatMovementDistances")).thenReturn (disruptWall);
		
		final List<String> borderTargetIDs = Arrays.asList ("CTB01");

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		when (unitBeingMoved.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (unitBeingMoved.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 7));
		when (unitBeingMoved.unitIgnoresCombatTerrain (db)).thenReturn (false);
		when (unitBeingMoved.getUnitID ()).thenReturn ("UN001");
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WALL_CRUSHER)).thenReturn (true);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TELEPORT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MERGING)).thenReturn (false);
		when (unitBeingMoved.getControllingPlayerID ()).thenReturn (1);
		when (unitBeingMoved.getMemoryUnit ()).thenReturn (new MemoryUnit ());
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.canMakeRangedAttack (unitBeingMoved)).thenReturn (false);
		
		final List<ExpandedUnitDetails> unitsBeingMoved = new ArrayList<ExpandedUnitDetails> ();
		unitsBeingMoved.add (unitBeingMoved);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Add 1 friendly and 1 enemy unit who are in the combat and visible
		// 1 of each who are visible to someone else on our side, but not to the unit moving
		// 1 of each who are not visible at all
		// 1 of each who aren't in the combat
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		final List<ExpandedUnitDetails> xus = new ArrayList<ExpandedUnitDetails> ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		for (int n = 0; n < 8; n++)
		{
			final MemoryUnit mu = new MemoryUnit ();
			mu.setCombatLocation (new MapCoordinates3DEx (20, 10, (n < 6) ? 1 : 0));
			mu.setStatus (UnitStatusID.ALIVE);
			mu.setCombatPosition (new MapCoordinates2DEx (n, 8));
			mu.setCombatSide (((n % 2) == 0) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			mu.setCombatHeading (n + 1);
			mem.getUnit ().add (mu);
			
			if (n < 6)
			{
				final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
				when (xu.getOwningPlayerID ()).thenReturn (((n % 2) == 0) ? 1 : 2);
				when (expandUnitDetails.expandUnitDetails (mu, unitsBeingMoved, null, null, players, mem, db)).thenReturn (xu);
				xus.add (xu);
				
				if ((n % 2) == 1)		// enemy unit
				{
					when (unitVisibilityUtils.canSeeUnitInCombat (xu, 1, players, mem, db, sys)).thenReturn ((n == 1) || (n == 3));
					
					if ((n == 1) || (n == 3))		// enemy unit we can see
						when (unitCalculations.determineCombatActionID (xu, false, db)).thenReturn ("X");
					
					if (n == 1)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
					else if (n == 3)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
				}
			}
		}
		
		// Can only destroy city walls from the outside
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.isWithinCityWalls (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (4, 7), combatMap, mem.getBuilding (), db)).thenReturn (false);		// us
		
		// Have the initial call on the start tile add 2 more cells to check
		final MovementUtils movementUtils = mock (MovementUtils.class);
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];

		@SuppressWarnings ("unchecked")
		final ArgumentCaptor<List<MapCoordinates2DEx>> cellsLeftToCheck = ArgumentCaptor.forClass (List.class);
		final ArgumentCaptor<boolean [] []> ourUnits = ArgumentCaptor.forClass (boolean [] [].class);
		final ArgumentCaptor<String [] []> enemyUnits = ArgumentCaptor.forClass (String [] [].class);
		
		doAnswer ((i) ->
		{
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (3, 7));
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (5, 7));
			
			return null;
		}).when (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (4, 7)), eq (unitBeingMoved), eq (false), cellsLeftToCheck.capture (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), ourUnits.capture (), enemyUnits.capture (), eq (borderTargetIDs), eq (combatMap), eq (sys), eq (db));
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		utils.setCombatMapUtils (combatMapUtils);
		
		// Run method
		utils.calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unitBeingMoved, mem, combatMap, sys, players, db);

		// Check unit grids
		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
			{
				// Can stay where we are for free
				if ((x == 4) && (y == 7))
				{
					assertEquals (0, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 0");
					assertEquals (CombatMovementType.MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be MOVE");
				}
				else
				{
					assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be NOT_YET_CHECKED");
					assertEquals (CombatMovementType.CANNOT_MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be CANNOT_MOVE");
				}

				// Our unit placed above
				if (((x == 0) || (x == 2) || (x == 4)) && (y == 8))
					assertTrue (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be true");
				else
					assertFalse (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be false");
				
				// Enemy unit placed above
				if (((x == 1) || (x == 3)) && (y == 8))
					assertEquals ("X", enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be a combatActionID");
				else
					assertNull (enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be null");
			}
		
		// Verify the additional calls took place
		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (3, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), eq (borderTargetIDs), eq (combatMap), eq (sys), eq (db));

		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (5, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), eq (borderTargetIDs), eq (combatMap), eq (sys), eq (db));
		
		verifyNoMoreInteractions (db, unitBeingMoved, unitCalculations, movementUtils, expandUnitDetails, unitVisibilityUtils);
		
		int index = 0;
		for (final ExpandedUnitDetails xu : xus)
		{
			System.out.println ("Checking no more interactions on unit index " + index);
			verifyNoMoreInteractions (xu);
			index++;
		}
	}

	/**
	 * Tests the calculateCombatMovementDistances method on a unit that can make ranged attacks against walls (catapults)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCombatMovementDistances_Ranged_AttackWalls () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN002"));

		final Spell disruptWall = new Spell ();
		disruptWall.getSpellValidBorderTarget ().add ("CTB01");
		when (db.findSpell (CommonDatabaseConstants.SPELL_ID_DISRUPT_WALL, "calculateCombatMovementDistances")).thenReturn (disruptWall);
		
		final List<String> borderTargetIDs = Arrays.asList ("CTB01");
		
		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		when (unitBeingMoved.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (unitBeingMoved.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 7));
		when (unitBeingMoved.unitIgnoresCombatTerrain (db)).thenReturn (false);
		when (unitBeingMoved.getUnitID ()).thenReturn ("UN001");
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WALL_CRUSHER)).thenReturn (true);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TELEPORT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MERGING)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT_FROM_SPELL)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TRUE_SIGHT_FROM_HERO_ITEM)).thenReturn (false);
		when (unitBeingMoved.getControllingPlayerID ()).thenReturn (1);
		when (unitBeingMoved.getMemoryUnit ()).thenReturn (new MemoryUnit ());
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.canMakeRangedAttack (unitBeingMoved)).thenReturn (true);
		
		final List<ExpandedUnitDetails> unitsBeingMoved = new ArrayList<ExpandedUnitDetails> ();
		unitsBeingMoved.add (unitBeingMoved);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Add 1 friendly and 1 enemy unit who are in the combat and visible
		// 1 of each who are visible to someone else on our side, but not to the unit moving
		// 1 of each who are not visible at all
		// 1 of each who aren't in the combat
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		final List<ExpandedUnitDetails> xus = new ArrayList<ExpandedUnitDetails> ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		for (int n = 0; n < 8; n++)
		{
			final MemoryUnit mu = new MemoryUnit ();
			mu.setCombatLocation (new MapCoordinates3DEx (20, 10, (n < 6) ? 1 : 0));
			mu.setStatus (UnitStatusID.ALIVE);
			mu.setCombatPosition (new MapCoordinates2DEx (n, 8));
			mu.setCombatSide (((n % 2) == 0) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			mu.setCombatHeading (n + 1);
			mem.getUnit ().add (mu);
			
			if (n < 6)
			{
				final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
				when (xu.getOwningPlayerID ()).thenReturn (((n % 2) == 0) ? 1 : 2);
				when (expandUnitDetails.expandUnitDetails (mu, unitsBeingMoved, null, null, players, mem, db)).thenReturn (xu);
				xus.add (xu);
				
				if ((n % 2) == 1)		// enemy unit
				{
					when (unitVisibilityUtils.canSeeUnitInCombat (xu, 1, players, mem, db, sys)).thenReturn ((n == 1) || (n == 3));
					
					if ((n == 1) || (n == 3))		// enemy unit we can see
						when (unitCalculations.determineCombatActionID (xu, false, db)).thenReturn ("X");
					
					if (n == 1)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
						when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (n, 8));
					}
					else if (n == 3)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
				}
			}
		}
		
		// Wall of darkness
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (4, 7), combatMap, mem.getMaintainedSpell (), db)).thenReturn (false);		// us
		when (combatMapUtils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (1, 8), combatMap, mem.getMaintainedSpell (), db)).thenReturn (false);		// enemy visible at range
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.findMaintainedSpell
			(mem.getMaintainedSpell (), null, CommonDatabaseConstants.SPELL_ID_WALL_OF_DARKNESS, null, null, new MapCoordinates3DEx (20, 10, 1), null)).thenReturn (null);

		// Can only destroy city walls from the outside
		when (combatMapUtils.isWithinCityWalls (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (4, 7), combatMap, mem.getBuilding (), db)).thenReturn (false);		// us
		
		// Destroyable wall segment
		final MomCombatTile wallSegment = combatMap.getRow ().get (3).getCell ().get (2);
		wallSegment.setBorderDirections ("1");
		wallSegment.getBorderID ().add ("CTB01");
		
		// Have the initial call on the start tile add 2 more cells to check
		final MovementUtils movementUtils = mock (MovementUtils.class);
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];

		@SuppressWarnings ("unchecked")
		final ArgumentCaptor<List<MapCoordinates2DEx>> cellsLeftToCheck = ArgumentCaptor.forClass (List.class);
		final ArgumentCaptor<boolean [] []> ourUnits = ArgumentCaptor.forClass (boolean [] [].class);
		final ArgumentCaptor<String [] []> enemyUnits = ArgumentCaptor.forClass (String [] [].class);
		
		doAnswer ((i) ->
		{
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (3, 7));
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (5, 7));
			
			return null;
		}).when (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (4, 7)), eq (unitBeingMoved), eq (false), cellsLeftToCheck.capture (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), ourUnits.capture (), enemyUnits.capture (), eq (borderTargetIDs), eq (combatMap), eq (sys), eq (db));
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		utils.setCombatMapUtils (combatMapUtils);
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Run method
		utils.calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unitBeingMoved, mem, combatMap, sys, players, db);

		// Check unit grids
		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
			{
				// Can stay where we are for free
				if ((x == 4) && (y == 7))
				{
					assertEquals (0, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 0");
					assertEquals (CombatMovementType.MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be MOVE");
				}
				else if ((x == 1) && (y == 8))	// The one enemy unit we can see clearly enough to hit it with a ranged attack
				{
					assertEquals (999, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 999");
					assertEquals (CombatMovementType.RANGED_UNIT, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be RANGED_UNIT");
				}
				else if ((x == 2) && (y == 3))	// Range attack wall segment
				{
					assertEquals (999, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 999");
					assertEquals (CombatMovementType.RANGED_WALL, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be RANGED_WALL");
				}
				else
				{
					assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be NOT_YET_CHECKED");
					assertEquals (CombatMovementType.CANNOT_MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be CANNOT_MOVE");
				}

				// Our unit placed above
				if (((x == 0) || (x == 2) || (x == 4)) && (y == 8))
					assertTrue (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be true");
				else
					assertFalse (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be false");
				
				// Enemy unit placed above
				if (((x == 1) || (x == 3)) && (y == 8))
					assertEquals ("X", enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be a combatActionID");
				else
					assertNull (enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be null");
			}
		
		// Verify the additional calls took place
		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (3, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), eq (borderTargetIDs), eq (combatMap), eq (sys), eq (db));

		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (5, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), eq (borderTargetIDs), eq (combatMap), eq (sys), eq (db));
		
		verifyNoMoreInteractions (db, unitBeingMoved, unitCalculations, movementUtils, expandUnitDetails, unitVisibilityUtils, combatMapUtils, memoryMaintainedSpellUtils);
		
		int index = 0;
		for (final ExpandedUnitDetails xu : xus)
		{
			System.out.println ("Checking no more interactions on unit index " + index);
			verifyNoMoreInteractions (xu);
			index++;
		}
	}
	
	/**
	 * Tests the calculateCombatMovementDistances method on a unit that can teleport everywhere
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCombatMovementDistances_Teleport () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN002"));

		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		
		// Unit being moved
		final ExpandedUnitDetails unitBeingMoved = mock (ExpandedUnitDetails.class);
		when (unitBeingMoved.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (unitBeingMoved.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 7));
		when (unitBeingMoved.unitIgnoresCombatTerrain (db)).thenReturn (false);
		when (unitBeingMoved.getUnitID ()).thenReturn ("UN001");
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WALL_CRUSHER)).thenReturn (false);
		when (unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TELEPORT)).thenReturn (true);
		when (unitBeingMoved.getControllingPlayerID ()).thenReturn (1);
		when (unitBeingMoved.getMemoryUnit ()).thenReturn (new MemoryUnit ());
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.canMakeRangedAttack (unitBeingMoved)).thenReturn (false);
		
		final List<ExpandedUnitDetails> unitsBeingMoved = new ArrayList<ExpandedUnitDetails> ();
		unitsBeingMoved.add (unitBeingMoved);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Add 1 friendly and 1 enemy unit who are in the combat and visible
		// 1 of each who are visible to someone else on our side, but not to the unit moving
		// 1 of each who are not visible at all
		// 1 of each who aren't in the combat
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		final List<ExpandedUnitDetails> xus = new ArrayList<ExpandedUnitDetails> ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		for (int n = 0; n < 8; n++)
		{
			final MemoryUnit mu = new MemoryUnit ();
			mu.setCombatLocation (new MapCoordinates3DEx (20, 10, (n < 6) ? 1 : 0));
			mu.setStatus (UnitStatusID.ALIVE);
			mu.setCombatPosition (new MapCoordinates2DEx (n, 8));
			mu.setCombatSide (((n % 2) == 0) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			mu.setCombatHeading (n + 1);
			mem.getUnit ().add (mu);
			
			if (n < 6)
			{
				final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
				when (xu.getOwningPlayerID ()).thenReturn (((n % 2) == 0) ? 1 : 2);
				when (expandUnitDetails.expandUnitDetails (mu, unitsBeingMoved, null, null, players, mem, db)).thenReturn (xu);
				xus.add (xu);
				
				if ((n % 2) == 1)		// enemy unit
				{
					when (unitVisibilityUtils.canSeeUnitInCombat (xu, 1, players, mem, db, sys)).thenReturn ((n == 1) || (n == 3));
					
					if ((n == 1) || (n == 3))		// enemy unit we can see
						when (unitCalculations.determineCombatActionID (xu, false, db)).thenReturn ("X");
					
					if (n == 1)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
					else if (n == 3)
					{
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
						when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
					}
				}
			}
		}
		
		// Passable terrain we can teleport onto
		final MovementUtils movementUtils = mock (MovementUtils.class);
		
		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
				if ((x != 4) || (y != 7))	// Everywhere except our start cell
					when (movementUtils.calculateDoubleMovementToEnterCombatTile (unitBeingMoved, combatMap.getRow ().get (y).getCell ().get (x), db)).thenReturn (2);
		
		// Have the initial call on the start tile add 2 more cells to check
		final int [] [] doubleMovementDistances = new int [sys.getHeight ()] [sys.getWidth ()];
		final int [] [] movementDirections = new int [sys.getHeight ()] [sys.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [sys.getHeight ()] [sys.getWidth ()];

		@SuppressWarnings ("unchecked")
		final ArgumentCaptor<List<MapCoordinates2DEx>> cellsLeftToCheck = ArgumentCaptor.forClass (List.class);
		final ArgumentCaptor<boolean [] []> ourUnits = ArgumentCaptor.forClass (boolean [] [].class);
		final ArgumentCaptor<String [] []> enemyUnits = ArgumentCaptor.forClass (String [] [].class);
		
		doAnswer ((i) ->
		{
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (3, 7));
			cellsLeftToCheck.getValue ().add (new MapCoordinates2DEx (5, 7));
			
			return null;
		}).when (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (4, 7)), eq (unitBeingMoved), eq (false), cellsLeftToCheck.capture (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), ourUnits.capture (), enemyUnits.capture (), isNull (), eq (combatMap), eq (sys), eq (db));
		
		// Set up object to test
		final UnitMovementImpl utils = new UnitMovementImpl ();
		utils.setMovementUtils (movementUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		
		// Run method
		utils.calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unitBeingMoved, mem, combatMap, sys, players, db);

		// Check unit grids
		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
			{
				// Can stay where we are for free
				if ((x == 4) && (y == 7))
				{
					assertEquals (0, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be 0");
					assertEquals (CombatMovementType.MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be MOVE");
				}
				
				// We can't teleport onto enemy units we can see, or our own units
				else if ((x <= 4) && (y == 8))
				{
					assertEquals (UnitMovementImpl.MOVEMENT_DISTANCE_NOT_YET_CHECKED, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be NOT_YET_CHECKED");
					assertEquals (CombatMovementType.CANNOT_MOVE, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be CANNOT_MOVE");
				}
				else
				{
					assertEquals (2, doubleMovementDistances [y] [x], "doubleMovementDistances (" + x + ", " + y + ") expected to be NOT_YET_CHECKED");
					assertEquals (CombatMovementType.TELEPORT, movementTypes [y] [x], "movementTypes (" + x + ", " + y + ") expected to be CANNOT_MOVE");
				}

				// Our unit placed above
				if (((x == 0) || (x == 2) || (x == 4)) && (y == 8))
					assertTrue (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be true");
				else
					assertFalse (ourUnits.getValue () [y] [x], "ourUnits (" + x + ", " + y + ") expected to be false");
				
				// Enemy unit placed above
				if (((x == 1) || (x == 3)) && (y == 8))
					assertEquals ("X", enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be a combatActionID");
				else
					assertNull (enemyUnits.getValue () [y] [x], "enemyUnits (" + x + ", " + y + ") expected to be null");
			}
		
		// Verify the additional calls took place
		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (3, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));

		verify (movementUtils).processCombatMovementCell (eq (new MapCoordinates2DEx (5, 7)), eq (unitBeingMoved), eq (false), anyList (),
			eq (doubleMovementDistances), eq (movementDirections), eq (movementTypes), eq (ourUnits.getValue ()), eq (enemyUnits.getValue ()), isNull (), eq (combatMap), eq (sys), eq (db));
		
		verifyNoMoreInteractions (db, unitBeingMoved, unitCalculations, movementUtils, expandUnitDetails, unitVisibilityUtils);
		
		int index = 0;
		for (final ExpandedUnitDetails xu : xus)
		{
			System.out.println ("Checking no more interactions on unit index " + index);
			verifyNoMoreInteractions (xu);
			index++;
		}
	}
}