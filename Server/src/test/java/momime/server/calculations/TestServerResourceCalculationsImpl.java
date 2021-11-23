package momime.server.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityProductionCalculations;
import momime.common.calculations.SkillCalculationsImpl;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.NodeStrength;
import momime.common.database.OverlandMapSize;
import momime.common.database.Plane;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.ProductionTypeEx;
import momime.common.database.RoundingDirectionID;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
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
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.ResourceValueUtilsImpl;
import momime.common.utils.SpellUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.process.resourceconsumer.MomResourceConsumer;
import momime.server.process.resourceconsumer.MomResourceConsumerBuilding;
import momime.server.process.resourceconsumer.MomResourceConsumerFactory;
import momime.server.process.resourceconsumer.MomResourceConsumerSpell;
import momime.server.process.resourceconsumer.MomResourceConsumerUnit;
import momime.server.utils.UnitServerUtils;

/**
 * Tests the ServerResourceCalculations class
 */
@ExtendWith(MockitoExtension.class)
public final class TestServerResourceCalculationsImpl extends ServerTestData
{
	/**
	 * Tests the recalculateAmountsPerTurn method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRecalculateAmountsPerTurn () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlane ()).thenReturn (planes);
		
		final ProductionTypeAndUndoubledValue crusadeUpkeep = new ProductionTypeAndUndoubledValue ();
		crusadeUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		crusadeUpkeep.setUndoubledProductionValue (10);
		
		final Spell crusadeDef = new Spell ();
		crusadeDef.getSpellUpkeep ().add (crusadeUpkeep);
		when (db.findSpell ("SP158", "recalculateAmountsPerTurn")).thenReturn (crusadeDef);
		
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final NodeStrength nodeStrength = new NodeStrength ();
		nodeStrength.setDoubleNodeAuraMagicPower (3);		// 1.5 mana per node aura cell
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setNodeStrength (nodeStrength);

		// Overland map
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);
		players.add (player);
		
		// Spells
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		// Set up test object
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		final CityProductionCalculations cityCalc = mock (CityProductionCalculations.class);

		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setExpandUnitDetails (expand);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setCityProductionCalculations (cityCalc);
		calc.setUnitServerUtils (mock (UnitServerUtils.class));
		
		// Use real resource value utils, so we don't have to mock every possible value of addToAmountPerTurn
		final ResourceValueUtilsImpl resourceValueUtils = new ResourceValueUtilsImpl ();
		resourceValueUtils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		resourceValueUtils.setExpandUnitDetails (expand);
		calc.setResourceValueUtils (resourceValueUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);

		// We have some shadow demons (7 mana upkeep)
		final MemoryUnit shadowDemons = new MemoryUnit ();
		shadowDemons.setUnitID ("UN172");
		shadowDemons.setStatus (UnitStatusID.ALIVE);
		shadowDemons.setOwningPlayerID (2);
		trueMap.getUnit ().add (shadowDemons);

		final ExpandedUnitDetails xuShadowDemons = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (shadowDemons, null, null, null, players, trueMap, db)).thenReturn (xuShadowDemons);
		when (xuShadowDemons.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (7);
		
		final Set<String> shadowDemonsUpkeeps = new HashSet<String> ();
		shadowDemonsUpkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		when (xuShadowDemons.listModifiedUpkeepProductionTypeIDs ()).thenReturn (shadowDemonsUpkeeps);
		
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

		// Wizard has Channeler, halfing spell maintenance (half of 17 is 8.5, proves that maintenance is rounded up)
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

		final ExpandedUnitDetails xuWarlocks = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (warlocks, null, null, null, players, trueMap, db)).thenReturn (xuWarlocks);
		when (xuWarlocks.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS)).thenReturn (1);
		when (xuWarlocks.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (5);
		
		final Set<String> warlocksUpkeeps = new HashSet<String> ();
		warlocksUpkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		warlocksUpkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		when (xuWarlocks.listModifiedUpkeepProductionTypeIDs ()).thenReturn (warlocksUpkeeps);

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
		
		when (cityCalc.calculateAllCityProductions (players, trueTerrain, trueMap.getBuilding (), trueMap.getMaintainedSpell (),
			new MapCoordinates3DEx (2, 2, 0), "TR04", sd, true, false, db)).thenReturn (cityBreakdown);

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
		pd.setHuman (true);
		
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
		calc.sendGlobalProductionValues (player, 17, false);
		assertEquals (1, msgs.getMessages ().size ());
		
		final UpdateGlobalEconomyMessage msg = (UpdateGlobalEconomyMessage) msgs.getMessages ().get (0);
		assertEquals (17, msg.getCastingSkillRemainingThisCombat ().intValue ());
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
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building wizardsGuildDef = new Building ();
		when (db.findBuilding ("BL21", "listConsumersOfProductionType")).thenReturn (wizardsGuildDef);
		
		final Building parthenonDef = new Building ();
		when (db.findBuilding ("BL24", "listConsumersOfProductionType")).thenReturn (parthenonDef);
		
		final Spell entangleDef = new Spell ();
		when (db.findSpell ("SP033", "listConsumersOfProductionType")).thenReturn (entangleDef);
		
		final ProductionTypeAndUndoubledValue natureAwarenessUpkeep = new ProductionTypeAndUndoubledValue ();
		natureAwarenessUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		natureAwarenessUpkeep.setUndoubledProductionValue (7);
		
		final Spell natureAwarenessDef = new Spell ();
		natureAwarenessDef.getSpellUpkeep ().add (natureAwarenessUpkeep);
		when (db.findSpell ("SP034", "listConsumersOfProductionType")).thenReturn (natureAwarenessDef);
		
		// Building consumption
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuildingConsumption (parthenonDef, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (0);
		when (buildingUtils.findBuildingConsumption (wizardsGuildDef, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (3);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);

		final ExpandedUnitDetails xuWarlocks = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (warlocks, null, null, null, players, trueMap, db)).thenReturn (xuWarlocks);
		
		final ExpandedUnitDetails xuGargoyles = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (gargoyles, null, null, null, players, trueMap, db)).thenReturn (xuGargoyles);
		when (xuGargoyles.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (5);
		
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
		
		// No Time Stop
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		// Set up object to test
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setMemoryBuildingUtils (buildingUtils);
		calc.setExpandUnitDetails (expand);
		calc.setMomResourceConsumerFactory (factory);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
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
	 * Tests the accumulateGlobalProductionValues method where the full amount of one resource is copied to another
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAccumulateGlobalProductionValues_FullAmount () throws Exception
	{
		// Mock production types
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionTypeID ("RE01");
		productionType.setAccumulatesInto ("RE02");
		
		when (db.getProductionTypes ()).thenReturn (Arrays.asList (productionType));
		
		// Only needed for mocks
		final SpellSetting spellSettings = new SpellSetting ();

		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (null, pub, priv, null, null);

		// Amount of resource
		final ResourceValueUtils utils = mock (ResourceValueUtils.class); 
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), "RE01", spellSettings, db)).thenReturn (9);
		
		// Set up object to test
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (utils);
		
		// Run method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);

		// Check results
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), "RE02", 9);
	}
	
	/**
	 * Tests the accumulateGlobalProductionValues method where half of one resource is copied to another, rounded down
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAccumulateGlobalProductionValues_RoundDown () throws Exception
	{
		// Mock production types
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionTypeID ("RE01");
		productionType.setAccumulatesInto ("RE02");
		productionType.setAccumulationHalved (RoundingDirectionID.ROUND_DOWN);
		
		when (db.getProductionTypes ()).thenReturn (Arrays.asList (productionType));
		
		// Only needed for mocks
		final SpellSetting spellSettings = new SpellSetting ();

		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (null, pub, priv, null, null);

		// Amount of resource
		final ResourceValueUtils utils = mock (ResourceValueUtils.class); 
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), "RE01", spellSettings, db)).thenReturn (9);
		
		// Set up object to test
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (utils);
		
		// Run method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);

		// Check results
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), "RE02", 4);
	}
	
	/**
	 * Tests the accumulateGlobalProductionValues method where half of one resource is copied to another, rounded up
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAccumulateGlobalProductionValues_RoundUp () throws Exception
	{
		// Mock production types
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionTypeID ("RE01");
		productionType.setAccumulatesInto ("RE02");
		productionType.setAccumulationHalved (RoundingDirectionID.ROUND_UP);
		
		when (db.getProductionTypes ()).thenReturn (Arrays.asList (productionType));
		
		// Only needed for mocks
		final SpellSetting spellSettings = new SpellSetting ();

		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (null, pub, priv, null, null);

		// Amount of resource
		final ResourceValueUtils utils = mock (ResourceValueUtils.class); 
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), "RE01", spellSettings, db)).thenReturn (9);
		
		// Set up object to test
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (utils);
		
		// Run method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);

		// Check results
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), "RE02", 5);
	}
	
	/**
	 * Tests the accumulateGlobalProductionValues method where half of one resource is copied to another, and it must be an exact multiple of 2, and is
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAccumulateGlobalProductionValues_ExactMultiple () throws Exception
	{
		// Mock production types
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionTypeID ("RE01");
		productionType.setAccumulatesInto ("RE02");
		productionType.setAccumulationHalved (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);
		
		when (db.getProductionTypes ()).thenReturn (Arrays.asList (productionType));
		
		// Only needed for mocks
		final SpellSetting spellSettings = new SpellSetting ();

		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (null, pub, priv, null, null);

		// Amount of resource
		final ResourceValueUtils utils = mock (ResourceValueUtils.class); 
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), "RE01", spellSettings, db)).thenReturn (8);
		
		// Set up object to test
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (utils);
		
		// Run method
		calc.accumulateGlobalProductionValues (player, spellSettings, db);

		// Check results
		verify (utils, times (1)).addToAmountStored (priv.getResourceValue (), "RE02", 4);
	}
	
	/**
	 * Tests the accumulateGlobalProductionValues method where half of one resource is copied to another, and it must be an exact multiple of 2, and isn't
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAccumulateGlobalProductionValues_NotExactMultiple () throws Exception
	{
		// Mock production types
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionTypeID ("RE01");
		productionType.setAccumulatesInto ("RE02");
		productionType.setAccumulationHalved (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);
		
		when (db.getProductionTypes ()).thenReturn (Arrays.asList (productionType));
		
		// Only needed for mocks
		final SpellSetting spellSettings = new SpellSetting ();

		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (null, pub, priv, null, null);

		// Amount of resource
		final ResourceValueUtils utils = mock (ResourceValueUtils.class); 
		when (utils.calculateAmountPerTurnForProductionType (priv, pub.getPick (), "RE01", spellSettings, db)).thenReturn (9);
		
		// Set up object to test
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (utils);

		// Run method
		final MomException e = assertThrows (MomException.class, () ->
		{
			calc.accumulateGlobalProductionValues (player, spellSettings, db);
		});
		
		assertEquals ("accumulateGlobalProductionValues: Expect value for RE01 being accumulated into RE02 to be exact multiple of 2 but was 9", e.getMessage ());
	}
	
	/**
	 * Tests the progressResearch method
	 * 
	 * @param isHuman Whether to test with human or AI player
	 * @throws Exception If there is a problem
	 */
	private final void testProgressResearch (final boolean isHuman) throws Exception
	{
		final CommonDatabase db = loadServerDatabase ();
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		final SpellSetting spellSettings = new SpellSetting ();	// Only used by mock, so don't really care what's actually in here		
		sd.setSpellSetting (spellSettings);
		
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiSpellResearchMultiplier (110);
		sd.setDifficultyLevel (difficultyLevel);

		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();

		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (isHuman);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, trans);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

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
			
			if (n == 2)
				when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), status.getSpellID ())).thenReturn (status);
		}
		
		// Generate 40 research each turn; AI players get +10% so they get 44 
		when (resourceValueUtils.calculateAmountPerTurnForProductionType
			(priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db)).thenReturn (40);
		
		// No spell being researched
		calc.progressResearch (player, players, sd, db);
		
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

		calc.progressResearch (player, players, sd, db);
		
		assertEquals ("SP002", priv.getSpellIDBeingResearched ());
		assertEquals (3, priv.getSpellResearchStatus ().size ());
		assertEquals ("SP001", priv.getSpellResearchStatus ().get (0).getSpellID ());
		assertEquals (50, priv.getSpellResearchStatus ().get (0).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (0).getStatus ());
		assertEquals ("SP002", priv.getSpellResearchStatus ().get (1).getSpellID ());
		assertEquals (isHuman ? 60 : 56, priv.getSpellResearchStatus ().get (1).getRemainingResearchCost ());		// Drops to 60
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (1).getStatus ());
		assertEquals ("SP003", priv.getSpellResearchStatus ().get (2).getSpellID ());
		assertEquals (150, priv.getSpellResearchStatus ().get (2).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (2).getStatus ());

		assertEquals (isHuman ? 1 : 0, msgs.getMessages ().size ());
		
		if (isHuman)
		{
			final UpdateRemainingResearchCostMessage msg1 = (UpdateRemainingResearchCostMessage) msgs.getMessages ().get (0);
			assertEquals ("SP002", msg1.getSpellID ());
			assertEquals (60, msg1.getRemainingResearchCost ());
		}

		verify (serverSpellCalculations, times (0)).randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);

		// Spend 40 research - 20 left
		msgs.getMessages ().clear ();
		calc.progressResearch (player, players, sd, db);
		
		assertEquals ("SP002", priv.getSpellIDBeingResearched ());
		assertEquals (3, priv.getSpellResearchStatus ().size ());
		assertEquals ("SP001", priv.getSpellResearchStatus ().get (0).getSpellID ());
		assertEquals (50, priv.getSpellResearchStatus ().get (0).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (0).getStatus ());
		assertEquals ("SP002", priv.getSpellResearchStatus ().get (1).getSpellID ());
		assertEquals (isHuman ? 20 : 12, priv.getSpellResearchStatus ().get (1).getRemainingResearchCost ());		// Drops to 20
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (1).getStatus ());
		assertEquals ("SP003", priv.getSpellResearchStatus ().get (2).getSpellID ());
		assertEquals (150, priv.getSpellResearchStatus ().get (2).getRemainingResearchCost ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE, priv.getSpellResearchStatus ().get (2).getStatus ());

		assertEquals (isHuman ? 1 : 0, msgs.getMessages ().size ());
		
		if (isHuman)
		{
			final UpdateRemainingResearchCostMessage msg2 = (UpdateRemainingResearchCostMessage) msgs.getMessages ().get (0);
			assertEquals ("SP002", msg2.getSpellID ());
			assertEquals (20, msg2.getRemainingResearchCost ());
		}
		
		verify (serverSpellCalculations, times (0)).randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);
		
		// Finish research
		msgs.getMessages ().clear ();
		calc.progressResearch (player, players, sd, db);
		
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

		assertEquals (isHuman ? 1 : 0, msgs.getMessages ().size ());
		
		if (isHuman)
		{
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
		}

		verify (serverSpellCalculations, times (1)).randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);		// <---
	}
	
	/**
	 * Tests the progressResearch method for a human player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressResearch_Human () throws Exception
	{
		testProgressResearch (true);
	}
	
	/**
	 * Tests the progressResearch method for an AI player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressResearch_AI () throws Exception
	{
		testProgressResearch (false);
	}
	
	/**
	 * Tests the resetCastingSkillRemainingThisTurnToFull method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResetCastingSkillRemainingThisTurnToFull () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Player
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);
		
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();

		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, trans);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Set amount of casting skill
		final MomResourceValue skillImprovement = new MomResourceValue ();
		skillImprovement.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		skillImprovement.setAmountStored (10);
		priv.getResourceValue ().add (skillImprovement);
		
		// Wizard's Fortress
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up test object
		final ResourceValueUtilsImpl resourceValueUtils = new ResourceValueUtilsImpl ();
		resourceValueUtils.setSkillCalculations (new SkillCalculationsImpl ());
		resourceValueUtils.setMemoryBuildingUtils (memoryBuildingUtils);
		
		final ServerResourceCalculationsImpl calc = new ServerResourceCalculationsImpl ();
		calc.setResourceValueUtils (resourceValueUtils);
		
		// Run test
		calc.resetCastingSkillRemainingThisTurnToFull (player, players, db);
		assertEquals (3, trans.getOverlandCastingSkillRemainingThisTurn ());
	}
}