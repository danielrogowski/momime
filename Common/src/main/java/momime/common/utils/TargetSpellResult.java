package momime.common.utils;

/**
 * Possible outcomes when we check if a particular city or unit is a valid target for a spell
 */
public enum TargetSpellResult
{
	/** City or unit is a valid target for this spell */
	VALID_TARGET (null, null, null),
	
	/** Can't cast a beneficial enchantment on an enemy city or unit */
	ENCHANTING_OR_HEALING_ENEMY ("EnchantingEnemyUnit", "EnchantingEnemyCity", null),
	
	/** Can't cast a curse on our own city or unit, or target an attack spell like fire bolt on it */
	CURSING_OR_ATTACKING_OWN ("CursingOwnUnit", "CursingOwnCity", null),
	
	/** Spell has no spellEffectIDs defined at all */
	NO_SPELL_EFFECT_IDS_DEFINED ("NoUnitSpellEffectIDsDefined", "NoCitySpellEffectIDsDefined", null),

	/** Spell has spellEffectIDs defined, but they're all already cast on this city or unit */
	ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS ("AlreadyHasAllPossibleUnitSpellEffects", "AlreadyHasAllPossibleCitySpellEffects", "LocationAlreadyCorrupted"),
	
	/** This spell creates a building (wall of stone, summoning circle or move fortress, null) and the target city already has that building */
	CITY_ALREADY_HAS_BUILDING (null, "AlreadyHasBuilding", null),
	
	/** There is no city at the specified location */
	NO_CITY_HERE (null, null, null),

	/** Spell can't be targetted on this magic realm/lifeform type of unit, e.g. Star Fires can only be targetted on Chaos+Death units */
	UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE ("InvalidMagicRealmLifeformType", null, null),
	
	/** Spell rolls against resistance, and the target unit has too high resistance to possibly be affected by it */
	TOO_HIGH_RESISTANCE ("TooHighResistance", null, null),
	
	/** The target is completely immune to this type of spell */
	IMMUNE ("Immune", null, null),
	
	/** Units combatLocation doesn't match the combat the spell is targetted at */
	UNIT_NOT_IN_EXPECTED_COMBAT ("UnitNotInExpectedCombat", null, null),
	
	/** Unit's status is not ALIVE */
	UNIT_DEAD ("UnitDead", null, null),
	
	/** Trying to heal a unit that has taken no damage */
	UNDAMAGED ("Undamaged", null, null),

	/** Trying to heal a unit that has taken only permanent damage */
	PERMANENTLY_DAMAGED ("PermanentlyDamaged", null, null),
	
	/** Trying to dispel a target that has no enemy spells cast on it */
	NOTHING_TO_DISPEL ("NoEnemySpellsUnit", null, null),
	
	/** Spell can only be targetted against locations that we can actually see */
	CANNOT_SEE_TARGET (null, "CannotSeeCity", "CannotSeeLocation"),
	
	/** Spell can only be targetted at land tiles */
	MUST_TARGET_LAND (null, null, "MustTargetLand");
	
	/** languageEntryID for the text describing this kind of error for targetting units; if null then no error will be displayed */
	private final String unitLanguageEntryID;

	/** languageEntryID for the text describing this kind of error for targetting cities; if null then no error will be displayed */
	private final String cityLanguageEntryID;
	
	/** languageEntryID for the text describing this kind of error for targetting locations; if null then no error will be displayed */
	private final String locationLanguageEntryID;
	
	/**
	 * @param aUnitLanguageEntryID languageEntryID for the text describing this kind of error for targetting units; if null then no error will be displayed
	 * @param aCityLanguageEntryID languageEntryID for the text describing this kind of error for targetting cities; if null then no error will be displayed
	 * @param aLocationLanguageEntryID languageEntryID for the text describing this kind of error for targetting locations; if null then no error will be displayed
	 */
	private TargetSpellResult (final String aUnitLanguageEntryID, final String aCityLanguageEntryID, final String aLocationLanguageEntryID)
	{
		unitLanguageEntryID = aUnitLanguageEntryID;
		cityLanguageEntryID = aCityLanguageEntryID;
		locationLanguageEntryID = aLocationLanguageEntryID;
	}

	/**
	 * @return languageEntryID for the text describing this kind of error for targetting units; if null then no error will be displayed
	 */
	public final String getUnitLanguageEntryID ()
	{
		return unitLanguageEntryID;
	}

	/**
	 * @return languageEntryID for the text describing this kind of error for targetting cities; if null then no error will be displayed
	 */
	public final String getCityLanguageEntryID ()
	{
		return cityLanguageEntryID;
	}
	
	/**
	 * @return languageEntryID for the text describing this kind of error for targetting locations; if null then no error will be displayed
	 */
	public final String getLocationLanguageEntryID ()
	{
		return locationLanguageEntryID;
	}
}