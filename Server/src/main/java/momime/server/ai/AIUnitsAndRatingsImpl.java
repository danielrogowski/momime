package momime.server.ai;

import java.util.ArrayList;

/**
 * This is just to get around the shortcoming that you cannot create arrays of parameterized types
 */
final class AIUnitsAndRatingsImpl extends ArrayList<AIUnitAndRatings> implements AIUnitsAndRatings
{
	/**
	 * @return Sum of all UCRs in this unit stack added together
	 */
	@Override
	public final int totalCombatUnitCurrentRatings ()
	{
		return stream ().filter (u -> u.getAiUnitType () == AIUnitType.COMBAT_UNIT).mapToInt (u -> u.getCurrentRating ()).sum ();
	}
	
	/**
	 * @return Sum of all UARs in this unit stack added together
	 */
	@Override
	public final int totalCombatUnitAverageRatings ()
	{
		return stream ().filter (u -> u.getAiUnitType () == AIUnitType.COMBAT_UNIT).mapToInt (u -> u.getAverageRating ()).sum ();
	}
}