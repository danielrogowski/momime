package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_5.FogOfWarSettingData;
import momime.common.database.newgame.v0_9_5.MapSizeData;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.servertoclient.v0_9_5.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.v0_9_5.CombatEndedMessage;
import momime.common.messages.servertoclient.v0_9_5.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.v0_9_5.StartCombatMessage;
import momime.common.messages.v0_9_5.CaptureCityDecisionID;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.OverlandMapTerrainData;
import momime.common.messages.v0_9_5.TurnSystem;
import momime.common.messages.v0_9_5.UnitCombatSideID;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_5.MapFeature;
import momime.server.database.v0_9_5.Plane;
import momime.server.database.v0_9_5.TileType;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;
import momime.server.messages.v0_9_5.ServerGridCell;
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
	 * Tests the startCombat method for the normal situation of one unit stack attacking another on the open map
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_RampagingMonsters () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final TileType tt = new TileType ();
		tt.setMagicRealmID (null);		// <-- so although we're attacking monsters, they're rampaging ones on the overland map, so createDefenders = false
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
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
		
		final MomPersistentPlayerPublicKnowledge defPub = new MomPersistentPlayerPublicKnowledge ();
		defPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, defPub, defendingPriv, null, null);
		
		// Have to put *something* in here to make the two lists different, so the mocks work
		attackingPriv.getResourceValue ().add (null);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "startCombat")).thenReturn (defendingPlayer);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		for (int n = 1; n <= 2; n++)
			attackingUnitURNs.add (n);
		
		// Defender has a unit (otherwise there's no defender)
		final MemoryUnit defendingUnit = new MemoryUnit ();
		defendingUnit.setOwningPlayerID (defendingPd.getPlayerID ());
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (defendingUnit);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		final ServerGridCell tc = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		tc.setTerrainData (terrainData);
		
		// Combat map generator
		final CoordinateSystem combatMapCoordinateSystem = ServerTestData.createCombatMapCoordinateSystem ();
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
		when (mom.getCombatMapCoordinateSystem ()).thenReturn (combatMapCoordinateSystem);
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
		cse.startCombat (defendingLocation, attackingFrom, null, attackingPlayer, attackingUnitURNs, mom);
		
		// Check that a map got generated
		assertNotNull (tc.getCombatMap ());
		assertEquals (28, tc.getCombatAttackerCastingSkillRemaining ().intValue ());
		assertEquals (22, tc.getCombatDefenderCastingSkillRemaining ().intValue ());

		// Check the messages sent to the client were correct
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (StartCombatMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final StartCombatMessage msg = (StartCombatMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (defendingLocation, msg.getCombatLocation ());
		assertNull (msg.getScheduledCombatURN ());
		assertSame (tc.getCombatMap (), msg.getCombatTerrain ());
		assertFalse (msg.isCreateDefenders ());
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
	 * Tests the startCombat method when attacking an occupied lair (this is to test that the createDefenders flag gets set correctly)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_OccupiedLair () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final TileType tt = new TileType ();
		tt.setMagicRealmID ("X");
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
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
		
		final MomPersistentPlayerPublicKnowledge defPub = new MomPersistentPlayerPublicKnowledge ();
		defPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, defPub, defendingPriv, null, null);
		
		// Have to put *something* in here to make the two lists different, so the mocks work
		attackingPriv.getResourceValue ().add (null);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "startCombat")).thenReturn (defendingPlayer);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		for (int n = 1; n <= 2; n++)
			attackingUnitURNs.add (n);
		
		// Defender has a unit (otherwise there's no defender)
		final MemoryUnit defendingUnit = new MemoryUnit ();
		defendingUnit.setOwningPlayerID (defendingPd.getPlayerID ());
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (defendingUnit);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		final ServerGridCell tc = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		tc.setTerrainData (terrainData);
		
		// Combat map generator
		final CoordinateSystem combatMapCoordinateSystem = ServerTestData.createCombatMapCoordinateSystem ();
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
		when (mom.getCombatMapCoordinateSystem ()).thenReturn (combatMapCoordinateSystem);
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
		cse.startCombat (defendingLocation, attackingFrom, null, attackingPlayer, attackingUnitURNs, mom);
		
		// Check that a map got generated
		assertNotNull (tc.getCombatMap ());
		assertEquals (28, tc.getCombatAttackerCastingSkillRemaining ().intValue ());
		assertEquals (22, tc.getCombatDefenderCastingSkillRemaining ().intValue ());

		// Check the messages sent to the client were correct
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (StartCombatMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final StartCombatMessage msg = (StartCombatMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (defendingLocation, msg.getCombatLocation ());
		assertNull (msg.getScheduledCombatURN ());
		assertSame (tc.getCombatMap (), msg.getCombatTerrain ());
		assertTrue (msg.isCreateDefenders ());
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
	 * Tests the startCombat method when attacking an empty lair, so defendingPlayer = null, and combatEnded is triggered immediately
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartCombat_EmptyLair () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final TileType tt = new TileType ();
		tt.setMagicRealmID ("X");
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		final MapFeature mf = new MapFeature ();
		mf.getMapFeatureMagicRealm ().add (null);		// Doesn't matter what's here, just that its a non-empty list
		when (db.findMapFeature (eq ("MF01"), anyString ())).thenReturn (mf);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Session description
		final FogOfWarSettingData fowSettings = new FogOfWarSettingData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setNodeLairTowerKnownUnitIDs (ServerTestData.createStringsVolume (sys));
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Have to put *something* in here to make the two lists different, so the mocks work
		attackingPriv.getResourceValue ().add (null);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<MemoryUnit> advancingUnits = new ArrayList<MemoryUnit> ();
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		for (int n = 1; n <= 3; n++)
		{
			attackingUnitURNs.add (n);
			
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
		
		// There are no defending units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (null);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID ("MF01");
		final ServerGridCell tc = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		tc.setTerrainData (terrainData);
		
		// It's a lair, not a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		// Combat map generator
		final CoordinateSystem combatMapCoordinateSystem = ServerTestData.createCombatMapCoordinateSystem ();
		final CombatMapGenerator mapGen = mock (CombatMapGenerator.class);
		when (mapGen.generateCombatMap (combatMapCoordinateSystem, db, trueMap, defendingLocation)).thenReturn (new MapAreaOfCombatTiles ());
		
		// Casting skill of each player
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.calculateCastingSkillOfPlayer (attackingPriv.getResourceValue ())).thenReturn (28);
		
		// Session variables		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getCombatMapCoordinateSystem ()).thenReturn (combatMapCoordinateSystem);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final MomServerResourceCalculations serverResourceCalculations = mock (MomServerResourceCalculations.class);

		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setUnitUtils (unitUtils);
		cse.setCombatMapGenerator (mapGen);
		cse.setCombatProcessing (combatProcessing);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setFogOfWarMidTurnChanges (midTurn);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		
		// Run method
		cse.startCombat (defendingLocation, attackingFrom, null, attackingPlayer, attackingUnitURNs, mom);
		
		// Check that a map got generated
		assertNotNull (tc.getCombatMap ());
		assertNull (tc.getCombatAttackerCastingSkillRemaining ());		// These never get set if the combat ends before it even starts
		assertNull (tc.getCombatDefenderCastingSkillRemaining ());

		// Check the messages sent to the client were correct, NB. StartCombatMessage never gets sent if the combat ends before it starts
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (defendingLocation, msg.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg.getWinningPlayerID ());
	    assertNull (msg.getCaptureCityDecisionID ());
	    assertNull (msg.getScheduledCombatURN ());
	    assertNull (msg.getGoldSwiped ());
	    assertNull (msg.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
	    
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
		
		// All the following assertions copied from testCombatEnded_CaptureEmptyLair, to prove that combatEnded worked normally
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, defendingLocation, attackingPlayer, null, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (defendingLocation, attackingPlayer, null, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, null, trueMap, defendingLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, defendingLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// We know the lair is now empty because we captured it
		assertEquals ("", attackingPriv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (1).getRow ().get (10).getCell ().get (20));
		
		// Check the lair was removed from the map
		assertNull (terrainData.getMapFeatureID ());
		verify (midTurn, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, defendingLocation, fowSettings.getTerrainAndNodeAuras ());
		
		// Check the attacker's units advanced forward into where the lair used to be
		verify (midTurn, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
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
		
		final TileType tt = new TileType ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
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
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "startCombat-CD")).thenReturn (defendingPlayer);
		
		// Attacker has 3 units in the cell they're attacking from, but only 2 of them are attacking
		final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
		for (int n = 1; n <= 2; n++)
			attackingUnitURNs.add (n);
		
		// No defending units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (null);

		// Attacking and defending locations
		final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (21, 10, 1);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		final ServerGridCell tc = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		tc.setTerrainData (terrainData);
		
		// There's a city here
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		tc.setCityData (cityData);
		
		// Combat map generator
		final CoordinateSystem combatMapCoordinateSystem = ServerTestData.createCombatMapCoordinateSystem ();
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
		when (mom.getCombatMapCoordinateSystem ()).thenReturn (combatMapCoordinateSystem);
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
		cse.startCombat (defendingLocation, attackingFrom, null, attackingPlayer, attackingUnitURNs, mom);
		
		// Check that a map got generated
		assertNotNull (tc.getCombatMap ());
		assertNull (tc.getCombatAttackerCastingSkillRemaining ());		// These never get set if the combat ends before it even starts
		assertNull (tc.getCombatDefenderCastingSkillRemaining ());

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
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
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
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
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
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final MomServerResourceCalculations serverResourceCalculations = mock (MomServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (defendingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getScheduledCombatURN ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, attackingPlayer, defendingPlayer, db, sd);
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
	 * Tests the combatEnded method when a human attacker captures an occupied lair (so there is a defendingPlayer)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureOccupiedLair () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final TileType tt = new TileType ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		final MapFeature mf = new MapFeature ();
		mf.getMapFeatureMagicRealm ().add (null);		// Doesn't matter what's here, just that its a non-empty list
		when (db.findMapFeature (eq ("MF01"), anyString ())).thenReturn (mf);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSettingData fowSettings = new FogOfWarSettingData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setNodeLairTowerKnownUnitIDs (ServerTestData.createStringsVolume (sys));
		
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
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID ("MF01");
		
		final MemoryGridCell gc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		
		// It's a lair, not a tower
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
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final MomServerResourceCalculations serverResourceCalculations = mock (MomServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getScheduledCombatURN ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, attackingPlayer, defendingPlayer, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// We know the lair is now empty because we captured it
		assertEquals ("", attackingPriv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (1).getRow ().get (10).getCell ().get (20));
		
		// Check the lair was removed from the map
		assertNull (terrainData.getMapFeatureID ());
		verify (midTurn, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, combatLocation, fowSettings.getTerrainAndNodeAuras ());
		
		// Check the attacker's units advanced forward into where the lair used to be
		verify (midTurn, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
	}

	/**
	 * Tests the combatEnded method when a human attacker captures an occupied lair (so defendingPlayer is null)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureEmptyLair () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final TileType tt = new TileType ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		final MapFeature mf = new MapFeature ();
		mf.getMapFeatureMagicRealm ().add (null);		// Doesn't matter what's here, just that its a non-empty list
		when (db.findMapFeature (eq ("MF01"), anyString ())).thenReturn (mf);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSettingData fowSettings = new FogOfWarSettingData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setNodeLairTowerKnownUnitIDs (ServerTestData.createStringsVolume (sys));
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
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
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID ("MF01");
		
		final MemoryGridCell gc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		
		// It's a lair, not a tower
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
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final MomServerResourceCalculations serverResourceCalculations = mock (MomServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, null, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getScheduledCombatURN ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, attackingPlayer, null, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, null, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, null, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// We know the lair is now empty because we captured it
		assertEquals ("", attackingPriv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (1).getRow ().get (10).getCell ().get (20));
		
		// Check the lair was removed from the map
		assertNull (terrainData.getMapFeatureID ());
		verify (midTurn, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, combatLocation, fowSettings.getTerrainAndNodeAuras ());
		
		// Check the attacker's units advanced forward into where the lair used to be
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
		
		final TileType tt = new TileType ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		final MapFeature mf = new MapFeature ();
		mf.getMapFeatureMagicRealm ().add (null);		// Doesn't matter what's here, just that its a non-empty list
		when (db.findMapFeature (eq (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY), anyString ())).thenReturn (mf);

		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlane ()).thenReturn (planes);
		
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSettingData fowSettings = new FogOfWarSettingData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setNodeLairTowerKnownUnitIDs (ServerTestData.createStringsVolume (sys));
		
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
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		final MemoryGridCell gc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
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
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final MomServerResourceCalculations serverResourceCalculations = mock (MomServerResourceCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertNull (msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getScheduledCombatURN ());
	    assertNull (msg1.getGoldSwiped ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, attackingPlayer, defendingPlayer, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// We know the tower is now empty because we captured it
		assertEquals ("", attackingPriv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (1).getRow ().get (10).getCell ().get (20));
		
		// Check the light was turned on in the tower
		assertEquals (CommonDatabaseConstants.VALUE_FEATURE_CLEARED_TOWER_OF_WIZARDRY, terrainData.getMapFeatureID ());
		verify (midTurn, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, combatLocation, fowSettings.getTerrainAndNodeAuras ());
		
		// Check the attacker's units advanced forward, in the process jumping to plane 0
		final MapCoordinates3DEx towerOnArcanus = new MapCoordinates3DEx (20, 10, 0);
		
		verify (midTurn, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), towerOnArcanus, players, trueMap, sd, db);
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
		
		final TileType tt = new TileType ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// General server knowledge
		final MapSizeData sys = ServerTestData.createMapSizeData ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSettingData fowSettings = new FogOfWarSettingData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		sd.setMapSize (sys);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setNodeLairTowerKnownUnitIDs (ServerTestData.createStringsVolume (sys));
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
		terrainData.setTileTypeID ("TT01");
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		cityData.setCityPopulation (8500);
		
		final MemoryGridCell gc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		gc.setCityData (cityData);
		
		// Defending player has 1200 coins in total
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (defendingPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD)).thenReturn (1200);
		
		// Defending player has 50000 population in total across 10 or so cities
		final OverlandMapServerUtils overlandMapServerUtils = mock (OverlandMapServerUtils.class);
		when (overlandMapServerUtils.totalPlayerPopulation (trueTerrain, defendingPd.getPlayerID (), sys, db)).thenReturn (50000);
		
		// Calc gold swiped
		final int goldSwiped = (1200 * 8500) / 50000;		// = 204
		
		// It's a city, not a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		// Defender's summoning circle was here (but not their fortress)
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.VALUE_BUILDING_FORTRESS)).thenReturn (false);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE)).thenReturn (true);
		
		// How many rebels the city will have after the attacker captures it
		final CityUnrestBreakdown attackerRebels = new CityUnrestBreakdown ();
		attackerRebels.setFinalTotal (2);
		
		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
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
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final MomServerResourceCalculations serverResourceCalculations = mock (MomServerResourceCalculations.class);
		final MomServerCityCalculations serverCityCalc = mock (MomServerCityCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnChanges (midTurn);
		cse.setFogOfWarProcessing (fowProcessing);
		cse.setCombatProcessing (combatProcessing);
		cse.setServerResourceCalculations (serverResourceCalculations);
		cse.setMemoryGridCellUtils (memoryGridCellUtils);
		cse.setResourceValueUtils (resourceValueUtils);
		cse.setOverlandMapServerUtils (overlandMapServerUtils);
		cse.setMemoryBuildingUtils (memoryBuildingUtils);
		cse.setCityCalculations (cityCalc);
		cse.setServerCityCalculations (serverCityCalc);
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, CaptureCityDecisionID.CAPTURE, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertEquals (CaptureCityDecisionID.CAPTURE, msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getScheduledCombatURN ());
	    assertEquals (goldSwiped, msg1.getGoldSwiped ().intValue ());
	    assertNull (msg1.getGoldFromRazing ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, attackingPlayer, defendingPlayer, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the city owner was updated
		assertEquals (attackingPd.getPlayerID (), cityData.getCityOwnerID ());
		assertEquals (2, cityData.getNumberOfRebels ().intValue ());
		verify (midTurn, times (1)).updatePlayerMemoryOfCity (trueTerrain, players, combatLocation, fowSettings, false);
		verify (serverCityCalc, times (1)).ensureNotTooManyOptionalFarmers (cityData);
		
		// Check the attacker's units advanced forward into the city
		verify (midTurn, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
		
		// Check the attacker swiped gold from the defender
		verify (resourceValueUtils, times (1)).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, goldSwiped);
		verify (resourceValueUtils, times (1)).addToAmountStored (defendingPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, -goldSwiped);
		
		// Check the summoning circle was removed (but not their fortress)
		verify (midTurn, times (0)).destroyBuildingOnServerAndClients (trueMap, players, combatLocation, CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, false, sd, db);
		verify (midTurn, times (1)).destroyBuildingOnServerAndClients (trueMap, players, combatLocation, CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, false, sd, db);
		
		// Check any old enchantments/curses cast by the old/new owner get switched off
		verify (midTurn, times (1)).switchOffMaintainedSpellsInLocationOnServerAndClients (trueMap, players, combatLocation, combatLocation,
			attackingPlayer, defendingPlayer, attackingPd.getPlayerID (), db, sd);
		verify (midTurn, times (1)).switchOffMaintainedSpellsInLocationOnServerAndClients (trueMap, players, combatLocation, combatLocation,
			attackingPlayer, defendingPlayer, defendingPd.getPlayerID (), db, sd);
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
		
		final TileType tt = new TileType ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// General server knowledge
		final MapSizeData sys = ServerTestData.createMapSizeData ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session description
		final FogOfWarSettingData fowSettings = new FogOfWarSettingData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setFogOfWarSetting (fowSettings);
		sd.setMapSize (sys);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setNodeLairTowerKnownUnitIDs (ServerTestData.createStringsVolume (sys));
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
		terrainData.setTileTypeID ("TT01");
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		cityData.setCityPopulation (8500);
		
		final MemoryGridCell gc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setTerrainData (terrainData);
		gc.setCityData (cityData);
		
		// Defending player has 1200 coins in total
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (defendingPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD)).thenReturn (1200);
		
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
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.VALUE_BUILDING_FORTRESS)).thenReturn (false);
		when (memoryBuildingUtils.findBuilding (trueMap.getBuilding (), combatLocation, CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE)).thenReturn (true);
		
		// How many rebels the city will have after the attacker captures it
		final CityUnrestBreakdown attackerRebels = new CityUnrestBreakdown ();
		attackerRebels.setFinalTotal (2);
		
		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
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
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarProcessing fowProcessing = mock (FogOfWarProcessing.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		final MomServerResourceCalculations serverResourceCalculations = mock (MomServerResourceCalculations.class);
		final MomServerCityCalculations serverCityCalc = mock (MomServerCityCalculations.class);
		
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl ();
		cse.setFogOfWarMidTurnChanges (midTurn);
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
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, CaptureCityDecisionID.RAZE, mom);
		
		// Check correct messages were generated
		assertEquals (2, attackingMsgs.getMessages ().size ());
		
		assertEquals (CombatEndedMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final CombatEndedMessage msg1 = (CombatEndedMessage) attackingMsgs.getMessages ().get (0);
	    assertEquals (combatLocation, msg1.getCombatLocation ());
	    assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getWinningPlayerID ());
	    assertEquals (CaptureCityDecisionID.RAZE, msg1.getCaptureCityDecisionID ());
	    assertNull (msg1.getScheduledCombatURN ());
	    assertEquals (goldSwiped, msg1.getGoldSwiped ().intValue ());
	    assertEquals (567, msg1.getGoldFromRazing ().intValue ());
		
		assertEquals (SelectNextUnitToMoveOverlandMessage.class.getName (), attackingMsgs.getMessages ().get (1).getClass ().getName ());
		
		// Check other tidyups were done
		verify (midTurn, times (1)).switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, combatLocation, attackingPlayer, defendingPlayer, db, sd);
		verify (combatProcessing, times (1)).purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (combatProcessing, times (1)).removeUnitsFromCombat (attackingPlayer, defendingPlayer, trueMap, combatLocation, db);
		verify (midTurn, times (1)).removeCombatAreaEffectsFromLocalisedSpells (trueMap, combatLocation, players, db, sd);
		
		// Update what both players can see
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, defendingPlayer, players, false, "combatEnded-D", sd, db);
		verify (fowProcessing, times (1)).updateAndSendFogOfWar (trueMap, attackingPlayer, players, false, "combatEnded-A", sd, db);
		
		// Update both players' production
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (defendingPd.getPlayerID (), false, mom);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (attackingPd.getPlayerID (), false, mom);
		
		// Check the city was trashed
		assertNull (gc.getCityData ());
		verify (midTurn, times (1)).updatePlayerMemoryOfCity (trueTerrain, players, combatLocation, fowSettings, false);
		
		// Check the attacker's units advanced forward into where the city used to be
		verify (midTurn, times (1)).moveUnitStackOneCellOnServerAndClients (advancingUnits, attackingPlayer,
			new MapCoordinates3DEx (21, 10, 1), new MapCoordinates3DEx (20, 10, 1), players, trueMap, sd, db);
		
		// Check the attacker swiped gold from the defender
		verify (resourceValueUtils, times (1)).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, goldSwiped + 567);
		verify (resourceValueUtils, times (1)).addToAmountStored (defendingPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, -goldSwiped);
		
		// Check all buildings were destroyed
		verify (midTurn, times (1)).destroyAllBuildingsInLocationOnServerAndClients (trueMap, players, combatLocation, sd, db);
		
		// Check all enchantments/curses were switched off
		verify (midTurn, times (1)).switchOffMaintainedSpellsInLocationOnServerAndClients (trueMap, players, combatLocation, combatLocation,
			attackingPlayer, defendingPlayer, 0, db, sd);
	}
}