package momime.server;

import java.net.Socket;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.servertoclient.NewGameDatabaseMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.server.MultiplayerBaseServerThread;
import com.ndg.multiplayer.server.MultiplayerClientConnection;
import com.ndg.multiplayer.server.MultiplayerSessionServer;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.sessionbase.SessionDescription;

/**
 * Main server class to listen for client connection requests and manage list of sessions
 */
public final class MomServer extends MultiplayerSessionServer
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MomServer.class);
	
	/** Message to send new game database to clients as they connect */
	private NewGameDatabaseMessage newGameDatabaseMessage;

	/** Factory interface for creating MomSessionThreads */
	private MomSessionThreadFactory sessionThreadFactory;

	/**
	 * @throws DatatypeConfigurationException If there is a problem creating the DatatypeFactory
	 */
	public MomServer () throws DatatypeConfigurationException
	{
		super ("MomServer");
	}
	
	/**
	 * Send new game database to clients as they connect
	 * @param socket Socket on which a new client has connected
	 * @return Thread object to handle requests from this client
	 * @throws InterruptedException If there is a problem waiting for the thread to start up
	 * @throws JAXBException If there is a problem sending something to connecting client
	 * @throws XMLStreamException If there is a problem sending something to connecting client
	 */
	@Override
	protected final MultiplayerBaseServerThread createAndStartClientThread (final Socket socket) throws InterruptedException, JAXBException, XMLStreamException
	{
		log.trace ("Entering createAndStartClientThread");
		
		final Object readyForMessagesMonitor = new Object ();
		
		final MultiplayerClientConnection conn = new MultiplayerClientConnection ("ClientConnection-" + socket);
		conn.setServer (this);
		conn.setSocket (socket);
		conn.setSendContext (getServerToClientContext ());
		conn.setReceiveContext (getClientToServerContext ());
		conn.setReceiveObjectFactoryArray (getClientToServerContextFactoryArray ());
		conn.setReadyForMessagesMonitor (readyForMessagesMonitor);
		conn.setConversationTag (getConversationTag ());
		conn.start ();
		
		// Wait until thread has started up properly, then send new game database to the client
		synchronized (readyForMessagesMonitor)
		{
			readyForMessagesMonitor.wait ();
		}

		conn.sendMessageToClient (newGameDatabaseMessage);
		
		log.trace ("Exiting createAndStartClientThread");
		return conn;
	}

	/**
	 * Descendant server classes will want to override this to create a thread that knows how to process useful messages
	 * @param sessionDescription Description of the new session
	 * @return Thread object to handle requests for this session
	 */
	@Override
	public final MultiplayerSessionThread createSessionThread (final SessionDescription sessionDescription)
	{
		log.trace ("Entering createSessionThread: Session ID " + sessionDescription.getSessionID ());

		final MomSessionThread thread = getSessionThreadFactory ().createThread ();
		thread.setSessionDescription (sessionDescription);

		return thread;
	}

	/**
	 * @return Message to send new game database to clients as they connect
	 */
	public final NewGameDatabaseMessage getNewGameDatabaseMessage ()
	{
		return newGameDatabaseMessage;
	}

	/**
	 * @param msg Message to send new game database to clients as they connect
	 */
	public final void setNewGameDatabaseMessage (final NewGameDatabaseMessage msg)
	{
		newGameDatabaseMessage = msg;
	}
	
	/** 
	 * @return Factory interface for creating MomSessionThreads
	 */
	public final MomSessionThreadFactory getSessionThreadFactory ()
	{
		return sessionThreadFactory;
	}

	/**
	 * @param factory Factory interface for creating MomSessionThreads
	 */
	public final void setSessionThreadFactory (final MomSessionThreadFactory factory)
	{
		sessionThreadFactory = factory;
	}
}