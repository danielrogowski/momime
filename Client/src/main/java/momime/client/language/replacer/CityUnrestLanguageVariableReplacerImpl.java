package momime.client.language.replacer;

import momime.client.language.database.BuildingLang;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.CityUnrestBreakdownBuilding;

/**
 * Language replacer for city unrest calculation variables
 */
public final class CityUnrestLanguageVariableReplacerImpl extends BreakdownLanguageVariableReplacerImpl<CityUnrestBreakdown> implements CityUnrestLanguageVariableReplacer
{
	/** Building specific breakdown */
	private CityUnrestBreakdownBuilding currentBuilding;
	
	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 */
	@Override
	protected final String determineVariableValue (final String code)
	{
		final String text;
		switch (code)
		{
			case "CURRENT_POPULATION_DIV_1000":
				text = new Integer (getBreakdown ().getPopulation ()).toString ();
				break;

			case "TAX_PERCENTAGE":
				text = new Integer (getBreakdown ().getTaxPercentage ()).toString ();
				break;

			case "RACE_PERCENTAGE":
				text = new Integer (getBreakdown ().getRacialPercentage ()).toString ();
				break;

			case "TOTAL_PERCENTAGE":
				text = new Integer (getBreakdown ().getTotalPercentage ()).toString ();
				break;

			case "BASE_UNREST":
				text = new Integer (getBreakdown ().getBaseValue ()).toString ();
				break;
				
			case "RACE_LITERAL":
				text = getTextUtils ().intToStrPlusMinus (getBreakdown ().getRacialLiteral ());
				break;
				
			case "RELIGIOUS_BUILDING_REDUCTION":
				text = new Integer (getBreakdown ().getReligiousBuildingReduction ()).toString ();
				break;
				
			case "RETORT_PERCENTAGE":
				text = new Integer (getBreakdown ().getReligiousBuildingRetortPercentage ()).toString ();
				break;

			case "RETORT_LIST":
				text = listPickDescriptions (getBreakdown ().getPickIdContributingToReligiousBuildingBonus ());
				break;

			case "RETORT_VALUE":
				text = new Integer (getBreakdown ().getReligiousBuildingRetortValue ()).toString ();
				break;
				
			case "UNIT_REDUCTION":
				text = new Integer (getBreakdown ().getUnitReduction ()).toString ();
				break;
				
			case "UNIT_COUNT":
				text = new Integer (getBreakdown ().getUnitCount ()).toString ();
				break;
				
			case "BASE_TOTAL":
				text = new Integer (getBreakdown ().getBaseTotal ()).toString ();
				break;
				
			case "MINIMUM_FARMERS":
				text = new Integer (getBreakdown ().getMinimumFarmers ()).toString ();
				break;
				
			case "TOTAL_AFTER_FARMERS":
				text = new Integer (getBreakdown ().getTotalAfterFarmers ()).toString ();
				break;
				
			// Dependant on current building
			case "BUILDING_NAME":
				final BuildingLang building = getLanguage ().findBuilding (getCurrentBuilding ().getBuildingID ());
				text = (building == null) ? getCurrentBuilding ().getBuildingID () : building.getBuildingName ();
				break;

			case "BUILDING_REDUCTION":
				text = "-" + getCurrentBuilding ().getUnrestReduction ();
				break;
				
			default:
				text = null;
		}
		return text;
	}

	/**
	 * @return Building specific breakdown
	 */
	public final CityUnrestBreakdownBuilding getCurrentBuilding ()
	{
		return currentBuilding;
	}

	/**
	 * @param building Building specific breakdown
	 */
	@Override
	public final void setCurrentBuilding (final CityUnrestBreakdownBuilding building)
	{
		currentBuilding = building;
	}
}