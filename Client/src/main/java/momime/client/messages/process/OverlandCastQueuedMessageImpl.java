package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.common.messages.servertoclient.v0_9_5.OverlandCastQueuedMessage;

/**
 * Server sends this to players trying to cast overland spells that are too big to cast instantly
 */
public final class OverlandCastQueuedMessageImpl extends OverlandCastQueuedMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (StartGameProgressMessageImpl.class);

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
		log.trace ("Entering start: " + getSpellID ());
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpellID ().add (getSpellID ());
		
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