package momime.client.language.replacer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.Simple;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.Pick;

/**
 * Tests the BreakdownLanguageVariableReplacerImpl class
 */
public final class TestBreakdownLanguageVariableReplacerImpl extends ClientTestData
{
	/**
	 * Tests the listPickDescriptions method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListPickDescriptions () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		for (int n = 1; n <= 3; n++)
		{
			final Pick pick = new Pick ();
			pick.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Retort " + n));
			
			when (db.findPick ("RT0" + n, "listPickDescriptions")).thenReturn (pick);
		}

		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getAnd ().add (createLanguageText (Language.ENGLISH, "and"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Set up object to test
		final TextUtilsImpl textUtils = new TextUtilsImpl ();
		textUtils.setLanguageHolder (langHolder);
		
		final DummyLanguageVariableReplacer replacer = new DummyLanguageVariableReplacer ();
		replacer.setLanguageHolder (langHolder);
		replacer.setTextUtils (textUtils);
		replacer.setClient (client);
		
		// Set up some sample lists of pick IDs
		final List<String> singlePickID = new ArrayList<String> ();
		singlePickID.add ("RT02");

		final List<String> multiplePickIDs = new ArrayList<String> ();
		multiplePickIDs.add ("RT01");
		multiplePickIDs.add ("RT02");
		multiplePickIDs.add ("RT03");
		
		// Run method
		assertEquals ("", replacer.listPickDescriptions (null));
		assertEquals ("", replacer.listPickDescriptions (new ArrayList<String> ()));
		assertEquals ("Retort 2", replacer.listPickDescriptions (singlePickID));
		assertEquals ("Retort 1, Retort 2 and Retort 3", replacer.listPickDescriptions (multiplePickIDs));
	}

	/**
	 * Dummy implementation to test with
	 */
	private final class DummyLanguageVariableReplacer extends BreakdownLanguageVariableReplacerImpl<Object>
	{
		/**
		 * @param code Code to replace
		 * @return Replacement value; or null if the code is not recognized
		 */
		@Override
		public final String determineVariableValue (@SuppressWarnings ("unused") final String code)
		{
			// This isn't needed by any of the unit tests here
			return null;
		}
	}
}