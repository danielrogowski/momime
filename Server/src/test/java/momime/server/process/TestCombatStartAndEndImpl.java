package momime.server.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CitySize;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.OverlandMapSize;
import momime.common.database.UnitCombatSideID;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PendingMovement;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.CombatEndedMessage;
import momime.common.messages.servertoclient.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.StartCombatMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.knowledge.CombatDetails;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.CityServerUtils;
import momime.server.utils.CombatMapServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.worldupdates.WorldUpdates;

/**
 * Tests the CombatStartAndEndImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCombatStartAndEndImpl extends ServerTestData
{
	/**
	 * Tests the startCombat method with a null list of attackers
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_NullAttackerList () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		
		// Set up object to test
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			cse.startCombat (defendingLocation, attackingFrom, null, null, null, null, mom);
		});
	}

	/**
	 * Tests the startCombat method with an empty list of attackers
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_EmptyAttackerList () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		
		// Set up object to test
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			cse.startCombat (defendingLocation, attackingFrom, new ArrayList<Integer> (), null, null, null, mom);
		});
	}

	/**
	 * Tests the startCombat method for the normal situation of one unit stack attacking another on the open map in a one-player-at-a-time game
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_OnePlayerAtATime () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
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

			if (n == 1)
			{
				final MemoryUnit attackingUnit = new MemoryUnit ();
				attackingUnit.setOwningPlayerID (attackingPd.getPlayerID ());
				when (unitUtils.findUnitURN (n, trueMap.getUnit (), "startCombat-A")).thenReturn (attackingUnit);
			}
		}
		
		// Defender has a unit (otherwise there's no defender)
		final MemoryUnit defendingUnit = new MemoryUnit ();
		defendingUnit.setOwningPlayerID (defendingPd.getPlayerID ());
		
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (defendingUnit);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		// Combat map generator
		final CombatMapSize combatMapCoordinateSystem = createCombatMapSize ();
		sd.setCombatMapSize (combatMapCoordinateSystem);
		
		final CombatMapGenerator mapGen = mock (CombatMapGenerator.class);
		when (mapGen.generateCombatMap (combatMapCoordinateSystem, db, trueMap, defendingLocation)).thenReturn (new MapAreaOfCombatTiles ());
		
		// Casting skill of each player
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		final KnownWizardDetails defendingWizard = new KnownWizardDetails ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "startCombat-A")).thenReturn (attackingWizard);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), defendingPd.getPlayerID (), "startCombat-D")).thenReturn (defendingWizard);
		
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.calculateModifiedCastingSkill (attackingPriv.getResourceValue (), attackingWizard, players,
			attackingPriv.getFogOfWarMemory (), db, false)).thenReturn (28);
		when (resourceValueUtils.calculateModifiedCastingSkill (defendingPriv.getResourceValue (), defendingWizard, players,
			defendingPriv.getFogOfWarMemory (), db, false)).thenReturn (22);
		
		// No existing combat here
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		
		// Session variables
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Set up object to test
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setUnitUtils (unitUtils);
		cse.setCombatMapGenerator (mapGen);
		cse.setCombatProcessing (combatProcessing);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setCombatMapServerUtils (combatMapServerUtils);
		
		// Run method
		cse.startCombat (defendingLocation, attackingFrom, attackingUnitURNs, null, null, null, mom);
		
		// Check that a map got generated
		assertEquals (1, combatList.size ());
		final CombatDetails combatDetails = combatList.get (0);
		
		assertNotNull (combatDetails.getCombatMap ());
		assertEquals (28, combatDetails.getAttackerCastingSkillRemaining ());
		assertEquals (22, combatDetails.getDefenderCastingSkillRemaining ());
		assertNull (combatDetails.getAttackerPendingMovement ());
		assertNull (combatDetails.getDefenderPendingMovement ());

		// Check the messages sent to the client were correct
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (StartCombatMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final StartCombatMessage msg = (StartCombatMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (defendingLocation, msg.getCombatLocation ());
		assertSame (combatDetails.getCombatMap (), msg.getCombatTerrain ());
		assertEquals (0, msg.getUnitPlacement ().size ());		// Is zero because these are added by positionCombatUnits (), which is mocked out
		
		// Check that units were added into combat on both sides
		verify (combatProcessing).positionCombatUnits (defendingLocation, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, defendingLocation,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_ROWS, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FACING,
			UnitCombatSideID.DEFENDER, null, combatDetails.getCombatMap (), mom);

		verify (combatProcessing).positionCombatUnits (defendingLocation, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, attackingFrom,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_ROWS, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, attackingUnitURNs, combatDetails.getCombatMap (), mom);
		
		// Check the combat was started
		verify (combatProcessing).progressCombat (defendingLocation, true, false, mom);
		
		verifyNoMoreInteractions	(combatProcessing);
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
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.SIMULTANEOUS);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
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
			
			if (n == 1)
			{
				final MemoryUnit attackingUnit = new MemoryUnit ();
				attackingUnit.setOwningPlayerID (attackingPd.getPlayerID ());
				when (unitUtils.findUnitURN (n, trueMap.getUnit (), "startCombat-A")).thenReturn (attackingUnit);
			}
		}
		
		// Defender likewise is counterattacking with 2 out of 3 units
		final List<Integer> defendingUnitURNs = new ArrayList<Integer> ();
		for (int n = 4; n <= 6; n++)
		{
			if (n < 6)
				defendingUnitURNs.add (n);
			
			if (n == 4)
			{
				final MemoryUnit defendingUnit = new MemoryUnit ();
				defendingUnit.setOwningPlayerID (defendingPd.getPlayerID ());
				when (unitUtils.findUnitURN (n, trueMap.getUnit (), "startCombat-D")).thenReturn (defendingUnit);
			}
		}

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		// Combat map generator
		final CombatMapSize combatMapCoordinateSystem = createCombatMapSize ();
		sd.setCombatMapSize (combatMapCoordinateSystem);
		
		final CombatMapGenerator mapGen = mock (CombatMapGenerator.class);
		when (mapGen.generateCombatMap (combatMapCoordinateSystem, db, trueMap, defendingLocation)).thenReturn (new MapAreaOfCombatTiles ());
		
		// Casting skill of each player
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		final KnownWizardDetails defendingWizard = new KnownWizardDetails ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "startCombat-A")).thenReturn (attackingWizard);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), defendingPd.getPlayerID (), "startCombat-D")).thenReturn (defendingWizard);
		
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.calculateModifiedCastingSkill (attackingPriv.getResourceValue (), attackingWizard, players,
			attackingPriv.getFogOfWarMemory (), db, false)).thenReturn (28);
		when (resourceValueUtils.calculateModifiedCastingSkill (defendingPriv.getResourceValue (), defendingWizard, players,
			defendingPriv.getFogOfWarMemory (), db, false)).thenReturn (22);
		
		// No existing combat here
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		
		// Session variables
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Set up object to test
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setUnitUtils (unitUtils);
		cse.setCombatMapGenerator (mapGen);
		cse.setCombatProcessing (combatProcessing);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setCombatMapServerUtils (combatMapServerUtils);
		
		// Run method
		final PendingMovement combatAttackerPendingMovement = new PendingMovement ();
		final PendingMovement combatDefenderPendingMovement = new PendingMovement ();
		cse.startCombat (defendingLocation, attackingFrom, attackingUnitURNs, defendingUnitURNs, combatAttackerPendingMovement, combatDefenderPendingMovement, mom);
		
		// Check that a map got generated
		assertEquals (1, combatList.size ());
		final CombatDetails combatDetails = combatList.get (0);
		
		assertNotNull (combatDetails.getCombatMap ());
		assertEquals (28, combatDetails.getAttackerCastingSkillRemaining ());
		assertEquals (22, combatDetails.getDefenderCastingSkillRemaining ());
		assertSame (combatAttackerPendingMovement, combatDetails.getAttackerPendingMovement ());
		assertSame (combatDefenderPendingMovement, combatDetails.getDefenderPendingMovement ());

		// Check the messages sent to the client were correct
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (StartCombatMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final StartCombatMessage msg = (StartCombatMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (defendingLocation, msg.getCombatLocation ());
		assertSame (combatDetails.getCombatMap (), msg.getCombatTerrain ());
		assertEquals (0, msg.getUnitPlacement ().size ());		// Is zero because these are added by positionCombatUnits (), which is mocked out
		
		// Check that units were added into combat on both sides
		verify (combatProcessing).positionCombatUnits (defendingLocation, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, defendingLocation,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_ROWS, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FACING,
			UnitCombatSideID.DEFENDER, defendingUnitURNs, combatDetails.getCombatMap (), mom);

		verify (combatProcessing).positionCombatUnits (defendingLocation, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, attackingFrom,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_ROWS, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, attackingUnitURNs, combatDetails.getCombatMap (), mom);
		
		// Check the combat was started
		verify (combatProcessing).progressCombat (defendingLocation, true, false, mom);

		verifyNoMoreInteractions	(combatProcessing);
	}
	
	/**
	 * Tests the startCombat method when attacking an empty city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_EmptyCity () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
		defendingPd.setPlayerID (-1);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		// Have to put *something* in here to make the two lists different, so the mocks work
		attackingPriv.getResourceValue ().add (null);
		
		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);
		
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

			if (n == 1)
			{
				final MemoryUnit attackingUnit = new MemoryUnit ();
				attackingUnit.setOwningPlayerID (attackingPd.getPlayerID ());
				when (unitUtils.findUnitURN (n, trueMap.getUnit (), "startCombat-A")).thenReturn (attackingUnit);
			}
		}
		
		// No defending units
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (null);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		// There's a city here
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1000);
		cityData.setCityOwnerID (defendingPd.getPlayerID ());

		final ServerGridCellEx tc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		tc.setCityData (cityData);
		
		// Combat map generator
		final CombatMapSize combatMapCoordinateSystem = createCombatMapSize ();
		sd.setCombatMapSize (combatMapCoordinateSystem);
		
		final CombatMapGenerator mapGen = mock (CombatMapGenerator.class);
		when (mapGen.generateCombatMap (combatMapCoordinateSystem, db, trueMap, defendingLocation)).thenReturn (new MapAreaOfCombatTiles ());
		
		// Casting skill of each player
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		
		// No existing combat here
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		
		// Session variables
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Set up object to test
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setUnitUtils (unitUtils);
		cse.setCombatMapGenerator (mapGen);
		cse.setCombatProcessing (combatProcessing);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setCombatMapServerUtils (combatMapServerUtils);
		
		// Run method
		cse.startCombat (defendingLocation, attackingFrom, attackingUnitURNs, null, null, null, mom);
		
		// Check that a map got generated
		assertEquals (1, combatList.size ());
		final CombatDetails combatDetails = combatList.get (0);
		
		assertNotNull (combatDetails.getCombatMap ());
		assertEquals (0, combatDetails.getAttackerCastingSkillRemaining ());
		assertEquals (0, combatDetails.getDefenderCastingSkillRemaining ());
		assertNull (combatDetails.getAttackerPendingMovement ());
		assertNull (combatDetails.getDefenderPendingMovement ());

		// Check the messages sent to the client were correct
		// Generating this message is really all that combatEnded does in this situation, see testCombatEnded_CaptureCityUndecided
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (AskForCaptureCityDecisionMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final AskForCaptureCityDecisionMessage msg = (AskForCaptureCityDecisionMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (defendingLocation, msg.getCityLocation ());
		assertEquals (defendingPd.getPlayerID ().intValue (), msg.getDefendingPlayerID ());
		
		// Check that units were added into combat on both sides
		verify (combatProcessing).positionCombatUnits (eq (defendingLocation), any (StartCombatMessage.class), eq (attackingPlayer), isNull (),
			eq (combatMapCoordinateSystem), eq (defendingLocation),
			eq (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X), eq (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y),
			eq (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_ROWS), eq (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FACING),
			eq (UnitCombatSideID.DEFENDER), isNull (), eq (combatDetails.getCombatMap ()), eq (mom));

		verify (combatProcessing).positionCombatUnits (eq (defendingLocation), any (StartCombatMessage.class), eq (attackingPlayer), isNull (),
			eq (combatMapCoordinateSystem), eq (attackingFrom),
			eq (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X), eq (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y),
			eq (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_ROWS), eq (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING),
			eq (UnitCombatSideID.ATTACKER), eq (attackingUnitURNs), eq (combatDetails.getCombatMap ()), eq (mom));
		
		// Check the combat wasn't started
		verifyNoMoreInteractions	(combatProcessing);
	}
	
	/**
	 * Tests the combatEnded method when we're captured a city, but didn't decide whether to capture or raze yet
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureCityUndecided () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.HUMAN);
		defendingPd.setPlayerID (5);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;

		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// There's a city here, owned by the defender
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1000);
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Combat details
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, null, null, 0, 0, 0, 0);
		
		// Set up object to test
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl (); 
		cse.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		cse.combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
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
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = defendingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "combatEnded (R)")).thenReturn (attackingPlayer);
		
		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);
		
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		
		// Current player whose turn it is to resume afterwards
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setCurrentPlayerID (attackingPd.getPlayerID ());

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		
		// It isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		// Combat details
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, null, null, 1, 1, 100, 100);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		
		// Run method
		cse.combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
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
		verify (midTurnMulti).switchOffSpellsCastInCombatAtLocation (combatLocation, mom);
		verify (combatProcessing).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, mom);
		verify (combatProcessing).removeUnitsFromCombat (attackingPlayer, defendingPlayer, combatLocation, mom);
		verify (midTurnMulti).removeCombatAreaEffectsFromLocalisedSpells (combatLocation, mom);
		
		// Update what both players can see
		verify (fowProcessing).updateAndSendFogOfWar (defendingPlayer, "combatEnded-D", mom);
		verify (fowProcessing).updateAndSendFogOfWar (attackingPlayer, "combatEnded-A", mom);
		
		// Update both players' production
		verify (serverResourceCalculations).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);

		verifyNoMoreInteractions	(midTurnMulti);
		verifyNoMoreInteractions	(fowProcessing);
		verifyNoMoreInteractions	(serverResourceCalculations);
	}

	/**
	 * Tests the combatEnded method when an the attacking player won, and so advances into the combat tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_AttackerWon () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "combatEnded (R)")).thenReturn (attackingPlayer);
		
		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);
		
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		
		// Current player whose turn it is to resume afterwards
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setCurrentPlayerID (attackingPd.getPlayerID ());

		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
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
		
		// Combat details
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, null, null, 1, 1, 100, 100);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		
		// Run method
		cse.combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
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
		verify (midTurnMulti).switchOffSpellsCastInCombatAtLocation (combatLocation, mom);
		verify (combatProcessing).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, mom);
		verify (combatProcessing).removeUnitsFromCombat (attackingPlayer, defendingPlayer, combatLocation, mom);
		verify (midTurnMulti).removeCombatAreaEffectsFromLocalisedSpells (combatLocation, mom);
		
		// Update what both players can see
		verify (fowProcessing).updateAndSendFogOfWar (defendingPlayer, "combatEnded-D", mom);
		verify (fowProcessing).updateAndSendFogOfWar (attackingPlayer, "combatEnded-A", mom);
		
		// Update both players' production
		verify (serverResourceCalculations).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the attacker's units advanced forward
		verify (midTurnMulti).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), mom);
		
		verifyNoMoreInteractions	(midTurnMulti);
		verifyNoMoreInteractions	(fowProcessing);
		verifyNoMoreInteractions	(serverResourceCalculations);
	}

	/**
	 * Tests the combatEnded method when a human attacker captures a tower of wizardry using units that were on Myrror
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureTowerFromMyrror () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "combatEnded (R)")).thenReturn (attackingPlayer);
		
		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);

		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		
		// Current player whose turn it is to resume afterwards
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setCurrentPlayerID (attackingPd.getPlayerID ());

		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
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
		
		// Combat details
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, null, null, 1, 1, 100, 100);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		
		// Run method
		cse.combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
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
		verify (midTurnMulti).switchOffSpellsCastInCombatAtLocation (combatLocation, mom);
		verify (combatProcessing).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, mom);
		verify (combatProcessing).removeUnitsFromCombat (attackingPlayer, defendingPlayer, combatLocation, mom);
		verify (midTurnMulti).removeCombatAreaEffectsFromLocalisedSpells (combatLocation, mom);
		
		// Update what both players can see
		verify (fowProcessing).updateAndSendFogOfWar (defendingPlayer, "combatEnded-D", mom);
		verify (fowProcessing).updateAndSendFogOfWar (attackingPlayer, "combatEnded-A", mom);
		
		// Update both players' production
		verify (serverResourceCalculations).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the attacker's units advanced forward, in the process jumping to plane 0
		verify (midTurnMulti).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 0), mom);

		verifyNoMoreInteractions	(midTurnMulti);
		verifyNoMoreInteractions	(fowProcessing);
		verifyNoMoreInteractions	(serverResourceCalculations);
	}
	
	/**
	 * Tests the combatEnded method when a human attacker captures a city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureCity () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySize citySize = new CitySize ();
		when (db.findCitySize ("CS01", "combatEnded")).thenReturn (citySize);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (sys);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setTaxRateID ("TR01");
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
		defendingPd.setPlayerID (-1);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "combatEnded (R)")).thenReturn (attackingPlayer);

		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		final KnownWizardDetails defendingWizard = new KnownWizardDetails ();
		defendingWizard.setWizardID ("WZ01");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), defendingPd.getPlayerID (), "combatEnded")).thenReturn (defendingWizard);
		
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (true);
		
		// Current player whose turn it is to resume afterwards
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setCurrentPlayerID (attackingPd.getPlayerID ());

		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		cityData.setCityPopulation (8500);
		cityData.setCitySizeID ("CS01");
		
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
		
		// How many rebels the city will have after the attacker captures it
		final CityUnrestBreakdown attackerRebels = new CityUnrestBreakdown ();
		attackerRebels.setFinalTotal (2);
		
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
		
		// It isn't their Wizard's Fortress
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (memoryBuildingUtils.	findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (cityServerUtils.countCities (trueTerrain, defendingPd.getPlayerID (), true)).thenReturn (10);
		
		// Combat details
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, null, null, 1, 1, 100, 100);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		final CityProcessing cityProcessing = mock (CityProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setOverlandMapServerUtils (overlandMapServerUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		cse.setCityProcessing (cityProcessing);
		cse.setMemoryBuildingUtils (memoryBuildingUtils);
		cse.setCityServerUtils (cityServerUtils);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setRandomUtils (mock (RandomUtils.class));
		cse.setSpellCasting (mock (SpellCasting.class));
		cse.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		
		// Run method
		cse.combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, CaptureCityDecisionID.CAPTURE, mom);
		
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
		verify (midTurnMulti).switchOffSpellsCastInCombatAtLocation (combatLocation, mom);
		verify (combatProcessing).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, mom);
		verify (combatProcessing).removeUnitsFromCombat (attackingPlayer, defendingPlayer, combatLocation, mom);
		verify (midTurnMulti).removeCombatAreaEffectsFromLocalisedSpells (combatLocation, mom);
		
		// Update what both players can see
		verify (fowProcessing).updateAndSendFogOfWar (defendingPlayer, "combatEnded-D", mom);
		verify (fowProcessing).updateAndSendFogOfWar (attackingPlayer, "combatEnded-A", mom);
		
		// Update both players' production
		verify (serverResourceCalculations).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the city owner was updated
		verify (cityProcessing).captureCity (combatLocation, attackingPlayer, defendingPlayer, mom);
		
		// Check the attacker's units advanced forward into the city
		verify (midTurnMulti).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), mom);
		
		// Check the attacker swiped gold from the defender
		verify (resourceValueUtils).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, goldSwiped);
		verify (resourceValueUtils).addToAmountStored (defendingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -goldSwiped);
		
		verifyNoMoreInteractions	(midTurnMulti);
		verifyNoMoreInteractions	(fowProcessing);
		verifyNoMoreInteractions	(serverResourceCalculations);
		verifyNoMoreInteractions	(resourceValueUtils);
		verifyNoMoreInteractions	(cityProcessing);
	}
	
	/**
	 * Tests the combatEnded method when a human attacker razes a city (because it was inhabited by Klackons...)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_RazeCity () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySize citySize = new CitySize ();
		when (db.findCitySize ("CS01", "combatEnded")).thenReturn (citySize);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (sys);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setTaxRateID ("TR01");
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
		defendingPd.setPlayerID (-1);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "combatEnded (R)")).thenReturn (attackingPlayer);
		
		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		final KnownWizardDetails defendingWizard = new KnownWizardDetails ();
		defendingWizard.setWizardID ("WZ01");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), defendingPd.getPlayerID (), "combatEnded")).thenReturn (defendingWizard);
		
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (true);
		
		// Current player whose turn it is to resume afterwards
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setCurrentPlayerID (attackingPd.getPlayerID ());

		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		cityData.setCityPopulation (8500);
		cityData.setCitySizeID ("CS01");
		
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
		
		// How many rebels the city will have after the attacker captures it
		final CityUnrestBreakdown attackerRebels = new CityUnrestBreakdown ();
		attackerRebels.setFinalTotal (2);
		
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
		
		// It isn't their Wizard's Fortress
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.	findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (null);
		when (cityServerUtils.countCities (trueTerrain, defendingPd.getPlayerID (), true)).thenReturn (10);
		
		// Combat details
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, null, null, 1, 1, 100, 100);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		final CityProcessing cityProcessing = mock (CityProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setOverlandMapServerUtils (overlandMapServerUtils);
		cse.setCityServerUtils (cityServerUtils);
		cse.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		cse.setCityProcessing (cityProcessing);
		cse.setMemoryBuildingUtils (memoryBuildingUtils);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		
		// Run method
		cse.combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, CaptureCityDecisionID.RAZE, mom);
		
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
		verify (midTurnMulti).switchOffSpellsCastInCombatAtLocation (combatLocation, mom);
		verify (combatProcessing).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, mom);
		verify (combatProcessing).removeUnitsFromCombat (attackingPlayer, defendingPlayer, combatLocation, mom);
		verify (midTurnMulti).removeCombatAreaEffectsFromLocalisedSpells (combatLocation, mom);
		
		// Update what both players can see
		verify (fowProcessing).updateAndSendFogOfWar (defendingPlayer, "combatEnded-D", mom);
		verify (fowProcessing).updateAndSendFogOfWar (attackingPlayer, "combatEnded-A", mom);
		
		// Update both players' production
		verify (serverResourceCalculations).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the city was trashed
		verify (cityProcessing).razeCity (combatLocation, mom);
		
		// Check the attacker's units advanced forward into where the city used to be
		verify (midTurnMulti).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), mom);
		
		// Check the attacker swiped gold from the defender
		verify (resourceValueUtils).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, goldSwiped + 567);
		verify (resourceValueUtils).addToAmountStored (defendingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -goldSwiped);
		
		verifyNoMoreInteractions	(midTurnMulti);
		verifyNoMoreInteractions	(fowProcessing);
		verifyNoMoreInteractions	(serverResourceCalculations);
		verifyNoMoreInteractions	(resourceValueUtils);
		verifyNoMoreInteractions	(cityProcessing);
	}

	/**
	 * Tests the combatEnded method in a simultaneous turns game ending a regular combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_Simultaneous () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.SIMULTANEOUS);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);
		
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
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
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		final SimultaneousTurnsProcessing simultaneousTurnsProcessing = mock (SimultaneousTurnsProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setSimultaneousTurnsProcessing (simultaneousTurnsProcessing);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		
		// Regular combat, so only the attacker has a pending movement
		final PendingMovement attackerPendingMovement = new PendingMovement ();
		
		final PendingMovement attackerOtherPendingMovement = new PendingMovement ();
		attackingPriv.getPendingMovement ().add (attackerOtherPendingMovement);
		attackingPriv.getPendingMovement ().add (attackerPendingMovement);
		
		// Combat details
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, attackerPendingMovement, null, 1, 1, 100, 100);
		
		// Run method
		cse.combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
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
		verify (midTurnMulti).switchOffSpellsCastInCombatAtLocation (combatLocation, mom);
		verify (combatProcessing).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, mom);
		verify (combatProcessing).removeUnitsFromCombat (attackingPlayer, defendingPlayer, combatLocation, mom);
		verify (midTurnMulti).removeCombatAreaEffectsFromLocalisedSpells (combatLocation, mom);
		
		// Update what both players can see
		verify (fowProcessing).updateAndSendFogOfWar (defendingPlayer, "combatEnded-D", mom);
		verify (fowProcessing).updateAndSendFogOfWar (attackingPlayer, "combatEnded-A", mom);
		
		// Update both players' production
		verify (serverResourceCalculations).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the attacker's units advanced forward
		verify (midTurnMulti).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), mom);
		
		// Check pending movement was removed
		assertEquals (1, attackingPriv.getPendingMovement ().size ());
		assertSame (attackerOtherPendingMovement, attackingPriv.getPendingMovement ().get (0));
		
		verifyNoMoreInteractions	(midTurnMulti);
		verifyNoMoreInteractions	(fowProcessing);
		verifyNoMoreInteractions	(serverResourceCalculations);
	}

	/**
	 * Tests the combatEnded method in a simultaneous turns game ending a border conflict
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_BorderConflcit () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.SIMULTANEOUS);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (3);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.AI);
		defendingPd.setPlayerID (-1);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		// Wizards
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "combatEnded")).thenReturn (attackingWizard);
		
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
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
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		final SimultaneousTurnsProcessing simultaneousTurnsProcessing = mock (SimultaneousTurnsProcessing.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setSimultaneousTurnsProcessing (simultaneousTurnsProcessing);
		cse.setKnownWizardUtils (knownWizardUtils);
		cse.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		
		// Border conflict, so we have two pending movements
		final PendingMovement attackerPendingMovement = new PendingMovement ();
		final PendingMovement defenderPendingMovement = new PendingMovement ();
		
		// Combat details
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, attackerPendingMovement, defenderPendingMovement, 1, 1, 100, 100);
		
		// Run method
		cse.combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
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
		verify (midTurnMulti).switchOffSpellsCastInCombatAtLocation (combatLocation, mom);
		verify (combatProcessing).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, mom);
		verify (combatProcessing).removeUnitsFromCombat (attackingPlayer, defendingPlayer, combatLocation, mom);
		verify (midTurnMulti).removeCombatAreaEffectsFromLocalisedSpells (combatLocation, mom);
		
		// Update what both players can see
		verify (fowProcessing).updateAndSendFogOfWar (defendingPlayer, "combatEnded-D", mom);
		verify (fowProcessing).updateAndSendFogOfWar (attackingPlayer, "combatEnded-A", mom);
		
		// Update both players' production
		verify (serverResourceCalculations).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the attacker's units do not advanced forward
		verifyNoMoreInteractions	(midTurnMulti);
		verifyNoMoreInteractions	(fowProcessing);
		verifyNoMoreInteractions	(serverResourceCalculations);
	}
}