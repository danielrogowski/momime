package momime.common.calculations;

/**
 * Public interface returned from calculateAllCityProductions () to ensure that only the calculation routine can
 * update the list, and callers can then only read the values out but are unable to modify them
 */
public interface CalculateCityProductionResults
{
	/**
	 * @param productionTypeID Production type to search for
	 * @return Requested production type, or null if the city does not produce or consume any of this type of production
	 */
	public CalculateCityProductionResult findProductionType (final String productionTypeID);
}
