package momime.server.process;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.servertoclient.v0_9_4.PlayerCombatRequestStatusMessage;
import momime.common.messages.v0_9_4.MomTransientPlayerPublicKnowledge;

import com.ndg.multiplayer.server.MultiplayerServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Combat scheduler is only used in Simultaneous turns games, where all the combats are queued up and processed in one go at the end of the turn.
 * The combat scheduled handles requests from clients about which order they wish to run combats in, and takes care of things like
 * when player A is busy in one combat, notifying all other clients that they cannot request a combat against player A until that one finishes.   
 */
public final class CombatSchedulerImpl implements CombatScheduler
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CombatSchedulerImpl.class.getName ());
	
	/** Server-side multiplayer utils */
	private MultiplayerServerUtils multiplayerServerUtils;
	
	/**
	 * Updates whether a player is involved in a simultaneous turns combat or not, on both the server & all clients
	 * @param player Player who is now busy or not
	 * @param players List of players in session
	 * @param currentlyPlayingCombat Whether player is now busy or not
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void informClientsOfPlayerBusyInCombat (final PlayerServerDetails player, final List<PlayerServerDetails> players, final boolean currentlyPlayingCombat)
		throws JAXBException, XMLStreamException
	{
		log.entering (CombatSchedulerImpl.class.getName (), "informClientsOfPlayerBusyInCombat",
			new String [] {player.getPlayerDescription ().getPlayerID ().toString (), new Boolean (currentlyPlayingCombat).toString ()});
		
		// Update on server
		// Note if we're entering combat, then we no longer have a pending request and so clear out any requested combat ID
		// If we ending combat, then we also don't have a pending request and so clear out any requested combat ID
		final MomTransientPlayerPublicKnowledge pub = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
		pub.setCurrentlyPlayingCombat (currentlyPlayingCombat);
		pub.setScheduledCombatUrnRequested (null);
		
		// Update on clients
		final PlayerCombatRequestStatusMessage msg = new PlayerCombatRequestStatusMessage ();
		msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
		msg.setCurrentlyPlayingCombat (currentlyPlayingCombat);
		getMultiplayerServerUtils ().sendMessageToAllClients (players, msg);

		log.exiting (CombatSchedulerImpl.class.getName (), "informClientsOfPlayerBusyInCombat");
	}

	/**
	 * @return Server-side multiplayer utils
	 */
	public final MultiplayerServerUtils getMultiplayerServerUtils ()
	{
		return multiplayerServerUtils;
	}
	
	/**
	 * @param utils Server-side multiplayer utils
	 */
	public final void setMultiplayerServerUtils (final MultiplayerServerUtils utils)
	{
		multiplayerServerUtils = utils;
	}
}
