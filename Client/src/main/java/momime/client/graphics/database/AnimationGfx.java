package momime.client.graphics.database;

import java.awt.image.BufferedImage;
import java.io.IOException;

import momime.client.graphics.database.v0_9_7.Animation;
import momime.common.MomException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Provides a consistency check on animations, to ensure all the images are the same size
 */
public final class AnimationGfx extends Animation
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (AnimationGfx.class);
	
	/** All frames of an animation must share the same width (these aren't private because a unit test sets them) */
	int animationWidth;
	
	/** All frames of an animation must share the same height (these aren't private because a unit test sets them) */
	int animationHeight;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/**
	 * Finds the width and height of all the frames in this animation
	 * @throws IOException If there is a problem loading any of the animation frames, or we fail the consistency checks
	 */
	final void deriveAnimationWidthAndHeight () throws IOException
	{
		log.trace ("Entering deriveAnimationWidthAndHeight: " + getAnimationID ());
		
		// Animations must be non-empty
		if (getFrame ().size () == 0)
			throw new MomException ("Animation " + getAnimationID () + " has 0 frames");
		
		// Check all the images
		boolean first = true;
		for (final String thisFrame : getFrame ())
		{
			final BufferedImage image = getUtils ().loadImage (thisFrame);
			if (first)
			{
				animationWidth = image.getWidth ();
				animationHeight = image.getHeight ();
				first = false;
			}
			else if ((animationWidth != image.getWidth ()) || (animationHeight != image.getHeight ()))
				throw new MomException ("Frames of animation " + getAnimationID () + " are not consistent sizes (some are " +
					animationWidth + "x" + animationHeight + " and some are " + image.getWidth () + "x" + image.getHeight () + ")");
		}
		
		log.trace ("Exiting deriveAnimationWidthAndHeight = " + animationWidth + "x" + animationHeight);
	}
	
	/**
	 * @return All frames of an animation must share the same width
	 */
	public final int getAnimationWidth ()
	{
		return animationWidth;
	}
	
	/**
	 * @return All frames of an animation must share the same height
	 */
	public final int getAnimationHeight ()
	{
		return animationHeight;
	}

	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}
}