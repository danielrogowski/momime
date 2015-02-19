package momime.server.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import momime.client.database.AvailableDatabase;
import momime.client.database.ClientDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.NewGameDatabaseMessage;
import momime.server.ServerTestData;
import momime.server.database.v0_9_6.ServerDatabase;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * Tests the ServerDatabaseConverters class
 */
public final class TestServerDatabaseConvertersImpl
{
	/**
	 * Tests the buildNewGameDatabase method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildNewGameDatabase () throws Exception
	{
		// Read XSD
		final URL xsdResource = getClass ().getResource (ServerDatabaseConstants.SERVER_XSD_LOCATION);
		assertNotNull ("MoM IME Server XSD could not be found on classpath", xsdResource);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));

		final Schema xsd = schemaFactory.newSchema (xsdResource);
		
		// Locate XML
		final File serverXml = ServerTestData.locateServerXmlFile ();
		assertNotNull ("MoM IME Server XML could not be found", serverXml);
		
		final Map<String, File> map = new HashMap<String, File> ();
		map.put ("Original Master of Magic 1.31 rules", serverXml);
		
		// Set up object to test
		final ServerDatabaseConvertersImpl conv = new ServerDatabaseConvertersImpl ();

		// Build it
		// Locate the server XML file, then go one level up to the folder that it is in
		final Unmarshaller serverDatabaseUnmarshaller = JAXBContext.newInstance (ServerDatabase.class).createUnmarshaller ();
		serverDatabaseUnmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {new ServerDatabaseFactory ()});
		serverDatabaseUnmarshaller.setSchema (xsd);
		
		final NewGameDatabaseMessage msg = conv.buildNewGameDatabase (map, serverDatabaseUnmarshaller);
		assertEquals (1, msg.getNewGameDatabase ().getMomimeXmlDatabase ().size ());

		final AvailableDatabase db = msg.getNewGameDatabase ().getMomimeXmlDatabase ().get (0);
		assertEquals ("Original Master of Magic 1.31 rules", db.getDbName ());
		assertEquals ("Failed to load correct number of map sizes",				5, db.getMapSize ().size ());
		assertEquals ("Failed to load correct number of land proportions",		3, db.getLandProportion ().size ());
		assertEquals ("Failed to load correct number of node strengths",		3, db.getNodeStrength ().size ());
		assertEquals ("Failed to load correct number of difficulty levels",			5, db.getDifficultyLevel ().size ());
		assertEquals ("Failed to load correct number of fog of war settings",	2, db.getFogOfWarSetting ().size ());
		assertEquals ("Failed to load correct number of unit settings",			2, db.getUnitSetting ().size ());
		assertEquals ("Failed to load correct number of spell settings",			2, db.getSpellSetting ().size ());

		// This tests not only that the objects are assembled correctly, but that they are marshalled correctly when we send the message - as common not server versions,
		// i.e. that the generated XML does not contain any descriptions, which are server-only
		@SuppressWarnings ("resource")
		final ApplicationContext applicationContext = new ClassPathXmlApplicationContext ("/momime-common-test-beans.xml");
		
		final JAXBContext serverToClientJaxbContext  = (JAXBContext) applicationContext.getBean ("serverToClientJaxbContext");
		final Marshaller marshaller = serverToClientJaxbContext.createMarshaller ();

		final ByteArrayOutputStream stream = new ByteArrayOutputStream ();
		marshaller.marshal (msg, stream);
		final String newGameDB = new String (stream.toByteArray ());
		assertFalse (newGameDB.contains ("escription"));
	}

	/**
	 * Tests the buildClientDatabase method on valid numbers of spell picks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildClientDatabase_Valid () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final ServerDatabaseConvertersImpl conv = new ServerDatabaseConvertersImpl ();

		for (int humanSpellPicks = 0; humanSpellPicks <= 20; humanSpellPicks++)
		{
			final ClientDatabase clientDB = conv.buildClientDatabase (db, humanSpellPicks);

			assertEquals ("MF01", clientDB.getMapFeature ().get (0).getMapFeatureID ());
			assertFalse (clientDB.getMapFeature ().get (0).isAnyMagicRealmsDefined ());

			assertEquals (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY, clientDB.getMapFeature ().get (11).getMapFeatureID ());
			assertTrue (clientDB.getMapFeature ().get (11).isAnyMagicRealmsDefined ());
		}
	}

	/**
	 * Tests the buildClientDatabase method on a number of spell picks that doesn't exist
	 * @throws Exception If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testBuildClientDatabase_PickCountDoesntExist () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final ServerDatabaseConvertersImpl conv = new ServerDatabaseConvertersImpl ();

		conv.buildClientDatabase (db, 21);
	}
}