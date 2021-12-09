package momime.server.events;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Event;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;

/**
 * Deals with random events which pick a wizard target:
 * Great Meteor, The Gift, Diplomatic Marriage, Earthquake, Piracy, Plague, Rebellion, Donation, Depletion, New Minerals, Population Boom
 */
public final class RandomWizardEventsImpl implements RandomWizardEvents
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RandomWizardEventsImpl.class);
	
	/** Random city events */
	private RandomCityEvents randomCityEvents;

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Rolls random events */
	private RandomEvents randomEvents;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/**
	 * Can only call this on events that are targeted at wizards
	 * 
	 * @param event Event we want to find a target for
	 * @param player Wizard we are considering for the event
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the wizard is a valid target for the event or not
	 * @throws RecordNotFoundException If we can't find the definition for a unit stationed in one of the wizard's cities
	 */
	@Override
	public final boolean isWizardValidTargetForEvent (final Event event, final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		boolean valid = false;

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		if ((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) && (pub.getWizardState () == WizardState.ACTIVE))
		{
			// Do we need to find a target city owned by the wizard?  Great Meteor, Earthquake, Plague, Rebellion, Depletion, New Minerals, Population Boom
			if ((event.isTargetCity () != null) && (event.isTargetCity ()))
			{
				int z = 0;
				while ((!valid) && (z < mom.getSessionDescription ().getOverlandMapSize ().getDepth ()))
				{
					int y = 0;
					while ((!valid) && (y < mom.getSessionDescription ().getOverlandMapSize ().getHeight ()))
					{
						int x = 0;
						while ((!valid) && (x < mom.getSessionDescription ().getOverlandMapSize ().getWidth ()))
						{
							if (getRandomCityEvents ().isCityValidTargetForEvent (event, new MapCoordinates3DEx (x, y, z), player.getPlayerDescription ().getPlayerID (), mom))
								valid = true;
							else
								x++;
						}
						y++;
					}
					z++;
				}
			}
			
			// Do we need to find a target neutral city?  Diplomatic Marriage
			else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_DIPLOMATIC_MARRIAGE))
			{
				PlayerServerDetails raiders = null;
				for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
				{
					final MomPersistentPlayerPublicKnowledge thisPub = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
					if (CommonDatabaseConstants.WIZARD_ID_RAIDERS.equals (thisPub.getWizardID ()))
						raiders = thisPlayer;
				}
				
				if (raiders != null)
				{
					int z = 0;
					while ((!valid) && (z < mom.getSessionDescription ().getOverlandMapSize ().getDepth ()))
					{
						int y = 0;
						while ((!valid) && (y < mom.getSessionDescription ().getOverlandMapSize ().getHeight ()))
						{
							int x = 0;
							while ((!valid) && (x < mom.getSessionDescription ().getOverlandMapSize ().getWidth ()))
							{
								if (getRandomCityEvents ().isCityValidTargetForEvent (event, new MapCoordinates3DEx (x, y, z), raiders.getPlayerDescription ().getPlayerID (), mom))
									valid = true;
								else
									x++;
							}
							y++;
						}
						z++;
					}
				}
			}
			
			// Do we need to find a target hero item?  The Gift
			else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_GIFT))
				valid = !mom.getGeneralServerKnowledge ().getAvailableHeroItem ().isEmpty ();
				
			// Do we have to have some gold?  Piracy
			else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_PIRACY))
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				valid = (getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD) > 0);
			}
				
			// Events with no requirements other than simply picking a wizard?  Donation
			else
				valid = true;
		}
		
		return valid;
	}

	/**
	 * @param event Event to trigger
	 * @param targetWizard Wizard the event is being triggered for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws MomException If there is another kind of error
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void triggerWizardEvent (final Event event, final PlayerServerDetails targetWizard, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// Do we need to pick a target city owned by the wizard?  Great Meteor, Earthquake, Plague, Rebellion, Depletion, New Minerals, Population Boom
		if ((event.isTargetCity () != null) && (event.isTargetCity ()))
		{
			final List<MapCoordinates3DEx> cityLocations = new ArrayList<MapCoordinates3DEx> ();
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
						if (getRandomCityEvents ().isCityValidTargetForEvent (event, cityLocation, targetWizard.getPlayerDescription ().getPlayerID (), mom))
							cityLocations.add (cityLocation);
					}
			
			if (cityLocations.isEmpty ())
				throw new MomException ("Event " + event.getEventID () + " was picked for wizard " + targetWizard.getPlayerDescription ().getPlayerName () +
					", but now finding no city is a valid target");
			
			final MapCoordinates3DEx targetCity = cityLocations.get (getRandomUtils ().nextInt (cityLocations.size ()));
			log.debug ("Triggering event " + event.getEventID () + " " + event.getEventName ().get (0).getText () + " on wizard " +
				targetWizard.getPlayerDescription ().getPlayerName () + " on city " + targetCity);
			
			getRandomCityEvents ().triggerCityEvent (event, targetWizard, targetCity, mom);
		}
		
		// Do we need to pick a target neutral city?  Diplomatic Marriage
		else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_DIPLOMATIC_MARRIAGE))
		{
			PlayerServerDetails raiders = null;
			for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
			{
				final MomPersistentPlayerPublicKnowledge thisPub = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
				if (CommonDatabaseConstants.WIZARD_ID_RAIDERS.equals (thisPub.getWizardID ()))
					raiders = thisPlayer;
			}
			
			if (raiders == null)
				throw new MomException ("Event " + event.getEventID () + " was picked for wizard " + targetWizard.getPlayerDescription ().getPlayerName () +
					", but can't find the raiders player");

			final List<MapCoordinates3DEx> cityLocations = new ArrayList<MapCoordinates3DEx> ();
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
						if (getRandomCityEvents ().isCityValidTargetForEvent (event, cityLocation, raiders.getPlayerDescription ().getPlayerID (), mom))
							cityLocations.add (cityLocation);
					}
			
			if (cityLocations.isEmpty ())
				throw new MomException ("Event " + event.getEventID () + " was picked for wizard " + targetWizard.getPlayerDescription ().getPlayerName () +
					", but now finding no raider city is a valid target");

			final MapCoordinates3DEx targetCity = cityLocations.get (getRandomUtils ().nextInt (cityLocations.size ()));
			log.debug ("Triggering event " + event.getEventID () + " " + event.getEventName ().get (0).getText () + " on wizard " +
				targetWizard.getPlayerDescription ().getPlayerName () + " on raider city " + targetCity);
			
			getRandomCityEvents ().triggerCityEvent (event, targetWizard, targetCity, mom);
		}
		
		else
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) targetWizard.getPersistentPlayerPrivateKnowledge ();
			
			// Do we need to pick a target hero item?  The Gift
			if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_GIFT))
			{
				final NumberedHeroItem item = mom.getGeneralServerKnowledge ().getAvailableHeroItem ().get
					(getRandomUtils ().nextInt (mom.getGeneralServerKnowledge ().getAvailableHeroItem ().size ()));
				log.debug ("Gift event giving wizard " + targetWizard.getPlayerDescription ().getPlayerName () + " hero item " + item.getHeroItemName ());
				
				getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
					null, null, null, item.getHeroItemName (), null, false, false, mom.getPlayers ());
				
				mom.getGeneralServerKnowledge ().getAvailableHeroItem ().remove (item);
				priv.getUnassignedHeroItem ().add (item);

				if (targetWizard.getPlayerDescription ().isHuman ())
				{
					final AddUnassignedHeroItemMessage msg = new AddUnassignedHeroItemMessage ();
					msg.setHeroItem (item);
					targetWizard.getConnection ().sendMessageToClient (msg);
				}
			}
	
			// Do we have to pick how much gold we'll lose?  Piracy
			else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_PIRACY))
			{
				final int gold = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				final int percent = 10 + getRandomUtils ().nextInt (16);
				int goldLost = (gold * percent) / 100;
				if (goldLost == 0)
					goldLost++;
				
				log.debug ("Piracy event, wizard " + targetWizard.getPlayerDescription ().getPlayerName () + " losing " + goldLost + " gold");
				
				getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
					null, null, null, null, goldLost, false, false, mom.getPlayers ());
				
				getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -goldLost);
				getServerResourceCalculations ().sendGlobalProductionValues (targetWizard, null, false);
			}
	
			// Do we have to pick how much gold we'll gain?  Donation
			else
			{
				// What's the maximum amount of gold that anyone has?
				int maxGold = 1000;
				for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
				{
					final MomPersistentPlayerPublicKnowledge thisPub = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
					if ((PlayerKnowledgeUtils.isWizard (thisPub.getWizardID ())) && (thisPub.getWizardState () == WizardState.ACTIVE))
					{
						final MomPersistentPlayerPrivateKnowledge thisPriv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
						final int gold = getResourceValueUtils ().findAmountStoredForProductionType (thisPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
						if (gold > maxGold)
							maxGold = gold;
					}
				}
				
				final int percent = 10 + getRandomUtils ().nextInt (16);
				final int goldGained = (maxGold * percent) / 100;
	
				log.debug ("Donation event, wizard " + targetWizard.getPlayerDescription ().getPlayerName () + " gaining " + goldGained + " gold");
				
				getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
					null, null, null, null, goldGained, false, false, mom.getPlayers ());
				
				getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, goldGained);
				getServerResourceCalculations ().sendGlobalProductionValues (targetWizard, null, false);
			}
		}
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

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
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
	 * @return Rolls random events
	 */
	public final RandomEvents getRandomEvents ()
	{
		return randomEvents;
	}

	/**
	 * @param e Rolls random events
	 */
	public final void setRandomEvents (final RandomEvents e)
	{
		randomEvents = e;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}
}