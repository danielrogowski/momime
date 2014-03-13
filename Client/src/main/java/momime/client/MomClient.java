package momime.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.logging.LogManager;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.database.v0_9_4.NewGameDatabase;
import momime.client.ui.ConnectToServerUI;
import momime.client.ui.MainMenuUI;
import momime.client.ui.MessageBoxUI;
import momime.client.ui.PrototypeFrameCreator;
import momime.common.MomCommonConstants;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.MultiplayerSessionClient;
import com.ndg.multiplayer.client.MultiplayerSessionClientEvent;
import com.ndg.multiplayer.sessionbase.CreateAccountFailedReason;
import com.ndg.multiplayer.sessionbase.JoinFailedReason;
import com.ndg.multiplayer.sessionbase.LeaveSessionFailedReason;
import com.ndg.multiplayer.sessionbase.LoginFailedReason;
import com.ndg.multiplayer.sessionbase.LogoutFailedReason;
import com.ndg.multiplayer.sessionbase.RequestSessionListFailedReason;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;

/**
 * Main class to kickstart client
 */
public final class MomClient extends MultiplayerSessionClient
{
	/** Name that we logged in using */
	private String ourPlayerName;
	
	/** Main menu with options to connect to a server and create or join games */
	private MainMenuUI mainMenuUI;

	/** Connect to server UI */
	private ConnectToServerUI connectToServerUI;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Info we need in order to create games; sent from server */
	private NewGameDatabase newGameDatabase;
	
	/**
	 * Kick off method invoked by spring's init-method
	 */
	public final void start ()
	{
		// Use Nimbus look and feel
		try
		{
		    for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels ())
		    {
		        if ("Nimbus".equals (info.getName ()))
		        {
		            UIManager.setLookAndFeel (info.getClassName ());
		            break;
		        }
		    }
		}
		catch (final Exception e)
		{
		}
		
		// Multiplayer client event handlers
		getEventListeners ().add (new MultiplayerSessionClientEvent ()
		{
			/**
			 * Event triggered when we server successfully creates an account for us
			 * 
			 * @param sender Connection to the server
			 * @param playerID Player ID allocated to our new account
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void accountCreated (final MultiplayerServerConnection sender, final int playerID)
				throws JAXBException, XMLStreamException, IOException
			{
				// Log in with the new account
				getConnectToServerUI ().afterAccountCreated ();
			}

			/**
			 * Event triggered after we successfully log in.
			 * 
			 * @param sender Connection to the server
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void loggedIn (final MultiplayerServerConnection sender)
				throws JAXBException, XMLStreamException, IOException
			{
				getConnectToServerUI ().afterLoggedIn ();
				getConnectToServerUI ().setVisible (false);
				getMainMenuUI ().enableActions ();
			}
			
			/**
			 * Event triggered as we successfully log out - event is
			 * triggered just before playerID and session variables are cleared.
			 * 
			 * @param sender Connection to the server
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void loggedOut (final MultiplayerServerConnection sender)
				throws JAXBException, XMLStreamException, IOException
			{
				getMainMenuUI ().enableActions ();
			}
			
			/**
			 * Forcibly logged out by another client logging in with our account.  This event is triggered just before
			 * the playerID and session variables are cleared.
			 * 
			 * @param sender Connection to the server
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void kickedByAnotherLogin (final MultiplayerServerConnection sender)
				throws JAXBException, XMLStreamException, IOException
			{
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setText ("Another player logged onto the server with your player name and password, so you have been kicked");
				try
				{
					msg.setVisible (true);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
				
				getMainMenuUI ().enableActions ();
			}
			
			/**
			 * Event triggered when server tells us which sessions we can join
			 * 
			 * @param sender Connection to the server
			 * @param sessions List of sessions the server is telling us we can join
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void receivedSessionList (final MultiplayerServerConnection sender, final List<SessionAndPlayerDescriptions> sessions)
				throws JAXBException, XMLStreamException, IOException
			{
			}

			/**
			 * Event triggered when we successfully join a session
			 * 
			 * @param sender Connection to the server
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void joinedSession (final MultiplayerServerConnection sender)
				throws JAXBException, XMLStreamException, IOException
			{
				System.out.println (getSessionDescription ());
			}

			/**
			 * Event triggered when somebody else joins the session we're already in.  This only gets sent if the player is genuinely new
			 * to the session - if they disconnect and rejoin back into their original slot, this message will not be sent.
			 * 
			 * @param sender Connection to the server
			 * @param playerID Player who joined
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void additionalPlayerJoined (final MultiplayerServerConnection sender, final int playerID)
				throws JAXBException, XMLStreamException, IOException
			{
			}

			/**
			 * Event triggered when somebody leaves the session we're in, note it could be us leaving.
			 * Event is triggered just prior to player being removed from the list, and if us leaving, before session variables being nulled out.
			 * 
			 * @param sender Connection to the server
			 * @param playerID Player who left
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void playerLeft (final MultiplayerServerConnection sender, final int playerID)
				throws JAXBException, XMLStreamException, IOException
			{
			}

			/**
			 * Event triggered when the server closes down a session when we're still in it.  This is triggered just before all the
			 * session variables are nulled out.
			 * 
			 * @param sender Connection to the server
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void sessionEnding (final MultiplayerServerConnection sender)
				throws JAXBException, XMLStreamException, IOException
			{
			}

			/**
			 * Event triggered when the server rejects a request.  Exactly one of the failure codes will be non-null
			 * which indicates which kind of request got rejected.
			 * 
			 * @param sender Connection to the server
			 * @param createAccountFailed If not null, indicates why our createAccount request was rejected
			 * @param loginFailed If not null, indicates why our login request was rejected
			 * @param logoutFailed If not null, indicates why our logout request was rejected
			 * @param requestSessionListFailed If not null, indicates why our session list request was rejected
			 * @param joinFailed If not null, indicates why our join request was rejected
			 * @param leaveSessionFailed If not null, indicates why our leave request was rejected
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void failed (final MultiplayerServerConnection sender, final CreateAccountFailedReason createAccountFailed,
				final LoginFailedReason loginFailed, final LogoutFailedReason logoutFailed, final RequestSessionListFailedReason requestSessionListFailed,
				final JoinFailedReason joinFailed, final LeaveSessionFailedReason leaveSessionFailed)
				throws JAXBException, XMLStreamException, IOException
			{
				// Get relevant entry from language XML
				final String languageCategoryID;
				final String languageEntryID;
				
				if (createAccountFailed != null)
				{
					languageCategoryID = "CreateAccountFailedReason";
					languageEntryID = createAccountFailed.value ();
				}
				else if (loginFailed != null)
				{
					languageCategoryID = "LoginFailedReason";
					languageEntryID = loginFailed.value ();
				}
				else if (logoutFailed != null)
				{
					languageCategoryID = "LogoutFailedReason";
					languageEntryID = logoutFailed.value ();
				}
				else if (requestSessionListFailed != null)
				{
					languageCategoryID = "RequestSessionListFailedReason";
					languageEntryID = requestSessionListFailed.value ();
				}
				else if (joinFailed != null)
				{
					languageCategoryID = "JoinFailedReason";
					languageEntryID = joinFailed.value ();
				}
				else if (leaveSessionFailed != null)
				{
					languageCategoryID = "LeaveSessionFailedReason";
					languageEntryID = leaveSessionFailed.value ();
				}
				else
					throw new IOException ("MultiplayerSessionClientEvent.failed handler: Every failure code was null");

				// Display in window
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setLanguageCategoryID (languageCategoryID);
				msg.setLanguageEntryID (languageEntryID);
				try
				{
					msg.setVisible (true);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		});
		
		// To be correct, should start up the first Swing frame in the Swing thread
		SwingUtilities.invokeLater (new Runnable ()
		{
			@Override
			public void run ()
			{
				try
				{
					getMainMenuUI ().setVisible (true);
				}
				catch (final IOException e)
				{
					e.printStackTrace ();
				}
			}
		});
	}

	/**
	 * @return Name that we logged in using
	 */
	public final String getOurPlayerName ()
	{
		return ourPlayerName;
	}

	/**
	 * @param name Name that we logged in using
	 */
	public final void setOurPlayerName (final String name)
	{
		ourPlayerName = name;
	}
	
	/**
	 * @return Main menu with options to connect to a server and create or join games
	 */
	public final MainMenuUI getMainMenuUI ()
	{
		return mainMenuUI;
	}

	/**
	 * @param ui Main menu with options to connect to a server and create or join game
	 */
	public final void setMainMenuUI (final MainMenuUI ui)
	{
		mainMenuUI = ui;
	}

	/**
	 * @return Connect to server UI
	 */
	public final ConnectToServerUI getConnectToServerUI ()
	{
		return connectToServerUI;
	}

	/**
	 * @param ui Connect to server UI
	 */
	public final void setConnectToServerUI (final ConnectToServerUI ui)
	{
		connectToServerUI = ui;
	}
	
	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
	
	/**
	 * @return Info we need in order to create games; sent from server
	 */
	public final NewGameDatabase getNewGameDatabase ()
	{
		return newGameDatabase;
	}

	/**
	 * @param db Info we need in order to create games; sent from server
	 */
	public final void setNewGameDatabase (final NewGameDatabase db)
	{
		newGameDatabase = db;
	}
	
	/**
	 * @param args Command line arguments, ignored
	 */
	@SuppressWarnings ("resource")
	public final static void main (final String [] args)
	{
		try
		{
			// Ensure v1.7 JVM
			final String [] javaVersion = System.getProperty ("java.version").split ("\\.");
			final int majorVersion = Integer.parseInt (javaVersion [0]);
			final int minorVersion = Integer.parseInt (javaVersion [1]);

			if ((majorVersion < MomCommonConstants.JAVA_REQUIRED_MAJOR_VERSION) ||
				((majorVersion == MomCommonConstants.JAVA_REQUIRED_MAJOR_VERSION) && (minorVersion < MomCommonConstants.JAVA_REQUIRED_MINOR_VERSION)))
				
				throw new InvalidParameterException ("MoM IME requires a Java Virtual Machine version " +
					MomCommonConstants.JAVA_REQUIRED_MAJOR_VERSION + "." + MomCommonConstants.JAVA_REQUIRED_MINOR_VERSION +
					" or newer to run, but only detected version " + majorVersion + "." + minorVersion);
			
			// Initialize logging first, in case debug logging for spring itself is enabled
			try (final FileInputStream in = new FileInputStream ("MoMIMEClientLogging.properties"))
			{
				LogManager.getLogManager ().readConfiguration (in);
				in.close ();
			}

			// Everything is now set to start with spring
			new ClassPathXmlApplicationContext ("/momime.client.spring/momime-client-beans.xml");			
		}
		catch (final Exception e)
		{
			System.out.println ("Exception in main method:");
			e.printStackTrace ();
		}
	}
}
