package momime.client.language.replacer;

import java.util.List;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.utils.TextUtils;
import momime.common.database.RecordNotFoundException;

/**
 * Language variable replacers typically work based on some breakdown object, and need text utils + the language XML
 * so this handles all that common functionality so it doesn't need to be repeated.
 * 
 * @param <B> Class of breakdown object
 */
public abstract class BreakdownLanguageVariableReplacerImpl<B> extends LanguageVariableReplacerTokenImpl implements BreakdownLanguageVariableReplacer<B>
{
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Text utils */
	private TextUtils textUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Overall breakdown */
	private B breakdown;
	
	/**
	 * @param pickIDs List of pick IDs
	 * @return List coverted to descriptions
	 * @throws RecordNotFoundException if one of the Pick IDs can't be found
	 */
	final String listPickDescriptions (final List<String> pickIDs) throws RecordNotFoundException
	{
		final StringBuilder retortList = new StringBuilder ();
		if (pickIDs != null)
			for (final String pickID : pickIDs)
			{
				final String pickDesc = getLanguageHolder ().findDescription (getClient ().getClientDB ().findPick (pickID, "listPickDescriptions").getPickDescriptionSingular ());
			
				if (retortList.length () > 0)
					retortList.append (", ");
			
				retortList.append (pickDesc);
			}

		final String s = getTextUtils ().replaceFinalCommaByAnd (retortList.toString ());
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
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