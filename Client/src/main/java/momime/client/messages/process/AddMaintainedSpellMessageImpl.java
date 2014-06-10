package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.servertoclient.v0_9_5.AddMaintainedSpellMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to notify clients of new maintained spells cast, or those that have newly come into view
 */
public final class AddMaintainedSpellMessageImpl extends AddMaintainedSpellMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddMaintainedSpellMessageImpl.class);

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
		log.trace ("Entering process: " + getData ().getSpellID ());
		
		log.trace ("Exiting process");

		throw new UnsupportedOperationException ("AddMaintainedSpellMessageImpl");
	}
}