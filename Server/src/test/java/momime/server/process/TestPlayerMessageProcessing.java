package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import momime.common.messages.servertoclient.v0_9_4.AddNewTurnMessagesMessage;
import momime.common.messages.servertoclient.v0_9_4.SetCurrentPlayerMessage;
import momime.common.messages.servertoclient.v0_9_4.StartSimultaneousTurnMessage;
import momime.common.messages.v0_9_4.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.TurnSystem;
import momime.server.DummyServerToClientConnection;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the PlayerMessageProcessing class
 */
public final class TestPlayerMessageProcessing
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
		final PlayerMessageProcessing proc = new PlayerMessageProcessing ();
		
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
		final PlayerMessageProcessing proc = new PlayerMessageProcessing ();
		
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
		final PlayerMessageProcessing proc = new PlayerMessageProcessing ();
		
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
}
