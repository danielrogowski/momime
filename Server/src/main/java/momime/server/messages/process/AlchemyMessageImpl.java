package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.clienttoserver.v0_9_4.AlchemyMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.server.IMomSessionVariables;

import com.ndg.multiplayer.server.ProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Client sends this to server when they want to convert Gold to Mana or vice versa
 */
public final class AlchemyMessageImpl extends AlchemyMessage implements ProcessableClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (AlchemyMessageImpl.class.getName ());
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException
	{
		log.entering (AlchemyMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getFromProductionTypeID (), new Integer (getFromValue ()).toString ()});

		final IMomSessionVariables mom = (IMomSessionVariables) thread;
		
		String error = null;
		String toProductionTypeID = null;

		// Check value is sensible
		if (getFromValue () <= 0)
			error = "Must specify a value of at least 1 to use alchemy";

		else if (getFromProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD))
			toProductionTypeID = CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA;

		else if (getFromProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA))
			toProductionTypeID = CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD;

		else
			error = "Can only use Alchemy to convert from Gold or Mana";

		// Check we have enough of the resource
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		if (toProductionTypeID != null)
			if (mom.getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), getFromProductionTypeID ()) < getFromValue ())
				error = "You don't have enough of the requested type of production to convert";

		if (error != null)
		{
			// Return error
			log.warning (ChooseInitialSpellsMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// All ok - work out 'to' amount
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();

			final int toValue;
			if (mom.getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY) > 0)
				toValue = getFromValue ();
			else
				toValue = getFromValue () / 2;

			// Make changes
			mom.getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), getFromProductionTypeID (), -getFromValue ());
			mom.getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), toProductionTypeID, toValue);

			// Send updated amounts to client
			mom.getServerResourceCalculations ().sendGlobalProductionValues (sender, 0);
		}

		log.exiting (AlchemyMessageImpl.class.getName (), "process");
	}
}
