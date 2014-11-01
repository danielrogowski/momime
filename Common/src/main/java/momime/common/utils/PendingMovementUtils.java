package momime.common.utils;

import java.util.List;

import momime.common.messages.PendingMovement;

/**
 * Helper methods for dealing with pending movement paths
 */
public interface PendingMovementUtils
{
	/**
	 * Cancels any pending moves for this unit
	 * If this is the last unit in any pending moves, then the pending move itself is removed
	 * 
	 * @param moves List of pending movements to work with
	 * @param unitURN Unit to remove from pending movements
	 */
	public void removeUnitFromAnyPendingMoves (final List<PendingMovement> moves, final int unitURN);

	/**
	 * Cancels any pending moves for this unit, and any other units stacked with it
	 * 
	 * @param moves List of pending movements to work with
	 * @param unitURN Unit to remove from pending movements
	 */
	public void removeAnyPendingMovesThatIncludeUnit (final List<PendingMovement> moves, final int unitURN);
	
	/**
	 * Searches for a pending movement that includes this unit
	 * 
	 * @param moves List of pending movements to work with
	 * @param unitURN Unit to search for
	 * @return Pending movement that includes this unit if there is one; null if none was found
	 */
	public PendingMovement findPendingMoveForUnit (final List<PendingMovement> moves, final int unitURN);
}