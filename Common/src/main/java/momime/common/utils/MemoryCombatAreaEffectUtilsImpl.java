package momime.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MemoryCombatAreaEffect;

/**
 * Helper methods for dealing with MemoryCombatAreaEffect objects
 */
public final class MemoryCombatAreaEffectUtilsImpl implements MemoryCombatAreaEffectUtils
{
	/**
	 * Checks to see if the specified CAE exists
	 * @param CAEs List of CAEs to search through
	 * @param mapLocation Location of the effect to look for; null for global enchantments
	 * @param combatAreaEffectID Effect to look for
	 * @param castingPlayerID Player to look for; null for natural CAEs like node auras
	 * @return Whether or not the specified combat area effect exists
	 */
	@Override
	public final MemoryCombatAreaEffect findCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final MapCoordinates3DEx mapLocation, final String combatAreaEffectID, final Integer castingPlayerID)
	{
		MemoryCombatAreaEffect found = null;
		
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();

			if ((CompareUtils.safeOverlandMapCoordinatesCompare (mapLocation, (MapCoordinates3DEx) thisCAE.getMapLocation ())) &&
				(combatAreaEffectID.equals (thisCAE.getCombatAreaEffectID ())) &&
				(CompareUtils.safeIntegerCompare (castingPlayerID, thisCAE.getCastingPlayerID ())))

				found = thisCAE;
		}

		return found;
	}

	/**
	 * @param combatAreaEffectURN CAE URN to search for
	 * @param CAEs List of CAEs to search through
	 * @return CAE with requested URN, or null if not found
	 */
	@Override
	public final MemoryCombatAreaEffect findCombatAreaEffectURN (final int combatAreaEffectURN, final List<MemoryCombatAreaEffect> CAEs)
	{
		MemoryCombatAreaEffect found = null;
		
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();
			if (thisCAE.getCombatAreaEffectURN () == combatAreaEffectURN)
				found = thisCAE;
		}

		return found;
	}

	/**
	 * @param combatAreaEffectURN CAE URN to search for
	 * @param CAEs List of CAEs to search through
	 * @param caller The routine that was looking for the value
	 * @return CAE with requested URN
	 * @throws RecordNotFoundException If CAE with requested URN is not found
	 */
	@Override
	public final MemoryCombatAreaEffect findCombatAreaEffectURN (final int combatAreaEffectURN, final List<MemoryCombatAreaEffect> CAEs, final String caller)
		throws RecordNotFoundException
	{
		final MemoryCombatAreaEffect result = findCombatAreaEffectURN (combatAreaEffectURN, CAEs);

		if (result == null)
			throw new RecordNotFoundException (MemoryCombatAreaEffect.class, combatAreaEffectURN, caller);
		
		return result;
	}

	/**
	 * @param combatAreaEffectURN CAE URN to remove
	 * @param CAEs List of CAEs to search through
	 * @throws RecordNotFoundException If CAE with requested URN is not found
	 */
	@Override
	public final void removeCombatAreaEffectURN (final int combatAreaEffectURN, final List<MemoryCombatAreaEffect> CAEs)
		throws RecordNotFoundException
	{
		boolean found = false;
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();
			if (thisCAE.getCombatAreaEffectURN () == combatAreaEffectURN)
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryCombatAreaEffect.class, combatAreaEffectURN, "removeCombatAreaEffectURN");
	}

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
	@Override
	public final List<String> listCombatEffectsNotYetCastAtLocation (final List<MemoryCombatAreaEffect> CAEs, final Spell spell,
		final int castingPlayerID, final MapCoordinates3DEx combatLocation)
	{
    	final List<String> combatAreaEffectIDs;
    	
    	if (spell.getSpellHasCombatEffect ().size () == 0)
    		combatAreaEffectIDs = null;
    	else
    	{
    		combatAreaEffectIDs = new ArrayList<String> ();
    		for (final String combatAreaEffectID : spell.getSpellHasCombatEffect ())
    			if (findCombatAreaEffect (CAEs, combatLocation, combatAreaEffectID, castingPlayerID) == null)
    				combatAreaEffectIDs.add (combatAreaEffectID);
    	}

    	return combatAreaEffectIDs;
	}
}