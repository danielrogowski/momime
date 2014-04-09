package momime.common.utils;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Equivalents of my StringUtils unit in Delphi
 */
public final class CompareUtils
{
	/**
	 * Safe string comparison that works even with either string is null
	 * @param first First string to compare
	 * @param second Second string to compare
	 * @return True if both strings are null, or both are non-null and identical
	 */
	public final static boolean safeStringCompare (final String first, final String second)
	{
		if ((first == null) && (second == null))
			return true;

		else if ((first == null) || (second == null))
			return false;

		else
			return first.equals (second);
	}

	/**
	 * Safe int comparison that works even with either int is null
	 * @param first First value to compare
	 * @param second Second value to compare
	 * @return True if both ints are null, or both are non-null and identical
	 */
	public final static boolean safeIntegerCompare (final Integer first, final Integer second)
	{
		if ((first == null) && (second == null))
			return true;

		else if ((first == null) || (second == null))
			return false;

		else
			return first.equals (second);
	}

	/**
	 * Safe coords comparison that works even with either coords are null
	 * @param first First value to compare
	 * @param second Second value to compare
	 * @return True if both coords are null, or both are non-null and identical
	 */
	public final static boolean safeOverlandMapCoordinatesCompare (final MapCoordinates3DEx first, final MapCoordinates3DEx second)
	{
		if ((first == null) && (second == null))
			return true;

		else if ((first == null) || (second == null))
			return false;

		else
			return first.equals (second);
	}
	
	/**
	 * Safe coords comparison that works even with either coords are null
	 * @param first First value to compare
	 * @param second Second value to compare
	 * @return True if both coords are null, or both are non-null and identical
	 */
	public final static boolean safeCombatMapCoordinatesCompare (final MapCoordinates2DEx first, final MapCoordinates2DEx second)
	{
		if ((first == null) && (second == null))
			return true;

		else if ((first == null) || (second == null))
			return false;

		else
			return first.equals (second);
	}
	
	/**
	 * Prevent instatiation of this class
	 */
	private CompareUtils ()
	{
	}
}
