package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageData;
import momime.common.messages.PendingMovement;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddNewTurnMessagesMessage;
import momime.common.messages.servertoclient.SetCurrentPlayerMessage;
import momime.common.messages.servertoclient.StartSimultaneousTurnMessage;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;

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
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player1 = new PlayerServerDetails (null, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player2 = new PlayerServerDetails (null, null, priv2, null, null);
		
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
		move1.setMoveFrom (new MapCoordinates3DEx (1, 10, 1));
		move1.setMoveTo (new MapCoordinates3DEx (2, 10, 1));
		move1.getPath ().add (3);
		move1.getUnitURN ().add (1);

		final PendingMovement move2 = new PendingMovement ();
		move2.setMoveFrom (new MapCoordinates3DEx (4, 10, 1));
		move2.setMoveTo (new MapCoordinates3DEx (3, 10, 1));
		move2.getPath ().add (7);
		move2.getUnitURN ().add (2);
		
		priv1.getPendingMovement ().add (move1);
		priv2.getPendingMovement ().add (move2);
		
		// Unit searches
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (1, trueMap.getUnit (), "continueMovement")).thenReturn (unit1);
		when (unitUtils.findUnitURN (2, trueMap.getUnit (), "continueMovement")).thenReturn (unit2);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionLogger ()).thenReturn (LogFactory.getLog (TestPlayerMessageProcessingImpl.class));
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up test object
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setUnitUtils (unitUtils);
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		
		// Run method
		proc.continueMovement (0, mom);
		
		// Check results
		verify (midTurn, times (1)).moveUnitStack (unitStack1, player1, false, (MapCoordinates3DEx) move1.getMoveFrom (), (MapCoordinates3DEx) move1.getMoveTo (), false, mom);
		verify (midTurn, times (1)).moveUnitStack (unitStack2, player2, false, (MapCoordinates3DEx) move2.getMoveFrom (), (MapCoordinates3DEx) move2.getMoveTo (), false, mom);
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
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd2.getPlayerID (), "continueMovement")).thenReturn (player2);
		
		// Units
		final MemoryUnit unit1 = new MemoryUnit ();
		final MemoryUnit unit2 = new MemoryUnit ();
		
		final List<MemoryUnit> unitStack1 = new ArrayList<MemoryUnit> ();
		unitStack1.add (unit1);
		
		final List<MemoryUnit> unitStack2 = new ArrayList<MemoryUnit> ();
		unitStack2.add (unit2);
		
		// Pending moves
		final PendingMovement move1 = new PendingMovement ();
		move1.setMoveFrom (new MapCoordinates3DEx (1, 10, 1));
		move1.setMoveTo (new MapCoordinates3DEx (2, 10, 1));
		move1.getPath ().add (3);
		move1.getUnitURN ().add (1);

		final PendingMovement move2 = new PendingMovement ();
		move2.setMoveFrom (new MapCoordinates3DEx (4, 10, 1));
		move2.setMoveTo (new MapCoordinates3DEx (3, 10, 1));
		move2.getPath ().add (7);
		move2.getUnitURN ().add (2);
		
		priv1.getPendingMovement ().add (move1);
		priv2.getPendingMovement ().add (move2);
		
		// Unit searches
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (1, trueMap.getUnit (), "continueMovement")).thenReturn (unit1);
		when (unitUtils.findUnitURN (2, trueMap.getUnit (), "continueMovement")).thenReturn (unit2);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionLogger ()).thenReturn (LogFactory.getLog (TestPlayerMessageProcessingImpl.class));
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up test object
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setUnitUtils (unitUtils);
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		proc.continueMovement (2, mom);
		
		// Check results
		verify (midTurn, times (0)).moveUnitStack (unitStack1, player1, false, (MapCoordinates3DEx) move1.getMoveFrom (), (MapCoordinates3DEx) move1.getMoveTo (), false, mom);
		verify (midTurn, times (1)).moveUnitStack (unitStack2, player2, false, (MapCoordinates3DEx) move2.getMoveFrom (), (MapCoordinates3DEx) move2.getMoveTo (), false, mom);
	}
	
	/**
	 * Tests the findAndProcessOneCellPendingMovement method when there are no pending movements at all 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindAndProcessOneCellPendingMovement_None () throws Exception
	{
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);

		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		
		// Run method
		assertFalse (proc.findAndProcessOneCellPendingMovement (mom));
	}

	/**
	 * Tests the findAndProcessOneCellPendingMovement method when there are pending movements,
	 * but all not applicable for non-combat moves for one reason or another
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindAndProcessOneCellPendingMovement_NoneApplicable () throws Exception
	{
		// Mock empty database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);

		// Needed for setting up units
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);

		// Combat move
		final PendingMovement move1 = new PendingMovement ();
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (2);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final List<MemoryUnit> move1Stack = new ArrayList<MemoryUnit> ();
		move1Stack.add (move1Unit);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move1Unit);
		
		final OneCellPendingMovement move1Cell = new OneCellPendingMovement (player1, move1, null, true);	// <--
		when (midTurn.determineOneCellPendingMovement (move1Stack, player1, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move1Cell);
		
		// Move where the unit stack has no movement left
		final PendingMovement move2 = new PendingMovement ();
		priv2.getPendingMovement ().add (move2);

		final MemoryUnit move2Unit = new MemoryUnit ();
		move2Unit.setUnitURN (2);
		move2Unit.setUnitID ("UN001");
		move2Unit.setDoubleOverlandMovesLeft (0);		// <--
		move2.getUnitURN ().add (move2Unit.getUnitURN ());
		
		final List<MemoryUnit> move2Stack = new ArrayList<MemoryUnit> ();
		move2Stack.add (move2Unit);
		
		when (unitUtils.findUnitURN (move2Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move2Unit);
		
		final OneCellPendingMovement move2Cell = new OneCellPendingMovement (player2, move2, null, false);
		when (midTurn.determineOneCellPendingMovement (move2Stack, player2, move2, move2Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move2Cell);
		
		// Move where the destination is unreachable
		final PendingMovement move3 = new PendingMovement ();
		priv2.getPendingMovement ().add (move3);

		final MemoryUnit move3Unit = new MemoryUnit ();
		move3Unit.setUnitURN (3);
		move3Unit.setUnitID ("UN001");
		move3Unit.setDoubleOverlandMovesLeft (2);
		move3.getUnitURN ().add (move3Unit.getUnitURN ());
		
		final List<MemoryUnit> move3Stack = new ArrayList<MemoryUnit> ();
		move3Stack.add (move3Unit);
		
		when (unitUtils.findUnitURN (move3Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move3Unit);
		
		when (midTurn.determineOneCellPendingMovement (move3Stack, player2, move3, move3Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (null);	// <--

		// Movement speed
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitSkillUtils.getModifiedSkillValue (any (MemoryUnit.class), anyListOf (UnitSkillAndValue.class),
			eq (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED), eq (UnitSkillComponent.ALL), eq (UnitSkillPositiveNegative.BOTH),
			eq (null), eq (null), eq (players), eq (fow), eq (db))).thenReturn (2);
		
		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
		proc.setUnitSkillUtils (unitSkillUtils);
		
		// Run method
		assertFalse (proc.findAndProcessOneCellPendingMovement (mom));
		
		// Check that only the unreachable move was removed
		assertEquals (1, priv1.getPendingMovement ().size ());
		assertSame (move1, priv1.getPendingMovement ().get (0));

		assertEquals (1, priv2.getPendingMovement ().size ());
		assertSame (move2, priv2.getPendingMovement ().get (0));
	}

	/**
	 * Tests the findAndProcessOneCellPendingMovement method when there are a couple of pending movements and we randomly select one
	 * and take a step towards it, but don't reach the destination
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindAndProcessOneCellPendingMovement_Step () throws Exception
	{
		// Mock empty database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);

		// Needed for setting up units
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);

		// Combat move
		final PendingMovement move1 = new PendingMovement ();
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (1);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final List<MemoryUnit> move1Stack = new ArrayList<MemoryUnit> ();
		move1Stack.add (move1Unit);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move1Unit);
		
		final OneCellPendingMovement move1Cell = new OneCellPendingMovement (player1, move1, null, false);
		when (midTurn.determineOneCellPendingMovement (move1Stack, player1, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move1Cell);
		
		// Move where the unit stack has no movement left
		final PendingMovement move2 = new PendingMovement ();
		priv2.getPendingMovement ().add (move2);

		final MemoryUnit move2Unit = new MemoryUnit ();
		move2Unit.setUnitURN (2);
		move2Unit.setUnitID ("UN001");
		move2Unit.setDoubleOverlandMovesLeft (2);
		move2.getUnitURN ().add (move2Unit.getUnitURN ());
		
		final List<MemoryUnit> move2Stack = new ArrayList<MemoryUnit> ();
		move2Stack.add (move2Unit);
		
		when (unitUtils.findUnitURN (move2Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move2Unit);
		
		final OneCellPendingMovement move2Cell = new OneCellPendingMovement (player2, move2, new MapCoordinates3DEx (21, 11, 1), false);
		when (midTurn.determineOneCellPendingMovement (move2Stack, player2, move2, move2Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move2Cell);
		
		// Move where the destination is unreachable
		final PendingMovement move3 = new PendingMovement ();
		priv2.getPendingMovement ().add (move3);

		final MemoryUnit move3Unit = new MemoryUnit ();
		move3Unit.setUnitURN (3);
		move3Unit.setUnitID ("UN001");
		move3Unit.setDoubleOverlandMovesLeft (3);
		move3.getUnitURN ().add (move3Unit.getUnitURN ());
		
		final List<MemoryUnit> move3Stack = new ArrayList<MemoryUnit> ();
		move3Stack.add (move3Unit);
		
		when (unitUtils.findUnitURN (move3Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move3Unit);
		
		final OneCellPendingMovement move3Cell = new OneCellPendingMovement (player2, move3, null, false);
		when (midTurn.determineOneCellPendingMovement (move3Stack, player2, move3, move3Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move3Cell);
		
		// Movement speed
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitSkillUtils.getModifiedSkillValue (any (MemoryUnit.class), anyListOf (UnitSkillAndValue.class),
			eq (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED), eq (UnitSkillComponent.ALL), eq (UnitSkillPositiveNegative.BOTH),
			eq (null), eq (null), eq (players), eq (fow), eq (db))).thenReturn (2);
		
		// List gets built up as 1, 1, 2, 2, 3, 3 so pick the last 2
		// (This proves that it works via total, not remaining move, because then the list would be 1, 2, 2, 3, 3, 3)
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (6)).thenReturn (3);
		
		// The one we're going to pick needs more info against it
		move2.setMoveFrom (new MapCoordinates3DEx (20, 10, 1));
		move2.setMoveTo (new MapCoordinates3DEx (22, 12, 1));
		
		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
		proc.setUnitSkillUtils (unitSkillUtils);
		proc.setRandomUtils (random);
		
		// Run method
		assertTrue (proc.findAndProcessOneCellPendingMovement (mom));
		
		// Check that all movements were retained
		assertEquals (1, priv1.getPendingMovement ().size ());
		assertSame (move1, priv1.getPendingMovement ().get (0));

		assertEquals (2, priv2.getPendingMovement ().size ());
		assertSame (move2, priv2.getPendingMovement ().get (0));
		assertSame (move3, priv2.getPendingMovement ().get (1));
		
		assertEquals (new MapCoordinates3DEx (21, 11, 1), move2.getMoveFrom ());	// <-- This moved forward one step
		assertEquals (new MapCoordinates3DEx (22, 12, 1), move2.getMoveTo ());
		
		// Check the units actually moved
		verify (midTurn, times (1)).moveUnitStack (move2Stack, player2, true, new MapCoordinates3DEx (20, 10, 1), new MapCoordinates3DEx (21, 11, 1), false, mom);
	}

	/**
	 * Tests the findAndProcessOneCellPendingMovement method when there are a couple of pending movements
	 * and we randomly select one and reach the destination
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindAndProcessOneCellPendingMovement_Reach () throws Exception
	{
		// Mock empty database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);

		// Needed for setting up units
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);

		// Combat move
		final PendingMovement move1 = new PendingMovement ();
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (1);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final List<MemoryUnit> move1Stack = new ArrayList<MemoryUnit> ();
		move1Stack.add (move1Unit);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move1Unit);
		
		final OneCellPendingMovement move1Cell = new OneCellPendingMovement (player1, move1, null, false);
		when (midTurn.determineOneCellPendingMovement (move1Stack, player1, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move1Cell);
		
		// Move where the unit stack has no movement left
		final PendingMovement move2 = new PendingMovement ();
		priv2.getPendingMovement ().add (move2);

		final MemoryUnit move2Unit = new MemoryUnit ();
		move2Unit.setUnitURN (2);
		move2Unit.setUnitID ("UN001");
		move2Unit.setDoubleOverlandMovesLeft (2);
		move2.getUnitURN ().add (move2Unit.getUnitURN ());
		
		final List<MemoryUnit> move2Stack = new ArrayList<MemoryUnit> ();
		move2Stack.add (move2Unit);
		
		when (unitUtils.findUnitURN (move2Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move2Unit);
		
		final OneCellPendingMovement move2Cell = new OneCellPendingMovement (player2, move2, new MapCoordinates3DEx (22, 12, 1), false);
		when (midTurn.determineOneCellPendingMovement (move2Stack, player2, move2, move2Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move2Cell);
		
		// Move where the destination is unreachable
		final PendingMovement move3 = new PendingMovement ();
		priv2.getPendingMovement ().add (move3);

		final MemoryUnit move3Unit = new MemoryUnit ();
		move3Unit.setUnitURN (3);
		move3Unit.setUnitID ("UN001");
		move3Unit.setDoubleOverlandMovesLeft (3);
		move3.getUnitURN ().add (move3Unit.getUnitURN ());
		
		final List<MemoryUnit> move3Stack = new ArrayList<MemoryUnit> ();
		move3Stack.add (move3Unit);
		
		when (unitUtils.findUnitURN (move3Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move3Unit);
		
		final OneCellPendingMovement move3Cell = new OneCellPendingMovement (player2, move3, null, false);
		when (midTurn.determineOneCellPendingMovement (move3Stack, player2, move3, move3Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move3Cell);

		// Movement speed
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitSkillUtils.getModifiedSkillValue (any (MemoryUnit.class), anyListOf (UnitSkillAndValue.class),
			eq (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED), eq (UnitSkillComponent.ALL), eq (UnitSkillPositiveNegative.BOTH),
			eq (null), eq (null), eq (players), eq (fow), eq (db))).thenReturn (2);
		
		// List gets built up as 1, 1, 2, 2, 3, 3 so pick the last 2
		// (This proves that it works via total, not remaining move, because then the list would be 1, 2, 2, 3, 3, 3)
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (6)).thenReturn (3);
		
		// The one we're going to pick needs more info against it
		move2.setMoveFrom (new MapCoordinates3DEx (21, 11, 1));
		move2.setMoveTo (new MapCoordinates3DEx (22, 12, 1));
		
		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
		proc.setUnitSkillUtils (unitSkillUtils);
		proc.setRandomUtils (random);
		
		// Run method
		assertTrue (proc.findAndProcessOneCellPendingMovement (mom));
		
		// Check the pending move that was processed was removed
		assertEquals (1, priv1.getPendingMovement ().size ());
		assertSame (move1, priv1.getPendingMovement ().get (0));

		assertEquals (1, priv2.getPendingMovement ().size ());
		assertSame (move3, priv2.getPendingMovement ().get (0));
		
		// Check the units actually moved
		verify (midTurn, times (1)).moveUnitStack (move2Stack, player2, true, new MapCoordinates3DEx (21, 11, 1), new MapCoordinates3DEx (22, 12, 1), false, mom);
	}

	/**
	 * Tests the findAndProcessOneCombat method when there are no pending movements at all 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindAndProcessOneCombat_None () throws Exception
	{
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);

		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		
		// Run method
		assertFalse (proc.findAndProcessOneCombat (mom));
	}

	/**
	 * Tests the findAndProcessOneCombat method when there is a pending movement with an unreachable destination.
	 * This is an error because these should already have been removed by findAndProcessOneCellPendingMovement.
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testFindAndProcessOneCombat_Unreachable () throws Exception
	{
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Needed for setting up units
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Unreachable
		final PendingMovement move1 = new PendingMovement ();
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (2);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final List<MemoryUnit> move1Stack = new ArrayList<MemoryUnit> ();
		move1Stack.add (move1Unit);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move1Unit);
		
		when (midTurn.determineOneCellPendingMovement (move1Stack, player2, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (null);

		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
		
		// Run method
		assertFalse (proc.findAndProcessOneCombat (mom));
	}

	/**
	 * Tests the findAndProcessOneCombat method when there is a pending movement for a non-combat move.
	 * This is an error because these should already have been dealt with by findAndProcessOneCellPendingMovement.
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testFindAndProcessOneCombat_Move () throws Exception
	{
		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Needed for setting up units
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Normal move
		final PendingMovement move1 = new PendingMovement ();
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (2);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final List<MemoryUnit> move1Stack = new ArrayList<MemoryUnit> ();
		move1Stack.add (move1Unit);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move1Unit);
		
		final OneCellPendingMovement move1Cell = new OneCellPendingMovement (player1, move1, null, false);
		when (midTurn.determineOneCellPendingMovement (move1Stack, player1, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move1Cell);

		// Set up test object
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
		
		// Run method
		assertFalse (proc.findAndProcessOneCombat (mom));
	}

	/**
	 * Tests the findAndProcessOneCombat method when there are a couple of regular combats to choose between
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindAndProcessOneCombat_Normal () throws Exception
	{
		// Mock empty database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Needed for setting up units
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// 1st Combat
		final PendingMovement move1 = new PendingMovement ();
		move1.setMoveFrom (new MapCoordinates3DEx (20, 10, 1));
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (2);
		move1Unit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		move1Unit.setStatus (UnitStatusID.ALIVE);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final List<MemoryUnit> move1Stack = new ArrayList<MemoryUnit> ();
		move1Stack.add (move1Unit);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move1Unit);
		
		final OneCellPendingMovement move1Cell = new OneCellPendingMovement (player1, move1, new MapCoordinates3DEx (21, 10, 1), true);
		when (midTurn.determineOneCellPendingMovement (move1Stack, player1, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move1Cell);
		
		// Another unit in the same location who isn't moving
		final MemoryUnit attackedUnit = new MemoryUnit ();
		attackedUnit.setUnitURN (4);
		attackedUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		attackedUnit.setStatus (UnitStatusID.ALIVE);

		// 2nd Combat - note these are attacking the location the 1st stack is moving *from*, so attacks only the non-moving unit
		final PendingMovement move2 = new PendingMovement ();
		move2.setMoveFrom (new MapCoordinates3DEx (20, 11, 1));
		priv2.getPendingMovement ().add (move2);

		final MemoryUnit move2Unit = new MemoryUnit ();
		move2Unit.setUnitURN (2);
		move2Unit.setUnitID ("UN001");
		move2Unit.setDoubleOverlandMovesLeft (2);
		move2.getUnitURN ().add (move2Unit.getUnitURN ());
		
		final List<MemoryUnit> move2Stack = new ArrayList<MemoryUnit> ();
		move2Stack.add (move2Unit);
		
		when (unitUtils.findUnitURN (move2Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move2Unit);
		
		final OneCellPendingMovement move2Cell = new OneCellPendingMovement (player2, move2, new MapCoordinates3DEx (20, 10, 1), true);
		when (midTurn.determineOneCellPendingMovement (move2Stack, player2, move2, move2Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move2Cell);

		// 3rd Combat
		final PendingMovement move3 = new PendingMovement ();
		move3.setMoveFrom (new MapCoordinates3DEx (20, 12, 1));
		priv2.getPendingMovement ().add (move3);

		final MemoryUnit move3Unit = new MemoryUnit ();
		move3Unit.setUnitURN (3);
		move3Unit.setUnitID ("UN001");
		move3Unit.setDoubleOverlandMovesLeft (2);
		move3.getUnitURN ().add (move3Unit.getUnitURN ());
		
		final List<MemoryUnit> move3Stack = new ArrayList<MemoryUnit> ();
		move3Stack.add (move3Unit);
		
		when (unitUtils.findUnitURN (move3Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move3Unit);
		
		final OneCellPendingMovement move3Cell = new OneCellPendingMovement (player2, move3, new MapCoordinates3DEx (21, 12, 1), true);
		when (midTurn.determineOneCellPendingMovement (move3Stack, player2, move3, move3Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move3Cell);
		
		// List all the units
		fow.getUnit ().add (move1Unit);
		fow.getUnit ().add (attackedUnit);
		fow.getUnit ().add (move2Unit);
		fow.getUnit ().add (move3Unit);

		// Pick combat 2
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (3)).thenReturn (1);
		
		// Set up test object
		final CombatStartAndEnd combatStartAndEnd = mock (CombatStartAndEnd.class);
		
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
		proc.setRandomUtils (random);
		proc.setCombatStartAndEnd (combatStartAndEnd);
		
		// Run method
		assertTrue (proc.findAndProcessOneCombat (mom));
		
		// Check the right combat was started with the right units
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		attackingUnitURNs.add (move2Unit.getUnitURN ());
		
		final List<Integer> defendingUnitURNs = new ArrayList<Integer> ();
		defendingUnitURNs.add (attackedUnit.getUnitURN ());
		
		verify (combatStartAndEnd, times (1)).startCombat (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates3DEx (20, 11, 1),
			attackingUnitURNs, defendingUnitURNs, move2, null, mom);
	}

	/**
	 * Tests the findAndProcessOneCombat method when there is a border conflict in addition to a normal combat
	 * so the border conflict always gets chosen in preference
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindAndProcessOneCombat_BorderConflict () throws Exception
	{
		// Mock empty database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Players
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);

		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);	

		// Session variables
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (map);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Needed for setting up units
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// 1st Combat - attacking the 2nd stack
		final PendingMovement move1 = new PendingMovement ();
		move1.setMoveFrom (new MapCoordinates3DEx (20, 10, 1));
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (2);
		move1Unit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		move1Unit.setStatus (UnitStatusID.ALIVE);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final List<MemoryUnit> move1Stack = new ArrayList<MemoryUnit> ();
		move1Stack.add (move1Unit);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move1Unit);
		
		final OneCellPendingMovement move1Cell = new OneCellPendingMovement (player1, move1, new MapCoordinates3DEx (20, 11, 1), true);
		when (midTurn.determineOneCellPendingMovement (move1Stack, player1, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move1Cell);
		
		// Another unit in the same location who isn't moving
		final MemoryUnit attackedUnit = new MemoryUnit ();
		attackedUnit.setUnitURN (4);
		attackedUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		attackedUnit.setStatus (UnitStatusID.ALIVE);

		// 2nd Combat - attacking the 1st stack
		final PendingMovement move2 = new PendingMovement ();
		move2.setMoveFrom (new MapCoordinates3DEx (20, 11, 1));
		priv2.getPendingMovement ().add (move2);

		final MemoryUnit move2Unit = new MemoryUnit ();
		move2Unit.setUnitURN (2);
		move2Unit.setUnitID ("UN001");
		move2Unit.setDoubleOverlandMovesLeft (2);
		move2.getUnitURN ().add (move2Unit.getUnitURN ());
		
		final List<MemoryUnit> move2Stack = new ArrayList<MemoryUnit> ();
		move2Stack.add (move2Unit);
		
		when (unitUtils.findUnitURN (move2Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move2Unit);
		
		final OneCellPendingMovement move2Cell = new OneCellPendingMovement (player2, move2, new MapCoordinates3DEx (20, 10, 1), true);
		when (midTurn.determineOneCellPendingMovement (move2Stack, player2, move2, move2Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move2Cell);

		// 3rd Combat
		final PendingMovement move3 = new PendingMovement ();
		move3.setMoveFrom (new MapCoordinates3DEx (20, 12, 1));
		priv2.getPendingMovement ().add (move3);

		final MemoryUnit move3Unit = new MemoryUnit ();
		move3Unit.setUnitURN (3);
		move3Unit.setUnitID ("UN001");
		move3Unit.setDoubleOverlandMovesLeft (2);
		move3.getUnitURN ().add (move3Unit.getUnitURN ());
		
		final List<MemoryUnit> move3Stack = new ArrayList<MemoryUnit> ();
		move3Stack.add (move3Unit);
		
		when (unitUtils.findUnitURN (move3Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move3Unit);
		
		final OneCellPendingMovement move3Cell = new OneCellPendingMovement (player2, move3, new MapCoordinates3DEx (21, 12, 1), true);
		when (midTurn.determineOneCellPendingMovement (move3Stack, player2, move3, move3Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move3Cell);
		
		// List all the units
		fow.getUnit ().add (move1Unit);
		fow.getUnit ().add (attackedUnit);
		fow.getUnit ().add (move2Unit);
		fow.getUnit ().add (move3Unit);

		// Set up test object
		final CombatStartAndEnd combatStartAndEnd = mock (CombatStartAndEnd.class);
		
		final PlayerMessageProcessingImpl proc = new PlayerMessageProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
		proc.setRandomUtils (mock (RandomUtils.class));
		proc.setCombatStartAndEnd (combatStartAndEnd);
		
		// Run method
		assertTrue (proc.findAndProcessOneCombat (mom));
		
		// Check the right combat was started with the right units
		// Note for regular combat, its the unit who *isn't* moving that gets attacked and the moving one walks away
		// Here the moving one (that is counterattacking us) attacks and the unit who isn't moving isn't involved
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		attackingUnitURNs.add (move2Unit.getUnitURN ());
		
		final List<Integer> defendingUnitURNs = new ArrayList<Integer> ();
		defendingUnitURNs.add (move1Unit.getUnitURN ());
		
		verify (combatStartAndEnd, times (1)).startCombat (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates3DEx (20, 11, 1),
			attackingUnitURNs, defendingUnitURNs, move2, move1, mom);
	}
}