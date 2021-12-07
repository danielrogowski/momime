package momime.server.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.Event;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Rolls random events
 */
public final class RandomEventsImpl implements RandomEvents
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RandomEventsImpl.class);
	
	/** Event chances are defined in 1/30720ths */
	private final static int EVENT_CHANCE_DIVISOR = 30720;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Random rvent targeting */
	private RandomEventTargeting randomEventTargeting;
	
	/**
	 * Rolls to see if server should trigger a random event this turn 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws MomException If there is another kind of error
	 */
	@Override
	public final void rollRandomEvent (final MomSessionVariables mom)
		throws RecordNotFoundException, MomException
	{
		if ((mom.getGeneralPublicKnowledge ().getTurnNumber () > mom.getSessionDescription ().getDifficultyLevel ().getEventMinimumTurnNumber ()) &&
			(mom.getGeneralPublicKnowledge ().getTurnNumber () > mom.getGeneralServerKnowledge ().getLastEventTurnNumber () +
				mom.getSessionDescription ().getDifficultyLevel ().getMinimumTurnsBetweenEvents ()))
		{
			final int turnsSinceLastEvent = mom.getGeneralPublicKnowledge ().getTurnNumber () - mom.getGeneralServerKnowledge ().getLastEventTurnNumber ();
			final int eventChance = turnsSinceLastEvent * mom.getSessionDescription ().getDifficultyLevel ().getEventChance ();
			final boolean triggerEvent = (getRandomUtils ().nextInt (EVENT_CHANCE_DIVISOR) < eventChance);
			
			log.debug ("Last event was " + turnsSinceLastEvent + " turns ago.  Chance of event this turn = " + eventChance + " / " + EVENT_CHANCE_DIVISOR + ".  Event = " + triggerEvent);
			if (triggerEvent)
			{
				// Just because we want an event doesn't necessarily mean we'll get one.  First make a list of which types of event will be valid.
				final List<Event> eventChoices = new ArrayList<Event> ();

				for (final Event event : mom.getServerDB ().getEvent ())
					if (((event.getOverrideMinimumTurnNumber () == null) ||
						(mom.getGeneralPublicKnowledge ().getTurnNumber () > event.getOverrideMinimumTurnNumber ())) &&
							(getRandomEventTargeting ().isAnyValidTargetForEvent (event, mom)))
							
						eventChoices.add (event);
				
				if (eventChoices.isEmpty ())
					log.debug ("Wanted an event, but none had valid targets");
				else
				{
					final Event event = eventChoices.get (getRandomUtils ().nextInt (eventChoices.size ()));
					log.debug ("Triggering event " + event.getEventID () + " " + event.getEventName ().get (0).getText ());
					
					getRandomEventTargeting ().triggerEvent (event, mom);
					mom.getGeneralServerKnowledge ().setLastEventTurnNumber (mom.getGeneralPublicKnowledge ().getTurnNumber ());
				}
			}
		}
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Random rvent targeting
	 */
	public final RandomEventTargeting getRandomEventTargeting ()
	{
		return randomEventTargeting;
	}

	/**
	 * @param e Random rvent targeting
	 */
	public final void setRandomEventTargeting (final RandomEventTargeting e)
	{
		randomEventTargeting = e;
	}
}