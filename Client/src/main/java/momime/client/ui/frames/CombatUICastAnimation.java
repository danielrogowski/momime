package momime.client.ui.frames;

import momime.common.database.AnimationEx;

/**
 * Since complicated spells like Call Chaos can result in displaying multiple spell animations simultaneously,
 * need a structure to hold info about each animation to draw.
 */
public final class CombatUICastAnimation
{
	/** X coordinate to draw the animation at */
	private int positionX;

	/** Y coordinate to draw the animation at */
	private int positionY;
	
	/** The animation to draw */
	private AnimationEx anim;
	
	/** Number of frames to display */
	private int frameCount;
	
	/** Frame number to display */
	private int frameNumber;
	
	/** Whether the combat case animation appears behind or in front of the units */
	private boolean inFront;

	/**
	 * @return X coordinate to draw the animation at
	 */
	public final int getPositionX ()
	{
		return positionX;
	}

	/**
	 * @param x X coordinate to draw the animation at
	 */
	public final void setPositionX (final int x)
	{
		positionX = x;
	}

	/**
	 * @return Y coordinate to draw the animation at
	 */
	public final int getPositionY ()
	{
		return positionY;
	}
	
	/**
	 * @param y Y coordinate to draw the animation at
	 */
	public final void setPositionY (final int y)
	{
		positionY = y;
	}

	/**
	 * @return The animation to draw
	 */
	public final AnimationEx getAnim ()
	{
		return anim;
	}

	/**
	 * @param a The animation to draw
	 */
	public final void setAnim (final AnimationEx a)
	{
		anim = a;
	}

	/**
	 * @return Number of frames to display
	 */
	public final int getFrameCount ()
	{
		return frameCount;
	}

	/**
	 * @param c Number of frames to display
	 */
	public final void setFrameCount (final int c)
	{
		frameCount = c;
	}
	
	/**
	 * @return Frame number to display
	 */
	public final int getFrameNumber ()
	{
		return frameNumber;
	}

	/**
	 * @param f Frame number to display
	 */
	public final void setFrameNumber (final int f)
	{
		frameNumber = f;
	}
	
	/**
	 * @return Whether the combat case animation appears behind or in front of the units
	 */
	public final boolean isInFront ()
	{
		return inFront;
	}
	
	/**
	 * @param f Whether the combat case animation appears behind or in front of the units
	 */
	public final void setInFront (final boolean f)
	{
		inFront = f;
	}
}