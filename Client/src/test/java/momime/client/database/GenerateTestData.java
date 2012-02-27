package momime.client.database;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import momime.client.database.v0_9_4.ClientDatabase;
import momime.client.database.v0_9_4.MapFeature;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.language.database.LanguageDatabaseConstants;
import momime.common.database.v0_9_4.TileType;

/**
 * Since the tests in the common project can't use the XML file (since the classes generated from the server XSD that allow
 * JAXB to load the server XML are server side only) yet many of the client-side tests need it, this manufactures pieces
 * of test data that are used by more than one test
 */
public final class GenerateTestData
{
	/**
	 * @return Location of "Original Master of Magic 1.31 rules.Master of Magic Server.xml" to test with
	 * @throws IOException If we are unable to locate the server XML file
	 */
	public final static File locateServerXmlFile () throws IOException
	{
		// Not straightforward to find this, because its in src/external/resources so isn't on the classpath

		// Moreover, if we search for something that is on the classpath of the MoMIMEServerDatabase project, and run this test as part of
		// a maven command line build, we get a URL back of the form jar:file:<maven repository>MoMIMEServerDatabase.jar!/momime.server.database/MoMIMEServerDatabase.xsd

		// We can't alter that URL to locate the server XML file within the JAR, simply because the server XML is intentionally not in the JAR or anywhere in Maven at all

		// So only way to do this is locate some resource in *this* project, and modify the location from there
		// This makes the assumption that the MoMIMEServerDatabase project is called as such and hasn't been checked out under a different name
		final URL languageXsd = new Object ().getClass ().getResource (LanguageDatabaseConstants.LANGUAGE_XSD_LOCATION);
		final File languageXsdFile = new File (languageXsd.getFile ());
		final File serverXmlFile = new File (languageXsdFile, "../../../../MoMIMEServerDatabase/src/external/resources/momime.server.database/Original Master of Magic 1.31 rules.Master of Magic Server.xml");

		return serverXmlFile.getCanonicalFile ();
	}

	/**
	 * @return Location of "English.Master of Magic Language.xml" to test with
	 * @throws IOException If we are unable to locate the English XML file
	 */
	public final static File locateEnglishXmlFile () throws IOException
	{
		// Not straightforward to find this, because its in src/external/resources so isn't on the classpath
		// So instead find something that is on the classpath of the MoMIMEClient project, then modify that location
		final URL languageXSD = new Object ().getClass ().getResource (LanguageDatabaseConstants.LANGUAGE_XSD_LOCATION);
		final File languageFile = new File (languageXSD.getFile ());
		final File englishXmlFile = new File (languageFile, "../../../src/external/resources/momime.client.language.database/English.Master of Magic Language.xml");

		return englishXmlFile.getCanonicalFile ();
	}

	/**
	 * @return Location of "Default.Master of Magic Graphics.xml" to test with
	 * @throws IOException If we are unable to locate the default graphics XML file
	 */
	public final static File locateDefaultGraphicsXmlFile () throws IOException
	{
		// Not straightforward to find this, because its in src/external/resources so isn't on the classpath
		// So instead find something that is on the classpath of the MoMIMEClient project, then modify that location
		final URL graphicsXSD = new Object ().getClass ().getResource (GraphicsDatabaseConstants.GRAPHICS_XSD_LOCATION);
		final File graphicsFile = new File (graphicsXSD.getFile ());
		final File graphicsXmlFile = new File (graphicsFile, "../../../src/external/resources/momime.client.graphics.database/Default.Master of Magic Graphics.xml");

		return graphicsXmlFile.getCanonicalFile ();
	}

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
