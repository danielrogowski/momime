package momime.server.ai;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomSessionDescription;

/**
 * Calculations the AI needs to make to decide where to place cities, and decide farmers and construction
 */
public interface AICityCalculations
{
	/**
	 * @param cityLocation Where to consider putting a city
	 * @param avoidOtherCities Whether to avoid putting this city close to any existing cities (regardless of who owns them); used for placing starter cities but not when AI builds new ones
	 * @param enforceMinimumQuality Whether to avoid returning data about cities that are too small to be useful; so usually true, but false if we want to evalulate even terrible cities
	 * @param knownMap Known terrain
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return null if enforceMinimumQuality = true and a city here is too small to be useful; otherwise an estimate of how good a city here is/will be
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	public Integer evaluateCityQuality (final MapCoordinates3DEx cityLocation, final boolean avoidOtherCities, final boolean enforceMinimumQuality,
		final MapVolumeOfMemoryGridCells knownMap, final MomSessionDescription sd, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;
	
	/**
	 * Chooses one city to convert a worker to a farmer when AI player isn't generating enough rations
	 *
	 * @param cities List of cities to check through 
	 * @param wantTradeGoods If true will only consider cities that are building trade goods; if false will only consider cities that are building something other than trade goods
	 * @param wantOverfarming If true will only consider cities that are overfarming; if false will only consider cities that are not overfarming
	 * @param trueTerrain True overland terrain
	 * @return Chosen city if one matched requirements; null if none matched
	 */
	public AICityRationDetails findWorkersToConvertToFarmers (final List<AICityRationDetails> cities, final boolean wantTradeGoods,
		final boolean wantOverfarming, final MapVolumeOfMemoryGridCells trueTerrain);
}