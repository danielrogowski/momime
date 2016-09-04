package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_8.Race;

/**
 * Just for typecasting the list of city names
 */
public final class RaceSvr extends Race
{
	/**
	 * @return List of all city names used by this race
	 */
	@SuppressWarnings ("unchecked")
	public final List<CityNameContainerSvr> getCityNames ()
	{
		return (List<CityNameContainerSvr>) (List<?>) getCityName ();
	}
}