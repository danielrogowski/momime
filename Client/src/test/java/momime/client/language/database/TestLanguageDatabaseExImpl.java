package momime.client.language.database;

import static org.junit.Assert.assertEquals;
import momime.client.language.database.v0_9_4.LanguageEntry;

import org.junit.Test;

/**
 * Tests the LanguageDatabaseExImpl class
 */
public final class TestLanguageDatabaseExImpl
{
	/**
	 * Tests the findCategoryEntry method 
	 */
	@Test
	public final void testFindCategoryEntry ()
	{
		// Create some dummy categories and entries
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		
		for (int catNo = 1; catNo <= 3; catNo++)
		{
			final LanguageCategoryEx cat = new LanguageCategoryEx ();
			cat.setLanguageCategoryID ("C" + catNo);
			
			for (int entryNo = 1; entryNo <= 3; entryNo++)
			{
				final LanguageEntry entry = new LanguageEntry ();
				entry.setLanguageEntryID ("C" + catNo + "E" + entryNo);
				entry.setLanguageEntryText ("Blah" + catNo + entryNo);
				cat.getLanguageEntry ().add (entry);
			}			
			
			lang.getLanguageCategory ().add (cat);
		}
		
		lang.buildMaps ();

		// Entry exists
		assertEquals ("Blah22", lang.findCategoryEntry ("C2", "C2E2"));
		
		// Entry missing
		assertEquals ("C2/C2E4", lang.findCategoryEntry ("C2", "C2E4"));
		
		// Whole category missing
		assertEquals ("C4/C4E4", lang.findCategoryEntry ("C4", "C4E4"));
	}
}
