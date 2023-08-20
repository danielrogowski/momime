package momime.server.process;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.MomSessionVariables;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.CityServerUtils;

/**
 * Tests the CityUpdatesImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCityUpdatesImpl
{
	/**
	 * Tests the conquerCity method when the attacker captures the city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConquerCity_capture () throws Exception
	{
		// Players
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (null, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (2);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Buildings
		final MapVolumeOfMemoryGridCells terrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);

		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)).thenReturn (null);
		
		// Other cities
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.countCities (terrain, 2, true)).thenReturn (1);
		
		// Set up object to test
		final CityProcessing cityProcessing = mock (CityProcessing.class);
		
		final CityUpdatesImpl updates = new CityUpdatesImpl ();
		updates.setMemoryBuildingUtils (memoryBuildingUtils);
		updates.setCityProcessing (cityProcessing);
		updates.setCityServerUtils (cityServerUtils);
		
		// Run method
		updates.conquerCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, CaptureCityDecisionID.CAPTURE, 99, mom);
		
		// Check correct methods were called
		verify (cityProcessing).captureCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, mom);
		
		verifyNoMoreInteractions (cityProcessing, cityServerUtils, memoryBuildingUtils);
	}

	/**
	 * Tests the conquerCity method when the attacker razes the city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConquerCity_raze () throws Exception
	{
		// Players
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (null, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (2);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Buildings
		final MapVolumeOfMemoryGridCells terrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);

		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)).thenReturn (null);
		
		// Other cities
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.countCities (terrain, 2, true)).thenReturn (1);
		
		// Set up object to test
		final CityProcessing cityProcessing = mock (CityProcessing.class);
		
		final CityUpdatesImpl updates = new CityUpdatesImpl ();
		updates.setMemoryBuildingUtils (memoryBuildingUtils);
		updates.setCityProcessing (cityProcessing);
		updates.setCityServerUtils (cityServerUtils);
		
		// Run method
		updates.conquerCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, CaptureCityDecisionID.RAZE, 99, mom);
		
		// Check correct methods were called
		verify (cityProcessing).razeCity (new MapCoordinates3DEx (20, 10, 1), mom);
		
		verifyNoMoreInteractions (cityProcessing, cityServerUtils, memoryBuildingUtils);
	}

	/**
	 * Tests the conquerCity method when the attacker leaves the city in ruins
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConquerCity_ruin () throws Exception
	{
		// Players
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (null, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (2);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Buildings
		final MapVolumeOfMemoryGridCells terrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);

		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)).thenReturn (null);
		
		// Other cities
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.countCities (terrain, 2, true)).thenReturn (1);
		
		// Set up object to test
		final CityProcessing cityProcessing = mock (CityProcessing.class);
		
		final CityUpdatesImpl updates = new CityUpdatesImpl ();
		updates.setMemoryBuildingUtils (memoryBuildingUtils);
		updates.setCityProcessing (cityProcessing);
		updates.setCityServerUtils (cityServerUtils);
		
		// Run method
		updates.conquerCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, CaptureCityDecisionID.RUIN, 99, mom);
		
		// Check correct methods were called
		verify (cityProcessing).ruinCity (new MapCoordinates3DEx (20, 10, 1), 99, mom);
		
		verifyNoMoreInteractions (cityProcessing, cityServerUtils, memoryBuildingUtils);
	}

	/**
	 * Tests the conquerCity method when the attacker captures the defender's last city and banishes them
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConquerCity_capture_banish () throws Exception
	{
		// Players
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (null, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (2);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Buildings
		final MapVolumeOfMemoryGridCells terrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);

		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)).thenReturn (null);
		
		// Other cities
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.countCities (terrain, 2, true)).thenReturn (0);
		
		// Set up object to test
		final CityProcessing cityProcessing = mock (CityProcessing.class);
		
		final CityUpdatesImpl updates = new CityUpdatesImpl ();
		updates.setMemoryBuildingUtils (memoryBuildingUtils);
		updates.setCityProcessing (cityProcessing);
		updates.setCityServerUtils (cityServerUtils);
		
		// Run method
		updates.conquerCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, CaptureCityDecisionID.CAPTURE, 99, mom);
		
		// Check correct methods were called
		verify (cityProcessing).captureCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, mom);
		verify (cityProcessing).banishWizard (attackingPlayer, defendingPlayer, false, mom);
		
		verifyNoMoreInteractions (cityProcessing, cityServerUtils, memoryBuildingUtils);
	}

	/**
	 * Tests the conquerCity method when the attacker captures the city containing the defender's summoning circle
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConquerCity_capture_summoningCircle () throws Exception
	{
		// Players
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (null, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (2);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Buildings
		final MapVolumeOfMemoryGridCells terrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);

		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (Arrays.asList (attackingPlayer, defendingPlayer));

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)).thenReturn (new MemoryBuilding ());
		
		// Other cities
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.countCities (terrain, 2, true)).thenReturn (1);
		
		// Set up object to test
		final CityProcessing cityProcessing = mock (CityProcessing.class);
		
		final CityUpdatesImpl updates = new CityUpdatesImpl ();
		updates.setMemoryBuildingUtils (memoryBuildingUtils);
		updates.setCityProcessing (cityProcessing);
		updates.setCityServerUtils (cityServerUtils);
		
		// Run method
		updates.conquerCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, CaptureCityDecisionID.CAPTURE, 99, mom);
		
		// Check correct methods were called
		verify (cityProcessing).captureCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, mom);
		verify (cityProcessing).moveSummoningCircleToWizardsFortress (2, mom);
		
		verifyNoMoreInteractions (cityProcessing, cityServerUtils, memoryBuildingUtils);
	}

	/**
	 * Tests the conquerCity method when the attacker captures the city containing the wizard's fortress
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConquerCity_capture_fortress () throws Exception
	{
		// Players
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (null, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (2);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Buildings
		final MapVolumeOfMemoryGridCells terrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);

		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)).thenReturn (null);
		
		// Set up object to test
		final CityProcessing cityProcessing = mock (CityProcessing.class);
		
		final CityUpdatesImpl updates = new CityUpdatesImpl ();
		updates.setMemoryBuildingUtils (memoryBuildingUtils);
		updates.setCityProcessing (cityProcessing);
		
		// Run method
		updates.conquerCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, CaptureCityDecisionID.CAPTURE, 99, mom);
		
		// Check correct methods were called
		verify (cityProcessing).captureCity (new MapCoordinates3DEx (20, 10, 1), attackingPlayer, defendingPlayer, mom);
		verify (cityProcessing).banishWizard (attackingPlayer, defendingPlayer, true, mom);
		
		verifyNoMoreInteractions (cityProcessing, memoryBuildingUtils);
	}
}