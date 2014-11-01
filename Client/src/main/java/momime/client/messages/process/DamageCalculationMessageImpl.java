package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.servertoclient.DamageCalculationMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server telling the two players involved in a combat how damage was calculated.
 * Either attackSkillID or attackAttributeID will be filled in, but not both:
 *		attackSkillID will be filled in if the attack is is a special skill like First Strike, Fire Breath etc.
 *		attackAttributeID will be filled in (UA01 or UA02) if the attack is a standard melee or ranged attack.
 */
public final class DamageCalculationMessageImpl extends DamageCalculationMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageCalculationMessageImpl.class);

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");
		
		// Just ignore this for now until calculator button implemented on combat screen
		
		log.trace ("Exiting start");
	}
}