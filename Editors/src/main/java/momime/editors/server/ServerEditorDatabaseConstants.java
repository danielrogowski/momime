package momime.editors.server;

/**
 * Now the main code uses JAXB, it doesn't need any of these constants anymore, so they're kept within the editor
 */
public final class ServerEditorDatabaseConstants
{
	/** For languageText entries, used all over the place */
	public final static String TAG_VALUE_TEXT = "text";
	
	// tileType

	/**
	 * Defines the types of terrain tiles that make up the map, e.g. Grassland, Forest, Mountains, Ocean, Nodes,
	 * plus some special entries which aren't really "terrain" - roads and fog of war - for defining movement rate rules
	 */
	public final static String TAG_ENTITY_TILE_TYPE = "tileType";

	/** Defines the identifier for this tile type */
	public final static String TAG_ATTRIBUTE_TILE_TYPE_ID = "tileTypeID";

	/** Description of a terrain tile type */
	public final static String TAG_VALUE_TILE_TYPE_DESCRIPTION = "tileTypeDescription";
	
	// pick

	/** pick entity */
	public final static String TAG_ENTITY_PICK = "pick";

	/** Uniquely identifies each pick */
	public final static String TAG_ATTRIBUTE_PICK_ID = "pickID";

	/** Description of a spell book or retort that can be chosen at the start of the game */
	public final static String TAG_VALUE_PICK_DESCRIPTION_SINGULAR = "pickDescriptionSingular";
	
	// pickType - pickTypeCount - spellCount

	/**
	 * For a particular number of a certain pick type, lists how many spells of each rank we get for free and can research,
	 * e.g. with 2 spell books, we have 5x Common, 2x Uncommon and 1x Rare spell available to search, and 1x Common spell free to start with
	 */
	public final static String TAG_GRANDCHILD_ENTITY_SPELL_COUNT = "spellCount";

	/** The spell rank for which we get some available and/or free spells from having a certain number of spell books */
	public final static String TAG_ATTRIBUTE_SPELL_COUNT_RANK = "spellRank";

	/** The number of spells of a particular rank we have available to research in our spell book for a given number of spell books */
	public final static String TAG_VALUE_SPELLS_AVAILABLE = "spellsAvailable";

	/** The number of spells of a particular rank we get for free at the start of the game for a given number of spell books */
	public final static String TAG_VALUE_SPELLS_FREE_AT_START = "spellsFreeAtStart";

	/** Combat casting cost */
	public final static String TAG_VALUE_SPELL_COMBAT_CASTING_COST = "combatCastingCost";
	
	// wizard - wizardPickCount - wizardPick

	/** wizardPick grandchild entity */
	public final static String TAG_GRANDCHILD_ENTITY_WIZARD_PICK = "wizardPick";

	/** How many of the book the wizard picks */
	public final static String TAG_VALUE_QUANTITY = "quantity";

	// race - raceCannotBuild

	/** Lists all the buildings that a particular race cannot build */
	public final static String TAG_CHILD_ENTITY_RACE_CANNOT_BUILD = "raceCannotBuild";

	/** The building ID of this building that this race cannot build */
	public final static String TAG_ATTRIBUTE_RACE_CANNOT_BUILD_BUILDING_ID = "cannotBuildBuildingID";
	
	// race - racePopulationTask
	
	/** How much each farmer, worker, etc. produces */
	// public final static String TAG_CHILD_ENTITY_RACE_POPULATION_TASK = "racePopulationTask";
	
	/** Image of race civilians performing each task */
	// public final static String TAG_VALUE_CIVILIAN_IMAGE_FILE = "civilianImageFile";

	// movementRateRule

	/**
	 * Defines a list of rules for deciding the movement rate of units over different types of terrain
	 * Rules are evaluated in order, and the first one found that applies will dictate the units' movement over this type of terrain
	 */
	public final static String TAG_ENTITY_MOVEMENT_RATE_RULE = "movementRateRule";

	/** The tile type ID for which this movment rate rule applies; if blank then this movement rate rule applies to all types of terrain (e.g. flying or wraithform) */
	public final static String TAG_VALUE_MOVEMENT_RATE_RULE_TILE_TYPE = "tileTypeID";

	/** The unit skill ID that a unit must have in order for this movment rate rule to apply - this is mandatory */
	public final static String TAG_VALUE_MOVEMENT_RATE_RULE_UNIT_SKILL = "unitSkillID";
	
	/** A unit skill ID that another unit in the same stack having may help us move - optional */
	public final static String TAG_VALUE_MOVEMENT_RATE_RULE_UNIT_STACK_SKILL = "unitStackSkillID";

	/** How many movement points this unit will take to move over this type of terrain if this movement rule applies */
	public final static String TAG_VALUE_MOVEMENT_RATE_RULE_DOUBLE_MOVEMENT = "doubleMovement";

	// building

	/**
	 * Lists all the different buildings that can be constructed in cities, plus a couple of special entries
	 * (the special settings Housing & Trade Goods, the Wizards' Fortress and Summoning Circle)
	 */
	public final static String TAG_ENTITY_BUILDING = "building";

	/** Defines the identifier for this building */
	public final static String TAG_ATTRIBUTE_BUILDING_ID = "buildingID";

	/** Name for each building */
	public final static String TAG_VALUE_BUILDING_NAME = "buildingName";

	/** Longer help text for each building */
	public final static String TAG_VALUE_BUILDING_HELP_TEXT = "buildingHelpText";

	// building - buildingPrerequisite

	/** For a particular building, defines the buildings you must already have in order to construct it, e.g. to build a Farmers' Market you must have a Granary and a Marketplace */
	public final static String TAG_CHILD_ENTITY_BUILDING_PREREQUISITE = "buildingPrerequisite";

	// unitSkill

	/**
	 * Defines all the unit skills available, e.g. First Strike, Flame Breath, Flying, Scouting, as well as skill-like effects from spells (both enchantments and curses),
	 * e.g. Bless, Endurance, Flame Blade, Confusion, Black Sleep - i.e. the list of special icons that appear on the lower half of the unit info screen.
	 */
	public final static String TAG_ENTITY_UNIT_SKILL = "unitSkill";

	/** Defines the identifier for this unit skill */
	public final static String TAG_ATTRIBUTE_UNIT_SKILL_ID = "unitSkillID";

	/** Description of each unit skill */
	public final static String TAG_VALUE_UNIT_SKILL_DESCRIPTION = "unitSkillDescription";

	// unit

	/** unit entity */
	public final static String TAG_ENTITY_UNIT = "unit";

	/** Uniquely identifies each unit */
	public final static String TAG_ATTRIBUTE_UNIT_ID = "unitID";

	/** Name for each unit */
	public final static String TAG_VALUE_UNIT_NAME = "unitName";

	// unit - unitHasSkill

	/** Defines which skills a particular unit has, e.g. First Strike, Flame Breath, Flying, Scouting */
	public final static String TAG_CHILD_ENTITY_UNIT_HAS_SKILL = "unitHasSkill";

	/** The unit skill ID of this skill that the unit has */
	public final static String TAG_ATTRIBUTE_UNIT_HAS_SKILL_ID = "unitSkillID";
	
	// hero

	/** Hero names */
	public final static String TAG_ENTITY_HERO = "hero";

	/** Uniquely identifies each hero name */
	public final static String TAG_ATTRIBUTE_HERO_NAME_ID = "heroNameID";

	/** Name of the hero */
	public final static String TAG_VALUE_HERO_NAME = "heroName";

	// spell rank

	/** Defines spell ranks, i.e. Common, Uncommon, Rare and Very Rare */
	public final static String TAG_ENTITY_SPELL_RANK = "spellRank";

	/** Uniquely identifies each spell rank */
	public final static String TAG_ATTRIBUTE_SPELL_RANK_ID = "spellRankID";

	/** Description of each spell rank (e.g. Common, Uncommon, Rare or Very Rare) */
	public final static String TAG_VALUE_SPELL_RANK_DESCRIPTION = "spellRankDescription";

	// spell

	/** Spell */
	public final static String TAG_ENTITY_SPELL = "spell";

	/** Defines the identifier for this spell */
	public final static String TAG_ATTRIBUTE_SPELL_ID = "spellID";

	/** Name of this spell */
	public final static String TAG_VALUE_SPELL_NAME = "spellName";

	/** Longer description for this spell */
	public final static String TAG_VALUE_SPELL_DESCRIPTION = "spellDescription";

	/** Full help text for this spell */
	public final static String TAG_VALUE_SPELL_HELP_TEXT = "spellHelpText";
	
	// spell - summonedUnit

	/** For summoning spells, defines all possible units that can be summoned by the spell */
	public final static String TAG_CHILD_ENTITY_SPELL_SUMMONED_UNIT = "summonedUnit";
	
	// heroItemBonus
	
	/** Lists all possible bonuses that can appear on hero items */
	public final static String TAG_ENTITY_HERO_ITEM_BONUS = "heroItemBonus";
	
	/** Uniquely identifies each hero item bonus */
	public final static String TAG_ATTRIBUTE_HERO_ITEM_BONUS_ID = "heroItemBonusID";

	/** Crafting cost of each bonus */
	public final static String TAG_VALUE_HERO_ITEM_BONUS_CRAFTING_COST = "bonusCraftingCost";

	/** Is crafting cost of the bonus higher for misc items? */
	public final static String TAG_VALUE_HERO_ITEM_BONUS_CRAFTING_COST_MULTIPLIER_APPLES = "craftingCostMultiplierApplies";

	/** Description of the bonus */
	public final static String TAG_VALUE_HERO_ITEM_BONUS_DESCRIPTION = "heroItemBonusDescription";
	
	/** Lists the actual skill boosts that a particular bonus gives */
	public final static String TAG_CHILD_ENTITY_HERO_ITEM_BONUS_STAT = "heroItemBonusStat";

	/** The number of skill points bonus provided */
	public final static String TAG_VALUE_UNIT_SKILL_VALUE = "unitSkillValue";

	/** Spell books you must have in order to be able to put a particular bonus onto a hero item */
	public final static String TAG_CHILD_ENTITY_HERO_ITEM_BONUS_PREREQ = "heroItemBonusPrerequisite";

	// heroItemType
	
	/** Lists all possible types of hero items (Swords, Maces and so on) */
	public final static String TAG_ENTITY_HERO_ITEM_TYPE = "heroItemType";

	/** Uniquely identifies each hero item type */
	public final static String TAG_ATTRIBUTE_HERO_ITEM_TYPE_ID = "heroItemTypeID";
	
	/** Cost to make the base item, before any bonuses are added to it */
	public final static String TAG_VALUE_HERO_ITEM_TYPE_BASE_CRAFTING_COST = "baseCraftingCost";

	/** Lists the actual skill boosts that a particular bonus gives */
	public final static String TAG_CHILD_ENTITY_HERO_ITEM_TYPE_ALLOWED_BONUS = "heroItemTypeAllowedBonus";
	
	// heroItem
	
	/** Predefined hero items */
	public final static String TAG_ENTITY_HERO_ITEM = "heroItem";
	
	/** Name of the item */
	public final static String TAG_VALUE_HERO_ITEM_NAME = "heroItemName";
	
	/** Image number */
	public final static String TAG_VALUE_HERO_ITEM_IMAGE_NUMBER = "heroItemImageNumber";
	
	/** Number of spell charges */
	public final static String TAG_VALUE_HERO_ITEM_SPELL_CHARGES = "spellChargeCount";
	
	/** Chosen bonus */
	public final static String TAG_CHILD_ENTITY_HERO_ITEM_CHOSEN_BONUS = "heroItemChosenBonus";
	
	// wizardPersonality
	
	/** AI wizard personalities */
	public final static String TAG_ENTITY_WIZARD_PERSONALITY = "wizardPersonality";
	
	/** Uniquely identifies each AI wizard personality */
	public final static String TAG_ATTRIBUTE_WIZARD_PERSONALITY_ID = "wizardPersonalityID";
	
	/** Variants of initial meeting phrase for a personality */
	public final static String TAG_CHILD_ENTITY_INITIAL_MEETING_PHRASE = "initialMeetingPhrase";

	/** Uniquely identifies each phrase variant */
	public final static String TAG_ATTRIBUTE_VARIANT_NUMBER = "variantNumber";
	
	/** Phrase variant in each language */
	public final static String TAG_GRANDCHILD_ENTITY_TEXT_VARIANT = "textVariant";
	
	/** Which language it is in */
	public final static String TAG_ATTRIBUTE_LANGUAGE = "language";
	
	/**
	 * Prevent instatiation of this class
	 */
	private ServerEditorDatabaseConstants ()
	{
	}
}