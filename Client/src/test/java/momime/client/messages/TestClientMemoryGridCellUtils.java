package momime.client.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.database.ClientDatabaseEx;
import momime.client.database.MapFeature;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.messages.OverlandMapTerrainData;

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
		// Set up mock database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		// Run test
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		assertFalse (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type has no magic realm defined, and map feature is null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeNo () throws RecordNotFoundException
	{
		// Set up mock database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final TileType tt = new TileType ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);

		// Run test
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		
		assertFalse (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type does have a magic realm defined, and map feature is null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeYes () throws RecordNotFoundException
	{
		// Set up mock database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final TileType tt = new TileType ();
		tt.setMagicRealmID ("X");
		when (db.findTileType ("TT02", "isNodeLairTower")).thenReturn (tt);
		
		// Run test
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT02");

		assertTrue (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature has no magic realm defined, and tile type is null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureNo () throws RecordNotFoundException
	{
		// Set up mock database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MapFeature mf = new MapFeature ();
		when (db.findMapFeature ("MF01", "isNodeLairTower")).thenReturn (mf);

		// Run test
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF01");

		assertFalse (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type is null
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes () throws RecordNotFoundException
	{
		// Set up mock database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);

		final MapFeature mf = new MapFeature ();
		mf.setAnyMagicRealmsDefined (true);
		when (db.findMapFeature ("MF02", "isNodeLairTower")).thenReturn (mf);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF02");

		// Run test
		assertTrue (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type doesn't
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes_WithTileType () throws RecordNotFoundException
	{
		// Set up mock database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);

		final TileType tt = new TileType ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);

		final MapFeature mf = new MapFeature ();
		mf.setAnyMagicRealmsDefined (true);
		when (db.findMapFeature ("MF02", "isNodeLairTower")).thenReturn (mf);
		
		// Run test
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID ("MF02");

		assertTrue (ClientMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}
}