package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.common.messages.servertoclient.v0_9_4.AddUnitMessage;

/**
 * Server sends this to clients to tell them about a new unit added to the map, or can add them in bulk as part of fogOfWarVisibleAreaChanged.
 * 
 * If readSkillsFromXML is true, client will read unit skills from the XML database (otherwise the client, receiving a message with zero skills, cannot tell if it is a hero who
 * genuinely has no skills (?) or is expected to read in the skills from the XML database).
 * 
 * If skills are included, the Experience value is not used so is omitted, since the Experience value will be included in the skill list.
 * 
 * Bulk adds (fogOfWarVisibleAreaChanged) can contain a mixture of units with and without skill lists included.
 */
public final class AddUnitMessageImpl extends AddUnitMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (AddUnitMessageImpl.class.getName ());

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
		log.entering (AddUnitMessageImpl.class.getName (), "process", getData ().getUnitID ());
		
		log.exiting (AddUnitMessageImpl.class.getName (), "process");

		throw new UnsupportedOperationException ("AddUnitMessageImpl");
	}
}
