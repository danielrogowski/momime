package momime.server.ai;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.NumberedHeroItem;
import momime.server.database.HeroItemBonusSvr;
import momime.server.database.ServerDatabaseEx;

/**
 * Methods that the AI uses to calculate ratings about how good hero items are
 */
public final class AIHeroItemRatingCalculationsImpl implements AIHeroItemRatingCalculations
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (AIHeroItemRatingCalculationsImpl.class);
	
	/**
	 * @param bonus Hero item bonus to evaluate
	 * @return Value AI estimates for how good of a hero item bonus this is 
	 */
	final int calculateHeroItemBonusRating (final HeroItemBonusSvr bonus)
	{
		log.trace ("Entering calculateHeroItemBonusRating: " + bonus.getHeroItemBonusID ());

		final int rating;
		
		if (bonus.getHeroItemBonusStat ().isEmpty ())
			rating = 2;
		else
			rating = bonus.getHeroItemBonusStat ().stream ().mapToInt (s -> (s.getUnitSkillValue () == null) ? 2 : s.getUnitSkillValue ()).sum ();
		
		log.trace ("Exiting calculateHeroItemBonusRating = " + rating);
		return rating;
	}

	/**
	 * @param item Hero item to evaluate
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for how good of a hero item this is
	 * @throws RecordNotFoundException If the item has a bonus property that we can't find in the database 
	 */
	@Override
	public final int calculateHeroItemRating (final NumberedHeroItem item, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering calculateHeroItemRating: Item URN " + item.getHeroItemURN () + ", name " + item.getHeroItemName ());

		int rating = 0;
		for (final HeroItemTypeAllowedBonus bonus : item.getHeroItemChosenBonus ())
			rating = rating + calculateHeroItemBonusRating (db.findHeroItemBonus (bonus.getHeroItemBonusID (), "calculateUnitPotentialRating"));
		
		log.trace ("Exiting calculateHeroItemRating = " + rating);
		return rating;
	}
}