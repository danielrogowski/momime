package momime.client.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ndg.random.RandomUtils;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.common.database.Pick;
import momime.common.database.PickPrerequisite;
import momime.common.database.PickType;
import momime.common.database.RecordNotFoundException;

/**
 * Client side only helper methods for dealing with players' pick choices
 */
public final class PlayerPickClientUtilsImpl implements PlayerPickClientUtils
{
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/**
	 * @param pick Pick we want pre-requisites for
	 * @return Description of the pre-requisites for this pick (e.g. "2 Spell Books in any 3 Realms of Magic"), or null if it has no pre-requisites
	 * @throws RecordNotFoundException If one of the prerequisite picks cannot be found
	 */
	@Override
	public final String describePickPreRequisites (final Pick pick) throws RecordNotFoundException
	{
		// This map of maps stores, for each pick type (i.e. books/retorts), how many times we need N picks
		// e.g. for the example above, the first map key would be B, the second map key would be 2, and the value would be 3
		final Map<String, Map<Integer, Integer>> genericPrerequisites = new HashMap<String, Map<Integer, Integer>> ();
		
		// Now go through each prerequisite
		final StringBuilder result = new StringBuilder ();
		for (final PickPrerequisite req : pick.getPickPrerequisite ())
			if (req.getPrerequisiteID () != null)
			{
				// Pre-requisite for a specific type of book, e.g. 4 life books, so just add this directly to the output
				final Pick prereq = getClient ().getClientDB ().findPick (req.getPrerequisiteID (), "describePickPreRequisites");
				
				final String pickDesc;
				if (req.getPrerequisiteCount () == 1)
					pickDesc = getLanguageHolder ().findDescription (prereq.getPickDescriptionSingular ());
				else
					pickDesc = getLanguageHolder ().findDescription (prereq.getPickDescriptionPlural ());
				
				if (result.length () > 0)
					result.append (", ");
				
				result.append (req.getPrerequisiteCount () + " " + pickDesc);
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
			final PickType pickTypePrereq = getClient ().getClientDB ().findPickType (pickType.getKey (), "describePickPreRequisites");
			for (final Entry<Integer, Integer> counts : pickType.getValue ().entrySet ())
			{
				// This is the number of books that we need (e.g. pairs)
				final String pickTypeDesc;
				if (counts.getKey () == 1)
					pickTypeDesc = getLanguageHolder ().findDescription (pickTypePrereq.getPickTypeDescriptionSingular ());
				else
					pickTypeDesc = getLanguageHolder ().findDescription (pickTypePrereq.getPickTypeDescriptionPlural ());
				
				// This is how many repetitions we need (e.g. 3 pairs)
				final String repetitionsDesc;
				if (counts.getValue () == 1)
					repetitionsDesc = getLanguageHolder ().findDescription (pickTypePrereq.getPickTypePrerequisiteSingular ());
				else
					repetitionsDesc = getLanguageHolder ().findDescription (pickTypePrereq.getPickTypePrerequisitePlural ());
				
				// Now can work out the text
				if (result.length () > 0)
					result.append (", ");
				
				result.append (repetitionsDesc.replaceAll
					("REPETITIONS", counts.getValue ().toString ()).replaceAll
					("PICK_TYPE", counts.getKey () + " " + pickTypeDesc));
			}
		}
		
		final String text = (result.length () == 0) ? null : getTextUtils ().replaceFinalCommaByAnd (result.toString ());
		return text;
	}
	
	/**
	 * @param pick Magic realm we want a book image for
	 * @return Randomly selected image for this type of pick
	 * @throws RecordNotFoundException If this wizard has no combat music playlists defined
	 */
	@Override
	public final String chooseRandomBookImageFilename (final Pick pick) throws RecordNotFoundException	
	{
		if (pick.getBookImageFile ().size () == 0)
			throw new RecordNotFoundException ("BookImage", null, "chooseRandomBookImageFilename");
		
		final String filename = pick.getBookImageFile ().get (getRandomUtils ().nextInt (pick.getBookImageFile ().size ()));
		return filename;
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
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
}