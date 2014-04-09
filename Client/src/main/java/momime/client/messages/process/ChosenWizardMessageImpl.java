package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.NewGameUI;
import momime.common.messages.servertoclient.v0_9_5.ChosenWizardMessage;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Message server sends to players to tell them which wizards players have chosen
 */
public final class ChosenWizardMessageImpl extends ChosenWizardMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChosenWizardMessageImpl.class.getName ());

	/** Multiplayer client */
	private MomClient client;
	
	/** New Game UI */
	private NewGameUI newGameUI;
	
	/**
	 * @param sender Connection to the server
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerServerConnection sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (ChosenWizardMessageImpl.class.getName (), "process", new String []
			{new Integer (getPlayerID ()).toString (), getWizardID ()});

		// Set the Wizard ID
		final PlayerPublicDetails player = MultiplayerSessionUtils.findPlayerWithID (getClient ().getPlayers (), getPlayerID (), "ChosenWizardMessageImpl");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		pub.setWizardID (getWizardID ());
		
		// If it is us, and we chose Custom, then we need to go to the Choose Portrait screen
		// If it is us and we picked a pre-defined Wizard then do nothing - the server will have already sent us either mmChooseInitialSpells or
		// mmChooseYourRaceNow to tell us what to do next before it sent this mmChosenWizard message
		if ((getPlayerID () == getClient ().getOurPlayerID ()) && (getWizardID () == null))
			getNewGameUI ().showPortraitPanel ();
			
		// Some screens need updating when we learn what wizard a player is using
		
		log.exiting (ChosenWizardMessageImpl.class.getName (), "process");
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
}
