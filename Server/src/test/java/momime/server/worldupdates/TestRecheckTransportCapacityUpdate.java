package momime.server.worldupdates;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.utils.random.RandomUtils;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the RecheckTransportCapacityUpdate class
 */
@ExtendWith(MockitoExtension.class)
public final class TestRecheckTransportCapacityUpdate extends ServerTestData
{
	/**
	 * Tests the process method
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testProcess () throws Exception
	{
		// Server database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx triremeDef = new UnitEx ();
		triremeDef.setTransportCapacity (2);
		
		final UnitEx spearmenDef = new UnitEx ();
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (terrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (terrainData);
		
		// Terrain tile
		final MemoryGridCellUtils gridCellUtils = mock (MemoryGridCellUtils.class);
		when (gridCellUtils.convertNullTileTypeToFOW (terrainData, false)).thenReturn ("TT01");

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Unit skills
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		
		final Set<String> unitStackSkills = new HashSet<String> ();
		
		// Units
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final MemoryUnit trireme = new MemoryUnit ();
		trireme.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trireme.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
		trireme.setStatus (UnitStatusID.ALIVE);
		trireme.setUnitID ("UN001");
		trireme.setOwningPlayerID (1);
		trueMap.getUnit ().add (trireme);
		
		final ExpandedUnitDetails xuTrireme = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (trireme, null, null, null, players, trueMap, db)).thenReturn (xuTrireme);
		when (xuTrireme.getUnitDefinition ()).thenReturn (triremeDef);

		when (unitCalc.calculateDoubleMovementToEnterTileType (xuTrireme, unitStackSkills, "TT01", db)).thenReturn (2);
		
		MemoryUnit killedUnit = null;
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitURN (n + 1);
			spearmen.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
			spearmen.setStatus (UnitStatusID.ALIVE);
			spearmen.setUnitID ("UN002");
			spearmen.setOwningPlayerID (1);
			trueMap.getUnit ().add (spearmen);
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			when (expand.expandUnitDetails (spearmen, null, null, null, players, trueMap, db)).thenReturn (xuSpearmen);
			when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);
			
			if (n == 1)
				when (xuSpearmen.getMemoryUnit ()).thenReturn (spearmen);

			when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT01", db)).thenReturn (null);
			
			if (n == 1)
				killedUnit = spearmen;
		}
		
		// Fix random numbers
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (3)).thenReturn (1);
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final RecheckTransportCapacityUpdate update = new RecheckTransportCapacityUpdate ();
		update.setExpandUnitDetails (expand);
		update.setUnitCalculations (unitCalc);
		update.setMemoryGridCellUtils (gridCellUtils);
		update.setRandomUtils (random);
		
		// Run method
		update.setMapLocation (new MapCoordinates3DEx (21, 10, 1));
		update.process (mom);
		
		// Check 1 unit of spearmen was killed
		verify (wu).killUnit (killedUnit.getUnitURN (), KillUnitActionID.HEALABLE_OVERLAND_DAMAGE);
		verifyNoMoreInteractions (wu);
	}
}