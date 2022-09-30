package momime.client.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import momime.common.database.UnitEx;

/**
 * Various items like unit backgrounds and city flags are displayed in the player's colour.
 * This caches the coloured images so that we don't have to recreate a multiplied colour image every time.
 */
public interface PlayerColourImageGenerator
{
	/**
	 * Clears the cache between games
	 */
	public void clearCache ();
	
	/**
	 * @param unitDef Unit to get the image for
	 * @param playerID Player ID
	 * @return Overland image for this unit, with the correct flag colour already drawn on; background square is not included
	 * @throws IOException If there is a problem loading the images
	 */
	public BufferedImage getOverlandUnitImage (final UnitEx unitDef, final int playerID) throws IOException;
	
	/**
	 * @param frameNumber Frame number of the node aura animation
	 * @param playerID Node owner player ID
	 * @param warped Whether to darken the node aura to show a warped node
	 * @return Node aura image in their correct colour
	 * @throws IOException If there is a problem loading the flag image
	 */
	public BufferedImage getNodeAuraImage (final int frameNumber, final int playerID, final boolean warped) throws IOException;
	
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
	 * @param playerColourEntireImage If true, the entire primary image is changed to the wizard's flag colour like e.g. the wizard gems
	 * 	or overland enchantment mirrors, in this case any flag values will be ignored; if false, the primary image is kept original colour and
	 * 	only the flag is changed to the wizard's colour and then superimposed onto the primary image
	 * @param flagName Name of flag image, if there is one
	 * @param flagOffsetX X offset to draw flag
	 * @param flagOffsetY Y offset to draw flag
	 * @param playerID Player who owns the unit; if this is not supplied then the flag image will be ignored (if there is one) 
	 * @param shadingColours List of shading colours to apply to the image
	 * @return Image with modified colours
	 * @throws IOException If there is a problem loading the image
	 */
	public BufferedImage getModifiedImage (final String imageName, final boolean playerColourEntireImage,
		final String flagName, final Integer flagOffsetX, final Integer flagOffsetY,
		final Integer playerID, final List<String> shadingColours) throws IOException;
}