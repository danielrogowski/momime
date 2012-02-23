package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_4.ChooseWizardMessage;
import momime.server.MomSessionThread;
import momime.server.process.PlayerMessageProcessing;

import com.ndg.multiplayer.server.IProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Message we send to the server when we choose which wizard we want to be
 */
public final class ChooseWizardMessageImpl extends ChooseWizardMessage implements IProcessableClientToServerMessage
{
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		debugLogger.entering (ChooseWizardMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getWizardID ()});

		final MomSessionThread mom = (MomSessionThread) thread;

		PlayerMessageProcessing.chooseWizard (getWizardID (), sender, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDBLookup (), debugLogger);

		debugLogger.exiting (ChooseWizardMessageImpl.class.getName (), "process");
	}
}
