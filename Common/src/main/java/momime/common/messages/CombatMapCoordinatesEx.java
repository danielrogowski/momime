package momime.common.messages;

import momime.common.messages.v0_9_4.CombatMapCoordinates;

/**
 * Provides .equals () and .toString () for CombatMapCoordinates
 */
public final class CombatMapCoordinatesEx extends CombatMapCoordinates
{
	/**
	 * @return String representation of coordinates 
	 */
	@Override
	public final String toString ()
	{
		return "(" + getX () + ", " + getY () + ")";
	}

	/**
	 * @return Whether the two sets of coordinates are the same
	 */
	@Override
	public final boolean equals (final Object obj)
	{
		if (obj instanceof CombatMapCoordinatesEx)
		{
			final CombatMapCoordinatesEx coords = (CombatMapCoordinatesEx) obj;
			return ((getX () == coords.getX ()) && (getY () == coords.getY ()));
		}
		else
			return super.equals (obj);
	}
}
