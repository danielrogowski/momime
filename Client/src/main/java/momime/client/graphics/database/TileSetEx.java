package momime.client.graphics.database;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import momime.client.graphics.database.v0_9_5.SmoothedTile;
import momime.client.graphics.database.v0_9_5.SmoothedTileType;
import momime.client.graphics.database.v0_9_5.SmoothingSystem;
import momime.client.graphics.database.v0_9_5.TileSet;
import momime.client.ui.MomUIUtils;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.utils.CompareUtils;

/**
 * Links together the cached smoothing reduction rules against the actual images for each tile type
 */
public final class TileSetEx extends TileSet
{
	/** Class logger */
	private final Logger log = Logger.getLogger (TileSetEx.class.getName ());
	
	/** All animations used by tiles in the same tile set must share the same number of frames, which gets set here; if tile set is all static images, will be set to 1 */
	private int animationFrameCount;

	/** All animations used by tiles in the same tile set must share the same animation speed, which gets set here; if tile set is all static images, will be left as null */
	private Double animationSpeed;

	/** All images and animation frames used by tiles in the same tile set must share the same width */
	int tileWidth;
	
	/** All images and animation frames used by tiles in the same tile set must share the same height */
	int tileHeight;
	
	/** Map of smoothing system IDs to smoothing systems */
	private Map<String, SmoothingSystemEx> smoothingSystemsMap;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private MomUIUtils utils;
	
	/**
	 * Builds all of the maps on all child objects.
	 * @throws MomException If there are invalid rules defined, i.e. with a mixture of null and non-null condition/set rules
	 * @throws RecordNotFoundException If an image is missing, or a tile type refers to a smoothing system that doesn't exist
	 */
	final void buildMaps () throws MomException, RecordNotFoundException
	{
		log.entering (TileSetEx.class.getName (), "buildMaps", getTileSetID ());
		
		// Have to do this in two passes, since we need all smoothing systems loaded before we can start loading smoothed tile types.
		// i.e. one smoothing system may be shared by multiple tile types.
		smoothingSystemsMap = new HashMap<String, SmoothingSystemEx> ();
		
		for (final SmoothingSystem ss : getSmoothingSystem ())
		{
			final SmoothingSystemEx ssex = (SmoothingSystemEx) ss;
			ssex.buildMap (getDirections ());
			smoothingSystemsMap.put (ssex.getSmoothingSystemID (), ssex);
		}
		
		// Now we can directly find each smoothing system and have the smoothed -> list of unsmoothed map build, can deal with each tile type
		for (final SmoothedTileType tt : getSmoothedTileType ())
		{
			final SmoothingSystemEx ss = smoothingSystemsMap.get (tt.getSmoothingSystemID ());
			if (ss == null)
				throw new RecordNotFoundException (SmoothingSystem.class.getName (), tt.getSmoothingSystemID (), "TileSetEx.buildMaps");
					
			final SmoothedTileTypeEx ttex = (SmoothedTileTypeEx) tt;
			ttex.buildMap (ss.getBitmasksMap ());
		}
		
		log.info ("Processed all smoothing system rules for the " + getTileSetName () + " tile set");		
		log.exiting (TileSetEx.class.getName (), "buildMaps");
	}
	
	/**
	 * Derives the animationFrameCount and animationSpeed values above from all of the tiles of all of the tile types for this tile set.
	 * Could have just made this part of buildMaps (), but decided to split the methods above since it gives more relevant and clear unit tests.
	 * 
	 * @param db Graphics database, so we can look up animations
	 * @throws RecordNotFoundException If one of the tiles refers to an animation that doesn't exist
	 * @throws MomException If the values aren't consistent - every animated tile under one tile set must share the same values; or we find an empty animation
	 */
	final void deriveAnimationFrameCountAndSpeed (final GraphicsDatabaseEx db) throws RecordNotFoundException, MomException
	{
		log.entering (TileSetEx.class.getName (), "deriveAnimationFrameCountAndSpeed", getTileSetID ());
		
		for (final SmoothedTileType tt : getSmoothedTileType ())
			for (final SmoothedTile tile : tt.getSmoothedTile ())
				if (tile.getTileAnimation () != null)
				{
					final AnimationEx anim = db.findAnimation (tile.getTileAnimation (), "deriveAnimationFrameCountAndSpeed");
					
					// If they've never been set, set now
					if (animationSpeed == null)
					{
						animationFrameCount = anim.getFrame ().size ();
						animationSpeed = anim.getAnimationSpeed ();
					}
					else
					{
						// Check consistency with existing values
						if (animationFrameCount != anim.getFrame ().size ())
							throw new MomException ("Frame count of all animations references by tile set " + getTileSetID () + " is not consistent (some are " +
								animationFrameCount + " and some are " + anim.getFrame ().size () + ")");
							
						if (!animationSpeed.equals (anim.getAnimationSpeed ()))
							throw new MomException ("Animation speed of all animations references by tile set " + getTileSetID () + " is not consistent (some are " +
								animationSpeed + " FPS and some are " + anim.getAnimationSpeed () + " FPS)");
					}					
				}

		// If only static images, set frameCount to 1
		if (animationSpeed == null)
			animationFrameCount = 1;
		
		log.info (getTileSetName () + " tile set consistently has " + animationFrameCount + " frames at " + animationSpeed + " FPS");		
		log.exiting (TileSetEx.class.getName (), "deriveAnimationFrameCountAndSpeed");
	}
	
	/**
	 * Derives tileWidth and tileHeight values above from all of the tiles of all of the tile types for this tile set.
	 * Could have just made this part of buildMaps (), but decided to split the methods above since it gives more relevant and clear unit tests.
	 * 
	 * This assumes all the animations in the DB have already had deriveAnimationWidthAndHeight ran on them to set the animationWidth + Height values correctly.
	 * 
	 * @param db Graphics database, so we can look up animations
	 * @throws IOException If there is a problem loading any of the images, or we fail the consistency checks
	 */
	final void deriveTileWidthAndHeight (final GraphicsDatabaseEx db) throws IOException
	{
		log.entering (TileSetEx.class.getName (), "deriveTileWidthAndHeight", getTileSetID ());

		boolean first = true;
		
		for (final SmoothedTileType tt : getSmoothedTileType ())
			
			// Features (like rocks and trees) and buildings are smaller or larger than the actual diagonal combat tiles, so exclude them from the check
			// Checking this on the code is a bit of a hack, it should look the code up in the DB and check if the layer = BUILDINGS_AND_TERRAIN_FEATURES
			// but that's not possible with the current setup because even if we include the map layer in the client DB, the server doesn't send us the client
			// DB until we join a game, and I really want the graphics XML parsing to be done at startup.  So, unless I do some kind of merger of all the XML
			// files in future, for now this is the only way.
			if ((tt.getCombatTileTypeID () == null) || (!tt.getCombatTileTypeID ().startsWith ("CBL")))
				for (final SmoothedTile tile : tt.getSmoothedTile ())
					if (tile.getTileFile () != null)
					{
						final BufferedImage image = getUtils ().loadImage (tile.getTileFile ());
						if (first)
						{
							tileWidth = image.getWidth ();
							tileHeight = image.getHeight ();
							first = false;
						}
						else if ((tileWidth != image.getWidth ()) || (tileHeight != image.getHeight ()))
							throw new MomException ("Images and/or animations referenced by tile set " + getTileSetID () + " are not consistent sizes (some are " +
								tileWidth + "x" + tileHeight + " and some are " + image.getWidth () + "x" + image.getHeight () + ")");
					}
				
					else if (tile.getTileAnimation () != null)
					{
						final AnimationEx anim = db.findAnimation (tile.getTileAnimation (), "deriveTileWidthAndHeight");
						if (first)
						{
							tileWidth = anim.getAnimationWidth ();
							tileHeight = anim.getAnimationHeight ();
							first = false;
						}
						else if ((tileWidth != anim.getAnimationWidth ()) || (tileHeight != anim.getAnimationHeight ()))
							throw new MomException ("Images and/or animations referenced by tile set " + getTileSetID () + " are not consistent sizes (some are " +
								tileWidth + "x" + tileHeight + " and some are " + anim.getAnimationWidth () + "x" + anim.getAnimationHeight () + ")");
					}
		
					else
						throw new MomException ("Tile set " + getTileSetID () + " includes a tile that neither includes an image filename or an animation ID");
		
		log.info (getTileSetName () + " tile set consistently has all tiles of size " + tileWidth + "x" + tileHeight);		
		log.exiting (TileSetEx.class.getName (), "deriveTileWidthAndHeight");
	}
	
	/**
	 * @param smoothingSystemID Smoothing system ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Smoothing system object
	 * @throws RecordNotFoundException If the smoothingSystemID doesn't exist
	 */
	public final SmoothingSystemEx findSmoothingSystem (final String smoothingSystemID, final String caller) throws RecordNotFoundException
	{
		final SmoothingSystemEx found = smoothingSystemsMap.get (smoothingSystemID);
		if (found == null)
			throw new RecordNotFoundException (SmoothingSystemEx.class.getName (), smoothingSystemID, caller);

		return found;
	}
	
	/**
	 * @param overlandMapTileTypeID Overland map tile type ID to search for
	 * @param planeNumber Plane number to search for
	 * @param combatTileTypeID Combat map tile type ID to search for; pass null for searching for overland map tiles
	 * @return Requested smoothed tile type
	 * @throws RecordNotFoundException If no matching tile type is found
	 */
	public final SmoothedTileTypeEx findSmoothedTileType (final String overlandMapTileTypeID, final Integer planeNumber, final String combatTileTypeID)
		throws RecordNotFoundException
	{
		// Note the overlandMapTileTypeID and planeNumber *in the database* are optional, in which case will match regardless of what values are passed in
		// This is why we have to do the search the hard way, rather than using a map
		SmoothedTileTypeEx match = null;
		final Iterator<SmoothedTileType> iter = getSmoothedTileType ().iterator ();
		while ((match == null) && (iter.hasNext ()))
		{
			final SmoothedTileTypeEx thisTileType = (SmoothedTileTypeEx) iter.next ();
			if (((thisTileType.getTileTypeID () == null) || (thisTileType.getTileTypeID ().equals (overlandMapTileTypeID))) &&
				((thisTileType.getPlaneNumber () == null) || (thisTileType.getPlaneNumber ().equals (planeNumber))) &&
				(CompareUtils.safeStringCompare (combatTileTypeID, thisTileType.getCombatTileTypeID ())))
				
				match = thisTileType;
		}
		
		if (match == null)
			throw new RecordNotFoundException (SmoothedTileTypeEx.class.getName (), overlandMapTileTypeID + "/" + planeNumber + "/" + combatTileTypeID, "findSmoothedTileType");
		
		return match;
	}

	/** 
	 * @return All animations used by tiles in the same tile set must share the same number of frames, which gets set here; if tile set is all static images, will be set to 1
	 */
	public final int getAnimationFrameCount ()
	{
		return animationFrameCount;
	}

	/**
	 * @return All animations used by tiles in the same tile set must share the same animation speed, which gets set here; if tile set is all static images, will be left as null
	 */
	public final Double getAnimationSpeed ()
	{
		return animationSpeed;
	}

	/**
	 * @return All images and animation frames used by tiles in the same tile set must share the same width
	 */
	public final int getTileWidth ()
	{
		return tileWidth;
	}
	
	/**
	 * @return All images and animation frames used by tiles in the same tile set must share the same height
	 */
	public final int getTileHeight ()
	{
		return tileHeight;
	}

	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final MomUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final MomUIUtils util)
	{
		utils = util;
	}
}
