package momime.common.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.GenerateTestData;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
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
		final MovementUtilsImpl move = new MovementUtilsImpl ();
		
		// Call method
		final int [] [] [] cellTransportCapacity = move.calculateCellTransportCapacity (unitStack, null, 2, null, null, null, null);
		
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
		when (unitCalculations.calculateDoubleMovementToEnterTileType (xu1, unitStackSkills, "TT01", db)).thenReturn (null);
		
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
		when (unitCalculations.calculateDoubleMovementToEnterTileType (xu4, unitStackSkills, "TT01", db)).thenReturn (null);
		
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
		when (unitCalculations.calculateDoubleMovementToEnterTileType (xu6, unitStackSkills, "TT01", db)).thenReturn (2);
		
		// At 0, 0, 5 there's somebody else's unit which makes no difference, we can "move onto it" to attack it
		final MemoryUnit unit7 = new MemoryUnit ();
		unit7.setStatus (UnitStatusID.ALIVE);
		unit7.setOwningPlayerID (1);
		unit7.setUnitLocation (new MapCoordinates3DEx (5, 0, 0));
		map.getUnit ().add (unit7);
		
		// Unit stack
		final UnitStack unitStack = new UnitStack ();
		
		// Set up object to test
		final MovementUtilsImpl move = new MovementUtilsImpl ();
		move.setMemoryGridCellUtils (memoryGridCellUtils);
		move.setExpandUnitDetails (expand);
		move.setUnitCalculations (unitCalculations);
		
		// Call method
		final int [] [] [] cellTransportCapacity = move.calculateCellTransportCapacity (unitStack, unitStackSkills, 2, map, players, sys, db);
		
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

		final MovementUtilsImpl move = new MovementUtilsImpl ();
		move.setExpandUnitDetails (expand);
		move.setUnitCalculations (unitCalc);
		
		// Single unit
		final List<ExpandedUnitDetails> units = new ArrayList<ExpandedUnitDetails> ();
		
		final ExpandedUnitDetails spearmenUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), anySet (), eq ("TT01"), eq (db))).thenReturn (4);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), anySet (), eq ("TT02"), eq (db))).thenReturn (6);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), anySet (), eq ("TT03"), eq (db))).thenReturn (null);
		
		units.add (spearmenUnit);

		final Map<String, Integer> spearmen = move.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, spearmen.size ());
		assertEquals (4, spearmen.get ("TT01").intValue ());
		assertEquals (6, spearmen.get ("TT02").intValue ());
		assertNull (spearmen.get ("TT03"));
		
		// Stacking a faster unit with it makes no difference - it always chooses the slowest movement rate
		final ExpandedUnitDetails flyingUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (flyingUnit), anySet (), any (String.class), eq (db))).thenReturn (2);
		
		units.add (flyingUnit);
		
		final Map<String, Integer> flying = move.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, flying.size ());
		assertEquals (4, flying.get ("TT01").intValue ());
		assertEquals (6, flying.get ("TT02").intValue ());
		assertNull (flying.get ("TT03"));
		
		// Stack a slower unit
		final ExpandedUnitDetails pathfindingUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (pathfindingUnit), anySet (), any (String.class), eq (db))).thenReturn (5);
		
		units.add (pathfindingUnit);
		
		final Map<String, Integer> pathfinding = move.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, pathfinding.size ());
		assertEquals (5, pathfinding.get ("TT01").intValue ());
		assertEquals (6, pathfinding.get ("TT02").intValue ());
		assertNull (pathfinding.get ("TT03"));
	
	}
}