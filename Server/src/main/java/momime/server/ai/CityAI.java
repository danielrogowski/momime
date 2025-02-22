package momime.server.ai;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.server.MomSessionVariables;

/**
 * Methods for AI players making decisions about where to place cities and what to build in them
 */
public interface CityAI
{
	/**
	 * NB. We don't always know the race of the city we're positioning, when positioning raiders at the start of the game their
	 * race will most likely be the race chosen for the continent we decide to put the city on, i.e. we have to pick position first, race second
	 *
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * 	When called during map creation to place initial cities, this is the true map; when called for AI players using settlers, this is only what that player knows
	 * @param plane Plane to place a city on
	 * @param avoidOtherCities Whether to avoid putting this city close to any existing cities (regardless of who owns them); used for placing starter cities but not when AI builds new ones
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param purpose What this city is being placed for, just for debug message
	 * @return Best possible location to put a new city, or null if there's no space left for any new cities on this plane
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	public MapCoordinates3DEx chooseCityLocation (final FogOfWarMemory mem,
		final int plane, final boolean avoidOtherCities, final MomSessionVariables mom, final String purpose)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;

	/**
	 * Sets the number of optional farmers optimally in every city owned by one player
	 *
	 * @param player Player who we want to reset the number of optional farmers for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws RecordNotFoundException If we encounter a unitID that doesn't exist
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void setOptionalFarmersInAllCities (final PlayerServerDetails player, final MomSessionVariables mom)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * AI player decides what to build in this city
	 *
	 * @param wizardDetails Which wizard the AI player is controlling
	 * @param cityLocation Location of the city
	 * @param cityData True info on the city, so it can be updated
	 * @param numberOfCities Number of cities we own
	 * @param isUnitFactory Is this one of our unit factories? (i.e. one of our cities that can construct the best units we can currently make?)
	 * @param constructableHere Map of all units we could choose to construct, broken down by unit type
	 * @param wantedUnitTypes List of unit types we have a need to build 
	 * @param needForNewUnitsMod Modifier to add/substract to base "need for new units" value from XML DB
	 * @param knownTerrain Known overland terrain
	 * @param knownBuildings Known list of buildings
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 */
	public void decideWhatToBuild (final KnownWizardDetails wizardDetails, final MapCoordinates3DEx cityLocation, final OverlandMapCityData cityData,
		final int numberOfCities, final boolean isUnitFactory, final int needForNewUnitsMod, Map<AIUnitType, List<AIConstructableUnit>> constructableHere,
		final List<AIUnitType> wantedUnitTypes, final MapVolumeOfMemoryGridCells knownTerrain, final List<MemoryBuilding> knownBuildings,
		final MomSessionDescription sd, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * AI player tests every tax rate and chooses the best one
	 * 
	 * @param player Player we want to pick tax rate for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public void decideTaxRate (final PlayerServerDetails player, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * AI checks cities to see if they want to rush buy anything
	 * 
	 * @param player Player we want to check for rush buying
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If there is a problem with city calculations 
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void checkForRushBuying (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * @param playerID Player we want cities for
	 * @param plane Which plane we want cities on
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @return List of coordinates of all our cities
	 */
	public List<MapCoordinates2DEx> listOurCitiesOnPlane (final int playerID, final int plane, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys);

	/**
	 * @param x X coordinate of location to search from
	 * @param y Y coordinate of location to search from
	 * @param ourCitiesOnPlane List output from listOurCitiesOnPlane
	 * @param sys Overland map coordinate system
	 * @return Distance to closest city; 0 if we have no cities on this plane or we are right on top of one
	 */
	public int findDistanceToClosestCity (final int x, final int y, final List<MapCoordinates2DEx> ourCitiesOnPlane, final CoordinateSystem sys);
	
	/**
	 * @param playerID Player whose cities we are interested in
	 * @param plane Plane to check
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @return List of all corrupted tiles near our cities
	 */
	public List<MapCoordinates3DEx> listCorruptedTilesNearOurCities (final int playerID, final int plane, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys);
}