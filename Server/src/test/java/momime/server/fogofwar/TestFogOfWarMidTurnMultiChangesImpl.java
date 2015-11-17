package momime.server.fogofwar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.OverlandMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.servertoclient.MoveUnitStackOverlandMessage;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.database.MapFeatureMagicRealmSvr;
import momime.server.database.MapFeatureSvr;
import momime.server.database.PlaneSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.messages.v0_9_7.MomGeneralServerKnowledge;

/**
 * Tests the FogOfWarMidTurnMultiChangesImpl class
 */
public final class TestFogOfWarMidTurnMultiChangesImpl
{
	/**
	 * Tests the switchOffMaintainedSpellsCastInCombatLocation_OnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffMaintainedSpellsCastInCombatLocation_OnServerAndClients () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// 3 spells, one combat spell in the right location, one combat spell in the wrong location, one overland spell in the right location		
		final MemoryMaintainedSpell spellOne = new MemoryMaintainedSpell ();
		spellOne.setSpellURN (1);
		spellOne.setCastInCombat (true);
		spellOne.setCityLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryMaintainedSpell spellTwo = new MemoryMaintainedSpell ();
		spellTwo.setSpellURN (2);
		spellTwo.setCastInCombat (false);
		spellTwo.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final MemoryMaintainedSpell spellThree = new MemoryMaintainedSpell ();
		spellThree.setSpellURN (3);
		spellThree.setCastInCombat (true);
		spellThree.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getMaintainedSpell ().add (spellOne);
		trueMap.getMaintainedSpell ().add (spellTwo);
		trueMap.getMaintainedSpell ().add (spellThree);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Set up object to test
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarMidTurnChanges (single);
		
		// Run test
		multi.switchOffMaintainedSpellsCastInCombatLocation_OnServerAndClients (trueMap, players, new MapCoordinates3DEx (20, 10, 1), db, sd);
		
		// Check results
		verify (single, times (0)).switchOffMaintainedSpellOnServerAndClients (trueMap, 1, players, db, sd);
		verify (single, times (0)).switchOffMaintainedSpellOnServerAndClients (trueMap, 2, players, db, sd);
		verify (single, times (1)).switchOffMaintainedSpellOnServerAndClients (trueMap, 3, players, db, sd);
	}
	
	/**
	 * Tests the switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// 4 spells, one combat spell cast on a unit in the right location, one combat spell cast on a unit in the wrong location, one overland spell cast on a unit in the right location,
		// and one combat spell in the right location but not cast on a unit 
		final MemoryMaintainedSpell spellOne = new MemoryMaintainedSpell ();
		spellOne.setSpellURN (1);
		spellOne.setCastInCombat (true);
		spellOne.setUnitURN (101);
		
		final MemoryUnit unitOne = new MemoryUnit ();
		unitOne.setUnitURN (101);
		unitOne.setCombatLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryMaintainedSpell spellTwo = new MemoryMaintainedSpell ();
		spellTwo.setSpellURN (2);
		spellTwo.setCastInCombat (false);
		spellTwo.setUnitURN (102);

		final MemoryUnit unitTwo = new MemoryUnit ();
		unitTwo.setUnitURN (102);
		unitTwo.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final MemoryMaintainedSpell spellThree = new MemoryMaintainedSpell ();
		spellThree.setSpellURN (3);
		spellThree.setCastInCombat (true);
		spellThree.setUnitURN (103);

		final MemoryUnit unitThree = new MemoryUnit ();
		unitThree.setUnitURN (103);
		unitThree.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final MemoryMaintainedSpell spellFour = new MemoryMaintainedSpell ();
		spellFour.setSpellURN (4);
		spellFour.setCastInCombat (true);
		spellFour.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getMaintainedSpell ().add (spellOne);
		trueMap.getMaintainedSpell ().add (spellTwo);
		trueMap.getMaintainedSpell ().add (spellThree);
		trueMap.getMaintainedSpell ().add (spellFour);
		
		// Units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (unitOne.getUnitURN (), trueMap.getUnit (), "switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients")).thenReturn (unitOne);
		when (unitUtils.findUnitURN (unitTwo.getUnitURN (), trueMap.getUnit (), "switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients")).thenReturn (unitTwo);
		when (unitUtils.findUnitURN (unitThree.getUnitURN (), trueMap.getUnit (), "switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients")).thenReturn (unitThree);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Set up object to test
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarMidTurnChanges (single);
		multi.setUnitUtils (unitUtils);
		
		// Run test
		multi.switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (trueMap, players, new MapCoordinates3DEx (20, 10, 1), db, sd);
		
		// Check results
		verify (single, times (0)).switchOffMaintainedSpellOnServerAndClients (trueMap, 1, players, db, sd);
		verify (single, times (0)).switchOffMaintainedSpellOnServerAndClients (trueMap, 2, players, db, sd);
		verify (single, times (1)).switchOffMaintainedSpellOnServerAndClients (trueMap, 3, players, db, sd);
		verify (single, times (0)).switchOffMaintainedSpellOnServerAndClients (trueMap, 4, players, db, sd);
	}
	
	/**
	 * Tests the switchOffMaintainedSpellsInLocationOnServerAndClients method, switching off spells for one player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffMaintainedSpellsInLocationOnServerAndClients_OnePlayer () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// 3 spells, one in the right location for the right player, one in the right location for the wrong player, one in the wrong location for the right player
		final MemoryMaintainedSpell spellOne = new MemoryMaintainedSpell ();
		spellOne.setSpellURN (1);
		spellOne.setCastingPlayerID (4);
		spellOne.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final MemoryMaintainedSpell spellTwo = new MemoryMaintainedSpell ();
		spellTwo.setSpellURN (2);
		spellTwo.setCastingPlayerID (3);
		spellTwo.setCityLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryMaintainedSpell spellThree = new MemoryMaintainedSpell ();
		spellThree.setSpellURN (3);
		spellThree.setCastingPlayerID (3);
		spellThree.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getMaintainedSpell ().add (spellOne);
		trueMap.getMaintainedSpell ().add (spellTwo);
		trueMap.getMaintainedSpell ().add (spellThree);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Set up object to test
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarMidTurnChanges (single);
		
		// Run test
		multi.switchOffMaintainedSpellsInLocationOnServerAndClients (trueMap, players, new MapCoordinates3DEx (20, 10, 1), 3, db, sd);

		// Check results
		verify (single, times (0)).switchOffMaintainedSpellOnServerAndClients (trueMap, 1, players, db, sd);
		verify (single, times (0)).switchOffMaintainedSpellOnServerAndClients (trueMap, 2, players, db, sd);
		verify (single, times (1)).switchOffMaintainedSpellOnServerAndClients (trueMap, 3, players, db, sd);
	}
	
	/**
	 * Tests the switchOffMaintainedSpellsInLocationOnServerAndClients method, switching off spells for all players
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffMaintainedSpellsInLocationOnServerAndClients_AllPlayers () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// 3 spells, one in the right location for the right player, one in the right location for the wrong player, one in the wrong location for the right player
		final MemoryMaintainedSpell spellOne = new MemoryMaintainedSpell ();
		spellOne.setSpellURN (1);
		spellOne.setCastingPlayerID (4);
		spellOne.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final MemoryMaintainedSpell spellTwo = new MemoryMaintainedSpell ();
		spellTwo.setSpellURN (2);
		spellTwo.setCastingPlayerID (3);
		spellTwo.setCityLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryMaintainedSpell spellThree = new MemoryMaintainedSpell ();
		spellThree.setSpellURN (3);
		spellThree.setCastingPlayerID (3);
		spellThree.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getMaintainedSpell ().add (spellOne);
		trueMap.getMaintainedSpell ().add (spellTwo);
		trueMap.getMaintainedSpell ().add (spellThree);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Set up object to test
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarMidTurnChanges (single);
		
		// Run test
		multi.switchOffMaintainedSpellsInLocationOnServerAndClients (trueMap, players, new MapCoordinates3DEx (20, 10, 1), 0, db, sd);

		// Check results
		verify (single, times (1)).switchOffMaintainedSpellOnServerAndClients (trueMap, 1, players, db, sd);		// <--
		verify (single, times (0)).switchOffMaintainedSpellOnServerAndClients (trueMap, 2, players, db, sd);
		verify (single, times (1)).switchOffMaintainedSpellOnServerAndClients (trueMap, 3, players, db, sd);
	}
	
	/**
	 * Tests the destroyAllBuildingsInLocationOnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDestroyAllBuildingsInLocationOnServerAndClients () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// 2 buildings, one in the right location, one in the wrong location
		final MemoryBuilding buildingOne = new MemoryBuilding ();
		buildingOne.setBuildingURN (1);
		buildingOne.setCityLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryBuilding buildingTwo = new MemoryBuilding ();
		buildingTwo.setBuildingURN (2);
		buildingTwo.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getBuilding ().add (buildingOne);
		trueMap.getBuilding ().add (buildingTwo);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Set up object to test
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarMidTurnChanges (single);
		
		// Run test
		multi.destroyAllBuildingsInLocationOnServerAndClients (trueMap, players, new MapCoordinates3DEx (20, 10, 1), sd, db);
		
		// Check results
		verify (single, times (0)).destroyBuildingOnServerAndClients (trueMap, players, 1, false, sd, db);
		verify (single, times (1)).destroyBuildingOnServerAndClients (trueMap, players, 2, false, sd, db);
	}
	
	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, when there's no cities, lairs, nodes, towers invovled
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Map feature is something irrelevant, like gems
		final MapFeatureSvr mapFeature = new MapFeatureSvr ();
		when (db.findMapFeature ("MF01", "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = ServerTestData.createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID ("MF01");
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		fow1.getUnit ().add (new MemoryUnit ());
		final MapVolumeOfFogOfWarStates fowArea1 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Player who can see the start of their move, but not the end
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (5);
		pd2.setHuman (true);

		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		fow2.getUnit ().add (new MemoryUnit ());
		final MapVolumeOfFogOfWarStates fowArea2 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (fowArea2);
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);

		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea2, db)).thenReturn (true);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea2, db)).thenReturn (false);
		
		// AI player who can see the end of their move, but not the start
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-1);
		pd3.setHuman (false);

		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		fow3.getUnit ().add (new MemoryUnit ());
		final MapVolumeOfFogOfWarStates fowArea3 = new MapVolumeOfFogOfWarStates ();

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (fowArea3);
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);

		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea3, db)).thenReturn (false);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea3, db)).thenReturn (true);
		
		// Player whose seen the units at some point in the past, but can't see them now
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (6);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		fow4.getUnit ().add (new MemoryUnit ());
		final MapVolumeOfFogOfWarStates fowArea4 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWar (fowArea4);
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);

		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea4, db)).thenReturn (false);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea4, db)).thenReturn (false);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		
		// Units being moved
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
		final List<Integer> unitURNList = new ArrayList<Integer> ();

		for (int n = 1; n <= 3; n++)
		{
			unitURNList.add (n);
			
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Players 1, 2 & 4 can see the units before they move; player 3 can't see them yet but they get added partway through by a mocked call so better to add it now
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu1);

			final MemoryUnit mu2 = new MemoryUnit ();
			mu2.setUnitURN (n);
			mu2.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow2.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu2);

			final MemoryUnit mu3 = new MemoryUnit ();
			mu3.setUnitURN (n);
			mu3.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow3.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu3);
			
			final MemoryUnit mu4 = new MemoryUnit ();
			mu4.setUnitURN (n);
			mu4.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow4.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu4);
		}
		
		// Set up object to test
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarCalculations (fowCalc);
		multi.setFogOfWarProcessing (fowProc);
		multi.setFogOfWarMidTurnChanges (single);
		multi.setUnitUtils (unitUtils);

		// Run method
		multi.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, gsk, sd, db);
		
		// Check player 1
		verify (single, times (0)).freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, player1);
		verify (single, times (0)).addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, trueMap.getMaintainedSpell (), player1, db);

		for (int n = 1; n <= 3; n++)
			assertEquals (moveTo, unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// Check player 2
		verify (single, times (1)).freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, player2);
		verify (single, times (0)).addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, trueMap.getMaintainedSpell (), player2, db);

		assertEquals (1, conn2.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg2 = (MoveUnitStackOverlandMessage) conn2.getMessages ().get (0);
		assertTrue (msg2.isFreeAfterMoving ());
		assertEquals (moveFrom, msg2.getMoveFrom ());
		assertEquals (moveTo, msg2.getMoveTo ());
		assertEquals (3, msg2.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg2.getUnitURN ().get (n-1).intValue ());

		// Check player 3
		verify (single, times (0)).freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, player3);
		verify (single, times (1)).addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, trueMap.getMaintainedSpell (), player3, db);

		for (int n = 1; n <= 3; n++)
			assertEquals (moveTo, unitUtils.findUnitURN (n, fow3.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		// Check player 4
		verify (single, times (0)).freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, player4);
		verify (single, times (0)).addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, trueMap.getMaintainedSpell (), player4, db);

		for (int n = 1; n <= 3; n++)		// Player still knows about the units, but at their old location, we didn't see them move
			assertEquals (moveFrom, unitUtils.findUnitURN (n, fow4.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		assertEquals (0, conn4.getMessages ().size ());
		
		// The gems are still there
		assertEquals ("MF01", moveToCell.getMapFeatureID ());
	}

	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, moving onto an empty lair.
	 * Note from this method's point of view, its irrelevant whether the lair had monsters in it - we could be coming here from the movement
	 * routine just directly moving onto an empty lair, or could be coming here from the combat routine after successfully clearing out the lair. 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients_MoveToLair () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Map feature is one that may contain monsters
		final MapFeatureSvr mapFeature = new MapFeatureSvr ();
		mapFeature.getMapFeatureMagicRealm ().add (new MapFeatureMagicRealmSvr ());
		when (db.findMapFeature ("MF01", "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = ServerTestData.createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID ("MF01");
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells terrain1 = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		fow1.setMap (terrain1);
		
		final MapVolumeOfFogOfWarStates fowArea1 = ServerTestData.createFogOfWarArea (overlandMapSize);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		
		// Units being moved
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
		final List<Integer> unitURNList = new ArrayList<Integer> ();

		for (int n = 1; n <= 3; n++)
		{
			unitURNList.add (n);
			
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Player can see the units before they move
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu1);
		}

		// Its a lair, not a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (moveToCell)).thenReturn (false);
		
		// Set up object to test
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarCalculations (fowCalc);
		multi.setFogOfWarProcessing (fowProc);
		multi.setFogOfWarMidTurnChanges (single);
		multi.setUnitUtils (unitUtils);
		multi.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		multi.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, gsk, sd, db);
		
		// Check player 1
		verify (single, times (0)).freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, player1);
		verify (single, times (0)).addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, trueMap.getMaintainedSpell (), player1, db);

		for (int n = 1; n <= 3; n++)
			assertEquals (moveTo, unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// The lair is gone
		assertNull (moveToCell.getMapFeatureID ());
	}

	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, moving onto a Tower.
	 * Note from this method's point of view, its irrelevant whether the tower had monsters in it - we could be coming here from the movement
	 * routine just directly moving onto an empty tower, or could be coming here from the combat routine after successfully clearing out the tower. 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients_MoveToTower () throws Exception
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
		
		// Map feature is one that may contain monsters
		final MapFeatureSvr mapFeature = new MapFeatureSvr ();
		mapFeature.getMapFeatureMagicRealm ().add (new MapFeatureMagicRealmSvr ());
		when (db.findMapFeature (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY, "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = ServerTestData.createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		final OverlandMapTerrainData moveToCellOtherPlane = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (1).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCellOtherPlane);
		moveToCellOtherPlane.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells terrain1 = ServerTestData.createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		fow1.setMap (terrain1);
		
		final MapVolumeOfFogOfWarStates fowArea1 = ServerTestData.createFogOfWarArea (overlandMapSize);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		
		// Units being moved
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
		final List<Integer> unitURNList = new ArrayList<Integer> ();

		for (int n = 1; n <= 3; n++)
		{
			unitURNList.add (n);
			
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Player can see the units before they move
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu1);
		}

		// Its a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (moveToCell)).thenReturn (true);
		
		// Set up object to test
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarCalculations (fowCalc);
		multi.setFogOfWarProcessing (fowProc);
		multi.setFogOfWarMidTurnChanges (single);
		multi.setUnitUtils (unitUtils);
		multi.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		multi.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, gsk, sd, db);
		
		// Check player 1
		verify (single, times (0)).freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, player1);
		verify (single, times (0)).addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, trueMap.getMaintainedSpell (), player1, db);

		for (int n = 1; n <= 3; n++)
			assertEquals (moveTo, unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// The tower is now cleared, on both planes
		assertEquals (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY, moveToCell.getMapFeatureID ());
		assertEquals (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY, moveToCellOtherPlane.getMapFeatureID ());
	}
}