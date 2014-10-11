package momime.client.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

/**
 * Methods dealing with combat movement and unit lists, to keep this from making CombatUI too large and complicated.
 * Also many of these have equivalents in OverlandMapProcessingImpl so it made sense to keep the separation the same. 
 */
public interface CombatMapProcessing
{
	/**
	 * At the start of our combat turn, once all our movement has been reset, this gets called.
	 * It builds a list of units we need to give orders during this combat turn. 
	 * 
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void buildUnitsLeftToMoveList ()
		throws JAXBException, XMLStreamException;
}