package momime.client.ui.fonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

import momime.client.ui.fonts.FontFactory;
import momime.client.ui.fonts.FontFactoryImpl;

/**
 * Creates fonts that match those defined in the Spring XML.
 * These are declared here rather than in the individual unit tests so that if the fonts defined in
 * the Spring XML ever change, there's only one place to change it to make all the unit tests match.
 */
public final class CreateFontsForTests
{
	/** Factory used to load fonts */
	private final static FontFactory fontFactory = new FontFactoryImpl ();

	/** TTF upon which medium+large fonts are based */
	private static Font mediumFontTTF;
	
	/** Large font */
	private static Font largeFont;
	
	/** TTF upon which small font is based */
	private static Font smallFontTTF;
	
	/** Small font */
	private static Font smallFont;

	/**
	 * @return TTF upon which medium+large fonts are based
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	private final static Font getMediumFontTTF () throws IOException, FontFormatException
	{
		if (mediumFontTTF == null)
			mediumFontTTF = fontFactory.loadFont ("/momime.client.ui.fonts/KingthingsPetrock.ttf");
		
		return mediumFontTTF;
	}

	/**
	 * @return Large font
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	public final static Font getLargeFont () throws IOException, FontFormatException
	{
		if (largeFont == null)
			largeFont = getMediumFontTTF ().deriveFont (20.0f);
		
		return largeFont;
	}
	
	/**
	 * @return TTF upon which small font is based
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	private final static Font getSmallFontTTF () throws IOException, FontFormatException
	{
		if (smallFontTTF == null)
			smallFontTTF = fontFactory.loadFont ("/momime.client.ui.fonts/Aclonica.ttf");
		
		return smallFontTTF;
	}
	
	/**
	 * @return Small font
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	public final static Font getSmallFont () throws IOException, FontFormatException
	{
		if (smallFont == null)
			smallFont = getSmallFontTTF ().deriveFont (10.0f);
		
		return smallFont;
	}
}