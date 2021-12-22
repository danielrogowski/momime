package momime.server;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.MultiplayerClientConnection;

import momime.common.messages.servertoclient.NewGameDatabaseMessage;

/**
 * Extends server-to-client connection so that as soon as the connection is established, we send new game database to the client
 */
public final class MomClientConnection extends MultiplayerClientConnection
{
	/** Message to send new game database to clients as they connect */
	private final NewGameDatabaseMessage newGameDatabaseMessage;
	
	/**
	 * Creates a new thread for handling requests from this client
	 * @param threadName Name for this thread
	 * @param aNewGameDatabaseMessage Message to send new game database to clients as they connect 
	 */
	public MomClientConnection (final String threadName, final NewGameDatabaseMessage aNewGameDatabaseMessage)
	{
		super (threadName);
		newGameDatabaseMessage = aNewGameDatabaseMessage;
	}
	
	/**
	 * Send new game database to the client
	 * 
	 * @throws IOException If we are unable to process the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 */
	@Override
	protected final void streamsInitialized ()
		throws IOException, JAXBException, XMLStreamException
	{
		sendMessageToClient (newGameDatabaseMessage);
	}
}