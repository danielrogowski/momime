package momime.server.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;

import org.junit.Test;

/**
 * Tests the ClientMemoryGridCellUtils class
 */
public final class TestServerMemoryGridCellUtils
{
	/**
	 * Tests the isNodeLairTower method when both the whole terrain data is null
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsNodeLairTower_TerrainDataNull () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (null, db));
	}

	/**
	 * Tests the isNodeLairTower method when both the terrain data exists, but has tile type and map feature both null
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsNodeLairTower_BothNull () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type has no magic realm defined, and map feature is null
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeNo () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_MOUNTAIN);

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type does have a magic realm defined, and map feature is null
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeYes () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature has no magic realm defined, and tile type is null
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureNo () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF01");

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type is null
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF12A");

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type doesn't
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes_WithTileType () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_MOUNTAIN);
		terrainData.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}
}