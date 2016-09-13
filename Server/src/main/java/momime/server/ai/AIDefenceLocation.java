package momime.server.ai;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Stores details about a map location the AI is wanting to capture or defend, and the unit strength that it is short by to do so 
 */
final class AIDefenceLocation implements Comparable<AIDefenceLocation>
{
	/** The map location we are wanting to defend */
	private final MapCoordinates3DEx mapLocation;
	
	/** The defence deficit at this location */
	private int averageRating;

	/**
	 * @param aMapLocation The map location we are wanting to defend
	 * @param anAverageRating The defence deficit at this location
	 */
	AIDefenceLocation (final MapCoordinates3DEx aMapLocation, final int anAverageRating)
	{
		mapLocation = aMapLocation;
		averageRating = anAverageRating;
	}	

	/**
	 * @return Value to sort units by 'average rating'
	 */
	@Override
	public final int compareTo (final AIDefenceLocation o)
	{
		return o.getAverageRating () - getAverageRating ();
	}
	
	/**
	 * @return String representation of values, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return getMapLocation () + " by " + getAverageRating ();
	}
	
	/**
	 * @return The map location we are wanting to defend
	 */
	public final MapCoordinates3DEx getMapLocation ()
	{
		return mapLocation;
	}
	
	/**
	 * @return The defence deficit at this location
	 */
	public final int getAverageRating ()
	{
		return averageRating;
	}

	/**
	 * @param rating The defence deficit at this location
	 */
	public final void setAverageRating (final int rating)
	{
		averageRating = rating;
	}
}