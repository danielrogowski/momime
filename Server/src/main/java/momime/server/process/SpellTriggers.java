package momime.server.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
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
	 * @param offendingPlayerID Player who caused the trigger, if a player action triggers this effect; null if it just triggers automatically every turn
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void triggerSpell (final MemoryMaintainedSpell spell, final Integer offendingPlayerID, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}