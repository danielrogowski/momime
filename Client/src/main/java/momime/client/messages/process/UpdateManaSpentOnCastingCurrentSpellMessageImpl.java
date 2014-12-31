package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.QueuedSpellsUI;
import momime.common.messages.servertoclient.UpdateManaSpentOnCastingCurrentSpellMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to client to let them know progress on casting big spells
 */
public final class UpdateManaSpentOnCastingCurrentSpellMessageImpl extends UpdateManaSpentOnCastingCurrentSpellMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UpdateManaSpentOnCastingCurrentSpellMessageImpl.class);

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
		log.trace ("Entering start: " + getManaSpentOnCastingCurrentSpell ());
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().setManaSpentOnCastingCurrentSpell (getManaSpentOnCastingCurrentSpell ());
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