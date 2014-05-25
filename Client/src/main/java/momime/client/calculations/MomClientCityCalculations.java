package momime.client.calculations;

import momime.common.MomException;
import momime.common.calculations.CalculateCityGrowthRateBreakdown;
import momime.common.calculations.CalculateCityProductionResult;
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
	
	/**
	 * @param breakdown Results of growth calculation
	 * @return Readable calculation details
	 */
	public String describeCityGrowthRateCalculation (final CalculateCityGrowthRateBreakdown breakdown);
	
	/**
	 * @param calc Results of production calculation
	 * @return Readable calculation details
	 * @throws MomException If we find a breakdown entry that we don't know how to describe
	 */
	public String describeCityProductionCalculation (final CalculateCityProductionResult calc) throws MomException;
}
