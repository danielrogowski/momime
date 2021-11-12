package momime.client.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystemUtilsImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.config.MomImeClientConfig;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.OverlandMapSize;
import momime.common.database.SmoothedTileTypeEx;
import momime.common.database.SmoothingSystemEx;
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
 * Tests the TileSetBitmaskGeneratorImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestTileSetBitmaskGeneratorImpl extends ClientTestData
{
	/**
	 * Tests the generateOverlandMapBitmask method when smoothing is switched off
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateOverlandMapBitmask_NoSmoothing () throws Exception
	{
		// Client config
		final MomImeClientConfig clientConfig = new MomImeClientConfig ();
		
		// Smoothed tile type
		final SmoothedTileTypeEx smoothedTileType = new SmoothedTileTypeEx ();
		
		// Set up object to test
		final TileSetBitmaskGeneratorImpl gen = new TileSetBitmaskGeneratorImpl ();
		gen.setClientConfig (clientConfig);
		
		// Run test
		assertEquals (CommonDatabaseConstants.TILE_BITMASK_NO_SMOOTHING, gen.generateOverlandMapBitmask (smoothedTileType, null, 20, 10, 0));
	}

	/**
	 * Tests the generateOverlandMapBitmask method with maxValueInEachDirection = 0
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateOverlandMapBitmask_MaxValueInEachDirectionZero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final SmoothingSystemEx smoothingSystem = new SmoothingSystemEx ();
		smoothingSystem.setSmoothingSystemID ("SS");
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.getSmoothingSystem ().add (smoothingSystem);
		overlandMapTileSet.buildMaps ();
		
		when (db.findTileSet (eq (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP), anyString ())).thenReturn (overlandMapTileSet);
		
		// Map
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Client config
		final MomImeClientConfig clientConfig = new MomImeClientConfig ();
		clientConfig.setOverlandSmoothTerrain (true);

		// Smoothed tile type
		final SmoothedTileTypeEx smoothedTileType = new SmoothedTileTypeEx ();
		smoothedTileType.setSmoothingSystemID ("SS");
		
		// Set up object to test
		final TileSetBitmaskGeneratorImpl gen = new TileSetBitmaskGeneratorImpl ();
		gen.setClient (client);
		gen.setClientConfig (clientConfig);
		gen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run test
		assertEquals ("00000000", gen.generateOverlandMapBitmask (smoothedTileType, null, 20, 10, 0));
	}

	/**
	 * Tests the generateOverlandMapBitmask method with maxValueInEachDirection = 1 on a river tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateOverlandMapBitmask_MaxValueInEachDirectionOne_River () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final SmoothingSystemEx smoothingSystem = new SmoothingSystemEx ();
		smoothingSystem.setSmoothingSystemID ("SS");
		smoothingSystem.setMaxValueEachDirection (1);
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.getSmoothingSystem ().add (smoothingSystem);
		overlandMapTileSet.buildMaps ();
		
		when (db.findTileSet (eq (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP), anyString ())).thenReturn (overlandMapTileSet);
		
		// Map
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Client config
		final MomImeClientConfig clientConfig = new MomImeClientConfig ();
		clientConfig.setOverlandSmoothTerrain (true);

		// Smoothed tile type
		final SmoothedTileTypeEx smoothedTileType = new SmoothedTileTypeEx ();
		smoothedTileType.setSmoothingSystemID ("SS");
		
		// Set up object to test
		final TileSetBitmaskGeneratorImpl gen = new TileSetBitmaskGeneratorImpl ();
		gen.setClient (client);
		gen.setClientConfig (clientConfig);
		gen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run test
		assertEquals ("10010111", gen.generateOverlandMapBitmask (smoothedTileType, "235", 20, 10, 0));
	}

	/**
	 * Tests the generateOverlandMapBitmask method with maxValueInEachDirection = 1 on a non-river tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateOverlandMapBitmask_MaxValueInEachDirectionOne_NoRiver () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final SmoothingSystemEx smoothingSystem = new SmoothingSystemEx ();
		smoothingSystem.setSmoothingSystemID ("SS");
		smoothingSystem.setMaxValueEachDirection (1);
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.getSmoothingSystem ().add (smoothingSystem);
		overlandMapTileSet.buildMaps ();
		
		when (db.findTileSet (eq (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP), anyString ())).thenReturn (overlandMapTileSet);
		
		// Map
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
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
		
		// Client config
		final MomImeClientConfig clientConfig = new MomImeClientConfig ();
		clientConfig.setOverlandSmoothTerrain (true);

		// Smoothed tile type
		final SmoothedTileTypeEx smoothedTileType = new SmoothedTileTypeEx ();
		smoothedTileType.setSmoothingSystemID ("SS");
		smoothedTileType.setTileTypeID ("TT01");
		smoothedTileType.setSecondaryTileTypeID ("TT02");
		smoothedTileType.setTertiaryTileTypeID ("TT03");
		
		// Some cells which do and don't match the tile type
		final OverlandMapTerrainData d1 = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (9).getCell ().get (20).setTerrainData (d1);
		d1.setTileTypeID ("TT01");
		
		final OverlandMapTerrainData d2 = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (9).getCell ().get (21).setTerrainData (d2);
		d2.setTileTypeID ("TT02");
		
		final OverlandMapTerrainData d3 = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (21).setTerrainData (d3);
		d3.setTileTypeID ("TT03");
		
		final OverlandMapTerrainData d4 = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (11).getCell ().get (21).setTerrainData (d4);
		d4.setTileTypeID ("TT04");
		
		// Set up object to test
		final TileSetBitmaskGeneratorImpl gen = new TileSetBitmaskGeneratorImpl ();
		gen.setClient (client);
		gen.setClientConfig (clientConfig);
		gen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run test
		assertEquals ("00010000", gen.generateOverlandMapBitmask (smoothedTileType, null, 20, 10, 0));
	}

	/**
	 * Tests the generateOverlandMapBitmask method with maxValueInEachDirection = 2
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateOverlandMapBitmask_MaxValueInEachDirectionTwo () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final SmoothingSystemEx smoothingSystem = new SmoothingSystemEx ();
		smoothingSystem.setSmoothingSystemID ("SS");
		smoothingSystem.setMaxValueEachDirection (2);
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.getSmoothingSystem ().add (smoothingSystem);
		overlandMapTileSet.buildMaps ();
		
		when (db.findTileSet (eq (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP), anyString ())).thenReturn (overlandMapTileSet);
		
		// Map
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
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
		
		// Client config
		final MomImeClientConfig clientConfig = new MomImeClientConfig ();
		clientConfig.setOverlandSmoothTerrain (true);

		// Smoothed tile type
		final SmoothedTileTypeEx smoothedTileType = new SmoothedTileTypeEx ();
		smoothedTileType.setSmoothingSystemID ("SS");
		smoothedTileType.setTileTypeID ("TT01");
		smoothedTileType.setSecondaryTileTypeID ("TT02");
		smoothedTileType.setTertiaryTileTypeID ("TT03");
		
		// Some cells which do and don't match the tile type
		final OverlandMapTerrainData d1 = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (9).getCell ().get (20).setTerrainData (d1);
		d1.setTileTypeID ("TT01");
		
		final OverlandMapTerrainData d2 = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (9).getCell ().get (21).setTerrainData (d2);
		d2.setTileTypeID ("TT02");
		
		final OverlandMapTerrainData d3 = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (21).setTerrainData (d3);
		d3.setTileTypeID ("TT03");
		
		final OverlandMapTerrainData d4 = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (11).getCell ().get (21).setTerrainData (d4);
		d4.setTileTypeID ("TT04");
		
		// Set up object to test
		final TileSetBitmaskGeneratorImpl gen = new TileSetBitmaskGeneratorImpl ();
		gen.setClient (client);
		gen.setClientConfig (clientConfig);
		gen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run test
		assertEquals ("00012020", gen.generateOverlandMapBitmask (smoothedTileType, "57", 20, 10, 0));
	}

	/**
	 * Tests the generateCombatMapBitmask method when smoothing is switched off
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateCombatMapBitmask_NoSmoothing () throws Exception
	{
		// Client config
		final MomImeClientConfig clientConfig = new MomImeClientConfig ();
		
		// Smoothed tile type
		final SmoothedTileTypeEx smoothedTileType = new SmoothedTileTypeEx ();
		
		// Set up object to test
		final TileSetBitmaskGeneratorImpl gen = new TileSetBitmaskGeneratorImpl ();
		gen.setClientConfig (clientConfig);
		
		// Run test
		assertEquals (CommonDatabaseConstants.TILE_BITMASK_NO_SMOOTHING, gen.generateCombatMapBitmask (null, smoothedTileType, CombatMapLayerID.TERRAIN, 8, 5));
	}

	/**
	 * Tests the generateCombatMapBitmask method with maxValueInEachDirection = 0
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateCombatMapBitmask_MaxValueInEachDirectionZero () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final SmoothingSystemEx smoothingSystem = new SmoothingSystemEx ();
		smoothingSystem.setSmoothingSystemID ("SS");
		
		final TileSetEx combatMapTileSet = new TileSetEx ();
		combatMapTileSet.getSmoothingSystem ().add (smoothingSystem);
		combatMapTileSet.buildMaps ();
		
		when (db.findTileSet (eq (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP), anyString ())).thenReturn (combatMapTileSet);
		
		// Map
		final CombatMapSize combatMapSize = createCombatMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);

		// Client config
		final MomImeClientConfig clientConfig = new MomImeClientConfig ();
		clientConfig.setCombatSmoothTerrain (true);
		
		// Smoothed tile type
		final SmoothedTileTypeEx smoothedTileType = new SmoothedTileTypeEx ();
		smoothedTileType.setSmoothingSystemID ("SS");
		
		// Set up object to test
		final TileSetBitmaskGeneratorImpl gen = new TileSetBitmaskGeneratorImpl ();
		gen.setClient (client);
		gen.setClientConfig (clientConfig);
		gen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run test
		assertEquals ("00000000", gen.generateCombatMapBitmask (null, smoothedTileType, CombatMapLayerID.TERRAIN, 8, 5));
	}

	/**
	 * Tests the generateCombatMapBitmask method with maxValueInEachDirection = 1
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateCombatMapBitmask_MaxValueInEachDirectionOne () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final SmoothingSystemEx smoothingSystem = new SmoothingSystemEx ();
		smoothingSystem.setSmoothingSystemID ("SS");
		smoothingSystem.setMaxValueEachDirection (1);
		
		final TileSetEx combatMapTileSet = new TileSetEx ();
		combatMapTileSet.getSmoothingSystem ().add (smoothingSystem);
		combatMapTileSet.buildMaps ();
		
		when (db.findTileSet (eq (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP), anyString ())).thenReturn (combatMapTileSet);
		
		// Map
		final CombatMapSize combatMapSize = createCombatMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);

		final MapAreaOfCombatTiles combatTerrain = createCombatMap (combatMapSize);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);

		// Client config
		final MomImeClientConfig clientConfig = new MomImeClientConfig ();
		clientConfig.setCombatSmoothTerrain (true);
		
		// Smoothed tile type
		final SmoothedTileTypeEx smoothedTileType = new SmoothedTileTypeEx ();
		smoothedTileType.setSmoothingSystemID ("SS");
		smoothedTileType.setCombatTileTypeID ("TT01");

		// Some cells which do and don't match the tile type
		// Diamond maps are complicated - the diamond up is y - 2 and the diamond below is y + 2
		final MomCombatTileLayer d1 = new MomCombatTileLayer ();
		d1.setLayer (CombatMapLayerID.TERRAIN);
		d1.setCombatTileTypeID ("TT01");
		combatTerrain.getRow ().get (3).getCell ().get (8).getTileLayer ().add (d1);

		final MomCombatTileLayer d2 = new MomCombatTileLayer ();
		d2.setLayer (CombatMapLayerID.TERRAIN);
		d2.setCombatTileTypeID ("TT02");
		combatTerrain.getRow ().get (7).getCell ().get (8).getTileLayer ().add (d2);
		
		// Set up object to test
		final TileSetBitmaskGeneratorImpl gen = new TileSetBitmaskGeneratorImpl ();
		gen.setClient (client);
		gen.setClientConfig (clientConfig);
		gen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		gen.setCombatMapUtils (new CombatMapUtilsImpl ());
		
		// Run test
		assertEquals ("00001000", gen.generateCombatMapBitmask (combatTerrain, smoothedTileType, CombatMapLayerID.TERRAIN, 8, 5));
	}
}