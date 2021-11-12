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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.random.RandomUtils;
import com.ndg.random.RandomUtilsImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.database.OverlandMapSize;
import momime.common.database.SmoothedTile;
import momime.common.database.SmoothedTileTypeEx;
import momime.common.database.TileSetEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.CombatMapUtilsImpl;

/**
 * Tests the CombatMapBitmapGeneratorImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCombatMapBitmapGeneratorImpl extends ClientTestData
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
		
		final TileSetEx combatMapTileSet = new TileSetEx ();
		when (db.findTileSet (eq (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP), anyString ())).thenReturn (combatMapTileSet);
		
		final SmoothedTileTypeEx tileType1 = new SmoothedTileTypeEx ();
		tileType1.setTileTypeID ("TT01");
		tileType1.setCombatTileTypeID ("CBT01");
		tileType1.setRandomUtils (random);
		combatMapTileSet.getSmoothedTileType ().add (tileType1);
		
		final SmoothedTile bitmask1 = new SmoothedTile ();
		bitmask1.setBitmask ("BM01");
		tileType1.getSmoothedTile ().add (bitmask1);
		tileType1.buildMap (createIdentityBitmaskMapping ("BM01"));
		
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		final CombatMapSize combatMapSize = createCombatMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setCombatMapSize (combatMapSize);
		
		// Maps
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);
		final MapAreaOfCombatTiles combatTerrain = createCombatMap (combatMapSize);
		
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
		
		// Overland tile type where the combat is
		final OverlandMapTerrainData overlandMapTerrainData = new OverlandMapTerrainData ();
		overlandMapTerrainData.setTileTypeID ("TT01");
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (overlandMapTerrainData);
		
		// Combat map tile
		final MomCombatTileLayer combatTile = new MomCombatTileLayer ();
		combatTile.setLayer (CombatMapLayerID.TERRAIN);
		combatTile.setCombatTileTypeID ("CBT01");
		combatTerrain.getRow ().get (4).getCell ().get (8).getTileLayer ().add (combatTile);
		
		when (bitmaskGenerator.generateCombatMapBitmask (combatTerrain, tileType1, CombatMapLayerID.TERRAIN, 8, 4)).thenReturn ("BM01");
		
		// Set up object to test
		final CombatMapBitmapGeneratorImpl gen = new CombatMapBitmapGeneratorImpl ();
		gen.setClient (client);
		gen.setTileSetBitmaskGenerator (bitmaskGenerator);
		gen.setCombatMapUtils (new CombatMapUtilsImpl ());
		
		// Run methods
		gen.afterJoinedSession ();
		gen.smoothMapTerrain (new MapCoordinates3DEx (20, 10, 0), combatTerrain);
		
		// Check results
		assertNull (gen.getSmoothedTiles ().get (CombatMapLayerID.TERRAIN)  [4] [9]);
		assertSame (bitmask1, gen.getSmoothedTiles ().get (CombatMapLayerID.TERRAIN)  [4] [8]);

		assertNull (gen.getSmoothedTileTypes ().get (CombatMapLayerID.TERRAIN)  [4] [9]);
		assertSame (tileType1, gen.getSmoothedTileTypes ().get (CombatMapLayerID.TERRAIN)  [4] [8]);
	}
}