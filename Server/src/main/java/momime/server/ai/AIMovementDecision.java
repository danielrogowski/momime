package momime.server.ai;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.UnitSpecialOrder;

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
	
	/** Set to a value to make the unit execute a special order instead of moving */
	private final UnitSpecialOrder specialOrder;
	
	/**
	 * @param aDestination Null for the unit to stay happy where it is; or a location for the unit to head there
	 */
	AIMovementDecision (final MapCoordinates3DEx aDestination)
	{
		destination = aDestination;
		specialOrder = null;
	}
	
	/**
	 * @param aSpecialOrder Set to a value to make the unit execute a special order instead of moving
	 */
	AIMovementDecision (final UnitSpecialOrder aSpecialOrder)
	{
		destination = null;
		specialOrder = aSpecialOrder;
	}
	
	/**
	 * @return String representation of values
	 */
	@Override
	public final String toString ()
	{
		final String s;
		if (getSpecialOrder () != null)
			s = getSpecialOrder ().toString ();
		else
			s = isStayHere () ? "Stay here" : getDestination ().toString ();
		
		return s;
	}

	/**
	 * @return Null for the unit to stay happy where it is; or a location for the unit to head there
	 */
	public final MapCoordinates3DEx getDestination ()
	{
		return destination;
	}

	/**
	 * @return Set to a value to make the unit execute a special order instead of moving
	 */
	public final UnitSpecialOrder getSpecialOrder ()
	{
		return specialOrder;
	}
	
	/**
	 * @return Whether or not the unit should stay happy where it is
	 */
	public final boolean isStayHere ()
	{
		return (getDestination () == null) && (getSpecialOrder () == null);
	}
}