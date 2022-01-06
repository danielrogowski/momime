package momime.server.ai;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.WeightedChoices;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;

/**
 * Methods relating to casting spells in combat
 */
public interface CombatSpellAI
{
	/**
	 * Checks whether casting the specified spell in combat is valid, e.g. does it have a valid target, and lists out all possible choices for casting it (if any).
	 * 
	 * @param player AI player who is considering casting a spell
	 * @param spell Spell they are considering casting
	 * @param combatLocation Combat location
	 * @param combatCastingUnit Unit who is going to cast the spell; null = the wizard
	 * @param combatCastingFixedSpellNumber For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null
	 * @param combatCastingSlotNumber For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param choices List of choices to add to
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	public void listChoicesForSpell (final PlayerServerDetails player, final Spell spell, final MapCoordinates3DEx combatLocation,
		final ExpandedUnitDetails combatCastingUnit, final Integer combatCastingFixedSpellNumber, final Integer combatCastingSlotNumber,
		final MomSessionVariables mom, final WeightedChoices<CombatAISpellChoice> choices)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;

	/**
	 * Given a choice of spells and targets the AI can choose from in combat, picks one and casts it
	 * 
	 * @param player AI player who is considering casting a spell
	 * @param combatLocation Combat location
	 * @param choices List of spells and targets to choose from
	 * @param combatCastingUnit Unit who is going to cast the spell; null = the wizard
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public CombatAIMovementResult makeCastingChoice (final PlayerServerDetails player, final MapCoordinates3DEx combatLocation,
		final WeightedChoices<CombatAISpellChoice> choices, final ExpandedUnitDetails combatCastingUnit, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}