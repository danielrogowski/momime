package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.common.messages.servertoclient.v0_9_4.DestroyBuildingMessage;

/**
 * Server sends this to clients to tell them about a building destroyed (or sold) from a city
 */
public final class DestroyBuildingMessageImpl extends DestroyBuildingMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (DestroyBuildingMessageImpl.class.getName ());

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
		log.entering (DestroyBuildingMessageImpl.class.getName (), "process", new String []
			{getData ().getCityLocation ().toString (), getData ().getBuildingID ()});
		
		log.exiting (DestroyBuildingMessageImpl.class.getName (), "process");

		throw new UnsupportedOperationException ("DestroyBuildingMessageImpl");
	}
}
