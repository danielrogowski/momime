package momime.client.language;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;

/**
 * Interface describing general contract that all UI components that display any language-variable text must conform to.
 * The key part to this is the languageChanged () method.  Everything else is just in support of this:
 * 
 * 1) Any descendant will need the getLanguage () method to access the language XML to be able to implement its languageChanged () method.
 * 2) The getLanguage () method will always access the XML via getLanguageHolder ().
 * 3) We need getLanguageChangeMaster () so that we can tell it to call our languageChanged () method when the language changes.
 */
public interface LanguageVariableUI
{
	/**
	 * @return Language database holder
	 */
	public LanguageDatabaseHolder getLanguageHolder ();
	
	/**
	 * @param holder Language database holder
	 */
	public void setLanguageHolder (final LanguageDatabaseHolder holder);

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public LanguageDatabaseEx getLanguage ();
	
	/**
	 * Notifies this screen that the language contained by the holder has changed
	 */
	public void languageChanged ();
	
	/**
	 * @return Component responsible for controlling the selected language
	 */
	public LanguageChangeMaster getLanguageChangeMaster ();

	/**
	 * @param master Component responsible for controlling the selected language
	 */
	public void setLanguageChangeMaster (final LanguageChangeMaster master);
}