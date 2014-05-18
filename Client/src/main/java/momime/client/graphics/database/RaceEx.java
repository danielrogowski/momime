package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.Race;
import momime.client.graphics.database.v0_9_5.RacePopulationTask;

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
	 * @return Filename for the image of this population task; or null if population task ID not found
	 */
	public final String findCivilianImageFile (final String populationTaskID)
	{
		return populationTasksMap.get (populationTaskID);
	}
}
