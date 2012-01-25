package momime.server.utils;

/**
 * Methods for dealing with Roman Numerals
 */
public final class RomanNumerals
{
	/**
	 * @param value Value to convert to roman numerals
	 * @param one Roman numeral letter to use for 1
	 * @param five Roman numeral letter to use for 5
	 * @param ten Roman numeral letter to use for 10
	 * @return Roman numerals representation of v
	 */
	private static final String digitToRoman (final int value, final String one, final String five, final String ten)
	{
		switch (value)
		{
			case 1: return one;
			case 2: return one + one;
			case 3: return one + one + one;
			case 4: return one + five;
			case 5: return five;
			case 6: return five + one;
			case 7: return five + one + one;
			case 8: return five + one + one + one;
			case 9: return one + ten;
			default: return "";
		}
	}

	/**
	 * @param value Value to convert to roman numerals
	 * @return Roman numeral representation of value
	 */
	public static final String intToRoman (final int value)
	{
		// Units
		String result = digitToRoman (value % 10, "I", "V", "X");
		int remainingValue = value / 10;

		// Tens
		result = digitToRoman (remainingValue % 10, "X", "L", "C") + result;
		remainingValue = remainingValue / 10;

		// Hundereds
		result = digitToRoman (remainingValue % 10, "C", "D", "M") + result;
		remainingValue = remainingValue / 10;

		// Thousands
		while (remainingValue > 0)
		{
			result = "M" + result;
			remainingValue--;
		}

		return result;
	}

	/**
	 * Prevent instantiation
	 */
	private RomanNumerals ()
	{
	}
}
