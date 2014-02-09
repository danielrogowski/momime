package momime.client.language.database;

/**
 * Describes operations that we need to support over the language XML file
 */
public interface LanguageDatabaseEx
{
	/**
	 * @param languageCategoryID Category ID to search for
	 * @param languageEntryID Entry ID to search for
	 * @return Text of the requested language entry; or replays the key back if the category or entry doesn't exist
	 */
	public String findCategoryEntry (final String languageCategoryID, final String languageEntryID);
}
