package momime.server.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;
import momime.server.MomSessionVariables;
import momime.server.database.SpellSvr;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Routines dealing with applying combat damage
 */
public interface DamageProcessor
{
	/**
	 * Performs one attack in combat.
	 * If a close combat attack, also resolves the defender retaliating.
	 * Also checks to see if the attack results in either side being wiped out, in which case ends the combat.
	 * 
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackerDirection The direction the attacker needs to turn to in order to be facing the defender; or null if the attack isn't coming from a unit
	 * @param attackAttributeID The attribute being used to attack, i.e. UA01 (swords) or UA02 (ranged); or null if the attack isn't coming from a unit
	 * @param spell The spell being cast; or null if the attack isn't coming from a spell
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt; or null if the attack isn't coming from a spell
	 * @param castingPlayer The player casting the spell; or null if the attack isn't coming from a spell
	 * @param combatLocation Where the combat is taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void resolveAttack (final MemoryUnit attacker, final MemoryUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final Integer attackerDirection, final String attackAttributeID,
		final SpellSvr spell, final Integer variableDamage, final PlayerServerDetails castingPlayer, 
		final MapCoordinates3DEx combatLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;
}