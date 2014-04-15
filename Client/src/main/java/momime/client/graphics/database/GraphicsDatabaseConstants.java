package momime.client.graphics.database;

/**
 * XML tags and values used in MoM IME graphics file
 */
public final class GraphicsDatabaseConstants
{
	/** Path and name to locate the graphics XSD file */
	public static final String GRAPHICS_XSD_LOCATION = "/momime.client.graphics/MoMIMEGraphicsDatabase.xsd";
	
	/** Tile set for the overland map */
	public static final String VALUE_TILE_SET_OVERLAND_MAP = "TS01";
	
	/** Tile set for combat maps */
	public static final String VALUE_TILE_SET_COMBAT_MAP = "TS02";
	
	/** Special bitmask for when smoothing is turned off */
	public static final String VALUE_TILE_BITMASK_NO_SMOOTHING = "NoSmooth";
	
	/** City flag image */
	public static final String IMAGE_CITY_FLAG = "/momime.client.graphics/overland/cities/cityFlag.png";

	/**
	 * Prevent instatiation of this class
	 */
	private GraphicsDatabaseConstants ()
	{
	}
}
