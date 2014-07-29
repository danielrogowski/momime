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
	 * Generates big bitmaps of the entire overland map in each frame of animation
	 * Delphi client did this rather differently, by building Direct3D vertex buffers to display all the map tiles; equivalent method there was RegenerateCompleteSceneryView
	 * 
	 * @param mapViewPlane Which plane to generate bitmaps for
	 * @return Array of overland map bitmaps
	 * @throws IOException If there is a problem loading any of the images
	 */
	public BufferedImage [] generateOverlandMapBitmaps (final int mapViewPlane) throws IOException;

	/**
	 * Generates big bitmap of the smoothed edges of blackness that obscure the edges
	 * of the outermost tiles we can see, so that the edges aren't just a solid black line
	 * 
	 * @param mapViewPlane Which plane to generate FOW bitmap for
	 * @return Fog of war bitmap
	 * @throws IOException If there is a problem loading any of the images
	 */
	public BufferedImage generateFogOfWarBitmap (final int mapViewPlane) throws IOException;
}