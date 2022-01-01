package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.ui.frames.NewGameUI;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.servertoclient.ChosenWizardMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Message server sends to players to tell them which wizards players have chosen
 */
public final class ChosenWizardMessageImpl extends ChosenWizardMessage implements BaseServerToClientMessage
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
		// Set the Wizard ID
		final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(getClient ().getOurPersistentPlayerPrivateKnowledge ().getKnownWizardDetails (), getPlayerID (), "ChosenWizardMessageImpl");
		
		wizardDetails.setWizardID (getWizardID ());
		
		// If it is us, and we chose Custom, then we need to go to the Choose Portrait screen
		// If it is us and we picked a pre-defined Wizard then do nothing - the server will have already sent us either mmChooseInitialSpells or
		// mmChooseYourRaceNow to tell us what to do next before it sent this mmChosenWizard message
		if ((getPlayerID () == getClient ().getOurPlayerID ()) && (getPlayerKnowledgeUtils ().isCustomWizard (getWizardID ())))
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