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
	
	/** For Evil Prescence, which has no effect on wizards who have death books */
	WIZARD_HAS_DEATH_BOOKS,
	
	/** City is protected against spells of this magic realm (Consecration or Spell Ward) */
	PROTECTED_AGAINST_SPELL_REALM,

	/** Spell can't be targeted on this magic realm/lifeform type of unit, e.g. Star Fires can only be targeted on Chaos+Death units */
	UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE,
	
	/** Spell rolls against resistance, and the target unit has too high resistance to possibly be affected by it */
	TOO_HIGH_RESISTANCE,
	
	/** The target is completely immune to this type of spell */
	IMMUNE,
	
	/** Units combatLocation doesn't match the combat the spell is targeted at */
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
	
	/** Spell can only be targeted against locations that we can actually see */
	CANNOT_SEE_TARGET,
	
	/**
	 * Can't target invisible units overland; this is special as we don't want to show the player an error like "You can't target invisible units" as it gives them
	 * information that an invisible unit is there.  So this gets trapped and avoid showing any error message at all, just like there was never a unit there to begin with.
	 */
	INVISIBLE,
	
	/** Spell cannot be targeted at certain tile types */
	INVALID_TILE_TYPE,
	
	/** Spell cannot be targeted at certain map features */
	INVALID_MAP_FEATURE,
	
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
	TOO_MUCH_EXPERIENCE,
	
	/** Trying to target enemy wizard spell on ourselves */
	ATTACKING_OWN_WIZARD,
	
	/** Trying to target a spell at raiders or rampaging monsters */
	NOT_A_WIZARD,

	/** Trying to target a spell at a wizard we have not met */
	WIZARD_NOT_MET,
	
	/** Trying to target a spell at a wizard who is banished or defeated */
	WIZARD_BANISHED_OR_DEFEATED,
	
	/** Tried to spell blast a wizard who is not casting a spell */
	NO_SPELL_BEING_CAST,
	
	/** Tried to spell blast a wizard but we don't have enough MP stored to blast their spell */
	INSUFFICIENT_MANA,
	
	/** Can't cast plane shift on any units while planar seal is in effect */
	PLANAR_SEAL,
	
	/** Warp node can only be used on nodes that have been captured by an enemy wizard */
	UNOWNED_NODE,
	
	/** Can't use Word of Recall on combat summoned units like phantom warriors */
	COMBAT_SUMMON;
}