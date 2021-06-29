package momime.client.graphics.database;

import java.awt.Dimension;

/**
 * XML tags and values used in MoM IME graphics file
 */
public final class GraphicsDatabaseConstants
{
	/** Path and name to locate the graphics XSD file */
	public final static String GRAPHICS_XSD_LOCATION = "/momime.client.graphics/MoMIMEGraphicsDatabase.xsd";
	
	/** Tile set for combat maps */
	public final static String TILE_SET_COMBAT_MAP = "TS02";
	
	/**
	 * Walk action.  Avoid referencing this wherever possible - combat actions should be obtained by calling determineCombatActionID ().
	 * The only reason this constant exists is for the options screen, so we can display units before we join a game
	 * i.e. before we know what movement skills they have.
	 */
	public final static String UNIT_COMBAT_ACTION_WALK = "WALK";
	
	/** Melee attack action */
	public final static String UNIT_COMBAT_ACTION_MELEE_ATTACK = "MELEE";
	
	/** Ranged attack action */
	public final static String UNIT_COMBAT_ACTION_RANGED_ATTACK = "RANGED";
	
	/** Sample of a grass tile, before obtaining the client XML so we can derive this properly from an AvailableUnit */
	public final static String SAMPLE_GRASS_TILE = "/momime.client.graphics/combat/terrain/arcanus/default/standard/00000000a.png";
	
	/** Sample of an ocean tile, before obtaining the client XML so we can derive this properly from an AvailableUnit */
	public final static String SAMPLE_OCEAN_TILE = "/momime.client.graphics/combat/terrain/arcanus/ocean/standard/00000000a-frame1.png";
	
	/** Mirror showing wizard's face then the overland enchantment they cast */
	public final static String OVERLAND_ENCHANTMENTS_MIRROR = "/momime.client.graphics/ui/mirror/mirror.png";
	
	/** Square background drawn behind units on the overland map */
	public final static String UNIT_BACKGROUND = "/momime.client.graphics/ui/overland/unitBackground.png";

	/** Controls how overland enchantment images fit inside the mirror */ 
	public final static int IMAGE_MIRROR_X_OFFSET = 9;
	
	/** Controls how overland enchantment images fit inside the mirror */
	public final static int IMAGE_MIRROR_Y_OFFSET = 8;
	
	/** Size that all wizard portraits are stretched to, whether standard or custom photos */
	public final static Dimension WIZARD_PORTRAIT_SIZE = new Dimension (218, 250);	
	
	/** Play list containing all the music for the overland map */
	public final static String PLAY_LIST_OVERLAND_MUSIC = "OVERLAND";
	
	/** Which wizard's combat music to play if an opponent wizard has no music defined, or is a custom wizard with no standardPhotoID */
	public final static String WIZARD_ID_GENERAL_COMBAT_MUSIC = "RAIDERS";
	
	/** Banishing animation single blast */
	public final static String ANIM_WIZARD_BANISHED_SINGLE_BLAST = "BANISHING_BLAST_SINGLE";

	/** Banishing animation double blast */
	public final static String ANIM_WIZARD_BANISHED_DOUBLE_BLAST = "BANISHING_BLAST_DOUBLE";
	
	/** Winning animation spinning worlds */
	public final static String ANIM_WIZARD_WON_WORLDS = "WORLDS_SPINNING";

	/** Winning animation sparkles */
	public final static String ANIM_WIZARD_WON_SPARKLES = "WORLDS_SPARKLES";

	/** Animation of cloud appearing above Wizards' Fortress */
	public final static String ANIM_SPELL_OF_MASTERY_START = "SPELL_OF_MASTERY_START";
	
	/** Animation of portal which sucks up enemy wizards */
	public final static String ANIM_SPELL_OF_MASTERY_PORTAL = "SPELL_OF_MASTERY_PORTAL";
	
	/**
	 * Prevent instatiation of this class
	 */
	private GraphicsDatabaseConstants ()
	{
	}
}