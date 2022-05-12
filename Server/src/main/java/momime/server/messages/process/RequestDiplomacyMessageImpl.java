package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.clienttoserver.RequestDiplomacyMessage;
import momime.common.messages.servertoclient.EndDiplomacyMessage;
import momime.common.messages.servertoclient.RequestAudienceMessage;
import momime.server.MomSessionVariables;
import momime.server.utils.KnownWizardServerUtils;

/**
 * We make a proposal, offer or demand to another wizard, the exact nature of which is set by the action value.
 */
public final class RequestDiplomacyMessageImpl extends RequestDiplomacyMessage implements PostSessionClientToServerMessage
{
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;

	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
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
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Have to deal here with the fact that maybe we know the wizard we want to talk to, but they don't know us.  Perhaps we
		// only know them because they cast an overland enchantment or banished someone.  In this case we don't show the
		// animation for meeting, as this is superceeded by the animation for us wanting to talk to them.
		// Don't need to test whether we need to do this.  If wizard already knows us then the routine will simply do nothing and exit.
		getKnownWizardServerUtils ().meetWizard (sender.getPlayerDescription ().getPlayerID (), getTalkToPlayerID (), false, mom);
		
		final PlayerServerDetails talkToPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), getTalkToPlayerID (), "RequestDiplomacyMessageImpl");
		switch (getAction ())
		{
			// If the player we want to talk to is an AI player then they can immediately decide if they want to talk to us
			// If the player we want to talk to is another human player then open up their DiplomacyUI and ask them if they will talk to us
			case INITIATE_DIPLOMACY:
				if (talkToPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				{
					final RequestAudienceMessage msg = new RequestAudienceMessage ();
					msg.setTalkFromPlayerID (sender.getPlayerDescription ().getPlayerID ());
					msg.setVisibleRelationScoreID (getVisibleRelationScoreID ());
					msg.setInitiatingRequest (true);
					talkToPlayer.getConnection ().sendMessageToClient (msg);
				}
				else
				{
					throw new IOException ("Rules for whether AI player has patience to talk to you not yet written");
				}
				break;
				
			case ACCEPT_TALKING:
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
				break;
				
			case REJECT_TALKING:
				if (talkToPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				{
					final EndDiplomacyMessage msg = new EndDiplomacyMessage ();
					msg.setTalkFromPlayerID (sender.getPlayerDescription ().getPlayerID ());
					talkToPlayer.getConnection ().sendMessageToClient (msg);
				}
				
				// Don't need to do anything to refuse talking to AI players, just nothing happens
				break;
				
			default:
				throw new IOException ("Server code not yet written for diplomacy action " + getAction ());
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
	
	/**
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
	}
}