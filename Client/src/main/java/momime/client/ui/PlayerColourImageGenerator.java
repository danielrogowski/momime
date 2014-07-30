package momime.client.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Various items like unit backgrounds and city flags are displayed in the player's colour.
 * This caches the coloured images so that we don't have to recreate a multiplied colour image every time.
 */
public interface PlayerColourImageGenerator
{
	/**
	 * @param playerID Unit owner player ID
	 * @return Unit background image in their correct colour 
	 * @throws IOException If there is a problem loading the background image
	 */
	public BufferedImage getUnitBackgroundImage (final int playerID) throws IOException;

	/**
	 * @param playerID City owner player ID
	 * @return City flag image in their correct colour 
	 * @throws IOException If there is a problem loading the flag image
	 */
	public BufferedImage getCityFlagImage (final int playerID) throws IOException;
}