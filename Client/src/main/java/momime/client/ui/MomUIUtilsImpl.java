package momime.client.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import momime.client.ui.components.OffsetShadowTextButtonUI;

/**
 * Helper methods and constants for creating and laying out Swing components 
 */
public final class MomUIUtilsImpl implements MomUIUtils
{
	/** Image cache */
	private final Map<String, BufferedImage> imageCache = new HashMap<String, BufferedImage> ();
	
	/**
	 * @param resourceName Name of image resource on classpath, e.g. /momime.client.graphics/something.png 
	 * @return Image
	 * @throws IOException If there is a problem loading the image
	 */
	@Override
	public final BufferedImage loadImage (final String resourceName) throws IOException
	{
		// Check cache first
		BufferedImage image = imageCache.get (resourceName);
		if (image == null)
		{
			image = ImageIO.read (getClass ().getResource (resourceName));
			imageCache.put (resourceName, image);
		}
		return image;
	}
	
	/**
	 * Creates a label with no text - typically because the text is going to be read from the language XML file later
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @return New label
	 */
	@Override
	public final JLabel createLabel (final Color colour, final Font font)
	{
		final JLabel label = new JLabel ();
		label.setForeground (colour);
		label.setFont (font);
		
		return label;
	}

	/**
	 * Creates a label with some text
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @param text Text for the label
	 * @return New label
	 */
	@Override
	public final JLabel createLabel (final Color colour, final Font font, final String text)
	{
		final JLabel label = createLabel (colour, font);
		label.setText (text);
		
		return label;
	}

	/**
	 * Creates a label of an image
	 * 
	 * @param image Image to create a label for
	 * @return New label
	 */
	@Override
	public final JLabel createImage (final BufferedImage image)
	{
		return new JLabel (new ImageIcon (image));
	}
	
	/**
	 * Creates a button comprising only of text, with no actual button appearance; assumed actual text will come from the action
	 * 
	 * @param action Action triggered by this button
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @return New button
	 */
	@Override
	public final JButton createTextOnlyButton (final Action action, final Color colour, final Font font)
	{
		final JButton button = new JButton (action);
		button.setForeground (colour);
		button.setFont (font);
		button.setContentAreaFilled (false);
		button.setMargin (new Insets (0, 0, 0, 0));
		button.setBorder (null);
		
		return button;
	}

	/**
	 * Creates a button with a background image; text will come from the action, so can be included or blank
	 * 
	 * @param action Action triggered by this button
	 * @param backgroundColour Colour to set the background text in
	 * @param foregroundColour Colour to set the foreground text in
	 * @param font Font to set the text in
	 * @param normalImage Image of the button in normal state
	 * @param pressedImage Image of the button when it is pressed
	 * @return New button
	 */
	@Override
	public JButton createImageButton (final Action action, final Color backgroundColour, final Color foregroundColour, final Font font,
		final BufferedImage normalImage, final BufferedImage pressedImage)
	{
		final JButton button = new JButton (action);
		button.setUI (new OffsetShadowTextButtonUI ());

		button.setForeground (foregroundColour);
		button.setBackground (backgroundColour);
		button.setFont (font);
		button.setContentAreaFilled (false);
		button.setMargin (new Insets (0, 0, 0, 0));
		button.setBorder (null);
		
		button.setIcon (new ImageIcon (normalImage));
		button.setPressedIcon (new ImageIcon (pressedImage));
		button.setHorizontalTextPosition (SwingConstants.CENTER);

		return button;
	}

	/**
	 * @param gridx X cell we are putting a component into
	 * @param gridy Y cell we are putting a component into
	 * @param insets Gap to leave around component
	 * @param anchor Position of the component within the grid cell
	 * @return Constraints object
	 */
	@Override
	public final GridBagConstraints createConstraints (final int gridx, final int gridy, final int insets, final int anchor)
	{
		final GridBagConstraints c = new GridBagConstraints ();
		c.gridx = gridx;
		c.gridy = gridy;
		c.anchor = anchor;
		
		if (insets > 0)
			c.insets = new Insets (insets, insets, insets, insets);
		
		return c;
	}
}
