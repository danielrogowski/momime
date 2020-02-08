package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests the ProductionTypeGfx class
 */
public final class TestProductionTypeGfx
{
	/**
	 * Tests the findProductionValueImageFile method
	 */
	@Test
	public final void testFindProductionValueImageFile ()
	{
		// Create some dummy entries
		final ProductionTypeGfx productionType = new ProductionTypeGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeImageGfx image = new ProductionTypeImageGfx ();
			image.setProductionValue (Integer.valueOf (n).toString ());
			image.setProductionImageFile ("Blah" + n + ".png");
			
			productionType.getProductionTypeImage ().add (image);
		}
		
		productionType.buildMap ();
		
		// Run tests
		assertEquals ("Blah2.png", productionType.findProductionValueImageFile ("2"));
		assertNull (productionType.findProductionValueImageFile ("4"));
	}
}