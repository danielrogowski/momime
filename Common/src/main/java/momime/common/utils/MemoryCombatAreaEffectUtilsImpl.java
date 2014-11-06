package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryCombatAreaEffect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Helper methods for dealing with MemoryCombatAreaEffect objects
 */
public final class MemoryCombatAreaEffectUtilsImpl implements MemoryCombatAreaEffectUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MemoryCombatAreaEffectUtilsImpl.class);
	
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
		log.trace ("Entering findCombatAreaEffect: " + mapLocation + ", " + combatAreaEffectID + ", " + castingPlayerID); 

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

		log.trace ("Exiting findCombatAreaEffect = " + found);
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
		log.trace ("Entering findCombatAreaEffectURN: " + combatAreaEffectURN); 

		MemoryCombatAreaEffect found = null;
		
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();
			if (thisCAE.getCombatAreaEffectURN () == combatAreaEffectURN)
				found = thisCAE;
		}

		log.trace ("Exiting findCombatAreaEffectURN = " + found);
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
		log.trace ("Entering findCombatAreaEffectURN: " + combatAreaEffectURN + ", " + caller); 

		final MemoryCombatAreaEffect result = findCombatAreaEffectURN (combatAreaEffectURN, CAEs);

		if (result == null)
			throw new RecordNotFoundException (MemoryCombatAreaEffect.class, combatAreaEffectURN, caller);
		
		log.trace ("Exiting findCombatAreaEffectURN = " + result);
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
		log.trace ("Entering removeCombatAreaEffectURN: " + combatAreaEffectURN); 

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

		log.trace ("Exiting removeCombatAreaEffectURN");
	}
}