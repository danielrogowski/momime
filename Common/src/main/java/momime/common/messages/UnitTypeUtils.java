package momime.common.messages;

import java.util.Iterator;

import momime.common.database.v0_9_4.ExperienceLevel;
import momime.common.database.v0_9_4.UnitType;

/**
 * Helper methods for dealing with unit types
 */
public final class UnitTypeUtils
{
	/**
	 * @param unitType Unit type to search under
	 * @param levelNumber Experience level to look for
	 * @return Experience level, or null if it doesn't exist
	 */
	public final static ExperienceLevel findExperienceLevel (final UnitType unitType, final int levelNumber)
	{
		ExperienceLevel result = null;
		final Iterator<ExperienceLevel> iter = unitType.getExperienceLevel ().iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final ExperienceLevel thisLevel = iter.next ();
			if (thisLevel.getLevelNumber () == levelNumber)
				result = thisLevel;
		}
		return result;
	}

	/**
	 * Prevent instantiation
	 */
	private UnitTypeUtils ()
	{
	}
}
