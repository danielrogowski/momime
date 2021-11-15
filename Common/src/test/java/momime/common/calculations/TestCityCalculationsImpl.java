package momime.common.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.areas.operations.BooleanMapAreaOperations2DImpl;
import com.ndg.map.areas.operations.MapAreaOperations2DImpl;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.BuildingRequiresTileType;
import momime.common.database.CitySpellEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.GenerateTestData;
import momime.common.database.MapFeatureEx;
import momime.common.database.OverlandMapSize;
import momime.common.database.Pick;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.ProductionAmountBucketID;
import momime.common.database.ProductionTypeAndDoubledValue;
import momime.common.database.ProductionTypeEx;
import momime.common.database.RaceEx;
import momime.common.database.RacePopulationTask;
import momime.common.database.RaceUnrest;
import momime.common.database.RecordNotFoundException;
import momime.common.database.RoundingDirectionID;
import momime.common.database.Spell;
import momime.common.database.TaxRate;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitEx;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownDying;
import momime.common.internal.CityGrowthRateBreakdownGrowing;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.CityUnrestBreakdownSpell;
import momime.common.messages.FogOfWarMemory;
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
import momime.common.messages.servertoclient.RenderCityData;
import momime.common.utils.CityProductionUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the calculations in the CityCalculationsImpl class
 */
@ExtendWith(MockitoExtension.class)
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

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
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

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final CityProductionBreakdown breakdown = calc.listCityProductionPercentageBonusesFromTerrainTiles (map, spells, new MapCoordinates3DEx (2, 2, 0), sys, db);
		
		// Check results
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
	 * Tests the calculateGoldTradeBonus method when no trade bonus is earned from any source
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldTradeBonus_None () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx adjacentTileType = new TileTypeEx ();
		adjacentTileType.setGoldBonus (10);
		when (db.findTileType ("TT02", "calculateGoldTradeBonus")).thenReturn (adjacentTileType);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Terrain
		final OverlandMapTerrainData adjacentTerrain = new OverlandMapTerrainData ();
		adjacentTerrain.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (adjacentTerrain);
		
		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		
		// Check results
		assertEquals (0, breakdown.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRace ());
		assertEquals (0, breakdown.getTradePercentageBonusUncapped ());
		assertEquals (0, breakdown.getTotalPopulation ());
		assertEquals (0, breakdown.getTradePercentageBonusCapped ());
		assertEquals (0, breakdown.getPercentageBonus ());
	}
	
	/**
	 * Tests the calculateGoldTradeBonus method when a trade bonus is earned from the adjacent tile, but it doesn't apply because there's no population yet
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldTradeBonus_Adjacent () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx adjacentTileType = new TileTypeEx ();
		adjacentTileType.setGoldBonus (10);
		adjacentTileType.setGoldBonusSurroundingTiles (true);
		when (db.findTileType ("TT02", "calculateGoldTradeBonus")).thenReturn (adjacentTileType);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Terrain
		final OverlandMapTerrainData adjacentTerrain = new OverlandMapTerrainData ();
		adjacentTerrain.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (adjacentTerrain);
		
		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		
		// Check results
		assertEquals (10, breakdown.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRace ());
		assertEquals (10, breakdown.getTradePercentageBonusUncapped ());
		assertEquals (0, breakdown.getTotalPopulation ());
		assertEquals (0, breakdown.getTradePercentageBonusCapped ());
		assertEquals (0, breakdown.getPercentageBonus ());
	}
	
	/**
	 * Tests the calculateGoldTradeBonus method when a trade bonus is earned from the adjacent tile, but is capped by a low population
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldTradeBonus_Capped () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx adjacentTileType = new TileTypeEx ();
		adjacentTileType.setGoldBonus (10);
		adjacentTileType.setGoldBonusSurroundingTiles (true);
		when (db.findTileType ("TT02", "calculateGoldTradeBonus")).thenReturn (adjacentTileType);

		final TileTypeEx uninterestingTileType = new TileTypeEx ();
		when (db.findTileType ("TT03", "calculateGoldTradeBonus")).thenReturn (uninterestingTileType);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Terrain
		final OverlandMapTerrainData adjacentTerrain = new OverlandMapTerrainData ();
		adjacentTerrain.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (adjacentTerrain);

		final OverlandMapTerrainData centreTerrain = new OverlandMapTerrainData ();
		centreTerrain.setTileTypeID ("TT03");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (centreTerrain);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (3456);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		
		// Check results
		assertEquals (10, breakdown.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRace ());
		assertEquals (10, breakdown.getTradePercentageBonusUncapped ());
		assertEquals (3, breakdown.getTotalPopulation ());
		assertEquals (9, breakdown.getTradePercentageBonusCapped ());
		assertEquals (9, breakdown.getPercentageBonus ());
	}
	
	/**
	 * Tests the calculateGoldTradeBonus method when a better trade bonus is earned from the centre tile, but is capped by a low population
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldTradeBonus_Centre () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx centreTileType = new TileTypeEx ();
		centreTileType.setGoldBonus (20);
		when (db.findTileType ("TT01", "calculateGoldTradeBonus")).thenReturn (centreTileType);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Terrain
		final OverlandMapTerrainData adjacentTerrain = new OverlandMapTerrainData ();
		adjacentTerrain.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (adjacentTerrain);

		final OverlandMapTerrainData centreTerrain = new OverlandMapTerrainData ();
		centreTerrain.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (centreTerrain);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (3456);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		
		// Check results
		assertEquals (20, breakdown.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRace ());
		assertEquals (20, breakdown.getTradePercentageBonusUncapped ());
		assertEquals (3, breakdown.getTotalPopulation ());
		assertEquals (9, breakdown.getTradePercentageBonusCapped ());
		assertEquals (9, breakdown.getPercentageBonus ());
	}
	
	/**
	 * Tests the calculateGoldTradeBonus method when a better trade bonus is earned from the centre tile, and there's enough population to support it
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldTradeBonus_Uncapped () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx centreTileType = new TileTypeEx ();
		centreTileType.setGoldBonus (20);
		when (db.findTileType ("TT01", "calculateGoldTradeBonus")).thenReturn (centreTileType);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Terrain
		final OverlandMapTerrainData adjacentTerrain = new OverlandMapTerrainData ();
		adjacentTerrain.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (adjacentTerrain);

		final OverlandMapTerrainData centreTerrain = new OverlandMapTerrainData ();
		centreTerrain.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (centreTerrain);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (11789);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		
		// Check results
		assertEquals (20, breakdown.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRace ());
		assertEquals (20, breakdown.getTradePercentageBonusUncapped ());
		assertEquals (11, breakdown.getTotalPopulation ());
		assertEquals (20, breakdown.getTradePercentageBonusCapped ());
		assertEquals (20, breakdown.getPercentageBonus ());
	}
	
	/**
	 * Tests the calculateGoldTradeBonus method when there's a terrain bonus earned but no race bonus
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldTradeBonus_NoRaceBonus () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx centreTileType = new TileTypeEx ();
		centreTileType.setGoldBonus (20);
		when (db.findTileType ("TT01", "calculateGoldTradeBonus")).thenReturn (centreTileType);
		
		final TileTypeEx adjacentTileType = new TileTypeEx ();
		adjacentTileType.setGoldBonus (10);
		adjacentTileType.setGoldBonusSurroundingTiles (true);

		final RaceEx uninterestingRace = new RaceEx ();
		when (db.findRace ("RC01", "calculateGoldTradeBonus")).thenReturn (uninterestingRace);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Terrain
		final OverlandMapTerrainData adjacentTerrain = new OverlandMapTerrainData ();
		adjacentTerrain.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (adjacentTerrain);

		final OverlandMapTerrainData centreTerrain = new OverlandMapTerrainData ();
		centreTerrain.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (centreTerrain);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (11789);
		cityData.setCityRaceID ("RC01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		
		// Check results
		assertEquals (20, breakdown.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRoads ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRace ());
		assertEquals (20, breakdown.getTradePercentageBonusUncapped ());
		assertEquals (11, breakdown.getTotalPopulation ());
		assertEquals (20, breakdown.getTradePercentageBonusCapped ());
		assertEquals (20, breakdown.getPercentageBonus ());
	}
	
	/**
	 * Tests the calculateGoldTradeBonus method when there's a terrain bonus earned but no race bonus
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	@Test
	public final void testCalculateGoldTradeBonus_WithRaceBonus () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TileTypeEx centreTileType = new TileTypeEx ();
		centreTileType.setGoldBonus (20);
		when (db.findTileType ("TT01", "calculateGoldTradeBonus")).thenReturn (centreTileType);
		
		final RaceEx nomads = new RaceEx ();
		nomads.setGoldTradeBonus (50);
		when (db.findRace ("RC02", "calculateGoldTradeBonus")).thenReturn (nomads);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Terrain
		final OverlandMapTerrainData adjacentTerrain = new OverlandMapTerrainData ();
		adjacentTerrain.setTileTypeID ("TT02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (adjacentTerrain);

		final OverlandMapTerrainData centreTerrain = new OverlandMapTerrainData ();
		centreTerrain.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (centreTerrain);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (11789);
		cityData.setCityRaceID ("RC02");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		calc.calculateGoldTradeBonus (breakdown, map, new MapCoordinates3DEx (20, 10, 1), null, sys, db);
		
		// Check results
		assertEquals (20, breakdown.getTradePercentageBonusFromTileType ());
		assertEquals (0, breakdown.getTradePercentageBonusFromRoads ());
		assertEquals (50, breakdown.getTradePercentageBonusFromRace ());
		assertEquals (70, breakdown.getTradePercentageBonusUncapped ());
		assertEquals (11, breakdown.getTotalPopulation ());
		assertEquals (33, breakdown.getTradePercentageBonusCapped ());
		assertEquals (33, breakdown.getPercentageBonus ());
	}

	/**
	 * Tests the buildingPassesTileTypeRequirements method, where the building has no tile type requirement so just automatically passes
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Test
	public final void testBuildingPassesTileTypeRequirements_NoRequirements () throws RecordNotFoundException
	{
		// Building we're trying to place
		final Building building = new Building ();
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Run method
		assertTrue (calc.buildingPassesTileTypeRequirements (null, new MapCoordinates3DEx (20, 10, 1), building, null));
	}

	/**
	 * Tests the buildingPassesTileTypeRequirements method, where the building requires a specific tile adjacent to the city, and we have it
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Test
	public final void testBuildingPassesTileTypeRequirements_DistanceOne_Matches () throws RecordNotFoundException
	{
		// Building we're trying to place
		final BuildingRequiresTileType buildingRequiresTileType = new BuildingRequiresTileType ();
		buildingRequiresTileType.setDistance (1);
		buildingRequiresTileType.setTileTypeID ("TT01");
		
		final Building building = new Building ();
		building.getBuildingRequiresTileType ().add (buildingRequiresTileType);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		final OverlandMapTerrainData oneAway = new OverlandMapTerrainData ();
		oneAway.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (oneAway);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertTrue (calc.buildingPassesTileTypeRequirements (map, new MapCoordinates3DEx (20, 10, 1), building, sys));
	}
	
	/**
	 * Tests the buildingPassesTileTypeRequirements method, where the building requires a specific tile adjacent to the city, and we don't have it
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Test
	public final void testBuildingPassesTileTypeRequirements_DistanceOne_NoMatch () throws RecordNotFoundException
	{
		// Building we're trying to place
		final BuildingRequiresTileType buildingRequiresTileType = new BuildingRequiresTileType ();
		buildingRequiresTileType.setDistance (1);
		buildingRequiresTileType.setTileTypeID ("TT01");
		
		final Building building = new Building ();
		building.getBuildingRequiresTileType ().add (buildingRequiresTileType);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		final OverlandMapTerrainData oneAway = new OverlandMapTerrainData ();
		oneAway.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (22).setTerrainData (oneAway);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertFalse (calc.buildingPassesTileTypeRequirements (map, new MapCoordinates3DEx (20, 10, 1), building, sys));
	}
	
	/**
	 * Tests the buildingPassesTileTypeRequirements method, where the building requires one of two possible tiles adjacent to the city, and we have one of them it
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Test
	public final void testBuildingPassesTileTypeRequirements_DistanceOne_Or () throws RecordNotFoundException
	{
		// Building we're trying to place
		final Building building = new Building ();

		for (int n = 1; n <= 2; n++)
		{
			final BuildingRequiresTileType buildingRequiresTileType = new BuildingRequiresTileType ();
			buildingRequiresTileType.setDistance (1);
			buildingRequiresTileType.setTileTypeID ("TT0" + n);
			building.getBuildingRequiresTileType ().add (buildingRequiresTileType);
		}
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		final OverlandMapTerrainData oneAway = new OverlandMapTerrainData ();
		oneAway.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (oneAway);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertTrue (calc.buildingPassesTileTypeRequirements (map, new MapCoordinates3DEx (20, 10, 1), building, sys));
	}
	
	/**
	 * Tests the buildingPassesTileTypeRequirements method, where the building requires a specific tile in the city terrain area, and we have it
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Test
	public final void testBuildingPassesTileTypeRequirements_DistanceTwo_Matches () throws RecordNotFoundException
	{
		// Building we're trying to place
		final BuildingRequiresTileType buildingRequiresTileType = new BuildingRequiresTileType ();
		buildingRequiresTileType.setDistance (2);
		buildingRequiresTileType.setTileTypeID ("TT01");
		
		final Building building = new Building ();
		building.getBuildingRequiresTileType ().add (buildingRequiresTileType);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		final OverlandMapTerrainData oneAway = new OverlandMapTerrainData ();
		oneAway.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (22).setTerrainData (oneAway);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertTrue (calc.buildingPassesTileTypeRequirements (map, new MapCoordinates3DEx (20, 10, 1), building, sys));
	}

	/**
	 * Tests the buildingPassesTileTypeRequirements method, where the building requires a specific tile in the city terrain area, that the corner tiles don't count
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Test
	public final void testBuildingPassesTileTypeRequirements_DistanceTwo_Corner () throws RecordNotFoundException
	{
		// Building we're trying to place
		final BuildingRequiresTileType buildingRequiresTileType = new BuildingRequiresTileType ();
		buildingRequiresTileType.setDistance (2);
		buildingRequiresTileType.setTileTypeID ("TT01");
		
		final Building building = new Building ();
		building.getBuildingRequiresTileType ().add (buildingRequiresTileType);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		final OverlandMapTerrainData oneAway = new OverlandMapTerrainData ();
		oneAway.setTileTypeID ("TT01");
		map.getPlane ().get (1).getRow ().get (12).getCell ().get (22).setTerrainData (oneAway);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertFalse (calc.buildingPassesTileTypeRequirements (map, new MapCoordinates3DEx (20, 10, 1), building, sys));
	}

	/**
	 * Tests the listCityFoodProductionFromTerrainTiles method where there is none, because no tile types are specified
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListCityFoodProductionFromTerrainTiles_None () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Session description
		final CoordinateSystem overlandMapCoordinateSystem = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final CityProductionBreakdown breakdown = calc.listCityFoodProductionFromTerrainTiles
			(map, new MapCoordinates3DEx (20, 10, 1), overlandMapCoordinateSystem, db);
		
		// Check results
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, breakdown.getProductionTypeID ());
		assertEquals (0, breakdown.getTileTypeProduction ().size ());
	}
	
	/**
	 * Tests the listCityFoodProductionFromTerrainTiles method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListCityFoodProductionFromTerrainTiles_Some () throws Exception
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
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Session description
		final CoordinateSystem overlandMapCoordinateSystem = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Add some tile types that grant food, 3 hills and 5 rivers, (3*1) + (4*5) = 23
		// Note we add hills in the NW and NE corners too, outside the city radius, so we prove that this isn't counted
		// Also adds some mountains, which don't produce any food at all, to prove they don't get included in the breakdown
		for (int x = 0; x <= 4; x++)
		{
			final OverlandMapTerrainData hillsTerrain = new OverlandMapTerrainData ();
			hillsTerrain.setTileTypeID ("TT01");
			map.getPlane ().get (1).getRow ().get (8).getCell ().get (18 + x).setTerrainData (hillsTerrain);

			final OverlandMapTerrainData riverTile = new OverlandMapTerrainData ();
			riverTile.setTileTypeID ("TT02");
			map.getPlane ().get (1).getRow ().get (9).getCell ().get (18 + x).setTerrainData (riverTile);

			final OverlandMapTerrainData mountainsTile = new OverlandMapTerrainData ();
			mountainsTile.setTileTypeID ("TT03");
			map.getPlane ().get (1).getRow ().get (10).getCell ().get (18 + x).setTerrainData (mountainsTile);
		}

		// Additions
		final CityProductionUtils cityProductionUtils = mock (CityProductionUtils.class);
		when (cityProductionUtils.addProductionAmountToBreakdown (any (CityProductionBreakdown.class), anyInt (),
			eq (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES), eq (db))).thenReturn (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		calc.setCityProductionUtils (cityProductionUtils);
		
		// Run method
		final CityProductionBreakdown breakdown = calc.listCityFoodProductionFromTerrainTiles
			(map, new MapCoordinates3DEx (20, 10, 1), overlandMapCoordinateSystem, db);
		
		// Check results
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, breakdown.getProductionTypeID ());
		assertEquals (2, breakdown.getTileTypeProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, breakdown.getTileTypeProduction ().get (0).getProductionAmountBucketID ());
		assertEquals ("TT02", breakdown.getTileTypeProduction ().get (0).getTileTypeID ());
		assertEquals (5, breakdown.getTileTypeProduction ().get (0).getCount ());
		assertEquals (4, breakdown.getTileTypeProduction ().get (0).getDoubleProductionAmountEachTile ());
		assertEquals (20, breakdown.getTileTypeProduction ().get (0).getDoubleProductionAmountAllTiles ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, breakdown.getTileTypeProduction ().get (1).getProductionAmountBucketID ());
		assertEquals ("TT01", breakdown.getTileTypeProduction ().get (1).getTileTypeID ());
		assertEquals (3, breakdown.getTileTypeProduction ().get (1).getCount ());
		assertEquals (1, breakdown.getTileTypeProduction ().get (1).getDoubleProductionAmountEachTile ());
		assertEquals (3, breakdown.getTileTypeProduction ().get (1).getDoubleProductionAmountAllTiles ());
		
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (breakdown, 20, ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, db);
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (breakdown, 3, ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, db);
	}

	/**
	 * Tests the calculateCityGrowthRate method when the city is already at max size
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_MaxSize () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Owner
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (10000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 10, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdown.class.getName (), breakdown.getClass ().getName ());
		assertEquals (10000, breakdown.getCurrentPopulation ());
		assertEquals (10000, breakdown.getMaximumPopulation ());
		assertEquals (0, breakdown.getInitialTotal ());
		assertEquals (0, breakdown.getCappedTotal ());
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the population is growing, and the difference between the current size and max size is an even number
	 * This is the example quoted in the strategy guide, however note the example is in contradiction with the formula - from testing
	 * I believe the example is right and the formula is supposed to be a -1 not a +1
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Growing_Even () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final RaceEx standardRace = new RaceEx ();
		when (db.findRace ("RC01", "calculateCityGrowthRate")).thenReturn (standardRace);
		
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
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (pd.getPlayerID ());
		cityData.setCityPopulation (12000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 22, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing growingEven = (CityGrowthRateBreakdownGrowing) breakdown;
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
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the population is growing, and the difference between the current size and max size is an odd number
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Growing_Odd () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final RaceEx standardRace = new RaceEx ();
		when (db.findRace ("RC01", "calculateCityGrowthRate")).thenReturn (standardRace);
		
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
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (pd.getPlayerID ());
		cityData.setCityPopulation (12000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 23, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing growingOdd = (CityGrowthRateBreakdownGrowing) breakdown;
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
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the population is growing, and the race gets a growth rate bonus
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Growing_RaceBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final RaceEx raceWithBonus = new RaceEx ();
		raceWithBonus.setGrowthRateModifier (20);
		when (db.findRace ("RC02", "calculateCityGrowthRate")).thenReturn (raceWithBonus);
		
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
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC02");
		cityData.setCityOwnerID (pd.getPlayerID ());
		cityData.setCityPopulation (12000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 22, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing barbarian = (CityGrowthRateBreakdownGrowing) breakdown;
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
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the population is growing, and the race gets a growth rate penalty
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Growing_RacePenalty () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final RaceEx raceWithPenalty = new RaceEx ();
		raceWithPenalty.setGrowthRateModifier (-20);
		when (db.findRace ("RC03", "calculateCityGrowthRate")).thenReturn (raceWithPenalty);
		
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
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC03");
		cityData.setCityOwnerID (pd.getPlayerID ());
		cityData.setCityPopulation (12000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 22, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing highElf = (CityGrowthRateBreakdownGrowing) breakdown;
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
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the population is growing, and the city has some buildings that increase growth rate
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Growing_BuildingBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

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
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC03");
		cityData.setCityOwnerID (pd.getPlayerID ());
		cityData.setCityPopulation (12000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID ("BL01");
		granary.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (granary);

		final MemoryBuilding farmersMarket = new MemoryBuilding ();
		farmersMarket.setBuildingID ("BL02");
		farmersMarket.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (farmersMarket);

		final MemoryBuilding sagesGuild = new MemoryBuilding ();		// Irrelevant building, to prove it doesn't get included in the list
		sagesGuild.setBuildingID ("BL03");
		sagesGuild.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (sagesGuild);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 22, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing withBuildings = (CityGrowthRateBreakdownGrowing) breakdown;
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
	}

	/**
	 * Tests the calculateCityGrowthRate method when the population is growing, and its an AI player who get a pretty huge bonus
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Growing_AIBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

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
		pd.setHuman (false);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID ("WZ01");

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null);
		
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityGrowthRate")).thenReturn (player);
		
		// Spells
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC03");
		cityData.setCityOwnerID (pd.getPlayerID ());
		cityData.setCityPopulation (12000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID ("BL01");
		granary.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (granary);

		final MemoryBuilding farmersMarket = new MemoryBuilding ();
		farmersMarket.setBuildingID ("BL02");
		farmersMarket.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (farmersMarket);

		final MemoryBuilding sagesGuild = new MemoryBuilding ();		// Irrelevant building, to prove it doesn't get included in the list
		sagesGuild.setBuildingID ("BL03");
		sagesGuild.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (sagesGuild);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 22, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing withBuildings = (CityGrowthRateBreakdownGrowing) breakdown;
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
		assertEquals (300, withBuildings.getDifficultyLevelMultiplier ());
		assertEquals (240, withBuildings.getTotalGrowthRateAdjustedForDifficultyLevel ());
		assertEquals (240, withBuildings.getInitialTotal ());
		assertEquals (240, withBuildings.getCappedTotal ());
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the population is growing and almost at max size, but we can still grow by the calculated amount
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Growing_AlmostCapped () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

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
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC03");
		cityData.setCityOwnerID (pd.getPlayerID ());
		cityData.setCityPopulation (21960);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID ("BL01");
		granary.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (granary);

		final MemoryBuilding farmersMarket = new MemoryBuilding ();
		farmersMarket.setBuildingID ("BL02");
		farmersMarket.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (farmersMarket);

		final MemoryBuilding sagesGuild = new MemoryBuilding ();		// Irrelevant building, to prove it doesn't get included in the list
		sagesGuild.setBuildingID ("BL03");
		sagesGuild.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (sagesGuild);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 22, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing almostCapped = (CityGrowthRateBreakdownGrowing) breakdown;
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
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the population is growing and almost at max size, but we can still grow by the calculated amount
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Growing_Capped () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

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
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC03");
		cityData.setCityOwnerID (pd.getPlayerID ());
		cityData.setCityPopulation (21980);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID ("BL01");
		granary.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (granary);

		final MemoryBuilding farmersMarket = new MemoryBuilding ();
		farmersMarket.setBuildingID ("BL02");
		farmersMarket.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (farmersMarket);

		final MemoryBuilding sagesGuild = new MemoryBuilding ();		// Irrelevant building, to prove it doesn't get included in the list
		sagesGuild.setBuildingID ("BL03");
		sagesGuild.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (sagesGuild);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 22, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownGrowing.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownGrowing almostCapped = (CityGrowthRateBreakdownGrowing) breakdown;
		assertEquals (21980, almostCapped.getCurrentPopulation ());
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
		assertEquals (20, almostCapped.getCappedTotal ());
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the city is over max size and the population is dying 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Dying () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Owner
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (21980);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 18, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownDying.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownDying dying = (CityGrowthRateBreakdownDying) breakdown;
		assertEquals (21980, dying.getCurrentPopulation ());
		assertEquals (18000, dying.getMaximumPopulation ());
		assertEquals (3, dying.getBaseDeathRate ());
		assertEquals (150, dying.getCityDeathRate ());
		assertEquals (-150, dying.getInitialTotal ());
		assertEquals (-150, dying.getCappedTotal ());
	}
	
	/**
	 * Tests the calculateCityGrowthRate method when the city is over max size and the population is dying, but can't go under 1000
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityGrowthRate_Dying_Capped () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Owner
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (1020);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		final CityGrowthRateBreakdown breakdown = calc.calculateCityGrowthRate (players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), 0, difficultyLevel, db);
		
		// Check results
		assertEquals (CityGrowthRateBreakdownDying.class.getName (), breakdown.getClass ().getName ());
		final CityGrowthRateBreakdownDying underCap = (CityGrowthRateBreakdownDying) breakdown;
		assertEquals (1020, underCap.getCurrentPopulation ());
		assertEquals (1000, underCap.getMaximumPopulation ());
		assertEquals (1, underCap.getBaseDeathRate ());
		assertEquals (50, underCap.getCityDeathRate ());
		assertEquals (-50, underCap.getInitialTotal ());
		assertEquals (-20, underCap.getCappedTotal ());
	}

	/**
	 * Tests the calculateCityRebels method, when tax rate is set to none, and so no rebels are generated
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_NoTaxes () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Tax rates
		final TaxRate taxRate1 = new TaxRate ();
		when (db.findTaxRate ("TR01", "calculateCityRebels")).thenReturn (taxRate1);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

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
		
		// Run method
		final CityUnrestBreakdown zeroPercent = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR01", db);
		
		// Check results
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
	}

	/**
	 * Tests the calculateCityRebels method, when tax rate is set to a high value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_HighTaxes () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

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
		
		// Run method
		final CityUnrestBreakdown highPercent = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);
		
		// Check results
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
		assertEquals (7, highPercent.getFinalTotal ());		// 7.65, prove that it rounds down
	}
	
	/**
	 * Tests the calculateCityRebels method, when tax rate is set so high that we have too many rebels for the minimum number of farmers the city requires
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_ConvertToFarmers () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Tax rates
		final TaxRate taxRate3 = new TaxRate ();
		taxRate3.setTaxUnrestPercentage (75);
		when (db.findTaxRate ("TR03", "calculateCityRebels")).thenReturn (taxRate3);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

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

		// Retorts, to prove this value does not get included in the output, as we have no religious buildings
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);

		// Run method
		final CityUnrestBreakdown maxPercent = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR03", db);
		
		// Check results
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
		assertEquals (11, maxPercent.getFinalTotal ());				// 12.75, but we have 6 minimum farmers so would be 18 population in a size 17 city
	}

	/**
	 * Tests the calculateCityRebels method, when a religious building improves unrest, but we have no retort that gives a bonus to religious buildings
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_ReligiousBuilding_NoBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
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
		
		// Run method
		final CityUnrestBreakdown shrine = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);
		
		// Check results
		assertEquals (17, shrine.getPopulation ());
		assertEquals (45, shrine.getTaxPercentage ());
		assertEquals (0, shrine.getRacialPercentage ());
		assertEquals (0, shrine.getRacialLiteral ());
		assertEquals (45, shrine.getTotalPercentage ());
		assertEquals (7, shrine.getBaseValue ());		// 45% tax rate = 7.65
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
	}
	
	/**
	 * Tests the calculateCityRebels method, when we have both a religious and non-religious building improving unrest,
	 * and a retort that only gives a bonus to the religious building
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_ReligiousBuilding_WithBonus () throws Exception
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
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL02");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

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

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown animistsGuild = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);

		// Check results
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
	}

	/**
	 * Tests the calculateCityRebels method, when we have two religious buildings, so now the +50% bonus from the retort is enough to make a difference
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_TwoReligiousBuildings_WithBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL03");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

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

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown temple = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);

		// Check results
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
	}
	
	/**
	 * Tests the calculateCityRebels method, when there is one unit guarding the city which is not enough to reduce unrest
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_OneUnit () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);

		final UnitEx normalUnitDef = new UnitEx ();
		normalUnitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "calculateCityRebels")).thenReturn (normalUnitDef);

		final Pick normalMagicRealm = new Pick ();
		normalMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("LTN", "calculateCityRebels")).thenReturn (normalMagicRealm);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL03");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID ("UN001");
		normalUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown firstUnit = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);

		// Check results
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
	}
	
	/**
	 * Tests the calculateCityRebels method, when there are two units guarding the city which reduce unrest even though one is a normal unit and the other a hero
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_TwoUnits () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);

		final UnitEx normalUnitDef = new UnitEx ();
		normalUnitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "calculateCityRebels")).thenReturn (normalUnitDef);

		final UnitEx heroUnitDef = new UnitEx ();
		heroUnitDef.setUnitMagicRealm ("LTH");
		when (db.findUnit ("UN003", "calculateCityRebels")).thenReturn (heroUnitDef);
		
		final Pick normalMagicRealm = new Pick ();
		normalMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("LTN", "calculateCityRebels")).thenReturn (normalMagicRealm);

		final Pick heroMagicRealm = new Pick ();
		heroMagicRealm.setUnitTypeID ("H");
		when (db.findPick ("LTH", "calculateCityRebels")).thenReturn (heroMagicRealm);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL03");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID ("UN001");
		normalUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final MemoryUnit heroUnit = new MemoryUnit ();
		heroUnit.setUnitID ("UN003");
		heroUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown secondUnit = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);

		// Check results
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
	}
	
	/**
	 * Tests the calculateCityRebels method, when there is a normal unit and a hero who help reduce unrest, plus some summoned units and dead units who don't
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_SummonedAndDeadUnits () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);

		final UnitEx normalUnitDef = new UnitEx ();
		normalUnitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "calculateCityRebels")).thenReturn (normalUnitDef);

		final UnitEx summonedUnitDef = new UnitEx ();
		summonedUnitDef.setUnitMagicRealm ("MB01");
		when (db.findUnit ("UN002", "calculateCityRebels")).thenReturn (summonedUnitDef);
		
		final UnitEx heroUnitDef = new UnitEx ();
		heroUnitDef.setUnitMagicRealm ("LTH");
		when (db.findUnit ("UN003", "calculateCityRebels")).thenReturn (heroUnitDef);
		
		final Pick normalMagicRealm = new Pick ();
		normalMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("LTN", "calculateCityRebels")).thenReturn (normalMagicRealm);
		
		final Pick summonedMagicRealm = new Pick ();
		summonedMagicRealm.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		when (db.findPick ("MB01", "calculateCityRebels")).thenReturn (summonedMagicRealm);

		final Pick heroMagicRealm = new Pick ();
		heroMagicRealm.setUnitTypeID ("H");
		when (db.findPick ("LTH", "calculateCityRebels")).thenReturn (heroMagicRealm);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL03");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID ("UN001");
		normalUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final MemoryUnit heroUnit = new MemoryUnit ();
		heroUnit.setUnitID ("UN003");
		heroUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);

		// summoned units or dead units don't help (unitCount still = 2)
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit deadUnit = new MemoryUnit ();
			deadUnit.setUnitID ("UN001");
			deadUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
			deadUnit.setStatus (UnitStatusID.DEAD);
			units.add (deadUnit);

			final MemoryUnit summonedUnit = new MemoryUnit ();
			summonedUnit.setUnitID ("UN002");
			summonedUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
			summonedUnit.setStatus (UnitStatusID.ALIVE);
			units.add (summonedUnit);
		}
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown extraUnits = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);

		// Check results
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
	}
	
	/**
	 * Tests the calculateCityRebels method, when we're at a Klackon capital so they get the special 2 unrest reduction
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_Klackons () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);

		final UnitEx normalUnitDef = new UnitEx ();
		normalUnitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "calculateCityRebels")).thenReturn (normalUnitDef);

		final UnitEx heroUnitDef = new UnitEx ();
		heroUnitDef.setUnitMagicRealm ("LTH");
		when (db.findUnit ("UN003", "calculateCityRebels")).thenReturn (heroUnitDef);
		
		final Pick normalMagicRealm = new Pick ();
		normalMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("LTN", "calculateCityRebels")).thenReturn (normalMagicRealm);

		final Pick heroMagicRealm = new Pick ();
		heroMagicRealm.setUnitTypeID ("H");
		when (db.findPick ("LTH", "calculateCityRebels")).thenReturn (heroMagicRealm);

		final RaceUnrest klackonUnrest = new RaceUnrest ();
		klackonUnrest.setCapitalRaceID ("RC01");
		klackonUnrest.setUnrestLiteral (-2);
		
		final RaceEx klackonsDef = new RaceEx ();
		klackonsDef.getRaceUnrest ().add (klackonUnrest);
		when (db.findRace ("RC01", "calculateCityRebels")).thenReturn (klackonsDef);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL03");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID ("UN001");
		normalUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final MemoryUnit heroUnit = new MemoryUnit ();
		heroUnit.setUnitID ("UN003");
		heroUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Capital race
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryBuilding fortressBuilding = new MemoryBuilding ();
		fortressBuilding.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		when (memoryBuildingUtils.findCityWithBuilding (1, CommonDatabaseConstants.BUILDING_FORTRESS, map, buildings)).thenReturn (fortressBuilding);
		
		// Set up object to test
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown klackons = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);

		// Check results
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
	}
	
	/**
	 * Tests the calculateCityRebels method, when the city is at our fortress, but anyone other than Klackons don't get any kind of unrest bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_NoCapitalRaceBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);

		final UnitEx normalUnitDef = new UnitEx ();
		normalUnitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "calculateCityRebels")).thenReturn (normalUnitDef);

		final UnitEx heroUnitDef = new UnitEx ();
		heroUnitDef.setUnitMagicRealm ("LTH");
		when (db.findUnit ("UN003", "calculateCityRebels")).thenReturn (heroUnitDef);
		
		final Pick normalMagicRealm = new Pick ();
		normalMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("LTN", "calculateCityRebels")).thenReturn (normalMagicRealm);

		final Pick heroMagicRealm = new Pick ();
		heroMagicRealm.setUnitTypeID ("H");
		when (db.findPick ("LTH", "calculateCityRebels")).thenReturn (heroMagicRealm);
		
		final RaceEx highElvesDef = new RaceEx ();
		when (db.findRace ("RC02", "calculateCityRebels")).thenReturn (highElvesDef);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC02");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL03");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID ("UN001");
		normalUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final MemoryUnit heroUnit = new MemoryUnit ();
		heroUnit.setUnitID ("UN003");
		heroUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Capital race
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryBuilding fortressBuilding = new MemoryBuilding ();
		fortressBuilding.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		when (memoryBuildingUtils.findCityWithBuilding (1, CommonDatabaseConstants.BUILDING_FORTRESS, map, buildings)).thenReturn (fortressBuilding);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown highElves = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);

		// Check results
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
	}

	/**
	 * Tests the calculateCityRebels method, when we calculate a negative number of rebels (no taxes, but some unrest bonuses) so gets forced up to 0
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_ForcePositive () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);

		final UnitEx normalUnitDef = new UnitEx ();
		normalUnitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "calculateCityRebels")).thenReturn (normalUnitDef);

		final UnitEx heroUnitDef = new UnitEx ();
		heroUnitDef.setUnitMagicRealm ("LTH");
		when (db.findUnit ("UN003", "calculateCityRebels")).thenReturn (heroUnitDef);
		
		final Pick normalMagicRealm = new Pick ();
		normalMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("LTN", "calculateCityRebels")).thenReturn (normalMagicRealm);

		final Pick heroMagicRealm = new Pick ();
		heroMagicRealm.setUnitTypeID ("H");
		when (db.findPick ("LTH", "calculateCityRebels")).thenReturn (heroMagicRealm);
		
		// Tax rates
		final TaxRate taxRate1 = new TaxRate ();
		when (db.findTaxRate ("TR01", "calculateCityRebels")).thenReturn (taxRate1);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL03");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID ("UN001");
		normalUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final MemoryUnit heroUnit = new MemoryUnit ();
		heroUnit.setUnitID ("UN003");
		heroUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown forcePositive = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR01", db);

		// Check results
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
	}

	/**
	 * Tests the calculateCityRebels method, when the race inhabiting the city and the capital race don't like each other and generate unrest
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_RacialUnrest () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Building shrineDef = new Building ();
		shrineDef.setBuildingUnrestReduction (1);
		shrineDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL01", "calculateCityRebels")).thenReturn (shrineDef);

		final Building templeDef = new Building ();
		templeDef.setBuildingUnrestReduction (1);
		templeDef.setBuildingUnrestReductionImprovedByRetorts (true);
		when (db.findBuilding ("BL03", "calculateCityRebels")).thenReturn (templeDef);

		final UnitEx normalUnitDef = new UnitEx ();
		normalUnitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "calculateCityRebels")).thenReturn (normalUnitDef);

		final UnitEx heroUnitDef = new UnitEx ();
		heroUnitDef.setUnitMagicRealm ("LTH");
		when (db.findUnit ("UN003", "calculateCityRebels")).thenReturn (heroUnitDef);
		
		final Pick normalMagicRealm = new Pick ();
		normalMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("LTN", "calculateCityRebels")).thenReturn (normalMagicRealm);

		final Pick heroMagicRealm = new Pick ();
		heroMagicRealm.setUnitTypeID ("H");
		when (db.findPick ("LTH", "calculateCityRebels")).thenReturn (heroMagicRealm);
		
		final RaceUnrest highElfDwarfUnrest = new RaceUnrest ();
		highElfDwarfUnrest.setCapitalRaceID ("RC03");
		highElfDwarfUnrest.setUnrestPercentage (30);
		
		final RaceEx highElvesDef = new RaceEx ();
		highElvesDef.getRaceUnrest ().add (highElfDwarfUnrest);
		when (db.findRace ("RC02", "calculateCityRebels")).thenReturn (highElvesDef);
		
		// Tax rates
		final TaxRate taxRate2 = new TaxRate ();
		taxRate2.setTaxUnrestPercentage (45);
		when (db.findTaxRate ("TR02", "calculateCityRebels")).thenReturn (taxRate2);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Cities
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC02");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (17900);
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final OverlandMapCityData capitalCityData = new OverlandMapCityData ();
		capitalCityData.setCityRaceID ("RC03");
		capitalCityData.setCityOwnerID (1);
		capitalCityData.setCityPopulation (1000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (30).setCityData (capitalCityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding shrineBuilding = new MemoryBuilding ();
		shrineBuilding.setBuildingID ("BL01");
		shrineBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (shrineBuilding);
		
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL03");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (secondBuilding);

		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		final MemoryUnit normalUnit = new MemoryUnit ();
		normalUnit.setUnitID ("UN001");
		normalUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		normalUnit.setStatus (UnitStatusID.ALIVE);
		units.add (normalUnit);

		final MemoryUnit heroUnit = new MemoryUnit ();
		heroUnit.setUnitID ("UN003");
		heroUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		heroUnit.setStatus (UnitStatusID.ALIVE);
		units.add (heroUnit);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Divine power doesn't work on non-religious building
		final List<String> religiousRetortsList = new ArrayList<String> ();
		religiousRetortsList.add ("RT01");
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (50);
		when (playerPickUtils.pickIdsContributingToReligiousBuildingBonus (ppk.getPick (), db)).thenReturn (religiousRetortsList);
		
		// Capital race
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryBuilding fortressBuilding = new MemoryBuilding ();
		fortressBuilding.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (new MapCoordinates3DEx (30, 10, 1));

		when (memoryBuildingUtils.findCityWithBuilding (1, CommonDatabaseConstants.BUILDING_FORTRESS, map, buildings)).thenReturn (fortressBuilding);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown racialUnrest = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", db);

		// Check results
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
	}
	
	/**
	 * Tests the calculateCityRebels method, when we calculate more rebels than the whole town population, so have to cap it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityRebels_ForceAll () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final RaceUnrest highElfDwarfUnrest = new RaceUnrest ();
		highElfDwarfUnrest.setCapitalRaceID ("RC03");
		highElfDwarfUnrest.setUnrestPercentage (30);
		
		final RaceEx highElvesDef = new RaceEx ();
		highElvesDef.getRaceUnrest ().add (highElfDwarfUnrest);
		when (db.findRace ("RC02", "calculateCityRebels")).thenReturn (highElvesDef);
		
		// Tax rates
		final TaxRate taxRate3 = new TaxRate ();
		taxRate3.setTaxUnrestPercentage (75);
		when (db.findTaxRate ("TR03", "calculateCityRebels")).thenReturn (taxRate3);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Cities
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC02");
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (24890);		// Has to be over 20 for 105% to round down to >1 person
		cityData.setMinimumFarmers (6);	// 6x2 = 12 food, +2 granary +3 farmers market = 17
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final OverlandMapCityData capitalCityData = new OverlandMapCityData ();
		capitalCityData.setCityRaceID ("RC03");
		capitalCityData.setCityOwnerID (1);
		capitalCityData.setCityPopulation (1000);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (30).setCityData (capitalCityData);
		
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

		// Divine power doesn't work on non-religious building
		// Capital race
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryBuilding fortressBuilding = new MemoryBuilding ();
		fortressBuilding.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		fortressBuilding.setCityLocation (new MapCoordinates3DEx (30, 10, 1));

		when (memoryBuildingUtils.findCityWithBuilding (1, CommonDatabaseConstants.BUILDING_FORTRESS, map, buildings)).thenReturn (fortressBuilding);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCityRebels")).thenReturn (player);

		// Set up object to test
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class); 
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final CityUnrestBreakdown forceAll = calc.calculateCityRebels
			(players, map, units, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR03", db);

		// Check results
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
	 * Tests the addGoldFromTaxes method, when we are generating some gold
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddGoldFromTaxes_Some () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TaxRate taxRate = new TaxRate ();
		taxRate.setDoubleTaxGold (3);
		when (db.findTaxRate ("TR02", "addGoldFromTaxes")).thenReturn (taxRate);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (8723);
		cityData.setNumberOfRebels (2);

		// Additions
		final CityProductionUtils cityProductionUtils = mock (CityProductionUtils.class);
		when (cityProductionUtils.addProductionAmountToBreakdown (any (CityProductionBreakdown.class), anyInt (),
			isNull (), eq (db))).thenReturn (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCityProductionUtils (cityProductionUtils);
		
		// Run method
		final CityProductionBreakdown gold = calc.addGoldFromTaxes (cityData, "TR02", db);
		
		// Check results
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, gold.getProductionTypeID ());
		assertEquals (6, gold.getApplicablePopulation ());
		assertEquals (3, gold.getDoubleProductionAmountEachPopulation ());
		assertEquals (18, gold.getDoubleProductionAmountAllPopulation ());
		
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (gold, 18, null, db);
	}
	
	/**
	 * Tests the addGoldFromTaxes method, when we have a tax rate chosen that generates no gold
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddGoldFromTaxes_NoTaxRate () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TaxRate taxRate = new TaxRate ();
		when (db.findTaxRate ("TR01", "addGoldFromTaxes")).thenReturn (taxRate);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (8723);
		cityData.setNumberOfRebels (2);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Run method
		assertNull (calc.addGoldFromTaxes (cityData, "TR01", db));
	}
	
	/**
	 * Tests the addGoldFromTaxes method, when there aren't enough people to generate any taxes
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddGoldFromTaxes_NoPeople () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final TaxRate taxRate = new TaxRate ();
		taxRate.setDoubleTaxGold (3);
		when (db.findTaxRate ("TR02", "addGoldFromTaxes")).thenReturn (taxRate);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (923);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Run method
		assertNull (calc.addGoldFromTaxes (cityData, "TR02", db));
	}
	
	/**
	 * Tests the addRationsEatenByPopulation method, when there are some civilians eating rations
	 */
	@Test
	public final void testAddRationsEatenByPopulation_Some ()
	{
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (8723);
		cityData.setNumberOfRebels (2);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Run method
		final CityProductionBreakdown rations = calc.addRationsEatenByPopulation (cityData);
		
		// Check results
		assertEquals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, rations.getProductionTypeID ());
		assertEquals (8, rations.getApplicablePopulation ());
		assertEquals (1, rations.getConsumptionAmountEachPopulation ());
		assertEquals (8, rations.getConsumptionAmountAllPopulation ());
		assertEquals (8, rations.getConsumptionAmount ());
	}
	
	/**
	 * Tests the addRationsEatenByPopulation method, when there aren't enough people to eat any rations
	 */
	@Test
	public final void testAddRationsEatenByPopulation_NoPeople ()
	{
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (923);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Run method
		assertNull (calc.addRationsEatenByPopulation (cityData));
	}
	
	/**
	 * Tests the addProductionFromPopulation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddProductionFromPopulation () throws Exception
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
		
		// Additions
		final CityProductionUtils cityProductionUtils = mock (CityProductionUtils.class);
		when (cityProductionUtils.addProductionAmountToBreakdown (any (CityProductionBreakdown.class), anyInt (),
			eq (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES), eq (db))).thenReturn (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (buildingUtils);
		calc.setCityProductionUtils (cityProductionUtils);
		
		when (buildingUtils.totalBonusProductionPerPersonFromBuildings (buildings, new MapCoordinates3DEx (20, 10, 1), "B", "RE01", db)).thenReturn (0);
		when (buildingUtils.totalBonusProductionPerPersonFromBuildings (buildings, new MapCoordinates3DEx (20, 10, 1), "B", "RE02", db)).thenReturn (2);
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromPopulation (productionValues, race, "B", 5, new MapCoordinates3DEx (20, 10, 1), buildings, db);
		
		// Check results
		assertEquals (2, productionValues.getProductionType ().size ());
		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());

		assertEquals (1, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getProductionAmountBucketID ());
		assertEquals ("B", productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getPopulationTaskID ());
		assertEquals (5, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getCount ());
		assertEquals (2, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getDoubleProductionAmountEachPopulation ());
		assertEquals (10, productionValues.getProductionType ().get (0).getPopulationTaskProduction ().get (0).getDoubleProductionAmountAllPopulation ());
		
		assertEquals (1, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getProductionAmountBucketID ());
		assertEquals ("B", productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getPopulationTaskID ());
		assertEquals (5, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getCount ());
		assertEquals (5, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getDoubleProductionAmountEachPopulation ());
		assertEquals (25, productionValues.getProductionType ().get (1).getPopulationTaskProduction ().get (0).getDoubleProductionAmountAllPopulation ());
		
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (0), 10,
			ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, db);
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (1), 25,
			ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, db);
	}
	
	/**
	 * Tests the addProductionFromFortressPickType method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddProductionFromFortressPickType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

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

		// Additions
		final CityProductionUtils cityProductionUtils = mock (CityProductionUtils.class);
		when (cityProductionUtils.addProductionAmountToBreakdown (any (CityProductionBreakdown.class), anyInt (),
			isNull (), eq (db))).thenReturn (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCityProductionUtils (cityProductionUtils);
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromFortressPickType (productionValues, pickType, 5, db);
		
		// Check results
		assertEquals (2, productionValues.getProductionType ().size ());
		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());

		assertEquals (1, productionValues.getProductionType ().get (0).getPickTypeProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getProductionAmountBucketID ());
		assertEquals ("B", productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getPickTypeID ());
		assertEquals (5, productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getCount ());
		assertEquals (2, productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getDoubleProductionAmountEachPick ());
		assertEquals (10, productionValues.getProductionType ().get (0).getPickTypeProduction ().get (0).getDoubleProductionAmountAllPicks ());
		
		assertEquals (1, productionValues.getProductionType ().get (1).getPickTypeProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getProductionAmountBucketID ());
		assertEquals ("B", productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getPickTypeID ());
		assertEquals (5, productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getCount ());
		assertEquals (3, productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getDoubleProductionAmountEachPick ());
		assertEquals (15, productionValues.getProductionType ().get (1).getPickTypeProduction ().get (0).getDoubleProductionAmountAllPicks ());
		
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (0), 10, null, db);
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (1), 15, null, db);
	}
	
	/**
	 * Tests the addProductionFromFortressPlane method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddProductionFromFortressPlane () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

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

		// Additions
		final CityProductionUtils cityProductionUtils = mock (CityProductionUtils.class);
		when (cityProductionUtils.addProductionAmountToBreakdown (any (CityProductionBreakdown.class), anyInt (),
			isNull (), eq (db))).thenReturn (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCityProductionUtils (cityProductionUtils);
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromFortressPlane (productionValues, plane, db);

		// Check results
		assertEquals (2, productionValues.getProductionType ().size ());
		
		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (1, productionValues.getProductionType ().get (0).getPlaneProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (0).getPlaneProduction ().get (0).getProductionAmountBucketID ());
		assertEquals (1, productionValues.getProductionType ().get (0).getPlaneProduction ().get (0).getFortressPlane ());
		assertEquals (2, productionValues.getProductionType ().get (0).getPlaneProduction ().get (0).getDoubleProductionAmountFortressPlane ());
		
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (1, productionValues.getProductionType ().get (1).getPlaneProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (1).getPlaneProduction ().get (0).getProductionAmountBucketID ());
		assertEquals (1, productionValues.getProductionType ().get (1).getPlaneProduction ().get (0).getFortressPlane ());
		assertEquals (3, productionValues.getProductionType ().get (1).getPlaneProduction ().get (0).getDoubleProductionAmountFortressPlane ());
		
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (0), 2, null, db);
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (1), 3, null, db);
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

		// Additions
		final CityProductionUtils cityProductionUtils = mock (CityProductionUtils.class);
		when (cityProductionUtils.addProductionAmountToBreakdown (any (CityProductionBreakdown.class), anyInt (),
			isNull (), eq (db))).thenReturn (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setPlayerPickUtils (pickUtils);
		calc.setCityProductionUtils (cityProductionUtils);
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionAndConsumptionFromBuilding (productionValues, building, null, picks, db);
		
		// Check results
		assertEquals (3, productionValues.getProductionType ().size ());
		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (2, productionValues.getProductionType ().get (1).getConsumptionAmount ());
		assertEquals ("RE03", productionValues.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (25, productionValues.getProductionType ().get (2).getPercentageBonus ());
		
		assertEquals (1, productionValues.getProductionType ().get (0).getBuildingBreakdown ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (0).getBuildingBreakdown ().get (0).getProductionAmountBucketID ());
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
		
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (0), 3, null, db);
	}
	
	/**
	 * Tests the addProductionFromMapFeatures method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddProductionFromMapFeatures () throws Exception
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
		
		// Additions
		final CityProductionUtils cityProductionUtils = mock (CityProductionUtils.class);
		when (cityProductionUtils.addProductionAmountToBreakdown (any (CityProductionBreakdown.class), anyInt (),
			isNull (), eq (db))).thenReturn (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		calc.setCityProductionUtils (cityProductionUtils);
		
		// Run method
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();
		calc.addProductionFromMapFeatures (productionValues, map, new MapCoordinates3DEx (2, 2, 0), sys, db, 2, 50);
		
		// Check results
		assertEquals (3, productionValues.getProductionType ().size ());

		assertEquals ("RE01", productionValues.getProductionType ().get (0).getProductionTypeID ());
		assertEquals (1, productionValues.getProductionType ().get (0).getMapFeatureProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getProductionAmountBucketID ());
		assertEquals ("MF01", productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (2, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (3, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (6, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (1, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (6, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (0, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (6, productionValues.getProductionType ().get (0).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());
		
		assertEquals ("RE02", productionValues.getProductionType ().get (1).getProductionTypeID ());
		assertEquals (1, productionValues.getProductionType ().get (1).getMapFeatureProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getProductionAmountBucketID ());
		assertEquals ("MF01", productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (2, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (4, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (8, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (1, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (8, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (0, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (8, productionValues.getProductionType ().get (1).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());

		assertEquals ("RE03", productionValues.getProductionType ().get (2).getProductionTypeID ());
		assertEquals (1, productionValues.getProductionType ().get (2).getMapFeatureProduction ().size ());
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getProductionAmountBucketID ());
		assertEquals ("MF02", productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getMapFeatureID ());
		assertEquals (3, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getCount ());
		assertEquals (4, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountEachFeature ());
		assertEquals (12, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleUnmodifiedProductionAmountAllFeatures ());
		assertEquals (2, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getRaceMineralBonusMultiplier ());
		assertEquals (24, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleProductionAmountAfterRacialMultiplier ());
		assertEquals (50, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getBuildingMineralPercentageBonus ());	
		assertEquals (36, productionValues.getProductionType ().get (2).getMapFeatureProduction ().get (0).getDoubleModifiedProductionAmountAllFeatures ());
		
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (0), 6, null, db);
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (1), 8, null, db);
		verify (cityProductionUtils, times (1)).addProductionAmountToBreakdown (productionValues.getProductionType ().get (2), 36, null, db);
	}
	
	/**
	 * Tests the createUnrestReductionFromSpell method that we get the benefit of our positive overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnrestReductionFromSpell_OurPositiveOverlandEnchantment () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell spellDef = new Spell ();
		spellDef.setSpellUnrestReduction (1);
		when (db.findSpell ("SP001", "createUnrestReductionFromSpell")).thenReturn (spellDef);
		
		// Spell being added
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setSpellID ("SP001");
		spell.setCastingPlayerID (1);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		final CityUnrestBreakdownSpell breakdown = calc.createUnrestReductionFromSpell (spell, 1, db);
		
		// Check results
		assertEquals ("SP001", breakdown.getSpellID ()); 
		assertEquals (1, breakdown.getUnrestReduction ().intValue ());
	}
	
	/**
	 * Tests the createUnrestReductionFromSpell method that we don't get the benefit of someone else's positive overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnrestReductionFromSpell_EnemyPositiveOverlandEnchantment () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell spellDef = new Spell ();
		spellDef.setSpellUnrestReduction (1);
		when (db.findSpell ("SP001", "createUnrestReductionFromSpell")).thenReturn (spellDef);
		
		// Spell being added
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setSpellID ("SP001");
		spell.setCastingPlayerID (1);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		assertNull (calc.createUnrestReductionFromSpell (spell, 2, db));
	}
	
	/**
	 * Tests the createUnrestReductionFromSpell method that we get the penalty of someone else's negative overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnrestReductionFromSpell_EnemyNegativeOverlandEnchantment () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell spellDef = new Spell ();
		spellDef.setSpellUnrestReduction (-1);
		when (db.findSpell ("SP001", "createUnrestReductionFromSpell")).thenReturn (spellDef);
		
		// Spell being added
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setSpellID ("SP001");
		spell.setCastingPlayerID (1);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		final CityUnrestBreakdownSpell breakdown = calc.createUnrestReductionFromSpell (spell, 2, db);
		
		// Check results
		assertEquals ("SP001", breakdown.getSpellID ()); 
		assertEquals (-1, breakdown.getUnrestReduction ().intValue ());
	}
	
	/**
	 * Tests the createUnrestReductionFromSpell method that we don't get the penalty of our own negative overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnrestReductionFromSpell_OurNegativeOverlandEnchantment () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell spellDef = new Spell ();
		spellDef.setSpellUnrestReduction (-1);
		when (db.findSpell ("SP001", "createUnrestReductionFromSpell")).thenReturn (spellDef);
		
		// Spell being added
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setSpellID ("SP001");
		spell.setCastingPlayerID (1);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		assertNull (calc.createUnrestReductionFromSpell (spell, 1, db));
	}
	
	/**
	 * Tests the createUnrestReductionFromSpell method on a normal spell whose unrest reduction is defined in the XML
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnrestReductionFromSpell_CAE () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Spell spellDef = new Spell ();
		when (db.findSpell ("SP001", "createUnrestReductionFromSpell")).thenReturn (spellDef);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.setCitySpellEffectUnrestReduction (2);
		effect.setCitySpellEffectUnrestPercentage (10);
		
		when (db.findCitySpellEffect ("SE001", "createUnrestReductionFromSpell")).thenReturn (effect);
		
		// Spell being added
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setSpellID ("SP001");
		spell.setCitySpellEffectID ("SE001");

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		final CityUnrestBreakdownSpell breakdown = calc.createUnrestReductionFromSpell (spell, 0, db);
		
		// Check results
		assertEquals ("SP001", breakdown.getSpellID ()); 
		assertEquals (2, breakdown.getUnrestReduction ().intValue ());
		assertEquals (10, breakdown.getUnrestPercentage ().intValue ());
	}
	
	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when the "before" amount is an exact multiple of 2, and there's no %s or after values involved
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_ExactMultiple () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (6);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, null, db);
		
		// Check results
		assertEquals (3, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (3, breakdown.getProductionAmountPlusPercentageBonus ());
		assertEquals (3, breakdown.getProductionAmountMinusPercentagePenalty ());
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (3, breakdown.getProductionAmountBaseTotal ());
		assertEquals (100, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (3, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (3, breakdown.getCappedProductionAmount ());
	}

	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when the "before" amount isn't an exact multiple of 2 but should be
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_NotExactMultiple () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setRoundingDirectionID (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);
		when (db.findProductionType ("RE02", "halveAddPercentageBonusAndCapProduction")).thenReturn (productionType);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (7);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		assertThrows (MomException.class, () ->
		{
			calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, null, db);
		});
	}

	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when the "before" amount isn't an exact multiple of 2, and gets rounded down
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_RoundDown () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		when (db.findProductionType ("RE02", "halveAddPercentageBonusAndCapProduction")).thenReturn (productionType);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (7);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, null, db);
		
		// Check results
		assertEquals (3, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (3, breakdown.getProductionAmountPlusPercentageBonus ());
		assertEquals (3, breakdown.getProductionAmountMinusPercentagePenalty ());
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (3, breakdown.getProductionAmountBaseTotal ());
		assertEquals (100, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (3, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (3, breakdown.getCappedProductionAmount ());
	}
	
	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when the "before" amount isn't an exact multiple of 2, and gets rounded up
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_RoundUp () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		when (db.findProductionType ("RE02", "halveAddPercentageBonusAndCapProduction")).thenReturn (productionType);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (7);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, null, db);
		
		// Check results
		assertEquals (4, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (4, breakdown.getProductionAmountPlusPercentageBonus ());
		assertEquals (4, breakdown.getProductionAmountMinusPercentagePenalty ());
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (4, breakdown.getProductionAmountBaseTotal ());
		assertEquals (100, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (4, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (4, breakdown.getCappedProductionAmount ());
	}
	
	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when a percentage bonus applies to the "before" value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_PercentageBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (30);
		breakdown.setPercentageBonus (50);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, null, db);
		
		// Check results
		assertEquals (15, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (22, breakdown.getProductionAmountPlusPercentageBonus ());	// Rounded down
		assertEquals (22, breakdown.getProductionAmountMinusPercentagePenalty ());
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (22, breakdown.getProductionAmountBaseTotal ());
		assertEquals (100, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (22, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (22, breakdown.getCappedProductionAmount ());
	}

	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when a percentage bonus applies, followed by a percentage penalty
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_PercentagePenalty () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (30);
		breakdown.setPercentageBonus (50);
		breakdown.setPercentagePenalty (25);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, null, db);
		
		// Check results
		assertEquals (15, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (22, breakdown.getProductionAmountPlusPercentageBonus ());	// 22.5 Rounded down
		assertEquals (17, breakdown.getProductionAmountMinusPercentagePenalty ());	// 16.5 Rounded up (the amount reduced by is rounded down)
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (17, breakdown.getProductionAmountBaseTotal ());
		assertEquals (100, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (17, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (17, breakdown.getCappedProductionAmount ());
	}

	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when a percentage bonus applies, followed by a percentage penalty
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_OverfarmingRule () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		breakdown.setDoubleProductionAmountBeforePercentages (30);
		breakdown.setPercentageBonus (50);
		breakdown.setPercentagePenalty (25);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, null, db);
		
		// Check results
		assertEquals (15, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (22, breakdown.getProductionAmountPlusPercentageBonus ());	// 22.5 Rounded down
		assertEquals (17, breakdown.getProductionAmountMinusPercentagePenalty ());	// 16.5 Rounded up (the amount reduced by is rounded down)
		assertEquals (12, breakdown.getFoodProductionFromTerrainTiles ().intValue ());
		assertEquals (14, breakdown.getProductionAmountAfterOverfarmingPenalty ().intValue ());	// Trying to farm 17 but can only farm 12 without penalty, remaining 5 is halved so 14
		assertEquals (14, breakdown.getProductionAmountBaseTotal ());
		assertEquals (100, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (14, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (14, breakdown.getCappedProductionAmount ());
	}

	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when a percentage bonus applies, followed by a percentage penalty, followed by an "after" value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_After () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, null, null);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (30);
		breakdown.setPercentageBonus (50);
		breakdown.setPercentagePenalty (25);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, null, db);
		
		// Check results
		assertEquals (15, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (22, breakdown.getProductionAmountPlusPercentageBonus ());	// 22.5 Rounded down
		assertEquals (17, breakdown.getProductionAmountMinusPercentagePenalty ());	// 16.5 Rounded up (the amount reduced by is rounded down)
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (20, breakdown.getProductionAmountBaseTotal ());
		assertEquals (100, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (20, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (20, breakdown.getCappedProductionAmount ());
	}

	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method applying the special multiplier for AI wizards
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_AIMultiplier_Wizard_Applies () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		productionType.setDifficultyLevelMultiplierApplies (true);
		when (db.findProductionType ("RE02", "halveAddPercentageBonusAndCapProduction")).thenReturn (productionType);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (false);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID ("WZ01");
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, pub, null);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsProductionRateMultiplier (300);
		difficultyLevel.setAiRaidersProductionRateMultiplier (200);
		difficultyLevel.setCityMaxSize (25);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (30);
		breakdown.setPercentageBonus (50);
		breakdown.setPercentagePenalty (25);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, difficultyLevel, db);
		
		// Check results
		assertEquals (15, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (22, breakdown.getProductionAmountPlusPercentageBonus ());	// 22.5 Rounded down
		assertEquals (17, breakdown.getProductionAmountMinusPercentagePenalty ());	// 16.5 Rounded up (the amount reduced by is rounded down)
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (20, breakdown.getProductionAmountBaseTotal ());
		assertEquals (300, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (60, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (60, breakdown.getCappedProductionAmount ());
	}
	
	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method applying the special multiplier for AI raiders
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_AIMultiplier_Raiders_Applies () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		productionType.setDifficultyLevelMultiplierApplies (true);
		when (db.findProductionType ("RE02", "halveAddPercentageBonusAndCapProduction")).thenReturn (productionType);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (false);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID (CommonDatabaseConstants.WIZARD_ID_RAIDERS);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, pub, null);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsProductionRateMultiplier (300);
		difficultyLevel.setAiRaidersProductionRateMultiplier (200);
		difficultyLevel.setCityMaxSize (25);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (30);
		breakdown.setPercentageBonus (50);
		breakdown.setPercentagePenalty (25);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, difficultyLevel, db);
		
		// Check results
		assertEquals (15, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (22, breakdown.getProductionAmountPlusPercentageBonus ());	// 22.5 Rounded down
		assertEquals (17, breakdown.getProductionAmountMinusPercentagePenalty ());	// 16.5 Rounded up (the amount reduced by is rounded down)
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (20, breakdown.getProductionAmountBaseTotal ());
		assertEquals (200, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (40, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (40, breakdown.getCappedProductionAmount ());
	}
	
	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when it is an AI player, but a production type where the special multiplier doesn't apply
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_AIMultiplier_Wizard_NotApplies () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		productionType.setDifficultyLevelMultiplierApplies (false);
		when (db.findProductionType ("RE02", "halveAddPercentageBonusAndCapProduction")).thenReturn (productionType);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (false);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID ("WZ01");
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, pub, null);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsProductionRateMultiplier (300);
		difficultyLevel.setAiRaidersProductionRateMultiplier (200);
		difficultyLevel.setCityMaxSize (25);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE02");
		breakdown.setDoubleProductionAmountBeforePercentages (30);
		breakdown.setPercentageBonus (50);
		breakdown.setPercentagePenalty (25);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, difficultyLevel, db);
		
		// Check results
		assertEquals (15, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (22, breakdown.getProductionAmountPlusPercentageBonus ());	// 22.5 Rounded down
		assertEquals (17, breakdown.getProductionAmountMinusPercentagePenalty ());	// 16.5 Rounded up (the amount reduced by is rounded down)
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (20, breakdown.getProductionAmountBaseTotal ());
		assertEquals (100, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (20, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (20, breakdown.getCappedProductionAmount ());
	}
	
	/**
	 * Tests the halveAddPercentageBonusAndCapProduction method when a city would be over size 25 so the cap kicks in
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHalveAddPercentageBonusAndCapProduction_MaxCitySizeCap () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		productionType.setDifficultyLevelMultiplierApplies (true);
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, "halveAddPercentageBonusAndCapProduction")).thenReturn (productionType);
		
		// City owner
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (false);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID (CommonDatabaseConstants.WIZARD_ID_RAIDERS);
		
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (pd, pub, null);
		
		// Difficulty level
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsProductionRateMultiplier (300);
		difficultyLevel.setAiRaidersProductionRateMultiplier (200);
		difficultyLevel.setCityMaxSize (25);
		
		// Calculation values
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		breakdown.setDoubleProductionAmountBeforePercentages (30);
		breakdown.setPercentageBonus (50);
		breakdown.setPercentagePenalty (25);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		
		// Call method
		calc.halveAddPercentageBonusAndCapProduction (cityOwner, breakdown, 12, difficultyLevel, db);
		
		// Check results
		assertEquals (15, breakdown.getProductionAmountBeforePercentages ());
		assertEquals (22, breakdown.getProductionAmountPlusPercentageBonus ());	// 22.5 Rounded down
		assertEquals (17, breakdown.getProductionAmountMinusPercentagePenalty ());	// 16.5 Rounded up (the amount reduced by is rounded down)
		assertNull (breakdown.getFoodProductionFromTerrainTiles ());
		assertNull (breakdown.getProductionAmountAfterOverfarmingPenalty ());
		assertEquals (20, breakdown.getProductionAmountBaseTotal ());
		assertEquals (200, breakdown.getDifficultyLevelMultiplier ());
		assertEquals (40, breakdown.getTotalAdjustedForDifficultyLevel ());
		assertEquals (25, breakdown.getCappedProductionAmount ());
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
	
	/**
	 * Tests the listUnitsCityCanConstruct method
	 */
	@Test
	public final void testListUnitsCityCanConstruct ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<UnitEx> unitDefs = new ArrayList<UnitEx> ();
		
		// 1 is a Hero
		// 2 is a raceless unit we can construct
		// 3 is incorrect race
		// 4 is correct race, but don't have required buildings
		// 5 is correct race, and have required buildings
		for (int n = 1; n <= 5; n++)
		{
			final UnitEx unitDef = new UnitEx ();
			unitDef.setUnitID ("UN00" + n);
			unitDef.setUnitMagicRealm ((n == 1) ? CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO : CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
			
			if (n == 3)
				unitDef.setUnitRaceID ("RC02");
			else if (n >= 4)
				unitDef.setUnitRaceID ("RC01");
			
			unitDefs.add (unitDef);
		}
		
		when (db.getUnits ()).thenReturn (unitDefs);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Which units we have the required buildings for
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.meetsUnitRequirements (buildings, new MapCoordinates3DEx (20, 10, 1), unitDefs.get (1))).thenReturn (true);
		when (memoryBuildingUtils.meetsUnitRequirements (buildings, new MapCoordinates3DEx (20, 10, 1), unitDefs.get (3))).thenReturn (false);
		when (memoryBuildingUtils.meetsUnitRequirements (buildings, new MapCoordinates3DEx (20, 10, 1), unitDefs.get (4))).thenReturn (true);

		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// Run method
		final List<UnitEx> list = calc.listUnitsCityCanConstruct (new MapCoordinates3DEx (20, 10, 1), map, buildings, db);
		
		// Check results
		assertEquals (2, list.size ());
		assertEquals ("UN002", list.get (0).getUnitID ());
		assertEquals ("UN005", list.get (1).getUnitID ());
	}
	
	/**
	 * Tests the buildRenderCityData method
	 */
	@Test
	public final void testBuildRenderCityData ()
	{
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityName ("Blah");
		cityData.setCityRaceID ("RC01");
		cityData.setCitySizeID ("CS01");
		cityData.setCityOwnerID (3);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Terrain tiles
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
			terrain.setTileTypeID ("TT0" + n);
			map.getPlane ().get (1).getRow ().get (10).getCell ().get (19 + n).setTerrainData (terrain);
		}
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		for (int n = 1; n <= 3; n++)
		{
			final MemoryBuilding building = new MemoryBuilding ();
			building.setBuildingID ("BL0" + n);
			building.setCityLocation (new MapCoordinates3DEx ((n < 3) ? 20 : 21, 10, 1));
			mem.getBuilding ().add (building);
		}

		for (int n = 1; n <= 4; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			if (n > 1)
				spell.setCitySpellEffectID ("SE00" + n);
			
			spell.setCityLocation (new MapCoordinates3DEx ((n < 4) ? 20 : 21, 10, 1));
			mem.getMaintainedSpell ().add (spell);
		}
		
		// Set up object to test
		final CityCalculationsImpl calc = new CityCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final RenderCityData data = calc.buildRenderCityData (new MapCoordinates3DEx (20, 10, 1), sys, mem);
		
		// Check results
		assertEquals (3, data.getCityOwnerID ());
		assertEquals ("CS01", data.getCitySizeID ());
		assertEquals ("Blah", data.getCityName ());
		assertEquals (1, data.getPlaneNumber ());
		assertEquals (0, data.getRubbleBuildingID ().size ());
		
		assertEquals (2, data.getBuildingID ().size ());
		assertEquals ("BL01", data.getBuildingID ().get (0));
		assertEquals ("BL02", data.getBuildingID ().get (1));

		assertEquals (2, data.getCitySpellEffectID ().size ());
		assertEquals ("SE002", data.getCitySpellEffectID ().get (0));
		assertEquals ("SE003", data.getCitySpellEffectID ().get (1));

		assertEquals (2, data.getAdjacentTileTypeID ().size ());
		assertEquals ("TT01", data.getAdjacentTileTypeID ().get (0));
		assertEquals ("TT02", data.getAdjacentTileTypeID ().get (1));
	}
}