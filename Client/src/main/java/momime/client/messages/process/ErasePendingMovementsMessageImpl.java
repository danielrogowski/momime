package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.common.messages.servertoclient.ErasePendingMovementsMessage;

/**
 * Server sends to clients to tell them to wipe out their pending movement store, before new pending movements are about to be sent
 */
public final class ErasePendingMovementsMessageImpl extends ErasePendingMovementsMessage implements BaseServerToClientMessage
{
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
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getPendingMovement ().clear ();
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