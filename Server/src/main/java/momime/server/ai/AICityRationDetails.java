package momime.server.ai;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Stores details about rations produced by each city owner by the AI player; used by setOptionalFarmersInAllCities + findWorkersToConvertToFarmers
 */
final class AICityRationDetails
{
	/** Location of the city */
	private MapCoordinates3DEx cityLocation;
	
	/** Net gain in rations produced by the city, so production - how much civilians are eating */
	private int rationsProduced;
	
	/** Whether overfarming penalty is applying in this city (or whether it will if we add +1 more famer) */
	private boolean overfarming;
	
	/**
	 * @return Location of the city
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param c Location of the city
	 */
	public final void setCityLocation (final MapCoordinates3DEx c)
	{
		cityLocation = c;
	}
	
	/**
	 * @return Net gain in rations produced by the city, so production - how much civilians are eating
	 */
	public final int getRationsProduced ()
	{
		return rationsProduced;
	}
	
	/**
	 * @param r Net gain in rations produced by the city, so production - how much civilians are eating
	 */
	public final void setRationsProduced (final int r)
	{
		rationsProduced = r;
	}
	
	/**
	 * @return Whether overfarming penalty is applying in this city (or whether it will if we add +1 more famer)
	 */
	public final boolean isOverfarming ()
	{
		return overfarming;
	}

	/**
	 * @param o Whether overfarming penalty is applying in this city (or whether it will if we add +1 more famer)
	 */
	public final void setOverfarming (final boolean o)
	{
		overfarming = o;
	}
}