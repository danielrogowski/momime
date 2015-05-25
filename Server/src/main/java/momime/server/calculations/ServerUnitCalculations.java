package momime.server.calculations;

import java.util.List;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.server.database.ServerDatabaseEx;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server only calculations pertaining to units, e.g. calculations relating to fog of war
 */
public interface ServerUnitCalculations
{
	/**
	 * @param unit The unit to check
	 * @param players Pre-locked players list
	 * @param spells Known spells (flight spell might increase scouting range)
	 * @param combatAreaEffects Known combat area effects (because theoretically, you could define a CAE which bumped up the scouting skill...)
	 * @param db Lookup lists built over the XML database
	 * @return How many squares this unit can see; by default = 1, flying units automatically get 2, and the Scouting unit skill can push this even higher
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public int calculateUnitScoutingRange (final MemoryUnit unit, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * @param x X coordinate of the location we want to check
	 * @param y Y coordinate of the location we want to check
	 * @param plane Plane we want to check
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge of the terrain
	 * @param units The player who is trying to move here's knowledge of units
	 * @param db Lookup lists built over the XML database
	 * @return Whether moving here will result in an attack or not
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	public boolean willMovingHereResultInAnAttack (final int x, final int y, final int plane, final int movingPlayerID,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units, final ServerDatabaseEx db) throws RecordNotFoundException;

	/**
	 * Calculates how many (doubled) movement points it will take to move from x, y to ever other location on the map plus whether we can move there or not
	 * MoM is a little weird with how movement works - providing you have even 1/2 move left, you can move anywhere, even somewhere which takes 3 movement to get to
	 * e.g. a unit on a road with 1 movement can walk onto the road (1/2 mp), and then into mountains (3 mp) for a total move of 3 1/2 mp even thought they only had 1 movement!
	 * Therefore knowing distances to each location is not enough - we need a separate boolean array to mark whether we can or cannot reach each location
	 *
	 * @param startX X coordinate of location to move from
	 * @param startY Y coordinate of location to move from
	 * @param startPlane Plane to move from
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param movementDirections The direction that we moved to get here, e.g. the tile directly above startX, startY will have value 1
	 * @param canMoveToInOneTurn Indicates the locations that we can reach in a single turn (see the forester example above)
	 * @param movingHereResultsInAttack Indicates whether we know that moving here will result in attacking an enemy unit stack
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	public void calculateOverlandMovementDistances (final int startX, final int startY, final int startPlane, final int movingPlayerID,
		final FogOfWarMemory map, final List<MemoryUnit> unitStack, final int doubleMovementRemaining,
		final int [] [] [] doubleMovementDistances, final int [] [] [] movementDirections, final boolean [] [] [] canMoveToInOneTurn,
		final boolean [] [] [] movingHereResultsInAttack,
		final MomSessionDescription sd, final ServerDatabaseEx db) throws RecordNotFoundException;
}