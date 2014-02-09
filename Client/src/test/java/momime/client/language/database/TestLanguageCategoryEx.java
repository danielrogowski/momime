package momime.client.language.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import momime.client.language.database.v0_9_4.LanguageEntry;

import org.junit.Test;

/**
 * Tests the LanguageCategoryEx class
 */
public final class TestLanguageCategoryEx
{
	/**
	 * Tests the findEntry method
	 */
	@Test
	public final void testFindEntry ()
	{
		// Create some dummy entries
		final LanguageCategoryEx cat = new LanguageCategoryEx ();
		for (int n = 1; n <= 3; n++)
		{
			final LanguageEntry entry = new LanguageEntry ();
			entry.setLanguageEntryID ("E" + n);
			entry.setLanguageEntryText ("Blah" + n);
			
			cat.getLanguageEntry ().add (entry);
		}
		
		cat.buildMap ();
		
		// Run tests
		assertEquals ("Blah2", cat.findEntry ("E2"));
		assertNull (cat.findEntry ("E4"));
	}
}
