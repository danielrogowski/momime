package momime.server.database;

import static org.junit.Assert.assertEquals;
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

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import momime.client.database.AvailableDatabase;
import momime.common.database.AnimationGfx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonDatabaseFactory;
import momime.common.database.CommonDatabaseImpl;
import momime.common.database.CommonDatabaseObjectFactory;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.MapFeatureEx;
import momime.common.database.MomDatabase;
import momime.common.database.SmoothedTileTypeEx;
import momime.common.database.TileSetEx;
import momime.common.database.WizardEx;
import momime.common.messages.servertoclient.NewGameDatabaseMessage;
import momime.server.ServerTestData;

/**
 * Tests the ServerDatabaseConverters class
 */
public final class TestServerDatabaseConvertersImpl extends ServerTestData
{
	/**
	 * Tests the buildNewGameDatabase method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildNewGameDatabase () throws Exception
	{
		// Need to set up a proper factory to create classes with spring injections
		final CommonDatabaseObjectFactory factory = new CommonDatabaseObjectFactory ();
		factory.setFactory (new CommonDatabaseFactory ()
		{
			@Override
			public final CommonDatabaseImpl createDatabase ()
			{
				return new CommonDatabaseImpl ();
			}
			
			@Override
			public final WizardEx createWizard ()
			{
				return new WizardEx (); 
			}
			
			@Override
			public final MapFeatureEx createMapFeature ()
			{
				return new MapFeatureEx ();
			}

			@Override
			public final TileSetEx createTileSet ()
			{
				return new TileSetEx ();
			}
			
			@Override
			public final SmoothedTileTypeEx createSmoothedTileType ()
			{
				return new SmoothedTileTypeEx ();
			}
			
			@Override
			public final AnimationGfx createAnimation ()
			{
				return new AnimationGfx ();
			}
		});
		
		// Read XSD
		final URL xsdResource = getClass ().getResource (CommonDatabaseConstants.COMMON_XSD_LOCATION);
		assertNotNull ("MoM IME Common XSD could not be found on classpath", xsdResource);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));

		final Schema xsd = schemaFactory.newSchema (xsdResource);
		
		// Locate XML
		final File serverXml = locateServerXmlFile ();
		assertNotNull ("MoM IME Server XML could not be found", serverXml);
		
		final Map<String, File> map = new HashMap<String, File> ();
		map.put ("Original Master of Magic 1.31 rules", serverXml);
		
		// Set up object to test
		final ServerDatabaseConvertersImpl conv = new ServerDatabaseConvertersImpl ();

		// Build it
		// Locate the server XML file, then go one level up to the folder that it is in
		final Unmarshaller serverDatabaseUnmarshaller = JAXBContext.newInstance (MomDatabase.class).createUnmarshaller ();
		serverDatabaseUnmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {factory});
		serverDatabaseUnmarshaller.setSchema (xsd);
		
		final NewGameDatabaseMessage msg = conv.buildNewGameDatabase (map, serverDatabaseUnmarshaller);
		assertEquals (1, msg.getNewGameDatabase ().getMomimeXmlDatabase ().size ());

		final AvailableDatabase db = msg.getNewGameDatabase ().getMomimeXmlDatabase ().get (0);
		assertEquals ("Original Master of Magic 1.31 rules", db.getDbName ());
		assertEquals ("Failed to load correct number of overland map sizes",	6, db.getOverlandMapSize ().size ());
		assertEquals ("Failed to load correct number of land proportions",		3, db.getLandProportion ().size ());
		assertEquals ("Failed to load correct number of node strengths",		3, db.getNodeStrength ().size ());
		assertEquals ("Failed to load correct number of difficulty levels",			6, db.getDifficultyLevel ().size ());
		assertEquals ("Failed to load correct number of fog of war settings",	2, db.getFogOfWarSetting ().size ());
		assertEquals ("Failed to load correct number of unit settings",			2, db.getUnitSetting ().size ());
		assertEquals ("Failed to load correct number of spell settings",			2, db.getSpellSetting ().size ());

		// Build new game database
		@SuppressWarnings ("resource")
		final ApplicationContext applicationContext = new ClassPathXmlApplicationContext ("/momime-common-test-beans.xml");
		
		final JAXBContext serverToClientJaxbContext  = (JAXBContext) applicationContext.getBean ("serverToClientJaxbContext");
		final Marshaller marshaller = serverToClientJaxbContext.createMarshaller ();

		final ByteArrayOutputStream stream = new ByteArrayOutputStream ();
		marshaller.marshal (msg, stream);
		
		// Should be present as part of DB name
		final String newGameDB = new String (stream.toByteArray ());
		assertTrue (newGameDB.contains ("1.31"));
	}
}