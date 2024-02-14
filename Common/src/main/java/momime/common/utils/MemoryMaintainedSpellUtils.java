package momime.common.utils;

import java.util.List;
import java.util.Set;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.MemoryMaintainedSpell;

/**
 * Methods for working with list of MemoryMaintainedSpells
 */
public interface MemoryMaintainedSpellUtils
{
	/**
	 * Searches for a maintained spell by its details
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
		final MapCoordinates3DEx cityLocation, final String citySpellEffectID);

	/**
	 * @param spellURN Spell URN to search for
	 * @param spells List of spells to search through
	 * @return Spell with requested URN, or null if not found
	 */
	public MemoryMaintainedSpell findSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells);

	/**
	 * @param spellURN Spell URN to search for
	 * @param spells List of spells to search through
	 * @param caller The routine that was looking for the value
	 * @return Spell with requested URN
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	public MemoryMaintainedSpell findSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells, final String caller)
		throws RecordNotFoundException;

	/**
	 * @param spellURN Spell URN to remove
	 * @param spells List of spells to search through
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	public void removeSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells)
		throws RecordNotFoundException;

	/**
	 * Removes all spells from the list that are cast on any of the units listed
	 *
	 * @param spells List of spells to search through
	 * @param unitURNs List of units to remove spells from
	 */
	public void removeSpellsCastOnUnitStack (final List<MemoryMaintainedSpell> spells, final List<Integer> unitURNs);

	/**
	 * When trying to cast a spell on a unit, this will make a list of all the unit spell effect IDs for that spell that aren't already in effect on that unit.
	 * This is mainly to deal with Warp Creature which has 3 seperate effects and can be multi-cast to get all 3 effects.
	 * 
	 * @param spells List of spells to check against
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param unitURN Which unit the spell is being case on
	 * @return Null = this spell has no unitSpellEffectIDs defined; empty list = has effect(s) defined but they're all cast on this unit already; non-empty list = list of effects that can still be cast
	 */
	public List<UnitSpellEffect> listUnitSpellEffectsNotYetCastOnUnit (final List<MemoryMaintainedSpell> spells, final Spell spell,
		final int castingPlayerID, final int unitURN);
	
	/**
	 * When trying to cast a spell on a city, this will make a list of all the city spell effect IDs for that spell that aren't already in effect on that city.
	 * This is mainly to deal with Spell Ward - we might have a Nature and Chaos Ward in place
	 * already, in that case this method will tell us that we can still cast a Life, Death or Sorcery Ward.
	 * 
	 * @param spells List of spells to check against
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param cityLocation Location of the city
	 * @return Null = this spell has no citySpellEffectIDs defined; empty list = has effect(s) defined but they're all cast on this city already; non-empty list = list of effects that can still be cast
	 */
	public List<String> listCitySpellEffectsNotYetCastAtLocation (final List<MemoryMaintainedSpell> spells, final Spell spell,
		final int castingPlayerID, final MapCoordinates3DEx cityLocation);

	/**
	 * @param cityLocation City to cast the spell on
	 * @param pickID Magic realm of spell being cast on the city
	 * @param castingPlayerID Player casting the spell
	 * @param spells List of known existing spells
	 * @param db Lookup lists built over the XML database
	 * @return Whether the city is protected against this magic realm
	 * @throws RecordNotFoundException If we can't find one of the city spell effects in the database
	 */
	public boolean isCityProtectedAgainstSpellRealm (final MapCoordinates3DEx cityLocation, final String pickID, final int castingPlayerID,
		final List<MemoryMaintainedSpell> spells, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param spells Known spells
	 * @param castingPlayerID Player casting the spell
	 * @param combatLocation Location we want to cast the spell at 
	 * @param pickID Magic realm of the spell we want to cast
	 * @param db Lookup lists built over the XML database
	 * @return True if there is a Spell Ward here that blocks casting combat spells of this magic realm
	 * @throws RecordNotFoundException If we can't find one of the city spell effects
	 */
	public boolean isBlockedCastingCombatSpellsOfRealm (final List<MemoryMaintainedSpell> spells, final int castingPlayerID,
		final MapCoordinates3DEx combatLocation, final String pickID, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param spells Known spells
	 * @param castingPlayerID Player casting the spell
	 * @param combatLocation Location we want to cast the spell at 
	 * @param db Lookup lists built over the XML database
	 * @return List of magic realms that we are not allowed to cast combat spells for
	 * @throws RecordNotFoundException If we can't find one of the city spell effects
	 */
	public Set<String> listMagicRealmsBlockedAsCombatSpells (final List<MemoryMaintainedSpell> spells, final int castingPlayerID,
		final MapCoordinates3DEx combatLocation, final CommonDatabase db) throws RecordNotFoundException;
}