package momime.server.worldupdates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.OverlandMapSize;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.KillUnitMessage;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.fogofwar.FogOfWarMidTurnVisibility;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the KillUnitUpdate class
 */
@ExtendWith(MockitoExtension.class)
public final class TestKillUnitUpdate extends ServerTestData
{
	/**
	 * Tests the process method, on a normal being killed overland by mainly healable damage.
	 * It should just be removed in all lists (server, server copy of player memory, and client copy of player memory).
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess_OverlandDamage_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "KillUnitUpdate")).thenReturn (unitDef);
		
		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (tu.getUnitURN (), trueMap.getUnit (), "KillUnitUpdate (tu)")).thenReturn (tu);
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player1, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player2, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player3, mom)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player4, mom)).thenReturn (false);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player5, mom)).thenReturn (false);

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
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final KillUnitUpdate update = new KillUnitUpdate ();
		update.setFogOfWarMidTurnVisibility (midTurn);
		update.setUnitUtils (unitUtils);
		update.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		update.setUnitURN (tu.getUnitURN ());
		update.setUntransmittedAction (KillUnitActionID.HEALABLE_OVERLAND_DAMAGE);
		update.process (mom);
		
		// Check was removed on server's true map details
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.ALIVE, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertNull (msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		assertEquals (0, conn4.getMessages ().size ());
		
		verifyNoMoreInteractions (unitUtils);
		verifyNoMoreInteractions (pendingMovementUtils);
	}
	
	/**
	 * Tests the process method, on a hero being killed overland by mainly healable damage.
	 * It should be retained in the server's true list and the owner's memory both on the server and client as DEAD, but for everybody else completedly removed.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess_OverlandDamage_Hero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN001", "KillUnitUpdate")).thenReturn (unitDef);
		
		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (tu.getUnitURN (), trueMap.getUnit (), "KillUnitUpdate (tu)")).thenReturn (tu);
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player1, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player2, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player3, mom)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player4, mom)).thenReturn (false);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player5, mom)).thenReturn (false);

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
		final MemoryUnit mu1 = new MemoryUnit ();
		mu1.setOwningPlayerID (tu.getOwningPlayerID ());
		mu1.setUnitURN (tu.getUnitURN ());
		mu1.setStatus (tu.getStatus ());
		mu1.setUnitID (tu.getUnitID ());
		when (unitUtils.findUnitURN (mu1.getUnitURN (), fow1.getUnit (), "KillUnitUpdate (mu)")).thenReturn (mu1);
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final KillUnitUpdate update = new KillUnitUpdate ();
		update.setFogOfWarMidTurnVisibility (midTurn);
		update.setUnitUtils (unitUtils);
		update.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		update.setUnitURN (tu.getUnitURN ());
		update.setUntransmittedAction (KillUnitActionID.HEALABLE_OVERLAND_DAMAGE);
		update.process (mom);
		
		// Check was set to DEAD on server's true map details
		assertEquals (UnitStatusID.DEAD, tu.getStatus ());
		
		// Check player who owns the unit
		assertEquals (UnitStatusID.DEAD, mu1.getStatus ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.DEAD, msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		assertEquals (0, conn4.getMessages ().size ());
		
		verifyNoMoreInteractions (unitUtils);
		verifyNoMoreInteractions (pendingMovementUtils);
	}
	
	/**
	 * Tests the process method, on a normal unit being dismissed (on the overland map).
	 * It should just be removed in all lists (server, server copy of player memory, and client copy of player memory).
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess_Dismiss_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "KillUnitUpdate")).thenReturn (unitDef);
		
		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (tu.getUnitURN (), trueMap.getUnit (), "KillUnitUpdate (tu)")).thenReturn (tu);
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player1, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player2, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player3, mom)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player4, mom)).thenReturn (false);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player5, mom)).thenReturn (false);

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
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final KillUnitUpdate update = new KillUnitUpdate ();
		update.setFogOfWarMidTurnVisibility (midTurn);
		update.setUnitUtils (unitUtils);
		update.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		update.setUnitURN (tu.getUnitURN ());
		update.setUntransmittedAction (KillUnitActionID.DISMISS);
		update.process (mom);
		
		// Check was removed on server's true map details
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.ALIVE, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertNull (msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		assertEquals (0, conn4.getMessages ().size ());
		
		verifyNoMoreInteractions (unitUtils);
		verifyNoMoreInteractions (pendingMovementUtils);
	}
	
	/**
	 * Tests the process method, on a hero being dismissed (on the overland map).
	 * It should be removed from the player memory lists (on both server and client) but set back to GENERATED in the master server list.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess_Dismiss_Hero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN001", "KillUnitUpdate")).thenReturn (unitDef);
		
		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (tu.getUnitURN (), trueMap.getUnit (), "KillUnitUpdate (tu)")).thenReturn (tu);
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player1, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player2, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player3, mom)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player4, mom)).thenReturn (false);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player5, mom)).thenReturn (false);

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
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final KillUnitUpdate update = new KillUnitUpdate ();
		update.setFogOfWarMidTurnVisibility (midTurn);
		update.setUnitUtils (unitUtils);
		update.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		update.setUnitURN (tu.getUnitURN ());
		update.setUntransmittedAction (KillUnitActionID.DISMISS);
		update.process (mom);

		// Check was set back to GENERATED on server's true map details
		assertEquals (UnitStatusID.GENERATED, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertNull (msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		assertEquals (0, conn4.getMessages ().size ());
		
		verifyNoMoreInteractions (unitUtils);
		verifyNoMoreInteractions (pendingMovementUtils);
	}

	/**
	 * Tests the process method, on a normal unit being lost due to lack of production (on the overland map).
	 * It should just be removed in both server side list (master, and server copy of player memory) but sent to client as the special KILLED_BY_LACK_OF_PRODUCTION value.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess_LackOfProduction_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "KillUnitUpdate")).thenReturn (unitDef);
		
		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (tu.getUnitURN (), trueMap.getUnit (), "KillUnitUpdate (tu)")).thenReturn (tu);
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player1, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player2, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player3, mom)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player4, mom)).thenReturn (false);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player5, mom)).thenReturn (false);

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
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final KillUnitUpdate update = new KillUnitUpdate ();
		update.setFogOfWarMidTurnVisibility (midTurn);
		update.setUnitUtils (unitUtils);
		update.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		update.setUnitURN (tu.getUnitURN ());
		update.setUntransmittedAction (KillUnitActionID.LACK_OF_PRODUCTION);
		update.process (mom);
		
		// Check was removed on server's true map details
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), trueMap.getUnit ());
		assertEquals (UnitStatusID.ALIVE, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.KILLED_BY_LACK_OF_PRODUCTION, msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		assertEquals (0, conn4.getMessages ().size ());
		
		verifyNoMoreInteractions (unitUtils);
		verifyNoMoreInteractions (pendingMovementUtils);
	}
	
	/**
	 * Tests the process method, on a hero unit being lost due to lack of production (on the overland map).
	 * It should just be removed in server copy of player's memory, set to GENERATED in the master server list, and sent to client as the special KILLED_BY_LACK_OF_PRODUCTION value.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess_LackOfProduction_Hero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN001", "KillUnitUpdate")).thenReturn (unitDef);
		
		// True map details on server
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Unit to kill
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (tu.getUnitURN (), trueMap.getUnit (), "KillUnitUpdate (tu)")).thenReturn (tu);
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player1, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player2, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player3, mom)).thenReturn (true);
		
		// A human player who can't see the unit
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player4, mom)).thenReturn (false);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player5, mom)).thenReturn (false);

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
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final KillUnitUpdate update = new KillUnitUpdate ();
		update.setFogOfWarMidTurnVisibility (midTurn);
		update.setUnitUtils (unitUtils);
		update.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		update.setUnitURN (tu.getUnitURN ());
		update.setUntransmittedAction (KillUnitActionID.LACK_OF_PRODUCTION);
		update.process (mom);
		
		// Check was removed on server's true map details
		assertEquals (UnitStatusID.GENERATED, tu.getStatus ());
		
		// Check player who owns the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow1.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.KILLED_BY_LACK_OF_PRODUCTION, msg1.getNewStatus ());

		// Check another human player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check an AI player who can see the unit
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow3.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv3.getPendingMovement (), tu.getUnitURN ());
		
		// Check a human player who can't see the unit
		assertEquals (0, conn4.getMessages ().size ());
		
		verifyNoMoreInteractions (unitUtils);
		verifyNoMoreInteractions (pendingMovementUtils);
	}

	/**
	 * Tests the process method, on a normal being killed in combat by mainly healable damage.
	 * It can potentially be the target of a raise or animate dead spell by either player involved in the combat, so must be set to DEAD in the master server list,
	 * as well as the server and client side lists of the two players involved in the combat, but removed entirely for any 3rd party observers of the combat.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess_CombatDamage_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "KillUnitUpdate")).thenReturn (unitDef);

		// True map details on server
		final OverlandMapSize mapSize = createOverlandMapSize ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (mapSize);
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setAttackingPlayerID (1);
		gc.setDefendingPlayerID (2);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Unit to kill
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (tu.getUnitURN (), trueMap.getUnit (), "KillUnitUpdate (tu)")).thenReturn (tu);
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player1, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player2, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player3, mom)).thenReturn (false);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player4, mom)).thenReturn (false);

		// A human player who is a 3rd party observer who can see the unit from outside of the combat
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (5);
		pd5.setHuman (true);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player5, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player6, mom)).thenReturn (true);
		
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
		final MemoryUnit mu1 = new MemoryUnit ();
		mu1.setOwningPlayerID (tu.getOwningPlayerID ());
		mu1.setUnitURN (tu.getUnitURN ());
		mu1.setStatus (tu.getStatus ());
		mu1.setUnitID (tu.getUnitID ());
		mu1.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) tu.getCombatLocation ()));
		when (unitUtils.findUnitURN (mu1.getUnitURN (), fow1.getUnit (), "KillUnitUpdate (mu)")).thenReturn (mu1);
		
		final MemoryUnit mu2 = new MemoryUnit ();
		mu2.setOwningPlayerID (tu.getOwningPlayerID ());
		mu2.setUnitURN (tu.getUnitURN ());
		mu2.setStatus (tu.getStatus ());
		mu2.setUnitID (tu.getUnitID ());
		mu2.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) tu.getCombatLocation ()));
		when (unitUtils.findUnitURN (mu2.getUnitURN (), fow2.getUnit (), "KillUnitUpdate (mu)")).thenReturn (mu2);
		
		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final KillUnitUpdate update = new KillUnitUpdate ();
		update.setFogOfWarMidTurnVisibility (midTurn);
		update.setUnitUtils (unitUtils);
		update.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		update.setUnitURN (tu.getUnitURN ());
		update.setUntransmittedAction (KillUnitActionID.HEALABLE_COMBAT_DAMAGE);
		update.process (mom);
		
		// Check was set to DEAD on server's true map details rather than completely removed
		assertEquals (UnitStatusID.DEAD, tu.getStatus ());
		
		// Check player who owns the unit
		assertEquals (UnitStatusID.DEAD, mu1.getStatus ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.DEAD, msg1.getNewStatus ());

		// Check the other human player who they are in combat with
		assertEquals (UnitStatusID.DEAD, mu2.getStatus ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertEquals (UnitStatusID.DEAD, msg2.getNewStatus ());
		
		// Check a human player who can't see the unit
		assertEquals (0, conn3.getMessages ().size ());
		
		// Check a human player who is a 3rd party observer who can see the unit from outside of the combat
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn5.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn5.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg5 = (KillUnitMessage) conn5.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg5.getUnitURN ());
		assertNull (msg5.getNewStatus ());

		// Check an AI player who is a 3rd party observer who can see the unit from outside of the combat
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow6.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv6.getPendingMovement (), tu.getUnitURN ());
		
		verifyNoMoreInteractions (unitUtils);
		verifyNoMoreInteractions (pendingMovementUtils);
	}
	
	/**
	 * Tests the process method, on a hero being killed in combat by mainly healable damage.
	 * It can potentially be the target of a raise dead spell by the unit owner, so must be set to DEAD in the master server list,
	 * as well as the server and client side lists of the unit owner, but removed entirely for everybody else, including the other player involved in the combat.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess_CombatDamage_Hero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN001", "KillUnitUpdate")).thenReturn (unitDef);

		// True map details on server
		final OverlandMapSize mapSize = createOverlandMapSize ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (mapSize);
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setAttackingPlayerID (1);
		gc.setDefendingPlayerID (2);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Unit to kill
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MemoryUnit tu = new MemoryUnit ();
		tu.setOwningPlayerID (1);
		tu.setUnitURN (55);
		tu.setStatus (UnitStatusID.ALIVE);
		tu.setUnitID ("UN001");
		tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (tu.getUnitURN (), trueMap.getUnit (), "KillUnitUpdate (tu)")).thenReturn (tu);
		
		// Player who owns the unit
		final FogOfWarMidTurnVisibility midTurn = mock (FogOfWarMidTurnVisibility.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player1, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player2, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player3, mom)).thenReturn (false);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player4, mom)).thenReturn (false);

		// A human player who is a 3rd party observer who can see the unit from outside of the combat
		final PlayerDescription pd5 = new PlayerDescription ();
		pd5.setPlayerID (5);
		pd5.setHuman (true);
		
		final FogOfWarMemory fow5 = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv5 = new MomPersistentPlayerPrivateKnowledge ();
		priv5.setFogOfWarMemory (fow5);
		
		final PlayerServerDetails player5 = new PlayerServerDetails (pd5, null, priv5, null, null);
		
		when (midTurn.canSeeUnitMidTurn (tu, player5, mom)).thenReturn (true);
		
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
		
		when (midTurn.canSeeUnitMidTurn (tu, player6, mom)).thenReturn (true);
		
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
		final MemoryUnit mu1 = new MemoryUnit ();
		mu1.setOwningPlayerID (tu.getOwningPlayerID ());
		mu1.setUnitURN (tu.getUnitURN ());
		mu1.setStatus (tu.getStatus ());
		mu1.setUnitID (tu.getUnitID ());
		mu1.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) tu.getCombatLocation ()));
		when (unitUtils.findUnitURN (mu1.getUnitURN (), fow1.getUnit (), "KillUnitUpdate (mu)")).thenReturn (mu1);
		
		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final KillUnitUpdate update = new KillUnitUpdate ();
		update.setFogOfWarMidTurnVisibility (midTurn);
		update.setUnitUtils (unitUtils);
		update.setPendingMovementUtils (pendingMovementUtils);
		
		// Run method
		update.setUnitURN (tu.getUnitURN ());
		update.setUntransmittedAction (KillUnitActionID.HEALABLE_COMBAT_DAMAGE);
		update.process (mom);
		
		// Check was set to DEAD on server's true map details rather than completely removed
		assertEquals (UnitStatusID.DEAD, tu.getStatus ());
		
		// Check player who owns the unit
		assertEquals (UnitStatusID.DEAD, mu1.getStatus ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv1.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg1 = (KillUnitMessage) conn1.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg1.getUnitURN ());
		assertEquals (UnitStatusID.DEAD, msg1.getNewStatus ());

		// Check the other human player who they are in combat with
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow2.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv2.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg2 = (KillUnitMessage) conn2.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg2.getUnitURN ());
		assertNull (msg2.getNewStatus ());
		
		// Check a human player who can't see the unit
		assertEquals (0, conn3.getMessages ().size ());
		
		// Check a human player who is a 3rd party observer who can see the unit from outside of the combat
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow5.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv5.getPendingMovement (), tu.getUnitURN ());
		
		assertEquals (1, conn5.getMessages ().size ());
		assertEquals (KillUnitMessage.class.getName (), conn5.getMessages ().get (0).getClass ().getName ());
		final KillUnitMessage msg5 = (KillUnitMessage) conn5.getMessages ().get (0);
		assertEquals (tu.getUnitURN (), msg5.getUnitURN ());
		assertNull (msg5.getNewStatus ());

		// Check an AI player who is a 3rd party observer who can see the unit from outside of the combat
		verify (unitUtils).removeUnitURN (tu.getUnitURN (), fow6.getUnit ());
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv6.getPendingMovement (), tu.getUnitURN ());
		
		verifyNoMoreInteractions (unitUtils);
		verifyNoMoreInteractions (pendingMovementUtils);
	}
}