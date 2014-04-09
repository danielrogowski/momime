package momime.server.ai;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.server.database.ServerDatabaseEx;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Methods for AI players making decisions about where to place cities and what to build in them
 */
public interface CityAI
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
	 * @return Best possible location to put a new city, or null if there's no space left for any new cities on this plane
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 */
	public MapCoordinates3DEx chooseCityLocation (final MapVolumeOfMemoryGridCells map, final int plane,
		final MomSessionDescription sd, final int totalFoodBonusFromBuildings, final ServerDatabaseEx db)
		throws RecordNotFoundException;

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
	public void setOptionalFarmersInAllCities (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final PlayerServerDetails player, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException;

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
	public void decideWhatToBuild (final MapCoordinates3DEx cityLocation, final OverlandMapCityData cityData,
		final MapVolumeOfMemoryGridCells trueTerrain, final List<MemoryBuilding> trueBuildings,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException;
}
