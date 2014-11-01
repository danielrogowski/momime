package momime.server.messages.process;

import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.ChooseCustomFlagColourMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Message client sends to server to give the server the custom colour of their flag
 */
public final class ChooseCustomFlagColourMessageImpl extends ChooseCustomFlagColourMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChooseCustomFlagColourMessageImpl.class);
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
	{
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID ());

		// No validation here or return message to send here, we just store it
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();
		ppk.setCustomFlagColour (getFlagColour ());

		log.trace ("Exiting process");
	}
}