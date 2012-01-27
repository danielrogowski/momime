package momime.server.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Plane;
import momime.server.process.FogOfWarProcessing;
import momime.server.utils.RandomUtils;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.BooleanMapArea2D;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Methods for AI players making decisions about where to place cities and what to build in them
 */
public final class CityAI
{
	/**
	 * NB. We don't always know the race of the city we're positioning, when positioning raiders at the start of the game their
	 * race will most likely be the race chosen for the continent we decide to put the city on, i.e. we have to pick position first, race second
	 *
	 * @param map Known terrain
	 * @param plane Plane to place a city on
	 * @param sd Session description
	 * @param totalFoodBonusFromBuildings Value calculated by MomServerCityCalculations.calculateTotalFoodBonusFromBuildings ()
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Best possible location to put a new city, or null if there's no space left for any new cities on this plane
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 */
	public static final OverlandMapCoordinates chooseCityLocation (final MapVolumeOfMemoryGridCells map, final int plane,
		final MomSessionDescription sd, final int totalFoodBonusFromBuildings, final ServerDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (CityAI.class.getName (), "chooseCityLocation", plane);

		// Mark off all places within 3 squares of an existing city, i.e. those spaces we can't put a new city
		final BooleanMapArea2D withinExistingCityRadius = MomCityCalculations.markWithinExistingCityRadius (map, plane, sd.getMapSize (), debugLogger);

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
						final int maxCitySizeAfterBuildings = totalFoodBonusFromBuildings + MomCityCalculations.calculateMaxCitySize
							(map, cityLocation, sd, true, true, db, debugLogger);

						final int maxCitySizeAfterBuildingsCapped = Math.min (maxCitySizeAfterBuildings, sd.getDifficultyLevel ().getCityMaxSize ());

						// Add on how good production and gold bonuses are
						int thisCityQuality = (maxCitySizeAfterBuildingsCapped * 10) +																	// Typically 5-25 -> 50-250
							MomCityCalculations.calculateGoldBonus (map, cityLocation, sd.getMapSize (), db, debugLogger) +			// Typically 0-30
							MomCityCalculations.calculateProductionBonus (map, cityLocation, sd.getMapSize (), db, debugLogger);	// Typically 0-80 usefully

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

		debugLogger.exiting (CityAI.class.getName (), "chooseCityLocation", CoordinatesUtils.overlandMapCoordinatesToString (bestLocation));
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Adjusted value of doubleRationsNeeded
	 * @throws RecordNotFoundException If there is a building that cannot be found in the DB
	 * @throws MomException If a city's race has no farmers defined or those farmers have no ration production defined
	 */
	static final int findWorkersToConvertToFarmers (final int doubleRationsNeeded, final boolean tradeGoods, final FogOfWarMemory trueMap,
		final PlayerServerDetails player, final ServerDatabaseLookup db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, MomException
	{
		debugLogger.entering (CityAI.class.getName (), "findWorkersToConvertToFarmers", tradeGoods);

		// Build a list of all the workers, by finding all the cities and adding the coordinates of the city to the list the number
		// of times for how many workers there are in the city that we could convert to farmers
		final List<OverlandMapCoordinates> workerCoordinates = new ArrayList<OverlandMapCoordinates> ();

		for (final Plane plane : db.getPlanes ())
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

		debugLogger.finest ("findWorkersToConvertToFarmers: Found " + workerCoordinates.size () + " workers available to convert");

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
			modifiedDoubleRationsNeeded = modifiedDoubleRationsNeeded - MomServerCityCalculations.calculateDoubleFarmingRate (trueMap.getMap (), trueMap.getBuilding (), cityLocation, db, debugLogger);
		}

		debugLogger.exiting (CityAI.class.getName (), "findWorkersToConvertToFarmers", modifiedDoubleRationsNeeded);
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws RecordNotFoundException If we encounter a unitID that doesn't exist
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public static final void setOptionalFarmersInAllCities (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final PlayerServerDetails player, final ServerDatabaseLookup db, final MomSessionDescription sd, final Logger debugLogger)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException
	{
		debugLogger.entering (CityAI.class.getName (), "setOptionalFarmersInAllCities", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// First find how many rations our current army needs
		int rationsNeeded = 0;
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
				rationsNeeded = rationsNeeded + UnitUtils.getModifiedUpkeepValue (thisUnit, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, players, db, debugLogger);

		debugLogger.finest ("setOptionalFarmersInAllCities: Armies require " + rationsNeeded + " rations");

		// Then take off how many rations cities are producing even if they have zero optional farmers set
		// e.g. a size 1 city with a granary next to a wild game resource will produce +3 rations even with no farmers,
		// or a size 1 city with no resources must be a farmer, but he only eats 1 of the 2 rations he produces so this also gives +1
		for (final Plane plane : db.getPlanes ())
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

						rationsNeeded = rationsNeeded - MomCityCalculations.calculateSingleCityProduction (players, trueMap.getMap (),
							trueMap.getBuilding (), cityLocation, priv.getTaxRateID (), sd, true, db, debugLogger,
							CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS);
					}
				}

		debugLogger.finest ("setOptionalFarmersInAllCities: Armies require " + rationsNeeded + " rations after taking off what is provided by setting 0 optional farmers in all cities");

		// Farming rate for each farmer is doubled
		int doubleRationsNeeded = rationsNeeded * 2;
		debugLogger.finest ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after doubling");

		// Use farmers from cities producing trade goods first
		if (doubleRationsNeeded > 0)
			doubleRationsNeeded = findWorkersToConvertToFarmers (doubleRationsNeeded, true, trueMap, player, db, sd, debugLogger);

		debugLogger.finest ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after using up trade goods cities");

		if (doubleRationsNeeded > 0)
			doubleRationsNeeded = findWorkersToConvertToFarmers (doubleRationsNeeded, false, trueMap, player, db, sd, debugLogger);

		debugLogger.finest ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after using up other production cities");

		// Update each player's memorised view of this city with the new number of optional farmers, if they can see it
		for (final Plane plane : db.getPlanes ())
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

						FogOfWarProcessing.updatePlayerMemoryOfCity (trueMap.getMap (), players, cityLocation, sd, debugLogger);
					}
				}

		debugLogger.exiting (CityAI.class.getName (), "setOptionalFarmersInAllCities");
	}

	/**
	 * Prevent instantiation
	 */
	private CityAI ()
	{
	}
}
