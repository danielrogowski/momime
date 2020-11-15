package momime.client.language.database;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.client.languages.database.KnownServer;
import momime.client.languages.database.LanguageOption;
import momime.client.languages.database.MomLanguages;
import momime.client.languages.database.Shortcut;
import momime.client.languages.database.ShortcutKey;
import momime.common.database.Language;
import momime.common.database.RecordNotFoundException;

/**
 * New singular language XML
 */
public final class MomLanguagesExImpl extends MomLanguages implements MomLanguagesEx
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MomLanguagesExImpl.class);
	
	/** Map of language enums to language options objects */
	private Map<Language, LanguageOptionEx> languageOptionsMap;
	
	/** Map of known server IDs to known server objects */
	private Map<String, KnownServer> knownServersMap;
	
	/** Map of shortcuts to shortcut keys objects */
	private Map<Shortcut, ShortcutKey> shortcutsMap;
	
	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		log.trace ("Entering buildMaps");
		
		languageOptionsMap = getLanguageOptions ().stream ().collect (Collectors.toMap (l -> l.getLanguage (), l -> l));
		knownServersMap = getKnownServer ().stream ().collect (Collectors.toMap (s -> s.getKnownServerID (), s -> s));
		shortcutsMap = getShortcutKey ().stream ().collect (Collectors.toMap (s -> s.getShortcut (), s -> s));
		
		log.trace ("Exiting buildMaps");
	}

	/**
	 * @return Container for language strings
	 */
	@Override
    public final SpellTargettingEx getSpellTargetting ()
    {
    	return (SpellTargettingEx) super.getSpellTargetting ();
    }
	
	/**
	 * @return List of all supported languages
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<LanguageOptionEx> getLanguageOptions ()
	{
		return (List<LanguageOptionEx>) (List<?>) getLanguageOption ();
	}

	/**
	 * @param lang Language to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Language option object
	 * @throws RecordNotFoundException If the language doesn't exist
	 */
	@Override
	public final LanguageOptionEx findLanguageOption (final Language lang, final String caller) throws RecordNotFoundException
	{
		final LanguageOptionEx found = languageOptionsMap.get (lang);
		if (found == null)
			throw new RecordNotFoundException (LanguageOption.class, lang.toString (), caller);

		return found;
	}

	/**
	 * @param knownServerID Known server to search for
	 * @return Known server object if exists, null if not found
	 */
	@Override
	public final KnownServer findKnownServer (final String knownServerID)
	{
		return knownServersMap.get (knownServerID);
	}

	/**
	 * This doesn't throw RecordNotFoundExceptions so that we don't have to define shortcut keys in unit tests 
	 * 
	 * @param shortcut Game shortcut that we're looking to see if there is a key defined for it
	 * @return Details of the keys that should activate this shortcut in different languages, or null if not found
	 */
	@Override
	public final ShortcutKey findShortcutKey (final Shortcut shortcut)
	{
		return shortcutsMap.get (shortcut);
	}
}