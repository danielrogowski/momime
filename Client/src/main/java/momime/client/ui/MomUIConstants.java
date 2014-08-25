package momime.client.ui;

import java.awt.Color;

/**
 * Colour constants used by Mom-style buttons, labels and so on 
 */
public final class MomUIConstants
{
	/** Closest possible match to the gold font colours from the original MoM */
	public static final Color GOLD = new Color (0xFCC864);
	
	/** Dull gold, for deselected buttons (got this colour from the Mana column heading on the magic sliders screen) */
	public static final Color DULL_GOLD = new Color (0xBC7C14);

	/** Green from the Research column heading on the magic sliders screen */
	public static final Color GREEN = new Color (0x388C38);

	/** Red from the Research column heading on the magic sliders screen */
	public static final Color RED = new Color (0xD01C00);
	
	/** Dark red used for some fonts in the original */
	public static final Color DARK_RED = new Color (0x5C1010);
	
	/** Color.GRAY is too bright and Color.DARK_GRAY is too dark */
	public static final Color GRAY = new Color (0x606060);
	
	/** Closest possible match to the silver font colours from the original MoM */
	public static final Color SILVER = new Color (0xD8DCEC);
	
	/** Dark brown foreground text on buttons */
	public static final Color DARK_BROWN = new Color (0x312918);
	
	/** Light brown background text on buttons */
	public static final Color LIGHT_BROWN = new Color (0xA5846B);

	/** Selected item in list boxes and such */
	public static final Color SELECTED = new Color (0xFF6060);
	
	/** Colour of fntMediumBlue font from the Delphi client */
	public static final Color AQUA = new Color (0xBDEFEF);
	
	/** Colour of the 'Overland Enchantments' heading on the magic sliders screen */
	public static final Color DULL_AQUA = new Color (0x527BA5);
	
	/** Colour used for text on the grey buttons */
	public static final Color DARK_GRAY = new Color (0x34281C);
	
	/** Colour used for text on the grey buttons */
	public static final Color LIGHT_GRAY = new Color (0xD3CBC7);
	
	/** Transparent colour */
	public static final Color TRANSPARENT = new Color (0, 0, 0, 0);
	
	/**
	 * Prevent instantiation
	 */
	private MomUIConstants ()
	{
	}
}