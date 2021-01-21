package momime.server.messages.process;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.ChooseCustomFlagColourMessage;

/**
 * Message client sends to server to give the server the custom colour of their flag
 */
public final class ChooseCustomFlagColourMessageImpl extends ChooseCustomFlagColourMessage implements PostSessionClientToServerMessage
{
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 */
	@Override
	public final void process (@SuppressWarnings ("unused") final MultiplayerSessionThread thread, final PlayerServerDetails sender)
	{
		// No validation here or return message to send here, we just store it
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();
		ppk.setCustomFlagColour (getFlagColour ());
	}
}