package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.QueuedSpellsUI;
import momime.common.messages.servertoclient.RemoveQueuedSpellMessage;

/**
 * Server sends this if a spell has finished casting to tell the client to remove it from the queue
 */
public final class RemoveQueuedSpellMessageImpl extends RemoveQueuedSpellMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Queued spells UI */
	private QueuedSpellsUI queuedSpellsUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpell ().remove (getQueuedSpellIndex ());
		getQueuedSpellsUI ().updateQueuedSpells ();
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
	 * @return Queued spells UI
	 */
	public final QueuedSpellsUI getQueuedSpellsUI ()
	{
		return queuedSpellsUI;
	}

	/**
	 * @param ui Queued spells UI
	 */
	public final void setQueuedSpellsUI (final QueuedSpellsUI ui)
	{
		queuedSpellsUI = ui;
	}
}