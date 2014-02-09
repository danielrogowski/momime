package momime.client.language.database;

/**
 * The Spring file wants to inject the language XML into all the forms - but that's a problem if the chosen language then
 * changes after the forms start up - we can't force a new value into Spring and force it to re-inject it everywhere.
 * 
 * So instead, this is the singleton that gets injected everywhere, and Spring will initially set the contained value
 * to the database specified in the config file, but still allows us the freedom to later alter the contained value
 * if the user picks the menu option to change language.
 */
public final class LanguageDatabaseHolder
{
	/** Currently chosen language */
	private LanguageDatabaseEx language;

	/**
	 * @return Currently chosen language
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return language;
	}

	/**
	 * @param lang Currently chosen language
	 */
	public final void setLanguage (final LanguageDatabaseEx lang)
	{
		language = lang;
	}
}
