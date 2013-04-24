package momime.common.messages;

import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Methods for working with list of MemoryMaintainedSpells
 */
public interface IMemoryMaintainedSpellUtils
{
	/**
	 * Searches for a maintained spell in a list
	 *
	 * The majority of the search fields are optional, since this allows us to do searches like
	 * looking for a maintained spell that grants unit skill SSxxx, even though we don't know what spell(s) might grant that skill
	 *
	 * @param spells List of spells to search through
	 * @param castingPlayerID Player who cast the spell to search for, or null to match any
	 * @param spellID Unique identifier for the spell to search for, or null to match any
	 * @param unitURN Which unit the spell is cast on - this is mandatory, null will match only non-unit spells or untargetted unit spells
	 * @param unitSkillID Which actual unit spell effect was granted, or null to match any
	 * @param cityLocation Which city the spell is cast on - this is mandatory, null will match only non-city spells or untargetted city spells
	 * @param citySpellEffectID Which actual city spell effect was granted, or null to match any
	 * @return First matching spell found, or null if none matched
	 */
	public MemoryMaintainedSpell findMaintainedSpell (final List<MemoryMaintainedSpell> spells,
		final Integer castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final OverlandMapCoordinates cityLocation, final String citySpellEffectID);

	/**
	 * Removes a maintained spell from the list
	 *
	 * @param spells List of spells to search through
	 * @param castingPlayerID Player who cast the spell to search for
	 * @param spellID Unique identifier for the spell to search for
	 * @param unitURN Which unit the spell is cast on
	 * @param unitSkillID Which actual unit spell effect was granted
	 * @param cityLocation Which city the spell is cast on
	 * @param citySpellEffectID Which actual city spell effect was granted
	 * @throws RecordNotFoundException If we can't find the requested spell
	 */
	public void switchOffMaintainedSpell (final List<MemoryMaintainedSpell> spells,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final OverlandMapCoordinates cityLocation, final String citySpellEffectID)
		throws RecordNotFoundException;

	/**
	 * Removes all spells from the list that are cast on any of the units listed
	 *
	 * @param spells List of spells to search through
	 * @param unitURNs List of units to remove spells from
	 */
	public void removeSpellsCastOnUnitStack (final List<MemoryMaintainedSpell> spells, final List<Integer> unitURNs);
}
