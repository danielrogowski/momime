package momime.client.language.replacer;

import momime.client.language.database.BuildingLang;
import momime.client.language.database.MapFeatureLang;
import momime.client.language.database.PickTypeLang;
import momime.client.language.database.PlaneLang;
import momime.client.language.database.PopulationTaskLang;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.TileTypeLang;
import momime.common.database.CommonDatabaseConstants;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityProductionBreakdownBuilding;
import momime.common.internal.CityProductionBreakdownMapFeature;
import momime.common.internal.CityProductionBreakdownPickType;
import momime.common.internal.CityProductionBreakdownPopulationTask;
import momime.common.internal.CityProductionBreakdownTileType;

/**
 * Language replacer for city production variables
 */
public final class CityProductionLanguageVariableReplacerImpl extends BreakdownLanguageVariableReplacerImpl<CityProductionBreakdown> implements CityProductionLanguageVariableReplacer
{
	/** Population task specific breakdown */
	private CityProductionBreakdownPopulationTask currentPopulationTask;
	
	/** Tile type specific breakdown */
	private CityProductionBreakdownTileType currentTileType;
	
	/** Map feature specific breakdown */
	private CityProductionBreakdownMapFeature currentMapFeature;
	
	/** Building specific breakdown */
	private CityProductionBreakdownBuilding currentBuilding;
	
	/** Pick type specific breakdown */
	private CityProductionBreakdownPickType currentPickType;
	
	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 */
	@Override
	public final String determineVariableValue (final String code)
	{
		final String text;
		switch (code)
		{
			case "PRODUCTION_TYPE":
				final ProductionTypeLang productionType = getLanguage ().findProductionType (getBreakdown ().getProductionTypeID ());
				text = (productionType == null) ? getBreakdown ().getProductionTypeID () : productionType.getProductionTypeDescription ();
				break;

			case "APPLICABLE_POPULATION":
				text = Integer.valueOf (getBreakdown ().getApplicablePopulation ()).toString ();
				break;

			case "PRODUCTION_PER_PERSON":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmountEachPopulation ());
				break;

			case "PRODUCTION_ALL_PEOPLE":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmountAllPopulation ());
				break;

			case "CONSUMPTION_PER_PERSON":
				text = Integer.valueOf (getBreakdown ().getConsumptionAmountEachPopulation ()).toString ();
				break;
				
			case "CONSUMPTION_ALL_PEOPLE":
				text = Integer.valueOf (getBreakdown ().getConsumptionAmountAllPopulation ()).toString ();
				break;
				
			case "FORTRESS_NAME":
				final BuildingLang fortress = getLanguage ().findBuilding (CommonDatabaseConstants.BUILDING_FORTRESS);
				text = (fortress == null) ? CommonDatabaseConstants.BUILDING_FORTRESS : fortress.getBuildingName ();
				break;
				
			case "PLANE_NAME":
				final PlaneLang plane = getLanguage ().findPlane (getBreakdown ().getFortressPlane ());
				text = (plane == null) ? Integer.valueOf (getBreakdown ().getFortressPlane ()).toString () : plane.getPlaneDescription ();
				break;
				
			case "FORTRESS_PLANE_PRODUCTION":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmountFortressPlane ());
				break;
				
			case "GOLD_TRADE_BONUS_TILE_TYPE":
				text = Integer.valueOf (getBreakdown ().getTradePercentageBonusFromTileType ()).toString ();
				break;
				
			case "GOLD_TRADE_BONUS_ROADS":
				text = Integer.valueOf (getBreakdown ().getTradePercentageBonusFromRoads ()).toString ();
				break;
				
			case "GOLD_TRADE_BONUS_RACE":
				text = Integer.valueOf (getBreakdown ().getTradePercentageBonusFromRace ()).toString ();
				break;
				
			case "GOLD_TRADE_BONUS_TOTAL":
				text = Integer.valueOf (getBreakdown ().getTradePercentageBonusUncapped ()).toString ();
				break;
				
			case "GOLD_TRADE_BONUS_CAPPED":
				text = Integer.valueOf (getBreakdown ().getTradePercentageBonusCapped ()).toString ();
				break;
				
			case "CURRENT_POPULATION_DIV_1000":
				text = Integer.valueOf (getBreakdown ().getTotalPopulation ()).toString ();
				break;
				
			case "UNMODIFIED_PRODUCTION_AMOUNT":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmount ());
				break;
				
			case "ROUNDED_PRODUCTION_AMOUNT":
				text = Integer.valueOf (getBreakdown ().getBaseProductionAmount ()).toString ();
				break;
				
			case "PERCENTAGE_BONUS":
				text = Integer.valueOf (getBreakdown ().getPercentageBonus ()).toString ();
				break;
				
			case "PRODUCTION_AMOUNT_FROM_PERCENTAGE_BONUS":
				text = Integer.valueOf (getBreakdown ().getModifiedProductionAmount () - getBreakdown ().getBaseProductionAmount ()).toString ();
				break;
				
			case "PRODUCTION_TOTAL":
				text = Integer.valueOf (getBreakdown ().getModifiedProductionAmount ()).toString ();
				break;
				
			case "PRODUCTION_CAPPED":
				text = Integer.valueOf (getBreakdown ().getCappedProductionAmount ()).toString ();
				break;
				
			case "CONSUMPTION_AMOUNT":
				text = Integer.valueOf (getBreakdown ().getConsumptionAmount ()).toString ();
				break;
				
			case "NET_GAIN":
				text = Integer.valueOf (getBreakdown ().getCappedProductionAmount () - getBreakdown ().getConsumptionAmount ()).toString ();
				break;
				
			case "NET_LOSS":
				text = Integer.valueOf (getBreakdown ().getConsumptionAmount () - getBreakdown ().getCappedProductionAmount ()).toString ();
				break;

			case "CONVERT_PRODUCTION_FROM_TYPE":
				final ProductionTypeLang convertProductionType = getLanguage ().findProductionType (getBreakdown ().getConvertFromProductionTypeID ());
				text = (convertProductionType == null) ? getBreakdown ().getConvertFromProductionTypeID () : convertProductionType.getProductionTypeDescription ();
				break;
				
			case "CONVERT_PRODUCTION_FROM_AMOUNT":
				text = Integer.valueOf (getBreakdown ().getConvertFromProductionAmount ()).toString ();
				break;
				
			case "CONVERT_PRODUCTION_TO_AMOUNT":
				text = Integer.valueOf (getBreakdown ().getConvertToProductionAmount ()).toString ();
				break;
				
			// Dependant on current population task
			case "TASK_NAME_SINGULAR":
				final PopulationTaskLang populationTaskSingular = getLanguage ().findPopulationTask (getCurrentPopulationTask ().getPopulationTaskID ());
				text = (populationTaskSingular == null) ? getCurrentPopulationTask ().getPopulationTaskID () : populationTaskSingular.getPopulationTaskSingular ();
				break;
				
			case "TASK_NAME_PLURAL":
				final PopulationTaskLang populationTaskPlural = getLanguage ().findPopulationTask (getCurrentPopulationTask ().getPopulationTaskID ());
				text = (populationTaskPlural == null) ? getCurrentPopulationTask ().getPopulationTaskID () : populationTaskPlural.getPopulationTaskPlural ();
				break;

			case "NUMBER_DOING_TASK":
				text = Integer.valueOf (getCurrentPopulationTask ().getCount ()).toString ();
				break;
				
			case "PRODUCTION_PER_TASK":
				text = getTextUtils ().halfIntToStr (getCurrentPopulationTask ().getDoubleProductionAmountEachPopulation ());
				break;

			case "PRODUCTION_ALL_PEOPLE_DOING_TASK":
				text = getTextUtils ().halfIntToStr (getCurrentPopulationTask ().getDoubleProductionAmountAllPopulation ());
				break;
				
			// Dependant on current tile type
			case "TILE_TYPE":
				final TileTypeLang tileType = getLanguage ().findTileType (getCurrentTileType ().getTileTypeID ());
				text = (tileType == null) ? getCurrentTileType ().getTileTypeID () : tileType.getTileTypeDescription ();
				break;

			case "TILE_COUNT":
				text = Integer.valueOf (getCurrentTileType ().getCount ()).toString ();
				break;
				
			case "PRODUCTION_PER_TILE":
				text = getTextUtils ().halfIntToStr (getCurrentTileType ().getDoubleProductionAmountEachTile ());
				break;
				
			case "PRODUCTION_ALL_TILES":
				text = getTextUtils ().halfIntToStr (getCurrentTileType ().getDoubleProductionAmountAllTiles ());
				break;
				
			case "PERCENTAGE_PER_TILE":
				text = Integer.valueOf (getCurrentTileType ().getPercentageBonusEachTile ()).toString ();
				break;
				
			case "PERCENTAGE_ALL_TILES":
				text = Integer.valueOf (getCurrentTileType ().getPercentageBonusAllTiles ()).toString ();
				break;

			// Dependant on current map feature
			case "MAP_FEATURE":
				final MapFeatureLang mapFeature = getLanguage ().findMapFeature (getCurrentMapFeature ().getMapFeatureID ());
				text = (mapFeature == null) ? getCurrentMapFeature ().getMapFeatureID () : mapFeature.getMapFeatureDescription ();
				break;

			case "MAP_FEATURE_COUNT":
				text = Integer.valueOf (getCurrentMapFeature ().getCount ()).toString ();
				break;

			case "PRODUCTION_PER_MAP_FEATURE":
				text = getTextUtils ().halfIntToStr (getCurrentMapFeature ().getDoubleUnmodifiedProductionAmountEachFeature ());
				break;
				
			case "PRODUCTION_ALL_MAP_FEATURES":
				text = getTextUtils ().halfIntToStr (getCurrentMapFeature ().getDoubleUnmodifiedProductionAmountAllFeatures ());
				break;
				
			case "MINERAL_BONUS_FROM_RACE":
				text = Integer.valueOf (getCurrentMapFeature ().getRaceMineralBonusMultiplier ()).toString ();
				break;
				
			case "MAP_FEATURE_PRODUCTION_AMOUNT_AFTER_RACE_BONUS":
				text = getTextUtils ().halfIntToStr (getCurrentMapFeature ().getDoubleProductionAmountAfterRacialMultiplier ());
				break;

			case "BUILDING_MINERAL_PERCENTAGE_BONUS":
				text = Integer.valueOf (getCurrentMapFeature ().getBuildingMineralPercentageBonus ()).toString ();
				break;

			case "MAP_FEATURE_PRODUCTION_AMOUNT_FINAL":
				text = getTextUtils ().halfIntToStr (getCurrentMapFeature ().getDoubleModifiedProductionAmountAllFeatures ());
				break;
				
			// Dependant on current building
			case "BUILDING_NAME":
				final BuildingLang building = getLanguage ().findBuilding (getCurrentBuilding ().getBuildingID ());
				text = (building == null) ? getCurrentBuilding ().getBuildingID () : building.getBuildingName ();
				break;

			case "BUILDING_UNMODIFIED_PRODUCTION":
				text = getTextUtils ().halfIntToStr (getCurrentBuilding ().getDoubleUnmodifiedProductionAmount ());
				break;

			case "RELIGIOUS_PERCENTAGE_BONUS":
				text = Integer.valueOf (getCurrentBuilding ().getReligiousBuildingPercentageBonus ()).toString ();
				break;
				
			case "BUILDING_MODIFIED_PRODUCTION":
				text = getTextUtils ().halfIntToStr (getCurrentBuilding ().getDoubleModifiedProductionAmount ());
				break;

			case "RETORT_LIST":
				text = listPickDescriptions (getCurrentBuilding ().getPickIdContributingToReligiousBuildingBonus ());
				break;

			case "BUILDING_PERCENTAGE_VALUE":
				text = Integer.valueOf (getCurrentBuilding ().getPercentageBonus ()).toString ();
				break;

			case "BUILDING_CONSUMPTION":
				text = Integer.valueOf (getCurrentBuilding ().getConsumptionAmount ()).toString ();
				break;

			// Dependant on current pick type
			case "PICK_TYPE":
				final PickTypeLang pickType = getLanguage ().findPickType (getCurrentPickType ().getPickTypeID ());
				final String pickTypeDescription = (pickType == null) ? null : pickType.getPickTypeDescriptionSingular ();
				text = (pickTypeDescription != null) ? pickTypeDescription : getCurrentPickType ().getPickTypeID ();
				break;

			case "PICK_COUNT":
				text = Integer.valueOf (getCurrentPickType ().getCount ()).toString ();
				break;
				
			case "PRODUCTION_PER_PICK":
				text = getTextUtils ().halfIntToStr (getCurrentPickType ().getDoubleProductionAmountEachPick ());
				break;
				
			case "PRODUCTION_ALL_PICKS":
				text = getTextUtils ().halfIntToStr (getCurrentPickType ().getDoubleProductionAmountAllPicks ());
				break;
				
			default:
				text = null;
		}
		return text;
	}

	/**
	 * @return Population task specific breakdown
	 */
	public final CityProductionBreakdownPopulationTask getCurrentPopulationTask ()
	{
		return currentPopulationTask;
	}

	/**
	 * @param pop Population task specific breakdown
	 */
	@Override
	public final void setCurrentPopulationTask (final CityProductionBreakdownPopulationTask pop)
	{
		currentPopulationTask = pop;
	}

	/**
	 * @return Tile type specific breakdown
	 */
	public final CityProductionBreakdownTileType getCurrentTileType ()
	{
		return currentTileType;
	}

	/**
	 * @param tile Tile type specific breakdown
	 */
	@Override
	public final void setCurrentTileType (final CityProductionBreakdownTileType tile)
	{
		currentTileType = tile;
	}

	/**
	 * @return Map feature specific breakdown
	 */
	public final CityProductionBreakdownMapFeature getCurrentMapFeature ()
	{
		return currentMapFeature;
	}

	/**
	 * @param feature Map feature specific breakdown
	 */
	@Override
	public final void setCurrentMapFeature (final CityProductionBreakdownMapFeature feature)
	{
		currentMapFeature = feature;
	}

	/**
	 * @return Building specific breakdown
	 */
	public final CityProductionBreakdownBuilding getCurrentBuilding ()
	{
		return currentBuilding;
	}

	/**
	 * @param building Building specific breakdown
	 */
	@Override
	public final void setCurrentBuilding (final CityProductionBreakdownBuilding building)
	{
		currentBuilding = building;
	}

	/**
	 * @return Pick type specific breakdown
	 */
	public final CityProductionBreakdownPickType getCurrentPickType ()
	{
		return currentPickType;
	}

	/**
	 * @param pickType Pick type specific breakdown
	 */
	@Override
	public final void setCurrentPickType (final CityProductionBreakdownPickType pickType)
	{
		currentPickType = pickType;
	}
}