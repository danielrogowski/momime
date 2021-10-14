package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.random.RandomUtils;

import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.MapFeatureEx;
import momime.common.database.OverlandMapSize;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.utils.CityServerUtils;

/**
 * Tests the AICityCalculationsImpl class
 */
public final class TestAICityCalculationsImpl extends ServerTestData
{
	/**
	 * Test the evaluateCityQuality method in the simplest case
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEvaluateCityQuality_Simple () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);

		// Map
		final MapVolumeOfMemoryGridCells knownMap = createOverlandMap (mapSize);
		
		// City production
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setCappedProductionAmount (15);
		
		final CityProductionBreakdown production = new CityProductionBreakdown ();
		production.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		production.setPercentageBonus (40);
		
		final CityProductionBreakdown gold = new CityProductionBreakdown ();
		gold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		gold.setTradePercentageBonusCapped (10);
		
		final CityProductionBreakdownsEx productions = new CityProductionBreakdownsEx ();
		productions.getProductionType ().add (food);
		productions.getProductionType ().add (production);
		productions.getProductionType ().add (gold);
		
		final CityProductionCalculations prodCalc = mock (CityProductionCalculations.class);
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null, sd, false, true, db)).thenReturn (productions);
		
		// Set up object to test
		final AICityCalculationsImpl calc = new AICityCalculationsImpl ();
		calc.setCityProductionCalculations (prodCalc);
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (150 + 40 + 10, calc.evaluateCityQuality (new MapCoordinates3DEx (20, 10, 1), false, true, knownMap, sd, db).intValue ());
	}

	/**
	 * Test the evaluateCityQuality method, adding in additional quality from nearby map features
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEvaluateCityQuality_MapFeatures () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 3; n++)
		{
			final MapFeatureEx mapFeature = new MapFeatureEx ();
			mapFeature.setCityQualityEstimate (n * 7);
			when (db.findMapFeature ("MF0" + n, "chooseCityLocation")).thenReturn (mapFeature);
		}
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);

		// Map
		final MapVolumeOfMemoryGridCells knownMap = createOverlandMap (mapSize);
		
		for (int y = 1; y <= 2; y++)
			for (int x = 1; x <= 3; x++)
			{
				final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
				terrain.setMapFeatureID ("MF0" + x);
				knownMap.getPlane ().get (1).getRow ().get (7 + y).getCell ().get (19 + x).setTerrainData (terrain);		// So one of the MF03's is off the corner of the city area
			}
		
		// City production
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setCappedProductionAmount (15);
		
		final CityProductionBreakdown production = new CityProductionBreakdown ();
		production.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		production.setPercentageBonus (40);
		
		final CityProductionBreakdown gold = new CityProductionBreakdown ();
		gold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		gold.setTradePercentageBonusCapped (10);
		
		final CityProductionBreakdownsEx productions = new CityProductionBreakdownsEx ();
		productions.getProductionType ().add (food);
		productions.getProductionType ().add (production);
		productions.getProductionType ().add (gold);
		
		final CityProductionCalculations prodCalc = mock (CityProductionCalculations.class);
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null, sd, false, true, db)).thenReturn (productions);
		
		// Set up object to test
		final AICityCalculationsImpl calc = new AICityCalculationsImpl ();
		calc.setCityProductionCalculations (prodCalc);
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (150 + 40 + 10 + (9 * 7), calc.evaluateCityQuality (new MapCoordinates3DEx (20, 10, 1), false, false, knownMap, sd, db).intValue ());
	}

	/**
	 * Test the evaluateCityQuality method on a tiny city when it isn't ignored
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEvaluateCityQuality_TinyCity () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);

		// Map
		final MapVolumeOfMemoryGridCells knownMap = createOverlandMap (mapSize);
		
		// City production
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setCappedProductionAmount (3);
		
		final CityProductionBreakdown production = new CityProductionBreakdown ();
		production.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		production.setPercentageBonus (40);
		
		final CityProductionBreakdown gold = new CityProductionBreakdown ();
		gold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		gold.setTradePercentageBonusCapped (10);
		
		final CityProductionBreakdownsEx productions = new CityProductionBreakdownsEx ();
		productions.getProductionType ().add (food);
		productions.getProductionType ().add (production);
		productions.getProductionType ().add (gold);
		
		final CityProductionCalculations prodCalc = mock (CityProductionCalculations.class);
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null, sd, false, true, db)).thenReturn (productions);
		
		// Set up object to test
		final AICityCalculationsImpl calc = new AICityCalculationsImpl ();
		calc.setCityProductionCalculations (prodCalc);
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (30 + 40 + 10, calc.evaluateCityQuality (new MapCoordinates3DEx (20, 10, 1), false, false, knownMap, sd, db).intValue ());
	}

	/**
	 * Test the evaluateCityQuality method on a tiny city when it is ignored
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEvaluateCityQuality_IgnoredCity () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);

		// Map
		final MapVolumeOfMemoryGridCells knownMap = createOverlandMap (mapSize);
		
		// City production
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setCappedProductionAmount (3);
		
		final CityProductionBreakdown production = new CityProductionBreakdown ();
		production.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		production.setPercentageBonus (40);
		
		final CityProductionBreakdown gold = new CityProductionBreakdown ();
		gold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		gold.setTradePercentageBonusCapped (10);
		
		final CityProductionBreakdownsEx productions = new CityProductionBreakdownsEx ();
		productions.getProductionType ().add (food);
		productions.getProductionType ().add (production);
		productions.getProductionType ().add (gold);
		
		final CityProductionCalculations prodCalc = mock (CityProductionCalculations.class);
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null, sd, false, true, db)).thenReturn (productions);
		
		// Set up object to test
		final AICityCalculationsImpl calc = new AICityCalculationsImpl ();
		calc.setCityProductionCalculations (prodCalc);
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertNull (calc.evaluateCityQuality (new MapCoordinates3DEx (20, 10, 1), false, true, knownMap, sd, db));
	}

	/**
	 * Test the evaluateCityQuality method when we're trying to avoid placing near other cities
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEvaluateCityQuality_Avoid () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);

		// Map
		final MapVolumeOfMemoryGridCells knownMap = createOverlandMap (mapSize);
		
		// City production
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setCappedProductionAmount (15);
		
		final CityProductionBreakdown production = new CityProductionBreakdown ();
		production.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		production.setPercentageBonus (40);
		
		final CityProductionBreakdown gold = new CityProductionBreakdown ();
		gold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		gold.setTradePercentageBonusCapped (10);
		
		final CityProductionBreakdownsEx productions = new CityProductionBreakdownsEx ();
		productions.getProductionType ().add (food);
		productions.getProductionType ().add (production);
		productions.getProductionType ().add (gold);
		
		final CityProductionCalculations prodCalc = mock (CityProductionCalculations.class);
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null, sd, false, true, db)).thenReturn (productions);
		
		// Avoid nearby city
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.findClosestCityTo (new MapCoordinates3DEx (20, 10, 1), knownMap, mapSize)).thenReturn (14);
		
		// Set up object to test
		final AICityCalculationsImpl calc = new AICityCalculationsImpl ();
		calc.setCityProductionCalculations (prodCalc);
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		calc.setCityServerUtils (cityServerUtils);
		
		// Run method
		assertEquals (150 + 40 + 10 + 28, calc.evaluateCityQuality (new MapCoordinates3DEx (20, 10, 1), true, true, knownMap, sd, db).intValue ());
	}
	
	/**
	 * Tests the findWorkersToConvertToFarmers method when we only need to find and convert one farmer
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindWorkersToConvertToFarmers_OnlyNeedOne () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);

		// Map
		final MapVolumeOfMemoryGridCells map = createOverlandMap (mapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (map);
		
		// Amount of rations each farmer generates
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.calculateDoubleFarmingRate (eq (trueMap.getMap ()), eq (trueMap.getBuilding ()), eq (trueMap.getMaintainedSpell ()),
			any (MapCoordinates3DEx.class), eq (db))).thenReturn (4);
		
		// Cities
		for (int x = 1; x <= 5; x++)
		{
			final OverlandMapCityData cityData = new OverlandMapCityData ();
			cityData.setCityOwnerID ((x == 5) ? 4 : 3);
			cityData.setCurrentlyConstructingBuildingID ((x == 1) ? "BL15" : CommonDatabaseConstants.BUILDING_TRADE_GOODS);
			cityData.setCityPopulation (x * 4000);
			cityData.setMinimumFarmers (x);
			cityData.setNumberOfRebels (x);		// So cities have 2, 4, 6, 8 and 10 available workers, but cities 1+5 are excluded because of owner+current construction
			map.getPlane ().get (1).getRow ().get (10).getCell ().get (20 + x).setCityData (cityData);
		}
		
		// Random selection
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (4 + 6 + 8)).thenReturn (7);
		
		// Set up object to test
		final AICityCalculationsImpl calc = new AICityCalculationsImpl ();
		calc.setRandomUtils (random);
		calc.setCityServerUtils (cityServerUtils);
		
		// Run method
		assertEquals (0, calc.findWorkersToConvertToFarmers (4, true, trueMap, 3, db, sd));
		
		// Check results
		assertEquals (0, map.getPlane ().get (1).getRow ().get (10).getCell ().get (22).getCityData ().getOptionalFarmers ());
		assertEquals (1, map.getPlane ().get (1).getRow ().get (10).getCell ().get (23).getCityData ().getOptionalFarmers ());
		assertEquals (0, map.getPlane ().get (1).getRow ().get (10).getCell ().get (24).getCityData ().getOptionalFarmers ());
	}

	/**
	 * Tests the findWorkersToConvertToFarmers method when we need to pick 3 farmers before we exit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindWorkersToConvertToFarmers_Multiple () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);

		// Map
		final MapVolumeOfMemoryGridCells map = createOverlandMap (mapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (map);
		
		// Amount of rations each farmer generates
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.calculateDoubleFarmingRate (eq (trueMap.getMap ()), eq (trueMap.getBuilding ()), eq (trueMap.getMaintainedSpell ()),
			any (MapCoordinates3DEx.class), eq (db))).thenReturn (4);
		
		// Cities
		for (int x = 1; x <= 5; x++)
		{
			final OverlandMapCityData cityData = new OverlandMapCityData ();
			cityData.setCityOwnerID ((x == 5) ? 4 : 3);
			cityData.setCurrentlyConstructingBuildingID ((x == 1) ? "BL15" : CommonDatabaseConstants.BUILDING_TRADE_GOODS);
			cityData.setCityPopulation (x * 4000);
			cityData.setMinimumFarmers (x);
			cityData.setNumberOfRebels (x);		// So cities have 2, 4, 6, 8 and 10 available workers, but cities 1+5 are excluded because of owner+current construction
			map.getPlane ().get (1).getRow ().get (10).getCell ().get (20 + x).setCityData (cityData);
		}
		
		// Random selection
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (18)).thenReturn (7);
		when (random.nextInt (17)).thenReturn (2);
		when (random.nextInt (16)).thenReturn (7);
		
		// Set up object to test
		final AICityCalculationsImpl calc = new AICityCalculationsImpl ();
		calc.setRandomUtils (random);
		calc.setCityServerUtils (cityServerUtils);
		
		// Run method
		assertEquals (-3, calc.findWorkersToConvertToFarmers (9, true, trueMap, 3, db, sd));
		
		// Check results
		assertEquals (1, map.getPlane ().get (1).getRow ().get (10).getCell ().get (22).getCityData ().getOptionalFarmers ());
		assertEquals (2, map.getPlane ().get (1).getRow ().get (10).getCell ().get (23).getCityData ().getOptionalFarmers ());
		assertEquals (0, map.getPlane ().get (1).getRow ().get (10).getCell ().get (24).getCityData ().getOptionalFarmers ());
	}
}