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
		final UnitStack unitStack, final int doubleMovementRemaining,
		final List<? extends PlayerPublicDetails> players, final CoordinateSystem overlandMapCoordinateSystem, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}