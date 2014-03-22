package momime.client.graphics.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.ndg.random.RandomUtils;

import momime.client.graphics.database.v0_9_5.SmoothedTile;
import momime.client.graphics.database.v0_9_5.SmoothedTileType;
import momime.common.database.RecordNotFoundException;

/**
 * Provides a map so we can go directly from an unsmoothed bitmask to a random tile image,
 * where all the reduction rules are applied up front rather than on the fly.
 */
public final class SmoothedTileTypeEx extends SmoothedTileType
{
	/** Class logger */
	private final Logger log = Logger.getLogger (SmoothedTileTypeEx.class.getName ());

	/** Map of unsmoothed bitmasks to sets of possible images */
	private final Map<String, List<SmoothedTile>> bitmasksMap = new HashMap<String, List<SmoothedTile>> ();
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/**
	 * Builds up the complete bitmasksMap
	 * 
	 * @param smoothingSystemBitmasksMap Unsmoothed->smoothed bitmasks map built from the smoothing system rules
	 * @throws RecordNotFoundException If a bitmaks that the smoothing system rules say should be in the graphics XML isn't there, i.e. an image is missing
	 */
	final void buildMap (final Map<String, List<String>> smoothingSystemBitmasksMap) throws RecordNotFoundException
	{
		log.entering (SmoothingSystemEx.class.getName (), "buildMap", new String []
			{getTileTypeID (), getSecondaryTileTypeID (), getTertiaryTileTypeID (), getCombatTileTypeID (), getSmoothingSystemID (),
			(getPlaneNumber () == null) ? "null" : getPlaneNumber ().toString ()});
		
		// For many tiles there are multiple possible images, e.g. there's multiple forest images so all large areas of forest don't look repetitive.
		// So, step 1 is to put these in a map so we can go from a *smoothed* bitmask to all of its possible images.
		// That way later on we don't have to re-search the entire ~200 tiles for every bitmask over and over.
		final Map<String, List<SmoothedTile>> smoothedMap = new HashMap<String, List<SmoothedTile>> ();
		for (final SmoothedTile tile : getSmoothedTile ())
		{
			// See if it exists in the map already
			List<SmoothedTile> images = smoothedMap.get (tile.getBitmask ());
			if (images == null)
			{
				images = new ArrayList<SmoothedTile> ();
				smoothedMap.put (tile.getBitmask (), images);
			}
			
			// Add bitmask to the list
			images.add (tile);
		}
		log.finest ("Graphics XML contains " + smoothedMap.size () + " unique smoothed bitmasks for this tile set"); 
		
		// Now step 2 we can use the smoothing system map to convert this into a map so we can go from an *unsmoothed* bitmask to all of its possible images.
		// And doing it like this means we reuse the image lists, rather than repeating it each time a smoothed bitmask is repeated.
		for (final Entry<String, List<String>> smoothedBitmask : smoothingSystemBitmasksMap.entrySet ())
		{
			// There had better be at least one image defined for it
			final List<SmoothedTile> images = smoothedMap.get (smoothedBitmask.getKey ());
			if (images == null)
				throw new RecordNotFoundException (SmoothedTile.class.getName (), smoothedBitmask.getKey (), "SmoothedTileTypeEx.buildMaps");
			
			// Add this image list against every unsmoothed bitmask
			for (final String unsmoothedBitmask : smoothedBitmask.getValue ())
				bitmasksMap.put (unsmoothedBitmask, images);
		}
		log.finest ("Built map of " + bitmasksMap.size () + " unsmoothed bitmasks for this tile set"); 
		
		log.exiting (SmoothingSystemEx.class.getName (), "buildMap", bitmasksMap.size ());
	}
	
	/**
	 * @param bitmask Unsmoothed bitmask
	 * @return Random image for this bitmask
	 * @throws RecordNotFoundException If this bitmask isn't in the map, i.e. its outside the set of unsmoothed bitmasks derived from the directions+maxValueEachDirection values
	 */
	public final SmoothedTile getRandomImage (final String bitmask) throws RecordNotFoundException
	{
		final List<SmoothedTile> images = getBitmasksMap ().get (bitmask);
		if (images == null)
			throw new RecordNotFoundException ("UnsmoothedBitmask", bitmask, "SmoothedTileTypeEx.getRandomImage");
		
		return images.get (getRandomUtils ().nextInt (images.size ()));
	}
	
	/**
	 * @return Map of unsmoothed bitmasks to sets of possible images
	 */
	final Map<String, List<SmoothedTile>> getBitmasksMap ()
	{
		return bitmasksMap;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
}
