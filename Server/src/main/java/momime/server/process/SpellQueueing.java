package momime.server.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.database.HeroItem;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Methods for validating spell requests and deciding whether to queue them up or cast immediately.
 * Once they're actually ready to cast, this is handled by the SpellProcessing interface.  I split these up so that the unit
 * tests dealing with validating and queueing don't have to invoke the real castOverlandNow/castCombatNow methods.
 */
public interface SpellQueueing
{
	/**
	 * Client wants to cast a spell, either overland or in combat
	 * We may not actually be able to cast it yet - big overland spells take a number of turns to channel, so this
	 * routine does all the checks to see if it can be instantly cast or needs to be queued up and cast over multiple turns
	 * 
	 * @param castingPlayer Player who is casting the spell
	 * @param combatCastingUnitURN Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param combatCastingFixedSpellNumber For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null
	 * @param combatCastingSlotNumber For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null
	 * @param spellID Which spell they want to cast
	 * @param heroItem The item being created; null for spells other than Enchant Item or Create Artifact
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param combatTargetLocation Which specific tile of the combat map the spell is being cast at, for cell-targetted spells like combat summons
	 * @param combatTargetUnitURN Which specific unit within combat the spell is being cast at, for unit-targetted spells like Fire Bolt
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
 	 * @return Whether the spell cast was a combat spell that was an attack that resulted in the combat ending
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public boolean requestCastSpell (final PlayerServerDetails castingPlayer, final Integer combatCastingUnitURN, final Integer combatCastingFixedSpellNumber,
		final Integer combatCastingSlotNumber, final String spellID, final HeroItem heroItem,
		final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatTargetLocation, final Integer combatTargetUnitURN,
		final Integer variableDamage, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * Adds a spell to a player's overland casting queue.  This assumes we've already been through all the validation to make sure they're allowed to cast it,
	 * and to make sure they can't cast it instantly.
	 * 
	 * @param castingPlayer Player casting the spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param spellID Which spell they want to cast
	 * @param heroItem If create item/artifact, the details of the item to create
	 * @param variableDamage Chosen damage selected for the spell, for spells like disenchant area where a varying amount of mana can be channeled into the spell
	 * @throws RecordNotFoundException If we can't find the wizard we are meeting
	 * @throws PlayerNotFoundException If we can't find the player we are meeting
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void queueSpell (final PlayerServerDetails castingPlayer, final MomSessionVariables mom, final String spellID, final HeroItem heroItem,
		final Integer variableDamage)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * Spends any skill/mana the player has left towards casting queued spells
	 *
	 * @param player Player whose casting to progress
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if we cast at least one spell
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public boolean progressOverlandCasting (final PlayerServerDetails player, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}