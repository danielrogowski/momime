package momime.server.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageOffer;
import momime.common.messages.NewTurnMessageOfferHero;
import momime.common.messages.NewTurnMessageOfferItem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.RequestAcceptOfferMessage;
import momime.common.messages.servertoclient.OfferAcceptedMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.HeroItemUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;

/**
 * Server has previously sent us an offer to hire a hero, units or buy an item, and we want to accept the offer.
 */
public final class RequestAcceptOfferMessageImpl extends RequestAcceptOfferMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestAcceptOfferMessageImpl.class);
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Hero item utils */
	private HeroItemUtils heroItemUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws IOException For other types of failures
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Find the offer
		final NewTurnMessageOffer offer = mom.getGeneralServerKnowledge ().getOffer ().stream ().filter
			(o -> o.getOfferURN () == getOfferURN ()).findAny ().orElse (null);

		// Find how much money we have
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		final int gold = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		
		// Validation
		String error = null;
		if (offer == null)
			error = "Cannot find the offer you are trying to accept";

		else if (gold < offer.getCost ())
			error = "You no longer have enough coins to afford this offer";
		
		else if (offer.getOfferForPlayerID () != sender.getPlayerDescription ().getPlayerID ())
			error = "You cannot accept somebody else's offer";
		
		// If its an offer for an item, we also have to make sure the item is still available, in case the ultra rare case where since the offer was
		// generated, someone else got the same offer and bought it faster than we did, or maybe even we won the item ourselves from a treasure reward
		else if (offer instanceof NewTurnMessageOfferItem)
		{
			final NewTurnMessageOfferItem itemOffer = (NewTurnMessageOfferItem) offer;
			if (getHeroItemUtils ().findHeroItemURN (itemOffer.getHeroItem ().getHeroItemURN (), mom.getGeneralServerKnowledge ().getAvailableHeroItem ()) == null)
				error = "The item you tried to buy is no longer available";
		}
		
		// Similarly if its an offer for a hero, we have to make sure the hero is still available too, its possible since getting the offer that we won them from a treasure reward
		else if (offer instanceof NewTurnMessageOfferHero)
		{
			final NewTurnMessageOfferHero heroOffer = (NewTurnMessageOfferHero) offer;
			final MemoryUnit hero = getUnitUtils ().findUnitURN (heroOffer.getHero ().getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
			if (hero == null)
				error = "Cannot find the hero you are trying to hire";
			
			else if (hero.getStatus () != UnitStatusID.GENERATED)
				error = "The hero you are trying to hire is no longer available";
		}
		
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
			// All ok - deduct money & send to client
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -offer.getCost ());
			getServerResourceCalculations ().sendGlobalProductionValues (sender, null);
			
			// Mark offer as accepted
			final OfferAcceptedMessage msg = new OfferAcceptedMessage ();
			msg.setOfferURN (getOfferURN ());
			sender.getConnection ().sendMessageToClient (msg);
			
			// Remove it so they can't accept it twice
			mom.getGeneralServerKnowledge ().getOffer ().remove (offer);
			
			// Actually give the units or items on offer
		}
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
}