package momime.server.mapgenerator;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.coordinates.MapCoordinates2DEx;

import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MomCombatTile;

/**
 * Bridge between the map storage API and the combat map areas generated from the XSDs
 */
public final class CombatMapArea implements MapArea2D<MomCombatTile>
{
	/** Underlying storage */
	private MapAreaOfCombatTiles area;

	/** Coordinate system of the map area */
	private CoordinateSystem coordinateSystem;
	
	/**
	 * @param x X coordinate of cell to read
	 * @param y Y coordinate of cell to read
	 * @return Value held at this cell
	 */
	@Override
	public final MomCombatTile get (final int x, final int y)
	{
		return getArea ().getRow ().get (y).getCell ().get (x);
	}

	/**
	 * @param x X coordinate of cell to set
	 * @param y Y coordinate of cell to set
	 * @param value Value to write to this cell
	 */
	@Override
	public final void set (final int x, final int y, final MomCombatTile value)
	{
		getArea ().getRow ().get (y).getCell ().set (x, value);
	}
	
	/**
	 * @param coords Coordinates of cell to read
	 * @return Value held at this cell
	 */
	@Override
	public final MomCombatTile get (final MapCoordinates2DEx coords)
	{
		return get (coords.getX (), coords.getY ());
	}

	/**
	 * @param coords Coordinates of cell to set
	 * @param value Value to write to this cell
	 */
	@Override
	public final void set (final MapCoordinates2DEx coords, final MomCombatTile value)
	{
		set (coords.getX (), coords.getY (), value);
	}
	
	/**
	 * @return Underlying storage
	 */
	public final MapAreaOfCombatTiles getArea ()
	{
		return area;
	}

	/**
	 * @param a Underlying storage
	 */
	public final void setArea (final MapAreaOfCombatTiles a)
	{
		area = a;
	}

	/**
	 * @return Coordinate system of the map area
	 */
	@Override
	public final CoordinateSystem getCoordinateSystem ()
	{
		return coordinateSystem;
	}

	/**
	 * @param sys Coordinate system of the map area
	 */
	@Override
	public final void setCoordinateSystem (final CoordinateSystem sys)
	{
		coordinateSystem = sys;
	}
}