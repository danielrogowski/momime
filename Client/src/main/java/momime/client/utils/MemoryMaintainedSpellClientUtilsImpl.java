package momime.client.utils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;

import momime.common.database.Spell;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.utils.MemoryMaintainedSpellUtils;

/**
 * Methods for working with spells that are only needed on the client
 */
public final class MemoryMaintainedSpellClientUtilsImpl implements MemoryMaintainedSpellClientUtils
{
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/**
	 * Checks whether the specified spell can be targetted at any locations on the combat map.
	 * This is only called for spells that are targetted at a location - section SPECIAL_COMBAT_SPELLS
	 * 
	 * @param spell Spell being cast
	 * @param map Combat map terrain
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return Whether the location is a valid target or not
	 */
	@Override
	public final boolean isAnyCombatLocationValidTargetForSpell (final Spell spell, final MapAreaOfCombatTiles map, final CoordinateSystem combatMapCoordinateSystem)
	{
		boolean found = false;
		int y = 0;
		while ((!found) && (y < combatMapCoordinateSystem.getHeight ()))
		{
			int x = 0;
			while ((!found) && (x < combatMapCoordinateSystem.getWidth ()))
			{
				if (getMemoryMaintainedSpellUtils ().isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (x, y), map))
					found = true;
				else
					x++;
			}
			y++;
		}
		
		return found;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param su MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils su)
	{
		memoryMaintainedSpellUtils = su;
	}
}