package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
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
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public boolean processDispelling (final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer,
		final List<MemoryMaintainedSpell> targetSpells, final List<MemoryCombatAreaEffect> targetCAEs,
		final MapCoordinates3DEx targetWarpedNode, final List<MemoryUnit> targetVortexes, final MomSessionVariables mom)
		throws MomException, JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException;
	
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
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return Whether the spell was successfully cast or not; so false = was dispelled
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public boolean processCountering (final PlayerServerDetails castingPlayer, final Spell spell, final int unmodifiedCastingCost,
		final MapCoordinates3DEx combatLocation, final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer,
		final Spell triggerSpellDef, final Integer triggerSpellCasterPlayerID,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException;
}