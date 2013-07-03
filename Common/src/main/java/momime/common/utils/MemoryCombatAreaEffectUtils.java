package momime.common.utils;

import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;

/**
 * Helper methods for dealing with MemoryCombatAreaEffect objects
 */
public interface MemoryCombatAreaEffectUtils
{
	/**
	 * Checks to see if the specified CAE exists
	 * @param CAEs List of CAEs to search through
	 * @param mapLocation Location of the effect to look for; null for global enchantments
	 * @param combatAreaEffectID Effect to look for
	 * @param castingPlayerID Player to look for; null for natural CAEs like node auras
	 * @return Whether or not the specified combat area effect exists
	 */
	public boolean findCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final OverlandMapCoordinatesEx mapLocation, final String combatAreaEffectID, final Integer castingPlayerID);

	/**
	 * Removes a CAE
	 * @param CAEs List of CAEs to remove from
	 * @param mapLocation Location of the effect to look for; null for global enchantments
	 * @param combatAreaEffectID Effect to look for
	 * @param castingPlayerID Player to look for; null for natural CAEs like node auras
	 * @throws RecordNotFoundException If the CAE doesn't exist
	 */
	public void cancelCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final OverlandMapCoordinatesEx mapLocation, final String combatAreaEffectID, final Integer castingPlayerID) throws RecordNotFoundException;
}
