package momime.common.database;

import java.util.Arrays;
import java.util.List;

/**
 * XML tags used in the part of the MoM database which is sent to the client when they've chosen a game that's using a specific XML file
 */
public final class CommonDatabaseConstants
{
	/** Path and name to locate the common XSD file on the classpath */
	public final static String COMMON_XSD_LOCATION = "/momime.common.database/MoMIMECommonDatabase.xsd";

	/** Namespace of the common XSD */
	public final static String COMMON_XSD_NAMESPACE_URI = "http://momime/common/database";

	/** Path and name to locate the client XSD file on the classpath */
	public final static String CLIENT_XSD_LOCATION = "/momime.client.database/MoMIMEClientDatabase.xsd";

	/** Namespace of the client XSD */
	public final static String CLIENT_XSD_NAMESPACE_URI = "http://momime/client/database";

	/** Path and name to locate the messages XSD file on the classpath */
	public final static String MESSAGES_XSD_LOCATION = "/momime.common.messages/MoMIMEMessages.xsd";

	/** Namespace of the messages XSD */
	public final static String MESSAGES_XSD_NAMESPACE_URI = "http://momime/common/messages";

	/** Path and name to locate the client-to-server messages XSD file on the classpath */
	public final static String CTOS_MESSAGES_XSD_LOCATION = "/momime.common.messages/MoMIMEClientToServerMessages.xsd";

	/** Namespace of the client-to-server messages XSD */
	public final static String CTOS_MESSAGES_XSD_NAMESPACE_URI = "http://momime/common/messages/clienttoserver";
	
	/** Path and name to locate the server-to-client messages XSD file on the classpath */
	public final static String STOC_MESSAGES_XSD_LOCATION = "/momime.common.messages/MoMIMEServerToClientMessages.xsd";

	/** Namespace of the server-to-client messages XSD */
	public final static String STOC_MESSAGES_XSD_NAMESPACE_URI = "http://momime/common/messages/servertoclient";
	
	/** Range of each magic power slider */
	public final static int MAGIC_POWER_DISTRIBUTION_MAX = 240;
	
	/** Combat map size - hard coded for now */
	public final static int COMBAT_MAP_WIDTH = 12;
	
	/** Combat map size - hard coded for now */
	public final static int COMBAT_MAP_HEIGHT = 25;
	
	/** Distance cities are apart to get a road connecting them at the start of the game */
	public final static int CITY_SEPARATION_TO_GET_STARTER_ROADS = 10;
	
	/** Maximum units in a map cell */
	public final static int MAX_UNITS_PER_MAP_CELL = 9;
	
	/*------------------------------------------------------
	 * Special values the various XML fields can take
	 *-------------------------------------------------- */

	// wizards

	/** Special wizard ID for the raider/neutral city player */
	public final static String WIZARD_ID_RAIDERS = "RAIDERS";

	/** Special wizard ID for monsters inhabiting nodes/lairs/towers */
	public final static String WIZARD_ID_MONSTERS = "MONSTERS";

	/* unit magic realm lifeform types */

	/** normal */
	public final static String UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL= "LTN";

	/** hero */
	public final static String UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO  = "LTH";

	// magic realms / spell books
	
	/** Death books */
	public final static String PICK_ID_DEATH_BOOK = "MB02";
	
	// retorts

	/** Alchemy (magic weapons, and convert gold <-> mana without the 50% loss) */
	public final static String RETORT_ID_ALCHEMY = "RT01";

	/** Warlord retort (+1 skill level) */
	public final static String RETORT_ID_WARLORD = "RT02";

	/** Channeler retort (all spell upkeep reduced 50%) */
	public final static String RETORT_ID_CHANNELER = "RT03";

	/** Archmage, twice as hard to dispel their spells + twice as resilient to being countered  */
	public final static String RETORT_ID_ARCHMAGE = "RT04";

	/** Double hiring chance */
	public final static String RETORT_ID_FAMOUS = "RT10";
	
	/** Runemaster (2x powerful dispels) */
	public final static String RETORT_NODE_RUNEMASTER = "RT11";

	/** Half price hiring heroes, mercenaries and buying items */
	public final static String RETORT_ID_CHARISMATIC = "RT12";
	
	/** Node mastery (2x magic power from nodes) */
	public final static String RETORT_NODE_MASTERY = "RT18";
	
	// production types

	/** Rations for feeding civvies + armies */
	public final static String PRODUCTION_TYPE_ID_RATIONS = "RE01";

	/** Production for constructing buildings and units */
	public final static String PRODUCTION_TYPE_ID_PRODUCTION = "RE02";

	/** Gold */
	public final static String PRODUCTION_TYPE_ID_GOLD = "RE03";

	/** Production type for magic power (i.e. power base, NOT magic crystal production controlled by the mana staff) */
	public final static String PRODUCTION_TYPE_ID_MAGIC_POWER = "RE04";

	/** Production type for Spell research */
	public final static String PRODUCTION_TYPE_ID_RESEARCH = "RE05";

	/** Food for max city size */
	public final static String PRODUCTION_TYPE_ID_FOOD = "RE06";

	/** Bonus added to map features, i.e. Miners" guild */
	public final static String PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER = "RE08";

	/** Production type for mana crystals (i.e. stored mana, NOT magic power that is distributed to all 3 staves) */
	public final static String PRODUCTION_TYPE_ID_MANA = "RE09";

	/** Production type for skill points */
	public final static String PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT = "RE10";

	/** Production type for reductions to spell casting cost, from e.g. Chaos Mastery and quite a few others */
	public final static String PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION = "RE11";

	/** Unit upkeep reduction (from Summoner retort) */
	public final static String PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION = "RE12";

	/** Fame */
	public final static String PRODUCTION_TYPE_ID_FAME = "RE13";
	
	// tile types

	/** Forest tiles */
	public final static String TILE_TYPE_FOREST = "TT03";

	/** Desert tiles */
	public final static String TILE_TYPE_DESERT = "TT04";

	/** Swamp tiles */
	public final static String TILE_TYPE_SWAMP = "TT05";

	/** Volcano (from Raise Volcano spell, not Chaos nodes) tiles */
	public final static String TILE_TYPE_RAISE_VOLCANO = "TT16";
	
	/** Normal road */
	public final static String TILE_TYPE_NORMAL_ROAD = "TT98";
	
	/** Enchanted road */
	public final static String TILE_TYPE_ENCHANTED_ROAD = "TT99";
	
	/** Areas of the map we've never seen */
	public final static String TILE_TYPE_FOG_OF_WAR = "FOW";

	/** Areas of the map we've previously seen, and are remembering the last state that we saw them in */
	public final static String TILE_TYPE_FOG_OF_WAR_HAVE_SEEN = "FOWPARTIAL";

	// map features

	/** Uncleared Tower of Wizardry */
	public final static String FEATURE_UNCLEARED_TOWER_OF_WIZARDRY  = "MF12A";

	/** Cleared Tower of Wizardry */
	public final static String FEATURE_CLEARED_TOWER_OF_WIZARDRY = "MF12B";

	// population tasks

	/** Farmers */
	public final static String POPULATION_TASK_ID_FARMER = "PT01";

	/** Workers */
	public final static String POPULATION_TASK_ID_WORKER = "PT02";

	/** Rebels */
	public final static String POPULATION_TASK_ID_REBEL = "PT03";

	// buildings

	/** Special Trade Goods setting */
	public final static String BUILDING_TRADE_GOODS = "BL01";

	/** Special Housing setting */
	public final static String BUILDING_HOUSING = "BL02";

	/** Marks the city where newly summoned units will appear */
	public final static String BUILDING_SUMMONING_CIRCLE = "BL98";

	/** Wizard's fortress */
	public final static String BUILDING_FORTRESS = "BL99";

	// combat area effects

	/** Crusade */
	public final static String COMBAT_AREA_EFFECT_CRUSADE = "CSE158";
	
	/** Mana leak */
	public final static String COMBAT_AREA_EFFECT_ID_MANA_LEAK = "CSE170";
	
	// unit types

	/** Units summoned from spells */
	public final static String UNIT_TYPE_ID_SUMMONED = "S";
	
	// units
	
	/** Example of simplest unit to use to test roads (High men spearmen) */
	public final static String UNIT_ID_EXAMPLE = "UN105";

	// unit skills

	/** Melee attack strength */
	public final static String UNIT_ATTRIBUTE_ID_MELEE_ATTACK = "UA01";
	
	/** Ranged attack strength - can be either phys like bows/rocks or mag like priest blasts */
	public final static String UNIT_ATTRIBUTE_ID_RANGED_ATTACK = "UA02";
	
	/** Gives better chance of each sword/ranged attack striking its target */
	public final static String UNIT_ATTRIBUTE_ID_PLUS_TO_HIT = "UA03";
	
	/** Shields for defending against phys hits */
	public final static String UNIT_ATTRIBUTE_ID_DEFENCE = "UA04";
	
	/** Resistance for avoiding negative spell effects */
	public final static String UNIT_ATTRIBUTE_ID_RESISTANCE = "UA05";
	
	/** Hearts for hit points */
	public final static String UNIT_ATTRIBUTE_ID_HIT_POINTS = "UA06";
	
	/** Gives better chance of each shield blocking a hit */ 
	public final static String UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK = "UA07";

	/** Movement speed skill; speed is calculated entirely separately from the -kind- of movement, via skills like Flight or Swimming */
	public final static String UNIT_SKILL_ID_MOVEMENT_SPEED = "UA08";

	/** Sailing (specifically ships, not swimming like Lizardmen) */
	public final static String UNIT_SKILL_ID_SAILING = "USX02";
	
	/** Natural flight */
	public final static String UNIT_SKILL_ID_FLIGHT = "USX04";
	
	/** Can teleport to anywhere in combat */
	public final static String UNIT_SKILL_ID_TELEPORT = "US000";
	
	/** All skills that let the unit freely jump between planes */
	public final static List<String> UNIT_SKILL_IDS_PLANE_SHIFT = Arrays.asList ("IS09", "US004", "SS138");
			
	/** Can attack city walls as well as units */
	public final static String UNIT_SKILL_ID_WALL_CRUSHER = "US015";
	
	/** Boosts healing rate of unit stack */
	public final static String UNIT_SKILL_ID_HEALER = "US016";
	
	/** Skill for settlers creating new cities */
	public final static String UNIT_SKILL_ID_CREATE_OUTPOST = "US017";

	/** Natural invisibility */
	public final static String UNIT_SKILL_ID_INVISIBILITY = "US018";

	/** Invisibility skill added from spell */
	public final static String UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL = "SS062";
	
	/** All invisibility skills */
	public final static List<String> UNIT_SKILL_IDS_INVISIBILITY = Arrays.asList ("IS08", UNIT_SKILL_ID_INVISIBILITY, UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL);

	/** Converts melee damage dealt into life stealing stored damage type (ghouls) */
	public final static String UNIT_SKILL_ID_CREATE_UNDEAD = "US019";

	/** All regeneration skills */
	public final static List<String> UNIT_SKILL_IDS_REGENERATION = Arrays.asList ("IS01", "US024", "SS031");

	/** All true sight skills */
	public final static List<String> UNIT_SKILL_IDS_TRUE_SIGHT = Arrays.asList ("IS03", "US010", "SS131");
	
	/** Skill for priests purifying corruption */
	public final static String UNIT_SKILL_ID_PURIFY = "US025";

	/** Converts melee and ranged damage dealt into armour piercing damage resolution type (elven lords/storm giants) */
	public final static String UNIT_SKILL_ID_ARMOUR_PIERCING = "US028";
	
	/** Skill for damage dealt being returned to the attackers' HP (this is subtely different than the ability to create undead - ghouls create undead but do not recover HP from it) */
	public final static String UNIT_SKILL_ID_LIFE_STEALING = "US031";

	/** Converts melee and ranged damage dealt into illusionary damage resolution type (phantom warriors/beasts) */
	public final static String UNIT_SKILL_ID_ILLUSIONARY_ATTACK = "US035";
	
	/** Skill for engineers building roads */
	public final static String UNIT_SKILL_ID_BUILD_ROAD = "US036";

	/** Scouting range */
	public final static String UNIT_SKILL_ID_SCOUTING = "US037";
	
	/** Skill for magic and guardian spirits capturing nodes */	
	public final static String UNIT_SKILL_ID_MELD_WITH_NODE = "US040";
	
	/** Allows unit to cast spells, value of the skill specifies how MP worth of spells they can cast, e.g. Archangels able to cast 40 MP worth of life magic */
	public final static String UNIT_SKILL_ID_CASTER_UNIT = "US051";

	/** the "skill" for experience levels */
	public final static String UNIT_SKILL_ID_EXPERIENCE = "US098";

	/** Units converted to undead */
	public final static String UNIT_SKILL_ID_UNDEAD = "US122";

	/** Limit ranged attack distance penalty to -10% */ 
	public final static String UNIT_SKILL_ID_LONG_RANGE = "US125";
	
	/** Skill whose value specifies how many ranged shots a unit can fire, used both for phys ammo like arrows and mag ammo like magicians' firebolts */ 
	public final static String UNIT_SKILL_ID_RANGED_ATTACK_AMMO = "US132";

	/** Can travel through ground to anywhere in combat */
	public final static String UNIT_SKILL_ID_MERGING = "US169";
	
	/** Unit lacks graphics for multiple figures, so always just draw one */
	public final static String UNIT_SKILL_ID_DRAW_ONE_FIGURE = "US170";

	/** Hidden skill granted by Blur CSE */
	public final static String UNIT_SKILL_ID_BLUR = "CS051";
	
	// hero skills

	/** Armsmaster raises exp of normal units stacked with them */
	public final static String UNIT_SKILL_ID_ARMSMASTER = "HS03";
	
	/** Allows heroes to cast spells plus provides MP for mag heroes to use as ammo, available MP = skill level * (exp level + 1) * 2½ */
	public final static String UNIT_SKILL_ID_CASTER_HERO = "HS05";

	/** Hero contributes to wizard's fame */
	public final static String UNIT_SKILL_ID_LEGENDARY = "HS09";
	
	/** Hero adds +10 gold instead of consuming gold */
	public final static String UNIT_SKILL_ID_NOBLE = "HS12";
	
	/** Hero contributes to wizard's research */
	public final static String UNIT_SKILL_ID_SAGE = "HS14";

	/** Hero item skill that penalises target's resistance attribute by the specified amount */
	public final static String UNIT_SKILL_ID_SAVING_THROW_PENALTY = "HS15";
	
	// curses
	
	/** Unit is stuck in web and cannot move (if value > 0) and cannot fly (regardless of value) */
	public final static String UNIT_SKILL_ID_WEB = "SC005";

	/** Confusion (unit rolls random action each combat turn) */
	public final static String UNIT_SKILL_ID_CONFUSION = "SC046";
	
	/** Stasis on the first turn when it cannot even be blocked by Magic Immunity */
	public final static String UNIT_SKILL_ID_STASIS_FIRST_TURN = "SC068A";

	/** Stasis on subsequent turns when it can be cancelled by Magic Immunity or passing a resistance roll */
	public final static String UNIT_SKILL_ID_STASIS_LATER_TURNS = "SC068B";
	
	/** All stasis skills */
	public final static List<String> UNIT_SKILL_IDS_STASIS = Arrays.asList (UNIT_SKILL_ID_STASIS_FIRST_TURN, UNIT_SKILL_ID_STASIS_LATER_TURNS); 
	
	/** Possession (unit is under permanent control of caster) */
	public final static String UNIT_SKILL_ID_POSSESSION = "SC172";
	
	/**
	 * Special code used for hero item bonuses, meaning "type(s) of attack appropriate for the type of weapon", so is used to differentiate
	 * that for example an Sword of Illuisonary Attack being wielded by a hero with a Thrown Attack affects his regular melee hits only, and not
	 * the Thrown Attack, but an Axe of Illuisonary Attack would affect both.
	 */
	public final static String UNIT_SKILL_ID_ATTACK_APPROPRIATE_FOR_TYPE_OF_HERO_ITEM = "HS16";
	
	// hero items
	
	/** Special hero item bonus ID for the ability to cast a spell a number of times in combat for free */
	public final static String HERO_ITEM_BONUS_ID_SPELL_CHARGES = "IB65";
	
	// damage resolution types
	
	/** All damage resolution types where some kind of roll or check is made against the target's resistance, rather than conventional damage */
	public final static List<DamageResolutionTypeID> RESISTANCE_BASED_DAMAGE = Arrays.asList
		(DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE, DamageResolutionTypeID.SINGLE_FIGURE_RESIST_OR_DIE,
			DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, 	DamageResolutionTypeID.RESISTANCE_ROLLS, DamageResolutionTypeID.DISINTEGRATE,
			DamageResolutionTypeID.FEAR, DamageResolutionTypeID.TERROR);
	
	// spells
	
	/** Wind Mastery spell */
	public final static String SPELL_ID_WIND_MASTERY = "SP057";
	
	/** Spell Blast spell */
	public final static String SPELL_ID_SPELL_BLAST = "SP058";
	
	/** Disrupt Wall spell */
	public final static String SPELL_ID_DISRUPT_WALL = "SP082";
	
	/** Wall of Fire spell */
	public final static String SPELL_ID_WALL_OF_FIRE = "SP087";
	
	/** Warp Lightning spell */
	public final static String SPELL_ID_WARP_LIGHTNING = "SP101";
	
	/** Chaos Rift spell */
	public final static String SPELL_ID_CHAOS_RIFT = "SP110";
	
	/** Just Cause spell */
	public final static String SPELL_ID_JUST_CAUSE = "SP127";
	
	/** Shift entire stack to the other plane */
	public final static String SPELL_ID_PLANE_SHIFT = "SP132";
	
	/** Block travel between planes */
	public final static String SPELL_ID_PLANAR_SEAL = "SP135"; 
	
	/** Stream of Life doubles population growth */
	public final static String SPELL_ID_STREAM_OF_LIFE = "SP148";

	/** Dark Rituals spell */
	public final static String SPELL_ID_DARK_RITUALS = "SP163";	
	
	/** Terror spell */
	public final static String SPELL_ID_TERROR = "SP168";
	
	/** Wall of Darkness spell */
	public final static String SPELL_ID_WALL_OF_DARKNESS = "SP178";
	
	/** Famine spell */
	public final static String SPELL_ID_FAMINE = "SP189";
	
	/** Summoning circle spell */
	public final static String SPELL_ID_SUMMONING_CIRCLE = "SP203";
	
	/** Detect magic spell */
	public final static String SPELL_ID_DETECT_MAGIC = "SP206";
	
	/** Summon hero spell - used as a means to get a list of the heroes we can rescue from lairs/nodes/towers */
	public final static String SPELL_ID_SUMMON_HERO = "SP208";

	/** Spell of Mastery has a lot of special rules */
	public final static String SPELL_ID_SPELL_OF_MASTERY = "SP213";
	
	/** Spell of Return has a lot of special rules - we are always blocked from casting it even when banished (server auto casts it for us), and can never cancel casting it */
	public final static String SPELL_ID_SPELL_OF_RETURN = "SP214";
	
	// city spell effects
	
	/** Evil presence nullifies magic power and unrest benefits of religious buildings, unless the city owner has any death books */
	public final static String CITY_SPELL_EFFECT_ID_EVIL_PRESENCE = "SE183";
	
	// client graphics

	/** Tile set for the overland map */
	public final static String TILE_SET_OVERLAND_MAP = "TS01";
	
	/** Special bitmask for when smoothing is turned off */
	public final static String TILE_BITMASK_NO_SMOOTHING = "NoSmooth";
	
	/**
	 * Prevent instatiation of this class
	 */
	private CommonDatabaseConstants ()
	{
	}
}