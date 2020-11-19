package momime.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.client.MultiplayerSessionClient;
import com.ndg.multiplayer.client.MultiplayerSessionClientEvent;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.BrowseSavePointsFailedReason;
import com.ndg.multiplayer.sessionbase.BrowseSavedGames;
import com.ndg.multiplayer.sessionbase.BrowseSavedGamesFailedReason;
import com.ndg.multiplayer.sessionbase.CreateAccountFailedReason;
import com.ndg.multiplayer.sessionbase.DeleteSavedGameFailedReason;
import com.ndg.multiplayer.sessionbase.JoinFailedReason;
import com.ndg.multiplayer.sessionbase.JoinSuccessfulReason;
import com.ndg.multiplayer.sessionbase.LeaveSessionFailedReason;
import com.ndg.multiplayer.sessionbase.LoadGameFailedReason;
import com.ndg.multiplayer.sessionbase.LoginFailedReason;
import com.ndg.multiplayer.sessionbase.LogoutFailedReason;
import com.ndg.multiplayer.sessionbase.RequestSessionListFailedReason;
import com.ndg.multiplayer.sessionbase.SavedGamePoint;
import com.ndg.multiplayer.sessionbase.SavedGameSession;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;
import com.ndg.swing.NdgUIUtils;

import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.database.NewGameDatabase;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.dialogs.CastCombatSpellFromUI;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.RazeCityUI;
import momime.client.ui.dialogs.VariableManaUI;
import momime.client.ui.frames.AlchemyUI;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CitiesListUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.ConnectToServerUI;
import momime.client.ui.frames.CreateArtifactUI;
import momime.client.ui.frames.DamageCalculationsUI;
import momime.client.ui.frames.HeroItemInfoUI;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.frames.JoinGameUI;
import momime.client.ui.frames.LoadGameUI;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.MainMenuUI;
import momime.client.ui.frames.NewGameUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.QueuedSpellsUI;
import momime.client.ui.frames.SelectAdvisorUI;
import momime.client.ui.frames.SpellBookUI;
import momime.client.ui.frames.TaxRateUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.frames.WizardsUI;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.LanguageText;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

/**
 * Main multiplayer controller class for the client
 */
public final class MomClientImpl extends MultiplayerSessionClient implements MomClient
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MomClientImpl.class);
	
	/** Name that we logged in using */
	private String ourPlayerName;
	
	/** Main menu with options to connect to a server and create or join games */
	private MainMenuUI mainMenuUI;

	/** Connect to server UI */
	private ConnectToServerUI connectToServerUI;
	
	/** New Game UI */
	private NewGameUI newGameUI;
	
	/** Join Game UI */
	private JoinGameUI joinGameUI;

	/** Load Game UI */
	private LoadGameUI loadGameUI;
	
	/** Tax rate UI */
	private TaxRateUI taxRateUI;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;

	/** Alchemy UI */
	private AlchemyUI alchemyUI;
	
	/** Spell book */
	private SpellBookUI spellBookUI;

	/** Queued spells UI */
	private QueuedSpellsUI queuedSpellsUI;
	
	/** Cities list */
	private CitiesListUI citiesListUI;

	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Advisors UI */
	private SelectAdvisorUI selectAdvisorUI;
	
	/** Combat UI */
	private CombatUI combatUI;

	/** Select casting source popup */
	private CastCombatSpellFromUI castCombatSpellFromUI;
	
	/** UI for displaying damage calculations */
	private DamageCalculationsUI damageCalculationsUI;

	/** Variable MP popup */
	private VariableManaUI variableManaUI;	
	
	/** Raze city UI */
	private RazeCityUI razeCityUI;

	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** Crafting popup */
	private CreateArtifactUI createArtifactUI;
	
	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/** Music player */
	private AudioPlayer musicPlayer;
	
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
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** List of all city views currently open, keyed by coordinates.toString () */
	private Map<String, CityViewUI> cityViews = new HashMap<String, CityViewUI> (); 
	
	/** List of all change constructions currently open, keyed by coordinates.toString () */
	private Map<String, ChangeConstructionUI> changeConstructions = new HashMap<String, ChangeConstructionUI> ();
	
	/** List of all unit info screens currently open, keyed by Unit URN */
	private Map<Integer, UnitInfoUI> unitInfos = new HashMap<Integer, UnitInfoUI> ();
	
	/** List of all hero item info screens currently open, keyed by Hero Item URN */
	private Map<Integer, HeroItemInfoUI> heroItemInfos = new HashMap<Integer, HeroItemInfoUI> ();
	
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
			public final void accountCreated (@SuppressWarnings ("unused") final int playerID) throws JAXBException, XMLStreamException, IOException
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
				msg.setLanguageTitle (getLanguages ().getMultiplayer ().getKickedTitle ());
				msg.setLanguageText (getLanguages ().getMultiplayer ().getKickedText ());
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
				getJoinGameUI ().setSessions (sessions);
			}

			/**
			 * Event triggered when we successfully join a session
			 * 
			 * @param reason The type of session we've joined into
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void joinedSession (final JoinSuccessfulReason reason) throws JAXBException, XMLStreamException, IOException
			{
				getClientDB ().buildMaps ();
				getClientDB ().consistencyChecks ();
				getClientDB ().clientConsistencyChecks ();
				getJoinGameUI ().setVisible (false);
				getLoadGameUI ().setVisible (false);
				getMainMenuUI ().setVisible (false);
				getNewGameUI ().setVisible (true);
				
				getNewGameUI ().afterJoinedSession ();
				getOverlandMapBitmapGenerator ().afterJoinedSession ();
				getCombatMapBitmapGenerator ().afterJoinedSession ();
				getTaxRateUI ().updateTaxRateButtons ();
				
				// If making or joining a new game, we won't yet know the photos and flag colours all the players are using
				// but if reloading a game, we'll already have this info, and have to use it to populate the flag colour in the player's transient data
				for (final PlayerPublicDetails player : getPlayers ())
				{
					final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
					final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
					
					if (pub.getCustomFlagColour () != null)
						trans.setFlagColour (pub.getCustomFlagColour ());
					
					else if (pub.getStandardPhotoID () != null)
						trans.setFlagColour (getClientDB ().findWizard (pub.getStandardPhotoID (), "joinedSession").getFlagColour ());
				}
				
				// Also if reloading a game, or joining a game being reloaded, show the wait for players screen
				if ((reason == JoinSuccessfulReason.LOADED_SAVED_GAME) || (reason == JoinSuccessfulReason.REJOINED_SESSION))
				{
					// Prepare the overland map
					getOverlandMapBitmapGenerator ().smoothMapTerrain (null);
					getOverlandMapUI ().regenerateOverlandMapBitmaps ();
					getOverlandMapUI ().regenerateFogOfWarBitmap ();
					
					// Wait for other players to join; the server tells us when we've got everybody and to start
					getNewGameUI ().showWaitPanel ();
				}
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
			public final void additionalPlayerJoined (@SuppressWarnings ("unused") final int playerID) throws JAXBException, XMLStreamException, IOException
			{
				getNewGameUI ().updateWaitPanelPlayersList ();
			}
			
			/**
			 * Event triggered when somebody leaves the session we're in, note it could be us leaving.
			 * Event is triggered before player is removed from the list, so if it is us leaving we have time to tidy up the session while we can still access it.
			 * 
			 * @param playerID Player who left
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void beforePlayerLeft (final int playerID) throws JAXBException, XMLStreamException, IOException
			{
				// Was it us who left?  If so then close down any game windows that may be open
				if ((getOurPlayerID () != null) && (playerID == getOurPlayerID ()))
				{
					final List<String> cityViewsToClose = getCityViews ().keySet ().stream ().collect (Collectors.toList ());
					cityViewsToClose.forEach (c -> getCityViews ().get (c).close ());

					final List<String> changeConstructionsToClose = getChangeConstructions ().keySet ().stream ().collect (Collectors.toList ());
					changeConstructionsToClose.forEach (c -> getChangeConstructions ().get (c).close ());
					
					final List<Integer> unitInfosToClose = getUnitInfos ().keySet ().stream ().collect (Collectors.toList ());
					unitInfosToClose.forEach (u -> getUnitInfos ().get (u).close ());
					
					final List<Integer> heroItemsToClose = getHeroItemInfos ().keySet ().stream ().collect (Collectors.toList ());
					heroItemsToClose.forEach (i -> getHeroItemInfos ().get (i).close ());
					
					getOverlandMapUI ().setVisible (false);
					getTaxRateUI ().setVisible (false);
					getMagicSlidersUI ().setVisible (false);
					getAlchemyUI ().setVisible (false);
					getSpellBookUI ().setVisible (false);
					getQueuedSpellsUI ().setVisible (false);
					getCitiesListUI ().setVisible (false);
					getNewTurnMessagesUI ().setVisible (false);
					getSelectAdvisorUI ().setVisible (false);
					
					getCombatUI ().setVisible (false);
					getCastCombatSpellFromUI ().setVisible (false);
					getRazeCityUI ().setVisible (false);
					getDamageCalculationsUI ().setVisible (false);
					getVariableManaUI ().setVisible (false);
					
					getWizardsUI ().setVisible (false);
					getCreateArtifactUI ().setVisible (false);
					getHeroItemsUI ().setVisible (false);
				}
			}

			/**
			 * Event triggered when somebody leaves the session we're in, note it could be us leaving.
			 * Event is triggered after player is removed from the list, and if us leaving, session description and others are already blanked out.
			 * 
			 * @param playerID Player who left
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void afterPlayerLeft (final int playerID) throws JAXBException, XMLStreamException, IOException
			{
				// Was it us who left?  If so then open up the main menu again
				if ((getOurPlayerID () != null) && (playerID == getOurPlayerID ()))
				{
					if (!getMainMenuUI ().isVisible ())
					{
						getMainMenuUI ().playMusic ();
						getMainMenuUI ().setVisible (true);
					}
				}
				else
					getNewGameUI ().updateWaitPanelPlayersList ();
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
			 * Event triggered when server tells us what saved games we have stored
			 * 
			 * @param savedGames List of saved games the server is telling us we can reload
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void receivedSavedGamesList (final List<SavedGameSession> savedGames)
				throws JAXBException, XMLStreamException, IOException
			{
				getLoadGameUI ().setVisible (true);
				getLoadGameUI ().setSavedGames (savedGames);
			}

			/**
			 * Event triggered when server tells us what save points one of our saved games has stored
			 * 
			 * @param savePoints List of save points within a saved game that the server is telling us we can join
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void receivedSavePointsList (final List<SavedGamePoint> savePoints)
				throws JAXBException, XMLStreamException, IOException
			{
				getLoadGameUI ().setVisible (true);
				getLoadGameUI ().setSavePoints (savePoints);
			}
			
			/**
			 * Event triggered when the server successfully deleted a saved game that we requested.
			 * 
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void savedGameDeleted () throws JAXBException, XMLStreamException, IOException
			{
				// Re-request the list of saved games, so the screen updates
				getServerConnection ().sendMessageToServer (new BrowseSavedGames ());
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
			 * @param browseSavedGamesFailed If not null, indicates why our browse saved games request was rejected
			 * @param browseSavePointsFailed If not null, indicates why our browse save points request was rejected
			 * @param deleteSavedGameFailed If not null, indicates why our delete saved game request was rejected
			 * @param loadGameFailed If not null, indicates why our load game request was rejected
			 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
			 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
			 * @throws IOException Can be used for more general types of processing failure
			 */
			@Override
			public final void failed (final CreateAccountFailedReason createAccountFailed,
				final LoginFailedReason loginFailed, final LogoutFailedReason logoutFailed, final RequestSessionListFailedReason requestSessionListFailed,
				final JoinFailedReason joinFailed, final LeaveSessionFailedReason leaveSessionFailed, final BrowseSavedGamesFailedReason browseSavedGamesFailed,
				final BrowseSavePointsFailedReason browseSavePointsFailed, final DeleteSavedGameFailedReason deleteSavedGameFailed, final LoadGameFailedReason loadGameFailed)
				throws JAXBException, XMLStreamException, IOException
			{
				// Get relevant entry from language XML
				final List<LanguageText> languageTitle;
				final List<LanguageText> languageText;
				
				if (createAccountFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getCreateAccountFailed ().getTitle ();
					switch (createAccountFailed)
					{
						case PLAYER_ALREADY_EXISTS:
							languageText = getLanguages ().getMultiplayer ().getCreateAccountFailed ().getPlayerAlreadyExists ();
							break;
							
						default:
							throw new MomException ("Don't understand create account failure code of " + createAccountFailed);
					}
				}
				else if (loginFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getLoginFailed ().getTitle ();
					switch (loginFailed)
					{
						case YOUR_CONNECTION_IS_ALREADY_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getLoginFailed ().getYourConnectionIsAlreadyLoggedIn ();
							break;
							
						case PLAYER_NAME_OR_PASSWORD_NOT_RECOGNIZED:
							languageText = getLanguages ().getMultiplayer ().getLoginFailed ().getPlayerNameOrPasswordNotRecognized ();
							break;

						case ANOTHER_CONNECTION_USING_YOUR_PLAYER_ID:
							languageText = getLanguages ().getMultiplayer ().getLoginFailed ().getAnotherConnectionUsingYourPlayerID ();
							break;
							
						default:
							throw new MomException ("Don't understand login failure code of " + loginFailed);
					}
				}
				else if (logoutFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getLogoutFailed ().getTitle ();
					switch (logoutFailed)
					{
						case NOT_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getLogoutFailed ().getNotLoggedIn ();
							break;
							
						default:
							throw new MomException ("Don't understand logout failure code of " + logoutFailed);
					}
				}
				else if (requestSessionListFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getRequestSessionListFailed ().getTitle ();
					switch (requestSessionListFailed)
					{
						case NOT_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getRequestSessionListFailed ().getNotLoggedIn ();
							break;
							
						default:
							throw new MomException ("Don't understand request session list failure code of " + requestSessionListFailed);
					}
				}
				else if (joinFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getJoinFailed ().getTitle ();
					switch (joinFailed)
					{
						case NOT_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getJoinFailed ().getNotLoggedIn ();
							break;
							
						case ALREADY_IN_SESSION:
							languageText = getLanguages ().getMultiplayer ().getJoinFailed ().getAlreadyInSession ();
							break;
							
						case SESSION_ID_NOT_FOUND:
							languageText = getLanguages ().getMultiplayer ().getJoinFailed ().getSessionIDNotFound ();
							break;
							
						case SESSION_FULL:
							languageText = getLanguages ().getMultiplayer ().getJoinFailed ().getSessionFull ();
							break;
							
						default:
							throw new MomException ("Don't understand join session failure code of " + joinFailed);
					}
				}
				else if (leaveSessionFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getLeaveSessionFailed ().getTitle ();
					switch (leaveSessionFailed)
					{
						case NOT_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getLeaveSessionFailed ().getNotLoggedIn ();
							break;
							
						case NOT_IN_SESSION:
							languageText = getLanguages ().getMultiplayer ().getLeaveSessionFailed ().getNotInSession ();
							break;
							
						default:
							throw new MomException ("Don't understand leave session failure code of " + leaveSessionFailed);
					}
				}
				else if (browseSavedGamesFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getBrowseSavedGamesFailed ().getTitle ();
					switch (browseSavedGamesFailed)
					{
						case NOT_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getBrowseSavedGamesFailed ().getNotLoggedIn ();
							break;
							
						case ALREADY_IN_SESSION:
							languageText = getLanguages ().getMultiplayer ().getBrowseSavedGamesFailed ().getAlreadyInSession ();
							break;
							
						case SAVED_GAMES_NOT_SUPPORTED:
							languageText = getLanguages ().getMultiplayer ().getBrowseSavedGamesFailed ().getSavedGamesNotSupported ();
							break;
							
						default:
							throw new MomException ("Don't understand browse saved games failure code of " + browseSavedGamesFailed);
					}
				}
				else if (browseSavePointsFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getBrowseSavePointsFailed ().getTitle ();
					switch (browseSavePointsFailed)
					{
						case NOT_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getBrowseSavePointsFailed ().getNotLoggedIn ();
							break;
							
						case ALREADY_IN_SESSION:
							languageText = getLanguages ().getMultiplayer ().getBrowseSavePointsFailed ().getAlreadyInSession ();
							break;
							
						case SAVE_GAME_ID_NOT_FOUND:
							languageText = getLanguages ().getMultiplayer ().getBrowseSavePointsFailed ().getSavedGameIDNotFound ();
							break;
							
						case SAVED_GAMES_NOT_SUPPORTED:
							languageText = getLanguages ().getMultiplayer ().getBrowseSavePointsFailed ().getSavedGamesNotSupported ();
							break;
							
						default:
							throw new MomException ("Don't understand browse save points failure code of " + browseSavePointsFailed);
					}
				}
				else if (deleteSavedGameFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getDeleteSavedGameFailed ().getTitle ();
					switch (deleteSavedGameFailed)
					{
						case ALREADY_IN_SESSION:
							languageText = getLanguages ().getMultiplayer ().getDeleteSavedGameFailed ().getAlreadyInSession ();
							break;
							
						case NOT_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getDeleteSavedGameFailed ().getNotLoggedIn ();
							break;
							
						case SAVE_GAME_ID_NOT_FOUND:
							languageText = getLanguages ().getMultiplayer ().getDeleteSavedGameFailed ().getSavedGameIDNotFound ();
							break;
							
						case SAVED_GAMES_NOT_SUPPORTED:
							languageText = getLanguages ().getMultiplayer ().getDeleteSavedGameFailed ().getSavedGamesNotSupported ();
							break;
							
						default:
							throw new MomException ("Don't understand delete saved game failure code of " + deleteSavedGameFailed);
					}
				}
				else if (loadGameFailed != null)
				{
					languageTitle = getLanguages ().getMultiplayer ().getLoadGameFailed ().getTitle ();
					switch (loadGameFailed)
					{
						case NOT_LOGGED_IN:
							languageText = getLanguages ().getMultiplayer ().getLoadGameFailed ().getNotLoggedIn ();
							break;
							
						case ALREADY_IN_SESSION:
							languageText = getLanguages ().getMultiplayer ().getLoadGameFailed ().getAlreadyInSession ();
							break;
							
						case SAVE_GAME_ID_NOT_FOUND:
							languageText = getLanguages ().getMultiplayer ().getLoadGameFailed ().getSavedGameIDNotFound ();
							break;
							
						case SAVE_GAME_FILENAME_NOT_FOUND:
							languageText = getLanguages ().getMultiplayer ().getLoadGameFailed ().getSavedGameFilenameNotFound ();
							break;
							
						case SAVED_GAMES_NOT_SUPPORTED:
							languageText = getLanguages ().getMultiplayer ().getLoadGameFailed ().getSavedGamesNotSupported ();
							break;
							
						default:
							throw new MomException ("Don't understand load game failure code of " + loadGameFailed);
					}
				}
				else
					throw new MomException ("MultiplayerSessionClientEvent.failed handler: Every failure code was null");

				// Display in window
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setLanguageTitle (languageTitle);
				msg.setLanguageText (languageText);
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
		SwingUtilities.invokeLater (() ->
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
	public final CommonDatabase getClientDB ()
	{
		return (CommonDatabase) getGeneralPublicKnowledge ().getMomDatabase ();
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
	 * @return Join Game UI
	 */
	public final JoinGameUI getJoinGameUI ()
	{
		return joinGameUI;
	}

	/**
	 * @param ui Join Game UI
	 */
	public final void setJoinGameUI (final JoinGameUI ui)
	{
		joinGameUI = ui;
	}

	/**
	 * @return Load Game UI
	 */
	public final LoadGameUI getLoadGameUI ()
	{
		return loadGameUI;
	}

	/**
	 * @param ui Load Game UI
	 */
	public final void setLoadGameUI (final LoadGameUI ui)
	{
		loadGameUI = ui;
	}
	
	/**
	 * @return Tax rate UI
	 */
	public final TaxRateUI getTaxRateUI ()
	{
		return taxRateUI;
	}

	/**
	 * @param ui Tax rate UI
	 */
	public final void setTaxRateUI (final TaxRateUI ui)
	{
		taxRateUI = ui;
	}

	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}

	/**
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
	}

	/**
	 * @return Alchemy UI
	 */
	public final AlchemyUI getAlchemyUI ()
	{
		return alchemyUI;
	}

	/**
	 * @param ui Alchemy UI
	 */
	public final void setAlchemyUI (final AlchemyUI ui)
	{
		alchemyUI = ui;
	}
	
	/**
	 * @return Spell book
	 */
	public final SpellBookUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookUI ui)
	{
		spellBookUI = ui;
	}

	/**
	 * @return Queued spells UI
	 */
	public final QueuedSpellsUI getQueuedSpellsUI ()
	{
		return queuedSpellsUI;
	}

	/**
	 * @param ui Queued spells UI
	 */
	public final void setQueuedSpellsUI (final QueuedSpellsUI ui)
	{
		queuedSpellsUI = ui;
	}
	
	/**
	 * @return Cities list
	 */
	public final CitiesListUI getCitiesListUI ()
	{
		return citiesListUI;
	}

	/**
	 * @param ui Cities list
	 */
	public final void setCitiesListUI (final CitiesListUI ui)
	{
		citiesListUI = ui;
	}
	
	/**
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
	}

	/**
	 * @return Advisors UI
	 */
	public final SelectAdvisorUI getSelectAdvisorUI ()
	{
		return selectAdvisorUI;
	}

	/**
	 * @param ui Advisors UI
	 */
	public final void setSelectAdvisorUI (final SelectAdvisorUI ui)
	{
		selectAdvisorUI = ui;
	}
	
	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}

	/**
	 * @return Select casting source popup
	 */
	public final CastCombatSpellFromUI getCastCombatSpellFromUI ()
	{
		return castCombatSpellFromUI;
	}

	/**
	 * @param ui Select casting source popup
	 */
	public final void setCastCombatSpellFromUI (final CastCombatSpellFromUI ui)
	{
		castCombatSpellFromUI = ui;
	}

	/**
	 * @return UI for displaying damage calculations
	 */
	public final DamageCalculationsUI getDamageCalculationsUI ()
	{
		return damageCalculationsUI;
	}

	/**
	 * @param ui UI for displaying damage calculations
	 */
	public final void setDamageCalculationsUI (final DamageCalculationsUI ui)
	{
		damageCalculationsUI = ui;
	}

	/**
	 * @return Variable MP popup
	 */
	public VariableManaUI getVariableManaUI ()
	{
		return variableManaUI;
	}

	/**
	 * @param ui Variable MP popup
	 */
	public final void setVariableManaUI (final VariableManaUI ui)
	{
		variableManaUI = ui;
	}
	
	/**
	 * @return Raze city UI
	 */
	public final RazeCityUI getRazeCityUI ()
	{
		return razeCityUI;
	}		

	/**
	 * @param ui Raze city UI
	 */
	public final void setRazeCityUI (final RazeCityUI ui)
	{
		razeCityUI = ui;
	}	

	/**
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
	}
	
	/**
	 * @return Crafting popup
	 */
	public final CreateArtifactUI getCreateArtifactUI ()
	{
		return createArtifactUI;
	}

	/**
	 * @param ui Crafting popup
	 */
	public final void setCreateArtifactUI (final CreateArtifactUI ui)
	{
		createArtifactUI = ui;
	}
	
	/**
	 * @return Hero items UI
	 */
	public final HeroItemsUI getHeroItemsUI ()
	{
		return heroItemsUI;
	}

	/**
	 * @param ui Hero items UI
	 */
	public final void setHeroItemsUI (final HeroItemsUI ui)
	{
		heroItemsUI = ui;
	}
	
	/**
	 * @return Music player
	 */
	public final AudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final AudioPlayer player)
	{
		musicPlayer = player;
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
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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

	/**
	 * @return List of all hero item info screens currently open, keyed by Hero Item URN
	 */
	@Override
	public final Map<Integer, HeroItemInfoUI> getHeroItemInfos ()
	{
		return heroItemInfos;
	}
}