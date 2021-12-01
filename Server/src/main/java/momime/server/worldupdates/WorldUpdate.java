package momime.server.worldupdates;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
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
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	public WorldUpdateResult process (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
}