package momime.common.messages;

import momime.common.messages.v0_9_4.CombatMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Methods for working with OverlandMapCoordinates and CombatMapCoordinates
 *
 * All of MoM IME should use these versions instead of the versions on CoordinateSystemUtils, beause of the additional plane coordinate
 * In particular CoordinateSystemUtils.mapCoordinatesEqual (overlandCoords1, overlandCoords2) will give incorrect results because it doesn't check the plane
 */
public final class CoordinatesUtils
{
	/**
	 * @param coords Overland map coordinates
	 * @return String representation, for debug messages
	 */
	public static final String overlandMapCoordinatesToString (final OverlandMapCoordinates coords)
	{
		final String result;
		if (coords == null)
			result = "(null)";
		else
			result = "(" + coords.getX () + ", " + coords.getY () + ", " + coords.getPlane () + ")";

		return result;
	}

	/**
	 * @param firstCoords First set of overland map coordinates to compare
	 * @param secondCoords Second set of overland map coordinates to compare
	 * @param considerTwoNullsAsEqual Specifies what result to return if both inputs are passed in null
	 * @return Whether the two sets of coordinates are the same
	 */
	public static final boolean overlandMapCoordinatesEqual (final OverlandMapCoordinates firstCoords, final OverlandMapCoordinates secondCoords, final boolean considerTwoNullsAsEqual)
	{
		final boolean result;
		if ((firstCoords == null) && (secondCoords == null))
			result = considerTwoNullsAsEqual;
		else if ((firstCoords == null) || (secondCoords == null))
			result = false;
		else
			result = ((firstCoords.getX () == secondCoords.getX ()) && (firstCoords.getY () == secondCoords.getY ()) && (firstCoords.getPlane () == secondCoords.getPlane ()));

		return result;
	}

	/**
	 * @param coords Combat map coordinates
	 * @return String representation, for debug messages
	 */
	public static final String combatMapCoordinatesToString (final CombatMapCoordinates coords)
	{
		final String result;
		if (coords == null)
			result = "(null)";
		else
			result = "(" + coords.getX () + ", " + coords.getY () + ")";

		return result;
	}

	/**
	 * @param firstCoords First set of combat map coordinates to compare
	 * @param secondCoords Second set of combat map coordinates to compare
	 * @param considerTwoNullsAsEqual Specifies what result to return if both inputs are passed in null
	 * @return Whether the two sets of coordinates are the same
	 */
	public static final boolean combatMapCoordinatesEqual (final CombatMapCoordinates firstCoords, final CombatMapCoordinates secondCoords, final boolean considerTwoNullsAsEqual)
	{
		final boolean result;
		if ((firstCoords == null) && (secondCoords == null))
			result = considerTwoNullsAsEqual;
		else if ((firstCoords == null) || (secondCoords == null))
			result = false;
		else
			result = ((firstCoords.getX () == secondCoords.getX ()) && (firstCoords.getY () == secondCoords.getY ()));

		return result;
	}

	/**
	 * Prevent instantiation
	 */
	private CoordinatesUtils ()
	{
	}
}
