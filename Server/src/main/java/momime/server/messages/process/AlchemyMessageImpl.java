package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.AlchemyMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.calculations.ServerResourceCalculations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Client sends this to server when they want to convert Gold to Mana or vice versa
 */
public final class AlchemyMessageImpl extends AlchemyMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AlchemyMessageImpl.class);

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void process (@SuppressWarnings ("unused") final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID () + ", " + getFromProductionTypeID () + ", " + getFromValue ());

		String error = null;
		String toProductionTypeID = null;

		// Check value is sensible
		if (getFromValue () <= 0)
			error = "Must specify a value of at least 1 to use alchemy";

		else if (getFromProductionTypeID ().equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD))
			toProductionTypeID = CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA;

		else if (getFromProductionTypeID ().equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA))
			toProductionTypeID = CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD;

		else
			error = "Can only use Alchemy to convert from Gold or Mana";

		// Check we have enough of the resource
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		if (toProductionTypeID != null)
			if (getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), getFromProductionTypeID ()) < getFromValue ())
				error = "You don't have enough of the requested type of production to convert";

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
			// All ok - work out 'to' amount
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();

			final int toValue;
			if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_ALCHEMY) > 0)
				toValue = getFromValue ();
			else
				toValue = getFromValue () / 2;

			// Make changes
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), getFromProductionTypeID (), -getFromValue ());
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), toProductionTypeID, toValue);

			// Send updated amounts to client
			getServerResourceCalculations ().sendGlobalProductionValues (sender, null);
		}

		log.trace ("Exiting process");
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
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