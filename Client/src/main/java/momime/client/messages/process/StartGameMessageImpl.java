package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.client.ui.NewGameUI;
import momime.common.messages.servertoclient.v0_9_5.StartGameMessage;

/**
 * Message server broadcasts when all game setup is complete and its time for clients to actually switch to the map screen
 */
public final class StartGameMessageImpl extends StartGameMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (StartGameMessageImpl.class.getName ());

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
		log.entering (StartGameMessageImpl.class.getName (), "process");

		getNewGameUI ().setVisible (false);
		
		log.exiting (StartGameMessageImpl.class.getName (), "process");
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
