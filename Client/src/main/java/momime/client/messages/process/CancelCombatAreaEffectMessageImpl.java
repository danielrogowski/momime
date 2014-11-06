package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.servertoclient.CancelCombatAreaEffectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to notify clients of cancelled CAEs, or those that have gone out of view.
 */
public final class CancelCombatAreaEffectMessageImpl extends CancelCombatAreaEffectMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (CancelCombatAreaEffectMessageImpl.class);

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: CAE URN " + getCombatAreaEffectURN ());
		
		log.trace ("Exiting start");

		throw new UnsupportedOperationException ("CancelCombatAreaEffectMessageImpl");
	}
}