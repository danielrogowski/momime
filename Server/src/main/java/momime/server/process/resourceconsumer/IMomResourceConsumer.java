package momime.server.process.resourceconsumer;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.database.ServerDatabaseLookup;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Generic interface for all things in MOM that consume resources, i.e. units, buildings and maintained spells
 * This allows the server to cancel them via a common interface if a player has insufficient resources
 */
public interface IMomResourceConsumer
{
	/**
	 * @return The player who's resources are being consumed
	 */
	public PlayerServerDetails getPlayer ();

	/**
	 * @return The type of resources being consumed
	 */
	public String getProductionTypeID ();

	/**
	 * @return The amount of production being consumed
	 */
	public int getConsumptionAmount ();

	/**
	 * Removes this unit/building/spell from the server, all clients who can see it, taking care of all
	 * knock on effects like giving the player gold for selling buildings, updating the visible area if they
	 * lose an Oracle, and so on
	 *
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void kill (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
}
