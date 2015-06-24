package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.OverlandMapSize;
import momime.common.database.UnitCombatSideID;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PendingMovement;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.CombatEndedMessage;
import momime.common.messages.servertoclient.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.StartCombatMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.PlaneSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CombatStartAndEndImpl class
 */
public final class TestCombatStartAndEndImpl
{
	/**
	 * Tests the startCombat method with a null list of attackers
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testStartCombat_NullAttackerList () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		
		// Run method
		cse.startCombat (defendingLocation, attackingFrom, null, null, null, null, mom);
	}

	/**
	 * Tests the startCombat method with an empty list of attackers
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testStartCombat_EmptyAttackerList () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		
		// Run method
		cse.startCombat (defendingLocation, attackingFrom, new ArrayList<Integer> (), null, null, null, mom);
	}

	/**
	 * Tests the startCombat method for the normal situation of one unit stack attacking another on the open map in a one-player-at-a-time game
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_OnePlayerAtATime () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		// Have to put *something* in here to make the two lists different, so the mocks work
		attackingPriv.getResourceValue ().add (null);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "startCombat-A")).thenReturn (attackingPlayer);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "startCombat-D")).thenReturn (defendingPlayer);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		for (int n = 1; n <= 3; n++)
		{
			if (n < 3)
				attackingUnitURNs.add (n);
			
			final MemoryUnit attackingUnit = new MemoryUnit ();
			attackingUnit.setOwningPlayerID (attackingPd.getPlayerID ());
			when (unitUtils.findUnitURN (n, trueMap.getUnit (), "startCombat-A")).thenReturn (attackingUnit);
		}
		
		// Defender has a unit (otherwise there's no defender)
		final MemoryUnit defendingUnit = new MemoryUnit ();
		defendingUnit.setOwningPlayerID (defendingPd.getPlayerID ());
		
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (defendingUnit);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		// Combat map generator
		final CombatMapSize combatMapCoordinateSystem = ServerTestData.createCombatMapSize ();
		sd.setCombatMapSize (combatMapCoordinateSystem);
		
		final CombatMapGenerator mapGen = mock (CombatMapGenerator.class);
		when (mapGen.generateCombatMap (combatMapCoordinateSystem, db, trueMap, defendingLocation)).thenReturn (new MapAreaOfCombatTiles ());
		
		// Casting skill of each player
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.calculateCastingSkillOfPlayer (attackingPriv.getResourceValue ())).thenReturn (28);
		when (resourceValueUtils.calculateCastingSkillOfPlayer (defendingPriv.getResourceValue ())).thenReturn (22);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setUnitUtils (unitUtils);
		cse.setCombatMapGenerator (mapGen);
		cse.setCombatProcessing (combatProcessing);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		cse.startCombat (defendingLocation, attackingFrom, attackingUnitURNs, null, null, null, mom);
		
		// Check that a map got generated
		final ServerGridCellEx tc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		assertNotNull (tc.getCombatMap ());
		assertEquals (28, tc.getCombatAttackerCastingSkillRemaining ().intValue ());
		assertEquals (22, tc.getCombatDefenderCastingSkillRemaining ().intValue ());
		assertNull (tc.getCombatAttackerPendingMovement ());
		assertNull (tc.getCombatDefenderPendingMovement ());

		// Check the messages sent to the client were correct
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (StartCombatMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final StartCombatMessage msg = (StartCombatMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (defendingLocation, msg.getCombatLocation ());
		assertSame (tc.getCombatMap (), msg.getCombatTerrain ());
		assertEquals (0, msg.getUnitPlacement ().size ());		// Is zero because these are added by positionCombatUnits (), which is mocked out
		
		// Check that units were added into combat on both sides
		verify (combatProcessing, times (1)).positionCombatUnits (defendingLocation, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, defendingLocation,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_ROWS, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FACING,
			UnitCombatSideID.DEFENDER, null, tc.getCombatMap (), mom);

		verify (combatProcessing, times (1)).positionCombatUnits (defendingLocation, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, attackingFrom,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_ROWS, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, attackingUnitURNs, tc.getCombatMap (), mom);
		
		// Check the combat was started
		verify (combatProcessing, times (1)).progressCombat (defendingLocation, true, false, mom);
	}

	/**
	 * Tests the startCombat method for a border conflict in a simultaneous turns game
	 * This is different from the the OnePlayerAtATime test in two ways:
	 * 1) CombatAttackerPendingMovement + CombatDefenderPendingMovement are set to real values
	 * 2) We supply a list of defendingUnitURNs 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_SimultaneousTurns_BorderConflict () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.SIMULTANEOUS);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		// Have to put *something* in here to make the two lists different, so the mocks work
		attackingPriv.getResourceValue ().add (null);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "startCombat-A")).thenReturn (attackingPlayer);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "startCombat-D")).thenReturn (defendingPlayer);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		for (int n = 1; n <= 3; n++)
		{
			if (n < 3)
				attackingUnitURNs.add (n);
			
			final MemoryUnit attackingUnit = new MemoryUnit ();
			attackingUnit.setOwningPlayerID (attackingPd.getPlayerID ());
			when (unitUtils.findUnitURN (n, trueMap.getUnit (), "startCombat-A")).thenReturn (attackingUnit);
		}
		
		// Defender likewise is counterattacking with 2 out of 3 units
		final List<Integer> defendingUnitURNs = new ArrayList<Integer> ();
		for (int n = 4; n <= 6; n++)
		{
			if (n < 6)
				defendingUnitURNs.add (n);
			
			final MemoryUnit defendingUnit = new MemoryUnit ();
			defendingUnit.setOwningPlayerID (defendingPd.getPlayerID ());
			when (unitUtils.findUnitURN (n, trueMap.getUnit (), "startCombat-D")).thenReturn (defendingUnit);
		}

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		// Combat map generator
		final CombatMapSize combatMapCoordinateSystem = ServerTestData.createCombatMapSize ();
		sd.setCombatMapSize (combatMapCoordinateSystem);
		
		final CombatMapGenerator mapGen = mock (CombatMapGenerator.class);
		when (mapGen.generateCombatMap (combatMapCoordinateSystem, db, trueMap, defendingLocation)).thenReturn (new MapAreaOfCombatTiles ());
		
		// Casting skill of each player
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.calculateCastingSkillOfPlayer (attackingPriv.getResourceValue ())).thenReturn (28);
		when (resourceValueUtils.calculateCastingSkillOfPlayer (defendingPriv.getResourceValue ())).thenReturn (22);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setUnitUtils (unitUtils);
		cse.setCombatMapGenerator (mapGen);
		cse.setCombatProcessing (combatProcessing);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		final PendingMovement combatAttackerPendingMovement = new PendingMovement ();
		final PendingMovement combatDefenderPendingMovement = new PendingMovement ();
		cse.startCombat (defendingLocation, attackingFrom, attackingUnitURNs, defendingUnitURNs, combatAttackerPendingMovement, combatDefenderPendingMovement, mom);
		
		// Check that a map got generated
		final ServerGridCellEx tc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		assertNotNull (tc.getCombatMap ());
		assertEquals (28, tc.getCombatAttackerCastingSkillRemaining ().intValue ());
		assertEquals (22, tc.getCombatDefenderCastingSkillRemaining ().intValue ());
		assertSame (combatAttackerPendingMovement, tc.getCombatAttackerPendingMovement ());
		assertSame (combatDefenderPendingMovement, tc.getCombatDefenderPendingMovement ());

		// Check the messages sent to the client were correct
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (StartCombatMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final StartCombatMessage msg = (StartCombatMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (defendingLocation, msg.getCombatLocation ());
		assertSame (tc.getCombatMap (), msg.getCombatTerrain ());
		assertEquals (0, msg.getUnitPlacement ().size ());		// Is zero because these are added by positionCombatUnits (), which is mocked out
		
		// Check that units were added into combat on both sides
		verify (combatProcessing, times (1)).positionCombatUnits (defendingLocation, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, defendingLocation,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_ROWS, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FACING,
			UnitCombatSideID.DEFENDER, defendingUnitURNs, tc.getCombatMap (), mom);

		verify (combatProcessing, times (1)).positionCombatUnits (defendingLocation, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, attackingFrom,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_ROWS, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, attackingUnitURNs, tc.getCombatMap (), mom);
		
		// Check the combat was started
		verify (combatProcessing, times (1)).progressCombat (defendingLocation, true, false, mom);
	}
	
	/**
	 * Tests the startCombat method when attacking an empty city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_EmptyCity () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		// Have to put *something* in here to make the two lists different, so the mocks work
		attackingPriv.getResourceValue ().add (null);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "startCombat-A")).thenReturn (attackingPlayer);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "startCombat-CD")).thenReturn (defendingPlayer);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		for (int n = 1; n <= 3; n++)
		{
			if (n < 3)
				attackingUnitURNs.add (n);

			final MemoryUnit attackingUnit = new MemoryUnit ();
			attackingUnit.setOwningPlayerID (attackingPd.getPlayerID ());
			when (unitUtils.findUnitURN (n, trueMap.getUnit (), "startCombat-A")).thenReturn (attackingUnit);
		}
		
		// No defending units
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (null);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		// There's a city here
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		cityData.setCityOwnerID (defendingPd.getPlayerID ());

		final ServerGridCellEx tc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		tc.setCityData (cityData);
		
		// Combat map generator
		final CombatMapSize combatMapCoordinateSystem = ServerTestData.createCombatMapSize ();
		sd.setCombatMapSize (combatMapCoordinateSystem);
		
		final CombatMapGenerator mapGen = mock (CombatMapGenerator.class);
		when (mapGen.generateCombatMap (combatMapCoordinateSystem, db, trueMap, defendingLocation)).thenReturn (new MapAreaOfCombatTiles ());
		
		// Casting skill of each player
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.calculateCastingSkillOfPlayer (attackingPriv.getResourceValue ())).thenReturn (28);
		when (resourceValueUtils.calculateCastingSkillOfPlayer (defendingPriv.getResourceValue ())).thenReturn (22);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setUnitUtils (unitUtils);
		cse.setCombatMapGenerator (mapGen);
		cse.setCombatProcessing (combatProcessing);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		cse.startCombat (defendingLocation, attackingFrom, attackingUnitURNs, null, null, null, mom);
		
		// Check that a map got generated
		assertNotNull (tc.getCombatMap ());
		assertNull (tc.getCombatAttackerCastingSkillRemaining ());		// These never get set if the combat ends before it even starts
		assertNull (tc.getCombatDefenderCastingSkillRemaining ());
		assertNull (tc.getCombatAttackerPendingMovement ());
		assertNull (tc.getCombatDefenderPendingMovement ());

		// Check the messages sent to the client were correct
		// Generating this message is really all that combatEnded does in this situation, see testCombatEnded_CaptureCityUndecided
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (AskForCaptureCityDecisionMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final AskForCaptureCityDecisionMessage msg = (AskForCaptureCityDecisionMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (defendingLocation, msg.getCityLocation ());
		assertEquals (defendingPd.getPlayerID ().intValue (), msg.getDefendingPlayerID ());
		
		// Check that units were added into combat on both sides
		verify (combatProcessing, times (1)).positionCombatUnits (eq (defendingLocation), any (StartCombatMessage.class), eq (attackingPlayer), isNull (PlayerServerDetails.class),
			eq (combatMapCoordinateSystem), eq (defendingLocation),
			eq (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X), eq (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y),
			eq (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_ROWS), eq (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FACING),
			eq (UnitCombatSideID.DEFENDER), anyListOf (Integer.class), eq (tc.getCombatMap ()), eq (mom));

		verify (combatProcessing, times (1)).positionCombatUnits (eq (defendingLocation), any (StartCombatMessage.class), eq (attackingPlayer), isNull (PlayerServerDetails.class),
			eq (combatMapCoordinateSystem), eq (attackingFrom),
			eq (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X), eq (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y),
			eq (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_ROWS), eq (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING),
			eq (UnitCombatSideID.ATTACKER), eq (attackingUnitURNs), eq (tc.getCombatMap ()), eq (mom));
		
		// Check the combat wasn't started
		verify (combatProcessing, times (0)).progressCombat (defendingLocation, true, false, mom);
	}
	
	/**
	 * Tests the combatEnded method when we're captured a city, but didn't decide whether to capture or raze yet
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureCityUndecided () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (true);
		defendingPd.setPlayerID (5);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// There's a city here, owned by the defender
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl (); 
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check message was sent
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (AskForCaptureCityDecisionMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final AskForCaptureCityDecisionMessage msg = (AskForCaptureCityDecisionMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (combatLocation, msg.getCityLocation ());
		assertEquals (defendingPd.getPlayerID ().intValue (), msg.getDefendingPlayerID ());
	}

	/**
	 * Tests the combatEnded method when a defender AI player won (most interesting stuff like capturing cities,
	 * advancing units, clearing nodes and so on only happens if the attacker won)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_DefenderWon () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = defendingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setServerUnitCalculations (mock (ServerUnitCalculations.class));
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (defendingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
	}

	/**
	 * Tests the combatEnded method when an the attacking player won, and so advances into the combat tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_AttackerWon () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		
		// It isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<MemoryUnit> advancingUnits = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			tu.setStatus (UnitStatusID.ALIVE);
			tu.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
			
			if (n < 3)
			{
				tu.setCombatSide (UnitCombatSideID.ATTACKER);
				tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
				advancingUnits.add (tu);
			}
			
			trueMap.getUnit ().add (tu);
		}
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setServerUnitCalculations (mock (ServerUnitCalculations.class));
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the attacker's units advanced forward
		verify (midTurn, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
	}

	/**
	 * Tests the combatEnded method when a human attacker captures a tower of wizardry using units that were on Myrror
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureTowerFromMyrror () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final PlaneSvr arcanus = new PlaneSvr ();
		final PlaneSvr myrror = new PlaneSvr ();
		myrror.setPlaneNumber (1);
		
		final List<PlaneSvr> planes = new ArrayList<PlaneSvr> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlanes ()).thenReturn (planes);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Location - note the comments in startCombat (), when we're attacking a tower from Myrror, combatLocation.getZ () = 1
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		
		// It's a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (true);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<MemoryUnit> advancingUnits = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			tu.setStatus (UnitStatusID.ALIVE);
			tu.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
			
			if (n < 3)
			{
				tu.setCombatSide (UnitCombatSideID.ATTACKER);
				tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
				advancingUnits.add (tu);
			}
			
			trueMap.getUnit ().add (tu);
		}
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setServerUnitCalculations (mock (ServerUnitCalculations.class));
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the attacker's units advanced forward, in the process jumping to plane 0
		verify (midTurn, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 0), players, trueMap, sd, db);
	}
	
	/**
	 * Tests the combatEnded method when a human attacker captures a city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureCity () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final OverlandMapSize sys = ServerTestData.createOverlandMapSize ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (sys);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setTaxRateID ("TR01");
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		cityData.setCityPopulation (8500);
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		gc.setCityData (cityData);
		
		// Defending player has 1200 coins in total
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (defendingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (1200);
		
		// Defending player has 50000 population in total across 10 or so cities
		final OverlandMapServerUtils overlandMapServerUtils = mock (OverlandMapServerUtils.class);
		when (overlandMapServerUtils.totalPlayerPopulation (trueTerrain, defendingPd.getPlayerID (), sys, db)).thenReturn (50000);
		
		// Calc gold swiped
		final int goldSwiped = (1200 * 8500) / 50000;		// = 204
		
		// It's a city, not a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		// Defender's summoning circle was here (but not their fortress)
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setBuildingURN (3);
		
		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setBuildingURN (4);
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)).thenReturn (summoningCircle);
		
		// How many rebels the city will have after the attacker captures it
		final CityUnrestBreakdown attackerRebels = new CityUnrestBreakdown ();
		attackerRebels.setFinalTotal (2);
		
		final CityCalculations cityCalc = mock (CityCalculations.class);
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), combatLocation, "TR01", db)).thenReturn (attackerRebels);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<MemoryUnit> advancingUnits = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			tu.setStatus (UnitStatusID.ALIVE);
			tu.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
			
			if (n < 3)
			{
				tu.setCombatSide (UnitCombatSideID.ATTACKER);
				tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
				advancingUnits.add (tu);
			}
			
			trueMap.getUnit ().add (tu);
		}
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		final ServerCityCalculations serverCityCalc = mock (ServerCityCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnChanges (midTurnSingle);
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setOverlandMapServerUtils (overlandMapServerUtils);
		cse.setMemoryBuildingUtils (memoryBuildingUtils);
		cse.setCityCalculations (cityCalc);
		cse.setServerCityCalculations (serverCityCalc);
		cse.setServerUnitCalculations (mock (ServerUnitCalculations.class));
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, CaptureCityDecisionID.CAPTURE, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertEquals (CaptureCityDecisionID.CAPTURE, msg1.getCaptureCityDecisionID ());
	    assertEquals (goldSwiped, msg1.getGoldSwiped ().intValue ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurnMulti, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurnMulti, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the city owner was updated
		assertEquals (attackingPd.getPlayerID ().intValue (), cityData.getCityOwnerID ());
		assertEquals (2, cityData.getNumberOfRebels ());
		verify (midTurnSingle, times (1)).updatePlayerMemoryOfCity (trueTerrain, players, combatLocation, fowSettings, false);
		verify (serverCityCalc, times (1)).ensureNotTooManyOptionalFarmers (cityData);
		
		// Check the attacker's units advanced forward into the city
		verify (midTurnMulti, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
		
		// Check the attacker swiped gold from the defender
		verify (resourceValueUtils, times (1)).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, goldSwiped);
		verify (resourceValueUtils, times (1)).addToAmountStored (defendingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -goldSwiped);
		
		// Check the summoning circle was removed (but not their fortress)
		verify (midTurnSingle, times (0)).destroyBuildingOnServerAndClients (trueMap, players, fortress.getBuildingURN (), false, sd, db);
		verify (midTurnSingle, times (1)).destroyBuildingOnServerAndClients (trueMap, players, summoningCircle.getBuildingURN (), false, sd, db);
		
		// Check any old enchantments/curses cast by the old/new owner get switched off
		verify (midTurnMulti, times (1)).switchOffMaintainedSpellsInLocationOnServerAndClients (trueMap, players, combatLocation, attackingPd.getPlayerID (), db, sd);
		verify (midTurnMulti, times (1)).switchOffMaintainedSpellsInLocationOnServerAndClients (trueMap, players, combatLocation, defendingPd.getPlayerID (), db, sd);
	}
	
	/**
	 * Tests the combatEnded method when a human attacker razes a city (because it was inhabited by Klackons...)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_RazeCity () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final OverlandMapSize sys = ServerTestData.createOverlandMapSize ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (sys);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setTaxRateID ("TR01");
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		cityData.setCityPopulation (8500);
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		gc.setCityData (cityData);
		
		// Defending player has 1200 coins in total
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (defendingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (1200);
		
		// Defending player has 50000 population in total across 10 or so cities
		final OverlandMapServerUtils overlandMapServerUtils = mock (OverlandMapServerUtils.class);
		when (overlandMapServerUtils.totalPlayerPopulation (trueTerrain, defendingPd.getPlayerID (), sys, db)).thenReturn (50000);
		
		// Calc gold swiped
		final int goldSwiped = (1200 * 8500) / 50000;		// = 204
		
		// Gold from razing
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.totalCostOfBuildingsAtLocation (combatLocation, trueMap.getBuilding (), db)).thenReturn (5678);
		
		// It's a city, not a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		// Defender's summoning circle was here (but not their fortress)
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setBuildingURN (3);
		
		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setBuildingURN (4);
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)).thenReturn (summoningCircle);
		
		// How many rebels the city will have after the attacker captures it
		final CityUnrestBreakdown attackerRebels = new CityUnrestBreakdown ();
		attackerRebels.setFinalTotal (2);
		
		final CityCalculations cityCalc = mock (CityCalculations.class);
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), combatLocation, "TR01", db)).thenReturn (attackerRebels);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<MemoryUnit> advancingUnits = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			tu.setStatus (UnitStatusID.ALIVE);
			tu.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
			
			if (n < 3)
			{
				tu.setCombatSide (UnitCombatSideID.ATTACKER);
				tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
				advancingUnits.add (tu);
			}
			
			trueMap.getUnit ().add (tu);
		}
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		final ServerCityCalculations serverCityCalc = mock (ServerCityCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnChanges (midTurnSingle);
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setOverlandMapServerUtils (overlandMapServerUtils);
		cse.setMemoryBuildingUtils (memoryBuildingUtils);
		cse.setCityCalculations (cityCalc);
		cse.setServerCityCalculations (serverCityCalc);
		cse.setCityServerUtils (cityServerUtils);
		cse.setServerUnitCalculations (mock (ServerUnitCalculations.class));
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, CaptureCityDecisionID.RAZE, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertEquals (CaptureCityDecisionID.RAZE, msg1.getCaptureCityDecisionID ());
	    assertEquals (goldSwiped, msg1.getGoldSwiped ().intValue ());
	    assertEquals (567, msg1.getGoldFromRazing ().intValue ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurnMulti, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurnMulti, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the city was trashed
		assertNull (gc.getCityData ());
		verify (midTurnSingle, times (1)).updatePlayerMemoryOfCity (trueTerrain, players, combatLocation, fowSettings, false);
		
		// Check the attacker's units advanced forward into where the city used to be
		verify (midTurnMulti, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
		
		// Check the attacker swiped gold from the defender
		verify (resourceValueUtils, times (1)).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, goldSwiped + 567);
		verify (resourceValueUtils, times (1)).addToAmountStored (defendingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -goldSwiped);
		
		// Check all buildings were destroyed
		verify (midTurnMulti, times (1)).destroyAllBuildingsInLocationOnServerAndClients (trueMap, players, combatLocation, sd, db);
		
		// Check all enchantments/curses were switched off
		verify (midTurnMulti, times (1)).switchOffMaintainedSpellsInLocationOnServerAndClients (trueMap, players, combatLocation, 0, db, sd);
	}

	/**
	 * Tests the combatEnded method in a simultaneous turns game ending a regular combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_Simultaneous () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.SIMULTANEOUS);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomTransientPlayerPrivateKnowledge attackingTrans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, attackingTrans);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		
		// It isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<MemoryUnit> advancingUnits = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			tu.setStatus (UnitStatusID.ALIVE);
			tu.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
			
			if (n < 3)
			{
				tu.setCombatSide (UnitCombatSideID.ATTACKER);
				tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
				advancingUnits.add (tu);
			}
			
			trueMap.getUnit ().add (tu);
		}
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		final PlayerMessageProcessing playerMessageProcessing = mock (PlayerMessageProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setPlayerMessageProcessing (playerMessageProcessing);
		cse.setServerUnitCalculations (mock (ServerUnitCalculations.class));
		
		// Regular combat, so only the attacker has a pending movement
		final PendingMovement attackerPendingMovement = new PendingMovement ();
		gc.setCombatAttackerPendingMovement (attackerPendingMovement);
		
		final PendingMovement attackerOtherPendingMovement = new PendingMovement ();
		attackingTrans.getPendingMovement ().add (attackerOtherPendingMovement);
		attackingTrans.getPendingMovement ().add (attackerPendingMovement);
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (1, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the attacker's units advanced forward
		verify (midTurn, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
		
		// Check pending movement was removed
		assertEquals (1, attackingTrans.getPendingMovement ().size ());
		assertSame (attackerOtherPendingMovement, attackingTrans.getPendingMovement ().get (0));
		assertNull (gc.getCombatAttackerPendingMovement ());
	}

	/**
	 * Tests the combatEnded method in a simultaneous turns game ending a border conflict
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_BorderConflcit () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.SIMULTANEOUS);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);

		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);

		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<MemoryUnit> advancingUnits = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			tu.setStatus (UnitStatusID.ALIVE);
			tu.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
			
			if (n < 3)
			{
				tu.setCombatSide (UnitCombatSideID.ATTACKER);
				tu.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
				advancingUnits.add (tu);
			}
			
			trueMap.getUnit ().add (tu);
		}
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurn = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		final PlayerMessageProcessing playerMessageProcessing = mock (PlayerMessageProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setPlayerMessageProcessing (playerMessageProcessing);
		cse.setServerUnitCalculations (mock (ServerUnitCalculations.class));
		
		// Border conflict, so we have two pending movements
		final PendingMovement attackerPendingMovement = new PendingMovement ();
		gc.setCombatAttackerPendingMovement (attackerPendingMovement);

		final PendingMovement defenderPendingMovement = new PendingMovement ();
		gc.setCombatDefenderPendingMovement (defenderPendingMovement);
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (1, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the attacker's units do not advanced forward
		verify (midTurn, times (0)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
		
		// Check pending movement was removed
		assertNull (gc.getCombatAttackerPendingMovement ());
		assertNull (gc.getCombatDefenderPendingMovement ());
	}
}