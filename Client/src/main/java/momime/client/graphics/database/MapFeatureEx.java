package momime.client.graphics.database;

import java.awt.image.BufferedImage;
import java.io.IOException;

import momime.client.graphics.database.v0_9_5.MapFeature;
import momime.client.ui.MomUIUtils;
import momime.common.MomException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extends graphics XML map feature object with consistency checks 
 */
public final class MapFeatureEx extends MapFeature
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MapFeatureEx.class);
	
	/** Helper methods and constants for creating and laying out Swing components */
	private MomUIUtils utils;
	
	/**
	 * Map features must have the same width and height as all the terrain tiles
	 * @param overlandMapTileSet Overland map terrain tile set
	 * @throws IOException If there is a problem loading the image, or the map feature is the wrong size
	 */
	public final void checkWidthAndHeight (final TileSetEx overlandMapTileSet) throws IOException
	{
		log.trace ("Entering checkWidthAndHeight: " + getMapFeatureID ());
		
		final BufferedImage image = getUtils ().loadImage (getOverlandMapImageFile ());
		if ((image.getWidth () != overlandMapTileSet.getTileWidth ()) || (image.getHeight () != overlandMapTileSet.getTileHeight ()))
			
			throw new MomException ("Overland map image for map feature " + getMapFeatureID () + " is the wrong size - " +
				image.getWidth () + "x" + image.getHeight () + " when it should match the terrain tiles which are all " +
				overlandMapTileSet.getTileWidth () + "x" + overlandMapTileSet.getTileHeight ());
		
		log.trace ("Exiting checkWidthAndHeight");
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