package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.ui.frames.NewGameUI;
import momime.common.messages.servertoclient.ChooseYourRaceNowMessage;

/**
 * Message server sends to a player when they've finished picking free spells at the start of the game, since the
 * client can't predict how many of what spell ranks and magic realms they need to pick
 */
public final class ChooseYourRaceNowMessageImpl extends ChooseYourRaceNowMessage implements BaseServerToClientMessage
{
	/** New Game UI */
	private NewGameUI newGameUI;

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		getNewGameUI ().showRacePanel ();
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