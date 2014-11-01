package momime.client.utils;

import momime.common.messages.MapVolumeOfMemoryGridCells;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Client side only helper methods for dealing with the overland map
 */
public interface OverlandMapClientUtils
{
	/**
	 * Similar to MomCityCalculations.buildingPassesTileTypeRequirements () except that searches for a
	 * specified tile type rather than the set of tile types specified under a building.
	 * 
	 * This is only used on the client for deciding whether to draw e.g. river or ocean on the city view.
	 * 
	 * It CAN be used on enemy cities, where we might not have accurate info, but in that case its perfectly correct to use
	 * the memory map rather than true data, since by looking at the city screen of such enemy cities, we're looking
	 * at what we remember, not the true state of the city now.
	 * 
	 * @param terrain Known terrain
	 * @param coords Coordinates to check around
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param tileTypeID Tile type ID to look for
	 * @return True if the terrain at coords, or in one of the 8 adjacent tiles, is tileTypeID
	 */
	public boolean findAdjacentTileType (final MapVolumeOfMemoryGridCells terrain, final MapCoordinates3DEx coords,
		final CoordinateSystem overlandMapCoordinateSystem, final String tileTypeID);
}