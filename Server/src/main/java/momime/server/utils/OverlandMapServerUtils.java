package momime.server.utils;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarValue;
import momime.common.database.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Server side only helper methods for dealing with the overland map
 */
public interface OverlandMapServerUtils
{
	/**
	 * Sets the continental race (mostly likely race raiders cities at each location will choose) for every land tile on the map
	 *
	 * @param map Known terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Generated area of race IDs
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
 	 * @throws MomException If no races are defined for a particular plane
	 */
	public MapArea3D<String> decideAllContinentalRaces (final MapVolumeOfMemoryGridCells map,
		final CoordinateSystem sys, final CommonDatabase db) throws RecordNotFoundException, MomException;

	/**
	 * NB. This will always return names unique from those names it has generated before - but if human players happen to rename their cities
	 * to a name that the generator hasn't produced yet, it won't avoid generating that name
	 *
	 * @param gsk Server knowledge data structure
	 * @param race The race who is creating a new city
	 * @return Auto generated city name
	 */
	public String generateCityName (final MomGeneralServerKnowledge gsk, final Race race);

	/**
	 * A spirit attempts to capture a node
	 * The only way it can fail is if the node is already owned by another player, in which case we only have a chance of success
	 * 
	 * @param attackingSpirit The Magic or Guardian spirit attempting to take the node; its location tells us where the node is
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param players List of players in this session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void attemptToMeldWithNode (final ExpandedUnitDetails attackingSpirit, final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;
	
	/**
	 * @param map Known terrain
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param playerID Player to check for
	 * @param db Lookup lists built over the XML database
	 * @return Total population this player has across all their cities
	 */
	public int totalPlayerPopulation (final MapVolumeOfMemoryGridCells map, final int playerID, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db);

	/**
	 * @param combatLocation Location of combat we're interested in
	 * @param combatSide Which side of combat we're interested in
	 * @param units True units list
	 * @return Which map cell the requested sides' units are in (i.e. for defender probably=combatLocation, for attacker will be some adjacent map cell)
	 * @throws MomException If the requested side is wiped out
	 */
	public MapCoordinates3DEx findMapLocationOfUnitsInCombat (final MapCoordinates3DEx combatLocation,
		final UnitCombatSideID combatSide, final List<MemoryUnit> units) throws MomException;

	/**
	 * Every turn, there is a 2% chance that volcanoes will degrade into regular mountains
	 * 
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param terrainAndNodeAurasSetting Terrain and Node Auras FOW setting from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void degradeVolcanoesIntoMountains (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final CoordinateSystem overlandMapCoordinateSystem,
		final FogOfWarValue terrainAndNodeAurasSetting)
		throws JAXBException, XMLStreamException;
}