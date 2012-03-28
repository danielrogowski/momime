package momime.common.messages;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.utils.CompareUtils;

/**
 * Helper methods for dealing with MemoryCombatAreaEffect objects
 */
public final class MemoryCombatAreaEffectUtils
{
	/**
	 * Checks to see if the specified CAE exists
	 * @param CAEs List of CAEs to search through
	 * @param mapLocation Location of the effect to look for
	 * @param combatAreaEffectID Effect to look for
	 * @param castingPlayerID Player to look for; null for natural CAEs like node auras
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether or not the specified combat area effect exists
	 */
	public static final boolean findCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final OverlandMapCoordinates mapLocation, final String combatAreaEffectID, final Integer castingPlayerID, final Logger debugLogger)
	{
		debugLogger.entering (MemoryCombatAreaEffectUtils.class.getName (), "findCombatAreaEffect",
			new String [] {CoordinatesUtils.overlandMapCoordinatesToString (mapLocation), combatAreaEffectID});

		boolean found = false;
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();

			if ((CoordinatesUtils.overlandMapCoordinatesEqual (mapLocation, thisCAE.getMapLocation (), true)) &&
				(combatAreaEffectID.equals (thisCAE.getCombatAreaEffectID ())) &&
				(CompareUtils.safeIntegerCompare (castingPlayerID, thisCAE.getCastingPlayerID ())))

				found = true;
		}

		debugLogger.exiting (MemoryCombatAreaEffectUtils.class.getName (), "findCombatAreaEffect", found);
		return found;
	}

	/**
	 * Removes a CAE
	 * @param CAEs List of CAEs to remove from
	 * @param mapLocation Location of the effect to look for
	 * @param combatAreaEffectID Effect to look for
	 * @param castingPlayerID Player to look for; null for natural CAEs like node auras
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If the CAE doesn't exist
	 */
	public static final void cancelCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final OverlandMapCoordinates mapLocation, final String combatAreaEffectID, final Integer castingPlayerID, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (MemoryCombatAreaEffectUtils.class.getName (), "cancelCombatAreaEffect",
			new String [] {CoordinatesUtils.overlandMapCoordinatesToString (mapLocation), combatAreaEffectID});

		boolean found = false;
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();

			if ((CoordinatesUtils.overlandMapCoordinatesEqual (mapLocation, thisCAE.getMapLocation (), true)) &&
				(combatAreaEffectID.equals (thisCAE.getCombatAreaEffectID ())) && (castingPlayerID == thisCAE.getCastingPlayerID ()))
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryCombatAreaEffect.class.getName (), combatAreaEffectID + " - " + castingPlayerID, "cancelCombatAreaEffect");

		debugLogger.exiting (MemoryCombatAreaEffectUtils.class.getName (), "cancelCombatAreaEffect");
	}

	/**
	 * Prevent instantiation
	 */
	private MemoryCombatAreaEffectUtils ()
	{
	}
}
