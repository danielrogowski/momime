package momime.common.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.Pick;
import momime.common.database.UnitEx;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.movement.UnitStack;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;

/**
 * Tests the UnitMovementImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitMovementImpl
{
	/**
	 * Tests the calculateDoubleMovementToEnterTile method
	 * @throws Exception If there is a problem
	 */
	@Test	
	public final void testCalculateDoubleMovementToEnterTile () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Pick normalUnit = new Pick ();
		normalUnit.setPickID (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		
		final CombatTileType cloudTile = new CombatTileType ();
		when (db.findCombatTileType (CommonDatabaseConstants.COMBAT_TILE_TYPE_CLOUD, "calculateDoubleMovementToEnterTile")).thenReturn (cloudTile);
		
		// Coordinate system
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Terrain
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);

		for (int x = 0; x < 5; x++)
		{
			final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
			terrain.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (terrainData);
			
			if (x != 1)
				when (memoryGridCellUtils.convertNullTileTypeToFOW (terrainData, true)).thenReturn ((x < 2) ? "TT01" : "TT02");
			
			if ((x == 3) || (x == 4))
				when (memoryGridCellUtils.convertNullTileTypeToFOW (terrainData, false)).thenReturn ((x < 2) ? "TT01" : "TT02");
		}
		
		when (memoryGridCellUtils.convertNullTileTypeToFOW (null, true)).thenReturn (null);
		
		// Map areas
		final int [] [] [] cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		final int [] [] [] ourUnitCountAtLocation = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];

		// Units
		final UnitEx unitDef = new UnitEx ();
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		
		// Unit stack
		final Set<String> unitStackSkills = new HashSet<String> ();

		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (xu1.getUnitDefinition ()).thenReturn (unitDef);
		when (xu1.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnit);

		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (xu2.getUnitDefinition ()).thenReturn (unitDef);
		when (xu2.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnit);
		
		when (unitCalculations.calculateDoubleMovementToEnterTileType (xu1, unitStackSkills, "TT02", db)).thenReturn (null);
		when (unitCalculations.calculateDoubleMovementToEnterTileType (xu2, unitStackSkills, "TT02", db)).thenReturn (null);
		
		final UnitStack unitStack = new UnitStack ();
		unitStack.getUnits ().add (xu1);
		unitStack.getUnits ().add (xu2);
		
		// Movement rates
		final Map<String, Integer> doubleMovementRates = new HashMap<String, Integer> ();
		doubleMovementRates.put ("TT01", 4);
		
		// There's already 1 unit at 0, 0, 0 so the 2 we're moving can fit
		ourUnitCountAtLocation [0] [0] [0] = 1;

		// There's already 8 units at 0, 0, 1 so the 2 we're moving can't fit
		// We're using max units per cell = 9 so 10 units won't fit
		ourUnitCountAtLocation [0] [0] [1] = 8;
		
		// 0, 0, 2 is impassable terrain

		// 0, 0, 3 is impassable terrain but there's a transport there we can get in and a unit standing beside it that is not inside the transport (passable terrain to that unit)
		// Transport can hold 2 units, there's 1 unit beside the transport already there, so adding 2 inside the transport both fits in the transport and within our 9 max units per cell
		ourUnitCountAtLocation [0] [0] [3] = 2;
		cellTransportCapacity [0] [0] [3] = 2; 

		// 0, 0, 4 is impassable terrain but there's a transport there we could get in, except its already partly full (impassable terrain to that unit)
		// Transport can hold 2 units, but there's 1 inside it already and we need 2 spaces, so its impassable even though we're within the 9 max units per cell
		ourUnitCountAtLocation [0] [0] [4] = 2;
		cellTransportCapacity [0] [0] [4] = 1;
		
		// No Spell Wards to block specific tiles
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Set up object to test
		final UnitMovementImpl move = new UnitMovementImpl ();
		move.setMemoryGridCellUtils (memoryGridCellUtils);
		move.setUnitCalculations (unitCalculations);
		
		// Call method
		final Integer [] [] [] result = move.calculateDoubleMovementToEnterTile (unitStack, unitStackSkills, 1, terrain,
			cellTransportCapacity, ourUnitCountAtLocation, doubleMovementRates, false, spells, sys, db);
		
		// Check results
		assertEquals (4, result [0] [0] [0].intValue ());
		assertNull (result [0] [0] [1]);
		assertNull (result [0] [0] [2]);
		assertEquals (2, result [0] [0] [3].intValue ());
		assertNull (result [0] [0] [4]);
	}
}