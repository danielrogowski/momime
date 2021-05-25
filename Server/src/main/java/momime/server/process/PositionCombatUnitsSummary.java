package momime.server.process;

/**
 * Holds results of positionCombatUnits method
 */
final class PositionCombatUnitsSummary
{
	/** Number of units that were placed */
	private final int unitCount;
	
	/** Most expensive unit that was placed */
	private final int mostExpensiveUnitCost;
	
	/**
	 * @param aUnitCount Number of units that were placed
	 * @param aMostExpensiveUnitCost Most expensive unit that was placed
	 */
	PositionCombatUnitsSummary (final int aUnitCount, final int aMostExpensiveUnitCost)
	{
		unitCount = aUnitCount;
		mostExpensiveUnitCost = aMostExpensiveUnitCost;
	}

	/**
	 * @return Number of units that were placed
	 */
	final int getUnitCount ()
	{
		return unitCount;
	}
	
	/**
	 * @return Most expensive unit that was placed
	 */
	final int getMostExpensiveUnitCost ()
	{
		return mostExpensiveUnitCost;
	}
}