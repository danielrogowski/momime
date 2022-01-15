package momime.server.process;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.server.MomSessionVariables;

/**
 * Handles dispelling spells making rolls to dispel other spells
 */
public interface SpellDispelling
{
	/**
	 * Makes dispel rolls against a list of target spells and CAEs
	 * 
	 * @param spell Dispel spell being cast
	 * @param variableDamage Chosen damage selected for the spell - in this case its dispelling power
	 * @param castingPlayer Player who is casting the dispel spell
	 * @param targetSpells Target spells that we will make rolls to try to dispel
	 * @param targetCAEs Target CAEs that we will make rolls to try to dispel, can be left null
	 * @param targetWarpedNode Warped node that we are trying to return to normal
	 * @param targetVortexes Vortexes are odd in that the unit as a whole gets dispelled (killed) rather than a spell cast on the unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether dispelling any spells resulted in the death of any units
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public boolean processDispelling (final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer,
		final List<MemoryMaintainedSpell> targetSpells, final List<MemoryCombatAreaEffect> targetCAEs,
		final MapCoordinates3DEx targetWarpedNode, final List<MemoryUnit> targetVortexes, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * Makes dispel rolls that try to block a spell from ever being cast in combat in the first place (nodes and Counter Magic)
	 * 
	 * @param castingPlayer Player who is trying to cast a spell
	 * @param spell The spell they are trying to cast
	 * @param unmodifiedCastingCost Unmodified mana cost of the spell they are trying to cast, including any extra MP for variable damage 
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param defendingPlayer Defending player in the combat
	 * @param attackingPlayer Attacking player in the combat
	 * @param triggerSpellDef Additional spell that's trying to counter the spell from being cast
	 * @param triggerSpellCasterPlayerID Player who cast the additional spell that's trying to counter the spell from being cast
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the spell was successfully cast or not; so false = was dispelled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public boolean processCountering (final PlayerServerDetails castingPlayer, final Spell spell, final int unmodifiedCastingCost,
		final MapCoordinates3DEx combatLocation, final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer,
		final Spell triggerSpellDef, final Integer triggerSpellCasterPlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * @param player AI player who is casting Disenchant Area
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Best location for AI to target an overland Disenchant Area spell
	 * @throws RecordNotFoundException If an expected data item cannot be found
	 */
	public MapCoordinates3DEx chooseDisenchantAreaTarget (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException;
}