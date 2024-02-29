package momime.common.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileBorderBlocksMovementID;
import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabase;
import momime.common.database.GenerateTestData;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.CombatMapUtilsImpl;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;

/**
 * Tests the MovementUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestMovementUtilsImpl 
{
	/**
	 * Tests the calculateCellTransportCapacity method for a move when transports are carrying the units
	 * @throws Exception If there is a problem
	 */
	@Test	
	public final void testCalculateCellTransportCapacity_Transported () throws Exception
	{
		// Unit stack has at least one transport in it
		// Note createUnitStack will not mark ANY unit as a transport unless ALL the units fit in transports
		final UnitStack unitStack = new UnitStack ();
		unitStack.getTransports ().add (null);
		
		// Set up object to test
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		
		// Call method
		final int [] [] [] cellTransportCapacity = utils.calculateCellTransportCapacity (unitStack, null, 2, null, null, null, null);
		
		// Check results
		assertNull (cellTransportCapacity);
	}

	/**
	 * Tests the calculateCellTransportCapacity method for a move when units may potentially get inside transports
	 * @throws Exception If there is a problem
	 */
	@Test	
	public final void testCalculateCellTransportCapacity_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Coordinate system
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Terrain
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		
		final FogOfWarMemory map = new FogOfWarMemory ();
		map.setMap (GenerateTestData.createOverlandMap (sys));
		
		for (int x = 0; x < 6; x++)
			map.getMap ().getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (new OverlandMapTerrainData ());
		
		when (memoryGridCellUtils.convertNullTileTypeToFOW (any (OverlandMapTerrainData.class), eq (false))).thenReturn ("TT01");

		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> (); 
		
		// Units
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final Set<String> unitStackSkills = new HashSet<String> ();
		
		final UnitEx unitDef = new UnitEx ();
		
		final UnitEx transportDef = new UnitEx ();
		transportDef.setTransportCapacity (2);
		
		// At 0, 0, 1 there's a regular unit (it shouldn't really be here, since its standing on impassable terrain with no transport holding it)
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setStatus (UnitStatusID.ALIVE);
		unit1.setOwningPlayerID (2);
		unit1.setUnitLocation (new MapCoordinates3DEx (1, 0, 0));
		map.getUnit ().add (unit1);
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (xu1.getUnitDefinition ()).thenReturn (unitDef);
		when (expand.expandUnitDetails (unit1, null, null, null, players, map, db)).thenReturn (xu1);
		when (unitCalculations.isTileTypeImpassable (xu1, unitStackSkills, "TT01", db)).thenReturn (true);
		
		// At 0, 0, 2 there's a transport with capacity 2
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setOwningPlayerID (2);
		unit2.setUnitLocation (new MapCoordinates3DEx (2, 0, 0));
		map.getUnit ().add (unit2);
		
		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (xu2.getUnitDefinition ()).thenReturn (transportDef);
		when (expand.expandUnitDetails (unit2, null, null, null, players, map, db)).thenReturn (xu2);

		// At 0, 0, 3 there's a transport with capacity 2 with 1 unit already inside it (terrain is impassable to that unit)
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setOwningPlayerID (2);
		unit3.setUnitLocation (new MapCoordinates3DEx (3, 0, 0));
		map.getUnit ().add (unit3);
		
		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (xu3.getUnitDefinition ()).thenReturn (transportDef);
		when (expand.expandUnitDetails (unit3, null, null, null, players, map, db)).thenReturn (xu3);

		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setOwningPlayerID (2);
		unit4.setUnitLocation (new MapCoordinates3DEx (3, 0, 0));
		map.getUnit ().add (unit4);
		
		final ExpandedUnitDetails xu4 = mock (ExpandedUnitDetails.class);
		when (xu4.getUnitDefinition ()).thenReturn (unitDef);
		when (expand.expandUnitDetails (unit4, null, null, null, players, map, db)).thenReturn (xu4);
		when (unitCalculations.isTileTypeImpassable (xu4, unitStackSkills, "TT01", db)).thenReturn (true);
		
		// At 0, 0, 4 there's a transport with capacity 2 with 1 unit standing next to it but not inside it (terrain is passable to that unit)
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setOwningPlayerID (2);
		unit5.setUnitLocation (new MapCoordinates3DEx (4, 0, 0));
		map.getUnit ().add (unit5);
		
		final ExpandedUnitDetails xu5 = mock (ExpandedUnitDetails.class);
		when (xu5.getUnitDefinition ()).thenReturn (transportDef);
		when (expand.expandUnitDetails (unit5, null, null, null, players, map, db)).thenReturn (xu5);

		final MemoryUnit unit6 = new MemoryUnit ();
		unit6.setStatus (UnitStatusID.ALIVE);
		unit6.setOwningPlayerID (2);
		unit6.setUnitLocation (new MapCoordinates3DEx (4, 0, 0));
		map.getUnit ().add (unit6);
		
		final ExpandedUnitDetails xu6 = mock (ExpandedUnitDetails.class);
		when (xu6.getUnitDefinition ()).thenReturn (unitDef);
		when (expand.expandUnitDetails (unit6, null, null, null, players, map, db)).thenReturn (xu6);
		when (unitCalculations.isTileTypeImpassable (xu6, unitStackSkills, "TT01", db)).thenReturn (false);
		
		// At 0, 0, 5 there's somebody else's unit which makes no difference, we can "move onto it" to attack it
		final MemoryUnit unit7 = new MemoryUnit ();
		unit7.setStatus (UnitStatusID.ALIVE);
		unit7.setOwningPlayerID (1);
		unit7.setUnitLocation (new MapCoordinates3DEx (5, 0, 0));
		map.getUnit ().add (unit7);
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		
		// Set up object to test
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		utils.setMemoryGridCellUtils (memoryGridCellUtils);
		utils.setExpandUnitDetails (expand);
		utils.setUnitCalculations (unitCalculations);
		
		// Call method
		final int [] [] [] cellTransportCapacity = utils.calculateCellTransportCapacity (unitStack, unitStackSkills, 2, map, players, sys, db);
		
		// Check results
		assertNotNull (cellTransportCapacity);
		assertEquals (0, cellTransportCapacity [0] [0] [0]);
		assertEquals (-1, cellTransportCapacity [0] [0] [1]);
		assertEquals (2, cellTransportCapacity [0] [0] [2]);
		assertEquals (1, cellTransportCapacity [0] [0] [3]);
		assertEquals (2, cellTransportCapacity [0] [0] [4]);
		assertEquals (0, cellTransportCapacity [0] [0] [5]);
	}

	/**
	 * Tests the calculateDoubleMovementRatesForUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementRatesForUnitStack () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);		
		
		// All possible tile types
		final List<TileTypeEx> tileTypes = new ArrayList<TileTypeEx> ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeEx thisTileType = new TileTypeEx ();
			thisTileType.setTileTypeID ("TT0" + n);
			tileTypes.add (thisTileType);
		}
		
		doReturn (tileTypes).when (db).getTileTypes ();

		// Set up object to test
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		utils.setExpandUnitDetails (expand);
		utils.setUnitCalculations (unitCalc);
		
		// Single unit
		final List<ExpandedUnitDetails> units = new ArrayList<ExpandedUnitDetails> ();
		
		final ExpandedUnitDetails spearmenUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), eq (units), anySet (), eq ("TT01"), eq (db))).thenReturn (4);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), eq (units), anySet (), eq ("TT02"), eq (db))).thenReturn (6);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), eq (units), anySet (), eq ("TT03"), eq (db))).thenReturn (null);
		
		units.add (spearmenUnit);

		final Map<String, Integer> spearmen = utils.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, spearmen.size ());
		assertEquals (4, spearmen.get ("TT01").intValue ());
		assertEquals (6, spearmen.get ("TT02").intValue ());
		assertNull (spearmen.get ("TT03"));
		
		// Stacking a faster unit with it makes no difference - it always chooses the slowest movement rate
		final ExpandedUnitDetails flyingUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (flyingUnit), eq (units), anySet (), any (String.class), eq (db))).thenReturn (2);
		
		units.add (flyingUnit);
		
		final Map<String, Integer> flying = utils.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, flying.size ());
		assertEquals (4, flying.get ("TT01").intValue ());
		assertEquals (6, flying.get ("TT02").intValue ());
		assertNull (flying.get ("TT03"));
		
		// Stack a slower unit
		final ExpandedUnitDetails pathfindingUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (pathfindingUnit), eq (units), anySet (), any (String.class), eq (db))).thenReturn (5);
		
		units.add (pathfindingUnit);
		
		final Map<String, Integer> pathfinding = utils.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, pathfinding.size ());
		assertEquals (5, pathfinding.get ("TT01").intValue ());
		assertEquals (6, pathfinding.get ("TT02").intValue ());
		assertNull (pathfinding.get ("TT03"));
	}

	/**
	 * Tests the countOurAliveUnitsAtEveryLocation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCountOurAliveUnitsAtEveryLocation () throws Exception
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();

		// Null location
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setOwningPlayerID (2);
		u1.setStatus (UnitStatusID.ALIVE);
		units.add (u1);

		// 3 at first location
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit u2 = new MemoryUnit ();
			u2.setOwningPlayerID (2);
			u2.setStatus (UnitStatusID.ALIVE);
			u2.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			units.add (u2);
		}

		// 4 at second location
		for (int n = 0; n < 4; n++)
		{
			final MemoryUnit u2 = new MemoryUnit ();
			u2.setOwningPlayerID (2);
			u2.setStatus (UnitStatusID.ALIVE);
			u2.setUnitLocation (new MapCoordinates3DEx (30, 20, 1));
			units.add (u2);
		}

		// Wrong player
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (3);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		units.add (u2);

		// Null status
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (2);
		u3.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		units.add (u3);

		// Unit is dead
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (2);
		u4.setStatus (UnitStatusID.DEAD);
		u4.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		units.add (u4);

		// Set up object to test
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		
		// Run test
		final int [] [] [] counts = utils.countOurAliveUnitsAtEveryLocation (2, units, sys);

		assertEquals (3, counts [0] [10] [20]);
		assertEquals (4, counts [1] [20] [30]);

		// Reset both the locations we already checked to 0, easier to check the whole array then
		counts [0] [10] [20] = 0;
		counts [1] [20] [30] = 0;
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
					assertEquals (0, counts [z] [y] [x]);
	}
	
	/**
	 * Tests the processOverlandMovementCell method in the simplest situation where a unit is not at a tower, earth gate or astral gate
	 */
	@Test
	public final void testProcessOverlandMovementCell_Simple ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Special locations
		final Set<MapCoordinates3DEx> blockedLocations = new HashSet<MapCoordinates3DEx> ();
		blockedLocations.add (new MapCoordinates3DEx (19, 10, 0));
		
		final Set<MapCoordinates3DEx> earthGates = new HashSet<MapCoordinates3DEx> ();
		earthGates.add (new MapCoordinates3DEx (40, 5, 0));		// Can't move to here if there's no gate to move from
		
		final Set<MapCoordinates2DEx> astralGates = new HashSet<MapCoordinates2DEx> ();
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		// Where we are moving from
		final OverlandMovementCell moveFromCell = new OverlandMovementCell ();
		moveFromCell.setDoubleMovementDistance (2);
		moves [0] [10] [20] = moveFromCell;
		
		// None of the 9 cells involved are towers
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		for (int x = 19; x <= 21; x++)
			for (int y = 9; y <= 11; y++)
			{
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
				terrain.getPlane ().get (0).getRow ().get (y).getCell ().get (x).setTerrainData (terrainData);
				when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
			}
		
		// Set up object to test
		final UnitMovement unitMovement = mock (UnitMovement.class);
		
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		utils.setMemoryGridCellUtils (memoryGridCellUtils);
		utils.setUnitMovement (unitMovement);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		utils.processOverlandMovementCell (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0), 1, 6, blockedLocations, earthGates, astralGates,
			cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, sys, mem, db);
		
		// Check correct moves were generated
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 1, new MapCoordinates3DEx (20, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 2, new MapCoordinates3DEx (21, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 3, new MapCoordinates3DEx (21, 10, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 4, new MapCoordinates3DEx (21, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 5, new MapCoordinates3DEx (20, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 6, new MapCoordinates3DEx (19, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		// Direction 7 is blocked
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 8, new MapCoordinates3DEx (19, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		verifyNoMoreInteractions (unitMovement);
	}

	/**
	 * Tests the processOverlandMovementCell method where a unit is at a tower so can move onto either plane
	 */
	@Test
	public final void testProcessOverlandMovementCell_FromTower ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Special locations
		final Set<MapCoordinates3DEx> blockedLocations = new HashSet<MapCoordinates3DEx> ();
		blockedLocations.add (new MapCoordinates3DEx (19, 10, 0));
		blockedLocations.add (new MapCoordinates3DEx (21, 11, 1));
		
		final Set<MapCoordinates3DEx> earthGates = new HashSet<MapCoordinates3DEx> ();
		earthGates.add (new MapCoordinates3DEx (40, 5, 0));		// Can't move to here if there's no gate to move from
		
		final Set<MapCoordinates2DEx> astralGates = new HashSet<MapCoordinates2DEx> ();
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		// Where we are moving from
		final OverlandMovementCell moveFromCell = new OverlandMovementCell ();
		moveFromCell.setDoubleMovementDistance (2);
		moves [0] [10] [20] = moveFromCell;
		
		// Starting cell is a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		for (int x = 19; x <= 21; x++)
			for (int y = 9; y <= 11; y++)
				for (int z = 0; z <= 1; z++)
				{
					final boolean isTowerLocation = (x == 20) && (y == 10) && (z == 0);
					final boolean isInvalidCell = (x == 20) && (y == 10) && (z == 1);
					
					if (!isInvalidCell)
					{
						final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
						terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x).setTerrainData (terrainData);
						when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (isTowerLocation);
					}
				}
		
		// Set up object to test
		final UnitMovement unitMovement = mock (UnitMovement.class);
		
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		utils.setMemoryGridCellUtils (memoryGridCellUtils);
		utils.setUnitMovement (unitMovement);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		utils.processOverlandMovementCell (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0), 1, 6, blockedLocations, earthGates, astralGates,
			cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, sys, mem, db);
		
		// Check correct moves were generated
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 1, new MapCoordinates3DEx (20, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 2, new MapCoordinates3DEx (21, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 3, new MapCoordinates3DEx (21, 10, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 4, new MapCoordinates3DEx (21, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 5, new MapCoordinates3DEx (20, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 6, new MapCoordinates3DEx (19, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		// Direction 7 on Arcanus is blocked
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 8, new MapCoordinates3DEx (19, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 1, new MapCoordinates3DEx (20, 9, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 2, new MapCoordinates3DEx (21, 9, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 3, new MapCoordinates3DEx (21, 10, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		// Direction 4 on Myrror is blocked
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 5, new MapCoordinates3DEx (20, 11, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 6, new MapCoordinates3DEx (19, 11, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 7, new MapCoordinates3DEx (19, 10, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 8, new MapCoordinates3DEx (19, 9, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		verifyNoMoreInteractions (unitMovement);
	}

	/**
	 * Tests the processOverlandMovementCell method when we're on Myrror standing next to a tower so one of our moves moves us to plane 0
	 */
	@Test
	public final void testProcessOverlandMovementCell_ToTower ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Special locations
		final Set<MapCoordinates3DEx> blockedLocations = new HashSet<MapCoordinates3DEx> ();
		blockedLocations.add (new MapCoordinates3DEx (19, 10, 1));
		
		final Set<MapCoordinates3DEx> earthGates = new HashSet<MapCoordinates3DEx> ();
		earthGates.add (new MapCoordinates3DEx (40, 5, 0));		// Can't move to here if there's no gate to move from
		
		final Set<MapCoordinates2DEx> astralGates = new HashSet<MapCoordinates2DEx> ();
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		// Where we are moving from
		final OverlandMovementCell moveFromCell = new OverlandMovementCell ();
		moveFromCell.setDoubleMovementDistance (2);
		moves [1] [10] [20] = moveFromCell;
		
		// Cell in direction 4 is a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		for (int x = 19; x <= 21; x++)
			for (int y = 9; y <= 11; y++)
			{
				final boolean isTowerLocation = (x == 21) && (y == 11);
				
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
				terrain.getPlane ().get (1).getRow ().get (y).getCell ().get (x).setTerrainData (terrainData);
				when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (isTowerLocation);
			}
		
		// Set up object to test
		final UnitMovement unitMovement = mock (UnitMovement.class);
		
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		utils.setMemoryGridCellUtils (memoryGridCellUtils);
		utils.setUnitMovement (unitMovement);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		utils.processOverlandMovementCell (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1), 1, 6, blockedLocations, earthGates, astralGates,
			cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, sys, mem, db);
		
		// Check correct moves were generated
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1),
			OverlandMovementType.ADJACENT, 1, new MapCoordinates3DEx (20, 9, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1),
			OverlandMovementType.ADJACENT, 2, new MapCoordinates3DEx (21, 9, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1),
			OverlandMovementType.ADJACENT, 3, new MapCoordinates3DEx (21, 10, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1),
			OverlandMovementType.ADJACENT, 4, new MapCoordinates3DEx (21, 11, 0), 1,		// <--------
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1),
			OverlandMovementType.ADJACENT, 5, new MapCoordinates3DEx (20, 11, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1),
			OverlandMovementType.ADJACENT, 6, new MapCoordinates3DEx (19, 11, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		// Direction 7 is blocked
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 1),
			OverlandMovementType.ADJACENT, 8, new MapCoordinates3DEx (19, 9, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		verifyNoMoreInteractions (unitMovement);
	}

	/**
	 * Tests the processOverlandMovementCell method where we are stood at a city with an Earth Gate
	 */
	@Test
	public final void testProcessOverlandMovementCell_EarthGate ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Special locations
		final Set<MapCoordinates3DEx> blockedLocations = new HashSet<MapCoordinates3DEx> ();
		blockedLocations.add (new MapCoordinates3DEx (19, 10, 0));
		blockedLocations.add (new MapCoordinates3DEx (50, 25, 0));
		
		final Set<MapCoordinates3DEx> earthGates = new HashSet<MapCoordinates3DEx> ();
		earthGates.add (new MapCoordinates3DEx (20, 10, 0));		// Where we start from
		earthGates.add (new MapCoordinates3DEx (40, 5, 0));
		earthGates.add (new MapCoordinates3DEx (50, 25, 0));		// Blocked
		earthGates.add (new MapCoordinates3DEx (45, 15, 1));		// Wrong plane
		
		final Set<MapCoordinates2DEx> astralGates = new HashSet<MapCoordinates2DEx> ();
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		// Where we are moving from
		final OverlandMovementCell moveFromCell = new OverlandMovementCell ();
		moveFromCell.setDoubleMovementDistance (2);
		moves [0] [10] [20] = moveFromCell;
		
		final OverlandMapTerrainData moveFromTerrain = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (moveFromTerrain);
		
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (moveFromTerrain)).thenReturn (false);
		
		// Set up object to test
		final UnitMovement unitMovement = mock (UnitMovement.class);
		
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		utils.setMemoryGridCellUtils (memoryGridCellUtils);
		utils.setUnitMovement (unitMovement);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		utils.processOverlandMovementCell (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0), 1, 6, blockedLocations, earthGates, astralGates,
			cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, sys, mem, db);
		
		// Check correct moves were generated
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 1, new MapCoordinates3DEx (20, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 2, new MapCoordinates3DEx (21, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 3, new MapCoordinates3DEx (21, 10, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 4, new MapCoordinates3DEx (21, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 5, new MapCoordinates3DEx (20, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 6, new MapCoordinates3DEx (19, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		// Direction 7 is blocked
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 8, new MapCoordinates3DEx (19, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.EARTH_GATE, 0, new MapCoordinates3DEx (40, 5, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		verifyNoMoreInteractions (unitMovement);
	}

	/**
	 * Tests the processOverlandMovementCell method where we are stood at a city with an Astral Gate (or on the corresponding cell on the other plane)
	 */
	@Test
	public final void testProcessOverlandMovementCell_AstralGate ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Terrain
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		// Special locations
		final Set<MapCoordinates3DEx> blockedLocations = new HashSet<MapCoordinates3DEx> ();
		blockedLocations.add (new MapCoordinates3DEx (19, 10, 0));
		
		final Set<MapCoordinates3DEx> earthGates = new HashSet<MapCoordinates3DEx> ();
		earthGates.add (new MapCoordinates3DEx (40, 5, 0));		// Can't move to here if there's no gate to move from
		
		final Set<MapCoordinates2DEx> astralGates = new HashSet<MapCoordinates2DEx> ();
		astralGates.add (new MapCoordinates2DEx (20, 10));
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		final Set<String> unitStackSkills = new HashSet<String> ();
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		
		// Movement array
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		
		// Where we are moving from
		final OverlandMovementCell moveFromCell = new OverlandMovementCell ();
		moveFromCell.setDoubleMovementDistance (2);
		moves [0] [10] [20] = moveFromCell;
		
		final OverlandMapTerrainData moveFromTerrain = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (moveFromTerrain);
		
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (moveFromTerrain)).thenReturn (false);
		
		// Set up object to test
		final UnitMovement unitMovement = mock (UnitMovement.class);
		
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		utils.setMemoryGridCellUtils (memoryGridCellUtils);
		utils.setUnitMovement (unitMovement);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		utils.processOverlandMovementCell (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0), 1, 6, blockedLocations, earthGates, astralGates,
			cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, sys, mem, db);
		
		// Check correct moves were generated
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 1, new MapCoordinates3DEx (20, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 2, new MapCoordinates3DEx (21, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 3, new MapCoordinates3DEx (21, 10, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 4, new MapCoordinates3DEx (21, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 5, new MapCoordinates3DEx (20, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 6, new MapCoordinates3DEx (19, 11, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		// Direction 7 is blocked
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ADJACENT, 8, new MapCoordinates3DEx (19, 9, 0), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		verify (unitMovement).considerPossibleOverlandMove (unitStack, unitStackSkills, new MapCoordinates3DEx (20, 10, 0),
			OverlandMovementType.ASTRAL_GATE, 0, new MapCoordinates3DEx (20, 10, 1), 1,
			2, 4, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		verifyNoMoreInteractions (unitMovement);
	}

	/**
	 * Tests the calculateDoubleMovementToEnterCombatTile method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementToEnterCombatTile () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatTileType tileDef = new CombatTileType ();			// Normal movement
		tileDef.setDoubleMovement (2);
		when (db.findCombatTileType ("CTL01", "calculateDoubleMovementToEnterCombatTile")).thenReturn (tileDef);

		final CombatTileType buildingDef = new CombatTileType ();		// Blocks movement
		buildingDef.setDoubleMovement (-1);
		when (db.findCombatTileType ("CBL03", "calculateDoubleMovementToEnterCombatTile")).thenReturn (buildingDef);

		final CombatTileType rockDef = new CombatTileType ();			// No effect on movement
		when (db.findCombatTileType ("CBL01", "calculateDoubleMovementToEnterCombatTile")).thenReturn (rockDef);

		final CombatTileType roadDef = new CombatTileType ();			// Cheap movement
		roadDef.setDoubleMovement (1);
		when (db.findCombatTileType ("CRL03", "calculateDoubleMovementToEnterCombatTile")).thenReturn (roadDef);
		
		final CombatTileBorder edgeBlockingBorder = new CombatTileBorder ();
		edgeBlockingBorder.setBlocksMovement (CombatTileBorderBlocksMovementID.CANNOT_CROSS_SPECIFIED_BORDERS);
		when (db.findCombatTileBorder ("CTB01", "calculateDoubleMovementToEnterCombatTile")).thenReturn (edgeBlockingBorder);
		
		final CombatTileBorder impassableBorder = new CombatTileBorder ();
		impassableBorder.setBlocksMovement (CombatTileBorderBlocksMovementID.WHOLE_TILE_IMPASSABLE);
		when (db.findCombatTileBorder ("CTB02", "calculateDoubleMovementToEnterCombatTile")).thenReturn (impassableBorder);
		
		final CombatTileBorder passableBorder = new CombatTileBorder ();
		passableBorder.setBlocksMovement (CombatTileBorderBlocksMovementID.NO);
		when (db.findCombatTileBorder ("CTB03", "calculateDoubleMovementToEnterCombatTile")).thenReturn (passableBorder);
		
		// Unit that is on the tile
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Set up object to test
		final MovementUtilsImpl utils = new MovementUtilsImpl ();
		utils.setCombatMapUtils (new CombatMapUtilsImpl ());		// Only search routine, easier to use the real one than mock it
		
		// Simplest test of a single grass layer
		final MomCombatTileLayer grass = new MomCombatTileLayer ();
		grass.setLayer (CombatMapLayerID.TERRAIN);
		grass.setCombatTileTypeID ("CTL01");
		
		final MomCombatTile tile = new MomCombatTile ();
		tile.getTileLayer ().add (grass);
		
		assertEquals (2, utils.calculateDoubleMovementToEnterCombatTile (xu, tile, db));
		
		// If its off the map edge, its automatically impassable
		tile.setOffMapEdge (true);
		assertEquals (-1, utils.calculateDoubleMovementToEnterCombatTile (xu, tile, db));
		tile.setOffMapEdge (false);
		
		// Borders with no blocking have no effect
		tile.getBorderID ().add ("CTB03");
		assertEquals (2, utils.calculateDoubleMovementToEnterCombatTile (xu, tile, db));
		
		// Borders with total blocking make tile impassable
		tile.getBorderID ().set (0, "CTB02");
		assertEquals (-1, utils.calculateDoubleMovementToEnterCombatTile (xu, tile, db));

		// Borders with edge blocking have no effect
		tile.getBorderID ().set (0, "CTB01");
		assertEquals (2, utils.calculateDoubleMovementToEnterCombatTile (xu, tile, db));
		
		// Building makes it impassable
		final MomCombatTileLayer building = new MomCombatTileLayer ();
		building.setLayer (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES);
		building.setCombatTileTypeID ("CBL03");
		tile.getTileLayer ().add (building);
		
		assertEquals (-1, utils.calculateDoubleMovementToEnterCombatTile (xu, tile, db));
		
		// Road doesn't prevent it from being impassable
		// Note have intentionally added the layers in the wrong order here - natural order is terrain-road-building
		// but we have terrain-building-road, to prove the routine forces the correct layer priority
		final MomCombatTileLayer road = new MomCombatTileLayer ();
		road.setLayer (CombatMapLayerID.ROAD);
		road.setCombatTileTypeID ("CRL03");
		tile.getTileLayer ().add (road);
		
		assertEquals (-1, utils.calculateDoubleMovementToEnterCombatTile (xu, tile, db));
		
		// Change the building to a rock, which doesn't block movement, now the road works
		tile.getTileLayer ().get (1).setCombatTileTypeID ("CBL01");
		assertEquals (1, utils.calculateDoubleMovementToEnterCombatTile (xu, tile, db));
	}
}