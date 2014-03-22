package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.SmoothedTile;
import momime.client.graphics.database.v0_9_5.SmoothedTileType;
import momime.client.graphics.database.v0_9_5.SmoothingSystem;
import momime.client.graphics.database.v0_9_5.TileSet;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;

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
		
		// Put them in a map as we go - I don't think this map has any use after the end of this routine (i.e. nothing cares about the smoothing
		// system anymore once the bitmask -> images map is built) so scoping it locally for now.
		final Map<String, SmoothingSystemEx> smoothingSystemsMap = new HashMap<String, SmoothingSystemEx> ();
		
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
					final Animation anim = db.findAnimation (tile.getTileAnimation (), "deriveAnimationFrameCountAndSpeed");
					
					// Animations must be non-empty
					if (anim.getFrame ().size () == 0)
						throw new MomException ("Tile set " + getTileSetID () + " references animation " + tile.getTileAnimation () + " which has 0 frames");
					
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
}
