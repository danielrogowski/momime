package momime.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
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
			final URL resource = getClass ().getResource (resourceName);
			if (resource == null)
				throw new IOException ("Image \"" + resourceName + "\" not found on classpath");
			
			image = ImageIO.read (resource);
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
	 * Creates a label (actually text area made to look like a label) with wrapping text
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @return New label
	 */
	@Override
	public final JTextArea createWrappingLabel (final Color colour, final Font font)
	{
		final JTextArea messageText = createTextArea (colour, font);
		messageText.setEditable (false);
		messageText.setOpaque (false);
		messageText.setWrapStyleWord (true);		// This is why we have to use a JTextArea, since JLabels don't support wrapping
		messageText.setLineWrap (true);
		messageText.setBorder (new EmptyBorder (0, 0, 0, 0));
		
		// Setting background to null just paints it black - to make it invisible we have to explicitly create a colour with 0 alpha component (4th param)
		messageText.setBackground (new Color (0, 0, 0, 0));
		return messageText;
	}
	
	/**
	 * Creates a text area with no text - typically because the text is going to be read from the language XML file later
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @return New text area
	 */
	@Override
	public final JTextArea createTextArea (final Color colour, final Font font)
	{
		final JTextArea label = new JTextArea ();
		label.setForeground (colour);
		label.setFont (font);
		
		return label;
	}
	
	/**
	 * Creates a label of an image
	 * 
	 * @param image Image to create a label for
	 * @return New label
	 */
	@Override
	public final JLabel createImage (final Image image)
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
	 * @param disabledImage Image of the button when it is disabled
	 * @return New button
	 */
	@Override
	public final JButton createImageButton (final Action action, final Color backgroundColour, final Color foregroundColour, final Font font,
		final BufferedImage normalImage, final BufferedImage pressedImage, final BufferedImage disabledImage)
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
		button.setDisabledIcon (new ImageIcon (disabledImage));
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
	 * Creates a transparent text field
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @param size Fixed size for the text field
	 * @return New text field
	 */
	@Override
	public final JTextField createTransparentTextField (final Color colour, final Font font, final Dimension size)
	{
		final JTextField tf = new JTextField ();

		tf.setFont (font);
		tf.setForeground (colour);
		tf.setMinimumSize (size);
		tf.setMaximumSize (size);
		tf.setPreferredSize (size);
		
		// Setting background to null just paints it black - to make it invisible we have to explicitly create a colour with 0 alpha component (4th param)
		tf.setBackground (new Color (0, 0, 0, 0));
		
		// Assume since its transparent that we created it exactly the right size, so don't need any border
		tf.setBorder (null);
		
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

	/**
	 * @param gridx X cell we are putting a component into
	 * @param gridy Y cell we are putting a component into
	 * @param spanx Number of cells wide this component is
	 * @param insets Custom insets
	 * @param anchor Position of the component within the grid cell
	 * @return Constraints object
	 */
	@Override
	public final GridBagConstraints createConstraints (final int gridx, final int gridy, final int spanx, final Insets insets, final int anchor)
	{
		final GridBagConstraints c = new GridBagConstraints ();
		c.gridx = gridx;
		c.gridy = gridy;
		c.gridwidth = spanx;
		c.anchor = anchor;
		c.insets = insets;
		
		return c;
	}

	/**
	 * @param src Source white image
	 * @param multRGB Colour to multiply the source image by
	 * @return New image created by multiplying the RGB components of the source image against the RGB components of the colour, and preserving the image alpha
	 */
	@Override
	public final BufferedImage multiplyImageByColour (final BufferedImage src, final int multRGB)
	{
		final int multBlue = (multRGB >> 16) & 0xFF;
		final int multGreen = (multRGB >> 8) & 0xFF;
		final int multRed = multRGB & 0xFF;
		
		final BufferedImage dest = new BufferedImage (src.getWidth (), src.getHeight (), src.getType ());
		for (int y = 0; y < src.getHeight (); y++)
			for (int x = 0; x < src.getWidth (); x++)
			{
				final int srcRGBA = src.getRGB (x, y);
				final int srcAlpha = srcRGBA >> 24;
				final int srcBlue = (srcRGBA >> 16) & 0xFF;
				final int srcGreen = (srcRGBA >> 8) & 0xFF;
				final int srcRed = srcRGBA & 0xFF;
				
				final int destAlpha = srcAlpha;
				final int destBlue = (srcBlue * multBlue) / 0xFF;
				final int destGreen = (srcGreen * multGreen) / 0xFF;
				final int destRed = (srcRed * multRed) / 0xFF;
				
				final int destRGBA = (destAlpha << 24) | (destBlue << 16) | (destGreen << 8) | destRed;
				dest.setRGB (x, y, destRGBA);
			}
		
		return dest;
	}
}
