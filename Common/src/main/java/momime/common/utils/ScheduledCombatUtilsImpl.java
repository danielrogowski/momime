package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MomScheduledCombat;

/**
 * Util methods for dealing with scheduled combats in simultaneous turns games
 */
public final class ScheduledCombatUtilsImpl implements ScheduledCombatUtils
{
	/**
	 * @param combats List of scheduled combats to search
	 * @param scheduledCombatURN scheduledCombatURN to search for
	 * @return Combat with requested key
	 * @throws RecordNotFoundException If the requested combat URN doesn't exist in the list
	 */
	@Override
	public final MomScheduledCombat findScheduledCombatURN (final List<MomScheduledCombat> combats, final Integer scheduledCombatURN)
		throws RecordNotFoundException
	{
		MomScheduledCombat found = null;
		final Iterator<MomScheduledCombat> iter = combats.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MomScheduledCombat thisCombat = iter.next ();
			if (thisCombat.getScheduledCombatURN () == scheduledCombatURN)
				found = thisCombat;
		}
		
		if (found == null)
			throw new RecordNotFoundException ("ScheduledCombats", scheduledCombatURN, "findScheduledCombatURN");
		
		return found;
	}
}
