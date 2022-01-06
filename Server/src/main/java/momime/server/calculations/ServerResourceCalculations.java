package momime.server.calculations;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.server.MomSessionVariables;

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
	 * @param castingSkillRemainingThisCombat Only specified when this is called as a result of a combat spell being cast by the wizard, thereby reducing skill and mana
	 * @param spellCastThisCombatTurn True if castingSkillRemainingThisCombat is set because we cast a spell (it can also be set because of Mana Leak, so need false here)
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void sendGlobalProductionValues (final PlayerServerDetails player, final Integer castingSkillRemainingThisCombat, final boolean spellCastThisCombatTurn)
		throws JAXBException, XMLStreamException;

	/**
	 * Recalculates the amount of production of all types that we make each turn and sends the updated figures to the player(s)
	 *
	 * @param onlyOnePlayerID If zero will calculate values in cities for all players; if non-zero will calculate values only for the specified player
	 * @param duringStartPhase If true does additional work around enforcing that we are producing enough, and progresses city construction, spell research & casting and so on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a reply message into XML
	 * @throws XMLStreamException If there is a problem writing a reply message to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void recalculateGlobalProductionValues (final int onlyOnePlayerID, final boolean duringStartPhase, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}