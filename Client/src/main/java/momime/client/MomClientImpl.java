package momime.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.database.ClientDatabaseEx;
import momime.client.database.ClientDatabaseExImpl;
import momime.client.database.NewGameDatabase;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.ConnectToServerUI;
import momime.client.ui.frames.MainMenuUI;
import momime.client.ui.frames.NewGameUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.client.MultiplayerSessionClient;
import com.ndg.multiplayer.client.MultiplayerSessionClientEvent;
import com.ndg.multiplayer.sessionbase.CreateAccountFailedReason;
import com.ndg.multiplayer.sessionbase.JoinFailedReason;
import com.ndg.multiplayer.sessionbase.LeaveSessionFailedReason;
import com.ndg.multiplayer.sessionbase.LoginFailedReason;
import com.ndg.multiplayer.sessionbase.LogoutFailedReason;
import com.ndg.multiplayer.sessionbase.RequestSessionListFailedReason;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;
import com.ndg.swing.NdgUIUtils;

/**
 * Main multiplayer controller class for the client
 */
public final class MomClientImpl extends MultiplayerSessionClient implements MomClient
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MomClientImpl.class);
	
	/** Name that we logged in using */
	private String ourPlayerName;
	
	/** Main menu with options to connect to a server and create or join games */
	private MainMenuUI mainMenuUI;

	/** Connect to server UI */
	private ConnectToServerUI connectToServerUI;
	
	/** New Game UI */
	private NewGameUI newGameUI;
	
	/** Overland map bitmap generator */
	private OverlandMapBitmapGenerator overlandMapBitmapGenerator;

	/** Bitmap generator for the static terrain */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Info we need in order to create games; sent from server */
	private NewGameDatabase newGameDatabase;

	/** UI manager helper */
	private NdgUIUtils utils;
	
	/** List of all city views currently open, keyed by coordinates.toString () */
	private Map<String, CityViewUI> cityViews = new HashMap<String, CityViewUI> (); 
	
	/** List of all change constructions currently open, keyed by coordinates.toString () */
	private Map<String, ChangeConstructionUI> changeConstructions = new HashMap<String, ChangeConstructionUI> ();
	
	/** List of all unit info screens currently open, keyed by Unit URN */
	private Map<Integer, UnitInfoUI> unitInfos = new HashMap<Integer, UnitInfoUI> ();
	
	/**
	 * Kick off method invoked by spring's init-method
	 */
	public final void start ()
	{
		log.trace ("Entering start");
		
		// Multiplayer client event handlers
		getEventListeners ().add (new MultiplayerSessionClientEvent ()
		{
			/**
			 * Event triggered when we server successfully creates an account for us
			 * 
			 * @param playerID Player ID allocated to our new account
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void accountCreated (final int playerID) throws JAXBException, XMLStreamException, IOException
			{
				// Log in with the new account
				getConnectToServerUI ().afterAccountCreated ();
			}

			/**
			 * Event triggered after we successfully log in.
			 * 
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void loggedIn () throws JAXBException, XMLStreamException, IOException
			{
				getConnectToServerUI ().afterLoggedIn ();
				getConnectToServerUI ().setVisible (false);
				getMainMenuUI ().enableActions ();
			}
			
			/**
			 * Event triggered as we successfully log out - event is
			 * triggered just before playerID and session variables are cleared.
			 * 
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void loggedOut () throws JAXBException, XMLStreamException, IOException
			{
				getMainMenuUI ().enableActions ();
			}
			
			/**
			 * Forcibly logged out by another client logging in with our account.  This event is triggered just before
			 * the playerID and session variables are cleared.
			 * 
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void kickedByAnotherLogin () throws JAXBException, XMLStreamException, IOException
			{
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setTitleLanguageCategoryID ("Multiplayer");
				msg.setTitleLanguageEntryID ("KickedTitle");
				msg.setTextLanguageCategoryID ("Multiplayer");
				msg.setTextLanguageEntryID ("KickedText");
				try
				{
					msg.setVisible (true);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
				
				getMainMenuUI ().enableActions ();
			}
			
			/**
			 * Event triggered when server tells us which sessions we can join
			 * 
			 * @param sessions List of sessions the server is telling us we can join
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void receivedSessionList (final List<SessionAndPlayerDescriptions> sessions) throws JAXBException, XMLStreamException, IOException
			{
			}

			/**
			 * Event triggered when we successfully join a session
			 * 
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void joinedSession () throws JAXBException, XMLStreamException, IOException
			{
				((ClientDatabaseExImpl) getClientDB ()).buildMapsAndRunConsistencyChecks ();
				getNewGameUI ().afterJoinedSession ();
				getOverlandMapBitmapGenerator ().afterJoinedSession ();
				getCombatMapBitmapGenerator ().afterJoinedSession ();
			}

			/**
			 * Event triggered when somebody else joins the session we're already in.  This only gets sent if the player is genuinely new
			 * to the session - if they disconnect and rejoin back into their original slot, this message will not be sent.
			 * 
			 * @param playerID Player who joined
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void additionalPlayerJoined (final int playerID) throws JAXBException, XMLStreamException, IOException
			{
			}

			/**
			 * Event triggered when somebody leaves the session we're in, note it could be us leaving.
			 * Event is triggered just prior to player being removed from the list, and if us leaving, before session variables being nulled out.
			 * 
			 * @param playerID Player who left
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void playerLeft (final int playerID) throws JAXBException, XMLStreamException, IOException
			{
			}

			/**
			 * Event triggered when the server closes down a session when we're still in it.  This is triggered just before all the
			 * session variables are nulled out.
			 * 
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void sessionEnding () throws JAXBException, XMLStreamException, IOException
			{
			}

			/**
			 * Event triggered when the server rejects a request.  Exactly one of the failure codes will be non-null
			 * which indicates which kind of request got rejected.
			 * 
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
			public final void failed (final CreateAccountFailedReason createAccountFailed,
				final LoginFailedReason loginFailed, final LogoutFailedReason logoutFailed, final RequestSessionListFailedReason requestSessionListFailed,
				final JoinFailedReason joinFailed, final LeaveSessionFailedReason leaveSessionFailed)
				throws JAXBException, XMLStreamException, IOException
			{
				// Get relevant entry from language XML
				final String languageCategoryID;
				final String languageEntryID;
				final String titleEntryID;
				
				if (createAccountFailed != null)
				{
					titleEntryID = "CreateAccountFailedTitle";
					languageCategoryID = "CreateAccountFailedReason";
					languageEntryID = createAccountFailed.value ();
				}
				else if (loginFailed != null)
				{
					titleEntryID = "LoginFailedTitle";
					languageCategoryID = "LoginFailedReason";
					languageEntryID = loginFailed.value ();
				}
				else if (logoutFailed != null)
				{
					titleEntryID = "LogoutFailedTitle";
					languageCategoryID = "LogoutFailedReason";
					languageEntryID = logoutFailed.value ();
				}
				else if (requestSessionListFailed != null)
				{
					titleEntryID = "RequestSessionListFailedTitle";
					languageCategoryID = "RequestSessionListFailedReason";
					languageEntryID = requestSessionListFailed.value ();
				}
				else if (joinFailed != null)
				{
					titleEntryID = "JoinSessionFailedTitle";
					languageCategoryID = "JoinFailedReason";
					languageEntryID = joinFailed.value ();
				}
				else if (leaveSessionFailed != null)
				{
					titleEntryID = "LeaveSessionFailedTitle";
					languageCategoryID = "LeaveSessionFailedReason";
					languageEntryID = leaveSessionFailed.value ();
				}
				else
					throw new IOException ("MultiplayerSessionClientEvent.failed handler: Every failure code was null");

				// Display in window
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setTitleLanguageCategoryID ("Multiplayer");
				msg.setTitleLanguageEntryID (titleEntryID);
				msg.setTextLanguageCategoryID (languageCategoryID);
				msg.setTextLanguageEntryID (languageEntryID);
				try
				{
					msg.setVisible (true);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
		
		// To be correct, should start up the first Swing frame in the Swing thread
		log.info ("Starting up UI");
		SwingUtilities.invokeLater (new Runnable ()
		{
			@Override
			public final void run ()
			{
				try
				{
					getUtils ().useNimbusLookAndFeel ();
					getMainMenuUI ().setVisible (true);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		});
		
		log.trace ("Exiting start");
	}
	
	/**
	 * @return Public knowledge structure, typecasted to MoM specific type
	 */
	@Override
	public final MomGeneralPublicKnowledge getGeneralPublicKnowledge ()
	{
		return (MomGeneralPublicKnowledge) super.getGeneralPublicKnowledge ();
	}

	/**
	 * @return Client XML in use for this session
	 */
	@Override
	public final ClientDatabaseEx getClientDB ()
	{
		return (ClientDatabaseEx) getGeneralPublicKnowledge ().getClientDatabase ();
	}
	
	/**
	 * @return Session description, typecasted to MoM specific type
	 */
	@Override
	public final MomSessionDescription getSessionDescription ()
	{
		return (MomSessionDescription) super.getSessionDescription ();
	}
	
	/**
	 * @return Private knowledge about our player that is persisted to save game files,  typecasted to MoM specific type
	 */
	@Override
	public final MomPersistentPlayerPrivateKnowledge getOurPersistentPlayerPrivateKnowledge ()
	{
		return (MomPersistentPlayerPrivateKnowledge) super.getOurPersistentPlayerPrivateKnowledge ();
	}
	
	/**
	 * @return Private knowledge about our player that is not persisted to save game files,  typecasted to MoM specific type
	 */
	@Override
	public final MomTransientPlayerPrivateKnowledge getOurTransientPlayerPrivateKnowledge ()
	{
		return (MomTransientPlayerPrivateKnowledge) super.getOurTransientPlayerPrivateKnowledge ();
	}
	
	/**
	 * @return Name that we logged in using
	 */
	@Override
	public final String getOurPlayerName ()
	{
		return ourPlayerName;
	}

	/**
	 * @param name Name that we logged in using
	 */
	@Override
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
	 * @return New Game UI
	 */
	public final NewGameUI getNewGameUI ()
	{
		return newGameUI;
	}

	/**
	 * @param ui New Game UI
	 */
	public final void setNewGameUI (final NewGameUI ui)
	{
		newGameUI = ui;
	}
	
	/**
	 * @return Overland map bitmap generator
	 */
	public final OverlandMapBitmapGenerator getOverlandMapBitmapGenerator ()
	{
		return overlandMapBitmapGenerator;
	}
	
	/**
	 * @param gen Overland map bitmap generator
	 */
	public final void setOverlandMapBitmapGenerator (final OverlandMapBitmapGenerator gen)
	{
		overlandMapBitmapGenerator = gen;
	}

	/**
	 * @return Bitmap generator for the static terrain
	 */
	public final CombatMapBitmapGenerator getCombatMapBitmapGenerator ()
	{
		return combatMapBitmapGenerator;
	}

	/**
	 * @param gen Bitmap generator for the static terrain
	 */
	public final void setCombatMapBitmapGenerator (final CombatMapBitmapGenerator gen)
	{
		combatMapBitmapGenerator = gen;
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
	@Override
	public final NewGameDatabase getNewGameDatabase ()
	{
		return newGameDatabase;
	}

	/**
	 * @param db Info we need in order to create games; sent from server
	 */
	@Override
	public final void setNewGameDatabase (final NewGameDatabase db)
	{
		newGameDatabase = db;
	}

	/**
	 * @return UI manager helper
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param mgr UI manager helper
	 */
	public final void setUtils (final NdgUIUtils mgr)
	{
		utils = mgr;
	}
	
	/**
	 * @return List of all city views currently open, keyed by coordinates.toString ()
	 */
	@Override
	public final Map<String, CityViewUI> getCityViews ()
	{
		return cityViews;
	}
	
	/**
	 * @return List of all change constructions currently open, keyed by coordinates.toString ()
	 */
	@Override
	public final Map<String, ChangeConstructionUI> getChangeConstructions () 
	{
		return changeConstructions;
	}

	/**
	 * @return List of all unit info screens currently open, keyed by Unit URN
	 */
	@Override
	public final Map<Integer, UnitInfoUI> getUnitInfos ()
	{
		return unitInfos;
	}
}