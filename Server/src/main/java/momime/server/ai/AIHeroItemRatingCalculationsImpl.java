package momime.server.ai;

import momime.common.database.CommonDatabase;
import momime.common.database.HeroItemBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.NumberedHeroItem;

/**
 * Methods that the AI uses to calculate ratings about how good hero items are
 */
public final class AIHeroItemRatingCalculationsImpl implements AIHeroItemRatingCalculations
{
	/**
	 * @param bonus Hero item bonus to evaluate
	 * @return Value AI estimates for how good of a hero item bonus this is 
	 */
	final int calculateHeroItemBonusRating (final HeroItemBonus bonus)
	{
		final int rating;
		
		if (bonus.getHeroItemBonusStat ().isEmpty ())
			rating = 2;
		else
			rating = bonus.getHeroItemBonusStat ().stream ().mapToInt (s -> (s.getUnitSkillValue () == null) ? 2 : s.getUnitSkillValue ()).sum ();
		
		return rating;
	}

	/**
	 * @param item Hero item to evaluate
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for how good of a hero item this is
	 * @throws RecordNotFoundException If the item has a bonus property that we can't find in the database 
	 */
	@Override
	public final int calculateHeroItemRating (final NumberedHeroItem item, final CommonDatabase db) throws RecordNotFoundException
	{
		int rating = 0;
		for (final String bonusID : item.getHeroItemChosenBonus ())
			rating = rating + calculateHeroItemBonusRating (db.findHeroItemBonus (bonusID, "calculateUnitPotentialRating"));
		
		return rating;
	}
}