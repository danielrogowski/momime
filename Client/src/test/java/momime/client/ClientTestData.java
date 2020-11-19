package momime.client;

import static org.junit.Assert.assertNotNull;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
import com.ndg.random.RandomUtils;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutConstants;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainer;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutObjectFactory;

import momime.client.graphics.database.GraphicsDatabase;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseExImpl;
import momime.client.graphics.database.GraphicsDatabaseObjectFactory;
import momime.client.language.database.LanguageDatabaseConstants;
import momime.common.database.AnimationGfx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonDatabaseFactory;
import momime.common.database.CommonDatabaseImpl;
import momime.common.database.CommonDatabaseObjectFactory;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.Language;
import momime.common.database.LanguageText;
import momime.common.database.MapFeatureEx;
import momime.common.database.OverlandMapSize;
import momime.common.database.SmoothedTileTypeEx;
import momime.common.database.TileSetEx;
import momime.common.database.WizardEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfCombatTiles;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomCombatTile;

/**
 * Since the tests in the common project can't use the XML file (since the classes generated from the server XSD that allow
 * JAXB to load the server XML are server side only) yet many of the client-side tests need it, this manufactures pieces
 * of test data that are used by more than one test
 */
public class ClientTestData
{
	/**
	 * @return Location of "English.Master of Magic Language.xml" and other language XMLs
	 * @throws IOException If we are unable to locate the English XML file
	 */
	public final File locateLanguageXmlFolder () throws IOException
	{
		// Not straightforward to find this, because its in src/external/resources so isn't on the classpath
		// So instead find something that is on the classpath of the MoMIMEClient project, then modify that location
		final URL languageXSD = getClass ().getResource (LanguageDatabaseConstants.LANGUAGES_XSD_LOCATION);
		final File languageFile = new File (languageXSD.getFile ());
		final File englishXmlFile = new File (languageFile, "../../../../src/external/resources/momime.client.language.database");

		return englishXmlFile.getCanonicalFile ();
	}

	/**
	 * @return Location of "Graphics.momime.xml" to test with
	 * @throws IOException If we are unable to locate the default graphics XML file
	 */
	public final File locateDefaultGraphicsXmlFile () throws IOException
	{
		// Not straightforward to find this, because its in src/external/resources so isn't on the classpath
		// So instead find something that is on the classpath of the MoMIMEClient project, then modify that location
		final URL graphicsXSD = getClass ().getResource (GraphicsDatabaseConstants.GRAPHICS_XSD_LOCATION);
		final File graphicsFile = new File (graphicsXSD.getFile ());
		final File graphicsXmlFile = new File (graphicsFile, "../../../../src/external/resources/momime.client.graphics.database/Graphics.momime.xml");

		return graphicsXmlFile.getCanonicalFile ();
	}

	/**
	 * @param utils UI utils to set against created objects that need it
	 * @param randomUtils Random number generator to set against created objects that need it
	 * @return Parsed graphics database with all the hash maps built, needed by a select few of the tests - usually avoid this because this makes tests really slow
	 * @throws Exception If there is a problem
	 */
	public final GraphicsDatabaseEx loadGraphicsDatabase (final NdgUIUtils utils, final RandomUtils randomUtils) throws Exception
	{
		// Need to set up a proper factory to create classes with spring injections
		final CommonDatabaseObjectFactory commonDatabaseFactory = new CommonDatabaseObjectFactory ();
		commonDatabaseFactory.setFactory (new CommonDatabaseFactory ()
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
			public final SmoothedTileTypeEx createSmoothedTileType ()
			{
				final SmoothedTileTypeEx tileType = new SmoothedTileTypeEx ();
				tileType.setRandomUtils (randomUtils);
				return tileType;
			}
			
			@Override
			public final TileSetEx createTileSet ()
			{
				final TileSetEx tileSet = new TileSetEx ();
				tileSet.setUtils (utils);
				return tileSet;
			}
			
			@Override
			public final AnimationGfx createAnimation ()
			{
				final AnimationGfx anim = new AnimationGfx ();
				anim.setUtils (utils);
				return anim;
			}
		});
		
		final GraphicsDatabaseObjectFactory graphicsDatabaseFactory = new GraphicsDatabaseObjectFactory ();
		
		// XSD
		final URL xsdResource = getClass ().getResource (GraphicsDatabaseConstants.GRAPHICS_XSD_LOCATION);
		assertNotNull ("MoM IME Graphics XSD could not be found on classpath", xsdResource);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Schema schema = schemaFactory.newSchema (xsdResource);

		final Unmarshaller unmarshaller = JAXBContext.newInstance (GraphicsDatabase.class).createUnmarshaller ();		
		unmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {commonDatabaseFactory, graphicsDatabaseFactory});
		unmarshaller.setSchema (schema);
		
		// XML
		final GraphicsDatabaseExImpl graphicsDB = (GraphicsDatabaseExImpl) unmarshaller.unmarshal (locateDefaultGraphicsXmlFile ());
		graphicsDB.buildMapsAndRunConsistencyChecks ();
		return graphicsDB;
	}
	
	/**
	 * @return Demo MoM overland map-like coordinate system with a 60x40 square map wrapping left-to-right but not top-to-bottom
	 */
	public final CoordinateSystem createOverlandMapCoordinateSystem ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setCoordinateSystemType (CoordinateSystemType.SQUARE);
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setDepth (2);
		sys.setWrapsLeftToRight (true);
		return sys;
	}

	/**
	 * @return Overland map coordinate system that can be included into session description
	 */
	public final OverlandMapSize createOverlandMapSize ()
	{
		final OverlandMapSize sys = new OverlandMapSize ();
		sys.setCoordinateSystemType (CoordinateSystemType.SQUARE);
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setDepth (2);
		sys.setWrapsLeftToRight (true);
		
		sys.setCitySeparation (3);
		sys.setContinentalRaceChance (75);
		
		return sys;
	}
	
	/**
	 * @param sys Overland map coordinate system
	 * @return Map area prepopulated with empty cells
	 */
	public final MapVolumeOfMemoryGridCells createOverlandMap (final CoordinateSystem sys)
	{
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < sys.getDepth (); plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sys.getWidth (); x++)
					row.getCell ().add (new MemoryGridCell ());

				area.getRow ().add (row);
			}

			map.getPlane ().add (area);
		}

		return map;
	}

	/**
	 * @return Demo MoM combat map-like coordinate system with a 60x40 diamond non-wrapping map
	 */
	public final CoordinateSystem createCombatMapCoordinateSystem ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		sys.setWidth (CommonDatabaseConstants.COMBAT_MAP_WIDTH);
		sys.setHeight (CommonDatabaseConstants.COMBAT_MAP_HEIGHT);
		return sys;
	}

	/**
	 * @return Combat map coordinate system that can be included into session description
	 */
	public final CombatMapSize createCombatMapSize ()
	{
		final CombatMapSize sys = new CombatMapSize ();
		sys.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		sys.setWidth (CommonDatabaseConstants.COMBAT_MAP_WIDTH);
		sys.setHeight (CommonDatabaseConstants.COMBAT_MAP_HEIGHT);
		sys.setZoneWidth (10);
		sys.setZoneHeight (8);
		return sys;
	}

	/**
	 * @param sys Combat map coordinate system
	 * @return Map area prepopulated with empty cells
	 */
	public final MapAreaOfCombatTiles createCombatMap (final CoordinateSystem sys)
	{
		final MapAreaOfCombatTiles map = new MapAreaOfCombatTiles ();
		for (int y = 0; y < sys.getHeight (); y++)
		{
			final MapRowOfCombatTiles row = new MapRowOfCombatTiles ();
			for (int x = 0; x < sys.getWidth (); x++)
				row.getCell ().add (new MomCombatTile ());

			map.getRow ().add (row);
		}

		return map;
	}
	
	/**
	 * @return XML layout unmarshaller
	 * @throws Exception If there is a problem
	 */
	public final Unmarshaller createXmlLayoutUnmarshaller () throws Exception
	{
		// XSD
		final URL xsdResource = getClass ().getResource (XmlLayoutConstants.XML_LAYOUT_XSD_LOCATION);
		assertNotNull ("XML layout XSD could not be found on classpath", xsdResource);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		final Schema schema = schemaFactory.newSchema (xsdResource);

		final Unmarshaller unmarshaller = JAXBContext.newInstance (XmlLayoutContainer.class).createUnmarshaller ();		
		unmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {new XmlLayoutObjectFactory ()});
		unmarshaller.setSchema (schema);
		
		return unmarshaller;
	}
	
	/**
	 * @param width Width to create image as
	 * @param height Height to create image as
	 * @param colour Colour to fill in image
	 * @return Solid colour image to test with 
	 */
	public final BufferedImage createSolidImage (final int width, final int height, final int colour)
	{
		final BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics ();
		try
		{
			g.setColor (new Color (colour));
			g.fillRect (0, 0, 640, 362);
		}
		finally
		{
			g.dispose ();
		}
		
		return image;
	}
	
	/**
	 * @param language Language to set
	 * @param text Text to set
	 * @return LanguageText object
	 */
	public final LanguageText createLanguageText (final Language language, final String text)
	{
		final LanguageText obj = new LanguageText ();
		obj.setLanguage (language);
		obj.setText (text);
		return obj;
	}
}