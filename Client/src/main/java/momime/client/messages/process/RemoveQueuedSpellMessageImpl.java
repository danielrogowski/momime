package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.common.messages.servertoclient.v0_9_5.RemoveQueuedSpellMessage;

/**
 * Server sends this if a spell has finished casting to tell the client to remove it from the queue
 */
public final class RemoveQueuedSpellMessageImpl extends RemoveQueuedSpellMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (RemoveQueuedSpellMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getQueuedSpellIndex ());
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpellID ().remove (getQueuedSpellIndex ());
		
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
}