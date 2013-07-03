package momime.server.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.utils.SpellUtils;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Spell;
import momime.server.utils.RandomUtils;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Methods for AI players making decisions about spells
 */
public final class SpellAI implements ISpellAI
{
	/** Class logger */
	private final Logger log = Logger.getLogger (SpellAI.class.getName ());
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/**
	 * Common routine between picking free spells at the start of the game and picking the next spell to research - it picks a spell from the supplied list
	 * @param spells List of possible spells to choose from
	 * @param aiPlayerName Player name, for debug message
	 * @return ID of chosen spell to research
	 * @throws MomException If the list was empty
	 */
	final Spell chooseSpellToResearchAI (final List<Spell> spells, final String aiPlayerName)
		throws MomException
	{
		log.entering (SpellAI.class.getName (), "chooseSpellToResearchAI", aiPlayerName);

		String debugLogMessage = null;

		// Check each spell in the list to find the the best research order, 1 being the best, 9 being the worst, and make a list of spells with this research order
		int bestResearchOrder = Integer.MAX_VALUE;
		final List<Spell> spellsWithBestResearchOrder = new ArrayList<Spell> ();

		for (final Spell spell : spells)
		{
			if (spell.getAiResearchOrder () != null)
			{
				// List possible choices in debug message
				if (debugLogMessage == null)
					debugLogMessage = spell.getSpellName () + " (" + spell.getAiResearchOrder () + ")";
				else
					debugLogMessage = debugLogMessage + ", " + spell.getSpellName () + " (" + spell.getAiResearchOrder () + ")";

				if (spell.getAiResearchOrder () < bestResearchOrder)
				{
					bestResearchOrder = spell.getAiResearchOrder ();
					spellsWithBestResearchOrder.clear ();
				}

				if (spell.getAiResearchOrder () == bestResearchOrder)
					spellsWithBestResearchOrder.add (spell);
			}
		}

		// Check we found one (this error should only happen if the list was totally empty)
		if ((bestResearchOrder == Integer.MAX_VALUE) || (spellsWithBestResearchOrder.size () == 0))
			throw new MomException ("chooseSpellToResearchAI: No appropriate spells to pick from list of " + spells.size ());

		// Pick one at random
		final Spell chosenSpell = spellsWithBestResearchOrder.get (RandomUtils.getGenerator ().nextInt (spellsWithBestResearchOrder.size ()));

		log.exiting (SpellAI.class.getName (), "chooseSpellToResearchAI", chosenSpell.getSpellID ());
		return chosenSpell;
	}

	/**
	 * @param player AI player who needs to choose what to research
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 * @throws MomException If there is an error in the logic
	 */
	@Override
	public final void decideWhatToResearch (final PlayerServerDetails player, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException
	{
		log.entering (SpellAI.class.getName (), "decideWhatToResearch", player.getPlayerDescription ().getPlayerName ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		final List<momime.common.database.v0_9_4.Spell> researchableSpells = getSpellUtils ().getSpellsForStatus
			(priv.getSpellResearchStatus (), SpellResearchStatusID.RESEARCHABLE_NOW, db);

		if (researchableSpells.size () >= 0)
		{
			final List<Spell> researchableServerSpells = new ArrayList<Spell> ();
			for (final momime.common.database.v0_9_4.Spell spell : researchableSpells)
				researchableServerSpells.add ((Spell) spell);

			final Spell chosenSpell = chooseSpellToResearchAI (researchableServerSpells, player.getPlayerDescription ().getPlayerName ());
			priv.setSpellIDBeingResearched (chosenSpell.getSpellID ());
		}

		log.exiting (SpellAI.class.getName (), "decideWhatToResearch", priv.getSpellIDBeingResearched ());
	}

	/**
	 * AI player at the start of the game chooses any spell of the specific magic realm & rank and researches it for free
	 * @param spells Pre-locked list of the player's spell
	 * @param magicRealmID Magic Realm (e.g. chaos) to pick a spell from
	 * @param spellRankID Spell rank (e.g. uncommon) to pick a spell of
	 * @param aiPlayerName Player name, for debug message
	 * @param db Lookup lists built over the XML database
	 * @return Spell AI chose to learn for free
	 * @throws MomException If no eligible spells are available (e.g. player has them all researched already)
	 * @throws RecordNotFoundException If the spell chosen couldn't be found in the player's spell list
	 */
	@Override
	public final SpellResearchStatus chooseFreeSpellAI (final List<SpellResearchStatus> spells, final String magicRealmID, final String spellRankID,
		final String aiPlayerName, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException
	{
		log.entering (SpellAI.class.getName (), "chooseFreeSpellAI", new String [] {aiPlayerName, magicRealmID, spellRankID});

		// Get candidate spells
		final List<momime.common.database.v0_9_4.Spell> commonSpellList = getSpellUtils ().getSpellsNotInBookForRealmAndRank (spells, magicRealmID, spellRankID, db);
		final List<Spell> spellList = new ArrayList<Spell> ();
		for (final momime.common.database.v0_9_4.Spell thisSpell : commonSpellList)
			spellList.add ((Spell) thisSpell);

		// Choose a spell
		final Spell chosenSpell = chooseSpellToResearchAI (spellList, aiPlayerName);

		// Return spell research status; calling routine sets it to available
		final SpellResearchStatus chosenSpellStatus = getSpellUtils ().findSpellResearchStatus (spells, chosenSpell.getSpellID ());

		log.exiting (SpellAI.class.getName (), "chooseFreeSpellAI", chosenSpellStatus.getSpellID ());
		return chosenSpellStatus;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}
}
