package momime.server.ai;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.NumberedHeroItem;
import momime.server.database.ServerDatabaseEx;

/**
 * Methods that the AI uses to calculate ratings about how good hero items are
 */
public interface AIHeroItemRatingCalculations
{
	/**
	 * @param item Hero item to evaluate
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for how good of a hero item this is
	 * @throws RecordNotFoundException If the item has a bonus property that we can't find in the database 
	 */
	public int calculateHeroItemRating (final NumberedHeroItem item, final ServerDatabaseEx db) throws RecordNotFoundException;
}