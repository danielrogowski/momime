package momime.client.utils;

/**
 * Types of unit names that the UnitClientUtils.getUnitName () method can generate. 
 * Besides the fixed unit names and suggested hero names in the language XML, unit names are generated according to 2 flags:
 * 
 * - The server XML contains an "includeRaceInUnitName" value, where the unit name is generic and needs a race qualifier in order to
 *   uniquely identify it.  So e.g. it is false for Triremes, because every race uses the same Trireme unit; it is false for Longbowmen,
 *   because they're unique to High Elves; but it is true for Swordsmen, because lots of different races have Swordsmen units and
 *   they all have different stats and appearances; it is false on all heroes and summoned units. 
 *
 * - The language XML contains a "prefix", which is "a" or "an" as appropriate, for singular units other than heroes.
 *   So it is implied that if this value is missing then it is a plural unit.
 *   
 * These same enum names are also supported by UnitStatsLanguageVariableReplacer so they can be used in language XML
 * strings that refer to units, e.g. "Do you want to disband THE_UNIT_OF_NAME?"
 */
public enum UnitNameType
{
	/**
	 * Outputs just the unit or hero name.
	 * e.g. "Valana the Bard", "Trireme", "Swordsmen", "Longbowmen", "Magic Spirit", "Hell Hounds".
	 * This is used when the race of the unit is already obvious, e.g. when choosing what to construct at a city.
	 */
	SIMPLE_UNIT_NAME,

	/**
	 * Outputs the unit or hero name including racial prefix.
	 * e.g. "Valana the Bard", "Trireme", "Orc Swordsmen", "Longbowmen", "Magic Spirit", "Hell Hounds".
	 * This is the most commonly displayed name in the game, e.g. on the unit info screen.
	 */
	RACE_UNIT_NAME,
	
	/**
	 * Outputs the unit or hero name including "a" or "an" prefix if it is singular.
	 * e.g. "Valana the Bard", "a Trireme", "Orc Swordsmen", "Longbowmen", "a Magic Spirit", "Hell Hounds".
	 * This is used in various dialog boxes, e.g. You have completed summoning "a Magic Spirit".
	 */
	A_UNIT_NAME,
	
	/**
	 * Outputs a the unit or hero name including "the unit of" prefix if it is plural.
	 * e.g. "Valana the Bard", "the Trireme", "the unit of Orc Swordsmen", "the unit of Longbowmen", "the Magic Spirit", "the unit of Hell Hounds".
	 * This is used in various dialog boxes, e.g. Do you want to disband "the unit of Orc Swordsmen" ?
	 */
	THE_UNIT_OF_NAME,
	
	/**
	 * Outputs a the unit or hero name including "a unit of" prefix if it is plural.
	 * e.g. "Valana the Bard", "a Trireme", "a unit of Orc Swordsmen", "a unit of Longbowmen", "a Magic Spirit", "a unit of Hell Hounds".
	 * This is used in offers, e.g. Do you want to hire "a unit of Orc Swordsmen" ?
	 */
	A_UNIT_OF_NAME,

	/**
	 * Outputs a the unit or hero name including "units of" prefix if it is plural, and putting an "s" on the end of singular unit names.
	 * e.g. "Valana the Bard", "Triremes", "units of Orc Swordsmen", "units of Longbowmen", "Magic Spirits", "units of Hell Hounds".
	 * This is used in offers, e.g. Do you want to hire "3 units of Orc Swordsmen" ?
	 */
	UNITS_OF_NAME;
}