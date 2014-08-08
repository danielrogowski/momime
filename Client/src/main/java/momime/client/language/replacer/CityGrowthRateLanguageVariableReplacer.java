package momime.client.language.replacer;

import momime.client.language.database.v0_9_5.Building;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownBuilding;
import momime.common.internal.CityGrowthRateBreakdownDying;
import momime.common.internal.CityGrowthRateBreakdownGrowing;

/**
 * Language replacer for city growth variables
 */
public final class CityGrowthRateLanguageVariableReplacer extends LanguageVariableReplacerEx<CityGrowthRateBreakdown>
{
	/** Building specific breakdown */
	private CityGrowthRateBreakdownBuilding currentBuilding;
	
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
			case "CURRENT_POPULATION":
				text = getTextUtils ().intToStrCommas (getBreakdown ().getCurrentPopulation ());
				break;
				
			case "MAXIMUM_POPULATION":
				text = getTextUtils ().intToStrCommas (getBreakdown ().getMaximumPopulation ());
				break;
				
			case "CURRENT_POPULATION_DIV_1000":
				text = new Integer (getBreakdown ().getCurrentPopulation () / 1000).toString ();
				break;
				
			case "MAXIMUM_POPULATION_DIV_1000":
				text = new Integer (getBreakdown ().getMaximumPopulation () / 1000).toString ();
				break;
				
			// Growing
			case "BASE_GROWTH_RATE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getBaseGrowthRate ());
				}
				else
					text = null;
				break;
				
			case "RACIAL_GROWTH_MODIFIER":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getRacialGrowthModifier ());
				}
				else
					text = null;
				break;
				
			case "TOTAL_GROWTH_RATE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getTotalGrowthRate ());
				}
				else
					text = null;
				break;
				
			case "CAPPED_GROWTH_RATE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getCappedGrowthRate ());
				}
				else
					text = null;
				break;
				
			// Dying
			case "BASE_DEATH_RATE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownDying)
				{
					final CityGrowthRateBreakdownDying dying = (CityGrowthRateBreakdownDying) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (dying.getBaseDeathRate ());
				}
				else
					text = null;
				break;
				
			case "CITY_DEATH_RATE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownDying)
				{
					final CityGrowthRateBreakdownDying dying = (CityGrowthRateBreakdownDying) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (dying.getCityDeathRate ());
				}
				else
					text = null;
				break;
				
			// Dependant on current building
			case "BUILDING_NAME":
				final Building building = getLanguage ().findBuilding (getCurrentBuilding ().getBuildingID ());
				text = (building == null) ? getCurrentBuilding ().getBuildingID () : building.getBuildingName ();
				break;
				
			case "BUILDING_GROWTH_MODIFIER":
				text = getTextUtils ().intToStrPlusMinus (getCurrentBuilding ().getGrowthRateBonus ());
				break;
				
			default:
				text = null;
		}
		return text;
	}

	/**
	 * @return Building specific breakdown
	 */
	public final CityGrowthRateBreakdownBuilding getCurrentBuilding ()
	{
		return currentBuilding;
	}

	/**
	 * @param building Building specific breakdown
	 */
	public final void setCurrentBuilding (final CityGrowthRateBreakdownBuilding building)
	{
		currentBuilding = building;
	}
}