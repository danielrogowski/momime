package momime.client.language.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.language.database.v0_9_5.LanguageCategory;
import momime.client.language.database.v0_9_5.LanguageDatabase;

/**
 * Implementation of language XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class LanguageDatabaseExImpl extends LanguageDatabase implements LanguageDatabaseEx
{
	/** Map of category IDs to category objects */
	private Map<String, LanguageCategoryEx> categoriesMap;

	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		// Create categories map
		categoriesMap = new HashMap<String, LanguageCategoryEx> ();
		for (final LanguageCategory thisCategory : getLanguageCategory ())
		{
			final LanguageCategoryEx catEx = (LanguageCategoryEx) thisCategory;
			catEx.buildMap ();
			categoriesMap.put (thisCategory.getLanguageCategoryID (), catEx);
		}
	}	

	/**
	 * @param languageCategoryID Category ID to search for
	 * @param languageEntryID Entry ID to search for
	 * @return Text of the requested language entry; or replays the key back if the category or entry doesn't exist
	 */
	@Override
	public final String findCategoryEntry (final String languageCategoryID, final String languageEntryID)
	{
		final LanguageCategoryEx cat = categoriesMap.get (languageCategoryID);
		final String entry = (cat == null) ? null : cat.findEntry (languageEntryID);
		return (entry == null) ? (languageCategoryID + "/" + languageEntryID) : entry;
	}
}
