package momime.client.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import momime.common.database.UnitUpkeep;

/**
 * Utils for dealing with graphics of resource values/production icons
 */
public interface ResourceValueClientUtils
{
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
	public BufferedImage generateProductionImage (final String productionTypeID, final int productionValue, final int consumptionValue)
		throws IOException;
	
	/**
	 * Similar to the above, except it includes multiple production types into the same image; it doesn't however handle consumption/grey icons
	 * 
	 * @param upkeeps List of upkeep values to draw
	 * @param halveManaUpkeep Whether to halve the mana upkeep value displayed (i.e. do we have the Channeler retort)
	 * @return Image of production icons; null if the list is null, empty, contains only zero values, or contains only production types that have no images
	 * @throws IOException If there is a problem loading any of the images
	 */
	public BufferedImage generateUpkeepImage (final List<UnitUpkeep> upkeeps, final boolean halveManaUpkeep) throws IOException;
}