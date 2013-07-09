package momime.server.messages.process;

import java.util.logging.Logger;

import momime.common.messages.clienttoserver.v0_9_4.ChooseCustomFlagColourMessage;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Message client sends to server to give the server the custom colour of their flag
 */
public final class ChooseCustomFlagColourMessageImpl extends ChooseCustomFlagColourMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChooseCustomFlagColourMessageImpl.class.getName ());
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
	{
		log.entering (ChooseCustomFlagColourMessageImpl.class.getName (), "process", sender.getPlayerDescription ().getPlayerID ());

		// No validation here or return message to send here, we just store it
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();
		ppk.setCustomFlagColour (getFlagColour ());

		log.entering (ChooseCustomFlagColourMessageImpl.class.getName (), "process");
	}
}
