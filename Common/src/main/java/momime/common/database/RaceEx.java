package momime.common.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		populationTasksMap = getRacePopulationTask ().stream ().collect (Collectors.toMap (p -> p.getPopulationTaskID (), p -> p.getCivilianImageFile ())); 
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
	
	/**
	 * The raceCannotBuild entries only list buildings the race explicitly cannot build, but doesn't take into account other buildings that depend on those buildings.
	 * For example Klackons list that they cannot build a Ship Yard, but that implicitly means they cannot build Martime Guild too.
	 * 
	 * @param db Database, so we can get the full building list
	 * @return Fully resolved list of buildings this race cannot build
	 */
	public final List<String> getRaceCannotBuildFullList (final CommonDatabase db)
	{
		// First copy the list of buildings they explicitly cannot build
		final List<String> cannotBuild = new ArrayList<String> ();
		cannotBuild.addAll (getRaceCannotBuild ());
		
		// Now scan through all buildings looking for any that require a building in the cannotBuild list, and keep doing it until we don't find any more
		boolean keepGoing = true;
		while (keepGoing)
		{
			keepGoing = false;
			
			for (final Building building : db.getBuilding ())
				if ((!cannotBuild.contains (building.getBuildingID ())) && (building.getBuildingPrerequisite ().stream ().anyMatch (b -> cannotBuild.contains (b))))
				{
					cannotBuild.add (building.getBuildingID ());
					keepGoing = true;
				}
		}
		
		return cannotBuild;
	}
}