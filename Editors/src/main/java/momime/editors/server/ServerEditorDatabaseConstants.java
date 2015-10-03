package momime.editors.server;

/**
 * Now the main code uses JAXB, it doesn't need any of these constants anymore, so they're kept within the editor
 */
public final class ServerEditorDatabaseConstants
{
	// tileType

	/**
	 * Defines the types of terrain tiles that make up the map, e.g. Grassland, Forest, Mountains, Ocean, Nodes,
	 * plus some special entries which aren't really "terrain" - roads and fog of war - for defining movement rate rules
	 */
	public static final String TAG_ENTITY_TILE_TYPE = "tileType";

	/** Defines the identifier for this tile type */
	public static final String TAG_ATTRIBUTE_TILE_TYPE_ID = "tileTypeID";

	/** Description of a terrain tile type */
	public static final String TAG_VALUE_TILE_TYPE_DESCRIPTION = "tileTypeDescription";
	
	// pick

	/** pick entity */
	public static final String TAG_ENTITY_PICK = "pick";

	/** Uniquely identifies each pick */
	public static final String TAG_ATTRIBUTE_PICK_ID = "pickID";

	/** Description of a spell book or retort that can be chosen at the start of the game */
	public static final String TAG_VALUE_PICK_DESCRIPTION = "pickDescription";
	
	// pickType - pickTypeCount - spellCount

	/**
	 * For a particular number of a certain pick type, lists how many spells of each rank we get for free and can research,
	 * e.g. with 2 spell books, we have 5x Common, 2x Uncommon and 1x Rare spell available to search, and 1x Common spell free to start with
	 */
	public static final String TAG_GRANDCHILD_ENTITY_SPELL_COUNT = "spellCount";

	/** The spell rank for which we get some available and/or free spells from having a certain number of spell books */
	public static final String TAG_ATTRIBUTE_SPELL_COUNT_RANK = "spellRank";

	/** The number of spells of a particular rank we have available to research in our spell book for a given number of spell books */
	public static final String TAG_VALUE_SPELLS_AVAILABLE = "spellsAvailable";

	/** The number of spells of a particular rank we get for free at the start of the game for a given number of spell books */
	public static final String TAG_VALUE_SPELLS_FREE_AT_START = "spellsFreeAtStart";
	
	// wizard - wizardPickCount - wizardPick

	/** wizardPick grandchild entity */
	public static final String TAG_GRANDCHILD_ENTITY_WIZARD_PICK = "wizardPick";

	/** How many of the book the wizard picks */
	public static final String TAG_VALUE_QUANTITY = "quantity";

	// race - raceCannotBuild

	/** Lists all the buildings that a particular race cannot build */
	public static final String TAG_CHILD_ENTITY_RACE_CANNOT_BUILD = "raceCannotBuild";

	/** The building ID of this building that this race cannot build */
	public static final String TAG_ATTRIBUTE_RACE_CANNOT_BUILD_BUILDING_ID = "cannotBuildBuildingID";
	
	// race - racePopulationTask
	
	/** How much each farmer, worker, etc. produces */
	// public static final String TAG_CHILD_ENTITY_RACE_POPULATION_TASK = "racePopulationTask";
	
	/** Image of race civilians performing each task */
	// public static final String TAG_VALUE_CIVILIAN_IMAGE_FILE = "civilianImageFile";

	// movementRateRule

	/**
	 * Defines a list of rules for deciding the movement rate of units over different types of terrain
	 * Rules are evaluated in order, and the first one found that applies will dictate the units' movement over this type of terrain
	 */
	public static final String TAG_ENTITY_MOVEMENT_RATE_RULE = "movementRateRule";

	/** The tile type ID for which this movment rate rule applies; if blank then this movement rate rule applies to all types of terrain (e.g. flying or wraithform) */
	public static final String TAG_VALUE_MOVEMENT_RATE_RULE_TILE_TYPE = "tileTypeID";

	/** The unit skill ID that a unit must have in order for this movment rate rule to apply - this is mandatory */
	public static final String TAG_VALUE_MOVEMENT_RATE_RULE_UNIT_SKILL = "unitSkillID";
	
	/** A unit skill ID that another unit in the same stack having may help us move - optional */
	public static final String TAG_VALUE_MOVEMENT_RATE_RULE_UNIT_STACK_SKILL = "unitStackSkillID";

	/** How many movement points this unit will take to move over this type of terrain if this movement rule applies */
	public static final String TAG_VALUE_MOVEMENT_RATE_RULE_DOUBLE_MOVEMENT = "doubleMovement";

	// building

	/**
	 * Lists all the different buildings that can be constructed in cities, plus a couple of special entries
	 * (the special settings Housing & Trade Goods, the Wizards' Fortress and Summoning Circle)
	 */
	public static final String TAG_ENTITY_BUILDING = "building";

	/** Defines the identifier for this building */
	public static final String TAG_ATTRIBUTE_BUILDING_ID = "buildingID";

	/** Name for each building */
	public static final String TAG_VALUE_BUILDING_NAME = "buildingName";

	/** Longer help text for each building */
	public static final String TAG_VALUE_BUILDING_HELP_TEXT = "buildingHelpText";

	// building - buildingPrerequisite

	/** For a particular building, defines the buildings you must already have in order to construct it, e.g. to build a Farmers' Market you must have a Granary and a Marketplace */
	public static final String TAG_CHILD_ENTITY_BUILDING_PREREQUISITE = "buildingPrerequisite";

	/** The building ID of the required building */
	public static final String TAG_ATTRIBUTE_BUILDING_PREREQUISITE_ID = "prerequisiteID";
	
	// unitSkill

	/**
	 * Defines all the unit skills available, e.g. First Strike, Flame Breath, Flying, Scouting, as well as skill-like effects from spells (both enchantments and curses),
	 * e.g. Bless, Endurance, Flame Blade, Confusion, Black Sleep - i.e. the list of special icons that appear on the lower half of the unit info screen.
	 */
	public static final String TAG_ENTITY_UNIT_SKILL = "unitSkill";

	/** Defines the identifier for this unit skill */
	public static final String TAG_ATTRIBUTE_UNIT_SKILL_ID = "unitSkillID";

	/** Description of each unit skill */
	public static final String TAG_VALUE_UNIT_SKILL_DESCRIPTION = "unitSkillDescription";

	// unit

	/** unit entity */
	public static final String TAG_ENTITY_UNIT = "unit";

	/** Uniquely identifies each unit */
	public static final String TAG_ATTRIBUTE_UNIT_ID = "unitID";

	/** Name for each unit */
	public static final String TAG_VALUE_UNIT_NAME = "unitName";

	// unit - unitHasSkill

	/** Defines which skills a particular unit has, e.g. First Strike, Flame Breath, Flying, Scouting */
	public static final String TAG_CHILD_ENTITY_UNIT_HAS_SKILL = "unitHasSkill";

	/** The unit skill ID of this skill that the unit has */
	public static final String TAG_ATTRIBUTE_UNIT_HAS_SKILL_ID = "unitSkillID";
	
	// hero

	/** Hero names */
	public static final String TAG_ENTITY_HERO = "hero";

	/** Uniquely identifies each hero name */
	public static final String TAG_ATTRIBUTE_HERO_NAME_ID = "heroNameID";

	/** Name of the hero */
	public static final String TAG_VALUE_HERO_NAME = "heroName";

	// spell rank

	/** Defines spell ranks, i.e. Common, Uncommon, Rare and Very Rare */
	public static final String TAG_ENTITY_SPELL_RANK = "spellRank";

	/** Uniquely identifies each spell rank */
	public static final String TAG_ATTRIBUTE_SPELL_RANK_ID = "spellRankID";

	/** Description of each spell rank (e.g. Common, Uncommon, Rare or Very Rare) */
	public static final String TAG_VALUE_SPELL_RANK_DESCRIPTION = "spellRankDescription";

	// spell

	/** Spell */
	public static final String TAG_ENTITY_SPELL = "spell";

	/** Defines the identifier for this spell */
	public static final String TAG_ATTRIBUTE_SPELL_ID = "spellID";

	/** Name of this spell */
	public static final String TAG_VALUE_SPELL_NAME = "spellName";

	/** Longer description for this spell */
	public static final String TAG_VALUE_SPELL_DESCRIPTION = "spellDescription";

	/** Full help text for this spell */
	public static final String TAG_VALUE_SPELL_HELP_TEXT = "spellHelpText";
	
	// spell - summonedUnit

	/** For summoning spells, defines all possible units that can be summoned by the spell */
	public static final String TAG_CHILD_ENTITY_SPELL_SUMMONED_UNIT = "summonedUnit";

	/** The unit ID of the unit which this spell may summon */
	public static final String TAG_ATTRIBUTE_SUMMONED_UNIT_ID = "summonedUnitID";
	
	/**
	 * Prevent instatiation of this class
	 */
	private ServerEditorDatabaseConstants ()
	{
	}
}