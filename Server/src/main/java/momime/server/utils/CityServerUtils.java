package momime.server.utils;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.server.database.ServerDatabaseEx;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;

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
		final String buildingID, final String unitID, final CoordinateSystem overlandMapCoordinateSystem, final ServerDatabaseEx db)
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
	 * @param gsk Server knowledge data structure
	 * @param player The player who owns the settler
	 * @param settler The settler being converted into a city
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void buildCityFromSettler (final MomGeneralServerKnowledgeEx gsk, final PlayerServerDetails player, final MemoryUnit settler,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * @param cityLocation City location
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @return Total production cost of all buildings at this location
	 * @throws RecordNotFoundException If one of the buildings can't be found in the db
	 */
	public int totalCostOfBuildingsAtLocation (final MapCoordinates3DEx cityLocation, final List<MemoryBuilding> buildings, final ServerDatabaseEx db)
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
	 * @return Number of cities the player has
	 */
	public int countCities (final MapVolumeOfMemoryGridCells terrain, final int playerID);
}