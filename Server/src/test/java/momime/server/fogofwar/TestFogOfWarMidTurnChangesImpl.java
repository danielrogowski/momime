package momime.server.fogofwar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.OverlandMapSize;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddBuildingMessage;
import momime.common.messages.servertoclient.AddMaintainedSpellMessage;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.messages.servertoclient.KillUnitMessage;
import momime.common.messages.servertoclient.SwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.UpdateCityMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the FogOfWarMidTurnChangesImpl class
 */
public final class TestFogOfWarMidTurnChangesImpl extends ServerTestData
{
	/**
	 * Tests the updatePlayerMemoryOfTerrain method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUpdatePlayerMemoryOfTerrain () throws Exception
	{
		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// True terrain
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		final MemoryGridCell tc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapTerrainData td = new OverlandMapTerrainData ();
		tc.setTerrainData (td);
		
		// Players can see the location or not, have up to date info already or not, and be human/AI, so create 8 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarDuplication dup = mock (FogOfWarDuplication.class);
		
		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean upToDateInfo : new boolean [] {false, true})
				for (final boolean human : new boolean [] {false, true})
				{
					playerID++;
					
					final PlayerDescription pd = new PlayerDescription ();
					pd.setPlayerID (playerID);
					pd.setHuman (human);
					
					// Mock whether the player can see the location
					final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
					vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);
					
					final FogOfWarMemory fow = new FogOfWarMemory ();
					fow.setMap (createOverlandMap (sys));
					
					final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
					priv.setFogOfWar (vis);
					priv.setFogOfWarMemory (fow);
					
					final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
					if (human)
						player.setConnection (new DummyServerToClientConnection ());
					
					players.add (player);
					
					// Mock whether the player has up to date info for this terrain already or not
					final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
					when (dup.copyTerrainAndNodeAura (tc, mc)).thenReturn (!upToDateInfo);
				}

		// Set up object to test
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setFogOfWarDuplication (dup);

		// Run test
		calc.updatePlayerMemoryOfTerrain (trueTerrain, players, new MapCoordinates3DEx (20, 10, 1), fowSetting);
		
		// Players 1-4 can't even see the location so shouldn't get the dup method called; players 5-8 should
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
			verify (dup, times (playerIndex < 4 ? 0 : 1)).copyTerrainAndNodeAura (tc, mc);
		}
		
		// Only player 6 (can see location, out of date info, human) should get a message
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 5)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final UpdateTerrainMessage updateMsg = (UpdateTerrainMessage) conn.getMessages ().get (0);
					assertEquals (20, updateMsg.getData ().getMapLocation ().getX ());
					assertEquals (10, updateMsg.getData ().getMapLocation ().getY ());
					assertEquals (1, updateMsg.getData ().getMapLocation ().getZ ());
					assertSame (td, updateMsg.getData ().getTerrainData ());
				}
			}
		}
	}
	
	/**
	 * Tests the updatePlayerMemoryOfCity method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUpdatePlayerMemoryOfCity () throws Exception
	{
		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setCitiesSpellsAndCombatAreaEffects (fowSetting);
		
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// True terrain
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		final MemoryGridCell tc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		// Players can see the location or not, have up to date info already or not, and be human/AI, so create 8 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarDuplication dup = mock (FogOfWarDuplication.class);
		
		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean upToDateInfo : new boolean [] {false, true})
				for (final boolean human : new boolean [] {false, true})
				{
					playerID++;
					
					final PlayerDescription pd = new PlayerDescription ();
					pd.setPlayerID (playerID);
					pd.setHuman (human);
					
					// Mock whether the player can see the location
					final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
					vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);

					final MapVolumeOfMemoryGridCells terrain = createOverlandMap (sys);
					final OverlandMapCityData playerCityData = new OverlandMapCityData ();
					terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (playerCityData);
					
					final FogOfWarMemory fow = new FogOfWarMemory ();
					fow.setMap (terrain);
					
					final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
					priv.setFogOfWar (vis);
					priv.setFogOfWarMemory (fow);
					
					final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
					if (human)
						player.setConnection (new DummyServerToClientConnection ());
					
					players.add (player);
					
					// Mock whether the player has up to date info for this terrain already or not
					final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
					when (dup.copyCityData (tc, mc, fowSettings.isSeeEnemyCityConstruction (), false)).thenReturn (!upToDateInfo);
				}
		
		// Set up object to test
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setFogOfWarDuplication (dup);

		// Run test
		calc.updatePlayerMemoryOfCity (trueTerrain, players, new MapCoordinates3DEx (20, 10, 1), fowSettings);
		
		// Players 1-4 can't even see the location so shouldn't get the dup method called; players 5-8 should
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
			verify (dup, times (playerIndex < 4 ? 0 : 1)).copyCityData (tc, mc, fowSettings.isSeeEnemyCityConstruction (), false);
		}
		
		// Only player 6 (can see location, out of date info, human) should get a message
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 5)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
					final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
					
					final UpdateCityMessage updateMsg = (UpdateCityMessage) conn.getMessages ().get (0);
					assertEquals (20, updateMsg.getData ().getMapLocation ().getX ());
					assertEquals (10, updateMsg.getData ().getMapLocation ().getY ());
					assertEquals (1, updateMsg.getData ().getMapLocation ().getZ ());
					assertSame (mc.getCityData (), updateMsg.getData ().getCityData ());
				}
			}
		}
	}

	/**
	 * Tests the killUnitOnServerAndClients method, on a normal being killed overland by mainly healable damage.
	 * It should just be removed in all lists (server, server copy of player memory, and client copy of player memory).
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testKillUnitOnServerAndClients_OverlandDamage_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "killUnitOnServerAndClients")).thenReturn (unitDef);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();

		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player1, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Another human player who can see the unit
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		
		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player2, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		// An AI player who can see the unit
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-3);
		pd3.setHuman (false);
		
		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player3, db, fowSettings)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player4, db, fowSettings)).thenReturn (false);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);
		
		// An AI player who can't see the unit
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (-5);
		pd5.setHuman (false);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player5, db, fowSettings)).thenReturn (false);

		// List of players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		players.add (player5);
		
		// Fiddle each player's unit lists in their server memory to make them unique from each other and the true units list
		int count = 0;
		for (final PlayerServerDetails player : players)
		{
			count++;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			for (int n = 0; n < count; n++)
			{
				priv.getFogOfWarMemory ().getUnit ().add (null);
				priv.getPendingMovement ().add (null);
			}
		}
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarMidTurnVisibility (midTurn);
		calc.setUnitUtils (unitUtils);
		calc.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		calc.killUnitOnServerAndClients (tu, KillUnitActionID.HEALABLE_OVERLAND_DAMAGE, trueMap, players, fowSettings, db);
		
		// Check results
		verify (unitUtils, times (1)).beforeKillingUnit (trueMap, tu.getUnitURN ());
		
		// Check was removed on server's true map details
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.ALIVE, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertNull (msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow4.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv4.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (0, conn4.getMessages ().size ());
		
		// Check an AI player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
	}
	
	/**
	 * Tests the killUnitOnServerAndClients method, on a hero being killed overland by mainly healable damage.
	 * It should be retained in the server's true list and the owner's memory both on the server and client as DEAD, but for everybody else completedly removed.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testKillUnitOnServerAndClients_OverlandDamage_Hero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN001", "killUnitOnServerAndClients")).thenReturn (unitDef);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();

		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player1, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Another human player who can see the unit
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		
		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player2, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		// An AI player who can see the unit
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-3);
		pd3.setHuman (false);
		
		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player3, db, fowSettings)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player4, db, fowSettings)).thenReturn (false);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);
		
		// An AI player who can't see the unit
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (-5);
		pd5.setHuman (false);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player5, db, fowSettings)).thenReturn (false);

		// List of players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		players.add (player5);
		
		// Fiddle each player's unit lists in their server memory to make them unique from each other and the true units list
		int count = 0;
		for (final PlayerServerDetails player : players)
		{
			count++;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			for (int n = 0; n < count; n++)
			{
				priv.getFogOfWarMemory ().getUnit ().add (null);
				priv.getPendingMovement ().add (null);
			}
		}
		
		// Player's memory of units on server
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final MemoryUnit mu1 = new MemoryUnit ();
		mu1.setOwningPlayerID (tu.getOwningPlayerID ());
		mu1.setUnitURN (tu.getUnitURN ());
		mu1.setStatus (tu.getStatus ());
		mu1.setUnitID (tu.getUnitID ());
		when (unitUtils.findUnitURN (mu1.getUnitURN (), fow1.getUnit (), "killUnitOnServerAndClients")).thenReturn (mu1);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarMidTurnVisibility (midTurn);
		calc.setUnitUtils (unitUtils);
		calc.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		calc.killUnitOnServerAndClients (tu, KillUnitActionID.HEALABLE_OVERLAND_DAMAGE, trueMap, players, fowSettings, db);
		
		// Check results
		verify (unitUtils, times (1)).beforeKillingUnit (trueMap, tu.getUnitURN ());
		
		// Check was set to DEAD on server's true map details
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.DEAD, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		assertEquals (UnitStatusID.DEAD, mu1.getStatus ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.DEAD, msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow4.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv4.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (0, conn4.getMessages ().size ());
		
		// Check an AI player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
	}
	
	/**
	 * Tests the killUnitOnServerAndClients method, on a normal unit being dismissed (on the overland map).
	 * It should just be removed in all lists (server, server copy of player memory, and client copy of player memory).
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testKillUnitOnServerAndClients_Dismiss_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "killUnitOnServerAndClients")).thenReturn (unitDef);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();

		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player1, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Another human player who can see the unit
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		
		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player2, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		// An AI player who can see the unit
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-3);
		pd3.setHuman (false);
		
		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player3, db, fowSettings)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player4, db, fowSettings)).thenReturn (false);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);
		
		// An AI player who can't see the unit
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (-5);
		pd5.setHuman (false);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player5, db, fowSettings)).thenReturn (false);

		// List of players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		players.add (player5);
		
		// Fiddle each player's unit lists in their server memory to make them unique from each other and the true units list
		int count = 0;
		for (final PlayerServerDetails player : players)
		{
			count++;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			for (int n = 0; n < count; n++)
			{
				priv.getFogOfWarMemory ().getUnit ().add (null);
				priv.getPendingMovement ().add (null);
			}
		}
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarMidTurnVisibility (midTurn);
		calc.setUnitUtils (unitUtils);
		calc.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		calc.killUnitOnServerAndClients (tu, KillUnitActionID.DISMISS, trueMap, players, fowSettings, db);
		
		// Check results
		verify (unitUtils, times (1)).beforeKillingUnit (trueMap, tu.getUnitURN ());
		
		// Check was removed on server's true map details
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.ALIVE, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertNull (msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow4.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv4.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (0, conn4.getMessages ().size ());
		
		// Check an AI player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
	}
	
	/**
	 * Tests the killUnitOnServerAndClients method, on a hero being dismissed (on the overland map).
	 * It should be removed from the player memory lists (on both server and client) but set back to GENERATED in the master server list.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testKillUnitOnServerAndClients_Dismiss_Hero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN001", "killUnitOnServerAndClients")).thenReturn (unitDef);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();

		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player1, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Another human player who can see the unit
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		
		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player2, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		// An AI player who can see the unit
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-3);
		pd3.setHuman (false);
		
		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player3, db, fowSettings)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player4, db, fowSettings)).thenReturn (false);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);
		
		// An AI player who can't see the unit
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (-5);
		pd5.setHuman (false);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player5, db, fowSettings)).thenReturn (false);

		// List of players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		players.add (player5);
		
		// Fiddle each player's unit lists in their server memory to make them unique from each other and the true units list
		int count = 0;
		for (final PlayerServerDetails player : players)
		{
			count++;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			for (int n = 0; n < count; n++)
			{
				priv.getFogOfWarMemory ().getUnit ().add (null);
				priv.getPendingMovement ().add (null);
			}
		}
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarMidTurnVisibility (midTurn);
		calc.setUnitUtils (unitUtils);
		calc.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		calc.killUnitOnServerAndClients (tu, KillUnitActionID.DISMISS, trueMap, players, fowSettings, db);
		
		// Check results
		verify (unitUtils, times (1)).beforeKillingUnit (trueMap, tu.getUnitURN ());
		
		// Check was set back to GENERATED on server's true map details
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.GENERATED, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertNull (msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow4.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv4.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (0, conn4.getMessages ().size ());
		
		// Check an AI player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
	}

	/**
	 * Tests the killUnitOnServerAndClients method, on a normal unit being lost due to lack of production (on the overland map).
	 * It should just be removed in both server side list (master, and server copy of player memory) but sent to client as the special KILLED_BY_LACK_OF_PRODUCTION value.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testKillUnitOnServerAndClients_LackOfProduction_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "killUnitOnServerAndClients")).thenReturn (unitDef);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();

		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player1, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Another human player who can see the unit
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		
		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player2, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		// An AI player who can see the unit
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-3);
		pd3.setHuman (false);
		
		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player3, db, fowSettings)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player4, db, fowSettings)).thenReturn (false);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);
		
		// An AI player who can't see the unit
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (-5);
		pd5.setHuman (false);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player5, db, fowSettings)).thenReturn (false);

		// List of players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		players.add (player5);
		
		// Fiddle each player's unit lists in their server memory to make them unique from each other and the true units list
		int count = 0;
		for (final PlayerServerDetails player : players)
		{
			count++;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			for (int n = 0; n < count; n++)
			{
				priv.getFogOfWarMemory ().getUnit ().add (null);
				priv.getPendingMovement ().add (null);
			}
		}
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarMidTurnVisibility (midTurn);
		calc.setUnitUtils (unitUtils);
		calc.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		calc.killUnitOnServerAndClients (tu, KillUnitActionID.LACK_OF_PRODUCTION, trueMap, players, fowSettings, db);
		
		// Check results
		verify (unitUtils, times (1)).beforeKillingUnit (trueMap, tu.getUnitURN ());
		
		// Check was removed on server's true map details
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.ALIVE, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.KILLED_BY_LACK_OF_PRODUCTION, msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow4.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv4.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (0, conn4.getMessages ().size ());
		
		// Check an AI player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
	}
	
	/**
	 * Tests the killUnitOnServerAndClients method, on a hero unit being lost due to lack of production (on the overland map).
	 * It should just be removed in server copy of player's memory, set to GENERATED in the master server list, and sent to client as the special KILLED_BY_LACK_OF_PRODUCTION value.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testKillUnitOnServerAndClients_LackOfProduction_Hero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN001", "killUnitOnServerAndClients")).thenReturn (unitDef);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();

		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player1, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Another human player who can see the unit
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		
		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player2, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		// An AI player who can see the unit
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-3);
		pd3.setHuman (false);
		
		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player3, db, fowSettings)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player4, db, fowSettings)).thenReturn (false);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);
		
		// An AI player who can't see the unit
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (-5);
		pd5.setHuman (false);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player5, db, fowSettings)).thenReturn (false);

		// List of players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		players.add (player5);
		
		// Fiddle each player's unit lists in their server memory to make them unique from each other and the true units list
		int count = 0;
		for (final PlayerServerDetails player : players)
		{
			count++;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			for (int n = 0; n < count; n++)
			{
				priv.getFogOfWarMemory ().getUnit ().add (null);
				priv.getPendingMovement ().add (null);
			}
		}
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarMidTurnVisibility (midTurn);
		calc.setUnitUtils (unitUtils);
		calc.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		calc.killUnitOnServerAndClients (tu, KillUnitActionID.LACK_OF_PRODUCTION, trueMap, players, fowSettings, db);
		
		// Check results
		verify (unitUtils, times (1)).beforeKillingUnit (trueMap, tu.getUnitURN ());
		
		// Check was removed on server's true map details
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.GENERATED, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.KILLED_BY_LACK_OF_PRODUCTION, msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow4.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv4.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (0, conn4.getMessages ().size ());
		
		// Check an AI player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
	}

	/**
	 * Tests the killUnitOnServerAndClients method, on a normal being killed in combat by mainly healable damage.
	 * It can potentially be the target of a raise or animate dead spell by either player involved in the combat, so must be set to DEAD in the master server list,
	 * as well as the server and client side lists of the two players involved in the combat, but removed entirely for any 3rd party observers of the combat.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testKillUnitOnServerAndClients_CombatDamage_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "killUnitOnServerAndClients")).thenReturn (unitDef);

		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		final CoordinateSystem mapSize = createOverlandMapCoordinateSystem ();

		// True map details on server
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (mapSize);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setAttackingPlayerID (1);
		gc.setDefendingPlayerID (2);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Unit to kill
		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player1, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// The other human player who they are in combat with
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		
		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player2, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		// A human player who can't see the unit
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (3);
		pd3.setHuman (true);
		
		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player3, db, fowSettings)).thenReturn (false);
		
		final DummyServerToClientConnection conn3 = new DummyServerToClientConnection ();
		player3.setConnection (conn3);
		
		// An AI player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (-4);
		pd4.setHuman (false);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player4, db, fowSettings)).thenReturn (false);

		// A human player who is a 3rd party observer who can see the unit from outside of the combat
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (5);
		pd5.setHuman (true);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player5, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn5 = new DummyServerToClientConnection ();
		player5.setConnection (conn5);

		// An AI player who is a 3rd party observer who can see the unit from outside of the combat
		final PlayerDescription pd6 = new PlayerDescription ();
		pd6.setPlayerID (-6);
		pd6.setHuman (false);
		
		final FogOfWarMemory fow6 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv6 = new MomPersistentPlayerPrivateKnowledge ();
		priv6.setFogOfWarMemory (fow6);
		
		final PlayerServerDetails player6 = new PlayerServerDetails (pd6, null, priv6, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player6, db, fowSettings)).thenReturn (true);
		
		// List of players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		players.add (player5);
		players.add (player6);
		
		// Fiddle each player's unit lists in their server memory to make them unique from each other and the true units list
		int count = 0;
		for (final PlayerServerDetails player : players)
		{
			count++;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			for (int n = 0; n < count; n++)
			{
				priv.getFogOfWarMemory ().getUnit ().add (null);
				priv.getPendingMovement ().add (null);
			}
		}

		// Player's memory of units on server
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final MemoryUnit mu1 = new MemoryUnit ();
		mu1.setOwningPlayerID (tu.getOwningPlayerID ());
		mu1.setUnitURN (tu.getUnitURN ());
		mu1.setStatus (tu.getStatus ());
		mu1.setUnitID (tu.getUnitID ());
		mu1.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) tu.getCombatLocation ()));
		when (unitUtils.findUnitURN (mu1.getUnitURN (), fow1.getUnit (), "killUnitOnServerAndClients")).thenReturn (mu1);
		
		final MemoryUnit mu2 = new MemoryUnit ();
		mu2.setOwningPlayerID (tu.getOwningPlayerID ());
		mu2.setUnitURN (tu.getUnitURN ());
		mu2.setStatus (tu.getStatus ());
		mu2.setUnitID (tu.getUnitID ());
		mu2.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) tu.getCombatLocation ()));
		when (unitUtils.findUnitURN (mu2.getUnitURN (), fow2.getUnit (), "killUnitOnServerAndClients")).thenReturn (mu2);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarMidTurnVisibility (midTurn);
		calc.setUnitUtils (unitUtils);
		calc.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		calc.killUnitOnServerAndClients (tu, KillUnitActionID.HEALABLE_COMBAT_DAMAGE, trueMap, players, fowSettings, db);
		
		// Check results
		verify (unitUtils, times (1)).beforeKillingUnit (trueMap, tu.getUnitURN ());
		
		// Check was set to DEAD on server's true map details rather than completely removed
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.DEAD, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		assertEquals (UnitStatusID.DEAD, mu1.getStatus ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.DEAD, msg1.getNewStatus ());

		// Check the other human player who they are in combat with
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		assertEquals (UnitStatusID.DEAD, mu2.getStatus ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertEquals (UnitStatusID.DEAD, msg2.getNewStatus ());
		
		// Check a human player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (0, conn3.getMessages ().size ());
		
		// Check an AI player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow4.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv4.getPendingMovement (), tu.getUnitURN ());

		// Check a human player who is a 3rd party observer who can see the unit from outside of the combat
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn5.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn5.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg5 = (KillUnitMessage) conn5.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg5.getUnitURN ());
		assertNull (msg5.getNewStatus ());

		// Check an AI player who is a 3rd party observer who can see the unit from outside of the combat
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow6.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv6.getPendingMovement (), tu.getUnitURN ());
	}
	
	/**
	 * Tests the killUnitOnServerAndClients method, on a hero being killed in combat by mainly healable damage.
	 * It can potentially be the target of a raise dead spell by the unit owner, so must be set to DEAD in the master server list,
	 * as well as the server and client side lists of the unit owner, but removed entirely for everybody else, including the other player involved in the combat.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testKillUnitOnServerAndClients_CombatDamage_Hero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN001", "killUnitOnServerAndClients")).thenReturn (unitDef);

		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		final CoordinateSystem mapSize = createOverlandMapCoordinateSystem ();

		// True map details on server
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (mapSize);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setAttackingPlayerID (1);
		gc.setDefendingPlayerID (2);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Unit to kill
		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player1, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// The other human player who they are in combat with
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setHuman (true);
		
		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player2, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		// A human player who can't see the unit
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (3);
		pd3.setHuman (true);
		
		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player3, db, fowSettings)).thenReturn (false);
		
		final DummyServerToClientConnection conn3 = new DummyServerToClientConnection ();
		player3.setConnection (conn3);
		
		// An AI player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (-4);
		pd4.setHuman (false);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player4, db, fowSettings)).thenReturn (false);

		// A human player who is a 3rd party observer who can see the unit from outside of the combat
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (5);
		pd5.setHuman (true);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player5, db, fowSettings)).thenReturn (true);
		
		final DummyServerToClientConnection conn5 = new DummyServerToClientConnection ();
		player5.setConnection (conn5);

		// An AI player who is a 3rd party observer who can see the unit from outside of the combat
		final PlayerDescription pd6 = new PlayerDescription ();
		pd6.setPlayerID (-6);
		pd6.setHuman (false);
		
		final FogOfWarMemory fow6 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv6 = new MomPersistentPlayerPrivateKnowledge ();
		priv6.setFogOfWarMemory (fow6);
		
		final PlayerServerDetails player6 = new PlayerServerDetails (pd6, null, priv6, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, trueMap.getMap (), player6, db, fowSettings)).thenReturn (true);
		
		// List of players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		players.add (player5);
		players.add (player6);
		
		// Fiddle each player's unit lists in their server memory to make them unique from each other and the true units list
		int count = 0;
		for (final PlayerServerDetails player : players)
		{
			count++;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			for (int n = 0; n < count; n++)
			{
				priv.getFogOfWarMemory ().getUnit ().add (null);
				priv.getPendingMovement ().add (null);
			}
		}

		// Player's memory of units on server
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final MemoryUnit mu1 = new MemoryUnit ();
		mu1.setOwningPlayerID (tu.getOwningPlayerID ());
		mu1.setUnitURN (tu.getUnitURN ());
		mu1.setStatus (tu.getStatus ());
		mu1.setUnitID (tu.getUnitID ());
		mu1.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) tu.getCombatLocation ()));
		when (unitUtils.findUnitURN (mu1.getUnitURN (), fow1.getUnit (), "killUnitOnServerAndClients")).thenReturn (mu1);
		
		final MemoryUnit mu2 = new MemoryUnit ();
		mu2.setOwningPlayerID (tu.getOwningPlayerID ());
		mu2.setUnitURN (tu.getUnitURN ());
		mu2.setStatus (tu.getStatus ());
		mu2.setUnitID (tu.getUnitID ());
		mu2.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) tu.getCombatLocation ()));
		when (unitUtils.findUnitURN (mu2.getUnitURN (), fow2.getUnit (), "killUnitOnServerAndClients")).thenReturn (mu2);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarMidTurnVisibility (midTurn);
		calc.setUnitUtils (unitUtils);
		calc.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		calc.killUnitOnServerAndClients (tu, KillUnitActionID.HEALABLE_COMBAT_DAMAGE, trueMap, players, fowSettings, db);
		
		// Check results
		verify (unitUtils, times (1)).beforeKillingUnit (trueMap, tu.getUnitURN ());
		
		// Check was set to DEAD on server's true map details rather than completely removed
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.DEAD, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		assertEquals (UnitStatusID.DEAD, mu1.getStatus ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.DEAD, msg1.getNewStatus ());

		// Check the other human player who they are in combat with
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check a human player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (0, conn3.getMessages ().size ());
		
		// Check an AI player who can't see the unit
		verify (unitUtils, times (0)).removeUnitURN (tu.getUnitURN (), fow4.getUnit ());
		verify (pendingMovementUtils, times (0)).removeUnitFromAnyPendingMoves (priv4.getPendingMovement (), tu.getUnitURN ());

		// Check a human player who is a 3rd party observer who can see the unit from outside of the combat
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn5.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn5.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg5 = (KillUnitMessage) conn5.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg5.getUnitURN ());
		assertNull (msg5.getNewStatus ());

		// Check an AI player who is a 3rd party observer who can see the unit from outside of the combat
		verify (unitUtils, times (1)).removeUnitURN (tu.getUnitURN (), fow6.getUnit ());
		verify (pendingMovementUtils, times (1)).removeUnitFromAnyPendingMoves (priv6.getPendingMovement (), tu.getUnitURN ());
	}

	/*
	 * NB. There is an additional call to killUnitOnServerAndClients from purgeDeadUnitsAndCombatSummonsFromCombat but it doesn't really
	 * warrant a unit test.  It gets called with FREE, but bear in mind the unit is already marked as DEAD for the players involved in the combat
	 * and hence canSeeUnitMidTurn (which is what the behaviour of killUnitOnServerAndClients hinges on) will always return false and hence
	 * never touch players' memory - that is the entire reason for the custom killing code in purgeDeadUnitsAndCombatSummonsFromCombat
	 * being done outside of killUnitOnServerAndClients to begin with.
	 * 
	 * So in that case, all that happens on the actual call to killUnitOnServerAndClients is that the unit is permanently removed from the
	 * server's true unit list, and nothing else, since no player can "see" the dead unit.
	 */
	
	/**
	 * Tests the addExistingTrueMaintainedSpellToClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddExistingTrueMaintainedSpellToClients () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		
		// Server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells (); 
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// The spell being added
		final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
		trueSpell.setCastingPlayerID (8);
		
		// Players can see the spell or not, and be human/AI, so create 8 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarDuplication dup = mock (FogOfWarDuplication.class);
		final FogOfWarMidTurnVisibility vis = mock (FogOfWarMidTurnVisibility.class);
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		
		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean upToDateInfo : new boolean [] {false, true})
				for (final boolean human : new boolean [] {false, true})
				{
					playerID++;
					
					final PlayerDescription pd = new PlayerDescription ();
					pd.setPlayerID (playerID);
					pd.setHuman (human);
					
					// Need to make the spell lists unique for verify to work correctly
					final FogOfWarMemory fow = new FogOfWarMemory ();
					fow.getMaintainedSpell ().add (new MemoryMaintainedSpell ());
					
					final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
					priv.setFogOfWarMemory (fow);
					
					final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
					if (human)
						player.setConnection (new DummyServerToClientConnection ());
					
					players.add (player);

					// Mock finding the player
					when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "addExistingTrueMaintainedSpellToClients")).thenReturn (player);
					
					// Mock whether the player can see the spell
					when (vis.canSeeSpellMidTurn (trueSpell, trueTerrain, trueMap.getUnit (), player, db, fowSettings)).thenReturn (canSee);
					
					// Mock whether the player has up to date info for this spell already or not
					when (dup.copyMaintainedSpell (trueSpell, fow.getMaintainedSpell ())).thenReturn (!upToDateInfo);
				}
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarDuplication (dup);
		calc.setFogOfWarMidTurnVisibility (vis);
		calc.setFogOfWarProcessing (proc);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		calc.addExistingTrueMaintainedSpellToClients (gsk, trueSpell, players, db, sd);
		
		// Players 1-4 can't even see the spell so shouldn't get the dup method called; players 5-8 should
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			verify (dup, times (playerIndex < 4 ? 0 : 1)).copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ());
		}

		// Only player 6 (can see spell, out of date info, human) should get a message
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 5)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final AddMaintainedSpellMessage addMsg = (AddMaintainedSpellMessage) conn.getMessages ().get (0);
					assertSame (trueSpell, addMsg.getMaintainedSpell ());
				}
			}
		}
		
		// Only the casters FOW should be updated
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
			verify (proc, times (playerIndex == (trueSpell.getCastingPlayerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "addExistingTrueMaintainedSpellToClients", sd, db);
	}
	
	/**
	 * Tests the switchOffMaintainedSpellOnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffMaintainedSpellOnServerAndClients () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);

		// Server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells (); 
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// The spell being added
		final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
		trueSpell.setSpellURN (101);
		trueSpell.setCastingPlayerID (4);
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.findSpellURN (trueSpell.getSpellURN (), trueMap.getMaintainedSpell (), "switchOffMaintainedSpellOnServerAndClients")).thenReturn (trueSpell);

		// Players can see the spell or not, and be human/AI, so create 4 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarMidTurnVisibility vis = mock (FogOfWarMidTurnVisibility.class);
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);

		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean human : new boolean [] {false, true})
			{
				playerID++;
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (playerID);
				pd.setHuman (human);
				
				// Need to make the spell lists unique for verify to work correctly
				final FogOfWarMemory fow = new FogOfWarMemory ();
				fow.getMaintainedSpell ().add (new MemoryMaintainedSpell ());
				
				final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
				priv.setFogOfWarMemory (fow);
				
				final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
				if (human)
					player.setConnection (new DummyServerToClientConnection ());
				
				players.add (player);

				// Mock finding the player
				when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "switchOffMaintainedSpellOnServerAndClients")).thenReturn (player);
				
				// Mock whether the player can see the spell
				when (vis.canSeeSpellMidTurn (trueSpell, trueTerrain, trueMap.getUnit (), player, db, fowSettings)).thenReturn (canSee);
			}
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setFogOfWarMidTurnVisibility (vis);
		calc.setFogOfWarProcessing (proc);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		calc.switchOffMaintainedSpellOnServerAndClients (trueMap, trueSpell.getSpellURN (), players, db, sd);

		// Players 1-2 can't even see the spell so shouldn't get the remove method called; players 3-4 should
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			verify (memoryMaintainedSpellUtils, times (playerIndex < 2 ? 0 : 1)).removeSpellURN (trueSpell.getSpellURN (), priv.getFogOfWarMemory ().getMaintainedSpell ());
		}

		// Only player 4 (can see spell, and is human) should get a message
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 3)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final SwitchOffMaintainedSpellMessage removeMsg = (SwitchOffMaintainedSpellMessage) conn.getMessages ().get (0);
					assertSame (trueSpell.getSpellURN (), removeMsg.getSpellURN ());
				}
			}
		}

		// Only the casters FOW should be updated
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
			verify (proc, times (playerIndex == (trueSpell.getCastingPlayerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "switchOffMaintainedSpellOnServerAndClients", sd, db);
	}
	
	/**
	 * Tests the addBuildingOnServerAndClients method, when its from the normal completion of constructing a building
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBuildingOnServerAndClients_OneBuilding () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setCitiesSpellsAndCombatAreaEffects (fowSetting);

		final OverlandMapSize sys = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setFogOfWarSetting (fowSettings);

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// Server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		gsk.setNextFreeBuildingURN (1);
		
		// Players can see the city or not, and be human/AI, so create 4 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);

		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean human : new boolean [] {false, true})
			{
				playerID++;
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (playerID);
				pd.setHuman (human);
				
				// Mock whether the player can see the city
				final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
				vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);
				
				final FogOfWarMemory fow = new FogOfWarMemory ();
				fow.setMap (createOverlandMap (sys));
				
				final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
				priv.setFogOfWar (vis);
				priv.setFogOfWarMemory (fow);
				
				final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
				if (human)
					player.setConnection (new DummyServerToClientConnection ());
				
				players.add (player);

				// Mock finding the player
				when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "addBuildingOnServerAndClients")).thenReturn (player);
			}
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (4);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();		// Used real one here, since makes it easier to check the output than via mock
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (single);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setFogOfWarDuplication (dup);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.addBuildingOnServerAndClients (gsk, players, new MapCoordinates3DEx (20, 10, 1), "BL03", null, null, null, sd, db);
		
		// Prove that building got added to server's true map
		assertEquals (1, trueMap.getBuilding ().size ());
		assertEquals (1, trueMap.getBuilding ().get (0).getBuildingURN ());
		assertEquals ("BL03", trueMap.getBuilding ().get (0).getBuildingID ());
		assertEquals (new MapCoordinates3DEx (20, 10, 1), trueMap.getBuilding ().get (0).getCityLocation ());
		
		// Prove that building got added to server's copy of each player's memory, but only the players who can see it
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			if (playerIndex < 2)
				assertEquals (0, priv.getFogOfWarMemory ().getBuilding ().size ());
			else
			{
				assertEquals (1, priv.getFogOfWarMemory ().getBuilding ().size ());
				assertEquals (1, priv.getFogOfWarMemory ().getBuilding ().get (0).getBuildingURN ());
				assertEquals ("BL03", priv.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
				assertEquals (new MapCoordinates3DEx (20, 10, 1), priv.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
			}
		}

		// Only player 4 (can see city, and is human) should get a message
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 3)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final AddBuildingMessage msg = (AddBuildingMessage) conn.getMessages ().get (0);
					assertEquals (new MapCoordinates3DEx (20, 10, 1), msg.getFirstBuilding ().getCityLocation ());
					assertEquals ("BL03", msg.getFirstBuilding ().getBuildingID ());
					assertEquals (1, msg.getFirstBuilding ().getBuildingURN ());
					assertNull (msg.getSecondBuilding ());
					assertNull (msg.getBuildingCreatedFromSpellID ());
					assertNull (msg.getBuildingCreationSpellCastByPlayerID ());
				}
			}
		}

		// Only the city owner's FOW should be updated
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
			verify (proc, times (playerIndex == (cityData.getCityOwnerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "addBuildingOnServerAndClients", sd, db);
	}

	/**
	 * Tests the addBuildingOnServerAndClients method, when its casting Move Fortress which adds two buildings at once
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBuildingOnServerAndClients_TwoBuildings () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setCitiesSpellsAndCombatAreaEffects (fowSetting);

		final OverlandMapSize sys = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setFogOfWarSetting (fowSettings);

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// Server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		gsk.setNextFreeBuildingURN (1);
		
		// Players can see the city or not, and be human/AI, so create 4 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);

		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean human : new boolean [] {false, true})
			{
				playerID++;
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (playerID);
				pd.setHuman (human);
				
				// Mock whether the player can see the city
				final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
				vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);
				
				final FogOfWarMemory fow = new FogOfWarMemory ();
				fow.setMap (createOverlandMap (sys));
				
				final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
				priv.setFogOfWar (vis);
				priv.setFogOfWarMemory (fow);
				
				final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
				if (human)
					player.setConnection (new DummyServerToClientConnection ());
				
				players.add (player);

				// Mock finding the player
				when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "addBuildingOnServerAndClients")).thenReturn (player);
			}
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (4);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();		// Used real one here, since makes it easier to check the output than via mock
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (single);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setFogOfWarDuplication (dup);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.addBuildingOnServerAndClients (gsk, players, new MapCoordinates3DEx (20, 10, 1), "BL03", "BL04", "SP001", 2, sd, db);
		
		// Prove that both buildings got added to server's true map
		assertEquals (2, trueMap.getBuilding ().size ());
		assertEquals (1, trueMap.getBuilding ().get (0).getBuildingURN ());
		assertEquals ("BL03", trueMap.getBuilding ().get (0).getBuildingID ());
		assertEquals (new MapCoordinates3DEx (20, 10, 1), trueMap.getBuilding ().get (0).getCityLocation ());
		assertEquals (2, trueMap.getBuilding ().get (1).getBuildingURN ());
		assertEquals ("BL04", trueMap.getBuilding ().get (1).getBuildingID ());
		assertEquals (new MapCoordinates3DEx (20, 10, 1), trueMap.getBuilding ().get (1).getCityLocation ());
		
		// Prove that the buildings got added to server's copy of each player's memory, but only the players who can see it
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			if (playerIndex < 2)
				assertEquals (0, priv.getFogOfWarMemory ().getBuilding ().size ());
			else
			{
				assertEquals (2, priv.getFogOfWarMemory ().getBuilding ().size ());
				assertEquals (1, priv.getFogOfWarMemory ().getBuilding ().get (0).getBuildingURN ());
				assertEquals ("BL03", priv.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
				assertEquals (new MapCoordinates3DEx (20, 10, 1), priv.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
				assertEquals (2, priv.getFogOfWarMemory ().getBuilding ().get (1).getBuildingURN ());
				assertEquals ("BL04", priv.getFogOfWarMemory ().getBuilding ().get (1).getBuildingID ());
				assertEquals (new MapCoordinates3DEx (20, 10, 1), priv.getFogOfWarMemory ().getBuilding ().get (1).getCityLocation ());
			}
		}

		// Only player 4 (can see city, and is human) should get a message
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 3)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final AddBuildingMessage msg = (AddBuildingMessage) conn.getMessages ().get (0);
					assertEquals (new MapCoordinates3DEx (20, 10, 1), msg.getFirstBuilding ().getCityLocation ());
					assertEquals ("BL03", msg.getFirstBuilding ().getBuildingID ());
					assertEquals (1, msg.getFirstBuilding ().getBuildingURN ());
					assertEquals (new MapCoordinates3DEx (20, 10, 1), msg.getSecondBuilding ().getCityLocation ());
					assertEquals ("BL04", msg.getSecondBuilding ().getBuildingID ());
					assertEquals (2, msg.getSecondBuilding ().getBuildingURN ());
					assertEquals ("SP001", msg.getBuildingCreatedFromSpellID ());
					assertEquals (2, msg.getBuildingCreationSpellCastByPlayerID ().intValue ());
				}
			}
		}

		// Only the city owner's FOW should be updated
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
			verify (proc, times (playerIndex == (cityData.getCityOwnerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "addBuildingOnServerAndClients", sd, db);
	}
	
	/**
	 * Tests the destroyBuildingOnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDestroyBuildingOnServerAndClients () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setCitiesSpellsAndCombatAreaEffects (fowSetting);

		final OverlandMapSize sys = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setFogOfWarSetting (fowSettings);

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// Overland map
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Players can see the city or not, and be human/AI, so create 4 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);

		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean human : new boolean [] {false, true})
			{
				playerID++;
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (playerID);
				pd.setHuman (human);
				
				// Mock whether the player can see the city
				final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
				vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);
				
				// Need to make the spell lists unique for verify to work correctly
				final FogOfWarMemory fow = new FogOfWarMemory ();
				fow.getBuilding ().add (new MemoryBuilding ());
				fow.setMap (createOverlandMap (sys));
				
				final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
				priv.setFogOfWar (vis);
				priv.setFogOfWarMemory (fow);
				
				final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
				if (human)
					player.setConnection (new DummyServerToClientConnection ());
				
				players.add (player);

				// Mock finding the player
				when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "destroyBuildingOnServerAndClients")).thenReturn (player);
			}
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (4);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// The building being destroyed
		final MemoryBuilding trueBuilding = new MemoryBuilding ();
		trueBuilding.setBuildingURN (3);
		trueBuilding.setBuildingID ("BL03");
		trueBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getBuilding ().add (trueBuilding);

		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuildingURN (trueBuilding.getBuildingURN (), trueMap.getBuilding (), "destroyBuildingOnServerAndClients")).thenReturn (trueBuilding);
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (single);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setMemoryBuildingUtils (buildingUtils);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.destroyBuildingOnServerAndClients (trueMap, players, trueBuilding.getBuildingURN (), false, sd, db);
		
		// Prove that building got removed from server's true map
		verify (buildingUtils, times (1)).removeBuildingURN (trueBuilding.getBuildingURN (), trueMap.getBuilding ());
		
		// Prove that building got removed from server's copy of each player's memory, but only the players who saw it disappear
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			verify (buildingUtils, times (playerIndex < 2 ? 0 : 1)).removeBuildingURN (trueBuilding.getBuildingURN (), priv.getFogOfWarMemory ().getBuilding ());
		}

		// Prove that only the human player's client that saw the building disappear was sent update msg
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 3)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final DestroyBuildingMessage msg = (DestroyBuildingMessage) conn.getMessages ().get (0);
					assertEquals (trueBuilding.getBuildingURN (), msg.getBuildingURN ());
					assertFalse (msg.isUpdateBuildingSoldThisTurn ());
				}
			}
		}
		
		// Only the city owner's FOW should be updated
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
			verify (proc, times (playerIndex == (cityData.getCityOwnerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "destroyBuildingOnServerAndClients", sd, db);
	}
}