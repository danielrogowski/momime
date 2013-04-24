package momime.common.calculations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ICommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Building;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_4.FortressPickTypeProduction;
import momime.common.database.v0_9_4.FortressPlaneProduction;
import momime.common.database.v0_9_4.MapFeature;
import momime.common.database.v0_9_4.MapFeatureProduction;
import momime.common.database.v0_9_4.PickType;
import momime.common.database.v0_9_4.Plane;
import momime.common.database.v0_9_4.Race;
import momime.common.database.v0_9_4.RacePopulationTask;
import momime.common.database.v0_9_4.RacePopulationTaskProduction;
import momime.common.messages.IMemoryBuildingUtils;
import momime.common.messages.IPlayerPickUtils;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.PlayerPick;

/**
 * Internal implementation of the list of all types of production generated from a city returned from calculateAllCityProductions ()
 * Provides all the update methods that the routine needs to build up the list
 */
final class CalculateCityProductionResultsImplementation implements CalculateCityProductionResults
{
	/** Underlying list */
	private final List<CalculateCityProductionResult> results;

	/** Memory building utils */
	private IMemoryBuildingUtils memoryBuildingUtils;
	
	/** Player pick utils */
	private IPlayerPickUtils playerPickUtils;
	
	/**
	 * Creates underlying list
	 */
	CalculateCityProductionResultsImplementation ()
	{
		results = new ArrayList<CalculateCityProductionResult> ();
	}

	/**
	 * @param productionTypeID Production type to search for
	 * @return Requested production type, or null if the city does not produce or consume any of this type of production
	 */
	@Override
	public final CalculateCityProductionResult findProductionType (final String productionTypeID)
	{
		CalculateCityProductionResult result = null;
		final Iterator<CalculateCityProductionResult> productions = results.iterator ();

		while ((result == null) && (productions.hasNext ()))
		{
			final CalculateCityProductionResult thisProduction = productions.next ();
			if (thisProduction.getProductionTypeID ().equals (productionTypeID))
				result = thisProduction;
		}

		return result;
	}

	/**
	 * @return Iterator allowing scanning over all the production results
	 */
	@Override
	public final Iterator<CalculateCityProductionResult> iterator ()
	{
		return results.iterator ();
	}

	/**
	 * @param productionTypeID Production type to search for
	 * @return Requested production type - if not already in the list, it will be added
	 */
	private final CalculateCityProductionResult findOrAddProductionType (final String productionTypeID)
	{
		CalculateCityProductionResult value = findProductionType (productionTypeID);
		if (value == null)
		{
			value = new CalculateCityProductionResult (productionTypeID);
			results.add (value);
		}
		return value;
	}

	/**
	 * @return Direct access to the underlying list
	 */
	final List<CalculateCityProductionResult> getResults ()
	{
		return results;
	}

	/**
	 * @param productionTypeID Type of production we're generating
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
	 * @param percentageBonus % bonus to add onto the basic production amount (or cumulatively onto the amount including the race mineral bonus, if applicable)
	 * @param doubleProductionAmount Final production amount after taking all item-level bonuses into account
	 */
	final void addProduction (final String productionTypeID, final String populationTaskID, final String buildingID, final String pickTypeID, final Integer planeNumber, final String mapFeatureID,
		final int numberDoingTask, final int doubleAmountPerPerson, final int doubleAmountPerPick,
		final int doubleUnmodifiedProductionAmount,
		final int raceMineralBonusMultipler, final int doubleAmountAfterRacialBonus,
		final int percentageBonus, final int doubleProductionAmount)
	{
		final CalculateCityProductionResult value = findOrAddProductionType (productionTypeID);

		value.setDoubleProductionAmount (value.getDoubleProductionAmount () + doubleProductionAmount);

		value.addProductionBreakdown (populationTaskID, buildingID, pickTypeID, planeNumber, mapFeatureID,
			numberDoingTask, doubleAmountPerPerson, doubleAmountPerPick,
			doubleUnmodifiedProductionAmount, raceMineralBonusMultipler, doubleAmountAfterRacialBonus, percentageBonus, doubleProductionAmount);
	}

	/**
	 * @param productionTypeID Type of production we're consuming
	 * @param buildingID Which building is consuming this production
	 * @param numberDoingTask Number of civilians doing the task (e.g. Farmers or Workers) that is consuming this production
	 * @param consumptionAmount Consumption amount
	 */
	final void addConsumption (final String productionTypeID, final String buildingID, final int numberDoingTask, final int consumptionAmount)
	{
		final CalculateCityProductionResult value = findOrAddProductionType (productionTypeID);

		value.setConsumptionAmount (value.getConsumptionAmount () + consumptionAmount);

		value.addConsumptionBreakdown (buildingID, numberDoingTask, consumptionAmount);
	}

	/**
	 * @param productionTypeID Type of production we're giving a bonus to
	 * @param buildingID Which building is generating this percentage bonus
	 * @param percentage Overall percentage bonus to apply on top of final production amount (e.g. gold bonus from Marketplace, cumulative on top of racial/Miners' Guild mineral bonus)
	 */
	final void addPercentage (final String productionTypeID, final String buildingID, final int percentage)
	{
		final CalculateCityProductionResult value = findOrAddProductionType (productionTypeID);

		value.setPercentageBonus (value.getPercentageBonus () + percentage);

		value.addPercentageBreakdown (buildingID, percentage);
	}

	/**
	 * Note unlike the other add* methods here, this only adds a breakdown - the calling routine has to actually implement the cap
	 *
	 * @param productionTypeID Type of production we're capping
	 * @param currentPopulation City population used when calculating the cap value (what value the gold trade bonus is capped at depends on the city population)
	 * @param cap Cap value applied
	 */
	final void addCap (final String productionTypeID, final int currentPopulation, final int cap)
	{
		findOrAddProductionType (productionTypeID).addCapBreakdown (currentPopulation, cap);
	}

	/**
	 * Adds all the productions from a certain number of a particular type of civilian
	 * @param race Race of civilian
	 * @param populationTaskID Task civilian is performing (farmer, worker or rebel)
	 * @param numberDoingTask Number of civilians doing this task
	 * @param cityLocation Location of the city where the civilians are
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	final void addProductionFromPopulation (final Race race, final String populationTaskID, final int numberDoingTask, final OverlandMapCoordinates cityLocation,
		final List<MemoryBuilding> buildings, final ICommonDatabase db) throws RecordNotFoundException
	{
		if (numberDoingTask > 0)

			// Find definition for this population task (farmer, worker, rebel) for the city's race
			// It may genuinely not be there - rebels of most races produce nothing so won't even be listed
			for (final RacePopulationTask populationTask : race.getRacePopulationTask ())
				if (populationTask.getPopulationTaskID ().equals (populationTaskID))
					for (final RacePopulationTaskProduction thisProduction : populationTask.getRacePopulationTaskProduction ())
					{
						// Are there are building we have which increase this type of production from this type of population
						// i.e. Animists' guild increasing farmers yield by +1
						final int doubleAmountPerPerson = thisProduction.getDoubleAmount () +
							getMemoryBuildingUtils ().totalBonusProductionPerPersonFromBuildings
								(buildings, cityLocation, populationTaskID, thisProduction.getProductionTypeID (), db);

						// Now add it
						addProduction (thisProduction.getProductionTypeID (), populationTaskID, null, null, null, null,
							numberDoingTask, doubleAmountPerPerson, 0,
							0,
							0, 0,
							0, numberDoingTask * doubleAmountPerPerson);
					}
	}

	/**
	 * Adds all the productions and/or consumptions generated by a particular building
	 * @param building The building to calculate for
	 * @param picks The list of spell picks belonging to the player who owns the city that this building is in
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	final void addProductionAndConsumptionFromBuilding (final Building building, final List<PlayerPick> picks, final ICommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		// Go through each type of production/consumption from this building
		for (final BuildingPopulationProductionModifier thisProduction : building.getBuildingPopulationProductionModifier ())

			// Only pick out production modifiers which come from the building by itself with no effect from the number of population
			// - such as the Library giving +2 research - we don't want modifiers such as the Animsts' Guild giving 1 to each farmer
			if (thisProduction.getPopulationTaskID () == null)
			{
				// Just to stop null pointer exceptions below
				if (thisProduction.getDoubleAmount () == null)
				{
				}

				// Production?
				else if (thisProduction.getDoubleAmount () > 0)
				{
					// Bonus from retorts?
					final int totalReligiousBuildingBonus;
					if ((building.isBuildingUnrestReductionImprovedByRetorts () != null) && (building.isBuildingUnrestReductionImprovedByRetorts ()))
						totalReligiousBuildingBonus = getPlayerPickUtils ().totalReligiousBuildingBonus (picks, db);
					else
						totalReligiousBuildingBonus = 0;

					// Calculate amounts
					final int amountBeforeReligiousBuildingBonus = thisProduction.getDoubleAmount ();
					final int amountAfterReligiousBuildingBonus = amountBeforeReligiousBuildingBonus + ((amountBeforeReligiousBuildingBonus * totalReligiousBuildingBonus) / 100);

					// Add it
					addProduction (thisProduction.getProductionTypeID (), null, building.getBuildingID (), null, null, null,
						0, 0, 0,
						amountBeforeReligiousBuildingBonus,
						0, 0,
						totalReligiousBuildingBonus, amountAfterReligiousBuildingBonus);
				}

				// Consumption?
				else if (thisProduction.getDoubleAmount () < 0)
				{
					// Must be an exact multiple of 2
					final int consumption = -thisProduction.getDoubleAmount ();
					if (consumption % 2 != 0)
						throw new MomException ("Building \"" + building.getBuildingID () + "\" has a consumption value for \"" + thisProduction.getProductionTypeID () + "\" that is not an exact multiple of 2");

					addConsumption (thisProduction.getProductionTypeID (), building.getBuildingID (), 0, consumption / 2);
				}

				// Percentage bonus?
				// Can have both (production or consumption) AND percentage bonus, e.g. Marketplace
				if ((thisProduction.getPercentageBonus () != null) && (thisProduction.getPercentageBonus () > 0))
					addPercentage (thisProduction.getProductionTypeID (), building.getBuildingID (), thisProduction.getPercentageBonus ());
			}
	}

	/**
	 * Adds on production generated by our fortress according to the number of picks of a particular type we chose at the start of the game
	 * @param pickType Type of picks (spell books or retorts)
	 * @param pickTypeCount The number of the pick we had at the start of the game
	 */
	final void addProductionFromFortressPickType (final PickType pickType, final int pickTypeCount)
	{
		if (pickTypeCount > 0)
			for (final FortressPickTypeProduction thisProduction : pickType.getFortressPickTypeProduction ())
				addProduction (thisProduction.getFortressProductionTypeID (), null, CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, pickType.getPickTypeID (), null, null,
					0, 0, thisProduction.getDoubleAmount (),
					0,
					0, 0,
					0, pickTypeCount * thisProduction.getDoubleAmount ());
	}

	/**
	 * Adds on production generated by our fortress being on a particular plane
	 * @param plane Which plane our fortress is on
	 */
	final void addProductionFromFortressPlane (final Plane plane)
	{
		for (final FortressPlaneProduction thisProduction : plane.getFortressPlaneProduction ())
			addProduction (thisProduction.getFortressProductionTypeID (), null, CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, null, plane.getPlaneNumber (), null,
					0, 0, 0,
					0,
					0, 0,
					0, thisProduction.getDoubleAmount ());
	}

	/**
	 * Adds on production generated from a particular map feature
	 * @param mapFeature The type of map feature
	 * @param raceMineralBonusMultipler The amount our race multiplies mineral bonuses by (2 for Dwarves, 1 for everyone else)
	 * @param mineralPercentageBonus The % bonus buildings give to mineral bonuses (50% if we have a Miners' Guild)
	 */
	final void addProductionFromMapFeature (final MapFeature mapFeature, final int raceMineralBonusMultipler, final int mineralPercentageBonus)
	{
		// Bonuses apply only to mines, not wild game
		final int useRaceMineralBonusMultipler;
		final int useMineralPercentageBonus;
		if ((mapFeature.isRaceMineralMultiplerApplies () != null) && (mapFeature.isRaceMineralMultiplerApplies ()))
		{
			useRaceMineralBonusMultipler = raceMineralBonusMultipler;
			useMineralPercentageBonus = mineralPercentageBonus;
		}
		else
		{
			useRaceMineralBonusMultipler = 1;
			useMineralPercentageBonus = 0;
		}

		// Check each type of production generated
		for (final MapFeatureProduction thisProduction : mapFeature.getMapFeatureProduction ())
		{
			// Apply modifiers
			final int doubleAmountBasic = thisProduction.getDoubleAmount ();
			final int doubleAmountAfterRacialBonus = doubleAmountBasic * useRaceMineralBonusMultipler;
			final int doubleAmountFinal = doubleAmountAfterRacialBonus + ((doubleAmountAfterRacialBonus * useMineralPercentageBonus) / 100);

			// Add it
			addProduction (thisProduction.getProductionTypeID (), null, null, null, null, mapFeature.getMapFeatureID (),
				0, 0, 0,
				doubleAmountBasic,
				useRaceMineralBonusMultipler, doubleAmountAfterRacialBonus,
				useMineralPercentageBonus, doubleAmountFinal);
		}
	}

	/**
	 * @return Memory building utils
	 */
	public final IMemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final IMemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return Player pick utils
	 */
	public final IPlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final IPlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
}
