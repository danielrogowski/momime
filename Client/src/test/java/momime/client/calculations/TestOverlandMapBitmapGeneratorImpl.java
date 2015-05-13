package momime.client.calculations;

import static org.junit.Assert.assertEquals;
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
import momime.client.config.MomImeClientConfigEx;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.OverlandMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.unittests.mapstorage.StoredOverlandMap;

import org.junit.Test;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.random.RandomUtilsImpl;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the OverlandMapBitmapGeneratorImpl class
 */
public final class TestOverlandMapBitmapGeneratorImpl
{
	/**
	 * Its hard to test these meaningfully in isolation, so this tests the smoothMapTerrain method followed by the generateOverlandMapBitmaps method.
	 * It uses an overland map generated by the server unit tests that's been copied into src/test/resources in the client project, so that we don't have to generate one.
	 * 
	 * This is a really heavyweight test method because it needs not only the big XML file of the overland map, but also the full graphics XML file
	 * with all the bitmasks and smoothing rules properly parsed, so this takes over 30 seconds to run.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateOverlandMapBitmaps () throws Exception
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
		
		final Unmarshaller unmarshaller = JAXBContext.newInstance (StoredOverlandMap.class).createUnmarshaller ();
		unmarshaller.setSchema (schema);

		final URL xmlResource = new Object ().getClass ().getResource ("/momime.unittests.mapstorage/generatedOverlandMap.xml");
		assertNotNull ("Example overland map XML could not be found on classpath", xmlResource);
		
		final StoredOverlandMap container = (StoredOverlandMap) unmarshaller.unmarshal (xmlResource);
		
		// Config
		final MomImeClientConfigEx config = new MomImeClientConfigEx ();
		config.setOverlandSmoothTerrain (true);
		
		// Session description
		final OverlandMapSize overlandMapSize = ClientTestData.createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Overland map
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (container.getOverlandMap ());
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Set up object to test
		final RandomUtilsImpl random = new RandomUtilsImpl (); 
		
		final OverlandMapBitmapGeneratorImpl gen = new OverlandMapBitmapGeneratorImpl ();
		gen.setClient (client);
		gen.setClientConfig (config);
		gen.setUtils (utils);
		gen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		gen.setGraphicsDB (ClientTestData.loadGraphicsDatabase (utils, random));
		
		// Run methods
		gen.afterJoinedSession ();
		gen.smoothMapTerrain (null);
		
		// Display each plane
		for (int plane = 0; plane < 2; plane++)
		{
			final BufferedImage [] bitmaps = gen.generateOverlandMapBitmaps (plane, 0, 0, overlandMapSize.getWidth (), overlandMapSize.getHeight ());
			
			// Do what automated checks we can
			assertEquals (4, bitmaps.length);
			for (final BufferedImage bitmap : bitmaps)
			{
				assertEquals (60 * 20, bitmap.getWidth ());
				assertEquals (40 * 18, bitmap.getHeight ());
			}
			
			// Set up a dummy frame to display the first of the bitmaps
			final JPanel contentPane = utils.createPanelWithBackgroundImage (bitmaps [0]);
	
			final JFrame frame = new JFrame ("testGenerateOverlandMapBitmaps - plane " + plane);
			frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
			frame.setContentPane (contentPane);
			frame.pack ();
			frame.setLocationRelativeTo (null);
			
			frame.setVisible (true);
			Thread.sleep (5000);
			frame.setVisible (false);
		}
	}
}