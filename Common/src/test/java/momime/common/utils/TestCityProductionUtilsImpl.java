package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.ProductionAmountBucketID;
import momime.common.database.ProductionTypeEx;
import momime.common.internal.CityProductionBreakdown;

/**
 * Tests the CityProductionUtilsImpl class
 */
public final class TestCityProductionUtilsImpl
{
	/**
	 * Tests the addProductionAmountToBreakdown method, adding to the "before" bucket
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddProductionAmountToBreakdown_Before () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionAmountBucketID (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES);
		when (db.findProductionType ("RE01", "addProductionAmountToBreakdown")).thenReturn (productionType);
		
		// Set up object to test
		final CityProductionUtilsImpl utils = new CityProductionUtilsImpl ();
		
		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE01");
		breakdown.setDoubleProductionAmountBeforePercentages (4);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, utils.addProductionAmountToBreakdown (breakdown, 5, null, db));
		
		// Check results
		assertEquals (9, breakdown.getDoubleProductionAmountBeforePercentages ());
		assertEquals (3, breakdown.getProductionAmountToAddAfterPercentages ());
	}

	/**
	 * Tests the addProductionAmountToBreakdown method, adding to the "after" bucket
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddProductionAmountToBreakdown_After () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionAmountBucketID (ProductionAmountBucketID.AFTER_PERCENTAGE_BONUSES);
		when (db.findProductionType ("RE01", "addProductionAmountToBreakdown")).thenReturn (productionType);
		
		// Set up object to test
		final CityProductionUtilsImpl utils = new CityProductionUtilsImpl ();
		
		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE01");
		breakdown.setDoubleProductionAmountBeforePercentages (4);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		assertEquals (ProductionAmountBucketID.AFTER_PERCENTAGE_BONUSES, utils.addProductionAmountToBreakdown (breakdown, 10, null, db));
		
		// Check results
		assertEquals (4, breakdown.getDoubleProductionAmountBeforePercentages ());
		assertEquals (8, breakdown.getProductionAmountToAddAfterPercentages ());
	}

	/**
	 * Tests the addProductionAmountToBreakdown method, trying to add an uneven doubled value to the "after" bucket
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testAddProductionAmountToBreakdown_NotEven () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionAmountBucketID (ProductionAmountBucketID.AFTER_PERCENTAGE_BONUSES);
		when (db.findProductionType ("RE01", "addProductionAmountToBreakdown")).thenReturn (productionType);
		
		// Set up object to test
		final CityProductionUtilsImpl utils = new CityProductionUtilsImpl ();
		
		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE01");
		breakdown.setDoubleProductionAmountBeforePercentages (4);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		utils.addProductionAmountToBreakdown (breakdown, 9, null, db);
	}

	/**
	 * Tests the addProductionAmountToBreakdown method, when the production type says to add to the "after" bucket but we override it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddProductionAmountToBreakdown_Override () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		productionType.setProductionAmountBucketID (ProductionAmountBucketID.AFTER_PERCENTAGE_BONUSES);
		when (db.findProductionType ("RE01", "addProductionAmountToBreakdown")).thenReturn (productionType);
		
		// Set up object to test
		final CityProductionUtilsImpl utils = new CityProductionUtilsImpl ();
		
		// Call method
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID ("RE01");
		breakdown.setDoubleProductionAmountBeforePercentages (4);
		breakdown.setProductionAmountToAddAfterPercentages (3);
		
		assertEquals (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, utils.addProductionAmountToBreakdown
			(breakdown, 10, ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, db));
		
		// Check results
		assertEquals (14, breakdown.getDoubleProductionAmountBeforePercentages ());
		assertEquals (3, breakdown.getProductionAmountToAddAfterPercentages ());
	}
}