package momime.server.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.server.MomSessionVariables;
import momime.server.utils.CityServerUtils;

/**
 * Calculations the AI needs to make to decide where to place cities, and decide farmers and construction
 */
public final class AICityCalculationsImpl implements AICityCalculations
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (AICityCalculationsImpl.class);
	
	/** City production calculations */
	private CityProductionCalculations cityProductionCalculations;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/**
	 * @param cityLocation Where to consider putting a city
	 * @param avoidOtherCities Whether to avoid putting this city close to any existing cities (regardless of who owns them); used for placing starter cities but not when AI builds new ones
	 * @param enforceMinimumQuality Whether to avoid returning data about cities that are too small to be useful; so usually true, but false if we want to evalulate even terrible cities
	 * @param knownMap Known terrain
	 * 	When called during map creation to place initial cities, this is the true map; when called for AI players using settlers, this is only what that player knows
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return null if enforceMinimumQuality = true and a city here is too small to be useful; otherwise an estimate of how good a city here is/will be
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final Integer evaluateCityQuality (final MapCoordinates3DEx cityLocation, final boolean avoidOtherCities, final boolean enforceMinimumQuality,
		final MapVolumeOfMemoryGridCells knownMap, final MomSessionVariables mom)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		Integer thisCityQuality;
		
		final CityProductionBreakdownsEx productions = getCityProductionCalculations ().calculateAllCityProductions
			(null, knownMap, null, null, cityLocation, null, mom.getSessionDescription (), null, false, true, mom.getServerDB ());
		final CityProductionBreakdown foodProduction = productions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		final int maxCitySize = (foodProduction != null) ? foodProduction.getCappedProductionAmount () : 0;
		
		// If city will be so small as to not be useful then just discount it; remember granary adds +2 and farmers' market adds +3 to this, so really minimum size is 10
		if ((enforceMinimumQuality) && (maxCitySize < 5))
			thisCityQuality = null;
		else
		{
			final CityProductionBreakdown productionProduction = productions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
			final CityProductionBreakdown goldProduction = productions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
			
			final int productionPercentageBonus = (productionProduction != null) ? productionProduction.getPercentageBonus () : 0;
			final int goldTradePercentageBonus = (goldProduction != null) ? goldProduction.getTradePercentageBonusCapped () : 0;
	
			// Add on how good production and gold bonuses are
			thisCityQuality = (maxCitySize * 10) +			// Typically 5-25 -> 50-250
				goldTradePercentageBonus +					// Typically 0-30
				productionPercentageBonus;					// Typically 0-80 usefully
	
			// Improve the estimate according to nearby map features e.g. always stick cities next to adamantium!
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData checkFeatureData = knownMap.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if ((checkFeatureData != null) && (checkFeatureData.getMapFeatureID () != null))
					{
						final Integer featureCityQualityEstimate = mom.getServerDB ().findMapFeature (checkFeatureData.getMapFeatureID (), "chooseCityLocation").getCityQualityEstimate ();
						if (featureCityQualityEstimate != null)
							thisCityQuality = thisCityQuality + featureCityQualityEstimate;
					}
				}
			
			// Avoid placing start cities near other cities, especially don't put wizards near each other
			if (avoidOtherCities)
			{
				// What's the closest existing city to these coordinates?
				Integer closestDistance = getCityServerUtils ().findClosestCityTo (cityLocation, knownMap, mom.getSessionDescription ().getOverlandMapSize ());
				if (closestDistance != null)
					thisCityQuality = thisCityQuality + (closestDistance * 2);		// Maximum would be 40 apart (north-south of map) x2 = 80
			}
		}
		
		return thisCityQuality;
	}

	/**
	 * Chooses one city to convert a worker to a farmer when AI player isn't generating enough rations
	 *
	 * @param cities List of cities to check through 
	 * @param wantTradeGoods If true will only consider cities that are building trade goods; if false will only consider cities that are building something other than trade goods
	 * @param wantOverfarming If true will only consider cities that are overfarming; if false will only consider cities that are not overfarming
	 * @param trueTerrain True overland terrain
	 * @return Chosen city if one matched requirements; null if none matched
	 */
	@Override
	public final AICityRationDetails findWorkersToConvertToFarmers (final List<AICityRationDetails> cities, final boolean wantTradeGoods,
		final boolean wantOverfarming, final MapVolumeOfMemoryGridCells trueTerrain)
	{
		// Build a list of all the workers, by finding all the cities and adding the coordinates of the city to the list the number
		// of times for how many workers there are in the city that we could convert to farmers
		final List<AICityRationDetails> workers = new ArrayList<AICityRationDetails> ();

		for (final AICityRationDetails city : cities)
			if (((wantOverfarming) && (city.isOverfarming ())) ||
				((!wantOverfarming) && (!city.isOverfarming ())))
			{
				final OverlandMapCityData cityData = trueTerrain.getPlane ().get (city.getCityLocation ().getZ ()).getRow ().get 
					(city.getCityLocation ().getY ()).getCell ().get (city.getCityLocation ().getX ()).getCityData ();
	
				if (((wantTradeGoods) && (CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))) ||
					((!wantTradeGoods) && (!CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))))
				{
					final int numberOfWorkers = (cityData.getCityPopulation () / 1000) - cityData.getMinimumFarmers () - cityData.getNumberOfRebels ();
					for (int workerNo = 0; workerNo < numberOfWorkers; workerNo++)
						workers.add (city);
				}
		}

		log.debug ("findWorkersToConvertToFarmers: Found " + workers.size () + " workers available to convert");
		
		// Pick one at random
		final AICityRationDetails city;
		if (workers.size () == 0)
			city = null;
		else
			city = workers.get (getRandomUtils ().nextInt (workers.size ()));
		
		return city;
	}
	
	/**
	 * @return City production calculations
	 */
	public final CityProductionCalculations getCityProductionCalculations ()
	{
		return cityProductionCalculations;
	}

	/**
	 * @param calc City production calculations
	 */
	public final void setCityProductionCalculations (final CityProductionCalculations calc)
	{
		cityProductionCalculations = calc;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return Server-only city utils
	 */
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
}