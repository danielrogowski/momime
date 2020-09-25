package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_9.ExperienceLevel;
import momime.client.graphics.database.v0_9_9.UnitType;
import momime.common.database.RecordNotFoundException;

/**
 * Adds a map over the experience levels, so we can find their images faster
 */
public final class UnitTypeGfx extends UnitType
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
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Filename for the image of this experience level
	 * @throws RecordNotFoundException If the experience level doesn't exist
	 */
	public final String findExperienceLevelImageFile (final int expLvl, final String caller) throws RecordNotFoundException
	{
		final String found = experienceLevelImagesMap.get (expLvl);

		if (found == null)
			throw new RecordNotFoundException (ExperienceLevel.class, expLvl, caller);
		
		return found;
	}
}