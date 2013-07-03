package momime.common.utils;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.ICommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Building;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_4.BuildingPrerequisite;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitPrerequisite;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.OverlandMapCityData;

/**
 * Methods for working with list of MemoryBuildings
 */
public final class MemoryBuildingUtils implements IMemoryBuildingUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MemoryBuildingUtils.class.getName ());
	
	/**
	 * Checks to see if the specified building exists
	 * @param buildingsList List of buildings to search through
	 * @param cityLocation Location of the city to look for
	 * @param buildingID Building to look for
	 * @return Whether or not the specified building exists
	 */
	@Override
	public final boolean findBuilding (final List<MemoryBuilding> buildingsList,
		final OverlandMapCoordinatesEx cityLocation, final String buildingID)
	{
		log.entering (MemoryBuildingUtils.class.getName (), "findBuilding", new String [] {cityLocation.toString (), buildingID});

		boolean found = false;
		final Iterator<MemoryBuilding> iter = buildingsList.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			if ((thisBuilding.getCityLocation ().equals (cityLocation)) && (thisBuilding.getBuildingID ().equals (buildingID)))
				found = true;
		}

		log.exiting (MemoryBuildingUtils.class.getName (), "findBuilding", found);
		return found;
	}

	/**
	 * Removes a building from a list
	 * @param buildingsList List of buildings to remove building from
	 * @param cityLocation Location of the city
	 * @param buildingID Building to remove
	 * @throws RecordNotFoundException If we can't find the requested building
	 */
	@Override
	public final void destroyBuilding (final List<MemoryBuilding> buildingsList,
		final OverlandMapCoordinatesEx cityLocation, final String buildingID)
		throws RecordNotFoundException
	{
		log.entering (MemoryBuildingUtils.class.getName (), "destroyBuilding", new String [] {cityLocation.toString (), buildingID});

		boolean found = false;
		final Iterator<MemoryBuilding> iter = buildingsList.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			if ((thisBuilding.getCityLocation ().equals (cityLocation)) && (thisBuilding.getBuildingID ().equals (buildingID)))
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryBuilding.class.getName (), cityLocation + " - " + buildingID, "destroyBuilding");

		log.exiting (MemoryBuildingUtils.class.getName (), "destroyBuilding");
	}

	/**
	 * @param playerID Player whose building we are looking for
	 * @param buildingID Which building we are looking for
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @return Location of the first of this type of building we find for this player, or null if they don't have one anywhere (or at least, one we can see)
	 */
	@Override
	public final OverlandMapCoordinatesEx findCityWithBuilding (final int playerID, final String buildingID, final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings)
	{
		log.entering (MemoryBuildingUtils.class.getName (), "findCityWithBuilding", new String [] {new Integer (playerID).toString (), buildingID});

		OverlandMapCoordinatesEx found = null;
		final Iterator<MemoryBuilding> iter = buildings.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryBuilding thisBuilding = iter.next ();
			final OverlandMapCoordinatesEx coords = (OverlandMapCoordinatesEx) thisBuilding.getCityLocation ();
			final OverlandMapCityData cityData = map.getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getCityData ();

			if ((thisBuilding.getBuildingID ().equals (buildingID)) && (cityData != null) && (cityData.getCityOwnerID () == playerID) &&
				(cityData.getCityPopulation () != null) && (cityData.getCityPopulation () > 0))

				found = coords;
		}

		log.exiting (MemoryBuildingUtils.class.getName (), "findCityWithBuilding", found);
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
		final OverlandMapCoordinatesEx cityLocation, final Building building)
	{
		log.entering (MemoryBuildingUtils.class.getName (), "meetsBuildingRequirements", new String [] {cityLocation.toString (), building.getBuildingID ()});

		boolean result = true;
		final Iterator<BuildingPrerequisite> iter = building.getBuildingPrerequisite ().iterator ();
		while ((result) && (iter.hasNext ()))
			if (!findBuilding (buildingsList, cityLocation, iter.next ().getPrerequisiteID ()))
				result = false;

		log.exiting (MemoryBuildingUtils.class.getName (), "meetsBuildingRequirements", result);
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
	public final boolean meetsUnitRequirements (final List<MemoryBuilding> buildingsList, final OverlandMapCoordinatesEx cityLocation, final Unit unit)
	{
		log.entering (MemoryBuildingUtils.class.getName (), "meetsUnitRequirements", new String [] {cityLocation.toString (), unit.getUnitID ()});

		boolean result = true;
		final Iterator<UnitPrerequisite> iter = unit.getUnitPrerequisite ().iterator ();
		while ((result) && (iter.hasNext ()))
			if (!findBuilding (buildingsList, cityLocation, iter.next ().getPrerequisiteID ()))
				result = false;

		log.exiting (MemoryBuildingUtils.class.getName (), "meetsUnitRequirements", result);
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
	public final String doAnyBuildingsDependOn (final List<MemoryBuilding> buildingsList, final OverlandMapCoordinatesEx cityLocation,
		final String buildingID, final ICommonDatabase db) throws RecordNotFoundException
	{
		log.entering (MemoryBuildingUtils.class.getName (), "doAnyBuildingsDependOn", new String [] {cityLocation.toString (), buildingID});

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

		log.exiting (MemoryBuildingUtils.class.getName (), "doAnyBuildingsDependOn", result);
		return result;
	}

	/**
	 * @param buildingID Building that is being removed from a city
	 * @param buildingOrUnitID The building or unit that we were trying to build
	 * @param db Lookup lists built over the XML database
	 * @return True if buildingID is a prerequisite for buildingOrUnitID
	 */
	@Override
	public final boolean isBuildingAPrerequisiteFor (final String buildingID, final String buildingOrUnitID,
		final ICommonDatabase db)
	{
		log.entering (MemoryBuildingUtils.class.getName (), "isBuildingAPrerequisiteFor", new String [] {buildingID, buildingOrUnitID});

		boolean result = false;

		// We don't know if it is a building or unit, so look for both
		try
		{
			final Building building = db.findBuilding (buildingOrUnitID, "isBuildingAPrerequisiteFor");
			final Iterator<BuildingPrerequisite> iter = building.getBuildingPrerequisite ().iterator ();
			while ((!result) && (iter.hasNext ()))
				if (iter.next ().getPrerequisiteID ().equals (buildingID))
					result = true;
		}
		catch (final RecordNotFoundException e)
		{
			// Ignore, it could be a unit
		}

		try
		{
			final Unit unit = db.findUnit (buildingOrUnitID, "isBuildingAPrerequisiteFor");
			final Iterator<UnitPrerequisite> iter = unit.getUnitPrerequisite ().iterator ();
			while ((!result) && (iter.hasNext ()))
				if (iter.next ().getPrerequisiteID ().equals (buildingID))
					result = true;
		}
		catch (final RecordNotFoundException e)
		{
			// Ignore, it could be a unit
		}

		log.exiting (MemoryBuildingUtils.class.getName (), "isBuildingAPrerequisiteFor", result);
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
		final OverlandMapCoordinatesEx cityLocation, final ICommonDatabase db) throws RecordNotFoundException
	{
		log.entering (MemoryBuildingUtils.class.getName (), "experienceFromBuildings", cityLocation);

		// Check all buildings at this location
		int result = 0;
		for (final MemoryBuilding thisBuilding : buildingsList)
			if (thisBuilding.getCityLocation ().equals (cityLocation))
			{
				final Integer exp = db.findBuilding (thisBuilding.getBuildingID (), "experienceFromBuildings").getBuildingExperience ();
				if (exp != null)
					result = Math.max (result, exp);
			}

		log.exiting (MemoryBuildingUtils.class.getName (), "experienceFromBuildings", result);
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
		final OverlandMapCoordinatesEx cityLocation, final String populationTaskID, final String productionTypeID,
		final ICommonDatabase db) throws RecordNotFoundException
	{
		log.entering (MemoryBuildingUtils.class.getName (), "totalBonusProductionPerPersonFromBuildings", cityLocation);

		// Check all buildings at this location
		int doubleAmount = 0;
		for (final MemoryBuilding thisBuilding : buildingsList)
			if (thisBuilding.getCityLocation ().equals (cityLocation))

				// Although it would be weird, theoretically you could have multiple entries for the same population task & production type, the XSD doesn't (can't) enforce there being only one
				// The Delphi code searches the full list so we'd better do the same
				for (final BuildingPopulationProductionModifier modifier : db.findBuilding (thisBuilding.getBuildingID (), "totalBonusProductionPerPersonFromBuildings").getBuildingPopulationProductionModifier ())
					if ((populationTaskID.equals (modifier.getPopulationTaskID ())) && (productionTypeID.equals (modifier.getProductionTypeID ())))
						doubleAmount = doubleAmount + modifier.getDoubleAmount ();

		log.exiting (MemoryBuildingUtils.class.getName (), "totalBonusProductionPerPersonFromBuildings", doubleAmount);
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
		log.entering (MemoryBuildingUtils.class.getName (), "findBuildingConsumption", new String [] {building.getBuildingID (), productionTypeID});

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

		log.exiting (MemoryBuildingUtils.class.getName (), "findBuildingConsumption", consumptionAmount);
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
