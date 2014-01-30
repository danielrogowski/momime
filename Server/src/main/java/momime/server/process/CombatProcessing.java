package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.CombatMoveType;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.CaptureCityDecisionID;
import momime.common.messages.v0_9_4.MemoryUnit;
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

	/**
	 * Progresses the combat happening at the specified location.
	 * Will cycle through playing out AI players' combat turns until either the combat ends or it is a human players turn, at which
	 * point it will send a message to them to tell them to take their turn and then exit.
	 * 
	 * @param combatLocation Where the combat is taking place
	 * @param initialFirstTurn True if this is the initial call from startCombat; false if it is being called by a human playing ending their combat turn or turning auto on
	 * @param initialAutoControlHumanPlayer True if it is being called by a human player turning auto on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void progressCombat (final OverlandMapCoordinatesEx combatLocation, final boolean initialFirstTurn,
		final boolean initialAutoControlHumanPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * Handles tidying up when a combat ends
	 * 
	 * If the combat results in the attacker capturing a city, and the attacker is a human player, then this gets called twice - the first time it
	 * will spot that CaptureCityDecision = cdcUndecided, send a message to the player to ask them for the decision, and when we get an answer back, it'll be called again
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param winningPlayer Player who won
	 * @param captureCityDecision If taken a city and winner has decided whether to raze or capture it then is passed in here; null = player hasn't decided yet (see comment above)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void combatEnded (final OverlandMapCoordinatesEx combatLocation,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final PlayerServerDetails winningPlayer,
		final CaptureCityDecisionID captureCityDecision, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
	
	/**
	 * Once we have mapped out the directions and distances around the combat map and verified that our desired destination is
	 * fine, this routine actually handles sending the movement animations, performing the updates, and resolving any attacks.
	 *  
	 * This is separate so that it can be called directly by the AI.
	 *  
	 * @param tu The unit being moved
	 * @param moveTo The position within the combat map that the unit wants to move to (or attack)
	 * @param movementDirections The map of movement directions generated by calculateCombatMovementDistances
	 * @param movementTypes The map of movement types generated by calculateCombatMovementDistances
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void okToMoveUnitInCombat (final MemoryUnit tu, final CombatMapCoordinatesEx moveTo,
		final int [] [] movementDirections, final CombatMoveType [] [] movementTypes, final MomSessionVariables mom)
		throws MomException, PlayerNotFoundException, RecordNotFoundException, JAXBException, XMLStreamException;
}
