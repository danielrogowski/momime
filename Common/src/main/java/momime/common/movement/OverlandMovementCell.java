package momime.common.movement;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Stores details about how we can (or can't) move to a particular cell.  By starting at the cell the player clicks they want to go to, it must be possible to
 * trace back through all the cells required to reach there.
 */
public final class OverlandMovementCell
{
	/** What type of move allowed us to get here; if this cell is known to be impassable, this will be null */
	private OverlandMovementType movementType;
	
	/** Where we moved here from; if this is where we started or a cell that we know is impassable, this will be null */
	private MapCoordinates3DEx movedFrom;

	/** What direction we moved to get here; only applicable for movementType ADJACENT; for other movement types and impassable cells, this will be 0 */
	private int direction;
	
	/** How many movement points it took to get here; if this cell is known to be impassable, this will be MOVEMENT_DISTANCE_CANNOT_MOVE_HERE */
	private int doubleMovementDistance;

	/** Can we reach this cell in one turn */
	private boolean moveToInOneTurn;

	/**
	 * @return What type of move allowed us to get here; if this cell is known to be impassable, this will be null
	 */
	public final OverlandMovementType getMovementType ()
	{
		return movementType;
	}

	/**
	 * @param t What type of move allowed us to get here; if this cell is known to be impassable, this will be null
	 */
	public final void setMovementType (final OverlandMovementType t)
	{
		movementType = t;
	}
	
	/**
	 * @return Where we moved here from; if this is where we started or a cell that we know is impassable, this will be null
	 */
	public final MapCoordinates3DEx getMovedFrom ()
	{
		return movedFrom;
	}
	
	/**
	 * @param f Where we moved here from; if this is where we started or a cell that we know is impassable, this will be null
	 */
	public final void setMovedFrom (final MapCoordinates3DEx f)
	{
		movedFrom = f;
	}

	/**
	 * @return What direction we moved to get here; only applicable for movementType ADJACENT; for other movement types and impassable cells, this will be 0
	 */
	public final int getDirection ()
	{
		return direction;
	}

	/**
	 * @param d What direction we moved to get here; only applicable for movementType ADJACENT; for other movement types and impassable cells, this will be 0
	 */
	public final void setDirection (final int d)
	{
		direction = d;
	}

	/**
	 * @return How many movement points it took to get here; if this cell is known to be impassable, this will be MOVEMENT_DISTANCE_CANNOT_MOVE_HERE
	 */
	public final int getDoubleMovementDistance ()
	{
		return doubleMovementDistance;
	}

	/**
	 * @param d How many movement points it took to get here; if this cell is known to be impassable, this will be MOVEMENT_DISTANCE_CANNOT_MOVE_HERE
	 */
	public final void setDoubleMovementDistance (final int d)
	{
		doubleMovementDistance = d;
	}

	/**
	 * @return Can we reach this cell in one turn
	 */
	public final boolean isMoveToInOneTurn ()
	{
		return moveToInOneTurn;
	}
	
	/**
	 * @param o Can we reach this cell in one turn
	 */
	public final void setMoveToInOneTurn (final boolean o)
	{
		moveToInOneTurn = o;
	}
}