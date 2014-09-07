package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.servertoclient.v0_9_5.UpdateNodeLairTowerUnitIDMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this as part of the main FOW message if we need to update our knowledge of what monsters are in
 * nodes/lairs/towers other than by scouting (initiating a combat). Or can send single message
 */
public final class UpdateNodeLairTowerUnitIDMessageImpl extends UpdateNodeLairTowerUnitIDMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UpdateNodeLairTowerUnitIDMessageImpl.class);

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getData ().getNodeLairTowerLocation ());
		
		log.trace ("Exiting start");

		throw new UnsupportedOperationException ("UpdateNodeLairTowerUnitIDMessageImpl");
	}
}