package momime.common.calculations;

/**
 * Stores the growth rate modifier granted by a particular building
 */
public final class CalculateCityGrowthRateBreakdown_Building
{
	/** The type of building modifying growth rate */
	private final String buildingID;

	/** The modification to the growth rate from this building */
	private final int growthRateModifier;

	/**
	 * @param aBuildingID The type of building modifying growth rate
	 * @param aGrowthRateModifier The modification to the growth rate from this building
	 */
	CalculateCityGrowthRateBreakdown_Building (final String aBuildingID, final int aGrowthRateModifier)
	{
		buildingID = aBuildingID;
		growthRateModifier = aGrowthRateModifier;
	}

	/**
	 * @return The type of building modifying growth rate
	 */
	public final String getBuildingID ()
	{
		return buildingID;
	}

	/**
	 * @return The modification to the growth rate from this building
	 */
	public final int getGrowthRateModifier ()
	{
		return growthRateModifier;
	}
}
