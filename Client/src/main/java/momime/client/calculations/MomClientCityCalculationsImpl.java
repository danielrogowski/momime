package momime.client.calculations;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.Pick;
import momime.client.language.database.v0_9_5.Plane;
import momime.client.language.database.v0_9_5.PopulationTask;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.calculations.CalculateCityGrowthRateBreakdown;
import momime.common.calculations.CalculateCityGrowthRateBreakdown_Building;
import momime.common.calculations.CalculateCityProductionResult;
import momime.common.calculations.CalculateCityProductionResultBreakdown;
import momime.common.calculations.CalculateCityUnrestBreakdown;
import momime.common.calculations.CalculateCityUnrestBreakdown_Building;
import momime.common.database.CommonDatabaseConstants;

/**
 * Client side only methods dealing with city calculations
 */
public final class MomClientCityCalculationsImpl implements MomClientCityCalculations
{
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
		
		text.append (line);
	}
	
	/**
	 * @param breakdown Results of unrest calculation
	 * @return Readable calculation details
	 */
	@Override
	public final String describeCityUnrestCalculation (final CalculateCityUnrestBreakdown breakdown)
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
		for (final CalculateCityUnrestBreakdown_Building buildingUnrest : breakdown.getBuildingsReducingUnrest ())
		{
			final Building building = getLanguage ().findBuilding (buildingUnrest.getBuildingID ());
			final String buildingName = (building == null) ? buildingUnrest.getBuildingID () : building.getBuildingName ();
			
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BuildingUnrestReduction").replaceAll
				("BUILDING_REDUCTION", new Integer (buildingUnrest.getUnrestReduction ()).toString ()).replaceAll
				("BUILDING_NAME", buildingName));
		}
		
		// Divine Power / Infernal Power retort
		if (breakdown.getReligiousBuildingReduction () != 0)
		{
			final StringBuilder retortList = new StringBuilder ();
			for (final String pickID : breakdown.getPickIdsContributingToReligiousBuildingBonus ())
			{
				final Pick pick = getLanguage ().findPick (pickID);
				final String pickDesc = (pick == null) ? pickID : pick.getPickDescription ();
				
				if (retortList.length () > 0)
					retortList.append (", ");
				
				retortList.append (pickDesc);
			}
			
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "Retort").replaceAll
				("RELIGIOUS_BUILDING_REDUCTION", new Integer (breakdown.getReligiousBuildingReduction ()).toString ()).replaceAll
				("RETORT_PERCENTAGE", new Integer (breakdown.getReligiousBuildingRetortPercentage ()).toString ()).replaceAll
				("RETORT_LIST", retortList.toString ()).replaceAll
				("RETORT_VALUE", new Integer (breakdown.getReligiousBuildingRetortValue ()).toString ()));
		}
		
		// Units stationed in city
		if (breakdown.getUnitCount () > 0)
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "UnitReduction").replaceAll
				("UNIT_REDUCTION", new Integer (breakdown.getUnitReduction ()).toString ()).replaceAll
				("UNIT_COUNT", new Integer (breakdown.getUnitCount ()).toString ()));
		
		// Total
		addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BaseTotal").replaceAll
			("BASE_TOTAL", new Integer (breakdown.getBaseTotal ()).toString ()));
		text.append ("\r\n");

		if (breakdown.getForcePositive ())
			addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "ForcePositive"));

		if (breakdown.getForceAll ())
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
	public final String describeCityGrowthRateCalculation (final CalculateCityGrowthRateBreakdown breakdown)
	{
		final StringBuilder text = new StringBuilder ();
		
		// Start off calculation description
		addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CurrentPopulation").replaceAll
			("CURRENT_POPULATION", new Integer (breakdown.getCurrentPopulation ()).toString ()));

		addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "MaximumPopulation").replaceAll
			("MAXIMUM_POPULATION", new Integer (breakdown.getMaximumPopulation ()).toString ()));
		
		switch (breakdown.getDirection ())
		{
			case GROWING:
				addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "BaseGrowthRate").replaceAll
					("MAXIMUM_POPULATION_DIV_1000", new Integer (breakdown.getMaximumPopulation () / 1000).toString ()).replaceAll
					("CURRENT_POPULATION_DIV_1000", new Integer (breakdown.getCurrentPopulation () / 1000).toString ()).replaceAll
					("BASE_GROWTH_RATE", new Integer (breakdown.getBaseGrowthRate ()).toString ()));

				boolean showTotal = false;
				if (breakdown.getRacialGrowthModifier () != 0)
				{
					showTotal = true;
					addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "RacialGrowthModifier").replaceAll
						("RACIAL_GROWTH_MODIFIER", new Integer (breakdown.getRacialGrowthModifier ()).toString ()));
				}
				
				// Bonuses from buildings
				for (final CalculateCityGrowthRateBreakdown_Building buildingGrowth : breakdown.getBuildingsModifyingGrowthRate ())
				{
					showTotal = true;
					final Building building = getLanguage ().findBuilding (buildingGrowth.getBuildingID ());
					final String buildingName = (building == null) ? buildingGrowth.getBuildingID () : building.getBuildingName ();					
					
					addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "GrowthBonusFromBuilding").replaceAll
						("BUILDING_NAME", buildingName).replaceAll
						("BUILDING_GROWTH_MODIFIER", new Integer (buildingGrowth.getGrowthRateModifier ()).toString ()));
				}
				
				if (showTotal)
					addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CityGrowthRateTotal").replaceAll
						("TOTAL_GROWTH_RATE", new Integer (breakdown.getTotalGrowthRate ()).toString ()));
				
				if (breakdown.getCappedGrowthRate () < breakdown.getTotalGrowthRate ())
					addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CityGrowthRateCapped").replaceAll
						("CAPPED_GROWTH_RATE", new Integer (breakdown.getCappedGrowthRate ()).toString ()));
				
				break;
				
			case DYING:
				addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "DeathRate").replaceAll
					("BASE_DEATH_RATE", new Integer (breakdown.getBaseDeathRate ()).toString ()).replaceAll
					("CITY_DEATH_RATE", new Integer (breakdown.getCityDeathRate ()).toString ()));
				break;
				
			case MAXIMUM:
				addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "AtMaximumSize"));
				break;
		}
		
		return text.toString ();
	}
	
	/**
	 * Finds and outputs all breakdown text for the specified heading
	 * 
	 * @param calc Results of production calculation
	 * @param heading Which type of heading to generate
	 * @param headingLanguageEntryID Title to put on the heading, if we output anything - can leave this null for no title
	 * @param text Text buffer to add to
	 * @return Whether any text was generated or not
	 * @throws MomException If we find a breakdown entry that we don't know how to describe
	 */
	final boolean processCityProductionCalculationHeading (final CalculateCityProductionResult calc,
		final CityProductionBreakdownHeading heading, final String headingLanguageEntryID, final StringBuilder text) throws MomException
	{
		// Write text to a temporary string here - then can use that to make sure we only output the heading if we actually generated some text
		final StringBuilder headingText = new StringBuilder ();
		
		// This is used in a number of places, so work it out once up front
		final ProductionType productionType = getLanguage ().findProductionType (calc.getProductionTypeID ());
		final String productionTypeDescription = (productionType == null) ? calc.getProductionTypeID () : productionType.getProductionTypeDescription ();
		
		// Check each breakdown to see if its applicable to this heading
		for (final CalculateCityProductionResultBreakdown breakdown : calc.getBreakdowns ())
		{
			// Final totals (check this first, since for these doubleProductionAmount is also filled in
			if (breakdown.getRoundingDirection () != null)
			{
				if (heading == CityProductionBreakdownHeading.PRODUCTION_ROUNDING)
				{
					// If there were multiple lines, i.e. we need to show a total over those lines
					if (text.toString ().contains ("\r\n"))
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "RoundingTotalBefore").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("UNMODIFIED_PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleUnmodifiedProductionAmount ())));
					
					switch (breakdown.getRoundingDirection ())
					{
						case ROUND_UP:
							addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "RoundingUp").replaceAll
								("PRODUCTION_TYPE", productionTypeDescription).replaceAll
								("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())));
							break;
							
						case ROUND_DOWN:
							addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "RoundingDown").replaceAll
								("PRODUCTION_TYPE", productionTypeDescription).replaceAll
								("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())));
							break;

						// Would have been enforced by calculation, nothing to output
						case MUST_BE_EXACT_MULTIPLE:
							throw new MomException ("processCityProductionCalculationHeading: Encountered breakdown section with rounding of 'must be exact multiple' for production type ID \"" +
								calc.getProductionTypeID () + "\"");
					}						
				}
			}
			
			// Production
			else if (breakdown.getDoubleProductionAmount () > 0)
			{
				if (heading == CityProductionBreakdownHeading.PRODUCTION)
				{
					// Production from population
					if (breakdown.getPopulationTaskID () != null)
					{
						final PopulationTask populationTask = getLanguage ().findPopulationTask (breakdown.getPopulationTaskID ());
						
						if (breakdown.getNumberDoingTask () == 1)
							addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromSinglePopulation").replaceAll
								("PRODUCTION_TYPE", productionTypeDescription).replaceAll
								("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())).replaceAll
								("TASK_NAME_SINGULAR", (populationTask == null) ? breakdown.getPopulationTaskID () : populationTask.getPopulationTaskSingular ()));
						else
							addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMultiplePopulation").replaceAll
								("PRODUCTION_TYPE", productionTypeDescription).replaceAll
								("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())).replaceAll
								("TASK_NAME_PLURAL", (populationTask == null) ? breakdown.getPopulationTaskID () : populationTask.getPopulationTaskPlural ()).replaceAll
								("NUMBER_DOING_TASK", new Integer (breakdown.getNumberDoingTask ()).toString ()).replaceAll
								("PRODUCTION_PER_PERSON", getTextUtils ().halfIntToStr (breakdown.getDoubleAmountPerPerson ())));
					}
					
					// Production from how many books we have at our wizards' fortress
					else if (breakdown.getPickTypeID () != null)
					{
						final Building building = getLanguage ().findBuilding (breakdown.getBuildingID ());
						
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromFortressPicks").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())).replaceAll
							("BUILDING_NAME", (building == null) ? breakdown.getBuildingID () : building.getBuildingName ()).replaceAll
							("PICK_TYPE", getLanguage ().findPickTypeDescription (breakdown.getPickTypeID ())).replaceAll
							("PRODUCTION_PER_PICK", getTextUtils ().halfIntToStr (breakdown.getDoubleAmountPerPick ())));
					}
					
					// Production from what plane our wizards' fortress is on
					else if (breakdown.getPlaneNumber () != null)
					{
						final Building building = getLanguage ().findBuilding (breakdown.getBuildingID ());
						final Plane plane = getLanguage ().findPlane (breakdown.getPlaneNumber ());

						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromFortressPlane").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())).replaceAll
							("BUILDING_NAME", (building == null) ? breakdown.getBuildingID () : building.getBuildingName ()).replaceAll
							("PLANE_NAME", (plane == null) ? breakdown.getPlaneNumber ().toString () : plane.getPlaneDescription ()));
					}
					
					// Production from a building other than the wizards' fortress
					else if (breakdown.getBuildingID () != null)
					{
						final Building building = getLanguage ().findBuilding (breakdown.getBuildingID ());

						if (breakdown.getPercentageBonus () > 0)
							addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromBuildingWithReligiousRetortBonus").replaceAll
								("UNMODIFIED_PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleUnmodifiedProductionAmount ())).replaceAll
								("PRODUCTION_TYPE", productionTypeDescription).replaceAll
								("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())).replaceAll
								("BUILDING_NAME", (building == null) ? breakdown.getBuildingID () : building.getBuildingName ()).replaceAll
								("PERCENTAGE_BONUS", new Integer (breakdown.getPercentageBonus ()).toString ()));
						else
							addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromBuildingWithoutReligiousRetortBonus").replaceAll
								("PRODUCTION_TYPE", productionTypeDescription).replaceAll
								("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())).replaceAll
								("BUILDING_NAME", (building == null) ? breakdown.getBuildingID () : building.getBuildingName ()));
					}
					
					// Production from map features, e.g. wild game or mines
					else if (breakdown.getMapFeatureID () != null)
					{
						final MapFeature mapFeature = getLanguage ().findMapFeature (breakdown.getMapFeatureID ());
						
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMapFeature").replaceAll
							("UNMODIFIED_PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleUnmodifiedProductionAmount ())).replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("MAP_FEATURE", (mapFeature == null) ? breakdown.getMapFeatureID () : mapFeature.getMapFeatureDescription ()));
						
						if (breakdown.getRaceMineralBonusMultipler () > 1)
							addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMapFeatureRaceBonus").replaceAll
								("MAP_FEATURE_PRODUCTION_AMOUNT_AFTER_RACE_BONUS", getTextUtils ().halfIntToStr (breakdown.getDoubleAmountAfterRacialBonus ())).replaceAll
								("PRODUCTION_TYPE", productionTypeDescription).replaceAll
								("MINERAL_BONUS_FROM_RACE", new Integer (breakdown.getRaceMineralBonusMultipler ()).toString ()));
					
						if (breakdown.getPercentageBonus () > 0)
							addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMapFeatureBuildingBonus").replaceAll
								("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())).replaceAll
								("PRODUCTION_TYPE", productionTypeDescription).replaceAll
								("PERCENTAGE_BONUS", new Integer (breakdown.getPercentageBonus ()).toString ()));
					}
					
					// Food harvested from surrounding terrain
					else if (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD.equals (calc.getProductionTypeID ()))
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "TerrainFood").replaceAll
							("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())));
						
					// Taxes
					else if (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD.equals (calc.getProductionTypeID ()))
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "GoldFromTaxes").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("PRODUCTION_AMOUNT", getTextUtils ().halfIntToStr (breakdown.getDoubleProductionAmount ())).replaceAll
							("NUMBER_DOING_TASK", new Integer (breakdown.getNumberDoingTask ()).toString ()).replaceAll
							("PRODUCTION_PER_PERSON", getTextUtils ().halfIntToStr (breakdown.getDoubleAmountPerPerson ())));
						
					else
						throw new MomException ("processCityProductionCalculationHeading: Production, but all identifying fields are blank for production type ID \"" +
							calc.getProductionTypeID () + "\"");
				}
			}
			
			// Consumption
			else if (breakdown.getConsumptionAmount () > 0)
			{
				if (heading == CityProductionBreakdownHeading.CONSUMPTION)
				{
					// Building maintenance
					if (breakdown.getBuildingID () != null)
					{
						final Building building = getLanguage ().findBuilding (breakdown.getBuildingID ());
						
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "BuildingConsumption").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("CONSUMPTION_AMOUNT", new Integer (breakdown.getConsumptionAmount ()).toString ()).replaceAll
							("BUILDING_NAME", (building == null) ? breakdown.getBuildingID () : building.getBuildingName ()));
					}
					
					// Population eating rations
					else if (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS.equals (calc.getProductionTypeID ()))
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "RationsAteByPopulation").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("CONSUMPTION_AMOUNT", new Integer (breakdown.getConsumptionAmount ()).toString ()).replaceAll
							("NUMBER_DOING_TASK", new Integer (breakdown.getNumberDoingTask ()).toString ()));

					else
						throw new MomException ("processCityProductionCalculationHeading: Consumption, but all identifying fields are blank for production type ID \"" +
							calc.getProductionTypeID () + "\"");
					
				}
			}
			
			// Percentage added to production
			else if (breakdown.getPercentage () > 0)
			{
				if (heading == CityProductionBreakdownHeading.PERCENTAGE_BONUS_TO_PRODUCTION)
				{
					if (breakdown.getBuildingID () != null)
					{
						final Building building = getLanguage ().findBuilding (breakdown.getBuildingID ());
						
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "PercentageBonusFromBuilding").replaceAll
							("PRODUCTION_TYPE", productionTypeDescription).replaceAll
							("PERCENTAGE_VALUE", new Integer (breakdown.getPercentage ()).toString ()).replaceAll
							("BUILDING_NAME", (building == null) ? breakdown.getBuildingID () : building.getBuildingName ()));
					}

					else if (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION.equals (calc.getProductionTypeID ()))
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "TerrainProductionBonus").replaceAll
							("PERCENTAGE_VALUE", new Integer (breakdown.getPercentage ()).toString ()));
						
					else if (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD.equals (calc.getProductionTypeID ()))
						addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonus").replaceAll
							("PERCENTAGE_VALUE", new Integer (breakdown.getPercentage ()).toString ()));

					else
						throw new MomException ("processCityProductionCalculationHeading: Percentage, but all identifying fields are blank for production type ID \"" +
							calc.getProductionTypeID () + "\"");
				}
			}
			
			// Cap to either production or the percentage
			else if (breakdown.getCap () > 0)
			{
				if ((heading == CityProductionBreakdownHeading.PRODUCTION_CAP) && (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD.equals (calc.getProductionTypeID ())))
					addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "MaxCitySizeCapped").replaceAll
						("CAP_VALUE", new Integer (breakdown.getCap ()).toString ()));

				else if ((heading == CityProductionBreakdownHeading.PERCENTAGE_CAP) && (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD.equals (calc.getProductionTypeID ())))
					addLine (headingText, getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusCapped").replaceAll
						("CURRENT_POPULATION_DIV_1000", new Integer (breakdown.getCurrentPopulation () / 1000).toString ()).replaceAll
						("CAP_VALUE", new Integer (breakdown.getCap ()).toString ()));
			}
		}
		
		// Did we generate anything?
		final boolean anyText = (headingText.length () > 0);
		if (anyText)
		{
			if (headingLanguageEntryID != null)
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", headingLanguageEntryID));
			
			addLine (text, headingText.toString ());
		}
		
		return anyText;
	}
	
	/**
	 * @param calc Results of production calculation
	 * @return Readable calculation details
	 * @throws MomException If we find a breakdown entry that we don't know how to describe
	 */
	@Override
	public final String describeCityProductionCalculation (final CalculateCityProductionResult calc) throws MomException
	{
		final StringBuilder text = new StringBuilder ();
		
		// There can't be any rounding or cap if there's no production!
		final boolean anyProductions = processCityProductionCalculationHeading (calc, CityProductionBreakdownHeading.PRODUCTION, "ProductionMainHeading", text);
		if (anyProductions)
		{
			processCityProductionCalculationHeading (calc, CityProductionBreakdownHeading.PRODUCTION_ROUNDING, null, text);
			processCityProductionCalculationHeading (calc, CityProductionBreakdownHeading.PRODUCTION_CAP, null, text);
			
			// Leave a gap either between production/rounding/cap and % bonus, or between production/rouding/cap and consumption
			text.append ("\r\n");
			
			if (processCityProductionCalculationHeading (calc, CityProductionBreakdownHeading.PERCENTAGE_BONUS_TO_PRODUCTION, "ProductionPercentageBonusHeading", text))
			{
				processCityProductionCalculationHeading (calc, CityProductionBreakdownHeading.PERCENTAGE_CAP, null, text);
				text.append ("\r\n");
				
				// There's no item created in the breakdown for the total percentage bonus or what effect this has, so do it here outside of any headings
				addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionPercentageBonusTotal").replaceAll
					("PERCENTAGE_BONUS", new Integer (calc.getPercentageBonus ()).toString ()).replaceAll
					("UNMODIFIED_PRODUCTION_AMOUNT", new Integer (calc.getBaseProductionAmount ()).toString ()).replaceAll
					("PRODUCTION_AMOUNT_FROM_PERCENTAGE_BONUS", new Integer (calc.getModifiedProductionAmount () - calc.getBaseProductionAmount ()).toString ()).replaceAll
					("PRODUCTION_AMOUNT", new Integer (calc.getModifiedProductionAmount ()).toString ()));
				text.append ("\r\n");
			}
		}
		
		// Consumption
		if (processCityProductionCalculationHeading (calc, CityProductionBreakdownHeading.CONSUMPTION, "ConsumptionMainHeading", text))
		{
			// If both production and consumption, then show net gain/loss
			if (anyProductions)
			{
				text.append ("\r\n");
				
				if (calc.getModifiedProductionAmount () == calc.getConsumptionAmount ())
					addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectBreakingEven"));

				else if (calc.getModifiedProductionAmount () > calc.getConsumptionAmount ())
					addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectGain").replaceAll
						("PRODUCTION_AMOUNT", new Integer (calc.getModifiedProductionAmount ()).toString ()).replaceAll
						("CONSUMPTION_AMOUNT", new Integer (calc.getConsumptionAmount ()).toString ()).replaceAll
						("NET_GAIN", new Integer (calc.getModifiedProductionAmount () - calc.getConsumptionAmount ()).toString ()));

				else
					addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectLoss").replaceAll
						("PRODUCTION_AMOUNT", new Integer (calc.getModifiedProductionAmount ()).toString ()).replaceAll
						("CONSUMPTION_AMOUNT", new Integer (calc.getConsumptionAmount ()).toString ()).replaceAll
						("NET_LOSS", new Integer (calc.getConsumptionAmount () - calc.getModifiedProductionAmount ()).toString ()));
			}
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
