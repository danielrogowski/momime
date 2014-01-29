package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.CombatAutoControlMessage;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Message client sends to server when they want the server to use its AI to move their combat units for this combat turn.
 * Client re-sends this each combat turn - the server has no persistent memory of whether a client has
 * Auto switched on or not - this makes it easier to allow the player to switch Auto back off again.
 */
public final class CombatAutoControlMessageImpl extends CombatAutoControlMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CombatAutoControlMessageImpl.class.getName ());

	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (CombatAutoControlMessageImpl.class.getName (), "process", getCombatLocation ());

		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		mom.getCombatProcessing ().progressCombat ((OverlandMapCoordinatesEx) getCombatLocation (), false, true, mom);

		log.exiting (CombatAutoControlMessageImpl.class.getName (), "process");
	}
}
