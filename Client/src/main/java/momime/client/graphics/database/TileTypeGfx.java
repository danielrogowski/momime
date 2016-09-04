package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_8.TileType;
import momime.client.graphics.database.v0_9_8.TileTypeMiniMap;

/**
 * Adds a map over the planes, so we can find the minimap colours quickly
 */
public final class TileTypeGfx extends TileType
{
	/** Map of planes to minimap colours */
	private Map<Integer, Integer> miniMapColoursMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		miniMapColoursMap = new HashMap<Integer, Integer> ();
		for (final TileTypeMiniMap miniMap : getTileTypeMiniMap ())
			miniMapColoursMap.put (miniMap.getPlaneNumber (), Integer.parseInt (miniMap.getMiniMapPixelColour (), 16));
	}
	
	/**
	 * NB. This is a bit of a special case, normally we expect all elements to be present in the graphics DB and throw exceptions if they aren't.
	 * But here its acceptable for there to be colours missing - for the fog of war "tile types" where there is nothing to draw.
	 * 
	 * @param planeNumber Plane number
	 * @return Color to draw pixels for this tile type on the minimap; or null if no colour is defined for this plane
	 */
	public final Integer findMiniMapColour (final int planeNumber)
	{
		return miniMapColoursMap.get (planeNumber);
	}
}