package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_4.ChooseWizardMessage;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Message we send to the server when we choose which wizard we want to be
 */
public final class ChooseWizardMessageImpl extends ChooseWizardMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChooseWizardMessageImpl.class.getName ());
	
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
		log.entering (ChooseWizardMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getWizardID ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;

		mom.getPlayerMessageProcessing ().chooseWizard
			(getWizardID (), sender, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

		log.exiting (ChooseWizardMessageImpl.class.getName (), "process");
	}
}
