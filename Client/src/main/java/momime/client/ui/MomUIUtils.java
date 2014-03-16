package momime.client.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Helper methods and constants for creating and laying out Swing components 
 */
public interface MomUIUtils
{
	/** Closest possible match to the gold font colours from the original MoM */
	public static final Color GOLD = new Color (0xFCC864);
	
	/** Closest possible match to the silver font colours from the original MoM */
	public static final Color SILVER = new Color (0xD8DCEC);
	
	/** Dark brown foreground text on buttons */
	public static final Color DARK_BROWN = new Color (0x312918);
	
	/** Light brown background text on buttons */
	public static final Color LIGHT_BROWN = new Color (0xA5846B);
	
	/**
	 * @param resourceName Name of image resource on classpath, e.g. /momime.client.graphics/something.png 
	 * @return Image
	 * @throws IOException If there is a problem loading the image
	 */
	public BufferedImage loadImage (final String resourceName) throws IOException;
	
	/**
	 * Creates a label with no text - typically because the text is going to be read from the language XML file later
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @return New label
	 */
	public JLabel createLabel (final Color colour, final Font font);

	/**
	 * Creates a label with some text
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @param text Text for the label
	 * @return New label
	 */
	public JLabel createLabel (final Color colour, final Font font, final String text);

	/**
	 * Creates a text area with no text - typically because the text is going to be read from the language XML file later
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @return New text area
	 */
	public JTextArea createTextArea (final Color colour, final Font font);
	
	/**
	 * Creates a label of an image
	 * 
	 * @param image Image to create a label for
	 * @return New label
	 */
	public JLabel createImage (final Image image);
	
	/**
	 * Creates a button comprising only of text, with no actual button appearance; assumed actual text will come from the action
	 * 
	 * @param action Action triggered by this button
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @return New button
	 */
	public JButton createTextOnlyButton (final Action action, final Color colour, final Font font);

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
	public JButton createImageButton (final Action action, final Color backgroundColour, final Color foregroundColour, final Font font,
		final BufferedImage normalImage, final BufferedImage pressedImage, final BufferedImage disabledImage);

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
	public JCheckBox createImageCheckBox (final Color colour, final Font font, final BufferedImage untickedImage, final BufferedImage tickedImage);
	
	/**
	 * Creates a text field that uses an image for its background rather than the standard drawing.
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @param backgroundImage Background image of the text field
	 * @return New text field
	 */
	public JTextField createTextFieldWithBackgroundImage (final Color colour, final Font font, final BufferedImage backgroundImage);
	
	/**
	 * Creates a password field that uses an image for its background rather than the standard drawing.
	 * 
	 * @param colour Colour to set the text in
	 * @param font Font to set the text in
	 * @param backgroundImage Background image of the text field
	 * @return New text field
	 */
	public JTextField createPasswordFieldWithBackgroundImage (final Color colour, final Font font, final BufferedImage backgroundImage);
	
	/**
	 * @param gridx X cell we are putting a component into
	 * @param gridy Y cell we are putting a component into
	 * @param spanx Number of cells wide this component is
	 * @param insets Gap to leave around component
	 * @param anchor Position of the component within the grid cell
	 * @return Constraints object
	 */
	public GridBagConstraints createConstraints (final int gridx, final int gridy, final int spanx, final int insets, final int anchor);

	/**
	 * @param src Source white image
	 * @param multRGB Colour to multiply the source image by
	 * @return New image created by multiplying the RGB components of the source image against the RGB components of the colour, and preserving the image alpha
	 */
	public BufferedImage multiplyImageByColour (final BufferedImage src, final int multRGB);
}
