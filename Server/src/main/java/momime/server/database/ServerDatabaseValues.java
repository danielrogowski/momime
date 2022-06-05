package momime.server.database;

import momime.common.database.CommonDatabaseConstants;

/**
 * IDs from the XML database file that are needed by the server
 */
public final class ServerDatabaseValues
{
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
	
	// diplomacy
	
	/** Initial maximum gold tribute between any two wizards */
	public final static int INITIAL_MAXIMUM_GOLD_TRIBUTE = 150;
	
	/**
	 * Prevent instantiation
	 */
	private ServerDatabaseValues ()
	{
	}
}
