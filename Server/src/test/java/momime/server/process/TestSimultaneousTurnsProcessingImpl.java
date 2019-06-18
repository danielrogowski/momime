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

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.areas.storage.MapArea2DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.OverlandMapSize;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PendingMovement;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.database.PlaneSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.TileTypeSvr;
import momime.server.database.UnitSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.UnitServerUtils;

/**
 * Tests the SimultaneousTurnsProcessingImpl class
 */
public final class TestSimultaneousTurnsProcessingImpl extends ServerTestData
{
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
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
		
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
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move1Unit, null, null, null, players, fow, db)).thenReturn (xu1);
		
		final List<ExpandedUnitDetails> move1Stack = new ArrayList<ExpandedUnitDetails> ();
		move1Stack.add (xu1);
		
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
		
		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move2Unit, null, null, null, players, fow, db)).thenReturn (xu2);
		
		final List<ExpandedUnitDetails> move2Stack = new ArrayList<ExpandedUnitDetails> ();
		move2Stack.add (xu2);
		
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

		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move3Unit, null, null, null, players, fow, db)).thenReturn (xu3);
		
		final List<ExpandedUnitDetails> move3Stack = new ArrayList<ExpandedUnitDetails> ();
		move3Stack.add (xu3);
		
		when (unitUtils.findUnitURN (move3Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move3Unit);
		
		when (midTurn.determineOneCellPendingMovement (move3Stack, player2, move3, move3Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (null);	// <--

		// Movement speed
		when (xu1.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		when (xu2.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		when (xu3.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		
		// Set up test object
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
		
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
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move1Unit, null, null, null, players, fow, db)).thenReturn (xu1);
		
		final List<ExpandedUnitDetails> move1Stack = new ArrayList<ExpandedUnitDetails> ();
		move1Stack.add (xu1);
		
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
		
		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move2Unit, null, null, null, players, fow, db)).thenReturn (xu2);
		
		final List<ExpandedUnitDetails> move2Stack = new ArrayList<ExpandedUnitDetails> ();
		move2Stack.add (xu2);
		
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
		
		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move3Unit, null, null, null, players, fow, db)).thenReturn (xu3);
		
		final List<ExpandedUnitDetails> move3Stack = new ArrayList<ExpandedUnitDetails> ();
		move3Stack.add (xu3);
		
		when (unitUtils.findUnitURN (move3Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move3Unit);
		
		final OneCellPendingMovement move3Cell = new OneCellPendingMovement (player2, move3, null, false);
		when (midTurn.determineOneCellPendingMovement (move3Stack, player2, move3, move3Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move3Cell);
		
		// Movement speed
		when (xu1.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		when (xu2.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		when (xu3.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		
		// List gets built up as 1, 1, 2, 2, 3, 3 so pick the last 2
		// (This proves that it works via total, not remaining move, because then the list would be 1, 2, 2, 3, 3, 3)
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (6)).thenReturn (3);
		
		// The one we're going to pick needs more info against it
		move2.setMoveFrom (new MapCoordinates3DEx (20, 10, 1));
		move2.setMoveTo (new MapCoordinates3DEx (22, 12, 1));
		
		// Set up test object
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
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
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move1Unit, null, null, null, players, fow, db)).thenReturn (xu1);
		
		final List<ExpandedUnitDetails> move1Stack = new ArrayList<ExpandedUnitDetails> ();
		move1Stack.add (xu1);
		
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
		
		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move2Unit, null, null, null, players, fow, db)).thenReturn (xu2);
		
		final List<ExpandedUnitDetails> move2Stack = new ArrayList<ExpandedUnitDetails> ();
		move2Stack.add (xu2);
		
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
		
		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move3Unit, null, null, null, players, fow, db)).thenReturn (xu3);
		
		final List<ExpandedUnitDetails> move3Stack = new ArrayList<ExpandedUnitDetails> ();
		move3Stack.add (xu3);
		
		when (unitUtils.findUnitURN (move3Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCellPendingMovement")).thenReturn (move3Unit);
		
		final OneCellPendingMovement move3Cell = new OneCellPendingMovement (player2, move3, null, false);
		when (midTurn.determineOneCellPendingMovement (move3Stack, player2, move3, move3Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move3Cell);

		// Movement speed
		when (xu1.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		when (xu2.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		when (xu3.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		
		// List gets built up as 1, 1, 2, 2, 3, 3 so pick the last 2
		// (This proves that it works via total, not remaining move, because then the list would be 1, 2, 2, 3, 3, 3)
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (6)).thenReturn (3);
		
		// The one we're going to pick needs more info against it
		move2.setMoveFrom (new MapCoordinates3DEx (21, 11, 1));
		move2.setMoveTo (new MapCoordinates3DEx (22, 12, 1));
		
		// Set up test object
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
		proc.setFogOfWarMidTurnMultiChanges (midTurn);
		proc.setUnitUtils (unitUtils);
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
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
		
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
		
		// Unreachable
		final PendingMovement move1 = new PendingMovement ();
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (2);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move1Unit, null, null, null, players, fow, db)).thenReturn (xu1);
		
		final List<ExpandedUnitDetails> move1Stack = new ArrayList<ExpandedUnitDetails> ();
		move1Stack.add (xu1);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move1Unit);
		
		when (midTurn.determineOneCellPendingMovement (move1Stack, player2, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (null);

		// Set up test object
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
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
		
		// Normal move
		final PendingMovement move1 = new PendingMovement ();
		priv1.getPendingMovement ().add (move1);

		final MemoryUnit move1Unit = new MemoryUnit ();
		move1Unit.setUnitURN (1);
		move1Unit.setUnitID ("UN001");
		move1Unit.setDoubleOverlandMovesLeft (2);
		move1.getUnitURN ().add (move1Unit.getUnitURN ());
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move1Unit, null, null, null, players, fow, db)).thenReturn (xu1);
		
		final List<ExpandedUnitDetails> move1Stack = new ArrayList<ExpandedUnitDetails> ();
		move1Stack.add (xu1);
		
		when (unitUtils.findUnitURN (move1Unit.getUnitURN (), fow.getUnit (), "findAndProcessOneCombat")).thenReturn (move1Unit);
		
		final OneCellPendingMovement move1Cell = new OneCellPendingMovement (player1, move1, null, false);
		when (midTurn.determineOneCellPendingMovement (move1Stack, player1, move1, move1Unit.getDoubleOverlandMovesLeft (), mom)).thenReturn (move1Cell);

		// Set up test object
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
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
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move1Unit, null, null, null, players, fow, db)).thenReturn (xu1);
		
		final List<ExpandedUnitDetails> move1Stack = new ArrayList<ExpandedUnitDetails> ();
		move1Stack.add (xu1);
		
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
		
		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move2Unit, null, null, null, players, fow, db)).thenReturn (xu2);
		
		final List<ExpandedUnitDetails> move2Stack = new ArrayList<ExpandedUnitDetails> ();
		move2Stack.add (xu2);
		
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
		
		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move3Unit, null, null, null, players, fow, db)).thenReturn (xu3);
		
		final List<ExpandedUnitDetails> move3Stack = new ArrayList<ExpandedUnitDetails> ();
		move3Stack.add (xu3);
		
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
		
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
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
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = createOverlandMap (sys);
		
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
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move1Unit, null, null, null, players, fow, db)).thenReturn (xu1);
		
		final List<ExpandedUnitDetails> move1Stack = new ArrayList<ExpandedUnitDetails> ();
		move1Stack.add (xu1);
		
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
		
		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move2Unit, null, null, null, players, fow, db)).thenReturn (xu2);
		
		final List<ExpandedUnitDetails> move2Stack = new ArrayList<ExpandedUnitDetails> ();
		move2Stack.add (xu2);
		
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
		
		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (move3Unit, null, null, null, players, fow, db)).thenReturn (xu3);
		
		final List<ExpandedUnitDetails> move3Stack = new ArrayList<ExpandedUnitDetails> ();
		move3Stack.add (xu3);
		
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
		
		final SimultaneousTurnsProcessingImpl proc = new SimultaneousTurnsProcessingImpl ();
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
		
		final UnitSvr normalUnitDef = new UnitSvr ();
		normalUnitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "processSpecialOrders-d")).thenReturn (normalUnitDef);
		
		final UnitSvr heroUnitDef = new UnitSvr ();
		heroUnitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		when (db.findUnit ("UN002", "processSpecialOrders-d")).thenReturn (heroUnitDef);

		final PlaneSvr arcanus = new PlaneSvr ();
		final PlaneSvr myrror = new PlaneSvr ();
		myrror.setPlaneNumber (1);
		
		final List<PlaneSvr> planes = new ArrayList<PlaneSvr> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlanes ()).thenReturn (planes);
		
		final TileTypeSvr tt = new TileTypeSvr ();
		tt.setCanBuildCity (true);
		when (db.findTileType ("TT01", "processSpecialOrders-t")).thenReturn (tt);
		
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setFogOfWarSetting (fogOfWarSettings);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
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
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd1.getPlayerID (), "processSpecialOrders-s")).thenReturn (player1);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd2.getPlayerID (), "processSpecialOrders-s")).thenReturn (player2);
		
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
		
		final MemoryBuilding trueBuilding = new MemoryBuilding ();
		trueBuilding.setBuildingURN (6);
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, tc.getBuildingIdSoldThisTurn ())).thenReturn (trueBuilding);
		
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
		falseArea.setCoordinateSystem (overlandMapSize);
		
		final MapArea2D<Boolean> trueArea = new MapArea2DArrayListImpl<Boolean> (); 
		trueArea.setCoordinateSystem (overlandMapSize);
		
		for (int x = 0; x < overlandMapSize.getWidth (); x++)
			for (int y = 0; y < overlandMapSize.getHeight (); y++)
			{
				falseArea.set (x, y, false);
				trueArea.set (x, y, true);
			}
		
		final CityCalculations cityCalc = mock (CityCalculations.class);
		when (cityCalc.markWithinExistingCityRadius (trueTerrain, 1, overlandMapSize)).thenReturn (falseArea, trueArea);
		
		// Player2 has 2 spirits he's trying to take a node from Player1 with; first fails, second succeeds, so third doesn't need to try
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final List<MemoryUnit> spirits = new ArrayList<MemoryUnit> ();
		final List<ExpandedUnitDetails> xuSpirits = new ArrayList<ExpandedUnitDetails> ();
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit spirit = new MemoryUnit ();
			spirit.setOwningPlayerID (pd2.getPlayerID ());
			spirit.setUnitLocation (new MapCoordinates3DEx (50, 20, 0));
			spirits.add (spirit);

			final ExpandedUnitDetails xuSpirit = mock (ExpandedUnitDetails.class);
			when (unitUtils.expandUnitDetails (spirit, null, null, null, players, trueMap, db)).thenReturn (xuSpirit);
			xuSpirits.add (xuSpirit);
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
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setRandomUtils (randomUtils);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setUnitUtils (unitUtils);
		
		// Run method
		proc.processSpecialOrders (mom);
		
		// Check units were dismissed
		verify (midTurn, times (1)).killUnitOnServerAndClients (dismissNormalUnit, KillUnitActionID.DISMISS, trueMap, players, fogOfWarSettings, db);
		verify (midTurn, times (1)).killUnitOnServerAndClients (dismissHeroUnit, KillUnitActionID.DISMISS, trueMap, players, fogOfWarSettings, db);
		
		// Check buildings were sold
		verify (cityProc, times (1)).sellBuilding (trueMap, players, cityLocation, trueBuilding.getBuildingURN (), false, true, sd, db);
		
		// Check only 1 settler was allowed to build
		verify (cityServerUtils, times (0)).buildCityFromSettler (gsk, player1, settler1, players, sd, db);
		verify (cityServerUtils, times (1)).buildCityFromSettler (gsk, player2, settler2, players, sd, db);
		
		assertEquals (0, conn2.getMessages ().size ());
		assertEquals (1, conn1.getMessages ().size ());
		assertEquals (TextPopupMessage.class.getName (), conn1.getMessages ().get (0).getClass ().getName ());
		final TextPopupMessage popup = (TextPopupMessage) conn1.getMessages ().get (0);
		assertEquals ("Another city was built before yours and is within 3 squares of where you are trying to build, so you cannot build here anymore", popup.getText ());
		
		// Both spirits tried to meld (the melding method is mocked, so we don't even know whether they succeeded)
		for (final ExpandedUnitDetails xuSpirit : xuSpirits)
			verify (overlandMapServerUtils, times (1)).attemptToMeldWithNode (xuSpirit, trueMap, players, sd, db);
	}
}