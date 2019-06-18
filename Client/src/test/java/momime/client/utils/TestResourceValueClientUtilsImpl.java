package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;

import momime.client.ClientTestData;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeGfx;
import momime.client.graphics.database.ProductionTypeImageGfx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndUndoubledValue;

/**
 * Tests the ResourceValueClientUtilsImpl class
 */
public final class TestResourceValueClientUtilsImpl extends ClientTestData
{
	/** Colour for a transparent pixel */
	private static final int TRANSPARENT = 0;
	
	/** Colour for a '1' pixel */
	private static final int PLUS_ONE = 0xFFFF0000;

	/** Colour for a '-1' pixel */
	private static final int MINUS_ONE = 0xFF800000;
	
	/** Colour for a '10' pixel */
	private static final int PLUS_TEN = 0xFF00FF00;

	/** Colour for a '-10' pixel */
	private static final int MINUS_TEN = 0xFF008000;

	/** Colour for a half pixel */
	private static final int PLUS_HALF = 0xFF0000FF;
	
	/** Colour for a '1' mana pixel */
	private static final int PLUS_ONE_MANA = 0xFFEE0000;

	/** Colour for a '-1' mana pixel */
	private static final int MINUS_ONE_MANA = 0xFF700000;
	
	/** Colour for a '10' mana pixel */
	private static final int PLUS_TEN_MANA = 0xFF00EE00;

	/** Colour for a '-10' mana pixel */
	private static final int MINUS_TEN_MANA = 0xFF007000;

	/** Colour for a half mana pixel */
	private static final int PLUS_HALF_MANA = 0xFF0000EE;
	
	/**
	 * Tests the generateProductionImage method when we don't supply images for +10 or -10
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Test
	public final void testGenerateProductionImage_WithoutTens () throws IOException
	{
		// Create different colour sample images
		final BufferedImage plusOneImage = createSolidImage (1, 1, PLUS_ONE);
		final BufferedImage minusOneImage = createSolidImage (1, 1, MINUS_ONE);

		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("I+1")).thenReturn (plusOneImage);
		when (utils.loadImage ("I-1")).thenReturn (minusOneImage);
		
		// Mock entries from the graphics XML
		final ProductionTypeImageGfx plusOneImageContainer = new ProductionTypeImageGfx ();
		plusOneImageContainer.setProductionImageFile ("I+1");
		plusOneImageContainer.setProductionValue ("1");
		
		final ProductionTypeImageGfx minusOneImageContainer = new ProductionTypeImageGfx ();
		minusOneImageContainer.setProductionImageFile ("I-1");
		minusOneImageContainer.setProductionValue ("-1");

		final ProductionTypeGfx productionTypeImages = new ProductionTypeGfx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (minusOneImageContainer);
		productionTypeImages.buildMap ();

		final ProductionTypeGfx noProductionTypeImages = new ProductionTypeGfx ();
		noProductionTypeImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findProductionType ("RE01", "generateProductionImage")).thenReturn (productionTypeImages);
		when (gfx.findProductionType ("RE02", "generateProductionImage")).thenReturn (noProductionTypeImages);
		
		// Set up object to test
		final ResourceValueClientUtilsImpl obj = new ResourceValueClientUtilsImpl ();
		obj.setGraphicsDB (gfx);
		obj.setUtils (utils);
		
		// Test null conditions
		assertNull (obj.generateProductionImage ("RE02", 10, 10));
		assertNull (obj.generateProductionImage ("RE01", 0, 0));
		
		// If gold Production is 12 and Consumption is 0, it will display 12 gold coins
		final BufferedImage image1 = obj.generateProductionImage ("RE01", 12, 0);
		checkImage (image1, "X X X X X X X X X X X X");
		
		// If gold Production is 12 and Consumption is 0, it will display 12 grey coins
		final BufferedImage image2 = obj.generateProductionImage ("RE01", 0, 12);
		checkImage (image2, "G G G G G G G G G G G G");
		
		 // If gold Production is 25 and Consumption is 12, it will display 12 gold coins, a gap, then 13 more gold coins
		final BufferedImage image3 = obj.generateProductionImage ("RE01", 25, 12);
		checkImage (image3, "X X X X X X X X X X X X      X X X X X X X X X X X X X");
		
		// If gold Production is 12 and Consumption is 25, it will display 12 gold coins, a gap, then 13 grey coins
		final BufferedImage image4 = obj.generateProductionImage ("RE01", 12, 25);
		checkImage (image4, "X X X X X X X X X X X X      G G G G G G G G G G G G G");
	}
	
	/**
	 * Tests the generateProductionImage method when we do supply images for +10 or -10
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Test
	public final void testGenerateProductionImage_WithTens () throws IOException
	{
		// Create different colour sample images
		final BufferedImage plusOneImage = createSolidImage (1, 1, PLUS_ONE);
		final BufferedImage minusOneImage = createSolidImage (1, 1, MINUS_ONE);
		final BufferedImage plusTenImage = createSolidImage (1, 1, PLUS_TEN);
		final BufferedImage minusTenImage = createSolidImage (1, 1, MINUS_TEN);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("I+1")).thenReturn (plusOneImage);
		when (utils.loadImage ("I-1")).thenReturn (minusOneImage);
		when (utils.loadImage ("I+10")).thenReturn (plusTenImage);
		when (utils.loadImage ("I-10")).thenReturn (minusTenImage);
		
		// Mock entries from the graphics XML
		final ProductionTypeImageGfx plusOneImageContainer = new ProductionTypeImageGfx ();
		plusOneImageContainer.setProductionImageFile ("I+1");
		plusOneImageContainer.setProductionValue ("1");
		
		final ProductionTypeImageGfx minusOneImageContainer = new ProductionTypeImageGfx ();
		minusOneImageContainer.setProductionImageFile ("I-1");
		minusOneImageContainer.setProductionValue ("-1");

		final ProductionTypeImageGfx plusTenImageContainer = new ProductionTypeImageGfx ();
		plusTenImageContainer.setProductionImageFile ("I+10");
		plusTenImageContainer.setProductionValue ("10");
		
		final ProductionTypeImageGfx minusTenImageContainer = new ProductionTypeImageGfx ();
		minusTenImageContainer.setProductionImageFile ("I-10");
		minusTenImageContainer.setProductionValue ("-10");

		final ProductionTypeGfx productionTypeImages = new ProductionTypeGfx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (minusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (plusTenImageContainer);
		productionTypeImages.getProductionTypeImage ().add (minusTenImageContainer);
		productionTypeImages.buildMap ();

		final ProductionTypeGfx noProductionTypeImages = new ProductionTypeGfx ();
		noProductionTypeImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findProductionType ("RE01", "generateProductionImage")).thenReturn (productionTypeImages);
		when (gfx.findProductionType ("RE02", "generateProductionImage")).thenReturn (noProductionTypeImages);
		
		// Set up object to test
		final ResourceValueClientUtilsImpl obj = new ResourceValueClientUtilsImpl ();
		obj.setGraphicsDB (gfx);
		obj.setUtils (utils);

		// Test null conditions
		assertNull (obj.generateProductionImage ("RE02", 10, 10));
		assertNull (obj.generateProductionImage ("RE01", 0, 0));
		
		// If gold Production is 12 and Consumption is 0, it will display 12 gold coins
		final BufferedImage image1 = obj.generateProductionImage ("RE01", 12, 0);
		checkImage (image1, "T X X");
		
		// If gold Production is 12 and Consumption is 0, it will display 12 grey coins
		final BufferedImage image2 = obj.generateProductionImage ("RE01", 0, 12);
		checkImage (image2, "M G G");
		
		 // If gold Production is 25 and Consumption is 12, it will display 12 gold coins, a gap, then 13 more gold coins
		final BufferedImage image3 = obj.generateProductionImage ("RE01", 25, 12);
		checkImage (image3, "T X X      T X X X");
		
		// If gold Production is 12 and Consumption is 25, it will display 12 gold coins, a gap, then 13 grey coins
		final BufferedImage image4 = obj.generateProductionImage ("RE01", 12, 25);
		checkImage (image4, "T X X      M G G G");
	}
	
	/**
	 * Tests the generateUpkeepImage method when we don't supply images for +10 or -10
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Test
	public final void testGenerateUpkeepImage_WithoutTens () throws IOException
	{
		// Create different colour sample images
		final BufferedImage plusOneImage = createSolidImage (1, 1, PLUS_ONE);
		final BufferedImage plusHalfImage = createSolidImage (1, 1, PLUS_HALF);
		final BufferedImage plusOneMana = createSolidImage (1, 1, PLUS_ONE_MANA);
		final BufferedImage plusHalfMana = createSolidImage (1, 1, PLUS_HALF_MANA);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("I+1")).thenReturn (plusOneImage);
		when (utils.loadImage ("I+half")).thenReturn (plusHalfImage);
		when (utils.loadImage ("M+1")).thenReturn (plusOneMana);
		when (utils.loadImage ("M+half")).thenReturn (plusHalfMana);

		// Mock entries from the graphics XML
		final ProductionTypeImageGfx plusOneImageContainer = new ProductionTypeImageGfx ();
		plusOneImageContainer.setProductionImageFile ("I+1");
		plusOneImageContainer.setProductionValue ("1");

		final ProductionTypeImageGfx plusHalfImageContainer = new ProductionTypeImageGfx ();
		plusHalfImageContainer.setProductionImageFile ("I+half");
		plusHalfImageContainer.setProductionValue ("½");

		final ProductionTypeGfx productionTypeImages = new ProductionTypeGfx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (plusHalfImageContainer);
		productionTypeImages.buildMap ();
		
		final ProductionTypeImageGfx plusOneManaContainer = new ProductionTypeImageGfx ();
		plusOneManaContainer.setProductionImageFile ("M+1");
		plusOneManaContainer.setProductionValue ("1");

		final ProductionTypeImageGfx plusHalfManaContainer = new ProductionTypeImageGfx ();
		plusHalfManaContainer.setProductionImageFile ("M+half");
		plusHalfManaContainer.setProductionValue ("½");

		final ProductionTypeGfx manaImages = new ProductionTypeGfx ();
		manaImages.getProductionTypeImage ().add (plusOneManaContainer);
		manaImages.getProductionTypeImage ().add (plusHalfManaContainer);
		manaImages.buildMap ();
		
		final ProductionTypeGfx noProductionTypeImages = new ProductionTypeGfx ();
		noProductionTypeImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (productionTypeImages);
		when (gfx.findProductionType ("RE02", "generateUpkeepImage")).thenReturn (noProductionTypeImages);
		when (gfx.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "generateUpkeepImage")).thenReturn (manaImages);
		
		// Set up object to test
		final ResourceValueClientUtilsImpl obj = new ResourceValueClientUtilsImpl ();
		obj.setGraphicsDB (gfx);
		obj.setUtils (utils);

		// Test null conditions
		assertNull (obj.generateUpkeepImage (null, false)); 
		assertNull (obj.generateUpkeepImage (new ArrayList<ProductionTypeAndUndoubledValue> (), false));
		
		// List containing a production type for which we have images, but the value is 0
		final ProductionTypeAndUndoubledValue list1upkeep = new ProductionTypeAndUndoubledValue ();
		list1upkeep.setProductionTypeID ("RE01");
		list1upkeep.setUndoubledProductionValue (0);
		
		final List<ProductionTypeAndUndoubledValue> list1 = new ArrayList<ProductionTypeAndUndoubledValue> ();
		list1.add (list1upkeep);
		
		assertNull (obj.generateUpkeepImage (list1, false));

		// List containing a production type for which don't we have images, but the value is 0
		list1upkeep.setProductionTypeID ("RE02");
		assertNull (obj.generateUpkeepImage (list1, false));

		// List containing a production type for which don't we have images, and the value is non-zero
		list1upkeep.setUndoubledProductionValue (10);
		assertNull (obj.generateUpkeepImage (list1, false));
		
		// Single production
		final ProductionTypeAndUndoubledValue list4upkeep = new ProductionTypeAndUndoubledValue ();
		list4upkeep.setProductionTypeID ("RE01");
		list4upkeep.setUndoubledProductionValue (12);
		
		final List<ProductionTypeAndUndoubledValue> list4 = new ArrayList<ProductionTypeAndUndoubledValue> ();
		list4.add (list4upkeep);
		
		final BufferedImage image4 = obj.generateUpkeepImage (list4, false);
		checkImage (image4, "X X X X X X X X X X X X");
		
		// Two productions
		final ProductionTypeAndUndoubledValue list4mana = new ProductionTypeAndUndoubledValue ();
		list4mana.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		list4mana.setUndoubledProductionValue (14);
		
		list4.add (list4mana);
		
		final BufferedImage image5 = obj.generateUpkeepImage (list4, false);
		checkImage (image5, "X X X X X X X X X X X X x x x x x x x x x x x x x x");		
		
		// Two productions, and one is halved but an even number
		final BufferedImage image6 = obj.generateUpkeepImage (list4, true);
		checkImage (image6, "X X X X X X X X X X X X x x x x x x x");		

		// Two productions, and one is halved and needs to display the half icon
		list4mana.setUndoubledProductionValue (15);
		
		final BufferedImage image7 = obj.generateUpkeepImage (list4, true);
		checkImage (image7, "X X X X X X X X X X X X x x x x x x x h");		
	}
	
	/**
	 * Tests the generateUpkeepImage method when we do supply images for +10 or -10
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Test
	public final void testGenerateUpkeepImage_WithTens () throws IOException
	{
		// Create different colour sample images
		final BufferedImage plusOneImage = createSolidImage (1, 1, PLUS_ONE);
		final BufferedImage plusTenImage = createSolidImage (1, 1, PLUS_TEN);
		final BufferedImage plusHalfImage = createSolidImage (1, 1, PLUS_HALF);
		final BufferedImage plusOneMana = createSolidImage (1, 1, PLUS_ONE_MANA);
		final BufferedImage plusTenMana = createSolidImage (1, 1, PLUS_TEN_MANA);
		final BufferedImage plusHalfMana = createSolidImage (1, 1, PLUS_HALF_MANA);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("I+1")).thenReturn (plusOneImage);
		when (utils.loadImage ("I+10")).thenReturn (plusTenImage);
		when (utils.loadImage ("I+half")).thenReturn (plusHalfImage);
		when (utils.loadImage ("M+1")).thenReturn (plusOneMana);
		when (utils.loadImage ("M+10")).thenReturn (plusTenMana);
		when (utils.loadImage ("M+half")).thenReturn (plusHalfMana);

		// Mock entries from the graphics XML
		final ProductionTypeImageGfx plusOneImageContainer = new ProductionTypeImageGfx ();
		plusOneImageContainer.setProductionImageFile ("I+1");
		plusOneImageContainer.setProductionValue ("1");

		final ProductionTypeImageGfx plusTenImageContainer = new ProductionTypeImageGfx ();
		plusTenImageContainer.setProductionImageFile ("I+10");
		plusTenImageContainer.setProductionValue ("10");
		
		final ProductionTypeImageGfx plusHalfImageContainer = new ProductionTypeImageGfx ();
		plusHalfImageContainer.setProductionImageFile ("I+half");
		plusHalfImageContainer.setProductionValue ("½");

		final ProductionTypeGfx productionTypeImages = new ProductionTypeGfx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (plusTenImageContainer);
		productionTypeImages.getProductionTypeImage ().add (plusHalfImageContainer);
		productionTypeImages.buildMap ();
		
		final ProductionTypeImageGfx plusOneManaContainer = new ProductionTypeImageGfx ();
		plusOneManaContainer.setProductionImageFile ("M+1");
		plusOneManaContainer.setProductionValue ("1");

		final ProductionTypeImageGfx plusTenManaContainer = new ProductionTypeImageGfx ();
		plusTenManaContainer.setProductionImageFile ("M+10");
		plusTenManaContainer.setProductionValue ("10");
		
		final ProductionTypeImageGfx plusHalfManaContainer = new ProductionTypeImageGfx ();
		plusHalfManaContainer.setProductionImageFile ("M+half");
		plusHalfManaContainer.setProductionValue ("½");

		final ProductionTypeGfx manaImages = new ProductionTypeGfx ();
		manaImages.getProductionTypeImage ().add (plusOneManaContainer);
		manaImages.getProductionTypeImage ().add (plusTenManaContainer);
		manaImages.getProductionTypeImage ().add (plusHalfManaContainer);
		manaImages.buildMap ();
		
		final ProductionTypeGfx noProductionTypeImages = new ProductionTypeGfx ();
		noProductionTypeImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (productionTypeImages);
		when (gfx.findProductionType ("RE02", "generateUpkeepImage")).thenReturn (noProductionTypeImages);
		when (gfx.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "generateUpkeepImage")).thenReturn (manaImages);
		
		// Set up object to test
		final ResourceValueClientUtilsImpl obj = new ResourceValueClientUtilsImpl ();
		obj.setGraphicsDB (gfx);
		obj.setUtils (utils);

		// Test null conditions
		assertNull (obj.generateUpkeepImage (null, false)); 
		assertNull (obj.generateUpkeepImage (new ArrayList<ProductionTypeAndUndoubledValue> (), false));
		
		// List containing a production type for which we have images, but the value is 0
		final ProductionTypeAndUndoubledValue list1upkeep = new ProductionTypeAndUndoubledValue ();
		list1upkeep.setProductionTypeID ("RE01");
		list1upkeep.setUndoubledProductionValue (0);
		
		final List<ProductionTypeAndUndoubledValue> list1 = new ArrayList<ProductionTypeAndUndoubledValue> ();
		list1.add (list1upkeep);
		
		assertNull (obj.generateUpkeepImage (list1, false));

		// List containing a production type for which don't we have images, but the value is 0
		list1upkeep.setProductionTypeID ("RE02");
		assertNull (obj.generateUpkeepImage (list1, false));

		// List containing a production type for which don't we have images, and the value is non-zero
		list1upkeep.setUndoubledProductionValue (10);
		assertNull (obj.generateUpkeepImage (list1, false));
		
		// Single production
		final ProductionTypeAndUndoubledValue list4upkeep = new ProductionTypeAndUndoubledValue ();
		list4upkeep.setProductionTypeID ("RE01");
		list4upkeep.setUndoubledProductionValue (12);
		
		final List<ProductionTypeAndUndoubledValue> list4 = new ArrayList<ProductionTypeAndUndoubledValue> ();
		list4.add (list4upkeep);
		
		final BufferedImage image4 = obj.generateUpkeepImage (list4, false);
		checkImage (image4, "T X X");
		
		// Two productions
		final ProductionTypeAndUndoubledValue list4mana = new ProductionTypeAndUndoubledValue ();
		list4mana.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		list4mana.setUndoubledProductionValue (14);
		
		list4.add (list4mana);
		
		final BufferedImage image5 = obj.generateUpkeepImage (list4, false);
		checkImage (image5, "T X X t x x x x");		
		
		// Two productions, and one is halved but an even number
		final BufferedImage image6 = obj.generateUpkeepImage (list4, true);
		checkImage (image6, "T X X x x x x x x x");		

		// Two productions, and one is halved and needs to display the half icon
		list4mana.setUndoubledProductionValue (15);
		
		final BufferedImage image7 = obj.generateUpkeepImage (list4, true);
		checkImage (image7, "T X X x x x x x x x h");		
	}
	
	/**
	 * Checks that a generated image was as expected, using a pattern where
	 * X = +1
	 * T = +10
	 * G = -1
	 * M = -10
	 * H = ½
	 * space = transparent
	 * 
	 * For generateUpkeepImage, mana upkeep uses the same letters in lower case
	 * 
	 * @param image Image generated by unit test
	 * @param pattern Pattern that the generated image should look like
	 */
	private final void checkImage (final BufferedImage image, final String pattern)
	{
		assertEquals (pattern.length (), image.getWidth ());
		assertEquals (1, image.getHeight ());
		
		for (int n = 0; n < image.getWidth (); n++)
		{
			final int expectedColour;
			switch (pattern.charAt (n))
			{
				case 'X':
					expectedColour = PLUS_ONE;
					break;
					
				case 'G':
					expectedColour = MINUS_ONE;
					break;

				case 'T':
					expectedColour = PLUS_TEN;
					break;
					
				case 'M':
					expectedColour = MINUS_TEN;
					break;

				case 'H':
					expectedColour = PLUS_HALF;
					break;
					
				case 'x':
					expectedColour = PLUS_ONE_MANA;
					break;
					
				case 'g':
					expectedColour = MINUS_ONE_MANA;
					break;

				case 't':
					expectedColour = PLUS_TEN_MANA;
					break;
					
				case 'm':
					expectedColour = MINUS_TEN_MANA;
					break;

				case 'h':
					expectedColour = PLUS_HALF_MANA;
					break;
					
				case ' ':
					expectedColour = TRANSPARENT;
					break;
					
				default:
					throw new RuntimeException ("pattern contained an unhandled char \"" + pattern.charAt (n) + "\"");
			}
			
			assertEquals (expectedColour, image.getRGB (n, 0));
		}
	}
}