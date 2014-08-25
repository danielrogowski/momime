package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

/**
 * The majority of NTMs are displayed as some kind of image to the left (or not) followed by some text.
 * Those can all be handled via this interface.  More complex custom drawn NTMs should implement NewTurnMessageComplexUI instead.
 */
public interface NewTurnMessageSimpleUI extends NewTurnMessageUI
{
	/**
	 * @return Image to draw for this NTM, or null to display only text
	 */
	public BufferedImage getImage ();
	
	/**
	 * @return Text to display for this NTM
	 */
	public String getText ();
	
	/**
	 * @return Font to display the text in
	 */
	public Font getFont ();
	
	/**
	 * @return Colour to display the text in
	 */
	public Color getColour ();
}