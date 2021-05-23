package momime.server;

import java.net.Socket;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.server.MultiplayerBaseServerThread;
import com.ndg.multiplayer.server.MultiplayerSessionServer;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.sessionbase.SessionDescription;

import momime.common.messages.servertoclient.NewGameDatabaseMessage;

/**
 * Main server class to listen for client connection requests and manage list of sessions
 */
public final class MomServer extends MultiplayerSessionServer
{
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
		final MomClientConnection conn = new MomClientConnection ("ClientConnection-" + socket, getNewGameDatabaseMessage ());
		conn.setServer (this);
		conn.setSocket (socket);
		conn.setSendContext (getServerToClientContext ());
		conn.setReceiveContext (getClientToServerContext ());
		conn.setReceiveObjectFactoryArray (getClientToServerContextFactoryArray ());
		conn.setConversationTag (getConversationTag ());
		conn.setCompress (isCompress ());
		conn.setDecompress (isDecompress ());
		conn.start ();
		
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