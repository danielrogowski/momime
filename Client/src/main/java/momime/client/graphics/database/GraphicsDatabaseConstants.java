package momime.client.graphics.database;

/**
 * XML tags and values used in MoM IME graphics file
 */
public final class GraphicsDatabaseConstants
{
	/** Path and name to locate the graphics XSD file */
	public static final String GRAPHICS_XSD_LOCATION = "/momime.client.graphics/MoMIMEGraphicsDatabase.xsd";
	
	/** Path and name to locate the graphics XSD file that doesn't include the link to the server XML */
	public static final String GRAPHICS_XSD_LOCATION_NO_SERVER_LINK = "/momime.client.graphics/MoMIMEGraphicsDatabase_NoServerXsdLink.xsd";
	
	/** Tile set for the overland map */
	public static final String VALUE_TILE_SET_OVERLAND_MAP = "TS01";
	
	/** Tile set for combat maps */
	public static final String VALUE_TILE_SET_COMBAT_MAP = "TS02";
	
	/** Special bitmask for when smoothing is turned off */
	public static final String VALUE_TILE_BITMASK_NO_SMOOTHING = "NoSmooth";
	
	/** Walk action - used for drawing units on the unit info panel, change construction screen and options screen */
	public static final String UNIT_COMBAT_ACTION_WALK = "WALK";
	
	/** Sample of a grass tile, before obtaining the client XML so we can derive this properly from an AvailableUnit */
	public static final String SAMPLE_GRASS_TILE = "/momime.client.graphics/combat/terrain/arcanus/default/standard/00000000a.png";
	
	/** Sample of an ocean tile, before obtaining the client XML so we can derive this properly from an AvailableUnit */
	public static final String SAMPLE_OCEAN_TILE = "/momime.client.graphics/combat/terrain/arcanus/ocean/standard/00000000a-frame1.png";
	
	/** Play list containing all the music for the overland map */
	public static final String PLAY_LIST_OVERLAND_MUSIC = "OVERLAND";
	
	/** Which wizard's combat music to play if an opponent wizard has no music defined, or is a custom wizard with no standardPhotoID */
	public static final String WIZARD_ID_GENERAL_COMBAT_MUSIC = "RAIDERS";
	
	/**
	 * Prevent instatiation of this class
	 */
	private GraphicsDatabaseConstants ()
	{
	}
}