package momime.client.ui;

import java.io.IOException;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;

/**
 * Interface describing general contract all UI screens must conform to
 */
public interface MomClientUI
{
	/**
	 * @return Whether this screen is currently displayed or not
	 */
	public boolean isVisible ();
	
	/**
	 * @param v Whether to display or hide this screen
	 * @throws IOException If a resource cannot be found
	 */
	public void setVisible (final boolean v) throws IOException;

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
