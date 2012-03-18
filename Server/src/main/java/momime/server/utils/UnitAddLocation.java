package momime.server.utils;

import momime.common.messages.CoordinatesUtils;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.UnitAddBumpTypeID;

/**
 * Small class to allow multiple outputs from findNearestLocationWhereUnitCanBeAdded
 */
public final class UnitAddLocation
{
	/** Location to add unit; null if it won't fit */
	private final OverlandMapCoordinates unitLocation;

	/** Whether this is the requested location or not */
	private final UnitAddBumpTypeID bumpType;

	/**
	 * @param aUnitLocation Location to add unit; null if it won't fit
	 * @param aBumpType Whether this is the requested location or not
	 */
	UnitAddLocation (final OverlandMapCoordinates aUnitLocation, final UnitAddBumpTypeID aBumpType)
	{
		super ();

		unitLocation = aUnitLocation;
		bumpType = aBumpType;
	}

	/**
	 * @return Location to add unit; null if it won't fit
	 */
	public final OverlandMapCoordinates getUnitLocation ()
	{
		return unitLocation;
	}

	/**
	 * @return Whether this is the requested location or not
	 */
	public final UnitAddBumpTypeID getBumpType ()
	{
		return bumpType;
	}

	/**
	 * @return String for debug messages
	 */
	@Override
	public final String toString ()
	{
		return CoordinatesUtils.overlandMapCoordinatesToString (getUnitLocation ()) + "-" + getBumpType ();
	}
}
