package momime.client.calculations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import momime.client.MomClient;
import momime.client.language.database.BuildingLang;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.replacer.CityGrowthRateLanguageVariableReplacer;
import momime.client.language.replacer.CityProductionLanguageVariableReplacer;
import momime.client.language.replacer.CityUnrestLanguageVariableReplacer;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.MomException;
import momime.common.database.BuildingPrerequisite;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Race;
import momime.common.database.RaceCannotBuild;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitPrerequisite;
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
import momime.common.messages.AvailableUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Client side only methods dealing with city calculations
 */
public final class ClientCityCalculationsImpl implements ClientCityCalculations
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ClientCityCalculationsImpl.class);
	
	/** Indentation used on calculation breakdowns that are additions to the previous line */
	private static final String INDENT = "     ";
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** City unrest variable replacer */
	private CityUnrestLanguageVariableReplacer unrestReplacer;
	
	/** City growth rate variable replacer */
	private CityGrowthRateLanguageVariableReplacer growthReplacer;
	
	/** City production variable replacer */
	private CityProductionLanguageVariableReplacer productionReplacer;
	
	/**
	 * @param breakdown Results of unrest calculation
	 * @return Readable calculation details
	 */
	@Override
	public final String describeCityUnrestCalculation (final CityUnrestBreakdown breakdown)
	{
		log.trace ("Entering describeCityUnrestCalculation");

		getUnrestReplacer ().setBreakdown (breakdown);
		final StringBuilder text = new StringBuilder ();
		
		// Percentages
		if (breakdown.getTaxPercentage () > 0)
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "TaxPercentage"));

		if (breakdown.getRacialPercentage () > 0)
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "RacialPercentage"));

		if ((breakdown.getTaxPercentage () > 0) && (breakdown.getRacialPercentage () > 0))
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "TotalPercentage"));
		
		if ((breakdown.getTaxPercentage () > 0) || (breakdown.getRacialPercentage () > 0))
		{
			text.append (System.lineSeparator ());
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BaseValue"));
		}
		
		// Klackons
		if (breakdown.getRacialLiteral () != 0)
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "RacialLiteral"));
		
		// Buildings
		for (final CityUnrestBreakdownBuilding buildingUnrest : breakdown.getBuildingReducingUnrest ())
		{
			getUnrestReplacer ().setCurrentBuilding (buildingUnrest);			
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BuildingUnrestReduction"));
		}
		
		// Divine Power / Infernal Power retort
		if (breakdown.getReligiousBuildingReduction () != 0)
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "Retort"));
		
		// Units stationed in city
		if (breakdown.getUnitCount () > 0)
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "UnitReduction"));
		
		// Total
		getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "BaseTotal"));
		text.append (System.lineSeparator ());

		if (breakdown.isForcePositive ())
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "ForcePositive"));

		if (breakdown.isForceAll ())
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "ForceAll"));
		
		if (breakdown.getMinimumFarmers () > 0)
			getUnrestReplacer ().addLine (text, getLanguage ().findCategoryEntry ("UnrestCalculation", "MinimumFarmers"));
		
		log.trace ("Exiting describeCityUnrestCalculation");
		return text.toString ();
	}
	
	/**
	 * @param breakdown Results of growth calculation
	 * @return Readable calculation details
	 */
	@Override
	public final String describeCityGrowthRateCalculation (final CityGrowthRateBreakdown breakdown)
	{
		log.trace ("Entering describeCityGrowthRateCalculation");
		
		getGrowthReplacer ().setBreakdown (breakdown);
		final StringBuilder text = new StringBuilder ();
		
		// Start off calculation description
		getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CurrentPopulation"));
		getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "MaximumPopulation"));
		
		if (breakdown instanceof CityGrowthRateBreakdownGrowing)
		{
			final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) breakdown;
			
			getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "BaseGrowthRate"));

			boolean showTotal = false;
			if (growing.getRacialGrowthModifier () != 0)
			{
				showTotal = true;
				getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "RacialGrowthModifier"));
			}
				
			// Bonuses from buildings
			for (final CityGrowthRateBreakdownBuilding buildingGrowth : growing.getBuildingModifier ())
			{
				showTotal = true;
				getGrowthReplacer ().setCurrentBuilding (buildingGrowth);
				getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "GrowthBonusFromBuilding"));
			}
				
			if (showTotal)
				getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CityGrowthRateTotal"));
			
			if (growing.getHousingPercentageBonus () > 0)
				getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "Housing"));
				
			if (growing.getCappedGrowthRate () < growing.getTotalGrowthRate ())
				getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "CityGrowthRateCapped"));
		}
		else if (breakdown instanceof CityGrowthRateBreakdownDying)
			getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "DeathRate"));
		
		else
			getGrowthReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityGrowthRate", "AtMaximumSize"));
		
		log.trace ("Exiting describeCityGrowthRateCalculation");
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
		log.trace ("Entering describeCityProductionCalculation");

		getProductionReplacer ().setBreakdown (calc);
		
		// List out into blocks of production, % bonuses and consumption - so we can test whether each block contains 0, 1 or many entries
		final List<String> productionBreakdowns = new ArrayList<String> ();
		final List<String> consumptionBreakdowns = new ArrayList<String> ();
		final List<String> percentageBonuses = new ArrayList<String> ();
		
		// Production from farmers/workers/rebels
		for (final CityProductionBreakdownPopulationTask populationTaskProduction : calc.getPopulationTaskProduction ())
		{
			getProductionReplacer ().setCurrentPopulationTask (populationTaskProduction);
			
			if (populationTaskProduction.getCount () == 1)
				productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromSinglePopulation")));
			else
				productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMultiplePopulation")));
		}
		
		// Production from population irrespective of what task they're performing (taxes)
		if (calc.getDoubleProductionAmountAllPopulation () > 0)
			productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "GoldFromTaxes")));
		
		// Consumption from population irrespective of what task they're performing (eating rations)
		if (calc.getConsumptionAmountAllPopulation () > 0)
			consumptionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "RationsAteByPopulation")));
		
		for (final CityProductionBreakdownTileType tileTypeProduction : calc.getTileTypeProduction ())
		{
			getProductionReplacer ().setCurrentTileType (tileTypeProduction);
			
			// Production from terrain tiles (food/max city size)
			if (tileTypeProduction.getDoubleProductionAmountAllTiles () > 0)
			{
				if (tileTypeProduction.getCount () == 1)
					productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromSingleTile")));
				else
					productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMultipleTiles")));
			}
			
			// % bonus from terrain tiles (production)
			if (tileTypeProduction.getPercentageBonusAllTiles () > 0)
			{
				if (tileTypeProduction.getCount () == 1)
					percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "PercentageBonusFromSingleTile")));
				else
					percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "PercentageBonusFromMultipleTiles")));
			}
		}
		
		// Production from map features
		for (final CityProductionBreakdownMapFeature mapFeatureProduction : calc.getMapFeatureProduction ())
		{
			getProductionReplacer ().setCurrentMapFeature (mapFeatureProduction);
			
			if (mapFeatureProduction.getCount () == 1)
				productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromSingleMapFeature")));
			else
				productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMultipleMapFeatures")));

			if (mapFeatureProduction.getRaceMineralBonusMultiplier () > 1)
				productionBreakdowns.add (INDENT + getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMapFeatureRaceBonus")));
		
			if (mapFeatureProduction.getBuildingMineralPercentageBonus () > 0)
				productionBreakdowns.add (INDENT + getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromMapFeatureBuildingBonus")));
		}
		
		for (final CityProductionBreakdownBuilding buildingProduction : calc.getBuildingBreakdown ())
		{
			getProductionReplacer ().setCurrentBuilding (buildingProduction);
			
			// Production from buildings, e.g. Library generating research, or Granary generating food+rations
			if (buildingProduction.getDoubleUnmodifiedProductionAmount () > 0)
			{
				// Shrines etc. generate +50% more power if wizard has Divine or Infernal Power retort
				if (buildingProduction.getReligiousBuildingPercentageBonus () == 0)
					productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromBuildingWithoutReligiousRetortBonus")));
				else
					productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromBuildingWithReligiousRetortBonus")));
			}
			
			// % bonus from buildings, e.g. Marketplace generating +25% gold
			if (buildingProduction.getPercentageBonus () > 0)
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "PercentageBonusFromBuilding")));
			
			// Consumption from buildings, mainly gold maintainence
			if (buildingProduction.getConsumptionAmount () > 0)
				consumptionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "BuildingConsumption")));
		}
		
		// Production from how many books we have at our wizards' fortress
		for (final CityProductionBreakdownPickType pickTypeProduction : calc.getPickTypeProduction ())
		{
			getProductionReplacer ().setCurrentPickType (pickTypeProduction);
			productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromFortressPicks")));
		}
		
		// Production from what plane our wizards' fortress is on
		if (calc.getDoubleProductionAmountFortressPlane () > 0)
			productionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "ProductionFromFortressPlane")));
		
		// Gold trade bonus
		if (calc.getDoubleProductionAmount () > 0)
		{
			int goldTradeBonusCount = 0;
			if (calc.getTradePercentageBonusFromTileType () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusFromTileType")));
			}
			
			if (calc.getTradePercentageBonusFromRoads () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusFromRoads")));
			}
			
			if (calc.getTradePercentageBonusFromRace () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusFromRace")));
			}
			
			// Show a total only if there were multiple bonuses to combine
			if (goldTradeBonusCount > 1)
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusUncapped")));
			
			// Show cap only if it has an effect
			if (calc.getTradePercentageBonusCapped () < calc.getTradePercentageBonusUncapped ())
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguage ().findCategoryEntry ("CityProduction", "GoldTradeBonusCapped")));
		}
		
		// Did we get 0, 1 or many sources of production?
		int netEffectCount = 0;
		final StringBuilder text = new StringBuilder ();
		if ((productionBreakdowns.size () > 0) && (calc.getDoubleProductionAmount () > 0))
		{
			netEffectCount++;
			
			// Heading
			getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionMainHeading"));
			
			// Detail line(s)
			for (final String line : productionBreakdowns)
				getProductionReplacer ().addLine (text, line);
			
			// Total
			if (productionBreakdowns.size ()  > 1)
				getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "RoundingTotalBefore"));
			
			// Rounding - roundingDirectionID only gets set if the production wasn't an exact multiple of 2
			if (calc.getRoundingDirectionID () != null)
				switch (calc.getRoundingDirectionID ())
				{
					case ROUND_UP:
						getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "RoundingUp"));
						break;
					
					case ROUND_DOWN:
						getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "RoundingDown"));
						break;
						
					default:
						throw new MomException ("describeCityProductionCalculation encountered a roundingDirectionID which wasn't up or down = " + calc.getRoundingDirectionID ());
				}
			
			// Percentage bonuses - put in a list first so we can test whether we get 0, 1 or many entries here
			if ((percentageBonuses.size () > 0) && (calc.getPercentageBonus () > 0))
			{
				// Heading
				getProductionReplacer ().addLine (text, null);
				getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionPercentageBonusHeading"));
				
				// Detail line(s)
				for (final String line : percentageBonuses)
					getProductionReplacer ().addLine (text, line);
				
				// Total
				getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionPercentageBonusTotal"));
			}
			
			// Cap
			if (calc.getCappedProductionAmount () < calc.getModifiedProductionAmount ())
				getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ProductionCapped"));
		}
		
		// Did we get 0, 1 or many sources of consumption?
		if ((consumptionBreakdowns.size () > 0) && (calc.getConsumptionAmount () > 0))
		{
			if (netEffectCount > 0)
				getProductionReplacer ().addLine (text, null);
			
			netEffectCount++;
			
			// Heading
			getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "ConsumptionMainHeading"));
			
			// Detail line(s)
			for (final String line : consumptionBreakdowns)
				getProductionReplacer ().addLine (text, line);
		}
		
		// If we had both production and consumption, then show net effect
		if (netEffectCount > 1)
		{				
			getProductionReplacer ().addLine (text, null);

			if (calc.getCappedProductionAmount () == calc.getConsumptionAmount ())
				getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectBreakingEven"));

			else if (calc.getCappedProductionAmount () > calc.getConsumptionAmount ())
				getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectGain"));

			else
				getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "NetEffectLoss"));
		}
		
		// Trade goods setting
		if (calc.getConvertFromProductionTypeID () != null)
		{
			getProductionReplacer ().addLine (text, null);
			getProductionReplacer ().addLine (text, getLanguage ().findCategoryEntry ("CityProduction", "TradeGoods"));
		}

		log.trace ("Exiting describeCityProductionCalculation");
		return text.toString ();
	}	

	/**
	 * @param buildingID Building that we're constructing
	 * @param cityLocation City location
	 * @return Readable list of what constructing this building will then allow us to build, both buildings and units
	 */
	@Override
	public final String describeWhatBuildingAllows (final String buildingID, final MapCoordinates3DEx cityLocation)
	{
		log.trace ("Entering describeWhatBuildingAllows: " + buildingID + ", " + cityLocation);
		
		final StringBuilder allows = new StringBuilder ();
		
		// City data
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		
		try
		{
			final Race race = getClient ().getClientDB ().findRace (cityData.getCityRaceID (), "describeWhatBuildingAllows");
		
			// Buildings
			for (final momime.common.database.Building building : getClient ().getClientDB ().getBuildings ())
			
				// Don't list buildings we already have
				if (getMemoryBuildingUtils ().findBuilding (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), cityLocation, building.getBuildingID ()) == null)
				{
					// Check if its actually a prerequsite
					boolean prereq = false;
					final Iterator<BuildingPrerequisite> iter = building.getBuildingPrerequisite ().iterator ();
					while ((!prereq) && (iter.hasNext ()))
						if (iter.next ().getPrerequisiteID ().equals (buildingID))
							prereq = true;
				
					if (prereq)
					{
						// Check the city's race can build it
						boolean canBuild = true;
						final Iterator<RaceCannotBuild> cannot = race.getRaceCannotBuild ().iterator ();
						while ((canBuild) && (cannot.hasNext ()))
							if (cannot.next ().getCannotBuildBuildingID ().equals (building.getBuildingID ()))
								canBuild = false;
					
						if (canBuild)
						{
							// Got one!
							if (allows.length () > 0)
								allows.append (", ");
						
							final BuildingLang buildingLang = getLanguage ().findBuilding (building.getBuildingID ());
							allows.append ((buildingLang != null) ? buildingLang.getBuildingName () : building.getBuildingID ());
						}
					}
				}

			// We need a dummy unit to generate names from
			final AvailableUnit dummyUnit = new AvailableUnit ();
			
			// Units
			for (final momime.common.database.Unit unit : getClient ().getClientDB ().getUnits ())
			
				// Check that the unit is the right race for this city or is a generic non-race specific unit, like a Trireme
				if ((CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL.equals (unit.getUnitMagicRealm ())) &&
					((unit.getUnitRaceID () == null) || (unit.getUnitRaceID ().equals (cityData.getCityRaceID ()))))
				{
					// Check if its actually a prerequsite
					boolean prereq = false;
					final Iterator<UnitPrerequisite> iter = unit.getUnitPrerequisite ().iterator ();
					while ((!prereq) && (iter.hasNext ()))
						if (iter.next ().getPrerequisiteID ().equals (buildingID))
							prereq = true;
				
					if (prereq)
					{
						// Got one!
						if (allows.length () > 0)
							allows.append (", ");
					
						dummyUnit.setUnitID (unit.getUnitID ());
						allows.append (getUnitClientUtils ().getUnitName (dummyUnit, UnitNameType.SIMPLE_UNIT_NAME));
					}
				}
		}
		catch (final RecordNotFoundException e)
		{
			// Log error and throw it away, its only for a description, and means we handle it once here rather than bothering to let it ripple up through the calling method stack
			log.error (e, e);
		}
		
		// Did we find anything?
		final String s = (allows.length () == 0) ? null : getLanguage ().findCategoryEntry ("frmChangeConstruction", "Allows") + " " + allows.toString () + ".";
		log.trace ("Exiting describeWhatBuildingAllows = " + s);
		return s;
	}
	
	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
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
	 * @return City unrest variable replacer
	 */
	public final CityUnrestLanguageVariableReplacer getUnrestReplacer ()
	{
		return unrestReplacer;
	}
	
	/**
	 * @param replacer City unrest variable replacer
	 */
	public final void setUnrestReplacer (final CityUnrestLanguageVariableReplacer replacer)
	{
		unrestReplacer = replacer;
	}
	
	/**
	 * @return City growth rate variable replacer
	 */
	public final CityGrowthRateLanguageVariableReplacer getGrowthReplacer ()
	{
		return growthReplacer;
	}

	/**
	 * @param replacer City growth rate variable replacer
	 */
	public final void setGrowthReplacer (final CityGrowthRateLanguageVariableReplacer replacer)
	{
		growthReplacer = replacer;
	}

	/**
	 * @return City production variable replacer
	 */
	public final CityProductionLanguageVariableReplacer getProductionReplacer ()
	{
		return productionReplacer;
	}

	/**
	 * @param replacer City production variable replacer
	 */
	public final void setProductionReplacer (final CityProductionLanguageVariableReplacer replacer)
	{
		productionReplacer = replacer;
	}
}