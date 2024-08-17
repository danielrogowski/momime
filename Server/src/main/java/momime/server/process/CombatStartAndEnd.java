package momime.server.process;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.PendingMovement;
import momime.server.MomSessionVariables;
import momime.server.knowledge.CombatDetails;

/**
 * Routines dealing with starting and ending combats
 */
public interface CombatStartAndEnd
{
	/**
	 * Sets up a combat on the server and any client(s) who are involved
	 *
	 * @param defendingLocation Location where defending units are standing
	 * @param attackingFrom Location where attacking units are standing (which will be a map tile adjacent to defendingLocation)
	 * @param attackingUnitURNs Which of the attacker's unit stack are attacking - they might be leaving some behind; mandatory
	 * @param defendingUnitURNs Which of the defender's unit stack are defending - used for simultaneous turns games; optional, null = all units in defendingLocation
	 * @param attackerPendingMovement In simultaneous turns games, the PendingMovement the attacker made which caused the combat currently taking place at this location
	 * @param defenderPendingMovement In simultaneous turns games, the PendingMovement the defender made which caused the combat currently taking place at this location (border conflicts/counterattacks only)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void startCombat (final MapCoordinates3DEx defendingLocation, final MapCoordinates3DEx attackingFrom,
		final List<Integer> attackingUnitURNs, final List<Integer> defendingUnitURNs,
		final PendingMovement attackerPendingMovement, final PendingMovement defenderPendingMovement, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * Handles tidying up when a combat ends
	 * 
	 * If the combat results in the attacker capturing a city, and the attacker is a human player, then this gets called twice - the first time it
	 * will spot that CaptureCityDecision = cdcUndecided, send a message to the player to ask them for the decision, and when we get an answer back, it'll be called again
	 * 
	 * @param combatDetails Details about the combat taking place
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - there should be no situations anymore where this can be passed in as null
	 * @param winningPlayer Player who won
	 * @param captureCityDecision If taken a city and winner has decided whether to raze or capture it then is passed in here; null = player hasn't decided yet (see comment above)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param awardFame Whether or not to award fame.  False for lame wins like the defender making the combat time out after 50 turns.
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void combatEnded (final CombatDetails combatDetails,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final PlayerServerDetails winningPlayer,
		final CaptureCityDecisionID captureCityDecision, final MomSessionVariables mom, final boolean awardFame)
		throws JAXBException, XMLStreamException, IOException;
}