package momime.client.scheduledcombatmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

/**
 * The majority of scheduled combats are displayed as some kind of image to the left (or not) followed by some text.
 * Those can all be handled via this interface.
 */
public interface ScheduledCombatMessageSimpleUI extends ScheduledCombatMessageUI
{
	/**
	 * @return Image to draw for this NTM, or null to display only text
	 */
	public Image getImage ();
	
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