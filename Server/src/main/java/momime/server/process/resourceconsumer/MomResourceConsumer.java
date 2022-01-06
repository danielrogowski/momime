package momime.server.process.resourceconsumer;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.server.MomSessionVariables;

/**
 * Generic interface for all things in MOM that consume resources, i.e. units, buildings and maintained spells
 * This allows the server to cancel them via a common interface if a player has insufficient resources
 */
public interface MomResourceConsumer
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
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public void kill (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}
