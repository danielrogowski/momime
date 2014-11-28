package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;

import momime.common.MomException;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;

/**
 * Tests the AnimationEx class
 */
public final class TestAnimationEx
{
	/**
	 * Tests the deriveAnimationWidthAndHeight method on an animation with no frames
	 * @throws IOException If there is a problem loading any of the animation frames, or we fail the consistency checks
	 */
	@Test(expected=MomException.class)
	public final void testDeriveAnimationWidthAndHeight_NoFrames () throws IOException
	{
		// Set up animation and some test frames
		final AnimationEx anim = new AnimationEx ();
		
		// Run method
		anim.deriveAnimationWidthAndHeight ();
	}

	/**
	 * Tests the deriveAnimationWidthAndHeight method on an animation with a single frame
	 * @throws IOException If there is a problem loading any of the animation frames, or we fail the consistency checks
	 */
	@Test
	public final void testDeriveAnimationWidthAndHeight_OneFrame () throws IOException
	{
		// Set up animation and some test frames
		final AnimationEx anim = new AnimationEx ();
		anim.getFrame ().add ("ImageFile1");
		
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		anim.setUtils (utils);
		
		final BufferedImage image1 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("ImageFile1")).thenReturn (image1);
		
		// Run method
		anim.deriveAnimationWidthAndHeight ();
		
		// Check results
		assertEquals (10, anim.getAnimationWidth ());
		assertEquals (5, anim.getAnimationHeight ());
	}

	/**
	 * Tests the deriveAnimationWidthAndHeight method on an animation with multiple consistent frames
	 * @throws IOException If there is a problem loading any of the animation frames, or we fail the consistency checks
	 */
	@Test
	public final void testDeriveAnimationWidthAndHeight_MultipleFrames () throws IOException
	{
		// Set up animation and some test frames
		final AnimationEx anim = new AnimationEx ();
		for (int n = 1; n <= 3; n++)
			anim.getFrame ().add ("ImageFile" + n);
		
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		anim.setUtils (utils);
		
		final BufferedImage image1 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("ImageFile1")).thenReturn (image1);

		final BufferedImage image2 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("ImageFile2")).thenReturn (image2);

		final BufferedImage image3 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("ImageFile3")).thenReturn (image3);
		
		// Run method
		anim.deriveAnimationWidthAndHeight ();
		
		// Check results
		assertEquals (10, anim.getAnimationWidth ());
		assertEquals (5, anim.getAnimationHeight ());
	}

	/**
	 * Tests the deriveAnimationWidthAndHeight method on an animation with multiple inconsistent frames
	 * @throws IOException If there is a problem loading any of the animation frames, or we fail the consistency checks
	 */
	@Test(expected=MomException.class)
	public final void testDeriveAnimationWidthAndHeight_Inconsistent () throws IOException
	{
		// Set up animation and some test frames
		final AnimationEx anim = new AnimationEx ();
		for (int n = 1; n <= 3; n++)
			anim.getFrame ().add ("ImageFile" + n);
		
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		anim.setUtils (utils);
		
		final BufferedImage image1 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("ImageFile1")).thenReturn (image1);

		final BufferedImage image2 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("ImageFile2")).thenReturn (image2);

		final BufferedImage image3 = new BufferedImage (10, 6, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("ImageFile3")).thenReturn (image3);
		
		// Run method
		anim.deriveAnimationWidthAndHeight ();
	}
}
