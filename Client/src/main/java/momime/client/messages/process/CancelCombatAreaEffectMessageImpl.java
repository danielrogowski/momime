package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.common.messages.servertoclient.v0_9_4.CancelCombatAreaEffectMessage;

/**
 * Server sends this to notify clients of cancelled CAEs, or those that have gone out of view.
 */
public final class CancelCombatAreaEffectMessageImpl extends CancelCombatAreaEffectMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CancelCombatAreaEffectMessageImpl.class.getName ());

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
		log.entering (CancelCombatAreaEffectMessageImpl.class.getName (), "process", getData ().getCombatAreaEffectID ());
		
		log.exiting (CancelCombatAreaEffectMessageImpl.class.getName (), "process");

		throw new UnsupportedOperationException ("CancelCombatAreaEffectMessageImpl");
	}
}