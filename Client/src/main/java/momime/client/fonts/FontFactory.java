package momime.client.fonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

/**
 * Loads fonts from the classpath so they can be read by Spring
 */
public interface FontFactory
{
	/**
	 * @param filename Name of font resource on classpath to load
	 * @return Font object
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	public Font loadFont (final String filename) throws IOException, FontFormatException;
}
