package momime.server.ai;

import java.util.ArrayList;

/**
 * This is just to get around the shortcoming that you cannot create arrays of parameterized types
 */
final class AIUnitsAndRatingsImpl extends ArrayList<AIUnitAndRatings> implements AIUnitsAndRatings
{
	/**
	 * @return Sum of all UARs in this unit stack added together
	 */
	@Override
	public final int totalAverageRatings ()
	{
		return stream ().mapToInt (u -> u.getAverageRating ()).sum ();
	}
}