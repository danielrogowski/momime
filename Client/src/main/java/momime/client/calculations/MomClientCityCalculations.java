package momime.client.calculations;

import momime.common.calculations.CalculateCityUnrestBreakdown;

/**
 * Client side only methods dealing with city calculations
 */
public interface MomClientCityCalculations
{
	/**
	 * @param breakdown Results of unrest calculation
	 * @return Readable calculation details
	 */
	public String describeCityUnrestCalculation (final CalculateCityUnrestBreakdown breakdown);
}
