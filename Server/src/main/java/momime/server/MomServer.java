package momime.server;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import momime.common.database.CommonXsdResourceResolver;
import momime.common.messages.servertoclient.v0_9_4.NewGameDatabaseMessage;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.config.ServerConfigConstants;
import momime.server.config.v0_9_4.MomImeServerConfig;
import momime.server.database.IServerDatabaseConverters;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseConstants;
import momime.server.database.ServerDatabaseConverters;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseFactory;
import momime.server.ui.MomServerUI;
import momime.server.ui.OneWindowPerGameUI;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.multiplayer.base.MultiplayerBaseServerThread;
import com.ndg.multiplayer.server.MultiplayerClientConnection;
import com.ndg.multiplayer.server.MultiplayerSessionServer;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.users.MultiplayerSessionUserRegistryImpl;
import com.ndg.multiplayer.sessionbase.SessionDescription;

/**
 * Main server class to listen for client connection requests and manage list of sessions
 */
public final class MomServer extends MultiplayerSessionServer implements ApplicationContextAware
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomServer.class.getName ());
	
	/** Minimum major version of JVM required to run MoM IME */
	private final static int JAVA_REQUIRED_MAJOR_VERSION = 1;

	/** Minimum minor version of JVM required to run MoM IME */
	private final static int JAVA_REQUIRED_MINOR_VERSION = 6;

	/** Message to send new game database to clients as they connect */
	private NewGameDatabaseMessage newGameDatabaseMessage;

	/** Server config loaded from XML config file */
	private MomImeServerConfig config;

	/** UI to display server status */
	private MomServerUI ui;

	/** Logger which writes to a disk file, if enabled */
	private Logger fileLogger;
	
	/** Spring context */
	private ApplicationContext applicationContext;
	
	/** Database converters */
	private IServerDatabaseConverters serverDatabaseConverters;

	/**
	 * @throws DatatypeConfigurationException If there is a problem creating the DatatypeFactory
	 */
	public MomServer () throws DatatypeConfigurationException
	{
		super ("MomServer");
	}
	
	/**
	 * Performs all remaining server initialization tasks, after the UI and debugLogger are configured
	 * @param createUserRegistry
	 */
	private final void init (final boolean createUserRegistry)
	{
		try
		{
			// XSD schema factory
			final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));

			// Load server config XSD
			final URL serverConfigXsdResource = new Object ().getClass ().getResource (ServerConfigConstants.CONFIG_XSD_LOCATION);
			if (serverConfigXsdResource == null)
				throw new IOException ("Server config XSD could not be located on classpath");

			final Validator serverConfigXSD = schemaFactory.newSchema (serverConfigXsdResource).newValidator ();

			// Validate the config file
			final File configFile = new File (ServerConfigConstants.CONFIG_XML_LOCATION);
			serverConfigXSD.validate (new StreamSource (configFile));

			// Load the config file
			setConfig ((MomImeServerConfig) JAXBContext.newInstance (MomImeServerConfig.class).createUnmarshaller ().unmarshal (configFile));
			setPort (getConfig ().getPortNumber ());

			// Load server database XSD
			final URL serverDatabaseXsdResource = new Object ().getClass ().getResource (ServerDatabaseConstants.SERVER_XSD_LOCATION);
			if (serverDatabaseXsdResource == null)
				throw new IOException ("Server database XSD could not be located on classpath");

			final Validator serverDatabaseXSD = schemaFactory.newSchema (serverDatabaseXsdResource).newValidator ();

			// Build the new game XML, which lets connecting clients know which XML files are available on the server and what new game settings they can choose
			final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
			setNewGameDatabaseMessage (getServerDatabaseConverters ().buildNewGameDatabase
				(new File (config.getPathToServerXmlDatabases ()), serverDatabaseXSD, serverDatabaseContext));

			// User registry
			setUserRegistry (new MultiplayerSessionUserRegistryImpl (new File (config.getUserRegistryFilename ()), createUserRegistry));
		
			// Start it up
			start ();
		}
		catch (final Exception e)
		{
			log.log (Level.SEVERE, "Error during MoM IME server initialization", e);
		}
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
		final MomSessionThread thread = (MomSessionThread) applicationContext.getBean ("sessionThread");
		thread.setMessageProcesser (getMessageProcesser ());
		thread.setSessionDescription (sessionDescription);
		
		final MomSessionDescription sd = (MomSessionDescription) sessionDescription;
		
		// Load server XML
		final File fullFilename = new File (config.getPathToServerXmlDatabases () + sd.getXmlDatabaseName () + ServerDatabaseConverters.SERVER_XML_FILE_EXTENSION);
		
		final Unmarshaller unmarshaller = JAXBContextCreator.createServerDatabaseContext ().createUnmarshaller ();		
		unmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {new ServerDatabaseFactory ()});
		thread.setServerDB ((ServerDatabaseEx) unmarshaller.unmarshal (fullFilename));

		// Create hash maps to look up all the values from the DB
		thread.getServerDB ().buildMaps ();
		
		// Create client database
		thread.getGeneralPublicKnowledge ().setClientDatabase (getServerDatabaseConverters ().buildClientDatabase
			(thread.getServerDB (), sd.getDifficultyLevel ().getHumanSpellPicks ()));

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
	 * @return Server config loaded from XML config file
	 */
	public final MomImeServerConfig getConfig ()
	{
		return config;
	}

	/**
	 * @param cfg Server config loaded from XML config file
	 */
	public final void setConfig (final MomImeServerConfig cfg)
	{
		config = cfg;
	}
	
	/**
	 * @return Spring context
	 */
	public final ApplicationContext getApplicationContext ()
	{
		return applicationContext;
	}

	/**
	 * @param ctx Spring context
	 */
	@Override
	public final void setApplicationContext (final ApplicationContext ctx)
	{
		applicationContext = ctx;
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
	 * @param args Command line arguments, following are allowed but all optional:
	 *		-debug, to turn on writing the full debug log out to a text file; without this, only messages of level INFO and higher are viewable on screen
	 *		-createUserRegistry, to initialize a new empty user registry XML file
	 *		-ui <class>, to select which user interface to display the state of the server with, one of
	 *			momime.server.ui.OneWindowPerGameUI - Similar to the old Delphi MoM IME server, opens up a new window for each game in progress, and msgs relating to that game appear in that window (this is the default)
	 *			momime.server.ui.SingleWindowUI - Still uses a graphical display, but there is only a single window so all msgs from all games appear in the same window
	 *			momime.server.ui.ConsoleUI - No GUI at all; msgs from all games are just written to the console
	 *			momime.server.ui.NoUI - The same as "console" except that only SEVERE log messages are output so unless something goes wrong, the server is totally silent
	 */
	public final static void main (final String [] args)
	{
		// These are needed outside of the first block of try..catches
		Logger fileLogger = null;
		boolean createUserRegistry = false;

		try
		{
			// Ensure v1.6 JVM
			final String [] javaVersion = System.getProperty ("java.version").split ("\\.");
			final int majorVersion = Integer.parseInt (javaVersion [0]);
			final int minorVersion = Integer.parseInt (javaVersion [1]);

			if ((majorVersion < JAVA_REQUIRED_MAJOR_VERSION) || ((majorVersion == JAVA_REQUIRED_MAJOR_VERSION) && (minorVersion < JAVA_REQUIRED_MINOR_VERSION)))
				throw new InvalidParameterException ("MoM IME requires a Java Virtual Machine version " + JAVA_REQUIRED_MAJOR_VERSION + "." + JAVA_REQUIRED_MINOR_VERSION +
					" or newer to run, but only detected version " + majorVersion + "." + minorVersion);

			// Create MomServer object, so we can start setting necessary values against it, from the command line options and so on
			final ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/momime.server.spring/momime-server-beans.xml");
			final MomServer server = (MomServer) applicationContext.getBean ("momServer");
			
			// Set defaults
			String uiClassName = OneWindowPerGameUI.class.getName ();

			// Parse the command line options
			boolean debugToFile = false;
			String previousArg = null;
			for (final String thisArg : args)
			{
				if (previousArg == null)
				{
					// Brand new argument, check which it is
					if (thisArg.equals ("-debug"))
						debugToFile = true;
					else if (thisArg.equals ("-createUserRegistry"))
						createUserRegistry = true;
					else if (thisArg.equals ("-ui"))
						previousArg = thisArg;
					else
						throw new InvalidParameterException ("Unrecognized command line argument " + thisArg + ".  Usage MomServer [-debug] [-createUserRegistry] [-ui <class>]");
				}
				else	// There is only one possible value of previousArg currently
				{
					uiClassName = thisArg;
					previousArg = null;
				}
			}

			if (previousArg != null)
				throw new InvalidParameterException ("Command line argument " + previousArg + " specified no value.  Usage MomServer [ff] [-createUserRegistry] [-ui <class>]");
			
			// Call methods to make the command line options take effect
			server.setUiClassName (uiClassName);

/*			// Create and configure debug logger
			debugLogger = Logger.getLogger ("MoMIMEServer");

			if (debugToFile)
				debugLogger.setLevel (Level.ALL);
			else
				debugLogger.setLevel (ui.getMinimumDebugLoggerLogLevel ());

			debugLogger.setUseParentHandlers (false);

			// Debug to text file on disk if requested
			if (debugToFile)
			{
				// Create a whole separate debugger, and copy messages from the debugLogger to the fileLogger
				// This is the only way we can get the session logs from OneWindowPerGameUI to be written out to the file without being written to the single window as well
				fileLogger = Logger.getLogger ("MoMIMEServerFileLogger");
				fileLogger.setLevel (Level.ALL);
				fileLogger.setUseParentHandlers (false);

				final Handler fileHandler = new FileHandler ("MoMIMEServer_debug.log");
				fileHandler.setLevel (Level.ALL);
				fileHandler.setFormatter (new SingleLineFormatter ());
				fileLogger.addHandler (fileHandler);

				final Handler copyDebugLoggerToFileLogger = new WriteToOtherLogHandler (fileLogger, null);
				copyDebugLoggerToFileLogger.setLevel (Level.ALL);
				debugLogger.addHandler (copyDebugLoggerToFileLogger);

				fileLogger.fine ("Debug file on, using UI \"" + uiClassName + "\"");
			} */

			// Create the main window, and set up logging to write to it
			server.getUI ().createMainWindow ();
			
			// Now the UI and loggers are configured correctly, can proceed with remainder of server startup
			server.init (createUserRegistry);
		}
		catch (final InvalidParameterException e)
		{
			System.out.println (e.getMessage ());
		}
	}
}
