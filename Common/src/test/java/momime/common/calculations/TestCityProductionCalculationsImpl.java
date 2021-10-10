package momime.common.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.GenerateTestData;
import momime.common.database.MapFeatureEx;
import momime.common.database.OverlandMapSize;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.ProductionTypeAndDoubledValue;
import momime.common.database.ProductionTypeEx;
import momime.common.database.RaceEx;
import momime.common.database.RacePopulationTask;
import momime.common.database.RecordNotFoundException;
import momime.common.database.RoundingDirectionID;
import momime.common.database.TaxRate;
import momime.common.database.TileTypeEx;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the calculations in the CityProductionCalculationsImpl class
 */
public final class TestCityProductionCalculationsImpl
{
	/**
	 * Tests the calculateAllCityProductions method
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Test
	public final void testCalculateAllCityProductions () throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Tile types
		final TileTypeEx hillsDef = new TileTypeEx ();
		hillsDef.setDoubleFood (1);
		hillsDef.setProductionBonus (3);
		when (db.findTileType (eq ("TT01"), anyString ())).thenReturn (hillsDef);

		final TileTypeEx riverDef = new TileTypeEx ();
		riverDef.setDoubleFood (4);
		riverDef.setGoldBonus (20);
		when (db.findTileType (eq ("TT02"), anyString ())).thenReturn (riverDef);

		// Map features
		final ProductionTypeAndDoubledValue wildGameFood = new ProductionTypeAndDoubledValue ();
		wildGameFood.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		wildGameFood.setDoubledProductionValue (4);		
		
		final ProductionTypeAndDoubledValue wildGameRations = new ProductionTypeAndDoubledValue ();
		wildGameRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		wildGameRations.setDoubledProductionValue (4);
		
		final MapFeatureEx wildGame = new MapFeatureEx ();
		wildGame.getMapFeatureProduction ().add (wildGameFood);
		wildGame.getMapFeatureProduction ().add (wildGameRations);
		when (db.findMapFeature ("MF01", "addProductionFromMapFeatures")).thenReturn (wildGame);

		final ProductionTypeAndDoubledValue gemsGold = new ProductionTypeAndDoubledValue ();
		gemsGold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		gemsGold.setDoubledProductionValue (10);
		
		final MapFeatureEx gems = new MapFeatureEx ();
		gems.setRaceMineralMultiplerApplies (true);
		gems.getMapFeatureProduction ().add (gemsGold);
		when (db.findMapFeature ("MF02", "addProductionFromMapFeatures")).thenReturn (gems);

		final ProductionTypeAndDoubledValue adamantiumMagicPower = new ProductionTypeAndDoubledValue ();
		adamantiumMagicPower.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		adamantiumMagicPower.setDoubledProductionValue (4);
		
		final MapFeatureEx adamantium = new MapFeatureEx ();
		adamantium.setRaceMineralMultiplerApplies (true);
		adamantium.getMapFeatureProduction ().add (adamantiumMagicPower);
		when (db.findMapFeature ("MF03", "addProductionFromMapFeatures")).thenReturn (adamantium);		

		// Production types
		final ProductionTypeEx rationProduction = new ProductionTypeEx ();
		rationProduction.setRoundingDirectionID (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);
		rationProduction.setDifficultyLevelMultiplierApplies (true);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, "halveAddPercentageBonusAndCapProduction")).thenReturn (rationProduction);

		final ProductionTypeEx foodProduction = new ProductionTypeEx ();
		foodProduction.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, "halveAddPercentageBonusAndCapProduction")).thenReturn (foodProduction);

		final ProductionTypeEx productionProduction = new ProductionTypeEx ();
		productionProduction.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		productionProduction.setDifficultyLevelMultiplierApplies (true);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, "halveAddPercentageBonusAndCapProduction")).thenReturn (productionProduction);

		final ProductionTypeEx goldProduction = new ProductionTypeEx ();
		goldProduction.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		goldProduction.setDifficultyLevelMultiplierApplies (true);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, "halveAddPercentageBonusAndCapProduction")).thenReturn (goldProduction);

		final ProductionTypeEx magicPowerProduction = new ProductionTypeEx ();
		magicPowerProduction.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		magicPowerProduction.setDifficultyLevelMultiplierApplies (true);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, "halveAddPercentageBonusAndCapProduction")).thenReturn (magicPowerProduction);
		
		final ProductionTypeEx spellResearchProduction = new ProductionTypeEx ();
		spellResearchProduction.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "halveAddPercentageBonusAndCapProduction")).thenReturn (spellResearchProduction);
		
		// Standard race
		final ProductionTypeAndDoubledValue raceFarmerRations = new ProductionTypeAndDoubledValue ();
		raceFarmerRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		raceFarmerRations.setDoubledProductionValue (4);

		final ProductionTypeAndDoubledValue raceFarmerProduction = new ProductionTypeAndDoubledValue ();
		raceFarmerProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		raceFarmerProduction.setDoubledProductionValue (1);
		
		final RacePopulationTask raceFarmers = new RacePopulationTask ();
		raceFarmers.getRacePopulationTaskProduction ().add (raceFarmerRations);
		raceFarmers.getRacePopulationTaskProduction ().add (raceFarmerProduction);
		raceFarmers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		
		final ProductionTypeAndDoubledValue raceWorkerProduction = new ProductionTypeAndDoubledValue ();
		raceWorkerProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		raceWorkerProduction.setDoubledProductionValue (4);
		
		final RacePopulationTask raceWorkers = new RacePopulationTask ();
		raceWorkers.getRacePopulationTaskProduction ().add (raceWorkerProduction);
		raceWorkers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER);
		
		final RaceEx raceDef = new RaceEx ();
		raceDef.setMineralBonusMultiplier (1);
		raceDef.getRacePopulationTask ().add (raceFarmers);
		raceDef.getRacePopulationTask ().add (raceWorkers);
		when (db.findRace ("RC01", "calculateAllCityProductions")).thenReturn (raceDef);
		
		// Dwarves
		final ProductionTypeAndDoubledValue dwarvesFarmerRations = new ProductionTypeAndDoubledValue ();
		dwarvesFarmerRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		dwarvesFarmerRations.setDoubledProductionValue (4);

		final ProductionTypeAndDoubledValue dwarvesFarmerProduction = new ProductionTypeAndDoubledValue ();
		dwarvesFarmerProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		dwarvesFarmerProduction.setDoubledProductionValue (1);
		
		final RacePopulationTask dwarvesFarmers = new RacePopulationTask ();
		dwarvesFarmers.getRacePopulationTaskProduction ().add (dwarvesFarmerRations);
		dwarvesFarmers.getRacePopulationTaskProduction ().add (dwarvesFarmerProduction);
		dwarvesFarmers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		
		final ProductionTypeAndDoubledValue dwarvesWorkerProduction = new ProductionTypeAndDoubledValue ();
		dwarvesWorkerProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		dwarvesWorkerProduction.setDoubledProductionValue (6);
		
		final RacePopulationTask dwarvesWorkers = new RacePopulationTask ();
		dwarvesWorkers.getRacePopulationTaskProduction ().add (dwarvesWorkerProduction);
		dwarvesWorkers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER);
		
		final RaceEx dwarvesDef = new RaceEx ();
		dwarvesDef.setMineralBonusMultiplier (2);
		dwarvesDef.getRacePopulationTask ().add (dwarvesFarmers);
		dwarvesDef.getRacePopulationTask ().add (dwarvesWorkers);
		when (db.findRace ("RC02", "calculateAllCityProductions")).thenReturn (dwarvesDef);
		
		// High elves
		final ProductionTypeAndDoubledValue highElfFarmerRations = new ProductionTypeAndDoubledValue ();
		highElfFarmerRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		highElfFarmerRations.setDoubledProductionValue (4);

		final ProductionTypeAndDoubledValue highElfFarmerProduction = new ProductionTypeAndDoubledValue ();
		highElfFarmerProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		highElfFarmerProduction.setDoubledProductionValue (1);

		final ProductionTypeAndDoubledValue highElfFarmerMagicPower = new ProductionTypeAndDoubledValue ();
		highElfFarmerMagicPower.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		highElfFarmerMagicPower.setDoubledProductionValue (1);
		
		final RacePopulationTask highElfFarmers = new RacePopulationTask ();
		highElfFarmers.getRacePopulationTaskProduction ().add (highElfFarmerRations);
		highElfFarmers.getRacePopulationTaskProduction ().add (highElfFarmerProduction);
		highElfFarmers.getRacePopulationTaskProduction ().add (highElfFarmerMagicPower);
		highElfFarmers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		
		final ProductionTypeAndDoubledValue highElfWorkerProduction = new ProductionTypeAndDoubledValue ();
		highElfWorkerProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		highElfWorkerProduction.setDoubledProductionValue (4);

		final ProductionTypeAndDoubledValue highElfWorkerMagicPower = new ProductionTypeAndDoubledValue ();
		highElfWorkerMagicPower.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		highElfWorkerMagicPower.setDoubledProductionValue (1);
		
		final RacePopulationTask highElfWorkers = new RacePopulationTask ();
		highElfWorkers.getRacePopulationTaskProduction ().add (highElfWorkerProduction);
		highElfWorkers.getRacePopulationTaskProduction ().add (highElfWorkerMagicPower);
		highElfWorkers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER);

		final ProductionTypeAndDoubledValue highElfRebelMagicPower = new ProductionTypeAndDoubledValue ();
		highElfRebelMagicPower.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		highElfRebelMagicPower.setDoubledProductionValue (1);
		
		final RacePopulationTask highElfRebels = new RacePopulationTask ();
		highElfRebels.getRacePopulationTaskProduction ().add (highElfRebelMagicPower);
		highElfRebels.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_REBEL);
		
		final RaceEx highElfDef = new RaceEx ();
		highElfDef.setMineralBonusMultiplier (1);
		highElfDef.getRacePopulationTask ().add (highElfFarmers);
		highElfDef.getRacePopulationTask ().add (highElfWorkers);
		highElfDef.getRacePopulationTask ().add (highElfRebels);
		when (db.findRace ("RC03", "calculateAllCityProductions")).thenReturn (highElfDef);
		
		// Tax rate
		final TaxRate taxRate = new TaxRate ();
		taxRate.setDoubleTaxGold (4);
		when (db.findTaxRate ("TR01", "calculateAllCityProductions")).thenReturn (taxRate);
		
		// Buildings
		final Building fortressDef = new Building ();
		fortressDef.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);

		final BuildingPopulationProductionModifier sagesGuildResearch = new BuildingPopulationProductionModifier ();
		sagesGuildResearch.setDoubleAmount (6);
		sagesGuildResearch.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);

		final BuildingPopulationProductionModifier sagesGuildUpkeep = new BuildingPopulationProductionModifier ();
		sagesGuildUpkeep.setDoubleAmount (-4);
		sagesGuildUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		
		final Building sagesGuildDef = new Building ();
		sagesGuildDef.setBuildingID ("BL01");
		sagesGuildDef.getBuildingPopulationProductionModifier ().add (sagesGuildResearch);
		sagesGuildDef.getBuildingPopulationProductionModifier ().add (sagesGuildUpkeep);

		final BuildingPopulationProductionModifier sawmillProduction = new BuildingPopulationProductionModifier ();
		sawmillProduction.setPercentageBonus (25);
		sawmillProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);

		final BuildingPopulationProductionModifier sawmillUpkeep = new BuildingPopulationProductionModifier ();
		sawmillUpkeep.setDoubleAmount (-4);
		sawmillUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		
		final Building sawmillDef = new Building ();
		sawmillDef.getBuildingPopulationProductionModifier ().add (sawmillProduction);
		sawmillDef.getBuildingPopulationProductionModifier ().add (sawmillUpkeep);
		sawmillDef.setBuildingID ("BL02");

		final BuildingPopulationProductionModifier minersGuildProduction = new BuildingPopulationProductionModifier ();
		minersGuildProduction.setPercentageBonus (50);
		minersGuildProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		
		final BuildingPopulationProductionModifier minersGuildMapFeatureBonus = new BuildingPopulationProductionModifier ();
		minersGuildMapFeatureBonus.setPercentageBonus (50);
		minersGuildMapFeatureBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER);
		
		final BuildingPopulationProductionModifier minersGuildUpkeep = new BuildingPopulationProductionModifier ();
		minersGuildUpkeep.setDoubleAmount (-6);
		minersGuildUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		
		final Building minersGuildDef = new Building ();
		minersGuildDef.getBuildingPopulationProductionModifier ().add (minersGuildProduction);
		minersGuildDef.getBuildingPopulationProductionModifier ().add (minersGuildMapFeatureBonus);
		minersGuildDef.getBuildingPopulationProductionModifier ().add (minersGuildUpkeep);
		minersGuildDef.setBuildingID ("BL03");
		
		final List<Building> buildingDefs = new ArrayList<Building> ();
		buildingDefs.add (fortressDef);
		buildingDefs.add (sagesGuildDef);
		buildingDefs.add (sawmillDef);
		buildingDefs.add (minersGuildDef);
		
		doReturn (buildingDefs).when (db).getBuilding ();
		
		// Planes
		final ProductionTypeAndDoubledValue fortressPlaneProduction = new ProductionTypeAndDoubledValue ();
		fortressPlaneProduction.setDoubledProductionValue (10);
		fortressPlaneProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		
		final Plane myrror = new Plane ();
		myrror.getFortressPlaneProduction ().add (fortressPlaneProduction);
		when (db.findPlane (1, "calculateAllCityProductions")).thenReturn (myrror);
		
		// Pick types
		final ProductionTypeAndDoubledValue fortressPickTypeProduction = new ProductionTypeAndDoubledValue ();
		fortressPickTypeProduction.setDoubledProductionValue (2);
		fortressPickTypeProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		
		final PickType books = new PickType ();
		books.setPickTypeID ("B");
		books.getFortressPickTypeProduction ().add (fortressPickTypeProduction);
		
		final List<PickType> pickTypes = new ArrayList<PickType> ();
		pickTypes.add (books);
		
		doReturn (pickTypes).when (db).getPickType ();
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 1);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Add some tile types that grant food, 3 hills and 5 rivers, (3*1) + (4*5) = 23
		// Note we add hills in the NW and NE corners too, outside the city radius, so we prove that this isn't counted
		for (int x = 0; x <= 4; x++)
		{
			final OverlandMapTerrainData hillsTerrain = new OverlandMapTerrainData ();
			hillsTerrain.setTileTypeID ("TT01");
			map.getPlane ().get (1).getRow ().get (0).getCell ().get (x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
			riverTile.setTileTypeID ("TT02");
			map.getPlane ().get (1).getRow ().get (1).getCell ().get (x).setTerrainData (riverTile);
		}

		// Put river right on the city too, to get the gold bonus
		final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
		riverTile.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (2).getCell ().get (2).setTerrainData (riverTile);

		// Add some wild game
		for (int y = 0; y <= 1; y++)
			map.getPlane ().get (1).getRow ().get (y).getCell ().get (2).getTerrainData ().setMapFeatureID ("MF01");

		// Session description
		final OverlandMapSize overlandMapSize = new OverlandMapSize ();
		overlandMapSize.setWidth (sys.getWidth ());
		overlandMapSize.setHeight (sys.getHeight ());
		overlandMapSize.setDepth (sys.getDepth ());
		overlandMapSize.setCoordinateSystemType (sys.getCoordinateSystemType ());
		overlandMapSize.setWrapsLeftToRight (sys.isWrapsLeftToRight ());
		overlandMapSize.setWrapsTopToBottom (sys.isWrapsTopToBottom ());

		final DifficultyLevel dl = new DifficultyLevel ();
		dl.setCityMaxSize (25);
		dl.setAiWizardsProductionRateMultiplier (250);

		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setDifficultyLevel (dl);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		cityData.setOptionalFarmers (2);
		cityData.setNumberOfRebels (2);		// 17 -6 -2 -2 = 7 workers
		map.getPlane ().get (1).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateAllCityProductions")).thenReturn (player);

		// This functions like an integration test for now, until these tests are rewritten
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);

		final CityCalculationsImpl cityCalc = new CityCalculationsImpl ();
		cityCalc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		cityCalc.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// Set up object to test
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);

		final CityProductionCalculationsImpl calc = new CityProductionCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		calc.setCityCalculations (cityCalc);
		
		// So far all we have are the basic production types:
		// a) production from the population, bumped up by the % bonus from terrain
		// b) max city size
		// c) people eating food
		// d) gold from taxes
		final CityProductionBreakdownsEx baseNoPeople = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, false, false, db);
		assertEquals (4, baseNoPeople.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, baseNoPeople.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (8, baseNoPeople.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (4, baseNoPeople.getProductionType ().get (0).getBaseProductionAmount ());				// 2 x2 from wild game = 4
		assertEquals (0, baseNoPeople.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (4, baseNoPeople.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (0).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, baseNoPeople.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (0, baseNoPeople.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (1).getBaseProductionAmount ());
		assertEquals (9, baseNoPeople.getProductionType ().get (1).getPercentageBonus ());						// 3 hills giving 3% each
		assertEquals (0, baseNoPeople.getProductionType ().get (1).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, baseNoPeople.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (0, baseNoPeople.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (2).getBaseProductionAmount ());
		assertEquals (20, baseNoPeople.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (0, baseNoPeople.getProductionType ().get (2).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, baseNoPeople.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (35, baseNoPeople.getProductionType ().get (3).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, baseNoPeople.getProductionType ().get (3).getBaseProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (18, baseNoPeople.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (3).getConsumptionAmount ());

		final CityProductionBreakdownsEx baseWithPeople = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (4, baseWithPeople.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, baseWithPeople.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, baseWithPeople.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, baseWithPeople.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, baseWithPeople.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, baseWithPeople.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, baseWithPeople.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, baseWithPeople.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, baseWithPeople.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, baseWithPeople.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (9, baseWithPeople.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each
		assertEquals (19, baseWithPeople.getProductionType ().get (1).getModifiedProductionAmount ());	// 18 * 1.09 = 19.62
		assertEquals (0, baseWithPeople.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, baseWithPeople.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (60, baseWithPeople.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (30, baseWithPeople.getProductionType ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, baseWithPeople.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (36, baseWithPeople.getProductionType ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (0, baseWithPeople.getProductionType ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, baseWithPeople.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (35, baseWithPeople.getProductionType ().get (3).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, baseWithPeople.getProductionType ().get (3).getBaseProductionAmount ());
		assertEquals (0, baseWithPeople.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (18, baseWithPeople.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, baseWithPeople.getProductionType ().get (3).getConsumptionAmount ());

		// Wizard fortress produces +1 mana per book, and +5 for being on myrror
		final PlayerPick lifeBook = new PlayerPick ();
		lifeBook.setPickID ("MB01");
		lifeBook.setOriginalQuantity (8);
		lifeBook.setQuantity (9);
		ppk.getPick ().add (lifeBook);

		final PlayerPick summoner = new PlayerPick ();
		summoner.setPickID ("RT01");
		summoner.setOriginalQuantity (1);
		summoner.setQuantity (1);
		ppk.getPick ().add (summoner);

		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (2, 2, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (new MemoryBuilding ());
		when (playerPickUtils.countPicksOfType (ppk.getPick (), "B", true, db)).thenReturn (8);

		final CityProductionBreakdownsEx fortress = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (5, fortress.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, fortress.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, fortress.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, fortress.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, fortress.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, fortress.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, fortress.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, fortress.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, fortress.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, fortress.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (9, fortress.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each
		assertEquals (19, fortress.getProductionType ().get (1).getModifiedProductionAmount ());	// 18 * 1.09 = 19.62
		assertEquals (0, fortress.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, fortress.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (60, fortress.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (30, fortress.getProductionType ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, fortress.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (36, fortress.getProductionType ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (0, fortress.getProductionType ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, fortress.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (26, fortress.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (13, fortress.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books + 5 for being on myrror = 13
		assertEquals (0, fortress.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (13, fortress.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, fortress.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, fortress.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (35, fortress.getProductionType ().get (4).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, fortress.getProductionType ().get (4).getBaseProductionAmount ());
		assertEquals (0, fortress.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (18, fortress.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, fortress.getProductionType ().get (4).getConsumptionAmount ());

		// Add some buildings that give production (both regular like sages guild, and percentage like sawmill), and consumption
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (2, 2, 1), "BL01")).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (2, 2, 1), "BL02")).thenReturn (new MemoryBuilding ());

		final CityProductionBreakdownsEx sawmill = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (6, sawmill.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, sawmill.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, sawmill.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, sawmill.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, sawmill.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, sawmill.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, sawmill.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, sawmill.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, sawmill.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, sawmill.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (34, sawmill.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill
		assertEquals (24, sawmill.getProductionType ().get (1).getModifiedProductionAmount ());	// 18 * 1.34 = 24.12
		assertEquals (0, sawmill.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, sawmill.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (60, sawmill.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (30, sawmill.getProductionType ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, sawmill.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (36, sawmill.getProductionType ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (4, sawmill.getProductionType ().get (2).getConsumptionAmount ());				// 2 buildings costing 2 gold each
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, sawmill.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (26, sawmill.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (13, sawmill.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books + 5 for being on myrror = 13
		assertEquals (0, sawmill.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (13, sawmill.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, sawmill.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, sawmill.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, sawmill.getProductionType ().get (4).getBaseProductionAmount ());			// 3 from sages' guild
		assertEquals (0, sawmill.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, sawmill.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, sawmill.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, sawmill.getProductionType ().get (5).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, sawmill.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, sawmill.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, sawmill.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getProductionType ().get (5).getConsumptionAmount ());

		// Add some map features, note there's already 2 wild game been added above
		map.getPlane ().get (1).getRow ().get (0).getCell ().get (3).getTerrainData ().setMapFeatureID ("MF02");
		map.getPlane ().get (1).getRow ().get (1).getCell ().get (3).getTerrainData ().setMapFeatureID ("MF03");

		final CityProductionBreakdownsEx minerals = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (6, minerals.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, minerals.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, minerals.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, minerals.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, minerals.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, minerals.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, minerals.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, minerals.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, minerals.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, minerals.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (34, minerals.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from minerals
		assertEquals (24, minerals.getProductionType ().get (1).getModifiedProductionAmount ());		// 18 * 1.34 = 24.12
		assertEquals (0, minerals.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, minerals.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (70, minerals.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (35, minerals.getProductionType ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +5 from gems = 35
		assertEquals (20, minerals.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (42, minerals.getProductionType ().get (2).getModifiedProductionAmount ());		// 35 * 1.2 = 42
		assertEquals (4, minerals.getProductionType ().get (2).getConsumptionAmount ());					// 2 buildings costing 2 gold each
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, minerals.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (30, minerals.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (15, minerals.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +2 from adamantium = 15
		assertEquals (0, minerals.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (15, minerals.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, minerals.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, minerals.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, minerals.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, minerals.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, minerals.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, minerals.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, minerals.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, minerals.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, minerals.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, minerals.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, minerals.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, minerals.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, minerals.getProductionType ().get (5).getConsumptionAmount ());

		// Miners' guild boosting bonuses from map features
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (2, 2, 1), "BL03")).thenReturn (new MemoryBuilding ());
		
		final CityProductionBreakdownsEx minersGuild = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (7, minersGuild.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, minersGuild.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, minersGuild.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, minersGuild.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, minersGuild.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, minersGuild.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, minersGuild.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, minersGuild.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, minersGuild.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, minersGuild.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (84, minersGuild.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (33, minersGuild.getProductionType ().get (1).getModifiedProductionAmount ());		// 18 * 1.84 = 33.12
		assertEquals (0, minersGuild.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, minersGuild.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (75, minersGuild.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (37, minersGuild.getProductionType ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +7.5 from gems = 37.5
		assertEquals (20, minersGuild.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (44, minersGuild.getProductionType ().get (2).getModifiedProductionAmount ());		// 37 * 1.2 = 44.4
		assertEquals (7, minersGuild.getProductionType ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, minersGuild.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (32, minersGuild.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (16, minersGuild.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16
		assertEquals (0, minersGuild.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (16, minersGuild.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, minersGuild.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, minersGuild.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, minersGuild.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, minersGuild.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, minersGuild.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, minersGuild.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, minersGuild.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, minersGuild.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, minersGuild.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, minersGuild.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getProductionType ().get (5).getConsumptionAmount ());

		// Dwarves double bonuses from map features, and also workers produce 3 production instead of 2
		cityData.setCityRaceID ("RC02");

		final CityProductionBreakdownsEx dwarves = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (7, dwarves.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, dwarves.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, dwarves.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, dwarves.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, dwarves.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, dwarves.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, dwarves.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, dwarves.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (50, dwarves.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (25, dwarves.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 3) = 25
		assertEquals (84, dwarves.getProductionType ().get (1).getPercentageBonus ());						// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (46, dwarves.getProductionType ().get (1).getModifiedProductionAmount ());		// 25 * 1.84 = 46
		assertEquals (0, dwarves.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, dwarves.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (90, dwarves.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (45, dwarves.getProductionType ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +15 from gems = 45
		assertEquals (20, dwarves.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (54, dwarves.getProductionType ().get (2).getModifiedProductionAmount ());		// 45 * 1.2 = 54
		assertEquals (7, dwarves.getProductionType ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, dwarves.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (38, dwarves.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (19, dwarves.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +6 from adamantium = 19
		assertEquals (0, dwarves.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (19, dwarves.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, dwarves.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, dwarves.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, dwarves.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, dwarves.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, dwarves.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, dwarves.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, dwarves.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, dwarves.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, dwarves.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, dwarves.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getProductionType ().get (5).getConsumptionAmount ());

		// High elf rebels produce mana too
		cityData.setCityRaceID ("RC03");

		final CityProductionBreakdownsEx highElves = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (7, highElves.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, highElves.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, highElves.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, highElves.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, highElves.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, highElves.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (100, highElves.getProductionType ().get (0).getDifficultyLevelMultiplier ());
		assertEquals (20, highElves.getProductionType ().get (0).getTotalAdjustedForDifficultyLevel ());
		assertEquals (17, highElves.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, highElves.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, highElves.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, highElves.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (84, highElves.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (33, highElves.getProductionType ().get (1).getModifiedProductionAmount ());		// 18 * 1.84 = 33.12
		assertEquals (100, highElves.getProductionType ().get (1).getDifficultyLevelMultiplier ());
		assertEquals (33, highElves.getProductionType ().get (1).getTotalAdjustedForDifficultyLevel ());
		assertEquals (0, highElves.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, highElves.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (75, highElves.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (37, highElves.getProductionType ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +7.5 from gems = 37.5
		assertEquals (20, highElves.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (44, highElves.getProductionType ().get (2).getModifiedProductionAmount ());		// 37 * 1.2 = 44.4
		assertEquals (100, highElves.getProductionType ().get (2).getDifficultyLevelMultiplier ());
		assertEquals (44, highElves.getProductionType ().get (2).getTotalAdjustedForDifficultyLevel ());
		assertEquals (7, highElves.getProductionType ().get (2).getConsumptionAmount ());				// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, highElves.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (49, highElves.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (24, highElves.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +17 from pop = 49
		assertEquals (0, highElves.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (24, highElves.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (100, highElves.getProductionType ().get (3).getDifficultyLevelMultiplier ());
		assertEquals (24, highElves.getProductionType ().get (3).getTotalAdjustedForDifficultyLevel ());
		assertEquals (0, highElves.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, highElves.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, highElves.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, highElves.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, highElves.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, highElves.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (100, highElves.getProductionType ().get (4).getDifficultyLevelMultiplier ());
		assertEquals (3, highElves.getProductionType ().get (4).getTotalAdjustedForDifficultyLevel ());
		assertEquals (0, highElves.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, highElves.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, highElves.getProductionType ().get (5).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, highElves.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, highElves.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, highElves.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (100, highElves.getProductionType ().get (5).getDifficultyLevelMultiplier ());
		assertEquals (18, highElves.getProductionType ().get (5).getTotalAdjustedForDifficultyLevel ());
		assertEquals (0, highElves.getProductionType ().get (5).getConsumptionAmount ());

		// Test bonus given to AI players
		pd.setHuman (false);

		final CityProductionBreakdownsEx aiHighElves = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (7, aiHighElves.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, aiHighElves.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, aiHighElves.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, aiHighElves.getProductionType ().get (0).getBaseProductionAmount ());				// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, aiHighElves.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, aiHighElves.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (250, aiHighElves.getProductionType ().get (0).getDifficultyLevelMultiplier ());
		assertEquals (50, aiHighElves.getProductionType ().get (0).getTotalAdjustedForDifficultyLevel ());
		assertEquals (17, aiHighElves.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, aiHighElves.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, aiHighElves.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, aiHighElves.getProductionType ().get (1).getBaseProductionAmount ());				// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (84, aiHighElves.getProductionType ().get (1).getPercentageBonus ());						// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (33, aiHighElves.getProductionType ().get (1).getModifiedProductionAmount ());		// 18 * 1.84 = 33.12
		assertEquals (250, aiHighElves.getProductionType ().get (1).getDifficultyLevelMultiplier ());
		assertEquals (82, aiHighElves.getProductionType ().get (1).getTotalAdjustedForDifficultyLevel ());
		assertEquals (0, aiHighElves.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, aiHighElves.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (75, aiHighElves.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (37, aiHighElves.getProductionType ().get (2).getBaseProductionAmount ());				// (15 non-rebels x2) +7.5 from gems = 37.5
		assertEquals (20, aiHighElves.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (44, aiHighElves.getProductionType ().get (2).getModifiedProductionAmount ());		// 37 * 1.2 = 44.4
		assertEquals (250, aiHighElves.getProductionType ().get (2).getDifficultyLevelMultiplier ());
		assertEquals (110, aiHighElves.getProductionType ().get (2).getTotalAdjustedForDifficultyLevel ());
		assertEquals (7, aiHighElves.getProductionType ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, aiHighElves.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (49, aiHighElves.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (24, aiHighElves.getProductionType ().get (3).getBaseProductionAmount ());				// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +17 from pop = 49
		assertEquals (0, aiHighElves.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (24, aiHighElves.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (250, aiHighElves.getProductionType ().get (3).getDifficultyLevelMultiplier ());
		assertEquals (60, aiHighElves.getProductionType ().get (3).getTotalAdjustedForDifficultyLevel ());
		assertEquals (0, aiHighElves.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, aiHighElves.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, aiHighElves.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, aiHighElves.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, aiHighElves.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, aiHighElves.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (100, aiHighElves.getProductionType ().get (4).getDifficultyLevelMultiplier ());
		assertEquals (3, aiHighElves.getProductionType ().get (4).getTotalAdjustedForDifficultyLevel ());
		assertEquals (0, aiHighElves.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, aiHighElves.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, aiHighElves.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, aiHighElves.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, aiHighElves.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, aiHighElves.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (100, aiHighElves.getProductionType ().get (5).getDifficultyLevelMultiplier ());
		assertEquals (18, aiHighElves.getProductionType ().get (5).getTotalAdjustedForDifficultyLevel ());
		assertEquals (0, aiHighElves.getProductionType ().get (5).getConsumptionAmount ());
		
		pd.setHuman (true);
		
		// Shrink city to size 6 - gold % bonus is then capped at 6 x3 = 18%
		cityData.setCityPopulation (6900);
		cityData.setMinimumFarmers (1);
		cityData.setOptionalFarmers (1);
		cityData.setNumberOfRebels (1);		// 6 -1 -1 -1 = 3 workers

		final CityProductionBreakdownsEx shrunk = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (7, shrunk.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, shrunk.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (16, shrunk.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (8, shrunk.getProductionType ().get (0).getBaseProductionAmount ());				// (2 farmers x2) + (2 x2 from wild game) = 8
		assertEquals (0, shrunk.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (8, shrunk.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (6, shrunk.getProductionType ().get (0).getConsumptionAmount ());				// 6 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, shrunk.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (14, shrunk.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (7, shrunk.getProductionType ().get (1).getBaseProductionAmount ());				// (2 farmers x ½) + (3 workers x 2) = 7
		assertEquals (84, shrunk.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (12, shrunk.getProductionType ().get (1).getModifiedProductionAmount ());		// 7 * 1.84 = 12.88
		assertEquals (0, shrunk.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, shrunk.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (35, shrunk.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (17, shrunk.getProductionType ().get (2).getBaseProductionAmount ());			// (5 non-rebels x2) +7.5 from gems = 17.5
		assertEquals (18, shrunk.getProductionType ().get (2).getPercentageBonus ());					// Capped due to city size
		assertEquals (20, shrunk.getProductionType ().get (2).getModifiedProductionAmount ());		// 17 * 1.18 = 20.06
		assertEquals (7, shrunk.getProductionType ().get (2).getConsumptionAmount ());				// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, shrunk.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (38, shrunk.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (19, shrunk.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +6 from pop = 38
		assertEquals (0, shrunk.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (19, shrunk.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, shrunk.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, shrunk.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, shrunk.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, shrunk.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, shrunk.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, shrunk.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, shrunk.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, shrunk.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, shrunk.getProductionType ().get (5).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, shrunk.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, shrunk.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, shrunk.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, shrunk.getProductionType ().get (5).getConsumptionAmount ());

		// Cap max city size at 25
		for (int y = 0; y <= 4; y++)
		{
			final MemoryGridCell mc = map.getPlane ().get (1).getRow ().get (y).getCell ().get (1);
			if (mc.getTerrainData () == null)
				mc.setTerrainData (new OverlandMapTerrainData ());

			mc.getTerrainData ().setMapFeatureID ("MF01");
		}

		final CityProductionBreakdownsEx maxSize = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (7, maxSize.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, maxSize.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (36, maxSize.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (18, maxSize.getProductionType ().get (0).getBaseProductionAmount ());				// (2 farmers x2) + (2 x7 from wild game) = 18
		assertEquals (0, maxSize.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (18, maxSize.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (6, maxSize.getProductionType ().get (0).getConsumptionAmount ());					// 6 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, maxSize.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (14, maxSize.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (7, maxSize.getProductionType ().get (1).getBaseProductionAmount ());				// (2 farmers x ½) + (3 workers x 2) = 7
		assertEquals (84, maxSize.getProductionType ().get (1).getPercentageBonus ());						// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (12, maxSize.getProductionType ().get (1).getModifiedProductionAmount ());		// 7 * 1.84 = 12.88
		assertEquals (0, maxSize.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, maxSize.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (35, maxSize.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (17, maxSize.getProductionType ().get (2).getBaseProductionAmount ());				// (5 non-rebels x2) +7.5 from gems = 17.5
		assertEquals (18, maxSize.getProductionType ().get (2).getPercentageBonus ());						// Capped due to city size
		assertEquals (20, maxSize.getProductionType ().get (2).getModifiedProductionAmount ());		// 17 * 1.18 = 20.06
		assertEquals (7, maxSize.getProductionType ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, maxSize.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (38, maxSize.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (19, maxSize.getProductionType ().get (3).getBaseProductionAmount ());				// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +6 from pop = 38
		assertEquals (0, maxSize.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (19, maxSize.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, maxSize.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, maxSize.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, maxSize.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, maxSize.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, maxSize.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, maxSize.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (55, maxSize.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x7 from wild game) = 35
		assertEquals (28, maxSize.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (28, maxSize.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (25, maxSize.getProductionType ().get (5).getCappedProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (5).getConsumptionAmount ());
		
		// Test trade goods setting
		assertNull (maxSize.getProductionType ().get (2).getConvertFromProductionTypeID ());
		assertEquals (0, maxSize.getProductionType ().get (2).getConvertFromProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (2).getConvertToProductionAmount ());
		
		cityData.setCurrentlyConstructingBuildingID (CommonDatabaseConstants.BUILDING_TRADE_GOODS);
		final CityProductionBreakdownsEx tradeGoods = calc.calculateAllCityProductions
			(players, map, buildings, spells, cityLocation, "TR01", sd, true, false, db);
		assertEquals (7, tradeGoods.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, tradeGoods.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (36, tradeGoods.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (18, tradeGoods.getProductionType ().get (0).getBaseProductionAmount ());			// (2 farmers x2) + (2 x7 from wild game) = 18
		assertEquals (0, tradeGoods.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (18, tradeGoods.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (6, tradeGoods.getProductionType ().get (0).getConsumptionAmount ());					// 6 population eating
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, tradeGoods.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (14, tradeGoods.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (7, tradeGoods.getProductionType ().get (1).getBaseProductionAmount ());				// (2 farmers x ½) + (3 workers x 2) = 7
		assertEquals (84, tradeGoods.getProductionType ().get (1).getPercentageBonus ());						// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (12, tradeGoods.getProductionType ().get (1).getModifiedProductionAmount ());		// 7 * 1.84 = 12.88
		assertEquals (0, tradeGoods.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, tradeGoods.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (35, tradeGoods.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (17, tradeGoods.getProductionType ().get (2).getBaseProductionAmount ());			// (5 non-rebels x2) +7.5 from gems = 17.5
		assertEquals (18, tradeGoods.getProductionType ().get (2).getPercentageBonus ());						// Capped due to city size
		assertEquals (20, tradeGoods.getProductionType ().get (2).getModifiedProductionAmount ());		// 17 * 1.18 = 20.06
		assertEquals (7, tradeGoods.getProductionType ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, tradeGoods.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (38, tradeGoods.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (19, tradeGoods.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +6 from pop = 38
		assertEquals (0, tradeGoods.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (19, tradeGoods.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, tradeGoods.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, tradeGoods.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, tradeGoods.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, tradeGoods.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, tradeGoods.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, tradeGoods.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, tradeGoods.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, tradeGoods.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (55, tradeGoods.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x7 from wild game) = 35
		assertEquals (28, tradeGoods.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, tradeGoods.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (28, tradeGoods.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (25, tradeGoods.getProductionType ().get (5).getCappedProductionAmount ());
		assertEquals (0, tradeGoods.getProductionType ().get (5).getConsumptionAmount ());

		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, tradeGoods.getProductionType ().get (2).getConvertFromProductionTypeID ());
		assertEquals (12, tradeGoods.getProductionType ().get (2).getConvertFromProductionAmount ());
		assertEquals (6, tradeGoods.getProductionType ().get (2).getConvertToProductionAmount ());
	}
	
	/**
	 * Tests the calculateAllCityProductions method to calculate scores for a potential city location
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateAllCityProductions_Potential () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx foodTile = new TileTypeEx ();
		foodTile.setDoubleFood (3);
		when (db.findTileType (eq ("TT01"), anyString ())).thenReturn (foodTile);
		
		final TileTypeEx bothTile = new TileTypeEx ();
		bothTile.setDoubleFood (1);
		bothTile.setProductionBonus (1);
		bothTile.setGoldBonus (30);
		when (db.findTileType (eq ("TT02"), anyString ())).thenReturn (bothTile);
		
		final TileTypeEx productionTile = new TileTypeEx ();
		productionTile.setProductionBonus (3);
		when (db.findTileType (eq ("TT03"), anyString ())).thenReturn (productionTile);
		
		final ProductionTypeEx foodProduction = new ProductionTypeEx ();
		foodProduction.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD), anyString ())).thenReturn (foodProduction);
		
		final BuildingPopulationProductionModifier granaryFood = new BuildingPopulationProductionModifier ();
		granaryFood.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		granaryFood.setDoubleAmount (4);
		
		final Building granary = new Building ();
		granary.setBuildingID ("BL01");
		granary.getBuildingPopulationProductionModifier ().add (granaryFood);
		
		final BuildingPopulationProductionModifier farmersMarketFood = new BuildingPopulationProductionModifier ();
		farmersMarketFood.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		farmersMarketFood.setDoubleAmount (6);
		
		final Building farmersMarket = new Building ();
		farmersMarket.setBuildingID ("BL02");
		farmersMarket.getBuildingPopulationProductionModifier ().add (farmersMarketFood);
		
		final List<Building> buildings = new ArrayList<Building> ();
		buildings.add (granary);
		buildings.add (farmersMarket);
		doReturn (buildings).when (db).getBuilding ();
		
		// Session description
		final OverlandMapSize overlandMapSize = GenerateTestData.createOverlandMapSize ();
		
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setCityMaxSize (25);
		difficultyLevel.setAiWizardsProductionRateMultiplier (250);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setDifficultyLevel (difficultyLevel);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		// Buildings
		final List<MemoryBuilding> memoryBuildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Put 3 of the 'food' tiles, 5 of the 'both' files and 3 of the 'production' tiles
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (overlandMapSize);
		
		for (int x = 0; x <= 4; x++)
		{
			final OverlandMapTerrainData foodTerrain = new OverlandMapTerrainData ();
			foodTerrain.setTileTypeID ("TT01");
			map.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (foodTerrain);

			final OverlandMapTerrainData bothTerrain = new OverlandMapTerrainData ();
			bothTerrain.setTileTypeID ("TT02");
			map.getPlane ().get (0).getRow ().get (2).getCell ().get (x).setTerrainData (bothTerrain);

			final OverlandMapTerrainData productionTerrain = new OverlandMapTerrainData ();
			productionTerrain.setTileTypeID ("TT03");
			map.getPlane ().get (0).getRow ().get (4).getCell ().get (x).setTerrainData (productionTerrain);
		}
		
		// This functions like an integration test for now, until these tests are rewritten
		final CityCalculationsImpl cityCalc = new CityCalculationsImpl ();
		cityCalc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Set up object to test
		final CityProductionCalculationsImpl calc = new CityProductionCalculationsImpl ();
		calc.setCityCalculations (cityCalc);
		
		// At the moment there's space for 7,000 people, so the gold trade bonus from the tile type is 30 so this is less than the 36 cap
		final CityProductionBreakdownsEx prod1 = calc.calculateAllCityProductions (players, map, memoryBuildings, spells,
			new MapCoordinates3DEx (2, 2, 0), "TR01", sd, false, true, db);
		assertEquals (3, prod1.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, prod1.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (14, prod1.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, prod1.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (30, prod1.getProductionType ().get (1).getTradePercentageBonusCapped ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, prod1.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (12, prod1.getProductionType ().get (2).getCappedProductionAmount ());		// (14/2) +5 from granary and farmers' market
		
		// Increase the gold trade bonus to 40, so it gets capped at 36
		bothTile.setGoldBonus (40);

		final CityProductionBreakdownsEx prod2 = calc.calculateAllCityProductions (players, map, memoryBuildings, spells,
			new MapCoordinates3DEx (2, 2, 0), "TR01", sd, false, true, db);
		assertEquals (3, prod2.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, prod2.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (14, prod2.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, prod2.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, prod2.getProductionType ().get (1).getTradePercentageBonusCapped ());
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, prod2.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (12, prod2.getProductionType ().get (2).getCappedProductionAmount ());		// (14/2) +5 from granary and farmers' market
	}
}