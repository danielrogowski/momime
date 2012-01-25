package momime.server.utils;

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
	 * @return True if both strings are null, or both are non-null and identical
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
	 * Prevent instatiation of this class
	 */
	private CompareUtils ()
	{
	}
}
