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
import momime.common.messages.servertoclient.YourFlagColourIsOkMessage;
import momime.common.utils.KnownWizardUtils;

/**
 * Message server sends to a player to let them know their choice of flag colour was OK, so proceed to the next stage of game setup.
 */
public final class YourFlagColourIsOkMessageImpl extends YourFlagColourIsOkMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
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
		// Record it
		final KnownWizardDetails ourWizard = getKnownWizardUtils ().findKnownWizardDetails (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (),
			getClient ().getOurPlayerID (), "YourFlagColourIsOkMessageImpl");
		ourWizard.setCustomFlagColour (getFlagColour ());
		
		// Record flag colour
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "YourFlagColourIsOkMessageImpl");
		final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) ourPlayer.getTransientPlayerPublicKnowledge ();
		trans.setFlagColour (getFlagColour ());
		
		// Go to next stage of setup
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