package momime.server.messages.process;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.clienttoserver.ChooseCustomFlagColourMessage;
import momime.common.messages.servertoclient.YourFlagColourIsOkMessage;
import momime.common.utils.KnownWizardUtils;
import momime.server.MomSessionVariables;

/**
 * Message client sends to server to give the server the custom colour of their flag
 */
public final class ChooseCustomFlagColourMessageImpl extends ChooseCustomFlagColourMessage implements PostSessionClientToServerMessage
{
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws RecordNotFoundException If we can't find either the true or known copy of this wizard's details 
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		// No validation here or return message to send here, we just store it
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		// Remember choice in true wizard details
		getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
			sender.getPlayerDescription ().getPlayerID (), "ChooseCustomFlagColourMessageImpl (T)").setCustomFlagColour (getFlagColour ());
		
		// Remember choice in player's knowledge of themselves (nobody else knows about them at this point during game setup)
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (),
			sender.getPlayerDescription ().getPlayerID (), "ChooseCustomFlagColourMessageImpl (K)").setCustomFlagColour (getFlagColour ());

		final YourFlagColourIsOkMessage reply = new YourFlagColourIsOkMessage ();
		reply.setFlagColour (getFlagColour ());
		sender.getConnection ().sendMessageToClient (reply);
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