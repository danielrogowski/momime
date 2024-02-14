package momime.client.utils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;

import momime.client.MomClient;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.utils.SpellTargetingUtils;

/**
 * Methods for working with spells that are only needed on the client
 */
public final class MemoryMaintainedSpellClientUtilsImpl implements MemoryMaintainedSpellClientUtils
{
	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
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
	@Override
	public final boolean isAnyCombatLocationValidTargetForSpell (final Spell spell, final MapAreaOfCombatTiles map, final CoordinateSystem combatMapCoordinateSystem)
		throws RecordNotFoundException, MomException
	{
		boolean found = false;
		int y = 0;
		while ((!found) && (y < combatMapCoordinateSystem.getHeight ()))
		{
			int x = 0;
			while ((!found) && (x < combatMapCoordinateSystem.getWidth ()))
			{
				if (getSpellTargetingUtils ().isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (x, y), map, getClient ().getClientDB ()))
					found = true;
				else
					x++;
			}
			y++;
		}
		
		return found;
	}

	/**
	 * @return Methods that determine whether something is a valid target for a spell
	 */
	public final SpellTargetingUtils getSpellTargetingUtils ()
	{
		return spellTargetingUtils;
	}

	/**
	 * @param s Methods that determine whether something is a valid target for a spell
	 */
	public final void setSpellTargetingUtils (final SpellTargetingUtils s)
	{
		spellTargetingUtils = s;
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
}