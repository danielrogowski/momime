package momime.client.language.database;

/**
 * XML tags used in MoM IME language files
 */
public class LanguageDatabaseConstants
{
	/** Path and name to locate the language XSD file */
	public static final String LANGUAGE_XSD_LOCATION = "/momime.client.language/MoMIMELanguageDatabase.xsd";

	/** Path and name to locate the language XSD file that doesn't cross check references against the server XSD */
	public static final String LANGUAGE_XSD_LOCATION_NO_SERVER_XSD_LINK = "/momime.client.language/MoMIMELanguageDatabase_NoServerXsdLink.xsd";
	
	/**
	 * Prevent instatiation of this class
	 */
	private LanguageDatabaseConstants ()
	{
	}
}