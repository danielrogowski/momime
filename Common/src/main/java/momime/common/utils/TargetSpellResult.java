package momime.common.utils;

/**
 * Possible outcomes when we check if a particular city or unit is a valid target for a spell
 */
public enum TargetSpellResult
{
	/** City or unit is a valid target for this spell */
	VALID_TARGET (null, null),
	
	/** Can't cast a beneficial enchantment on an enemy city or unit */
	ENCHANTING_ENEMY ("EnchantingEnemyUnit", "EnchantingEnemyCity"),
	
	/** Can't cast a curse on our own city or unit, or target an attack spell like fire bolt on it */
	CURSING_OR_ATTACKING_OWN ("CursingOwnUnit", "CursingOwnCity"),
	
	/** Spell has no spellEffectIDs defined at all */
	NO_SPELL_EFFECT_IDS_DEFINED ("NoUnitSpellEffectIDsDefined", "NoCitySpellEffectIDsDefined"),

	/** Spell has spellEffectIDs defined, but they're all already cast on this city or unit */
	ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS ("AlreadyHasAllPossibleUnitSpellEffects", "AlreadyHasAllPossibleCitySpellEffects"),
	
	/** This spell creates a building (wall of stone, summoning circle or move fortress) and the target city already has that building */
	CITY_ALREADY_HAS_BUILDING (null, "AlreadyHasBuilding"),
	
	/** There is no city at the specified location */
	NO_CITY_HERE (null, null),

	/** Spell can't be targetted on this magic realm/lifeform type of unit, e.g. Star Fires can only be targetted on Chaos+Death units */
	UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE ("InvalidMagicRealmLifeformType", null),
	
	/** Spell rolls against resistance, and the target unit has too high resistance to possibly be affected by it */
	TOO_HIGH_RESISTANCE ("TooHighResistance", null),
	
	/** Units combatLocation doesn't match the combat the spell is targetted at */
	UNIT_NOT_IN_EXPECTED_COMBAT ("UnitNotInExpectedCombat", null),
	
	/** Unit's status is not ALIVE */
	UNIT_DEAD ("UnitDead", null);
	
	/** languageEntryID for the text describing this kind of error for units; if null then no error will be displayed */
	private final String unitLanguageEntryID;

	/** languageEntryID for the text describing this kind of error for cities; if null then no error will be displayed */
	private final String cityLanguageEntryID;
	
	/**
	 * @param aUnitLanguageEntryID languageEntryID for the text describing each kind of error; if null then no error will be displayed
	 * @param aCityLanguageEntryID languageEntryID for the text describing each kind of error; if null then no error will be displayed
	 */
	private TargetSpellResult (final String aUnitLanguageEntryID, final String aCityLanguageEntryID)
	{
		unitLanguageEntryID = aUnitLanguageEntryID;
		cityLanguageEntryID = aCityLanguageEntryID;
	}

	/**
	 * @return languageEntryID for the text describing this kind of error for units; if null then no error will be displayed
	 */
	public final String getUnitLanguageEntryID ()
	{
		return unitLanguageEntryID;
	}

	/**
	 * @return languageEntryID for the text describing this kind of error for cities; if null then no error will be displayed
	 */
	public final String getCityLanguageEntryID ()
	{
		return cityLanguageEntryID;
	}
}