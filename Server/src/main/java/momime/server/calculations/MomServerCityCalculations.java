package momime.server.calculations;

import java.util.List;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_5.Building;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server only calculations pertaining to cities, e.g. calculating resources gathered from within the city radius
 */
public interface MomServerCityCalculations
{
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
	public int calculateTotalFoodBonusFromBuildings (final ServerDatabaseEx db)
		throws MomException;

	/**
	 * @param map True terrain
	 * @param buildings True list of buildings
	 * @param cityLocation Location of the city to calculate for
	 * @param db Lookup lists built over the XML database
	 * @return Rations produced by one farmer in this city
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 * @throws MomException If the city's race has no farmers defined or those farmers have no ration production defined
	 */
	public int calculateDoubleFarmingRate (final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings, final MapCoordinates3DEx cityLocation, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException;

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
	public void calculateCitySizeIDAndMinimumFarmers (final List<PlayerServerDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings, final MapCoordinates3DEx cityLocation,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException;

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
	public void ensureNotTooManyOptionalFarmers (final OverlandMapCityData city)
		throws MomException;

	/**
	 * @param buildings Locked buildings list
	 * @param cityLocation Location of the city to test
	 * @param db Lookup lists built over the XML database
	 * @return -1 if this city has no buildings which give any particular bonus to sight range and so can see the regular resource pattern; 1 or above indicates a longer radius this city can 'see'
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	public int calculateCityScoutingRange (final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final ServerDatabaseEx db) throws RecordNotFoundException;

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
	public boolean canEventuallyConstructBuilding (final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final Building building,
		final CoordinateSystem overlandMapCoordinateSystem, final ServerDatabaseEx db)
		throws RecordNotFoundException;
}
