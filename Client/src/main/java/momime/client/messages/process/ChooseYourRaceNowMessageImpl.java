package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.client.ui.NewGameUI;
import momime.common.messages.servertoclient.v0_9_5.ChooseYourRaceNowMessage;

/**
 * Message server sends to a player when they've finished picking free spells at the start of the game, since the
 * client can't predict how many of what spell ranks and magic realms they need to pick
 */
public final class ChooseYourRaceNowMessageImpl extends ChooseYourRaceNowMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChooseYourRaceNowMessageImpl.class.getName ());

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
		log.entering (ChooseYourRaceNowMessageImpl.class.getName (), "process");

		getNewGameUI ().showRacePanel ();
		
		log.exiting (ChooseYourRaceNowMessageImpl.class.getName (), "process");
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
