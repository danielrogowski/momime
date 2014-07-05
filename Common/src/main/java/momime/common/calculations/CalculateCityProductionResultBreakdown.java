package momime.common.calculations;

import momime.common.database.v0_9_5.RoundingDirectionID;

/**
 * Stores one component of the detailed breakdown of the calculation that produced a city production value.
 *   
 * The server never sets storeBreakdown to true so this never gets used, it only cares about the end calculated value, but the client uses this
 * both to display the breakdown in words, and to draw the production icons on the city screen.
 */
public final class CalculateCityProductionResultBreakdown
{
	/** Which type of civilian (e.g. Farmers or Workers) is generating this production (P) */
	private final String populationTaskID;
	
	/** Which building is generating this production (P,C,%) */
	private final String buildingID;
	
	/** Which pick type (books or retorts) is generating this production (this is used for spell books generating magic power at your fortress city) (P) */
	private final String pickTypeID;
	
	/** Which plane is generating this production (this is used for your fortress generating +5 magic power if on Myrror) (P) */
	private final Integer planeNumber;
	
	/** Which type of map feature (e.g. Quork crystals) is generating this production (P) */
	private final String mapFeatureID;
	
	/** City population used when calculating the cap value (what value the gold trade bonus is capped at depends on the city population) (A) */
	private final int currentPopulation;
	
	/** Number of civilians doing the task (e.g. Farmers or Workers) that is generating this production (P,C) */
	private final int numberDoingTask;
	
	/** Double the amount of generating each civilian is producing (P) */
	private final int doubleAmountPerPerson;
	
	/** Double the amount of production each pick (i.e. spell book, see pickTypeID parameter) is generating (P) */
	private final int doubleAmountPerPick;
	
	/** Basic production calculated from the above - this is only included if there is a bonus to apply, otherwise left as zero and value will be in doubleProductionAmount (P,H) */
	private final int doubleUnmodifiedProductionAmount;
	
	/** Whole number multipler to apply to mineral bonuses for this race (i.e. to give dwarves 2x bonuses from mines) (P) */
	private final int raceMineralBonusMultipler;
	
	/** Basic production after race mineral bonus applied (P) */
	private final int doubleAmountAfterRacialBonus;
	
	/** % bonus to add onto the basic production amount (or cumulatively onto the amount including the race mineral bonus, if applicable) (P) */
	private final int percentageBonus;
	
	/** Final production amount after taking all item-level bonuses into account (P,H) */
	private final int doubleProductionAmount;
	
	/** Consumption amount (C) */
	private final int consumptionAmount;
	
	/** Overall percentage bonus to apply on top of final production amount (e.g. gold bonus from Marketplace, cumulative on top of racial/Miners' Guild mineral bonus) (%) */
	private final int percentage;
	
	/** Cap value applied (A) */
	private final int cap;
	
	/** Rounding direction used to halve production value (H) */
	private final RoundingDirectionID roundingDirection;
	
	/**
	 * The (P,C,%,A,H) at the end of each parameter comment indicates whether this is used for production, consumption, percentage bonus, cap or
	 * halving the production amount (it is initially calculated doubled); any values not applicable will be left as 0 or null
	 *
	 * @param aPopulationTaskID Which type of civilian (e.g. Farmers or Workers) is generating this production (P)
	 * @param aBuildingID Which building is generating this production (P,C,%)
	 * @param aPickTypeID Which pick type (books or retorts) is generating this production (this is used for spell books generating magic power at your fortress city) (P)
	 * @param aPlaneNumber Which plane is generating this production (this is used for your fortress generating +5 magic power if on Myrror) (P)
	 * @param aMapFeatureID Which type of map feature (e.g. Quork crystals) is generating this production (P)
	 * @param aCurrentPopulation City population used when calculating the cap value (what value the gold trade bonus is capped at depends on the city population) (A)
	 * @param aNumberDoingTask Number of civilians doing the task (e.g. Farmers or Workers) that is generating this production (P,C)
	 * @param aDoubleAmountPerPerson Double the amount of generating each civilian is producing (P)
	 * @param aDoubleAmountPerPick Double the amount of production each pick (i.e. spell book, see pickTypeID parameter) is generating (P)
	 *
	 * @param aDoubleUnmodifiedProductionAmount Basic production calculated from the above - this is only included if there is a bonus to apply, otherwise left as zero and value will be in doubleProductionAmount (P,H)
	 *
	 * @param aRaceMineralBonusMultipler Whole number multipler to apply to mineral bonuses for this race (i.e. to give dwarves 2x bonuses from mines) (P)
	 * @param aDoubleAmountAfterRacialBonus Basic production after race mineral bonus applied (P)
	 *
	 * @param aPercentageBonus % bonus to add onto the basic production amount (or cumulatively onto the amount including the race mineral bonus, if applicable) (P)
	 * @param aDoubleProductionAmount Final production amount after taking all item-level bonuses into account (P,H)
	 *
	 * @param aConsumptionAmount Consumption amount (C)
	 * @param aPercentage Overall percentage bonus to apply on top of final production amount (e.g. gold bonus from Marketplace, cumulative on top of racial/Miners' Guild mineral bonus) (%)
	 * @param aCap Cap value applied (A)
	 * @param aRoundingDirection Rounding direction used to halve production value (H)
	 */
	CalculateCityProductionResultBreakdown (final String aPopulationTaskID, final String aBuildingID, final String aPickTypeID, final Integer aPlaneNumber, final String aMapFeatureID,
		final int aCurrentPopulation, final int aNumberDoingTask, final int aDoubleAmountPerPerson, final int aDoubleAmountPerPick,
		final int aDoubleUnmodifiedProductionAmount,
		final int aRaceMineralBonusMultipler, final int aDoubleAmountAfterRacialBonus,
		final int aPercentageBonus, final int aDoubleProductionAmount,
		final int aConsumptionAmount, final int aPercentage, final int aCap, final RoundingDirectionID aRoundingDirection)
	{
		super ();
		
		populationTaskID = aPopulationTaskID;
		buildingID = aBuildingID;
		pickTypeID = aPickTypeID;
		planeNumber = aPlaneNumber;
		mapFeatureID = aMapFeatureID;
		currentPopulation = aCurrentPopulation;
		numberDoingTask = aNumberDoingTask;
		doubleAmountPerPerson = aDoubleAmountPerPerson;
		doubleAmountPerPick = aDoubleAmountPerPick;
		doubleUnmodifiedProductionAmount = aDoubleUnmodifiedProductionAmount;
		raceMineralBonusMultipler = aRaceMineralBonusMultipler;
		doubleAmountAfterRacialBonus = aDoubleAmountAfterRacialBonus;
		percentageBonus = aPercentageBonus;
		doubleProductionAmount = aDoubleProductionAmount;
		consumptionAmount = aConsumptionAmount;
		percentage = aPercentage;
		cap = aCap;
		roundingDirection = aRoundingDirection;
	}

	/**
	 * @return Which type of civilian (e.g. Farmers or Workers) is generating this production (P)
	 */
	public final String getPopulationTaskID ()
	{
		return populationTaskID;
	}
	
	/**
	 * @return Which building is generating this production (P,C,%)
	 */
	public final String getBuildingID ()
	{
		return buildingID;
	}
	
	/**
	 * @return Which pick type (books or retorts) is generating this production (this is used for spell books generating magic power at your fortress city) (P)
	 */
	public final String getPickTypeID ()
	{
		return pickTypeID;
	}
	
	/**
	 * @return Which plane is generating this production (this is used for your fortress generating +5 magic power if on Myrror) (P)
	 */
	public final Integer getPlaneNumber ()
	{
		return planeNumber;
	}
	
	/**
	 * @return Which type of map feature (e.g. Quork crystals) is generating this production (P)
	 */
	public final String getMapFeatureID ()
	{
		return mapFeatureID;
	}
	
	/**
	 * @return City population used when calculating the cap value (what value the gold trade bonus is capped at depends on the city population) (A)
	 */
	public final int getCurrentPopulation ()
	{
		return currentPopulation;
	}
	
	/**
	 * @return Number of civilians doing the task (e.g. Farmers or Workers) that is generating this production (P,C)
	 */
	public final int getNumberDoingTask ()
	{
		return numberDoingTask;
	}
	
	/**
	 * @return Double the amount of generating each civilian is producing (P)
	 */
	public final int getDoubleAmountPerPerson ()
	{
		return doubleAmountPerPerson;
	}
	
	/**
	 * @return Double the amount of production each pick (i.e. spell book, see pickTypeID parameter) is generating (P)
	 */
	public final int getDoubleAmountPerPick ()
	{
		return doubleAmountPerPick;
	}
	
	/**
	 * @return Basic production calculated from the above - this is only included if there is a bonus to apply, otherwise left as zero and value will be in doubleProductionAmount (P,H)
	 */
	public final int getDoubleUnmodifiedProductionAmount ()
	{
		return doubleUnmodifiedProductionAmount;
	}
	
	/**
	 * @return Whole number multipler to apply to mineral bonuses for this race (i.e. to give dwarves 2x bonuses from mines) (P)
	 */
	public final int getRaceMineralBonusMultipler ()
	{
		return raceMineralBonusMultipler;
	}
	
	/**
	 * @return Basic production after race mineral bonus applied (P)
	 */
	public final int getDoubleAmountAfterRacialBonus ()
	{
		return doubleAmountAfterRacialBonus;
	}
	
	/**
	 * @return % bonus to add onto the basic production amount (or cumulatively onto the amount including the race mineral bonus, if applicable) (P)
	 */
	public final int getPercentageBonus ()
	{
		return percentageBonus;
	}
	
	/**
	 * @return Final production amount after taking all item-level bonuses into account (P,H)
	 */
	public final int getDoubleProductionAmount ()
	{
		return doubleProductionAmount;
	}
	
	/**
	 * @return Consumption amount (C)
	 */
	public final int getConsumptionAmount ()
	{
		return consumptionAmount;
	}
	
	/**
	 * @return Overall percentage bonus to apply on top of final production amount (e.g. gold bonus from Marketplace, cumulative on top of racial/Miners' Guild mineral bonus) (%)
	 */
	public final int getPercentage ()
	{
		return percentage;
	}
	
	/**
	 * @return Cap value applied (A)
	 */
	public final int getCap ()
	{
		return cap;
	}
	
	/**
	 * @return Rounding direction used to halve production value (H)
	 */
	public final RoundingDirectionID getRoundingDirection ()
	{
		return roundingDirection;
	}
}