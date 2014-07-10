package momime.common.calculations;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.v0_9_5.RoundingDirectionID;

/**
 * Stores the details of one result generated by the calculateCityProductions () method
 *
 * We have to count up production and consumption separately since the percentageBonus only applies to production, not
 * consumption (e.g. a marketplace adds a percentage to gold production - it doesn't also add 10% to the maintenance of all buildings!)
 *
 * The values are not fixed, so this class is not immutable, because the calculateCityProductions () adds production e.g. building at a time,
 * so it may need to add to an existing value.  However we also don't need read/update interfaces, because the values are only updated
 * inside calculateCityProductions () and are fixed once the routine ends (and so thread safe).
 * Note the update methods are all package private to try to enforce this.
 */
public final class CalculateCityProductionResult implements Comparable<CalculateCityProductionResult>
{
	/** Type of production */
	private final String productionTypeID;

	/** Double the amount of this type of production the city is producing */
	private int doubleProductionAmount;

	/** The amount of this type of production the city is producing, after the value is halved */
	private int baseProductionAmount;

	/** The amount of production the city is consuming, e.g. gold to maintain buildings */
	private int consumptionAmount;

	/** Percentage bonus to apply to the amount the city is producing */
	private int percentageBonus;
	
	/** Whether to store breakdown objects or throw the details away and just keep the results */
	private boolean storeBreakdown;
	
	/** Detailed calculation breakdowns */
	private List<CalculateCityProductionResultBreakdown> breakdowns;

	/**
	 * @param aProductionTypeID Type of production
	 */
	public CalculateCityProductionResult (final String aProductionTypeID)
	{
		productionTypeID = aProductionTypeID;
		doubleProductionAmount = 0;
		baseProductionAmount = 0;
		consumptionAmount = 0;
		percentageBonus = 0;
	}

	/*
	 * Read methods
	 */

	/**
	 * @return Type of production
	 */
	public final String getProductionTypeID ()
	{
		return productionTypeID;
	}

	/**
	 * @return Double the amount of this type of production the city is producing
	 */
	public final int getDoubleProductionAmount ()
	{
		return doubleProductionAmount;
	}

	/**
	 * @return The amount of production the city is consuming, e.g. gold to maintain buildings
	 */
	public final int getConsumptionAmount ()
	{
		return consumptionAmount;
	}

	/**
	 * @return Percentage bonus to apply to the amount the city is producing
	 */
	public final int getPercentageBonus ()
	{
		return percentageBonus;
	}

	/**
	 * @return The amount of this type of production the city is producing, after the value is halved
	 */
	public final int getBaseProductionAmount ()
	{
		return baseProductionAmount;
	}

	/**
	 * @return The amount of this type of production the city is producing after applying any % bonus
	 */
	public final int getModifiedProductionAmount ()
	{
		return baseProductionAmount + ((baseProductionAmount * getPercentageBonus ()) / 100);
	}

	/*
	 * Update methods
	 */

	/**
	 * @param newDoubleAmount Double the amount of this type of production the city is producing
	 */
	final void setDoubleProductionAmount (final int newDoubleAmount)
	{
		doubleProductionAmount = newDoubleAmount;
	}

	/**
	 * @param newAmount The amount of this type of production the city is producing, after the value is halved
	 */
	public final void setBaseProductionAmount (final int newAmount)
	{
		baseProductionAmount = newAmount;
	}

	/**
	 * @param newAmount The amount of production the city is consuming, e.g. gold to maintain buildings
	 */
	public final void setConsumptionAmount (final int newAmount)
	{
		consumptionAmount = newAmount;
	}

	/**
	 * @param newPercentage Percentage bonus to apply to the amount the city is producing
	 */
	final void setPercentageBonus (final int newPercentage)
	{
		percentageBonus = newPercentage;
	}

	/**
	 * The (P,C,%,A,H) at the end of each parameter comment indicates whether this is used for production, consumption, percentage bonus, cap or
	 * halving the production amount (it is initially calculated doubled); any values not applicable will be left as 0 or null
	 *
	 * @param populationTaskID Which type of civilian (e.g. Farmers or Workers) is generating this production (P)
	 * @param buildingID Which building is generating this production (P,C,%)
	 * @param pickTypeID Which pick type (books or retorts) is generating this production (this is used for spell books generating magic power at your fortress city) (P)
	 * @param planeNumber Which plane is generating this production (this is used for your fortress generating +5 magic power if on Myrror) (P)
	 * @param mapFeatureID Which type of map feature (e.g. Quork crystals) is generating this production (P)
	 * @param currentPopulation City population used when calculating the cap value (what value the gold trade bonus is capped at depends on the city population) (A)
	 * @param numberDoingTask Number of civilians doing the task (e.g. Farmers or Workers) that is generating this production (P,C)
	 * @param doubleAmountPerPerson Double the amount of generating each civilian is producing (P)
	 * @param doubleAmountPerPick Double the amount of production each pick (i.e. spell book, see pickTypeID parameter) is generating (P)
	 *
	 * @param doubleUnmodifiedProductionAmount Basic production calculated from the above - this is only included if there is a bonus to apply, otherwise left as zero and value will be in doubleProductionAmount (P,H)
	 *
	 * @param raceMineralBonusMultipler Whole number multipler to apply to mineral bonuses for this race (i.e. to give dwarves 2x bonuses from mines) (P)
	 * @param doubleAmountAfterRacialBonus Basic production after race mineral bonus applied (P)
	 *
	 * @param aPercentageBonus % bonus to add onto the basic production amount (or cumulatively onto the amount including the race mineral bonus, if applicable) (P)
	 * @param aDoubleProductionAmount Final production amount after taking all item-level bonuses into account (P,H)
	 *
	 * @param aConsumptionAmount Consumption amount (C)
	 * @param percentage Overall percentage bonus to apply on top of final production amount (e.g. gold bonus from Marketplace, cumulative on top of racial/Miners' Guild mineral bonus) (%)
	 * @param cap Cap value applied (A)
	 * @param roundingDirection Rounding direction used to halve production value (H)
	 */
	protected void addBreakdown (final String populationTaskID, final String buildingID, final String pickTypeID, final Integer planeNumber, final String mapFeatureID,
		final int currentPopulation, final int numberDoingTask, final int doubleAmountPerPerson, final int doubleAmountPerPick,
		final int doubleUnmodifiedProductionAmount,
		final int raceMineralBonusMultipler, final int doubleAmountAfterRacialBonus,
		final int aPercentageBonus, final int aDoubleProductionAmount,
		final int aConsumptionAmount, final int percentage, final int cap, final RoundingDirectionID roundingDirection)
	{
		// Does nothing here since the server doesn't care about the breakdown, just the final calculation
		// If client gets ported to Java, will override this method to actually record the breakdown

		// In particular note that adding a breakdown detailing where e.g. +5 magic power is coming from does not actually call setDoubleProductionAmount () to add
		// the +5 magic power - it literally only records the breakdown - the calling routine has to update the values as appropriate as well
		if (isStoreBreakdown ())
			getBreakdowns ().add (new CalculateCityProductionResultBreakdown (populationTaskID, buildingID, pickTypeID, planeNumber, mapFeatureID,
				currentPopulation, numberDoingTask, doubleAmountPerPerson, doubleAmountPerPick, doubleUnmodifiedProductionAmount,
				raceMineralBonusMultipler, doubleAmountAfterRacialBonus, aPercentageBonus, aDoubleProductionAmount,
				aConsumptionAmount, percentage, cap, roundingDirection));
	}

	/**
	 * @param populationTaskID Which type of civilian (e.g. Farmers or Workers) is generating this production
	 * @param buildingID Which building is generating this production
	 * @param pickTypeID Which pick type (books or retorts) is generating this production (this is used for spell books generating magic power at your fortress city)
	 * @param planeNumber Which plane is generating this production (this is used for your fortress generating +5 magic power if on Myrror)
	 * @param mapFeatureID Which type of map feature (e.g. Quork crystals) is generating this production
	 * @param numberDoingTask Number of civilians doing the task (e.g. Farmers or Workers) that is generating this production
	 * @param doubleAmountPerPerson Double the amount of generating each civilian is producing
	 * @param doubleAmountPerPick Double the amount of production each pick (i.e. spell book, see pickTypeID parameter) is generating
	 *
	 * @param doubleUnmodifiedProductionAmount Basic production calculated from the above - this is only included if there is a bonus to apply, otherwise left as zero and value will be in doubleProductionAmount
	 *
	 * @param raceMineralBonusMultipler Whole number multipler to apply to mineral bonuses for this race (i.e. to give dwarves 2x bonuses from mines)
	 * @param doubleAmountAfterRacialBonus Basic production after race mineral bonus applied
	 *
	 * @param aPercentageBonus % bonus to add onto the basic production amount (or cumulatively onto the amount including the race mineral bonus, if applicable)
	 * @param aDoubleProductionAmount Final production amount after taking all item-level bonuses into account
	 */
	final void addProductionBreakdown (final String populationTaskID, final String buildingID, final String pickTypeID, final Integer planeNumber, final String mapFeatureID,
		final int numberDoingTask, final int doubleAmountPerPerson, final int doubleAmountPerPick,
		final int doubleUnmodifiedProductionAmount,
		final int raceMineralBonusMultipler, final int doubleAmountAfterRacialBonus,
		final int aPercentageBonus, final int aDoubleProductionAmount)
	{
		addBreakdown (populationTaskID, buildingID, pickTypeID, planeNumber, mapFeatureID,
			0, numberDoingTask, doubleAmountPerPerson, doubleAmountPerPick,
			doubleUnmodifiedProductionAmount,
			raceMineralBonusMultipler, doubleAmountAfterRacialBonus,
			aPercentageBonus, aDoubleProductionAmount,
			0, 0, 0, null);
	}

	/**
	 * @param buildingID Which building is generating this production
	 * @param numberDoingTask Number of civilians doing the task (e.g. Farmers or Workers) that is generating this production
	 * @param aConsumptionAmount Consumption amount
	 */
	final void addConsumptionBreakdown (final String buildingID, final int numberDoingTask, final int aConsumptionAmount)
	{
		addBreakdown (null, buildingID, null, null, null,
			0, numberDoingTask, 0, 0,
			0,
			0, 0,
			0, 0,
			aConsumptionAmount, 0, 0, null);
	}

	/**
	 * @param buildingID Which building is generating this production
	 * @param percentage Overall percentage bonus to apply on top of final production amount (e.g. gold bonus from Marketplace, cumulative on top of racial/Miners' Guild mineral bonus)
	 */
	final void addPercentageBreakdown (final String buildingID, final int percentage)
	{
		addBreakdown (null, buildingID, null, null, null,
			0, 0, 0, 0,
			0,
			0, 0,
			0, 0,
			0, percentage, 0, null);
	}

	/**
	 * @param currentPopulation City population used when calculating the cap value (what value the gold trade bonus is capped at depends on the city population)
	 * @param cap Cap value applied
	 */
	final void addCapBreakdown (final int currentPopulation, final int cap)
	{
		addBreakdown (null, null, null, null, null,
			currentPopulation, 0, 0, 0,
			0,
			0, 0,
			0, 0,
			0, 0, cap, null);
	}

	/**
	 * @param doubleUnmodifiedProductionAmount Basic production calculated from the above - this is only included if there is a bonus to apply, otherwise left as zero and value will be in doubleProductionAmount
	 * @param aDoubleProductionAmount Final production amount after taking all item-level bonuses into account
	 * @param roundingDirection Rounding direction used to halve production value
	 */
	final void addRoundingBreakdown (final int doubleUnmodifiedProductionAmount, final int aDoubleProductionAmount, final RoundingDirectionID roundingDirection)
	{
		addBreakdown (null, null, null, null, null,
			0, 0, 0, 0,
			doubleUnmodifiedProductionAmount,
			0, 0,
			0, aDoubleProductionAmount,
			0, 0, 0, roundingDirection);
	}

	/*
	 * Methods from Comparable
	 */

	/**
	 * Compares two production values - we have to define this in order to be able to sort the list
	 */
	@Override
	public final int compareTo (final CalculateCityProductionResult other)
	{
		// Compare the two codes, e.g. RE03 vs RE05
		return getProductionTypeID ().compareTo (other.getProductionTypeID ());
	}
	
	/*
	 * Getters and setters
	 */

	/**
	 * @return Whether to store breakdown objects or throw the details away and just keep the results
	 */
	public final boolean isStoreBreakdown ()
	{
		return storeBreakdown;
	}

	/**
	 * @param value Whether to store breakdown objects or throw the details away and just keep the results
	 */
	public final void setStoreBreakdown (final boolean value)
	{
		storeBreakdown = value;
		if (storeBreakdown)
			breakdowns = new ArrayList<CalculateCityProductionResultBreakdown> ();
	}

	/**
	 * @return Detailed calculation breakdowns
	 */
	public final List<CalculateCityProductionResultBreakdown> getBreakdowns ()
	{
		return breakdowns;
	}
}
