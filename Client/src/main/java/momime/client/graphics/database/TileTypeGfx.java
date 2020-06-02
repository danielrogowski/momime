package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_8.TileType;
import momime.client.graphics.database.v0_9_8.TileTypeMiniMap;
import momime.client.graphics.database.v0_9_8.TileTypeRoad;
import momime.common.database.RecordNotFoundException;

/**
 * Adds a map over the planes, so we can find the minimap colours quickly
 */
public final class TileTypeGfx extends TileType
{
	/** Map of planes to minimap colours */
	private Map<Integer, Integer> miniMapColoursMap;
	
	/** Map of directions to road graphics, including 0 for the dot when a road leads nowhere */
	private Map<Integer, TileTypeRoadGfx> roadsMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		miniMapColoursMap = new HashMap<Integer, Integer> ();
		for (final TileTypeMiniMap miniMap : getTileTypeMiniMap ())
			miniMapColoursMap.put (miniMap.getPlaneNumber (), Integer.parseInt (miniMap.getMiniMapPixelColour (), 16));
		
		roadsMap = new HashMap<Integer, TileTypeRoadGfx> ();
		for (final TileTypeRoad road : getTileTypeRoad ())
			roadsMap.put (road.getDirection (), (TileTypeRoadGfx) road);
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
	
	/**
	 * @param direction Direction to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Object with either static road image or link to animation
	 * @throws RecordNotFoundException If the road direction doesn't exist
	 */
	public final TileTypeRoadGfx findRoadDirection (final int direction, final String caller) throws RecordNotFoundException
	{
		final TileTypeRoadGfx road = roadsMap.get (direction);
		
		if (road == null)
			throw new RecordNotFoundException (TileTypeRoad.class, direction, caller);
		
		return road;
	}
}