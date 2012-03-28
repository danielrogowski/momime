package momime.server.calculations;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.messages.PlayerPickUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;
import momime.server.process.resourceconsumer.IMomResourceConsumer;
import momime.server.process.resourceconsumer.MomResourceConsumerBuilding;
import momime.server.process.resourceconsumer.MomResourceConsumerSpell;
import momime.server.process.resourceconsumer.MomResourceConsumerUnit;

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
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

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
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (serverDB, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
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

		// We have some shadow demons (7 mana upkeep)
		final MemoryUnit shadowDemons = UnitUtils.createMemoryUnit ("UN172", 1, null, -1, true, db, debugLogger);
		shadowDemons.setOwningPlayerID (2);
		trueMap.getUnit ().add (shadowDemons);

		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-7, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Add a crusade spell (10 mana upkeep)
		final MemoryMaintainedSpell crusade = new MemoryMaintainedSpell ();
		crusade.setSpellID ("SP158");
		crusade.setCastingPlayerID (2);
		trueMap.getMaintainedSpell ().add (crusade);

		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-17, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Wizard has Channeler, halfing spell maintainence (half of 17 is 8.5, proves that maintainence is rounded up)
		PlayerPickUtils.updatePickQuantity (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_CHANNELER, 1, debugLogger);

		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Add some warlocks, so we get some other production type IDs
		final MemoryUnit warlocks = UnitUtils.createMemoryUnit ("UN065", 2, 0, 0, true, db, debugLogger);
		warlocks.setOwningPlayerID (2);
		trueMap.getUnit ().add (warlocks);

		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
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

		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
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
		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
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
		final OverlandMapCoordinates granaryLocation = new OverlandMapCoordinates ();
		granaryLocation.setX (2);
		granaryLocation.setY (2);
		granaryLocation.setPlane (0);

		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID ("BL29");
		granary.setCityLocation (granaryLocation);

		trueMap.getBuilding ().add (granary);

		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
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
		final OverlandMapCoordinates templeLocation = new OverlandMapCoordinates ();
		templeLocation.setX (2);
		templeLocation.setY (2);
		templeLocation.setPlane (0);

		final MemoryBuilding temple = new MemoryBuilding ();
		temple.setBuildingID ("BL23");
		temple.setCityLocation (templeLocation);

		trueMap.getBuilding ().add (temple);

		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
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

		MomServerResourceCalculations.recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);
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
	 * Tests the listConsumersOfProductionType method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListConsumersOfProductionType () throws Exception
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

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
		final OverlandMapCoordinates parthenonLocation = new OverlandMapCoordinates ();
		parthenonLocation.setX (20);
		parthenonLocation.setY (10);
		parthenonLocation.setPlane (1);

		final MemoryBuilding parthenon = new MemoryBuilding ();
		parthenon.setBuildingID ("BL24");
		parthenon.setCityLocation (parthenonLocation);
		trueMap.getBuilding ().add (parthenon);

		// Building with right type of consumption
		final OverlandMapCoordinates wizardsGuildLocation = new OverlandMapCoordinates ();
		wizardsGuildLocation.setX (20);
		wizardsGuildLocation.setY (10);
		wizardsGuildLocation.setPlane (1);

		final MemoryBuilding wizardsGuild = new MemoryBuilding ();
		wizardsGuild.setBuildingID ("BL21");
		wizardsGuild.setCityLocation (wizardsGuildLocation);
		trueMap.getBuilding ().add (wizardsGuild);

		// Building with wrong owner
		final OverlandMapCoordinates wizardsGuildEnemyCityLocation = new OverlandMapCoordinates ();
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

		// Run test
		final List<IMomResourceConsumer> consumptions = MomServerResourceCalculations.listConsumersOfProductionType
			(player, players, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, trueMap, db, debugLogger);

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
}
