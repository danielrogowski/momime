package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.MomSpellCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.MemoryMaintainedSpellUtils;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.PlayerPickUtils;
import momime.common.messages.ResourceValueUtils;
import momime.common.messages.SpellUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.servertoclient.v0_9_4.OverlandCastQueuedMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomResourceValue;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.messages.v0_9_4.UnitAddBumpTypeID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.DummyServerToClientConnection;
import momime.server.IMomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.IMomServerResourceCalculations;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Spell;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.utils.UnitServerUtils;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the SpellProcessing class
 */
public final class TestSpellProcessing
{
	/**
	 * Tests the castOverlandNow spell casting a spell that we haven't researched yet
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testCastOverlandNow_Unavailable () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);
		players.add (player3);

		// Crusade
		final Spell spell = db.findSpell ("SP158", "testCastOverlandNow_Unavailable");
		
		// Isn't researched yet
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP158");
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (new SpellUtils ());

		// Run test
		proc.castOverlandNow (null, player3, spell, players, db, sd);
	}

	/**
	 * Tests the castOverlandNow spell casting an overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_OverlandEnchantment () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		players.add (player1);

		// AI player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		players.add (player2);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		players.add (player3);

		// Crusade
		final Spell spell = db.findSpell ("SP158", "testCastOverlandNow_OverlandEnchantment");
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP158");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (new SpellUtils ());
		proc.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtils ());

		// Run test
		proc.castOverlandNow (gsk, player3, spell, players, db, sd);
		
		// Mocked method handles adding the spell to the true map, player's memories and sending the network msgs, so don't need to worry about any of that
		verify (midTurn, times (1)).addMaintainedSpellOnServerAndClients (gsk, pd3.getPlayerID (), "SP158", null, null, false, null, null, players, null, null, null, db, sd);
		
		// CAE should get added also
		verify (midTurn, times (1)).addCombatAreaEffectOnServerAndClients (gsk, "CSE158", pd3.getPlayerID (), null, players, db, sd);
		
		// Human players should both have NTMs about it
		assertEquals (1, trans1.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.OVERLAND_ENCHANTMENT, trans1.getNewTurnMessage ().get (0).getMsgType ());
		assertEquals ("SP158", trans1.getNewTurnMessage ().get (0).getSpellID ());
		assertEquals (pd3.getPlayerID (), trans1.getNewTurnMessage ().get (0).getOtherPlayerID ());

		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.OVERLAND_ENCHANTMENT, trans3.getNewTurnMessage ().get (0).getMsgType ());
		assertEquals ("SP158", trans3.getNewTurnMessage ().get (0).getSpellID ());
		assertEquals (pd3.getPlayerID (), trans3.getNewTurnMessage ().get (0).getOtherPlayerID ());
	}

	/**
	 * Tests the castOverlandNow spell casting an overland enchantment that we already have
	 * This isn't an error - just nothing happens
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_OverlandEnchantment_AlreadyExists () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Maintained spell list
		final MemoryMaintainedSpell existingSpell = new MemoryMaintainedSpell ();
		existingSpell.setCastingPlayerID (7);		// =player3
		existingSpell.setSpellID ("SP158");
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getMaintainedSpell ().add (existingSpell);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		players.add (player1);

		// AI player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		players.add (player2);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		players.add (player3);

		// Crusade
		final Spell spell = db.findSpell ("SP158", "testCastOverlandNow_OverlandEnchantment");
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP158");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (new SpellUtils ());
		proc.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtils ());

		// Run test
		proc.castOverlandNow (gsk, player3, spell, players, db, sd);
		
		// So this shouldn't happen
		verify (midTurn, times (0)).addMaintainedSpellOnServerAndClients (gsk, pd3.getPlayerID (), "SP158", null, null, false, null, null, players, null, null, null, db, sd);
		
		// CAE shouldn't be added either
		verify (midTurn, times (0)).addCombatAreaEffectOnServerAndClients (gsk, "CSE158", pd3.getPlayerID (), null, players, db, sd);
		
		// Human players won't get any NTMs about it
		assertEquals (0, trans1.getNewTurnMessage ().size ());
		assertEquals (0, trans3.getNewTurnMessage ().size ());
	}

	/**
	 * Tests the castOverlandNow spell casting a creature summoning spell overland
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_Summon_Creature () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Set location of summoning circle
		final OverlandMapCoordinatesEx summoningCircleLocation = new OverlandMapCoordinatesEx ();
		summoningCircleLocation.setX (15);
		summoningCircleLocation.setY (25);
		summoningCircleLocation.setPlane (0);
		
		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setCityLocation (summoningCircleLocation);
		summoningCircle.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (7);
		cityData.setCityPopulation (1);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST);
		
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		trueTerrain.getPlane ().get (0).getRow ().get (25).getCell ().get (15).setCityData (cityData);
		trueTerrain.getPlane ().get (0).getRow ().get (25).getCell ().get (15).setTerrainData (terrainData);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		trueMap.getBuilding ().add (summoningCircle);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);

		// Hell hounds
		final Spell spell = db.findSpell ("SP084", "testCastOverlandNow_Summon_Creature");
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP084");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		
		final UnitServerUtils unitServerUtils = new UnitServerUtils ();
		unitServerUtils.setUnitUtils (new UnitUtils ());
		unitServerUtils.setServerUnitCalculations (new MomServerUnitCalculations ());
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (new SpellUtils ());
		proc.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		proc.setUnitServerUtils (unitServerUtils);

		// Run test
		proc.castOverlandNow (gsk, player3, spell, players, db, sd);
		
		// Prove that unit got added
		verify (midTurn, times (1)).addUnitOnServerAndClients (gsk, "UN156", summoningCircleLocation, summoningCircleLocation, null, player3,
			UnitStatusID.ALIVE, players, sd, db);
		
		// Casting player gets the "You have summoned Hell Hounds!" new turn message popup
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.SUMMONED_UNIT, trans3.getNewTurnMessage ().get (0).getMsgType ());
		assertEquals ("SP084", trans3.getNewTurnMessage ().get (0).getSpellID ());
		assertEquals ("UN156", trans3.getNewTurnMessage ().get (0).getBuildingOrUnitID ());
		assertEquals (summoningCircleLocation, trans3.getNewTurnMessage ().get (0).getLocation ());
		assertEquals (UnitAddBumpTypeID.CITY, trans3.getNewTurnMessage ().get (0).getUnitAddBumpType ());
	}

	/**
	 * Tests the castOverlandNow spell casting a hero summoning spell overland
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_Summon_Hero () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Set location of summoning circle
		final OverlandMapCoordinatesEx summoningCircleLocation = new OverlandMapCoordinatesEx ();
		summoningCircleLocation.setX (15);
		summoningCircleLocation.setY (25);
		summoningCircleLocation.setPlane (0);
		
		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setCityLocation (summoningCircleLocation);
		summoningCircle.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (7);
		cityData.setCityPopulation (1);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST);
		
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		trueTerrain.getPlane ().get (0).getRow ().get (25).getCell ().get (15).setCityData (cityData);
		trueTerrain.getPlane ().get (0).getRow ().get (25).getCell ().get (15).setTerrainData (terrainData);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		trueMap.getBuilding ().add (summoningCircle);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);

		// Summon hero - note this can be any of UN001-UN025
		// but it will only summon heroes it can find in the true map's unit list as NOT_GENERATED or GENERATED
		final Spell spell = db.findSpell ("SP208", "testCastOverlandNow_Summon_Hero");
		
		// Create some heroes
		for (int n = 1; n <= 9; n++)
		{
			final MemoryUnit hero = new MemoryUnit ();
			hero.setOwningPlayerID (player3.getPlayerDescription ().getPlayerID ());
			hero.setUnitID ("UN00" + n);
			hero.setStatus (UnitStatusID.ALIVE);
			trueMap.getUnit ().add (hero);
		}

		for (int n = 20; n <= 29; n++)
		{
			final MemoryUnit hero = new MemoryUnit ();
			hero.setOwningPlayerID (player3.getPlayerDescription ().getPlayerID ());
			hero.setUnitID ("UN0" + n);
			hero.setStatus (UnitStatusID.DEAD);
			trueMap.getUnit ().add (hero);
		}

		final MemoryUnit hero = new MemoryUnit ();
		hero.setOwningPlayerID (player3.getPlayerDescription ().getPlayerID ());
		hero.setUnitID ("UN010");
		hero.setStatus (UnitStatusID.GENERATED);
		trueMap.getUnit ().add (hero);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP208");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);

		final UnitServerUtils unitServerUtils = new UnitServerUtils ();
		unitServerUtils.setUnitUtils (new UnitUtils ());
		unitServerUtils.setServerUnitCalculations (new MomServerUnitCalculations ());
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (new SpellUtils ());
		proc.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		proc.setUnitServerUtils (unitServerUtils);

		// Run test
		proc.castOverlandNow (gsk, player3, spell, players, db, sd);
		
		// Prove that hero got updated to alive
		verify (midTurn, times (1)).updateUnitStatusToAliveOnServerAndClients (hero, summoningCircleLocation, player3, players, gsk.getTrueMap (), sd, db);
		
		// Casting player gets the "You have summoned some hero!" new turn message popup
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.SUMMONED_UNIT, trans3.getNewTurnMessage ().get (0).getMsgType ());
		assertEquals ("SP208", trans3.getNewTurnMessage ().get (0).getSpellID ());
		assertEquals ("UN010", trans3.getNewTurnMessage ().get (0).getBuildingOrUnitID ());
		assertEquals (summoningCircleLocation, trans3.getNewTurnMessage ().get (0).getLocation ());
		assertEquals (UnitAddBumpTypeID.CITY, trans3.getNewTurnMessage ().get (0).getUnitAddBumpType ());
	}
	
	/**
	 * Tests the castOverlandNow spell casting a summoning spell overland, except that we're banished and have no summoning circle
	 * This isn't an error - just nothing happens
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_Summoning_NoCircle () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Set location of summoning circle
		final OverlandMapCoordinatesEx summoningCircleLocation = new OverlandMapCoordinatesEx ();
		summoningCircleLocation.setX (15);
		summoningCircleLocation.setY (25);
		summoningCircleLocation.setPlane (0);
		
		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setCityLocation (summoningCircleLocation);
		summoningCircle.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (6);		// Its somebody else's summoning circle
		cityData.setCityPopulation (1);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST);
		
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		trueTerrain.getPlane ().get (0).getRow ().get (25).getCell ().get (15).setCityData (cityData);
		trueTerrain.getPlane ().get (0).getRow ().get (25).getCell ().get (15).setTerrainData (terrainData);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		trueMap.getBuilding ().add (summoningCircle);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);

		// Hell hounds
		final Spell spell = db.findSpell ("SP084", "testCastOverlandNow_Summoning_NoCircle");
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP084");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (new SpellUtils ());
		proc.setMemoryBuildingUtils (new MemoryBuildingUtils ());

		// Run test
		proc.castOverlandNow (gsk, player3, spell, players, db, sd);
		
		// Prove that unit wasn't added
		verify (midTurn, times (0)).addUnitOnServerAndClients (gsk, "UN156", summoningCircleLocation, summoningCircleLocation, null, player3,
			UnitStatusID.ALIVE, players, sd, db);
		
		// Casting player doesn't gets the "You have summoned Hell Hounds!" new turn message popup
		assertEquals (0, trans3.getNewTurnMessage ().size ());
	}

	/**
	 * Tests the castOverlandNow spell casting a unit enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_UnitEnchantment () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);

		// Holy weapon
		final Spell spell = db.findSpell ("SP124", "testCastOverlandNow_Summon_UnitEnchantment");
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP124");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (new SpellUtils ());

		// Run test
		proc.castOverlandNow (gsk, player3, spell, players, db, sd);
		
		// Spell gets added server side, but with no target
		assertEquals (1, trueMap.getMaintainedSpell ().size ());
		assertEquals (player3.getPlayerDescription ().getPlayerID ().intValue (), trueMap.getMaintainedSpell ().get (0).getCastingPlayerID ());
		assertEquals ("SP124", trueMap.getMaintainedSpell ().get (0).getSpellID ());
		assertNull (trueMap.getMaintainedSpell ().get (0).getUnitURN ());
		assertNull (trueMap.getMaintainedSpell ().get (0).getUnitSkillID ());
		assertNull (trueMap.getMaintainedSpell ().get (0).getCityLocation ());
		
		// Client gets told to choose a target
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.TARGET_SPELL, trans3.getNewTurnMessage ().get (0).getMsgType ());
		assertEquals ("SP124", trans3.getNewTurnMessage ().get (0).getSpellID ());
	}
	
	/**
	 * Tests trying to cast a spell that we don't have researched
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_Unavailable () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		final IMomSessionVariables mom = mock (IMomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		
		// Isn't researched yet
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP123");
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final SpellProcessing proc = new SpellProcessing ();
		proc.setSpellUtils (new SpellUtils ());

		// Run test
		proc.requestCastSpell (player3, "SP123", null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("You don't have that spell researched and/or available so can't cast it.", msg.getText ());
	}

	/**
	 * Tests trying to cast an overland-only spell in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_Overland_InCombat () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		final IMomSessionVariables mom = mock (IMomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		
		// Endurance is overland-only
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP123");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final SpellProcessing proc = new SpellProcessing ();
		proc.setSpellUtils (new SpellUtils ());

		// Run test
		proc.requestCastSpell (player3, "SP123", new OverlandMapCoordinatesEx (), null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("That spell cannot be cast in combat.", msg.getText ());
	}

	/**
	 * Tests trying to cast an overland unit enchantment and give it a target immediately
	 * (This is supposed to happen via sending back a TARGET_SPELL NewTurnMessage, and then client
	 * provides the target in a separate message)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_Overland_WithUnitTarget () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		final IMomSessionVariables mom = mock (IMomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		
		// Endurance
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP123");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// Set up test object
		final SpellProcessing proc = new SpellProcessing ();
		proc.setSpellUtils (new SpellUtils ());

		// Run test
		proc.requestCastSpell (player3, "SP123", null, null, 1, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("Cannot specify a unit target when casting an overland spell.", msg.getText ());
	}

	/**
	 * Tests trying to cast a spell that we have enough skill + mana to cast instantly
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_Instant () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		
		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub3 = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, pub3, priv3, null, trans3);
		players.add (player3);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		
		// Endurance
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP123");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// It costs 30 to cast
		trans3.setOverlandCastingSkillRemainingThisTurn (50);
		final MomResourceValue mana = new MomResourceValue ();
		mana.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		mana.setAmountStored (45);
		priv3.getResourceValue ().add (mana);
		
		// Set up test object
		final IMomSessionVariables mom = mock (IMomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);

		final IPlayerMessageProcessing msgProc = mock (IPlayerMessageProcessing.class);
		final IMomServerResourceCalculations calc = mock (IMomServerResourceCalculations.class);
		
		final SpellUtils spellUtils = new SpellUtils ();
		spellUtils.setPlayerPickUtils (new PlayerPickUtils ());
		
		final MomSpellCalculations spellCalculations = new MomSpellCalculations ();
		spellUtils.setSpellCalculations (spellCalculations);
		spellCalculations.setSpellUtils (spellUtils);
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setPlayerMessageProcessing (msgProc);
		proc.setServerResourceCalculations (calc);
		proc.setSpellUtils (spellUtils);
		proc.setResourceValueUtils (new ResourceValueUtils ());

		// Run test
		proc.requestCastSpell (player3, "SP123", null, null, null, mom);
		
		// Spell gets added server side, but with no target
		assertEquals (1, trueMap.getMaintainedSpell ().size ());
		assertEquals (player3.getPlayerDescription ().getPlayerID ().intValue (), trueMap.getMaintainedSpell ().get (0).getCastingPlayerID ());
		assertEquals ("SP123", trueMap.getMaintainedSpell ().get (0).getSpellID ());
		assertNull (trueMap.getMaintainedSpell ().get (0).getUnitURN ());
		assertNull (trueMap.getMaintainedSpell ().get (0).getUnitSkillID ());
		assertNull (trueMap.getMaintainedSpell ().get (0).getCityLocation ());
		
		// Client gets told to choose a target
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.TARGET_SPELL, trans3.getNewTurnMessage ().get (0).getMsgType ());
		assertEquals ("SP123", trans3.getNewTurnMessage ().get (0).getSpellID ());
		
		// Verify target NTM gets sent
		verify (msgProc).sendNewTurnMessages (null, players, null);
		
		// Should be charged relevant amount of mana
		assertEquals (20, trans3.getOverlandCastingSkillRemainingThisTurn ());
		assertEquals (15, mana.getAmountStored ());
		
		// Recalc GPV to take into account higher spell maintaince (maybe - it doesn't know it isn't a maintained spell)
		verify (calc).recalculateGlobalProductionValues (pd3.getPlayerID (), false, mom);
		
		// Nothing gets queued
		assertEquals (0, priv3.getQueuedSpellID ().size ());
	}

	/**
	 * Tests trying to cast a spell that gets only partially cast and put in casting queue
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_Queued () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		
		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub3 = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, pub3, priv3, null, trans3);
		players.add (player3);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player3.setConnection (msgs3);
		
		// Endurance
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setSpellID ("SP123");
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		priv3.getSpellResearchStatus ().add (researchStatus);
		
		// It costs 30 to cast
		trans3.setOverlandCastingSkillRemainingThisTurn (20);
		final MomResourceValue mana = new MomResourceValue ();
		mana.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		mana.setAmountStored (25);
		priv3.getResourceValue ().add (mana);
		
		// Set up test object
		final IMomSessionVariables mom = mock (IMomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);

		final IPlayerMessageProcessing msgProc = mock (IPlayerMessageProcessing.class);
		final IMomServerResourceCalculations calc = mock (IMomServerResourceCalculations.class);
		
		final SpellUtils spellUtils = new SpellUtils ();
		spellUtils.setPlayerPickUtils (new PlayerPickUtils ());
		
		final MomSpellCalculations spellCalculations = new MomSpellCalculations ();
		spellUtils.setSpellCalculations (spellCalculations);
		spellCalculations.setSpellUtils (spellUtils);
		
		final SpellProcessing proc = new SpellProcessing ();
		proc.setPlayerMessageProcessing (msgProc);
		proc.setServerResourceCalculations (calc);
		proc.setSpellUtils (spellUtils);
		proc.setResourceValueUtils (new ResourceValueUtils ());

		// Run test
		proc.requestCastSpell (player3, "SP123", null, null, null, mom);
		
		// No spell gets added server side
		assertEquals (0, trueMap.getMaintainedSpell ().size ());

		// Queued on server side
		assertEquals (1, priv3.getQueuedSpellID ().size ());
		assertEquals ("SP123", priv3.getQueuedSpellID ().get (0));
		
		// Queued on client side
		assertEquals (1, msgs3.getMessages ().size ());
		final OverlandCastQueuedMessage msg = (OverlandCastQueuedMessage) msgs3.getMessages ().get (0);
		assertEquals ("SP123", msg.getSpellID ());
		
		assertEquals (0, trans3.getNewTurnMessage ().size ());
		
		// Nothing gets deducted until end of the turn
		assertEquals (20, trans3.getOverlandCastingSkillRemainingThisTurn ());
		assertEquals (25, mana.getAmountStored ());
	}
}
