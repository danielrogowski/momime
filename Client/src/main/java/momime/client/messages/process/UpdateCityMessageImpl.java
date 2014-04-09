package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.common.messages.servertoclient.v0_9_5.UpdateCityMessage;

/**
 * Server sends this to the client to tell them the map scenery
y */
public final class UpdateCityMessageImpl extends UpdateCityMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (UpdateCityMessageImpl.class.getName ());

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
		log.entering (UpdateCityMessageImpl.class.getName (), "process", getData ().getMapLocation ());
		
		log.exiting (UpdateCityMessageImpl.class.getName (), "process");

		throw new UnsupportedOperationException ("UpdateCityMessageImpl");
	}
}
