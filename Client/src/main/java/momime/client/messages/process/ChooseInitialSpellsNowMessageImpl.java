package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.ui.NewGameUI;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowMessage;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to client to tell them how many spells of each rank they can choose for free at the start of the game
 */
public final class ChooseInitialSpellsNowMessageImpl extends ChooseInitialSpellsNowMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChooseInitialSpellsNowMessageImpl.class.getName ());

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
		log.entering (ChooseInitialSpellsNowMessageImpl.class.getName (), "process", new String []
			{getMagicRealmID (), new Integer (getSpellRank ().size ()).toString ()});

		getNewGameUI ().showInitialSpellsPanel (getMagicRealmID(), getSpellRank());
		
		log.exiting (ChooseInitialSpellsNowMessageImpl.class.getName (), "process");
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
