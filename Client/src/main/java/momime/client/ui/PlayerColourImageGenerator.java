package momime.client.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * Various items like unit backgrounds and city flags are displayed in the player's colour.
 * This caches the coloured images so that we don't have to recreate a multiplied colour image every time.
 */
public interface PlayerColourImageGenerator
{
	/**
	 * @param playerID Unit owner player ID
	 * @return Unit background image in their correct colour; null for the monsters player who has no colour
	 * @throws IOException If there is a problem loading the background image
	 */
	public BufferedImage getUnitBackgroundImage (final int playerID) throws IOException;

	/**
	 * @param playerID City owner player ID
	 * @return City flag image in their correct colour
	 * @throws IOException If there is a problem loading the flag image
	 */
	public BufferedImage getCityFlagImage (final int playerID) throws IOException;

	/**
	 * @param frameNumber Frame number of the node aura animation
	 * @param playerID Node owner player ID
	 * @return Node aura image in their correct colour
	 * @throws IOException If there is a problem loading the flag image
	 */
	public BufferedImage getNodeAuraImage (final int frameNumber, final int playerID) throws IOException;
	
	/**
	 * @param playerID Spell owner player ID
	 * @return Mirror image in their correct colour
	 * @throws IOException If there is a problem loading the mirror image
	 */
	public BufferedImage getOverlandEnchantmentMirror (final int playerID) throws IOException;

	/**
	 * @param playerID Unit owner player ID
	 * @return Wizard gem background image in their correct colour
	 * @throws IOException If there is a problem loading the background image
	 */
	public BufferedImage getWizardGemImage (final int playerID) throws IOException;
	
	/**
	 * @param playerID Unit owner player ID
	 * @return Cracked wizard gem background image in their correct colour
	 * @throws IOException If there is a problem loading the background image
	 */
	public BufferedImage getWizardGemCrackedImage (final int playerID) throws IOException;
	
	/**
	 * @param d Direction of border edge to draw
	 * @param playerID ID of player whose border we are drawing
	 * @return Border edge in player colour
	 * @throws IOException If there is a problem loading the border image
	 */
	public BufferedImage getFriendlyZoneBorderImage (final int d, final int playerID) throws IOException;

	/**
	 * Images (typically of unit figures) shaded by one or more unit skills that change a unit's appearance, e.g. Black Sleep or Invisibility
	 * The key to this is the name of the image, followed by the colour codes that have been applied to its appearance, in alphabetical order, with a : delimiter
	 * e.g. "/momime.client.graphics/units/UN123/d5-stand.png:808080:FFFF50"
	 * 
	 * @param imageName Filename of the base colour image
	 * @param shadingColours List of shading colours to apply to the image
	 * @return Image with modified colours
	 * @throws IOException If there is a problem loading the image
	 */
	public BufferedImage getSkillShadedImage (final String imageName, final List<String> shadingColours) throws IOException;
}