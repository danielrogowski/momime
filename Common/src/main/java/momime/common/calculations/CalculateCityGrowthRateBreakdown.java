package momime.common.calculations;

/**
 * Stores the breakdown of all the values used in calculating the population growth rate/death rate in a city
 */
public final class CalculateCityGrowthRateBreakdown
{
	/** Current population of the city */
	private final int currentPopulation;

	/** Maximum population of the city */
	private final int maximumPopulation;

	/** Whether the population is growing, dying or at maximum size */
	private final MomCityGrowthDirection direction;

	/** Basic calculated growth rate (only filled in if direction = GROWING) */
	private final int baseGrowthRate;

	/** Racial modifier to growth rate (only filled in if direction = GROWING) */
	private final int racialGrowthModifier;

	/** List of buildings that are altering growth rate and how much each is altering it by (only filled in if direction = GROWING, otherwise null) */
	private final CalculateCityGrowthRateBreakdown_Building [] buildingsModifyingGrowthRate;

	/** Total growth = base + racial + buildings (only filled in if direction = GROWING) */
	private final int totalGrowthRate;

	/** Total growth rate, possibly reduced to prevent city going over maximum size (only filled in if direction = GROWING) */
	private final int cappedGrowthRate;

	/** Sets of 1,000 people or part thereof that we are in excess of maximum population (only filled in if direction = DYING) */
	private final int baseDeathRate;

	/** Number of people dying each turn (only filled in if direction = DYING) */
	private final int cityDeathRate;

	/** Either cappedGrowthRate or -cityDeathRate, as appropriate */
	private final int finalTotal;

	/**
	 * @param aCurrentPopulation Current population of the city
	 * @param aMaximumPopulation Maximum population of the city
	 * @param aDirection Whether the population is growing, dying or at maximum size
	 * @param aBaseGrowthRate Basic calculated growth rate (only filled in if direction = GROWING)
	 * @param aRacialGrowthModifier Racial modifier to growth rate (only filled in if direction = GROWING)
	 * @param aBuildingsModifyingGrowthRate List of buildings that are altering growth rate and how much each is altering it by (only filled in if direction = GROWING, otherwise null)
	 * @param aTotalGrowthRate Total growth = base + racial + buildings (only filled in if direction = GROWING)
	 * @param aCappedGrowthRate Total growth rate, possibly reduced to prevent city going over maximum size (only filled in if direction = GROWING)
	 * @param aBaseDeathRate Sets of 1,000 people or part thereof that we are in excess of maximum population (only filled in if direction = DYING)
	 * @param aCityDeathRate Number of people dying each turn (only filled in if direction = DYING)
	 * @param aFinalTotal Either totalGrowthRate or -cityDeathRate, as appropriate
	 */
	CalculateCityGrowthRateBreakdown (final int aCurrentPopulation, final int aMaximumPopulation, final MomCityGrowthDirection aDirection,
		final int aBaseGrowthRate, final int aRacialGrowthModifier, final CalculateCityGrowthRateBreakdown_Building [] aBuildingsModifyingGrowthRate,
		final int aTotalGrowthRate, final int aCappedGrowthRate, final int aBaseDeathRate, final int aCityDeathRate, final int aFinalTotal)
	{
		currentPopulation = aCurrentPopulation;
		maximumPopulation = aMaximumPopulation;
		direction = aDirection;

		baseGrowthRate = aBaseGrowthRate;
		racialGrowthModifier = aRacialGrowthModifier;
		buildingsModifyingGrowthRate = aBuildingsModifyingGrowthRate;
		totalGrowthRate = aTotalGrowthRate;
		cappedGrowthRate = aCappedGrowthRate;

		baseDeathRate = aBaseDeathRate;
		cityDeathRate = aCityDeathRate;

		finalTotal = aFinalTotal;
	}

	/**
	 * @return Current population of the city
	 */
	public final int getCurrentPopulation ()
	{
		return currentPopulation;
	}

	/**
	 * @return Maximum population of the city
	 */
	public final int getMaximumPopulation ()
	{
		return maximumPopulation;
	}

	/**
	 * @return Whether the population is growing, dying or at maximum size
	 */
	public final MomCityGrowthDirection getDirection ()
	{
		return direction;
	}

	/**
	 * @return Basic calculated growth rate (only filled in if direction = GROWING)
	 */
	public final int getBaseGrowthRate ()
	{
		return baseGrowthRate;
	}

	/**
	 * @return Racial modifier to growth rate (only filled in if direction = GROWING)
	 */
	public final int getRacialGrowthModifier ()
	{
		return racialGrowthModifier;
	}

	/**
	 * @return List of buildings that are altering growth rate and how much each is altering it by (only filled in if direction = GROWING, otherwise null)
	 */
	public final CalculateCityGrowthRateBreakdown_Building [] getBuildingsModifyingGrowthRate ()
	{
		return buildingsModifyingGrowthRate;
	}

	/**
	 * @return Total growth = base + racial + buildings (only filled in if direction = GROWING)
	 */
	public final int getTotalGrowthRate ()
	{
		return totalGrowthRate;
	}

	/**
	 * @return Total growth rate, possibly reduced to prevent city going over maximum size (only filled in if direction = GROWING)
	 */
	public final int getCappedGrowthRate ()
	{
		return cappedGrowthRate;
	}

	/**
	 * @return Sets of 1,000 people or part thereof that we are in excess of maximum population (only filled in if direction = DYING)
	 */
	public final int getBaseDeathRate ()
	{
		return baseDeathRate;
	}

	/**
	 * @return Number of people dying each turn (only filled in if direction = DYING)
	 */
	public final int getCityDeathRate ()
	{
		return cityDeathRate;
	}

	/**
	 * @return Either cappedGrowthRate or -cityDeathRate, as appropriate
	 */
	public final int getFinalTotal ()
	{
		return finalTotal;
	}
}
