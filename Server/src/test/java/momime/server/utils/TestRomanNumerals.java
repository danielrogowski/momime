package momime.server.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for the RomanNumerals class
 */
public final class TestRomanNumerals
{
	/**
	 * Tests the intToRoman method
	 */
	@Test
	public final void testIntToRoman ()
	{
		// Got these examples from http://mathforum.org/dr.math/faq/faq.roman.html
		assertEquals ("30", "XXX", RomanNumerals.intToRoman (30));
		assertEquals ("61", "LXI", RomanNumerals.intToRoman (61));
		assertEquals ("4", "IV", RomanNumerals.intToRoman (4));
		assertEquals ("982", "CMLXXXII", RomanNumerals.intToRoman (982));

		assertEquals ("8", "VIII", RomanNumerals.intToRoman (8));
		assertEquals ("19", "XIX", RomanNumerals.intToRoman (19));
		assertEquals ("14", "XIV", RomanNumerals.intToRoman (14));

		assertEquals ("2564", "MMDLXIV", RomanNumerals.intToRoman (2564));
		assertEquals ("1999", "MCMXCIX", RomanNumerals.intToRoman (1999));
	}
}
