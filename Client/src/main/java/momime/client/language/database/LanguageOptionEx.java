package momime.client.language.database;

import momime.client.languages.database.LanguageOption;

/**
 * Makes combo box of language options display the name
 */
public final class LanguageOptionEx extends LanguageOption
{
	/**
	 * @return Language display name
	 */
	@Override
	public final String toString ()
	{
		return getLanguageDescription ();
	}
}