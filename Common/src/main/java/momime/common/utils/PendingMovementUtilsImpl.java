package momime.common.utils;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.messages.v0_9_5.PendingMovement;

/**
 * Helper methods for dealing with pending movement paths
 */
public final class PendingMovementUtilsImpl implements PendingMovementUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (PendingMovementUtilsImpl.class.getName ());
	
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
		log.entering (PendingMovementUtilsImpl.class.getName (), "removeUnitFromAnyPendingMoves", unitURN);
		
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
		
		log.exiting (PendingMovementUtilsImpl.class.getName (), "removeUnitFromAnyPendingMoves");
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
		log.entering (PendingMovementUtilsImpl.class.getName (), "removeUnitFromAnyPendingMoves", unitURN);
		
		final Iterator<PendingMovement> movesIter = moves.iterator ();
		while (movesIter.hasNext ())
		{
			final PendingMovement thisMove = movesIter.next ();
			if (thisMove.getUnitURN ().contains (unitURN))
				movesIter.remove ();
		}
		
		log.exiting (PendingMovementUtilsImpl.class.getName (), "removeUnitFromAnyPendingMoves");
	}
}
