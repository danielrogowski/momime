package momime.client.utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeGfx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndUndoubledValue;

/**
 * Utils for dealing with graphics of resource values/production icons
 */
public final class ResourceValueClientUtilsImpl implements ResourceValueClientUtils
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (ResourceValueClientUtilsImpl.class);
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/**
	 * Generates an image showing a number of production icons.  This is used in a few places, e.g. the panel
	 * on the city view that shows all the production and consumption of the city; or showing upkeep on the unit details screen.
	 * 
	 * Its a bit odd that we generate a single image for this, rather than just creating n JLabels in the layout manger, for
	 * however many icons need to be displayed.  The reason it is done like this is because the production icons on the city
	 * view must be created as buttons, so they can be clicked to display the calculation breakdown.
	 * So the buttons need to be a complete single image. 
	 * 
	 * Production+Consumption are shown by splitting the icons into 2 sections - the left shows production+consumption that
	 * cancel each other out, and the right shows whatever is leftover.
	 * 
	 * e.g. if gold Production is 9 and Consumption is 0, it will display 9 gold coins. 
	 * e.g. if gold Production is 0 and Consumption is 9, it will display 9 grey coins. 
	 * e.g. if gold Production is 9 and Consumption is 3, it will display 3 gold coins, a gap, then 6 more gold coins. 
	 * e.g. if gold Production is 3 and Consumption is 9, it will display 3 gold coins, a gap, then 6 grey coins.
	 * e.g. if gold Production is 9½ and Consumption is 3, it will display 3 gold coins, a gap, then 6½ more gold coins. 
	 * e.g. if gold Production is 3½ and Consumption is 9, this should ideally display 3½ gold coins, a gap, then 5½ grey coins but negative halves aren't supported.
	 * 
	 * "Production" here is a loose term, since typically (e.g. unit details screen, and change city construction screens)
	 * all display upkeep using gold coins so they're displaying upkeep as a "production", not consumption.
	 * 
	 * Note not all resource types include all images.  Many do not define negative grey production icons.  Requesting an image
	 * of a production type that doesn't have the necessary icons (e.g. negative production on a production type which has no grey icons)
	 * will just result in getting a null image back.   
	 *  
	 * @param productionTypeID Type of production being shown
	 * @param productionValue Positive production to show
	 * @param consumptionValue Negative production to show
	 * @return Image of production icons; null if this type of production has no images at all or the values are both zero
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Override
	public final BufferedImage generateProductionImage (final String productionTypeID, final int productionValue, final int consumptionValue)
		throws IOException
	{
		log.trace ("Entering generateProductionImage: " + productionTypeID + ", +" + productionValue + ", -" + consumptionValue);

		BufferedImage image = null;

		if ((productionValue > 0) || (consumptionValue > 0))
		{
			final ProductionTypeGfx productionTypeImages = getGraphicsDB ().findProductionType (productionTypeID, "generateProductionImage");

			// Get a list of all the images we need to draw, so we know how big to create the merged image
			final List<BufferedImage> consumptionImages = new ArrayList<BufferedImage> ();
			final List<BufferedImage> productionImages = new ArrayList<BufferedImage> ();
			
			// Draw consumption on the left and production on the right
			// or more accurately, the production+consumption that cancel each other out are drawn on the left, and whatever is leftover on the right
			addProductionImages (productionTypeImages, consumptionImages, Math.min (consumptionValue, productionValue));
			addProductionImages (productionTypeImages, productionImages, productionValue - consumptionValue);
			
			// Now can generate the bitmap from the lists
			image = generateProductionImageFromLists (consumptionImages, productionImages);
		}
		
		log.trace ("Exiting generateProductionImage = " + ((image == null) ? "null" : (image.getWidth () + " x " + image.getHeight ())));
		return image;
	}
	
	/**
	 * Similar to the above, except it includes multiple production types into the same image; it doesn't however handle consumption/grey icons
	 * 
	 * @param upkeeps List of upkeep values to draw
	 * @param halveManaUpkeep Whether to halve the mana upkeep value displayed (i.e. do we have the Channeler retort)
	 * @return Image of production icons; null if the list is null, empty, contains only zero values, or contains only production types that have no images
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Override
	public final BufferedImage generateUpkeepImage (final List<ProductionTypeAndUndoubledValue> upkeeps, final boolean halveManaUpkeep) throws IOException
	{
		log.trace ("Entering generateUpkeepImage: " + ((upkeeps == null) ? "null" : Integer.valueOf (upkeeps.size ()).toString ()) + ", " + halveManaUpkeep);
		
		BufferedImage image = null;
		if ((upkeeps != null) && (upkeeps.size () > 0))
		{
			// Generate a list of images that we need to include
			final List<BufferedImage> productionImages = new ArrayList<BufferedImage> ();
			
			for (final ProductionTypeAndUndoubledValue upkeep : upkeeps)
			{
				final ProductionTypeGfx productionTypeImages = getGraphicsDB ().findProductionType (upkeep.getProductionTypeID (), "generateUpkeepImage");

				// Halve value or not?  This is so summoned units display half upkeep if we have the Channeler retort
				int value = upkeep.getUndoubledProductionValue ();
				boolean showHalf = false;
				if ((halveManaUpkeep) && (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA.equals (upkeep.getProductionTypeID ())))
				{
					showHalf = ((value % 2) != 0);
					value = value / 2;
				}
						
				// Add images for this type of upkeep
				addProductionImages (productionTypeImages, productionImages, value);
				if (showHalf)
					addProductionHalfImage (productionTypeImages, productionImages);
			}
			
			// Now can generate the bitmap from the list
			image = generateProductionImageFromLists (productionImages, null);
		}
		
		log.trace ("Exiting generateUpkeepImage: " + ((image == null) ? "null" : (image.getWidth () + " x " + image.getHeight ())));
		return image;
	}


	/**
	 * Adds production type images to a list, for whatever amount is specified, e.g. if amount = 23 then will output two "ten" images and three "one" images
	 * 
	 * @param productionTypeImages Images for this production type from the graphics XML
	 * @param productionTypeButtonImages The list to add to
	 * @param amount The amount to represent with images
	 * @throws IOException If there is a problem loading any of the images
	 */
	final void addProductionImages (final ProductionTypeGfx productionTypeImages,
		final List<BufferedImage> productionTypeButtonImages, final int amount) throws IOException
	{
		if (amount != 0)
		{
			final String oneImageFilename;
			final String tenImageFilename;
			int displayAmount;
			
			if (amount > 0)
			{
				// Positive
				oneImageFilename = productionTypeImages.findProductionValueImageFile ("1");
				tenImageFilename = productionTypeImages.findProductionValueImageFile ("10");
				displayAmount = amount;
			}
			else
			{
				// Negative
				oneImageFilename = productionTypeImages.findProductionValueImageFile ("-1");
				tenImageFilename = productionTypeImages.findProductionValueImageFile ("-10");
				displayAmount = -amount;
			}
			
			final BufferedImage oneImage = (oneImageFilename == null) ? null : getUtils ().loadImage (oneImageFilename);
			final BufferedImage tenImage = (tenImageFilename == null) ? null : getUtils ().loadImage (tenImageFilename);
			
			// Now add the images
			while ((displayAmount >= 10) && (tenImage != null))
			{
				productionTypeButtonImages.add (tenImage);
				displayAmount = displayAmount - 10;
			}

			while ((displayAmount >= 1) && (oneImage != null))
			{
				productionTypeButtonImages.add (oneImage);
				displayAmount = displayAmount - 1;
			}
		}
	}
	
	/**
	 * Adds the production images for a ½ to a list
	 * 
	 * @param productionTypeImages Images for this production type from the graphics XML
	 * @param productionTypeButtonImages The list to add to
	 * @throws IOException If there is a problem loading any of the images
	 */
	final void addProductionHalfImage (final ProductionTypeGfx productionTypeImages, final List<BufferedImage> productionTypeButtonImages) throws IOException
	{
		final String halfImageFilename = productionTypeImages.findProductionValueImageFile ("½");
		if (halfImageFilename != null)
			productionTypeButtonImages.add (getUtils ().loadImage (halfImageFilename));
	}

	/**
	 * Merged all the production icons included in the left+right lists into a single image.  Either list can be left blank or null.
	 * If both lists are blank or null then a null will be output.  A gap will only be included if both lists are not empty.
	 * 
	 * @param leftImages List of images to include to the left of the gap
	 * @param rightImages List of images to include to the right of the gap
	 * @return Merged image, or null if both lists are empty or null
	 */
	final BufferedImage generateProductionImageFromLists (final List<BufferedImage> leftImages, final List<BufferedImage> rightImages)
	{
		BufferedImage image = null;
		if (((leftImages != null) && (leftImages.size () > 0)) || ((rightImages != null) && (rightImages.size () > 0)))
		{
			// Work out what size the merged image needs to be
			int buttonWidth = 0;
			int buttonHeight = 0;
		
			if (leftImages != null)
				for (final BufferedImage thisImage : leftImages)
				{
					if (buttonWidth > 0)
						buttonWidth++;
			
					buttonWidth = buttonWidth + thisImage.getWidth ();
					buttonHeight = Math.max (buttonHeight, thisImage.getHeight ());
				}

			if (rightImages != null)
				for (final BufferedImage thisImage : rightImages)
				{
					if (buttonWidth > 0)
						buttonWidth++;
			
					buttonWidth = buttonWidth + thisImage.getWidth ();
					buttonHeight = Math.max (buttonHeight, thisImage.getHeight ());
				}
		
			if ((leftImages != null) && (leftImages.size () > 0) && (rightImages != null) && (rightImages.size () > 0))
				buttonWidth = buttonWidth + 5;

			// Create the merged image
			image = new BufferedImage (buttonWidth, buttonHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = image.createGraphics ();
			try
			{
				int xpos = 0;
				if (leftImages != null)
					for (final BufferedImage thisImage : leftImages)
					{
						g.drawImage (thisImage, xpos, 0, null);
						xpos = xpos + thisImage.getWidth () + 1;
					}
			
				if (xpos > 0)
					xpos = xpos + 5;

				if (rightImages != null)
					for (final BufferedImage thisImage : rightImages)
					{
						g.drawImage (thisImage, xpos, 0, null);
						xpos = xpos + thisImage.getWidth () + 1;
					}
			}
			finally
			{
				g.dispose ();
			}
		}
		
		return image;
	}
	
	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
	}

	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}
}