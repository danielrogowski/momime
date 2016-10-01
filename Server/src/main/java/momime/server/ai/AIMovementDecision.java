package momime.server.ai;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * As the AI considers various movement codes for each unit stack, each of the decision methods outputs either this structure, or a null:
 * 
 * null (instead of this object) = nothing to do with this movement code, try another
 * destination is null = happy where we are, don't move, but don't try more codes either
 * destination is filled in = location to head for
 */
final class AIMovementDecision
{
	/** Null for the unit to stay happy where it is; or a location for the unit to head there */
	private final MapCoordinates3DEx destination;
	
	/**
	 * @param aDestination Null for the unit to stay happy where it is; or a location for the unit to head there
	 */
	AIMovementDecision (final MapCoordinates3DEx aDestination)
	{
		destination = aDestination;
	}
	
	/**
	 * @return String representation of values
	 */
	@Override
	public final String toString ()
	{
		return isStayHere () ? "Stay here" : getDestination ().toString ();
	}

	/**
	 * @return Null for the unit to stay happy where it is; or a location for the unit to head there
	 */
	public final MapCoordinates3DEx getDestination ()
	{
		return destination;
	}

	/**
	 * @return Whether or not the unit should stay happy where it is
	 */
	public final boolean isStayHere ()
	{
		return (destination == null);
	}
}