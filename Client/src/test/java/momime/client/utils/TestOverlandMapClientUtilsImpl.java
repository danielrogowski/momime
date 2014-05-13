package momime.client.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import momime.client.ClientTestData;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.OverlandMapTerrainData;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Tests the OverlandMapClientUtilsImpl class
 */
public final class TestOverlandMapClientUtilsImpl
{
	/**
	 * Tests the findAdjacentTileType method where the centre tile matches
	 */
	@Test
	public final void testFindAdjacentTileType_Centre ()
	{
		// Set up sample map
		final CoordinateSystem sys = ClientTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (sys);
		
		final OverlandMapTerrainData centre = new OverlandMapTerrainData ();
		centre.setTileTypeID ("TT02");
		terrain.getPlane ().get (1).getRow ().get (15).getCell ().get (20).setTerrainData (centre);
		
		// Set up object to test
		final OverlandMapClientUtilsImpl utils = new OverlandMapClientUtilsImpl ();
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (20, 15, 1); 
		
		assertTrue (utils.findAdjacentTileType (terrain, coords, sys, "TT02"));
	}

	/**
	 * Tests the findAdjacentTileType method where an adjacent tile matches
	 */
	@Test
	public final void testFindAdjacentTileType_Adjacent ()
	{
		// Set up sample map
		final CoordinateSystem sys = ClientTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (sys);
		
		final OverlandMapTerrainData centre = new OverlandMapTerrainData ();
		centre.setTileTypeID ("TT02");
		terrain.getPlane ().get (1).getRow ().get (15).getCell ().get (20).setTerrainData (centre);

		// Direction 1 has a null OverlandMapTerrainData
		// Directtion 2 has a OverlandMapTerrainData with a null typeTileID
		terrain.getPlane ().get (1).getRow ().get (14).getCell ().get (21).setTerrainData (new OverlandMapTerrainData ());
		
		// One that actually matches, in direction 3
		final OverlandMapTerrainData adjacent = new OverlandMapTerrainData ();
		adjacent.setTileTypeID ("TT03");
		terrain.getPlane ().get (1).getRow ().get (15).getCell ().get (21).setTerrainData (adjacent);
		
		// Set up object to test
		final OverlandMapClientUtilsImpl utils = new OverlandMapClientUtilsImpl ();
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (20, 15, 1); 
		
		assertTrue (utils.findAdjacentTileType (terrain, coords, sys, "TT03"));
	}

	/**
	 * Tests the findAdjacentTileType method where no tile matches
	 */
	@Test
	public final void testFindAdjacentTileType_NoMatch ()
	{
		// Set up sample map
		final CoordinateSystem sys = ClientTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (sys);
		
		final OverlandMapTerrainData centre = new OverlandMapTerrainData ();
		centre.setTileTypeID ("TT02");
		terrain.getPlane ().get (1).getRow ().get (15).getCell ().get (20).setTerrainData (centre);

		// Direction 1 has a null OverlandMapTerrainData
		// Directtion 2 has a OverlandMapTerrainData with a null typeTileID
		terrain.getPlane ().get (1).getRow ().get (14).getCell ().get (21).setTerrainData (new OverlandMapTerrainData ());
		
		// One that actually matches, in direction 3
		final OverlandMapTerrainData adjacent = new OverlandMapTerrainData ();
		adjacent.setTileTypeID ("TT03");
		terrain.getPlane ().get (1).getRow ().get (15).getCell ().get (21).setTerrainData (adjacent);
		
		// Set up object to test
		final OverlandMapClientUtilsImpl utils = new OverlandMapClientUtilsImpl ();
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (20, 15, 1); 
		
		assertFalse (utils.findAdjacentTileType (terrain, coords, sys, "TT04"));
	}
}
