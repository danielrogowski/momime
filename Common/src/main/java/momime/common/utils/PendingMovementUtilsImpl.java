package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.messages.PendingMovement;

/**
 * Helper methods for dealing with pending movement paths
 */
public final class PendingMovementUtilsImpl implements PendingMovementUtils
{
	/**
	 * Cancels any pending moves for this unit
	 * If this is the last unit in any pending moves, then the pending move itself is removed
	 * 
	 * @param moves List of pending movements to work with
	 * @param unitURN Unit to remove from pending movements
	 */
	@Override
	public final void removeUnitFromAnyPendingMoves (final List<PendingMovement> moves, final int unitURN)
	{
		final Iterator<PendingMovement> movesIter = moves.iterator ();
		while (movesIter.hasNext ())
		{
			final PendingMovement thisMove = movesIter.next ();
			
			boolean anyUnitsLeft = false;
			final Iterator<Integer> unitsIter = thisMove.getUnitURN ().iterator ();
			while (unitsIter.hasNext ())
			{
				final Integer thisUnit = unitsIter.next ();
				
				if (thisUnit == unitURN)
					unitsIter.remove ();
				else
					anyUnitsLeft = true;
			}
			
			// Remove this pending movement totally?
			if (!anyUnitsLeft)
				movesIter.remove ();
		}
	}
	
	/**
	 * Cancels any pending moves for this unit, and any other units stacked with it
	 * 
	 * @param moves List of pending movements to work with
	 * @param unitURN Unit to remove from pending movements
	 */
	@Override
	public final void removeAnyPendingMovesThatIncludeUnit (final List<PendingMovement> moves, final int unitURN)
	{
		final Iterator<PendingMovement> movesIter = moves.iterator ();
		while (movesIter.hasNext ())
		{
			final PendingMovement thisMove = movesIter.next ();
			if (thisMove.getUnitURN ().contains (unitURN))
				movesIter.remove ();
		}
	}

	/**
	 * Searches for a pending movement that includes this unit
	 * 
	 * @param unitURN Unit to search for
	 * @return Pending movement that includes this unit if there is one; null if none was found
	 */
	@Override
	public final PendingMovement findPendingMoveForUnit (final List<PendingMovement> moves, final int unitURN)
	{
		PendingMovement found = null;
		final Iterator<PendingMovement> movesIter = moves.iterator ();
		while ((found == null) && (movesIter.hasNext ()))
		{
			final PendingMovement thisMove = movesIter.next ();
			if (thisMove.getUnitURN ().contains (unitURN))
				found = thisMove;
		}

		return found;
	}
}