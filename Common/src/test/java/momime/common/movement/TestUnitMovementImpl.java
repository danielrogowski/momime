package momime.common.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
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
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.GenerateTestData;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;

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
	}
}