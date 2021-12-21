package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.ui.frames.NewGameUI;
import momime.common.messages.servertoclient.YourPhotoIsOkMessage;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Message server sends to a player to let them know their choice of photo was OK (regardless of whether it was a
 * standard wizard portrait or custom .ndgbmp), so proceed to the next stage of game setup.
 */
public final class YourPhotoIsOkMessageImpl extends YourPhotoIsOkMessage implements BaseServerToClientMessage
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
		// Standard portraits have fixed colours, only if we chose a custom portrait do we need to go to the flag colour screen
		if (PlayerKnowledgeUtils.isCustomWizard (getNewGameUI ().getPortraitChosen ()))
			getNewGameUI ().showCustomFlagColourPanel ();
		else
			getNewGameUI ().showCustomPicksPanel ();
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