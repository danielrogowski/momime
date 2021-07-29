package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MemoryUnit;
import momime.server.MomSessionVariables;

/**
 * Routines dealing with applying combat damage
 */
public interface DamageProcessor
{
	/**
	 * Performs one attack in combat, which may be a melee, ranged or spell attack.
	 * If a close combat attack, also resolves the defender retaliating.
	 * Also checks to see if the attack results in either side being wiped out, in which case ends the combat.
	 * 
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit
	 * @param defenders Unit(s) being hit; some attacks can hit multiple units such as Flame Strike
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param wreckTileChance Whether to roll an attack against the tile in addition to the defenders; null = no, any other value is the chance so 4 = 1/4 chance
	 * @param wreckTilePosition The position within the combat map of the tile that will be attacked
	 * @param attackerDirection The direction the attacker needs to turn to in order to be facing the defender; or null if the attack isn't coming from a unit
	 * @param attackSkillID The skill being used to attack, i.e. UA01 (swords) or UA02 (ranged); or null if the attack isn't coming from a unit
	 * @param spell The spell being cast; or null if the attack isn't coming from a spell
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt; or null if the attack isn't coming from a spell
	 * @param castingPlayer The player casting the spell; or null if the attack isn't coming from a spell
	 * @param combatLocation Where the combat is taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the attack resulted in the combat ending
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public boolean resolveAttack (final MemoryUnit attacker, final List<MemoryUnit> defenders,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final Integer wreckTileChance, final MapCoordinates2DEx wreckTilePosition,
		final Integer attackerDirection, final String attackSkillID,
		final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer, 
		final MapCoordinates3DEx combatLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;

	/**
	 * When we are trying to curse a unit, for example with Confusion or Black Sleep, handles making the resistance roll to see if they are affected or not
	 * 
	 * @param attacker Unit casting the spell; or null if wizard is casting
	 * @param defender Unit we are trying to curse
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param spell The spell being cast
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt
	 * @param castingPlayer The player casting the spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void makeResistanceRoll (final MemoryUnit attacker, final MemoryUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param combatLocation Location that combat is taking place
	 * @param combatSide Which side to count
	 * @param trueUnits List of true units
	 * @return How many units are still left alive in combat on the requested side
	 */
	public int countUnitsInCombat (final MapCoordinates3DEx combatLocation, final UnitCombatSideID combatSide,
		final List<MemoryUnit> trueUnits);
}