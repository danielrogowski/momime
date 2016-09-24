package momime.server.ai;

import java.util.List;

/**
 * This is just to get around the shortcoming that you cannot create arrays of parameterized types
 */
interface AIUnitsAndRatings extends List<AIUnitAndRatings>
{
	/**
	 * @return Sum of all UARs in this unit stack added together
	 */
	public int totalAverageRatings ();
}