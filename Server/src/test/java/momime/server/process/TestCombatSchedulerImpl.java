package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.messages.servertoclient.v0_9_5.AddScheduledCombatMessage;
import momime.common.messages.servertoclient.v0_9_5.ScheduledCombatWalkInWithoutAFightMessage;
import momime.common.messages.servertoclient.v0_9_5.ShowListAndOtherScheduledCombatsMessage;
import momime.common.messages.servertoclient.v0_9_5.UpdateOtherScheduledCombatsMessage;
import momime.common.messages.v0_9_5.MomScheduledCombat;
import momime.common.utils.ScheduledCombatUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CombatSchedulerImpl class
 */
public final class TestCombatSchedulerImpl
{
	/**
	 * Tests the sendScheduledCombats method with the updateOthersCountOnly flag set to true
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSendScheduledCombats_UpdateOthersCountOnly () throws Exception
	{
		// Players
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setHuman (true);
		pd1.setPlayerID (3);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, null);
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);

		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setHuman (true);
		pd2.setPlayerID (5);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);

		// Combats
		final MomScheduledCombat combat1 = new MomScheduledCombat ();
		combat1.setAttackingPlayerID (3);
		combat1.setDefendingPlayerID (5);

		final MomScheduledCombat combat2 = new MomScheduledCombat ();
		combat2.setAttackingPlayerID (3);
		combat2.setDefendingPlayerID (2); 

		final MomScheduledCombat combat3 = new MomScheduledCombat ();
		combat3.setAttackingPlayerID (10);
		combat3.setDefendingPlayerID (7);

		final List<MomScheduledCombat> combats = new ArrayList<MomScheduledCombat> ();
		combats.add (combat1);
		combats.add (combat2);
		combats.add (combat3);
				
		// Set up object to test
		final CombatSchedulerImpl scheduler = new CombatSchedulerImpl ();
		
		// Run method
		scheduler.sendScheduledCombats (players, combats, true);
		
		// Check results
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (UpdateOtherScheduledCombatsMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final UpdateOtherScheduledCombatsMessage msg1 = (UpdateOtherScheduledCombatsMessage) conn1.getMessages ().get (0);
		assertEquals (1, msg1.getCombatCount ());

		assertEquals (1, conn2.getMessages ().size ());
		assertEquals (UpdateOtherScheduledCombatsMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		final UpdateOtherScheduledCombatsMessage msg2 = (UpdateOtherScheduledCombatsMessage) conn2.getMessages ().get (0);
		assertEquals (2, msg2.getCombatCount ());
	}

	/**
	 * Tests the sendScheduledCombats method with the updateOthersCountOnly flag set to false
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSendScheduledCombats_SendAll () throws Exception
	{
		// Players
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setHuman (true);
		pd1.setPlayerID (3);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, null);
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);

		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setHuman (true);
		pd2.setPlayerID (5);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);

		// Combats
		final MomScheduledCombat combat1 = new MomScheduledCombat ();
		combat1.setAttackingPlayerID (3);
		combat1.setDefendingPlayerID (5);

		final MomScheduledCombat combat2 = new MomScheduledCombat ();
		combat2.setAttackingPlayerID (3);
		combat2.setDefendingPlayerID (2); 

		final MomScheduledCombat combat3 = new MomScheduledCombat ();
		combat3.setAttackingPlayerID (10);
		combat3.setDefendingPlayerID (7);

		final List<MomScheduledCombat> combats = new ArrayList<MomScheduledCombat> ();
		combats.add (combat1);
		combats.add (combat2);
		combats.add (combat3);
				
		// Set up object to test
		final CombatSchedulerImpl scheduler = new CombatSchedulerImpl ();
		
		// Run method
		scheduler.sendScheduledCombats (players, combats, false);
		
		// Check results
		assertEquals (3, conn1.getMessages ().size ());
		assertEquals (AddScheduledCombatMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		assertEquals (combat1, ((AddScheduledCombatMessage) conn1.getMessages ().get (0)).getScheduledCombatData ());
		assertEquals (AddScheduledCombatMessage.class.getName (), conn1.getMessages ().get (1).getClass ().getName ());
		assertEquals (combat2, ((AddScheduledCombatMessage) conn1.getMessages ().get (1)).getScheduledCombatData ());
		assertEquals (ShowListAndOtherScheduledCombatsMessage.class.getName (), conn1.getMessages ().get (2).getClass ().getName ());
		final ShowListAndOtherScheduledCombatsMessage msg1 = (ShowListAndOtherScheduledCombatsMessage) conn1.getMessages ().get (2);
		assertEquals (1, msg1.getCombatCount ());

		assertEquals (2, conn2.getMessages ().size ());
		assertEquals (combat1, ((AddScheduledCombatMessage) conn2.getMessages ().get (0)).getScheduledCombatData ());
		assertEquals (AddScheduledCombatMessage.class.getName (), conn2.getMessages ().get (0).getClass ().getName ());
		assertEquals (ShowListAndOtherScheduledCombatsMessage.class.getName (), conn2.getMessages ().get (1).getClass ().getName ());
		final ShowListAndOtherScheduledCombatsMessage msg2 = (ShowListAndOtherScheduledCombatsMessage) conn2.getMessages ().get (1);
		assertEquals (2, msg2.getCombatCount ());
	}
	
	/**
	 * Tests the processEndOfScheduledCombat method when there's no more scheduled combats left to play
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessEndOfScheduledCombat_NoMoreCombats () throws Exception
	{
		// General server knowledge
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();

		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;		// Lets say there's no defender, i.e. capturing an empty lair

		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "processEndOfScheduledCombat")).thenReturn (attackingPlayer);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Details about the combat that ended
		final MomScheduledCombat endingCombat = new MomScheduledCombat ();
		endingCombat.setScheduledCombatURN (101);
		endingCombat.setAttackingPlayerID (attackingPd.getPlayerID ());
		
		final ScheduledCombatUtils scheduledCombatUtils = mock (ScheduledCombatUtils.class);
		when (scheduledCombatUtils.findScheduledCombatURN (gsk.getScheduledCombat (), endingCombat.getScheduledCombatURN (),
			"processEndOfScheduledCombat")).thenReturn (endingCombat);
		gsk.getScheduledCombat ().add (endingCombat);
		
		// Set up object to test
		final PlayerMessageProcessing msgProc = mock (PlayerMessageProcessing.class);
		
		final CombatSchedulerImpl scheduler = new CombatSchedulerImpl ();
		scheduler.setScheduledCombatUtils (scheduledCombatUtils);
		scheduler.setPlayerMessageProcessing (msgProc);
		scheduler.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		scheduler.processEndOfScheduledCombat (endingCombat.getScheduledCombatURN (), winningPlayer, mom);

		// Check combat was removed
		assertEquals (0, gsk.getScheduledCombat ().size ());
		
		// Check next turn was triggered
		verify (msgProc, times (1)).endPhase (mom, 0);
	}

	/**
	 * Tests the processEndOfScheduledCombat method when there's more scheduled combats left to play
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessEndOfScheduledCombat_SomeMoreCombats () throws Exception
	{
		// General server knowledge
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();

		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;		// Lets say there's no defender, i.e. capturing an empty lair

		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "processEndOfScheduledCombat")).thenReturn (attackingPlayer);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Details about the combat that ended
		final MapCoordinates3DEx endingCombatLocation = new MapCoordinates3DEx (25, 15, 1);
		
		final MomScheduledCombat endingCombat = new MomScheduledCombat ();
		endingCombat.setScheduledCombatURN (101);
		endingCombat.setAttackingPlayerID (attackingPd.getPlayerID ());
		endingCombat.setDefendingLocation (endingCombatLocation);
		gsk.getScheduledCombat ().add (endingCombat);

		final MapCoordinates3DEx otherCombat1Location = new MapCoordinates3DEx (25, 15, 1);		// <-- Same loc
		
		final MomScheduledCombat otherCombat1 = new MomScheduledCombat ();
		otherCombat1.setScheduledCombatURN (102);
		otherCombat1.setAttackingPlayerID (attackingPd.getPlayerID ());
		otherCombat1.setDefendingLocation (otherCombat1Location);
		gsk.getScheduledCombat ().add (otherCombat1);

		final MapCoordinates3DEx otherCombat2Location = new MapCoordinates3DEx (26, 15, 1);		// <--- Diff loc
		
		final MomScheduledCombat otherCombat2 = new MomScheduledCombat ();
		otherCombat2.setScheduledCombatURN (103);
		otherCombat2.setAttackingPlayerID (attackingPd.getPlayerID ());
		otherCombat2.setDefendingLocation (otherCombat2Location);
		gsk.getScheduledCombat ().add (otherCombat2);
		
		final ScheduledCombatUtils scheduledCombatUtils = mock (ScheduledCombatUtils.class);
		when (scheduledCombatUtils.findScheduledCombatURN (gsk.getScheduledCombat (), endingCombat.getScheduledCombatURN (),
			"processEndOfScheduledCombat")).thenReturn (endingCombat);
		
		// Set up object to test
		final PlayerMessageProcessing msgProc = mock (PlayerMessageProcessing.class);
		
		final CombatSchedulerImpl scheduler = new CombatSchedulerImpl ();
		scheduler.setScheduledCombatUtils (scheduledCombatUtils);
		scheduler.setPlayerMessageProcessing (msgProc);
		scheduler.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		scheduler.processEndOfScheduledCombat (endingCombat.getScheduledCombatURN (), winningPlayer, mom);

		// Check combat was removed
		assertEquals (2, gsk.getScheduledCombat ().size ());
		assertSame (otherCombat1, gsk.getScheduledCombat ().get (0));
		assertSame (otherCombat2, gsk.getScheduledCombat ().get (1));
		
		// Check other combat in same place was altered to 'walk in without a fight'
		assertTrue (otherCombat1.isWalkInWithoutAFight ());
		assertFalse (otherCombat2.isWalkInWithoutAFight ());
		
		assertEquals (2, attackingConn.getMessages ().size ());
		
		assertEquals (ScheduledCombatWalkInWithoutAFightMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final ScheduledCombatWalkInWithoutAFightMessage msg1 = (ScheduledCombatWalkInWithoutAFightMessage) attackingConn.getMessages ().get (0);
		assertEquals (102, msg1.getScheduledCombatURN ());
		
		// This comes from the call to sendScheduledCombats which isn't mocked
		assertEquals (UpdateOtherScheduledCombatsMessage.class.getName (), attackingConn.getMessages ().get (1).getClass ().getName ());
		final UpdateOtherScheduledCombatsMessage msg2 = (UpdateOtherScheduledCombatsMessage) attackingConn.getMessages ().get (1);
		assertEquals (0, msg2.getCombatCount ());
		
		// Check next turn was not triggered
		verify (msgProc, times (0)).endPhase (mom, 0);
	}
}