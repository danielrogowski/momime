package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MomScheduledCombat;
import momime.server.MomSessionVariables;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Combat scheduler is only used in Simultaneous turns games, where all the combats are queued up and processed in one go at the end of the turn.
 * The combat scheduled handles requests from clients about which order they wish to run combats in, and takes care of things like
 * when player A is busy in one combat, notifying all other clients that they cannot request a combat against player A until that one finishes.   
 */
public interface CombatScheduler
{
	/**
	 * Records details of a combat that needs to be added to the combat scheduler, allocating it a new scheduledCombatURN in the process
	 * 
	 * @param gsk Server knowledge structure to add the combat to
	 * @param defendingLocation Location being attacked
	 * @param attackingFrom Attacking from where
	 * @param defendingPlayer Player being attacked; may be null if attacking an empty node/lair/tower
	 * @param attackingPlayer Player owning the attacking units
	 * @param attackingUnitURNs Which specific units are attacking (may not be everybody standing at in attackingFrom)
	 */
	public void addScheduledCombatGeneratedURN (final MomGeneralServerKnowledge gsk,
		final MapCoordinates3DEx defendingLocation, final MapCoordinates3DEx attackingFrom,
		final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer, final List<Integer> attackingUnitURNs);
	
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
	
	/**
	 * Sends to all human players details of the scheduled combats that they are and aren't involved in 
	 * 
	 * @param players List of players in session 
	 * @param combats List of all scheduled combats in session
	 * @param updateOthersCountOnly true will send to each player only an update to the number of combats they *aren't* involved in, with no details of the combats they *are* involved in
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void sendScheduledCombats (final List<PlayerServerDetails> players,
		final List<MomScheduledCombat> combats, final boolean updateOthersCountOnly)
		throws JAXBException, XMLStreamException;

	/**
	 * Handles tidying up after a scheduled combat
	 * 
	 * @param scheduledCombatURN The scheduled combat that ended
	 * @param winningPlayer Player who won; if they scouted a node/lair/tower but clicked No to not attack those Sky Drakes, this will be null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void processEndOfScheduledCombat (final int scheduledCombatURN, final PlayerServerDetails winningPlayer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
}