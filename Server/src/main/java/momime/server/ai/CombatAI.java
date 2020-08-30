package momime.server.ai;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * AI for deciding what to do with units in combat
 * This is used for human players who put their units on 'auto' as well as actual AI players
 */
public interface CombatAI
{
	/**
	 * AI plays out one round in combat, deciding which units to move in which order, and taking all actions.
	 * 
	 * This might be for node defenders, raiders, rampaging monsters, an AI controlled
	 * wizard, or a human controlled wizard who has put the combat on 'Auto'
	 * 
	 * @param combatLocation Where the combat is taking place 
	 * @param currentPlayer The player whose turn is being taken
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we had at least one unit take some useful action or not
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public CombatAIMovementResult aiCombatTurn (final MapCoordinates3DEx combatLocation, final PlayerServerDetails currentPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;
}
