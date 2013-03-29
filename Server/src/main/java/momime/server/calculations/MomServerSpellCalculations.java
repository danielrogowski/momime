package momime.server.calculations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Spell;
import momime.common.messages.SpellUtils;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Pick;
import momime.server.database.v0_9_4.PickType;
import momime.server.database.v0_9_4.PickTypeCountContainer;
import momime.server.database.v0_9_4.PickTypeGrantsSpells;
import momime.server.utils.RandomUtils;

/**
 * Calculations pertaining to spells that are only used on the server
 */
public final class MomServerSpellCalculations
{
	/** The number of choices we're offered from which to pick a spell to research */
	private static final int SPELL_COUNT_TO_PICK_RESEARCH_FROM = 8;

	/**
	 * For all the spells that we did NOT get for free at the start of the game, decides whether or not they are in our spell book to be available to be researched
	 *
	 * This also marks which spells are 'not in spell book', i.e. spells which aren't in our spell book but are still of a rank so we can get the spell via other means (trading, banishing
	 * or treasure from nodes/lairs/towers), e.g. 3 books gives you 1 very rare spell in your book (researchable) but the other 9 will be 'not in spell book' (they are not 'unavailable')
	 *
	 * @param spells List of spells to update
	 * @param picks List of the picks that we gaining spells from
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If we encounter a pick or other item that we can't find in the cache
	 */
	public static final void randomizeResearchableSpells (final List<SpellResearchStatus> spells, final List<PlayerPick> picks,
		final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (MomServerSpellCalculations.class.getName (), "randomizeResearchableSpells");

		final List<String> spellIdsToMakeResearchable = new ArrayList<String> ();
		final List<String> spellIdsToMakeNotInBook = new ArrayList<String> ();

		for (final PlayerPick thisPick : picks)
		{
			final Pick pickRecord = db.findPick (thisPick.getPickID (), "randomizeResearchableSpells");
			final PickType pickTypeRecord = db.findPickType (pickRecord.getPickType (), "randomizeResearchableSpells");

			// Look for an entry for the quantity of this pick that we have - failing to find this is fine,
			// it just means that this number of this pick type doesn't give us anything e.g. its a retort, not a number of books
			PickTypeCountContainer spellCounts = null;
			final Iterator<PickTypeCountContainer> iter = pickTypeRecord.getPickTypeCount ().iterator ();
			while ((spellCounts == null) && (iter.hasNext ()))
			{
				final PickTypeCountContainer thisSpellCounts = iter.next ();
				if (thisSpellCounts.getCount () == thisPick.getQuantity ())
					spellCounts = thisSpellCounts;
			}

			if (spellCounts != null)
				for (final PickTypeGrantsSpells thisRank : spellCounts.getSpellCount ())
				{
					// Take off how many spells of this realm/rank we already know, or are already in our spell book waiting to be researched
					// This should allow this routine to be used again when we gain a new spell book
					int availableCount = thisRank.getSpellsAvailable () -
						SpellUtils.getSpellsForRealmRankStatus (spells, thisPick.getPickID (), thisRank.getSpellRank (), SpellResearchStatusID.AVAILABLE, db, debugLogger).size () -
						SpellUtils.getSpellsForRealmRankStatus (spells, thisPick.getPickID (), thisRank.getSpellRank (), SpellResearchStatusID.RESEARCHABLE_NOW, db, debugLogger).size () -
						SpellUtils.getSpellsForRealmRankStatus (spells, thisPick.getPickID (), thisRank.getSpellRank (), SpellResearchStatusID.RESEARCHABLE, db, debugLogger).size ();

					if (availableCount > 0)
					{
						// List all the spells of this realm/rank that aren't yet in our spell book
						final List<Spell> spellsToChooseFrom = SpellUtils.getSpellsNotInBookForRealmAndRank (spells, thisPick.getPickID (), thisRank.getSpellRank (), db, debugLogger);

						// Randomly pick some to make researchable
						while ((availableCount > 0) && (spellsToChooseFrom.size () > 0))
						{
							final Spell chosenSpell = spellsToChooseFrom.get (RandomUtils.getGenerator ().nextInt (spellsToChooseFrom.size ()));

							spellIdsToMakeResearchable.add (chosenSpell.getSpellID ());
							spellsToChooseFrom.remove (chosenSpell);
							availableCount--;
						}

						// Mark all the rest as gettable-by-other-means
						for (final Spell thisSpell : spellsToChooseFrom)
							spellIdsToMakeNotInBook.add (thisSpell.getSpellID ());
					}
				}
		}

		// Update spell statuses
		if ((spellIdsToMakeResearchable.size () > 0) || (spellIdsToMakeNotInBook.size () > 0))
			for (final SpellResearchStatus researchStatus : spells)
			{
				if (spellIdsToMakeResearchable.contains (researchStatus.getSpellID ()))
					researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE);

				else if (spellIdsToMakeNotInBook.contains (researchStatus.getSpellID ()))
					researchStatus.setStatus (SpellResearchStatusID.NOT_IN_SPELL_BOOK);
			}

		debugLogger.exiting (MomServerSpellCalculations.class.getName (), "randomizeResearchableSpells");
	}

	/**
	 * Picks the 8 spells we can choose from to research next
	 *
	 * @param spells List of spells to update
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public static final void randomizeSpellsResearchableNow (final List<SpellResearchStatus> spells, final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (MomServerSpellCalculations.class.getName (), "randomizeSpellsResearchableNow");

		// First find how many 'researchable now' spells we already have - maybe we already have 8 and have nothing to do
		int researchableNow = SpellUtils.getSpellsForStatus (spells, SpellResearchStatusID.RESEARCHABLE_NOW, db, debugLogger).size ();
		if (researchableNow < SPELL_COUNT_TO_PICK_RESEARCH_FROM)
		{
			final List<String> spellIdsToMakeResearchableNow = new ArrayList<String> ();

			// Get a list of the spell ranks for which we have spells we can research, and sort it
			// This is because we must research (or put onto the research screen) all common spells before moving up to uncommon, etc
			final List<String> spellRanks = SpellUtils.getSpellRanksForStatus (spells, SpellResearchStatusID.RESEARCHABLE, db, debugLogger);
			Collections.sort (spellRanks);

			// Keep going until all are chosen
			while ((researchableNow < SPELL_COUNT_TO_PICK_RESEARCH_FROM) && (spellRanks.size () > 0))
			{
				// Check spells of this rank
				final List<Spell> spellChoices = SpellUtils.getSpellsForRankAndStatus (spells, spellRanks.get (0), SpellResearchStatusID.RESEARCHABLE, db, debugLogger);
				while ((researchableNow < SPELL_COUNT_TO_PICK_RESEARCH_FROM) && (spellChoices.size () > 0))
				{
					// Pick a random spell
					final Spell chosenSpell = spellChoices.get (RandomUtils.getGenerator ().nextInt (spellChoices.size ()));

					spellIdsToMakeResearchableNow.add (chosenSpell.getSpellID ());
					spellChoices.remove (chosenSpell);
					researchableNow++;
				}

				// Finished this spell rank (doesn't matter if we didn't actually use up all this rank because we reached 8 'researchable now' spells, in that case the loop will exit anyway)
				spellRanks.remove (0);
			}

			// Update spell statuses
			if (spellIdsToMakeResearchableNow.size () > 0)
				for (final SpellResearchStatus researchStatus : spells)
					if (spellIdsToMakeResearchableNow.contains (researchStatus.getSpellID ()))
						researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		}

		debugLogger.exiting (MomServerSpellCalculations.class.getName (), "randomizeSpellsResearchableNow");
	}

	/**
	 * Prevent instantiation
	 */
	private MomServerSpellCalculations ()
	{
	}
}
