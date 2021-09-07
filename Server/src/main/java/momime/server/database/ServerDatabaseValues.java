package momime.server.database;

import momime.common.database.CommonDatabaseConstants;

/**
 * IDs from the XML database file that are needed by the server
 */
public final class ServerDatabaseValues
{
	// tile types

	/** Mountain tiles */
	public final static String TILE_TYPE_MOUNTAIN = "TT01";

	/** Hill tiles */
	public final static String TILE_TYPE_HILLS = "TT02";

	/** Forest/Desert/Swamp -TT03/04/05 - are defined in CommonDatabaseConstants */

	/** Grass tiles */
	public final static String TILE_TYPE_GRASS = "TT06";

	/** Tundra tiles */
	public final static String TILE_TYPE_TUNDRA = "TT07";

	/** Shore tiles (i.e. ocean tiles that have been converted to shore by smoothing) */
	public final static String TILE_TYPE_SHORE = "TT08";

	/** Water tiles */
	public final static String TILE_TYPE_OCEAN = "TT09";

	/** Inland river tiles */
	public final static String TILE_TYPE_RIVER = "TT10";

	/** Shore tiles with a river leading from them */
	public final static String TILE_TYPE_OCEANSIDE_RIVER_MOUTH = "TT11";

	/** Land tiles that are the first river tile from the ocean */
	public final static String TILE_TYPE_LANDSIDE_RIVER_MOUTH = "TT15";

	// buildings

	/** Default construction project for new cities */
	public final static String CITY_CONSTRUCTION_DEFAULT = CommonDatabaseConstants.BUILDING_HOUSING;

	// tax rates

	/** Default tax rate of 1 gold, 20% unrest */
	public final static String TAX_RATE_DEFAULT = "TR03";

	// damage types
	
	/** Life stealing / create undead damage */
	public final static String DAMAGE_TYPE_ID_LIFE_STEALING = "DT13";
	
	// spells

	/** Nature awareness - see whole map */
	public final static String SPELL_ID_NATURE_AWARENESS = "SP034";

	/** Awareness - see areas around enemy cities */
	public final static String SPELL_ID_AWARENESS = "SP209";
	
	// combat maps
	
	/** Grass tiles */
	public final static String COMBAT_TILE_TYPE_GRASS = "CTL01";

	/** Dips in the terrain */
	public final static String COMBAT_TILE_TYPE_DARK = "CTL02";
	
	/** Hills in the terrain */
	public final static String COMBAT_TILE_TYPE_RIDGE = "CTL03";

	/** Trees/rocks on the combat map */
	public final static String COMBAT_TILE_TERRAIN_FEATURE = "CBL01";
	
	/**
	 * Prevent instantiation
	 */
	private ServerDatabaseValues ()
	{
	}
}
