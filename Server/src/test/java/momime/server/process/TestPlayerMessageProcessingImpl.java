package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.messages.servertoclient.v0_9_5.AddNewTurnMessagesMessage;
import momime.common.messages.servertoclient.v0_9_5.SetCurrentPlayerMessage;
import momime.common.messages.servertoclient.v0_9_5.StartSimultaneousTurnMessage;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_5.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.NewTurnMessageData;
import momime.common.messages.v0_9_5.PendingMovement;
import momime.common.messages.v0_9_5.TurnSystem;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the PlayerMessageProcessingImpl class
 */
public final class TestPlayerMessageProcessingImpl
{
	/**
	 * Tests the sendNewTurnMessages method, sending messages mid-turn
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSendNewTurnMessages_MidTurn () throws Exception
	{
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (2);
		pd1.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player1message1 = new NewTurnMessageData ();		// These get used as-is, so no need to put real values in here
		final NewTurnMessageData player1message2 = new NewTurnMessageData ();
		trans1.getNewTurnMessage ().add (player1message1);
		trans1.getNewTurnMessage ().add (player1message2);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		
		final DummyServerToClientConnection msgs1 = new DummyServerToClientConnection ();
		player1.setConnection (msgs1);
		players.add (player1);
		
		// Computer player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);
		
		final MomTransientPlayerPrivateKnowledge trans2 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player2message1 = new NewTurnMessageData ();
		final NewTurnMessageData player2message2 = new NewTurnMessageData ();
		trans2.getNewTurnMessage ().add (player2message1);
		trans2.getNewTurnMessage ().add (player2message2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, trans2);
		players.add (player2);
		
		// Human player
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player3message1 = new NewTurnMessageData ();
		final NewTurnMessageData player3message2 = new NewTurnMessageData ();
		trans3.getNewTurnMessage ().add (player3message1);
		trans3.getNewTurnMessage ().add (player3message2);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, null, null, trans3);
		
		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		players.add (player3);

		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		
		// Run test
		proc.sendNewTurnMessages (null, players, null);
		
		// Check player1's messages
		assertEquals (1, msgs1.getMessages ().size ());
		
		final AddNewTurnMessagesMessage msg1 = (AddNewTurnMessagesMessage) msgs1.getMessages ().get (0);
		assertFalse (msg1.isExpireMessages ());
		assertEquals (2, msg1.getMessage ().size ());
		assertSame (player1message1, msg1.getMessage ().get (0));
		assertSame (player1message2, msg1.getMessage ().get (1));

		// Check player3's messages
		assertEquals (1, msgs3.getMessages ().size ());
		
		final AddNewTurnMessagesMessage msg3 = (AddNewTurnMessagesMessage) msgs3.getMessages ().get (0);
		assertFalse (msg3.isExpireMessages ());
		assertEquals (2, msg3.getMessage ().size ());
		assertSame (player3message1, msg3.getMessage ().get (0));
		assertSame (player3message2, msg3.getMessage ().get (1));
	}

	/**
	 * Tests the sendNewTurnMessages method, at the start of a simultaneous turn
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSendNewTurnMessages_SimultaneousTurns () throws Exception
	{
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (2);
		pd1.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player1message1 = new NewTurnMessageData ();		// These get used as-is, so no need to put real values in here
		final NewTurnMessageData player1message2 = new NewTurnMessageData ();
		trans1.getNewTurnMessage ().add (player1message1);
		trans1.getNewTurnMessage ().add (player1message2);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		
		final DummyServerToClientConnection msgs1 = new DummyServerToClientConnection ();
		player1.setConnection (msgs1);
		players.add (player1);
		
		// Computer player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);
		
		final MomTransientPlayerPrivateKnowledge trans2 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player2message1 = new NewTurnMessageData ();
		final NewTurnMessageData player2message2 = new NewTurnMessageData ();
		trans2.getNewTurnMessage ().add (player2message1);
		trans2.getNewTurnMessage ().add (player2message2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, trans2);
		players.add (player2);
		
		// Human player
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player3message1 = new NewTurnMessageData ();
		final NewTurnMessageData player3message2 = new NewTurnMessageData ();
		trans3.getNewTurnMessage ().add (player3message1);
		trans3.getNewTurnMessage ().add (player3message2);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, null, null, trans3);
		
		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		players.add (player3);
		
		// Turn/Current player
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (5);

		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		
		// Run test
		proc.sendNewTurnMessages (gpk, players, TurnSystem.SIMULTANEOUS);
		
		// Check player1's messages
		assertEquals (1, msgs1.getMessages ().size ());
		
		final StartSimultaneousTurnMessage msg1 = (StartSimultaneousTurnMessage) msgs1.getMessages ().get (0);
		assertTrue (msg1.isExpireMessages ());
		assertEquals (5, msg1.getTurnNumber ());
		assertEquals (2, msg1.getMessage ().size ());
		assertSame (player1message1, msg1.getMessage ().get (0));
		assertSame (player1message2, msg1.getMessage ().get (1));

		// Check player3's messages
		assertEquals (1, msgs3.getMessages ().size ());
		
		final StartSimultaneousTurnMessage msg3 = (StartSimultaneousTurnMessage) msgs3.getMessages ().get (0);
		assertTrue (msg3.isExpireMessages ());
		assertEquals (5, msg3.getTurnNumber ());
		assertEquals (2, msg3.getMessage ().size ());
		assertSame (player3message1, msg3.getMessage ().get (0));
		assertSame (player3message2, msg3.getMessage ().get (1));
	}

	/**
	 * Tests the sendNewTurnMessages method, at the start of a simultaneous turn
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSendNewTurnMessages_SingleTurns () throws Exception
	{
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (2);
		pd1.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player1message1 = new NewTurnMessageData ();		// These get used as-is, so no need to put real values in here
		final NewTurnMessageData player1message2 = new NewTurnMessageData ();
		trans1.getNewTurnMessage ().add (player1message1);
		trans1.getNewTurnMessage ().add (player1message2);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		
		final DummyServerToClientConnection msgs1 = new DummyServerToClientConnection ();
		player1.setConnection (msgs1);
		players.add (player1);
		
		// Computer player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);
		
		final MomTransientPlayerPrivateKnowledge trans2 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player2message1 = new NewTurnMessageData ();
		final NewTurnMessageData player2message2 = new NewTurnMessageData ();
		trans2.getNewTurnMessage ().add (player2message1);
		trans2.getNewTurnMessage ().add (player2message2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, trans2);
		players.add (player2);
		
		// Human player
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		final NewTurnMessageData player3message1 = new NewTurnMessageData ();
		final NewTurnMessageData player3message2 = new NewTurnMessageData ();
		trans3.getNewTurnMessage ().add (player3message1);
		trans3.getNewTurnMessage ().add (player3message2);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, null, null, trans3);
		
		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		players.add (player3);
		
		// Turn/Current player
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (5);
		gpk.setCurrentPlayerID (7);
		
		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		
		// Run test
		proc.sendNewTurnMessages (gpk, players, TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Check player1's messages
		assertEquals (1, msgs1.getMessages ().size ());
		
		final SetCurrentPlayerMessage msg1 = (SetCurrentPlayerMessage) msgs1.getMessages ().get (0);
		assertFalse (msg1.isExpireMessages ());
		assertEquals (5, msg1.getTurnNumber ());
		assertEquals (7, msg1.getCurrentPlayerID ());
		assertEquals (0, msg1.getMessage ().size ());

		// Check player3's messages
		assertEquals (1, msgs3.getMessages ().size ());
		
		final SetCurrentPlayerMessage msg3 = (SetCurrentPlayerMessage) msgs3.getMessages ().get (0);
		assertTrue (msg3.isExpireMessages ());
		assertEquals (5, msg3.getTurnNumber ());
		assertEquals (7, msg3.getCurrentPlayerID ());
		assertEquals (2, msg3.getMessage ().size ());
		assertSame (player3message1, msg3.getMessage ().get (0));
		assertSame (player3message2, msg3.getMessage ().get (1));
	}
	
	/**
	 * Tests the continueMovement method for all players (i.e. as in a simultaneous turns game)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testContinueMovement_AllPlayers () throws Exception
	{
		// General server knowledge
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();
		final PlayerServerDetails player1 = new PlayerServerDetails (null, null, null, null, trans1);

		final MomTransientPlayerPrivateKnowledge trans2 = new MomTransientPlayerPrivateKnowledge ();
		final PlayerServerDetails player2 = new PlayerServerDetails (null, null, null, null, trans2);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	
		
		// Units
		final MemoryUnit unit1 = new MemoryUnit ();
		final MemoryUnit unit2 = new MemoryUnit ();
		
		final List<MemoryUnit> unitStack1 = new ArrayList<MemoryUnit> ();
		unitStack1.add (unit1);
		
		final List<MemoryUnit> unitStack2 = new ArrayList<MemoryUnit> ();
		unitStack2.add (unit2);
		
		// Pending moves
		final PendingMovement move1 = new PendingMovement ();
		move1.setMoveFrom (createCoordinates (1));
		move1.setMoveTo (createCoordinates (2));
		move1.getPath ().add (3);
		move1.getUnitURN ().add (1);

		final PendingMovement move2 = new PendingMovement ();
		move2.setMoveFrom (createCoordinates (4));
		move2.setMoveTo (createCoordinates (3));
		move2.getPath ().add (7);
		move2.getUnitURN ().add (2);
		
		trans1.getPendingMovement ().add (move1);
		trans2.getPendingMovement ().add (move2);
		
		// Unit searches
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (1, trueMap.getUnit (), "continueMovement")).thenReturn (unit1);
		when (unitUtils.findUnitURN (2, trueMap.getUnit (), "continueMovement")).thenReturn (unit2);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionLogger ()).thenReturn (Logger.getLogger (TestPlayerMessageProcessingImpl.class.getName ()));
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setUnitUtils (unitUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		
		// Run method
		proc.continueMovement (0, mom);
		
		// Check results
		verify (midTurn, times (1)).moveUnitStack (unitStack1, player1, (MapCoordinates3DEx) move1.getMoveFrom (), (MapCoordinates3DEx) move1.getMoveTo (), false, mom);
		verify (midTurn, times (1)).moveUnitStack (unitStack2, player2, (MapCoordinates3DEx) move2.getMoveFrom (), (MapCoordinates3DEx) move2.getMoveTo (), false, mom);
	}

	/**
	 * Tests the continueMovement method for a single players (i.e. as in a one-at-a-time turns game)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testContinueMovement_OnePlayers () throws Exception
	{
		// General server knowledge
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);

		final MomTransientPlayerPrivateKnowledge trans2 = new MomTransientPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, trans2);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	
		
		// Units
		final MemoryUnit unit1 = new MemoryUnit ();
		final MemoryUnit unit2 = new MemoryUnit ();
		
		final List<MemoryUnit> unitStack1 = new ArrayList<MemoryUnit> ();
		unitStack1.add (unit1);
		
		final List<MemoryUnit> unitStack2 = new ArrayList<MemoryUnit> ();
		unitStack2.add (unit2);
		
		// Pending moves
		final PendingMovement move1 = new PendingMovement ();
		move1.setMoveFrom (createCoordinates (1));
		move1.setMoveTo (createCoordinates (2));
		move1.getPath ().add (3);
		move1.getUnitURN ().add (1);

		final PendingMovement move2 = new PendingMovement ();
		move2.setMoveFrom (createCoordinates (4));
		move2.setMoveTo (createCoordinates (3));
		move2.getPath ().add (7);
		move2.getUnitURN ().add (2);
		
		trans1.getPendingMovement ().add (move1);
		trans2.getPendingMovement ().add (move2);
		
		// Unit searches
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (1, trueMap.getUnit (), "continueMovement")).thenReturn (unit1);
		when (unitUtils.findUnitURN (2, trueMap.getUnit (), "continueMovement")).thenReturn (unit2);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionLogger ()).thenReturn (Logger.getLogger (TestPlayerMessageProcessingImpl.class.getName ()));
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setUnitUtils (unitUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		
		// Run method
		proc.continueMovement (2, mom);
		
		// Check results
		verify (midTurn, times (0)).moveUnitStack (unitStack1, player1, (MapCoordinates3DEx) move1.getMoveFrom (), (MapCoordinates3DEx) move1.getMoveTo (), false, mom);
		verify (midTurn, times (1)).moveUnitStack (unitStack2, player2, (MapCoordinates3DEx) move2.getMoveFrom (), (MapCoordinates3DEx) move2.getMoveTo (), false, mom);
	}

	/**
	 * Just to save repeating this a dozen times in the test cases
	 * @param x X coord
	 * @return Coordinates object
	 */
	private final MapCoordinates3DEx createCoordinates (final int x)
	{
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx ();
		combatLocation.setX (x);
		combatLocation.setY (10);
		combatLocation.setZ (1);
		return combatLocation;
	}
}