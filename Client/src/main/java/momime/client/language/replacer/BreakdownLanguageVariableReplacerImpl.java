package momime.client.language.replacer;

import java.util.List;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.PickLang;
import momime.client.utils.TextUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Language variable replacers typically work based on some breakdown object, and need text utils + the language XML
 * so this handles all that common functionality so it doesn't need to be repeated.
 * 
 * @param <B> Class of breakdown object
 */
public abstract class BreakdownLanguageVariableReplacerImpl<B> extends LanguageVariableReplacerTokenImpl implements BreakdownLanguageVariableReplacer<B>
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (BreakdownLanguageVariableReplacerImpl.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Text utils */
	private TextUtils textUtils;
	
	/** Overall breakdown */
	private B breakdown;
	
	/**
	 * @param pickIDs List of pick IDs
	 * @return List coverted to descriptions
	 */
	final String listPickDescriptions (final List<String> pickIDs)
	{
		log.trace ("Entering listPickDescriptions: " + ((pickIDs == null) ? "null" : new Integer (pickIDs.size ()).toString ()));
		
		final StringBuilder retortList = new StringBuilder ();
		if (pickIDs != null)
			for (final String pickID : pickIDs)
			{
				final PickLang pick = getLanguage ().findPick (pickID);
				final String pickDesc = (pick == null) ? pickID : pick.getPickDescriptionSingular ();
			
				if (retortList.length () > 0)
					retortList.append (", ");
			
				retortList.append (pickDesc);
			}

		final String s = getTextUtils ().replaceFinalCommaByAnd (retortList.toString ());
		log.trace ("Entering listPickDescriptions = " + s);
		return s;
	}
	
	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
	}

	/**
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
	}
	
	/**
	 * @return Overall breakdown
	 */
	public final B getBreakdown ()
	{
		return breakdown;
	}
	
	/**
	 * @param obj Overall breakdown
	 */
	@Override
	public final void setBreakdown (final B obj)
	{
		breakdown = obj;
	}
}