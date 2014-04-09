package momime.server.calculations;

import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.PlayerPick;
import momime.common.messages.v0_9_5.SpellResearchStatus;
import momime.server.database.ServerDatabaseEx;

/**
 * Calculations pertaining to spells that are only used on the server
 */
public interface MomServerSpellCalculations
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
		final ServerDatabaseEx db)
		throws RecordNotFoundException;

	/**
	 * Picks the 8 spells we can choose from to research next
	 *
	 * @param spells List of spells to update
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public void randomizeSpellsResearchableNow (final List<SpellResearchStatus> spells, final ServerDatabaseEx db)
		throws RecordNotFoundException;
}
