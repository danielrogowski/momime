package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeEx;
import momime.client.graphics.database.v0_9_5.ProductionTypeImage;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitUpkeep;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;

/**
 * Tests the ResourceValueClientUtilsImpl class
 */
public final class TestResourceValueClientUtilsImpl
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
		final BufferedImage plusOneImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusOneImage.setRGB (0, 0, PLUS_ONE);

		final BufferedImage minusOneImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		minusOneImage.setRGB (0, 0, MINUS_ONE);

		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("I+1")).thenReturn (plusOneImage);
		when (utils.loadImage ("I-1")).thenReturn (minusOneImage);
		
		// Mock entries from the graphics XML
		final ProductionTypeImage plusOneImageContainer = new ProductionTypeImage ();
		plusOneImageContainer.setProductionImageFile ("I+1");
		plusOneImageContainer.setProductionValue ("1");
		
		final ProductionTypeImage minusOneImageContainer = new ProductionTypeImage ();
		minusOneImageContainer.setProductionImageFile ("I-1");
		minusOneImageContainer.setProductionValue ("-1");

		final ProductionTypeEx productionTypeImages = new ProductionTypeEx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (minusOneImageContainer);
		productionTypeImages.buildMap ();

		final ProductionTypeEx noProductionTypeImages = new ProductionTypeEx ();
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
		final BufferedImage plusOneImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusOneImage.setRGB (0, 0, PLUS_ONE);

		final BufferedImage minusOneImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		minusOneImage.setRGB (0, 0, MINUS_ONE);

		final BufferedImage plusTenImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusTenImage.setRGB (0, 0, PLUS_TEN);

		final BufferedImage minusTenImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		minusTenImage.setRGB (0, 0, MINUS_TEN);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("I+1")).thenReturn (plusOneImage);
		when (utils.loadImage ("I-1")).thenReturn (minusOneImage);
		when (utils.loadImage ("I+10")).thenReturn (plusTenImage);
		when (utils.loadImage ("I-10")).thenReturn (minusTenImage);
		
		// Mock entries from the graphics XML
		final ProductionTypeImage plusOneImageContainer = new ProductionTypeImage ();
		plusOneImageContainer.setProductionImageFile ("I+1");
		plusOneImageContainer.setProductionValue ("1");
		
		final ProductionTypeImage minusOneImageContainer = new ProductionTypeImage ();
		minusOneImageContainer.setProductionImageFile ("I-1");
		minusOneImageContainer.setProductionValue ("-1");

		final ProductionTypeImage plusTenImageContainer = new ProductionTypeImage ();
		plusTenImageContainer.setProductionImageFile ("I+10");
		plusTenImageContainer.setProductionValue ("10");
		
		final ProductionTypeImage minusTenImageContainer = new ProductionTypeImage ();
		minusTenImageContainer.setProductionImageFile ("I-10");
		minusTenImageContainer.setProductionValue ("-10");

		final ProductionTypeEx productionTypeImages = new ProductionTypeEx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (minusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (plusTenImageContainer);
		productionTypeImages.getProductionTypeImage ().add (minusTenImageContainer);
		productionTypeImages.buildMap ();

		final ProductionTypeEx noProductionTypeImages = new ProductionTypeEx ();
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
		final BufferedImage plusOneImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusOneImage.setRGB (0, 0, PLUS_ONE);

		final BufferedImage plusHalfImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusHalfImage.setRGB (0, 0, PLUS_HALF);

		final BufferedImage plusOneMana = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusOneMana.setRGB (0, 0, PLUS_ONE_MANA);

		final BufferedImage plusHalfMana = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusHalfMana.setRGB (0, 0, PLUS_HALF_MANA);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("I+1")).thenReturn (plusOneImage);
		when (utils.loadImage ("I+half")).thenReturn (plusHalfImage);
		when (utils.loadImage ("M+1")).thenReturn (plusOneMana);
		when (utils.loadImage ("M+half")).thenReturn (plusHalfMana);

		// Mock entries from the graphics XML
		final ProductionTypeImage plusOneImageContainer = new ProductionTypeImage ();
		plusOneImageContainer.setProductionImageFile ("I+1");
		plusOneImageContainer.setProductionValue ("1");

		final ProductionTypeImage plusHalfImageContainer = new ProductionTypeImage ();
		plusHalfImageContainer.setProductionImageFile ("I+half");
		plusHalfImageContainer.setProductionValue ("½");

		final ProductionTypeEx productionTypeImages = new ProductionTypeEx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (plusHalfImageContainer);
		productionTypeImages.buildMap ();
		
		final ProductionTypeImage plusOneManaContainer = new ProductionTypeImage ();
		plusOneManaContainer.setProductionImageFile ("M+1");
		plusOneManaContainer.setProductionValue ("1");

		final ProductionTypeImage plusHalfManaContainer = new ProductionTypeImage ();
		plusHalfManaContainer.setProductionImageFile ("M+half");
		plusHalfManaContainer.setProductionValue ("½");

		final ProductionTypeEx manaImages = new ProductionTypeEx ();
		manaImages.getProductionTypeImage ().add (plusOneManaContainer);
		manaImages.getProductionTypeImage ().add (plusHalfManaContainer);
		manaImages.buildMap ();
		
		final ProductionTypeEx noProductionTypeImages = new ProductionTypeEx ();
		noProductionTypeImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (productionTypeImages);
		when (gfx.findProductionType ("RE02", "generateUpkeepImage")).thenReturn (noProductionTypeImages);
		when (gfx.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, "generateUpkeepImage")).thenReturn (manaImages);
		
		// Set up object to test
		final ResourceValueClientUtilsImpl obj = new ResourceValueClientUtilsImpl ();
		obj.setGraphicsDB (gfx);
		obj.setUtils (utils);

		// Test null conditions
		assertNull (obj.generateUpkeepImage (null, false)); 
		assertNull (obj.generateUpkeepImage (new ArrayList<UnitUpkeep> (), false));
		
		// List containing a production type for which we have images, but the value is 0
		final UnitUpkeep list1upkeep = new UnitUpkeep ();
		list1upkeep.setProductionTypeID ("RE01");
		list1upkeep.setUpkeepValue (0);
		
		final List<UnitUpkeep> list1 = new ArrayList<UnitUpkeep> ();
		list1.add (list1upkeep);
		
		assertNull (obj.generateUpkeepImage (list1, false));

		// List containing a production type for which don't we have images, but the value is 0
		list1upkeep.setProductionTypeID ("RE02");
		assertNull (obj.generateUpkeepImage (list1, false));

		// List containing a production type for which don't we have images, and the value is non-zero
		list1upkeep.setUpkeepValue (10);
		assertNull (obj.generateUpkeepImage (list1, false));
		
		// Single production
		final UnitUpkeep list4upkeep = new UnitUpkeep ();
		list4upkeep.setProductionTypeID ("RE01");
		list4upkeep.setUpkeepValue (12);
		
		final List<UnitUpkeep> list4 = new ArrayList<UnitUpkeep> ();
		list4.add (list4upkeep);
		
		final BufferedImage image4 = obj.generateUpkeepImage (list4, false);
		checkImage (image4, "X X X X X X X X X X X X");
		
		// Two productions
		final UnitUpkeep list4mana = new UnitUpkeep ();
		list4mana.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		list4mana.setUpkeepValue (14);
		
		list4.add (list4mana);
		
		final BufferedImage image5 = obj.generateUpkeepImage (list4, false);
		checkImage (image5, "X X X X X X X X X X X X x x x x x x x x x x x x x x");		
		
		// Two productions, and one is halved but an even number
		final BufferedImage image6 = obj.generateUpkeepImage (list4, true);
		checkImage (image6, "X X X X X X X X X X X X x x x x x x x");		

		// Two productions, and one is halved and needs to display the half icon
		list4mana.setUpkeepValue (15);
		
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
		final BufferedImage plusOneImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusOneImage.setRGB (0, 0, PLUS_ONE);

		final BufferedImage plusTenImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusTenImage.setRGB (0, 0, PLUS_TEN);
		
		final BufferedImage plusHalfImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusHalfImage.setRGB (0, 0, PLUS_HALF);

		final BufferedImage plusOneMana = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusOneMana.setRGB (0, 0, PLUS_ONE_MANA);

		final BufferedImage plusTenMana = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusTenMana.setRGB (0, 0, PLUS_TEN_MANA);
		
		final BufferedImage plusHalfMana = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		plusHalfMana.setRGB (0, 0, PLUS_HALF_MANA);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("I+1")).thenReturn (plusOneImage);
		when (utils.loadImage ("I+10")).thenReturn (plusTenImage);
		when (utils.loadImage ("I+half")).thenReturn (plusHalfImage);
		when (utils.loadImage ("M+1")).thenReturn (plusOneMana);
		when (utils.loadImage ("M+10")).thenReturn (plusTenMana);
		when (utils.loadImage ("M+half")).thenReturn (plusHalfMana);

		// Mock entries from the graphics XML
		final ProductionTypeImage plusOneImageContainer = new ProductionTypeImage ();
		plusOneImageContainer.setProductionImageFile ("I+1");
		plusOneImageContainer.setProductionValue ("1");

		final ProductionTypeImage plusTenImageContainer = new ProductionTypeImage ();
		plusTenImageContainer.setProductionImageFile ("I+10");
		plusTenImageContainer.setProductionValue ("10");
		
		final ProductionTypeImage plusHalfImageContainer = new ProductionTypeImage ();
		plusHalfImageContainer.setProductionImageFile ("I+half");
		plusHalfImageContainer.setProductionValue ("½");

		final ProductionTypeEx productionTypeImages = new ProductionTypeEx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.getProductionTypeImage ().add (plusTenImageContainer);
		productionTypeImages.getProductionTypeImage ().add (plusHalfImageContainer);
		productionTypeImages.buildMap ();
		
		final ProductionTypeImage plusOneManaContainer = new ProductionTypeImage ();
		plusOneManaContainer.setProductionImageFile ("M+1");
		plusOneManaContainer.setProductionValue ("1");

		final ProductionTypeImage plusTenManaContainer = new ProductionTypeImage ();
		plusTenManaContainer.setProductionImageFile ("M+10");
		plusTenManaContainer.setProductionValue ("10");
		
		final ProductionTypeImage plusHalfManaContainer = new ProductionTypeImage ();
		plusHalfManaContainer.setProductionImageFile ("M+half");
		plusHalfManaContainer.setProductionValue ("½");

		final ProductionTypeEx manaImages = new ProductionTypeEx ();
		manaImages.getProductionTypeImage ().add (plusOneManaContainer);
		manaImages.getProductionTypeImage ().add (plusTenManaContainer);
		manaImages.getProductionTypeImage ().add (plusHalfManaContainer);
		manaImages.buildMap ();
		
		final ProductionTypeEx noProductionTypeImages = new ProductionTypeEx ();
		noProductionTypeImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (productionTypeImages);
		when (gfx.findProductionType ("RE02", "generateUpkeepImage")).thenReturn (noProductionTypeImages);
		when (gfx.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, "generateUpkeepImage")).thenReturn (manaImages);
		
		// Set up object to test
		final ResourceValueClientUtilsImpl obj = new ResourceValueClientUtilsImpl ();
		obj.setGraphicsDB (gfx);
		obj.setUtils (utils);

		// Test null conditions
		assertNull (obj.generateUpkeepImage (null, false)); 
		assertNull (obj.generateUpkeepImage (new ArrayList<UnitUpkeep> (), false));
		
		// List containing a production type for which we have images, but the value is 0
		final UnitUpkeep list1upkeep = new UnitUpkeep ();
		list1upkeep.setProductionTypeID ("RE01");
		list1upkeep.setUpkeepValue (0);
		
		final List<UnitUpkeep> list1 = new ArrayList<UnitUpkeep> ();
		list1.add (list1upkeep);
		
		assertNull (obj.generateUpkeepImage (list1, false));

		// List containing a production type for which don't we have images, but the value is 0
		list1upkeep.setProductionTypeID ("RE02");
		assertNull (obj.generateUpkeepImage (list1, false));

		// List containing a production type for which don't we have images, and the value is non-zero
		list1upkeep.setUpkeepValue (10);
		assertNull (obj.generateUpkeepImage (list1, false));
		
		// Single production
		final UnitUpkeep list4upkeep = new UnitUpkeep ();
		list4upkeep.setProductionTypeID ("RE01");
		list4upkeep.setUpkeepValue (12);
		
		final List<UnitUpkeep> list4 = new ArrayList<UnitUpkeep> ();
		list4.add (list4upkeep);
		
		final BufferedImage image4 = obj.generateUpkeepImage (list4, false);
		checkImage (image4, "T X X");
		
		// Two productions
		final UnitUpkeep list4mana = new UnitUpkeep ();
		list4mana.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		list4mana.setUpkeepValue (14);
		
		list4.add (list4mana);
		
		final BufferedImage image5 = obj.generateUpkeepImage (list4, false);
		checkImage (image5, "T X X t x x x x");		
		
		// Two productions, and one is halved but an even number
		final BufferedImage image6 = obj.generateUpkeepImage (list4, true);
		checkImage (image6, "T X X x x x x x x x");		

		// Two productions, and one is halved and needs to display the half icon
		list4mana.setUpkeepValue (15);
		
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