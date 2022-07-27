package momime.server.ai;

import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;

/**
 * Methods for AI making decisions about diplomacy with other wizards
 */
public final class DiplomacyAIImpl implements DiplomacyAI
{
	/**
	 * @param requestSpellID The spell the other wizard wants from us
	 * @param spellIDsWeCanOffer Spells we can request in return
	 * @param db Lookup lists built over the XML database
	 * @return Spell to request in return if there's one we like, or null if all of them would be a bad trade
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	@Override
	public final String chooseSpellToRequestInReturn (final String requestSpellID, final List<String> spellIDsWeCanOffer, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final int requestSpellResearchCost = db.findSpell (requestSpellID, "chooseSpellToRequestInReturn").getResearchCost ();
		
		// Don't request anything cheaper - assume the requester wants the spell they asked for badly enough that we aren't going to give it to them easily
		// but don't request anything ridiculously more expensive either, or they'll never agree to it
		final int minimumResearchCost = requestSpellResearchCost;
		final int maximumResearchCost = requestSpellResearchCost * 2;
		
		// Check each possible spell
		String bestSpellID = null;
		Integer bestResearchCost = null;
		
		for (final String spellID : spellIDsWeCanOffer)
		{
			final int thisResearchCost = db.findSpell (spellID, "chooseSpellToRequestInReturn").getResearchCost ();
			if ((thisResearchCost >= minimumResearchCost) && (thisResearchCost <= maximumResearchCost) &&
				((bestResearchCost == null) || (thisResearchCost > bestResearchCost)))
			{
				bestSpellID = spellID;
				bestResearchCost = thisResearchCost;
			}
		}
		
		return bestSpellID;
	}
}