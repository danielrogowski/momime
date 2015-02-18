package momime.client.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.PickLang;
import momime.client.language.database.PickTypeLang;
import momime.common.database.Pick;
import momime.common.database.PickPrerequisite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Client side only helper methods for dealing with players' pick choices
 */
public final class PlayerPickClientUtilsImpl implements PlayerPickClientUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (PlayerPickClientUtilsImpl.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/**
	 * @param pick Pick we want pre-requisites for
	 * @return Description of the pre-requisites for this pick (e.g. "2 Spell Books in any 3 Realms of Magic"), or null if it has no pre-requisites
	 */
	@Override
	public final String describePickPreRequisites (final Pick pick)
	{
		log.trace ("Entering describePickPreRequisites: " + pick.getPickID ());

		// This map of maps stores, for each pick type (i.e. books/retorts), how many times we need N picks
		// e.g. for the example above, the first map key would be B, the second map key would be 2, and the value would be 3
		final Map<String, Map<Integer, Integer>> genericPrerequisites = new HashMap<String, Map<Integer, Integer>> ();
		
		// Now go through each prerequisite
		final StringBuilder result = new StringBuilder ();
		for (final PickPrerequisite req : pick.getPickPrerequisite ())
			if (req.getPrerequisiteID () != null)
			{
				// Pre-requisite for a specific type of book, e.g. 4 life books, so just add this directly to the output
				final PickLang pickLang = getLanguage ().findPick (req.getPrerequisiteID ());
				
				final String pickDesc;
				if (pickLang == null)
					pickDesc = null;
				else if (req.getPrerequisiteCount () == 1)
					pickDesc = pickLang.getPickDescriptionSingular ();
				else
					pickDesc = pickLang.getPickDescriptionPlural ();
				
				if (result.length () > 0)
					result.append (", ");
				
				result.append (req.getPrerequisiteCount () + " " + ((pickDesc != null) ? pickDesc : req.getPrerequisiteID ()));
			}
			else if (req.getPrerequisiteTypeID () != null)
			{
				// Get the map for books or retorts
				Map<Integer, Integer> pickTypeMap = genericPrerequisites.get (req.getPrerequisiteTypeID ());
				if (pickTypeMap == null)
				{
					pickTypeMap = new HashMap<Integer, Integer> ();
					genericPrerequisites.put (req.getPrerequisiteTypeID (), pickTypeMap);
				}
				
				// Get the map for the count
				Integer count = pickTypeMap.get (req.getPrerequisiteCount ());
				count = (count == null) ? 1 : (count + 1);
				pickTypeMap.put (req.getPrerequisiteCount (), count);
			}
		
		// Now generate text for the values in the generic map
		for (final Entry<String, Map<Integer, Integer>> pickType : genericPrerequisites.entrySet ())
		{
			final PickTypeLang pickTypeLang = getLanguage ().findPickType (pickType.getKey ());
			for (final Entry<Integer, Integer> counts : pickType.getValue ().entrySet ())
			{
				// This is the number of books that we need (e.g. pairs)
				final String pickTypeDesc;
				if (pickTypeLang == null)
					pickTypeDesc = null;
				else if (counts.getKey () == 1)
					pickTypeDesc = pickTypeLang.getPickTypeDescriptionSingular ();
				else
					pickTypeDesc = pickTypeLang.getPickTypeDescriptionPlural ();
				
				// This is how many repetitions we need (e.g. 3 pairs)
				final String repetitionsDesc;
				if (pickTypeLang == null)
					repetitionsDesc = null;
				else if (counts.getValue () == 1)
					repetitionsDesc = pickTypeLang.getPickTypePrerequisiteSingular ();
				else
					repetitionsDesc = pickTypeLang.getPickTypePrerequisitePlural ();
				
				// Now can work out the text
				if (result.length () > 0)
					result.append (", ");
				
				result.append (((repetitionsDesc != null ) ? repetitionsDesc : pickType.getKey ()).replaceAll
					("REPETITIONS", counts.getValue ().toString ()).replaceAll
					("PICK_TYPE", counts.getKey () + " " + ((pickTypeDesc != null) ? pickTypeDesc : pickType.getKey ())));
			}
		}
		
		final String text = (result.length () == 0) ? null : getTextUtils ().replaceFinalCommaByAnd (result.toString ());
		log.trace ("Exiting describePickPreRequisites = " + text);
		return text;
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
}