package momime.client.language.replacer;

import momime.common.database.RecordNotFoundException;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownBuilding;
import momime.common.internal.CityGrowthRateBreakdownDying;
import momime.common.internal.CityGrowthRateBreakdownGrowing;

/**
 * Language replacer for city growth variables
 */
public final class CityGrowthRateLanguageVariableReplacerImpl extends BreakdownLanguageVariableReplacerImpl<CityGrowthRateBreakdown> implements CityGrowthRateLanguageVariableReplacer
{
	/** Building specific breakdown */
	private CityGrowthRateBreakdownBuilding currentBuilding;
	
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
			case "CURRENT_POPULATION":
				text = getTextUtils ().intToStrCommas (getBreakdown ().getCurrentPopulation ());
				break;
				
			case "MAXIMUM_POPULATION":
				text = getTextUtils ().intToStrCommas (getBreakdown ().getMaximumPopulation ());
				break;
				
			case "CURRENT_POPULATION_DIV_1000":
				text = Integer.valueOf (getBreakdown ().getCurrentPopulation () / 1000).toString ();
				break;
				
			case "MAXIMUM_POPULATION_DIV_1000":
				text = Integer.valueOf (getBreakdown ().getMaximumPopulation () / 1000).toString ();
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
				
			case "STREAM_OF_LIFE_GROWTH_RATE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getTotalGrowthRateAfterStreamOfLife ());
				}
				else
					text = null;
				break;
				
			case "HOUSING_PERCENTAGE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = Integer.valueOf (growing.getHousingPercentageBonus ()).toString ();
				}
				else
					text = null;
				break;
				
			case "DARK_RITUALS_PERCENTAGE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = Integer.valueOf (growing.getDarkRitualsPercentagLoss ()).toString ();
				}
				else
					text = null;
				break;

			case "PERCENTAGE_MODIFIERS_PERCENTAGE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = Integer.valueOf (growing.getHousingPercentageBonus () - growing.getDarkRitualsPercentagLoss ()).toString ();
				}
				else
					text = null;
				break;
				
			case "PERCENTAGE_MODIFIERS":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getPercentageModifiers ());
				}
				else
					text = null;
				break;
				
			case "TOTAL_GROWTH_RATE_INCLUDING_PERCENTAGE_MODIFIERS":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getTotalGrowthRateIncludingPercentageModifiers ());
				}
				else
					text = null;
				break;
				
			// AI players
			case "AI_POPULATION_GROWTH_RATE_MULTIPLIER":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = Integer.valueOf (growing.getDifficultyLevelMultiplier ()).toString ();
				}
				else
					text = null;
				break;
				
			case "TOTAL_GROWTH_RATE_ADJUSTED_FOR_DIFFICULTY_LEVEL":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getTotalGrowthRateAdjustedForDifficultyLevel ());
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

			// Applicable to both growing + dying
			case "CAPPED_RATE":
				if (getBreakdown () instanceof CityGrowthRateBreakdownGrowing)
				{
					final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) getBreakdown ();
					text = getTextUtils ().intToStrPlusMinus (growing.getCappedTotal ());
				}
				else
					text = null;
				break;
				
			// Dependant on current building
			case "BUILDING_NAME":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (getCurrentBuilding ().getBuildingID (), "determineVariableValue").getBuildingName ());
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
	@Override
	public final void setCurrentBuilding (final CityGrowthRateBreakdownBuilding building)
	{
		currentBuilding = building;
	}
}