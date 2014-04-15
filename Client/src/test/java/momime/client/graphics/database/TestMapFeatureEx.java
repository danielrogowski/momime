package momime.client.graphics.database;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;

import momime.client.ui.MomUIUtils;
import momime.common.MomException;

import org.junit.Test;

/**
 * Tests the MapFeatureEx class
 */
public final class TestMapFeatureEx
{
	/**
	 * Tests the checkWidthAndHeight method on a map feature that's the right size
	 * @throws IOException If there is a problem loading the image, or the map feature is the wrong size
	 */
	@Test
	public final void testCheckWidthAndHeight_Consistent () throws IOException
	{
		// Mock an images
		final MomUIUtils utils = mock (MomUIUtils.class);
		
		final BufferedImage image = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("FeatureImage")).thenReturn (image);
		
		// Set up dummy tile set
		final TileSetEx ts = new TileSetEx ();
		ts.tileWidth = 10;
		ts.tileHeight = 5;
		
		// Set up object to test
		final MapFeatureEx feature = new MapFeatureEx ();
		feature.setUtils (utils);
		feature.setOverlandMapImageFile ("FeatureImage");
		
		// Run method
		feature.checkWidthAndHeight (ts);
	}

	/**
	 * Tests the checkWidthAndHeight method on a map feature that's the right size
	 * @throws IOException If there is a problem loading the image, or the map feature is the wrong size
	 */
	@Test(expected=MomException.class)
	public final void testCheckWidthAndHeight_Inconsistent () throws IOException
	{
		// Mock an images
		final MomUIUtils utils = mock (MomUIUtils.class);
		
		final BufferedImage image = new BufferedImage (10, 6, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("FeatureImage")).thenReturn (image);
		
		// Set up dummy tile set
		final TileSetEx ts = new TileSetEx ();
		ts.tileWidth = 10;
		ts.tileHeight = 5;
		
		// Set up object to test
		final MapFeatureEx feature = new MapFeatureEx ();
		feature.setUtils (utils);
		feature.setOverlandMapImageFile ("FeatureImage");
		
		// Run method
		feature.checkWidthAndHeight (ts);
	}
}
