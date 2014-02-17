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
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import momime.client.ui.components.JPasswordFieldWithBackgroundImage;
import momime.client.ui.components.JTextFieldWithBackgroundImage;
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
	public final JButton createImageButton (final Action action, final Color backgroundColour, final Color foregroundColour, final Font font,
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
	 * Creates an image from an unticked image and a ticked image
	 * Text, if any, is assumed that it will be set later from the language XML file
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @param untickedImage Image of the checkbox in unticked state
	 * @param tickedImage Image of the checkbox in ticked state
	 * @return New checkbox
	 */
	@Override
	public final JCheckBox createImageCheckBox (final Color colour, final Font font, final BufferedImage untickedImage, final BufferedImage tickedImage)
	{
		final JCheckBox checkbox = new JCheckBox ();
		
		checkbox.setForeground (colour);
		checkbox.setFont (font);
		
		checkbox.setIcon (new ImageIcon (untickedImage));
		checkbox.setRolloverIcon (new ImageIcon (untickedImage));
		checkbox.setSelectedIcon (new ImageIcon (tickedImage));
		
		return checkbox;
	}

	/**
	 * Creates a text field that uses an image for its background rather than the standard drawing.
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @param backgroundImage Background image of the text field
	 * @return New text field
	 */
	@Override
	public final JTextField createTextFieldWithBackgroundImage (final Color colour, final Font font, final BufferedImage backgroundImage)
	{
		final JTextFieldWithBackgroundImage tf = new JTextFieldWithBackgroundImage ();

		tf.setBackgroundImage (backgroundImage);
		tf.setFont (font);
		tf.setForeground (colour);
		
		// Setting background to null just paints it black - to make it invisible we have to explicitly create a colour with 0 alpha component (4th param)
		tf.setBackground (new Color (0, 0, 0, 0));
		
		// Leave small gap at left and right so the text doesn't overlap the borders drawn on the image
		tf.setBorder (new EmptyBorder (3, 6, 3, 6));
		
		return tf;
	}
	
	/**
	 * Creates a password field that uses an image for its background rather than the standard drawing.
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @param backgroundImage Background image of the text field
	 * @return New text field
	 */
	@Override
	public final JTextField createPasswordFieldWithBackgroundImage (final Color colour, final Font font, final BufferedImage backgroundImage)
	{
		final JPasswordFieldWithBackgroundImage tf = new JPasswordFieldWithBackgroundImage ();

		tf.setBackgroundImage (backgroundImage);
		tf.setFont (font);
		tf.setForeground (colour);
		
		// Setting background to null just paints it black - to make it invisible we have to explicitly create a colour with 0 alpha component (4th param)
		tf.setBackground (new Color (0, 0, 0, 0));
		
		// Leave small gap at left and right so the text doesn't overlap the borders drawn on the image
		tf.setBorder (new EmptyBorder (3, 6, 3, 6));
		
		return tf;
	}
	
	/**
	 * @param gridx X cell we are putting a component into
	 * @param gridy Y cell we are putting a component into
	 * @param spanx Number of cells wide this component is
	 * @param insets Gap to leave around component
	 * @param anchor Position of the component within the grid cell
	 * @return Constraints object
	 */
	@Override
	public final GridBagConstraints createConstraints (final int gridx, final int gridy, final int spanx, final int insets, final int anchor)
	{
		final GridBagConstraints c = new GridBagConstraints ();
		c.gridx = gridx;
		c.gridy = gridy;
		c.gridwidth = spanx;
		c.anchor = anchor;
		
		if (insets > 0)
			c.insets = new Insets (insets, insets, insets, insets);
		
		return c;
	}
}
