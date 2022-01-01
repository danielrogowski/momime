package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.ui.frames.NewGameUI;
import momime.common.MomException;
import momime.common.messages.servertoclient.MeetWizardMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Message server sends when we meet a wizard for the first time.
 * We get sent our own wizard record when we pick which wizard we want to be.  We get sent the raiders/monsters records when the game starts.
 * Other wizards' records we are only sent when we learn who they are.
 */
public final class MeetWizardMessageImpl extends MeetWizardMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** New Game UI */
	private NewGameUI newGameUI;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// We should never receive this twice for the same player
		if (getKnownWizardUtils ().findKnownWizardDetails (getClient ().getOurPersistentPlayerPrivateKnowledge ().getKnownWizardDetails (), getKnownWizardDetails ().getPlayerID ()) != null)
			throw new MomException ("Server sent us KnownWizardDetails for player ID " + getKnownWizardDetails ().getPlayerID () + " when we already had them");
		
		// Add it
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getKnownWizardDetails ().add (getKnownWizardDetails ());
		
		// If it is us, and we chose Custom, then we need to go to the Choose Portrait screen
		// If it is us and we picked a pre-defined Wizard then do nothing - the server will have already sent us either mmChooseInitialSpells or
		// mmChooseYourRaceNow to tell us what to do next before it sent this mmChosenWizard message
		if ((getKnownWizardDetails ().getPlayerID () == getClient ().getOurPlayerID ()) && (getPlayerKnowledgeUtils ().isCustomWizard (getKnownWizardDetails ().getWizardID ())))
			getNewGameUI ().showPortraitPanel ();
		
		// Show chosen wizard on wait for players list
		getNewGameUI ().updateWaitPanelPlayersList ();
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
}