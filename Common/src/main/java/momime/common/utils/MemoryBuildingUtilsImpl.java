package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.BuildingPrerequisite;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitPrerequisite;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.OverlandMapCityData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Methods for working with list of MemoryBuildings
 */
public final class MemoryBuildingUtilsImpl implements MemoryBuildingUtils
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MemoryBuildingUtilsImpl.class);
	
	/**
	 * Checks to see if the specified building exists
	 * @param buildings List of buildings to search through
	 * @param cityLocation Location of the city to look for
	 * @param buildingID Building to look for
	 * @return Whether or not the specified building exists
	 */
	@Override
	public final MemoryBuilding findBuilding (final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final String buildingID)
	{
		log.trace ("Entering findBuilding: " + cityLocation + ", " + cityLocation);

		MemoryBuilding found = null;
		
		final Iterator<MemoryBuilding> iter = buildings.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			if ((thisBuilding.getCityLocation ().equals (cityLocation)) && (thisBuilding.getBuildingID ().equals (buildingID)))
				found = thisBuilding;
		}

		log.trace ("Exiting findBuilding = " + found);
		return found;
	}

	/**
	 * @param buildingURN Building URN to search for
	 * @param buildings List of buildings to search through
	 * @return Building with requested URN, or null if not found
	 */
	@Override
	public final MemoryBuilding findBuildingURN (final int buildingURN, final List<MemoryBuilding> buildings)
	{
		log.trace ("Entering findBuildingURN: Building URN " + buildingURN);

		MemoryBuilding found = null;
		
		final Iterator<MemoryBuilding> iter = buildings.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			if (thisBuilding.getBuildingURN () == buildingURN)
				found = thisBuilding;
		}

		log.trace ("Exiting findBuildingURN = " + found);
		return found;
	}
	
	/**
	 * @param buildingURN Building URN to search for
	 * @param buildings List of buildings to search through
	 * @param caller The routine that was looking for the value
	 * @return Building with requested URN, or null if not found
	 * @throws RecordNotFoundException If building with requested URN is not found
	 */
	@Override
	public final MemoryBuilding findBuildingURN (final int buildingURN, final List<MemoryBuilding> buildings, final String caller)
		throws RecordNotFoundException
	{
		log.trace ("Entering findBuildingURN: Building URN " + buildingURN + ", " + caller);

		final MemoryBuilding result = findBuildingURN (buildingURN, buildings);
					
		if (result == null)
			throw new RecordNotFoundException (MemoryBuilding.class, buildingURN, caller);

		log.trace ("Exiting findBuildingURN = " + result);
		return result;
	}
	
	/**
	 * @param buildingURN Building URN to remove
	 * @param buildings List of buildings to search through
	 * @throws RecordNotFoundException If building with requested URN is not found
	 */
	@Override
	public final void removeBuildingURN (final int buildingURN, final List<MemoryBuilding> buildings)
		throws RecordNotFoundException
	{
		log.trace ("Entering removeBuildingURN: " + buildingURN);

		boolean found = false;
		final Iterator<MemoryBuilding> iter = buildings.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			if (thisBuilding.getBuildingURN () == buildingURN)
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryBuilding.class, buildingURN, "removeBuildingURN");

		log.trace ("Exiting removeBuildingURN");
	}

	/**
	 * @param playerID Player whose building we are looking for
	 * @param buildingID Which building we are looking for
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @return Details of the first of this type of building we find for this player, or null if they don't have one anywhere (or at least, one we can see)
	 */
	@Override
	public final MemoryBuilding findCityWithBuilding (final int playerID, final String buildingID, final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings)
	{
		log.trace ("Entering findCityWithBuilding: Player ID " + playerID + ", " + buildingID);

		MemoryBuilding found = null;
		final Iterator<MemoryBuilding> iter = buildings.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			final MapCoordinates3DEx coords = (MapCoordinates3DEx) thisBuilding.getCityLocation ();
			final OverlandMapCityData cityData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getCityData ();

			if ((thisBuilding.getBuildingID ().equals (buildingID)) && (cityData != null) && (cityData.getCityOwnerID () == playerID))
				found = thisBuilding;
		}

		log.trace ("Exiting findCityWithBuilding = " + found);
		return found;
	}

	/**
	 * Checks to see if this city has the necessary pre-requisite buildings in order to build a particular building, e.g. to build a Farmer's Market we need to have a Granary
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city to test
	 * @param building Building we want to construct
	 * @return Whether or not the city has the necessary pre-requisite buildings
	 */
	@Override
	public final boolean meetsBuildingRequirements (final List<MemoryBuilding> buildingsList,
		final MapCoordinates3DEx cityLocation, final Building building)
	{
		log.trace ("Entering meetsBuildingRequirements: " + cityLocation + ", " + building.getBuildingID ());

		boolean result = true;
		final Iterator<BuildingPrerequisite> iter = building.getBuildingPrerequisite ().iterator ();
		while ((result) && (iter.hasNext ()))
			if (findBuilding (buildingsList, cityLocation, iter.next ().getPrerequisiteID ()) == null)
				result = false;

		log.trace ("Exiting meetsBuildingRequirements = " + result);
		return result;
	}

	/**
	 * Checks to see if this city has the necessary pre-requisite buildings in order to build a particular unit, e.g. to build Halbardiers we may need an Armoury
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city to test
	 * @param unit Unit we want to construct
	 * @return Whether or not the city has the necessary pre-requisite buildings
	 */
	@Override
	public final boolean meetsUnitRequirements (final List<MemoryBuilding> buildingsList, final MapCoordinates3DEx cityLocation, final Unit unit)
	{
		log.trace ("Entering meetsUnitRequirements: " + cityLocation + ", " + unit.getUnitID ());

		boolean result = true;
		final Iterator<UnitPrerequisite> iter = unit.getUnitPrerequisite ().iterator ();
		while ((result) && (iter.hasNext ()))
			if (findBuilding (buildingsList, cityLocation, iter.next ().getPrerequisiteID ()) == null)
				result = false;

		log.trace ("Exiting meetsUnitRequirements = " + result);
		return result;
	}

	/**
	 * Checks to see if any of the other buildings in a city depend on the specified one, e.g. Armoury will return true if we have a Fighter's Guild- used to test if we can sell it
	 * @param buildingsList List of buildings to check against
	 * @param cityLocation Location of the city containing the building that we want to sell
	 * @param buildingID Building we want to sell
	 * @param db Lookup lists built over the XML database
	 * @return Building that depends on the specified building, or null if there is none
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	@Override
	public final String doAnyBuildingsDependOn (final List<MemoryBuilding> buildingsList, final MapCoordinates3DEx cityLocation,
		final String buildingID, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering doAnyBuildingsDependOn: " + cityLocation + ", " + buildingID);

		String result = null;
		final Iterator<MemoryBuilding> iter = buildingsList.iterator ();

		// Check all buildings at this location
		while ((result == null) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			if (thisBuilding.getCityLocation ().equals (cityLocation))
			{
				// This building is at the same location as the one we're tryign to sell - does it have as
				// a prerequisite the building we are trying to sell?
				final Iterator<BuildingPrerequisite> prereqs = db.findBuilding (thisBuilding.getBuildingID (), "doAnyBuildingsDependOn").getBuildingPrerequisite ().iterator ();
				while ((result == null) && (prereqs.hasNext ()))
					if (prereqs.next ().getPrerequisiteID ().equals (buildingID))
						result = thisBuilding.getBuildingID ();
			}
		}

		log.trace ("Exiting doAnyBuildingsDependOn = " + result);
		return result;
	}

	/**
	 * @param buildingBeingRemoved Building that is being removed from a city
	 * @param buildingID The building that we were trying to build
	 * @param db Lookup lists built over the XML database
	 * @return True if buildingBeingRemoved is a prerequisite for buildingID
	 * @throws RecordNotFoundException If buildingID can't be found in the db
	 */
	@Override
	public final boolean isBuildingAPrerequisiteForBuilding (final String buildingBeingRemoved, final String buildingID, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering isBuildingAPrerequisiteForBuilding: " + buildingBeingRemoved + ", " + buildingID);

		boolean result = false;
		final Building building = db.findBuilding (buildingID, "isBuildingAPrerequisiteForBuilding");
		final Iterator<BuildingPrerequisite> iter = building.getBuildingPrerequisite ().iterator ();
		while ((!result) && (iter.hasNext ()))
			if (iter.next ().getPrerequisiteID ().equals (buildingBeingRemoved))
				result = true;

		log.trace ("Exiting isBuildingAPrerequisiteForBuilding = " + result);
		return result;
	}

	/**
	 * @param buildingBeingRemoved Building that is being removed from a city
	 * @param unitID The unit that we were trying to build
	 * @param db Lookup lists built over the XML database
	 * @return True if buildingBeingRemoved is a prerequisite for buildingID
	 * @throws RecordNotFoundException If unitID can't be found in the db
	 */
	@Override
	public final boolean isBuildingAPrerequisiteForUnit (final String buildingBeingRemoved, final String unitID, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering isBuildingAPrerequisiteForUnit: " + buildingBeingRemoved + ", " + unitID);

		boolean result = false;
		final Unit unit = db.findUnit (unitID, "isBuildingAPrerequisiteForUnit");
		final Iterator<UnitPrerequisite> iter = unit.getUnitPrerequisite ().iterator ();
		while ((!result) && (iter.hasNext ()))
			if (iter.next ().getPrerequisiteID ().equals (buildingBeingRemoved))
				result = true;

		log.trace ("Exiting isBuildingAPrerequisiteForUnit = " + result);
		return result;
	}

	/**
	 * Checks to see if this city contains any buildings that grant free experience to units constructed there (Fighters' Guild or War College)
	 * @param buildingsList List of buildings to search through
	 * @param cityLocation Location of the city to test
	 * @param db Lookup lists built over the XML database
	 * @return Number of free experience points units constructed here will have
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	@Override
	public final int experienceFromBuildings (final List<MemoryBuilding> buildingsList,
		final MapCoordinates3DEx cityLocation, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering experienceFromBuildings: " + cityLocation);

		// Check all buildings at this location
		int result = 0;
		for (final MemoryBuilding thisBuilding : buildingsList)
			if (thisBuilding.getCityLocation ().equals (cityLocation))
			{
				final Integer exp = db.findBuilding (thisBuilding.getBuildingID (), "experienceFromBuildings").getBuildingExperience ();
				if (exp != null)
					result = Math.max (result, exp);
			}

		log.trace ("Exiting experienceFromBuildings = " + result);
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
	 * @return Double the additional per population production granted by buildings
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	@Override
	public final int totalBonusProductionPerPersonFromBuildings (final List<MemoryBuilding> buildingsList,
		final MapCoordinates3DEx cityLocation, final String populationTaskID, final String productionTypeID,
		final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering totalBonusProductionPerPersonFromBuildings: " + cityLocation);

		// Check all buildings at this location
		int doubleAmount = 0;
		for (final MemoryBuilding thisBuilding : buildingsList)
			if (thisBuilding.getCityLocation ().equals (cityLocation))

				// Although it would be weird, theoretically you could have multiple entries for the same population task & production type, the XSD doesn't (can't) enforce there being only one
				// The Delphi code searches the full list so we'd better do the same
				for (final BuildingPopulationProductionModifier modifier : db.findBuilding (thisBuilding.getBuildingID (), "totalBonusProductionPerPersonFromBuildings").getBuildingPopulationProductionModifier ())
					if ((populationTaskID.equals (modifier.getPopulationTaskID ())) && (productionTypeID.equals (modifier.getProductionTypeID ())))
						doubleAmount = doubleAmount + modifier.getDoubleAmount ();

		log.trace ("Exiting totalBonusProductionPerPersonFromBuildings = " + doubleAmount);
		return doubleAmount;
	}

	/**
	 * @param building Building we want the consumption of
	 * @param productionTypeID Production type that we want the consumption of
	 * @return The amount of this production type that this building consumes; these are positive undoubled values
	 * @throws MomException If we find a building consumption that isn't a multiple of 2
	 */
	@Override
	public final int findBuildingConsumption (final Building building, final String productionTypeID)
		throws MomException
	{
		log.trace ("Entering findBuildingConsumption: " +building.getBuildingID () + ", " + productionTypeID);

		// Find the right type of production
		int consumptionAmount = 0;
		final Iterator<BuildingPopulationProductionModifier> iter = building.getBuildingPopulationProductionModifier ().iterator ();
		while ((consumptionAmount == 0) && (iter.hasNext ()))
		{
			final BuildingPopulationProductionModifier production = iter.next ();

			if ((production.getProductionTypeID ().equals (productionTypeID)) && (production.getPopulationTaskID () == null) && (production.getDoubleAmount () != null))

				// Also we only want consumptions, not productions
				if (production.getDoubleAmount () < 0)
				{
					// Consumption - must be an exact multiple of 2
					final int doubleAmount = -production.getDoubleAmount ();
					if (doubleAmount % 2 == 0)
						consumptionAmount = doubleAmount / 2;
					else
						throw new MomException ("Building " + building.getBuildingID () + " has a consumption value for production type " + productionTypeID + " that is not a multiple of 2");
				}
		}

		log.trace ("Exiting findBuildingConsumption = " + consumptionAmount);
		return consumptionAmount;
	}

	/**
	 * @param building Building being sold
	 * @return Gold obtained from selling building; will be 0 for special buildings such as trade goods
	 */
	@Override
	public final int goldFromSellingBuilding (final Building building)
	{
		final int gold;
		if (building.getProductionCost () == null)
			gold = 0;
		else
			gold = building.getProductionCost () / 3;

		return gold;
	}
}