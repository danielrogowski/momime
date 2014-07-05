package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_5.ChatMessage;
import momime.common.messages.servertoclient.v0_9_5.BroadcastChatMessage;
import momime.server.MomSessionVariables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.MultiplayerServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client sends to server to send a chat message to other players
 */
public final class ChatMessageImpl extends ChatMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChatMessageImpl.class);

	/** Server-side multiplayer utils */
	private MultiplayerServerUtils multiplayerServerUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID ());

		final MomSessionVariables mom = (MomSessionVariables) thread;

		final BroadcastChatMessage msg = new BroadcastChatMessage ();
		msg.setPlayerName (sender.getPlayerDescription ().getPlayerName ());
		msg.setText (getText ());
		
		getMultiplayerServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);

		log.trace ("Exiting process");
	}

	/**
	 * @return Server-side multiplayer utils
	 */
	public final MultiplayerServerUtils getMultiplayerServerUtils ()
	{
		return multiplayerServerUtils;
	}
	
	/**
	 * @param utils Server-side multiplayer utils
	 */
	public final void setMultiplayerServerUtils (final MultiplayerServerUtils utils)
	{
		multiplayerServerUtils = utils;
	}
}