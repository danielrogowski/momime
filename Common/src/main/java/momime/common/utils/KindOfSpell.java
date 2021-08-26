package momime.common.utils;

/**
 * Spell book sections denote the targeting rules for spells, but within a single spell book section there are different kinds of spells.
 */
public enum KindOfSpell
{
	/** Summoning spell that "summons" dead units back to life */
	RAISE_DEAD,
	
	/** Normal summoning spell */
	SUMMONING,
	
	/** Overland enchantments, no subdivision of spell book section */
	OVERLAND_ENCHANTMENTS,
	
	/** City enchantments, no subdivision of spell book section */
	CITY_ENCHANTMENTS,
	
	/** Unit enchantments, no subdivision of spell book section */
	UNIT_ENCHANTMENTS,
	
	/** Combat enchantments, no subdivision of spell book section */
	COMBAT_ENCHANTMENTS,
	
	/** City cursesno subdivision of spell book section */
	CITY_CURSES,
	
	/** Unit curses, no subdivision of spell book section */
	UNIT_CURSES,
	
	/** Attack spells that are directed at single or multiple units */
	ATTACK_UNITS,
	
	/** Attack spells that are directed at single units, and also hit the walls the unit is standing next to; or can be targeted at walls only */
	ATTACK_UNITS_AND_WALLS,
	
	/** Spells that heal our units */
	HEALING,
	
	/** Spells that teleport our units back to our fortress */
	RECALL,
	
	/** Enchant road */
	ENCHANT_ROAD,
	
	/** Earth lore */
	EARTH_LORE,
	
	/** Corruption */
	CORRUPTION,
	
	/** Earth to mud */
	EARTH_TO_MUD,
	
	/** Spell is targeted only at walls */
	ATTACK_WALLS,
	
	/** Dispel spells that take over the spell */
	SPELL_BINDING,
	
	/** Dispel spells that target overland enchantments */
	DISPEL_OVERLAND_ENCHANTMENTS,
	
	/** Dispel spells that target unit enchantments/curses, city enchantments/curses and combat enchantments */
	DISPEL_UNIT_CITY_COMBAT_SPELLS,
	
	/** Special spells that do not require a target at all; exact effect of the spell is simply hard coded by the spellID */
	SPECIAL_SPELLS;
}