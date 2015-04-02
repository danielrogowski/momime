package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomSessionDescription;
import momime.server.MomSessionVariables;
import momime.server.database.ServerDatabaseEx;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

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
	 * @param player Player who is casting the spell
	 * @param spellID Which spell they want to cast
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param combatTargetLocation Which specific tile of the combat map the spell is being cast at, for cell-targetted spells like combat summons
	 * @param combatTargetUnitURN Which specific unit within combat the spell is being cast at, for unit-targetted spells like Fire Bolt
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	public void requestCastSpell (final PlayerServerDetails player, final String spellID,
		final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatTargetLocation, final Integer combatTargetUnitURN,
		final Integer variableDamage, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException;

	/**
	 * Spends any skill/mana the player has left towards casting queued spells
	 *
	 * @param gsk Server knowledge structure
	 * @param player Player whose casting to progress
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return True if we cast at least one spell
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public boolean progressOverlandCasting (final MomGeneralServerKnowledgeEx gsk, final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}