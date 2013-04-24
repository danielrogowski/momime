package momime.server.utils;

import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SwitchResearch;
import momime.server.database.ServerDatabaseEx;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Server side methods dealing with researching and casting spells
 */
public interface ISpellServerUtils
{
	/**
	 * @param player Player who wants to switch research
	 * @param spellID Spell that we want to research
	 * @param switchResearch Switch research option from session description
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choice isn't acceptable
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	public String validateResearch (final PlayerServerDetails player, final String spellID,
		final SwitchResearch switchResearch, final ServerDatabaseEx db) throws RecordNotFoundException;
}
