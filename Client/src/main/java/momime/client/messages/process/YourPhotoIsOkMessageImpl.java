package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.ui.frames.NewGameUI;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.servertoclient.YourPhotoIsOkMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Message server sends to a player to let them know their choice of photo was OK (regardless of whether it was a
 * standard wizard portrait or custom .ndgbmp), so proceed to the next stage of game setup.
 */
public final class YourPhotoIsOkMessageImpl extends YourPhotoIsOkMessage implements BaseServerToClientMessage
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
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Record it
		final KnownWizardDetails ourWizard = getKnownWizardUtils ().findKnownWizardDetails (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (),
			getClient ().getOurPlayerID (), "YourPhotoIsOkMessageImpl");
		ourWizard.setStandardPhotoID (getStandardPhotoID ());
		ourWizard.setCustomPhoto (getCustomPhoto ());
		
		// Record flag colour
		if (getStandardPhotoID () != null)
		{
			final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "YourPhotoIsOkMessageImpl");
			final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) ourPlayer.getTransientPlayerPublicKnowledge ();
			trans.setFlagColour (getClient ().getClientDB ().findWizard (getStandardPhotoID (), "YourPhotoIsOkMessageImpl").getFlagColour ());
		}		
		
		// Standard portraits have fixed colours, only if we chose a custom portrait do we need to go to the flag colour screen
		if (getPlayerKnowledgeUtils ().isCustomWizard (getNewGameUI ().getPortraitChosen ()))
			getNewGameUI ().showCustomFlagColourPanel ();
		else
			getNewGameUI ().showCustomPicksPanel ();
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
}