package momime.client.calculations;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.config.v0_9_5.MomImeClientConfig;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.CombatMapSizeData;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.CombatMapUtilsImpl;
import momime.unittests.mapstorage.StoredCombatMap;

import org.junit.Test;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.random.RandomUtilsImpl;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the CombatMapBitmapGeneratorImpl class
 */
public final class TestCombatMapBitmapGeneratorImpl
{
	/**
	 * Its hard to test these meaningfully in isolation, so this tests the smoothMapTerrain method followed by the generateCombatMapBitmap method.
	 * It uses a combat map generated by the server unit tests that's been copied into src/test/resources in the client project, so that we don't have to generate one.
	 * 
	 * This is a really heavyweight test method because it needs not only the big XML file of the combat map, but also the full graphics XML file
	 * with all the bitmasks and smoothing rules properly parsed, so this takes over 30 seconds to run.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateCombatMapBitmap () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Load in map generated by the server unit tests
		final URL xsdResource = new Object ().getClass ().getResource ("/momime.unittests.mapstorage/MapStorage.xsd");
		assertNotNull ("Map storage XSD could not be found on classpath", xsdResource);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Schema schema = schemaFactory.newSchema (xsdResource);
		
		final Unmarshaller unmarshaller = JAXBContext.newInstance (StoredCombatMap.class).createUnmarshaller ();
		unmarshaller.setSchema (schema);

		final URL xmlResource = new Object ().getClass ().getResource ("/momime.unittests.mapstorage/generatedCombatMap.xml");
		assertNotNull ("Example combat map XML could not be found on classpath", xmlResource);
		
		final StoredCombatMap container = (StoredCombatMap) unmarshaller.unmarshal (xmlResource);
		
		// Config
		final MomImeClientConfig config = new MomImeClientConfig ();
		config.setCombatSmoothTerrain (true);
		
		// Session description
		final MapSizeData mapSizeData = ClientTestData.createMapSizeData ();
		final CombatMapSizeData combatMapSizeData = ClientTestData.createCombatMapSizeData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSizeData);
		sd.setCombatMapSize (combatMapSizeData);
		
		// Overland map (we need the tile type)
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST);
		
		final MapVolumeOfMemoryGridCells overlandMap = ClientTestData.createOverlandMap (mapSizeData);
		overlandMap.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (overlandMap);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Set up object to test
		final RandomUtilsImpl random = new RandomUtilsImpl (); 
		
		final CombatMapBitmapGeneratorImpl gen = new CombatMapBitmapGeneratorImpl ();
		gen.setClient (client);
		gen.setClientConfig (config);
		gen.setUtils (utils);
		gen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		gen.setCombatMapUtils (new CombatMapUtilsImpl ());
		gen.setGraphicsDB (ClientTestData.loadGraphicsDatabase (utils, random));
		
		// Run methods
		gen.afterJoinedSession ();
		gen.smoothMapTerrain (new MapCoordinates3DEx (20, 10, 0), container.getCombatMap ());
		
		// Display each plane
		final BufferedImage bitmap = gen.generateCombatMapBitmaps () [0];
		
		// Set up a dummy frame to display the first of the bitmaps
		final JPanel contentPane = utils.createPanelWithBackgroundImage (bitmap);

		final JFrame frame = new JFrame ("testGenerateCombatMapBitmap");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane (contentPane);
		frame.pack ();
		frame.setLocationRelativeTo (null);
		frame.setVisible (true);
		
		Thread.sleep (5000);
	}
}