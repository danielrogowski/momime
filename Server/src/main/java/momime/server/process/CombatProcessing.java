package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MoveResultsInAttackTypeID;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Routines dealing with initiating, progressing and ending combats
 */
public interface CombatProcessing
{
	/**
	 * Sets up a potential combat on the server.  If we're attacking an enemy unit stack or city, then all this does is calls StartCombat.
	 * If we're attacking a node/lair/tower, then this handles scouting the node/lair/tower, sending to the client the details
	 * of what monster we scouted, and StartCombat is only called when/if they click "Yes" they want to attack.
	 * This is declared separately so it can be used immediately from MoveUnitStack in one-at-a-time games, or from requesting scheduled combats in Simultaneous turns games.
	 *
	 * @param defendingLocation Location where defending units are standing
	 * @param attackingFrom Location where attacking units are standing (which will be a map tile adjacent to defendingLocation)
	 * @param scheduledCombatURN Scheduled combat URN, if simultaneous turns game; null for one-at-a-time games
	 * @param attackingPlayer Player who is attacking
	 * @param attackingUnitURNs Which of the attacker's unit stack are attacking - they might be leaving some behind
	 * @param typeOfCombat Whether we are scouting a node/lair/tower, or actually attacking something
	 * @param monsterUnitID If typeOfCombat=SCOUT, then this says the type of monster that the attacker can see; if typeOfCombat=something else this is null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void initiateCombat (final OverlandMapCoordinatesEx defendingLocation, final OverlandMapCoordinatesEx attackingFrom,
		final Integer scheduledCombatURN, final PlayerServerDetails attackingPlayer, final List<Integer> attackingUnitURNs,
		final MoveResultsInAttackTypeID typeOfCombat, final String monsterUnitID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * Sets up a combat on the server and any client(s) who are involved
	 *
	 * @param defendingLocation Location where defending units are standing
	 * @param attackingFrom Location where attacking units are standing (which will be a map tile adjacent to defendingLocation)
	 * @param scheduledCombatURN Scheduled combat URN, if simultaneous turns game; null for one-at-a-time games
	 * @param attackingPlayer Player who is attacking
	 * @param attackingUnitURNs Which of the attacker's unit stack are attacking - they might be leaving some behind
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void startCombat (final OverlandMapCoordinatesEx defendingLocation, final OverlandMapCoordinatesEx attackingFrom,
		final Integer scheduledCombatURN, final PlayerServerDetails attackingPlayer, final List<Integer> attackingUnitURNs, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
}
