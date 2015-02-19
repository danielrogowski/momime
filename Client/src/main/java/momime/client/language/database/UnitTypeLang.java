package momime.client.language.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.language.database.v0_9_6.ExperienceLevel;
import momime.client.language.database.v0_9_6.UnitType;

/**
 * Adds a map over the experience levels, so we can find their names faster
 */
public final class UnitTypeLang extends UnitType
{
	/** Map of experience level numbers to names */
	private Map<Integer, String> experienceLevelNamesMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		experienceLevelNamesMap = new HashMap<Integer, String> ();
		for (final ExperienceLevel expLvl : getExperienceLevel ())
			experienceLevelNamesMap.put (expLvl.getLevelNumber (), expLvl.getExperienceLevelName ());
	}
	
	/**
	 * @param expLvl Experience level number
	 * @return Name for this experience level, or replays back the number if the name can't be found
	 */
	public final String findExperienceLevelName (final int expLvl)
	{
		final String name = experienceLevelNamesMap.get (expLvl);
		return (name != null) ? name : new Integer (expLvl).toString ();
	}
}