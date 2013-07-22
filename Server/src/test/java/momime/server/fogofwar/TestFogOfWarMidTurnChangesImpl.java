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
import momime.common.database.newgame.v0_9_4.FogOfWarValue;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.AddBuildingMessage;
import momime.common.messages.servertoclient.v0_9_4.DestroyBuildingMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateCityMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateTerrainMessage;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.calculations.MomFogOfWarCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
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
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		
		final OverlandMapTerrainData td = new OverlandMapTerrainData ();
		
		final MemoryGridCell tc = new MemoryGridCell ();
		tc.setTerrainData (td);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().set (20, tc);
		
		// Set up coordinates
		final OverlandMapCoordinatesEx coords = createCoordinates (20);
		
		// Human player who can't see the location
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells map1 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc1 = map1.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		final FogOfWarMemory mem1 = new FogOfWarMemory ();
		mem1.setMap (map1);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (mem1);
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);

		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);

		final MapVolumeOfMemoryGridCells map2 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc2 = map2.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem2 = new FogOfWarMemory ();
		mem2.setMap (map2);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (mem2);
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);

		// Human player who can see the location and whose info is out of date
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MapVolumeOfMemoryGridCells map3 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc3 = map3.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem3 = new FogOfWarMemory ();
		mem3.setMap (map3);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (mem3);
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		players.add (player3);
		
		// AI player who can see the location and whose info is out of date
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (-2);
		pd4.setHuman (false);

		final MapVolumeOfMemoryGridCells map4 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc4 = map4.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem4 = new FogOfWarMemory ();
		mem4.setMap (map4);
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (mem4);
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);

		// Human player who can see the location and already has up to date info
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (8);
		pd5.setHuman (true);

		final MapVolumeOfMemoryGridCells map5 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc5 = map5.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		final FogOfWarMemory mem5 = new FogOfWarMemory ();
		mem5.setMap (map5);
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (mem5);
		priv5.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv5.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		players.add (player5);
		
		// Set up test object
		final MomFogOfWarCalculations single = mock (MomFogOfWarCalculations.class);
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
		assertEquals (1, updateMsg.getData ().getMapLocation ().getPlane ());
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
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		
		final OverlandMapCityData td = new OverlandMapCityData ();
		
		final MemoryGridCell tc = new MemoryGridCell ();
		tc.setCityData (td);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().set (20, tc);
		
		// Set up coordinates
		final OverlandMapCoordinatesEx coords = createCoordinates (20);
		
		// Human player who can't see the location
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells map1 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc1 = map1.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		final FogOfWarMemory mem1 = new FogOfWarMemory ();
		mem1.setMap (map1);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (mem1);
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);

		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);

		final MapVolumeOfMemoryGridCells map2 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc2 = map2.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem2 = new FogOfWarMemory ();
		mem2.setMap (map2);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (mem2);
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);

		// Human player who can see the location and whose info is out of date
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MapVolumeOfMemoryGridCells map3 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc3 = map3.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem3 = new FogOfWarMemory ();
		mem3.setMap (map3);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (mem3);
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		players.add (player3);
		
		// AI player who can see the location and whose info is out of date
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (-2);
		pd4.setHuman (false);

		final MapVolumeOfMemoryGridCells map4 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc4 = map4.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final FogOfWarMemory mem4 = new FogOfWarMemory ();
		mem4.setMap (map4);
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (mem4);
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);

		// Human player who can see the location and already has up to date info
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (8);
		pd5.setHuman (true);

		final MapVolumeOfMemoryGridCells map5 = ServerTestData.createOverlandMap (sd.getMapSize ());
		final MemoryGridCell mc5 = map5.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		final FogOfWarMemory mem5 = new FogOfWarMemory ();
		mem5.setMap (map5);
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (mem5);
		priv5.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv5.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		players.add (player5);
		
		// Set up test object
		final MomFogOfWarCalculations single = mock (MomFogOfWarCalculations.class);
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
		assertEquals (1, updateMsg.getData ().getMapLocation ().getPlane ());
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
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// True terrain
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

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
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);

		final PlayerServerDetails player3 = new PlayerServerDetails (null, null, null, null, null);
		players.add (player3);
		
		// Set up test object
		final MomFogOfWarCalculations single = mock (MomFogOfWarCalculations.class);

		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		
		// The unit we're trying to see
		// Note creating units like this defaults them to ALIVE, so we don't need to set that
		final OverlandMapCoordinatesEx unitLocation = new OverlandMapCoordinatesEx ();
		unitLocation.setX (20);
		unitLocation.setY (10);
		unitLocation.setPlane (0);

		final MemoryUnit spearmen = new UnitUtilsImpl ().createMemoryUnit ("UN105", 1, 0, 0, true, db);
		spearmen.setOwningPlayerID (pd1.getPlayerID ());
		spearmen.setUnitLocation (unitLocation);
		
		// Regular situation of a unit we can't see because we can't see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (unitLocation, sd.getFogOfWarSetting ().getUnits (), trueTerrain, priv2.getFogOfWar (), db)).thenReturn (false);
		assertFalse (calc.canSeeUnitMidTurn (spearmen, players, trueTerrain, player2, null, null, null, db, sd));

		// Regular situation of a unit we can see because we can see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (unitLocation, sd.getFogOfWarSetting ().getUnits (), trueTerrain, priv2.getFogOfWar (), db)).thenReturn (true);
		assertTrue (calc.canSeeUnitMidTurn (spearmen, players, trueTerrain, player2, null, null, null, db, sd));

		// Can't see dead units, even if we can see their location
		spearmen.setStatus (UnitStatusID.DEAD);
		assertFalse (calc.canSeeUnitMidTurn (spearmen, players, trueTerrain, player2, null, null, null, db, sd));
		
		// Rampaging monsters running around map that we can't see because we can't see that location
		spearmen.setStatus (UnitStatusID.ALIVE);
		pub1.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);

		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, sd.getFogOfWarSetting ().getUnits ())).thenReturn (false);
		assertFalse (calc.canSeeUnitMidTurn (spearmen, players, trueTerrain, player2, null, null, null, db, sd));
		
		// Rampaging monsters running around map that we can see because we can see that location
		priv2.getFogOfWar ().getPlane ().get (0).getRow ().get (10).getCell ().set (20,  FogOfWarStateID.CAN_SEE);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, sd.getFogOfWarSetting ().getUnits ())).thenReturn (true);
		assertTrue (calc.canSeeUnitMidTurn (spearmen, players, trueTerrain, player2, null, null, null, db, sd));
		
		// Rampaging monsters that we can't see because they're hiding in a node/lair/tower
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT14");
		trueTerrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		assertFalse (calc.canSeeUnitMidTurn (spearmen, players, trueTerrain, player2, null, null, null, db, sd));
		
		// Still can't see them if someone else is attacking the lair
		final OverlandMapCoordinatesEx unitCombatLocation = new OverlandMapCoordinatesEx ();
		unitCombatLocation.setX (21);
		unitCombatLocation.setY (10);
		unitCombatLocation.setPlane (0);
		spearmen.setCombatLocation (unitCombatLocation);
		
		final OverlandMapCoordinatesEx playerCombatLocation = new OverlandMapCoordinatesEx ();
		playerCombatLocation.setX (21);
		playerCombatLocation.setY (10);
		playerCombatLocation.setPlane (0);
		
		assertFalse (calc.canSeeUnitMidTurn (spearmen, players, trueTerrain, player2, playerCombatLocation, player3, player1, db, sd));
		
		// But can see them if we're attacking it
		assertTrue (calc.canSeeUnitMidTurn (spearmen, players, trueTerrain, player2, playerCombatLocation, player3, player2, db, sd));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on a unit enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_UnitEnchantment () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// True terrain
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

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
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Set up test object
		final MomFogOfWarCalculations single = mock (MomFogOfWarCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setUnitUtils (unitUtils);

		// Spell to check
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setUnitURN (11);
		
		// The unit we're trying to see
		// Note creating units like this defaults them to ALIVE, so we don't need to set that
		final OverlandMapCoordinatesEx unitLocation = new OverlandMapCoordinatesEx ();
		unitLocation.setX (20);
		unitLocation.setY (10);
		unitLocation.setPlane (0);

		final MemoryUnit spearmen = new UnitUtilsImpl ().createMemoryUnit ("UN105", 11, 0, 0, true, db);
		spearmen.setOwningPlayerID (pd1.getPlayerID ());
		spearmen.setUnitLocation (unitLocation);
		
		final List<MemoryUnit> trueUnits = new ArrayList<MemoryUnit> ();
		
		when (unitUtils.findUnitURN (11, trueUnits, "canSeeSpellMidTurn")).thenReturn (spearmen);
		
		// Regular situation of a unit we can't see because we can't see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (unitLocation, sd.getFogOfWarSetting ().getUnits (), trueTerrain, priv2.getFogOfWar (), db)).thenReturn (false);
		assertFalse (calc.canSeeSpellMidTurn (spell, players, trueTerrain, trueUnits, player2, null, null, null, db, sd));

		// Regular situation of a unit we can see because we can see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (unitLocation, sd.getFogOfWarSetting ().getUnits (), trueTerrain, priv2.getFogOfWar (), db)).thenReturn (true);
		assertTrue (calc.canSeeSpellMidTurn (spell, players, trueTerrain, trueUnits, player2, null, null, null, db, sd));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on a city enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_CityEnchantment () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Player who is trying to see it
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		
		final PlayerServerDetails player = new PlayerServerDetails (null, null, priv, null, null);

		// The location of the city that has the spell we're trying to see
		final OverlandMapCoordinatesEx spellLocation = createCoordinates (20);

		// Spell to check
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setCityLocation (spellLocation);

		// Set up test object
		final MomFogOfWarCalculations single = mock (MomFogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (true);

		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		
		// Run test
		assertFalse (calc.canSeeSpellMidTurn (spell, null, null, null, player, null, null, null, db, sd));
		
		priv.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		assertTrue (calc.canSeeSpellMidTurn (spell, null, null, null, player, null, null, null, db, sd));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on an overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_OverlandEnchantment () throws Exception
	{
		// Set up test object
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();

		// Spell to check - assumed to be overland since it has no UnitURN or CityLocation set
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();

		// Run test
		assertTrue (calc.canSeeSpellMidTurn (spell, null, null, null, null, null, null, null, null, null));
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

		// Set up test object
		final MomFogOfWarCalculations fow = mock (MomFogOfWarCalculations.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (fow);
		
		// One cell we can see, another that we can't
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (true);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (false);

		// Set matching states in two locations
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (21, FogOfWarStateID.NEVER_SEEN);
		
		// CAE
		final OverlandMapCoordinatesEx caeLocation = createCoordinates (20);

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
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player who can't see the location
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv1.setFogOfWarMemory (new FogOfWarMemory ());
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);
		
		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (4);
		pd2.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv2.setFogOfWarMemory (new FogOfWarMemory ());
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Human player who can see the location
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (5);
		pd3.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
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
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv4.setFogOfWarMemory (new FogOfWarMemory ());
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (pd3.getPlayerID ());
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = createCoordinates (20);
		
		// Set up test object
		final MomFogOfWarCalculations fow = mock (MomFogOfWarCalculations.class);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (false);
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (true);		
		
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();		// Used real one here, since makes it easier to check the output than via mock
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fow);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setFogOfWarDuplication (dup);
		
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
		assertEquals (cityLocation, msg.getData ().getCityLocation ());
		assertEquals ("BL03", msg.getData ().getFirstBuildingID ());
		assertNull (msg.getData ().getSecondBuildingID ());
		assertNull (msg.getData ().getBuildingCreatedFromSpellID ());
		assertNull (msg.getData ().getBuildingCreationSpellCastByPlayerID ());
	}

	/**
	 * Tests the addBuildingOnServerAndClients method, when its from the move fortress spell, so adds 2 buildings resulting from a spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBuildingOnServerAndClients_MoveFortress () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player who can't see the location
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv1.setFogOfWarMemory (new FogOfWarMemory ());
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);
		
		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (4);
		pd2.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv2.setFogOfWarMemory (new FogOfWarMemory ());
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Human player who can see the location
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (5);
		pd3.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
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
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv4.setFogOfWarMemory (new FogOfWarMemory ());
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (pd3.getPlayerID ());
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = createCoordinates (20);
		
		// Set up test object
		final MomFogOfWarCalculations fow = mock (MomFogOfWarCalculations.class);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (false);
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (true);		
		
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();		// Used real one here, since makes it easier to check the output than via mock
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fow);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setFogOfWarDuplication (dup);
		
		// Run test
		midTurn.addBuildingOnServerAndClients (gsk, players, cityLocation, CommonDatabaseConstants.VALUE_BUILDING_FORTRESS,
			CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, "SP028", 7, sd, db);
		
		// Prove that building got added to server's true map
		assertEquals (2, trueMap.getBuilding ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, trueMap.getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, trueMap.getBuilding ().get (0).getCityLocation ());
		assertEquals (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, trueMap.getBuilding ().get (1).getBuildingID ());
		assertEquals (cityLocation, trueMap.getBuilding ().get (1).getCityLocation ());
		
		// Prove that building got added to server's copy of each player's memory, but only the players who can see it
		assertEquals (0, priv1.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals (0, priv2.getFogOfWarMemory ().getBuilding ().size ());
		
		assertEquals (2, priv3.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, priv3.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, priv3.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
		assertEquals (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, priv3.getFogOfWarMemory ().getBuilding ().get (1).getBuildingID ());
		assertEquals (cityLocation, priv3.getFogOfWarMemory ().getBuilding ().get (1).getCityLocation ());
		
		assertEquals (2, priv4.getFogOfWarMemory ().getBuilding ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, priv4.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
		assertEquals (cityLocation, priv4.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
		assertEquals (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, priv4.getFogOfWarMemory ().getBuilding ().get (1).getBuildingID ());
		assertEquals (cityLocation, priv4.getFogOfWarMemory ().getBuilding ().get (1).getCityLocation ());
		
		// Prove that human player's client was sent update msg
		assertEquals (1, conn3.getMessages ().size ());
		final AddBuildingMessage msg = (AddBuildingMessage) conn3.getMessages ().get (0);
		assertEquals (cityLocation, msg.getData ().getCityLocation ());
		assertEquals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, msg.getData ().getFirstBuildingID ());
		assertEquals (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, msg.getData ().getSecondBuildingID ());
		assertEquals ("SP028", msg.getData ().getBuildingCreatedFromSpellID ());
		assertEquals (7, msg.getData ().getBuildingCreationSpellCastByPlayerID ().intValue ());
	}
	
	/**
	 * Tests the destroyBuildingOnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDestroyBuildingOnServerAndClients () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		final MemoryBuilding trueBuilding = new MemoryBuilding ();		// The building being destroyed
		trueBuilding.setBuildingID ("BL03");
		trueBuilding.setCityLocation (createCoordinates (20));
		trueMap.getBuilding ().add (trueBuilding);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player who can't see the location
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv1.setFogOfWarMemory (new FogOfWarMemory ());
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final MemoryBuilding building1 = new MemoryBuilding ();		// Some other building, just to make the building lists unique
		building1.setBuildingID ("BL04");
		building1.setCityLocation (createCoordinates (21));
		priv1.getFogOfWarMemory ().getBuilding ().add (building1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);
		
		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (4);
		pd2.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv2.setFogOfWarMemory (new FogOfWarMemory ());
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);

		final MemoryBuilding building2 = new MemoryBuilding ();		// Some other building, just to make the building lists unique
		building2.setBuildingID ("BL04");
		building2.setCityLocation (createCoordinates (21));
		priv2.getFogOfWarMemory ().getBuilding ().add (building2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Human player who can see the location
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (5);
		pd3.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv3.setFogOfWarMemory (new FogOfWarMemory ());
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final MemoryBuilding building3 = new MemoryBuilding ();		// The building being destroyed
		building3.setBuildingID ("BL03");
		building3.setCityLocation (createCoordinates (20));
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
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv4.setFogOfWarMemory (new FogOfWarMemory ());
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final MemoryBuilding building4 = new MemoryBuilding ();		// The building being destroyed
		building4.setBuildingID ("BL03");
		building4.setCityLocation (createCoordinates (20));
		priv4.getFogOfWarMemory ().getBuilding ().add (building4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (pd3.getPlayerID ());
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = createCoordinates (20);

		// Set up test object
		final MomFogOfWarCalculations fow = mock (MomFogOfWarCalculations.class);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (false);
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (true);		

		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class); 
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fow);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setMemoryBuildingUtils (buildingUtils);
		
		// Run test
		midTurn.destroyBuildingOnServerAndClients (trueMap, players, cityLocation, "BL03", false, sd, db);
		
		// Prove that building got removed from server's true map
		verify (buildingUtils, times (1)).destroyBuilding (trueMap.getBuilding (), cityLocation, "BL03");
		
		// Prove that building got removed from server's copy of each player's memory, but only the players who saw it disappear
		verify (buildingUtils, times (0)).destroyBuilding (priv1.getFogOfWarMemory ().getBuilding (), cityLocation, "BL03");
		verify (buildingUtils, times (0)).destroyBuilding (priv2.getFogOfWarMemory ().getBuilding (), cityLocation, "BL03");
		verify (buildingUtils, times (1)).destroyBuilding (priv3.getFogOfWarMemory ().getBuilding (), cityLocation, "BL03");
		verify (buildingUtils, times (1)).destroyBuilding (priv4.getFogOfWarMemory ().getBuilding (), cityLocation, "BL03");

		// Prove that only the human player's client that saw the building disappear was sent update msg
		assertEquals (1, conn3.getMessages ().size ());
		final DestroyBuildingMessage msg = (DestroyBuildingMessage) conn3.getMessages ().get (0);
		assertEquals (cityLocation, msg.getData ().getCityLocation ());
		assertEquals ("BL03", msg.getData ().getBuildingID ());
		assertFalse (msg.getData ().isUpdateBuildingSoldThisTurn ());
	}

	/**
	 * Tests the destroyAllBuildingsInLocationOnServerAndClients method - this is basically a copy of the destroyBuildingOnServerAndClients test
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDestroyAllBuildingsInLocationOnServerAndClients () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		final MemoryBuilding trueBuilding = new MemoryBuilding ();		// The building being destroyed
		trueBuilding.setBuildingID ("BL03");
		trueBuilding.setCityLocation (createCoordinates (20));
		trueMap.getBuilding ().add (trueBuilding);

		final MemoryBuilding otherBuilding = new MemoryBuilding ();		// Some building in another location so it doesn't get destroyed
		otherBuilding.setBuildingID ("BL04");
		otherBuilding.setCityLocation (createCoordinates (21));
		trueMap.getBuilding ().add (otherBuilding);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player who can't see the location
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv1.setFogOfWarMemory (new FogOfWarMemory ());
		priv1.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		
		final MemoryBuilding building1 = new MemoryBuilding ();		// Some other building, just to make the building lists unique
		building1.setBuildingID ("BL04");
		building1.setCityLocation (createCoordinates (21));
		priv1.getFogOfWarMemory ().getBuilding ().add (building1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		players.add (player1);
		
		// AI player who can't see the location
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (4);
		pd2.setHuman (false);
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv2.setFogOfWarMemory (new FogOfWarMemory ());
		priv2.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);

		final MemoryBuilding building2 = new MemoryBuilding ();		// Some other building, just to make the building lists unique
		building2.setBuildingID ("BL04");
		building2.setCityLocation (createCoordinates (21));
		priv2.getFogOfWarMemory ().getBuilding ().add (building2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		players.add (player2);
		
		// Human player who can see the location
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (5);
		pd3.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv3.setFogOfWarMemory (new FogOfWarMemory ());
		priv3.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final MemoryBuilding building3 = new MemoryBuilding ();		// The building being destroyed
		building3.setBuildingID ("BL03");
		building3.setCityLocation (createCoordinates (20));
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
		priv4.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		priv4.setFogOfWarMemory (new FogOfWarMemory ());
		priv4.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		final MemoryBuilding building4 = new MemoryBuilding ();		// The building being destroyed
		building4.setBuildingID ("BL03");
		building4.setCityLocation (createCoordinates (20));
		priv4.getFogOfWarMemory ().getBuilding ().add (building4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		players.add (player4);
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (pd3.getPlayerID ());
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = createCoordinates (20);

		// Set up test object
		final MomFogOfWarCalculations fow = mock (MomFogOfWarCalculations.class);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (false);
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ())).thenReturn (true);		

		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class); 
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (fow);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setMemoryBuildingUtils (buildingUtils);
		
		// Run test
		midTurn.destroyAllBuildingsInLocationOnServerAndClients (trueMap, players, cityLocation, sd, db);
		
		// Prove that building got removed from server's true map
		verify (buildingUtils, times (1)).destroyBuilding (trueMap.getBuilding (), cityLocation, "BL03");
		
		// Prove that building got removed from server's copy of each player's memory, but only the players who saw it disappear
		verify (buildingUtils, times (0)).destroyBuilding (priv1.getFogOfWarMemory ().getBuilding (), cityLocation, "BL03");
		verify (buildingUtils, times (0)).destroyBuilding (priv2.getFogOfWarMemory ().getBuilding (), cityLocation, "BL03");
		verify (buildingUtils, times (1)).destroyBuilding (priv3.getFogOfWarMemory ().getBuilding (), cityLocation, "BL03");
		verify (buildingUtils, times (1)).destroyBuilding (priv4.getFogOfWarMemory ().getBuilding (), cityLocation, "BL03");

		// Prove that only the human player's client that saw the building disappear was sent update msg
		assertEquals (1, conn3.getMessages ().size ());
		final DestroyBuildingMessage msg = (DestroyBuildingMessage) conn3.getMessages ().get (0);
		assertEquals (cityLocation, msg.getData ().getCityLocation ());
		assertEquals ("BL03", msg.getData ().getBuildingID ());
		assertFalse (msg.getData ().isUpdateBuildingSoldThisTurn ());
	}

	/**
	 * Just to save repeating this a dozen times in the test cases
	 * @param x X coord
	 * @return Coordinates object
	 */
	private final OverlandMapCoordinatesEx createCoordinates (final int x)
	{
		final OverlandMapCoordinatesEx combatLocation = new OverlandMapCoordinatesEx ();
		combatLocation.setX (x);
		combatLocation.setY (10);
		combatLocation.setPlane (1);
		return combatLocation;
	}
}
