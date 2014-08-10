package momime.common.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_5.DifficultyLevelData;
import momime.common.database.newgame.v0_9_5.MapSizeData;
import momime.common.database.v0_9_5.Building;
import momime.common.database.v0_9_5.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_5.FortressPickTypeProduction;
import momime.common.database.v0_9_5.FortressPlaneProduction;
import momime.common.database.v0_9_5.MapFeature;
import momime.common.database.v0_9_5.MapFeatureProduction;
import momime.common.database.v0_9_5.PickType;
import momime.common.database.v0_9_5.Plane;
import momime.common.database.v0_9_5.ProductionType;
import momime.common.database.v0_9_5.Race;
import momime.common.database.v0_9_5.RacePopulationTask;
import momime.common.database.v0_9_5.RacePopulationTaskProduction;
import momime.common.database.v0_9_5.RoundingDirectionID;
import momime.common.database.v0_9_5.TileType;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownDying;
import momime.common.internal.CityGrowthRateBreakdownGrowing;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.OverlandMapTerrainData;
import momime.common.messages.v0_9_5.PlayerPick;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.PlayerPickUtilsImpl;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.areas.operations.MapAreaOperations2DImpl;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the calculations in the MomCityCalculations class
 */
public final class TestMomCityCalculationsImpl
{
	/**
	 * Tests the listCityProductionPercentageBonusesFromTerrainTiles method
	 * @throws RecordNotFoundException If we encounter a tile type that we cannot find in the cache
	 */
	@Test
	public final void testListCityProductionPercentageBonusesFromTerrainTiles () throws RecordNotFoundException
	{
		// Mock database
		final TileType hillsTileType = new TileType ();
		hillsTileType.setProductionBonus (3);

		final TileType mountainsTileType = new TileType ();
		mountainsTileType.setProductionBonus (5);

		final TileType riverTileType = new TileType ();

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "listCityProductionPercentageBonusesFromTerrainTiles")).thenReturn (hillsTileType);
		when (db.findTileType ("TT02", "listCityProductionPercentageBonusesFromTerrainTiles")).thenReturn (mountainsTileType);
		when (db.findTileType ("TT03", "listCityProductionPercentageBonusesFromTerrainTiles")).thenReturn (riverTileType);
		
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
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
		
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, breakdown.getProductionTypeID ());
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

		final TileType centreTileType = new TileType ();
		centreTileType.setGoldBonus (20);
		when (db.findTileType ("TT01", "calculateGoldTradeBonus")).thenReturn (centreTileType);
		
		final TileType adjacentTileType = new TileType ();
		adjacentTileType.setGoldBonus (10);
		when (db.findTileType ("TT02", "calculateGoldTradeBonus")).thenReturn (adjacentTileType);

		final TileType uninterestingTileType = new TileType ();
		when (db.findTileType ("TT03", "calculateGoldTradeBonus")).thenReturn (uninterestingTileType);
		
		final Race uninterestingRace = new Race ();
		when (db.findRace ("RC01", "calculateGoldTradeBonus")).thenReturn (uninterestingRace);
		
		final Race nomads = new Race ();
		nomads.setGoldTradeBonus (50);
		when (db.findRace ("RC02", "calculateGoldTradeBonus")).thenReturn (nomads);
		
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
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
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Building which has no pre-requisites
		assertTrue (calc.buildingPassesTileTypeRequirements (map, cityLocation,
			GenerateTestData.createDB ().findBuilding (GenerateTestData.SAGES_GUILD, "testBuildingPassesTileTypeRequirements_DistanceTwo"), sys));

		// Can't pass yet, since there's no tiles
		final Building building = GenerateTestData.createDB ().findBuilding (GenerateTestData.MINERS_GUILD, "testBuildingPassesTileTypeRequirements_DistanceTwo");
		assertFalse (calc.buildingPassesTileTypeRequirements (map, cityLocation, building, sys));

		// 2 requirements, but its an 'or', so setting one of them should be enough
		final OverlandMapTerrainData mountainsTerrain = new OverlandMapTerrainData ();
		mountainsTerrain.setTileTypeID (GenerateTestData.MOUNTAINS_TILE);
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
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Can't pass yet, since there's no tiles
		final Building building = GenerateTestData.createDB ().findBuilding (GenerateTestData.SHIP_WRIGHTS_GUILD, "testBuildingPassesTileTypeRequirements_DistanceOne");
		assertFalse (calc.buildingPassesTileTypeRequirements (map, cityLocation, building, sys));

		// Putting it 2 tiles away doesn't help
		final OverlandMapTerrainData twoAway = new OverlandMapTerrainData ();
		twoAway.setTileTypeID (GenerateTestData.RIVER_TILE);
		map.getPlane ().get (0).getRow ().get (1).getCell ().get (4).setTerrainData (twoAway);
		assertFalse (calc.buildingPassesTileTypeRequirements (map, cityLocation, building, sys));

		// Putting it 1 tile away does
		final OverlandMapTerrainData oneAway = new OverlandMapTerrainData ();
		oneAway.setTileTypeID (GenerateTestData.RIVER_TILE);
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
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Session description
		final CoordinateSystem overlandMapCoordinateSystem = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// 0 so far
		final CityProductionBreakdown breakdown1 = calc.listCityFoodProductionFromTerrainTiles (map, cityLocation, overlandMapCoordinateSystem, GenerateTestData.createDB ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, breakdown1.getProductionTypeID ());
		assertEquals (0, breakdown1.getDoubleProductionAmount ());
		assertEquals (0, breakdown1.getTileTypeProduction ().size ());

		// Add some tile types that grant food, 3 hills and 5 rivers, (3*1) + (4*5) = 23
		// Note we add hills in the NW and NE corners too, outside the city radius, so we prove that this isn't counted
		// Also adds some mountains, which don't produce any food at all, to prove they don't get included in the breakdown
		for (int x = 0; x <= 4; x++)
		{
			final OverlandMapTerrainData hillsTerrain = new OverlandMapTerrainData ();
			hillsTerrain.setTileTypeID (GenerateTestData.HILLS_TILE);
			map.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
			riverTile.setTileTypeID (GenerateTestData.RIVER_TILE);
			map.getPlane ().get (0).getRow ().get (1).getCell ().get (x).setTerrainData (riverTile);

			final OverlandMapTerrainData mountainsTile = new OverlandMapTerrainData ();
			mountainsTile.setTileTypeID (GenerateTestData.MOUNTAINS_TILE);
			map.getPlane ().get (0).getRow ().get (2).getCell ().get (x).setTerrainData (mountainsTile);
		}

		final CityProductionBreakdown breakdown2 = calc.listCityFoodProductionFromTerrainTiles (map, cityLocation, overlandMapCoordinateSystem, GenerateTestData.createDB ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, breakdown2.getProductionTypeID ());
		assertEquals (23, breakdown2.getDoubleProductionAmount ());
		assertEquals (2, breakdown2.getTileTypeProduction ().size ());
		assertEquals (GenerateTestData.RIVER_TILE, breakdown2.getTileTypeProduction ().get (0).getTileTypeID ());
		assertEquals (5, breakdown2.getTileTypeProduction ().get (0).getCount ());
		assertEquals (4, breakdown2.getTileTypeProduction ().get (0).getDoubleProductionAmountEachTile ());
		assertEquals (20, breakdown2.getTileTypeProduction ().get (0).getDoubleProductionAmountAllTiles ());
		assertEquals (GenerateTestData.HILLS_TILE, breakdown2.getTileTypeProduction ().get (1).getTileTypeID ());
		assertEquals (3, breakdown2.getTileTypeProduction ().get (1).getCount ());
		assertEquals (1, breakdown2.getTileTypeProduction ().get (1).getDoubleProductionAmountEachTile ());
		assertEquals (3, breakdown2.getTileTypeProduction ().get (1).getDoubleProductionAmountAllTiles ());
	}

	/**
	 * Tests the calculateCityGrowthRate method
	 * @throws RecordNotFoundException If we encounter a race or building that can't be found in the cache
	 */
	@Test
	public final void testCalculateCityGrowthRate () throws RecordNotFoundException
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID (GenerateTestData.HIGH_MEN);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// At max size
		cityData.setCityPopulation (10000);
		final CityGrowthRateBreakdown maximum = calc.calculateCityGrowthRate (map, buildings, cityLocation, 10, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdown.class.getName (), maximum.getClass ().getName ());
		assertEquals (10000, maximum.getCurrentPopulation ());
		assertEquals (10000, maximum.getMaximumPopulation ());
		assertEquals (0, maximum.getFinalTotal ());

		// Growing (this is the example quoted in the strategy guide, however note the example is in contradiction with the formula - from testing I believe the example is right and the formula is supposed to be a -1 not a +1)
		cityData.setCityPopulation (12000);
		final CityGrowthRateBreakdown growingEvenBreakdown = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), growingEvenBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing growingEven = (CityGrowthRateBreakdownGrowing) growingEvenBreakdown;
		assertEquals (12000, growingEven.getCurrentPopulation ());
		assertEquals (22000, growingEven.getMaximumPopulation ());
		assertEquals (50, growingEven.getBaseGrowthRate ());
		assertEquals (0, growingEven.getRacialGrowthModifier ());
		assertEquals (0, growingEven.getBuildingModifier ().size ());
		assertEquals (50, growingEven.getTotalGrowthRate ());
		assertEquals (50, growingEven.getCappedGrowthRate ());
		assertEquals (50, growingEven.getFinalTotal ());

		final CityGrowthRateBreakdown growingOddBreakdown = calc.calculateCityGrowthRate (map, buildings, cityLocation, 23, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), growingOddBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing growingOdd = (CityGrowthRateBreakdownGrowing) growingOddBreakdown;
		assertEquals (12000, growingOdd.getCurrentPopulation ());
		assertEquals (23000, growingOdd.getMaximumPopulation ());
		assertEquals (50, growingOdd.getBaseGrowthRate ());
		assertEquals (0, growingOdd.getRacialGrowthModifier ());
		assertEquals (0, growingOdd.getBuildingModifier ().size ());
		assertEquals (50, growingOdd.getTotalGrowthRate ());
		assertEquals (50, growingOdd.getCappedGrowthRate ());
		assertEquals (50, growingOdd.getFinalTotal ());

		// Bonus from race - positive
		cityData.setCityRaceID (GenerateTestData.BARBARIAN);
		final CityGrowthRateBreakdown barbarianBreakdown = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), barbarianBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing barbarian = (CityGrowthRateBreakdownGrowing) barbarianBreakdown;
		assertEquals (12000, barbarian.getCurrentPopulation ());
		assertEquals (22000, barbarian.getMaximumPopulation ());
		assertEquals (50, barbarian.getBaseGrowthRate ());
		assertEquals (20, barbarian.getRacialGrowthModifier ());
		assertEquals (0, barbarian.getBuildingModifier ().size ());
		assertEquals (70, barbarian.getTotalGrowthRate ());
		assertEquals (70, barbarian.getCappedGrowthRate ());
		assertEquals (70, barbarian.getFinalTotal ());

		// Bonus from race - negative
		cityData.setCityRaceID (GenerateTestData.HIGH_ELF);
		final CityGrowthRateBreakdown highElfBreakdown = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), highElfBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing highElf = (CityGrowthRateBreakdownGrowing) highElfBreakdown;
		assertEquals (12000, highElf.getCurrentPopulation ());
		assertEquals (22000, highElf.getMaximumPopulation ());
		assertEquals (50, highElf.getBaseGrowthRate ());
		assertEquals (-20, highElf.getRacialGrowthModifier ());
		assertEquals (0, highElf.getBuildingModifier ().size ());
		assertEquals (30, highElf.getTotalGrowthRate ());
		assertEquals (30, highElf.getCappedGrowthRate ());
		assertEquals (30, highElf.getFinalTotal ());

		// Bonus from buildings
		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID (GenerateTestData.GRANARY);
		granary.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (granary);

		final MemoryBuilding farmersMarket = new MemoryBuilding ();
		farmersMarket.setBuildingID (GenerateTestData.FARMERS_MARKET);
		farmersMarket.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (farmersMarket);

		final MemoryBuilding sagesGuild = new MemoryBuilding ();		// Irrelevant building, to prove it doesn't get included in the list
		sagesGuild.setBuildingID (GenerateTestData.SAGES_GUILD);
		sagesGuild.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (sagesGuild);

		final CityGrowthRateBreakdown withBuildingsBreakdown = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), withBuildingsBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing withBuildings = (CityGrowthRateBreakdownGrowing) withBuildingsBreakdown;
		assertEquals (12000, withBuildings.getCurrentPopulation ());
		assertEquals (22000, withBuildings.getMaximumPopulation ());
		assertEquals (50, withBuildings.getBaseGrowthRate ());
		assertEquals (-20, withBuildings.getRacialGrowthModifier ());
		assertEquals (2, withBuildings.getBuildingModifier ().size ());
		assertEquals (GenerateTestData.GRANARY, withBuildings.getBuildingModifier ().get (0).getBuildingID ());
		assertEquals (20, withBuildings.getBuildingModifier ().get (0).getGrowthRateBonus ());
		assertEquals (GenerateTestData.FARMERS_MARKET, withBuildings.getBuildingModifier ().get (1).getBuildingID ());
		assertEquals (30, withBuildings.getBuildingModifier ().get (1).getGrowthRateBonus ());
		assertEquals (80, withBuildings.getTotalGrowthRate ());
		assertEquals (80, withBuildings.getCappedGrowthRate ());
		assertEquals (80, withBuildings.getFinalTotal ());

		// With all those buildings, at almost max size we still get a reasonable increase
		cityData.setCityPopulation (21960);
		final CityGrowthRateBreakdown almostCappedBreakdown = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), almostCappedBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing almostCapped = (CityGrowthRateBreakdownGrowing) almostCappedBreakdown;
		assertEquals (21960, almostCapped.getCurrentPopulation ());
		assertEquals (22000, almostCapped.getMaximumPopulation ());
		assertEquals (0, almostCapped.getBaseGrowthRate ());
		assertEquals (-20, almostCapped.getRacialGrowthModifier ());
		assertEquals (2, almostCapped.getBuildingModifier ().size ());
		assertEquals (GenerateTestData.GRANARY, almostCapped.getBuildingModifier ().get (0).getBuildingID ());
		assertEquals (20, almostCapped.getBuildingModifier ().get (0).getGrowthRateBonus ());
		assertEquals (GenerateTestData.FARMERS_MARKET, almostCapped.getBuildingModifier ().get (1).getBuildingID ());
		assertEquals (30, almostCapped.getBuildingModifier ().get (1).getGrowthRateBonus ());
		assertEquals (30, almostCapped.getTotalGrowthRate ());
		assertEquals (30, almostCapped.getCappedGrowthRate ());
		assertEquals (30, almostCapped.getFinalTotal ());

		// +30 with only 20 to spare would push us over max size
		cityData.setCityPopulation (21980);
		final CityGrowthRateBreakdown overCapBreakdown = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), overCapBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing overCap = (CityGrowthRateBreakdownGrowing) overCapBreakdown;
		assertEquals (21980, overCap.getCurrentPopulation ());
		assertEquals (22000, overCap.getMaximumPopulation ());
		assertEquals (0, overCap.getBaseGrowthRate ());
		assertEquals (-20, overCap.getRacialGrowthModifier ());
		assertEquals (2, overCap.getBuildingModifier ().size ());
		assertEquals (GenerateTestData.GRANARY, overCap.getBuildingModifier ().get (0).getBuildingID ());
		assertEquals (20, overCap.getBuildingModifier ().get (0).getGrowthRateBonus ());
		assertEquals (GenerateTestData.FARMERS_MARKET, overCap.getBuildingModifier ().get (1).getBuildingID ());
		assertEquals (30, overCap.getBuildingModifier ().get (1).getGrowthRateBonus ());
		assertEquals (30, overCap.getTotalGrowthRate ());
		assertEquals (20, overCap.getCappedGrowthRate ());
		assertEquals (20, overCap.getFinalTotal ());

		// Dying - note the race and building modifiers don't apply, because we can't by virtue of bonuses force a city to go over max size
		final CityGrowthRateBreakdown dyingBreakdown = calc.calculateCityGrowthRate (map, buildings, cityLocation, 18, GenerateTestData.createDB ());
		assertEquals (CityGrowthRateBreakdownDying.class.getName (), dyingBreakdown.getClass ().getName ());
		final CityGrowthRateBreakdownDying dying = (CityGrowthRateBreakdownDying) dyingBreakdown;
		assertEquals (21980, dying.getCurrentPopulation ());
		assertEquals (18000, dying.getMaximumPopulation ());
		assertEquals (3, dying.getBaseDeathRate ());
		assertEquals (150, dying.getCityDeathRate ());
		assertEquals (-150, dying.getFinalTotal ());
	}

	/**
	 * Tests the calculateCityRebels method
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If any of a number of items cannot be found in the cache
	 */
	@Test
	public final void testCalculateCityRebels () throws PlayerNotFoundException, RecordNotFoundException
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		calc.setPlayerPickUtils (new PlayerPickUtilsImpl ());
		
		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID (GenerateTestData.KLACKONS);
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails ppd = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (ppd);

		// Tax rate with no rebels!  and no gold...
		final CityUnrestBreakdown zeroPercent = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_0_GOLD_0_UNREST, GenerateTestData.createDB ());
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
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
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
		final PlayerPick divinePower = new PlayerPick ();
		divinePower.setPickID (GenerateTestData.DIVINE_POWER);
		divinePower.setQuantity (1);
		ppk.getPick ().add (divinePower);
		
		final CityUnrestBreakdown maxPercent = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_3_GOLD_75_UNREST, GenerateTestData.createDB ());
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
		shrineBuilding.setBuildingID (GenerateTestData.SHRINE);
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (shrineBuilding);

		ppk.getPick ().remove (divinePower);
		
		final CityUnrestBreakdown shrine = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, shrine.getPopulation ());
		assertEquals (45, shrine.getTaxPercentage ());
		assertEquals (0, shrine.getRacialPercentage ());
		assertEquals (0, shrine.getRacialLiteral ());
		assertEquals (45, shrine.getTotalPercentage ());
		assertEquals (7, shrine.getBaseValue ());
		assertEquals (1, shrine.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, shrine.getBuildingReducingUnrest ().get (0).getBuildingID ());
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
		ppk.getPick ().add (divinePower);

		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID (GenerateTestData.ANIMISTS_GUILD);
		secondBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (secondBuilding);

		final CityUnrestBreakdown animistsGuild = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, animistsGuild.getPopulation ());
		assertEquals (45, animistsGuild.getTaxPercentage ());
		assertEquals (0, animistsGuild.getRacialPercentage ());
		assertEquals (0, animistsGuild.getRacialLiteral ());
		assertEquals (45, animistsGuild.getTotalPercentage ());
		assertEquals (7, animistsGuild.getBaseValue ());
		assertEquals (2, animistsGuild.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, animistsGuild.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, animistsGuild.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.ANIMISTS_GUILD, animistsGuild.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, animistsGuild.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, animistsGuild.getReligiousBuildingRetortPercentage ());						// Now the 50% comes out, because of the shrine
		assertEquals (1, animistsGuild.getPickIdContributingToReligiousBuildingBonus ().size ());	// Likewise
		assertEquals (GenerateTestData.DIVINE_POWER, animistsGuild.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
		secondBuilding.setBuildingID (GenerateTestData.TEMPLE);
		final CityUnrestBreakdown temple = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, temple.getPopulation ());
		assertEquals (45, temple.getTaxPercentage ());
		assertEquals (0, temple.getRacialPercentage ());
		assertEquals (0, temple.getRacialLiteral ());
		assertEquals (45, temple.getTotalPercentage ());
		assertEquals (7, temple.getBaseValue ());
		assertEquals (2, temple.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, temple.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, temple.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, temple.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, temple.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, temple.getReligiousBuildingRetortPercentage ());
		assertEquals (1, temple.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (GenerateTestData.DIVINE_POWER, temple.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
		normalUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		normalUnit.setUnitLocation (new MapCoordinates3DEx (2, 2, 0));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final CityUnrestBreakdown firstUnit = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, firstUnit.getPopulation ());
		assertEquals (45, firstUnit.getTaxPercentage ());
		assertEquals (0, firstUnit.getRacialPercentage ());
		assertEquals (0, firstUnit.getRacialLiteral ());
		assertEquals (45, firstUnit.getTotalPercentage ());
		assertEquals (7, firstUnit.getBaseValue ());
		assertEquals (2, firstUnit.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, firstUnit.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, firstUnit.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, firstUnit.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, firstUnit.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, firstUnit.getReligiousBuildingRetortPercentage ());
		assertEquals (1, firstUnit.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (GenerateTestData.DIVINE_POWER, firstUnit.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
		heroUnit.setUnitID (GenerateTestData.DWARF_HERO);
		heroUnit.setUnitLocation (new MapCoordinates3DEx (2, 2, 0));
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);

		final CityUnrestBreakdown secondUnit = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, secondUnit.getPopulation ());
		assertEquals (45, secondUnit.getTaxPercentage ());
		assertEquals (0, secondUnit.getRacialPercentage ());
		assertEquals (0, secondUnit.getRacialLiteral ());
		assertEquals (45, secondUnit.getTotalPercentage ());
		assertEquals (7, secondUnit.getBaseValue ());
		assertEquals (2, secondUnit.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, secondUnit.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, secondUnit.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, secondUnit.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, secondUnit.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, secondUnit.getReligiousBuildingRetortPercentage ());
		assertEquals (1, secondUnit.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (GenerateTestData.DIVINE_POWER, secondUnit.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
			deadUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
			deadUnit.setUnitLocation (new MapCoordinates3DEx (2, 2, 0));
			deadUnit.setStatus (UnitStatusID.DEAD);
			units.add (deadUnit);

			final MemoryUnit summonedUnit = new MemoryUnit ();
			summonedUnit.setUnitID (GenerateTestData.WAR_BEARS_UNIT);
			summonedUnit.setUnitLocation (new MapCoordinates3DEx (2, 2, 0));
			summonedUnit.setStatus (UnitStatusID.ALIVE);
			units.add (summonedUnit);
		}

		final CityUnrestBreakdown extraUnits = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, extraUnits.getPopulation ());
		assertEquals (45, extraUnits.getTaxPercentage ());
		assertEquals (0, extraUnits.getRacialPercentage ());
		assertEquals (0, extraUnits.getRacialLiteral ());
		assertEquals (45, extraUnits.getTotalPercentage ());
		assertEquals (7, extraUnits.getBaseValue ());
		assertEquals (2, extraUnits.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, extraUnits.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, extraUnits.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, extraUnits.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, extraUnits.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, extraUnits.getReligiousBuildingRetortPercentage ());
		assertEquals (1, extraUnits.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (GenerateTestData.DIVINE_POWER, extraUnits.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
		fortressBuilding.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		buildings.add (fortressBuilding);

		final CityUnrestBreakdown klackons = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, klackons.getPopulation ());
		assertEquals (45, klackons.getTaxPercentage ());
		assertEquals (0, klackons.getRacialPercentage ());
		assertEquals (-2, klackons.getRacialLiteral ());
		assertEquals (45, klackons.getTotalPercentage ());
		assertEquals (7, klackons.getBaseValue ());
		assertEquals (2, klackons.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, klackons.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, klackons.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, klackons.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, klackons.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, klackons.getReligiousBuildingRetortPercentage ());
		assertEquals (1, klackons.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (GenerateTestData.DIVINE_POWER, klackons.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
		cityData.setCityRaceID (GenerateTestData.HIGH_ELF);

		final CityUnrestBreakdown highElves = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, highElves.getPopulation ());
		assertEquals (45, highElves.getTaxPercentage ());
		assertEquals (0, highElves.getRacialPercentage ());
		assertEquals (0, highElves.getRacialLiteral ());
		assertEquals (45, highElves.getTotalPercentage ());
		assertEquals (7, highElves.getBaseValue ());
		assertEquals (2, highElves.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, highElves.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, highElves.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, highElves.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, highElves.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, highElves.getReligiousBuildingRetortPercentage ());
		assertEquals (1, highElves.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (GenerateTestData.DIVINE_POWER, highElves.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_0_GOLD_0_UNREST, GenerateTestData.createDB ());
		assertEquals (17, forcePositive.getPopulation ());
		assertEquals (0, forcePositive.getTaxPercentage ());
		assertEquals (0, forcePositive.getRacialPercentage ());
		assertEquals (0, forcePositive.getRacialLiteral ());
		assertEquals (0, forcePositive.getTotalPercentage ());
		assertEquals (0, forcePositive.getBaseValue ());
		assertEquals (2, forcePositive.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, forcePositive.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, forcePositive.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, forcePositive.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, forcePositive.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, forcePositive.getReligiousBuildingRetortPercentage ());
		assertEquals (1, forcePositive.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (GenerateTestData.DIVINE_POWER, forcePositive.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
		capitalCityData.setCityRaceID (GenerateTestData.DWARVES);
		capitalCityData.setCityOwnerID (1);
		capitalCityData.setCityPopulation (1000);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (20).setCityData (capitalCityData);

		fortressBuilding.setCityLocation (new MapCoordinates3DEx (20, 2, 0));

		final CityUnrestBreakdown racialUnrest = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, racialUnrest.getPopulation ());
		assertEquals (45, racialUnrest.getTaxPercentage ());
		assertEquals (30, racialUnrest.getRacialPercentage ());
		assertEquals (0, racialUnrest.getRacialLiteral ());
		assertEquals (75, racialUnrest.getTotalPercentage ());
		assertEquals (12, racialUnrest.getBaseValue ());
		assertEquals (2, racialUnrest.getBuildingReducingUnrest ().size ());
		assertEquals (GenerateTestData.SHRINE, racialUnrest.getBuildingReducingUnrest ().get (0).getBuildingID ());
		assertEquals (1, racialUnrest.getBuildingReducingUnrest ().get (0).getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, racialUnrest.getBuildingReducingUnrest ().get (1).getBuildingID ());
		assertEquals (1, racialUnrest.getBuildingReducingUnrest ().get (1).getUnrestReduction ());
		assertEquals (50, racialUnrest.getReligiousBuildingRetortPercentage ());
		assertEquals (1, racialUnrest.getPickIdContributingToReligiousBuildingBonus ().size ());
		assertEquals (GenerateTestData.DIVINE_POWER, racialUnrest.getPickIdContributingToReligiousBuildingBonus ().get (0));
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
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_3_GOLD_75_UNREST, GenerateTestData.createDB ());
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
		
		final RacePopulationTaskProduction prod1 = new RacePopulationTaskProduction ();
		prod1.setProductionTypeID ("RE01");
		prod1.setDoubleAmount (2);
		
		final RacePopulationTaskProduction prod2 = new RacePopulationTaskProduction ();
		prod2.setProductionTypeID ("RE02");
		prod2.setDoubleAmount (3);
		
		final RacePopulationTask populationTask = new RacePopulationTask ();
		populationTask.setPopulationTaskID ("B");
		populationTask.getRacePopulationTaskProduction ().add (prod1);
		populationTask.getRacePopulationTaskProduction ().add (prod2);
		
		final Race race = new Race ();
		race.getRacePopulationTask ().add (populationTask);
		
		// Building list
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
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
		final FortressPickTypeProduction prod1 = new FortressPickTypeProduction ();
		prod1.setFortressProductionTypeID ("RE01");
		prod1.setDoubleAmount (2);
		
		final FortressPickTypeProduction prod2 = new FortressPickTypeProduction ();
		prod2.setFortressProductionTypeID ("RE02");
		prod2.setDoubleAmount (3);
		
		final PickType pickType = new PickType ();
		pickType.setPickTypeID ("B");
		pickType.getFortressPickTypeProduction ().add (prod1);
		pickType.getFortressPickTypeProduction ().add (prod2);
		
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		
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
		final FortressPlaneProduction prod1 = new FortressPlaneProduction ();
		prod1.setFortressProductionTypeID ("RE01");
		prod1.setDoubleAmount (2);
		
		final FortressPlaneProduction prod2 = new FortressPlaneProduction ();
		prod2.setFortressProductionTypeID ("RE02");
		prod2.setDoubleAmount (3);

		final Plane plane = new Plane ();
		plane.setPlaneNumber (1);
		plane.getFortressPlaneProduction ().add (prod1);
		plane.getFortressPlaneProduction ().add (prod2);
		
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		
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
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
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
		final MapFeatureProduction firstProd = new MapFeatureProduction ();
		firstProd.setProductionTypeID ("RE01");
		firstProd.setDoubleAmount (3);

		final MapFeatureProduction secondProd = new MapFeatureProduction ();
		secondProd.setProductionTypeID ("RE02");
		secondProd.setDoubleAmount (4);
		
		final MapFeatureProduction thirdProd = new MapFeatureProduction ();
		thirdProd.setProductionTypeID ("RE03");
		thirdProd.setDoubleAmount (4);
		
		final MapFeature firstFeature = new MapFeature ();
		firstFeature.getMapFeatureProduction ().add (firstProd);
		firstFeature.getMapFeatureProduction ().add (secondProd);
		
		final MapFeature secondFeature = new MapFeature ();
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
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromMapFeatures (productionValues, map, new MapCoordinates3DEx (2, 2, 0), sys, db, 2, 50);
		
		// Check results
		assertEquals (3, productionValues.getProductionType ().size ());
		
		assertEquals ("RE03", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (36, productionValues.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (1, productionValues.getProductionType ().get (0).getMapFeatureProduction ().size ());
		assertEquals ("MF02", productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (3, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (4, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (12, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (2, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (24, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (50, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (36, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());
		
		assertEquals ("RE01", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (6, productionValues.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (1, productionValues.getProductionType ().get (1).getMapFeatureProduction ().size ());
		assertEquals ("MF01", productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (2, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (3, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (6, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (1, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (6, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (0, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (6, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());
				
		assertEquals ("RE02", productionValues.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (8, productionValues.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (1, productionValues.getProductionType ().get (2).getMapFeatureProduction ().size ());
		assertEquals ("MF01", productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (2, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (4, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (8, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (1, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (8, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (0, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (8, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());
	}

	/**
	 * Tests the calculateAllCityProductions method
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Test
	public final void testCalculateAllCityProductions () throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		calc.setPlayerPickUtils (new PlayerPickUtilsImpl ());
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
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
			hillsTerrain.setTileTypeID (GenerateTestData.HILLS_TILE);
			map.getPlane ().get (1).getRow ().get (0).getCell ().get (x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
			riverTile.setTileTypeID (GenerateTestData.RIVER_TILE);
			map.getPlane ().get (1).getRow ().get (1).getCell ().get (x).setTerrainData (riverTile);
		}

		// Put river right on the city too, to get the gold bonus
		final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
		riverTile.setTileTypeID (GenerateTestData.RIVER_TILE);
		map.getPlane ().get (1).getRow ().get (2).getCell ().get (2).setTerrainData (riverTile);

		// Add some wild game
		for (int y = 0; y <= 1; y++)
			map.getPlane ().get (1).getRow ().get (y).getCell ().get (2).getTerrainData ().setMapFeatureID (GenerateTestData.WILD_GAME);

		// Session description
		final MapSizeData mapSize = new MapSizeData ();
		mapSize.setWidth (sys.getWidth ());
		mapSize.setHeight (sys.getHeight ());
		mapSize.setCoordinateSystemType (sys.getCoordinateSystemType ());
		mapSize.setWrapsLeftToRight (sys.isWrapsLeftToRight ());
		mapSize.setWrapsTopToBottom (sys.isWrapsTopToBottom ());

		final DifficultyLevelData dl = new DifficultyLevelData ();
		dl.setCityMaxSize (25);

		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		sd.setDifficultyLevel (dl);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID (GenerateTestData.HIGH_MEN);
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		cityData.setOptionalFarmers (2);
		cityData.setNumberOfRebels (2);		// 17 -6 -2 -2 = 7 workers
		map.getPlane ().get (1).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails ppd = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (ppd);

		// So far all we have are the basic production types:
		// a) production from the population, bumped up by the % bonus from terrain
		// b) max city size
		// c) people eating food
		// d) gold from taxes
		final CityProductionBreakdownsEx baseNoPeople = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, false, false, GenerateTestData.createDB ());
		assertEquals (4, baseNoPeople.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, baseNoPeople.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (8, baseNoPeople.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (4, baseNoPeople.getProductionType ().get (0).getBaseProductionAmount ());				// 2 x2 from wild game = 4
		assertEquals (0, baseNoPeople.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (4, baseNoPeople.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (0).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, baseNoPeople.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (0, baseNoPeople.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (1).getBaseProductionAmount ());
		assertEquals (9, baseNoPeople.getProductionType ().get (1).getPercentageBonus ());						// 3 hills giving 3% each
		assertEquals (0, baseNoPeople.getProductionType ().get (1).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, baseNoPeople.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (0, baseNoPeople.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (2).getBaseProductionAmount ());
		assertEquals (20, baseNoPeople.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (0, baseNoPeople.getProductionType ().get (2).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, baseNoPeople.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (35, baseNoPeople.getProductionType ().get (3).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, baseNoPeople.getProductionType ().get (3).getBaseProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (18, baseNoPeople.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getProductionType ().get (3).getConsumptionAmount ());

		final CityProductionBreakdownsEx baseWithPeople = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (4, baseWithPeople.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, baseWithPeople.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, baseWithPeople.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, baseWithPeople.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, baseWithPeople.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, baseWithPeople.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, baseWithPeople.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, baseWithPeople.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, baseWithPeople.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, baseWithPeople.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x �) + (7 workers x 2) = 18
		assertEquals (9, baseWithPeople.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each
		assertEquals (19, baseWithPeople.getProductionType ().get (1).getModifiedProductionAmount ());	// 18 * 1.09 = 19.62
		assertEquals (0, baseWithPeople.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, baseWithPeople.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (60, baseWithPeople.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (30, baseWithPeople.getProductionType ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, baseWithPeople.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (36, baseWithPeople.getProductionType ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (0, baseWithPeople.getProductionType ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, baseWithPeople.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (35, baseWithPeople.getProductionType ().get (3).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, baseWithPeople.getProductionType ().get (3).getBaseProductionAmount ());
		assertEquals (0, baseWithPeople.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (18, baseWithPeople.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, baseWithPeople.getProductionType ().get (3).getConsumptionAmount ());

		// Wizard fortress produces +1 mana per book, and +5 for being on myrror
		final PlayerPick lifeBook = new PlayerPick ();
		lifeBook.setPickID (GenerateTestData.LIFE_BOOK);
		lifeBook.setOriginalQuantity (8);
		lifeBook.setQuantity (9);
		ppk.getPick ().add (lifeBook);

		final PlayerPick summoner = new PlayerPick ();
		summoner.setPickID (GenerateTestData.SUMMONER);
		summoner.setOriginalQuantity (1);
		summoner.setQuantity (1);
		ppk.getPick ().add (summoner);

		final MemoryBuilding fortressBuilding = new MemoryBuilding ();
		fortressBuilding.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 1));
		buildings.add (fortressBuilding);

		final CityProductionBreakdownsEx fortress = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (5, fortress.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, fortress.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, fortress.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, fortress.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, fortress.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, fortress.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, fortress.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, fortress.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, fortress.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, fortress.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x �) + (7 workers x 2) = 18
		assertEquals (9, fortress.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each
		assertEquals (19, fortress.getProductionType ().get (1).getModifiedProductionAmount ());	// 18 * 1.09 = 19.62
		assertEquals (0, fortress.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, fortress.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (60, fortress.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (30, fortress.getProductionType ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, fortress.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (36, fortress.getProductionType ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (0, fortress.getProductionType ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, fortress.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (26, fortress.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (13, fortress.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books + 5 for being on myrror = 13
		assertEquals (0, fortress.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (13, fortress.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, fortress.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, fortress.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (35, fortress.getProductionType ().get (4).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, fortress.getProductionType ().get (4).getBaseProductionAmount ());
		assertEquals (0, fortress.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (18, fortress.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, fortress.getProductionType ().get (4).getConsumptionAmount ());

		// Add some buildings that give production (both regular like sages guild, and percentage like sawmill), and consumption
		final MemoryBuilding sagesGuildBuilding = new MemoryBuilding ();
		sagesGuildBuilding.setBuildingID (GenerateTestData.SAGES_GUILD);
		sagesGuildBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 1));
		buildings.add (sagesGuildBuilding);

		final MemoryBuilding sawmillBuilding = new MemoryBuilding ();
		sawmillBuilding.setBuildingID (GenerateTestData.SAWMILL);
		sawmillBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 1));
		buildings.add (sawmillBuilding);

		final CityProductionBreakdownsEx sawmill = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (6, sawmill.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, sawmill.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, sawmill.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, sawmill.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, sawmill.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, sawmill.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, sawmill.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, sawmill.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, sawmill.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, sawmill.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x �) + (7 workers x 2) = 18
		assertEquals (34, sawmill.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill
		assertEquals (24, sawmill.getProductionType ().get (1).getModifiedProductionAmount ());	// 18 * 1.34 = 24.12
		assertEquals (0, sawmill.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, sawmill.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (60, sawmill.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (30, sawmill.getProductionType ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, sawmill.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (36, sawmill.getProductionType ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (4, sawmill.getProductionType ().get (2).getConsumptionAmount ());				// 2 buildings costing 2 gold each
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, sawmill.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (26, sawmill.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (13, sawmill.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books + 5 for being on myrror = 13
		assertEquals (0, sawmill.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (13, sawmill.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, sawmill.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, sawmill.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, sawmill.getProductionType ().get (4).getBaseProductionAmount ());			// 3 from sages' guild
		assertEquals (0, sawmill.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, sawmill.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, sawmill.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, sawmill.getProductionType ().get (5).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, sawmill.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, sawmill.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, sawmill.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getProductionType ().get (5).getConsumptionAmount ());

		// Add some map features, note there's already 2 wild game been added above
		map.getPlane ().get (1).getRow ().get (0).getCell ().get (3).getTerrainData ().setMapFeatureID (GenerateTestData.GEMS);
		map.getPlane ().get (1).getRow ().get (1).getCell ().get (3).getTerrainData ().setMapFeatureID (GenerateTestData.ADAMANTIUM_ORE);

		final CityProductionBreakdownsEx minerals = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (6, minerals.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, minerals.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, minerals.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, minerals.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, minerals.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, minerals.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, minerals.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, minerals.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, minerals.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, minerals.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x �) + (7 workers x 2) = 18
		assertEquals (34, minerals.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from minerals
		assertEquals (24, minerals.getProductionType ().get (1).getModifiedProductionAmount ());		// 18 * 1.34 = 24.12
		assertEquals (0, minerals.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, minerals.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (70, minerals.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (35, minerals.getProductionType ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +5 from gems = 35
		assertEquals (20, minerals.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (42, minerals.getProductionType ().get (2).getModifiedProductionAmount ());		// 35 * 1.2 = 42
		assertEquals (4, minerals.getProductionType ().get (2).getConsumptionAmount ());					// 2 buildings costing 2 gold each
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, minerals.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (30, minerals.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (15, minerals.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +2 from adamantium = 15
		assertEquals (0, minerals.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (15, minerals.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, minerals.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, minerals.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, minerals.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, minerals.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, minerals.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, minerals.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, minerals.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, minerals.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, minerals.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, minerals.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, minerals.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, minerals.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, minerals.getProductionType ().get (5).getConsumptionAmount ());

		// Miners' guild boosting bonuses from map features
		final MemoryBuilding minersGuildBuilding = new MemoryBuilding ();
		minersGuildBuilding.setBuildingID (GenerateTestData.MINERS_GUILD);
		minersGuildBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 1));
		buildings.add (minersGuildBuilding);

		final CityProductionBreakdownsEx minersGuild = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (7, minersGuild.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, minersGuild.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, minersGuild.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, minersGuild.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, minersGuild.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, minersGuild.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, minersGuild.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, minersGuild.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, minersGuild.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, minersGuild.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x �) + (7 workers x 2) = 18
		assertEquals (84, minersGuild.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (33, minersGuild.getProductionType ().get (1).getModifiedProductionAmount ());		// 18 * 1.84 = 33.12
		assertEquals (0, minersGuild.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, minersGuild.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (75, minersGuild.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (37, minersGuild.getProductionType ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +7.5 from gems = 37.5
		assertEquals (20, minersGuild.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (44, minersGuild.getProductionType ().get (2).getModifiedProductionAmount ());		// 37 * 1.2 = 44.4
		assertEquals (7, minersGuild.getProductionType ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, minersGuild.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (32, minersGuild.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (16, minersGuild.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16
		assertEquals (0, minersGuild.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (16, minersGuild.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, minersGuild.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, minersGuild.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, minersGuild.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, minersGuild.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, minersGuild.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, minersGuild.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, minersGuild.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, minersGuild.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, minersGuild.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, minersGuild.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getProductionType ().get (5).getConsumptionAmount ());

		// Dwarves double bonuses from map features, and also workers produce 3 production instead of 2
		cityData.setCityRaceID (GenerateTestData.DWARVES);

		final CityProductionBreakdownsEx dwarves = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (7, dwarves.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, dwarves.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, dwarves.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, dwarves.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, dwarves.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, dwarves.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, dwarves.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, dwarves.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (50, dwarves.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (25, dwarves.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x �) + (7 workers x 3) = 25
		assertEquals (84, dwarves.getProductionType ().get (1).getPercentageBonus ());						// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (46, dwarves.getProductionType ().get (1).getModifiedProductionAmount ());		// 25 * 1.84 = 46
		assertEquals (0, dwarves.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, dwarves.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (90, dwarves.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (45, dwarves.getProductionType ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +15 from gems = 45
		assertEquals (20, dwarves.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (54, dwarves.getProductionType ().get (2).getModifiedProductionAmount ());		// 45 * 1.2 = 54
		assertEquals (7, dwarves.getProductionType ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, dwarves.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (38, dwarves.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (19, dwarves.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +6 from adamantium = 19
		assertEquals (0, dwarves.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (19, dwarves.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, dwarves.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, dwarves.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, dwarves.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, dwarves.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, dwarves.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, dwarves.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, dwarves.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, dwarves.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, dwarves.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, dwarves.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getProductionType ().get (5).getConsumptionAmount ());

		// High elf rebels produce mana too
		cityData.setCityRaceID (GenerateTestData.HIGH_ELF);

		final CityProductionBreakdownsEx highElves = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (7, highElves.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, highElves.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (40, highElves.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (20, highElves.getProductionType ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, highElves.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (20, highElves.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (17, highElves.getProductionType ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, highElves.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, highElves.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (18, highElves.getProductionType ().get (1).getBaseProductionAmount ());			// (8 farmers x �) + (7 workers x 2) = 18
		assertEquals (84, highElves.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (33, highElves.getProductionType ().get (1).getModifiedProductionAmount ());		// 18 * 1.84 = 33.12
		assertEquals (0, highElves.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, highElves.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (75, highElves.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (37, highElves.getProductionType ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +7.5 from gems = 37.5
		assertEquals (20, highElves.getProductionType ().get (2).getPercentageBonus ());
		assertEquals (44, highElves.getProductionType ().get (2).getModifiedProductionAmount ());		// 37 * 1.2 = 44.4
		assertEquals (7, highElves.getProductionType ().get (2).getConsumptionAmount ());				// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, highElves.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (49, highElves.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (24, highElves.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +17 from pop = 49
		assertEquals (0, highElves.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (24, highElves.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, highElves.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, highElves.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, highElves.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, highElves.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, highElves.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, highElves.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, highElves.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, highElves.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (35, highElves.getProductionType ().get (5).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, highElves.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, highElves.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (18, highElves.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (0, highElves.getProductionType ().get (5).getConsumptionAmount ());

		// Shrink city to size 6 - gold % bonus is then capped at 6 x3 = 18%
		cityData.setCityPopulation (6900);
		cityData.setMinimumFarmers (1);
		cityData.setOptionalFarmers (1);
		cityData.setNumberOfRebels (1);		// 6 -1 -1 -1 = 3 workers

		final CityProductionBreakdownsEx shrunk = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (7, shrunk.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, shrunk.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (16, shrunk.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (8, shrunk.getProductionType ().get (0).getBaseProductionAmount ());				// (2 farmers x2) + (2 x2 from wild game) = 8
		assertEquals (0, shrunk.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (8, shrunk.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (6, shrunk.getProductionType ().get (0).getConsumptionAmount ());				// 6 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, shrunk.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (14, shrunk.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (7, shrunk.getProductionType ().get (1).getBaseProductionAmount ());				// (2 farmers x �) + (3 workers x 2) = 7
		assertEquals (84, shrunk.getProductionType ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (12, shrunk.getProductionType ().get (1).getModifiedProductionAmount ());		// 7 * 1.84 = 12.88
		assertEquals (0, shrunk.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, shrunk.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (35, shrunk.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (17, shrunk.getProductionType ().get (2).getBaseProductionAmount ());			// (5 non-rebels x2) +7.5 from gems = 17.5
		assertEquals (18, shrunk.getProductionType ().get (2).getPercentageBonus ());					// Capped due to city size
		assertEquals (20, shrunk.getProductionType ().get (2).getModifiedProductionAmount ());		// 17 * 1.18 = 20.06
		assertEquals (7, shrunk.getProductionType ().get (2).getConsumptionAmount ());				// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, shrunk.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (38, shrunk.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (19, shrunk.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +6 from pop = 38
		assertEquals (0, shrunk.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (19, shrunk.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, shrunk.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, shrunk.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, shrunk.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, shrunk.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, shrunk.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, shrunk.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, shrunk.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, shrunk.getProductionType ().get (5).getProductionTypeID ());
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

			mc.getTerrainData ().setMapFeatureID (GenerateTestData.WILD_GAME);
		}

		final CityProductionBreakdownsEx maxSize = calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, false, GenerateTestData.createDB ());
		assertEquals (7, maxSize.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, maxSize.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (36, maxSize.getProductionType ().get (0).getDoubleProductionAmount ());
		assertEquals (18, maxSize.getProductionType ().get (0).getBaseProductionAmount ());			// (2 farmers x2) + (2 x7 from wild game) = 18
		assertEquals (0, maxSize.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (18, maxSize.getProductionType ().get (0).getModifiedProductionAmount ());
		assertEquals (6, maxSize.getProductionType ().get (0).getConsumptionAmount ());					// 6 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, maxSize.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (14, maxSize.getProductionType ().get (1).getDoubleProductionAmount ());
		assertEquals (7, maxSize.getProductionType ().get (1).getBaseProductionAmount ());				// (2 farmers x �) + (3 workers x 2) = 7
		assertEquals (84, maxSize.getProductionType ().get (1).getPercentageBonus ());						// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (12, maxSize.getProductionType ().get (1).getModifiedProductionAmount ());		// 7 * 1.84 = 12.88
		assertEquals (0, maxSize.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, maxSize.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (35, maxSize.getProductionType ().get (2).getDoubleProductionAmount ());
		assertEquals (17, maxSize.getProductionType ().get (2).getBaseProductionAmount ());			// (5 non-rebels x2) +7.5 from gems = 17.5
		assertEquals (18, maxSize.getProductionType ().get (2).getPercentageBonus ());						// Capped due to city size
		assertEquals (20, maxSize.getProductionType ().get (2).getModifiedProductionAmount ());		// 17 * 1.18 = 20.06
		assertEquals (7, maxSize.getProductionType ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, maxSize.getProductionType ().get (3).getProductionTypeID ());
		assertEquals (38, maxSize.getProductionType ().get (3).getDoubleProductionAmount ());
		assertEquals (19, maxSize.getProductionType ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +6 from pop = 38
		assertEquals (0, maxSize.getProductionType ().get (3).getPercentageBonus ());
		assertEquals (19, maxSize.getProductionType ().get (3).getModifiedProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, maxSize.getProductionType ().get (4).getProductionTypeID ());
		assertEquals (6, maxSize.getProductionType ().get (4).getDoubleProductionAmount ());
		assertEquals (3, maxSize.getProductionType ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, maxSize.getProductionType ().get (4).getPercentageBonus ());
		assertEquals (3, maxSize.getProductionType ().get (4).getModifiedProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, maxSize.getProductionType ().get (5).getProductionTypeID ());
		assertEquals (55, maxSize.getProductionType ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x7 from wild game) = 35
		assertEquals (28, maxSize.getProductionType ().get (5).getBaseProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (5).getPercentageBonus ());
		assertEquals (28, maxSize.getProductionType ().get (5).getModifiedProductionAmount ());
		assertEquals (25, maxSize.getProductionType ().get (5).getCappedProductionAmount ());
		assertEquals (0, maxSize.getProductionType ().get (5).getConsumptionAmount ());
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
		
		final TileType foodTile = new TileType ();
		foodTile.setDoubleFood (3);
		when (db.findTileType (eq ("TT01"), anyString ())).thenReturn (foodTile);
		
		final TileType bothTile = new TileType ();
		bothTile.setDoubleFood (1);
		bothTile.setProductionBonus (1);
		bothTile.setGoldBonus (30);
		when (db.findTileType (eq ("TT02"), anyString ())).thenReturn (bothTile);
		
		final TileType productionTile = new TileType ();
		productionTile.setProductionBonus (3);
		when (db.findTileType (eq ("TT03"), anyString ())).thenReturn (productionTile);
		
		final ProductionType foodProduction = new ProductionType ();
		foodProduction.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		when (db.findProductionType (eq (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD), anyString ())).thenReturn (foodProduction);
		
		final BuildingPopulationProductionModifier granaryFood = new BuildingPopulationProductionModifier ();
		granaryFood.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
		granaryFood.setDoubleAmount (4);
		
		final Building granary = new Building ();
		granary.setBuildingID ("BL01");
		granary.getBuildingPopulationProductionModifier ().add (granaryFood);
		
		final BuildingPopulationProductionModifier farmersMarketFood = new BuildingPopulationProductionModifier ();
		farmersMarketFood.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
		farmersMarketFood.setDoubleAmount (6);
		
		final Building farmersMarket = new Building ();
		farmersMarket.setBuildingID ("BL02");
		farmersMarket.getBuildingPopulationProductionModifier ().add (farmersMarketFood);
		
		final List<Building> buildings = new ArrayList<Building> ();
		buildings.add (granary);
		buildings.add (farmersMarket);
		doReturn (buildings).when (db).getBuilding ();
		
		// Session description
		final MapSizeData mapSize = GenerateTestData.createMapSizeData ();
		
		final DifficultyLevelData difficultyLevel = new DifficultyLevelData ();
		difficultyLevel.setCityMaxSize (25);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		sd.setDifficultyLevel (difficultyLevel);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		// Buildings
		final List<MemoryBuilding> memoryBuildings = new ArrayList<MemoryBuilding> ();
		
		// Put 3 of the 'food' tiles, 5 of the 'both' files and 3 of the 'production' tiles
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (mapSize);
		
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
		
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// At the moment there's space for 7,000 people, so the gold trade bonus from the tile type is 30 so this is less than the 36 cap
		final CityProductionBreakdownsEx prod1 = calc.calculateAllCityProductions (players, map, memoryBuildings, new MapCoordinates3DEx (2, 2, 0), "TR01", sd, false, true, db);
		assertEquals (3, prod1.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, prod1.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (14, prod1.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, prod1.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (30, prod1.getProductionType ().get (1).getTradePercentageBonusCapped ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, prod1.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (12, prod1.getProductionType ().get (2).getCappedProductionAmount ());		// (14/2) +5 from granary and farmers' market
		
		// Increase the gold trade bonus to 40, so it gets capped at 36
		bothTile.setGoldBonus (40);

		final CityProductionBreakdownsEx prod2 = calc.calculateAllCityProductions (players, map, memoryBuildings, new MapCoordinates3DEx (2, 2, 0), "TR01", sd, false, true, db);
		assertEquals (3, prod2.getProductionType ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, prod2.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (14, prod2.getProductionType ().get (0).getPercentageBonus ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, prod2.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (36, prod2.getProductionType ().get (1).getTradePercentageBonusCapped ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, prod2.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (12, prod2.getProductionType ().get (2).getCappedProductionAmount ());		// (14/2) +5 from granary and farmers' market
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
		
		final ProductionType gold = new ProductionType ();
		gold.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		when (db.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, "halveAddPercentageBonusAndCapProduction")).thenReturn (gold);
		
		final ProductionType food = new ProductionType ();
		food.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		when (db.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, "halveAddPercentageBonusAndCapProduction")).thenReturn (food);
		
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		
		// Exact multiple
		final CityProductionBreakdown prod1 = new CityProductionBreakdown ();
		prod1.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
		prod1.setDoubleProductionAmount (18);
		prod1.setPercentageBonus (25);
		
		calc.halveAddPercentageBonusAndCapProduction (prod1, 25, db);
		
		assertNull (prod1.getRoundingDirectionID ());
		assertEquals (9, prod1.getBaseProductionAmount ());
		assertEquals (11, prod1.getModifiedProductionAmount ());
		assertEquals (11, prod1.getCappedProductionAmount ());
		
		// Round down
		final CityProductionBreakdown prod2 = new CityProductionBreakdown ();
		prod2.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
		prod2.setDoubleProductionAmount (19);
		prod2.setPercentageBonus (25);

		calc.halveAddPercentageBonusAndCapProduction (prod2, 25, db);
		
		assertEquals (RoundingDirectionID.ROUND_DOWN, prod2.getRoundingDirectionID ());
		assertEquals (9, prod2.getBaseProductionAmount ());
		assertEquals (11, prod2.getModifiedProductionAmount ());
		assertEquals (11, prod2.getCappedProductionAmount ());
		
		// Round up
		gold.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);

		final CityProductionBreakdown prod3 = new CityProductionBreakdown ();
		prod3.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
		prod3.setDoubleProductionAmount (19);
		prod3.setPercentageBonus (25);

		calc.halveAddPercentageBonusAndCapProduction (prod3, 25, db);
		
		assertEquals (RoundingDirectionID.ROUND_UP, prod3.getRoundingDirectionID ());
		assertEquals (10, prod3.getBaseProductionAmount ());
		assertEquals (12, prod3.getModifiedProductionAmount ());
		assertEquals (12, prod3.getCappedProductionAmount ());
		
		// Value is over cap, but isn't food so cap doesn't apply
		final CityProductionBreakdown prod4 = new CityProductionBreakdown ();
		prod4.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
		prod4.setDoubleProductionAmount (41);
		prod4.setPercentageBonus (25);

		calc.halveAddPercentageBonusAndCapProduction (prod4, 25, db);
		
		assertEquals (RoundingDirectionID.ROUND_UP, prod4.getRoundingDirectionID ());
		assertEquals (21, prod4.getBaseProductionAmount ());
		assertEquals (26, prod4.getModifiedProductionAmount ());
		assertEquals (26, prod4.getCappedProductionAmount ());

		// Cap applies to food
		final CityProductionBreakdown prod5 = new CityProductionBreakdown ();
		prod5.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
		prod5.setDoubleProductionAmount (41);
		prod5.setPercentageBonus (25);

		calc.halveAddPercentageBonusAndCapProduction (prod5, 25, db);
		
		assertEquals (RoundingDirectionID.ROUND_UP, prod5.getRoundingDirectionID ());
		assertEquals (21, prod5.getBaseProductionAmount ());
		assertEquals (26, prod5.getModifiedProductionAmount ());
		assertEquals (25, prod5.getCappedProductionAmount ());
	}

	/**
	 * Tests the calculateSingleCityProduction method
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Test
	public final void testCalculateSingleCityProduction () throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// This is the same initial setup as the calculateAllCityProductions test
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
			hillsTerrain.setTileTypeID (GenerateTestData.HILLS_TILE);
			map.getPlane ().get (1).getRow ().get (0).getCell ().get (x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
			riverTile.setTileTypeID (GenerateTestData.RIVER_TILE);
			map.getPlane ().get (1).getRow ().get (1).getCell ().get (x).setTerrainData (riverTile);
		}

		// Put river right on the city too, to get the gold bonus
		final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
		riverTile.setTileTypeID (GenerateTestData.RIVER_TILE);
		map.getPlane ().get (1).getRow ().get (2).getCell ().get (2).setTerrainData (riverTile);

		// Add some wild game
		for (int y = 0; y <= 1; y++)
			map.getPlane ().get (1).getRow ().get (y).getCell ().get (2).getTerrainData ().setMapFeatureID (GenerateTestData.WILD_GAME);

		// Session description
		final MapSizeData mapSize = new MapSizeData ();
		mapSize.setWidth (sys.getWidth ());
		mapSize.setHeight (sys.getHeight ());
		mapSize.setCoordinateSystemType (sys.getCoordinateSystemType ());
		mapSize.setWrapsLeftToRight (sys.isWrapsLeftToRight ());
		mapSize.setWrapsTopToBottom (sys.isWrapsTopToBottom ());

		final DifficultyLevelData dl = new DifficultyLevelData ();
		dl.setCityMaxSize (25);

		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		sd.setDifficultyLevel (dl);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID (GenerateTestData.HIGH_MEN);
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		cityData.setOptionalFarmers (2);
		cityData.setNumberOfRebels (2);		// 17 -6 -2 -2 = 7 workers
		map.getPlane ().get (1).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails ppd = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (ppd);

		// 20 production - 17 consumption = 3
		assertEquals (3, calc.calculateSingleCityProduction (players, map, buildings, cityLocation,
			GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS));
	}

	/**
	 * Tests the blankBuildingsSoldThisTurn method with specifing a player
	 */
	@Test
	public final void testBlankBuildingsSoldThisTurn_OnePlayer ()
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		
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
				mc.setBuildingIdSoldThisTurn (GenerateTestData.SAWMILL);
			}

		calc.blankBuildingsSoldThisTurn (map, 4);

		for (int x = 1; x <= 3; x++)
			for (int y = 1; y <= 3; y++)
				if (x + y == 4)
					assertNull (map.getPlane ().get (0).getRow ().get (y).getCell ().get (x).getBuildingIdSoldThisTurn ());
				else
					assertEquals (GenerateTestData.SAWMILL, map.getPlane ().get (0).getRow ().get (y).getCell ().get (x).getBuildingIdSoldThisTurn ());
	}

	/**
	 * Tests the blankBuildingsSoldThisTurn method for all players
	 */
	@Test
	public final void testBlankBuildingsSoldThisTurn_AllPlayers ()
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		
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
				mc.setBuildingIdSoldThisTurn (GenerateTestData.SAWMILL);
			}

		calc.blankBuildingsSoldThisTurn (map, 0);

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
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final MapAreaOperations2DImpl<Boolean> op = new MapAreaOperations2DImpl<Boolean> (); 

		// Coordinate system
		final MapSizeData mapSize = new MapSizeData ();
		mapSize.setWidth (sys.getWidth ());
		mapSize.setHeight (sys.getHeight ());
		mapSize.setCoordinateSystemType (sys.getCoordinateSystemType ());
		mapSize.setWrapsLeftToRight (sys.isWrapsLeftToRight ());
		mapSize.setWrapsTopToBottom (sys.isWrapsTopToBottom ());

		mapSize.setCitySeparation (3);

		// No cities
		final MapArea2D<Boolean> none = calc.markWithinExistingCityRadius (map, 1, mapSize);
		assertEquals (0, op.countCellsEqualTo (none, true));

		// City on the wrong plane
		final OverlandMapCityData wrongPlaneCity = new OverlandMapCityData ();
		wrongPlaneCity.setCityPopulation (1);
		map.getPlane ().get (0).getRow ().get (4).getCell ().get (6).setCityData (wrongPlaneCity);

		final MapArea2D<Boolean> wrongPlane = calc.markWithinExistingCityRadius (map, 1, mapSize);
		assertEquals (0, op.countCellsEqualTo (wrongPlane, true));

		// City in the middle of the map
		final OverlandMapCityData oneCity = new OverlandMapCityData ();
		oneCity.setCityPopulation (1);
		map.getPlane ().get (1).getRow ().get (4).getCell ().get (6).setCityData (oneCity);

		final MapArea2D<Boolean> one = calc.markWithinExistingCityRadius (map, 1, mapSize);
		assertEquals (49, op.countCellsEqualTo (one, true));

		for (int x = 3; x <= 9; x++)
			for (int y = 1; y <= 7; y++)
				assertTrue (one.get (x, y));

		// 2nd city at top edge of map so some of it gets clipped
		final OverlandMapCityData twoCities = new OverlandMapCityData ();
		twoCities.setCityPopulation (1);
		map.getPlane ().get (1).getRow ().get (1).getCell ().get (16).setCityData (twoCities);

		final MapArea2D<Boolean> two = calc.markWithinExistingCityRadius (map, 1, mapSize);
		assertEquals (49 + 35, op.countCellsEqualTo (two, true));

		// 3nd city at left edge of map so some of it gets wrapped
		final OverlandMapCityData threeCities = new OverlandMapCityData ();
		threeCities.setCityPopulation (1);
		map.getPlane ().get (1).getRow ().get (14).getCell ().get (1).setCityData (threeCities);

		final MapArea2D<Boolean> three = calc.markWithinExistingCityRadius (map, 1, mapSize);
		assertEquals (49 + 49 + 35, op.countCellsEqualTo (three, true));
	}

	/**
	 * Tests the goldToRushBuy method
	 */
	@Test
	public final void testGoldToRushBuy ()
	{
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		assertEquals (240, calc.goldToRushBuy (60, 0));
		assertEquals (177, calc.goldToRushBuy (60, 1));		// The above 2 are the actual examples in the strategy guide
		
		assertEquals (93, calc.goldToRushBuy (60, 29));
		assertEquals (60, calc.goldToRushBuy (60, 30));
		assertEquals (2, calc.goldToRushBuy (60, 59));
	}
}