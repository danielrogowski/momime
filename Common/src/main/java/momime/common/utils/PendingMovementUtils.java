package momime.common.utils;

import java.util.List;

import momime.common.messages.v0_9_4.PendingMovement;

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
}
