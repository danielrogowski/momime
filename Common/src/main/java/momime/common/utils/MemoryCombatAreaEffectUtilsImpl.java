package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MemoryCombatAreaEffect;

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
	public final boolean findCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final MapCoordinates3DEx mapLocation, final String combatAreaEffectID, final Integer castingPlayerID)
	{
		log.trace ("Entering findCombatAreaEffect: " + mapLocation + ", " + combatAreaEffectID + ", " + castingPlayerID); 

		boolean found = false;
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();

			if ((CompareUtils.safeOverlandMapCoordinatesCompare (mapLocation, (MapCoordinates3DEx) thisCAE.getMapLocation ())) &&
				(combatAreaEffectID.equals (thisCAE.getCombatAreaEffectID ())) &&
				(CompareUtils.safeIntegerCompare (castingPlayerID, thisCAE.getCastingPlayerID ())))

				found = true;
		}

		log.trace ("Exiting findCombatAreaEffect = " + found);
		return found;
	}

	/**
	 * Removes a CAE
	 * @param CAEs List of CAEs to remove from
	 * @param mapLocation Location of the effect to look for; null for global enchantments
	 * @param combatAreaEffectID Effect to look for
	 * @param castingPlayerID Player to look for; null for natural CAEs like node auras
	 * @throws RecordNotFoundException If the CAE doesn't exist
	 */
	@Override
	public final void cancelCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final MapCoordinates3DEx mapLocation, final String combatAreaEffectID, final Integer castingPlayerID) throws RecordNotFoundException
	{
		log.trace ("Entering cancelCombatAreaEffect: " + mapLocation + ", " + combatAreaEffectID + ", " + castingPlayerID); 

		boolean found = false;
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();

			if ((CompareUtils.safeOverlandMapCoordinatesCompare (mapLocation, (MapCoordinates3DEx) thisCAE.getMapLocation ())) &&
				(combatAreaEffectID.equals (thisCAE.getCombatAreaEffectID ())) && (castingPlayerID == thisCAE.getCastingPlayerID ()))
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryCombatAreaEffect.class, combatAreaEffectID + " - " + castingPlayerID, "cancelCombatAreaEffect");

		log.trace ("Exiting cancelCombatAreaEffect");
	}
}