package momime.server.utils;

import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SwitchResearch;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.utils.SpellUtils;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Spell;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Server side methods dealing with researching and casting spells
 */
public final class SpellServerUtilsImpl implements ISpellServerUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (SpellServerUtilsImpl.class.getName ());
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/**
	 * @param player Player who wants to switch research
	 * @param spellID Spell that we want to research
	 * @param switchResearch Switch research option from session description
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choice isn't acceptable
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	@Override
	public final String validateResearch (final PlayerServerDetails player, final String spellID,
		final SwitchResearch switchResearch, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.entering (SpellServerUtilsImpl.class.getName (), "validateResearch", new String [] {player.getPlayerDescription ().getPlayerID ().toString (), spellID});

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Find the spell that we want to research
		final SpellResearchStatus spellWeWantToResearch = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spellID);

		// Find the spell that was previously being researched
		final Spell spellPreviouslyBeingResearched;
		final SpellResearchStatus spellPreviouslyBeingResearchedStatus;

		if (priv.getSpellIDBeingResearched () == null)
		{
			spellPreviouslyBeingResearched = null;
			spellPreviouslyBeingResearchedStatus = null;
		}
		else
		{
			spellPreviouslyBeingResearched = db.findSpell (priv.getSpellIDBeingResearched (), "validateResearch");
			spellPreviouslyBeingResearchedStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), priv.getSpellIDBeingResearched ());
		}

		// If we can't research it then its obviously disallowed regardless of the status of the previous research
		final String msg;
		if (spellWeWantToResearch.getStatus () != SpellResearchStatusID.RESEARCHABLE_NOW)
			msg = "The spell you've requested is currently not available for you to research.";

		// Picking research when we've got no current research, or switching to what we're already researching is always fine
		else if ((priv.getSpellIDBeingResearched () == null) || (priv.getSpellIDBeingResearched ().equals (spellID)))
			msg = null;

		// Check game option
		else if (switchResearch == SwitchResearch.DISALLOWED)
			msg = "You can't start researching a different spell until you've finished your current research.";

		else if ((spellPreviouslyBeingResearchedStatus.getRemainingResearchCost () < spellPreviouslyBeingResearched.getResearchCost ()) && (switchResearch == SwitchResearch.ONLY_IF_NOT_STARTED))
			msg = "You can't start researching a different spell until you've finished your current research.";

		else
			msg = null;

		log.exiting (SpellServerUtilsImpl.class.getName (), "validateResearch", msg);
		return msg;
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
