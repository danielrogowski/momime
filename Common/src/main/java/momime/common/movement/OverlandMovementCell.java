package momime.common.movement;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Stores details about how we can (or can't) move to a particular cell.  By starting at the cell the player clicks they want to go to, it must be possible to
 * trace back through all the cells required to reach there.
 * 
 * There has to be a traceable chain through the movement cells so we can start at chosen target and trace back through all the "movedFrom" cells and
 * get back to the start.  This means that:
 * 1) If clicking on a tower on Myrror, chosen target must adjust this immediately and treat moveTo as being on Arcanus before trying to trace the path.
 * 	(This is handled purely client side - the movement routines and AI players see the movement array the way it really should be, with tower locations on
 * 	 myrror greyed out and impassable).
 * 2) If at a tower, the 16 adjacent moves generated will all have moveFrom with the plane being Arcanus.
 * 3) If on Myrror and we generate an adjacent move to go to a tower, the move to cell for this will be on Arcanus, with moveFrom being on Myrror.
 * 
 * Cells with x, y coordinates of a tower and plane=Myrror in this array should be dead cells and never accessed.
 */
public final class OverlandMovementCell
{
	/** What type of move allowed us to get here; if this cell is known to be impassable, this will be null */
	private OverlandMovementType movementType;
	
	/** Where we moved here from; if this is where we started or a cell that we know is impassable, this will be null */
	private MapCoordinates3DEx movedFrom;

	/** What direction we moved to get here; only applicable for movementType ADJACENT; for other movement types and impassable cells, this will be 0 */
	private int direction;

	/** Movement cost to enter this tile; this basically serves as a cache so if we hit the same tile multiple times, we don't have to work this out again */
	private int doubleMovementToEnterTile;
	
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
	 * @return Movement cost to enter this tile; this basically serves as a cache so if we hit the same tile multiple times, we don't have to work this out again
	 */
	public final int getDoubleMovementToEnterTile ()
	{
		return doubleMovementToEnterTile;
	}

	/**
	 * @param m Movement cost to enter this tile; this basically serves as a cache so if we hit the same tile multiple times, we don't have to work this out again
	 */
	public final void setDoubleMovementToEnterTile (final int m)
	{
		doubleMovementToEnterTile = m;
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