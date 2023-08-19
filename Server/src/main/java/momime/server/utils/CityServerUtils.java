package momime.server.utils;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.server.MomSessionVariables;

/**
 * Server side only helper methods for dealing with cities
 */
public interface CityServerUtils
{
	/**
	 * @param location Location we we base our search from
	 * @param map Known terrain
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @return Distance to closest city on the same plane as location; null if there are no cities on this plane yet
	 */
	public Integer findClosestCityTo (final MapCoordinates3DEx location, final MapVolumeOfMemoryGridCells map, final CoordinateSystem overlandMapCoordinateSystem);
	
	/**
	 * Validates that a building or unit that we want to construct at a particular city is a valid choice
	 *
	 * @param player Player who wants to change construction
	 * @param trueMap True map details
	 * @param cityLocation Location where they want to set the construction project
	 * @param buildingID The building that we want to construct
	 * @param unitID The unit that we want to construct
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choices isn't acceptable
	 * @throws RecordNotFoundException If the race inhabiting the city cannot be found
	 */
	public String validateCityConstruction (final PlayerServerDetails player, final FogOfWarMemory trueMap, final MapCoordinates3DEx cityLocation,
		final String buildingID, final String unitID, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Validates that a number of optional farmers we want to set a particular city to is a valid choice
	 *
	 * @param player Player who wants to set farmers
	 * @param trueTerrain True terrain details
	 * @param cityLocation Location where they want to set the farmers
	 * @param optionalFarmers The number of optional farmers we want
	 * @return null if choice is acceptable; message to send back to client if choices isn't acceptable
	 */
	public String validateOptionalFarmers (final PlayerServerDetails player, final MapVolumeOfMemoryGridCells trueTerrain, final MapCoordinates3DEx cityLocation,
		final int optionalFarmers);

	/**
	 * @param player The player who owns the settler
	 * @param settler The settler being converted into a city
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public void buildCityFromSettler (final PlayerServerDetails player, final MemoryUnit settler, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * @param cityLocation City location
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @return Total production cost of all buildings at this location
	 * @throws RecordNotFoundException If one of the buildings can't be found in the db
	 */
	public int totalCostOfBuildingsAtLocation (final MapCoordinates3DEx cityLocation, final List<MemoryBuilding> buildings, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * @param searchLocation Map location to search around
	 * @param trueTerrain Terrain to search
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @return Location of a city that pulls in requested tile as one of its resource locations; null if there is no city closeby
	 */
	public MapCoordinates3DEx findCityWithinRadius (final MapCoordinates3DEx searchLocation, final MapVolumeOfMemoryGridCells trueTerrain,
		final CoordinateSystem overlandMapCoordinateSystem);

	/**
	 * @param terrain Terrain to search
	 * @param playerID Player whose cities to look for
	 * @param includeOutposts Whether to also count outposts
	 * @return Number of cities the player has
	 */
	public int countCities (final MapVolumeOfMemoryGridCells terrain, final int playerID, final boolean includeOutposts);

	/**
	 * Attempts to find all the cells that we need to build a road on in order to join up two cities.  We don't know that its actually possible yet - maybe they're on two different islands.
	 * If we fail to create a road, that's fine, the method just exits with an empty list, it isn't an error.
	 * 
	 * @param firstCityLocation Location of first city
	 * @param secondCityLocation Location of second city
	 * @param playerID Player who owns the cities
	 * @param fogOfWarMemory Known terrain, buildings, spells and so on
	 * 	When called during map creation to create the initial roads between raider cities, this is the true map; when called for AI players using engineers, this is only what that player knows
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of map cells where we need to add road
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	public List<MapCoordinates3DEx> listMissingRoadCellsBetween (final MapCoordinates3DEx firstCityLocation, final MapCoordinates3DEx secondCityLocation, final int playerID,
		final FogOfWarMemory fogOfWarMemory, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Must take care in how this is used.  If a city allocates a lot of farmers then the overfarming rule applies,
	 * and any additional farmers will generate less than the value output here. 
	 * 
	 * @param map True terrain
	 * @param buildings True list of buildings
	 * @param spells True list of spells
	 * @param cityLocation Location of the city to calculate for
	 * @param db Lookup lists built over the XML database
	 * @return Rations produced by one farmer in this city
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 * @throws MomException If the city's race has no farmers defined or those farmers have no ration production defined
	 */
	public int calculateDoubleFarmingRate (final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings, final List<MemoryMaintainedSpell> spells, final MapCoordinates3DEx cityLocation, final CommonDatabase db)
		throws MomException, RecordNotFoundException;
}