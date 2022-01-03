package momime.common.calculations;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomSessionDescription;

/**
 * Uses the individual calculation methods in CityCalculations to work out the entire production of a city
 */
public interface CityProductionCalculations
{
	/**
	 * @param players Players list
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * @param cityLocation Location of the city to calculate for; NB. It must be possible to call this on a map location which is not yet a city, so the AI can consider potential sites
	 * @param taxRateID Tax rate to use for the calculation
	 * @param sd Session description
	 * @param conjunctionEventID Currently active conjunction, if there is one
	 * @param includeProductionAndConsumptionFromPopulation Normally true; if false, production and consumption from civilian population will be excluded
	 * 	(This is needed when calculating minimumFarmers, i.e. how many rations does the city produce from buildings and map features only, without considering farmers)
	 * @param calculatePotential Normally false; if true, will consider city size and gold trade bonus to be as they will be after the city is built up
	 * 	(This is typically used in conjunction with includeProductionAndConsumptionFromPopulation=false for the AI to consider the potential value of sites where it may build cities)
	 * @param db Lookup lists built over the XML database
	 * @return List of all productions and consumptions from this city
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	public CityProductionBreakdownsEx calculateAllCityProductions (final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd, final String conjunctionEventID,
		final boolean includeProductionAndConsumptionFromPopulation, final boolean calculatePotential, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;
}