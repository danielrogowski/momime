package momime.server.ai;

import java.util.List;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.SpellResearchStatus;
import momime.server.database.ServerDatabaseEx;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Methods for AI players making decisions about spells
 */
public interface SpellAI
{
	/**
	 * @param player AI player who needs to choose what to research
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 * @throws MomException If there is an error in the logic
	 */
	public void decideWhatToResearch (final PlayerServerDetails player, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException;

	/**
	 * AI player at the start of the game chooses any spell of the specific magic realm & rank and researches it for free
	 * @param spells Pre-locked list of the player's spell
	 * @param magicRealmID Magic Realm (e.g. chaos) to pick a spell from
	 * @param spellRankID Spell rank (e.g. uncommon) to pick a spell of
	 * @param aiPlayerID Player ID, for debug message
	 * @param db Lookup lists built over the XML database
	 * @return Spell AI chose to learn for free
	 * @throws MomException If no eligible spells are available (e.g. player has them all researched already)
	 * @throws RecordNotFoundException If the spell chosen couldn't be found in the player's spell list
	 */
	public SpellResearchStatus chooseFreeSpellAI (final List<SpellResearchStatus> spells, final String magicRealmID, final String spellRankID,
		final int aiPlayerID, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException;
}