package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.QueuedSpellsUI;
import momime.common.messages.QueuedSpell;
import momime.common.messages.servertoclient.OverlandCastQueuedMessage;

/**
 * Server sends this to players trying to cast overland spells that are too big to cast instantly
 */
public final class OverlandCastQueuedMessageImpl extends OverlandCastQueuedMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (OverlandCastQueuedMessageImpl.class);

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
		log.trace ("Entering start: " + getSpellID ());
		
		final QueuedSpell queued = new QueuedSpell ();
		queued.setQueuedSpellID (getSpellID ());
		queued.setHeroItem (getHeroItem ());
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpell ().add (queued);
		getQueuedSpellsUI ().updateQueuedSpells ();
		
		log.trace ("Exiting start");
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