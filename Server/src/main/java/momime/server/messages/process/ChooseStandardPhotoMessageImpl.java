package momime.server.messages.process;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.ChooseStandardPhotoMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.YourPhotoIsOkMessage;
import momime.server.MomSessionVariables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Message we send to the server when we've chosen a custom wizard and then which standard (wizard) photo we want
 */
public final class ChooseStandardPhotoMessageImpl extends ChooseStandardPhotoMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ChooseStandardPhotoMessageImpl.class);
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Check is valid - we don't need the record from the DB, we just need to prove that it exists
		try
		{
			mom.getServerDB ().findWizard (getPhotoID (), "ChooseStandardPhotoMessageImpl");

			// Remember choice on server
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();
			ppk.setStandardPhotoID (getPhotoID ());

			// Tell client choice was OK
			sender.getConnection ().sendMessageToClient (new YourPhotoIsOkMessage ());
		}
		catch (final RecordNotFoundException e)
		{
			// Send error back to client
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " tried to choose invalid photo ID \"" + getPhotoID () + "\"");

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("Photo choice invalid, please try again");
			sender.getConnection ().sendMessageToClient (reply);
		}
	}
}