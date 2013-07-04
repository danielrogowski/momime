package momime.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.servertoclient.v0_9_4.NewGameDatabaseMessage;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.database.IServerDatabaseConverters;
import momime.server.database.ServerDatabaseConvertersImpl;
import momime.server.database.ServerDatabaseEx;
import momime.server.mapgenerator.OverlandMapGeneratorImpl;
import momime.server.ui.MomServerUI;
import momime.server.ui.SessionWindow;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ndg.multiplayer.base.MultiplayerBaseServerThread;
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
	private final Logger log = Logger.getLogger (MomServer.class.getName ());
	
	/** Minimum major version of JVM required to run MoM IME */
	private final static int JAVA_REQUIRED_MAJOR_VERSION = 1;

	/** Minimum minor version of JVM required to run MoM IME */
	private final static int JAVA_REQUIRED_MINOR_VERSION = 6;

	/** Message to send new game database to clients as they connect */
	private NewGameDatabaseMessage newGameDatabaseMessage;

	/** UI to display server status */
	private MomServerUI ui;

	/** Database converters */
	private IServerDatabaseConverters serverDatabaseConverters;
	
	/** Path to where all the server database XMLs are - from config file */
	private String pathToServerXmlDatabases;
	
	/** JAXB unmarshaller for reading server databases */
	private Unmarshaller serverDatabaseUnmarshaller;
	
	/** Factory interface for creating MomSessionThreads */
	private IMomSessionThreadFactory sessionThreadFactory;

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
		log.entering (MomServer.class.getName (), "createAndStartClientThread");
		
		final Object readyForMessagesMonitor = new Object ();
		final MultiplayerClientConnection conn = new MultiplayerClientConnection (this, getMessageProcesser (), socket,
			getClientToServerContext (), getClientToServerContextFactoryArray (), getServerToClientContext (), readyForMessagesMonitor);
		conn.start ();

		// Wait until thread has started up properly, then send new game database to the client
		synchronized (readyForMessagesMonitor)
		{
			readyForMessagesMonitor.wait ();
		}

		conn.sendMessageToClient (newGameDatabaseMessage);
		
		log.exiting (MomServer.class.getName (), "createAndStartClientThread");
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
	protected final MultiplayerSessionThread createSessionThread (final SessionDescription sessionDescription) throws JAXBException, IOException
	{
		log.entering (MomServer.class.getName (), "createSessionThread", sessionDescription.getSessionID ());

		final MomSessionThread thread = getSessionThreadFactory ().createThread ();
		thread.setMessageProcesser (getMessageProcesser ());
		thread.setSessionDescription (sessionDescription);

		final MomSessionDescription sd = (MomSessionDescription) sessionDescription;
		
		// Start logger for this sesssion
		final SessionWindow sessionWindow = ui.createWindowForNewSession (sd);
		thread.setSessionLogger (ui.createLoggerForNewSession (sd, sessionWindow));

		// Load server XML
		final File fullFilename = new File (getPathToServerXmlDatabases () + sd.getXmlDatabaseName () + ServerDatabaseConvertersImpl.SERVER_XML_FILE_EXTENSION);
		thread.setServerDB ((ServerDatabaseEx) getServerDatabaseUnmarshaller ().unmarshal (fullFilename));

		// Create hash maps to look up all the values from the DB
		thread.getServerDB ().buildMaps ();
		
		// Create client database
		thread.getGeneralPublicKnowledge ().setClientDatabase (getServerDatabaseConverters ().buildClientDatabase
			(thread.getServerDB (), sd.getDifficultyLevel ().getHumanSpellPicks ()));
		
		// Generate the overland map
		final OverlandMapGeneratorImpl mapGen = (OverlandMapGeneratorImpl) thread.getOverlandMapGenerator ();
		mapGen.setSessionDescription (sd);
		mapGen.setServerDB (thread.getServerDB ());
		mapGen.setTrueTerrain (thread.getGeneralServerKnowledge ().getTrueMap ());		// See comment in spring XML for why this isn't just injected
		mapGen.generateOverlandTerrain ();

		log.exiting (MomServer.class.getName (), "createSessionThread", thread);
		return thread;
	}

	/**
	 * @param uiClassName Class name to use for UI to display server status
	 */
	public final void setUiClassName (final String uiClassName)
	{
		final Object uiObject;
		try
		{
			uiObject = Class.forName (uiClassName).newInstance ();
		}
		catch (final ClassNotFoundException e)
		{
			throw new InvalidParameterException ("Requested UI class " + uiClassName + " could not be found on the classpath (full error: " + e.getMessage () + ")");
		}
		catch (final IllegalAccessException e)
		{
			throw new InvalidParameterException ("Requested UI class " + uiClassName + " found but could not be accessed (full error: " + e.getMessage () + ")");
		}
		catch (final InstantiationException e)
		{
			throw new InvalidParameterException ("Requested UI class " + uiClassName + " found but could not be instantiated (full error: " + e.getMessage () + ")");
		}

		if (!(uiObject instanceof MomServerUI))
			throw new InvalidParameterException ("Requested UI class " + uiClassName + " but this does not implement the " + MomServerUI.class.getName () + " interface");

		setUI ((MomServerUI) uiObject);
		getUI ().createMainWindow ();
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
		return ui;
	}

	/**
	 * @param newUI UI to display server status
	 */
	public final void setUI (final MomServerUI newUI)
	{
		ui = newUI;
	}

	/**
	 * @return Database converters
	 */
	public final IServerDatabaseConverters getServerDatabaseConverters ()
	{
		return serverDatabaseConverters;
	}

	/**
	 * @param conv Database converters
	 */
	public final void setServerDatabaseConverters (final IServerDatabaseConverters conv)
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
	public final IMomSessionThreadFactory getSessionThreadFactory ()
	{
		return sessionThreadFactory;
	}

	/**
	 * @param factory Factory interface for creating MomSessionThreads
	 */
	public final void setSessionThreadFactory (final IMomSessionThreadFactory factory)
	{
		sessionThreadFactory = factory;
	}
	
	/**
	 * @param args Command line arguments, following are allowed but all optional:
	 *		-debug, to turn on writing the full debug log out to a text file; without this, only messages of level INFO and higher are viewable on screen
	 */
	@SuppressWarnings ("resource")
	public final static void main (final String [] args)
	{
		try
		{
			// Ensure v1.6 JVM
			final String [] javaVersion = System.getProperty ("java.version").split ("\\.");
			final int majorVersion = Integer.parseInt (javaVersion [0]);
			final int minorVersion = Integer.parseInt (javaVersion [1]);

			if ((majorVersion < JAVA_REQUIRED_MAJOR_VERSION) || ((majorVersion == JAVA_REQUIRED_MAJOR_VERSION) && (minorVersion < JAVA_REQUIRED_MINOR_VERSION)))
				throw new InvalidParameterException ("MoM IME requires a Java Virtual Machine version " + JAVA_REQUIRED_MAJOR_VERSION + "." + JAVA_REQUIRED_MINOR_VERSION +
					" or newer to run, but only detected version " + majorVersion + "." + minorVersion);
			
			// Initialize logging first, in case debug logging for spring itself is enabled
			try (final FileInputStream in = new FileInputStream ("MoMIMEServerLogging.properties"))
			{
				LogManager.getLogManager ().readConfiguration (in);
				in.close ();
			}

			// Everything is now set to start with spring
			new ClassPathXmlApplicationContext ("/momime.server.spring/momime-server-beans.xml");			
		}
		catch (final Exception e)
		{
			System.out.println ("Exception in main method:");
			e.printStackTrace ();
		}
	}
}
