package momime.server.ai;

import momime.common.messages.MemoryUnit;

/**
 * Caches details about an AI unit and its calculated ratings
 */
final class AIUnitAndRatings implements Comparable<AIUnitAndRatings>
{
	/** Unit we are caching ratings for */
	private final MemoryUnit unit;
	
	/** Type of unit the AI classifies this unit to be */
	private final AIUnitType aiUnitType;
	
	/** Current rating */
	private final int currentRating;
	
	/** Average rating */
	private final int averageRating;
	
	/**
	 * @param aUnit Unit we are caching ratings for
	 * @param anAiUnitType Type of unit the AI classifies this unit to be
	 * @param aCurrentRating Current rating
	 * @param anAverageRating Average rating
	 */
	AIUnitAndRatings (final MemoryUnit aUnit, final AIUnitType anAiUnitType, final int aCurrentRating, final int anAverageRating)
	{
		unit = aUnit;
		aiUnitType = anAiUnitType;
		currentRating = aCurrentRating;
		averageRating = anAverageRating;
	}
	
	/**
	 * @return Value to sort units by 'average rating'
	 */
	@Override
	public final int compareTo (final AIUnitAndRatings o)
	{
		return o.getAverageRating () - getAverageRating ();
	}

	/**
	 * @return String representation of values, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return "Unit URN " + getUnit ().getUnitURN () + ", which is a " + getUnit ().getUnitID () + " which is type " + getAiUnitType() + " is currently at " + getUnit ().getUnitLocation ();
	}
	
	/**
	 * @return Unit we are caching ratings for
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
	}
	
	/**
	 * @return Type of unit the AI classifies this unit to be
	 */
	public final AIUnitType getAiUnitType ()
	{
		return aiUnitType;
	}
	
	/**
	 * @return Current rating
	 */
	public final int getCurrentRating ()
	{
		return currentRating;
	}
	
	/**
	 * @return Average rating
	 */
	public final int getAverageRating ()
	{
		return averageRating;
	}
}