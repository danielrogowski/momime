package momime.server.calculations;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.calculations.IMomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_4.BuildingPrerequisite;
import momime.common.database.v0_9_4.RaceCannotBuild;
import momime.common.database.v0_9_4.RacePopulationTask;
import momime.common.database.v0_9_4.RacePopulationTaskProduction;
import momime.common.messages.IMemoryBuildingUtils;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.CitySize;
import momime.server.database.v0_9_4.Race;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server only calculations pertaining to cities, e.g. calculating resources gathered from within the city radius
 */
public final class MomServerCityCalculations implements IMomServerCityCalculations
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomServerCityCalculations.class.getName ());
	
	/** Memory building utils */
	private IMemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private IMomCityCalculations cityCalculations;
	
	/**
	 * Could do this inside chooseCityLocation, however declaring it separately avoids having to repeat this over
	 * and over when adding multiple new cities at the start of the game
	 *
	 * Currently this doesn't take any race restrictions into account (i.e. if a certain race can't build one of the
	 * buildings that gives a food bonus, or a pre-requisite of it) or similarly tile type restrictions
	 *
	 * @param db Lookup lists built over the XML database
	 * @return Amount of free food we'll get from building all buildings, with the default server database, this gives 5 = 2 from Granary + 3 from Farmers' Market
	 * @throws MomException If the food production values from the XML database aren't multiples of 2
	 */
	@Override
	public final int calculateTotalFoodBonusFromBuildings (final ServerDatabaseEx db)
		throws MomException
	{
		log.entering (MomServerCityCalculations.class.getName (), "calculateTotalFoodBonusFromBuildings");
		int doubleTotalFood = 0;

		for (final Building thisBuilding : db.getBuilding ())
			for (final BuildingPopulationProductionModifier productionModifier : thisBuilding.getBuildingPopulationProductionModifier ())

				// Only pick out production modifiers which come from the building by itself with no effect from the number of population
				if ((productionModifier.getPopulationTaskID () == null) && (productionModifier.getProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD)))
					doubleTotalFood = doubleTotalFood + productionModifier.getDoubleAmount ();

		if (doubleTotalFood % 2 != 0)
			throw new MomException ("CalculateTotalFoodBonusFromBuildings: Expect answer to be an exact multiple of 2, but got " + doubleTotalFood);

		final int totalFood = doubleTotalFood / 2;

		log.exiting (MomServerCityCalculations.class.getName (), "calculateTotalFoodBonusFromBuildings", totalFood);
		return totalFood;
	}

	/**
	 * @param map True terrain
	 * @param buildings True list of buildings
	 * @param cityLocation Location of the city to calculate for
	 * @param db Lookup lists built over the XML database
	 * @return Rations produced by one farmer in this city
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 * @throws MomException If the city's race has no farmers defined or those farmers have no ration production defined
	 */
	@Override
	public final int calculateDoubleFarmingRate (final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings, final OverlandMapCoordinatesEx cityLocation, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException
	{
		log.entering (MomServerCityCalculations.class.getName (), "calculateDoubleFarmingRate", cityLocation);

		final OverlandMapCityData cityData = map.getPlane ().get (cityLocation.getPlane ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		// Find the farmers for this race
		RacePopulationTask farmer = null;
		final Iterator<RacePopulationTask> taskIter = db.findRace (cityData.getCityRaceID (), "calculateDoubleFarmingRate").getRacePopulationTask ().iterator ();
		while ((farmer == null) && (taskIter.hasNext ()))
		{
			final RacePopulationTask thisTask = taskIter.next ();
			if (thisTask.getPopulationTaskID ().equals (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER))
				farmer = thisTask;
		}

		if (farmer == null)
			throw new MomException ("calculateDoubleFarmingRate: Race " + cityData.getCityRaceID () + " has no farmers defined");

		// Find how many rations each farmer produces
		RacePopulationTaskProduction rations = null;;
		final Iterator<RacePopulationTaskProduction> prodIter = farmer.getRacePopulationTaskProduction ().iterator ();
		while ((rations == null) && (prodIter.hasNext ()))
		{
			final RacePopulationTaskProduction thisProd = prodIter.next ();
			if (thisProd.getProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS))
				rations = thisProd;
		}

		if (rations == null)
			throw new MomException ("calculateDoubleFarmingRate: Farmers for race " + cityData.getCityRaceID () + " do not produce any rations");

		// Every race has farmers, and every farmer produces rations, so the chain of records must exist
		final int doubleFarmingRate = rations.getDoubleAmount () +

			// Bump up farming rate if we have an Animists' guild
			getMemoryBuildingUtils ().totalBonusProductionPerPersonFromBuildings (buildings, cityLocation,
				CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, db);

		log.exiting (MomServerCityCalculations.class.getName (), "calculateDoubleFarmingRate", doubleFarmingRate);
		return doubleFarmingRate;
	}

	/**
	 * Updates the city size ID and minimum number of farmers
	 *
	 * After updating city population, must call the routines in this sequence:
	 * 1) City size ID & Minimum farmers
	 * 2) Rebels
	 * 3) Check not too many optional farmers
	 *
	 * For changing the tax rate (or anything else that might alter the number of rebels, e.g. units moving in/out), just need to call the last two
	 *
	 * This is called on the TrueMap to update the values there, then the calling routine checks each player's Fog of War to see
	 * if they can see the city, and if so then sends them the updated values
	 *
	 * @param players Pre-locked list of players in the game
	 * @param map True terrain
	 * @param buildings True list of buildings
	 * @param cityLocation Location of the city to update
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the player who owns the city
	 * @throws MomException If any of a number of expected items aren't found in the database
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 */
	@Override
	public final void calculateCitySizeIDAndMinimumFarmers (final List<PlayerServerDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings, final OverlandMapCoordinatesEx cityLocation,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (MomServerCityCalculations.class.getName (), "calculateCitySizeIDAndMinimumFarmers", cityLocation);

		final OverlandMapCityData cityData = map.getPlane ().get (cityLocation.getPlane ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		// First work out the Size ID - There should only be one entry in the DB which matches
		boolean found = false;
		final Iterator<CitySize> iter = db.getCitySize ().iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final CitySize thisSize = iter.next ();

			// 0 indicates that there is no minimum/maximum
			if (((thisSize.getCitySizeMinimum () == null) || (cityData.getCityPopulation () >= thisSize.getCitySizeMinimum ())) &&
				((thisSize.getCitySizeMaximum () == null) || (cityData.getCityPopulation () <= thisSize.getCitySizeMaximum ())))
			{
				cityData.setCitySizeID (thisSize.getCitySizeID ());
				found = true;
			}
		}

		if (!found)
			throw new MomException ("No city size ID is defined for cities of size " + cityData.getCityPopulation ());

		// Next work out how many farmers we need to support the population (strategy guide p187)
		// Subtract 2 for each wild game resource in the city area and 2 for granary, 3 for farmer's market, 2 for foresters' guild
		final PlayerServerDetails cityOwner = MultiplayerSessionServerUtils.findPlayerWithID (players, cityData.getCityOwnerID (), "calculateCitySizeIDAndMinimumFarmers");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();

		final int rationsNeeded = (cityData.getCityPopulation () / 1000) - getCityCalculations ().calculateSingleCityProduction
			(players, map, buildings, cityLocation, priv.getTaxRateID (), sd, false,
				db, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS);

		// See if we need any farmers at all
		if (rationsNeeded <= 0)
			cityData.setMinimumFarmers (0);
		else
		{
			// Get the farming rate for this race
			final int doubleFarmingRate = calculateDoubleFarmingRate (map, buildings, cityLocation, db);

			// Now can do calculation, round up
			cityData.setMinimumFarmers (((rationsNeeded * 2) + doubleFarmingRate - 1) / doubleFarmingRate);
		}

		log.exiting (MomServerCityCalculations.class.getName (), "calculateCitySizeIDAndMinimumFarmers",
			new String [] {cityData.getCitySizeID (), new Integer (cityData.getMinimumFarmers ()).toString ()});
	}

	/**
	 * Checks that minimum farmers + optional farmers + rebels is not > population
	 *
	 * After updating city population, must call the routines in this sequence:
	 * 1) City size ID & Minimum farmers
	 * 2) Rebels
	 * 3) Check not too many optional farmers
	 *
	 * For changing the tax rate (or anything else that might alter the number of rebels, e.g. units moving in/out), just need to call the last two
	 *
	 * This is called on the TrueMap to update the values there, then the calling routine checks each player's Fog of War to see
	 * if they can see the city, and if so then sends them the updated values
	 *
	 * @param city City data
	 * @throws MomException If we end up setting optional farmers to negative, which indicates that minimum farmers and/or rebels have been previously calculated incorrectly
	 */
	@Override
	public final void ensureNotTooManyOptionalFarmers (final OverlandMapCityData city)
		throws MomException
	{
		log.entering (MomServerCityCalculations.class.getName (), "ensureNotTooManyOptionalFarmers");

		final boolean tooMany = (city.getMinimumFarmers () + city.getOptionalFarmers () + city.getNumberOfRebels () > city.getCityPopulation () / 1000);
		if (tooMany)
		{
			city.setOptionalFarmers ((city.getCityPopulation () / 1000) - city.getMinimumFarmers () - city.getNumberOfRebels ());

			if (city.getOptionalFarmers () < 0)
				throw new MomException ("EnsureNotTooManyOptionalFarmers set number of optional farmers to negative: " +
					city.getCityPopulation () + " population, " + city.getMinimumFarmers () + " minimum farmers, " + city.getNumberOfRebels () + " rebels");
		}

		log.exiting (MomServerCityCalculations.class.getName (), "ensureNotTooManyOptionalFarmers", tooMany);
	}

	/**
	 * @param buildings Locked buildings list
	 * @param cityLocation Location of the city to test
	 * @param db Lookup lists built over the XML database
	 * @return -1 if this city has no buildings which give any particular bonus to sight range and so can see the regular resource pattern; 1 or above indicates a longer radius this city can 'see'
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	@Override
	public final int calculateCityScoutingRange (final List<MemoryBuilding> buildings,
		final OverlandMapCoordinatesEx cityLocation, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.entering (MomServerCityCalculations.class.getName (), "calculateCityScoutingRange", cityLocation);

		// Check all buildings at this location
		int result = -1;
		for (final MemoryBuilding thisBuilding : buildings)
			if (cityLocation.equals (thisBuilding.getCityLocation ()))
			{
				final Integer scoutingRange = db.findBuilding (thisBuilding.getBuildingID (), "calculateCityScoutingRange").getBuildingScoutingRange ();

				if (scoutingRange != null)
					result = Math.max (result, scoutingRange);
			}

		log.exiting (MomServerCityCalculations.class.getName (), "calculateCityScoutingRange", result);
		return result;
	}

	/**
	 * Checks whether we will eventually (possibly after constructing various other buildings first) be able to construct the specified building
	 * Human players construct buildings one at a time, but the AI routines look a lot further ahead, and so had issues where they AI would want to construct
	 * a Merchants' Guild, which requires a Bank, which requires a University, but oops our race can't build Universities
	 * So this routine is used by the AI to tell it, give up on the idea of ever constructing a Merchants' Guild because you'll never meet one of the lower requirements
	 *
	 * @param map Known terrain
	 * @param buildings List of buildings that have already been constructed
	 * @param cityLocation Location of the city to check
	 * @param building Cache for the building that we want to construct
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return True if the surrounding terrain has one of the tile type options that we need to construct this building
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or one of the buildings involved
	 */
	@Override
	public final boolean canEventuallyConstructBuilding (final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings,
		final OverlandMapCoordinatesEx cityLocation, final Building building,
		final CoordinateSystem overlandMapCoordinateSystem, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (MomServerCityCalculations.class.getName (), "canEventuallyConstructBuilding",
			new String [] {cityLocation.toString (), building.getBuildingID ()});

		// Need to get the city race
		final OverlandMapCityData cityData = map.getPlane ().get (cityLocation.getPlane ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		final Race race = db.findRace (cityData.getCityRaceID (), "canEventuallyConstructBuilding");

		// Check any direct blocks to us constructing this building
		boolean passes = getCityCalculations ().buildingPassesTileTypeRequirements (map, cityLocation, building, overlandMapCoordinateSystem);
		final Iterator<RaceCannotBuild> iter = race.getRaceCannotBuild ().iterator ();
		while ((passes) && (iter.hasNext ()))
			if (iter.next ().getCannotBuildBuildingID ().equals (building.getBuildingID ()))
				passes = false;

		// Recursively check whether we can meet the requirements of each prerequisite
		final Iterator<BuildingPrerequisite> recursiveIter = building.getBuildingPrerequisite ().iterator ();
		while ((passes) && (recursiveIter.hasNext ()))
		{
			final Building thisBuilding = db.findBuilding (recursiveIter.next ().getPrerequisiteID (), "canEventuallyConstructBuilding");

			// Don't check it is we've already got it - its possible, for example, for a sawmill to be built and then us lose the only forest tile, so while
			// we don't have the prerequisites for it anymore, we still have the building
			if (!getMemoryBuildingUtils ().findBuilding (buildings, cityLocation, thisBuilding.getBuildingID ()))
				if (!canEventuallyConstructBuilding (map, buildings, cityLocation, thisBuilding, overlandMapCoordinateSystem, db))
					passes = false;
		}

		log.exiting (MomServerCityCalculations.class.getName (), "canEventuallyConstructBuilding", passes);
		return passes;
	}

	/**
	 * @return Memory building utils
	 */
	public final IMemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final IMemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return City calculations
	 */
	public final IMomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final IMomCityCalculations calc)
	{
		cityCalculations = calc;
	}
}
