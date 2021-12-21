package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.newturnmessages.NewTurnMessageProcessing;
import momime.client.newturnmessages.NewTurnMessageStatus;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.common.messages.servertoclient.AddNewTurnMessagesMessage;

/**
 * Server sends this if additional messages are generated during a turn
 * (e.g. casting an overland enchantment instantly, or capturing or losing a node)
 */
public final class AddNewTurnMessagesMessageImpl extends AddNewTurnMessagesMessage implements BaseServerToClientMessage
{
	/** New turn messages helper methods */
	private NewTurnMessageProcessing newTurnMessageProcessing;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Read in the new turn messages
		if (isExpireMessages ())
			getNewTurnMessageProcessing ().expireMessages ();
		
		getNewTurnMessageProcessing ().readNewTurnMessagesFromServer (getMessage (), NewTurnMessageStatus.AFTER_OUR_TURN_BEGAN);
		getNewTurnMessagesUI ().setNewTurnMessages (getNewTurnMessageProcessing ().sortAndAddCategories ());
		
		// Only show the form if we got new messages - doesn't matter if there's some old ones we've already seen
		if (getMessage ().size () > 0)
			getNewTurnMessagesUI ().setVisible (true);
	}

	/**
	 * @return New turn messages helper methods
	 */
	public final NewTurnMessageProcessing getNewTurnMessageProcessing ()
	{
		return newTurnMessageProcessing;
	}

	/**
	 * @param proc New turn messages helper methods
	 */
	public final void setNewTurnMessageProcessing (final NewTurnMessageProcessing proc)
	{
		newTurnMessageProcessing = proc;
	}

	/**
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
	}
}