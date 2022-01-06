package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import jakarta.xml.bind.JAXBException;
import momime.server.MomSessionVariables;

/**
 * Interface that all world updates must implement so they know how to process themselves.
 * Additionally they must all implement .equals () to avoid duplicate entries being added into the list.
 */
interface WorldUpdate
{
	/**
	 * @return Enum indicating which kind of update this is
	 */
	public KindOfWorldUpdate getKindOfWorldUpdate ();
	
	/**
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this update was processed and/or generated any further updates
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 * @throws IOException If there is another kind of problem
	 */
	public WorldUpdateResult process (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}