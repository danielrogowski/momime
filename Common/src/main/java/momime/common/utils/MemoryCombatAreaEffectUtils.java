package momime.common.utils;

import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Helper methods for dealing with MemoryCombatAreaEffect objects
 */
public interface MemoryCombatAreaEffectUtils
{
	/**
	 * Searches for a CAE by its details
	 * 
	 * @param CAEs List of CAEs to search through
	 * @param mapLocation Location of the effect to look for; null for global enchantments
	 * @param combatAreaEffectID Effect to look for
	 * @param castingPlayerID Player to look for; null for natural CAEs like node auras
	 * @return CAE with requested details, or null if not found
	 */
	public MemoryCombatAreaEffect findCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final MapCoordinates3DEx mapLocation, final String combatAreaEffectID, final Integer castingPlayerID);

	/**
	 * @param combatAreaEffectURN CAE URN to search for
	 * @param CAEs List of CAEs to search through
	 * @return CAE with requested URN, or null if not found
	 */
	public MemoryCombatAreaEffect findCombatAreaEffectURN (final int combatAreaEffectURN, final List<MemoryCombatAreaEffect> CAEs);

	/**
	 * @param combatAreaEffectURN CAE URN to search for
	 * @param CAEs List of CAEs to search through
	 * @param caller The routine that was looking for the value
	 * @return CAE with requested URN
	 * @throws RecordNotFoundException If CAE with requested URN is not found
	 */
	public MemoryCombatAreaEffect findCombatAreaEffectURN (final int combatAreaEffectURN, final List<MemoryCombatAreaEffect> CAEs, final String caller)
		throws RecordNotFoundException;

	/**
	 * @param combatAreaEffectURN CAE URN to remove
	 * @param CAEs List of CAEs to search through
	 * @throws RecordNotFoundException If CAE with requested URN is not found
	 */
	public void removeCombatAreaEffectURN (final int combatAreaEffectURN, final List<MemoryCombatAreaEffect> CAEs)
		throws RecordNotFoundException;
	
	/**
	 * When trying to cast a spell in combat, this will make a list of all the combat enhancement effect IDs for that spell that aren't already in effect in that location.
	 * This is to stop casting spells like Prayer twice.
	 * 
	 * @param CAEs List of CAEs to search through
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param combatLocation Location of the combat
	 * @return Null = this spell has no combatAreaEffectIDs defined; empty list = has effect(s) defined but they're all cast on this combat already; non-empty list = list of effects that can still be cast
	 */
	public List<String> listCombatEffectsNotYetCastAtLocation (final List<MemoryCombatAreaEffect> CAEs, final Spell spell,
		final int castingPlayerID, final MapCoordinates3DEx combatLocation);

	/**
	 * @param mem Player knowledge of spells and CAEs
	 * @param mapLocation Location the combat is taking place
	 * @param db Lookup lists built over the XML database
	 * @return List of CAEs cast in the combat at this location
	 * @throws RecordNotFoundException If we can't find the definition for one of the city spell effects
	 */
	public List<MemoryCombatAreaEffect> listCombatAreaEffectsFromLocalisedSpells
		(final FogOfWarMemory mem, final MapCoordinates3DEx mapLocation, final CommonDatabase db)
		throws RecordNotFoundException;
}