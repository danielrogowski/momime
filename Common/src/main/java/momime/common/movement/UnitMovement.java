package momime.common.movement;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Methods dealing with unit movement
 */
public interface UnitMovement
{
	/**
	 * Given a cell we can move from and to, checks if the destination cell is actually passable,
	 * if its a quicker route to it than we may have already found. 
	 * 
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param moveFrom Coordinates of the cell we are moving from
	 * @param movementType How we are moving between the two cells
	 * @param direction Direction from the from to the to cell, only if movementType is ADJACENT
	 * @param moveTo Coordinates of the cell we can move to
	 * @param movingPlayerID The player who is trying to move here
	 * @param doubleDistanceToHere Amount of movement it took to reach moveFrom
	 * @param doubleMovementRemainingToHere Amount of movement we have remaining after moveFrom
	 * @param cellTransportCapacity Count of the number of free transport spaces at every map cell
	 * @param doubleMovementRates Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param cellsLeftToCheck List of cells that still need to be checked (we add adjacent cells to the end of this list)
	 * @param mem The player who is trying to move here's knowledge
	 * @param db Lookup lists built over the XML database
	 */
	public void considerPossibleMove (final UnitStack unitStack, final Set<String> unitStackSkills,
		final MapCoordinates3DEx moveFrom, final OverlandMovementType movementType, final int direction,
		final MapCoordinates3DEx moveTo, final int movingPlayerID, final int doubleDistanceToHere, final int doubleMovementRemainingToHere,
		final int [] [] [] cellTransportCapacity, final Map<String, Integer> doubleMovementRates,
		final OverlandMovementCell [] [] [] moves, final List<MapCoordinates3DEx> cellsLeftToCheck, final FogOfWarMemory mem, final CommonDatabase db);

	/**
	 * @param start Where the unit stack is standing and starting their move from
	 * @param movingPlayerID Owner of the unit stack
	 * @param unitStack Unit stack to move
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param players List of players in this session
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param mem The player who is trying to move here's knowledge
	 * @param db Lookup lists built over the XML database
	 * @return Array listing all cells we can reach and the paths to get there
	 * @throws PlayerNotFoundException If an expected player can't be found
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If there is a problem with any of the calculations
	 */
	public OverlandMovementCell [] [] [] calculateOverlandMovementDistances (final MapCoordinates3DEx start, final int movingPlayerID,
		final UnitStack unitStack, final int doubleMovementRemaining, final List<? extends PlayerPublicDetails> players,
		final CoordinateSystem overlandMapCoordinateSystem, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Calculates how many (doubled) movement points it will take to move from x, y to ever other location in the combat map whether we can move there or not.
	 * 
	 * MoM is a little weird with how movement works - providing you have even 1/2 move left, you can move anywhere, even somewhere
	 * which takes 3 movement to get to - this can happen in combat as well, especially combats in cities when units can walk on the roads.
	 * 
	 * Therefore knowing distances to each location is not enough - we need a separate boolean array
	 * to mark whether we can or cannot reach each location - this is set in MovementTypes.
	 * 
	 * @param doubleMovementDistances Double the number of movement points it takes to move here, 0=free (enchanted road), negative=cannot reach
	 * @param movementDirections Trace of unit directions taken to reach here
	 * @param movementTypes Type of move (or lack of) for every location on the combat map (these correspond exactly to the X, move, attack, icons displayed in the client)
	 * @param unitBeingMoved The unit moving in combat
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on
	 * @param combatMap The details of the combat terrain
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public void calculateCombatMovementDistances (final int [] [] doubleMovementDistances, final int [] [] movementDirections,
		final CombatMovementType [] [] movementTypes, final ExpandedUnitDetails unitBeingMoved, final FogOfWarMemory fogOfWarMemory,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem,
		final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException;
}