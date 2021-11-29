package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

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
	 * @throws IOException If there was a problem
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 */
	public WorldUpdateResult process (final MomSessionVariables mom) throws IOException, JAXBException, XMLStreamException;
}