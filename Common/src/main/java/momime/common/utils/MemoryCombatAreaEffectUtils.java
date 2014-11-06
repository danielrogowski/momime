package momime.common.utils;

import java.util.List;

import momime.common.database.RecordNotFoundException;
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
}