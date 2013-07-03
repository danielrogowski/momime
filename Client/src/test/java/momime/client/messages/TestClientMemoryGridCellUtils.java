package momime.client.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import momime.client.ClientTestData;
import momime.client.database.ClientDatabaseEx;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;

import org.junit.Test;

/**
 * Tests the ClientMemoryGridCellUtils class
 */
public final class TestClientMemoryGridCellUtils
{
	/**
	 * Tests the isNodeLairTower method when both the tile type and map feature are both null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_BothNull () throws RecordNotFoundException
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();

		final ClientDatabaseEx db = ClientTestData.createDB ();

		assertFalse (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type has no magic realm defined, and map feature is null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeNo () throws RecordNotFoundException
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");

		final ClientDatabaseEx db = ClientTestData.createDB ();

		assertFalse (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type does have a magic realm defined, and map feature is null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeYes () throws RecordNotFoundException
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT02");

		final ClientDatabaseEx db = ClientTestData.createDB ();

		assertTrue (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature has no magic realm defined, and tile type is null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureNo () throws RecordNotFoundException
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF01");

		final ClientDatabaseEx db = ClientTestData.createDB ();

		assertFalse (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type is null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes () throws RecordNotFoundException
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF02");

		final ClientDatabaseEx db = ClientTestData.createDB ();

		assertTrue (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type doesn't
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes_WithTileType () throws RecordNotFoundException
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID ("MF02");

		final ClientDatabaseEx db = ClientTestData.createDB ();

		assertTrue (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}
}
