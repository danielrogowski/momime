package momime.client.calculations;

import java.awt.image.BufferedImage;

import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.database.RecordNotFoundException;

/**
 * Generates the bitmap for the mini overview map in the top right corner
 */
public interface MiniMapBitmapGenerator
{
	/**
	 * @param mapViewPlane Which plane to generate bitmap for
	 * @return Bitmap the same number of pixels in size as the map is large, i.e. 60x40 pixels for a standard size map
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the graphics DB
	 * @throws PlayerNotFoundException If we encounter a city owner that can't be found in the player list
	 */
	public BufferedImage generateMiniMapBitmap (final int mapViewPlane) throws RecordNotFoundException, PlayerNotFoundException;
}