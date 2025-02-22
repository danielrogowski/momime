package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.utils.swing.NdgUIUtils;

import momime.common.MomException;

/**
 * Tests the MapFeatureEx class
 */
@ExtendWith(MockitoExtension.class)
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
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage image = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("FeatureImage")).thenReturn (image);
		
		// Set up dummy tile set
		final TileSetEx ts = new TileSetEx ();
		ts.setTileWidth (10);
		ts.setTileHeight (5);
		
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
	@Test
	public final void testCheckWidthAndHeight_Inconsistent () throws IOException
	{
		// Mock an images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage image = new BufferedImage (10, 6, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("FeatureImage")).thenReturn (image);
		
		// Set up dummy tile set
		final TileSetEx ts = new TileSetEx ();
		ts.setTileWidth (10);
		ts.setTileHeight (5);
		
		// Set up object to test
		final MapFeatureEx feature = new MapFeatureEx ();
		feature.setUtils (utils);
		feature.setOverlandMapImageFile ("FeatureImage");
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			feature.checkWidthAndHeight (ts);
		});
	}
}