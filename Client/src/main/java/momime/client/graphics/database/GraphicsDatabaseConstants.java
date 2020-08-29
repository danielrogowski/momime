package momime.client.graphics.database;

import java.awt.Dimension;

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
	public static final String TILE_SET_OVERLAND_MAP = "TS01";
	
	/** Tile set for combat maps */
	public static final String TILE_SET_COMBAT_MAP = "TS02";
	
	/** Special bitmask for when smoothing is turned off */
	public static final String TILE_BITMASK_NO_SMOOTHING = "NoSmooth";
	
	/**
	 * Walk action.  Avoid referencing this wherever possible - combat actions should be obtained by calling determineCombatActionID ().
	 * The only reason this constant exists is for the options screen, so we can display units before we join a game
	 * i.e. before we know what movement skills they have.
	 */
	public static final String UNIT_COMBAT_ACTION_WALK = "WALK";
	
	/** Melee attack action */
	public static final String UNIT_COMBAT_ACTION_MELEE_ATTACK = "MELEE";
	
	/** Ranged attack action */
	public static final String UNIT_COMBAT_ACTION_RANGED_ATTACK = "RANGED";
	
	/** Sample of a grass tile, before obtaining the client XML so we can derive this properly from an AvailableUnit */
	public static final String SAMPLE_GRASS_TILE = "/momime.client.graphics/combat/terrain/arcanus/default/standard/00000000a.png";
	
	/** Sample of an ocean tile, before obtaining the client XML so we can derive this properly from an AvailableUnit */
	public static final String SAMPLE_OCEAN_TILE = "/momime.client.graphics/combat/terrain/arcanus/ocean/standard/00000000a-frame1.png";

	/** Controls how overland enchantment images fit inside the mirror */ 
	public static final int IMAGE_MIRROR_X_OFFSET = 9;
	
	/** Controls how overland enchantment images fit inside the mirror */
	public static final int IMAGE_MIRROR_Y_OFFSET = 8;
	
	/** Size that all wizard portraits are stretched to, whether standard or custom photos */
	public static final Dimension WIZARD_PORTRAIT_SIZE = new Dimension (218, 250);	
	
	/** Play list containing all the music for the overland map */
	public static final String PLAY_LIST_OVERLAND_MUSIC = "OVERLAND";
	
	/** Which wizard's combat music to play if an opponent wizard has no music defined, or is a custom wizard with no standardPhotoID */
	public static final String WIZARD_ID_GENERAL_COMBAT_MUSIC = "RAIDERS";
	
	/** Banishing animation single blast */
	public static final String ANIM_WIZARD_BANISHED_SINGLE_BLAST = "BANISHING_BLAST_SINGLE";

	/** Banishing animation double blast */
	public static final String ANIM_WIZARD_BANISHED_DOUBLE_BLAST = "BANISHING_BLAST_DOUBLE";
	
	/** Winning animation spinning worlds */
	public static final String ANIM_WIZARD_WON_WORLDS = "WORLDS_SPINNING";

	/** Winning animation sparkles */
	public static final String ANIM_WIZARD_WON_SPARKLES = "WORLDS_SPARKLES";
	
	/**
	 * Prevent instatiation of this class
	 */
	private GraphicsDatabaseConstants ()
	{
	}
}