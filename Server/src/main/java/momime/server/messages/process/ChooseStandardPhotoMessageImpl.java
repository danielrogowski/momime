package momime.server.messages.process;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.clienttoserver.ChooseStandardPhotoMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.YourPhotoIsOkMessage;
import momime.common.utils.KnownWizardUtils;
import momime.server.MomSessionVariables;

/**
 * Message we send to the server when we've chosen a custom wizard and then which standard (wizard) photo we want
 */
public final class ChooseStandardPhotoMessageImpl extends ChooseStandardPhotoMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ChooseStandardPhotoMessageImpl.class);

	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we can't find either the true or known copy of this wizard's details 
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Check is valid - we don't need the record from the DB, we just need to prove that it exists
		if (mom.getServerDB ().getWizards ().stream ().anyMatch (w -> w.getWizardID ().equals (getPhotoID ())))
		{
			// Remember choice in true wizard details
			getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
				sender.getPlayerDescription ().getPlayerID (), "ChooseStandardPhotoMessageImpl (T)").setStandardPhotoID (getPhotoID ());
			
			// Remember choice in player's knowledge of themselves (nobody else knows about them at this point during game setup)
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
			getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (),
				sender.getPlayerDescription ().getPlayerID (), "ChooseStandardPhotoMessageImpl (K)").setStandardPhotoID (getPhotoID ());

			// Tell client choice was OK
			final YourPhotoIsOkMessage reply = new YourPhotoIsOkMessage ();
			reply.setStandardPhotoID (getPhotoID ());
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Send error back to client
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " tried to choose invalid photo ID \"" + getPhotoID () + "\"");

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("Photo choice invalid, please try again");
			sender.getConnection ().sendMessageToClient (reply);
		}
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