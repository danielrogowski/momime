package momime.common.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.DifficultyLevelData;
import momime.common.database.newgame.v0_9_4.MapSizeData;
import momime.common.database.v0_9_4.Building;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.common.utils.PlayerPickUtilsImpl;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.areas.operations.MapAreaOperations2DImpl;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the calculations in the MomCityCalculations class
 */
public final class TestMomCityCalculationsImpl
{
	/**
	 * Tests the calculateProductionBonus method
	 * @throws RecordNotFoundException If we encounter a tile type that we cannot find in the cache
	 */
	@Test
	public final void testCalculateProductionBonus () throws RecordNotFoundException
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// 0 so far
		assertEquals (0, calc.calculateProductionBonus (map, cityLocation, sys, GenerateTestData.createDB ()));

		// Add 3 hills and 5 mountains, (3*3) + (5*5) = 34
		// Note we add hills in the NW and NE corners too, outside the city radius, so we prove that this isn't counted
		for (int x = 0; x <= 4; x++)
		{
			final OverlandMapTerrainData hillsTerrain = new OverlandMapTerrainData ();
			hillsTerrain.setTileTypeID (GenerateTestData.HILLS_TILE);
			map.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData mountainsTerrain = new OverlandMapTerrainData ();
			mountainsTerrain.setTileTypeID (GenerateTestData.MOUNTAINS_TILE);
			map.getPlane ().get (0).getRow ().get (1).getCell ().get (x).setTerrainData (mountainsTerrain);
		}

		assertEquals (34, calc.calculateProductionBonus (map, cityLocation, sys, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the calculateGoldBonus method
	 * @throws RecordNotFoundException If we encounter a tile type that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldBonus () throws RecordNotFoundException
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// 0 so far
		assertEquals (0, calc.calculateGoldBonus (map, cityLocation, sys, GenerateTestData.createDB ()));

		// Bonus from centre square
		final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
		riverTile.setTileTypeID (GenerateTestData.RIVER_TILE);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setTerrainData (riverTile);
		assertEquals (20, calc.calculateGoldBonus (map, cityLocation, sys, GenerateTestData.createDB ()));

		// Bonus from adjacent tile, but centre takes precedence
		final OverlandMapTerrainData shoreTile = new OverlandMapTerrainData ();
		shoreTile.setTileTypeID (GenerateTestData.SHORE_TILE);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (3).setTerrainData (shoreTile);
		assertEquals (20, calc.calculateGoldBonus (map, cityLocation, sys, GenerateTestData.createDB ()));

		// Remove bonus from centre square so we get adjacent bonus
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setTerrainData (null);
		assertEquals (10, calc.calculateGoldBonus (map, cityLocation, sys, GenerateTestData.createDB ()));

		// Adjacent bonus only applies if adjacent flag is set on the tile type
		// To test this we fudge the DB after creating it
		final CommonDatabase db = GenerateTestData.createDB ();
		db.findTileType (GenerateTestData.SHORE_TILE, "testCalculateGoldBonus").setGoldBonusSurroundingTiles (null);
		assertEquals (0, calc.calculateGoldBonus (map, cityLocation, sys, db));
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
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (0);

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
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (0);

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
	 * Tests the calculateMaxCitySize method
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 */
	@Test
	public final void testCalculateMaxCitySize () throws RecordNotFoundException
	{
		// Set up object to test
		final MomCityCalculationsImpl calc = new MomCityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

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

		// 0 so far
		assertEquals (0, calc.calculateMaxCitySize (map, cityLocation, sd, false, false, GenerateTestData.createDB ()));
		assertEquals (0, calc.calculateMaxCitySize (map, cityLocation, sd, true, false, GenerateTestData.createDB ()));
		assertEquals (0, calc.calculateMaxCitySize (map, cityLocation, sd, true, true, GenerateTestData.createDB ()));

		// Add some tile types that grant food, 3 hills and 5 rivers, (3*1) + (4*5) = 23
		// Note we add hills in the NW and NE corners too, outside the city radius, so we prove that this isn't counted
		for (int x = 0; x <= 4; x++)
		{
			final OverlandMapTerrainData hillsTerrain = new OverlandMapTerrainData ();
			hillsTerrain.setTileTypeID (GenerateTestData.HILLS_TILE);
			map.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
			riverTile.setTileTypeID (GenerateTestData.RIVER_TILE);
			map.getPlane ().get (0).getRow ().get (1).getCell ().get (x).setTerrainData (riverTile);
		}

		assertEquals (23, calc.calculateMaxCitySize (map, cityLocation, sd, false, false, GenerateTestData.createDB ()));
		assertEquals (23, calc.calculateMaxCitySize (map, cityLocation, sd, true, false, GenerateTestData.createDB ()));
		assertEquals (12, calc.calculateMaxCitySize (map, cityLocation, sd, true, true, GenerateTestData.createDB ()));

		// Add some wild game
		for (int y = 0; y <= 1; y++)
			map.getPlane ().get (0).getRow ().get (y).getCell ().get (2).getTerrainData ().setMapFeatureID (GenerateTestData.WILD_GAME);

		assertEquals (23, calc.calculateMaxCitySize (map, cityLocation, sd, false, false, GenerateTestData.createDB ()));
		assertEquals (31, calc.calculateMaxCitySize (map, cityLocation, sd, true, false, GenerateTestData.createDB ()));
		assertEquals (16, calc.calculateMaxCitySize (map, cityLocation, sd, true, true, GenerateTestData.createDB ()));

		// Test cap
		// Uncapped would be 26 - previous tests prove that rounding is up
		for (int y = 0; y <= 4; y++)
		{
			final MemoryGridCell mc = map.getPlane ().get (0).getRow ().get (y).getCell ().get (1);
			if (mc.getTerrainData () == null)
				mc.setTerrainData (new OverlandMapTerrainData ());

			mc.getTerrainData ().setMapFeatureID (GenerateTestData.WILD_GAME);
		}

		assertEquals (23, calc.calculateMaxCitySize (map, cityLocation, sd, false, false, GenerateTestData.createDB ()));
		assertEquals (51, calc.calculateMaxCitySize (map, cityLocation, sd, true, false, GenerateTestData.createDB ()));
		assertEquals (25, calc.calculateMaxCitySize (map, cityLocation, sd, true, true, GenerateTestData.createDB ()));
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
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (0);

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
		final CalculateCityGrowthRateBreakdown maximum = calc.calculateCityGrowthRate (map, buildings, cityLocation, 10, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.MAXIMUM, maximum.getDirection ());
		assertEquals (10000, maximum.getCurrentPopulation ());
		assertEquals (10000, maximum.getMaximumPopulation ());
		assertEquals (0, maximum.getBaseGrowthRate ());
		assertEquals (0, maximum.getRacialGrowthModifier ());
		assertNull (maximum.getBuildingsModifyingGrowthRate ());
		assertEquals (0, maximum.getTotalGrowthRate ());
		assertEquals (0, maximum.getCappedGrowthRate ());
		assertEquals (0, maximum.getBaseDeathRate ());
		assertEquals (0, maximum.getCityDeathRate ());
		assertEquals (0, maximum.getFinalTotal ());

		// Growing (this is the example quoted in the strategy guide, however note the example is in contradiction with the formula - from testing I believe the example is right and the formula is supposed to be a -1 not a +1)
		cityData.setCityPopulation (12000);
		final CalculateCityGrowthRateBreakdown growingEven = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.GROWING, growingEven.getDirection ());
		assertEquals (12000, growingEven.getCurrentPopulation ());
		assertEquals (22000, growingEven.getMaximumPopulation ());
		assertEquals (50, growingEven.getBaseGrowthRate ());
		assertEquals (0, growingEven.getRacialGrowthModifier ());
		assertEquals (0, growingEven.getBuildingsModifyingGrowthRate ().length);
		assertEquals (50, growingEven.getTotalGrowthRate ());
		assertEquals (50, growingEven.getCappedGrowthRate ());
		assertEquals (0, growingEven.getBaseDeathRate ());
		assertEquals (0, growingEven.getCityDeathRate ());
		assertEquals (50, growingEven.getFinalTotal ());

		final CalculateCityGrowthRateBreakdown growingOdd = calc.calculateCityGrowthRate (map, buildings, cityLocation, 23, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.GROWING, growingOdd.getDirection ());
		assertEquals (12000, growingOdd.getCurrentPopulation ());
		assertEquals (23000, growingOdd.getMaximumPopulation ());
		assertEquals (50, growingOdd.getBaseGrowthRate ());
		assertEquals (0, growingOdd.getRacialGrowthModifier ());
		assertEquals (0, growingOdd.getBuildingsModifyingGrowthRate ().length);
		assertEquals (50, growingOdd.getTotalGrowthRate ());
		assertEquals (50, growingOdd.getCappedGrowthRate ());
		assertEquals (0, growingOdd.getBaseDeathRate ());
		assertEquals (0, growingOdd.getCityDeathRate ());
		assertEquals (50, growingOdd.getFinalTotal ());

		// Bonus from race - positive
		cityData.setCityRaceID (GenerateTestData.BARBARIAN);
		final CalculateCityGrowthRateBreakdown barbarian = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.GROWING, barbarian.getDirection ());
		assertEquals (12000, barbarian.getCurrentPopulation ());
		assertEquals (22000, barbarian.getMaximumPopulation ());
		assertEquals (50, barbarian.getBaseGrowthRate ());
		assertEquals (20, barbarian.getRacialGrowthModifier ());
		assertEquals (0, barbarian.getBuildingsModifyingGrowthRate ().length);
		assertEquals (70, barbarian.getTotalGrowthRate ());
		assertEquals (70, barbarian.getCappedGrowthRate ());
		assertEquals (0, barbarian.getBaseDeathRate ());
		assertEquals (0, barbarian.getCityDeathRate ());
		assertEquals (70, barbarian.getFinalTotal ());

		// Bonus from race - negative
		cityData.setCityRaceID (GenerateTestData.HIGH_ELF);
		final CalculateCityGrowthRateBreakdown highElf = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.GROWING, highElf.getDirection ());
		assertEquals (12000, highElf.getCurrentPopulation ());
		assertEquals (22000, highElf.getMaximumPopulation ());
		assertEquals (50, highElf.getBaseGrowthRate ());
		assertEquals (-20, highElf.getRacialGrowthModifier ());
		assertEquals (0, highElf.getBuildingsModifyingGrowthRate ().length);
		assertEquals (30, highElf.getTotalGrowthRate ());
		assertEquals (30, highElf.getCappedGrowthRate ());
		assertEquals (0, highElf.getBaseDeathRate ());
		assertEquals (0, highElf.getCityDeathRate ());
		assertEquals (30, highElf.getFinalTotal ());

		// Bonus from buildings
		final OverlandMapCoordinatesEx granaryLocation = new OverlandMapCoordinatesEx ();
		granaryLocation.setX (2);
		granaryLocation.setY (2);
		granaryLocation.setZ (0);

		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID (GenerateTestData.GRANARY);
		granary.setCityLocation (granaryLocation);
		buildings.add (granary);

		final OverlandMapCoordinatesEx farmersMarketLocation = new OverlandMapCoordinatesEx ();
		farmersMarketLocation.setX (2);
		farmersMarketLocation.setY (2);
		farmersMarketLocation.setZ (0);

		final MemoryBuilding farmersMarket = new MemoryBuilding ();
		farmersMarket.setBuildingID (GenerateTestData.FARMERS_MARKET);
		farmersMarket.setCityLocation (farmersMarketLocation);
		buildings.add (farmersMarket);

		final OverlandMapCoordinatesEx sagesGuildLocation = new OverlandMapCoordinatesEx ();
		sagesGuildLocation.setX (2);
		sagesGuildLocation.setY (2);
		sagesGuildLocation.setZ (0);

		final MemoryBuilding sagesGuild = new MemoryBuilding ();		// Irrelevant building, to prove it doesn't get included in the list
		sagesGuild.setBuildingID (GenerateTestData.SAGES_GUILD);
		sagesGuild.setCityLocation (sagesGuildLocation);
		buildings.add (sagesGuild);

		final CalculateCityGrowthRateBreakdown withBuildings = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.GROWING, withBuildings.getDirection ());
		assertEquals (12000, withBuildings.getCurrentPopulation ());
		assertEquals (22000, withBuildings.getMaximumPopulation ());
		assertEquals (50, withBuildings.getBaseGrowthRate ());
		assertEquals (-20, withBuildings.getRacialGrowthModifier ());
		assertEquals (2, withBuildings.getBuildingsModifyingGrowthRate ().length);
		assertEquals (GenerateTestData.GRANARY, withBuildings.getBuildingsModifyingGrowthRate () [0].getBuildingID ());
		assertEquals (20, withBuildings.getBuildingsModifyingGrowthRate () [0].getGrowthRateModifier ());
		assertEquals (GenerateTestData.FARMERS_MARKET, withBuildings.getBuildingsModifyingGrowthRate () [1].getBuildingID ());
		assertEquals (30, withBuildings.getBuildingsModifyingGrowthRate () [1].getGrowthRateModifier ());
		assertEquals (80, withBuildings.getTotalGrowthRate ());
		assertEquals (80, withBuildings.getCappedGrowthRate ());
		assertEquals (0, withBuildings.getBaseDeathRate ());
		assertEquals (0, withBuildings.getCityDeathRate ());
		assertEquals (80, withBuildings.getFinalTotal ());

		// With all those buildings, at almost max size we still get a reasonable increase
		cityData.setCityPopulation (21960);
		final CalculateCityGrowthRateBreakdown almostCapped = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.GROWING, almostCapped.getDirection ());
		assertEquals (21960, almostCapped.getCurrentPopulation ());
		assertEquals (22000, almostCapped.getMaximumPopulation ());
		assertEquals (0, almostCapped.getBaseGrowthRate ());
		assertEquals (-20, almostCapped.getRacialGrowthModifier ());
		assertEquals (2, almostCapped.getBuildingsModifyingGrowthRate ().length);
		assertEquals (GenerateTestData.GRANARY, almostCapped.getBuildingsModifyingGrowthRate () [0].getBuildingID ());
		assertEquals (20, almostCapped.getBuildingsModifyingGrowthRate () [0].getGrowthRateModifier ());
		assertEquals (GenerateTestData.FARMERS_MARKET, almostCapped.getBuildingsModifyingGrowthRate () [1].getBuildingID ());
		assertEquals (30, almostCapped.getBuildingsModifyingGrowthRate () [1].getGrowthRateModifier ());
		assertEquals (30, almostCapped.getTotalGrowthRate ());
		assertEquals (30, almostCapped.getCappedGrowthRate ());
		assertEquals (0, almostCapped.getBaseDeathRate ());
		assertEquals (0, almostCapped.getCityDeathRate ());
		assertEquals (30, almostCapped.getFinalTotal ());

		// +30 with only 20 to spare would push us over max size
		cityData.setCityPopulation (21980);
		final CalculateCityGrowthRateBreakdown overCap = calc.calculateCityGrowthRate (map, buildings, cityLocation, 22, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.GROWING, overCap.getDirection ());
		assertEquals (21980, overCap.getCurrentPopulation ());
		assertEquals (22000, overCap.getMaximumPopulation ());
		assertEquals (0, overCap.getBaseGrowthRate ());
		assertEquals (-20, overCap.getRacialGrowthModifier ());
		assertEquals (2, overCap.getBuildingsModifyingGrowthRate ().length);
		assertEquals (GenerateTestData.GRANARY, overCap.getBuildingsModifyingGrowthRate () [0].getBuildingID ());
		assertEquals (20, overCap.getBuildingsModifyingGrowthRate () [0].getGrowthRateModifier ());
		assertEquals (GenerateTestData.FARMERS_MARKET, overCap.getBuildingsModifyingGrowthRate () [1].getBuildingID ());
		assertEquals (30, overCap.getBuildingsModifyingGrowthRate () [1].getGrowthRateModifier ());
		assertEquals (30, overCap.getTotalGrowthRate ());
		assertEquals (20, overCap.getCappedGrowthRate ());
		assertEquals (0, overCap.getBaseDeathRate ());
		assertEquals (0, overCap.getCityDeathRate ());
		assertEquals (20, overCap.getFinalTotal ());

		// Dying - note the race and building modifiers don't apply, because we can't by virtue of bonuses force a city to go over max size
		final CalculateCityGrowthRateBreakdown dying = calc.calculateCityGrowthRate (map, buildings, cityLocation, 18, GenerateTestData.createDB ());
		assertEquals (MomCityGrowthDirection.DYING, dying.getDirection ());
		assertEquals (21980, dying.getCurrentPopulation ());
		assertEquals (18000, dying.getMaximumPopulation ());
		assertEquals (0, dying.getBaseGrowthRate ());
		assertEquals (0, dying.getRacialGrowthModifier ());
		assertNull (dying.getBuildingsModifyingGrowthRate ());
		assertEquals (0, dying.getTotalGrowthRate ());
		assertEquals (0, dying.getCappedGrowthRate ());
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
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (0);

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
		final CalculateCityUnrestBreakdown zeroPercent = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_0_GOLD_0_UNREST, GenerateTestData.createDB ());
		assertEquals (17, zeroPercent.getPopulation ());
		assertEquals (0, zeroPercent.getTaxPercentage ());
		assertEquals (0, zeroPercent.getRacialPercentage ());
		assertEquals (0, zeroPercent.getRacialLiteral ());
		assertEquals (0, zeroPercent.getTotalPercentage ());
		assertEquals (0, zeroPercent.getBaseValue ());
		assertEquals (0, zeroPercent.getBuildingsReducingUnrest ().length);
		assertEquals (0, zeroPercent.getBaseTotal ());
		assertEquals (0, zeroPercent.getFinalTotal ());

		// Harsh 45% tax rate = 7.65, prove that it rounds down
		final CalculateCityUnrestBreakdown highPercent = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, highPercent.getPopulation ());
		assertEquals (45, highPercent.getTaxPercentage ());
		assertEquals (0, highPercent.getRacialPercentage ());
		assertEquals (0, highPercent.getRacialLiteral ());
		assertEquals (45, highPercent.getTotalPercentage ());
		assertEquals (7, highPercent.getBaseValue ());
		assertEquals (0, highPercent.getBuildingsReducingUnrest ().length);
		assertEquals (7, highPercent.getBaseTotal ());
		assertEquals (7, highPercent.getFinalTotal ());

		// Worst 75% tax rate = 12.75, but we have 6 minimum farmers so would be 18 population in a size 17 city - prove rebels will revert to farmers to avoid starving
		final CalculateCityUnrestBreakdown maxPercent = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_3_GOLD_75_UNREST, GenerateTestData.createDB ());
		assertEquals (17, maxPercent.getPopulation ());
		assertEquals (75, maxPercent.getTaxPercentage ());
		assertEquals (0, maxPercent.getRacialPercentage ());
		assertEquals (0, maxPercent.getRacialLiteral ());
		assertEquals (75, maxPercent.getTotalPercentage ());
		assertEquals (12, maxPercent.getBaseValue ());
		assertEquals (0, maxPercent.getBuildingsReducingUnrest ().length);
		assertEquals (12, maxPercent.getBaseTotal ());
		assertEquals (11, maxPercent.getFinalTotal ());

		// Add some buildings that reduce unrest - and back to 45% tax rate = 7.65
		final OverlandMapCoordinatesEx shrineLocation = new OverlandMapCoordinatesEx ();
		shrineLocation.setX (2);
		shrineLocation.setY (2);
		shrineLocation.setZ (0);

		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID (GenerateTestData.SHRINE);
		shrineBuilding.setCityLocation (shrineLocation);
		buildings.add (shrineBuilding);

		final CalculateCityUnrestBreakdown shrine = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, shrine.getPopulation ());
		assertEquals (45, shrine.getTaxPercentage ());
		assertEquals (0, shrine.getRacialPercentage ());
		assertEquals (0, shrine.getRacialLiteral ());
		assertEquals (45, shrine.getTotalPercentage ());
		assertEquals (7, shrine.getBaseValue ());
		assertEquals (1, shrine.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, shrine.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, shrine.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (6, shrine.getBaseTotal ());
		assertEquals (6, shrine.getFinalTotal ());

		// Divine power doesn't work on non-religious building
		final PlayerPick divinePower = new PlayerPick ();
		divinePower.setPickID (GenerateTestData.DIVINE_POWER);
		divinePower.setQuantity (1);
		ppk.getPick ().add (divinePower);

		final OverlandMapCoordinatesEx secondBuildingLocation = new OverlandMapCoordinatesEx ();
		secondBuildingLocation.setX (2);
		secondBuildingLocation.setY (2);
		secondBuildingLocation.setZ (0);

		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID (GenerateTestData.ANIMISTS_GUILD);
		secondBuilding.setCityLocation (secondBuildingLocation);
		buildings.add (secondBuilding);

		final CalculateCityUnrestBreakdown animistsGuild = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, animistsGuild.getPopulation ());
		assertEquals (45, animistsGuild.getTaxPercentage ());
		assertEquals (0, animistsGuild.getRacialPercentage ());
		assertEquals (0, animistsGuild.getRacialLiteral ());
		assertEquals (45, animistsGuild.getTotalPercentage ());
		assertEquals (7, animistsGuild.getBaseValue ());
		assertEquals (2, animistsGuild.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, animistsGuild.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, animistsGuild.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (GenerateTestData.ANIMISTS_GUILD, animistsGuild.getBuildingsReducingUnrest () [1].getBuildingID ());
		assertEquals (1, animistsGuild.getBuildingsReducingUnrest () [1].getUnrestReduction ());
		assertEquals (5, animistsGuild.getBaseTotal ());
		assertEquals (5, animistsGuild.getFinalTotal ());

		// Divine power does work on 2nd religious building
		secondBuilding.setBuildingID (GenerateTestData.TEMPLE);
		final CalculateCityUnrestBreakdown temple = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, temple.getPopulation ());
		assertEquals (45, temple.getTaxPercentage ());
		assertEquals (0, temple.getRacialPercentage ());
		assertEquals (0, temple.getRacialLiteral ());
		assertEquals (45, temple.getTotalPercentage ());
		assertEquals (7, temple.getBaseValue ());
		assertEquals (2, temple.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, temple.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, temple.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, temple.getBuildingsReducingUnrest () [1].getBuildingID ());
		assertEquals (1, temple.getBuildingsReducingUnrest () [1].getUnrestReduction ());
		assertEquals (4, temple.getBaseTotal ());
		assertEquals (4, temple.getFinalTotal ());

		// 1 unit does nothing
		final OverlandMapCoordinatesEx normalUnitLocation = new OverlandMapCoordinatesEx ();
		normalUnitLocation.setX (2);
		normalUnitLocation.setY (2);
		normalUnitLocation.setZ (0);

		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		normalUnit.setUnitLocation (normalUnitLocation);
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final CalculateCityUnrestBreakdown firstUnit = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, firstUnit.getPopulation ());
		assertEquals (45, firstUnit.getTaxPercentage ());
		assertEquals (0, firstUnit.getRacialPercentage ());
		assertEquals (0, firstUnit.getRacialLiteral ());
		assertEquals (45, firstUnit.getTotalPercentage ());
		assertEquals (7, firstUnit.getBaseValue ());
		assertEquals (2, firstUnit.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, firstUnit.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, firstUnit.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, firstUnit.getBuildingsReducingUnrest () [1].getBuildingID ());
		assertEquals (1, firstUnit.getBuildingsReducingUnrest () [1].getUnrestReduction ());
		assertEquals (1, firstUnit.getUnitCount ());
		assertEquals (0, firstUnit.getUnitReduction ());
		assertEquals (4, firstUnit.getBaseTotal ());
		assertEquals (4, firstUnit.getFinalTotal ());

		// 2nd unit reduces unrest, even if one is normal and one a hero
		final OverlandMapCoordinatesEx heroUnitLocation = new OverlandMapCoordinatesEx ();
		heroUnitLocation.setX (2);
		heroUnitLocation.setY (2);
		heroUnitLocation.setZ (0);

		final MemoryUnit heroUnit = new MemoryUnit ();
		heroUnit.setUnitID (GenerateTestData.DWARF_HERO);
		heroUnit.setUnitLocation (heroUnitLocation);
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);

		final CalculateCityUnrestBreakdown secondUnit = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, secondUnit.getPopulation ());
		assertEquals (45, secondUnit.getTaxPercentage ());
		assertEquals (0, secondUnit.getRacialPercentage ());
		assertEquals (0, secondUnit.getRacialLiteral ());
		assertEquals (45, secondUnit.getTotalPercentage ());
		assertEquals (7, secondUnit.getBaseValue ());
		assertEquals (2, secondUnit.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, secondUnit.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, secondUnit.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, secondUnit.getBuildingsReducingUnrest () [1].getBuildingID ());
		assertEquals (1, secondUnit.getBuildingsReducingUnrest () [1].getUnrestReduction ());
		assertEquals (2, secondUnit.getUnitCount ());
		assertEquals (1, secondUnit.getUnitReduction ());
		assertEquals (3, secondUnit.getBaseTotal ());
		assertEquals (3, secondUnit.getFinalTotal ());

		// summoned units or dead units don't help (unitCount still = 2)
		for (int n = 0; n < 2; n++)
		{
			final OverlandMapCoordinatesEx deadUnitLocation = new OverlandMapCoordinatesEx ();
			deadUnitLocation.setX (2);
			deadUnitLocation.setY (2);
			deadUnitLocation.setZ (0);

			final MemoryUnit deadUnit = new MemoryUnit ();
			deadUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
			deadUnit.setUnitLocation (deadUnitLocation);
			deadUnit.setStatus (UnitStatusID.DEAD);
			units.add (deadUnit);

			final OverlandMapCoordinatesEx summonedUnitLocation = new OverlandMapCoordinatesEx ();
			summonedUnitLocation.setX (2);
			summonedUnitLocation.setY (2);
			summonedUnitLocation.setZ (0);

			final MemoryUnit summonedUnit = new MemoryUnit ();
			summonedUnit.setUnitID (GenerateTestData.WAR_BEARS_UNIT);
			summonedUnit.setUnitLocation (summonedUnitLocation);
			summonedUnit.setStatus (UnitStatusID.ALIVE);
			units.add (summonedUnit);
		}

		final CalculateCityUnrestBreakdown extraUnits = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, extraUnits.getPopulation ());
		assertEquals (45, extraUnits.getTaxPercentage ());
		assertEquals (0, extraUnits.getRacialPercentage ());
		assertEquals (0, extraUnits.getRacialLiteral ());
		assertEquals (45, extraUnits.getTotalPercentage ());
		assertEquals (7, extraUnits.getBaseValue ());
		assertEquals (2, extraUnits.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, extraUnits.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, extraUnits.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, extraUnits.getBuildingsReducingUnrest () [1].getBuildingID ());
		assertEquals (1, extraUnits.getBuildingsReducingUnrest () [1].getUnrestReduction ());
		assertEquals (2, extraUnits.getUnitCount ());
		assertEquals (1, extraUnits.getUnitReduction ());
		assertEquals (3, extraUnits.getBaseTotal ());
		assertEquals (3, extraUnits.getFinalTotal ());

		// Put our captial here, and its klackons so we get -2
		final OverlandMapCoordinatesEx fortressLocation = new OverlandMapCoordinatesEx ();
		fortressLocation.setX (2);
		fortressLocation.setY (2);
		fortressLocation.setZ (0);

		final MemoryBuilding fortressBuilding = new MemoryBuilding ();
		fortressBuilding.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (fortressLocation);
		buildings.add (fortressBuilding);

		final CalculateCityUnrestBreakdown klackons = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, klackons.getPopulation ());
		assertEquals (45, klackons.getTaxPercentage ());
		assertEquals (0, klackons.getRacialPercentage ());
		assertEquals (-2, klackons.getRacialLiteral ());
		assertEquals (45, klackons.getTotalPercentage ());
		assertEquals (7, klackons.getBaseValue ());
		assertEquals (2, klackons.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, klackons.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, klackons.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, klackons.getBuildingsReducingUnrest () [1].getBuildingID ());
		assertEquals (1, klackons.getBuildingsReducingUnrest () [1].getUnrestReduction ());
		assertEquals (2, klackons.getUnitCount ());
		assertEquals (1, klackons.getUnitReduction ());
		assertEquals (1, klackons.getBaseTotal ());
		assertEquals (1, klackons.getFinalTotal ());

		// Other races get no bonus from being same as capital race
		cityData.setCityRaceID (GenerateTestData.HIGH_ELF);

		final CalculateCityUnrestBreakdown highElves = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, highElves.getPopulation ());
		assertEquals (45, highElves.getTaxPercentage ());
		assertEquals (0, highElves.getRacialPercentage ());
		assertEquals (0, highElves.getRacialLiteral ());
		assertEquals (45, highElves.getTotalPercentage ());
		assertEquals (7, highElves.getBaseValue ());
		assertEquals (2, highElves.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, highElves.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, highElves.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, highElves.getBuildingsReducingUnrest () [1].getBuildingID ());
		assertEquals (1, highElves.getBuildingsReducingUnrest () [1].getUnrestReduction ());
		assertEquals (2, highElves.getUnitCount ());
		assertEquals (1, highElves.getUnitReduction ());
		assertEquals (3, highElves.getBaseTotal ());
		assertEquals (3, highElves.getFinalTotal ());

		// Move capital to a different city with a different race
		final OverlandMapCoordinatesEx capitalCityLocation = new OverlandMapCoordinatesEx ();
		capitalCityLocation.setX (20);
		capitalCityLocation.setY (2);
		capitalCityLocation.setZ (0);

		final OverlandMapCityData capitalCityData = new OverlandMapCityData ();
		capitalCityData.setCityRaceID (GenerateTestData.DWARVES);
		capitalCityData.setCityOwnerID (1);
		capitalCityData.setCityPopulation (1000);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (20).setCityData (capitalCityData);

		fortressBuilding.setCityLocation (capitalCityLocation);

		final CalculateCityUnrestBreakdown racialUnrest = calc.calculateCityRebels
			(players, map, units, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, GenerateTestData.createDB ());
		assertEquals (17, racialUnrest.getPopulation ());
		assertEquals (45, racialUnrest.getTaxPercentage ());
		assertEquals (30, racialUnrest.getRacialPercentage ());
		assertEquals (0, racialUnrest.getRacialLiteral ());
		assertEquals (75, racialUnrest.getTotalPercentage ());
		assertEquals (12, racialUnrest.getBaseValue ());
		assertEquals (2, racialUnrest.getBuildingsReducingUnrest ().length);
		assertEquals (GenerateTestData.SHRINE, racialUnrest.getBuildingsReducingUnrest () [0].getBuildingID ());
		assertEquals (1, racialUnrest.getBuildingsReducingUnrest () [0].getUnrestReduction ());
		assertEquals (GenerateTestData.TEMPLE, racialUnrest.getBuildingsReducingUnrest () [1].getBuildingID ());
		assertEquals (1, racialUnrest.getBuildingsReducingUnrest () [1].getUnrestReduction ());
		assertEquals (2, racialUnrest.getUnitCount ());
		assertEquals (1, racialUnrest.getUnitReduction ());
		assertEquals (8, racialUnrest.getBaseTotal ());
		assertEquals (8, racialUnrest.getFinalTotal ());
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
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (1);

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
		final CalculateCityProductionResultsImplementation baseNoPeople = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, false, GenerateTestData.createDB ());
		assertEquals (4, baseNoPeople.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, baseNoPeople.getResults ().get (0).getProductionTypeID ());
		assertEquals (8, baseNoPeople.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (4, baseNoPeople.getResults ().get (0).getBaseProductionAmount ());				// 2 x2 from wild game = 4
		assertEquals (0, baseNoPeople.getResults ().get (0).getPercentageBonus ());
		assertEquals (4, baseNoPeople.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getResults ().get (0).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, baseNoPeople.getResults ().get (1).getProductionTypeID ());
		assertEquals (0, baseNoPeople.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (0, baseNoPeople.getResults ().get (1).getBaseProductionAmount ());
		assertEquals (9, baseNoPeople.getResults ().get (1).getPercentageBonus ());						// 3 hills giving 3% each
		assertEquals (0, baseNoPeople.getResults ().get (1).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, baseNoPeople.getResults ().get (2).getProductionTypeID ());
		assertEquals (0, baseNoPeople.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (0, baseNoPeople.getResults ().get (2).getBaseProductionAmount ());
		assertEquals (20, baseNoPeople.getResults ().get (2).getPercentageBonus ());
		assertEquals (0, baseNoPeople.getResults ().get (2).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getResults ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, baseNoPeople.getResults ().get (3).getProductionTypeID ());
		assertEquals (35, baseNoPeople.getResults ().get (3).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, baseNoPeople.getResults ().get (3).getBaseProductionAmount ());
		assertEquals (0, baseNoPeople.getResults ().get (3).getPercentageBonus ());
		assertEquals (18, baseNoPeople.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, baseNoPeople.getResults ().get (3).getConsumptionAmount ());

		final CalculateCityProductionResultsImplementation baseWithPeople = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (4, baseWithPeople.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, baseWithPeople.getResults ().get (0).getProductionTypeID ());
		assertEquals (40, baseWithPeople.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (20, baseWithPeople.getResults ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, baseWithPeople.getResults ().get (0).getPercentageBonus ());
		assertEquals (20, baseWithPeople.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (17, baseWithPeople.getResults ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, baseWithPeople.getResults ().get (1).getProductionTypeID ());
		assertEquals (36, baseWithPeople.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (18, baseWithPeople.getResults ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (9, baseWithPeople.getResults ().get (1).getPercentageBonus ());					// 3 hills giving 3% each
		assertEquals (19, baseWithPeople.getResults ().get (1).getModifiedProductionAmount ());	// 18 * 1.09 = 19.62
		assertEquals (0, baseWithPeople.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, baseWithPeople.getResults ().get (2).getProductionTypeID ());
		assertEquals (60, baseWithPeople.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (30, baseWithPeople.getResults ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, baseWithPeople.getResults ().get (2).getPercentageBonus ());
		assertEquals (36, baseWithPeople.getResults ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (0, baseWithPeople.getResults ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, baseWithPeople.getResults ().get (3).getProductionTypeID ());
		assertEquals (35, baseWithPeople.getResults ().get (3).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, baseWithPeople.getResults ().get (3).getBaseProductionAmount ());
		assertEquals (0, baseWithPeople.getResults ().get (3).getPercentageBonus ());
		assertEquals (18, baseWithPeople.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, baseWithPeople.getResults ().get (3).getConsumptionAmount ());

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

		final OverlandMapCoordinatesEx fortressLocation = new OverlandMapCoordinatesEx ();
		fortressLocation.setX (2);
		fortressLocation.setY (2);
		fortressLocation.setZ (1);

		final MemoryBuilding fortressBuilding = new MemoryBuilding ();
		fortressBuilding.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (fortressLocation);
		buildings.add (fortressBuilding);

		final CalculateCityProductionResultsImplementation fortress = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (5, fortress.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, fortress.getResults ().get (0).getProductionTypeID ());
		assertEquals (40, fortress.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (20, fortress.getResults ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, fortress.getResults ().get (0).getPercentageBonus ());
		assertEquals (20, fortress.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (17, fortress.getResults ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, fortress.getResults ().get (1).getProductionTypeID ());
		assertEquals (36, fortress.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (18, fortress.getResults ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (9, fortress.getResults ().get (1).getPercentageBonus ());					// 3 hills giving 3% each
		assertEquals (19, fortress.getResults ().get (1).getModifiedProductionAmount ());	// 18 * 1.09 = 19.62
		assertEquals (0, fortress.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, fortress.getResults ().get (2).getProductionTypeID ());
		assertEquals (60, fortress.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (30, fortress.getResults ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, fortress.getResults ().get (2).getPercentageBonus ());
		assertEquals (36, fortress.getResults ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (0, fortress.getResults ().get (2).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, fortress.getResults ().get (3).getProductionTypeID ());
		assertEquals (26, fortress.getResults ().get (3).getDoubleProductionAmount ());
		assertEquals (13, fortress.getResults ().get (3).getBaseProductionAmount ());			// 8 books + 5 for being on myrror = 13
		assertEquals (0, fortress.getResults ().get (3).getPercentageBonus ());
		assertEquals (13, fortress.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, fortress.getResults ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, fortress.getResults ().get (4).getProductionTypeID ());
		assertEquals (35, fortress.getResults ().get (4).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, fortress.getResults ().get (4).getBaseProductionAmount ());
		assertEquals (0, fortress.getResults ().get (4).getPercentageBonus ());
		assertEquals (18, fortress.getResults ().get (4).getModifiedProductionAmount ());
		assertEquals (0, fortress.getResults ().get (4).getConsumptionAmount ());

		// Add some buildings that give production (both regular like sages guild, and percentage like sawmill), and consumption
		final OverlandMapCoordinatesEx sagesGuildLocation = new OverlandMapCoordinatesEx ();
		sagesGuildLocation.setX (2);
		sagesGuildLocation.setY (2);
		sagesGuildLocation.setZ (1);

		final MemoryBuilding sagesGuildBuilding = new MemoryBuilding ();
		sagesGuildBuilding.setBuildingID (GenerateTestData.SAGES_GUILD);
		sagesGuildBuilding.setCityLocation (sagesGuildLocation);
		buildings.add (sagesGuildBuilding);

		final OverlandMapCoordinatesEx sawmillLocation = new OverlandMapCoordinatesEx ();
		sawmillLocation.setX (2);
		sawmillLocation.setY (2);
		sawmillLocation.setZ (1);

		final MemoryBuilding sawmillBuilding = new MemoryBuilding ();
		sawmillBuilding.setBuildingID (GenerateTestData.SAWMILL);
		sawmillBuilding.setCityLocation (sawmillLocation);
		buildings.add (sawmillBuilding);

		final CalculateCityProductionResultsImplementation sawmill = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (6, sawmill.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, sawmill.getResults ().get (0).getProductionTypeID ());
		assertEquals (40, sawmill.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (20, sawmill.getResults ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, sawmill.getResults ().get (0).getPercentageBonus ());
		assertEquals (20, sawmill.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (17, sawmill.getResults ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, sawmill.getResults ().get (1).getProductionTypeID ());
		assertEquals (36, sawmill.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (18, sawmill.getResults ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (34, sawmill.getResults ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill
		assertEquals (24, sawmill.getResults ().get (1).getModifiedProductionAmount ());	// 18 * 1.34 = 24.12
		assertEquals (0, sawmill.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, sawmill.getResults ().get (2).getProductionTypeID ());
		assertEquals (60, sawmill.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (30, sawmill.getResults ().get (2).getBaseProductionAmount ());			// 15 non-rebels x2 = 30
		assertEquals (20, sawmill.getResults ().get (2).getPercentageBonus ());
		assertEquals (36, sawmill.getResults ().get (2).getModifiedProductionAmount ());	// 30 * 1.2 = 36
		assertEquals (4, sawmill.getResults ().get (2).getConsumptionAmount ());				// 2 buildings costing 2 gold each
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, sawmill.getResults ().get (3).getProductionTypeID ());
		assertEquals (26, sawmill.getResults ().get (3).getDoubleProductionAmount ());
		assertEquals (13, sawmill.getResults ().get (3).getBaseProductionAmount ());			// 8 books + 5 for being on myrror = 13
		assertEquals (0, sawmill.getResults ().get (3).getPercentageBonus ());
		assertEquals (13, sawmill.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getResults ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, sawmill.getResults ().get (4).getProductionTypeID ());
		assertEquals (6, sawmill.getResults ().get (4).getDoubleProductionAmount ());
		assertEquals (3, sawmill.getResults ().get (4).getBaseProductionAmount ());			// 3 from sages' guild
		assertEquals (0, sawmill.getResults ().get (4).getPercentageBonus ());
		assertEquals (3, sawmill.getResults ().get (4).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getResults ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, sawmill.getResults ().get (5).getProductionTypeID ());
		assertEquals (35, sawmill.getResults ().get (5).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, sawmill.getResults ().get (5).getBaseProductionAmount ());
		assertEquals (0, sawmill.getResults ().get (5).getPercentageBonus ());
		assertEquals (18, sawmill.getResults ().get (5).getModifiedProductionAmount ());
		assertEquals (0, sawmill.getResults ().get (5).getConsumptionAmount ());

		// Add some map features, note there's already 2 wild game been added above
		map.getPlane ().get (1).getRow ().get (0).getCell ().get (3).getTerrainData ().setMapFeatureID (GenerateTestData.GEMS);
		map.getPlane ().get (1).getRow ().get (1).getCell ().get (3).getTerrainData ().setMapFeatureID (GenerateTestData.ADAMANTIUM_ORE);

		final CalculateCityProductionResultsImplementation minerals = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (6, minerals.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, minerals.getResults ().get (0).getProductionTypeID ());
		assertEquals (40, minerals.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (20, minerals.getResults ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, minerals.getResults ().get (0).getPercentageBonus ());
		assertEquals (20, minerals.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (17, minerals.getResults ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, minerals.getResults ().get (1).getProductionTypeID ());
		assertEquals (36, minerals.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (18, minerals.getResults ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (34, minerals.getResults ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from minerals
		assertEquals (24, minerals.getResults ().get (1).getModifiedProductionAmount ());		// 18 * 1.34 = 24.12
		assertEquals (0, minerals.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, minerals.getResults ().get (2).getProductionTypeID ());
		assertEquals (70, minerals.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (35, minerals.getResults ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +5 from gems = 35
		assertEquals (20, minerals.getResults ().get (2).getPercentageBonus ());
		assertEquals (42, minerals.getResults ().get (2).getModifiedProductionAmount ());		// 35 * 1.2 = 42
		assertEquals (4, minerals.getResults ().get (2).getConsumptionAmount ());					// 2 buildings costing 2 gold each
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, minerals.getResults ().get (3).getProductionTypeID ());
		assertEquals (30, minerals.getResults ().get (3).getDoubleProductionAmount ());
		assertEquals (15, minerals.getResults ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +2 from adamantium = 15
		assertEquals (0, minerals.getResults ().get (3).getPercentageBonus ());
		assertEquals (15, minerals.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, minerals.getResults ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, minerals.getResults ().get (4).getProductionTypeID ());
		assertEquals (6, minerals.getResults ().get (4).getDoubleProductionAmount ());
		assertEquals (3, minerals.getResults ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, minerals.getResults ().get (4).getPercentageBonus ());
		assertEquals (3, minerals.getResults ().get (4).getModifiedProductionAmount ());
		assertEquals (0, minerals.getResults ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, minerals.getResults ().get (5).getProductionTypeID ());
		assertEquals (35, minerals.getResults ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, minerals.getResults ().get (5).getBaseProductionAmount ());
		assertEquals (0, minerals.getResults ().get (5).getPercentageBonus ());
		assertEquals (18, minerals.getResults ().get (5).getModifiedProductionAmount ());
		assertEquals (0, minerals.getResults ().get (5).getConsumptionAmount ());

		// Miners' guild boosting bonuses from map features
		final OverlandMapCoordinatesEx minersGuildLocation = new OverlandMapCoordinatesEx ();
		minersGuildLocation.setX (2);
		minersGuildLocation.setY (2);
		minersGuildLocation.setZ (1);

		final MemoryBuilding minersGuildBuilding = new MemoryBuilding ();
		minersGuildBuilding.setBuildingID (GenerateTestData.MINERS_GUILD);
		minersGuildBuilding.setCityLocation (minersGuildLocation);
		buildings.add (minersGuildBuilding);

		final CalculateCityProductionResultsImplementation minersGuild = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (7, minersGuild.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, minersGuild.getResults ().get (0).getProductionTypeID ());
		assertEquals (40, minersGuild.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (20, minersGuild.getResults ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, minersGuild.getResults ().get (0).getPercentageBonus ());
		assertEquals (20, minersGuild.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (17, minersGuild.getResults ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, minersGuild.getResults ().get (1).getProductionTypeID ());
		assertEquals (36, minersGuild.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (18, minersGuild.getResults ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (84, minersGuild.getResults ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (33, minersGuild.getResults ().get (1).getModifiedProductionAmount ());		// 18 * 1.84 = 33.12
		assertEquals (0, minersGuild.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, minersGuild.getResults ().get (2).getProductionTypeID ());
		assertEquals (75, minersGuild.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (37, minersGuild.getResults ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +7.5 from gems = 37.5
		assertEquals (20, minersGuild.getResults ().get (2).getPercentageBonus ());
		assertEquals (44, minersGuild.getResults ().get (2).getModifiedProductionAmount ());		// 37 * 1.2 = 44.4
		assertEquals (7, minersGuild.getResults ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, minersGuild.getResults ().get (3).getProductionTypeID ());
		assertEquals (32, minersGuild.getResults ().get (3).getDoubleProductionAmount ());
		assertEquals (16, minersGuild.getResults ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16
		assertEquals (0, minersGuild.getResults ().get (3).getPercentageBonus ());
		assertEquals (16, minersGuild.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getResults ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, minersGuild.getResults ().get (4).getProductionTypeID ());
		assertEquals (6, minersGuild.getResults ().get (4).getDoubleProductionAmount ());
		assertEquals (3, minersGuild.getResults ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, minersGuild.getResults ().get (4).getPercentageBonus ());
		assertEquals (3, minersGuild.getResults ().get (4).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getResults ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, minersGuild.getResults ().get (5).getProductionTypeID ());
		assertEquals (35, minersGuild.getResults ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, minersGuild.getResults ().get (5).getBaseProductionAmount ());
		assertEquals (0, minersGuild.getResults ().get (5).getPercentageBonus ());
		assertEquals (18, minersGuild.getResults ().get (5).getModifiedProductionAmount ());
		assertEquals (0, minersGuild.getResults ().get (5).getConsumptionAmount ());

		// Dwarves double bonuses from map features, and also workers produce 3 production instead of 2
		cityData.setCityRaceID (GenerateTestData.DWARVES);

		final CalculateCityProductionResultsImplementation dwarves = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (7, dwarves.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, dwarves.getResults ().get (0).getProductionTypeID ());
		assertEquals (40, dwarves.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (20, dwarves.getResults ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, dwarves.getResults ().get (0).getPercentageBonus ());
		assertEquals (20, dwarves.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (17, dwarves.getResults ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, dwarves.getResults ().get (1).getProductionTypeID ());
		assertEquals (50, dwarves.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (25, dwarves.getResults ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 3) = 25
		assertEquals (84, dwarves.getResults ().get (1).getPercentageBonus ());						// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (46, dwarves.getResults ().get (1).getModifiedProductionAmount ());		// 25 * 1.84 = 46
		assertEquals (0, dwarves.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, dwarves.getResults ().get (2).getProductionTypeID ());
		assertEquals (90, dwarves.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (45, dwarves.getResults ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +15 from gems = 45
		assertEquals (20, dwarves.getResults ().get (2).getPercentageBonus ());
		assertEquals (54, dwarves.getResults ().get (2).getModifiedProductionAmount ());		// 45 * 1.2 = 54
		assertEquals (7, dwarves.getResults ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, dwarves.getResults ().get (3).getProductionTypeID ());
		assertEquals (38, dwarves.getResults ().get (3).getDoubleProductionAmount ());
		assertEquals (19, dwarves.getResults ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +6 from adamantium = 19
		assertEquals (0, dwarves.getResults ().get (3).getPercentageBonus ());
		assertEquals (19, dwarves.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getResults ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, dwarves.getResults ().get (4).getProductionTypeID ());
		assertEquals (6, dwarves.getResults ().get (4).getDoubleProductionAmount ());
		assertEquals (3, dwarves.getResults ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, dwarves.getResults ().get (4).getPercentageBonus ());
		assertEquals (3, dwarves.getResults ().get (4).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getResults ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, dwarves.getResults ().get (5).getProductionTypeID ());
		assertEquals (35, dwarves.getResults ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, dwarves.getResults ().get (5).getBaseProductionAmount ());
		assertEquals (0, dwarves.getResults ().get (5).getPercentageBonus ());
		assertEquals (18, dwarves.getResults ().get (5).getModifiedProductionAmount ());
		assertEquals (0, dwarves.getResults ().get (5).getConsumptionAmount ());

		// High elf rebels produce mana too
		cityData.setCityRaceID (GenerateTestData.HIGH_ELF);

		final CalculateCityProductionResultsImplementation highElves = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (7, highElves.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, highElves.getResults ().get (0).getProductionTypeID ());
		assertEquals (40, highElves.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (20, highElves.getResults ().get (0).getBaseProductionAmount ());			// (6 min + 2 optional farmers) x2 + (2 x2 from wild game) = 20
		assertEquals (0, highElves.getResults ().get (0).getPercentageBonus ());
		assertEquals (20, highElves.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (17, highElves.getResults ().get (0).getConsumptionAmount ());				// 17 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, highElves.getResults ().get (1).getProductionTypeID ());
		assertEquals (36, highElves.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (18, highElves.getResults ().get (1).getBaseProductionAmount ());			// (8 farmers x ½) + (7 workers x 2) = 18
		assertEquals (84, highElves.getResults ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (33, highElves.getResults ().get (1).getModifiedProductionAmount ());		// 18 * 1.84 = 33.12
		assertEquals (0, highElves.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, highElves.getResults ().get (2).getProductionTypeID ());
		assertEquals (75, highElves.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (37, highElves.getResults ().get (2).getBaseProductionAmount ());			// (15 non-rebels x2) +7.5 from gems = 37.5
		assertEquals (20, highElves.getResults ().get (2).getPercentageBonus ());
		assertEquals (44, highElves.getResults ().get (2).getModifiedProductionAmount ());		// 37 * 1.2 = 44.4
		assertEquals (7, highElves.getResults ().get (2).getConsumptionAmount ());				// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, highElves.getResults ().get (3).getProductionTypeID ());
		assertEquals (49, highElves.getResults ().get (3).getDoubleProductionAmount ());
		assertEquals (24, highElves.getResults ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +17 from pop = 49
		assertEquals (0, highElves.getResults ().get (3).getPercentageBonus ());
		assertEquals (24, highElves.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, highElves.getResults ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, highElves.getResults ().get (4).getProductionTypeID ());
		assertEquals (6, highElves.getResults ().get (4).getDoubleProductionAmount ());
		assertEquals (3, highElves.getResults ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, highElves.getResults ().get (4).getPercentageBonus ());
		assertEquals (3, highElves.getResults ().get (4).getModifiedProductionAmount ());
		assertEquals (0, highElves.getResults ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, highElves.getResults ().get (5).getProductionTypeID ());
		assertEquals (35, highElves.getResults ().get (5).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, highElves.getResults ().get (5).getBaseProductionAmount ());
		assertEquals (0, highElves.getResults ().get (5).getPercentageBonus ());
		assertEquals (18, highElves.getResults ().get (5).getModifiedProductionAmount ());
		assertEquals (0, highElves.getResults ().get (5).getConsumptionAmount ());

		// Shrink city to size 6 - gold % bonus is then capped at 6 x3 = 18%
		cityData.setCityPopulation (6900);
		cityData.setMinimumFarmers (1);
		cityData.setOptionalFarmers (1);
		cityData.setNumberOfRebels (1);		// 6 -1 -1 -1 = 3 workers

		final CalculateCityProductionResultsImplementation shrunk = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (7, shrunk.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, shrunk.getResults ().get (0).getProductionTypeID ());
		assertEquals (16, shrunk.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (8, shrunk.getResults ().get (0).getBaseProductionAmount ());				// (2 farmers x2) + (2 x2 from wild game) = 8
		assertEquals (0, shrunk.getResults ().get (0).getPercentageBonus ());
		assertEquals (8, shrunk.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (6, shrunk.getResults ().get (0).getConsumptionAmount ());				// 6 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, shrunk.getResults ().get (1).getProductionTypeID ());
		assertEquals (14, shrunk.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (7, shrunk.getResults ().get (1).getBaseProductionAmount ());				// (2 farmers x ½) + (3 workers x 2) = 7
		assertEquals (84, shrunk.getResults ().get (1).getPercentageBonus ());					// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (12, shrunk.getResults ().get (1).getModifiedProductionAmount ());		// 7 * 1.84 = 12.88
		assertEquals (0, shrunk.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, shrunk.getResults ().get (2).getProductionTypeID ());
		assertEquals (35, shrunk.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (17, shrunk.getResults ().get (2).getBaseProductionAmount ());			// (5 non-rebels x2) +7.5 from gems = 17.5
		assertEquals (18, shrunk.getResults ().get (2).getPercentageBonus ());					// Capped due to city size
		assertEquals (20, shrunk.getResults ().get (2).getModifiedProductionAmount ());		// 17 * 1.18 = 20.06
		assertEquals (7, shrunk.getResults ().get (2).getConsumptionAmount ());				// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, shrunk.getResults ().get (3).getProductionTypeID ());
		assertEquals (38, shrunk.getResults ().get (3).getDoubleProductionAmount ());
		assertEquals (19, shrunk.getResults ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +6 from pop = 38
		assertEquals (0, shrunk.getResults ().get (3).getPercentageBonus ());
		assertEquals (19, shrunk.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, shrunk.getResults ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, shrunk.getResults ().get (4).getProductionTypeID ());
		assertEquals (6, shrunk.getResults ().get (4).getDoubleProductionAmount ());
		assertEquals (3, shrunk.getResults ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, shrunk.getResults ().get (4).getPercentageBonus ());
		assertEquals (3, shrunk.getResults ().get (4).getModifiedProductionAmount ());
		assertEquals (0, shrunk.getResults ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, shrunk.getResults ().get (5).getProductionTypeID ());
		assertEquals (35, shrunk.getResults ().get (5).getDoubleProductionAmount ());		// 27 from terrain + (2 x4 from wild game) = 35
		assertEquals (18, shrunk.getResults ().get (5).getBaseProductionAmount ());
		assertEquals (0, shrunk.getResults ().get (5).getPercentageBonus ());
		assertEquals (18, shrunk.getResults ().get (5).getModifiedProductionAmount ());
		assertEquals (0, shrunk.getResults ().get (5).getConsumptionAmount ());

		// Cap max city size at 25
		for (int y = 0; y <= 4; y++)
		{
			final MemoryGridCell mc = map.getPlane ().get (1).getRow ().get (y).getCell ().get (1);
			if (mc.getTerrainData () == null)
				mc.setTerrainData (new OverlandMapTerrainData ());

			mc.getTerrainData ().setMapFeatureID (GenerateTestData.WILD_GAME);
		}

		final CalculateCityProductionResultsImplementation maxSize = (CalculateCityProductionResultsImplementation) calc.calculateAllCityProductions
			(players, map, buildings, cityLocation, GenerateTestData.TAX_RATE_2_GOLD_45_UNREST, sd, true, GenerateTestData.createDB ());
		assertEquals (7, maxSize.getResults ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, maxSize.getResults ().get (0).getProductionTypeID ());
		assertEquals (36, maxSize.getResults ().get (0).getDoubleProductionAmount ());
		assertEquals (18, maxSize.getResults ().get (0).getBaseProductionAmount ());			// (2 farmers x2) + (2 x7 from wild game) = 18
		assertEquals (0, maxSize.getResults ().get (0).getPercentageBonus ());
		assertEquals (18, maxSize.getResults ().get (0).getModifiedProductionAmount ());
		assertEquals (6, maxSize.getResults ().get (0).getConsumptionAmount ());					// 6 population eating
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, maxSize.getResults ().get (1).getProductionTypeID ());
		assertEquals (14, maxSize.getResults ().get (1).getDoubleProductionAmount ());
		assertEquals (7, maxSize.getResults ().get (1).getBaseProductionAmount ());				// (2 farmers x ½) + (3 workers x 2) = 7
		assertEquals (84, maxSize.getResults ().get (1).getPercentageBonus ());						// 3 hills giving 3% each +25% from sawmill +50% from miners' guild
		assertEquals (12, maxSize.getResults ().get (1).getModifiedProductionAmount ());		// 7 * 1.84 = 12.88
		assertEquals (0, maxSize.getResults ().get (1).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, maxSize.getResults ().get (2).getProductionTypeID ());
		assertEquals (35, maxSize.getResults ().get (2).getDoubleProductionAmount ());
		assertEquals (17, maxSize.getResults ().get (2).getBaseProductionAmount ());			// (5 non-rebels x2) +7.5 from gems = 17.5
		assertEquals (18, maxSize.getResults ().get (2).getPercentageBonus ());						// Capped due to city size
		assertEquals (20, maxSize.getResults ().get (2).getModifiedProductionAmount ());		// 17 * 1.18 = 20.06
		assertEquals (7, maxSize.getResults ().get (2).getConsumptionAmount ());					// 2 + 2 + 3
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, maxSize.getResults ().get (3).getProductionTypeID ());
		assertEquals (38, maxSize.getResults ().get (3).getDoubleProductionAmount ());
		assertEquals (19, maxSize.getResults ().get (3).getBaseProductionAmount ());			// 8 books +5 for being on myrror +3 from adamantium = 16 x2 = 32 +6 from pop = 38
		assertEquals (0, maxSize.getResults ().get (3).getPercentageBonus ());
		assertEquals (19, maxSize.getResults ().get (3).getModifiedProductionAmount ());
		assertEquals (0, maxSize.getResults ().get (3).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, maxSize.getResults ().get (4).getProductionTypeID ());
		assertEquals (6, maxSize.getResults ().get (4).getDoubleProductionAmount ());
		assertEquals (3, maxSize.getResults ().get (4).getBaseProductionAmount ());				// 3 from sages' guild
		assertEquals (0, maxSize.getResults ().get (4).getPercentageBonus ());
		assertEquals (3, maxSize.getResults ().get (4).getModifiedProductionAmount ());
		assertEquals (0, maxSize.getResults ().get (4).getConsumptionAmount ());
		assertEquals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, maxSize.getResults ().get (5).getProductionTypeID ());
		assertEquals (55, maxSize.getResults ().get (5).getDoubleProductionAmount ());			// 27 from terrain + (2 x7 from wild game) = 35
		assertEquals (25, maxSize.getResults ().get (5).getBaseProductionAmount ());
		assertEquals (0, maxSize.getResults ().get (5).getPercentageBonus ());
		assertEquals (25, maxSize.getResults ().get (5).getModifiedProductionAmount ());
		assertEquals (0, maxSize.getResults ().get (5).getConsumptionAmount ());
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
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (1);

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
