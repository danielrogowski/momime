package momime.common.database;

/**
 * XML tags used in the part of the MoM database which is sent to the client when they've chosen a game that's using a specific XML file
 */
public final class CommonDatabaseConstants
{
	/** Version string - used to build the namespaces of the XSD/XML files */
	public static final String MOM_IME_VERSION = "v0_9_4";

	/** Path and name to locate the common XSD file on the classpath */
	public static final String NEW_GAME_XSD_LOCATION = "/momime.common.database/MoMIMENewGameDatabase.xsd";

	/** Namespace of the common XSD */
	public static final String NEW_GAME_XSD_NAMESPACE_URI = "http://momime/common/database/newgame/" + MOM_IME_VERSION;

	/** Path and name to locate the common XSD file on the classpath */
	public static final String COMMON_XSD_LOCATION = "/momime.common.database/MoMIMECommonDatabase.xsd";

	/** Namespace of the common XSD */
	public static final String COMMON_XSD_NAMESPACE_URI = "http://momime/common/database/" + MOM_IME_VERSION;

	/*------------------------------------------------------
	 * Special values the various XML fields can take
	 *-------------------------------------------------- */

	// wizards

	/** Special wizard ID for the raider/neutral city player */
	public static final String WIZARD_ID_RAIDERS = "RAIDERS";

	/** Special wizard ID for monsters inhabiting nodes/lairs/towers */
	public static final String WIZARD_ID_MONSTERS = "MONSTERS";

	/* unit magic realm lifeform types */

	/** normal */
	public static final String VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL= "LTN";

	/** hero */
	public static final String VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO  = "LTH";

	// retorts

	/** Alchemy (magic weapons, and convert gold <-> mana without the 50% loss) */
	public static final String VALUE_RETORT_ID_ALCHEMY = "RT01";

	/** Warlord retort (+1 skill level) */
	public static final String VALUE_RETORT_ID_WARLORD = "RT02";

	/** Channeler retort (all spell upkeep reduced 50%) */
	public static final String VALUE_RETORT_ID_CHANNELER = "RT03";

	// production types

	/** Rations for feeding civvies + armies */
	public static final String VALUE_PRODUCTION_TYPE_ID_RATIONS = "RE01";

	/** Production for constructing buildings and units */
	public static final String VALUE_PRODUCTION_TYPE_ID_PRODUCTION = "RE02";

	/** Gold */
	public static final String VALUE_PRODUCTION_TYPE_ID_GOLD = "RE03";

	/** Production type for magic power (i.e. power base, NOT magic crystal production controlled by the mana staff) */
	public static final String VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER = "RE04";

	/** Production type for Spell research */
	public static final String VALUE_PRODUCTION_TYPE_ID_RESEARCH = "RE05";

	/** Food for max city size */
	public static final String VALUE_PRODUCTION_TYPE_ID_FOOD = "RE06";

	/** Bonus added to map features, i.e. Miners" guild */
	public static final String VALUE_PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER = "RE08";

	/** Production type for mana crystals (i.e. stored mana, NOT magic power that is distributed to all 3 staves) */
	public static final String VALUE_PRODUCTION_TYPE_ID_MANA = "RE09";

	/** Production type for skill points */
	public static final String VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT = "RE10";

	/** Production type for reductions to spell casting cost, from e.g. Chaos Mastery and quite a few others */
	public static final String VALUE_PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION = "RE11";

	/** Unit upkeep reduction (from Summoner retort) */
	public static final String VALUE_PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION = "RE12";

	// tile types

	/** Forest tiles */
	public static final String VALUE_TILE_TYPE_FOREST = "TT03";

	/** Desert tiles */
	public static final String VALUE_TILE_TYPE_DESERT = "TT04";

	/** Swamp tiles */
	public static final String VALUE_TILE_TYPE_SWAMP = "TT05";

	/** Areas of the map we've never seen */
	public static final String VALUE_TILE_TYPE_FOG_OF_WAR = "FOW";

	/** Areas of the map we've previously seen, and are remembering the last state that we saw them in */
	public static final String VALUE_TILE_TYPE_FOG_OF_WAR_HAVE_SEEN = "FOWPARTIAL";

	// map features

	/** Uncleared Tower of Wizardry */
	public static final String VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY  = "MF12A";

	/** Cleared Tower of Wizardry */
	public static final String VALUE_FEATURE_CLEARED_TOWER_OF_WIZARDRY = "MF12B";

	// population tasks

	/** Farmers */
	public static final String VALUE_POPULATION_TASK_ID_FARMER = "PT01";

	/** Workers */
	public static final String VALUE_POPULATION_TASK_ID_WORKER = "PT02";

	/** Rebels */
	public static final String VALUE_POPULATION_TASK_ID_REBEL = "PT03";

	// buildings

	/** Special Trade Goods setting */
	public static final String VALUE_BUILDING_TRADE_GOODS = "BL01";

	/** Special Housing setting */
	public static final String VALUE_BUILDING_HOUSING = "BL02";

	/** Marks the city where newly summoned units will appear */
	public static final String VALUE_BUILDING_SUMMONING_CIRCLE = "BL98";

	/** Wizard's fortress */
	public static final String VALUE_BUILDING_FORTRESS = "BL99";

	// unit types

	/** Units summoned from spells */
	public static final String VALUE_UNIT_TYPE_ID_SUMMONED = "S";

	// unit skills

	/** the "skill" for experience levels */
	public static final String VALUE_UNIT_SKILL_ID_EXPERIENCE = "US098";

	// spell book sections

	/** Spells in spell book but not researched and not in the list of 8 researchable spells */
	public static final String SPELL_BOOK_SECTION_UNKNOWN_SPELLS = "SC99";

	/** Spells in spell book, not researched, but in the list of 8 researchable spells */
	public static final String SPELL_BOOK_SECTION_RESEARCH_SPELLS = "SC98";

	/** Spells not in spell book but possible to get from lair/node/tower battles or from trading or from the ruins of a wizard's fortress */
	public static final String SPELL_BOOK_SECTION_NOT_IN_SPELL_BOOK = null;

	/** Spells in the summoning section of the spell book */
	public static final String SPELL_BOOK_SECTION_SUMMONING = "SC01";

	/** Spells in the overland section of the spell book */
	public static final String SPELL_BOOK_SECTION_OVERLAND_ENCHANTMENTS = "SC02";

	/** Spells in the city enchantments section of the spell book */
	public static final String SPELL_BOOK_SECTION_CITY_ENCHANTMENTS = "SC03";

	/** Spells in the unit enchantments section of the spell book */
	public static final String SPELL_BOOK_SECTION_UNIT_ENCHANTMENTS = "SC04";

	/** Spells in the city curses section of the spell book */
	public static final String SPELL_BOOK_SECTION_CITY_CURSES = "SC06";

	/** Spells in the unit curses section of the spell book */
	public static final String SPELL_BOOK_SECTION_UNIT_CURSES = "SC07";

	// combat area effects

	/** Crusade */
	public static final String COMBAT_AREA_EFFECT_CRUSADE = "CSE158";

	//public static final String VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK          = "UA01";
	//public static final String VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK         = "UA02";
	//public static final String VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_HIT           = "UA03";
	//public static final String VALUE_UNIT_ATTRIBUTE_ID_DEFENCE               = "UA04";
	//public static final String VALUE_UNIT_ATTRIBUTE_ID_RESISTANCE            = "UA05";
	//public static final String VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS            = "UA06";
	//public static final String VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK         = "UA07";

	//public static final String VALUE_UNIT_SKILL_ID_CREATE_OUTPOST            = "US017";
	//public static final String VALUE_UNIT_SKILL_ID_MELD_WITH_NODE            = "US040";
	//public static final String VALUE_UNIT_SKILL_ID_CASTER_UNIT               = "US051";
	//public static final String VALUE_UNIT_SKILL_ID_RANGED_ATTACK_AMMO        = "US132";

	//public static final String VALUE_UNIT_SKILL_ID_CASTER_HERO               = "HS05";

	//public static final String SPELL_BOOK_SECTION_COMBAT_ENCHANTMENTS        = "SC05";

	/**
	 * Prevent instatiation of this class
	 */
	private CommonDatabaseConstants ()
	{
	}
}
