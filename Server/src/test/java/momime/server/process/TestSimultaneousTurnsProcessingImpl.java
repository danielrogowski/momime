package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_5.FogOfWarSettingData;
import momime.common.database.newgame.v0_9_5.MapSizeData;
import momime.common.messages.servertoclient.v0_9_5.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_5.TextPopupMessage;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.OverlandMapTerrainData;
import momime.common.messages.v0_9_5.UnitSpecialOrder;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_5.Plane;
import momime.server.database.v0_9_5.TileType;
import momime.server.database.v0_9_5.Unit;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.UnitServerUtils;

import org.junit.Test;

import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.areas.storage.MapArea2DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

/**
 * Tests the SimultaneousTurnsProcessingImpl class
 */
public final class TestSimultaneousTurnsProcessingImpl
{
	/**
	 * Tests the processSpecialOrders method
	 * @throws Exception If there is a problem
	 */
	@SuppressWarnings ("unchecked")
	@Test
	public final void testProcessSpecialOrders () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final Unit normalUnitDef = new Unit ();
		normalUnitDef.setUnitMagicRealm (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "processSpecialOrders-d")).thenReturn (normalUnitDef);
		
		final Unit heroUnitDef = new Unit ();
		heroUnitDef.setUnitMagicRealm (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN002", "processSpecialOrders-d")).thenReturn (heroUnitDef);

		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlane ()).thenReturn (planes);
		
		final TileType tt = new TileType ();
		tt.setCanBuildCity (true);
		when (db.findTileType ("TT01", "processSpecialOrders-t")).thenReturn (tt);
		
		// Session description
		final MapSizeData mapSize = ServerTestData.createMapSizeData ();
		final FogOfWarSettingData fogOfWarSettings = new FogOfWarSettingData ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		sd.setFogOfWarSetting (fogOfWarSettings);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (mapSize);
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, null);
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection (); 
		player1.setConnection (conn1);
		
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection (); 
		player2.setConnection (conn2);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Units to dismiss
		final List<MemoryUnit> dismisses = new ArrayList<MemoryUnit> (); 
		
		final MemoryUnit dismissNormalUnit = new MemoryUnit ();
		dismissNormalUnit.setUnitID ("UN001");
		dismisses.add (dismissNormalUnit);

		final MemoryUnit dismissHeroUnit = new MemoryUnit ();
		dismissHeroUnit.setUnitID ("UN002");
		dismisses.add (dismissHeroUnit);
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.listUnitsWithSpecialOrder (trueMap.getUnit (), UnitSpecialOrder.DISMISS)).thenReturn (dismisses);
		
		// Buildings to sell
		final MemoryGridCell tc = trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		tc.setBuildingIdSoldThisTurn ("BL01");
		tc.setCityData (cityData);
		
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (25, 15, 1);

		// Two settlers trying to build cities right next to each other - so only one can "win"
		final List<MemoryUnit> settlers = new ArrayList<MemoryUnit> ();

		final MapCoordinates3DEx settler1Location = new MapCoordinates3DEx (40, 20, 1);
		
		final MemoryUnit settler1 = new MemoryUnit ();
		settler1.setOwningPlayerID (pd1.getPlayerID ());
		settler1.setUnitLocation (settler1Location);
		settlers.add (settler1);

		final MapCoordinates3DEx settler2Location = new MapCoordinates3DEx (41, 20, 1);
		
		final MemoryUnit settler2 = new MemoryUnit ();
		settler2.setOwningPlayerID (pd2.getPlayerID ());
		settler2.setUnitLocation (settler2Location);
		settlers.add (settler2);

		when (unitServerUtils.listUnitsWithSpecialOrder (trueMap.getUnit (), UnitSpecialOrder.BUILD_CITY)).thenReturn (settlers);
		
		// Terrain where the settlers are
		final OverlandMapTerrainData terrain1 = new OverlandMapTerrainData ();
		terrain1.setTileTypeID ("TT01");
		trueTerrain.getPlane ().get (1).getRow ().get (20).getCell ().get (40).setTerrainData (terrain1);

		final OverlandMapTerrainData terrain2 = new OverlandMapTerrainData ();
		terrain2.setTileTypeID ("TT01");
		trueTerrain.getPlane ().get (1).getRow ().get (20).getCell ().get (41).setTerrainData (terrain2);
		
		// Existing city radius
		final MapArea2D<Boolean> falseArea = new MapArea2DArrayListImpl<Boolean> ();
		falseArea.setCoordinateSystem (mapSize);
		
		final MapArea2D<Boolean> trueArea = new MapArea2DArrayListImpl<Boolean> (); 
		trueArea.setCoordinateSystem (mapSize);
		
		for (int x = 0; x < mapSize.getWidth (); x++)
			for (int y = 0; y < mapSize.getHeight (); y++)
			{
				falseArea.set (x, y, false);
				trueArea.set (x, y, true);
			}
		
		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
		when (cityCalc.markWithinExistingCityRadius (trueTerrain, 1, mapSize)).thenReturn (falseArea, trueArea);
		
		// Player2 has 2 spirits he's trying to take a node from Player1 with; first fails, second succeeds, so third doesn't need to try
		final List<MemoryUnit> spirits = new ArrayList<MemoryUnit> ();
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit spirit = new MemoryUnit ();
			spirit.setOwningPlayerID (pd2.getPlayerID ());
			spirit.setUnitLocation (new MapCoordinates3DEx (50, 20, 0));
			spirits.add (spirit);
		}

		when (unitServerUtils.listUnitsWithSpecialOrder (trueMap.getUnit (), UnitSpecialOrder.MELD_WITH_NODE)).thenReturn (spirits);
		
		// Fix random results
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (2)).thenReturn (1);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final CityProcessing cityProc = mock (CityProcessing.class);
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		final OverlandMapServerUtils overlandMapServerUtils = mock (OverlandMapServerUtils.class);
		
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
		proc.setUnitServerUtils (unitServerUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setCityProcessing (cityProc);
		proc.setCityCalculations (cityCalc);
		proc.setCityServerUtils (cityServerUtils);
		proc.setOverlandMapServerUtils (overlandMapServerUtils);
		proc.setRandomUtils (randomUtils);
		
		// Run method
		proc.processSpecialOrders (mom);
		
		// Check units were dismissed
		verify (midTurn, times (1)).killUnitOnServerAndClients (dismissNormalUnit, KillUnitActionID.FREE, null, trueMap, players, fogOfWarSettings, db);
		verify (midTurn, times (1)).killUnitOnServerAndClients (dismissHeroUnit, KillUnitActionID.HERO_DIMISSED_VOLUNTARILY, null, trueMap, players, fogOfWarSettings, db);
		
		// Check buildings were sold
		verify (cityProc, times (1)).sellBuilding (trueMap, players, cityLocation, "BL01", false, true, sd, db);
		
		// Check only 1 settler was allowed to build
		verify (cityServerUtils, times (0)).buildCityFromSettler (gsk, player1, settler1, players, sd, db);
		verify (cityServerUtils, times (1)).buildCityFromSettler (gsk, player2, settler2, players, sd, db);
		
		assertEquals (0, conn2.getMessages ().size ());
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (TextPopupMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final TextPopupMessage popup = (TextPopupMessage) conn1.getMessages ().get (0);
		assertEquals ("Another city was built before yours and is within 3 squares of where you are trying to build, so you cannot build here anymore", popup.getText ());
		
		// Both spirits tried to meld (the melding method is mocked, so we don't even know whether they succeeded)
		for (final MemoryUnit spirit : spirits)
			verify (overlandMapServerUtils, times (1)).attemptToMeldWithNode (spirit, trueMap, players, sd, db);
	}
}
