package momime.client.calculations;

import java.awt.image.BufferedImage;
import java.io.IOException;

import momime.common.database.RecordNotFoundException;

import com.ndg.map.areas.storage.MapArea3D;

/**
 * Generates large bitmap images showing the current state of the entire overland map and fog of war.
 * This is moved out from OverlandMapUI to allow the bitmap generation and the screen layout to be more independantly unit testable.
 */
public interface OverlandMapBitmapGenerator
{
	/**
	 * Creates the smoothedTiles array as the correct size
	 * Can't just do this at the start of init (), because the server sends the first FogOfWarVisibleAreaChanged prior to the overland map being displayed,
	 * so we can prepare the map image before displaying it - so we have to create the area for it to prepare it into
	 */
	public void afterJoinedSession ();
	
	/**
	 * Converts the tile types sent by the server into actual tile numbers, smoothing the edges of various terrain types in the process
	 * @param areaToSmooth Keeps track of which tiles have been updated, so we know which need graphics updating for; or can supply null to resmooth everything
	 * @throws RecordNotFoundException If required entries in the graphics XML cannot be found
	 */
	public void smoothMapTerrain (final MapArea3D<Boolean> areaToSmooth) throws RecordNotFoundException;

	/**
	 * Generates big bitmaps of the entire overland map in each frame of animation.
	 * Delphi client did this rather differently, by building Direct3D vertex buffers to display all the map tiles; equivalent method there was RegenerateCompleteSceneryView.
	 * 
	 * Generated bitmaps will all be 20x countX by 18x countY pixels in size.
	 * 
	 * @param mapViewPlane Which plane to generate bitmaps for
	 * @param startX Map coordinate of the cell to draw at the left edge of the bitmaps
	 * @param startY Map coordinate of the cell to draw at the top edge of the bitmaps
	 * @param countX Width of the bitmap to generate, in number of map cells
	 * @param countY Height of the bitmap to generate, in number of map cells
	 * @return Array of overland map bitmaps
	 * @throws IOException If there is a problem loading any of the images
	 */
	public BufferedImage [] generateOverlandMapBitmaps (final int mapViewPlane, final int startX, final int startY, final int countX, final int countY) throws IOException;

	/**
	 * Generates big bitmap of the smoothed edges of blackness that obscure the edges
	 * of the outermost tiles we can see, so that the edges aren't just a solid black line.
	 * 
	 * Generated bitmap will be 20x countX by 18x countY pixels in size.
	 * 
	 * @param mapViewPlane Which plane to generate FOW bitmap for
	 * @param startX Map coordinate of the cell to draw at the left edge of the bitmaps
	 * @param startY Map coordinate of the cell to draw at the top edge of the bitmaps
	 * @param countX Width of the bitmap to generate, in number of map cells
	 * @param countY Height of the bitmap to generate, in number of map cells
	 * @return Fog of war bitmap
	 * @throws IOException If there is a problem loading any of the images
	 */
	public BufferedImage generateFogOfWarBitmap (final int mapViewPlane, final int startX, final int startY, final int countX, final int countY) throws IOException;
}