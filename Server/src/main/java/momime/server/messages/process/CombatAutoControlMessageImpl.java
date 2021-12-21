package momime.server.messages.process;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.CombatAutoControlMessage;
import momime.server.MomSessionVariables;
import momime.server.process.CombatProcessing;

/**
 * Message client sends to server when they want the server to use its AI to move their combat units for this combat turn.
 * Client re-sends this each combat turn - the server has no persistent memory of whether a client has
 * Auto switched on or not - this makes it easier to allow the player to switch Auto back off again.
 */
public final class CombatAutoControlMessageImpl extends CombatAutoControlMessage implements PostSessionClientToServerMessage
{
	/** Combat processing */
	private CombatProcessing combatProcessing;

	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@SuppressWarnings ("unused")
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		getCombatProcessing ().progressCombat ((MapCoordinates3DEx) getCombatLocation (), false, true, mom);
	}

	/**
	 * @return Combat processing
	 */
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param proc Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing proc)
	{
		combatProcessing = proc;
	}
}