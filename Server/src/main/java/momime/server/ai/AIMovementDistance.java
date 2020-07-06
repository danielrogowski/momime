package momime.server.ai;

/**
 * AI principally sees movement distances in terms of how many movement points it takes to get there, but when there's
 * enchanted road everywhere and all cells take 0 movement points to reach, that's not really good enough and makes
 * the AI too non-deterministic.  So we need a secondary measure of the actual distance (number of map cells)
 * away that a target cell is.
 */
final class AIMovementDistance
{
	/** Double the number of movement points it will take to reach this cell */
	private final int doubleMovementDistance;
	
	/** Number of actual map cells away that this cell is, regardless of how much movement they cost to get over */
	private final int mapCellDistance;
	
	/**
	 * @param aDoubleMovementDistance Double the number of movement points it will take to reach this cell
	 * @param aMapCellDistance Number of actual map cells away that this cell is, regardless of how much movement they cost to get over
	 */
	AIMovementDistance (final int aDoubleMovementDistance, final int aMapCellDistance)
	{
		doubleMovementDistance = aDoubleMovementDistance;
		mapCellDistance = aMapCellDistance;
	}

	/**
	 * @param otherObj Another distance obj to compare against
	 * @return True if this obj is exactly the same distance than the other one
	 */
	@Override
	public final boolean equals (final Object otherObj)
	{
		final boolean equal;
		if (otherObj instanceof AIMovementDistance)
		{
			final AIMovementDistance other = (AIMovementDistance) otherObj;
			equal = (getDoubleMovementDistance () == other.getDoubleMovementDistance ()) && (getMapCellDistance () == other.getMapCellDistance ());
		}
		else
			equal = false;
		
		return equal;
	}
	
	/**
	 * @param other Another distance obj to compare against
	 * @return True if this obj is shorter distance than the other one
	 */
	public final boolean isShorterThan (final AIMovementDistance other)
	{
		return (getDoubleMovementDistance () < other.getDoubleMovementDistance ()) ||
			((getDoubleMovementDistance () == other.getDoubleMovementDistance ()) && (getMapCellDistance () < other.getMapCellDistance ()));
	}
	
	/**
	 * @return String representation of class values, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return getDoubleMovementDistance () + "|" + getMapCellDistance ();
	}

	/**
	 * @return Double the number of movement points it will take to reach this cell
	 */
	public final int getDoubleMovementDistance ()
	{
		return doubleMovementDistance;
	}
	
	/**
	 * @return Number of actual map cells away that this cell is, regardless of how much movement they cost to get over
	 */
	public final int getMapCellDistance ()
	{
		return mapCellDistance;
	}
}