package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.ExperienceLevel;
import momime.client.graphics.database.v0_9_5.UnitType;

/**
 * Adds a map over the experience levels, so we can find their images faster
 */
public final class UnitTypeEx extends UnitType
{
	/** Map of experience level numbers to image filenames */
	private Map<Integer, String> experienceLevelImagesMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		experienceLevelImagesMap = new HashMap<Integer, String> ();
		for (final ExperienceLevel expLvl : getExperienceLevel ())
			experienceLevelImagesMap.put (expLvl.getLevelNumber (), expLvl.getExperienceLevelImageFile ());
	}
	
	/**
	 * @param expLvl Experience level number
	 * @return Filename for the image of this experience level; or null if no image exists for it
	 */
	public final String findExperienceLevelImageFile (final int expLvl)
	{
		return experienceLevelImagesMap.get (expLvl);
	}
}