package momime.client.language.database;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.client.languages.database.Shortcut;
import momime.client.languages.database.ShortcutKey;
import momime.client.languages.database.ShortcutKeyLang;
import momime.common.database.Language;
import momime.common.database.LanguageText;

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
	/** Class logger */
	private final static Log log = LogFactory.getLog (LanguageDatabaseHolder.class);
	
	/** Currently chosen language */
	private Language language;

	/** New singular language XML */
	private MomLanguagesEx languages;
	
	/**
	 * @return Currently chosen language
	 */
	public final Language getLanguage ()
	{
		return language;
	}

	/**
	 * @param lang Currently chosen language
	 */
	public final void setLanguage (final Language lang)
	{
		language = lang;
	}
	
	/**
	 * @return New singular language XML
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languages;
	}

	/**
	 * @param lang New singular language XML
	 */
	public final void setLanguages (final MomLanguagesEx lang)
	{
		languages = lang;
	}
	
	/**
	 * @param list List of language alternatives to read from
	 * @return Text to display
	 */
	public final String findDescription (final List<LanguageText> list)
	{
		LanguageText entry = list.stream ().filter (t -> t.getLanguage () == getLanguage ()).findAny ().orElse (null);
		
		if ((entry == null) && (list.size () > 0))
			entry = list.get (0);
		
		final String text = (entry == null) ? null : entry.getText ();
		return (text == null) ? "" : text;
	}	

	/**
	 * @param list List of language alternatives to read from
	 * @return Shortcut key, or null if there isn't one
	 */
	public final ShortcutKeyLang findShortcutKeyLang (final List<ShortcutKeyLang> list)
	{
		ShortcutKeyLang entry = list.stream ().filter (t -> t.getLanguage () == getLanguage ()).findAny ().orElse (null);
		
		if ((entry == null) && (list.size () > 0))
			entry = list.get (0);
		
		return entry;
	}
	
	/**
	 * Shortcuts must have been registered prior to call this, e.g. contentPane.getActionMap ().put (Shortcut.MESSAGE_BOX_CLOSE,	okAction);
	 * 
	 * @param contentPane Content pane to configure shortcut keys for
	 */
	public final void configureShortcutKeys (final JComponent contentPane)
	{
		contentPane.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).clear ();
		
		final Object [] keys = contentPane.getActionMap ().keys ();
		if ((keys != null) && (getLanguages () != null))
			for (final Object shortcut : contentPane.getActionMap ().keys ())
				if (shortcut instanceof Shortcut)
				{
					final ShortcutKey shortcutKey = getLanguages ().findShortcutKey ((Shortcut) shortcut);
					if (shortcutKey != null)
					{
						final ShortcutKeyLang shortcutKeyLang = findShortcutKeyLang (shortcutKey.getKeyLang ());
						if (shortcutKeyLang != null)
						{
							final String keyCode = (shortcutKeyLang.getNormalKey () != null) ? shortcutKeyLang.getNormalKey () : shortcutKeyLang.getVirtualKey ().value ().substring (3);
							log.debug ("Binding \"" + keyCode + "\" to action " + shortcut);
							contentPane.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (keyCode), shortcut);
						}
					}
				}
	}
}