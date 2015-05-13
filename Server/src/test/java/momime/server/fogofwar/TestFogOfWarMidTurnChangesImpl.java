package momime.server.fogofwar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.OverlandMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddBuildingMessage;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.messages.servertoclient.MoveUnitStackOverlandMessage;
import momime.common.messages.servertoclient.UpdateCityMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.database.MapFeatureMagicRealmSvr;
import momime.server.database.MapFeatureSvr;
import momime.server.database.PlaneSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the FogOfWarMidTurnChangesImpl class
 */
public final class TestFogOfWarMidTurnChangesImpl
{
	/**
	 * Tests the updatePlayerMemoryOfTerrain method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUpdatePlayerMemoryOfTerrain () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// True terrain
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		
		final OverlandMapTerrainData td = new OverlandMapTerrainData ();
		
		final MemoryGridCell tc = new MemoryGridCell ();
		tc.setTerrainData (td);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().set (20, tc);
		
		// Set up coordinates
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (20, 10, 1);
		
		// Human player who can't see the location
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells map1 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc1 = map1.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		final FogOfWarMemory mem1 = new FogOfWarMemory ();
		mem1.setMap (map1);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (mem1);
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);

		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);

		final MapVolumeOfMemoryGridCells map2 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc2 = map2.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem2 = new FogOfWarMemory ();
		mem2.setMap (map2);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (mem2);
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);

		// Human player who can see the location and whose info is out of date
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MapVolumeOfMemoryGridCells map3 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc3 = map3.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem3 = new FogOfWarMemory ();
		mem3.setMap (map3);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (mem3);
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		players.add (player3);
		
		// AI player who can see the location and whose info is out of date
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (-2);
		pd4.setHuman (false);

		final MapVolumeOfMemoryGridCells map4 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc4 = map4.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem4 = new FogOfWarMemory ();
		mem4.setMap (map4);
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (mem4);
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);

		// Human player who can see the location and already has up to date info
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (8);
		pd5.setHuman (true);

		final MapVolumeOfMemoryGridCells map5 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc5 = map5.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		final FogOfWarMemory mem5 = new FogOfWarMemory ();
		mem5.setMap (map5);
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (mem5);
		priv5.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv5.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		players.add (player5);
		
		// Set up object to test
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (true);
		
		final FogOfWarDuplication dup = mock (FogOfWarDuplication.class);
		when (dup.copyTerrainAndNodeAura (tc, mc3)).thenReturn (true);
		when (dup.copyTerrainAndNodeAura (tc, mc4)).thenReturn (true);
		when (dup.copyTerrainAndNodeAura (tc, mc5)).thenReturn (false);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setFogOfWarDuplication (dup);

		// Run test
		calc.updatePlayerMemoryOfTerrain (trueTerrain, players, coords, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		
		// 3rd player is only one who should actually receive a message
		// (The other players don't even have dummy connections created for them, so would fall over with null pointer exception
		// if a msg was generated hence we don't need asserts for it here)
		assertEquals (1, msgs3.getMessages ().size ());
		
		final UpdateTerrainMessage updateMsg = (UpdateTerrainMessage) msgs3.getMessages ().get (0);
		assertEquals (20, updateMsg.getData ().getMapLocation ().getX ());
		assertEquals (10, updateMsg.getData ().getMapLocation ().getY ());
		assertEquals (1, updateMsg.getData ().getMapLocation ().getZ ());
		assertSame (td, updateMsg.getData ().getTerrainData ());
		
		// Prove that data was copied for players 3,4&5 but not for 1&2
		verify (dup, times (0)).copyTerrainAndNodeAura (tc, mc1);
		verify (dup, times (0)).copyTerrainAndNodeAura (tc, mc2);
		verify (dup, times (1)).copyTerrainAndNodeAura (tc, mc3);
		verify (dup, times (1)).copyTerrainAndNodeAura (tc, mc4);
		verify (dup, times (1)).copyTerrainAndNodeAura (tc, mc5);
	}
	
	/**
	 * Tests the updatePlayerMemoryOfCity method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUpdatePlayerMemoryOfCity () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// True terrain
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		
		final OverlandMapCityData td = new OverlandMapCityData ();
		
		final MemoryGridCell tc = new MemoryGridCell ();
		tc.setCityData (td);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().set (20, tc);
		
		// Set up coordinates
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (20, 10, 1);
		
		// Human player who can't see the location
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells map1 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc1 = map1.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		final FogOfWarMemory mem1 = new FogOfWarMemory ();
		mem1.setMap (map1);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (mem1);
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);

		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);

		final MapVolumeOfMemoryGridCells map2 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc2 = map2.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem2 = new FogOfWarMemory ();
		mem2.setMap (map2);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (mem2);
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);

		// Human player who can see the location and whose info is out of date
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MapVolumeOfMemoryGridCells map3 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc3 = map3.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem3 = new FogOfWarMemory ();
		mem3.setMap (map3);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (mem3);
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		players.add (player3);
		
		// AI player who can see the location and whose info is out of date
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (-2);
		pd4.setHuman (false);

		final MapVolumeOfMemoryGridCells map4 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc4 = map4.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem4 = new FogOfWarMemory ();
		mem4.setMap (map4);
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (mem4);
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);

		// Human player who can see the location and already has up to date info
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (8);
		pd5.setHuman (true);

		final MapVolumeOfMemoryGridCells map5 = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final MemoryGridCell mc5 = map5.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		final FogOfWarMemory mem5 = new FogOfWarMemory ();
		mem5.setMap (map5);
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (mem5);
		priv5.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv5.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		players.add (player5);
		
		// Set up object to test
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (true);
		
		final FogOfWarDuplication dup = mock (FogOfWarDuplication.class);
		when (dup.copyCityData (tc, mc3, false)).thenReturn (true);
		when (dup.copyCityData (tc, mc4, false)).thenReturn (true);
		when (dup.copyCityData (tc, mc5, false)).thenReturn (false);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setFogOfWarDuplication (dup);

		// Run test
		calc.updatePlayerMemoryOfCity (trueTerrain, players, coords, sd.getFogOfWarSetting (), false);
		
		// 3rd player is only one who should actually receive a message
		// (The other players don't even have dummy connections created for them, so would fall over with null pointer exception
		// if a msg was generated hence we don't need asserts for it here)
		assertEquals (1, msgs3.getMessages ().size ());
		
		final UpdateCityMessage updateMsg = (UpdateCityMessage) msgs3.getMessages ().get (0);
		assertEquals (20, updateMsg.getData ().getMapLocation ().getX ());
		assertEquals (10, updateMsg.getData ().getMapLocation ().getY ());
		assertEquals (1, updateMsg.getData ().getMapLocation ().getZ ());
		assertSame (mc3.getCityData (), updateMsg.getData ().getCityData ());		// NB. not true data as like terrain test, see comments in method
		
		// Prove that data was copied for players 3,4&5 but not for 1&2
		verify (dup, times (0)).copyCityData (tc, mc1, false);
		verify (dup, times (0)).copyCityData (tc, mc2, false);
		verify (dup, times (1)).copyCityData (tc, mc3, false);
		verify (dup, times (1)).copyCityData (tc, mc4, false);
		verify (dup, times (1)).copyCityData (tc, mc5, false);
		
		verify (dup, times (0)).copyCityData (tc, mc1, true);
		verify (dup, times (0)).copyCityData (tc, mc2, true);
		verify (dup, times (0)).copyCityData (tc, mc3, true);
		verify (dup, times (0)).copyCityData (tc, mc4, true);
		verify (dup, times (0)).copyCityData (tc, mc5, true);
	}
	
	/**
	 * Tests the canSeeUnitMidTurn method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitMidTurn () throws Exception
	{
		// Mock server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		final FogOfWarSetting settings = new FogOfWarSetting ();

		// True terrain
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

		// player1 owns the unit, player2 is trying to see it
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();
		pub1.setWizardID ("WZ01");		// i.e. anything other than "Rampaging monsters"
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, pub1, null, null, null);
		players.add (player1);
		
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (7);
		pd2.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sys));
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);

		final PlayerServerDetails player3 = new PlayerServerDetails (null, null, null, null, null);
		players.add (player3);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd1.getPlayerID (), "canSeeUnitMidTurn")).thenReturn (player1);
		
		// Set up object to test
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);

		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// The unit we're trying to see
		final MapCoordinates3DEx unitLocation = new MapCoordinates3DEx (20, 10, 0);

		final MemoryUnit spearmen = new MemoryUnit ();
		spearmen.setOwningPlayerID (pd1.getPlayerID ());
		spearmen.setUnitLocation (unitLocation);
		spearmen.setStatus (UnitStatusID.ALIVE);
		
		// Regular situation of a unit we can't see because we can't see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (unitLocation, settings.getUnits (), trueTerrain, priv2.getFogOfWar (), db)).thenReturn (false);
		assertFalse (calc.canSeeUnitMidTurn (spearmen, trueTerrain, player2, db, settings));

		// Regular situation of a unit we can see because we can see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (unitLocation, settings.getUnits (), trueTerrain, priv2.getFogOfWar (), db)).thenReturn (true);
		assertTrue (calc.canSeeUnitMidTurn (spearmen, trueTerrain, player2, db, settings));

		// Can't see dead units, even if we can see their location
		spearmen.setStatus (UnitStatusID.DEAD);
		assertFalse (calc.canSeeUnitMidTurn (spearmen, trueTerrain, player2, db, settings));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on a unit enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_UnitEnchantment () throws Exception
	{
		// Mock server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		final FogOfWarSetting settings = new FogOfWarSetting ();

		// True terrain
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

		// player1 owns the unit, player2 is trying to see it
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();
		pub1.setWizardID ("WZ01");		// i.e. anything other than "Rampaging monsters"
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, pub1, null, null, null);
		players.add (player1);
		
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (7);
		pd2.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sys));
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd1.getPlayerID (), "canSeeUnitMidTurn")).thenReturn (player1);
		
		// Set up object to test
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setUnitUtils (unitUtils);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);

		// Spell to check
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setUnitURN (11);
		
		// The unit we're trying to see
		// Note creating units like this defaults them to ALIVE, so we don't need to set that
		final MapCoordinates3DEx unitLocation = new MapCoordinates3DEx (20, 10, 0);

		final MemoryUnit spearmen = new MemoryUnit ();
		spearmen.setOwningPlayerID (pd1.getPlayerID ());
		spearmen.setUnitLocation (unitLocation);
		spearmen.setStatus (UnitStatusID.ALIVE);
		
		final List<MemoryUnit> trueUnits = new ArrayList<MemoryUnit> ();
		
		when (unitUtils.findUnitURN (11, trueUnits, "canSeeSpellMidTurn")).thenReturn (spearmen);
		
		// Regular situation of a unit we can't see because we can't see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (unitLocation, settings.getUnits (), trueTerrain, priv2.getFogOfWar (), db)).thenReturn (false);
		assertFalse (calc.canSeeSpellMidTurn (spell, trueTerrain, trueUnits, player2, db, settings));

		// Regular situation of a unit we can see because we can see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (unitLocation, settings.getUnits (), trueTerrain, priv2.getFogOfWar (), db)).thenReturn (true);
		assertTrue (calc.canSeeSpellMidTurn (spell, trueTerrain, trueUnits, player2, db, settings));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on a city enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_CityEnchantment () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		final FogOfWarSetting settings = new FogOfWarSetting ();
		settings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();

		// Player who is trying to see it
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWar (ServerTestData.createFogOfWarArea (sys));
		
		final PlayerServerDetails player = new PlayerServerDetails (null, null, priv, null, null);

		// The location of the city that has the spell we're trying to see
		final MapCoordinates3DEx spellLocation = new MapCoordinates3DEx (20, 10, 1);

		// Spell to check
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setCityLocation (spellLocation);

		// Set up object to test
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (true);

		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		
		// Run test
		assertFalse (calc.canSeeSpellMidTurn (spell, null, null, player, db, settings));
		
		priv.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		assertTrue (calc.canSeeSpellMidTurn (spell, null, null, player, db, settings));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on an overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_OverlandEnchantment () throws Exception
	{
		// Set up object to test
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();

		// Spell to check - assumed to be overland since it has no UnitURN or CityLocation set
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();

		// Run test
		assertTrue (calc.canSeeSpellMidTurn (spell, null, null, null, null, null));
	}
	
	/**
	 * Tests the canSeeCombatAreaEffectMidTurn method on a global CAE
	 */
	@Test
	public final void testCanSeeCombatAreaEffectMidTurn_Global ()
	{
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		// CAE
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();

		// Can see this regardless of settings or visible area
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertTrue (calc.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, setting));
	}

	/**
	 * Tests the canSeeCombatAreaEffectMidTurn method on a localized CAE
	 */
	@Test
	public final void testCanSeeCombatAreaEffectMidTurn_Localized ()
	{
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		// Set up object to test
		final FogOfWarCalculations fow = mock (FogOfWarCalculations.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (fow);
		
		// One cell we can see, another that we can't
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (true);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (false);

		// Set matching states in two locations
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (21, FogOfWarStateID.NEVER_SEEN);
		
		// CAE
		final MapCoordinates3DEx caeLocation = new MapCoordinates3DEx (20, 10, 1);

		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		cae.setMapLocation (caeLocation);

		// Method should return the value from wherever the location of the CAE is
		assertTrue (calc.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));
		caeLocation.setX (21);
		assertFalse (calc.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));
	}
	
	/**
	 * Tests the addBuildingOnServerAndClients method, when its from the normal completion of constructing a building
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBuildingOnServerAndClients_RegularConstruction () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player who can't see the location
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv1.setFogOfWarMemory (new FogOfWarMemory ());
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);
		
		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (4);
		pd2.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv2.setFogOfWarMemory (new FogOfWarMemory ());
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Human player who can see the location
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (5);
		pd3.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv3.setFogOfWarMemory (new FogOfWarMemory ());
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		players.add (player3);
		
		final DummyServerToClientConnection conn3 = new DummyServerToClientConnection ();
		player3.setConnection (conn3);
		
		// AI player who can see the location
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (6);
		pd4.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv4.setFogOfWarMemory (new FogOfWarMemory ());
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (pd3.getPlayerID ());
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd3.getPlayerID (), "addBuildingOnServerAndClients")).thenReturn (player3);
		
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up object to test
		final FogOfWarCalculations fow = mock (FogOfWarCalculations.class);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (false);
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (true);		
		
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();		// Used real one here, since makes it easier to check the output than via mock
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fow);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setFogOfWarDuplication (dup);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.addBuildingOnServerAndClients (gsk, players, cityLocation, "BL03", null, null, null, sd, db);
		
		// Prove that building got added to server's true map
		assertEquals (1, trueMap.getBuilding ().size ());
		assertEquals ("BL03", trueMap.getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, trueMap.getBuilding ().get (0).getCityLocation ());
		
		// Prove that building got added to server's copy of each player's memory, but only the players who can see it
		assertEquals (0, priv1.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals (0, priv2.getFogOfWarMemory ().getBuilding ().size ());
		
		assertEquals (1, priv3.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals ("BL03", priv3.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, priv3.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
		
		assertEquals (1, priv4.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals ("BL03", priv4.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, priv4.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
		
		// Prove that human player's client was sent update msg
		assertEquals (1, conn3.getMessages ().size ());
		final AddBuildingMessage msg = (AddBuildingMessage) conn3.getMessages ().get (0);
		assertEquals (cityLocation, msg.getFirstBuilding ().getCityLocation ());
		assertEquals ("BL03", msg.getFirstBuilding ().getBuildingID ());
		assertNull (msg.getSecondBuilding ());
		assertNull (msg.getBuildingCreatedFromSpellID ());
		assertNull (msg.getBuildingCreationSpellCastByPlayerID ());
	}

	/**
	 * Tests the addBuildingOnServerAndClients method, when its from the move fortress spell, so adds 2 buildings resulting from a spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBuildingOnServerAndClients_MoveFortress () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getOverlandMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player who can't see the location
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv1.setFogOfWarMemory (new FogOfWarMemory ());
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);
		
		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (4);
		pd2.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv2.setFogOfWarMemory (new FogOfWarMemory ());
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Human player who can see the location
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (5);
		pd3.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv3.setFogOfWarMemory (new FogOfWarMemory ());
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		players.add (player3);
		
		final DummyServerToClientConnection conn3 = new DummyServerToClientConnection ();
		player3.setConnection (conn3);
		
		// AI player who can see the location
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (6);
		pd4.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getOverlandMapSize ()));
		priv4.setFogOfWarMemory (new FogOfWarMemory ());
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (pd3.getPlayerID ());
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd3.getPlayerID (), "addBuildingOnServerAndClients")).thenReturn (player3);
		
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up object to test
		final FogOfWarCalculations fow = mock (FogOfWarCalculations.class);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (false);
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (true);		
		
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();		// Used real one here, since makes it easier to check the output than via mock
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fow);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setFogOfWarDuplication (dup);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.addBuildingOnServerAndClients (gsk, players, cityLocation, CommonDatabaseConstants.BUILDING_FORTRESS,
			CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, "SP028", 7, sd, db);
		
		// Prove that building got added to server's true map
		assertEquals (2, trueMap.getBuilding ().size ());
		assertEquals (CommonDatabaseConstants.BUILDING_FORTRESS, trueMap.getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, trueMap.getBuilding ().get (0).getCityLocation ());
		assertEquals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, trueMap.getBuilding ().get (1).getBuildingID ());
		assertEquals (cityLocation, trueMap.getBuilding ().get (1).getCityLocation ());
		
		// Prove that building got added to server's copy of each player's memory, but only the players who can see it
		assertEquals (0, priv1.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals (0, priv2.getFogOfWarMemory ().getBuilding ().size ());
		
		assertEquals (2, priv3.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals (CommonDatabaseConstants.BUILDING_FORTRESS, priv3.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, priv3.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
		assertEquals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, priv3.getFogOfWarMemory ().getBuilding ().get (1).getBuildingID ());
		assertEquals (cityLocation, priv3.getFogOfWarMemory ().getBuilding ().get (1).getCityLocation ());
		
		assertEquals (2, priv4.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals (CommonDatabaseConstants.BUILDING_FORTRESS, priv4.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, priv4.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
		assertEquals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, priv4.getFogOfWarMemory ().getBuilding ().get (1).getBuildingID ());
		assertEquals (cityLocation, priv4.getFogOfWarMemory ().getBuilding ().get (1).getCityLocation ());
		
		// Prove that human player's client was sent update msg
		assertEquals (1, conn3.getMessages ().size ());
		final AddBuildingMessage msg = (AddBuildingMessage) conn3.getMessages ().get (0);
		assertEquals (cityLocation, msg.getFirstBuilding ().getCityLocation ());
		assertEquals (CommonDatabaseConstants.BUILDING_FORTRESS, msg.getFirstBuilding ().getBuildingID ());
		assertEquals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, msg.getSecondBuilding ().getBuildingID ());
		assertEquals ("SP028", msg.getBuildingCreatedFromSpellID ());
		assertEquals (7, msg.getBuildingCreationSpellCastByPlayerID ().intValue ());
	}
	
	/**
	 * Tests the destroyBuildingOnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDestroyBuildingOnServerAndClients () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final OverlandMapSize overlandMapSize = ServerTestData.createOverlandMapSize ();
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setFogOfWarSetting (fowSettings);

		// Overland map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (overlandMapSize);
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// The building being destroyed
		final MemoryBuilding trueBuilding = new MemoryBuilding ();
		trueBuilding.setBuildingURN (3);
		trueBuilding.setBuildingID ("BL03");
		trueBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getBuilding ().add (trueBuilding);

		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuildingURN (trueBuilding.getBuildingURN (), trueMap.getBuilding (), "destroyBuildingOnServerAndClients")).thenReturn (trueBuilding);
		
		// Human player who can't see the location
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (overlandMapSize));
		priv1.setFogOfWarMemory (new FogOfWarMemory ());
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final MemoryBuilding building1 = new MemoryBuilding ();		// Some other building, just to make the building lists unique
		building1.setBuildingURN (4);
		building1.setBuildingID ("BL04");
		building1.setCityLocation (new MapCoordinates3DEx (21, 10, 1));
		priv1.getFogOfWarMemory ().getBuilding ().add (building1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);
		
		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (4);
		pd2.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (overlandMapSize));
		priv2.setFogOfWarMemory (new FogOfWarMemory ());
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);

		final MemoryBuilding building2 = new MemoryBuilding ();		// Some other building, just to make the building lists unique
		building2.setBuildingURN (building1.getBuildingURN ());
		building2.setBuildingID ("BL04");
		building2.setCityLocation (new MapCoordinates3DEx (21, 10, 1));
		priv2.getFogOfWarMemory ().getBuilding ().add (building2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Human player who can see the location
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (5);
		pd3.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (overlandMapSize));
		priv3.setFogOfWarMemory (new FogOfWarMemory ());
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final MemoryBuilding building3 = new MemoryBuilding ();		// The building being destroyed
		building3.setBuildingURN (trueBuilding.getBuildingURN ());
		building3.setBuildingID ("BL03");
		building3.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		priv3.getFogOfWarMemory ().getBuilding ().add (building3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		players.add (player3);
		
		final DummyServerToClientConnection conn3 = new DummyServerToClientConnection ();
		player3.setConnection (conn3);
		
		// AI player who can see the location
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (6);
		pd4.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (overlandMapSize));
		priv4.setFogOfWarMemory (new FogOfWarMemory ());
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final MemoryBuilding building4 = new MemoryBuilding ();		// The building being destroyed
		building4.setBuildingURN (trueBuilding.getBuildingURN ());
		building4.setBuildingID ("BL03");
		building4.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		priv4.getFogOfWarMemory ().getBuilding ().add (building4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (pd3.getPlayerID ());
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd3.getPlayerID (), "destroyBuildingOnServerAndClients")).thenReturn (player3);

		// Set up object to test
		final FogOfWarCalculations fow = mock (FogOfWarCalculations.class);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSettings.getCitiesSpellsAndCombatAreaEffects ())).thenReturn (false);
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSettings.getCitiesSpellsAndCombatAreaEffects ())).thenReturn (true);		

		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fow);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setMemoryBuildingUtils (buildingUtils);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.destroyBuildingOnServerAndClients (trueMap, players, trueBuilding.getBuildingURN (), false, sd, db);
		
		// Prove that building got removed from server's true map
		verify (buildingUtils, times (1)).removeBuildingURN (trueBuilding.getBuildingURN (), trueMap.getBuilding ());
		
		// Prove that building got removed from server's copy of each player's memory, but only the players who saw it disappear
		verify (buildingUtils, times (0)).removeBuildingURN (trueBuilding.getBuildingURN (), priv1.getFogOfWarMemory ().getBuilding ());
		verify (buildingUtils, times (0)).removeBuildingURN (trueBuilding.getBuildingURN (), priv2.getFogOfWarMemory ().getBuilding ());
		verify (buildingUtils, times (1)).removeBuildingURN (trueBuilding.getBuildingURN (), priv3.getFogOfWarMemory ().getBuilding ());
		verify (buildingUtils, times (1)).removeBuildingURN (trueBuilding.getBuildingURN (), priv4.getFogOfWarMemory ().getBuilding ());

		// Prove that only the human player's client that saw the building disappear was sent update msg
		assertEquals (1, conn3.getMessages ().size ());
		final DestroyBuildingMessage msg = (DestroyBuildingMessage) conn3.getMessages ().get (0);
		assertEquals (trueBuilding.getBuildingURN (), msg.getBuildingURN ());
		assertFalse (msg.isUpdateBuildingSoldThisTurn ());
	}

	/**
	 * Tests the destroyAllBuildingsInLocationOnServerAndClients method - this is basically a copy of the destroyBuildingOnServerAndClients test
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDestroyAllBuildingsInLocationOnServerAndClients () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final OverlandMapSize overlandMapSize = ServerTestData.createOverlandMapSize ();
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setFogOfWarSetting (fowSettings);

		// Overland map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (overlandMapSize);
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// The building being destroyed
		final MemoryBuilding trueBuilding = new MemoryBuilding ();
		trueBuilding.setBuildingURN (3);
		trueBuilding.setBuildingID ("BL03");
		trueBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getBuilding ().add (trueBuilding);

		// Some building in another location so it doesn't get destroyed
		final MemoryBuilding otherBuilding = new MemoryBuilding ();
		otherBuilding.setBuildingURN (4);
		otherBuilding.setBuildingID ("BL04");
		otherBuilding.setCityLocation (new MapCoordinates3DEx (21, 10, 1));
		trueMap.getBuilding ().add (otherBuilding);
		
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuildingURN (trueBuilding.getBuildingURN (), trueMap.getBuilding (), "destroyBuildingOnServerAndClients")).thenReturn (trueBuilding);
		when (buildingUtils.findBuildingURN (otherBuilding.getBuildingURN (), trueMap.getBuilding (), "destroyBuildingOnServerAndClients")).thenReturn (otherBuilding);
		
		// Human player who can't see the location
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (overlandMapSize));
		priv1.setFogOfWarMemory (new FogOfWarMemory ());
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final MemoryBuilding building1 = new MemoryBuilding ();		// Some other building, just to make the building lists unique
		building1.setBuildingURN (otherBuilding.getBuildingURN ());
		building1.setBuildingID ("BL04");
		building1.setCityLocation (new MapCoordinates3DEx (21, 10, 1));
		priv1.getFogOfWarMemory ().getBuilding ().add (building1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);
		
		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (4);
		pd2.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (overlandMapSize));
		priv2.setFogOfWarMemory (new FogOfWarMemory ());
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);

		final MemoryBuilding building2 = new MemoryBuilding ();		// Some other building, just to make the building lists unique
		building2.setBuildingURN (otherBuilding.getBuildingURN ());
		building2.setBuildingID ("BL04");
		building2.setCityLocation (new MapCoordinates3DEx (21, 10, 1));
		priv2.getFogOfWarMemory ().getBuilding ().add (building2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Human player who can see the location
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (5);
		pd3.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (overlandMapSize));
		priv3.setFogOfWarMemory (new FogOfWarMemory ());
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final MemoryBuilding building3 = new MemoryBuilding ();		// The building being destroyed
		building3.setBuildingURN (trueBuilding.getBuildingURN ());
		building3.setBuildingID ("BL03");
		building3.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		priv3.getFogOfWarMemory ().getBuilding ().add (building3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		players.add (player3);
		
		final DummyServerToClientConnection conn3 = new DummyServerToClientConnection ();
		player3.setConnection (conn3);
		
		// AI player who can see the location
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (6);
		pd4.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (overlandMapSize));
		priv4.setFogOfWarMemory (new FogOfWarMemory ());
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final MemoryBuilding building4 = new MemoryBuilding ();		// The building being destroyed
		building4.setBuildingURN (trueBuilding.getBuildingURN ());
		building4.setBuildingID ("BL03");
		building4.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		priv4.getFogOfWarMemory ().getBuilding ().add (building4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (pd3.getPlayerID ());
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd3.getPlayerID (), "destroyBuildingOnServerAndClients")).thenReturn (player3);
		
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);

		// Set up object to test
		final FogOfWarCalculations fow = mock (FogOfWarCalculations.class);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSettings.getCitiesSpellsAndCombatAreaEffects ())).thenReturn (false);
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSettings.getCitiesSpellsAndCombatAreaEffects ())).thenReturn (true);		

		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fow);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setMemoryBuildingUtils (buildingUtils);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.destroyAllBuildingsInLocationOnServerAndClients (trueMap, players, cityLocation, sd, db);
		
		// Prove that building got removed from server's true map
		verify (buildingUtils, times (1)).removeBuildingURN (trueBuilding.getBuildingURN (), trueMap.getBuilding ());
		
		// Prove that building got removed from server's copy of each player's memory, but only the players who saw it disappear
		verify (buildingUtils, times (0)).removeBuildingURN (trueBuilding.getBuildingURN (), priv1.getFogOfWarMemory ().getBuilding ());
		verify (buildingUtils, times (0)).removeBuildingURN (trueBuilding.getBuildingURN (), priv2.getFogOfWarMemory ().getBuilding ());
		verify (buildingUtils, times (1)).removeBuildingURN (trueBuilding.getBuildingURN (), priv3.getFogOfWarMemory ().getBuilding ());
		verify (buildingUtils, times (1)).removeBuildingURN (trueBuilding.getBuildingURN (), priv4.getFogOfWarMemory ().getBuilding ());

		// Prove that only the human player's client that saw the building disappear was sent update msg
		assertEquals (1, conn3.getMessages ().size ());
		final DestroyBuildingMessage msg = (DestroyBuildingMessage) conn3.getMessages ().get (0);
		assertEquals (trueBuilding.getBuildingURN (), msg.getBuildingURN ());
		assertFalse (msg.isUpdateBuildingSoldThisTurn ());
	}
	
	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, when there's no cities, lairs, nodes, towers invovled
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients () throws Exception
	{
		// Server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Map feature is something irrelevant, like gems
		final MapFeatureSvr mapFeature = new MapFeatureSvr ();
		when (db.findMapFeature ("MF01", "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = ServerTestData.createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID ("MF01");
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		final MapVolumeOfFogOfWarStates fowArea1 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Player who can see the start of their move, but not the end
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (5);
		pd2.setHuman (true);

		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		final MapVolumeOfFogOfWarStates fowArea2 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (fowArea2);
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);

		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea2, db)).thenReturn (true);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea2, db)).thenReturn (false);
		
		// AI player who can see the end of their move, but not the start
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-1);
		pd3.setHuman (false);

		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		final MapVolumeOfFogOfWarStates fowArea3 = new MapVolumeOfFogOfWarStates ();

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (fowArea3);
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);

		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea3, db)).thenReturn (false);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea3, db)).thenReturn (true);
		
		// Player whose seen the units at some point in the past, but can't see them now
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (6);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		final MapVolumeOfFogOfWarStates fowArea4 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWar (fowArea4);
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);

		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea4, db)).thenReturn (false);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea4, db)).thenReturn (false);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		
		// Units being moved
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Players 1, 2 & 4 can see the units before they move
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			fow1.getUnit ().add (mu1);

			final MemoryUnit mu2 = new MemoryUnit ();
			mu2.setUnitURN (n);
			mu2.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			fow2.getUnit ().add (mu2);

			final MemoryUnit mu4 = new MemoryUnit ();
			mu4.setUnitURN (n);
			mu4.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			fow4.getUnit ().add (mu4);
		}
		
		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		
		final FogOfWarDuplicationImpl fowDup = new FogOfWarDuplicationImpl ();
		fowDup.setUnitUtils (unitUtils);
		
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fowCalc);
		midTurn.setFogOfWarProcessing (fowProc);
		midTurn.setFogOfWarDuplication (fowDup);
		midTurn.setUnitUtils (unitUtils);

		// Run method
		midTurn.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, trueMap, sd, db);
		
		// Check player 1
		assertEquals (3, fow1.getUnit ().size ());
		for (final MemoryUnit mu1 : fow1.getUnit ())
			assertEquals (moveTo, mu1.getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// Check player 2
		assertEquals (0, fow2.getUnit ().size ());		// Units removed from player's memory on server

		assertEquals (1, conn2.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg2 = (MoveUnitStackOverlandMessage) conn2.getMessages ().get (0);
		assertTrue (msg2.isFreeAfterMoving ());
		assertEquals (moveFrom, msg2.getMoveFrom ());
		assertEquals (moveTo, msg2.getMoveTo ());
		assertEquals (3, msg2.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg2.getUnitURN ().get (n-1).intValue ());

		// Check player 3
		assertEquals (3, fow3.getUnit ().size ());		// Units added to player's memory on server
		for (final MemoryUnit mu3 : fow3.getUnit ())
			assertEquals (moveTo, mu3.getUnitLocation ());

		// Check player 4
		assertEquals (3, fow4.getUnit ().size ());
		for (final MemoryUnit mu4 : fow4.getUnit ())
			assertEquals (moveFrom, mu4.getUnitLocation ());		// Player still knows about the units, but at their old location, we didn't see them move

		assertEquals (0, conn4.getMessages ().size ());
		
		// The gems are still there
		assertEquals ("MF01", moveToCell.getMapFeatureID ());
	}

	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, moving onto an empty lair.
	 * Note from this method's point of view, its irrelevant whether the lair had monsters in it - we could be coming here from the movement
	 * routine just directly moving onto an empty lair, or could be coming here from the combat routine after successfully clearing out the lair. 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients_MoveToLair () throws Exception
	{
		// Server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Map feature is one that may contain monsters
		final MapFeatureSvr mapFeature = new MapFeatureSvr ();
		mapFeature.getMapFeatureMagicRealm ().add (new MapFeatureMagicRealmSvr ());
		when (db.findMapFeature ("MF01", "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = ServerTestData.createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID ("MF01");
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells terrain1 = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		fow1.setMap (terrain1);
		
		final MapVolumeOfFogOfWarStates fowArea1 = ServerTestData.createFogOfWarArea (overlandMapSize);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		
		// Units being moved
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Players can see the units before they move
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			fow1.getUnit ().add (mu1);
		}

		// Its a lair, not a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (moveToCell)).thenReturn (false);
		
		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		
		final FogOfWarDuplicationImpl fowDup = new FogOfWarDuplicationImpl ();
		fowDup.setUnitUtils (unitUtils);
		
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fowCalc);
		midTurn.setFogOfWarProcessing (fowProc);
		midTurn.setFogOfWarDuplication (fowDup);
		midTurn.setUnitUtils (unitUtils);
		midTurn.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		midTurn.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, trueMap, sd, db);
		
		// Check player 1
		assertEquals (3, fow1.getUnit ().size ());
		for (final MemoryUnit mu1 : fow1.getUnit ())
			assertEquals (moveTo, mu1.getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// The lair is gone
		assertNull (moveToCell.getMapFeatureID ());
	}

	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, moving onto a Tower.
	 * Note from this method's point of view, its irrelevant whether the tower had monsters in it - we could be coming here from the movement
	 * routine just directly moving onto an empty tower, or could be coming here from the combat routine after successfully clearing out the tower. 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients_MoveToTower () throws Exception
	{
		// Server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		final PlaneSvr arcanus = new PlaneSvr ();
		final PlaneSvr myrror = new PlaneSvr ();
		myrror.setPlaneNumber (1);
		
		final List<PlaneSvr> planes = new ArrayList<PlaneSvr> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlanes ()).thenReturn (planes);
		
		// Map feature is one that may contain monsters
		final MapFeatureSvr mapFeature = new MapFeatureSvr ();
		mapFeature.getMapFeatureMagicRealm ().add (new MapFeatureMagicRealmSvr ());
		when (db.findMapFeature (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY, "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = ServerTestData.createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		final OverlandMapTerrainData moveToCellOtherPlane = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (1).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCellOtherPlane);
		moveToCellOtherPlane.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells terrain1 = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		fow1.setMap (terrain1);
		
		final MapVolumeOfFogOfWarStates fowArea1 = ServerTestData.createFogOfWarArea (overlandMapSize);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		
		// Units being moved
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Players can see the units before they move
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			fow1.getUnit ().add (mu1);
		}

		// Its a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (moveToCell)).thenReturn (true);
		
		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		
		final FogOfWarDuplicationImpl fowDup = new FogOfWarDuplicationImpl ();
		fowDup.setUnitUtils (unitUtils);
		
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fowCalc);
		midTurn.setFogOfWarProcessing (fowProc);
		midTurn.setFogOfWarDuplication (fowDup);
		midTurn.setUnitUtils (unitUtils);
		midTurn.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		midTurn.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, trueMap, sd, db);
		
		// Check player 1
		assertEquals (3, fow1.getUnit ().size ());
		for (final MemoryUnit mu1 : fow1.getUnit ())
			assertEquals (moveTo, mu1.getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// The tower is now cleared, on both planes
		assertEquals (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY, moveToCell.getMapFeatureID ());
		assertEquals (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY, moveToCellOtherPlane.getMapFeatureID ());
	}
}