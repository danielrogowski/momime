package momime.common.calculations;

/**
 * Stores the unrest reduction granted by a particular building
 */
public final class CalculateCityUnrestBreakdown_Building
{
	/** The type of building granting unrest */
	private final String buildingID;

	/** How much unrest the building reduces - stored as a negative value */
	private final int unrestReduction;

	/**
	 * @param aBuildingID The type of building granting unrest
	 * @param anUnrestReduction How much unrest the building reduces - stored as a negative value
	 */
	CalculateCityUnrestBreakdown_Building (final String aBuildingID, final int anUnrestReduction)
	{
		buildingID = aBuildingID;
		unrestReduction = anUnrestReduction;
	}

	/**
	 * @return The type of building granting unrest
	 */
	public final String getBuildingID ()
	{
		return buildingID;
	}

	/**
	 * @return How much unrest the building reduces - stored as a negative value
	 */
	public final int getUnrestReduction ()
	{
		return unrestReduction;
	}
}
