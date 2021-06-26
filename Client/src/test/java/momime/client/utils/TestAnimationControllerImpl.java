package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;

import momime.client.graphics.AnimationContainer;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.MomException;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;

/**
 * Tests the AnimationControllerImpl class
 */
public final class TestAnimationControllerImpl
{
	/**
	 * Tests the loadImageOrAnimationFrame method passing in two nulls
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testLoadImageOrAnimationFrame_BothNull () throws Exception
	{
		// Set up object to test
		final AnimationControllerImpl controller = new AnimationControllerImpl ();
		
		// Run method
		controller.loadImageOrAnimationFrame (null, null, true, AnimationContainer.GRAPHICS_XML);
	}

	/**
	 * Tests the loadImageOrAnimationFrame method specifying both a static image and an animationID
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testLoadImageOrAnimationFrame_BothSpecifeid () throws Exception
	{
		// Set up object to test
		final AnimationControllerImpl controller = new AnimationControllerImpl ();
		
		// Run method
		controller.loadImageOrAnimationFrame ("a", "b", true, AnimationContainer.GRAPHICS_XML);
	}

	/**
	 * Tests the loadImageOrAnimationFrame method to retrieve a static image
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testLoadImageOrAnimationFrame_StaticImage () throws Exception
	{
		// Create a sample image
		final BufferedImage image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);

		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("blah.png")).thenReturn (image);
		
		// Set up object to test
		final AnimationControllerImpl controller = new AnimationControllerImpl ();
		controller.setUtils (utils);
		
		// Run method
		final BufferedImage imageResult = controller.loadImageOrAnimationFrame ("blah.png", null, true, AnimationContainer.GRAPHICS_XML);
	
		// Check results
		assertSame (image, imageResult);
	}

	/**
	 * Tests the loadImageOrAnimationFrame method trying to retrieve an animation that we didn't request interest in
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testLoadImageOrAnimationFrame_WithoutRegistering () throws Exception
	{
		// Set up object to test
		final AnimationControllerImpl controller = new AnimationControllerImpl ();
		
		// Run method
		controller.loadImageOrAnimationFrame (null, "ANIM", true, AnimationContainer.GRAPHICS_XML);
	}

	/**
	 * Tests the loadImageOrAnimationFrame and associated methods to set up an animation
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testLoadImageOrAnimationFrame_Anim () throws Exception
	{
		// Create 4 sample images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		final List<BufferedImage> images = new ArrayList<BufferedImage> ();
		for (int n = 0; n < 4; n++)
		{
			final BufferedImage image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
			when (utils.loadImage (n + ".png")).thenReturn (image);
			images.add (image);
		}

		// Mock entries from the graphics XML
		final AnimationEx anim = new AnimationEx ();
		anim.setAnimationSpeed (2);
		for (int n = 0; n < 4; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile (n + ".png");
			anim.getFrame ().add (frame);
		}

		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation ("ANIM", "registerRepaintTrigger")).thenReturn (anim);

		// Set up object to test
		final AnimationControllerImpl controller = new AnimationControllerImpl ();
		controller.setUtils (utils);
		controller.setGraphicsDB (gfx);
		
		// Set up a dummy component
		final StringBuffer triggeredFrames = new StringBuffer (); 
		final JComponent dummy = new JComponent ()
		{
			@Override
			public final void repaint ()
			{
				try
				{
					triggeredFrames.append (images.indexOf (controller.loadImageOrAnimationFrame (null, "ANIM", true, AnimationContainer.GRAPHICS_XML)));
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};
		
		// Register to receive repaint events
		controller.registerRepaintTrigger ("ANIM", dummy, AnimationContainer.GRAPHICS_XML);
		
		// This is here to simulate that normally this would all be being done in a frame's init () method, and so the frame would get drawn
		// once by virtue of being displayed, prior to the frame timer starting to send any triggers
		dummy.repaint ();
		assertEquals ("0", triggeredFrames.toString ());
		
		// Timing of this is a bit iffy, sometimes we get the "1" on the end and sometimes we don't, depending how the test runs
		Thread.sleep (5000);
		if (!triggeredFrames.toString ().equals ("012301230"))
			assertEquals ("0123012301", triggeredFrames.toString ());
		
		controller.unregisterRepaintTrigger ("ANIM", dummy);
		Thread.sleep (1000);
		if (!triggeredFrames.toString ().equals ("012301230"))
			assertEquals ("0123012301", triggeredFrames.toString ());
	}
}