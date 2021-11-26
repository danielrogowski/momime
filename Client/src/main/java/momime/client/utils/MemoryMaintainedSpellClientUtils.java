package momime.client.utils;

import com.ndg.map.CoordinateSystem;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MapAreaOfCombatTiles;

/**
 * Methods for working with spells that are only needed on the client
 */
public interface MemoryMaintainedSpellClientUtils
{
	/**
	 * Checks whether the specified spell can be targetted at any locations on the combat map.
	 * This is only called for spells that are targetted at a location - section SPECIAL_COMBAT_SPELLS
	 * 
	 * @param spell Spell being cast
	 * @param map Combat map terrain
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return Whether the location is a valid target or not
	 * @throws RecordNotFoundException If we can't find the a combat tile type in the DB
	 * @throws MomException If we encounter a spell book section we don't know how to handle
	 */
	public boolean isAnyCombatLocationValidTargetForSpell (final Spell spell, final MapAreaOfCombatTiles map, final CoordinateSystem combatMapCoordinateSystem)
		throws RecordNotFoundException, MomException;
}