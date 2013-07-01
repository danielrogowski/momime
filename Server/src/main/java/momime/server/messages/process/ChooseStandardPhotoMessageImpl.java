package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_4.ChooseStandardPhotoMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.servertoclient.v0_9_4.YourPhotoIsOkMessage;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.server.IMomSessionVariables;

import com.ndg.multiplayer.server.ProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Message we send to the server when we've chosen a custom wizard and then which standard (wizard) photo we want
 */
public final class ChooseStandardPhotoMessageImpl extends ChooseStandardPhotoMessage implements ProcessableClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChooseStandardPhotoMessageImpl.class.getName ());
	
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
		log.entering (ChooseStandardPhotoMessageImpl.class.getName (), "process", new String [] {new Integer (sender.getPlayerDescription ().getPlayerID ()).toString (), getPhotoID ()});

		final IMomSessionVariables mom = (IMomSessionVariables) thread;

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
			log.warning (sender.getPlayerDescription ().getPlayerName () + " tried to choose invalid photo ID \"" + getPhotoID () + "\"");

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("Photo choice invalid, please try again");
			sender.getConnection ().sendMessageToClient (reply);
		}

		log.exiting (ChooseStandardPhotoMessageImpl.class.getName (), "process");
	}
}
