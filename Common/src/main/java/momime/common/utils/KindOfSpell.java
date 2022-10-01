package momime.common.utils;

/**
 * Spell book sections denote the targeting rules for spells, but within a single spell book section there are different kinds of spells.
 */
public enum KindOfSpell
{
	/** Summoning spell that "summons" dead units back to life */
	RAISE_DEAD,
	
	/** Summoning spell that "summons" an item */
	CREATE_ARTIFACT,
	
	/** Normal summoning spell */
	SUMMONING,
	
	/** Overland enchantments, no subdivision of spell book section */
	OVERLAND_ENCHANTMENTS,
	
	/** City enchantments, no subdivision of spell book section */
	CITY_ENCHANTMENTS,
	
	/** Unit enchantment that acts like a summoning spell, by killing off the existing unit and summoning a new one (Lycanthropy) */
	CHANGE_UNIT_ID,
	
	/** Unit enchantments other than Lycanthropy*/
	UNIT_ENCHANTMENTS,
	
	/** Combat enchantments, no subdivision of spell book section */
	COMBAT_ENCHANTMENTS,
	
	/** City cursesno subdivision of spell book section */
	CITY_CURSES,
	
	/** Unit curses, no subdivision of spell book section */
	UNIT_CURSES,
	
	/** Attack spells that are directed at single units, and also hit the walls the unit is standing next to; or can be targeted at walls only */
	ATTACK_UNITS_AND_WALLS,
	
	/** Spell hits both units and buildings (earthquake) so must be targeted at a city, it can't hit units just walking around the map */
	ATTACK_UNITS_AND_BUILDINGS,
	
	/** Attack spells that are directed at single or multiple units */
	ATTACK_UNITS,
	
	/** Shift entire stack to the other plane */
	PLANE_SHIFT,
	
	/** Spells that heal our units */
	HEALING,
	
	/** Spells that teleport our units back to our fortress */
	RECALL,
	
	/** Enchant road */
	ENCHANT_ROAD,
	
	/** Earth lore */
	EARTH_LORE,
	
	/** Change Terrain or Raise Volcano */
	CHANGE_TILE_TYPE,
	
	/** Transmute */
	CHANGE_MAP_FEATURE,
	
	/** Warp node */
	WARP_NODE,
	
	/** Corruption */
	CORRUPTION,
	
	/** Spell is targeted only at walls */
	ATTACK_WALLS,
	
	/** Earth to mud */
	EARTH_TO_MUD,
	
	/** Dispel spells that take over the spell */
	SPELL_BINDING,
	
	/** Dispel spells that target overland enchantments */
	DISPEL_OVERLAND_ENCHANTMENTS,
	
	/** Dispel spells that target unit enchantments/curses, city enchantments/curses and combat enchantments */
	DISPEL_UNIT_CITY_COMBAT_SPELLS,
	
	/** Special spells that do not require a target at all; exact effect of the spell is simply hard coded by the spellID */
	SPECIAL_SPELLS,
	
	/** Spell Blast - this is separate from regular enemy wizard spells as it has additional validation rules -
	 * 	the wizard must be casting a spell, and we must have enough MP to blast it */
	SPELL_BLAST,
	
	/** Spells that are targeted at enemy wizards without any other special requirements */
	ENEMY_WIZARD_SPELLS;
}