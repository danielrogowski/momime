package momime.client.language;

import java.io.IOException;

import com.ndg.utils.swing.NdgUIUtils;

import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.common.database.Language;

/**
 * Implements standard portions of LanguageVariableUI that don't depend on the type of UI component being created.
 */
public abstract class LanguageVariableUIImpl implements LanguageVariableUI
{
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Component responsible for controlling the selected language */
	private LanguageChangeMaster languageChangeMaster;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/**
	 * Descendant classes put code here to create and lay out all the Swing components
	 * @throws IOException If a resource cannot be found
	 */
	protected abstract void init () throws IOException;

	/**
	 * @return Language database holder
	 */
	@Override
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	@Override
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * @return Currently chosen language
	 */
	@Override
	public final Language getLanguage ()
	{
		return getLanguageHolder ().getLanguage ();
	}
	
	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return New singular language XML
	 */
	@Override
	public final MomLanguagesEx getLanguages ()
	{
		return getLanguageHolder ().getLanguages ();
	}

	/**
	 * @return Component responsible for controlling the selected language
	 */
	@Override
	public final LanguageChangeMaster getLanguageChangeMaster ()
	{
		return languageChangeMaster;
	}

	/**
	 * @param master Component responsible for controlling the selected language
	 */
	@Override
	public final void setLanguageChangeMaster (final LanguageChangeMaster master)
	{
		languageChangeMaster = master;
	}
	
	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}
}