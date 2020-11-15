package momime.client.language.replacer;

import momime.common.database.RecordNotFoundException;
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
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	@Override
	public final String determineVariableValue (final String code) throws RecordNotFoundException
	{
		final String text;
		switch (code)
		{
			case "CURRENT_POPULATION_DIV_1000":
				text = Integer.valueOf (getBreakdown ().getPopulation ()).toString ();
				break;

			case "TAX_PERCENTAGE":
				text = Integer.valueOf (getBreakdown ().getTaxPercentage ()).toString ();
				break;

			case "RACE_PERCENTAGE":
				text = Integer.valueOf (getBreakdown ().getRacialPercentage ()).toString ();
				break;

			case "TOTAL_PERCENTAGE":
				text = Integer.valueOf (getBreakdown ().getTotalPercentage ()).toString ();
				break;

			case "BASE_UNREST":
				text = Integer.valueOf (getBreakdown ().getBaseValue ()).toString ();
				break;
				
			case "RACE_LITERAL":
				text = getTextUtils ().intToStrPlusMinus (getBreakdown ().getRacialLiteral ());
				break;
				
			case "RELIGIOUS_BUILDING_REDUCTION":
				text = Integer.valueOf (getBreakdown ().getReligiousBuildingReduction ()).toString ();
				break;
				
			case "RETORT_PERCENTAGE":
				text = Integer.valueOf (getBreakdown ().getReligiousBuildingRetortPercentage ()).toString ();
				break;

			case "RETORT_LIST":
				text = listPickDescriptions (getBreakdown ().getPickIdContributingToReligiousBuildingBonus ());
				break;

			case "RETORT_VALUE":
				text = Integer.valueOf (getBreakdown ().getReligiousBuildingRetortValue ()).toString ();
				break;
				
			case "UNIT_REDUCTION":
				text = Integer.valueOf (getBreakdown ().getUnitReduction ()).toString ();
				break;
				
			case "UNIT_COUNT":
				text = Integer.valueOf (getBreakdown ().getUnitCount ()).toString ();
				break;
				
			case "BASE_TOTAL":
				text = Integer.valueOf (getBreakdown ().getBaseTotal ()).toString ();
				break;
				
			case "MINIMUM_FARMERS":
				text = Integer.valueOf (getBreakdown ().getMinimumFarmers ()).toString ();
				break;
				
			case "TOTAL_AFTER_FARMERS":
				text = Integer.valueOf (getBreakdown ().getTotalAfterFarmers ()).toString ();
				break;
				
			// Dependant on current building
			case "BUILDING_NAME":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (getCurrentBuilding ().getBuildingID (), "determineVariableValue").getBuildingName ());
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