package momime.server.messages.process;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.UploadCustomPhotoMessage;
import momime.common.messages.servertoclient.YourPhotoIsOkMessage;

/**
 * Message clients send to the server when they choose a custom photo
 */
public final class UploadCustomPhotoMessageImpl extends UploadCustomPhotoMessage implements PostSessionClientToServerMessage
{
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void process (@SuppressWarnings ("unused") final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException
	{
		// No validation here, just remember the photo and send back confirmation
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();
		ppk.setCustomPhoto (getNdgBmpImage ());

		sender.getConnection ().sendMessageToClient (new YourPhotoIsOkMessage ());
	}
}