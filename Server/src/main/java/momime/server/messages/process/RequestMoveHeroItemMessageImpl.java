package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.MomException;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.clienttoserver.RequestMoveHeroItemMessage;
import momime.common.messages.servertoclient.RemoveUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.HeroItemUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;

/**
 * Client sends to request that a hero item be moved from one location to another.
 * The to/from locations can be their bank, the anvil, or actually being used by a hero.
 */
public final class RequestMoveHeroItemMessageImpl extends RequestMoveHeroItemMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (RequestMoveHeroItemMessageImpl.class);

	/** Hero item utils */
	private HeroItemUtils heroItemUtils;
	
	/** Hero item calculations */
	private HeroItemCalculations heroItemCalculations;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		log.trace ("Entering process: Hero Item URN " + getHeroItemURN () + " from " + getFromLocation () + " to " + getToLocation ());
		
		final MomSessionVariables mom = (MomSessionVariables) thread;

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		String error = null;
		NumberedHeroItem item = null;
		
		// Validate that the item is where they claim it is
		if (getFromLocation () == null)
			error = "You tried to move a hero item from an unspecified location.";
		else
			switch (getFromLocation ())
			{
				case UNASSIGNED:
					item = getHeroItemUtils ().findHeroItemURN (getHeroItemURN (), priv.getUnassignedHeroItem ());
					if (item == null)
						error = "Cannot find the hero item you are trying to move from your bank.";
					break;
					
				default:
					error = "You tried to move a hero item from an unrecognized location " + getFromLocation () + ".";
			}
		
		// Validate destination
		if (error == null)
		{
			if (getToLocation () == null)
				error = "You tried to move a hero item to an unspecified location.";
			else
				switch (getToLocation ())
				{
					// Breaking item on the anvil is always OK
					case DESTROY:
						break;

					default:
						error = "You tried to move a hero item to an unrecognized location " + getToLocation () + ".";
				}
		}
		
		// All ok?
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Remove item from location
			switch (getFromLocation ())
			{
				case UNASSIGNED:
					// Remove on server
					priv.getUnassignedHeroItem ().remove (item);
					
					// Remove on client
					final RemoveUnassignedHeroItemMessage msg = new RemoveUnassignedHeroItemMessage ();
					msg.setHeroItemURN (getHeroItemURN ());
					sender.getConnection ().sendMessageToClient (msg);					
					break;
					
				default:
					throw new MomException ("RequestMoveHeroItemMessage doesn't know how to remove hero item from location " + getFromLocation ());
			}
			
			// Move item new location
			switch (getToLocation ())
			{
				// Break item on the anvil
				case DESTROY:
					final int manaGained = getHeroItemCalculations ().calculateCraftingCost (item, mom.getServerDB ()) / 2;
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, manaGained);
					getServerResourceCalculations ().sendGlobalProductionValues (sender, 0);

					break;

				default:
					throw new MomException ("RequestMoveHeroItemMessage doesn't know how to move hero item to location " + getToLocation ());
			}
		}

		log.trace ("Exiting process");
	}

	/**
	 * @return Hero item utils
	 */
	public final HeroItemUtils getHeroItemUtils ()
	{
		return heroItemUtils;
	}

	/**
	 * @param util Hero item utils
	 */
	public final void setHeroItemUtils (final HeroItemUtils util)
	{
		heroItemUtils = util;
	}

	/**
	 * @return Hero item calculations
	 */
	public final HeroItemCalculations getHeroItemCalculations ()
	{
		return heroItemCalculations;
	}

	/**
	 * @param calc Hero item calculations
	 */
	public final void setHeroItemCalculations (final HeroItemCalculations calc)
	{
		heroItemCalculations = calc;
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