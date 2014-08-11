package momime.client.language.replacer;

import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownBuilding;

/**
 * Language replacer for city growth variables
 */
public interface CityGrowthRateLanguageVariableReplacer extends BreakdownLanguageVariableReplacer<CityGrowthRateBreakdown>
{
	/**
	 * @param building Building specific breakdown
	 */
	public void setCurrentBuilding (final CityGrowthRateBreakdownBuilding building);
}