package momime.server;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import momime.common.MomException;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.messages.servertoclient.v0_9_4.NewGameDatabaseMessage;
import momime.server.config.ServerConfigConstants;
import momime.server.config.v0_9_4.MomImeServerConfig;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseConstants;
import momime.server.database.ServerDatabaseConverters;
import momime.server.logging.SingleLineFormatter;
import momime.server.logging.WriteToOtherLogHandler;
import momime.server.messages.process.ObjectFactoryClientToServerMessages;
import momime.server.ui.MomServerUI;
import momime.server.ui.OneWindowPerGameUI;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.xml.sax.SAXException;

import com.ndg.multiplayer.base.MultiplayerBaseServerThread;
import com.ndg.multiplayer.server.MultiplayerClientConnection;
import com.ndg.multiplayer.server.MultiplayerSessionServer;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.users.MultiplayerSessionUserRegistry;
import com.ndg.multiplayer.server.users.MultiplayerSessionUserRegistryImpl;
import com.ndg.multiplayer.sessionbase.SessionDescription;

/**
 * Main server class to listen for client connection requests and manage list of sessions
 */
public final class MomServer extends MultiplayerSessionServer
{
	/** Minimum major version of JVM required to run MoM IME */
	private final static int JAVA_REQUIRED_MAJOR_VERSION = 1;

	/** Minimum minor version of JVM required to run MoM IME */
	private final static int JAVA_REQUIRED_MINOR_VERSION = 6;

	/** Message to send new game database to clients as they connect */
	private final NewGameDatabaseMessage newGameDatabaseMessage;

	/** Server config loaded from XML config file */
	private final MomImeServerConfig config;

	/** UI to display server status */
	private final MomServerUI ui;

	/** Logger which writes to a disk file, if enabled */
	private final Logger fileLogger;

	/**
	 * @param aUserRegistry User registry
	 * @param aNewGameDatabaseMessage Message to send new game database to clients as they connect
	 * @param aConfig Server config loaded from XML config file
	 * @param aUI UI to display server status
	 * @param aFileLogger Logger which writes to a disk file, if enabled
	 * @param aDebugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem creating the JAXB contexts
	 * @throws DatatypeConfigurationException If there is a problem creating the DatatypeFactory
	 */
	public MomServer (final MultiplayerSessionUserRegistry aUserRegistry, final NewGameDatabaseMessage aNewGameDatabaseMessage,
		final MomImeServerConfig aConfig, final MomServerUI aUI, final Logger aFileLogger, final Logger aDebugLogger)
		throws JAXBException, DatatypeConfigurationException
	{
		super (aUserRegistry, aConfig.getPortNumber (), "MomServer",
			JAXBContextCreator.createClientToServerMessageContext (), new Object [] {new ObjectFactoryClientToServerMessages ()},
			JAXBContextCreator.createServerToClientMessageContext (),
			null, false, aDebugLogger);

		newGameDatabaseMessage = aNewGameDatabaseMessage;
		config = aConfig;
		ui = aUI;
		fileLogger = aFileLogger;
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
			clientToServerContext, clientToServerContextFactoryArray, serverToClientContext, readyForMessagesMonitor, debugLogger);
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
	 */
	@Override
	protected final MultiplayerSessionThread createSessionThread (final SessionDescription sessionDescription) throws JAXBException
	{
		return new MomSessionThread (getMessageProcesser (), sessionDescription, getConfig (), ui, fileLogger, debugLogger);
	}

	/**
	 * @return Server config loaded from XML config file
	 */
	public final MomImeServerConfig getConfig ()
	{
		return config;
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
		Logger debugLogger = null;
		Logger fileLogger = null;
		MomServerUI ui = null;
		boolean proceed = false;
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

			// Create the UI object
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

			ui = (MomServerUI) uiObject;

			// Create and configure debug logger
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
			}

			// Create the main window, and set up logging to write to it
			ui.createMainWindow (debugLogger);
			proceed = true;
		}
		catch (final InvalidParameterException e)
		{
			System.out.println (e.getMessage ());
		}
		catch (final IOException e)
		{
			System.out.println ("IOException during pre-logging server startup: " + e.getMessage ());
		}

		// Any exceptions up until now just get written to System.out, but now we've got the debugLogger and UI set up properly,
		// we should log any further exceptions to there as well
		// This is mainly so errors in the config file not matching the XSD are logged correctly
		if (proceed)
		{
			debugLogger.entering (MomServer.class.getName (), "main (proceed section)");
			try
			{
				// XSD schema factory
				final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
				schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance (), debugLogger));

				// Load server config XSD
				final URL serverConfigXsdResource = new Object ().getClass ().getResource (ServerConfigConstants.CONFIG_XSD_LOCATION);
				if (serverConfigXsdResource == null)
					throw new IOException ("Server config XSD could not be located on classpath");

				final Validator serverConfigXSD = schemaFactory.newSchema (serverConfigXsdResource).newValidator ();

				// Validate the config file
				final File configFile = new File (ServerConfigConstants.CONFIG_XML_LOCATION);
				serverConfigXSD.validate (new StreamSource (configFile));

				// Load the config file
				final MomImeServerConfig config = (MomImeServerConfig) JAXBContext.newInstance (MomImeServerConfig.class).createUnmarshaller ().unmarshal (configFile);

				// Load server database XSD
				final URL serverDatabaseXsdResource = new Object ().getClass ().getResource (ServerDatabaseConstants.SERVER_XSD_LOCATION);
				if (serverDatabaseXsdResource == null)
					throw new IOException ("Server database XSD could not be located on classpath");

				final Validator serverDatabaseXSD = schemaFactory.newSchema (serverDatabaseXsdResource).newValidator ();

				// Build the new game XML, which lets connecting clients know which XML files are available on the server and what new game settings they can choose
				final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
				final NewGameDatabaseMessage newGameDatabaseMessage = ServerDatabaseConverters.buildNewGameDatabase
					(new File (config.getPathToServerXmlDatabases ()), serverDatabaseXSD, serverDatabaseContext, debugLogger);

				// User registry
				final MultiplayerSessionUserRegistry userRegistry = new MultiplayerSessionUserRegistryImpl (new File (config.getUserRegistryFilename ()), createUserRegistry, debugLogger);

				// Now everything is ready, open up the TCP/IP port
				new MomServer (userRegistry, newGameDatabaseMessage, config, ui, fileLogger, debugLogger).start ();
				debugLogger.exiting (MomServer.class.getName (), "main (proceed section)");
			}
			catch (final MomException e)
			{
				debugLogger.severe ("MomException during post-logging server startup: " + e.getMessage ());
				e.printStackTrace ();
			}
			catch (final IOException e)
			{
				debugLogger.severe ("IOException during post-logging server startup: " + e.getMessage ());
				e.printStackTrace ();
			}
			catch (final SAXException e)
			{
				debugLogger.severe ("SAXException during post-logging server startup: " + e.getMessage ());
				e.printStackTrace ();
			}
			catch (final JAXBException e)
			{
				debugLogger.severe ("JAXBException during post-logging server startup: " + e.getMessage ());
				e.printStackTrace ();
			}
			catch (final IllegalAccessException e)
			{
				debugLogger.severe ("IllegalAccessException during post-logging server startup: " + e.getMessage ());
				e.printStackTrace ();
			}
			catch (final InstantiationException e)
			{
				debugLogger.severe ("InstantiationException during post-logging server startup: " + e.getMessage ());
				e.printStackTrace ();
			}
			catch (final ClassNotFoundException e)
			{
				debugLogger.severe ("ClassNotFoundException during post-logging server startup: " + e.getMessage ());
				e.printStackTrace ();
			}
			catch (final DatatypeConfigurationException e)
			{
				debugLogger.severe ("DatatypeConfigurationException during post-logging server startup: " + e.getMessage ());
				e.printStackTrace ();
			}
		}
	}
}
