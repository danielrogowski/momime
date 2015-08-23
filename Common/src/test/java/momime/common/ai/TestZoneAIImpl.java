package momime.common.ai;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.areas.operations.BooleanMapAreaOperations2DImpl;
import com.ndg.map.areas.operations.BooleanMapAreaOperations3DImpl;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CommonDatabase;
import momime.common.database.GenerateTestData;
import momime.common.database.TileType;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;

/**
 * Tests the ZoneAIImpl class
 */
public final class TestZoneAIImpl
{
	/** Locations of our cities to test with */
	private static final MapCoordinates3DEx [] OUR_CITY_LOCATIONS = new MapCoordinates3DEx []
	{
		new MapCoordinates3DEx (14, 8, 0), new MapCoordinates3DEx (20, 9, 0), new MapCoordinates3DEx (17, 14, 0),
		new MapCoordinates3DEx (26, 13, 0), new MapCoordinates3DEx (25, 18, 0),
		
		new MapCoordinates3DEx (50, 8, 1), new MapCoordinates3DEx (56, 10, 1), new MapCoordinates3DEx (2, 12, 1),
		new MapCoordinates3DEx (59, 16, 1)
	};
	
	/**
	 * Tests the calculateFriendlyZone method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateFriendlyZone () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileType grass = new TileType ();
		grass.setLand (true);
		when (db.findTileType ("TT01", "calculateFriendlyZone")).thenReturn (grass);

		final TileType water = new TileType ();
		water.setLand (false);
		when (db.findTileType ("TT02", "calculateFriendlyZone")).thenReturn (water);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		fogOfWarMemory.setMap (terrain);
		
		// Set all to grass
		for (final MapAreaOfMemoryGridCells plane : terrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell mc : row.getCell ())
				{
					final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
					terrainData.setTileTypeID ("TT01");
					mc.setTerrainData (terrainData);
				}
		
		// Our cities
		for (final MapCoordinates3DEx coords : OUR_CITY_LOCATIONS)
		{
			final OverlandMapCityData cityData = new OverlandMapCityData ();
			cityData.setCityOwnerID (1);
			terrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).setCityData (cityData);
		}
		
		// Enemy city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		terrain.getPlane ().get (0).getRow ().get (20).getCell ().get (27).setCityData (cityData);
		
		// Enemy unit
		final MemoryUnit enemy = new MemoryUnit ();
		enemy.setOwningPlayerID (2);
		enemy.setUnitLocation (new MapCoordinates3DEx (17, 13, 0));
		enemy.setStatus (UnitStatusID.ALIVE);
		fogOfWarMemory.getUnit ().add (enemy);
		
		// Water tile
		terrain.getPlane ().get (0).getRow ().get (8).getCell ().get (13).getTerrainData ().setTileTypeID ("TT02");
		
		// Unknown tile
		terrain.getPlane ().get (0).getRow ().get (8).getCell ().get (12).setTerrainData (null);
		
		// Set up object to test
		final CoordinateSystemUtilsImpl utils = new CoordinateSystemUtilsImpl ();
		
		final BooleanMapAreaOperations2DImpl op2d = new BooleanMapAreaOperations2DImpl ();
		op2d.setCoordinateSystemUtils (utils);

		final BooleanMapAreaOperations3DImpl op3d = new BooleanMapAreaOperations3DImpl ();
		op3d.setCoordinateSystemUtils (utils);		
		
		final ZoneAIImpl ai = new ZoneAIImpl ();
		ai.setCoordinateSystemUtils (utils);
		ai.setBooleanMapAreaOperations2D (op2d);
		ai.setBooleanMapAreaOperations3D (op3d);
		
		// Run method
		final MapArea3D<Boolean> zone = ai.calculateFriendlyZone (fogOfWarMemory, sys, 1, 6, db);
		
		// Check results
		for (int plane = 0; plane < sys.getDepth (); plane++)
		{
			final BufferedImage image = ImageIO.read (getClass ().getResource ("/momime.common.ai.calculateFriendlyZone/Plane-" + plane + ".gif"));
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
					
					// Can't use Color.BLACK.getRGB() since the alpha components don't match
					assertEquals (image.getRGB (x, y) != 0, zone.get (new MapCoordinates3DEx (x, y, plane)));
		}
	}
}