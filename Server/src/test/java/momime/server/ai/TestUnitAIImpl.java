package momime.server.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.calculations.UnitCalculations;
import momime.common.database.AiUnitCategory;
import momime.common.database.CommonDatabase;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.server.ServerTestData;

/**
 * Tests the UnitAIImpl class
 */
public final class TestUnitAIImpl extends ServerTestData
{
	/**
	 * Tests the unitMatchesCategory method in the simplest case where the category specifies no criteria
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_NoCriteria () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Category
		final AiUnitCategory category = new AiUnitCategory ();
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}

	/**
	 * Tests the unitMatchesCategory method where the criteria requires the unit to have a certain skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_UnitSkill () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Category
		final AiUnitCategory category = new AiUnitCategory ();
		category.setUnitSkillID ("US001");
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		when (xu.hasModifiedSkill ("US001")).thenReturn (true);
		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}

	/**
	 * Tests the unitMatchesCategory method where the criteria requires that the unit is a transport
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_Transport () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		
		// Category
		final AiUnitCategory category = new AiUnitCategory ();
		category.setTransport (true);
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitDefinition ()).thenReturn (unitDef);
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		unitDef.setTransportCapacity (1);
		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}

	/**
	 * Tests the unitMatchesCategory method where the criteria requires the unit to be able to pass over all terrain (swimmer, flyer, non-corporeal)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_AllTerrain () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Category
		final AiUnitCategory category = new AiUnitCategory ();
		category.setAllTerrainPassable (true);
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Set up object to test
		final UnitCalculations calc = mock (UnitCalculations.class);
		
		final UnitAIImpl ai = new UnitAIImpl ();
		ai.setUnitCalculations (calc);
		
		// Run method
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		when (calc.areAllTerrainTypesPassable (xu, xu.listModifiedSkillIDs (), db)).thenReturn (true);
		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}

	/**
	 * Tests the unitMatchesCategory method where the criteria requires the unit be in a transport
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_InTransport () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx nonTransportDef = new UnitEx ();
		when (db.findUnit ("UN001", "unitMatchesCategory")).thenReturn (nonTransportDef);
		
		final UnitEx transportDef = new UnitEx ();
		transportDef.setTransportCapacity (1);
		when (db.findUnit ("UN002", "unitMatchesCategory")).thenReturn (transportDef);
		
		// Category
		final AiUnitCategory category = new AiUnitCategory ();
		category.setInTransport (true);
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
		when (xu.getOwningPlayerID ()).thenReturn (3);
		
		// Fog of war memory
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);

		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.convertNullTileTypeToFOW (terrainData, false)).thenReturn ("TT01");

		// Set up object to test
		final UnitCalculations calc = mock (UnitCalculations.class);
		
		final UnitAIImpl ai = new UnitAIImpl ();
		ai.setUnitCalculations (calc);
		ai.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Unit can move over this terrain, it doesn't need a transport
		when (calc.calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), "TT01", db)).thenReturn (2);
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		
		// Unit can't move over this terrain, but it can't find a transport to get in either
		when (calc.calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), "TT01", db)).thenReturn (null);
		
		final MemoryUnit nonTransport = new MemoryUnit ();
		nonTransport.setOwningPlayerID (3);
		nonTransport.setStatus (UnitStatusID.ALIVE);
		nonTransport.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		nonTransport.setUnitID ("UN001");
		mem.getUnit ().add (nonTransport);
		
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		
		// Add a transport
		final MemoryUnit transport = new MemoryUnit ();
		transport.setOwningPlayerID (3);
		transport.setStatus (UnitStatusID.ALIVE);
		transport.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		transport.setUnitID ("UN002");
		mem.getUnit ().add (transport);

		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}
}