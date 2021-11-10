package momime.common.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import momime.common.internal.CityProductionBreakdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the CityProductionBreakdownsEx class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCityProductionBreakdownsEx
{
	/**
	 * Tests the findProductionType method
	 */
	@Test
	public final void testFindProductionType ()
	{
		// Set up object to test
		final CityProductionBreakdownsEx breakdowns = new CityProductionBreakdownsEx ();
		for (int n = 1; n <= 3; n++)
		{
			final CityProductionBreakdown productionType = new CityProductionBreakdown ();
			productionType.setProductionTypeID (Integer.valueOf (n).toString ());
			breakdowns.getProductionType ().add (productionType);
		}
		
		// Run method and check results
		assertEquals ("2", breakdowns.findProductionType ("2").getProductionTypeID ());
		assertNull (breakdowns.findProductionType ("4"));
	}
	
	/**
	 * Tests the findOrAddProductionType method
	 */
	@Test
	public final void testFindOrAddProductionType ()
	{
		// Set up object to test
		final CityProductionBreakdownsEx breakdowns = new CityProductionBreakdownsEx ();
		for (int n = 1; n <= 3; n++)
		{
			final CityProductionBreakdown productionType = new CityProductionBreakdown ();
			productionType.setProductionTypeID (Integer.valueOf (n).toString ());
			breakdowns.getProductionType ().add (productionType);
		}
		
		// Run method and check results
		assertEquals ("2", breakdowns.findOrAddProductionType ("2").getProductionTypeID ());
		assertEquals ("4", breakdowns.findOrAddProductionType ("4").getProductionTypeID ());
	}
}