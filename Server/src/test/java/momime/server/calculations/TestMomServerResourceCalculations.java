package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.calculations.MomSkillCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_4.RoundingDirectionID;
import momime.common.messages.IResourceValueUtils;
import momime.common.messages.ISpellUtils;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.PlayerPickUtils;
import momime.common.messages.ResourceValueUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.servertoclient.v0_9_4.FullSpellListMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateGlobalEconomyMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateRemainingResearchCostMessage;
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
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.process.resourceconsumer.IMomResourceConsumer;
import momime.server.process.resourceconsumer.MomResourceConsumerBuilding;
import momime.server.process.resourceconsumer.MomResourceConsumerSpell;
import momime.server.process.resourceconsumer.MomResourceConsumerUnit;
import momime.server.utils.UnitServerUtils;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the MomServerResourceCalculations class
 */
public final class TestMomServerResourceCalculations
{
	/**
	 * Tests the recalculateAmountsPerTurn method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Test
	public final void testRecalculateAmountsPerTurn () throws IOException, JAXBException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);
		players.add (player);
		
		// Set up test object
		final UnitUtils unitUtils = new UnitUtils ();
		final PlayerPickUtils playerPickUtils = new PlayerPickUtils ();
		unitUtils.setPlayerPickUtils (playerPickUtils);
		
		final MomCityCalculations cityCalculations = new MomCityCalculations ();
		cityCalculations.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		cityCalculations.setPlayerPickUtils (playerPickUtils);
		
		final MomServerResourceCalculations calc = new MomServerResourceCalculations ();
		calc.setResourceValueUtils (new ResourceValueUtils ());
		calc.setUnitServerUtils (new UnitServerUtils ());
		calc.setCityCalculations (cityCalculations);
		calc.setUnitUtils (unitUtils);
		calc.setPlayerPickUtils (playerPickUtils);

		// We have some shadow demons (7 mana upkeep)
		final MemoryUnit shadowDemons = unitUtils.createMemoryUnit ("UN172", 1, null, -1, true, db);
		shadowDemons.setOwningPlayerID (2);
		trueMap.getUnit ().add (shadowDemons);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-7, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Add a crusade spell (10 mana upkeep)
		final MemoryMaintainedSpell crusade = new MemoryMaintainedSpell ();
		crusade.setSpellID ("SP158");
		crusade.setCastingPlayerID (2);
		trueMap.getMaintainedSpell ().add (crusade);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-17, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Wizard has Channeler, halfing spell maintainence (half of 17 is 8.5, proves that maintainence is rounded up)
		playerPickUtils.updatePickQuantity (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_CHANNELER, 1);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Add some warlocks, so we get some other production type IDs
		final MemoryUnit warlocks = unitUtils.createMemoryUnit ("UN065", 2, 0, 0, true, db);
		warlocks.setOwningPlayerID (2);
		trueMap.getUnit ().add (warlocks);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (3, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-5, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (-1, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());

		// Prove that any prior values in Per Turn get wiped, and any prior values in Stored get preserved
		priv.getResourceValue ().get (1).setAmountPerTurn (10);
		priv.getResourceValue ().get (1).setAmountStored (10);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (3, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-5, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (10, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (-1, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());

		// Add a city; note calculateAllCityProductions itself is complicated but has its own unit tests, so we don't need to test
		// every scenario here, just basically that production and consumption from cities gets added in
		priv.setTaxRateID ("TR04");			// 1.5 gold from each population

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityRaceID ("RC05");		// High men
		cityData.setCityPopulation (5900);
		cityData.setMinimumFarmers (1);
		cityData.setOptionalFarmers (1);
		cityData.setNumberOfRebels (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Population will eat 5 rations, but produce 2x2 = 4 rations, and generate 3 x 1.5 = 4.5 gold from taxes and 2x.5 + 1x2 production
		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (5, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-1, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (10, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (-2, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, priv.getResourceValue ().get (3).getProductionTypeID ());
		assertEquals (3, priv.getResourceValue ().get (3).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (3).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, priv.getResourceValue ().get (4).getProductionTypeID ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountStored ());

		// Add a granary, costs 1 gold but produces 2 rations and 2 food (city size)
		final OverlandMapCoordinatesEx granaryLocation = new OverlandMapCoordinatesEx ();
		granaryLocation.setX (2);
		granaryLocation.setY (2);
		granaryLocation.setPlane (0);

		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID ("BL29");
		granary.setCityLocation (granaryLocation);

		trueMap.getBuilding ().add (granary);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (5, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-2, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (10, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, priv.getResourceValue ().get (3).getProductionTypeID ());
		assertEquals (3, priv.getResourceValue ().get (3).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (3).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, priv.getResourceValue ().get (4).getProductionTypeID ());
		assertEquals (2, priv.getResourceValue ().get (4).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountStored ());

		// Add a temple, costs 2 gold but produces 2 magic power
		final OverlandMapCoordinatesEx templeLocation = new OverlandMapCoordinatesEx ();
		templeLocation.setX (2);
		templeLocation.setY (2);
		templeLocation.setPlane (0);

		final MemoryBuilding temple = new MemoryBuilding ();
		temple.setBuildingID ("BL23");
		temple.setCityLocation (templeLocation);

		trueMap.getBuilding ().add (temple);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (6, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-4, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (10, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, priv.getResourceValue ().get (3).getProductionTypeID ());
		assertEquals (3, priv.getResourceValue ().get (3).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (3).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, priv.getResourceValue ().get (4).getProductionTypeID ());
		assertEquals (2, priv.getResourceValue ().get (4).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, priv.getResourceValue ().get (5).getProductionTypeID ());
		assertEquals (2, priv.getResourceValue ().get (5).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (5).getAmountStored ());

		// Player owns a node that covers 3 tiles of aura, giving 3x1.5 = 4.5 magic power
		for (int x = 10; x <= 12; x++)
		{
			final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
			terrainData.setNodeOwnerID (2);
			trueTerrain.getPlane ().get (0).getRow ().get (10).getCell ().get (x).setTerrainData (terrainData);
		}

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (6, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-4, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (10, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, priv.getResourceValue ().get (3).getProductionTypeID ());
		assertEquals (3, priv.getResourceValue ().get (3).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (3).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, priv.getResourceValue ().get (4).getProductionTypeID ());
		assertEquals (2, priv.getResourceValue ().get (4).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, priv.getResourceValue ().get (5).getProductionTypeID ());
		assertEquals (6, priv.getResourceValue ().get (5).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (5).getAmountStored ());
	}

	/**
	 * Tests the sendGlobalProductionValues method
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Test
	public final void testSendGlobalProductionValues () throws JAXBException, XMLStreamException 
	{
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final MomResourceValue resource1 = new MomResourceValue ();
		resource1.setAmountPerTurn (5);
		resource1.setAmountStored (25);
		resource1.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		priv.getResourceValue ().add (resource1);

		final MomResourceValue resource2 = new MomResourceValue ();
		resource2.setAmountPerTurn (7);
		resource2.setAmountStored (16);
		resource2.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS);
		priv.getResourceValue ().add (resource2);
		
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		trans.setOverlandCastingSkillRemainingThisTurn (25);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, trans);
		
		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);

		// Set up test object
		final MomServerResourceCalculations calc = new MomServerResourceCalculations ();
		
		// Run test
		calc.sendGlobalProductionValues (player, 17);
		assertEquals (1, msgs.getMessages ().size ());
		
		final UpdateGlobalEconomyMessage msg = (UpdateGlobalEconomyMessage) msgs.getMessages ().get (0);
		assertEquals (17, msg.getCastingSkillRemainingThisCombat ());
		assertEquals (25, msg.getOverlandCastingSkillRemainingThisTurn ());
		assertEquals (2, msg.getResourceValue ().size ());
		
		assertEquals (5, msg.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (25, msg.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, msg.getResourceValue ().get (0).getProductionTypeID ());

		assertEquals (7, msg.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (16, msg.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, msg.getResourceValue ().get (1).getProductionTypeID ());
	}

	/**
	 * Tests the listConsumersOfProductionType method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListConsumersOfProductionType () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Modify wizards' guild type of consumption so we can test all 3 types in one run
		final BuildingPopulationProductionModifier wizardsGuildConsumption = db.findBuilding ("BL21", "testListConsumersOfProductionType").getBuildingPopulationProductionModifier ().get (1);
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, wizardsGuildConsumption.getProductionTypeID ());
		wizardsGuildConsumption.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, null, null, null);
		players.add (player);

		// Cities used below
		final OverlandMapCityData ourCity = new OverlandMapCityData ();
		ourCity.setCityOwnerID (2);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (ourCity);

		final OverlandMapCityData enemyCity = new OverlandMapCityData ();
		enemyCity.setCityOwnerID (3);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setCityData (enemyCity);

		// Unit with wrong type of consumption
		final MemoryUnit warlocks = new MemoryUnit ();
		warlocks.setUnitID ("UN065");
		warlocks.setStatus (UnitStatusID.ALIVE);
		warlocks.setOwningPlayerID (2);
		trueMap.getUnit ().add (warlocks);

		// Unit with right type of consumption
		final MemoryUnit gargoyles = new MemoryUnit ();
		gargoyles.setUnitID ("UN157");
		gargoyles.setStatus (UnitStatusID.ALIVE);
		gargoyles.setOwningPlayerID (2);
		trueMap.getUnit ().add (gargoyles);

		// Unit with wrong owner
		final MemoryUnit gargoylesOtherPlayer = new MemoryUnit ();
		gargoylesOtherPlayer.setUnitID ("UN157");
		gargoylesOtherPlayer.setStatus (UnitStatusID.ALIVE);
		gargoylesOtherPlayer.setOwningPlayerID (3);
		trueMap.getUnit ().add (gargoylesOtherPlayer);

		// Unit with wrong status
		final MemoryUnit gargoylesOtherStatus = new MemoryUnit ();
		gargoylesOtherStatus.setUnitID ("UN157");
		gargoylesOtherStatus.setStatus (UnitStatusID.DEAD);
		gargoylesOtherStatus.setOwningPlayerID (2);
		trueMap.getUnit ().add (gargoylesOtherStatus);

		// Building with wrong type of consumption
		final OverlandMapCoordinatesEx parthenonLocation = new OverlandMapCoordinatesEx ();
		parthenonLocation.setX (20);
		parthenonLocation.setY (10);
		parthenonLocation.setPlane (1);

		final MemoryBuilding parthenon = new MemoryBuilding ();
		parthenon.setBuildingID ("BL24");
		parthenon.setCityLocation (parthenonLocation);
		trueMap.getBuilding ().add (parthenon);

		// Building with right type of consumption
		final OverlandMapCoordinatesEx wizardsGuildLocation = new OverlandMapCoordinatesEx ();
		wizardsGuildLocation.setX (20);
		wizardsGuildLocation.setY (10);
		wizardsGuildLocation.setPlane (1);

		final MemoryBuilding wizardsGuild = new MemoryBuilding ();
		wizardsGuild.setBuildingID ("BL21");
		wizardsGuild.setCityLocation (wizardsGuildLocation);
		trueMap.getBuilding ().add (wizardsGuild);

		// Building with wrong owner
		final OverlandMapCoordinatesEx wizardsGuildEnemyCityLocation = new OverlandMapCoordinatesEx ();
		wizardsGuildEnemyCityLocation.setX (21);
		wizardsGuildEnemyCityLocation.setY (10);
		wizardsGuildEnemyCityLocation.setPlane (1);

		final MemoryBuilding wizardsGuildEnemyCity = new MemoryBuilding ();
		wizardsGuildEnemyCity.setBuildingID ("BL21");
		wizardsGuildEnemyCity.setCityLocation (wizardsGuildEnemyCityLocation);
		trueMap.getBuilding ().add (wizardsGuildEnemyCity);

		// All spells have same type of consumption, but can test with a spell that doesn't have any consumption at all
		final MemoryMaintainedSpell entangle = new MemoryMaintainedSpell ();
		entangle.setSpellID ("SP033");
		entangle.setCastingPlayerID (2);
		trueMap.getMaintainedSpell ().add (entangle);

		// Spell with right type of consumption
		final MemoryMaintainedSpell natureAwareness = new MemoryMaintainedSpell ();
		natureAwareness.setSpellID ("SP034");
		natureAwareness.setCastingPlayerID (2);
		trueMap.getMaintainedSpell ().add (natureAwareness);

		// Spell with wrong owner
		final MemoryMaintainedSpell natureAwarenessOtherPlayer = new MemoryMaintainedSpell ();
		natureAwarenessOtherPlayer.setSpellID ("SP034");
		natureAwarenessOtherPlayer.setCastingPlayerID (3);
		trueMap.getMaintainedSpell ().add (natureAwarenessOtherPlayer);

		// Set up test object
		final UnitUtils unitUtils = new UnitUtils ();
		unitUtils.setPlayerPickUtils (new PlayerPickUtils ());
		
		final MomServerResourceCalculations calc = new MomServerResourceCalculations ();
		calc.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final List<IMomResourceConsumer> consumptions = calc.listConsumersOfProductionType
			(player, players, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, trueMap, db);

		assertEquals (3, consumptions.size ());

		assertEquals (MomResourceConsumerUnit.class, consumptions.get (0).getClass ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, consumptions.get (0).getProductionTypeID ());
		assertEquals (5, consumptions.get (0).getConsumptionAmount ());
		assertEquals (player, consumptions.get (0).getPlayer ());
		assertEquals (gargoyles, ((MomResourceConsumerUnit) consumptions.get (0)).getUnit ());

		assertEquals (MomResourceConsumerBuilding.class, consumptions.get (1).getClass ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, consumptions.get (1).getProductionTypeID ());
		assertEquals (3, consumptions.get (1).getConsumptionAmount ());
		assertEquals (player, consumptions.get (1).getPlayer ());
		assertEquals (wizardsGuild, ((MomResourceConsumerBuilding) consumptions.get (1)).getBuilding ());

		assertEquals (MomResourceConsumerSpell.class, consumptions.get (2).getClass ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, consumptions.get (2).getProductionTypeID ());
		assertEquals (7, consumptions.get (2).getConsumptionAmount ());
		assertEquals (player, consumptions.get (2).getPlayer ());
		assertEquals (natureAwareness, ((MomResourceConsumerSpell) consumptions.get (2)).getSpell ());
	}

	/**
	 * Tests the accumulateGlobalProductionValues method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If one of the production types in our resource list can't be found in the db
	 * @throws MomException If we encounter an unknown rounding direction, or a value that should be an exact multiple of 2 isn't
	 */
	@Test
	public final void testAccumulateGlobalProductionValues ()
		throws IOException, JAXBException, RecordNotFoundException, MomException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final SpellSettingData spellSettings = new SpellSettingData ();	// Only used by mock, so don't really care what's actually in here

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);

		// Set up test object
		final IResourceValueUtils utils = mock (IResourceValueUtils.class); 

		final MomServerResourceCalculations calc = new MomServerResourceCalculations ();
		calc.setResourceValueUtils (utils);
		
		// Research resource has no accumulation defined
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db)).thenReturn (10);
		
		// Mana accumulates into itself
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, spellSettings, db)).thenReturn (12);

		// Gold accumulates into itself
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (7);

		// Rations are accumulates into gold, halved + rounded down
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (9);

		// Call method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);

		// Check results
		verify (utils, times (0)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, 10);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, 12);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, 7);
		verify (utils, times (0)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, 9);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, 4);		// from rations

		// Negate all the per turn amounts and run it again (this is here to prove how the rounding down works on a negative value)
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db)).thenReturn (-10);
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, spellSettings, db)).thenReturn (-12);
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (-7);
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (-9);

		calc.accumulateGlobalProductionValues (player, spellSettings, db);

		verify (utils, times (0)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, -10);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, -12);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, -7);
		verify (utils, times (0)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, -9);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, -4);		// from rations
	}

	/**
	 * Tests the accumulateGlobalProductionValues method when we have a +ve production amount that should be a multiple of 2 but isn't
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If one of the production types in our resource list can't be found in the db
	 * @throws MomException If we encounter an unknown rounding direction, or a value that should be an exact multiple of 2 isn't
	 */
	@Test(expected=MomException.class)
	public final void testAccumulateGlobalProductionValues_NotMultipleOfTwoPositive ()
		throws IOException, JAXBException, RecordNotFoundException, MomException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final SpellSettingData spellSettings = new SpellSettingData ();	// Only used by mock, so don't really care what's actually in here

		db.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, "testAccumulateGlobalProductionValues_NotMultipleOfTwoPositive").setAccumulationHalved (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);

		// Set up test object
		final IResourceValueUtils utils = mock (IResourceValueUtils.class);
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (9);
		
		final MomServerResourceCalculations calc = new MomServerResourceCalculations ();
		calc.setResourceValueUtils (utils);
		
		// Call method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);
	}

	/**
	 * Tests the accumulateGlobalProductionValues method when we have a -ve production amount that should be a multiple of 2 but isn't
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If one of the production types in our resource list can't be found in the db
	 * @throws MomException If we encounter an unknown rounding direction, or a value that should be an exact multiple of 2 isn't
	 */
	@Test(expected=MomException.class)
	public final void testAccumulateGlobalProductionValues_NotMultipleOfTwoNegative ()
		throws IOException, JAXBException, RecordNotFoundException, MomException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final SpellSettingData spellSettings = new SpellSettingData ();	// Only used by mock, so don't really care what's actually in here

		db.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, "testAccumulateGlobalProductionValues_NotMultipleOfTwoNegative").setAccumulationHalved (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);

		// Set up test object
		final IResourceValueUtils utils = mock (IResourceValueUtils.class); 
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (-9);
		
		final MomServerResourceCalculations calc = new MomServerResourceCalculations ();
		calc.setResourceValueUtils (utils);
		
		// Call method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);
	}
	
	/**
	 * Tests the progressResearch method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressResearch () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final SpellSettingData spellSettings = new SpellSettingData ();	// Only used by mock, so don't really care what's actually in here

		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();

		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, trans);

		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		// Set up test object
		final IMomServerSpellCalculations serverSpellCalculations = mock (IMomServerSpellCalculations.class);
		final ISpellUtils spellUtils = mock (ISpellUtils.class);
		final IResourceValueUtils resourceValueUtils = mock (IResourceValueUtils.class);
		
		final MomServerResourceCalculations calc = new MomServerResourceCalculations ();
		calc.setResourceValueUtils (resourceValueUtils);
		calc.setSpellUtils (spellUtils);
		calc.setServerSpellCalculations (serverSpellCalculations);

		// Put in some dummy spells
		for (int n = 1; n <= 3; n++)
		{
			final SpellResearchStatus status = new SpellResearchStatus ();
			status.setSpellID ("SP00" + n);
			status.setStatus (SpellResearchStatusID.RESEARCHABLE);
			status.setRemainingResearchCost (n * 50);
			priv.getSpellResearchStatus ().add (status);
			
			when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), status.getSpellID ())).thenReturn (status);
		}
		
		// Generate 40 research each turn
		when (resourceValueUtils.calculateAmountPerTurnForProductionType
			(priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db)).thenReturn (40);
		
		// No spell being researched
		calc.progressResearch (player, spellSettings, db);
		
		assertNull (priv.getSpellIDBeingResearched ());
		assertEquals (3, priv.getSpellResearchStatus ().size ());
		assertEquals ("SP001", priv.getSpellResearchStatus ().get (0).getSpellID ());
		assertEquals (50, priv.getSpellResearchStatus ().get (0).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (0).getStatus ());
		assertEquals ("SP002", priv.getSpellResearchStatus ().get (1).getSpellID ());
		assertEquals (100, priv.getSpellResearchStatus ().get (1).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (1).getStatus ());
		assertEquals ("SP003", priv.getSpellResearchStatus ().get (2).getSpellID ());
		assertEquals (150, priv.getSpellResearchStatus ().get (2).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (2).getStatus ());
		
		assertEquals (0, msgs.getMessages ().size ());
		
		verify (serverSpellCalculations, times (0)).randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);
		
		// Spend 40 research - 60 left
		priv.setSpellIDBeingResearched ("SP002");

		calc.progressResearch (player, spellSettings, db);
		
		assertEquals ("SP002", priv.getSpellIDBeingResearched ());
		assertEquals (3, priv.getSpellResearchStatus ().size ());
		assertEquals ("SP001", priv.getSpellResearchStatus ().get (0).getSpellID ());
		assertEquals (50, priv.getSpellResearchStatus ().get (0).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (0).getStatus ());
		assertEquals ("SP002", priv.getSpellResearchStatus ().get (1).getSpellID ());
		assertEquals (60, priv.getSpellResearchStatus ().get (1).getRemainingResearchCost ());		// Drops to 60
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (1).getStatus ());
		assertEquals ("SP003", priv.getSpellResearchStatus ().get (2).getSpellID ());
		assertEquals (150, priv.getSpellResearchStatus ().get (2).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (2).getStatus ());

		assertEquals (1, msgs.getMessages ().size ());
		
		final UpdateRemainingResearchCostMessage msg1 = (UpdateRemainingResearchCostMessage) msgs.getMessages ().get (0);
		assertEquals ("SP002", msg1.getSpellID ());
		assertEquals (60, msg1.getRemainingResearchCost ());

		verify (serverSpellCalculations, times (0)).randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);

		// Spend 40 research - 20 left
		msgs.getMessages ().clear ();
		calc.progressResearch (player, spellSettings, db);
		
		assertEquals ("SP002", priv.getSpellIDBeingResearched ());
		assertEquals (3, priv.getSpellResearchStatus ().size ());
		assertEquals ("SP001", priv.getSpellResearchStatus ().get (0).getSpellID ());
		assertEquals (50, priv.getSpellResearchStatus ().get (0).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (0).getStatus ());
		assertEquals ("SP002", priv.getSpellResearchStatus ().get (1).getSpellID ());
		assertEquals (20, priv.getSpellResearchStatus ().get (1).getRemainingResearchCost ());		// Drops to 20
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (1).getStatus ());
		assertEquals ("SP003", priv.getSpellResearchStatus ().get (2).getSpellID ());
		assertEquals (150, priv.getSpellResearchStatus ().get (2).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (2).getStatus ());

		assertEquals (1, msgs.getMessages ().size ());
		
		final UpdateRemainingResearchCostMessage msg2 = (UpdateRemainingResearchCostMessage) msgs.getMessages ().get (0);
		assertEquals ("SP002", msg2.getSpellID ());
		assertEquals (20, msg2.getRemainingResearchCost ());
		
		verify (serverSpellCalculations, times (0)).randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);
		
		// Finish research
		msgs.getMessages ().clear ();
		calc.progressResearch (player, spellSettings, db);
		
		assertNull (priv.getSpellIDBeingResearched ());
		assertEquals (3, priv.getSpellResearchStatus ().size ());
		assertEquals ("SP001", priv.getSpellResearchStatus ().get (0).getSpellID ());
		assertEquals (50, priv.getSpellResearchStatus ().get (0).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (0).getStatus ());		// would be RESEARCHABLE_NOW if method wasn't mocked out
		assertEquals ("SP002", priv.getSpellResearchStatus ().get (1).getSpellID ());
		assertEquals (0, priv.getSpellResearchStatus ().get (1).getRemainingResearchCost ());		// Done
		assertEquals (SpellResearchStatusID.AVAILABLE, priv.getSpellResearchStatus ().get (1).getStatus ());
		assertEquals ("SP003", priv.getSpellResearchStatus ().get (2).getSpellID ());
		assertEquals (150, priv.getSpellResearchStatus ().get (2).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (2).getStatus ());		// same

		assertEquals (1, msgs.getMessages ().size ());
		
		final FullSpellListMessage msg3 = (FullSpellListMessage) msgs.getMessages ().get (0);
		assertEquals (3, msg3.getSpellResearchStatus ().size ());
		assertEquals ("SP001", msg3.getSpellResearchStatus ().get (0).getSpellID ());
		assertEquals (50, msg3.getSpellResearchStatus ().get (0).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, msg3.getSpellResearchStatus ().get (0).getStatus ());		// same
		assertEquals ("SP002", msg3.getSpellResearchStatus ().get (1).getSpellID ());
		assertEquals (0, msg3.getSpellResearchStatus ().get (1).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.AVAILABLE, msg3.getSpellResearchStatus ().get (1).getStatus ());
		assertEquals ("SP003", msg3.getSpellResearchStatus ().get (2).getSpellID ());
		assertEquals (150, msg3.getSpellResearchStatus ().get (2).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, msg3.getSpellResearchStatus ().get (2).getStatus ());		// same

		verify (serverSpellCalculations, times (1)).randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);		// <---
	}
	
	/**
	 * Tests the resetCastingSkillRemainingThisTurnToFull method
	 */
	@Test
	public final void testResetCastingSkillRemainingThisTurnToFull ()
	{
		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (null, null, priv, null, trans);
		
		// Set amount of casting skill
		final MomResourceValue skillImprovement = new MomResourceValue ();
		skillImprovement.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		skillImprovement.setAmountStored (10);
		priv.getResourceValue ().add (skillImprovement);
		
		// Set up test object
		final ResourceValueUtils resourceValueUtils = new ResourceValueUtils ();
		resourceValueUtils.setSkillCalculations (new MomSkillCalculations ());
		
		final MomServerResourceCalculations calc = new MomServerResourceCalculations ();
		calc.setResourceValueUtils (resourceValueUtils);
		
		// Run test
		calc.resetCastingSkillRemainingThisTurnToFull (player);
		assertEquals (3, trans.getOverlandCastingSkillRemainingThisTurn ());
	}
}
