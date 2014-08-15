package momime.client.utils;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.Unit;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.MemoryUnit;

/**
 * Client side only helper methods for dealing with units
 */
public final class UnitClientUtilsImpl implements UnitClientUtils
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/**
	 * Note the generated unit names are obviously very dependant on the selected language, but the names themselves don't get notified
	 * to update themselves when the language changes.  It is the responsibility of whatever is calling this method to register itself to be
	 * notified of language updates, and cause this method to be re-evalulated when that happens.
	 * 
	 * @param unit Unit to generate the name of
	 * @param unitNameType Type of name to generate (see comments against that enum)
	 * @return Generated unit name
	 * @throws RecordNotFoundException If we can't find the unit definition in the server XML
	 */
	@Override
	public final String getUnitName (final AvailableUnit unit, final UnitNameType unitNameType) throws RecordNotFoundException
	{
		// Heroes just output their name for all unitNameTypes, so in that case we don't need to look up anything at all
		String unitName = null;
		if (unit instanceof MemoryUnit)
			unitName = ((MemoryUnit) unit).getUnitName ();
		
		if (unitName == null)
		{
			// Find the name in the language XML
			final momime.client.language.database.v0_9_5.Unit unitLang = getLanguage ().findUnit (unit.getUnitID ());
			unitName = (unitLang != null) ? unitLang.getUnitName () : unit.getUnitID ();
			
			// If we just want the simple name with no race prefix, then we're done
			if (unitNameType != UnitNameType.SIMPLE_UNIT_NAME)
			{
				// Need the record from the server XML to know whether to put the racial prefix on or not
				final Unit unitInfo = getClient ().getClientDB ().findUnit (unit.getUnitID (), "getUnitName");
				if ((unitInfo.isIncludeRaceInUnitName () != null) && (unitInfo.isIncludeRaceInUnitName ()))
				{
					final Race race = getLanguage ().findRace (unitInfo.getUnitRaceID ());
					unitName = ((race != null) ? race.getRaceName () : unitInfo.getUnitRaceID ()) + " " + unitName;
				}
				
				// How we modify this now depends on the requested unitNameType
				switch (unitNameType)
				{
					case A_UNIT_NAME:
						if ((unitLang != null) && (unitLang.getUnitNamePrefix () != null))
							unitName = unitLang.getUnitNamePrefix () + " " + unitName;
						break;
						
					case THE_UNIT_OF_NAME:
						final String languageEntryID;
						if ((unitLang != null) && (unitLang.getUnitNamePrefix () != null))
							languageEntryID = "Singular";
						else
							languageEntryID = "Plural";
						
						unitName = getLanguage ().findCategoryEntry ("UnitName", "TheUnitOfName" + languageEntryID).replaceAll ("RACE_UNIT_NAME", unitName);
						break;
						
					// Other name types are handled elsewhere or require nothing to be added
					default:
						break;
				}
			}
		}
		
		return unitName;
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
}