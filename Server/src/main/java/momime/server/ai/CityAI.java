package momime.server.ai;

import java.util.logging.Logger;

import momime.common.calculations.MomCityCalculations;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.database.ServerDatabaseLookup;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.BooleanMapArea2D;

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
	 * Prevent instantiation
	 */
	private CityAI ()
	{
	}
}
