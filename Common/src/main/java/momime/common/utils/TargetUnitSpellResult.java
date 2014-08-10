package momime.common.utils;

/**
 * Possible outcomes when we check if a particular unit is a valid target for a spell
 */
public enum TargetUnitSpellResult
{
	/** Unit is a valid target for this spell */
	VALID_TARGET,
	
	/** Can't cast a beneficial enchantment on an enemy unit */
	ENCHANTING_ENEMY_UNIT,
	
	/** Can't cast a curse on our own unit */
	CURSING_OWN_UNIT,
	
	/** Spell has no unitSpellEffectIDs defined at all */
	NO_SPELL_EFFECT_IDS_DEFINED,

	/** Spell has unitSpellEffectIDs defined, but they're all already cast on this unit */
	ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS,
	
	/** Spell can't be targetted on this magic realm/lifeform type of unit, e.g. Star Fires can only be targetted on Chaos+Death units */
	INVALID_MAGIC_REALM_LIFEFORM_TYPE;	
}
