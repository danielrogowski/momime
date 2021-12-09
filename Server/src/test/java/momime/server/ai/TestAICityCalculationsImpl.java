package momime.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
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
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.utils.CityServerUtils;

/**
 * Tests the AICityCalculationsImpl class
 */
@ExtendWith(MockitoExtension.class)
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
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null,
			sd, null, false, true, db)).thenReturn (productions);
		
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
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null,
			sd, null, false, true, db)).thenReturn (productions);
		
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
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null,
			sd, null, false, true, db)).thenReturn (productions);
		
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
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null,
			sd, null, false, true, db)).thenReturn (productions);
		
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
		when (prodCalc.calculateAllCityProductions (null, knownMap, null, null, new MapCoordinates3DEx (20, 10, 1), null,
			sd, null, false, true, db)).thenReturn (productions);
		
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
	 * Tests the findWorkersToConvertToFarmers method
	 */
	@Test
	public final void testFindWorkersToConvertToFarmers ()
	{
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = createOverlandMap (sys);
		
		// Cities
		final List<AICityRationDetails> cities = new ArrayList<AICityRationDetails> ();
		for (int x = 1; x <= 5; x++)
		{
			final OverlandMapCityData cityData = new OverlandMapCityData ();
			cityData.setCurrentlyConstructingBuildingID ((x == 1) ? "BL15" : CommonDatabaseConstants.BUILDING_TRADE_GOODS);
			cityData.setCityPopulation (x * 4000);
			cityData.setMinimumFarmers (x);
			cityData.setNumberOfRebels (x);		// So cities have 2, 4, 6 and 8 available workers, but 1st city is excluded because its building something useful
			map.getPlane ().get (1).getRow ().get (10).getCell ().get (20 + x).setCityData (cityData);
			
			final AICityRationDetails cityDetails = new AICityRationDetails ();
			cityDetails.setCityLocation (new MapCoordinates3DEx (20 + x, 10, 1));
			cityDetails.setOverfarming (x == 5);		// City with 10 available workers excluded because its overfarming already
			cities.add (cityDetails);
		}
		
		// Random selection
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (4 + 6 + 8)).thenReturn (7);
		
		// Set up object to test
		final AICityCalculationsImpl calc = new AICityCalculationsImpl ();
		calc.setRandomUtils (random);
		
		// Run method
		final AICityRationDetails chosenCity = calc.findWorkersToConvertToFarmers (cities, true, false, map);
		
		// Check results
		assertEquals (23, chosenCity.getCityLocation ().getX ());
	}
}