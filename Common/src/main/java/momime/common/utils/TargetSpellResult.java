package momime.common.utils;

/**
 * Possible outcomes when we check if a particular city or unit is a valid target for a spell
 */
public enum TargetSpellResult
{
	/** City or unit is a valid target for this spell */
	VALID_TARGET,
	
	/** Can't cast a beneficial enchantment on an enemy city or unit */
	ENCHANTING_OR_HEALING_ENEMY,
	
	/** Can't raise dead an enemy unit, if the spell doesn't explicitly allow this */
	RAISING_ENEMY,
	
	/** Can't cast a curse on our own city or unit, or target an attack spell like fire bolt on it */
	CURSING_OR_ATTACKING_OWN,
	
	/** Spell has no spellEffectIDs defined at all */
	NO_SPELL_EFFECT_IDS_DEFINED,

	/** Spell has spellEffectIDs defined, but they're all already cast on this city or unit */
	ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS,
	
	/** This spell creates a building (wall of stone, summoning circle or move fortress, null) and the target city already has that building */
	CITY_ALREADY_HAS_BUILDING,
	
	/** There is no city at the specified location */
	NO_CITY_HERE,

	/** Spell can't be targetted on this magic realm/lifeform type of unit, e.g. Star Fires can only be targetted on Chaos+Death units */
	UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE,
	
	/** Spell rolls against resistance, and the target unit has too high resistance to possibly be affected by it */
	TOO_HIGH_RESISTANCE,
	
	/** The target is completely immune to this type of spell */
	IMMUNE,
	
	/** Units combatLocation doesn't match the combat the spell is targetted at */
	UNIT_NOT_IN_EXPECTED_COMBAT,
	
	/** Unit's status is not ALIVE */
	UNIT_DEAD,
	
	/** Unit's status is not DEAD (used for raise dead-type spells) */
	UNIT_NOT_DEAD,
	
	/** Trying to heal a unit that has taken no damage */
	UNDAMAGED,

	/** Trying to heal a unit that has taken only permanent damage */
	PERMANENTLY_DAMAGED,
	
	/** Trying to heal a Death or Undead creature */
	UNHEALABLE_LIFEFORM_TYPE,
	
	/** Trying to dispel a target that has no enemy spells cast on it */
	NOTHING_TO_DISPEL,
	
	/** Spell can only be targetted against locations that we can actually see */
	CANNOT_SEE_TARGET,
	
	/** Spell cannot be targetted at certain tile types */
	INVALID_TILE_TYPE,
	
	/** Can only disjunct overland enchantments */
	OVERLAND_ENCHANTMENTS_ONLY,
	
	/** Can't summon on top of enemy units */
	ENEMIES_HERE,
	
	/** Map cell already has maximum number of units */
	CELL_FULL,
	
	/** Can't summon on impassable terrain */
	TERRAIN_IMPASSABLE,
	
	/** Target has no ranged attack (Warp wood) */
	NO_RANGED_ATTACK,

	/** Target doesn't have right kind of RAT for this spell (Warp wood) */
	INVALID_RANGED_ATTACK_TYPE,
	
	/** Target has no ammunition left (Warp wood) */
	NO_AMMUNITION,
	
	/** Target already has too much experience (Heroism) */
	TOO_MUCH_EXPERIENCE;
}