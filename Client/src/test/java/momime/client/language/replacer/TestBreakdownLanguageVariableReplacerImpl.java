package momime.client.language.replacer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Pick;

import org.junit.Test;

/**
 * Tests the BreakdownLanguageVariableReplacerImpl class
 */
public final class TestBreakdownLanguageVariableReplacerImpl
{
	/**
	 * Tests the listPickDescriptions method
	 */
	@Test
	public final void testListPickDescriptions ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		for (int n = 1; n <= 3; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickDescriptionSingular ("Retort " + n);
			
			when (lang.findPick ("RT0" + n)).thenReturn (pick);
		}

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Set up object to test
		final DummyLanguageVariableReplacer replacer = new DummyLanguageVariableReplacer ();
		replacer.setLanguageHolder (langHolder);
		
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
		assertEquals ("Retort 1, Retort 2, Retort 3", replacer.listPickDescriptions (multiplePickIDs));
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
		protected final String determineVariableValue (final String code)
		{
			// This isn't needed by any of the unit tests here
			return null;
		}
	}
}