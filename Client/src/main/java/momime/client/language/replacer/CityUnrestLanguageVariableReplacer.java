package momime.client.language.replacer;

import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.CityUnrestBreakdownBuilding;
import momime.common.internal.CityUnrestBreakdownSpell;

/**
 * Language replacer for city unrest calculation variables
 */
public interface CityUnrestLanguageVariableReplacer extends BreakdownLanguageVariableReplacer<CityUnrestBreakdown>
{
	/**
	 * @param building Building specific breakdown
	 */
	public void setCurrentBuilding (final CityUnrestBreakdownBuilding building);

	/**
	 * @param spell Spell specific breakdown
	 */
	public void setCurrentSpell (final CityUnrestBreakdownSpell spell);
}