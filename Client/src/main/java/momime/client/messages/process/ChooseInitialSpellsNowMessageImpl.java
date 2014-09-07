package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.ui.frames.NewGameUI;
import momime.common.messages.servertoclient.v0_9_5.ChooseInitialSpellsNowMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to client to tell them how many spells of each rank they can choose for free at the start of the game
 */
public final class ChooseInitialSpellsNowMessageImpl extends ChooseInitialSpellsNowMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChooseInitialSpellsNowMessageImpl.class);

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
		log.trace ("Entering start: " + getMagicRealmID () + ", " + getSpellRank ().size ());

		getNewGameUI ().showInitialSpellsPanel (getMagicRealmID(), getSpellRank());
		
		log.trace ("Exiting start");
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