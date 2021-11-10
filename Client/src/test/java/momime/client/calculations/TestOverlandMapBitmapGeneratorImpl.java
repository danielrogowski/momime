package momime.client.calculations;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ndg.random.RandomUtils;
import com.ndg.random.RandomUtilsImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.OverlandMapSize;
import momime.common.database.SmoothedTile;
import momime.common.database.SmoothedTileTypeEx;
import momime.common.database.TileSetEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;

/**
 * Tests the OverlandMapBitmapGeneratorImpl class
 */
public final class TestOverlandMapBitmapGeneratorImpl extends ClientTestData
{
	/**
	 * @param bitmask Bitmask to test with
	 * @return Map needed for SmoothedTileTypeEx buildMaps
	 */
	private final Map<String, List<String>> createIdentityBitmaskMapping (final String bitmask)
	{
		final Map<String, List<String>> map = new HashMap<String, List<String>> ();
		map.put (bitmask, Arrays.asList (bitmask));
		return map;
	}
	
	/**
	 * Tests the smoothMapTerrain method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSmoothMapTerrain () throws Exception
	{
		final RandomUtils random = new RandomUtilsImpl ();
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		when (db.findTileSet (eq (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP), anyString ())).thenReturn (overlandMapTileSet);

		final SmoothedTileTypeEx tileType1 = new SmoothedTileTypeEx ();
		tileType1.setTileTypeID ("TT01");
		tileType1.setRandomUtils (random);
		overlandMapTileSet.getSmoothedTileType ().add (tileType1);
		
		final SmoothedTile bitmask1 = new SmoothedTile ();
		bitmask1.setBitmask ("BM01");
		tileType1.getSmoothedTile ().add (bitmask1);
		tileType1.buildMap (createIdentityBitmaskMapping ("BM01"));
		
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Map
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Bitmask generator
		final TileSetBitmaskGenerator bitmaskGenerator = mock (TileSetBitmaskGenerator.class);
		
		// Map cells
		final OverlandMapTerrainData c1 = new OverlandMapTerrainData ();
		c1.setTileTypeID ("TT01");
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (c1);
		
		when (bitmaskGenerator.generateOverlandMapBitmask (tileType1, null, 20, 10, 0)).thenReturn ("BM01");
		
		// Set up object to test
		final OverlandMapBitmapGeneratorImpl gen = new OverlandMapBitmapGeneratorImpl ();
		gen.setClient (client);
		gen.setTileSetBitmaskGenerator (bitmaskGenerator);
		
		// Run methods
		gen.afterJoinedSession ();		// This just initializes the output area
		gen.smoothMapTerrain (null);
		
		// Check results
		assertNull (gen.getSmoothedTiles () [0] [10] [21]);
		assertSame (bitmask1, gen.getSmoothedTiles () [0] [10] [20]);
	}
}