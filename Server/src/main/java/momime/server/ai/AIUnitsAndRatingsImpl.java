package momime.server.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is just to get around the shortcoming that you cannot create arrays of parameterized types
 */
final class AIUnitsAndRatingsImpl extends ArrayList<AIUnitAndRatings> implements AIUnitsAndRatings
{
	/** Need to consider ratings for triemes, galleys, warships, priests, shaman as well as regular combat units - just not settlers, engineers, magic/guardian spirits, those are REALLY non-combat units */
	private final static List<AIUnitType> COMBAT_CAPABLE_UNIT_TYPES = Arrays.asList
		(AIUnitType.COMBAT_UNIT, AIUnitType.TRANSPORT, AIUnitType.PURIFY);
	
	/**
	 * @return Sum of all UCRs in this unit stack added together
	 */
	@Override
	public final int totalCombatUnitCurrentRatings ()
	{
		return stream ().filter (u -> COMBAT_CAPABLE_UNIT_TYPES.contains (u.getAiUnitType ())).mapToInt (u -> u.getCurrentRating ()).sum ();
	}
	
	/**
	 * @return Sum of all UARs in this unit stack added together
	 */
	@Override
	public final int totalCombatUnitAverageRatings ()
	{
		return stream ().filter (u -> COMBAT_CAPABLE_UNIT_TYPES.contains (u.getAiUnitType ())).mapToInt (u -> u.getAverageRating ()).sum ();
	}
}