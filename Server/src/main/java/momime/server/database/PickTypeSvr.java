package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_9.PickType;

/**
 * Just for typecasting the list of pick type counts
 */
public final class PickTypeSvr extends PickType
{
	/**
	 * @return List of bonuses gained from having a certain number of picks
	 */
	@SuppressWarnings ("unchecked")
	public final List<PickTypeCountContainerSvr> getPickTypeCounts ()
	{
		return (List<PickTypeCountContainerSvr>) (List<?>) getPickTypeCount ();
	}
}