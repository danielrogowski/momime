package momime.common.calculations;

/**
 * Stores the breakdown of all the values used in calculating the number of rebels in a city
 */
public final class CalculateCityUnrestBreakdown
{
	/** % unrest from taxation rate */
	private final int taxPercentage;

	/** % unrest from how much the race inhabiting this city distrusts our capital race */
	private final int racialPercentage;

	/** Total % unrest, i.e. taxPercentage + racialPercentage */
	private final int totalPercentage;

	/** Population of city in 1000s */
	private final int population;

	/** Unrest generated from totalPercentage */
	private final int baseValue;

	/** Literal unrest modifier (i.e. not percentage-based) between race inhabiting this city and our capital race (used for Klackons special -2 modifier) */
	private final int racialLiteral;

	/** % that our retorts (Divine or Infernal Power) are improving the effectiveness of reglious buildings by; only included if we actually have some religious buildings */
	private final int religiousBuildingRetortPercentage;

	/** List of retorts contributing to religiousBuildingRetortPercentage */
	private final String [] pickIdsContributingToReligiousBuildingBonus;

	/** Unrest reduction from religious buildings, before applying % bonus from retorts; only included if we actually have a suitable retort; will be a negative value */
	private final int religiousBuildingReduction;

	/** Additional unrest reduction from the retort bonus to our religious buildings; will be a negative value */
	private final int religiousBuildingRetortValue;

	/** Number of non-summoned units in the city */
	private final int unitCount;

	/** Amount of unrest reduction from non-summoned units in the city; will be a negative value */
	private final int unitReduction;

	/** Total unrest, before applying bounding limits */
	private final int baseTotal;

	/** True if baseTotal was negative and we forced it up to zero */
	private final boolean forcePositive;

	/** True if baseTotal was more than the population of the city (!) and we forced it back to the total population all being rebels */
	private final boolean forceAll;

	/** Minimum farmers in the city necessary to feed the population; only included if the number of rebels was decreased to allow the minimum number of farmers */
	private final int minimumFarmers;

	/** Number rebels were reduced to in order to allow some rebels to satisfy the minimum number of farmers; only included if the number of rebels was decreased to allow the minimum number of farmers */
	private final int totalAfterFarmers;

	/** Final total number of rebels */
	private final int finalTotal;

	/** List of buildings that are reducing unrest and how much each is reducing it by */
	private final CalculateCityUnrestBreakdown_Building [] buildingsReducingUnrest;

	/**
	 * @param aTaxPercentage % unrest from taxation rate
	 * @param aRacialPercentage % unrest from how much the race inhabiting this city distrusts our capital race
	 * @param aTotalPercentage Total % unrest, i.e. taxPercentage + racialPercentage
	 * @param aPopulation Population of city in 1000s
	 * @param aBaseValue Unrest generated from totalPercentage
	 * @param aRacialLiteral Literal unrest modifier (i.e. not percentage-based) between race inhabiting this city and our capital race (used for Klackons special -2 modifier)
	 * @param aReligiousBuildingRetortPercentage % that our retorts (Divine or Infernal Power) are improving the effectiveness of reglious buildings by; only included if we actually have some religious buildings
	 * @param aPickIdsContributingToReligiousBuildingBonus List of retorts contributing to religiousBuildingRetortPercentage
	 * @param aReligiousBuildingReduction Unrest reduction from religious buildings, before applying % bonus from retorts; only included if we actually have a suitable retort; will be a negative value
	 * @param aReligiousBuildingRetortValue Additional unrest reduction from the retort bonus to our religious buildings; will be a negative value
	 * @param aUnitCount Number of non-summoned units in the city
	 * @param aUnitReduction Amount of unrest reduction from non-summoned units in the city; will be a negative value
	 * @param aBaseTotal Total unrest, before applying bounding limits
	 * @param aForcePositive True if baseTotal was negative and we forced it up to zero
	 * @param aForceAll True if baseTotal was more than the population of the city (!) and we forced it back to the total population all being rebels
	 * @param aMinimumFarmers Minimum farmers in the city necessary to feed the population; only included if the number of rebels was decreased to allow the minimum number of farmers
	 * @param aTotalAfterFarmers Number rebels were reduced to in order to allow some rebels to satisfy the minimum number of farmers; only included if the number of rebels was decreased to allow the minimum number of farmers
	 * @param aFinalTotal Final total number of rebels
	 * @param aBuildingsReducingUnrest List of buildings that are reducing unrest and how much each is reducing it by
	 */
	CalculateCityUnrestBreakdown (final int aTaxPercentage, final int aRacialPercentage, final int aTotalPercentage, final int aPopulation, final int aBaseValue,
		final int aRacialLiteral, final int aReligiousBuildingRetortPercentage, final int aReligiousBuildingReduction, final int aReligiousBuildingRetortValue,
		final int aUnitCount, final int aUnitReduction, final int aBaseTotal,
		final boolean aForcePositive, final boolean aForceAll, final int aMinimumFarmers, final int aTotalAfterFarmers, final int aFinalTotal,
		final CalculateCityUnrestBreakdown_Building [] aBuildingsReducingUnrest, final String [] aPickIdsContributingToReligiousBuildingBonus)
	{
		taxPercentage = aTaxPercentage;
		racialPercentage = aRacialPercentage;
		totalPercentage = aTotalPercentage;
		population = aPopulation;
		baseValue = aBaseValue;
		racialLiteral = aRacialLiteral;
		religiousBuildingRetortPercentage = aReligiousBuildingRetortPercentage;
		religiousBuildingReduction = aReligiousBuildingReduction;
		religiousBuildingRetortValue = aReligiousBuildingRetortValue;
		unitCount = aUnitCount;
		unitReduction = aUnitReduction;
		baseTotal = aBaseTotal;
		forcePositive = aForcePositive;
		forceAll = aForceAll;
		minimumFarmers = aMinimumFarmers;
		totalAfterFarmers = aTotalAfterFarmers;
		finalTotal = aFinalTotal;
		buildingsReducingUnrest = aBuildingsReducingUnrest;
		pickIdsContributingToReligiousBuildingBonus = aPickIdsContributingToReligiousBuildingBonus;
	}

	/**
	 * @return % unrest from taxation rate
	 */
	public final int getTaxPercentage ()
	{
		return taxPercentage;
	}

	/**
	 * @return % unrest from how much the race inhabiting this city distrusts our capital race
	 */
	public final int getRacialPercentage ()
	{
		return racialPercentage;
	}

	/**
	 * @return Total % unrest, i.e. taxPercentage + racialPercentage
	 */
	public final int getTotalPercentage ()
	{
		return totalPercentage;
	}

	/**
	 * @return Population of city in 1000s
	 */
	public final int getPopulation ()
	{
		return population;
	}

	/**
	 * @return Unrest generated from totalPercentage
	 */
	public final int getBaseValue ()
	{
		return baseValue;
	}

	/**
	 * @return Literal unrest modifier (i.e. not percentage-based) between race inhabiting this city and our capital race (used for Klackons special -2 modifier)
	 */
	public final int getRacialLiteral ()
	{
		return racialLiteral;
	}

	/**
	 * @return % that our retorts (Divine or Infernal Power) are improving the effectiveness of reglious buildings by; only included if we actually have some religious buildings
	 */
	public final int getReligiousBuildingRetortPercentage ()
	{
		return religiousBuildingRetortPercentage;
	}

	/**
	 * @return List of retorts contributing to religiousBuildingRetortPercentage
	 */
	public final String [] getPickIdsContributingToReligiousBuildingBonus ()
	{
		return pickIdsContributingToReligiousBuildingBonus;
	}

	/**
	 * @return Unrest reduction from religious buildings, before applying % bonus from retorts; only included if we actually have a suitable retort; will be a negative value
	 */
	public final int getReligiousBuildingReduction ()
	{
		return religiousBuildingReduction;
	}

	/**
	 * @return Additional unrest reduction from the retort bonus to our religious buildings; will be a negative value
	 */
	public final int getReligiousBuildingRetortValue ()
	{
		return religiousBuildingRetortValue;
	}

	/**
	 * @return Number of non-summoned units in the city
	 */
	public final int getUnitCount ()
	{
		return unitCount;
	}

	/**
	 * @return Amount of unrest reduction from non-summoned units in the city; will be a negative value
	 */
	public final int getUnitReduction ()
	{
		return unitReduction;
	}

	/**
	 * @return Total unrest, before applying bounding limits
	 */
	public final int getBaseTotal ()
	{
		return baseTotal;
	}

	/**
	 * @return True if baseTotal was negative and we forced it up to zero
	 */
	public final boolean getForcePositive ()
	{
		return forcePositive;
	}

	/**
	 * @return True if baseTotal was more than the population of the city (!) and we forced it back to the total population all being rebels
	 */
	public final boolean getForceAll ()
	{
		return forceAll;
	}

	/**
	 * @return Minimum farmers in the city necessary to feed the population; only included if the number of rebels was decreased to allow the minimum number of farmers
	 */
	public final int getMinimumFarmers ()
	{
		return minimumFarmers;
	}

	/**
	 * @return Number rebels were reduced to in order to allow some rebels to satisfy the minimum number of farmers; only included if the number of rebels was decreased to allow the minimum number of farmers
	 */
	public final int getTotalAfterFarmers ()
	{
		return totalAfterFarmers;
	}

	/**
	 * @return Final total number of rebels
	 */
	public final int getFinalTotal ()
	{
		return finalTotal;
	}

	/**
	 * @return List of buildings that are reducing unrest and how much each is reducing it by
	 */
	public final CalculateCityUnrestBreakdown_Building [] getBuildingsReducingUnrest ()
	{
		return buildingsReducingUnrest;
	}
}
