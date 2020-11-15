package momime.common.database;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds a map over the civlian types, so we can find their images faster
 */
public final class RaceEx extends Race
{
	/** Map of population task IDs to image filenames */
	private Map<String, String> populationTasksMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		populationTasksMap = new HashMap<String, String> ();
		for (final RacePopulationTask task : getRacePopulationTask ())
			populationTasksMap.put (task.getPopulationTaskID (), task.getCivilianImageFile ());
	}
	
	/**
	 * @param populationTaskID Population task ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Filename for the image of this population task
	 * @throws RecordNotFoundException If the populationTaskID doesn't exist
	 */
	public final String findCivilianImageFile (final String populationTaskID, final String caller) throws RecordNotFoundException
	{
		final String found = populationTasksMap.get (populationTaskID);

		if (found == null)
			throw new RecordNotFoundException (RacePopulationTask.class, populationTaskID, caller);
		
		return found;
	}
}