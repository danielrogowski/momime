package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the ProductionTypeEx class
 */
@ExtendWith(MockitoExtension.class)
public final class TestProductionTypeEx
{
	/**
	 * Tests the findProductionValueImageFile method
	 */
	@Test
	public final void testFindProductionValueImageFile ()
	{
		// Create some dummy entries
		final ProductionTypeEx productionType = new ProductionTypeEx ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeImage image = new ProductionTypeImage ();
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