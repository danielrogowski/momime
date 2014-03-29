package momime.server.utils;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.database.ServerDatabaseEx;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server side only helper methods for dealing with cities
 */
public interface CityServerUtils
{
	/**
	 * Validates that a building or unit that we want to construct at a particular city is a valid choice
	 *
	 * @param player Player who wants to change construction
	 * @param trueMap True map details
	 * @param cityLocation Location where they want to set the construction project
	 * @param buildingOrUnitID The building or unit that we want to construct
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choices isn't acceptable
	 * @throws RecordNotFoundException If the race inhabiting the city cannot be found
	 */
	public String validateCityConstruction (final PlayerServerDetails player, final FogOfWarMemory trueMap, final OverlandMapCoordinatesEx cityLocation,
		final String buildingOrUnitID, final CoordinateSystem overlandMapCoordinateSystem, final ServerDatabaseEx db)
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
	public String validateOptionalFarmers (final PlayerServerDetails player, final MapVolumeOfMemoryGridCells trueTerrain, final OverlandMapCoordinatesEx cityLocation,
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
	public void buildCityFromSettler (final MomGeneralServerKnowledge gsk, final PlayerServerDetails player, final MemoryUnit settler,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * @param cityLocation City location
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @return Total production cost of all buildings at this location
	 * @throws RecordNotFoundException If one of the buildings can't be found in the db
	 */
	public int totalCostOfBuildingsAtLocation (final OverlandMapCoordinatesEx cityLocation, final List<MemoryBuilding> buildings, final ServerDatabaseEx db)
		throws RecordNotFoundException;
}
