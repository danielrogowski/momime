package momime.client.language.database;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import momime.common.database.Language;
import momime.common.database.RecordNotFoundException;

/**
 * Tests the MomLanguagesExImpl class
 */
public final class TestMomLanguagesExImpl
{
	/**
	 * Tests the findLanguageOption option searching for a language that exists
	 * @throws RecordNotFoundException If the language is not found
	 */
	@Test
	public final void testFindLanguageOption_Exists () throws RecordNotFoundException
	{
		final MomLanguagesExImpl db = new MomLanguagesExImpl ();
		
		final LanguageOptionEx english = new LanguageOptionEx ();
		english.setLanguage (Language.ENGLISH);
		english.setLanguageDescription ("English");
		db.getLanguageOptions ().add (english);
		
		final LanguageOptionEx french = new LanguageOptionEx ();
		french.setLanguage (Language.FRENCH);
		french.setLanguageDescription ("French");
		db.getLanguageOptions ().add (french);
		
		db.buildMaps ();
		
		assertSame (english, db.findLanguageOption (Language.ENGLISH, "testFindLanguageOption_Exists"));
		assertSame (french, db.findLanguageOption (Language.FRENCH, "testFindLanguageOption_Exists"));
	}

	/**
	 * Tests the findLanguageOption option searching for a language that exists
	 * @throws RecordNotFoundException If the language is not found
	 */
	@Test
	public final void testFindLanguageOption_NotExists () throws RecordNotFoundException
	{
		final MomLanguagesExImpl db = new MomLanguagesExImpl ();
		
		final LanguageOptionEx english = new LanguageOptionEx ();
		english.setLanguage (Language.ENGLISH);
		english.setLanguageDescription ("English");
		db.getLanguageOptions ().add (english);
		
		final LanguageOptionEx french = new LanguageOptionEx ();
		french.setLanguage (Language.FRENCH);
		french.setLanguageDescription ("French");
		db.getLanguageOptions ().add (french);
		
		db.buildMaps ();
		
		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findLanguageOption (Language.GERMAN, "testFindLanguageOption_NotExists");
		});
	}
}