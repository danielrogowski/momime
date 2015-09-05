package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_7.PickTypeCountContainer;

/**
 * Just for typecasting the list of free spells
 */
public final class PickTypeCountContainerSvr extends PickTypeCountContainer
{
	/**
	 * @return The number of spells, and free spells at start, that having a certain number of picks will grant
	 */
	@SuppressWarnings ("unchecked")
	public final List<PickTypeGrantsSpellsSvr> getSpellCounts ()
	{
		return (List<PickTypeGrantsSpellsSvr>) (List<?>) getSpellCount ();
	}
}