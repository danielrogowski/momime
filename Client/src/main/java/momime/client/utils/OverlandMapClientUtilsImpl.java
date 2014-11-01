package momime.client.utils;

import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.OverlandMapTerrainData;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Client side only helper methods for dealing with the overland map
 */
public final class OverlandMapClientUtilsImpl implements OverlandMapClientUtils
{
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
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
	@Override
	public final boolean findAdjacentTileType (final MapVolumeOfMemoryGridCells terrain, final MapCoordinates3DEx coords,
		final CoordinateSystem overlandMapCoordinateSystem, final String tileTypeID)
	{
		// Deal with centre square
		final OverlandMapTerrainData centreTerrain = terrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
		boolean found = (centreTerrain == null) ? false : (tileTypeID.equals (centreTerrain.getTileTypeID ()));
		
		int d = 1;
		while ((!found) & (d <= getCoordinateSystemUtils ().getMaxDirection (overlandMapCoordinateSystem.getCoordinateSystemType ())))
		{
			final MapCoordinates3DEx newCoords = new MapCoordinates3DEx (coords);
			if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, newCoords, d))
			{
				final OverlandMapTerrainData ringTerrain = terrain.getPlane ().get (newCoords.getZ ()).getRow ().get (newCoords.getY ()).getCell ().get (newCoords.getX ()).getTerrainData ();
				if ((ringTerrain != null) && (tileTypeID.equals (ringTerrain.getTileTypeID ())))
					found = true;
		
			}
			
			d++;
		}
		
		
		return found;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}
}