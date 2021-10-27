package momime.common.utils;

import java.util.HashMap;
import java.util.Map;

import momime.common.database.UnitSkillComponent;

/**
 * Breakdown about where a modified skill came from, and the breakdown of how its value was totalled up
 */
public final class UnitSkillValueBreakdown
{
	/** The source of the original skill - for skills with values you can normally tell that from the values breakdown, but valueless skills need this separate indicator */
	private final UnitSkillComponent source;
	
	/** Breakdown of how the skill value total was arrived at - say unit originally had value 2, then +2 from some spell, +1 from weapon grade, or so on */
	private final Map<UnitSkillComponent, Integer> components  = new HashMap<UnitSkillComponent, Integer> ();

	/**
	 * @param aSource The source of the original skill - for skills with values you can normally tell that from the values breakdown, but valueless skills need this separate indicator
	 */
	public UnitSkillValueBreakdown (final UnitSkillComponent aSource)
	{
		source = aSource;
	}

	/**
	 * @return The source of the original skill - for skills with values you can normally tell that from the values breakdown, but valueless skills need this separate indicator
	 */
	public final UnitSkillComponent getSource ()
	{
		return source;
	}
	
	/**
	 * @return Breakdown of how the skill value total was arrived at - say unit originally had value 2, then +2 from some spell, +1 from weapon grade, or so on
	 */
	public final Map<UnitSkillComponent, Integer> getComponents ()
	{
		return components;
	}
}