package momime.server.ai;

import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;

/**
 * Methods for AI making decisions about diplomacy with other wizards
 */
public interface DiplomacyAI
{
	/**
	 * @param requestSpellID The spell the other wizard wants from us
	 * @param spellIDsWeCanOffer Spells we can request in return
	 * @param db Lookup lists built over the XML database
	 * @return Spell to request in return if there's one we like, or null if all of them would be a bad trade
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	public String chooseSpellToRequestInReturn (final String requestSpellID, final List<String> spellIDsWeCanOffer, final CommonDatabase db)
		throws RecordNotFoundException;
}