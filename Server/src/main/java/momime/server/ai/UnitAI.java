package momime.server.ai;

import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomSessionDescription;
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
}