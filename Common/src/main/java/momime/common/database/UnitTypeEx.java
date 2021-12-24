package momime.common.database;

import java.util.Iterator;

/**
 * Unit types for normal, hero and summoned
 */
public final class UnitTypeEx extends UnitType
{
	/**
	 * @param levelNumber Experience level to look for
	 * @return Experience level, or null if it doesn't exist
	 */
	public final ExperienceLevel findExperienceLevel (final int levelNumber)
	{
		ExperienceLevel result = null;
		final Iterator<ExperienceLevel> iter = getExperienceLevel ().iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final ExperienceLevel thisLevel = iter.next ();
			if (thisLevel.getLevelNumber () == levelNumber)
				result = thisLevel;
		}
		return result;
	}
}