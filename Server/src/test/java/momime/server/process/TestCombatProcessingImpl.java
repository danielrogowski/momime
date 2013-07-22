package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessage;
import momime.common.messages.servertoclient.v0_9_4.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitCombatSideID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CombatProcessingImpl class
 */
public final class TestCombatProcessingImpl
{
	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked another player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingOtherPlayer () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID ("WZ02");

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		defendingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection defendingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		defendingPlayer.setConnection (defendingPlayerConnection);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		assertEquals (2, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveDefendersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getData ().getUnitURN ());

		assertEquals (2, defendingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellDefenderToRemoveAttackersDeadUnit = (KillUnitMessage) defendingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellDefenderToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellDefenderToRemoveAttackersDeadUnit.getData ().getUnitURN ());
		final KillUnitMessage tellDefenderToRemoveDefendersDeadUnit = (KillUnitMessage) defendingPlayerConnection.getMessages ().get (1);
		assertEquals (KillUnitActionID.FREE, tellDefenderToRemoveDefendersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (8, tellDefenderToRemoveDefendersDeadUnit.getData ().getUnitURN ());
		
		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (3, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, defendingPriv.getFogOfWarMemory ().getUnit ());
	}

	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked monsters walking around map
	 * Behaves exactly the same way, except that we don't send messages to the computer defender
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingRampagingMonsters () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		defendingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		assertEquals (2, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveDefendersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getData ().getUnitURN ());

		// Defender is now a computer player so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (3, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, defendingPriv.getFogOfWarMemory ().getUnit ());
	}

	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked monsters in a node
	 * Now ALL the defending units get "killed off" on the client, even any left alive, since client doesn't remember monsters guarding nodes/lairs/towers outside of combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingNode () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		defendingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (3, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersAliveMonster = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveDefendersAliveMonster.getData ().getKillUnitActionID ());
		assertEquals (7, tellAttackerToRemoveDefendersAliveMonster.getData ().getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (2);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveDefendersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getData ().getUnitURN ());

		// Defender is now a computer player so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (7, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (3, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, defendingPriv.getFogOfWarMemory ().getUnit ());		// Its the monster player's own unit, so don't remove it from their server memory
		verify (unitUtils, times (1)).removeUnitURN (8, defendingPriv.getFogOfWarMemory ().getUnit ());
	}
	
	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked an empty node, so defendingPlayer is null
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingEmptyNode () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, null, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());

		// Defender doesn't even exist, so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
	}
	
	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked a location that we'd already
	 * cleared with a previous unit stack in a simultaneous turns game, so defendingPlayer is null.
	 * The only difference from the EmptyNode test is the tileTypeID. 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_WalkInWithoutAFight () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, null, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());

		// Defender doesn't even exist, so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for summoning phantom warriors into combat against another human player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_TwoHumanPlayers () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID ("WZ02");
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection defendingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		defendingPlayer.setConnection (defendingPlayerConnection);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.ATTACKER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 3; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			if (n == 2)
			{
				assertNull (playerUnit.getCombatLocation ());
				assertNull (playerUnit.getCombatPosition ());
				assertNull (playerUnit.getCombatHeading ());
				assertNull (playerUnit.getCombatSide ());
			}
			else
			{
				assertEquals (combatLocation, playerUnit.getCombatLocation ());
				assertEquals (combatPosition, playerUnit.getCombatPosition ());
				assertEquals (7, playerUnit.getCombatHeading ().intValue ());
				assertEquals (UnitCombatSideID.ATTACKER, playerUnit.getCombatSide ());
			}
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		assertEquals (1, defendingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage defendingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) defendingPlayerConnection.getMessages ().get (0);
		assertEquals (1, defendingMsg.getUnitURN ());
		assertEquals (combatLocation, defendingMsg.getCombatLocation ());
		assertEquals (combatPosition, defendingMsg.getCombatPosition ());
		assertEquals (7, defendingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, defendingMsg.getCombatSide ());
		assertEquals ("SP045", defendingMsg.getSummonedBySpellID ());

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}

	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for summoning phantom warriors into combat against rampaging monsters walking around the map
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_AgainstRampagingMonsters () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.ATTACKER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 3; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			if (n == 2)
			{
				assertNull (playerUnit.getCombatLocation ());
				assertNull (playerUnit.getCombatPosition ());
				assertNull (playerUnit.getCombatHeading ());
				assertNull (playerUnit.getCombatSide ());
			}
			else
			{
				assertEquals (combatLocation, playerUnit.getCombatLocation ());
				assertEquals (combatPosition, playerUnit.getCombatPosition ());
				assertEquals (7, playerUnit.getCombatHeading ().intValue ());
				assertEquals (UnitCombatSideID.ATTACKER, playerUnit.getCombatSide ());
			}
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection
		
		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for rampaging monsters walking around the map summoning phantom warriors against us
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_RampagingMonstersAgainstUs () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (20));
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (20));
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.DEFENDER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.DEFENDER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 3; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			if (n == 2)
			{
				assertNull (playerUnit.getCombatLocation ());
				assertNull (playerUnit.getCombatPosition ());
				assertNull (playerUnit.getCombatHeading ());
				assertNull (playerUnit.getCombatSide ());
			}
			else
			{
				assertEquals (combatLocation, playerUnit.getCombatLocation ());
				assertEquals (combatPosition, playerUnit.getCombatPosition ());
				assertEquals (7, playerUnit.getCombatHeading ().intValue ());
				assertEquals (UnitCombatSideID.DEFENDER, playerUnit.getCombatSide ());
			}
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.DEFENDER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection
		
		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}

	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for summoning phantom warriors into combat against monsters in a node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_AgainstNode () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit - outside observer can see it because was the attacker who summoned it
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.ATTACKER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 3; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			if (n == 2)
			{
				assertNull (playerUnit.getCombatLocation ());
				assertNull (playerUnit.getCombatPosition ());
				assertNull (playerUnit.getCombatHeading ());
				assertNull (playerUnit.getCombatSide ());
			}
			else
			{
				assertEquals (combatLocation, playerUnit.getCombatLocation ());
				assertEquals (combatPosition, playerUnit.getCombatPosition ());
				assertEquals (7, playerUnit.getCombatHeading ().intValue ());
				assertEquals (UnitCombatSideID.ATTACKER, playerUnit.getCombatSide ());
			}
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection
		
		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for monsters in a node summoning phantom warriors against us
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_NodeAgainstUs () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (20));
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players already know about the unit, but the outside observer can't see it because its a monster in a node
		for (int index = 0; index < 2; index ++)
		{
			final PlayerServerDetails thisPlayer = players.get (index);
			
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (20));
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.DEFENDER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.DEFENDER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 2; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertEquals (combatLocation, playerUnit.getCombatLocation ());
			assertEquals (combatPosition, playerUnit.getCombatPosition ());
			assertEquals (7, playerUnit.getCombatHeading ().intValue ());
			assertEquals (UnitCombatSideID.DEFENDER, playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.DEFENDER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection
		
		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}

	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a unit at the end of a combat against another human player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_TwoHumanPlayers () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID ("WZ02");
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection defendingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		defendingPlayer.setConnection (defendingPlayerConnection);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		// Both players and the outside observer all already know about the unit, but the outside observer doesn't know that its in combat
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square

			if (thisPlayer != otherPlayer)
			{
				playerUnit.setCombatLocation (combatLocation);
				playerUnit.setCombatHeading (7);
				playerUnit.setCombatPosition (combatPosition);
				playerUnit.setCombatSide (UnitCombatSideID.ATTACKER);
			}
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertNull (attackingMsg.getCombatLocation ());
		assertNull (attackingMsg.getCombatPosition ());
		assertNull (attackingMsg.getCombatHeading ());
		assertNull (attackingMsg.getCombatSide ());
		assertNull (attackingMsg.getSummonedBySpellID ());

		assertEquals (1, defendingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage defendingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) defendingPlayerConnection.getMessages ().get (0);
		assertEquals (1, defendingMsg.getUnitURN ());
		assertNull (defendingMsg.getCombatLocation ());
		assertNull (defendingMsg.getCombatPosition ());
		assertNull (defendingMsg.getCombatHeading ());
		assertNull (defendingMsg.getCombatSide ());
		assertNull (defendingMsg.getSummonedBySpellID ());

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a unit at the end of a combat against rampaging monsters
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_AgainstRampagingMonsters () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		// Both players and the outside observer all already know about the unit, but the outside observer doesn't know that its in combat
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square

			if (thisPlayer != otherPlayer)
			{
				playerUnit.setCombatLocation (combatLocation);
				playerUnit.setCombatHeading (7);
				playerUnit.setCombatPosition (combatPosition);
				playerUnit.setCombatSide (UnitCombatSideID.ATTACKER);
			}
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertNull (attackingMsg.getCombatLocation ());
		assertNull (attackingMsg.getCombatPosition ());
		assertNull (attackingMsg.getCombatHeading ());
		assertNull (attackingMsg.getCombatSide ());
		assertNull (attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a rampaging monsters unit at the end of combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_RampagingMonstersAgainstUs () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (20));
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		// Both players and the outside observer all already know about the unit, but the outside observer doesn't know that its in combat
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (20));

			if (thisPlayer != otherPlayer)
			{
				playerUnit.setCombatLocation (combatLocation);
				playerUnit.setCombatHeading (7);
				playerUnit.setCombatPosition (combatPosition);
				playerUnit.setCombatSide (UnitCombatSideID.DEFENDER);
			}
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertNull (attackingMsg.getCombatLocation ());
		assertNull (attackingMsg.getCombatPosition ());
		assertNull (attackingMsg.getCombatHeading ());
		assertNull (attackingMsg.getCombatSide ());
		assertNull (attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}

	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a unit at the end of a combat against a node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_AgainstNode () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		// Both players and the outside observer all already know about the unit, but the outside observer doesn't know that its in combat
		// Outside observer can see it because its the attacker's unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square

			if (thisPlayer != otherPlayer)
			{
				playerUnit.setCombatLocation (combatLocation);
				playerUnit.setCombatHeading (7);
				playerUnit.setCombatPosition (combatPosition);
				playerUnit.setCombatSide (UnitCombatSideID.ATTACKER);
			}
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertNull (attackingMsg.getCombatLocation ());
		assertNull (attackingMsg.getCombatPosition ());
		assertNull (attackingMsg.getCombatHeading ());
		assertNull (attackingMsg.getCombatSide ());
		assertNull (attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a rampaging monsters unit at the end of combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_NodeAgainstUs () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (20));
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		// By the time this method runs, all dead units, summoned units, and monsters guarding nodes/lairs/towers have already
		// been removed on the client - so the attacker now doesn't know about the units they were fighting against
		{
			final PlayerServerDetails thisPlayer = defendingPlayer;
			
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (20));

			playerUnit.setCombatLocation (combatLocation);
			playerUnit.setCombatHeading (7);
			playerUnit.setCombatPosition (combatPosition);
			playerUnit.setCombatSide (UnitCombatSideID.DEFENDER);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		{
			final PlayerServerDetails thisPlayer = defendingPlayer;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (0, attackingPlayerConnection.getMessages ().size ());

		// Defending player is now AI so has no connection

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Just to save repeating this a dozen times in the test cases
	 * @param x X coord
	 * @return Coordinates object
	 */
	private final OverlandMapCoordinatesEx createCoordinates (final int x)
	{
		final OverlandMapCoordinatesEx combatLocation = new OverlandMapCoordinatesEx ();
		combatLocation.setX (x);
		combatLocation.setY (10);
		combatLocation.setPlane (1);
		return combatLocation;
	}
}
