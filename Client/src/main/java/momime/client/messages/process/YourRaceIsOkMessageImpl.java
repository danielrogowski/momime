package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.client.ui.NewGameUI;
import momime.common.messages.servertoclient.v0_9_5.YourRaceIsOkMessage;

/**
 * Message server sends to a player to let them know their choice of race was OK,
 * and so to move to the 'waiting for other players to join' screen
 */
public final class YourRaceIsOkMessageImpl extends YourRaceIsOkMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (YourRaceIsOkMessageImpl.class.getName ());

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
		log.entering (YourRaceIsOkMessageImpl.class.getName (), "process");

		getNewGameUI ().showWaitPanel ();
		
		log.exiting (YourRaceIsOkMessageImpl.class.getName (), "process");
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
