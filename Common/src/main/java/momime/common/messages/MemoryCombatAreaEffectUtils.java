package momime.common.messages;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Helper methods for dealing with MemoryCombatAreaEffect objects
 */
public final class MemoryCombatAreaEffectUtils
{
	/**
	 * Checks to see if the specified building exists
	 * @param CAEs List of CAEs to search through
	 * @param mapLocation Location of the effect to look for
	 * @param combatAreaEffectID Effect to look for
	 * @param castingPlayerID Player to look for
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether or not the specified combat area effect exists
	 */
	public static final boolean findCombatAreaEffect (final List<MemoryCombatAreaEffect> CAEs,
		final OverlandMapCoordinates mapLocation, final String combatAreaEffectID, final int castingPlayerID, final Logger debugLogger)
	{
		debugLogger.entering (MemoryCombatAreaEffectUtils.class.getName (), "findCombatAreaEffect",
			new String [] {CoordinatesUtils.overlandMapCoordinatesToString (mapLocation), combatAreaEffectID, new Integer (castingPlayerID).toString ()});

		boolean found = false;
		final Iterator<MemoryCombatAreaEffect> iter = CAEs.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryCombatAreaEffect thisCAE = iter.next ();

			if ((((mapLocation == null) && (thisCAE.getMapLocation () == null)) || ((mapLocation != null) && (CoordinatesUtils.overlandMapCoordinatesEqual (mapLocation, thisCAE.getMapLocation ())))) &&
				(combatAreaEffectID.equals (thisCAE.getCombatAreaEffectID ())) && (castingPlayerID == thisCAE.getCastingPlayerID ()))

				found = true;
		}

		debugLogger.exiting (MemoryCombatAreaEffectUtils.class.getName (), "findCombatAreaEffect", found);
		return found;
	}

	/**
	 * Prevent instantiation
	 */
	private MemoryCombatAreaEffectUtils ()
	{
	}
}
