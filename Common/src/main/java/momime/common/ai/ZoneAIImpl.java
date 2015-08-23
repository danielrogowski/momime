package momime.common.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.operations.BooleanMapAreaOperations2D;
import com.ndg.map.areas.operations.BooleanMapAreaOperations3D;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.areas.storage.MapArea3Dto2D;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.OverlandMapCityData;

/**
 * Calculates zones / national borders from what a player knows about the overland map.
 * This is principally used for the AI on the server, but is in the common project so that the client can
 * have a tickbox on the options screen to display their own border - which is mostly for purposes
 * of testing that this works as intended.
 */
public final class ZoneAIImpl implements ZoneAI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ZoneAIImpl.class);
	
	/** Boolean operations for 2D maps */
	private BooleanMapAreaOperations2D booleanMapAreaOperations2D;

	/** Boolean operations for 3D maps */
	private BooleanMapAreaOperations3D booleanMapAreaOperations3D;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/**
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param playerID Player whose border we want to calculate
	 * @param separation How close our cities have to be to consider the area between them to be probably passable without getting attacked
	 * @param db Lookup lists built over the XML database
	 * @return 3D area marked with all the locations we consider to be our territory
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the db
	 */
	@Override
	public final MapArea3D<Boolean> calculateFriendlyZone (final FogOfWarMemory fogOfWarMemory,
		final CoordinateSystem overlandMapCoordinateSystem, final int playerID, final int separation, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering calculateFriendlyZone: Player ID " + playerID + ", " + separation);

		// Make a list of all of our cities
		final List<MapCoordinates3DEx> cityLocations = new ArrayList<MapCoordinates3DEx> ();
		for (int plane = 0; plane < overlandMapCoordinateSystem.getDepth (); plane++)
			for (int y = 0; y < overlandMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < overlandMapCoordinateSystem.getWidth (); x++)
				{
					final OverlandMapCityData cityData = fogOfWarMemory.getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == playerID))
						cityLocations.add (new MapCoordinates3DEx (x, y, plane));
				}
		
		// Create output area
		final MapArea3D<Boolean> zone = new MapArea3DArrayListImpl<Boolean> ();
		zone.setCoordinateSystem (overlandMapCoordinateSystem);
		getBooleanMapAreaOperations3D ().deselectAll (zone);
		
		// Mark the regular resource area of every city,
		// draw a line between each 2 cities that are close to each other, and
		// draw a triangle between each 3 cities that are close to each other
		for (int index1 = 0; index1 < cityLocations.size (); index1++)
		{
			final MapCoordinates3DEx cityLocation1 = cityLocations.get (index1);
			
			// Mark this city
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation1);
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ()))
					zone.set (coords, true);
			
			for (int index2 = index1 + 1; index2 < cityLocations.size (); index2++)
			{
				final MapCoordinates3DEx cityLocation2 = cityLocations.get (index2);
				
				if ((cityLocation1.getZ () == cityLocation2.getZ ()) &&
					(getCoordinateSystemUtils ().findDistanceBetweenXCoordinates (overlandMapCoordinateSystem, cityLocation1.getX (), cityLocation2.getX ()) <= separation) &&
					(getCoordinateSystemUtils ().findDistanceBetweenYCoordinates (overlandMapCoordinateSystem, cityLocation1.getY (), cityLocation2.getY ()) <= separation))
				{
					// Draw line between 2 cities
					final MapArea3Dto2D<Boolean> zone2D = new MapArea3Dto2D<Boolean> ();
					zone2D.setStorage (zone);
					zone2D.setZ (cityLocation1.getZ ());
					
					getBooleanMapAreaOperations2D ().setLine (zone2D,
						new MapCoordinates2DEx (cityLocation1.getX (), cityLocation1.getY ()),
						new MapCoordinates2DEx (cityLocation2.getX (), cityLocation2.getY ()), 1, true);
					
					for (int index3 = index2 + 1; index3 < cityLocations.size (); index3++)
					{
						final MapCoordinates3DEx cityLocation3 = cityLocations.get (index3);
						
						if ((cityLocation1.getZ () == cityLocation3.getZ ()) &&
							(getCoordinateSystemUtils ().findDistanceBetweenXCoordinates (overlandMapCoordinateSystem, cityLocation1.getX (), cityLocation3.getX ()) <= separation) &&
							(getCoordinateSystemUtils ().findDistanceBetweenYCoordinates (overlandMapCoordinateSystem, cityLocation1.getY (), cityLocation3.getY ()) <= separation) &&
							(getCoordinateSystemUtils ().findDistanceBetweenXCoordinates (overlandMapCoordinateSystem, cityLocation2.getX (), cityLocation3.getX ()) <= separation) &&
							(getCoordinateSystemUtils ().findDistanceBetweenYCoordinates (overlandMapCoordinateSystem, cityLocation2.getY (), cityLocation3.getY ()) <= separation))
						{
							// Draw triangle between 3 cities.
							// In testing this I found this was pretty much never necessary - the lines between each pair plus the city radius itself almost
							// always covers up all the area anyway, even without the triangle.  But do it just to be on the safe side.
							final List<MapCoordinates2DEx> triangle = new ArrayList<MapCoordinates2DEx> ();
							triangle.add (new MapCoordinates2DEx (cityLocation1.getX (), cityLocation1.getY ()));
							triangle.add (new MapCoordinates2DEx (cityLocation2.getX (), cityLocation2.getY ()));
							triangle.add (new MapCoordinates2DEx (cityLocation3.getX (), cityLocation3.getY ()));
							
							getBooleanMapAreaOperations2D ().setPolygon (zone2D, triangle, true);
						}
					}
				}
			}
		}
		
		// Remove any area close to enemy cities, or on water tiles
		for (int plane = 0; plane < overlandMapCoordinateSystem.getDepth (); plane++)
			for (int y = 0; y < overlandMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < overlandMapCoordinateSystem.getWidth (); x++)
				{
					final MemoryGridCell mc = fogOfWarMemory.getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x);
					final OverlandMapCityData cityData = mc.getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () != playerID))
					{
						final MapArea3Dto2D<Boolean> zone2D = new MapArea3Dto2D<Boolean> ();
						zone2D.setStorage (zone);
						zone2D.setZ (plane);

						getBooleanMapAreaOperations2D ().deselectRadius (zone2D, x, y, 1);
					}
					else if ((mc.getTerrainData () != null) && (mc.getTerrainData ().getTileTypeID () != null))
					{
						final TileType tileType = db.findTileType (mc.getTerrainData ().getTileTypeID (), "calculateFriendlyZone");
						if ((tileType.isLand () != null) && (!tileType.isLand ()))
							zone.set (x, y, plane, false);
					}
				}
		
		// Remove any area close to enemy units
		// Taking this out - or how will the AI detect that its being invaded if their border automatically shrinks as enemy units get close? :)
		/*
		for (final MemoryUnit unit : fogOfWarMemory.getUnit ())
			if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getOwningPlayerID () != playerID) && (unit.getUnitLocation () != null))
			{
				final MapArea3Dto2D<Boolean> zone2D = new MapArea3Dto2D<Boolean> ();
				zone2D.setStorage (zone);
				zone2D.setZ (unit.getUnitLocation ().getZ ());
				
				getBooleanMapAreaOperations2D ().deselectRadius (zone2D, unit.getUnitLocation ().getX (), unit.getUnitLocation ().getY (), 1);
			} */
		
		// Reselect our cities, in case those cells were deselected by an enemy standing next to one of them
		for (final MapCoordinates3DEx cityLocation : cityLocations)
			zone.set (cityLocation, true);

		log.trace ("Exiting calculateFriendlyZone = " + zone);
		return zone;
	}

	/**
	 * @return Boolean operations for 2D maps
	 */
	public final BooleanMapAreaOperations2D getBooleanMapAreaOperations2D ()
	{
		return booleanMapAreaOperations2D;
	}

	/**
	 * @param op Boolean operations for 2D maps
	 */
	public final void setBooleanMapAreaOperations2D (final BooleanMapAreaOperations2D op)
	{
		booleanMapAreaOperations2D = op;
	}
	
	/**
	 * @return Boolean operations for 3D maps
	 */
	public final BooleanMapAreaOperations3D getBooleanMapAreaOperations3D ()
	{
		return booleanMapAreaOperations3D;
	}

	/**
	 * @param op Boolean operations for 3D maps
	 */
	public final void setBooleanMapAreaOperations3D (final BooleanMapAreaOperations3D op)
	{
		booleanMapAreaOperations3D = op;
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