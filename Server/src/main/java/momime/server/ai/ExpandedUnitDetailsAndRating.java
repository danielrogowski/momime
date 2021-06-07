package momime.server.ai;

import momime.common.utils.ExpandedUnitDetails;

/**
 * Stores a link to a unit together with its rating, so we can sort them
 */
final class ExpandedUnitDetailsAndRating implements Comparable<ExpandedUnitDetailsAndRating>
{
	/** Unit being positioned into combat */
	private final ExpandedUnitDetails unit;
	
	/** Estimate of how good this unit is */
	private final int rating;
	
	/**
	 * @param aUnit Unit being positioned into combat
	 * @param aRating Estimate of how good this unit is
	 */
	ExpandedUnitDetailsAndRating (final ExpandedUnitDetails aUnit, final int aRating)
	{
		unit = aUnit;
		rating = aRating;
	}

	/**
	 * @return Value to sort units by rating
	 */
	@Override
	public final int compareTo (final ExpandedUnitDetailsAndRating o)
	{
		return o.getRating () - getRating ();
	}

	/**
	 * @return Unit being positioned into combat
	 */
	public final ExpandedUnitDetails getUnit ()
	{
		return unit;
	}
	
	/**
	 * @return Estimate of how good this unit is
	 */
	public final int getRating ()
	{
		return rating;
	}
}