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
	
	/** Weapon grade of units constructed at this city */
	private final String weaponGradeImageFile;
	
	/** Number of our spells cast on this city */
	private final int enchantmentCount;
	
	/** Number of enemy spells cast on this city */ 
	private final int curseCount;

	/**
	 * @param aCityData All of the non calculated elements can be read straight from here
	 * @param aCityLocation Location of the city
	 * @param aCapital Whether this city contains the wizard's fortress
	 * @param aWeaponGradeImageFile Weapon grade of units constructed at this city
	 * @param anEnchantmentCount Number of our spells cast on this city
	 * @param aCurseCount Number of enemy spells cast on this city
	 */
	public CitiesListEntry (final OverlandMapCityData aCityData, final MapCoordinates3DEx aCityLocation, final boolean aCapital,
		final String aWeaponGradeImageFile, final int anEnchantmentCount, final int aCurseCount)
	{
		cityData = aCityData;
		cityLocation = aCityLocation;
		capital = aCapital;
		weaponGradeImageFile = aWeaponGradeImageFile;
		enchantmentCount = anEnchantmentCount;
		curseCount = aCurseCount;
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
	 * @return Minimum number of farmers to feed the civilian population
	 */
	public final int getMinimumFarmers ()
	{
		return cityData.getMinimumFarmers ();
	}
	
	/**
	 * @return Chosen number of optional additional farmers 
	 */
	public final int getOptionalFarmers ()
	{
		return cityData.getOptionalFarmers ();
	}
	
	/**
	 * @return Number of rebels
	 */
	public final int getNumberOfRebels ()
	{
		return cityData.getNumberOfRebels ();
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
	 * @return Weapon grade of units constructed at this city
	 */
	public final String getWeaponGradeImageFile ()
	{
		return weaponGradeImageFile;
	}
	
	/**
	 * @return Number of our spells cast on this city
	 */
	public final int getEnchantmentCount ()
	{
		return enchantmentCount;
	}
	
	/**
	 * @return Number of enemy spells cast on this city
	 */ 
	public final int getCurseCount ()
	{
		return curseCount;
	}
}