package momime.common.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.areas.operations.BooleanMapAreaOperations2DImpl;
import com.ndg.map.areas.operations.MapAreaOperations2DImpl;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.BuildingRequiresTileType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.GenerateTestData;
import momime.common.database.MapFeatureEx;
import momime.common.database.OverlandMapSize;
import momime.common.database.Pick;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.ProductionTypeAndDoubledValue;
import momime.common.database.ProductionTypeEx;
import momime.common.database.RaceEx;
import momime.common.database.RacePopulationTask;
import momime.common.database.RaceUnrest;
import momime.common.database.RecordNotFoundException;
import momime.common.database.RoundingDirectionID;
import momime.common.database.TaxRate;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitEx;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownDying;
import momime.common.internal.CityGrowthRateBreakdownGrowing;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the calculations in the CityCalculationsImpl class
 */
public final class TestCityCalculationsImpl
{
	/**
	 * Tests the listCityProductionPercentageBonusesFromTerrainTiles method
	 * @throws RecordNotFoundException If we encounter a tile type that we cannot find in the cache
	 */
	@Test
	public final void testListCityProductionPercentageBonusesFromTerrainTiles () throws RecordNotFoundException
	{
		// Mock database
		final TileTypeEx hillsTileType = new TileTypeEx ();
		hillsTileType.setProductionBonus (3);

		final TileTypeEx mountainsTileType = new TileTypeEx ();
		mountainsTileType.setProductionBonus (5);

		final TileTypeEx riverTileType = new TileTypeEx ();

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "listCityProductionPercentageBonusesFromTerrainTiles")).thenReturn (hillsTileType);
		when (db.findTileType ("TT02", "listCityProductionPercentageBonusesFromTerrainTiles")).thenReturn (mountainsTileType);
		when (db.findTileType ("TT03", "listCityProductionPercentageBonusesFromTerrainTiles")).thenReturn (riverTileType);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Add 3 hills and 5 mountains, (3*3) + (5*5) = 34
		// Note we add hills in the NW and NE corners too, outside the city radius, so we prove that this isn't counted
		// Also adds some rivers, which don't give any production % bonus at all, to prove they don't get included in the breakdown
		for (int x = 0; x <= 4; x++)
		{
			final OverlandMapTerrainData hillsTerrain = new OverlandMapTerrainData ();
			hillsTerrain.setTileTypeID ("TT01");
			map.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData mountainsTile = new OverlandMapTerrainData ();
			mountainsTile.setTileTypeID ("TT02");
			map.getPlane ().get (0).getRow ().get (1).getCell ().get (x).setTerrainData (mountainsTile);

			final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
			riverTile.setTileTypeID ("TT03");
			map.getPlane ().get (0).getRow ().get (2).getCell ().get (x).setTerrainData (riverTile);
		}

		final CityProductionBreakdown breakdown = calc.listCityProductionPercentageBonusesFromTerrainTiles (map, new MapCoordinates3DEx (2, 2, 0), sys, db);
		
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION, breakdown.getProductionTypeID ());
		assertEquals (34, breakdown.getPercentageBonus ());
		assertEquals (2, breakdown.getTileTypeProduction ().size ());
		assertEquals ("TT02", breakdown.getTileTypeProduction ().get (0).getTileTypeID ());
		assertEquals (5, breakdown.getTileTypeProduction ().get (0).getCount ());
		assertEquals (5, breakdown.getTileTypeProduction ().get (0).getPercentageBonusEachTile ());
		assertEquals (25, breakdown.getTileTypeProduction ().get (0).getPercentageBonusAllTiles ());
		assertEquals ("TT01", breakdown.getTileTypeProduction ().get (1).getTileTypeID ());
		assertEquals (3, breakdown.getTileTypeProduction ().get (1).getCount ());
		assertEquals (3, breakdown.getTileTypeProduction ().get (1).getPercentageBonusEachTile ());
		assertEquals (9, breakdown.getTileTypeProduction ().get (1).getPercentageBonusAllTiles ());
	}

	/**
	 * Tests the calculateGoldTradeBonus method
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldTradeBonus () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx centreTileType = new TileTypeEx ();
		centreTileType.setGoldBonus (20);
		when (db.findTileType ("TT01", "calculateGoldTradeBonus")).thenReturn (centreTileType);
		
		final TileTypeEx adjacentTileType = new TileTypeEx ();
		adjacentTileType.setGoldBonus (10);
		when (db.findTileType ("TT02", "calculateGoldTradeBonus")).thenReturn (adjacentTileType);

		final TileTypeEx uninterestingTileType = new TileTypeEx ();
		when (db.findTileType ("TT03", "calculateGoldTradeBonus")).thenReturn (uninterestingTileType);
		
		final RaceEx uninterestingRace = new RaceEx ();
		when (db.findRace ("RC01", "calculateGoldTradeBonus")).thenReturn (uninterestingRace);
		
		final RaceEx nomads = new RaceEx ();
		nomads.setGoldTradeBonus (50);
		when (db.findRace ("RC02", "calculateGoldTradeBonus")).thenReturn (nomads);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Prove that gold bonus from adjacent tile type doesn't count, since adjacent flag isn't set
		// Also prove it doesn't fall over with no city details or race
		final OverlandMapTerrainData adjacentTerrain = new OverlandMapTerrainData ();
		adjacentTerrain.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (adjacentTerrain);
		
		final CityProductionBreakdown breakdown1 = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown1, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		assertEquals (0, breakdown1.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown1.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown1.getTradePercentageBonusFromRace ());
		assertEquals (0, breakdown1.getTradePercentageBonusUncapped ());
		assertEquals (0, breakdown1.getTotalPopulation ());
		assertEquals (0, breakdown1.getTradePercentageBonusCapped ());
		assertEquals (0, breakdown1.getPercentageBonus ());
		
		// Set the adjacent flag, prove it now gets counted
		adjacentTileType.setGoldBonusSurroundingTiles (true);
		
		final CityProductionBreakdown breakdown2 = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown2, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		assertEquals (10, breakdown2.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown2.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown2.getTradePercentageBonusFromRace ());
		assertEquals (10, breakdown2.getTradePercentageBonusUncapped ());
		assertEquals (0, breakdown2.getTotalPopulation ());
		assertEquals (0, breakdown2.getTradePercentageBonusCapped ());
		assertEquals (0, breakdown2.getPercentageBonus ());
		
		// Put the city on an uninteresting tile type, and give it some people
		final OverlandMapTerrainData centreTerrain = new OverlandMapTerrainData ();
		centreTerrain.setTileTypeID ("TT03");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (centreTerrain);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (3456);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final CityProductionBreakdown breakdown3 = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown3, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		assertEquals (10, breakdown3.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown3.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown3.getTradePercentageBonusFromRace ());
		assertEquals (10, breakdown3.getTradePercentageBonusUncapped ());
		assertEquals (3, breakdown3.getTotalPopulation ());
		assertEquals (9, breakdown3.getTradePercentageBonusCapped ());
		assertEquals (9, breakdown3.getPercentageBonus ());
		
		// Put the city on a better tile type, but still cap at 3 people
		centreTerrain.setTileTypeID ("TT01");

		final CityProductionBreakdown breakdown4 = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown4, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		assertEquals (20, breakdown4.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown4.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown4.getTradePercentageBonusFromRace ());
		assertEquals (20, breakdown4.getTradePercentageBonusUncapped ());
		assertEquals (3, breakdown4.getTotalPopulation ());
		assertEquals (9, breakdown4.getTradePercentageBonusCapped ());
		assertEquals (9, breakdown4.getPercentageBonus ());
		
		// Increase the cap high enough to get the full bonus
		cityData.setCityPopulation (11789);

		final CityProductionBreakdown breakdown5 = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown5, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		assertEquals (20, breakdown5.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown5.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown5.getTradePercentageBonusFromRace ());
		assertEquals (20, breakdown5.getTradePercentageBonusUncapped ());
		assertEquals (11, breakdown5.getTotalPopulation ());
		assertEquals (20, breakdown5.getTradePercentageBonusCapped ());
		assertEquals (20, breakdown5.getPercentageBonus ());
		
		// Give the city an uninteresting race
		cityData.setCityRaceID ("RC01");

		final CityProductionBreakdown breakdown6 = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown6, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		assertEquals (20, breakdown6.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown6.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown6.getTradePercentageBonusFromRace ());
		assertEquals (20, breakdown6.getTradePercentageBonusUncapped ());
		assertEquals (11, breakdown6.getTotalPopulation ());
		assertEquals (20, breakdown6.getTradePercentageBonusCapped ());
		assertEquals (20, breakdown6.getPercentageBonus ());
		
		// Give them a race with a trade bonus
		cityData.setCityRaceID ("RC02");

		final CityProductionBreakdown breakdown7 = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown7, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		assertEquals (20, breakdown7.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown7.getTradePercentageBonusFromRoads ());
		assertEquals (50, breakdown7.getTradePercentageBonusFromRace ());
		assertEquals (70, breakdown7.getTradePercentageBonusUncapped ());
		assertEquals (11, breakdown7.getTotalPopulation ());
		assertEquals (33, breakdown7.getTradePercentageBonusCapped ());
		assertEquals (33, breakdown7.getPercentageBonus ());
	}

	/**
	 * Tests the buildingPassesTileTypeRequirements method, where the building requires the tiles within 2 spaces of the city
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Test
	public final void testBuildingPassesTileTypeRequirements_DistanceTwo () throws RecordNotFoundException
	{
		// Building we're trying to place
		final BuildingRequiresTileType buildingRequiresTileType = new BuildingRequiresTileType ();
		buildingRequiresTileType.setDistance (2);
		buildingRequiresTileType.setTileTypeID ("TT01");
		
		final Building building = new Building ();
		building.getBuildingRequiresTileType ().add (buildingRequiresTileType);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Building which has no pre-requisites
		assertTrue (calc.buildingPassesTileTypeRequirements (map, cityLocation, new Building (), sys));

		// Can't pass yet, since there's no tiles
		assertFalse (calc.buildingPassesTileTypeRequirements (map, cityLocation, building, sys));

		// 2 requirements, but its an 'or', so setting one of them should be enough
		final OverlandMapTerrainData mountainsTerrain = new OverlandMapTerrainData ();
		mountainsTerrain.setTileTypeID ("TT01");
		map.getPlane ().get (0).getRow ().get (1).getCell ().get (4).setTerrainData (mountainsTerrain);
		assertTrue (calc.buildingPassesTileTypeRequirements (map, cityLocation, building, sys));
	}

	/**
	 * Tests the buildingPassesTileTypeRequirements method, where the building requires the tiles within 1 space of the city
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Test
	public final void testBuildingPassesTileTypeRequirements_DistanceOne () throws RecordNotFoundException
	{
		// Building we're trying to place
		final BuildingRequiresTileType buildingRequiresTileType = new BuildingRequiresTileType ();
		buildingRequiresTileType.setDistance (1);
		buildingRequiresTileType.setTileTypeID ("TT01");
		
		final Building building = new Building ();
		building.getBuildingRequiresTileType ().add (buildingRequiresTileType);
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Can't pass yet, since there's no tiles
		assertFalse (calc.buildingPassesTileTypeRequirements (map, cityLocation, building, sys));

		// Putting it 2 tiles away doesn't help
		final OverlandMapTerrainData twoAway = new OverlandMapTerrainData ();
		twoAway.setTileTypeID ("TT01");
		map.getPlane ().get (0).getRow ().get (1).getCell ().get (4).setTerrainData (twoAway);
		assertFalse (calc.buildingPassesTileTypeRequirements (map, cityLocation, building, sys));

		// Putting it 1 tile away does
		final OverlandMapTerrainData oneAway = new OverlandMapTerrainData ();
		oneAway.setTileTypeID ("TT01");
		map.getPlane ().get (0).getRow ().get (1).getCell ().get (3).setTerrainData (oneAway);
		assertTrue (calc.buildingPassesTileTypeRequirements (map, cityLocation, building, sys));
	}

	/**
	 * Tests the listCityFoodProductionFromTerrainTiles method
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 */
	@Test
	public final void testListCityFoodProductionFromTerrainTiles () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx hillsDef = new TileTypeEx ();
		hillsDef.setDoubleFood (1);
		when (db.findTileType ("TT01", "listCityFoodProductionFromTerrainTiles")).thenReturn (hillsDef);
		
		final TileTypeEx riverDef = new TileTypeEx ();
		riverDef.setDoubleFood (4);
		when (db.findTileType ("TT02", "listCityFoodProductionFromTerrainTiles")).thenReturn (riverDef);
		
		final TileTypeEx mountainsDef = new TileTypeEx ();
		when (db.findTileType ("TT03", "listCityFoodProductionFromTerrainTiles")).thenReturn (mountainsDef);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Session description
		final CoordinateSystem overlandMapCoordinateSystem = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// 0 so far
		final CityProductionBreakdown breakdown1 = calc.listCityFoodProductionFromTerrainTiles (map, cityLocation, overlandMapCoordinateSystem, db);
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, breakdown1.getProductionTypeID ());
		assertEquals (0, breakdown1.getDoubleProductionAmount ());
		assertEquals (0, breakdown1.getTileTypeProduction ().size ());

		// Add some tile types that grant food, 3 hills and 5 rivers, (3*1) + (4*5) = 23
		// Note we add hills in the NW and NE corners too, outside the city radius, so we prove that this isn't counted
		// Also adds some mountains, which don't produce any food at all, to prove they don't get included in the breakdown
		for (int x = 0; x <= 4; x++)
		{
			final OverlandMapTerrainData hillsTerrain = new OverlandMapTerrainData ();
			hillsTerrain.setTileTypeID ("TT01");
			map.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
			riverTile.setTileTypeID ("TT02");
			map.getPlane ().get (0).getRow ().get (1).getCell ().get (x).setTerrainData (riverTile);

			final OverlandMapTerrainData mountainsTile = new OverlandMapTerrainData ();
			mountainsTile.setTileTypeID ("TT03");
			map.getPlane ().get (0).getRow ().get (2).getCell ().get (x).setTerrainData (mountainsTile);
		}

		final CityProductionBreakdown breakdown2 = calc.listCityFoodProductionFromTerrainTiles (map, cityLocation, overlandMapCoordinateSystem, db);
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, breakdown2.getProductionTypeID ());
		assertEquals (23, breakdown2.getDoubleProductionAmount ());
		assertEquals (2, breakdown2.getTileTypeProduction ().size ());
		assertEquals ("TT02", breakdown2.getTileTypeProduction ().get (0).getTileTypeID ());
		assertEquals (5, breakdown2.getTileTypeProduction ().get (0).getCount ());
		assertEquals (4, breakdown2.getTileTypeProduction ().get (0).getDoubleProductionAmountEachTile ());
		assertEquals (20, breakdown2.getTileTypeProduction ().get (0).getDoubleProductionAmountAllTiles ());
		assertEquals ("TT01", breakdown2.getTileTypeProduction ().get (1).getTileTypeID ());
		assertEquals (3, breakdown2.getTileTypeProduction ().get (1).getCount ());
		assertEquals (1, breakdown2.getTileTypeProduction ().get (1).getDoubleProductionAmountEachTile ());
		assertEquals (3, breakdown2.getTileTypeProduction ().get (1).getDoubleProductionAmountAllTiles ());
	}

	/**
	 * Tests the calculateCityGrowthRate method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final RaceEx standardRace = new RaceEx ();
		when (db.findRace ("RC01", "calculateCityGrowthRate")).thenReturn (standardRace);

		final RaceEx raceWithBonus = new RaceEx ();
		raceWithBonus.setGrowthRateModifier (20);
		when (db.findRace ("RC02", "calculateCityGrowthRate")).thenReturn (raceWithBonus);
		
		final RaceEx raceWithPenalty = new RaceEx ();
		raceWithPenalty.setGrowthRateModifier (-20);
		when (db.findRace ("RC03", "calculateCityGrowthRate")).thenReturn (raceWithPenalty);

		final Building granaryDef = new Building ();
		granaryDef.setGrowthRateBonus (20);
		when (db.findBuilding ("BL01", "calculateCityGrowthRate")).thenReturn (granaryDef);

		final Building farmersMarketDef = new Building ();
		farmersMarketDef.setGrowthRateBonus (30);
		when (db.findBuilding ("BL02", "calculateCityGrowthRate")).thenReturn (farmersMarketDef);
		
		final Building sagesGuildDef = new Building ();
		when (db.findBuilding ("BL03", "calculateCityGrowthRate")).thenReturn (sagesGuildDef);

		// Owner
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);

		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID ("WZ01");

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null);
		
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityGrowthRate")).thenReturn (player);
		
		// Spells
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);

		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (pd.getPlayerID ());
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// At max size
		cityData.setCityPopulation (10000);
		final CityGrowthRateBreakdown maximum = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 10, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdown.class.getName (), maximum.getClass ().getName ());
		assertEquals (10000, maximum.getCurrentPopulation ());
		assertEquals (10000, maximum.getMaximumPopulation ());
		assertEquals (0, maximum.getInitialTotal ());
		assertEquals (0, maximum.getCappedTotal ());

		// Growing (this is the example quoted in the strategy guide, however note the example is in contradiction with the formula - from testing I believe the example is right and the formula is supposed to be a -1 not a +1)
		cityData.setCityPopulation (12000);
		final CityGrowthRateBreakdown growingEvenBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 22, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), growingEvenBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing growingEven = (CityGrowthRateBreakdownGrowing) growingEvenBreakdown;
		assertEquals (12000, growingEven.getCurrentPopulation ());
		assertEquals (22000, growingEven.getMaximumPopulation ());
		assertEquals (50, growingEven.getBaseGrowthRate ());
		assertEquals (0, growingEven.getRacialGrowthModifier ());
		assertEquals (0, growingEven.getBuildingModifier ().size ());
		assertEquals (50, growingEven.getTotalGrowthRate ());
		assertEquals (100, growingEven.getDifficultyLevelMultiplier ());
		assertEquals (50, growingEven.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (50, growingEven.getInitialTotal ());
		assertEquals (50, growingEven.getCappedTotal ());

		final CityGrowthRateBreakdown growingOddBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 23, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), growingOddBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing growingOdd = (CityGrowthRateBreakdownGrowing) growingOddBreakdown;
		assertEquals (12000, growingOdd.getCurrentPopulation ());
		assertEquals (23000, growingOdd.getMaximumPopulation ());
		assertEquals (50, growingOdd.getBaseGrowthRate ());
		assertEquals (0, growingOdd.getRacialGrowthModifier ());
		assertEquals (0, growingOdd.getBuildingModifier ().size ());
		assertEquals (50, growingOdd.getTotalGrowthRate ());
		assertEquals (100, growingOdd.getDifficultyLevelMultiplier ());
		assertEquals (50, growingOdd.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (50, growingOdd.getInitialTotal ());
		assertEquals (50, growingOdd.getCappedTotal ());

		// Bonus from race - positive
		cityData.setCityRaceID ("RC02");
		final CityGrowthRateBreakdown barbarianBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 22, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), barbarianBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing barbarian = (CityGrowthRateBreakdownGrowing) barbarianBreakdown;
		assertEquals (12000, barbarian.getCurrentPopulation ());
		assertEquals (22000, barbarian.getMaximumPopulation ());
		assertEquals (50, barbarian.getBaseGrowthRate ());
		assertEquals (20, barbarian.getRacialGrowthModifier ());
		assertEquals (0, barbarian.getBuildingModifier ().size ());
		assertEquals (70, barbarian.getTotalGrowthRate ());
		assertEquals (100, barbarian.getDifficultyLevelMultiplier ());
		assertEquals (70, barbarian.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (70, barbarian.getInitialTotal ());
		assertEquals (70, barbarian.getCappedTotal ());

		// Bonus from race - negative
		cityData.setCityRaceID ("RC03");
		final CityGrowthRateBreakdown highElfBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 22, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), highElfBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing highElf = (CityGrowthRateBreakdownGrowing) highElfBreakdown;
		assertEquals (12000, highElf.getCurrentPopulation ());
		assertEquals (22000, highElf.getMaximumPopulation ());
		assertEquals (50, highElf.getBaseGrowthRate ());
		assertEquals (-20, highElf.getRacialGrowthModifier ());
		assertEquals (0, highElf.getBuildingModifier ().size ());
		assertEquals (30, highElf.getTotalGrowthRate ());
		assertEquals (100, highElf.getDifficultyLevelMultiplier ());
		assertEquals (30, highElf.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (30, highElf.getInitialTotal ());
		assertEquals (30, highElf.getCappedTotal ());

		// Bonus from buildings
		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID ("BL01");
		granary.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (granary);

		final MemoryBuilding farmersMarket = new MemoryBuilding ();
		farmersMarket.setBuildingID ("BL02");
		farmersMarket.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (farmersMarket);

		final MemoryBuilding sagesGuild = new MemoryBuilding ();		// Irrelevant building, to prove it doesn't get included in the list
		sagesGuild.setBuildingID ("BL03");
		sagesGuild.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (sagesGuild);

		final CityGrowthRateBreakdown withBuildingsBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 22, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), withBuildingsBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing withBuildings = (CityGrowthRateBreakdownGrowing) withBuildingsBreakdown;
		assertEquals (12000, withBuildings.getCurrentPopulation ());
		assertEquals (22000, withBuildings.getMaximumPopulation ());
		assertEquals (50, withBuildings.getBaseGrowthRate ());
		assertEquals (-20, withBuildings.getRacialGrowthModifier ());
		assertEquals (2, withBuildings.getBuildingModifier ().size ());
		assertEquals ("BL01", withBuildings.getBuildingModifier ().get (0).getBuildingID ());
		assertEquals (20, withBuildings.getBuildingModifier ().get (0).getGrowthRateBonus ());
		assertEquals ("BL02", withBuildings.getBuildingModifier ().get (1).getBuildingID ());
		assertEquals (30, withBuildings.getBuildingModifier ().get (1).getGrowthRateBonus ());
		assertEquals (80, withBuildings.getTotalGrowthRate ());
		assertEquals (100, withBuildings.getDifficultyLevelMultiplier ());
		assertEquals (80, withBuildings.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (80, withBuildings.getInitialTotal ());
		assertEquals (80, withBuildings.getCappedTotal ());
		
		// Bonus for AI players
		pd.setHuman (false);

		final CityGrowthRateBreakdown aiWithBuildingsBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 22, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), aiWithBuildingsBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing aiWithBuildings = (CityGrowthRateBreakdownGrowing) aiWithBuildingsBreakdown;
		assertEquals (12000, aiWithBuildings.getCurrentPopulation ());
		assertEquals (22000, aiWithBuildings.getMaximumPopulation ());
		assertEquals (50, aiWithBuildings.getBaseGrowthRate ());
		assertEquals (-20, aiWithBuildings.getRacialGrowthModifier ());
		assertEquals (2, aiWithBuildings.getBuildingModifier ().size ());
		assertEquals ("BL01", aiWithBuildings.getBuildingModifier ().get (0).getBuildingID ());
		assertEquals (20, aiWithBuildings.getBuildingModifier ().get (0).getGrowthRateBonus ());
		assertEquals ("BL02", aiWithBuildings.getBuildingModifier ().get (1).getBuildingID ());
		assertEquals (30, aiWithBuildings.getBuildingModifier ().get (1).getGrowthRateBonus ());
		assertEquals (80, aiWithBuildings.getTotalGrowthRate ());
		assertEquals (300, aiWithBuildings.getDifficultyLevelMultiplier ());
		assertEquals (240, aiWithBuildings.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (240, aiWithBuildings.getInitialTotal ());
		assertEquals (240, aiWithBuildings.getCappedTotal ());
		
		pd.setHuman (true);
		
		// With all those buildings, at almost max size we still get a reasonable increase
		cityData.setCityPopulation (21960);
		final CityGrowthRateBreakdown almostCappedBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 22, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), almostCappedBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing almostCapped = (CityGrowthRateBreakdownGrowing) almostCappedBreakdown;
		assertEquals (21960, almostCapped.getCurrentPopulation ());
		assertEquals (22000, almostCapped.getMaximumPopulation ());
		assertEquals (0, almostCapped.getBaseGrowthRate ());
		assertEquals (-20, almostCapped.getRacialGrowthModifier ());
		assertEquals (2, almostCapped.getBuildingModifier ().size ());
		assertEquals ("BL01", almostCapped.getBuildingModifier ().get (0).getBuildingID ());
		assertEquals (20, almostCapped.getBuildingModifier ().get (0).getGrowthRateBonus ());
		assertEquals ("BL02", almostCapped.getBuildingModifier ().get (1).getBuildingID ());
		assertEquals (30, almostCapped.getBuildingModifier ().get (1).getGrowthRateBonus ());
		assertEquals (30, almostCapped.getTotalGrowthRate ());
		assertEquals (100, almostCapped.getDifficultyLevelMultiplier ());
		assertEquals (30, almostCapped.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (30, almostCapped.getInitialTotal ());
		assertEquals (30, almostCapped.getCappedTotal ());

		// +30 with only 20 to spare would push us over max size
		cityData.setCityPopulation (21980);
		final CityGrowthRateBreakdown overCapBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 22, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), overCapBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing overCap = (CityGrowthRateBreakdownGrowing) overCapBreakdown;
		assertEquals (21980, overCap.getCurrentPopulation ());
		assertEquals (22000, overCap.getMaximumPopulation ());
		assertEquals (0, overCap.getBaseGrowthRate ());
		assertEquals (-20, overCap.getRacialGrowthModifier ());
		assertEquals (2, overCap.getBuildingModifier ().size ());
		assertEquals ("BL01", overCap.getBuildingModifier ().get (0).getBuildingID ());
		assertEquals (20, overCap.getBuildingModifier ().get (0).getGrowthRateBonus ());
		assertEquals ("BL02", overCap.getBuildingModifier ().get (1).getBuildingID ());
		assertEquals (30, overCap.getBuildingModifier ().get (1).getGrowthRateBonus ());
		assertEquals (30, overCap.getTotalGrowthRate ());
		assertEquals (100, overCap.getDifficultyLevelMultiplier ());
		assertEquals (30, overCap.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (30, overCap.getInitialTotal ());
		assertEquals (20, overCap.getCappedTotal ());

		// Dying - note the race and building modifiers don't apply, because we can't by virtue of bonuses force a city to go over max size
		final CityGrowthRateBreakdown dyingBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 18, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownDying.class.getName (), dyingBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownDying dying = (CityGrowthRateBreakdownDying) dyingBreakdown;
		assertEquals (21980, dying.getCurrentPopulation ());
		assertEquals (18000, dying.getMaximumPopulation ());
		assertEquals (3, dying.getBaseDeathRate ());
		assertEquals (150, dying.getCityDeathRate ());
		assertEquals (-150, dying.getInitialTotal ());
		assertEquals (-150, dying.getCappedTotal ());

		// Dying and would go under minimum size
		cityData.setCityPopulation (1020);
		final CityGrowthRateBreakdown underCapBreakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, cityLocation, 0, difficultyLevel, db);
		assertEquals (CityGrowthRateBreakdownDying.class.getName (), underCapBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownDying underCap = (CityGrowthRateBreakdownDying) underCapBreakdown;
		assertEquals (1020, underCap.getCurrentPopulation ());
		assertEquals (1000, underCap.getMaximumPopulation ());
		assertEquals (1, underCap.getBaseDeathRate ());
		assertEquals (50, underCap.getCityDeathRate ());
		assertEquals (-50, underCap.getInitialTotal ());
		assertEquals (-20, underCap.getCappedTotal ());
	}

	/**
	 * Tests the calculateCityRebels method
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If any of a number of items cannot be found in the cache
	 */
	@Test
	public final void testCalculateCityRebels () throws PlayerNotFoundException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building animstsGuildDef = new Building ();
		animstsGuildDef.setBuildingUnrestReduction (1);
		animstsGuildDef.setBuildingUnrestReductionImprovedByRetorts (false);
		when (db.findBuilding ("BL02", "calculateCityRebels")).thenReturn (animstsGuildDef);
		
		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);

		// Tax rates
		final TaxRate taxRate1 = new TaxRate ();
		when (db.findTaxRate ("TR01", "calculateCityRebels")).thenReturn (taxRate1);

		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);

		final TaxRate taxRate3 = new TaxRate ();
		taxRate3.setTaxUnrestPercentage (75);
		when (db.findTaxRate ("TR03", "calculateCityRebels")).thenReturn (taxRate3);
		
		// Units
		final UnitEx normalUnitDef = new UnitEx ();
		normalUnitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "calculateCityRebels")).thenReturn (normalUnitDef);
		
		final Pick normalMagicRealm = new Pick ();
		normalMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("LTN", "calculateCityRebels")).thenReturn (normalMagicRealm);

		final UnitEx summonedUnitDef = new UnitEx ();
		summonedUnitDef.setUnitMagicRealm ("MB01");
		when (db.findUnit ("UN002", "calculateCityRebels")).thenReturn (summonedUnitDef);
		
		final Pick summonedMagicRealm = new Pick ();
		summonedMagicRealm.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		when (db.findPick ("MB01", "calculateCityRebels")).thenReturn (summonedMagicRealm);
		
		final UnitEx heroUnitDef = new UnitEx ();
		heroUnitDef.setUnitMagicRealm ("LTH");
		when (db.findUnit ("UN003", "calculateCityRebels")).thenReturn (heroUnitDef);
		
		final Pick heroMagicRealm = new Pick ();
		heroMagicRealm.setUnitTypeID ("H");
		when (db.findPick ("LTH", "calculateCityRebels")).thenReturn (heroMagicRealm);
		
		// Races
		final RaceUnrest klackonUnrest = new RaceUnrest ();
		klackonUnrest.setCapitalRaceID ("RC01");
		klackonUnrest.setUnrestLiteral (-2);
		
		final RaceEx klackonsDef = new RaceEx ();
		klackonsDef.getRaceUnrest ().add (klackonUnrest);
		when (db.findRace ("RC01", "calculateCityRebels")).thenReturn (klackonsDef);
		
		final RaceUnrest highElfDwarfUnrest = new RaceUnrest ();
		highElfDwarfUnrest.setCapitalRaceID ("RC03");
		highElfDwarfUnrest.setUnrestPercentage (30);
		
		final RaceEx highElvesDef = new RaceEx ();
		highElvesDef.getRaceUnrest ().add (highElfDwarfUnrest);
		when (db.findRace ("RC02", "calculateCityRebels")).thenReturn (highElvesDef);
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Tax rate with no rebels!  and no gold...
		final CityUnrestBreakdown zeroPercent = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR01", db);
		assertEquals (17, zeroPercent.getPopulation ());
		assertEquals (0, zeroPercent.getTaxPercentage ());
		assertEquals (0, zeroPercent.getRacialPercentage ());
		assertEquals (0, zeroPercent.getRacialLiteral ());
		assertEquals (0, zeroPercent.getTotalPercentage ());
		assertEquals (0, zeroPercent.getBaseValue ());
		assertEquals (0, zeroPercent.getBuildingReducingUnrest ().size ());
		assertEquals (0, zeroPercent.getReligiousBuildingRetortPercentage ());
		assertEquals (0, zeroPercent.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (0, zeroPercent.getReligiousBuildingReduction ());
		assertEquals (0, zeroPercent.getReligiousBuildingRetortValue ());
		assertEquals (0, zeroPercent.getUnitCount ());
		assertEquals (0, zeroPercent.getUnitReduction ());
		assertEquals (0, zeroPercent.getBaseTotal ());
		assertFalse (zeroPercent.isForcePositive ());
		assertFalse (zeroPercent.isForceAll ());
		assertEquals (0, zeroPercent.getMinimumFarmers ());		// We have 6 minimum farmers, but set to 0 since the cap doesn't affect the result
		assertEquals (0, zeroPercent.getTotalAfterFarmers ());
		assertEquals (0, zeroPercent.getFinalTotal ());

		// Harsh 45% tax rate = 7.65, prove that it rounds down
		final CityUnrestBreakdown highPercent = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, highPercent.getPopulation ());
		assertEquals (45, highPercent.getTaxPercentage ());
		assertEquals (0, highPercent.getRacialPercentage ());
		assertEquals (0, highPercent.getRacialLiteral ());
		assertEquals (45, highPercent.getTotalPercentage ());
		assertEquals (7, highPercent.getBaseValue ());
		assertEquals (0, highPercent.getBuildingReducingUnrest ().size ());
		assertEquals (0, highPercent.getReligiousBuildingRetortPercentage ());
		assertEquals (0, highPercent.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (0, highPercent.getReligiousBuildingReduction ());
		assertEquals (0, highPercent.getReligiousBuildingRetortValue ());
		assertEquals (0, highPercent.getUnitCount ());
		assertEquals (0, highPercent.getUnitReduction ());
		assertEquals (7, highPercent.getBaseTotal ());
		assertFalse (highPercent.isForcePositive ());
		assertFalse (highPercent.isForceAll ());
		assertEquals (0, highPercent.getMinimumFarmers ());
		assertEquals (0, highPercent.getTotalAfterFarmers ());
		assertEquals (7, highPercent.getFinalTotal ());

		// Worst 75% tax rate = 12.75, but we have 6 minimum farmers so would be 18 population in a size 17 city - prove rebels will revert to farmers to avoid starving
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		
		final CityUnrestBreakdown maxPercent = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR03", db);
		assertEquals (17, maxPercent.getPopulation ());
		assertEquals (75, maxPercent.getTaxPercentage ());
		assertEquals (0, maxPercent.getRacialPercentage ());
		assertEquals (0, maxPercent.getRacialLiteral ());
		assertEquals (75, maxPercent.getTotalPercentage ());
		assertEquals (12, maxPercent.getBaseValue ());
		assertEquals (0, maxPercent.getBuildingReducingUnrest ().size ());
		assertEquals (0, maxPercent.getReligiousBuildingRetortPercentage ());							// 0 even though we do get 50% from the retort, because we have no religious buildings
		assertEquals (0, maxPercent.getPickIdContributingToReligiousBuildingBonus ().size ());		// Likewise
		assertEquals (0, maxPercent.getReligiousBuildingReduction ());
		assertEquals (0, maxPercent.getReligiousBuildingRetortValue ());
		assertEquals (0, maxPercent.getUnitCount ());
		assertEquals (0, maxPercent.getUnitReduction ());
		assertEquals (12, maxPercent.getBaseTotal ());
		assertFalse (maxPercent.isForcePositive ());
		assertFalse (maxPercent.isForceAll ());
		assertEquals (6, maxPercent.getMinimumFarmers ());		// Now the mininmum farmers makes a difference, the value gets listed
		assertEquals (11, maxPercent.getTotalAfterFarmers ());
		assertEquals (11, maxPercent.getFinalTotal ());

		// Add some buildings that reduce unrest - and back to 45% tax rate = 7.65
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (shrineBuilding);

		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (0);
		
		final CityUnrestBreakdown shrine = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, shrine.getPopulation ());
		assertEquals (45, shrine.getTaxPercentage ());
		assertEquals (0, shrine.getRacialPercentage ());
		assertEquals (0, shrine.getRacialLiteral ());
		assertEquals (45, shrine.getTotalPercentage ());
		assertEquals (7, shrine.getBaseValue ());
		assertEquals (1, shrine.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", shrine.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, shrine.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (0, shrine.getReligiousBuildingRetortPercentage ());
		assertEquals (0, shrine.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (0, shrine.getReligiousBuildingReduction ());		// 0 even though we do have a shrine, because we've got no retort that improves it
		assertEquals (0, shrine.getReligiousBuildingRetortValue ());
		assertEquals (0, shrine.getUnitCount ());
		assertEquals (0, shrine.getUnitReduction ());
		assertEquals (6, shrine.getBaseTotal ());
		assertFalse (shrine.isForcePositive ());
		assertFalse (shrine.isForceAll ());
		assertEquals (0, shrine.getMinimumFarmers ());
		assertEquals (0, shrine.getTotalAfterFarmers ());
		assertEquals (6, shrine.getFinalTotal ());

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);

		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL02");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (secondBuilding);

		final CityUnrestBreakdown animistsGuild = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, animistsGuild.getPopulation ());
		assertEquals (45, animistsGuild.getTaxPercentage ());
		assertEquals (0, animistsGuild.getRacialPercentage ());
		assertEquals (0, animistsGuild.getRacialLiteral ());
		assertEquals (45, animistsGuild.getTotalPercentage ());
		assertEquals (7, animistsGuild.getBaseValue ());
		assertEquals (2, animistsGuild.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", animistsGuild.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, animistsGuild.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL02", animistsGuild.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, animistsGuild.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, animistsGuild.getReligiousBuildingRetortPercentage ());						// Now the 50% comes out, because of the shrine
		assertEquals (1, animistsGuild.getPickIdContributingToReligiousBuildingBonus ().size ());	// Likewise
		assertEquals ("RT01", animistsGuild.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-1, animistsGuild.getReligiousBuildingReduction ());									// Now the shrine gets counted
		assertEquals (0, animistsGuild.getReligiousBuildingRetortValue ());									// 1 religious building isn't enough to get the 50% bonus
		assertEquals (0, animistsGuild.getUnitCount ());
		assertEquals (0, animistsGuild.getUnitReduction ());
		assertEquals (5, animistsGuild.getBaseTotal ());
		assertFalse (animistsGuild.isForcePositive ());
		assertFalse (animistsGuild.isForceAll ());
		assertEquals (0, animistsGuild.getMinimumFarmers ());
		assertEquals (0, animistsGuild.getTotalAfterFarmers ());
		assertEquals (5, animistsGuild.getFinalTotal ());

		// Divine power does work on 2nd religious building
		secondBuilding.setBuildingID ("BL03");
		final CityUnrestBreakdown temple = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, temple.getPopulation ());
		assertEquals (45, temple.getTaxPercentage ());
		assertEquals (0, temple.getRacialPercentage ());
		assertEquals (0, temple.getRacialLiteral ());
		assertEquals (45, temple.getTotalPercentage ());
		assertEquals (7, temple.getBaseValue ());
		assertEquals (2, temple.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", temple.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, temple.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL03", temple.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, temple.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, temple.getReligiousBuildingRetortPercentage ());
		assertEquals (1, temple.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", temple.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-2, temple.getReligiousBuildingReduction ());
		assertEquals (-1, temple.getReligiousBuildingRetortValue ());			// Now with 2 religious buildings we get the bonus
		assertEquals (0, temple.getUnitCount ());
		assertEquals (0, temple.getUnitReduction ());
		assertEquals (4, temple.getBaseTotal ());
		assertFalse (temple.isForcePositive ());
		assertFalse (temple.isForceAll ());
		assertEquals (0, temple.getMinimumFarmers ());
		assertEquals (0, temple.getTotalAfterFarmers ());
		assertEquals (4, temple.getFinalTotal ());

		// 1 unit does nothing
		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID ("UN001");
		normalUnit.setUnitLocation (new MapCoordinates3DEx (2, 2, 0));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final CityUnrestBreakdown firstUnit = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, firstUnit.getPopulation ());
		assertEquals (45, firstUnit.getTaxPercentage ());
		assertEquals (0, firstUnit.getRacialPercentage ());
		assertEquals (0, firstUnit.getRacialLiteral ());
		assertEquals (45, firstUnit.getTotalPercentage ());
		assertEquals (7, firstUnit.getBaseValue ());
		assertEquals (2, firstUnit.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", firstUnit.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, firstUnit.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL03", firstUnit.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, firstUnit.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, firstUnit.getReligiousBuildingRetortPercentage ());
		assertEquals (1, firstUnit.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", firstUnit.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-2, firstUnit.getReligiousBuildingReduction ());
		assertEquals (-1, firstUnit.getReligiousBuildingRetortValue ());
		assertEquals (1, firstUnit.getUnitCount ());
		assertEquals (0, firstUnit.getUnitReduction ());
		assertEquals (4, firstUnit.getBaseTotal ());
		assertFalse (firstUnit.isForcePositive ());
		assertFalse (firstUnit.isForceAll ());
		assertEquals (0, firstUnit.getMinimumFarmers ());
		assertEquals (0, firstUnit.getTotalAfterFarmers ());
		assertEquals (4, firstUnit.getFinalTotal ());

		// 2nd unit reduces unrest, even if one is normal and one a hero
		final MemoryUnit heroUnit = new MemoryUnit ();
		heroUnit.setUnitID ("UN003");
		heroUnit.setUnitLocation (new MapCoordinates3DEx (2, 2, 0));
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);

		final CityUnrestBreakdown secondUnit = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, secondUnit.getPopulation ());
		assertEquals (45, secondUnit.getTaxPercentage ());
		assertEquals (0, secondUnit.getRacialPercentage ());
		assertEquals (0, secondUnit.getRacialLiteral ());
		assertEquals (45, secondUnit.getTotalPercentage ());
		assertEquals (7, secondUnit.getBaseValue ());
		assertEquals (2, secondUnit.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", secondUnit.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, secondUnit.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL03", secondUnit.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, secondUnit.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, secondUnit.getReligiousBuildingRetortPercentage ());
		assertEquals (1, secondUnit.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", secondUnit.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-2, secondUnit.getReligiousBuildingReduction ());
		assertEquals (-1, secondUnit.getReligiousBuildingRetortValue ());
		assertEquals (2, secondUnit.getUnitCount ());
		assertEquals (-1, secondUnit.getUnitReduction ());
		assertEquals (3, secondUnit.getBaseTotal ());
		assertFalse (secondUnit.isForcePositive ());
		assertFalse (secondUnit.isForceAll ());
		assertEquals (0, secondUnit.getMinimumFarmers ());
		assertEquals (0, secondUnit.getTotalAfterFarmers ());
		assertEquals (3, secondUnit.getFinalTotal ());

		// summoned units or dead units don't help (unitCount still = 2)
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit deadUnit = new MemoryUnit ();
			deadUnit.setUnitID ("UN001");
			deadUnit.setUnitLocation (new MapCoordinates3DEx (2, 2, 0));
			deadUnit.setStatus (UnitStatusID.DEAD);
			units.add (deadUnit);

			final MemoryUnit summonedUnit = new MemoryUnit ();
			summonedUnit.setUnitID ("UN002");
			summonedUnit.setUnitLocation (new MapCoordinates3DEx (2, 2, 0));
			summonedUnit.setStatus (UnitStatusID.ALIVE);
			units.add (summonedUnit);
		}

		final CityUnrestBreakdown extraUnits = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, extraUnits.getPopulation ());
		assertEquals (45, extraUnits.getTaxPercentage ());
		assertEquals (0, extraUnits.getRacialPercentage ());
		assertEquals (0, extraUnits.getRacialLiteral ());
		assertEquals (45, extraUnits.getTotalPercentage ());
		assertEquals (7, extraUnits.getBaseValue ());
		assertEquals (2, extraUnits.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", extraUnits.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, extraUnits.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL03", extraUnits.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, extraUnits.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, extraUnits.getReligiousBuildingRetortPercentage ());
		assertEquals (1, extraUnits.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", extraUnits.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-2, extraUnits.getReligiousBuildingReduction ());
		assertEquals (-1, extraUnits.getReligiousBuildingRetortValue ());
		assertEquals (2, extraUnits.getUnitCount ());
		assertEquals (-1, extraUnits.getUnitReduction ());
		assertEquals (3, extraUnits.getBaseTotal ());
		assertFalse (extraUnits.isForcePositive ());
		assertFalse (extraUnits.isForceAll ());
		assertEquals (0, extraUnits.getMinimumFarmers ());
		assertEquals (0, extraUnits.getTotalAfterFarmers ());
		assertEquals (3, extraUnits.getFinalTotal ());

		// Put our captial here, and its klackons so we get -2
		final MemoryBuilding fortressBuilding = new MemoryBuilding ();
		fortressBuilding.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		when (memoryBuildingUtils.findCityWithBuilding (1, CommonDatabaseConstants.BUILDING_FORTRESS, map, buildings)).thenReturn (fortressBuilding);

		final CityUnrestBreakdown klackons = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, klackons.getPopulation ());
		assertEquals (45, klackons.getTaxPercentage ());
		assertEquals (0, klackons.getRacialPercentage ());
		assertEquals (-2, klackons.getRacialLiteral ());
		assertEquals (45, klackons.getTotalPercentage ());
		assertEquals (7, klackons.getBaseValue ());
		assertEquals (2, klackons.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", klackons.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, klackons.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL03", klackons.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, klackons.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, klackons.getReligiousBuildingRetortPercentage ());
		assertEquals (1, klackons.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", klackons.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-2, klackons.getReligiousBuildingReduction ());
		assertEquals (-1, klackons.getReligiousBuildingRetortValue ());
		assertEquals (2, klackons.getUnitCount ());
		assertEquals (-1, klackons.getUnitReduction ());
		assertEquals (1, klackons.getBaseTotal ());
		assertFalse (klackons.isForcePositive ());
		assertFalse (klackons.isForceAll ());
		assertEquals (0, klackons.getMinimumFarmers ());
		assertEquals (0, klackons.getTotalAfterFarmers ());
		assertEquals (1, klackons.getFinalTotal ());
		
		// Other races get no bonus from being same as capital race
		cityData.setCityRaceID ("RC02");

		final CityUnrestBreakdown highElves = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, highElves.getPopulation ());
		assertEquals (45, highElves.getTaxPercentage ());
		assertEquals (0, highElves.getRacialPercentage ());
		assertEquals (0, highElves.getRacialLiteral ());
		assertEquals (45, highElves.getTotalPercentage ());
		assertEquals (7, highElves.getBaseValue ());
		assertEquals (2, highElves.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", highElves.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, highElves.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL03", highElves.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, highElves.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, highElves.getReligiousBuildingRetortPercentage ());
		assertEquals (1, highElves.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", highElves.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-2, highElves.getReligiousBuildingReduction ());
		assertEquals (-1, highElves.getReligiousBuildingRetortValue ());
		assertEquals (2, highElves.getUnitCount ());
		assertEquals (-1, highElves.getUnitReduction ());
		assertEquals (3, highElves.getBaseTotal ());
		assertFalse (highElves.isForcePositive ());
		assertFalse (highElves.isForceAll ());
		assertEquals (0, highElves.getMinimumFarmers ());
		assertEquals (0, highElves.getTotalAfterFarmers ());
		assertEquals (3, highElves.getFinalTotal ());

		// If reduce the tax rate from only having 3 rebels, they're so happy that we get a negative number of rebels
		final CityUnrestBreakdown forcePositive = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR01", db);
		assertEquals (17, forcePositive.getPopulation ());
		assertEquals (0, forcePositive.getTaxPercentage ());
		assertEquals (0, forcePositive.getRacialPercentage ());
		assertEquals (0, forcePositive.getRacialLiteral ());
		assertEquals (0, forcePositive.getTotalPercentage ());
		assertEquals (0, forcePositive.getBaseValue ());
		assertEquals (2, forcePositive.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", forcePositive.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, forcePositive.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL03", forcePositive.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, forcePositive.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, forcePositive.getReligiousBuildingRetortPercentage ());
		assertEquals (1, forcePositive.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", forcePositive.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-2, forcePositive.getReligiousBuildingReduction ());
		assertEquals (-1, forcePositive.getReligiousBuildingRetortValue ());
		assertEquals (2, forcePositive.getUnitCount ());
		assertEquals (-1, forcePositive.getUnitReduction ());
		assertEquals (-4, forcePositive.getBaseTotal ());			// Negative baseTotal
		assertTrue (forcePositive.isForcePositive ());
		assertFalse (forcePositive.isForceAll ());
		assertEquals (0, forcePositive.getMinimumFarmers ());
		assertEquals (0, forcePositive.getTotalAfterFarmers ());
		assertEquals (0, forcePositive.getFinalTotal ());
		
		// Move capital to a different city with a different race
		final OverlandMapCityData capitalCityData = new OverlandMapCityData ();
		capitalCityData.setCityRaceID ("RC03");
		capitalCityData.setCityOwnerID (1);
		capitalCityData.setCityPopulation (1000);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (20).setCityData (capitalCityData);

		fortressBuilding.setCityLocation (new MapCoordinates3DEx (20, 2, 0));

		final CityUnrestBreakdown racialUnrest = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR02", db);
		assertEquals (17, racialUnrest.getPopulation ());
		assertEquals (45, racialUnrest.getTaxPercentage ());
		assertEquals (30, racialUnrest.getRacialPercentage ());
		assertEquals (0, racialUnrest.getRacialLiteral ());
		assertEquals (75, racialUnrest.getTotalPercentage ());
		assertEquals (12, racialUnrest.getBaseValue ());
		assertEquals (2, racialUnrest.getBuildingReducingUnrest ().size ());
		assertEquals ("BL01", racialUnrest.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, racialUnrest.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals ("BL03", racialUnrest.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, racialUnrest.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, racialUnrest.getReligiousBuildingRetortPercentage ());
		assertEquals (1, racialUnrest.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", racialUnrest.getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals (-2, racialUnrest.getReligiousBuildingReduction ());
		assertEquals (-1, racialUnrest.getReligiousBuildingRetortValue ());
		assertEquals (2, racialUnrest.getUnitCount ());
		assertEquals (-1, racialUnrest.getUnitReduction ());
		assertEquals (8, racialUnrest.getBaseTotal ());
		assertFalse (racialUnrest.isForcePositive ());
		assertFalse (racialUnrest.isForceAll ());
		assertEquals (0, racialUnrest.getMinimumFarmers ());
		assertEquals (0, racialUnrest.getTotalAfterFarmers ());
		assertEquals (8, racialUnrest.getFinalTotal ());
		
		// Make them so mad that there's more rebels than there are people
		buildings.remove (shrineBuilding);
		buildings.remove (secondBuilding);
		units.clear ();
		
		cityData.setCityPopulation (24890);		// Has to be over 20 for 105% to round down to >1 person
		
		final CityUnrestBreakdown forceAll = calc.calculateCityRebels
			(players, map, units, buildings, spells, cityLocation, "TR03", db);
		assertEquals (24, forceAll.getPopulation ());
		assertEquals (75, forceAll.getTaxPercentage ());
		assertEquals (30, forceAll.getRacialPercentage ());
		assertEquals (0, forceAll.getRacialLiteral ());
		assertEquals (105, forceAll.getTotalPercentage ());
		assertEquals (25, forceAll.getBaseValue ());
		assertEquals (0, forceAll.getBuildingReducingUnrest ().size ());
		assertEquals (0, forceAll.getReligiousBuildingRetortPercentage ());
		assertEquals (0, forceAll.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (0, forceAll.getReligiousBuildingReduction ());
		assertEquals (0, forceAll.getReligiousBuildingRetortValue ());
		assertEquals (0, forceAll.getUnitCount ());
		assertEquals (0, forceAll.getUnitReduction ());
		assertEquals (25, forceAll.getBaseTotal ());				// More than the population of 24
		assertFalse (forceAll.isForcePositive ());
		assertTrue (forceAll.isForceAll ());
		assertEquals (6, forceAll.getMinimumFarmers ());		// Gets reduced from 25>24 because of population size, then from 24>18 because of minimum farmers
		assertEquals (18, forceAll.getTotalAfterFarmers ());
		assertEquals (18, forceAll.getFinalTotal ());
	}
	
	/**
	 * Tests the addProductionFromPopulation method
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	@Test
	public final void testAddProductionFromPopulation () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeAndDoubledValue prod1 = new ProductionTypeAndDoubledValue ();
		prod1.setProductionTypeID ("RE01");
		prod1.setDoubledProductionValue (2);
		
		final ProductionTypeAndDoubledValue prod2 = new ProductionTypeAndDoubledValue ();
		prod2.setProductionTypeID ("RE02");
		prod2.setDoubledProductionValue (3);
		
		final RacePopulationTask populationTask = new RacePopulationTask ();
		populationTask.setPopulationTaskID ("B");
		populationTask.getRacePopulationTaskProduction ().add (prod1);
		populationTask.getRacePopulationTaskProduction ().add (prod2);
		
		final RaceEx race = new RaceEx ();
		race.getRacePopulationTask ().add (populationTask);
		
		// Building list
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (buildingUtils);
		
		when (buildingUtils.totalBonusProductionPerPersonFromBuildings (buildings, new MapCoordinates3DEx (20, 10, 1), "B", "RE02", db)).thenReturn (2);
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromPopulation (productionValues, race, "B", 5, new MapCoordinates3DEx (20, 10, 1), buildings, db);
		
		// Check results
		assertEquals (2, productionValues.getProductionType ().size ());
		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (10, productionValues.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (25, productionValues.getProductionType ().get (1).getDoubleProductionAmount ());

		assertEquals (1, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().size ());
		assertEquals ("B", productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getPopulationTaskID ());
		assertEquals (5, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getCount ());
		assertEquals (2, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getDoubleProductionAmountEachPopulation ());
		assertEquals (10, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getDoubleProductionAmountAllPopulation ());
		
		assertEquals (1, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().size ());
		assertEquals ("B", productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getPopulationTaskID ());
		assertEquals (5, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getCount ());
		assertEquals (5, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getDoubleProductionAmountEachPopulation ());
		assertEquals (25, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getDoubleProductionAmountAllPopulation ());
	}
	
	/**
	 * Tests the addProductionFromFortressPickType method
	 */
	@Test
	public final void testAddProductionFromFortressPickType ()
	{
		// Mock database
		final ProductionTypeAndDoubledValue prod1 = new ProductionTypeAndDoubledValue ();
		prod1.setProductionTypeID ("RE01");
		prod1.setDoubledProductionValue (2);
		
		final ProductionTypeAndDoubledValue prod2 = new ProductionTypeAndDoubledValue ();
		prod2.setProductionTypeID ("RE02");
		prod2.setDoubledProductionValue (3);
		
		final PickType pickType = new PickType ();
		pickType.setPickTypeID ("B");
		pickType.getFortressPickTypeProduction ().add (prod1);
		pickType.getFortressPickTypeProduction ().add (prod2);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromFortressPickType (productionValues, pickType, 5);
		
		// Check results
		assertEquals (2, productionValues.getProductionType ().size ());
		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (10, productionValues.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (15, productionValues.getProductionType ().get (1).getDoubleProductionAmount ());

		assertEquals (1, productionValues.getProductionType ().get (0).getPickTypeProduction ().size ());
		assertEquals ("B", productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getPickTypeID ());
		assertEquals (5, productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getCount ());
		assertEquals (2, productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getDoubleProductionAmountEachPick ());
		assertEquals (10, productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getDoubleProductionAmountAllPicks ());
		
		assertEquals (1, productionValues.getProductionType ().get (1).getPickTypeProduction ().size ());
		assertEquals ("B", productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getPickTypeID ());
		assertEquals (5, productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getCount ());
		assertEquals (3, productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getDoubleProductionAmountEachPick ());
		assertEquals (15, productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getDoubleProductionAmountAllPicks ());
	}
	
	/**
	 * Tests the addProductionFromFortressPlane method
	 */
	@Test
	public final void testAddProductionFromFortressPlane ()
	{
		// Mock database
		final ProductionTypeAndDoubledValue prod1 = new ProductionTypeAndDoubledValue ();
		prod1.setProductionTypeID ("RE01");
		prod1.setDoubledProductionValue (2);
		
		final ProductionTypeAndDoubledValue prod2 = new ProductionTypeAndDoubledValue ();
		prod2.setProductionTypeID ("RE02");
		prod2.setDoubledProductionValue (3);

		final Plane plane = new Plane ();
		plane.setPlaneNumber (1);
		plane.getFortressPlaneProduction ().add (prod1);
		plane.getFortressPlaneProduction ().add (prod2);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromFortressPlane (productionValues, plane);

		// Check results
		assertEquals (2, productionValues.getProductionType ().size ());
		
		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (2, productionValues.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (1, productionValues.getProductionType ().get (0).getFortressPlane ());
		assertEquals (2, productionValues.getProductionType ().get (0).getDoubleProductionAmountFortressPlane ());
		
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (3, productionValues.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (1, productionValues.getProductionType ().get (1).getFortressPlane ());
		assertEquals (3, productionValues.getProductionType ().get (1).getDoubleProductionAmountFortressPlane ());
	}
	
	/**
	 * Tests the addProductionAndConsumptionFromBuilding method
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Test
	public final void testAddProductionAndConsumptionFromBuilding () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final BuildingPopulationProductionModifier prod = new BuildingPopulationProductionModifier ();
		prod.setProductionTypeID ("RE01");
		prod.setDoubleAmount (2);

		final BuildingPopulationProductionModifier cons = new BuildingPopulationProductionModifier ();
		cons.setProductionTypeID ("RE02");
		cons.setDoubleAmount (-4);

		final BuildingPopulationProductionModifier per = new BuildingPopulationProductionModifier ();
		per.setProductionTypeID ("RE03");
		per.setPercentageBonus (25);
		
		final Building building = new Building ();
		building.setBuildingID ("BL01");
		building.setBuildingUnrestReductionImprovedByRetorts (true);
		building.getBuildingPopulationProductionModifier ().add (prod);
		building.getBuildingPopulationProductionModifier ().add (cons);
		building.getBuildingPopulationProductionModifier ().add (per);
		
		// Player's retorts
		final List<String> pickIDs = new ArrayList<String> ();
		pickIDs.add ("RT01");
		
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		
		final PlayerPickUtils pickUtils = mock (PlayerPickUtils.class);
		when (pickUtils.totalReligiousBuildingBonus (picks, db)).thenReturn (50);
		when (pickUtils.pickIdsContributingToReligiousBuildingBonus (picks, db)).thenReturn (pickIDs);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setPlayerPickUtils (pickUtils);
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionAndConsumptionFromBuilding (productionValues, building, picks, db);
		
		// Check results
		assertEquals (3, productionValues.getProductionType ().size ());
		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (3, productionValues.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (2, productionValues.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals ("RE03", productionValues.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (25, productionValues.getProductionType ().get (2).getPercentageBonus ());
		
		assertEquals (1, productionValues.getProductionType ().get (0).getBuildingBreakdown ().size ());
		assertEquals ("BL01", productionValues.getProductionType ().get (0).getBuildingBreakdown ().get (0).getBuildingID ());
		assertEquals (2, productionValues.getProductionType ().get (0).getBuildingBreakdown ().get (0).getDoubleUnmodifiedProductionAmount ());
		assertEquals (3, productionValues.getProductionType ().get (0).getBuildingBreakdown ().get (0).getDoubleModifiedProductionAmount ());
		assertEquals (50, productionValues.getProductionType ().get (0).getBuildingBreakdown ().get (0).getReligiousBuildingPercentageBonus ());
		assertEquals (1, productionValues.getProductionType ().get (0).getBuildingBreakdown ().get (0).getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals ("RT01", productionValues.getProductionType ().get (0).getBuildingBreakdown ().get (0).getPickIdContributingToReligiousBuildingBonus ().get (0));
		assertEquals ("BL01", productionValues.getProductionType ().get (1).getBuildingBreakdown ().get (0).getBuildingID ());
		assertEquals (2, productionValues.getProductionType ().get (1).getBuildingBreakdown ().get (0).getConsumptionAmount ());
		assertEquals ("BL01", productionValues.getProductionType ().get (2).getBuildingBreakdown ().get (0).getBuildingID ());
		assertEquals (25, productionValues.getProductionType ().get (2).getBuildingBreakdown ().get (0).getPercentageBonus ());
	}
	
	/**
	 * Tests the addProductionFromMapFeatures method
	 * @throws RecordNotFoundException If we encounter a map feature that we cannot find in the cache
	 */
	@Test
	public final void testAddProductionFromMapFeatures () throws RecordNotFoundException
	{
		// Mock database
		final ProductionTypeAndDoubledValue firstProd = new ProductionTypeAndDoubledValue ();
		firstProd.setProductionTypeID ("RE01");
		firstProd.setDoubledProductionValue (3);

		final ProductionTypeAndDoubledValue secondProd = new ProductionTypeAndDoubledValue ();
		secondProd.setProductionTypeID ("RE02");
		secondProd.setDoubledProductionValue (4);
		
		final ProductionTypeAndDoubledValue thirdProd = new ProductionTypeAndDoubledValue ();
		thirdProd.setProductionTypeID ("RE03");
		thirdProd.setDoubledProductionValue (4);
		
		final MapFeatureEx firstFeature = new MapFeatureEx ();
		firstFeature.getMapFeatureProduction ().add (firstProd);
		firstFeature.getMapFeatureProduction ().add (secondProd);
		
		final MapFeatureEx secondFeature = new MapFeatureEx ();
		secondFeature.setRaceMineralMultiplerApplies (true);
		secondFeature.getMapFeatureProduction ().add (thirdProd);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findMapFeature ("MF01", "addProductionFromMapFeatures")).thenReturn (firstFeature);
		when (db.findMapFeature ("MF02", "addProductionFromMapFeatures")).thenReturn (secondFeature);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Add 2 of one type and 3 of another, note MF01 we add 3 of it as well but the top left corner is outside the city radius
		for (int x = 0; x <= 2; x++)
		{
			final OverlandMapTerrainData firstTile = new OverlandMapTerrainData ();
			firstTile.setMapFeatureID ("MF01");
			map.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (firstTile);

			final OverlandMapTerrainData secondTile = new OverlandMapTerrainData ();
			secondTile.setMapFeatureID ("MF02");
			map.getPlane ().get (0).getRow ().get (1).getCell ().get (x).setTerrainData (secondTile);
		}
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromMapFeatures (productionValues, map, new MapCoordinates3DEx (2, 2, 0), sys, db, 2, 50);
		
		// Check results
		assertEquals (3, productionValues.getProductionType ().size ());

		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (6, productionValues.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (1, productionValues.getProductionType ().get (0).getMapFeatureProduction ().size ());
		assertEquals ("MF01", productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (2, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (3, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (6, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (1, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (6, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (0, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (6, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());
		
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (8, productionValues.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (1, productionValues.getProductionType ().get (1).getMapFeatureProduction ().size ());
		assertEquals ("MF01", productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (2, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (4, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (8, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (1, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (8, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (0, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (8, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());

		assertEquals ("RE03", productionValues.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (36, productionValues.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (1, productionValues.getProductionType ().get (2).getMapFeatureProduction ().size ());
		assertEquals ("MF02", productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (3, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (4, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (12, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (2, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (24, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (50, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (36, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());
	}
	
	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method
	 * @throws RecordNotFoundException If we encounter a production type that can't be found in the DB
	 * @throws MomException If we encounter a production value that the DB states should always be an exact multiple of 2, but isn't
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction () throws RecordNotFoundException, MomException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx gold = new ProductionTypeEx ();
		gold.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, "halveAddPercentageBonusAndCapProduction")).thenReturn (gold);
		
		final ProductionTypeEx food = new ProductionTypeEx ();
		food.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, "halveAddPercentageBonusAndCapProduction")).thenReturn (food);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setCityMaxSize (25);
		difficultyLevel.setAiWizardsProductionRateMultiplier (250);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Exact multiple
		final CityProductionBreakdown prod1 = new CityProductionBreakdown ();
		prod1.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		prod1.setDoubleProductionAmount (18);
		prod1.setPercentageBonus (25);
		
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, prod1, difficultyLevel, db);
		
		assertNull (prod1.getRoundingDirectionID ());
		assertEquals (9, prod1.getBaseProductionAmount ());
		assertEquals (11, prod1.getModifiedProductionAmount ());
		assertEquals (11, prod1.getCappedProductionAmount ());
		
		// Round down
		final CityProductionBreakdown prod2 = new CityProductionBreakdown ();
		prod2.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		prod2.setDoubleProductionAmount (19);
		prod2.setPercentageBonus (25);

		calc.halveAddPercentageBonusAndCapProduction (cityOwner, prod2, difficultyLevel, db);
		
		assertEquals (RoundingDirectionID.ROUND_DOWN, prod2.getRoundingDirectionID ());
		assertEquals (9, prod2.getBaseProductionAmount ());
		assertEquals (11, prod2.getModifiedProductionAmount ());
		assertEquals (11, prod2.getCappedProductionAmount ());
		
		// Round up
		gold.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);

		final CityProductionBreakdown prod3 = new CityProductionBreakdown ();
		prod3.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		prod3.setDoubleProductionAmount (19);
		prod3.setPercentageBonus (25);

		calc.halveAddPercentageBonusAndCapProduction (cityOwner, prod3, difficultyLevel, db);
		
		assertEquals (RoundingDirectionID.ROUND_UP, prod3.getRoundingDirectionID ());
		assertEquals (10, prod3.getBaseProductionAmount ());
		assertEquals (12, prod3.getModifiedProductionAmount ());
		assertEquals (12, prod3.getCappedProductionAmount ());
		
		// Value is over cap, but isn't food so cap doesn't apply
		final CityProductionBreakdown prod4 = new CityProductionBreakdown ();
		prod4.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		prod4.setDoubleProductionAmount (41);
		prod4.setPercentageBonus (25);

		calc.halveAddPercentageBonusAndCapProduction (cityOwner, prod4, difficultyLevel, db);
		
		assertEquals (RoundingDirectionID.ROUND_UP, prod4.getRoundingDirectionID ());
		assertEquals (21, prod4.getBaseProductionAmount ());
		assertEquals (26, prod4.getModifiedProductionAmount ());
		assertEquals (26, prod4.getCappedProductionAmount ());

		// Cap applies to food
		final CityProductionBreakdown prod5 = new CityProductionBreakdown ();
		prod5.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		prod5.setDoubleProductionAmount (41);
		prod5.setPercentageBonus (25);

		calc.halveAddPercentageBonusAndCapProduction (cityOwner, prod5, difficultyLevel, db);
		
		assertEquals (RoundingDirectionID.ROUND_UP, prod5.getRoundingDirectionID ());
		assertEquals (21, prod5.getBaseProductionAmount ());
		assertEquals (26, prod5.getModifiedProductionAmount ());
		assertEquals (25, prod5.getCappedProductionAmount ());
	}

	/**
	 * Tests the calculateSingleCityProduction method where the production type does exist in the output from "calculate all"
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateSingleCityProduction_Found () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Map
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		// Mock "calculate all" method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		for (int n = 1; n <= 3; n++)
		{
			final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
			breakdown.setProductionTypeID ("RE0" + n);
			breakdown.setCappedProductionAmount (n * 10);
			breakdown.setConsumptionAmount (n * 2);
			breakdown.setConvertToProductionAmount (3);
			
			productionValues.getProductionType ().add (breakdown);
		}
		
		final CityProductionCalculations prod = mock (CityProductionCalculations.class);
		when (prod.calculateAllCityProductions (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR01", sd, true, false, db)).thenReturn (productionValues);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCityProductionCalculations (prod);
		
		// Check results
		assertEquals (20 - 4 + 3, calc.calculateSingleCityProduction (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR01", sd, true, db, "RE02"));
	}

	/**
	 * Tests the calculateSingleCityProduction method where the production type does not exist in the output from "calculate all"
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateSingleCityProduction_NotFound () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Map
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		// Mock "calculate all" method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		for (int n = 1; n <= 3; n++)
		{
			final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
			breakdown.setProductionTypeID ("RE0" + n);
			breakdown.setCappedProductionAmount (n * 10);
			breakdown.setConsumptionAmount (n * 2);
			breakdown.setConvertToProductionAmount (3);
			
			productionValues.getProductionType ().add (breakdown);
		}
		
		final CityProductionCalculations prod = mock (CityProductionCalculations.class);
		when (prod.calculateAllCityProductions (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR01", sd, true, false, db)).thenReturn (productionValues);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCityProductionCalculations (prod);
		
		// Check results
		assertEquals (0, calc.calculateSingleCityProduction (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR01", sd, true, db, "RE04"));
	}

	/**
	 * Tests the blankBuildingsSoldThisTurn method with specifing a player
	 */
	@Test
	public final void testBlankBuildingsSoldThisTurn_OnePlayer ()
	{
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Fill some cells for one player, some for other player
		for (int x = 1; x <= 3; x++)
			for (int y = 1; y <= 3; y++)
			{
				final OverlandMapCityData cityData = new OverlandMapCityData ();
				cityData.setCityPopulation (1);
				cityData.setCityOwnerID (x+y);

				final MemoryGridCell mc = map.getPlane ().get (0).getRow ().get (y).getCell ().get (x);
				mc.setCityData (cityData);
				mc.setBuildingIdSoldThisTurn ("X");
			}

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();

		// Run method
		calc.blankBuildingsSoldThisTurn (map, 4);

		// Check results
		for (int x = 1; x <= 3; x++)
			for (int y = 1; y <= 3; y++)
				if (x + y == 4)
					assertNull (map.getPlane ().get (0).getRow ().get (y).getCell ().get (x).getBuildingIdSoldThisTurn ());
				else
					assertEquals ("X", map.getPlane ().get (0).getRow ().get (y).getCell ().get (x).getBuildingIdSoldThisTurn ());
	}

	/**
	 * Tests the blankBuildingsSoldThisTurn method for all players
	 */
	@Test
	public final void testBlankBuildingsSoldThisTurn_AllPlayers ()
	{
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Fill some cells for one player, some for other player
		for (int x = 1; x <= 3; x++)
			for (int y = 1; y <= 3; y++)
			{
				final OverlandMapCityData cityData = new OverlandMapCityData ();
				cityData.setCityPopulation (1);
				cityData.setCityOwnerID (x+y);

				final MemoryGridCell mc = map.getPlane ().get (0).getRow ().get (y).getCell ().get (x);
				mc.setCityData (cityData);
				mc.setBuildingIdSoldThisTurn ("BL01");
			}

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();

		// Run method
		calc.blankBuildingsSoldThisTurn (map, 0);

		// Check results
		for (int x = 1; x <= 3; x++)
			for (int y = 1; y <= 3; y++)
				assertNull (map.getPlane ().get (0).getRow ().get (y).getCell ().get (x).getBuildingIdSoldThisTurn ());
	}

	/**
	 * Tests the markWithinExistingCityRadius method
	 */
	@Test
	public final void testMarkWithinExistingCityRadius ()
	{
		// Set up object to test
		final CoordinateSystemUtilsImpl coordinateSystemUtils = new CoordinateSystemUtilsImpl ();
				
		final BooleanMapAreaOperations2DImpl booleanMapAreaOperations2D = new BooleanMapAreaOperations2DImpl ();
		booleanMapAreaOperations2D.setCoordinateSystemUtils (coordinateSystemUtils);
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (coordinateSystemUtils);
		calc.setBooleanMapAreaOperations2D (booleanMapAreaOperations2D);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		
		final MapAreaOperations2DImpl<Boolean> op = new MapAreaOperations2DImpl<Boolean> ();
		op.setCoordinateSystemUtils (coordinateSystemUtils);

		// Coordinate system
		final OverlandMapSize overlandMapSize = new OverlandMapSize ();
		overlandMapSize.setWidth (sys.getWidth ());
		overlandMapSize.setHeight (sys.getHeight ());
		overlandMapSize.setCoordinateSystemType (sys.getCoordinateSystemType ());
		overlandMapSize.setWrapsLeftToRight (sys.isWrapsLeftToRight ());
		overlandMapSize.setWrapsTopToBottom (sys.isWrapsTopToBottom ());

		overlandMapSize.setCitySeparation (3);

		// No cities
		final MapArea2D<Boolean> none = calc.markWithinExistingCityRadius (map, null, 1, overlandMapSize);
		assertEquals (0, op.countCellsEqualTo (none, true));

		// City on the wrong plane
		final OverlandMapCityData wrongPlaneCity = new OverlandMapCityData ();
		wrongPlaneCity.setCityPopulation (1);
		map.getPlane ().get (0).getRow ().get (4).getCell ().get (6).setCityData (wrongPlaneCity);

		final MapArea2D<Boolean> wrongPlane = calc.markWithinExistingCityRadius (map, null, 1, overlandMapSize);
		assertEquals (0, op.countCellsEqualTo (wrongPlane, true));

		// City in the middle of the map
		final OverlandMapCityData oneCity = new OverlandMapCityData ();
		oneCity.setCityPopulation (1);
		map.getPlane ().get (1).getRow ().get (4).getCell ().get (6).setCityData (oneCity);

		final MapArea2D<Boolean> one = calc.markWithinExistingCityRadius (map, null, 1, overlandMapSize);
		assertEquals (49, op.countCellsEqualTo (one, true));

		for (int x = 3; x <= 9; x++)
			for (int y = 1; y <= 7; y++)
				assertTrue (one.get (x, y));

		// 2nd city at top edge of map so some of it gets clipped
		final OverlandMapCityData twoCities = new OverlandMapCityData ();
		twoCities.setCityPopulation (1);
		map.getPlane ().get (1).getRow ().get (1).getCell ().get (16).setCityData (twoCities);

		final MapArea2D<Boolean> two = calc.markWithinExistingCityRadius (map, null, 1, overlandMapSize);
		assertEquals (49 + 35, op.countCellsEqualTo (two, true));

		// 3nd city at left edge of map so some of it gets wrapped
		final OverlandMapCityData threeCities = new OverlandMapCityData ();
		threeCities.setCityPopulation (1);
		map.getPlane ().get (1).getRow ().get (14).getCell ().get (1).setCityData (threeCities);

		final MapArea2D<Boolean> three = calc.markWithinExistingCityRadius (map, null, 1, overlandMapSize);
		assertEquals (49 + 49 + 35, op.countCellsEqualTo (three, true));
	}

	/**
	 * Tests the goldToRushBuy method
	 */
	@Test
	public final void testGoldToRushBuy ()
	{
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		assertEquals (240, calc.goldToRushBuy (60, 0));
		assertEquals (177, calc.goldToRushBuy (60, 1));		// The above 2 are the actual examples in the strategy guide
		
		assertEquals (93, calc.goldToRushBuy (60, 29));
		assertEquals (60, calc.goldToRushBuy (60, 30));
		assertEquals (2, calc.goldToRushBuy (60, 59));
	}
}