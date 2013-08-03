package momime.common.utils;

import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MomScheduledCombat;

/**
 * Util methods for dealing with scheduled combats in simultaneous turns games
 */
public interface ScheduledCombatUtils
{
	/**
	 * @param combats List of scheduled combats to search
	 * @param scheduledCombatURN scheduledCombatURN to search for
	 * @return Combat with requested key
	 * @throws RecordNotFoundException If the requested combat URN doesn't exist in the list
	 */
	public MomScheduledCombat findScheduledCombatURN (final List<MomScheduledCombat> combats, final Integer scheduledCombatURN)
		throws RecordNotFoundException;
}
