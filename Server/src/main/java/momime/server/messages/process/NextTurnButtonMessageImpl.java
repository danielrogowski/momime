package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_4.NextTurnButtonMessage;
import momime.server.MomSessionThread;
import momime.server.process.PlayerMessageProcessing;

import com.ndg.multiplayer.server.IProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Message clients send out when the next turn button is clicked.
 * Server figures out the effect, based on the type of game (one-at-a-time or simultaneous).
 */
public final class NextTurnButtonMessageImpl extends NextTurnButtonMessage implements IProcessableClientToServerMessage
{
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (NextTurnButtonMessageImpl.class.getName (), "process", sender.getPlayerDescription ().getPlayerID ());

		PlayerMessageProcessing.nextTurnButton ((MomSessionThread) thread, sender, debugLogger);

		debugLogger.exiting (NextTurnButtonMessageImpl.class.getName (), "process");
	}
}
