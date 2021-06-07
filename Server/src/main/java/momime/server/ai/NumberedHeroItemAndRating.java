package momime.server.ai;

import momime.common.messages.NumberedHeroItem;

/**
 * Stores a link to a hero item together with its rating, so we can sort them
 */
final class NumberedHeroItemAndRating implements Comparable<NumberedHeroItemAndRating>
{
	/** Item being considered */
	private final NumberedHeroItem item;
	
	/** Estimate of how good this unit is */
	private final int rating;
	
	/**
	 * @param anItem Item being considered
	 * @param aRating Estimate of how good this unit is
	 */
	NumberedHeroItemAndRating (final NumberedHeroItem anItem, final int aRating)
	{
		item = anItem;
		rating = aRating;
	}

	/**
	 * @return Value to sort units by rating
	 */
	@Override
	public final int compareTo (final NumberedHeroItemAndRating o)
	{
		return o.getRating () - getRating ();
	}

	/**
	 * @return Item being considered
	 */
	public final NumberedHeroItem getItem ()
	{
		return item;
	}
	
	/**
	 * @return Estimate of how good this unit is
	 */
	public final int getRating ()
	{
		return rating;
	}
}