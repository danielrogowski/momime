package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.clienttoserver.AcceptDiplomacyMessage;
import momime.common.messages.servertoclient.RequestAudienceMessage;
import momime.server.MomSessionVariables;

/**
 * Another wizard wanted to talk to us, and we accept or refuse.
 */
public final class AcceptDiplomacyMessageImpl extends AcceptDiplomacyMessage implements PostSessionClientToServerMessage
{
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		if (isAccept ())
		{
			final MomSessionVariables mom = (MomSessionVariables) thread;
			
			final PlayerServerDetails talkToPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), getTalkToPlayerID (), "AcceptDiplomacyMessageImpl");
			if (talkToPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
			{
				final RequestAudienceMessage msg = new RequestAudienceMessage ();
				msg.setTalkFromPlayerID (sender.getPlayerDescription ().getPlayerID ());
				msg.setVisibleRelationScoreID (getVisibleRelationScoreID ());
				msg.setInitiatingRequest (false);		// Responding to request from the other wizard, rather than initiating diplomacy ourselves
				talkToPlayer.getConnection ().sendMessageToClient (msg);
			}
			else
			{
				throw new IOException ("Rules for accepting an AI player's request to talk not yet written");
			}
		}
	}
	
	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}
}