package momime.client.calculations;

import java.util.ArrayList;
import java.util.List;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.Pick;
import momime.client.language.database.v0_9_5.Plane;
import momime.client.language.database.v0_9_5.PopulationTask;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.TileType;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownBuilding;
import momime.common.internal.CityGrowthRateBreakdownDying;
import momime.common.internal.CityGrowthRateBreakdownGrowing;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityProductionBreakdownBuilding;
import momime.common.internal.CityProductionBreakdownMapFeature;
import momime.common.internal.CityProductionBreakdownPickType;
import momime.common.internal.CityProductionBreakdownPopulationTask;
import momime.common.internal.CityProductionBreakdownTileType;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.CityUnrestBreakdownBuilding;

/**
 * Client side only methods dealing with city calculations
 */
public final class MomClientCityCalculationsImpl implements MomClientCityCalculations
{
	/** Indentation used on calculation breakdowns that are additions to the previous line */
	private static final String INDENT = "     ";
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Text utils */
	private TextUtils textUtils;
	
	/**
	 * @param text Text buffer to add to
	 * @param line Text to add
	 */
	final void addLine (final StringBuilder text, final String line)
	{
		if (text.length () > 0)
			text.append ("\r\n");
		
		if (line != null)
			text.append (line);
	}

	/**
	 * @param pickIDs List of pick IDs
	 * @return List coverted to descriptions
	 */
	final String listPickDescriptions (final List<String> pickIDs)
	{
		final StringBuilder retortList = new StringBuilder ();
		for (final String pickID : pickIDs)
		{
			final Pick pick = getLanguage ().findPick (pickID);
			final String pickDesc = (pick == null) ? pickID : pick.getPickDescription ();
			
			if (retortList.length () > 0)
				retortList.append (", ");
			
			retortList.append (pickDesc);
		}
		
		return retortList.toString ();
	}
	
	/**
	 * @param breakdown Results of unrest calculation
	 * @return Readable calculation details
	 */
	@Override
	public final String describeCityUnrestCalculation (final CityUnrestBreakdown breakdown)
	{
		final StringBuilder text = new StringBuilder ();
		
		// Percentages
		if (breakdown.getTaxPercentage () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "TaxPercentage").replaceAll
				("TAX_PERCENTAGE", new Integer (breakdown.getTaxPercentage ()).toString ()));

		if (breakdown.getRacialPercentage () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "RacialPercentage").replaceAll
				("RACE_PERCENTAGE", new Integer (breakdown.getRacialPercentage ()).toString ()));

		if ((breakdown.getTaxPercentage () > 0) && (breakdown.getRacialPercentage () > 0))
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "TotalPercentage").replaceAll
				("TAX_PERCENTAGE", new Integer (breakdown.getTaxPercentage ()).toString ()).replaceAll
				("RACE_PERCENTAGE", new Integer (breakdown.getRacialPercentage ()).toString ()).replaceAll
				("TOTAL_PERCENTAGE", new Integer (breakdown.getTotalPercentage ()).toString ()));
		
		if ((breakdown.getTaxPercentage () > 0) || (breakdown.getRacialPercentage () > 0))
		{
			text.append ("\r\n");
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BaseValue").replaceAll
				("TOTAL_PERCENTAGE", new Integer (breakdown.getTotalPercentage ()).toString ()).replaceAll
				("CURRENT_POPULATION_DIV_1000", new Integer (breakdown.getPopulation ()).toString ()).replaceAll
				("BASE_UNREST", new Integer (breakdown.getBaseValue ()).toString ()));
		}
		
		// Klackons
		if (breakdown.getRacialLiteral () != 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "RacialLiteral").replaceAll
				("RACE_LITERAL", getTextUtils ().intToStrPlusMinus (breakdown.getRacialLiteral ())));
		
		// Buildings
		for (final CityUnrestBreakdownBuilding buildingUnrest : breakdown.getBuildingReducingUnrest ())
		{
			final Building building = getLanguage ().findBuilding (buildingUnrest.getBuildingID ());
			final String buildingName = (building == null) ? buildingUnrest.getBuildingID () : building.getBuildingName ();
			
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BuildingUnrestReduction").replaceAll
				("BUILDING_REDUCTION", new Integer (buildingUnrest.getUnrestReduction ()).toString ()).replaceAll
				("BUILDING_NAME", buildingName));
		}
		
		// Divine Power / Infernal Power retort
		if (breakdown.getReligiousBuildingReduction () != 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "Retort").replaceAll
				("RELIGIOUS_BUILDING_REDUCTION", new Integer (breakdown.getReligiousBuildingReduction ()).toString ()).replaceAll
				("RETORT_PERCENTAGE", new Integer (breakdown.getReligiousBuildingRetortPercentage ()).toString ()).replaceAll
				("RETORT_LIST", listPickDescriptions (breakdown.getPickIdContributingToReligiousBuildingBonus ())).replaceAll
				("RETORT_VALUE", new Integer (breakdown.getReligiousBuildingRetortValue ()).toString ()));
		
		// Units stationed in city
		if (breakdown.getUnitCount () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "UnitReduction").replaceAll
				("UNIT_REDUCTION", new Integer (breakdown.getUnitReduction ()).toString ()).replaceAll
				("UNIT_COUNT", new Integer (breakdown.getUnitCount ()).toString ()));
		
		// Total
		addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BaseTotal").replaceAll
			("BASE_TOTAL", new Integer (breakdown.getBaseTotal ()).toString ()));
		text.append ("\r\n");

		if (breakdown.isForcePositive ())
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "ForcePositive"));

		if (breakdown.isForceAll ())
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "ForceAll").replaceAll
				("CURRENT_POPULATION_DIV_1000", new Integer (breakdown.getPopulation ()).toString ()));
		
		if (breakdown.getMinimumFarmers () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "MinimumFarmers").replaceAll
				("MINIMUM_FARMERS", new Integer (breakdown.getMinimumFarmers ()).toString ()).replaceAll
				("TOTAL_AFTER_FARMERS", new Integer (breakdown.getTotalAfterFarmers ()).toString ()));
		
		return text.toString ();
	}
	
	/**
	 * @param breakdown Results of growth calculation
	 * @return Readable calculation details
	 */
	@Override
	public final String describeCityGrowthRateCalculation (final CityGrowthRateBreakdown breakdown)
	{
		final StringBuilder text = new StringBuilder ();
		
		// Start off calculation description
		addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CurrentPopulation").replaceAll
			("CURRENT_POPULATION", new Integer (breakdown.getCurrentPopulation ()).toString ()));

		addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "MaximumPopulation").replaceAll
			("MAXIMUM_POPULATION", new Integer (breakdown.getMaximumPopulation ()).toString ()));
		
		if (breakdown instanceof CityGrowthRateBreakdownGrowing)
		{
			final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) breakdown;
			
			addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "BaseGrowthRate").replaceAll
				("MAXIMUM_POPULATION_DIV_1000", new Integer (growing.getMaximumPopulation () / 1000).toString ()).replaceAll
				("CURRENT_POPULATION_DIV_1000", new Integer (growing.getCurrentPopulation () / 1000).toString ()).replaceAll
				("BASE_GROWTH_RATE", new Integer (growing.getBaseGrowthRate ()).toString ()));

			boolean showTotal = false;
			if (growing.getRacialGrowthModifier () != 0)
			{
				showTotal = true;
				addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "RacialGrowthModifier").replaceAll
					("RACIAL_GROWTH_MODIFIER", new Integer (growing.getRacialGrowthModifier ()).toString ()));
			}
				
			// Bonuses from buildings
			for (final CityGrowthRateBreakdownBuilding buildingGrowth : growing.getBuildingModifier ())
			{
				showTotal = true;
				final Building building = getLanguage ().findBuilding (buildingGrowth.getBuildingID ());
				final String buildingName = (building == null) ? buildingGrowth.getBuildingID () : building.getBuildingName ();					
					
				addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "GrowthBonusFromBuilding").replaceAll
					("BUILDING_NAME", buildingName).replaceAll
					("BUILDING_GROWTH_MODIFIER", new Integer (buildingGrowth.getGrowthRateBonus ()).toString ()));
			}
				
			if (showTotal)
				addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CityGrowthRateTotal").replaceAll
					("TOTAL_GROWTH_RATE", new Integer (growing.getTotalGrowthRate ()).toString ()));
				
			if (growing.getCappedGrowthRate () < growing.getTotalGrowthRate ())
				addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CityGrowthRateCapped").replaceAll
					("CAPPED_GROWTH_RATE", new Integer (growing.getCappedGrowthRate ()).toString ()));
		}
		else if (breakdown instanceof CityGrowthRateBreakdownDying)
		{
			final CityGrowthRateBreakdownDying dying = (CityGrowthRateBreakdownDying) breakdown;
			
			addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "DeathRate").replaceAll
				("BASE_DEATH_RATE", new Integer (dying.getBaseDeathRate ()).toString ()).replaceAll
				("CITY_DEATH_RATE", new Integer (dying.getCityDeathRate ()).toString ()));
		}
		else
		{
			addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "AtMaximumSize"));
		}
		
		return text.toString ();
	}
	
	/**
	 * @param calc Results of production calculation
	 * @return Readable calculation details
	 * @throws MomException If we find a breakdown entry that we don't know how to describe
	 */
	@Override
	public final String describeCityProductionCalculation (final CityProductionBreakdown calc) throws MomException
	{
		// This is used in a number of places, so work it out once up front
		final ProductionType productionType = getLanguage ().findProductionType (calc.getProductionTypeID ());
		final String productionTypeDescription = (productionType == null) ? calc.getProductionTypeID () : productionType.getProductionTypeDescription ();
				
		// List out into blocks of production, % bonuses and consumption - so we can test whether each block contains 0, 1 or many entries
		final List<String> productionBreakdowns = new ArrayList<String> ();
		final List<String> consumptionBreakdowns = new ArrayList<String> ();
		final List<String> percentageBonuses = new ArrayList<String> ();
		
		// Production from farmers/workers/rebels
		for (final CityProductionBreakdownPopulationTask populationTaskProduction : calc.getPopulationTaskProduction ())
		{
			final PopulationTask populationTask = getLanguage ().findPopulationTask (populationTaskProduction.getPopulationTaskID ());
			
			if (populationTaskProduction.getCount () == 1)
				productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromSinglePopulation").replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (populationTaskProduction.getDoubleProductionAmountAllPopulation ())).replaceAll
					("TASK_NAME_SINGULAR", (populationTask == null) ? populationTaskProduction.getPopulationTaskID () : populationTask.getPopulationTaskSingular ()));
			else
				productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMultiplePopulation").replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (populationTaskProduction.getDoubleProductionAmountAllPopulation ())).replaceAll
					("TASK_NAME_PLURAL", (populationTask == null) ? populationTaskProduction.getPopulationTaskID () : populationTask.getPopulationTaskPlural ()).replaceAll
					("NUMBER_DOING_TASK", new Integer (populationTaskProduction.getCount ()).toString ()).replaceAll
					("PRODUCTION_PER_PERSON", getTextUtils ().halfIntToStr (populationTaskProduction.getDoubleProductionAmountEachPopulation ())));
		}
		
		// Production from population irrespective of what task they're performing (taxes)
		if (calc.getDoubleProductionAmountAllPopulation () > 0)
			productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "GoldFromTaxes").replaceAll
				("PRODUCTION_TYPE", productionTypeDescription).replaceAll
				("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (calc.getDoubleProductionAmountAllPopulation ())).replaceAll
				("NUMBER_DOING_TASK", new Integer (calc.getApplicablePopulation ()).toString ()).replaceAll
				("PRODUCTION_PER_PERSON", getTextUtils ().halfIntToStr (calc.getDoubleProductionAmountEachPopulation ())));
		
		// Consumption from population irrespective of what task they're performing (eating rations)
		if (calc.getConsumptionAmountAllPopulation () > 0)
			consumptionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "RationsAteByPopulation").replaceAll
				("PRODUCTION_TYPE", productionTypeDescription).replaceAll
				("CONSUMPTION_AMOUNT", new Integer (calc.getConsumptionAmountAllPopulation ()).toString ()).replaceAll
				("NUMBER_DOING_TASK", new Integer (calc.getApplicablePopulation ()).toString ()).replaceAll
				("CONSUMPTION_PER_PERSON", new Integer (calc.getConsumptionAmountEachPopulation ()).toString ()));
		
		for (final CityProductionBreakdownTileType tileTypeProduction : calc.getTileTypeProduction ())
		{
			final TileType tileType = getLanguage ().findTileType (tileTypeProduction.getTileTypeID ());

			// Production from terrain tiles (food/max city size)
			if (tileTypeProduction.getDoubleProductionAmountAllTiles () > 0)
			{
				if (tileTypeProduction.getCount () == 1)
					productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromSingleTile").replaceAll
						("PRODUCTION_TYPE", productionTypeDescription).replaceAll
						("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (tileTypeProduction.getDoubleProductionAmountAllTiles ())).replaceAll
						("TILE_TYPE", (tileType == null) ? tileTypeProduction.getTileTypeID () : tileType.getTileTypeDescription ()));
				else
					productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMultipleTiles").replaceAll
						("PRODUCTION_TYPE", productionTypeDescription).replaceAll
						("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (tileTypeProduction.getDoubleProductionAmountAllTiles ())).replaceAll
						("TILE_TYPE", (tileType == null) ? tileTypeProduction.getTileTypeID () : tileType.getTileTypeDescription ()).replaceAll
						("TILE_COUNT", new Integer (tileTypeProduction.getCount ()).toString ()).replaceAll
						("PRODUCTION_PER_TILE", getTextUtils ().halfIntToStr (tileTypeProduction.getDoubleProductionAmountEachTile ())));
			}
			
			// % bonus from terrain tiles (production)
			if (tileTypeProduction.getPercentageBonusAllTiles () > 0)
			{
				if (tileTypeProduction.getCount () == 1)
					percentageBonuses.add (getLanguage ().findCategoryEntry ("CityProduction", "PercentageBonusFromSingleTile").replaceAll
						("PRODUCTION_TYPE", productionTypeDescription).replaceAll
						("PERCENTAGE_VALUE", new Integer (tileTypeProduction.getPercentageBonusAllTiles ()).toString ()).replaceAll
						("TILE_TYPE", (tileType == null) ? tileTypeProduction.getTileTypeID () : tileType.getTileTypeDescription ()));
				else
					percentageBonuses.add (getLanguage ().findCategoryEntry ("CityProduction", "PercentageBonusFromMultipleTiles").replaceAll
						("PRODUCTION_TYPE", productionTypeDescription).replaceAll
						("PERCENTAGE_VALUE", new Integer (tileTypeProduction.getPercentageBonusAllTiles ()).toString ()).replaceAll
						("TILE_TYPE", (tileType == null) ? tileTypeProduction.getTileTypeID () : tileType.getTileTypeDescription ()).replaceAll
						("TILE_COUNT", new Integer (tileTypeProduction.getCount ()).toString ()).replaceAll
						("PERCENTAGE_PER_TILE", new Integer (tileTypeProduction.getPercentageBonusEachTile ()).toString ()));
			}
		}
		
		// Production from map features
		for (final CityProductionBreakdownMapFeature mapFeatureProduction : calc.getMapFeatureProduction ())
		{
			final MapFeature mapFeature = getLanguage ().findMapFeature (mapFeatureProduction.getMapFeatureID ());

			if (mapFeatureProduction.getCount () == 1)
				productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromSingleMapFeature").replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (mapFeatureProduction.getDoubleUnmodifiedProductionAmountAllFeatures ())).replaceAll
					("MAP_FEATURE", (mapFeature == null) ? mapFeatureProduction.getMapFeatureID () : mapFeature.getMapFeatureDescription ()));
			else
				productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMultipleMapFeatures").replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (mapFeatureProduction.getDoubleUnmodifiedProductionAmountAllFeatures ())).replaceAll
					("MAP_FEATURE_COUNT", new Integer (mapFeatureProduction.getCount ()).toString ()).replaceAll
					("PRODUCTION_PER_MAP_FEATURE", getTextUtils ().halfIntToStr (mapFeatureProduction.getDoubleUnmodifiedProductionAmountEachFeature ())).replaceAll
					("MAP_FEATURE", (mapFeature == null) ? mapFeatureProduction.getMapFeatureID () : mapFeature.getMapFeatureDescription ()));

			if (mapFeatureProduction.getRaceMineralBonusMultiplier () > 1)
				productionBreakdowns.add (INDENT + getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMapFeatureRaceBonus").replaceAll
					("MAP_FEATURE_PRODUCTION_AMOUNT_AFTER_RACE_BONUS", getTextUtils ().halfIntToStr (mapFeatureProduction.getDoubleProductionAmountAfterRacialMultiplier ())).replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("MINERAL_BONUS_FROM_RACE", new Integer (mapFeatureProduction.getRaceMineralBonusMultiplier ()).toString ()));
		
			if (mapFeatureProduction.getBuildingMineralPercentageBonus () > 0)
				productionBreakdowns.add (INDENT + getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMapFeatureBuildingBonus").replaceAll
					("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (mapFeatureProduction.getDoubleModifiedProductionAmountAllFeatures ())).replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("PERCENTAGE_BONUS", new Integer (mapFeatureProduction.getBuildingMineralPercentageBonus ()).toString ()));
		}
		
		for (final CityProductionBreakdownBuilding buildingProduction : calc.getBuildingBreakdown ())
		{
			final Building building = getLanguage ().findBuilding (buildingProduction.getBuildingID ());
			
			// Production from buildings, e.g. Library generating research, or Granary generating food+rations
			if (buildingProduction.getDoubleUnmodifiedProductionAmount () > 0)
			{
				// Shrines etc. generate +50% more power if wizard has Divine or Infernal Power retort
				if (buildingProduction.getReligiousBuildingPercentageBonus () == 0)
					productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromBuildingWithoutReligiousRetortBonus").replaceAll
						("PRODUCTION_TYPE", productionTypeDescription).replaceAll
						("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (buildingProduction.getDoubleModifiedProductionAmount ())).replaceAll
						("BUILDING_NAME", (building == null) ? buildingProduction.getBuildingID () : building.getBuildingName ()));
				else
					productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromBuildingWithReligiousRetortBonus").replaceAll
						("UNMODIFIED_PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (buildingProduction.getDoubleUnmodifiedProductionAmount ())).replaceAll
						("PRODUCTION_TYPE", productionTypeDescription).replaceAll
						("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (buildingProduction.getDoubleModifiedProductionAmount ())).replaceAll
						("BUILDING_NAME", (building == null) ? buildingProduction.getBuildingID () : building.getBuildingName ()).replaceAll
						("PERCENTAGE_BONUS", new Integer (buildingProduction.getReligiousBuildingPercentageBonus ()).toString ()).replaceAll
						("RETORT_LIST", listPickDescriptions (buildingProduction.getPickIdContributingToReligiousBuildingBonus ())));
			}
			
			// % bonus from buildings, e.g. Marketplace generating +25% gold
			if (buildingProduction.getPercentageBonus () > 0)
				percentageBonuses.add (getLanguage ().findCategoryEntry ("CityProduction", "PercentageBonusFromBuilding").replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("PERCENTAGE_VALUE", new Integer (buildingProduction.getPercentageBonus ()).toString ()).replaceAll
					("BUILDING_NAME", (building == null) ? buildingProduction.getBuildingID () : building.getBuildingName ()));
			
			// Consumption from buildings, mainly gold maintainence
			if (buildingProduction.getConsumptionAmount () > 0)
				consumptionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "BuildingConsumption").replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("CONSUMPTION_AMOUNT", new Integer (buildingProduction.getConsumptionAmount ()).toString ()).replaceAll
					("BUILDING_NAME", (building == null) ? buildingProduction.getBuildingID () : building.getBuildingName ()));
		}
		
		// Production from how many books we have at our wizards' fortress
		for (final CityProductionBreakdownPickType pickTypeProduction : calc.getPickTypeProduction ())
		{
			final Building building = getLanguage ().findBuilding (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
			
			productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromFortressPicks").replaceAll
				("PRODUCTION_TYPE", productionTypeDescription).replaceAll
				("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (pickTypeProduction.getDoubleProductionAmountAllPicks ())).replaceAll
				("BUILDING_NAME", (building == null) ? CommonDatabaseConstants.VALUE_BUILDING_FORTRESS : building.getBuildingName ()).replaceAll
				("PICK_TYPE", getLanguage ().findPickTypeDescription (pickTypeProduction.getPickTypeID ())).replaceAll
				("PRODUCTION_PER_PICK", getTextUtils ().halfIntToStr (pickTypeProduction.getDoubleProductionAmountEachPick ())).replaceAll
				("PICK_COUNT", new Integer (pickTypeProduction.getCount ()).toString ()));
		}
		
		// Production from what plane our wizards' fortress is on
		if (calc.getDoubleProductionAmountFortressPlane () > 0)
		{
			final Building building = getLanguage ().findBuilding (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
			final Plane plane = getLanguage ().findPlane (calc.getFortressPlane ());

			productionBreakdowns.add (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromFortressPlane").replaceAll
				("PRODUCTION_TYPE", productionTypeDescription).replaceAll
				("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (calc.getDoubleProductionAmountFortressPlane ())).replaceAll
				("BUILDING_NAME", (building == null) ? CommonDatabaseConstants.VALUE_BUILDING_FORTRESS : building.getBuildingName ()).replaceAll
				("PLANE_NAME", (plane == null) ? new Integer (calc.getFortressPlane ()).toString () : plane.getPlaneDescription ()));
		}
		
		// Gold trade bonus
		if (calc.getDoubleProductionAmount () > 0)
		{
			int goldTradeBonusCount = 0;
			if (calc.getTradePercentageBonusFromTileType () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusFromTileType").replaceAll
					("PERCENTAGE_VALUE", new Integer (calc.getTradePercentageBonusFromTileType ()).toString ()));
			}
			
			if (calc.getTradePercentageBonusFromRoads () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusFromRoads").replaceAll
					("PERCENTAGE_VALUE", new Integer (calc.getTradePercentageBonusFromRoads ()).toString ()));
			}
			
			if (calc.getTradePercentageBonusFromRace () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusFromRace").replaceAll
					("PERCENTAGE_VALUE", new Integer (calc.getTradePercentageBonusFromRace ()).toString ()));
			}
			
			// Show a total only if there were multiple bonuses to combine
			if (goldTradeBonusCount > 1)
				percentageBonuses.add (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusUncapped").replaceAll
					("PERCENTAGE_VALUE", new Integer (calc.getTradePercentageBonusUncapped ()).toString ()));
			
			// Show cap only if it has an effect
			if (calc.getTradePercentageBonusCapped () < calc.getTradePercentageBonusUncapped ())
				percentageBonuses.add (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusCapped").replaceAll
					("CURRENT_POPULATION_DIV_1000", new Integer (calc.getTotalPopulation ()).toString ()).replaceAll
					("CAP_VALUE", new Integer (calc.getTradePercentageBonusCapped ()).toString ()));
		}
		
		// Did we get 0, 1 or many sources of production?
		int netEffectCount = 0;
		final StringBuilder text = new StringBuilder ();
		if ((productionBreakdowns.size () > 0) && (calc.getDoubleProductionAmount () > 0))
		{
			netEffectCount++;
			
			// Heading
			addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionMainHeading"));
			
			// Detail line(s)
			for (final String line : productionBreakdowns)
				addLine (text, line);
			
			// Total
			if (productionBreakdowns.size ()  > 1)
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "RoundingTotalBefore").replaceAll
					("PRODUCTION_TYPE", productionTypeDescription).replaceAll
					("UNMODIFIED_PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (calc.getDoubleProductionAmount ())));
			
			// Rounding - roundingDirectionID only gets set if the production wasn't an exact multiple of 2
			if (calc.getRoundingDirectionID () != null)
				switch (calc.getRoundingDirectionID ())
				{
					case ROUND_UP:
						addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "RoundingUp").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("PRODUCTION_AMOUNT", new Integer (calc.getBaseProductionAmount ()).toString ()));
						break;
					
					case ROUND_DOWN:
						addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "RoundingDown").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("PRODUCTION_AMOUNT", new Integer (calc.getBaseProductionAmount ()).toString ()));
						break;
						
					default:
						throw new MomException ("describeCityProductionCalculation encountered a roundingDirectionID which wasn't up or down = " + calc.getRoundingDirectionID ());
				}
			
			// Percentage bonuses - put in a list first so we can test whether we get 0, 1 or many entries here
			if ((percentageBonuses.size () > 0) && (calc.getPercentageBonus () > 0))
			{
				// Heading
				addLine (text, null);
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionPercentageBonusHeading"));
				
				// Detail line(s)
				for (final String line : percentageBonuses)
					addLine (text, line);
				
				// Total
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionPercentageBonusTotal").replaceAll
					("PRODUCTION_AMOUNT_FROM_PERCENTAGE_BONUS", new Integer (calc.getModifiedProductionAmount () - calc.getBaseProductionAmount ()).toString ()).replaceAll
					("PERCENTAGE_BONUS", new Integer (calc.getPercentageBonus ()).toString ()).replaceAll
					("UNMODIFIED_PRODUCTION_AMOUNT", new Integer (calc.getBaseProductionAmount ()).toString ()).replaceAll
					("PRODUCTION_AMOUNT", new Integer (calc.getModifiedProductionAmount ()).toString ()));
			}
			
			// Cap
			if (calc.getCappedProductionAmount () < calc.getModifiedProductionAmount ())
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionCapped").replaceAll
					("CAP_VALUE", new Integer (calc.getCappedProductionAmount ()).toString ()));
		}
		
		// Did we get 0, 1 or many sources of consumption?
		if ((consumptionBreakdowns.size () > 0) && (calc.getConsumptionAmount () > 0))
		{
			if (netEffectCount > 0)
				addLine (text, null);
			
			netEffectCount++;
			
			// Heading
			addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ConsumptionMainHeading"));
			
			// Detail line(s)
			for (final String line : consumptionBreakdowns)
				addLine (text, line);
		}
		
		// If we had both production and consumption, then show net effect
		if (netEffectCount > 1)
		{				
			addLine (text, null);

			if (calc.getCappedProductionAmount () == calc.getConsumptionAmount ())
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectBreakingEven"));

			else if (calc.getCappedProductionAmount () > calc.getConsumptionAmount ())
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectGain").replaceAll
					("PRODUCTION_AMOUNT", new Integer (calc.getCappedProductionAmount ()).toString ()).replaceAll
					("CONSUMPTION_AMOUNT", new Integer (calc.getConsumptionAmount ()).toString ()).replaceAll
					("NET_GAIN", new Integer (calc.getCappedProductionAmount () - calc.getConsumptionAmount ()).toString ()));

			else
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectLoss").replaceAll
					("PRODUCTION_AMOUNT", new Integer (calc.getCappedProductionAmount ()).toString ()).replaceAll
					("CONSUMPTION_AMOUNT", new Integer (calc.getConsumptionAmount ()).toString ()).replaceAll
					("NET_LOSS", new Integer (calc.getConsumptionAmount () - calc.getCappedProductionAmount ()).toString ()));
		}

		return text.toString ();
	}	

	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
	}

	/**
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
	}
}