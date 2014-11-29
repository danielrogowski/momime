package momime.server.calculations;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server side methods for dealing with calculating and updating the global economy
 * e.g. gold being produced, cities growing, buildings progressing construction, spells being researched and so on
 */
public interface ServerResourceCalculations
{
	/**
	 * Sends one player's global production values to them
	 *
	 * Note Delphi version could either send the values to one player or all players
	 * Java version operates only on one player because each player now has their own resource list
	 *
	 * @param player Player whose values to send
	 * @param castingSkillRemainingThisCombat Only specified when this is called as a result of a combat spell being cast, thereby reducing skill and mana
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void sendGlobalProductionValues (final PlayerServerDetails player, final Integer castingSkillRemainingThisCombat)
		throws JAXBException, XMLStreamException;

	/**
	 * Recalculates the amount of production of all types that we make each turn and sends the updated figures to the player(s)
	 *
	 * @param onlyOnePlayerID If zero will calculate values in cities for all players; if non-zero will calculate values only for the specified player
	 * @param duringStartPhase If true does additional work around enforcing that we are producing enough, and progresses city construction, spell research & casting and so on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 * @throws JAXBException If there is a problem converting a reply message into XML
	 * @throws XMLStreamException If there is a problem writing a reply message to the XML stream
	 */
	public void recalculateGlobalProductionValues (final int onlyOnePlayerID, final boolean duringStartPhase,
		final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}