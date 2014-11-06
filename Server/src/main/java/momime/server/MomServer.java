package momime.server;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.MomSessionDescription;
import momime.common.messages.servertoclient.NewGameDatabaseMessage;
import momime.server.database.ServerDatabaseConverters;
import momime.server.database.ServerDatabaseConvertersImpl;
import momime.server.database.ServerDatabaseExImpl;
import momime.server.mapgenerator.OverlandMapGeneratorImpl;
import momime.server.ui.MomServerUI;
import momime.server.ui.MomServerUIHolder;

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
	/** Prefix for all session loggers */
	public static final String MOM_SESSION_LOGGER_PREFIX = "MoMIMESession.";

	/** Class logger */
	private final Log log = LogFactory.getLog (MomServer.class);
	
	/** Message to send new game database to clients as they connect */
	private NewGameDatabaseMessage newGameDatabaseMessage;

	/** Database converters */
	private ServerDatabaseConverters serverDatabaseConverters;
	
	/** Path to where all the server database XMLs are - from config file */
	private String pathToServerXmlDatabases;
	
	/** JAXB unmarshaller for reading server databases */
	private Unmarshaller serverDatabaseUnmarshaller;
	
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
	 * @throws JAXBException If there is a problem loading the server XML file
	 * @throws IOException If there is a problem generating the client database for this session
	 */
	@Override
	public final MultiplayerSessionThread createSessionThread (final SessionDescription sessionDescription) throws JAXBException, IOException
	{
		log.trace ("Entering createSessionThread: Session ID " + sessionDescription.getSessionID ());

		final MomSessionThread thread = getSessionThreadFactory ().createThread ();
		thread.setSessionDescription (sessionDescription);

		final MomSessionDescription sd = (MomSessionDescription) sessionDescription;
		
		// Start logger for this sesssion.  These are much the same as the class loggers, except named MoMIMESession.1, MoMIMESession.2 and so on.
		if (getUI () != null)
			getUI ().createWindowForNewSession (sd);
		
		thread.setSessionLogger (LogFactory.getLog (MOM_SESSION_LOGGER_PREFIX + sd.getSessionID ()));

		// Load server XML
		thread.getSessionLogger ().info ("Loading server XML...");
		final File fullFilename = new File (getPathToServerXmlDatabases () + "/" + sd.getXmlDatabaseName () + ServerDatabaseConvertersImpl.SERVER_XML_FILE_EXTENSION);
		final ServerDatabaseExImpl db = (ServerDatabaseExImpl) getServerDatabaseUnmarshaller ().unmarshal (fullFilename); 

		// Create hash maps to look up all the values from the DB
		thread.getSessionLogger ().info ("Building maps over XML data...");
		db.buildMaps ();
		thread.setServerDB (db); 
		
		// Create client database
		thread.getSessionLogger ().info ("Generating client XML...");
		thread.getGeneralPublicKnowledge ().setClientDatabase (getServerDatabaseConverters ().buildClientDatabase
			(thread.getServerDB (), sd.getDifficultyLevel ().getHumanSpellPicks ()));
		
		// Generate the overland map
		thread.getSessionLogger ().info ("Generating overland map...");
		final OverlandMapGeneratorImpl mapGen = (OverlandMapGeneratorImpl) thread.getOverlandMapGenerator ();
		mapGen.setSessionDescription (sd);
		mapGen.setServerDB (thread.getServerDB ());
		mapGen.setGsk (thread.getGeneralServerKnowledge ());		// See comment in spring XML for why this isn't just injected
		mapGen.generateOverlandTerrain ();
		
		try
		{
			mapGen.generateInitialCombatAreaEffects ();
			
			// Take this catch out after switching to the latest multiplayer layer version which includes XMLStreamException in the throws clause
		}
		catch (final XMLStreamException e)
		{
			throw new IOException (e);
		}

		thread.getSessionLogger ().info ("Session startup completed");
		log.trace ("Exiting createSessionThread = " + thread);
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
	 * @return UI to display server status
	 */
	public final MomServerUI getUI ()
	{
		return MomServerUIHolder.getUI ();
	}

	/**
	 * @return Database converters
	 */
	public final ServerDatabaseConverters getServerDatabaseConverters ()
	{
		return serverDatabaseConverters;
	}

	/**
	 * @param conv Database converters
	 */
	public final void setServerDatabaseConverters (final ServerDatabaseConverters conv)
	{
		serverDatabaseConverters = conv;
	}
	
	/**
	 * @return Path to where all the server database XMLs are - from config file
	 */
	public final String getPathToServerXmlDatabases ()
	{
		return pathToServerXmlDatabases;
	}

	/**
	 * @param path Path to where all the server database XMLs are - from config file
	 */
	public final void setPathToServerXmlDatabases (final String path)
	{
		pathToServerXmlDatabases = path;
	}

	/**
	 * @return JAXB unmarshaller for reading server databases
	 */
	public final Unmarshaller getServerDatabaseUnmarshaller ()
	{
		return serverDatabaseUnmarshaller;
	}

	/**
	 * @param unmarshaller JAXB unmarshaller for reading server databases
	 */
	public final void setServerDatabaseUnmarshaller (final Unmarshaller unmarshaller)
	{
		serverDatabaseUnmarshaller = unmarshaller;
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