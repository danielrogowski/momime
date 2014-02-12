package momime.client.ui.fonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads fonts from the classpath so they can be read by Spring
 */
public final class FontFactoryImpl implements FontFactory
{
	/**
	 * @param filename Name of font resource on classpath to load
	 * @return Font object
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	@Override
	public final Font loadFont (final String filename) throws IOException, FontFormatException
	{
		final Font font;
		try (final InputStream in = getClass ().getResourceAsStream (filename))
		{
			if (in == null)
				throw new IOException ("loadFont cannot find font resource named \"" + filename + "\"");
			
			font = Font.createFont (Font.TRUETYPE_FONT, in);
		}
		
		return font;
	}
}
