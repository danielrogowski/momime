package momime.client.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests the TextUtilsImpl class
 */
public final class TestTextUtilsImpl
{
	/**
	 * Tests the intToStrCommas method, with a locale that uses comma separators
	 */
	@Test
	public final void testIntToStrCommas ()
	{
		final TextUtilsImpl utils = new TextUtilsImpl ();
		
		assertEquals ("0", utils.intToStrCommas (0));
		assertEquals ("12", utils.intToStrCommas (12));
		assertEquals ("123", utils.intToStrCommas (123));
		assertEquals ("1,234", utils.intToStrCommas (1234));
		assertEquals ("12,345", utils.intToStrCommas (12345));
		assertEquals ("123,456", utils.intToStrCommas (123456));
		assertEquals ("1,234,567", utils.intToStrCommas (1234567));
		assertEquals ("-123", utils.intToStrCommas (-123));
		assertEquals ("-1,234", utils.intToStrCommas (-1234));
	}

	/**
	 * Tests the intToStrPlusMinus method
	 */
	@Test
	public final void testIntToStrPlusMinus ()
	{
		final TextUtilsImpl utils = new TextUtilsImpl ();

		assertEquals ("0", utils.intToStrPlusMinus (0));
		assertEquals ("+123", utils.intToStrPlusMinus (123));
		assertEquals ("+1234", utils.intToStrPlusMinus (1234));
		assertEquals ("-123", utils.intToStrPlusMinus (-123));
		assertEquals ("-1234", utils.intToStrPlusMinus (-1234));
	}
}
