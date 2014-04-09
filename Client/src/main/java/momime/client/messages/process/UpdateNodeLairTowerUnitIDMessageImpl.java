package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.common.messages.servertoclient.v0_9_5.UpdateNodeLairTowerUnitIDMessage;

/**
 * Server sends this as part of the main FOW message if we need to update our knowledge of what monsters are in
 * nodes/lairs/towers other than by scouting (initiating a combat). Or can send single message
 */
public final class UpdateNodeLairTowerUnitIDMessageImpl extends UpdateNodeLairTowerUnitIDMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (UpdateNodeLairTowerUnitIDMessageImpl.class.getName ());

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
		log.entering (UpdateNodeLairTowerUnitIDMessageImpl.class.getName (), "process", getData ().getNodeLairTowerLocation ());
		
		log.exiting (UpdateNodeLairTowerUnitIDMessageImpl.class.getName (), "process");

		throw new UnsupportedOperationException ("UpdateNodeLairTowerUnitIDMessageImpl");
	}
}
