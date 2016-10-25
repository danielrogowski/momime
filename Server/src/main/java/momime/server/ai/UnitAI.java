package momime.server.ai;

import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.AiMovementCode;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomSessionDescription;
import momime.server.MomSessionVariables;
import momime.server.database.AiUnitCategorySvr;
import momime.server.database.ServerDatabaseEx;

/**
 * Methods for AI players evaluating the strength of units
 */
public interface UnitAI
{
	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the current quality, usefulness and effectiveness of this unit
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public int calculateUnitCurrentRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness that this unit has the potential to become
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public int calculateUnitPotentialRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness for defensive purposes
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public int calculateUnitAverageRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Lists every unit this AI player can build at every city they own, as well as any units they can summon, sorted with the best units first.
	 * This won't list heroes, since if we cast Summon Hero/Champion, we never know which one we're going to get.
	 * Will not include any units that we wouldn't be able to afford the maintenance cost of if we constructed them
	 * (mana for summoned units; gold for units constructed in cities - will ignore rations since we can always allocate more farmers).
	 * 
	 * @param player AI player who is considering constructing a unit
	 * @param players Players list
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return List of all possible units this AI player can construct or summon, sorted with the best first
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public List<AIConstructableUnit> listAllUnitsWeCanConstruct (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Calculates the current and average rating of every unit we can see on the map.
	 * 
	 * @param ourUnits Array to populate our unit ratings into
	 * @param enemyUnits Array to populate enemy unit ratings into
	 * @param playerID Player ID to consider as "our" units
	 * @param mem Memory data known to playerID
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public void calculateUnitRatingsAtEveryMapCell (final AIUnitsAndRatings [] [] [] ourUnits, final AIUnitsAndRatings [] [] [] enemyUnits,
		final int playerID, final FogOfWarMemory mem, final List<PlayerServerDetails> players, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Checks every city, node, lair and tower that we either own or is undefended, and checks how much short of our desired defence level it currently is.
	 * As a side effect, any units where we have too much defence, or units which are not in a defensive location, are put into a list of mobile units.
	 * 
	 * @param ourUnits Array of our unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param mobileUnits List to populate with details of all units that are in excess of defensive requirements, or are not in defensive positions
	 * @param playerID Player ID to consider as "our" units
	 * @param mem Memory data known to playerID
	 * @param desiredDefenceRating How much defence we are aiming for at every location
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return List of all defence locations, and how many points short we are of our desired defence level
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	public List<AIDefenceLocation> evaluateCurrentDefence (final AIUnitsAndRatings [] [] [] ourUnits, final AIUnitsAndRatings [] [] [] enemyUnits,
		final List<AIUnitAndRatings> mobileUnits, final int playerID, final FogOfWarMemory mem, final int desiredDefenceRating,
		final CoordinateSystem sys, final ServerDatabaseEx db) throws RecordNotFoundException;

	/**
	 * @param units Flat list of units to convert
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Units split by category and their map location, so units are grouped into stacks only of matching types
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public Map<String, List<AIUnitsAndRatings>> categoriseAndStackUnits (final List<AIUnitAndRatings> units,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Uses an ordered list of AI movement codes to try to decide what to do with a particular unit stack
	 * 
	 * @param units The units to move
	 * @param movementCodes List of movement codes to try
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param underdefendedLocations Locations which are either ours (cities/towers) but lack enough defence, or not ours but can be freely captured (empty lairs/cities/etc)
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If an expected record cannot be found
	 * @throws MomException If we encounter a movement code that we don't know how to process
	 */
	public AIMovementDecision decideUnitMovement (final AIUnitsAndRatings units, final List<AiMovementCode> movementCodes, final int [] [] [] doubleMovementDistances,
		final List<AIDefenceLocation> underdefendedLocations, final AIUnitsAndRatings [] [] [] enemyUnits, final MapVolumeOfMemoryGridCells terrain,
		final CoordinateSystem sys, final ServerDatabaseEx db) throws MomException, RecordNotFoundException;
	
	/**
	 * AI decides where to move a unit to on the overland map and actually does the move.
	 * 
	 * @param units The units to move
	 * @param category What category of units these are
	 * @param underdefendedLocations Locations we should consider a priority to aim for
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param player Player who owns the unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we found something to do or not
	 * @throws RecordNotFoundException If an expected record cannot be found
	 * @throws PlayerNotFoundException If a player cannot be found
	 * @throws MomException If there is a significant problem in the game logic
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public boolean decideAndExecuteUnitMovement (final AIUnitsAndRatings units, final AiUnitCategorySvr category, final List<AIDefenceLocation> underdefendedLocations,
		final AIUnitsAndRatings [] [] [] enemyUnits, final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}