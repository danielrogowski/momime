package momime.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import momime.client.ClientTestData;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.Simple;
import momime.common.database.Language;

/**
 * Tests the TextUtilsImpl class
 */
public final class TestTextUtilsImpl extends ClientTestData
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
	
	/**
	 * Tests the halfIntToStr method
	 */
	@Test
	public final void testHalfIntToStr ()
	{
		final TextUtilsImpl utils = new TextUtilsImpl ();

		assertEquals ("0", utils.halfIntToStr (0));
		assertEquals ("" + TextUtilsImpl.HALF, utils.halfIntToStr (1));
		assertEquals ("2", utils.halfIntToStr (4));
		assertEquals ("2123", utils.halfIntToStr (4246));
		assertEquals ("2" + TextUtilsImpl.HALF, utils.halfIntToStr (5));
		assertEquals ("2123" + TextUtilsImpl.HALF, utils.halfIntToStr (4247));

		assertEquals ("-" + TextUtilsImpl.HALF, utils.halfIntToStr (-1));
		assertEquals ("-2", utils.halfIntToStr (-4));
		assertEquals ("-2" + TextUtilsImpl.HALF, utils.halfIntToStr (-5));
		assertEquals ("-2123", utils.halfIntToStr (-4246));
		assertEquals ("-2123" + TextUtilsImpl.HALF, utils.halfIntToStr (-4247));
	}

	/**
	 * Tests the halfIntToStrPlusMinus method
	 */
	@Test
	public final void testHalfIntToStrPlusMinus ()
	{
		final TextUtilsImpl utils = new TextUtilsImpl ();

		assertEquals ("0", utils.halfIntToStrPlusMinus (0));
		assertEquals ("+" + TextUtilsImpl.HALF, utils.halfIntToStrPlusMinus (1));
		assertEquals ("+2", utils.halfIntToStrPlusMinus (4));
		assertEquals ("+2123", utils.halfIntToStrPlusMinus (4246));
		assertEquals ("+2" + TextUtilsImpl.HALF, utils.halfIntToStrPlusMinus (5));
		assertEquals ("+2123" + TextUtilsImpl.HALF, utils.halfIntToStrPlusMinus (4247));

		assertEquals ("-" + TextUtilsImpl.HALF, utils.halfIntToStrPlusMinus (-1));
		assertEquals ("-2", utils.halfIntToStrPlusMinus (-4));
		assertEquals ("-2" + TextUtilsImpl.HALF, utils.halfIntToStrPlusMinus (-5));
		assertEquals ("-2123", utils.halfIntToStrPlusMinus (-4246));
		assertEquals ("-2123" + TextUtilsImpl.HALF, utils.halfIntToStrPlusMinus (-4247));
	}
	
	/**
	 * Tests the insertDecimalPoint method
	 */
	@Test
	public final void testInsertDecimalPoint ()
	{
		final TextUtilsImpl utils = new TextUtilsImpl ();
		
		assertEquals ("0.005", utils.insertDecimalPoint (5, 3));
		assertEquals ("0.045", utils.insertDecimalPoint (45, 3));
		assertEquals ("0.345", utils.insertDecimalPoint (345, 3));
		assertEquals ("2.345", utils.insertDecimalPoint (2345, 3));
		assertEquals ("12.345", utils.insertDecimalPoint (12345, 3));
		assertEquals ("123.45", utils.insertDecimalPoint (12345, 2));
		assertEquals ("1234.5", utils.insertDecimalPoint (12345, 1));
	}
	
	/**
	 * Tests the replaceFinalCommaByAnd method
	 */
	@Test
	public final void testReplaceFinalCommaByAnd ()
	{
		// Mock entry from language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getAnd ().add (createLanguageText (Language.ENGLISH, "and"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Set up object to test
		final TextUtilsImpl utils = new TextUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		
		// Run tests
		assertNull (utils.replaceFinalCommaByAnd (null));
		assertEquals ("bah", utils.replaceFinalCommaByAnd ("bah"));
		assertEquals ("bah,hum", utils.replaceFinalCommaByAnd ("bah,hum"));
		assertEquals ("bah and hum", utils.replaceFinalCommaByAnd ("bah, hum"));
		assertEquals ("bah, hum and bug", utils.replaceFinalCommaByAnd ("bah, hum, bug"));
	}
}