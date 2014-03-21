package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.common.messages.servertoclient.v0_9_4.KillUnitMessage;

/**
 * Server sends this to everyone to notify of dead units, except where it is already obvious from an Apply Damage message that a unit is dead
 */
public final class KillUnitMessageImpl extends KillUnitMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (KillUnitMessageImpl.class.getName ());

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
		log.entering (KillUnitMessageImpl.class.getName (), "process", getData ().getUnitURN ());
		
		log.exiting (KillUnitMessageImpl.class.getName (), "process");

		throw new UnsupportedOperationException ("KillUnitMessageImpl");
	}
}
