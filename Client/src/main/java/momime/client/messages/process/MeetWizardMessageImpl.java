package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.ui.frames.DiplomacyPortraitState;
import momime.client.ui.frames.DiplomacyTextState;
import momime.client.ui.frames.DiplomacyUI;
import momime.client.ui.frames.HistoryUI;
import momime.client.ui.frames.NewGameUI;
import momime.client.ui.frames.WizardsUI;
import momime.common.MomException;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.servertoclient.MeetWizardMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Message server sends when we meet a wizard for the first time.
 * We get sent our own wizard record when we pick which wizard we want to be.  We get sent the raiders/monsters records when the game starts.
 * Other wizards' records we are only sent when we learn who they are.
 */
public final class MeetWizardMessageImpl extends MeetWizardMessage implements CustomDurationServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** New Game UI */
	private NewGameUI newGameUI;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** UI for screen showing power base history for each wizard */
	private HistoryUI historyUI;
	
	/** Diplomacy UI */
	private DiplomacyUI diplomacyUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// We should never receive this twice for the same player
		if (getKnownWizardUtils ().findKnownWizardDetails (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), getKnownWizardDetails ().getPlayerID ()) != null)
			throw new MomException ("Server sent us KnownWizardDetails for player ID " + getKnownWizardDetails ().getPlayerID () + " when we already had them");
		
		// Add it
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails ().add (getKnownWizardDetails ());
		
		// Set flag colour (if this is us receiving our own wizard details during game setup and we chose custom wizard, we may not know this yet)
		final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getKnownWizardDetails ().getPlayerID (), "MeetWizardMessageImpl");
		final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
		
		if (getKnownWizardDetails ().getStandardPhotoID () != null)
			trans.setFlagColour (getClient ().getClientDB ().findWizard (getKnownWizardDetails ().getStandardPhotoID (), "MeetWizardMessageImpl").getFlagColour ());
		else
			trans.setFlagColour (getKnownWizardDetails ().getCustomFlagColour ());
		
		// If it is us, and we chose Custom, then we need to go to the Choose Portrait screen
		// If it is us and we picked a pre-defined Wizard then do nothing - the server will have already sent us either mmChooseInitialSpells or
		// mmChooseYourRaceNow to tell us what to do next before it sent this mmChosenWizard message
		if ((getKnownWizardDetails ().getPlayerID () == getClient ().getOurPlayerID ()) && (getPlayerKnowledgeUtils ().isCustomWizard (getKnownWizardDetails ().getWizardID ())))
			getNewGameUI ().showPortraitPanel ();
			
		// Update screens to show opponent wizards as we meet them
		getWizardsUI ().updateWizards (false);
		getHistoryUI ().redrawChart ();
		
		// Show diplomacy screen?
		if ((getKnownWizardDetails ().getPlayerID () != getClient ().getOurPlayerID ()) && (isShowAnimation () != null) && (isShowAnimation ()))
		{
			getDiplomacyUI ().setTalkingWizardID (getKnownWizardDetails ().getPlayerID ());
			getDiplomacyUI ().initializeTalkingWizard ();
			getDiplomacyUI ().setProposingWizardID (getKnownWizardDetails ().getPlayerID ());
			getDiplomacyUI ().setMeetWizardMessage (this);
			getDiplomacyUI ().setDiplomacyAction (null);
			getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.APPEARING);
			getDiplomacyUI ().setTextState (DiplomacyTextState.NONE);		// Text doesn't appear until the animation showing the wizard appearing completes
			getDiplomacyUI ().setVisibleRelationScoreID (getVisibleRelationScoreID ());
			getDiplomacyUI ().updateRelationScore ();
			getDiplomacyUI ().initializeText ();
			getDiplomacyUI ().initializePortrait ();
			getDiplomacyUI ().setVisible (true);
		}
		else
			getClient ().finishCustomDurationMessage (this);
	}
	
	/**
	 * Nothing to do here when the message completes, because DiplomacyUI already closed itself
	 */
	@Override
	public final void finish ()
	{
	}
	
	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
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
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
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
	 * @return UI for screen showing power base history for each wizard
	 */
	public final HistoryUI getHistoryUI ()
	{
		return historyUI;
	}

	/**
	 * @param h UI for screen showing power base history for each wizard
	 */
	public final void setHistoryUI (final HistoryUI h)
	{
		historyUI = h;
	}

	/**
	 * @return Diplomacy UI
	 */
	public final DiplomacyUI getDiplomacyUI ()
	{
		return diplomacyUI;
	}

	/**
	 * @param ui Diplomacy UI
	 */
	public final void setDiplomacyUI (final DiplomacyUI ui)
	{
		diplomacyUI = ui;
	}
}