package momime.client.database;

import momime.client.database.v0_9_4.ClientDatabase;
import momime.client.database.v0_9_4.MapFeature;
import momime.common.database.v0_9_4.TileType;

/**
 * Since the tests in the common project can't use the XML file (since the classes generated from the server XSD that allow
 * JAXB to load the server XML are server side only) yet many of the client-side tests need it, this manufactures pieces
 * of test data that are used by more than one test
 */
public final class GenerateTestData
{
	/**
	 * @return Selected data required by the tests
	 */
	public final static ClientDatabaseLookup createDB ()
	{
		final ClientDatabase db = new ClientDatabase ();

		// Tile types
		final TileType tileTypeWithoutMagicRealm = new TileType ();
		tileTypeWithoutMagicRealm.setTileTypeID ("TT01");
		db.getTileType ().add (tileTypeWithoutMagicRealm);

		final TileType tileTypeWithMagicRealm = new TileType ();
		tileTypeWithMagicRealm.setTileTypeID ("TT02");
		tileTypeWithMagicRealm.setMagicRealmID ("MB01");
		db.getTileType ().add (tileTypeWithMagicRealm);

		// Map features
		final MapFeature mapFeatureWithoutRealm = new MapFeature ();
		mapFeatureWithoutRealm.setMapFeatureID ("MF01");
		db.getMapFeature ().add (mapFeatureWithoutRealm);

		final MapFeature mapFeatureWithRealm = new MapFeature ();
		mapFeatureWithRealm.setMapFeatureID ("MF02");
		mapFeatureWithRealm.setAnyMagicRealmsDefined (true);
		db.getMapFeature ().add (mapFeatureWithRealm);

		return new ClientDatabaseLookup (db);
	}

	/**
	 * Prevent instantiation
	 */
	private GenerateTestData ()
	{
	}
}
