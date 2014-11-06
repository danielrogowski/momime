package momime.common.utils;

import java.util.List;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Methods for working with list of MemoryBuildings
 */
public interface MemoryBuildingUtils
{
	/**
	 * Searches for a building by its details
	 * 
	 * @param buildings List of buildings to search through
	 * @param cityLocation Location of the city to look for
	 * @param buildingID Building to look for
	 * @return Building with requested details, or null if not found
	 */
	public MemoryBuilding findBuilding (final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final String buildingID);

	/**
	 * @param buildingURN Building URN to search for
	 * @param buildings List of buildings to search through
	 * @return Building with requested URN, or null if not found
	 */
	public MemoryBuilding findBuildingURN (final int buildingURN, final List<MemoryBuilding> buildings);
	
	/**
	 * @param buildingURN Building URN to search for
	 * @param buildings List of buildings to search through
	 * @param caller The routine that was looking for the value
	 * @return Building with requested URN, or null if not found
	 * @throws RecordNotFoundException If building with requested URN is not found
	 */
	public MemoryBuilding findBuildingURN (final int buildingURN, final List<MemoryBuilding> buildings, final String caller)
		throws RecordNotFoundException;
	
	/**
	 * @param buildingURN Building URN to remove
	 * @param buildings List of buildings to search through
	 * @throws RecordNotFoundException If building with requested URN is not found
	 */
	public void removeBuildingURN (final int buildingURN, final List<MemoryBuilding> buildings)
		throws RecordNotFoundException;

	/**
	 * @param playerID Player whose building we are looking for
	 * @param buildingID Which building we are looking for
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @return Details of the first of this type of building we find for this player, or null if they don't have one anywhere (or at least, one we can see)
	 */
	public MemoryBuilding findCityWithBuilding (final int playerID, final String buildingID, final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings);

	/**
	 * Checks to see if this city has the necessary pre-requisite buildings in order to build a particular building, e.g. to build a Farmer's Market we need to have a Granary
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city to test
	 * @param building Building we want to construct
	 * @return Whether or not the city has the necessary pre-requisite buildings
	 */
	public boolean meetsBuildingRequirements (final List<MemoryBuilding> buildingsList,
		final MapCoordinates3DEx cityLocation, final Building building);

	/**
	 * Checks to see if this city has the necessary pre-requisite buildings in order to build a particular unit, e.g. to build Halbardiers we may need an Armoury
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city to test
	 * @param unit Unit we want to construct
	 * @return Whether or not the city has the necessary pre-requisite buildings
	 */
	public boolean meetsUnitRequirements (final List<MemoryBuilding> buildingsList,
		final MapCoordinates3DEx cityLocation, final Unit unit);

	/**
	 * Checks to see if any of the other buildings in a city depend on the specified one, e.g. Armoury will return true if we have a Fighter's Guild- used to test if we can sell it
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city containing the building that we want to sell
	 * @param buildingID Building we want to sell
	 * @param db Lookup lists built over the XML database
	 * @return Building that depends on the specified building, or null if there is none
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	public String doAnyBuildingsDependOn (final List<MemoryBuilding> buildingsList, final MapCoordinates3DEx cityLocation,
		final String buildingID, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param buildingBeingRemoved Building that is being removed from a city
	 * @param buildingID The building that we were trying to build
	 * @param db Lookup lists built over the XML database
	 * @return True if buildingBeingRemoved is a prerequisite for buildingID
	 * @throws RecordNotFoundException If buildingID can't be found in the db
	 */
	public boolean isBuildingAPrerequisiteForBuilding (final String buildingBeingRemoved, final String buildingID, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param buildingBeingRemoved Building that is being removed from a city
	 * @param unitID The unit that we were trying to build
	 * @param db Lookup lists built over the XML database
	 * @return True if buildingBeingRemoved is a prerequisite for unitID
	 * @throws RecordNotFoundException If unitID can't be found in the db
	 */
	public boolean isBuildingAPrerequisiteForUnit (final String buildingBeingRemoved, final String unitID, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * Checks to see if this city contains any buildings that grant free experience to units constructed there (Fighters' Guild or War College)
	 * @param buildingsList List of buildings to search through
	 * @param cityLocation Location of the city to test
	 * @param db Lookup lists built over the XML database
	 * @return Number of free experience points units constructed here will have
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	public int experienceFromBuildings (final List<MemoryBuilding> buildingsList,
		final MapCoordinates3DEx cityLocation, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * Checks to see if any buildings give a bonus to production produced from population
	 * Currently the only place this is used is to get Animsts' Guilds to give +1 ration production to farmers
	 * @param buildingsList List of buildings to search through
	 * @param cityLocation Location of the city to test
	 * @param populationTaskID Population task we want to find the bonus for (i.e. Farmer)
	 * @param productionTypeID Production type we want to find the bonus for (i.e. Rations)
	 * @param db Lookup lists built over the XML database
	 * @return Double the additional per population production granted by buildings
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	public int totalBonusProductionPerPersonFromBuildings (final List<MemoryBuilding> buildingsList,
		final MapCoordinates3DEx cityLocation, final String populationTaskID, final String productionTypeID,
		final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param building Building we want the consumption of
	 * @param productionTypeID Production type that we want the consumption of
	 * @return The amount of this production type that this building consumes; these are positive undoubled values
	 * @throws MomException If we find a building consumption that isn't a multiple of 2
	 */
	public int findBuildingConsumption (final Building building, final String productionTypeID)
		throws MomException;

	/**
	 * @param building Building being sold
	 * @return Gold obtained from selling building; will be 0 for special buildings such as trade goods
	 */
	public int goldFromSellingBuilding (final Building building);
}