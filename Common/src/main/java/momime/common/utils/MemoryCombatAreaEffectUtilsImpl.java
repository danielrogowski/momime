package momime.common.utils;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;

/**
 * Helper methods for dealing with MemoryCombatAreaEffect objects
 */
public final class MemoryCombatAreaEffectUtilsImpl implements MemoryCombatAreaEffectUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MemoryCombatAreaEffectUtilsImpl.class.getName ());
	
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
		final OverlandMapCoordinatesEx mapLocation, final String combatAreaEffectID, final Integer castingPlayerID)
	{
		log.entering (MemoryCombatAreaEffectUtilsImpl.class.getName (), "findCombatAreaEffect",
			new String [] {(mapLocation == null) ? "Global" : mapLocation.toString (), combatAreaEffectID});

		boolean found = false;
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();

			if ((CompareUtils.safeOverlandMapCoordinatesCompare (mapLocation, (OverlandMapCoordinatesEx) thisCAE.getMapLocation ())) &&
				(combatAreaEffectID.equals (thisCAE.getCombatAreaEffectID ())) &&
				(CompareUtils.safeIntegerCompare (castingPlayerID, thisCAE.getCastingPlayerID ())))

				found = true;
		}

		log.exiting (MemoryCombatAreaEffectUtilsImpl.class.getName (), "findCombatAreaEffect", found);
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
		final OverlandMapCoordinatesEx mapLocation, final String combatAreaEffectID, final Integer castingPlayerID) throws RecordNotFoundException
	{
		log.entering (MemoryCombatAreaEffectUtilsImpl.class.getName (), "cancelCombatAreaEffect",
			new String [] {(mapLocation == null) ? "Global" : mapLocation.toString (), combatAreaEffectID});

		boolean found = false;
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();

			if ((CompareUtils.safeOverlandMapCoordinatesCompare (mapLocation, (OverlandMapCoordinatesEx) thisCAE.getMapLocation ())) &&
				(combatAreaEffectID.equals (thisCAE.getCombatAreaEffectID ())) && (castingPlayerID == thisCAE.getCastingPlayerID ()))
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryCombatAreaEffect.class.getName (), combatAreaEffectID + " - " + castingPlayerID, "cancelCombatAreaEffect");

		log.exiting (MemoryCombatAreaEffectUtilsImpl.class.getName (), "cancelCombatAreaEffect");
	}
}
