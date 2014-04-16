package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.common.messages.servertoclient.v0_9_5.ErasePendingMovementsMessage;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends to clients to tell them to wipe out their pending movement store, before new pending movements are about to be sent
 */
public final class ErasePendingMovementsMessageImpl extends ErasePendingMovementsMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ErasePendingMovementsMessageImpl.class.getName ());

	/** Multiplayer client */
	private MomClient client;
	
	/**
	 * @param sender Connection to the server
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerServerConnection sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (ErasePendingMovementsMessageImpl.class.getName (), "process");
		
		getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement ().clear ();
		
		log.exiting (ErasePendingMovementsMessageImpl.class.getName (), "process");
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
