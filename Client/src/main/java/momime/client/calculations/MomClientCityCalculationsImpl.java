package momime.client.calculations;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.Pick;
import momime.client.utils.TextUtils;
import momime.common.calculations.CalculateCityUnrestBreakdown;
import momime.common.calculations.CalculateCityUnrestBreakdown_Building;

/**
 * Client side only methods dealing with city calculations
 */
public final class MomClientCityCalculationsImpl implements MomClientCityCalculations
{
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Text utils */
	private TextUtils textUtils;
	
	/**
	 * @param text Text buffer to add to
	 * @param line Text to add
	 */
	private final void addLine (final StringBuilder text, final String line)
	{
		if (text.length () > 0)
			text.append ("\r\n");
		
		text.append (line);
	}
	
	/**
	 * @param breakdown Results of unrest calculation
	 * @return Readable calculation details
	 */
	@Override
	public final String describeCityUnrestCalculation (final CalculateCityUnrestBreakdown breakdown)
	{
		final StringBuilder text = new StringBuilder ();
		
		// Percentages
		if (breakdown.getTaxPercentage () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "TaxPercentage").replaceAll
				("TAX_PERCENTAGE", new Integer (breakdown.getTaxPercentage ()).toString ()));

		if (breakdown.getRacialPercentage () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "RacialPercentage").replaceAll
				("RACE_PERCENTAGE", new Integer (breakdown.getRacialPercentage ()).toString ()));

		if ((breakdown.getTaxPercentage () > 0) && (breakdown.getRacialPercentage () > 0))
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "TotalPercentage").replaceAll
				("TAX_PERCENTAGE", new Integer (breakdown.getTaxPercentage ()).toString ()).replaceAll
				("RACE_PERCENTAGE", new Integer (breakdown.getRacialPercentage ()).toString ()).replaceAll
				("TOTAL_PERCENTAGE", new Integer (breakdown.getTotalPercentage ()).toString ()));
		
		if ((breakdown.getTaxPercentage () > 0) || (breakdown.getRacialPercentage () > 0))
		{
			text.append ("\r\n");
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BaseValue").replaceAll
				("TOTAL_PERCENTAGE", new Integer (breakdown.getTotalPercentage ()).toString ()).replaceAll
				("CURRENT_POPULATION_DIV_1000", new Integer (breakdown.getPopulation ()).toString ()).replaceAll
				("BASE_UNREST", new Integer (breakdown.getBaseValue ()).toString ()));
		}
		
		// Klackons
		if (breakdown.getRacialLiteral () != 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "RacialLiteral").replaceAll
				("RACE_LITERAL", getTextUtils ().intToStrPlusMinus (breakdown.getRacialLiteral ())));
		
		// Buildings
		for (final CalculateCityUnrestBreakdown_Building buildingUnrest : breakdown.getBuildingsReducingUnrest ())
		{
			final Building building = getLanguage ().findBuilding (buildingUnrest.getBuildingID ());
			final String buildingName = (building == null) ? buildingUnrest.getBuildingID () : building.getBuildingName ();
			
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BuildingUnrestReduction").replaceAll
				("BUILDING_REDUCTION", new Integer (buildingUnrest.getUnrestReduction ()).toString ()).replaceAll
				("BUILDING_NAME", buildingName));
		}
		
		// Divine Power / Infernal Power retort
		if (breakdown.getReligiousBuildingReduction () != 0)
		{
			final StringBuilder retortList = new StringBuilder ();
			for (final String pickID : breakdown.getPickIdsContributingToReligiousBuildingBonus ())
			{
				final Pick pick = getLanguage ().findPick (pickID);
				final String pickDesc = (pick == null) ? pickID : pick.getPickDescription ();
				
				if (retortList.length () > 0)
					retortList.append (", ");
				
				retortList.append (pickDesc);
			}
			
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "Retort").replaceAll
				("RELIGIOUS_BUILDING_REDUCTION", new Integer (breakdown.getReligiousBuildingReduction ()).toString ()).replaceAll
				("RETORT_PERCENTAGE", new Integer (breakdown.getReligiousBuildingRetortPercentage ()).toString ()).replaceAll
				("RETORT_LIST", retortList.toString ()).replaceAll
				("RETORT_VALUE", new Integer (breakdown.getReligiousBuildingRetortValue ()).toString ()));
		}
		
		// Units stationed in city
		if (breakdown.getUnitCount () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "UnitReduction").replaceAll
				("UNIT_REDUCTION", new Integer (breakdown.getUnitReduction ()).toString ()).replaceAll
				("UNIT_COUNT", new Integer (breakdown.getUnitCount ()).toString ()));
		
		// Total
		addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BaseTotal").replaceAll
			("BASE_TOTAL", new Integer (breakdown.getBaseTotal ()).toString ()));
		text.append ("\r\n");

		if (breakdown.getForcePositive ())
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "ForcePositive"));

		if (breakdown.getForceAll ())
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "ForceAll").replaceAll
				("CURRENT_POPULATION_DIV_1000", new Integer (breakdown.getPopulation ()).toString ()));
		
		if (breakdown.getMinimumFarmers () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "MinimumFarmers").replaceAll
				("MINIMUM_FARMERS", new Integer (breakdown.getMinimumFarmers ()).toString ()).replaceAll
				("TOTAL_AFTER_FARMERS", new Integer (breakdown.getTotalAfterFarmers ()).toString ()));
		
		return text.toString ();
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
