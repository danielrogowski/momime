package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.ui.frames.NewGameUI;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.servertoclient.ChosenWizardMessage;
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
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Set the Wizard ID
		final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getPlayerID (), "ChosenWizardMessageImpl");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		pub.setWizardID (getWizardID ());
		
		// If it is us, and we chose Custom, then we need to go to the Choose Portrait screen
		// If it is us and we picked a pre-defined Wizard then do nothing - the server will have already sent us either mmChooseInitialSpells or
		// mmChooseYourRaceNow to tell us what to do next before it sent this mmChosenWizard message
		if ((getPlayerID () == getClient ().getOurPlayerID ()) && (PlayerKnowledgeUtils.isCustomWizard (getWizardID ())))
			getNewGameUI ().showPortraitPanel ();
		
		// Show chosen wizard on wait for players list, or enable/disable selection buttons, depending what stage of game setup we're at
		getNewGameUI ().enableOrDisableWizardButtons ();
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
}