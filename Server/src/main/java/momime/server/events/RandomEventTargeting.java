package momime.server.events;

import momime.common.MomException;
import momime.common.database.Event;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Deals with targeting for all random events
 */
public interface RandomEventTargeting
{
	/**
	 * @param event Event we want to find a target for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this event can find any valid target or not
	 * @throws RecordNotFoundException If we can't find an expected data item
	 */
	public boolean isAnyValidTargetForEvent (final Event event, final MomSessionVariables mom)
		throws RecordNotFoundException;
	
	/**
	 * @param event Event to trigger
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws MomException If there is another kind of error
	 */
	public void triggerEvent (final Event event, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException;
}