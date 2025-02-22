package momime.server.process;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;
import momime.server.knowledge.CombatDetails;

/**
 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left)
 */
public interface CombatEndTurn
{
	/**
	 * Makes any rolls necessary at the start of a combat turn.  Note what this means by "combat turn" is different than what combatEndTurn below means.
	 * Here we mean before EITHER player has taken their turn, i.e. immediately before the defender gets a turn.
	 * 
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param combatDetails Details about the combat taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void combatBeforeEitherTurn (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final CombatDetails combatDetails,
		final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * Deals with any processing at the start of one player's turn in combat, before their movement is initialized.
	 * 
	 * @param combatDetails Details about the combat taking place
	 * @param playerID Which player is about to have their combat turn
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of units frozen in terror who will not get any movement allocation this turn; will return null (rather than an empty list) if the combat ends
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public List<Integer> startCombatTurn (final CombatDetails combatDetails, final int playerID,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left) 
	 * 
	 * @param combatDetails Details about the combat taking place
	 * @param playerID Which player just finished their combat turn
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If an expected data item cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void combatEndTurn (final CombatDetails combatDetails, final int playerID, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}