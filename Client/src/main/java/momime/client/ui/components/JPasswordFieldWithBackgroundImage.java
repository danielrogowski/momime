package momime.client.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPasswordField;

/**
 * JPasswordField, where an image is used to draw the background rather than the default drawing.
 * The size of the component is then fixed to the size of the image.
 */
public final class JPasswordFieldWithBackgroundImage extends JPasswordField
{
	/** Unique value for serialization */
	private static final long serialVersionUID = 4026194950424385684L;
	
	/** Background image */
	private BufferedImage backgroundImage;

	/**
	 * @param g The graphics object used for painting
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		// Draw background image
		g.drawImage (getBackgroundImage (), 0, 0, getWidth (), getHeight (), null);
		
		// Draw text
		super.paintComponent (g);
	}
	
	/**
	 * @return Background image
	 */
	public final BufferedImage getBackgroundImage ()
	{
		return backgroundImage;
	}

	/**
	 * @param img Background image
	 */
	public final void setBackgroundImage (final BufferedImage img)
	{
		backgroundImage = img;

		// Fix size of the text field to the same size as the background image
		final Dimension d = new Dimension (img.getWidth (), img.getHeight ());
		setMinimumSize (d);
		setMaximumSize (d);
		setPreferredSize (d);
	}
}
