package momime.client.ui;

import java.awt.Color;

/**
 * Colour constants used by Mom-style buttons, labels and so on 
 */
public final class MomUIConstants
{
	/** Closest possible match to the gold font colours from the original MoM */
	public static final Color GOLD = new Color (0xFCC864);
	
	/** Dull gold, for deselected buttons */
	public static final Color DULL_GOLD = new Color (0xA88542);
	
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
	
	/**
	 * Prevent instantiation
	 */
	private MomUIConstants ()
	{
	}
}