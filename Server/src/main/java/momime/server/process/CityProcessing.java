package momime.server.process;

import java.util.List;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomSessionDescription;
import momime.server.MomSessionVariables;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Methods for any significant message processing to do with cities that isn't done in the message implementations
 */
public interface CityProcessing
{
	/**
	 * Creates the starting cities for each Wizard and Raiders
	 *
	 * This happens BEFORE we initialize each players' fog of war (of course... without their cities they wouldn't be able to see much of the map!)
	 * and so we don't need to send any messages out to anyone here, whether to add the city itself, buildings or units - just add everything to the true map
	 *
	 * @param players List of players in the session
	 * @param gsk Server knowledge data structure
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
 	 * @throws MomException If no races are defined for a particular plane
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void createStartingCities (final List<PlayerServerDetails> players,
		final MomGeneralServerKnowledge gsk, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;

	/**
	 * Attempts to find all the cells that we need to build a road on in order to join up all cities owned by a particular player on a particular plane.
	 * 
	 * @param playerID Player who owns the cities
	 * @param plane Plane to check cities on
	 * @param maximumSeparation Connect cities who are at most this distance apart; null = connect all cities regardless of how far apart they are
	 * @param players List of players in this session
	 * @param fogOfWarMemory Known terrain, buildings, spells and so on
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return List of map cells where we need to add road
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	public List<MapCoordinates3DEx> listMissingRoadCells (final int playerID, final int plane, final Integer maximumSeparation,
		final List<PlayerServerDetails> players, final FogOfWarMemory fogOfWarMemory, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * All cities progress a little towards construction projects
	 *
	 * @param onlyOnePlayerID If zero, will process grow cities + progress construction for all players; if specified will do so only for the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void progressConstructionProjects (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * All cities grow population a little
	 *
	 * @param onlyOnePlayerID If zero, will process grow cities + progress construction for all players; if specified will do so only for the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void growCities (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
		
	/**
	 * The method in the FOW class physically removed builidngs from the server and players' memory; this method
	 * deals with all the knock on effects of buildings being sold, such as paying the gold from the sale to the city owner,
	 * making sure the current construction project is still valid, and if the building sale alters unrest or production in any way.
	 *
	 * Does not recalc global production (which will now be reduced from not having to pay the maintenance of the sold building),
	 * this has to be done by the calling routine.
	 * 
	 * NB. Dephi method was called OkToSellBuilding.
	 *
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param cityLocation Location of the city to remove the building from
	 * @param buildingURN Which building to remove; this can be null to cancel a pending sale
	 * @param pendingSale If true, building is not sold immediately but merely marked that it will be sold at the end of the turn; used for simultaneous turns games
	 * @param voluntarySale True if building is being sold by the player's choice; false if they are being forced to sell it e.g. due to lack of production
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param conjunctionEventID Currently active conjunction, if there is one
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void sellBuilding (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx cityLocation, final Integer buildingURN,
		final boolean pendingSale, final boolean voluntarySale,
		final MomSessionDescription sd, final CommonDatabase db, final String conjunctionEventID)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * Similar to sellBuilding above, but the building was being destroyed by some spell or other action, so the owner doesn't get gold for it and
	 * the need to be notified about it as well.  Also it handles destroying multiple buildings all at once, potentially in different cities owned by
	 * different players.
	 * 
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param buildingsToDestroy List of buildings to destroy, from server's true list
	 * @param buildingsDestroyedBySpellID The spell that resulted in destroying these building(s), e.g. Earthquake; null if buildings destroyed for any other reason
	 * @param buildingDestructionSpellCastByPlayerID The player who cast the spell that resulted in the destruction of these buildings; null if not from a spell
	 * @param buildingDestructionSpellLocation The location the spell was targeted - need this because it might have destroyed 0 buildings; null if not from a spell
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param conjunctionEventID Currently active conjunction, if there is one
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void destroyBuildings (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final List<MemoryBuilding> buildingsToDestroy,
		final String buildingsDestroyedBySpellID, final Integer buildingDestructionSpellCastByPlayerID, final MapCoordinates3DEx buildingDestructionSpellLocation,
		final MomSessionDescription sd, final CommonDatabase db, final String conjunctionEventID)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
	
	/**
	 * Changes a player's tax rate, currently only clients can change their tax rate, but the AI should use this method too.
	 * Although this is currently only used by human players, kept it separate from ChangeTaxRateMessageImpl in anticipation of AI players using it
	 * 
	 * @param player Player who is changing their tax rate
	 * @param taxRateID New tax rate
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void changeTaxRate (final PlayerServerDetails player, final String taxRateID, final MomSessionVariables mom)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * Changes ownership of a city after it is captured in combat
	 * 
	 * @param cityLocation Location of the city
	 * @param attackingPlayer Player who won the combat, who is taking ownership of the city
	 * @param defendingPlayer Player who lost the combat, and who is losing the city
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void captureCity (final MapCoordinates3DEx cityLocation, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MomSessionVariables mom) throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
	
	/**
	 * Destroys a city after it is taken in combat
	 * 
	 * @param cityLocation Location of the city
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void razeCity (final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

	/**
	 * Rampaging monsters destroy a city and convert it to a ruin
	 * 
	 * @param cityLocation Location of the city
	 * @param goldInRuin The gold the player lost in the city is kept in the ruin and awarded to whoever captures it
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void ruinCity (final MapCoordinates3DEx cityLocation, final int goldInRuin, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;
	
	/**
	 * Handles when the city housing a wizard's fortress is captured in combat and the wizard gets banished
	 * 
	 * @param attackingPlayer Player who won the combat, who is doing the banishing
	 * @param defendingPlayer Player who lost the combat, who is the one being banished
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void banishWizard (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;

	/**
	 * If a wizard loses the city where their summoning circle is, but they still have their Wizard's Fortress at a different location,
	 * then this method auto adds their summoning circle back at the same location as their fortress.
	 * 
	 * @param playerID Player who lost their summoning circle 
	 * @param gsk Server knowledge structure to add the building(s) to
	 * @param players List of players in this session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void moveSummoningCircleToWizardsFortress (final int playerID, final MomGeneralServerKnowledge gsk, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * Certain buildings require certain tile types to construct them, e.g. a Sawmill can only be constructed if there is a forest tile.
	 * So this method rechecks that city construction is still valid after there's been a change to an overland tile.
	 * 
	 * @param targetLocation Location where terrain was changed
	 * @param trueMap True map details
	 * @param players List of players in this session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void recheckCurrentConstructionIsStillValid (final MapCoordinates3DEx targetLocation,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
}