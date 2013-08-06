package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Combat scheduler is only used in Simultaneous turns games, where all the combats are queued up and processed in one go at the end of the turn.
 * The combat scheduled handles requests from clients about which order they wish to run combats in, and takes care of things like
 * when player A is busy in one combat, notifying all other clients that they cannot request a combat against player A until that one finishes.   
 */
public interface CombatScheduler
{
	/**
	 * Updates whether a player is involved in a simultaneous turns combat or not, on both the server & all clients
	 * @param player Player who is now busy or not
	 * @param players List of players in session
	 * @param currentlyPlayingCombat Whether player is now busy or not
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	public void informClientsOfPlayerBusyInCombat (final PlayerServerDetails player, final List<PlayerServerDetails> players, final boolean currentlyPlayingCombat)
		throws JAXBException, XMLStreamException;
}
