package momime.client.ui.renderer;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.messages.OverlandMapCityData;

/**
 * Stores summary information about a city.  Some of these values (e.g. production, max city size) are expensive to compute and
 * so we don't want to do so repeatedly, e.g. during sorting or every time the list is redisplayed.  So a list of these objects is
 * generated to cache the values.
 */
public final class CitiesListEntry
{
	/** All of the non calculated elements can be read straight from here */
	private final OverlandMapCityData cityData;
	
	/** Location of the city */
	private final MapCoordinates3DEx cityLocation;
	
	/** Whether this city contains the wizard's fortress */
	private final boolean capital;
	
	/** Amount of rations being harvested - amount of rations being eaten by population */
	private final int rations;
	
	/** Gold being generated - upkeep of buildings */
	private final int gold;
	
	/** Production generated each turn */ 
	private final int production;

	/**
	 * @param aCityData All of the non calculated elements can be read straight from here
	 * @param aCityLocation Location of the city
	 * @param aCapital Whether this city contains the wizard's fortress
	 * @param aRations Amount of rations being harvested - amount of rations being eaten by population
	 * @param aGold Gold being generated - upkeep of buildings
	 * @param aProduction Production generated each turn
	 */
	public CitiesListEntry (final OverlandMapCityData aCityData, final MapCoordinates3DEx aCityLocation, final boolean aCapital, final int aRations, final int aGold, final int aProduction)
	{
		cityData = aCityData;
		cityLocation = aCityLocation;
		capital = aCapital;
		rations = aRations;
		gold = aGold;
		production = aProduction;
	}

	/**
	 * @return Name of the city
	 */
	public final String getCityName ()
	{
		return cityData.getCityName ();
	}
	
	/**
	 * @return Race inhabiting the city
	 */
	public final String getCityRaceID ()
	{
		return cityData.getCityRaceID ();
	}
	
	/**
	 * @return Building currently being constructed; or null if a unit is being constructed
	 */
	public final String getCurrentlyConstructingBuildingID ()
	{
		return cityData.getCurrentlyConstructingBuildingID ();
	}
	
	/**
	 * @return Unit currently being constructed; or null if a building is being constructed
	 */
	public final String getCurrentlyConstructingUnitID ()
	{
		return cityData.getCurrentlyConstructingUnitID ();
	}
	
	/**
	 * @return Population of the city
	 */
	public final int getCityPopulation ()
	{
		return cityData.getCityPopulation ();
	}
	
	/**
	 * @return Location of the city
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}
	
	/**
	 * @return Whether this city contains the wizard's fortress
	 */
	public final boolean isCapital ()
	{
		return capital;
	}
	
	/**
	 * @return Amount of rations being harvested - amount of rations being eaten by population
	 */
	public final int getRations ()
	{
		return rations;
	}
	
	/**
	 * @return Gold being generated - upkeep of buildings
	 */
	public final int getGold ()
	{
		return gold;
	}
	
	/**
	 * @return Production generated each turn
	 */ 
	public final int getProduction ()
	{
		return production;
	}
}