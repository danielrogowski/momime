package momime.server.ai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.IMomCityCalculations;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.BuildingPrerequisite;
import momime.common.database.v0_9_4.RaceCannotBuild;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.IMemoryBuildingUtils;
import momime.common.messages.IUnitUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.calculations.IMomServerCityCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.AiBuildingTypeID;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Race;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.utils.RandomUtils;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.BooleanMapArea2D;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Methods for AI players making decisions about where to place cities and what to build in them
 */
public final class CityAI implements ICityAI
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CityAI.class.getName ());
	
	/** Methods for updating true map + players' memory */
	private IFogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Unit utils */
	private IUnitUtils unitUtils;
	
	/** MemoryBuilding utils */
	private IMemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private IMomCityCalculations cityCalculations;

	/** Server-only city calculations */
	private IMomServerCityCalculations serverCityCalculations;
	
	/**
	 * NB. We don't always know the race of the city we're positioning, when positioning raiders at the start of the game their
	 * race will most likely be the race chosen for the continent we decide to put the city on, i.e. we have to pick position first, race second
	 *
	 * @param map Known terrain
	 * @param plane Plane to place a city on
	 * @param sd Session description
	 * @param totalFoodBonusFromBuildings Value calculated by MomServerCityCalculations.calculateTotalFoodBonusFromBuildings ()
	 * @param db Lookup lists built over the XML database
	 * @return Best possible location to put a new city, or null if there's no space left for any new cities on this plane
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 */
	@Override
	public final OverlandMapCoordinates chooseCityLocation (final MapVolumeOfMemoryGridCells map, final int plane,
		final MomSessionDescription sd, final int totalFoodBonusFromBuildings, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (CityAI.class.getName (), "chooseCityLocation", plane);

		// Mark off all places within 3 squares of an existing city, i.e. those spaces we can't put a new city
		final BooleanMapArea2D withinExistingCityRadius = getCityCalculations ().markWithinExistingCityRadius (map, plane, sd.getMapSize ());

		// Now consider every map location as a possible location for a new city
		OverlandMapCoordinates bestLocation = null;
		int bestCityQuality = -1;

		for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
			{
				// Can we build a city here?
				final OverlandMapTerrainData terrainData = map.getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ();

				if (!withinExistingCityRadius.get (x, y))
				{
					final Boolean canBuildCityOnThisTerrain = db.findTileType (terrainData.getTileTypeID (), "chooseCityLocation").isCanBuildCity ();
					final Boolean canBuildCityOnThisFeature = (terrainData.getMapFeatureID () == null) ? true : db.findMapFeature (terrainData.getMapFeatureID (), "chooseCityLocation").isCanBuildCity ();

					if ((canBuildCityOnThisTerrain != null) && (canBuildCityOnThisTerrain) &&
						(canBuildCityOnThisFeature != null) && (canBuildCityOnThisFeature))
					{
						// How good will this city be?
						final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
						cityLocation.setX (x);
						cityLocation.setY (y);
						cityLocation.setPlane (plane);

						// First find what the max. size will be after we've built all buildings, and re-cap this at the game maximum
						// This ensures cities with max size 20 are considered just as good as cities with max size 25+
						final int maxCitySizeAfterBuildings = totalFoodBonusFromBuildings + getCityCalculations ().calculateMaxCitySize
							(map, cityLocation, sd, true, true, db);

						final int maxCitySizeAfterBuildingsCapped = Math.min (maxCitySizeAfterBuildings, sd.getDifficultyLevel ().getCityMaxSize ());

						// Add on how good production and gold bonuses are
						int thisCityQuality = (maxCitySizeAfterBuildingsCapped * 10) +											// Typically 5-25 -> 50-250
							getCityCalculations ().calculateGoldBonus (map, cityLocation, sd.getMapSize (), db) +		// Typically 0-30
							getCityCalculations ().calculateProductionBonus (map, cityLocation, sd.getMapSize (), db);	// Typically 0-80 usefully

						// Improve the estimate according to nearby map features e.g. always stick cities next to adamantium!
						final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
						coords.setX (x);
						coords.setY (y);
						coords.setPlane (plane);

						for (final SquareMapDirection direction : MomCityCalculations.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
							if (CoordinateSystemUtils.moveCoordinates (sd.getMapSize (), coords, direction.getDirectionID ()))
							{
								final OverlandMapTerrainData checkFeatureData = map.getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
								if (checkFeatureData.getMapFeatureID () != null)
								{
									final Integer featureCityQualityEstimate = db.findMapFeature (checkFeatureData.getMapFeatureID (), "chooseCityLocation").getCityQualityEstimate ();
									if (featureCityQualityEstimate != null)
										thisCityQuality = thisCityQuality + featureCityQualityEstimate;
								}
							}

						// Is it the best so far?
						if ((bestLocation == null) || (thisCityQuality > bestCityQuality))
						{
							bestLocation = cityLocation;
							bestCityQuality = thisCityQuality;
						}
					}
				}
			}

		log.exiting (CityAI.class.getName (), "chooseCityLocation", CoordinatesUtils.overlandMapCoordinatesToString (bestLocation));
		return bestLocation;
	}

	/**
	 * Finds workers in cities to convert to optional farmers
	 *
	 * @param doubleRationsNeeded 2x number of rations that we still need to find optional farmers to help produce
	 * @param tradeGoods If true will only consider cities that are building trade goods; if false will only consider cities that are building something other than trade goods
	 * @param trueMap True map details
	 * @param player Player who we want to convert workers into farmers for
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @return Adjusted value of doubleRationsNeeded
	 * @throws RecordNotFoundException If there is a building that cannot be found in the DB
	 * @throws MomException If a city's race has no farmers defined or those farmers have no ration production defined
	 */
	final int findWorkersToConvertToFarmers (final int doubleRationsNeeded, final boolean tradeGoods, final FogOfWarMemory trueMap,
		final PlayerServerDetails player, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, MomException
	{
		log.entering (CityAI.class.getName (), "findWorkersToConvertToFarmers", tradeGoods);

		// Build a list of all the workers, by finding all the cities and adding the coordinates of the city to the list the number
		// of times for how many workers there are in the city that we could convert to farmers
		final List<OverlandMapCoordinates> workerCoordinates = new ArrayList<OverlandMapCoordinates> ();

		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityPopulation () != null) && (cityData.getCurrentlyConstructingBuildingOrUnitID () != null) &&
						(cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) && (cityData.getCityPopulation () > 0))

						if (((tradeGoods) && (cityData.getCurrentlyConstructingBuildingOrUnitID ().equals (CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS))) ||
							((!tradeGoods) && (!cityData.getCurrentlyConstructingBuildingOrUnitID ().equals (CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS))))
						{
							final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
							cityLocation.setX (x);
							cityLocation.setY (y);
							cityLocation.setPlane (plane.getPlaneNumber ());

							final int numberOfWorkers = (cityData.getCityPopulation () / 1000) - cityData.getMinimumFarmers () - cityData.getNumberOfRebels ();
							for (int workerNo = 0; workerNo < numberOfWorkers; workerNo++)
								workerCoordinates.add (cityLocation);
						}
				}

		log.finest ("findWorkersToConvertToFarmers: Found " + workerCoordinates.size () + " workers available to convert");

		// Now pick workers at random from the list to convert to farmers
		// In this way, we tend to favour putting farmers in larger cities that can spare the population more
		int modifiedDoubleRationsNeeded = doubleRationsNeeded;
		while ((modifiedDoubleRationsNeeded > 0) && (workerCoordinates.size () > 0))
		{
			final int workerNo = RandomUtils.getGenerator ().nextInt (workerCoordinates.size ());
			final OverlandMapCoordinates cityLocation = workerCoordinates.get (workerNo);
			workerCoordinates.remove (workerNo);

			final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getPlane ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

			// Add 1 optional farmer in this city
			cityData.setOptionalFarmers (cityData.getOptionalFarmers () + 1);
			modifiedDoubleRationsNeeded = modifiedDoubleRationsNeeded - getServerCityCalculations ().calculateDoubleFarmingRate (trueMap.getMap (), trueMap.getBuilding (), cityLocation, db);
		}

		log.exiting (CityAI.class.getName (), "findWorkersToConvertToFarmers", modifiedDoubleRationsNeeded);
		return modifiedDoubleRationsNeeded;
	}

	/**
	 * Sets the number of optional farmers optimally in every city owned by one player
	 *
	 * @param trueMap True map details
	 * @param players List of players in the session
	 * @param player Player who we want to reset the number of optional farmers for
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws RecordNotFoundException If we encounter a unitID that doesn't exist
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void setOptionalFarmersInAllCities (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final PlayerServerDetails player, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.entering (CityAI.class.getName (), "setOptionalFarmersInAllCities", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// First find how many rations our current army needs
		int rationsNeeded = 0;
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
				rationsNeeded = rationsNeeded + getUnitUtils ().getModifiedUpkeepValue (thisUnit, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, players, db);

		log.finest ("setOptionalFarmersInAllCities: Armies require " + rationsNeeded + " rations");

		// Then take off how many rations cities are producing even if they have zero optional farmers set
		// e.g. a size 1 city with a granary next to a wild game resource will produce +3 rations even with no farmers,
		// or a size 1 city with no resources must be a farmer, but he only eats 1 of the 2 rations he produces so this also gives +1
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityPopulation () != null) &&
						(cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) && (cityData.getCityPopulation () > 0))
					{
						cityData.setOptionalFarmers (0);

						final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
						cityLocation.setX (x);
						cityLocation.setY (y);
						cityLocation.setPlane (plane.getPlaneNumber ());

						rationsNeeded = rationsNeeded - getCityCalculations ().calculateSingleCityProduction (players, trueMap.getMap (),
							trueMap.getBuilding (), cityLocation, priv.getTaxRateID (), sd, true, db,
							CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS);
					}
				}

		log.finest ("setOptionalFarmersInAllCities: Armies require " + rationsNeeded + " rations after taking off what is provided by setting 0 optional farmers in all cities");

		// Farming rate for each farmer is doubled
		int doubleRationsNeeded = rationsNeeded * 2;
		log.finest ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after doubling");

		// Use farmers from cities producing trade goods first
		if (doubleRationsNeeded > 0)
			doubleRationsNeeded = findWorkersToConvertToFarmers (doubleRationsNeeded, true, trueMap, player, db, sd);

		log.finest ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after using up trade goods cities");

		if (doubleRationsNeeded > 0)
			doubleRationsNeeded = findWorkersToConvertToFarmers (doubleRationsNeeded, false, trueMap, player, db, sd);

		log.finest ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after using up other production cities");

		// Update each player's memorised view of this city with the new number of optional farmers, if they can see it
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityPopulation () != null) &&
						(cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) && (cityData.getCityPopulation () > 0))
					{
						final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
						cityLocation.setX (x);
						cityLocation.setY (y);
						cityLocation.setPlane (plane.getPlaneNumber ());

						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (trueMap.getMap (), players, cityLocation, sd.getFogOfWarSetting ());
					}
				}

		log.exiting (CityAI.class.getName (), "setOptionalFarmersInAllCities");
	}

	/**
	 * AI player decides what to build in this city
	 *
	 * @param cityLocation Location of the city
	 * @param cityData Info on the city
	 * @param trueTerrain True overland terrain
	 * @param trueBuildings True list of buildings
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 */
	@Override
	public final void decideWhatToBuild (final OverlandMapCoordinates cityLocation, final OverlandMapCityData cityData,
		final MapVolumeOfMemoryGridCells trueTerrain, final List<MemoryBuilding> trueBuildings,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (CityAI.class.getName (), "decideWhatToBuild", CoordinatesUtils.overlandMapCoordinatesToString (cityLocation));

		// Convert list of buildings that our race can't build into a string list, so its easier to search
		final Race race = db.findRace (cityData.getCityRaceID (), "decideWhatToBuild");
		final List<String> raceCannotBuild = new ArrayList<String> ();
		for (final RaceCannotBuild cannotBuild : race.getRaceCannotBuild ())
			raceCannotBuild.add (cannotBuild.getCannotBuildBuildingID ());

		// Currently we can't build units or expand, so there's basically nothing to do other than build the buildings in the most logical order

		// Once there are decisions to be made about 'does this city have enough defence' and 'do we need more gold' then the code
		// will be able to make some intelligent choices about the types of buildings it needs more urgently

		// Also this string is likely to be derived partially from the database e.g. an Expansionist AI player
		// will have different build preferences than a Perfectionist AI player
		final List<AiBuildingTypeID> buildingTypes = new ArrayList<AiBuildingTypeID> ();
		buildingTypes.add (AiBuildingTypeID.GROWTH);
		buildingTypes.add (AiBuildingTypeID.PRODUCTION);
		buildingTypes.add (AiBuildingTypeID.RESEARCH);
		buildingTypes.add (AiBuildingTypeID.UNREST_AND_MAGIC_POWER);
		buildingTypes.add (AiBuildingTypeID.GOLD);
		buildingTypes.add (AiBuildingTypeID.UNREST_WITHOUT_MAGIC_POWER);
		buildingTypes.add (AiBuildingTypeID.UNITS);

		boolean decided = false;

		final Iterator<AiBuildingTypeID> buildingTypesIter = buildingTypes.iterator ();
		while ((!decided) && (buildingTypesIter.hasNext ()))
		{
			final AiBuildingTypeID buildingType = buildingTypesIter.next ();

			// List out all buildings, except those that:
			// a) are of the wrong type
			// b) we already have, or
			// c) our race cannot build
			// Keep buildings in the list even if we don't yet have the pre-requisites necessary to build them
			final List<Building> buildingOptions = new ArrayList<Building> ();
			for (final Building building : db.getBuilding ())
				if ((building.getAiBuildingTypeID () == buildingType) && (!getMemoryBuildingUtils ().findBuilding (trueBuildings, cityLocation, building.getBuildingID ())) &&
					(getServerCityCalculations ().canEventuallyConstructBuilding (trueTerrain, trueBuildings, cityLocation, building, sd.getMapSize (), db)))

					buildingOptions.add (building);

			// Now step through the list of possible buildings until we find one we have all the pre-requisites for
			// If we find one that we DON'T have all the pre-requisites for, then add the pre-requisites that we don't have but can build onto the end of the list
			// In this way we can decide that we want e.g. a Wizards' Guild and this repeating loop will work out all the buildings that we need to build first in order to get it

			// Use an index so we can add to the tail of the list as we go along
			int buildingIndex = 0;
			while ((!decided) && (buildingIndex < buildingOptions.size ()))
			{
				final Building thisBuilding = buildingOptions.get (buildingIndex);

				if (getMemoryBuildingUtils ().meetsBuildingRequirements (trueBuildings, cityLocation, thisBuilding))
				{
					// Got one we can decide upon
					cityData.setCurrentlyConstructingBuildingOrUnitID (thisBuilding.getBuildingID ());
					decided = true;
				}
				else
				{
					// We want this, but we need to build something else first - find out what
					// Note we don't need to check whether each prerequisite can itself be constructed, or for the right tile types, because
					// canEventuallyConstructBuilding () above already checked all this over the entire prerequisite tree, so all we need to do is add them
					for (final BuildingPrerequisite prereq : thisBuilding.getBuildingPrerequisite ())
					{
						final Building buildingPrereq = db.findBuilding (prereq.getPrerequisiteID (), "decideWhatToBuild");
						if ((!buildingOptions.contains (buildingPrereq)) && (!getMemoryBuildingUtils ().findBuilding (trueBuildings, cityLocation, buildingPrereq.getBuildingID ())))
							buildingOptions.add (buildingPrereq);
					}

					buildingIndex++;
				}
			}
		}

		// If no appropriate buildings at all then resort to Trade Gooods
		if (!decided)
			cityData.setCurrentlyConstructingBuildingOrUnitID (CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS);

		// Put this into the calling method, just to make this easier to test
		// FogOfWarMidTurnChanges.updatePlayerMemoryOfCity (trueTerrain, players, cityLocation, sd);

		log.exiting (CityAI.class.getName (), "decideWhatToBuild", cityData.getCurrentlyConstructingBuildingOrUnitID ());
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final IFogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final IFogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Unit utils
	 */
	public final IUnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final IUnitUtils utils)
	{
		unitUtils = utils;
	}
	
	/**
	 * @return MemoryBuilding utils
	 */
	public final IMemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
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

	/**
	 * @return Server-only city calculations
	 */
	public final IMomServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final IMomServerCityCalculations calc)
	{
		serverCityCalculations = calc;
	}
}
