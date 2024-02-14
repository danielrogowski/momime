package momime.server.calculations;

import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;

/**
 * Calculations pertaining to spells that are only used on the server
 */
public interface ServerSpellCalculations
{
	/**
	 * For all the spells that we did NOT get for free at the start of the game, decides whether or not they are in our spell book to be available to be researched
	 *
	 * This also marks which spells are 'not in spell book', i.e. spells which aren't in our spell book but are still of a rank so we can get the spell via other means (trading, banishing
	 * or treasure from nodes/lairs/towers), e.g. 3 books gives you 1 very rare spell in your book (researchable) but the other 9 will be 'not in spell book' (they are not 'unavailable')
	 *
	 * @param spells List of spells to update
	 * @param picks List of the picks that we gaining spells from
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a pick or other item that we can't find in the cache
	 */
	public void randomizeResearchableSpells (final List<SpellResearchStatus> spells, final List<PlayerPick> picks,
		final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Picks the 8 spells we can choose from to research next
	 *
	 * @param spells List of spells to update
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public void randomizeSpellsResearchableNow (final List<SpellResearchStatus> spells, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * @param tradeableSpells Spells known by the player who is giving away a spell
	 * @param knownSpells Spells known by the player who is being given a spell
	 * @return List of tradeable spells
	 */
	public List<String> listTradeableSpells (final List<SpellResearchStatus> tradeableSpells, final List<SpellResearchStatus> knownSpells);
	
	/**
	 * @param spellIDs List of candidate spells
	 * @param count Maximum number of spells to return
	 * @param db Lookup lists built over the XML database
	 * @return List of (count) cheapest spells from the list
	 * @throws RecordNotFoundException If there's a spell in the list that can't be found in the database
	 */
	public List<String> findCheapestSpells (final List<String> spellIDs, final int count, final CommonDatabase db)
		throws RecordNotFoundException;
}