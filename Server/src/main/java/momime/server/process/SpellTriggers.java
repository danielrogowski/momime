package momime.server.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.Spell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.server.MomSessionVariables;

/**
 * Methods mainly dealing with when very rare overland enchantments are triggered by a certain effect.
 * For some this is tied to another wizard casting as spell of a certain magic realm overland (e.g. casting Chaos spells);
 * some others just activate every turn.
 */
public interface SpellTriggers
{
	/**
	 * Handles an overland enchantment triggering its effect.
	 * 
	 * @param spell Overland enchantment that was triggered
	 * @param offendingPlayer Player who caused the trigger, if a player action triggers this effect; null if it just triggers automatically every turn
	 * @param offendingSpell The spell that triggered this overland enchantement effect; null if it just triggers automatically every turn
	 * @param offendingUnmodifiedCastingCost Unmodified mana cost of the spell that triggered this effect, including any extra MP for variable damage 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the spell was successfully cast or not; so false = was dispelled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public boolean triggerSpell (final MemoryMaintainedSpell spell,
		final PlayerServerDetails offendingPlayer, final Spell offendingSpell, final Integer offendingUnmodifiedCastingCost, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}