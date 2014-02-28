package momime.client.language.database;

import java.util.List;

import momime.client.language.database.v0_9_5.KnownServer;

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
	
	/**
	 * @return List of all known servers
	 */
	public List<KnownServer> getKnownServer ();
}
