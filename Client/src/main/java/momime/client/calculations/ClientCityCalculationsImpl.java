package momime.client.calculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.language.replacer.CityGrowthRateLanguageVariableReplacer;
import momime.client.language.replacer.CityProductionLanguageVariableReplacer;
import momime.client.language.replacer.CityUnrestLanguageVariableReplacer;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.Building;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionAmountBucketID;
import momime.common.database.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownBuilding;
import momime.common.internal.CityGrowthRateBreakdownDying;
import momime.common.internal.CityGrowthRateBreakdownGrowing;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityProductionBreakdownBuilding;
import momime.common.internal.CityProductionBreakdownEvent;
import momime.common.internal.CityProductionBreakdownMapFeature;
import momime.common.internal.CityProductionBreakdownPickType;
import momime.common.internal.CityProductionBreakdownPlane;
import momime.common.internal.CityProductionBreakdownPopulationTask;
import momime.common.internal.CityProductionBreakdownSpell;
import momime.common.internal.CityProductionBreakdownTileType;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.CityUnrestBreakdownBuilding;
import momime.common.internal.CityUnrestBreakdownSpell;
import momime.common.messages.AvailableUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Client side only methods dealing with city calculations
 */
public final class ClientCityCalculationsImpl implements ClientCityCalculations
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ClientCityCalculationsImpl.class);
	
	/** Indentation used on calculation breakdowns that are additions to the previous line */
	private final static String INDENT = "     ";
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
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
		getUnrestReplacer ().setBreakdown (breakdown);
		final StringBuilder text = new StringBuilder ();
		
		// If Stream of Life is in effect then there's no point listing anything else at all
		final CityUnrestBreakdownSpell streamOfLife = breakdown.getSpellReducingUnrest ().stream ().filter
			(s -> (s.getUnrestReduction () != null) && (s.getUnrestReduction () >= 100)).findAny ().orElse (null);
		if (streamOfLife != null)
		{
			getUnrestReplacer ().setCurrentSpell (streamOfLife);			
			getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getSpellEliminatesUnrest ()));
		}
		else
		{
			// Percentages
			int valuesContributingToPercentage = 0;
			if (breakdown.getTaxPercentage () > 0)
			{
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getTaxPercentage ()));
				valuesContributingToPercentage++;
			}
	
			if (breakdown.getRacialPercentage () > 0)
			{
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getRacialPercentage ()));
				valuesContributingToPercentage++;
			}
	
			// Spell percentages
			for (final CityUnrestBreakdownSpell spellUnrest : breakdown.getSpellReducingUnrest ())
				if (spellUnrest.getUnrestPercentage () != null)
				{
					getUnrestReplacer ().setCurrentSpell (spellUnrest);			
					getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getSpellUnrestPercentage ()));
					valuesContributingToPercentage++;
				}
	
			// Base total
			if (valuesContributingToPercentage > 1)
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getTotalPercentage ()));
	
			if (valuesContributingToPercentage > 0)
			{
				text.append (System.lineSeparator ());
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getBaseValue ()));
			}
			
			// Klackons
			if (breakdown.getRacialLiteral () != 0)
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getRacialLiteral ()));
			
			// Buildings
			for (final CityUnrestBreakdownBuilding buildingUnrest : breakdown.getBuildingReducingUnrest ())
			{
				getUnrestReplacer ().setCurrentBuilding (buildingUnrest);		
				if (buildingUnrest.getNegatedBySpellID () != null)
					getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getBuildingUnrestReductionNegated ()));
				else
					getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getBuildingUnrestReduction ()));
			}
			
			// Divine Power / Infernal Power retort
			if (breakdown.getReligiousBuildingReduction () != 0)
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getRetort ()));
	
			// Spell literal values
			for (final CityUnrestBreakdownSpell spellUnrest : breakdown.getSpellReducingUnrest ())
				if (spellUnrest.getUnrestReduction () != null)
				{
					getUnrestReplacer ().setCurrentSpell (spellUnrest);			
					getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getSpellUnrestReduction ()));
				}
			
			// Units stationed in city
			if (breakdown.getUnitCount () > 0)
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getUnitReduction ()));
			
			// Total
			getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getBaseTotal ()));
			text.append (System.lineSeparator ());
	
			if (breakdown.isForcePositive ())
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getForcePositive ()));
	
			if (breakdown.isForceAll ())
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getForceAll ()));
			
			if (breakdown.getMinimumFarmers () > 0)
				getUnrestReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getMinimumFarmers ()));
		}
		
		return text.toString ();
	}
	
	/**
	 * @param breakdown Results of growth calculation
	 * @return Readable calculation details
	 */
	@Override
	public final String describeCityGrowthRateCalculation (final CityGrowthRateBreakdown breakdown)
	{
		getGrowthReplacer ().setBreakdown (breakdown);
		final StringBuilder text = new StringBuilder ();
		
		// Start off calculation description
		getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getCurrentPopulation ()));
		getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getMaximumPopulation ()));
		
		if (breakdown instanceof CityGrowthRateBreakdownGrowing)
		{
			final CityGrowthRateBreakdownGrowing growing = (CityGrowthRateBreakdownGrowing) breakdown;
			
			getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getBaseGrowthRate ()));

			boolean showTotal = false;
			if (growing.getRacialGrowthModifier () != 0)
			{
				showTotal = true;
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getRacialGrowthModifier ()));
			}
				
			// Bonuses from buildings
			for (final CityGrowthRateBreakdownBuilding buildingGrowth : growing.getBuildingModifier ())
			{
				showTotal = true;
				getGrowthReplacer ().setCurrentBuilding (buildingGrowth);
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getGrowthBonusFromBuilding ()));
			}
				
			if (showTotal)
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getCityGrowthRateTotal ()));
			
			if (growing.getTotalGrowthRate () != growing.getTotalGrowthRateAfterStreamOfLife ())
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getCityGrowthRateTotalAfterStreamOfLife ()));
			
			if (growing.getHousingPercentageBonus () > 0)
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getHousing ()));

			if (growing.getDarkRitualsPercentagLoss () > 0)
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getDarkRituals ()));

			if ((growing.getHousingPercentageBonus () > 0) || (growing.getDarkRitualsPercentagLoss () > 0))
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getPercentageModifiers ()));
			
			if (growing.isPopulationBoom ())
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getPopulationBoom ()));
			
			// Special boost for AI players
			if (growing.getDifficultyLevelMultiplier () != 100)
				getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getDifficultyLevelMultiplier ()));
		}
		else if (breakdown instanceof CityGrowthRateBreakdownDying)
			getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getDeathRate ()));
		
		else
			getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getAtMaximumSize ()));

		// Population cap
		if (breakdown.getCappedTotal () != breakdown.getInitialTotal ())
			getGrowthReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getCityGrowthRateCapped ()));
		
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
		getProductionReplacer ().setBreakdown (calc);
		
		// List out into blocks of production, % bonuses and consumption - so we can test whether each block contains 0, 1 or many entries
		final List<String> productionBeforeBreakdowns = new ArrayList<String> ();
		final List<String> productionAfterBreakdowns = new ArrayList<String> ();
		final List<String> consumptionBreakdowns = new ArrayList<String> ();
		final List<String> percentageBonuses = new ArrayList<String> ();
		final List<String> percentagePenalties = new ArrayList<String> ();
		
		// Try to make a shorthand way of picking the right production bucket so don't have to have ternaries all over the place
		final Map<ProductionAmountBucketID, List<String>> buckets = new HashMap<ProductionAmountBucketID, List<String>> ();
		buckets.put (ProductionAmountBucketID.BEFORE_PERCENTAGE_BONUSES, productionBeforeBreakdowns);
		buckets.put (ProductionAmountBucketID.AFTER_PERCENTAGE_BONUSES, productionAfterBreakdowns);
		
		// Production from farmers/workers/rebels
		for (final CityProductionBreakdownPopulationTask populationTaskProduction : calc.getPopulationTaskProduction ())
		{
			getProductionReplacer ().setCurrentPopulationTask (populationTaskProduction);
			
			if (populationTaskProduction.getCount () == 1)
				buckets.get (populationTaskProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
					(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromSinglePopulation ())));
			else
				buckets.get (populationTaskProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
					(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromMultiplePopulation ())));
		}
		
		// Production from population irrespective of what task they're performing (taxes)
		if (calc.getDoubleProductionAmountAllPopulation () > 0)
			productionBeforeBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getGoldFromTaxes ())));
		
		// Consumption from population irrespective of what task they're performing (eating rations)
		if (calc.getConsumptionAmountAllPopulation () > 0)
			consumptionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getRationsAteByPopulation ())));
		
		for (final CityProductionBreakdownTileType tileTypeProduction : calc.getTileTypeProduction ())
		{
			getProductionReplacer ().setCurrentTileType (tileTypeProduction);
			
			// Production from terrain tiles (food/max city size)
			if (tileTypeProduction.getDoubleProductionAmountAllTiles () > 0)
			{
				if (tileTypeProduction.getCount () == 1)
					buckets.get (tileTypeProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
						(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromSingleTile ())));
				else
					buckets.get (tileTypeProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
						(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromMultipleTiles ())));
			}
			
			// % bonus from terrain tiles (production)
			if (tileTypeProduction.getPercentageBonusAllTiles () > 0)
			{
				if (tileTypeProduction.getCount () == 1)
					percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getPercentageBonusFromSingleTile ())));
				else
					percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getPercentageBonusFromMultipleTiles ())));
			}
		}
		
		// Production from map features
		for (final CityProductionBreakdownMapFeature mapFeatureProduction : calc.getMapFeatureProduction ())
		{
			getProductionReplacer ().setCurrentMapFeature (mapFeatureProduction);
			
			if (mapFeatureProduction.getCount () == 1)
				buckets.get (mapFeatureProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
					(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromSingleMapFeature ())));
			else
				buckets.get (mapFeatureProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
					(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromMultipleMapFeatures ())));

			if (mapFeatureProduction.getRaceMineralBonusMultiplier () > 1)
				buckets.get (mapFeatureProduction.getProductionAmountBucketID ()).add (INDENT + getProductionReplacer ().replaceVariables
					(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromMapFeatureRaceBonus ())));
		
			if (mapFeatureProduction.getBuildingMineralPercentageBonus () > 0)
				buckets.get (mapFeatureProduction.getProductionAmountBucketID ()).add (INDENT + getProductionReplacer ().replaceVariables
					(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromMapFeatureBuildingBonus ())));
		}
		
		for (final CityProductionBreakdownBuilding buildingProduction : calc.getBuildingBreakdown ())
		{
			getProductionReplacer ().setCurrentBuilding (buildingProduction);
			
			// Production from buildings, e.g. Library generating research, or Granary generating food+rations
			if (buildingProduction.getDoubleUnmodifiedProductionAmount () > 0)
			{
				// Evil Presence stops it from producing anything
				if (buildingProduction.getNegatedBySpellID () != null)
					buckets.get (buildingProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
						(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromBuildingNegated ())));
				
				// Shrines etc. generate +50% more power if wizard has Divine or Infernal Power retort
				else if (buildingProduction.getReligiousBuildingPercentageBonus () == 0)
					buckets.get (buildingProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
						(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromBuildingWithoutReligiousRetortBonus ())));
				else
					buckets.get (buildingProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
						(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromBuildingWithReligiousRetortBonus ())));
			}
			
			// % bonus from buildings, e.g. Marketplace generating +25% gold
			if (buildingProduction.getPercentageBonus () > 0)
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getPercentageBonusFromBuilding ())));
			
			// Consumption from buildings, mainly gold maintenance
			if (buildingProduction.getConsumptionAmount () > 0)
				consumptionBreakdowns.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getBuildingConsumption ())));
		}

		// Production from events
		for (final CityProductionBreakdownEvent eventProduction : calc.getEventBreakdown ())
		{
			getProductionReplacer ().setCurrentEvent (eventProduction);
			
			if (eventProduction.getDoubleProductionAmount () != 0)
				buckets.get (eventProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
					(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromEvent ())));
		}
		
		// Production from spells
		for (final CityProductionBreakdownSpell spellProduction : calc.getSpellBreakdown ())
		{
			getProductionReplacer ().setCurrentSpell (spellProduction);
			
			if (spellProduction.getDoubleProductionAmount () != 0)
				buckets.get (spellProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
					(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromSpell ())));
			
			if (spellProduction.getPercentageBonus () > 0)
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getPercentageBonusFromSpell ())));
			else if (spellProduction.getPercentageBonus () < 0)
				percentagePenalties.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getPercentagePenaltyFromSpell ())));
		}
		
		// Production from how many books we have at our wizards' fortress
		for (final CityProductionBreakdownPickType pickTypeProduction : calc.getPickTypeProduction ())
		{
			getProductionReplacer ().setCurrentPickType (pickTypeProduction);
			buckets.get (pickTypeProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
				(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromFortressPicks ())));
		}
		
		// Production from what plane our wizards' fortress is on
		for (final CityProductionBreakdownPlane planeProduction : calc.getPlaneProduction ())
		{
			getProductionReplacer ().setCurrentPlane (planeProduction);
			buckets.get (planeProduction.getProductionAmountBucketID ()).add (getProductionReplacer ().replaceVariables
				(getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionFromFortressPlane ())));
		}			
		
		// Gold trade bonus
		if (calc.getProductionAmountBeforePercentages () > 0)
		{
			int goldTradeBonusCount = 0;
			if (calc.getTradePercentageBonusFromTileType () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getGoldTradeBonusFromTileType ())));
			}
			
			if (calc.getTradePercentageBonusFromRoads () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getGoldTradeBonusFromRoads ())));
			}
			
			if (calc.getTradePercentageBonusFromRace () > 0)
			{
				goldTradeBonusCount++;
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getGoldTradeBonusFromRace ())));
			}
			
			// Show a total only if there were multiple bonuses to combine
			if (goldTradeBonusCount > 1)
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getGoldTradeBonusUncapped ())));
			
			// Show cap only if it has an effect
			if (calc.getTradePercentageBonusCapped () < calc.getTradePercentageBonusUncapped ())
				percentageBonuses.add (getProductionReplacer ().replaceVariables (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getGoldTradeBonusCapped ())));
		}
		
		// "before" productions, and any percentage bonuses or penalties apply to the "before" values,
		// so only bother to include those too if we actually have some "before" values
		int netEffectCount = 0;
		final StringBuilder text = new StringBuilder ();
		if ((productionBeforeBreakdowns.size () > 0) || (productionAfterBreakdowns.size () > 0))
		{
			netEffectCount++;
			
			// Output the befores, and any percentage bonuses or penalties that apply to them
			if (productionBeforeBreakdowns.size () > 0)
			{
				// Heading
				getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionBeforeHeading ()));
				
				// Detail line(s)
				for (final String line : productionBeforeBreakdowns)
					getProductionReplacer ().addLine (text, line);
				
				// Total
				if (productionBeforeBreakdowns.size ()  > 1)
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getRoundingTotalBefore ()));
				
				// Rounding - roundingDirectionID only gets set if the production wasn't an exact multiple of 2
				if (calc.getRoundingDirectionID () != null)
					switch (calc.getRoundingDirectionID ())
					{
						case ROUND_UP:
							getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getRoundingUp ()));
							break;
						
						case ROUND_DOWN:
							getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getRoundingDown ()));
							break;
							
						default:
							throw new MomException ("describeCityProductionCalculation encountered a roundingDirectionID which wasn't up or down = " + calc.getRoundingDirectionID ());
					}
				
				// Percentage bonuses - put in a list first so we can test whether we get 0, 1 or many entries here
				if (percentageBonuses.size () > 0)
				{
					// Heading
					getProductionReplacer ().addLine (text, null);
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionPercentageBonusHeading ()));
					
					// Detail line(s)
					for (final String line : percentageBonuses)
						getProductionReplacer ().addLine (text, line);
					
					// Total
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionPercentageBonusTotal ()));
				}
				
				// Percentage Penalties - put in a list first so we can test whether we get 0, 1 or many entries here
				if (percentagePenalties.size () > 0)
				{
					// Heading
					getProductionReplacer ().addLine (text, null);
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionPercentagePenaltyHeading ()));
					
					// Detail line(s)
					for (final String line : percentagePenalties)
						getProductionReplacer ().addLine (text, line);
					
					// Total
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionPercentagePenaltyTotal ()));
				}
				
				// Overfarming rule?
				if (calc.getFoodProductionFromTerrainTiles () != null)
				{
					// Heading
					getProductionReplacer ().addLine (text, null);
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getOverfarmingRuleHeading ()));
					
					// Adjustment
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getOverfarmingRuleLine ()));
				}
			}
			
			// Output the afters (note because of pre-cleaning done to the lists, this can only happen if we have befores AND afters AND some percentage bonuses or penalties)
			if (productionAfterBreakdowns.size () > 0)
			{
				// Heading - if there weren't any "before"s, then use that heading instead so it doesn't say "Additional"
				getProductionReplacer ().addLine (text, null);
				
				if (productionBeforeBreakdowns.size () > 0)
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionAfterHeading ()));
				else
					getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionBeforeHeading ()));

				// Detail line(s)
				for (final String line : productionAfterBreakdowns)
					getProductionReplacer ().addLine (text, line);
				
				// Total
				getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getBaseTotal ()));
			}			
			
			// Special boost for AI players
			if (calc.getDifficultyLevelMultiplier () != 100)
				getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getDifficultyLevelMultiplier ()));
			
			// Cap
			if (calc.getCappedProductionAmount () < calc.getTotalAdjustedForDifficultyLevel ())
				getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getProductionCapped ()));
		}
		
		// Did we get 0, 1 or many sources of consumption?
		if (consumptionBreakdowns.size () > 0)
		{
			if (netEffectCount > 0)
				getProductionReplacer ().addLine (text, null);
			
			netEffectCount++;
			
			// Heading
			getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getConsumptionMainHeading ()));
			
			// Detail line(s)
			for (final String line : consumptionBreakdowns)
				getProductionReplacer ().addLine (text, line);
		}
		
		// If we had both production and consumption, then show net effect
		if (netEffectCount > 1)
		{				
			getProductionReplacer ().addLine (text, null);

			if (calc.getCappedProductionAmount () == calc.getConsumptionAmount ())
				getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getNetEffectBreakingEven ()));

			else if (calc.getCappedProductionAmount () > calc.getConsumptionAmount ())
				getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getNetEffectGain ()));

			else
				getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getNetEffectLoss ()));
		}
		
		// Trade goods setting
		if (calc.getConvertFromProductionTypeID () != null)
		{
			getProductionReplacer ().addLine (text, null);
			getProductionReplacer ().addLine (text, getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getTradeGoods ()));
		}

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
		final StringBuilder allows = new StringBuilder ();
		
		// City data
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		
		try
		{
			final Race race = getClient ().getClientDB ().findRace (cityData.getCityRaceID (), "describeWhatBuildingAllows");
		
			// Buildings
			for (final momime.common.database.Building building : getClient ().getClientDB ().getBuilding ())
			
				// Don't list buildings we already have
				if (getMemoryBuildingUtils ().findBuilding (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), cityLocation, building.getBuildingID ()) == null)
				{
					// Check if its actually a prerequsite
					boolean prereq = false;
					final Iterator<String> iter = building.getBuildingPrerequisite ().iterator ();
					while ((!prereq) && (iter.hasNext ()))
						if (iter.next ().equals (buildingID))
							prereq = true;
				
					if (prereq)
					{
						// Check the city's race can build it
						boolean canBuild = true;
						final Iterator<String> cannot = race.getRaceCannotBuild ().iterator ();
						while ((canBuild) && (cannot.hasNext ()))
							if (cannot.next ().equals (building.getBuildingID ()))
								canBuild = false;
					
						if (canBuild)
						{
							// Got one!
							if (allows.length () > 0)
								allows.append (", ");
						
							allows.append (getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (building.getBuildingID (), "describeWhatBuildingAllows").getBuildingName ()));
						}
					}
				}

			// We need a dummy unit to generate names from
			final AvailableUnit dummyUnit = new AvailableUnit ();
			
			// Units
			for (final UnitEx unit : getClient ().getClientDB ().getUnits ())
			
				// Check that the unit is the right race for this city or is a generic non-race specific unit, like a Trireme
				if ((CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL.equals (unit.getUnitMagicRealm ())) &&
					((unit.getUnitRaceID () == null) || (unit.getUnitRaceID ().equals (cityData.getCityRaceID ()))))
				{
					// Check if its actually a prerequsite
					boolean prereq = false;
					final Iterator<String> iter = unit.getUnitPrerequisite ().iterator ();
					while ((!prereq) && (iter.hasNext ()))
						if (iter.next ().equals (buildingID))
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
		final String s = (allows.length () == 0) ? null : getLanguageHolder ().findDescription (getLanguages ().getChangeConstructionScreen ().getAllows ()) + " " + allows.toString () + ".";
		return s;
	}
	
	/**
	 * @param cityLocation City location
	 * @return List of all buildings that the player can choose between to construct at the city (including Housing and Trade Goods)
	 * @throws RecordNotFoundException If the race inhabiting the city cannot be found
	 */
	@Override
	public final List<Building> listBuildingsCityCanConstruct (final MapCoordinates3DEx cityLocation) throws RecordNotFoundException
	{
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		
		final Race race = getClient ().getClientDB ().findRace (cityData.getCityRaceID (), "listBuildingsCityCanConstruct");
		
		final List<Building> buildList = new ArrayList<Building> ();
		for (final Building thisBuilding : getClient ().getClientDB ().getBuilding ())
			
			// If we don't have this building already
			if ((getMemoryBuildingUtils ().findBuilding (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
				cityLocation, thisBuilding.getBuildingID ()) == null) &&
				
				// and we have necessary prerequisite buildings (e.g. Farmers' Market requires a Granary)
				(getMemoryBuildingUtils ().meetsBuildingRequirements (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
					cityLocation, thisBuilding)) &&
				
				// and we have any necessary prerequisite tile types (e.g. Ship Wrights' Guild requires an adjacent Ocean tile)
				(getCityCalculations ().buildingPassesTileTypeRequirements (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
					cityLocation, thisBuilding, getClient ().getSessionDescription ().getOverlandMapSize ())))
			{
				// Check the race inhabiting this city can construct this kind of building
				boolean canBuild = true;
				final Iterator<String> iter = race.getRaceCannotBuild ().iterator ();
				while ((canBuild) && (iter.hasNext ()))
					if (iter.next ().equals (thisBuilding.getBuildingID ()))
						canBuild = false;
				
				if (canBuild)
					buildList.add (thisBuilding);
			}
		
		return buildList;
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
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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