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

	/** Medium font */
	private static Font mediumFont;
	
	/** TTF upon which small font is based */
	private static Font smallFontTTF;
	
	/** Small font */
	private static Font smallFont;

	/** TTF upon which tiny font is based */
	private static Font tinyFontTTF;
	
	/** Tiny font */
	private static Font tinyFont;

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
	 * @return Medium font
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	public final static Font getMediumFont () throws IOException, FontFormatException
	{
		if (mediumFont == null)
			mediumFont = getMediumFontTTF ().deriveFont (15.0f);
		
		return mediumFont;
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

	/**
	 * @return TTF upon which tiny font is based
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	private final static Font getTinyFontTTF () throws IOException, FontFormatException
	{
		if (tinyFontTTF == null)
			tinyFontTTF = fontFactory.loadFont ("/momime.client.ui.fonts/DreamOrphanage.ttf");
		
		return tinyFontTTF;
	}
	
	/**
	 * @return Tiny font
	 * @throws IOException If the resource cannot be located
	 * @throws FontFormatException If the format of the .ttf file is invalid
	 */
	public final static Font getTinyFont () throws IOException, FontFormatException
	{
		if (tinyFont == null)
			tinyFont = getTinyFontTTF ().deriveFont (11.0f);
		
		return tinyFont;
	}
}