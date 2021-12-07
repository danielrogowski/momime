package momime.server.events;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Rolls random events
 */
public interface RandomEvents
{
	/**
	 * Rolls to see if server should trigger a random event this turn 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws MomException If there is another kind of error
	 */
	public void rollRandomEvent (final MomSessionVariables mom)
		throws RecordNotFoundException, MomException;
}