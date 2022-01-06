package momime.server.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.Event;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.servertoclient.AttackCitySpellResult;
import momime.common.messages.servertoclient.RandomEventMessage;
import momime.server.MomSessionVariables;
import momime.server.knowledge.ServerGridCellEx;

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
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Random city events */
	private RandomCityEvents randomCityEvents;
	
	/**
	 * Rolls to see if server should trigger a random event this turn 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void rollRandomEvent (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
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
	 * Rolls to see if an active event with a duration should end 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there is another kind of error
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void rollToEndRandomEvents (final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// Is there a good/bad moon or a conjunction of red/blue/green nodes?
		if (mom.getGeneralPublicKnowledge ().getConjunctionEventID () != null)
		{
			final Event eventDef = mom.getServerDB ().findEvent (mom.getGeneralPublicKnowledge ().getConjunctionEventID (), "rollToEndRandomEvents");
			final int turnsPastMinimum = mom.getGeneralPublicKnowledge ().getTurnNumber () - mom.getGeneralServerKnowledge ().getConjunctionStartedTurnNumber () - eventDef.getMinimumDuration ();
			if (turnsPastMinimum > 0)
			{
				int chance = eventDef.getInitialEndingChance ();
				if ((eventDef.getIncreaseEndingChance () != null) && (turnsPastMinimum > 1))
					chance = chance + (eventDef.getIncreaseEndingChance () * (turnsPastMinimum-1));
				
				final boolean ending = (getRandomUtils ().nextInt (100) < chance);
				log.debug ("Event " + mom.getGeneralPublicKnowledge ().getConjunctionEventID () + " " + eventDef.getEventName ().get (0).getText () +
					" has " + chance + "% chance of ending this turn.  Ending = " + ending);
				
				if (ending)
					getRandomEventTargeting ().cancelEvent (eventDef, mom);
			}
		}
		
		// Also check population events on cities
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(z).getRow ().get (y).getCell ().get (x);
					final OverlandMapCityData cityData = gc.getCityData ();
					
					if ((cityData != null) && (cityData.getPopulationEventID () != null))
					{
						final Event eventDef = mom.getServerDB ().findEvent (cityData.getPopulationEventID (), "rollToEndRandomEvents");
						final int turnsPastMinimum = mom.getGeneralPublicKnowledge ().getTurnNumber () - gc.getPopulationEventStartedTurnNumber () - eventDef.getMinimumDuration ();
						if (turnsPastMinimum > 0)
						{
							int chance = eventDef.getInitialEndingChance ();
							if ((eventDef.getIncreaseEndingChance () != null) && (turnsPastMinimum > 1))
								chance = chance + (eventDef.getIncreaseEndingChance () * (turnsPastMinimum-1));
							
							final boolean ending = (getRandomUtils ().nextInt (100) < chance);
							final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
							log.debug ("Event " + cityData.getPopulationEventID () + " " + eventDef.getEventName ().get (0).getText () +
								" on city at " + cityLocation + " has " + chance + "% chance of ending this turn.  Ending = " + ending);
							
							if (ending)
								getRandomCityEvents ().cancelCityEvent (eventDef, cityLocation, mom);
						}
					}
				}
	}
	
	/**
	 * @param eventID Which kind of event it is
	 * @param targetPlayerID If its an event that targets a wizard, then who was targeted
	 * @param citySizeID If its an event that targets a city, then the size of the city (since all players receiving the message may not be able to see the city)
	 * @param cityName If its an event that targets a city, then the name of the city (since all players receiving the message may not be able to see the city)
	 * @param mapFeatureID If its an event that targets a city mineral deposit, then which kind of mineral it is
	 * @param heroItemName If its an event that grants a hero item, then the name of the item
	 * @param goldAmount If its an event that takes or gives gold, then how much gold
	 * @param conjunction Tells the client to update their conjunctionEventID
	 * @param ending Whether we're broadcasting the start or end of the event
	 * @param players List of players in the session
	 * @param attackCitySpellResult Counts of how many units, buildings and population were killed by Earthquake or Great Meteor 
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void sendRandomEventMessage (final String eventID, final Integer targetPlayerID, final String citySizeID, final String cityName,
		final String mapFeatureID, final String heroItemName, final Integer goldAmount, final boolean conjunction, final boolean ending,
		final List<PlayerServerDetails> players, final AttackCitySpellResult attackCitySpellResult)
		throws JAXBException, XMLStreamException
	{
		final RandomEventMessage msg = new RandomEventMessage ();
		msg.setEventID (eventID);
		msg.setTargetPlayerID (targetPlayerID);
		msg.setCitySizeID (citySizeID);
		msg.setCityName (cityName);
		msg.setMapFeatureID (mapFeatureID);
		msg.setHeroItemName (heroItemName);
		msg.setGoldAmount (goldAmount);
		msg.setConjunction (conjunction);
		msg.setEnding (ending);
		msg.setAttackCitySpellResult (attackCitySpellResult);
		
		getMultiplayerSessionServerUtils ().sendMessageToAllClients (players, msg);
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

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}

	/**
	 * @return Random city events
	 */
	public final RandomCityEvents getRandomCityEvents ()
	{
		return randomCityEvents;
	}

	/**
	 * @param e Random city events
	 */
	public final void setRandomCityEvents (final RandomCityEvents e)
	{
		randomCityEvents = e;
	}
}