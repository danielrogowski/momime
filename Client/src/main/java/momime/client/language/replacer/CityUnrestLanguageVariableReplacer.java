package momime.client.language.replacer;

import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.CityUnrestBreakdownBuilding;

/**
 * Language replacer for city unrest calculation variables
 */
public interface CityUnrestLanguageVariableReplacer extends BreakdownLanguageVariableReplacer<CityUnrestBreakdown>
{
	/**
	 * @param building Building specific breakdown
	 */
	public void setCurrentBuilding (final CityUnrestBreakdownBuilding building);
}