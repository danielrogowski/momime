package momime.client.language.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.language.database.v0_9_9.LanguageCategory;
import momime.client.language.database.v0_9_9.LanguageEntry;

/**
 * Adds a map over the entries, so we can search for entry keys faster
 */
public final class LanguageCategoryEx extends LanguageCategory
{
	/** Map of entry IDs to the language text */
	private Map<String, String> entriesMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		entriesMap = new HashMap<String, String> ();
		for (final LanguageEntry thisEntry : getLanguageEntry ())
			entriesMap.put (thisEntry.getLanguageEntryID (), thisEntry.getLanguageEntryText ());
	}
	
	/**
	 * @param languageEntryID Entry ID to search for
	 * @return Text of the requested language entry; or null if entry not found
	 */
	public final String findEntry (final String languageEntryID)
	{
		return entriesMap.get (languageEntryID);
	}
}
