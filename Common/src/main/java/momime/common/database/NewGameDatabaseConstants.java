package momime.common.database;

/**
 * XML tags used in the part of the MoM database which is sent to the client before they join a game, containing all the settings they need to be able to choose from when creating a new game
 */
public class NewGameDatabaseConstants
{
	/** The very top level node of of the new game database */
	public static final String TOP_LEVEL = "databases";

	// database

	/** Each XML database the server has found to be compatible */
	public static final String TAG_ENTITY_XML_DATABASE = "momimexmldb";

	/** The name of the XML database */
	public static final String TAG_ATTRIBUTE_XML_DATABASE_NAME = "dbname";

	// Map size
	
	/** Defines pre-defined map size settings */
	public static final String TAG_ENTITY_MAP_SIZE = "mapsize";

	/** Unique identifier for this set of pre-defined map size settings */
	public static final String TAG_ATTRIBUTE_MAP_SIZE_ID = "mapsizeid";

	/** Width of the map */
	public static final String TAG_VALUE_MAP_SIZE_WIDTH = "width";

	/** Height of the map */
	public static final String TAG_VALUE_MAP_SIZE_HEIGHT = "height";

	/** Width of each zone the map generator splits the map into when generating the initial height map */
	public static final String TAG_VALUE_MAP_SIZE_ZONE_WIDTH = "zonewidth";

	/** Height of each zone the map generator splits the map into when generating the initial height map */
	public static final String TAG_VALUE_MAP_SIZE_ZONE_HEIGHT = "zoneheight";

	/** Whether the map wraps at the left-right edge */
	public static final String TAG_VALUE_MAP_SIZE_WRAPS_LEFT_TO_RIGHT = "wrapslefttoright";

	/** Whether the map wraps at the top-bottom edge */
	public static final String TAG_VALUE_MAP_SIZE_WRAPS_TOP_TO_BOTTOM = "wrapstoptobottom";

	/** Number of Towers of Wizardry */
	public static final String TAG_VALUE_MAP_SIZE_TOWER_COUNT = "towersofwizardrycount";

	/** How far apart Towers of Wizardry must be kept */
	public static final String TAG_VALUE_MAP_SIZE_TOWER_SEPARATION = "towersofwizardryseparation";

	/** % chance that raider cities are the race chosen for the continent */
	public static final String TAG_VALUE_MAP_SIZE_CONTINENTAL_RACE_CHANCE = "continentalracechance";

	/** How far apart Cities must be kept */
	public static final String TAG_VALUE_MAP_SIZE_CITY_SEPARATION = "cityseparation";

	// Land proportion

	/** Defines pre-defined land proportion settings */
	public static final String TAG_ENTITY_LAND_PROPORTION = "landproportion";

	/** Unique identifier for this set of pre-defined land proportion settings */
	public static final String TAG_ATTRIBUTE_LAND_PROPORTION_ID = "landproportionid";

	/** % of map which is land */
	public static final String TAG_VALUE_LAND_PROPORTION_PERCENTAGE_LAND = "percentageofmapisland";

	/** % of land which is hills */
	public static final String TAG_VALUE_LAND_PROPORTION_PERCENTAGE_HILLS = "percentageoflandishills";

	/** % of hills which are mountains */
	public static final String TAG_VALUE_LAND_PROPORTION_PERCENTAGE_MOUNTAINS = "percentageofhillsaremountains";

	/** Nbr. rows from map edge where tundra can appear */
	public static final String TAG_VALUE_LAND_PROPORTION_TUNDRA_ROWS = "tundrarowcount";

	/** Nbr. Rivers on each plane */
	public static final String TAG_VALUE_LAND_PROPORTION_RIVER_COUNT = "rivercount";

	
	/** Defines pre-defined land proportion settings for each tile type */
	public static final String TAG_CHILD_ENTITY_LAND_PROPORTION_TILE_TYPE = "landproportiontiletype";

	/** Which tile type these land proportion settings are for */
	public static final String TAG_ATTRIBUTE_LAND_PROPORTION_TILE_TYPE_ID = "tiletypeid";

	/** % of land which is this terrain */
	public static final String TAG_VALUE_LAND_PROPORTION_TILE_TYPE_PERCENTAGE = "percentageofland";

	/** Approx. size of each area of this terrain */
	public static final String TAG_VALUE_LAND_PROPORTION_TILE_TYPE_AREA_TILE_COUNT = "eachareatilecount";

	
	/** Defines pre-defined land proportion settings for each plane */
	public static final String TAG_CHILD_ENTITY_LAND_PROPORTION_PLANE = "landproportionplane";

	/** Which plane these land proportion settings are for */
	public static final String TAG_ATTRIBUTE_LAND_PROPORTION_PLANE_ID = "planeid";

	/** 1 in X chance of each terrain tile gaining a special feature/mineral */
	public static final String TAG_VALUE_LAND_PROPORTION_PLANE_FEATURE_CHANCE = "featurechance";

	// Node strength

	/** Defines pre-defined node strength settings */
	public static final String TAG_ENTITY_NODE_STRENGTH = "nodestrength";

	/** Unique identifier for this set of pre-defined node strength settings */
	public static final String TAG_ATTRIBUTE_NODE_STRENGTH_ID = "nodestrengthid";

	/** 2x node aura magic power */
	public static final String TAG_VALUE_NODE_STRENGTH_DOUBLE_AURA_MAGIC_POWER = "doublenodeauramagicpower";


	/** Defines pre-defined node strength settings for each plane */
	public static final String TAG_CHILD_ENTITY_NODE_STRENGTH_PLANE = "nodestrengthplane";

	/** Which plane these node strength settings are for */
	public static final String TAG_ATTRIBUTE_NODE_STRENGTH_PLANE_ID = "planeid";

	/** Number of nodes on Plane */
	public static final String TAG_VALUE_NODE_STRENGTH_PLANE_NODE_COUNT = "numberofnodesonplane";

	/** Min. map cells node aura covers */
	public static final String TAG_VALUE_NODE_STRENGTH_PLANE_AURA_SQUARES_MIN = "nodeaurasquaresminimum";

	/** Max. map cells node aura covers */
	public static final String TAG_VALUE_NODE_STRENGTH_PLANE_AURA_SQUARES_MAX = "nodeaurasquaresmaximum";

	// Difficulty level
	
	/** Defines pre-defined difficulty level settings */
	public static final String TAG_ENTITY_DIFFICULTY_LEVEL = "difficultylevel";

	/** Unique identifier for this set of pre-defined difficulty level settings */
	public static final String TAG_ATTRIBUTE_DIFFICULTY_LEVEL_ID = "difficultylevelid";

	/** Number of spell picks that human players get at the start of the game */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_HUMAN_SPELL_PICKS = "humanspellpicks";

	/** Number of spell picks that AI players get at the start of the game */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_AI_SPELL_PICKS = "aispellpicks";

	/** Amount of gold that human players get at the start of the game */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_HUMAN_STARTING_GOLD = "humanstartinggold";

	/** Amount of gold that AI players get at the start of the game */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_AI_STARTING_GOLD = "aistartinggold";

	/** True to allow human players to choose their own picks; false to force them to choose one of the pre-defined wizards */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_CUSTOM_WIZARDS = "customwizards";

	/** True to allow each wizard to be chosen only once, e.g. in a 14 player game, this would ensure every wizard was in play */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_EACH_WIZARD_ONLY_ONCE = "eachwizardonlyonce";

	/** Nbr. normal lairs */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_NORMAL_LAIR_COUNT = "normallaircount";

	/** Nbr. weak lairs */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_WEAK_LAIR_COUNT = "weaklaircount";

	/** Tower Min. monster strength */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_TOWER_MONSTER_MIN = "towermonstersminimum";

	/** Tower Max. monster strength */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_TOWER_MONSTER_MAX = "towermonstersmaximum";

	/** Tower Min. treasure value */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_TOWER_TREASURE_MIN = "towertreasureminimum";

	/** Tower Max. treasure value */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_TOWER_TREASURE_MAX = "towertreasuremaximum";

	/** Nbr. raider cities */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_RAIDER_CITY_COUNT = "raidercitycount";

	/** Min. starting size of raider cities */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_RAIDER_CITY_START_SIZE_MIN = "raidercitystartsizemin";

	/** Max. starting size of raider cities */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_RAIDER_CITY_START_SIZE_MAX = "raidercitystartsizemax";

	/** Amount raider cities are allowed to grow */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_RAIDER_CITY_GROWTH_CAP = "raidercitygrowthcap";

	/** Starting size of wizard cities */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_WIZARD_CITY_START_SIZE = "wizardcitystartsize";

	/** Max. city size */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_CITY_MAX_SIZE = "citymaxsize";
	

	/** Defines pre-defined difficulty level settings for each plane */
	public static final String TAG_CHILD_ENTITY_DIFFICULTY_LEVEL_PLANE = "difficultylevelplane";

	/** Which plane these difficulty level settings are for */
	public static final String TAG_ATTRIBUTE_DIFFICULTY_LEVEL_PLANE_ID = "planeid";

	/** Norm. Lair Min. monster strength */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_PLANE_NORMAL_LAIR_MONSTER_MIN = "normallairmonstersminimum";

	/** Norm. Lair Max. monster strength */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_PLANE_NORMAL_LAIR_MONSTER_MAX = "normallairmonstersmaximum";

	/** Norm. Lair Min. treasure value */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_PLANE_NORMAL_LAIR_TREASURE_MIN = "normallairtreasureminimum";

	/** Norm. Lair Max. treasure value */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_PLANE_NORMAL_LAIR_TREASURE_MAX = "normallairtreasuremaximum";

	/** Weak Lair Min. monster strength */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_PLANE_WEAK_LAIR_MONSTER_MIN = "weaklairmonstersminimum";

	/** Weak Lair Max. monster strength */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_PLANE_WEAK_LAIR_MONSTER_MAX = "weaklairmonstersmaximum";

	/** Weak Lair Min. treasure value */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_PLANE_WEAK_LAIR_TREASURE_MIN = "weaklairtreasureminimum";

	/** Weak Lair Max. treasure value */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_PLANE_WEAK_LAIR_TREASURE_MAX = "weaklairtreasuremaximum";
	

	/** Defines pre-defined difficulty level settings for each node strength */
	public static final String TAG_CHILD_ENTITY_DIFFICULTY_LEVEL_NODE_STRENGTH = "difficultylevelnodestrength";

	/** Which node strength these difficulty level settings are for */
	public static final String TAG_ATTRIBUTE_DIFFICULTY_LEVEL_NODE_STRENGTH_NODESTR_ID = "nodestrengthid";

	/** Which plane these difficulty level settings are for */
	public static final String TAG_ATTRIBUTE_DIFFICULTY_LEVEL_NODE_STRENGTH_PLANE_ID = "planeid";

	/** Min. monster strength element */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_NODE_STRENGTH_MONSTER_MIN = "monstersminimum";

	/** Max. monster strength element */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_NODE_STRENGTH_MONSTER_MAX = "monstersmaximum";

	/** Min. treasure value element */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_NODE_STRENGTH_TREASURE_MIN = "treasureminimum";

	/** Max. treasure value element */
	public static final String TAG_VALUE_DIFFICULTY_LEVEL_NODE_STRENGTH_TREASURE_MAX = "treasuremaximum";

	// Fog of war settings
	
	/** Defines pre-defined fog of war settings */
	public static final String TAG_ENTITY_FOG_OF_WAR_SETTING = "fogofwarsetting";

	/** Unique identifier for this set of pre-defined fog of war settings */
	public static final String TAG_ATTRIBUTE_FOG_OF_WAR_SETTING_ID = "fogofwarsettingid";

	/** Fog of war setting for Terrain and Node Auras */
	public static final String TAG_VALUE_FOG_OF_WAR_SETTING_TERRAIN_NODE_AURAS = "terrainandnodeauras";

	/** Fog of war setting for Cities, Combat Spells and CAEs */
	public static final String TAG_VALUE_FOG_OF_WAR_SETTING_CITIES_SPELLS_CAES = "citiesspellsandcombatareaeffects";

	/** Fog of war setting for Units */
	public static final String TAG_VALUE_FOG_OF_WAR_SETTING_UNITS = "units";

	/** Whether players can see what enemy cities are constructing */
	public static final String TAG_VALUE_FOG_OF_WAR_SETTING_SEE_ENEMY_CITY_CONSTRUCTION = "seeenemycityconstruction";

	// Unit settings

	/** Defines pre-defined unit settings */
	public static final String TAG_ENTITY_UNIT_SETTING = "unitsetting";

	/** Unique identifier for this set of pre-defined unit settings */
	public static final String TAG_ATTRIBUTE_UNIT_SETTING_ID = "unitsettingid";

	/** Maximum number of units in a map cell */
	public static final String TAG_VALUE_UNIT_SETTING_MAX_PER_MAP_CELL = "unitspermapcell";

	/** Whether we can temporarily exceed the maximum number of units in a map cell during combat with combat summons like Fire Elemental or Phantom Warriors */
	public static final String TAG_VALUE_UNIT_SETTING_EXCEED_MAX_DURING_COMBAT = "canexceedmaximumunitsduringcombat";

	/** Maximum number of heroes hired at once */
	public static final String TAG_VALUE_UNIT_SETTING_HERO_COUNT = "maxheroes";

	/** If true, hero random skills are rolled at the start of the game so you cannot alter hero stats by reloading; if false, if you get a hero with back picks, you can reload the game and try again */
	public static final String TAG_VALUE_UNIT_SETTING_ROLL_HERO_STATS_AT_START = "rollheroskillsatstartofgame";

	// Spell settings

	/** Defines pre-defined spell settings */
	public static final String TAG_ENTITY_SPELL_SETTING = "spellsetting";

	/** Unique identifier for this set of pre-defined spell settings */
	public static final String TAG_ATTRIBUTE_SPELL_SETTING_ID = "spellsettingid";

	/** Whether to allow switching which spell we're researching while in the middle of researching a spell */
	public static final String TAG_VALUE_SPELL_SETTING_SWITCH_RESEARCH = "switchresearch";

	/** How many spell books of a certain magic realm we need before start getting casting cost reductions & bonuses to research (default is 8) */
	public static final String TAG_VALUE_SPELL_SETTING_BOOKS_TO_OBTAIN_FIRST_REDUCTION = "spellbookstoobtainfirstreduction";

	/** Percentage casting cost reduction for each additional spell book (10% in original MoM, 8% in MoM IME recommended settings) */
	public static final String TAG_VALUE_SPELL_SETTING_EACH_BOOK_CASTING_REDUCTION = "spellbookscastingreduction";

	/** Maximum percentage casting cost reduction (100% in original MoM so you can get totally free spells, 90% in MoM IME recommended settings) */
	public static final String TAG_VALUE_SPELL_SETTING_BOOK_CASTING_REDUCTION_CAP = "spellbookscastingreductioncap";

	/** Whether casting cost reductions are added or multiplied together (using "multiplied" means you get decreasing benefit with each subsequent book) */
	public static final String TAG_VALUE_SPELL_SETTING_BOOK_CASTING_REDUCTION_COMBINATION = "spellbookscastingreductioncombination";

	/** Percentage boost to research for each additional spell book (10% in original MoM and in MoM IME recommended settings) */
	public static final String TAG_VALUE_SPELL_SETTING_EACH_BOOK_RESEARCH_BONUS = "spellbooksresearchbonus";

	/** Maximum percentage boost to research (very high value (bascially uncapped) in original MoM and in MoM IME recommended settings) */
	public static final String TAG_VALUE_SPELL_SETTING_BOOK_RESEARCH_BONUS_CAP = "spellbooksresearchbonuscap";

	/** Whether research bonuses are added or multiplied together (using "multiplied" means you get increasing benefit with each subsequent book) */
	public static final String TAG_VALUE_SPELL_SETTING_BOOK_RESEARCH_BONUS_COMBINATION = "spellbooksresearchbonuscombination";
}
