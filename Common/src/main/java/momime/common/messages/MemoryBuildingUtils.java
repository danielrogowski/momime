package momime.common.messages;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.CommonDatabaseLookup;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Building;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_4.BuildingPrerequisite;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitPrerequisite;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Methods for working with list of MemoryBuildings
 */
public final class MemoryBuildingUtils
{
	/**
	 * Checks to see if the specified building exists
	 * @param buildingsList List of buildings to search through
	 * @param cityLocation Location of the city to look for
	 * @param buildingID Building to look for
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether or not the specified building exists
	 */
	public static final boolean findBuilding (final List<MemoryBuilding> buildingsList,
		final OverlandMapCoordinates cityLocation, final String buildingID, final Logger debugLogger)
	{
		debugLogger.entering (MemoryBuildingUtils.class.getName (), "findBuilding", new String [] {CoordinatesUtils.overlandMapCoordinatesToString (cityLocation), buildingID});

		boolean found = false;
		final Iterator<MemoryBuilding> iter = buildingsList.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			if ((CoordinatesUtils.overlandMapCoordinatesEqual (cityLocation, thisBuilding.getCityLocation (), true)) && (thisBuilding.getBuildingID ().equals (buildingID)))
				found = true;
		}

		debugLogger.exiting (MemoryBuildingUtils.class.getName (), "findBuilding", found);
		return found;
	}

	/**
	 * @param playerID Player whose building we are looking for
	 * @param buildingID Which building we are looking for
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Location of the first of this type of building we find for this player, or null if they don't have one anywhere (or at least, one we can see)
	 */
	public static final OverlandMapCoordinates findCityWithBuilding (final int playerID, final String buildingID, final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings, final Logger debugLogger)
	{
		debugLogger.entering (MemoryBuildingUtils.class.getName (), "findCityWithBuilding", new String [] {new Integer (playerID).toString (), buildingID});

		OverlandMapCoordinates found = null;
		final Iterator<MemoryBuilding> iter = buildings.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			final OverlandMapCoordinates coords = thisBuilding.getCityLocation ();
			final OverlandMapCityData cityData = map.getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getCityData ();

			if ((thisBuilding.getBuildingID ().equals (buildingID)) && (cityData != null) && (cityData.getCityOwnerID () == playerID) &&
				(cityData.getCityPopulation () != null) && (cityData.getCityPopulation () > 0))

				found = thisBuilding.getCityLocation ();
		}

		debugLogger.exiting (MemoryBuildingUtils.class.getName (), "findCityWithBuilding", found);
		return found;
	}

	/**
	 * Checks to see if this city has the necessary pre-requisite buildings in order to build a particular building, e.g. to build a Farmer's Market we need to have a Granary
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city to test
	 * @param building Building we want to construct
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether or not the city has the necessary pre-requisite buildings
	 */
	public static final boolean meetsBuildingRequirements (final List<MemoryBuilding> buildingsList,
		final OverlandMapCoordinates cityLocation, final Building building, final Logger debugLogger)
	{
		debugLogger.entering (MemoryBuildingUtils.class.getName (), "meetsBuildingRequirements",
			new String [] {CoordinatesUtils.overlandMapCoordinatesToString (cityLocation), building.getBuildingID ()});

		boolean result = true;
		final Iterator<BuildingPrerequisite> iter = building.getBuildingPrerequisite ().iterator ();
		while ((result) && (iter.hasNext ()))
			if (!findBuilding (buildingsList, cityLocation, iter.next ().getPrerequisiteID (), debugLogger))
				result = false;

		debugLogger.exiting (MemoryBuildingUtils.class.getName (), "meetsBuildingRequirements", result);
		return result;
	}

	/**
	 * Checks to see if this city has the necessary pre-requisite buildings in order to build a particular unit, e.g. to build Halbardiers we may need an Armoury
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city to test
	 * @param unit Unit we want to construct
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether or not the city has the necessary pre-requisite buildings
	 */
	public static final boolean meetsUnitRequirements (final List<MemoryBuilding> buildingsList,
		final OverlandMapCoordinates cityLocation, final Unit unit, final Logger debugLogger)
	{
		debugLogger.entering (MemoryBuildingUtils.class.getName (), "meetsUnitRequirements",
			new String [] {CoordinatesUtils.overlandMapCoordinatesToString (cityLocation), unit.getUnitID ()});

		boolean result = true;
		final Iterator<UnitPrerequisite> iter = unit.getUnitPrerequisite ().iterator ();
		while ((result) && (iter.hasNext ()))
			if (!findBuilding (buildingsList, cityLocation, iter.next ().getPrerequisiteID (), debugLogger))
				result = false;

		debugLogger.exiting (MemoryBuildingUtils.class.getName (), "meetsUnitRequirements", result);
		return result;
	}

	/**
	 * Checks to see if any of the other buildings in a city depend on the specified one, e.g. Armoury will return true if we have a Fighter's Guild- used to test if we can sell it
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city containing the building that we want to sell
	 * @param buildingID Building we want to sell
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Building that depends on the specified building, or null if there is none
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	public static final String doAnyBuildingsDependOn (final List<MemoryBuilding> buildingsList, final OverlandMapCoordinates cityLocation,
		final String buildingID, final CommonDatabaseLookup db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (MemoryBuildingUtils.class.getName (), "doAnyBuildingsDependOn",
			new String [] {CoordinatesUtils.overlandMapCoordinatesToString (cityLocation), buildingID});

		String result = null;
		final Iterator<MemoryBuilding> iter = buildingsList.iterator ();

		// Check all buildings at this location
		while ((result == null) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			if (CoordinatesUtils.overlandMapCoordinatesEqual (thisBuilding.getCityLocation (), cityLocation, true))
			{
				// This building is at the same location as the one we're tryign to sell - does it have as
				// a prerequisite the building we are trying to sell?
				final Iterator<BuildingPrerequisite> prereqs = db.findBuilding (thisBuilding.getBuildingID (), "doAnyBuildingsDependOn").getBuildingPrerequisite ().iterator ();
				while ((result == null) && (prereqs.hasNext ()))
					if (prereqs.next ().getPrerequisiteID ().equals (buildingID))
						result = thisBuilding.getBuildingID ();
			}
		}

		debugLogger.exiting (MemoryBuildingUtils.class.getName (), "doAnyBuildingsDependOn", result);
		return result;
	}

	/**
	 * Checks to see if this city contains any buildings that grant free experience to units constructed there (Fighters' Guild or War College)
	 * @param buildingsList List of buildings to search through
	 * @param cityLocation Location of the city to test
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Number of free experience points units constructed here will have
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	public static final int experienceFromBuildings (final List<MemoryBuilding> buildingsList,
		final OverlandMapCoordinates cityLocation, final CommonDatabaseLookup db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (MemoryBuildingUtils.class.getName (), "experienceFromBuildings", CoordinatesUtils.overlandMapCoordinatesToString (cityLocation));

		// Check all buildings at this location
		int result = 0;
		for (final MemoryBuilding thisBuilding : buildingsList)
			if (CoordinatesUtils.overlandMapCoordinatesEqual (thisBuilding.getCityLocation (), cityLocation, true))
			{
				final Integer exp = db.findBuilding (thisBuilding.getBuildingID (), "experienceFromBuildings").getBuildingExperience ();
				if (exp != null)
					result = Math.max (result, exp);
			}

		debugLogger.exiting (MemoryBuildingUtils.class.getName (), "experienceFromBuildings", result);
		return result;
	}

	/**
	 * Checks to see if any buildings give a bonus to production produced from population
	 * Currently the only place this is used is to get Animsts' Guilds to give +1 ration production to farmers
	 * @param buildingsList List of buildings to search through
	 * @param cityLocation Location of the city to test
	 * @param populationTaskID Population task we want to find the bonus for (i.e. Farmer)
	 * @param productionTypeID Production type we want to find the bonus for (i.e. Rations)
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Double the additional per population production granted by buildings
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	public static final int totalBonusProductionPerPersonFromBuildings (final List<MemoryBuilding> buildingsList,
		final OverlandMapCoordinates cityLocation, final String populationTaskID, final String productionTypeID,
		final CommonDatabaseLookup db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (MemoryBuildingUtils.class.getName (), "totalBonusProductionPerPersonFromBuildings", cityLocation.toString ());

		// Check all buildings at this location
		int doubleAmount = 0;
		for (final MemoryBuilding thisBuilding : buildingsList)
			if (CoordinatesUtils.overlandMapCoordinatesEqual (thisBuilding.getCityLocation (), cityLocation, true))

				// Although it would be weird, theoretically you could have multiple entries for the same population task & production type, the XSD doesn't (can't) enforce there being only one
				// The Delphi code searches the full list so we'd better do the same
				for (final BuildingPopulationProductionModifier modifier : db.findBuilding (thisBuilding.getBuildingID (), "totalBonusProductionPerPersonFromBuildings").getBuildingPopulationProductionModifier ())
					if ((populationTaskID.equals (modifier.getPopulationTaskID ())) && (productionTypeID.equals (modifier.getProductionTypeID ())))
						doubleAmount = doubleAmount + modifier.getDoubleAmount ();

		debugLogger.exiting (MemoryBuildingUtils.class.getName (), "totalBonusProductionPerPersonFromBuildings", doubleAmount);
		return doubleAmount;
	}

	/**
	 * Prevent instantiation
	 */
	private MemoryBuildingUtils ()
	{
	}
}
