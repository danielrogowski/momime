package momime.common.messages;

import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Provides .equals () and .toString () for OverlandMapCoordinates
 */
public final class OverlandMapCoordinatesEx extends OverlandMapCoordinates
{
	/**
	 * @return String representation of coordinates 
	 */
	@Override
	public final String toString ()
	{
		return "(" + getX () + ", " + getY () + ", " + getZ () + ")";
	}

	/**
	 * @return Whether the two sets of coordinates are the same
	 */
	@Override
	public final boolean equals (final Object obj)
	{
		if (obj instanceof OverlandMapCoordinatesEx)
		{
			final OverlandMapCoordinatesEx coords = (OverlandMapCoordinatesEx) obj;
			return ((getX () == coords.getX ()) && (getY () == coords.getY ()) && (getZ () == coords.getZ ()));
		}
		else
			return super.equals (obj);
	}
}
