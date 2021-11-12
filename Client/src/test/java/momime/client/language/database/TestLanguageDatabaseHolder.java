package momime.client.language.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.client.languages.database.ShortcutKeyLang;
import momime.common.database.Language;
import momime.common.database.LanguageText;

/**
 * Tests the LanguageDatabaseHolder class
 */
@ExtendWith(MockitoExtension.class)
public final class TestLanguageDatabaseHolder
{
	/**
	 * Tests the findDescription method when the list is completely empty
	 */
	@Test
	public final void testFindDescription_None ()
	{
		// Set up object to test
		final LanguageDatabaseHolder holder = new LanguageDatabaseHolder ();
		holder.setLanguage (Language.ENGLISH);
		
		// Run method
		assertEquals ("", holder.findDescription (Arrays.asList ()));		
	}
	
	/**
	 * Tests the findDescription method when the requested language exists
	 */
	@Test
	public final void testFindDescription_Found ()
	{
		// Set up test descriptions
		final LanguageText english = new LanguageText ();
		english.setLanguage (Language.ENGLISH);
		english.setText ("A");

		final LanguageText french = new LanguageText ();
		french.setLanguage (Language.FRENCH);
		french.setText ("B");

		// Set up object to test
		final LanguageDatabaseHolder holder = new LanguageDatabaseHolder ();
		holder.setLanguage (Language.FRENCH);
		
		// Run method
		assertEquals ("B", holder.findDescription (Arrays.asList (english, french)));		
	}

	/**
	 * Tests the findDescription method when the requested language doesn't exist, so we default to the 1st one
	 */
	@Test
	public final void testFindDescription_Default ()
	{
		// Set up test descriptions
		final LanguageText english = new LanguageText ();
		english.setLanguage (Language.ENGLISH);
		english.setText ("A");

		final LanguageText french = new LanguageText ();
		french.setLanguage (Language.FRENCH);
		french.setText ("B");

		// Set up object to test
		final LanguageDatabaseHolder holder = new LanguageDatabaseHolder ();
		holder.setLanguage (Language.GERMAN);
		
		// Run method
		assertEquals ("A", holder.findDescription (Arrays.asList (english, french)));		
	}

	/**
	 * Tests the findShortcutKeyLang method when the list is completely empty
	 */
	@Test
	public final void testFindShortcutKeyLang_None ()
	{
		// Set up object to test
		final LanguageDatabaseHolder holder = new LanguageDatabaseHolder ();
		holder.setLanguage (Language.ENGLISH);
		
		// Run method
		assertNull (holder.findShortcutKeyLang (Arrays.asList ()));		
	}
	
	/**
	 * Tests the findShortcutKeyLang method when the requested language exists
	 */
	@Test
	public final void testFindShortcutKeyLang_Found ()
	{
		// Set up test descriptions
		final ShortcutKeyLang english = new ShortcutKeyLang ();
		english.setLanguage (Language.ENGLISH);
		english.setNormalKey ("A");

		final ShortcutKeyLang french = new ShortcutKeyLang ();
		french.setLanguage (Language.FRENCH);
		french.setNormalKey ("B");

		// Set up object to test
		final LanguageDatabaseHolder holder = new LanguageDatabaseHolder ();
		holder.setLanguage (Language.FRENCH);
		
		// Run method
		assertEquals ("B", holder.findShortcutKeyLang (Arrays.asList (english, french)).getNormalKey ());		
	}

	/**
	 * Tests the findShortcutKeyLang method when the requested language doesn't exist, so we default to the 1st one
	 */
	@Test
	public final void testFindShortcutKeyLang_Default ()
	{
		// Set up test descriptions
		final ShortcutKeyLang english = new ShortcutKeyLang ();
		english.setLanguage (Language.ENGLISH);
		english.setNormalKey ("A");

		final ShortcutKeyLang french = new ShortcutKeyLang ();
		french.setLanguage (Language.FRENCH);
		french.setNormalKey ("B");

		// Set up object to test
		final LanguageDatabaseHolder holder = new LanguageDatabaseHolder ();
		holder.setLanguage (Language.GERMAN);
		
		// Run method
		assertEquals ("A", holder.findShortcutKeyLang (Arrays.asList (english, french)).getNormalKey ());
	}
}