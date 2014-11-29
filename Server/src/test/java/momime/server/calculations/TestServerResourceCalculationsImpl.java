package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.SkillCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RoundingDirectionID;
import momime.common.database.SpellUpkeep;
import momime.common.database.UnitUpkeep;
import momime.common.database.newgame.MapSizeData;
import momime.common.database.newgame.NodeStrengthData;
import momime.common.database.newgame.SpellSettingData;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.messages.servertoclient.UpdateGlobalEconomyMessage;
import momime.common.messages.servertoclient.UpdateRemainingResearchCostMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.ResourceValueUtilsImpl;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_5.Building;
import momime.server.database.v0_9_5.Plane;
import momime.server.database.v0_9_5.Spell;
import momime.server.database.v0_9_5.Unit;
import momime.server.process.resourceconsumer.MomResourceConsumer;
import momime.server.process.resourceconsumer.MomResourceConsumerBuilding;
import momime.server.process.resourceconsumer.MomResourceConsumerFactory;
import momime.server.process.resourceconsumer.MomResourceConsumerSpell;
import momime.server.process.resourceconsumer.MomResourceConsumerUnit;
import momime.server.utils.UnitServerUtils;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the ServerResourceCalculations class
 */
public final class TestServerResourceCalculationsImpl
{
	/**
	 * Tests the recalculateAmountsPerTurn method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRecalculateAmountsPerTurn () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlane ()).thenReturn (planes);
		
		final UnitUpkeep shadowDemonsUpkeep = new UnitUpkeep ();
		shadowDemonsUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		
		final Unit shadowDemonsDef = new Unit ();
		shadowDemonsDef.getUnitUpkeep ().add (shadowDemonsUpkeep);
		when (db.findUnit ("UN172", "recalculateAmountsPerTurn")).thenReturn (shadowDemonsDef);

		final UnitUpkeep warlocksRations = new UnitUpkeep ();
		warlocksRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);

		final UnitUpkeep warlocksGold = new UnitUpkeep ();
		warlocksGold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		
		final Unit warlocksDef = new Unit ();
		warlocksDef.getUnitUpkeep ().add (warlocksGold);
		warlocksDef.getUnitUpkeep ().add (warlocksRations);
		when (db.findUnit ("UN065", "recalculateAmountsPerTurn")).thenReturn (warlocksDef);
		
		final SpellUpkeep crusadeUpkeep = new SpellUpkeep ();
		crusadeUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		crusadeUpkeep.setUpkeepValue (10);
		
		final Spell crusadeDef = new Spell ();
		crusadeDef.getSpellUpkeep ().add (crusadeUpkeep);
		when (db.findSpell ("SP158", "recalculateAmountsPerTurn")).thenReturn (crusadeDef);
		
		// Session description
		final MapSizeData mapSize = ServerTestData.createMapSizeData ();
		
		final NodeStrengthData nodeStrength = new NodeStrengthData ();
		nodeStrength.setDoubleNodeAuraMagicPower (3);		// 1.5 mana per node aura cell
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		sd.setNodeStrength (nodeStrength);

		// Overland map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (mapSize);
		
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
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		final CityCalculations cityCalc = mock (CityCalculations.class);

		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setCityCalculations (cityCalc);
		calc.setUnitServerUtils (mock (UnitServerUtils.class));
		
		// Use real resource value utils, so we don't have to mock every possible value of addToAmountPerTurn
		calc.setResourceValueUtils (new ResourceValueUtilsImpl ());		

		// We have some shadow demons (7 mana upkeep)
		final MemoryUnit shadowDemons = new MemoryUnit ();
		shadowDemons.setUnitID ("UN172");
		shadowDemons.setStatus (UnitStatusID.ALIVE);
		shadowDemons.setOwningPlayerID (2);
		trueMap.getUnit ().add (shadowDemons);

		when (unitUtils.getModifiedUpkeepValue (shadowDemons, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, players, db)).thenReturn (7);
		
		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-7, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Add a crusade spell (10 mana upkeep)
		final MemoryMaintainedSpell crusade = new MemoryMaintainedSpell ();
		crusade.setSpellID ("SP158");
		crusade.setCastingPlayerID (2);
		trueMap.getMaintainedSpell ().add (crusade);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-17, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Wizard has Channeler, halfing spell maintainence (half of 17 is 8.5, proves that maintainence is rounded up)
		when (playerPickUtils.getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER)).thenReturn (1);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (1, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());

		// Add some warlocks, so we get some other production type IDs
		final MemoryUnit warlocks = new MemoryUnit ();
		warlocks.setUnitID ("UN065");
		warlocks.setStatus (UnitStatusID.ALIVE);
		warlocks.setOwningPlayerID (2);
		trueMap.getUnit ().add (warlocks);
		
		when (unitUtils.getModifiedUpkeepValue (warlocks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, players, db)).thenReturn (1);
		when (unitUtils.getModifiedUpkeepValue (warlocks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, players, db)).thenReturn (5);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (3, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-5, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (-1, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());

		// Prove that any prior values in Per Turn get wiped, and any prior values in Stored get preserved
		priv.getResourceValue ().get (1).setAmountPerTurn (10);
		priv.getResourceValue ().get (1).setAmountStored (10);

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (3, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-5, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (10, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
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
		
		final CityProductionBreakdown cityGold = new CityProductionBreakdown ();
		cityGold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		cityGold.setCappedProductionAmount (4);

		final CityProductionBreakdown cityProduction = new CityProductionBreakdown ();
		cityProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		cityProduction.setCappedProductionAmount (3);

		final CityProductionBreakdown cityRations = new CityProductionBreakdown ();
		cityRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		cityRations.setCappedProductionAmount (4);
		cityRations.setConsumptionAmount (5);
		
		final CityProductionBreakdown cityMaxSize = new CityProductionBreakdown ();
		cityMaxSize.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		
		final CityProductionBreakdownsEx cityBreakdown = new CityProductionBreakdownsEx ();
		cityBreakdown.getProductionType ().add (cityGold);
		cityBreakdown.getProductionType ().add (cityProduction);
		cityBreakdown.getProductionType ().add (cityRations);
		cityBreakdown.getProductionType ().add (cityMaxSize);
		
		when (cityCalc.calculateAllCityProductions (players, trueTerrain, trueMap.getBuilding (), new MapCoordinates3DEx (2, 2, 0), "TR04", sd, true, false, db)).thenReturn (cityBreakdown);

		// Population will eat 5 rations, but produce 2x2 = 4 rations, and generate 3 x 1.5 = 4.5 gold from taxes and 2x.5 + 1x2 production
		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (5, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-1, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (10, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (-2, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, priv.getResourceValue ().get (3).getProductionTypeID ());
		assertEquals (3, priv.getResourceValue ().get (3).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (3).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, priv.getResourceValue ().get (4).getProductionTypeID ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountStored ());

		// Player owns a node that covers 3 tiles of aura, giving 3x1.5 = 4.5 magic power
		for (int x = 10; x <= 12; x++)
		{
			final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
			terrainData.setNodeOwnerID (2);
			trueTerrain.getPlane ().get (0).getRow ().get (10).getCell ().get (x).setTerrainData (terrainData);
		}

		calc.recalculateAmountsPerTurn (player, players, trueMap, sd, db);
		assertEquals (6, priv.getResourceValue ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, priv.getResourceValue ().get (0).getProductionTypeID ());
		assertEquals (-9, priv.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, priv.getResourceValue ().get (1).getProductionTypeID ());
		assertEquals (-1, priv.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (10, priv.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, priv.getResourceValue ().get (2).getProductionTypeID ());
		assertEquals (-2, priv.getResourceValue ().get (2).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (2).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, priv.getResourceValue ().get (3).getProductionTypeID ());
		assertEquals (3, priv.getResourceValue ().get (3).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (3).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, priv.getResourceValue ().get (4).getProductionTypeID ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountPerTurn ());
		assertEquals (0, priv.getResourceValue ().get (4).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, priv.getResourceValue ().get (5).getProductionTypeID ());
		assertEquals (4, priv.getResourceValue ().get (5).getAmountPerTurn ());
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
		resource1.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		priv.getResourceValue ().add (resource1);

		final MomResourceValue resource2 = new MomResourceValue ();
		resource2.setAmountPerTurn (7);
		resource2.setAmountStored (16);
		resource2.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		priv.getResourceValue ().add (resource2);
		
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		trans.setOverlandCastingSkillRemainingThisTurn (25);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, trans);
		
		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);

		// Set up test object
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		
		// Run test
		calc.sendGlobalProductionValues (player, 17);
		assertEquals (1, msgs.getMessages ().size ());
		
		final UpdateGlobalEconomyMessage msg = (UpdateGlobalEconomyMessage) msgs.getMessages ().get (0);
		assertEquals (17, msg.getCastingSkillRemainingThisCombat ());
		assertEquals (25, msg.getOverlandCastingSkillRemainingThisTurn ());
		assertEquals (2, msg.getResourceValue ().size ());
		
		assertEquals (5, msg.getResourceValue ().get (0).getAmountPerTurn ());
		assertEquals (25, msg.getResourceValue ().get (0).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, msg.getResourceValue ().get (0).getProductionTypeID ());

		assertEquals (7, msg.getResourceValue ().get (1).getAmountPerTurn ());
		assertEquals (16, msg.getResourceValue ().get (1).getAmountStored ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, msg.getResourceValue ().get (1).getProductionTypeID ());
	}

	/**
	 * Tests the listConsumersOfProductionType method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListConsumersOfProductionType () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		final UnitUpkeep gargoylesUpkeep = new UnitUpkeep ();
		gargoylesUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		
		final Unit gargoylesDef = new Unit ();
		gargoylesDef.getUnitUpkeep ().add (gargoylesUpkeep);
		when (db.findUnit ("UN157", "listConsumersOfProductionType")).thenReturn (gargoylesDef);
		
		final Building wizardsGuildDef = new Building ();
		when (db.findBuilding ("BL21", "listConsumersOfProductionType")).thenReturn (wizardsGuildDef);
		
		final Spell entangleDef = new Spell ();
		when (db.findSpell ("SP033", "listConsumersOfProductionType")).thenReturn (entangleDef);
		
		final SpellUpkeep natureAwarenessUpkeep = new SpellUpkeep ();
		natureAwarenessUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		natureAwarenessUpkeep.setUpkeepValue (7);
		
		final Spell natureAwarenessDef = new Spell ();
		natureAwarenessDef.getSpellUpkeep ().add (natureAwarenessUpkeep);
		when (db.findSpell ("SP034", "listConsumersOfProductionType")).thenReturn (natureAwarenessDef);
		
		// Building consumption
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuildingConsumption (wizardsGuildDef, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (3);
		
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
		final MemoryBuilding parthenon = new MemoryBuilding ();
		parthenon.setBuildingID ("BL24");
		parthenon.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getBuilding ().add (parthenon);

		// Building with right type of consumption
		final MemoryBuilding wizardsGuild = new MemoryBuilding ();
		wizardsGuild.setBuildingID ("BL21");
		wizardsGuild.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getBuilding ().add (wizardsGuild);

		// Building with wrong owner
		final MemoryBuilding wizardsGuildEnemyCity = new MemoryBuilding ();
		wizardsGuildEnemyCity.setBuildingID ("BL21");
		wizardsGuildEnemyCity.setCityLocation (new MapCoordinates3DEx (21, 10, 1));
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

		// Unit upkeep
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedUpkeepValue (gargoyles, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, players, db)).thenReturn (5);
		when (unitUtils.getModifiedUpkeepValue (gargoylesOtherStatus, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, players, db)).thenReturn (5);
		when (unitUtils.getModifiedUpkeepValue (gargoylesOtherPlayer, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, players, db)).thenReturn (5);
		
		// Create dummy implementation for the factory that is usually provided by spring
		final MomResourceConsumerFactory factory = new MomResourceConsumerFactory ()
		{
			@Override
			public final MomResourceConsumerBuilding createBuildingConsumer ()
			{
				return new MomResourceConsumerBuilding ();
			}

			@Override
			public final MomResourceConsumerSpell createSpellConsumer ()
			{
				return new MomResourceConsumerSpell ();
			}

			@Override
			public final MomResourceConsumerUnit createUnitConsumer ()
			{
				return new MomResourceConsumerUnit ();
			}
		};
		
		// Set up object to test
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setMemoryBuildingUtils (buildingUtils);
		calc.setUnitUtils (unitUtils);
		calc.setMomResourceConsumerFactory (factory);
		
		// Run test
		final List<MomResourceConsumer> consumptions = calc.listConsumersOfProductionType
			(player, players, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, trueMap, db);

		assertEquals (3, consumptions.size ());

		assertEquals (MomResourceConsumerUnit.class, consumptions.get (0).getClass ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, consumptions.get (0).getProductionTypeID ());
		assertEquals (5, consumptions.get (0).getConsumptionAmount ());
		assertEquals (player, consumptions.get (0).getPlayer ());
		assertSame (gargoyles, ((MomResourceConsumerUnit) consumptions.get (0)).getUnit ());

		assertEquals (MomResourceConsumerBuilding.class, consumptions.get (1).getClass ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, consumptions.get (1).getProductionTypeID ());
		assertEquals (3, consumptions.get (1).getConsumptionAmount ());
		assertEquals (player, consumptions.get (1).getPlayer ());
		assertSame (wizardsGuild, ((MomResourceConsumerBuilding) consumptions.get (1)).getBuilding ());

		assertEquals (MomResourceConsumerSpell.class, consumptions.get (2).getClass ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, consumptions.get (2).getProductionTypeID ());
		assertEquals (7, consumptions.get (2).getConsumptionAmount ());
		assertEquals (player, consumptions.get (2).getPlayer ());
		assertSame (natureAwareness, ((MomResourceConsumerSpell) consumptions.get (2)).getSpell ());
	}

	/**
	 * Tests the accumulateGlobalProductionValues method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAccumulateGlobalProductionValues () throws Exception
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
		final ResourceValueUtils utils = mock (ResourceValueUtils.class); 

		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (utils);
		
		// Research resource has no accumulation defined
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db)).thenReturn (10);
		
		// Mana accumulates into itself
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, spellSettings, db)).thenReturn (12);

		// Gold accumulates into itself
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (7);

		// Rations are accumulates into gold, halved + rounded down
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (9);

		// Call method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);

		// Check results
		verify (utils, times (0)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, 10);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, 12);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 7);
		verify (utils, times (0)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, 9);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 4);		// from rations

		// Negate all the per turn amounts and run it again (this is here to prove how the rounding down works on a negative value)
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db)).thenReturn (-10);
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, spellSettings, db)).thenReturn (-12);
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (-7);
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (-9);

		calc.accumulateGlobalProductionValues (player, spellSettings, db);

		verify (utils, times (0)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, -10);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -12);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -7);
		verify (utils, times (0)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, -9);
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -4);		// from rations
	}

	/**
	 * Tests the accumulateGlobalProductionValues method when we have a +ve production amount that should be a multiple of 2 but isn't
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testAccumulateGlobalProductionValues_NotMultipleOfTwoPositive () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final SpellSettingData spellSettings = new SpellSettingData ();	// Only used by mock, so don't really care what's actually in here

		db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, "testAccumulateGlobalProductionValues_NotMultipleOfTwoPositive").setAccumulationHalved (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);

		// Set up test object
		final ResourceValueUtils utils = mock (ResourceValueUtils.class);
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (9);
		
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (utils);
		
		// Call method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);
	}

	/**
	 * Tests the accumulateGlobalProductionValues method when we have a -ve production amount that should be a multiple of 2 but isn't
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testAccumulateGlobalProductionValues_NotMultipleOfTwoNegative () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final SpellSettingData spellSettings = new SpellSettingData ();	// Only used by mock, so don't really care what's actually in here

		db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, "testAccumulateGlobalProductionValues_NotMultipleOfTwoNegative").setAccumulationHalved (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);

		// Set up test object
		final ResourceValueUtils utils = mock (ResourceValueUtils.class); 
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (-9);
		
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
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
		final ServerSpellCalculations serverSpellCalculations = mock (ServerSpellCalculations.class);
		final SpellUtils spellUtils = mock (SpellUtils.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
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
			(priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db)).thenReturn (40);
		
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

		final PlayerDescription pd = new PlayerDescription ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, trans);
		
		// Set amount of casting skill
		final MomResourceValue skillImprovement = new MomResourceValue ();
		skillImprovement.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		skillImprovement.setAmountStored (10);
		priv.getResourceValue ().add (skillImprovement);
		
		// Set up test object
		final ResourceValueUtilsImpl resourceValueUtils = new ResourceValueUtilsImpl ();
		resourceValueUtils.setSkillCalculations (new SkillCalculationsImpl ());
		
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (resourceValueUtils);
		
		// Run test
		calc.resetCastingSkillRemainingThisTurnToFull (player);
		assertEquals (3, trans.getOverlandCastingSkillRemainingThisTurn ());
	}
}