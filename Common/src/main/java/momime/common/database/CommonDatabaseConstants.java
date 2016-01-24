package momime.common.database;

/**
 * XML tags used in the part of the MoM database which is sent to the client when they've chosen a game that's using a specific XML file
 */
public final class CommonDatabaseConstants
{
	/** Path and name to locate the common XSD file on the classpath */
	public static final String COMMON_XSD_LOCATION = "/momime.common.database/MoMIMECommonDatabase.xsd";

	/** Namespace of the common XSD */
	public static final String COMMON_XSD_NAMESPACE_URI = "http://momime/common/database";

	/** Path and name to locate the client XSD file on the classpath */
	public static final String CLIENT_XSD_LOCATION = "/momime.client.database/MoMIMEClientDatabase.xsd";

	/** Namespace of the client XSD */
	public static final String CLIENT_XSD_NAMESPACE_URI = "http://momime/client/database";

	/** Path and name to locate the messages XSD file on the classpath */
	public static final String MESSAGES_XSD_LOCATION = "/momime.common.messages/MoMIMEMessages.xsd";

	/** Namespace of the messages XSD */
	public static final String MESSAGES_XSD_NAMESPACE_URI = "http://momime/common/messages";

	/** Path and name to locate the client-to-server messages XSD file on the classpath */
	public static final String CTOS_MESSAGES_XSD_LOCATION = "/momime.common.messages/MoMIMEClientToServerMessages.xsd";

	/** Namespace of the client-to-server messages XSD */
	public static final String CTOS_MESSAGES_XSD_NAMESPACE_URI = "http://momime/common/messages/clienttoserver";
	
	/** Path and name to locate the server-to-client messages XSD file on the classpath */
	public static final String STOC_MESSAGES_XSD_LOCATION = "/momime.common.messages/MoMIMEServerToClientMessages.xsd";

	/** Namespace of the server-to-client messages XSD */
	public static final String STOC_MESSAGES_XSD_NAMESPACE_URI = "http://momime/common/messages/servertoclient";
	
	/** Range of each magic power slider */
	public static final int MAGIC_POWER_DISTRIBUTION_MAX = 240;
	
	/** Combat map size - hard coded for now */
	public final static int COMBAT_MAP_WIDTH = 12;
	
	/** Combat map size - hard coded for now */
	public final static int COMBAT_MAP_HEIGHT = 25;
	
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
	public static final String UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL= "LTN";

	/** hero */
	public static final String UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO  = "LTH";

	// retorts

	/** Alchemy (magic weapons, and convert gold <-> mana without the 50% loss) */
	public static final String RETORT_ID_ALCHEMY = "RT01";

	/** Warlord retort (+1 skill level) */
	public static final String RETORT_ID_WARLORD = "RT02";

	/** Channeler retort (all spell upkeep reduced 50%) */
	public static final String RETORT_ID_CHANNELER = "RT03";

	// production types

	/** Rations for feeding civvies + armies */
	public static final String PRODUCTION_TYPE_ID_RATIONS = "RE01";

	/** Production for constructing buildings and units */
	public static final String PRODUCTION_TYPE_ID_PRODUCTION = "RE02";

	/** Gold */
	public static final String PRODUCTION_TYPE_ID_GOLD = "RE03";

	/** Production type for magic power (i.e. power base, NOT magic crystal production controlled by the mana staff) */
	public static final String PRODUCTION_TYPE_ID_MAGIC_POWER = "RE04";

	/** Production type for Spell research */
	public static final String PRODUCTION_TYPE_ID_RESEARCH = "RE05";

	/** Food for max city size */
	public static final String PRODUCTION_TYPE_ID_FOOD = "RE06";

	/** Bonus added to map features, i.e. Miners" guild */
	public static final String PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER = "RE08";

	/** Production type for mana crystals (i.e. stored mana, NOT magic power that is distributed to all 3 staves) */
	public static final String PRODUCTION_TYPE_ID_MANA = "RE09";

	/** Production type for skill points */
	public static final String PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT = "RE10";

	/** Production type for reductions to spell casting cost, from e.g. Chaos Mastery and quite a few others */
	public static final String PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION = "RE11";

	/** Unit upkeep reduction (from Summoner retort) */
	public static final String PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION = "RE12";

	// tile types

	/** Forest tiles */
	public static final String TILE_TYPE_FOREST = "TT03";

	/** Desert tiles */
	public static final String TILE_TYPE_DESERT = "TT04";

	/** Swamp tiles */
	public static final String TILE_TYPE_SWAMP = "TT05";

	/** Areas of the map we've never seen */
	public static final String TILE_TYPE_FOG_OF_WAR = "FOW";

	/** Areas of the map we've previously seen, and are remembering the last state that we saw them in */
	public static final String TILE_TYPE_FOG_OF_WAR_HAVE_SEEN = "FOWPARTIAL";

	// map features

	/** Uncleared Tower of Wizardry */
	public static final String FEATURE_UNCLEARED_TOWER_OF_WIZARDRY  = "MF12A";

	/** Cleared Tower of Wizardry */
	public static final String FEATURE_CLEARED_TOWER_OF_WIZARDRY = "MF12B";

	// population tasks

	/** Farmers */
	public static final String POPULATION_TASK_ID_FARMER = "PT01";

	/** Workers */
	public static final String POPULATION_TASK_ID_WORKER = "PT02";

	/** Rebels */
	public static final String POPULATION_TASK_ID_REBEL = "PT03";

	// buildings

	/** Special Trade Goods setting */
	public static final String BUILDING_TRADE_GOODS = "BL01";

	/** Special Housing setting */
	public static final String BUILDING_HOUSING = "BL02";

	/** Marks the city where newly summoned units will appear */
	public static final String BUILDING_SUMMONING_CIRCLE = "BL98";

	/** Wizard's fortress */
	public static final String BUILDING_FORTRESS = "BL99";

	// combat area effects

	/** Crusade */
	public static final String COMBAT_AREA_EFFECT_CRUSADE = "CSE158";
	
	// unit types

	/** Units summoned from spells */
	public static final String UNIT_TYPE_ID_SUMMONED = "S";

	// unit skills

	/** Melee attack strength */
	public static final String UNIT_ATTRIBUTE_ID_MELEE_ATTACK = "UA01";
	
	/** Ranged attack strength - can be either phys like bows/rocks or mag like priest blasts */
	public static final String UNIT_ATTRIBUTE_ID_RANGED_ATTACK = "UA02";
	
	/** Gives better chance of each sword/ranged attack striking its target */
	public static final String UNIT_ATTRIBUTE_ID_PLUS_TO_HIT = "UA03";
	
	/** Shields for defending against phys hits */
	public static final String UNIT_ATTRIBUTE_ID_DEFENCE = "UA04";
	
	/** Resistance for avoiding negative spell effects */
	public static final String UNIT_ATTRIBUTE_ID_RESISTANCE = "UA05";
	
	/** Hearts for hit points */
	public static final String UNIT_ATTRIBUTE_ID_HIT_POINTS = "UA06";
	
	/** Gives better chance of each shield blocking a hit */ 
	public static final String UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK = "UA07";

	/** Movement speed skill; speed is calculated entirely separately from the -kind- of movement, via skills like Flight or Swimming */
	public static final String UNIT_SKILL_ID_MOVEMENT_SPEED = "UA08";
	
	/** Skill for settlers creating new cities */
	public static final String UNIT_SKILL_ID_CREATE_OUTPOST = "US017";
	
	/** Skill for magic and guardian spirits capturing nodes */	
	public static final String UNIT_SKILL_ID_MELD_WITH_NODE = "US040";
	
	/** the "skill" for experience levels */
	public static final String UNIT_SKILL_ID_EXPERIENCE = "US098";

	/** Allows unit to cast spells, value of the skill specifies how MP worth of spells they can cast, e.g. Archangels able to cast 40 MP worth of life magic */
	public static final String UNIT_SKILL_ID_CASTER_UNIT = "US051";

	/** Units converted to undead */
	public final static String UNIT_SKILL_ID_UNDEAD = "US122";
	
	/** Skill whose value specifies how many ranged shots a unit can fire, used both for phys ammo like arrows and mag ammo like magicians' firebolts */ 
	public static final String UNIT_SKILL_ID_RANGED_ATTACK_AMMO = "US132";

	// hero skills
	
	/** Allows heroes to cast spells plus provides MP for mag heroes to use as ammo, available MP = skill level * (exp level + 1) * 2½ */
	public static final String UNIT_SKILL_ID_CASTER_HERO = "HS05";

	/**
	 * Special code used for hero item bonuses, meaning "type(s) of attack appropriate for the type of weapon", so is used to differentiate
	 * that for example an Sword of Illuisonary Attack being wielded by a hero with a Thrown Attack affects his regular melee hits only, and not
	 * the Thrown Attack, but an Axe of Illuisonary Attack would affect both.
	 */
	public static final String UNIT_SKILL_ID_ATTACK_APPROPRIATE_FOR_TYPE_OF_HERO_ITEM = "HS16";
	
	// hero items
	
	/** Special hero item bonus ID for the ability to cast a spell a number of times in combat for free */
	public static final String HERO_ITEM_BONUS_ID_SPELL_CHARGES = "IB65";
	
	// spells
	
	/** Summon hero spell - used as a means to get a list of the heroes we can rescue from lairs/nodes/towers */
	public static final String SPELL_ID_SUMMON_HERO = "SP208";
	
	/**
	 * Prevent instatiation of this class
	 */
	private CommonDatabaseConstants ()
	{
	}
}