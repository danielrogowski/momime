package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.clienttoserver.AlchemyMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.utils.PlayerServerUtils;

/**
 * Client sends this to server when they want to convert Gold to Mana or vice versa
 */
public final class AlchemyMessageImpl extends AlchemyMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (AlchemyMessageImpl.class);

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Player utils */
	private PlayerServerUtils playerServerUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		String error = null;
		String toProductionTypeID = null;

		// Check value is sensible
		if (!getPlayerServerUtils ().isPlayerTurn (sender, mom.getGeneralPublicKnowledge (), mom.getSessionDescription ().getTurnSystem ()))
			error = "You can't use alchemy when it isn't your turn";
		
		else if (getFromValue () <= 0)
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
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
				sender.getPlayerDescription ().getPlayerID (), "AlchemyMessageImpl");

			final int toValue;
			if (getPlayerPickUtils ().getQuantityOfPick (wizardDetails.getPick (), CommonDatabaseConstants.RETORT_ID_ALCHEMY) > 0)
				toValue = getFromValue ();
			else
				toValue = getFromValue () / 2;

			// Make changes
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), getFromProductionTypeID (), -getFromValue ());
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), toProductionTypeID, toValue);

			// Send updated amounts to client
			getServerResourceCalculations ().sendGlobalProductionValues (sender, null, false);
		}
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

	/**
	 * @return Player utils
	 */
	public final PlayerServerUtils getPlayerServerUtils ()
	{
		return playerServerUtils;
	}
	
	/**
	 * @param utils Player utils
	 */
	public final void setPlayerServerUtils (final PlayerServerUtils utils)
	{
		playerServerUtils = utils;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}