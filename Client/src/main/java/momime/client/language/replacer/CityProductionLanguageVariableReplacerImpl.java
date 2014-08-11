package momime.client.language.replacer;

import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.Plane;
import momime.client.language.database.v0_9_5.PopulationTask;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.TileType;
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
	protected final String determineVariableValue (final String code)
	{
		final String text;
		switch (code)
		{
			case "PRODUCTION_TYPE":
				final ProductionType productionType = getLanguage ().findProductionType (getBreakdown ().getProductionTypeID ());
				text = (productionType == null) ? getBreakdown ().getProductionTypeID () : productionType.getProductionTypeDescription ();
				break;

			case "APPLICABLE_POPULATION":
				text = new Integer (getBreakdown ().getApplicablePopulation ()).toString ();
				break;

			case "PRODUCTION_PER_PERSON":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmountEachPopulation ());
				break;

			case "PRODUCTION_ALL_PEOPLE":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmountAllPopulation ());
				break;

			case "CONSUMPTION_PER_PERSON":
				text = new Integer (getBreakdown ().getConsumptionAmountEachPopulation ()).toString ();
				break;
				
			case "CONSUMPTION_ALL_PEOPLE":
				text = new Integer (getBreakdown ().getConsumptionAmountAllPopulation ()).toString ();
				break;
				
			case "FORTRESS_NAME":
				final Building fortress = getLanguage ().findBuilding (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
				text = (fortress == null) ? CommonDatabaseConstants.VALUE_BUILDING_FORTRESS : fortress.getBuildingName ();
				break;
				
			case "PLANE_NAME":
				final Plane plane = getLanguage ().findPlane (getBreakdown ().getFortressPlane ());
				text = (plane == null) ? new Integer (getBreakdown ().getFortressPlane ()).toString () : plane.getPlaneDescription ();
				break;
				
			case "FORTRESS_PLANE_PRODUCTION":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmountFortressPlane ());
				break;
				
			case "GOLD_TRADE_BONUS_TILE_TYPE":
				text = new Integer (getBreakdown ().getTradePercentageBonusFromTileType ()).toString ();
				break;
				
			case "GOLD_TRADE_BONUS_ROADS":
				text = new Integer (getBreakdown ().getTradePercentageBonusFromRoads ()).toString ();
				break;
				
			case "GOLD_TRADE_BONUS_RACE":
				text = new Integer (getBreakdown ().getTradePercentageBonusFromRace ()).toString ();
				break;
				
			case "GOLD_TRADE_BONUS_TOTAL":
				text = new Integer (getBreakdown ().getTradePercentageBonusUncapped ()).toString ();
				break;
				
			case "GOLD_TRADE_BONUS_CAPPED":
				text = new Integer (getBreakdown ().getTradePercentageBonusCapped ()).toString ();
				break;
				
			case "CURRENT_POPULATION_DIV_1000":
				text = new Integer (getBreakdown ().getTotalPopulation ()).toString ();
				break;
				
			case "UNMODIFIED_PRODUCTION_AMOUNT":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmount ());
				break;
				
			case "ROUNDED_PRODUCTION_AMOUNT":
				text = new Integer (getBreakdown ().getBaseProductionAmount ()).toString ();
				break;
				
			case "PERCENTAGE_BONUS":
				text = new Integer (getBreakdown ().getPercentageBonus ()).toString ();
				break;
				
			case "PRODUCTION_AMOUNT_FROM_PERCENTAGE_BONUS":
				text = new Integer (getBreakdown ().getModifiedProductionAmount () - getBreakdown ().getBaseProductionAmount ()).toString ();
				break;
				
			case "PRODUCTION_TOTAL":
				text = new Integer (getBreakdown ().getModifiedProductionAmount ()).toString ();
				break;
				
			case "PRODUCTION_CAPPED":
				text = new Integer (getBreakdown ().getCappedProductionAmount ()).toString ();
				break;
				
			case "CONSUMPTION_AMOUNT":
				text = new Integer (getBreakdown ().getConsumptionAmount ()).toString ();
				break;
				
			case "NET_GAIN":
				text = new Integer (getBreakdown ().getCappedProductionAmount () - getBreakdown ().getConsumptionAmount ()).toString ();
				break;
				
			case "NET_LOSS":
				text = new Integer (getBreakdown ().getConsumptionAmount () - getBreakdown ().getCappedProductionAmount ()).toString ();
				break;
				
			// Dependant on current population task
			case "TASK_NAME_SINGULAR":
				final PopulationTask populationTaskSingular = getLanguage ().findPopulationTask (getCurrentPopulationTask ().getPopulationTaskID ());
				text = (populationTaskSingular == null) ? getCurrentPopulationTask ().getPopulationTaskID () : populationTaskSingular.getPopulationTaskSingular ();
				break;
				
			case "TASK_NAME_PLURAL":
				final PopulationTask populationTaskPlural = getLanguage ().findPopulationTask (getCurrentPopulationTask ().getPopulationTaskID ());
				text = (populationTaskPlural == null) ? getCurrentPopulationTask ().getPopulationTaskID () : populationTaskPlural.getPopulationTaskPlural ();
				break;

			case "NUMBER_DOING_TASK":
				text = new Integer (getCurrentPopulationTask ().getCount ()).toString ();
				break;
				
			case "PRODUCTION_PER_TASK":
				text = getTextUtils ().halfIntToStr (getCurrentPopulationTask ().getDoubleProductionAmountEachPopulation ());
				break;

			case "PRODUCTION_ALL_PEOPLE_DOING_TASK":
				text = getTextUtils ().halfIntToStr (getCurrentPopulationTask ().getDoubleProductionAmountAllPopulation ());
				break;
				
			// Dependant on current tile type
			case "TILE_TYPE":
				final TileType tileType = getLanguage ().findTileType (getCurrentTileType ().getTileTypeID ());
				text = (tileType == null) ? getCurrentTileType ().getTileTypeID () : tileType.getTileTypeDescription ();
				break;

			case "TILE_COUNT":
				text = new Integer (getCurrentTileType ().getCount ()).toString ();
				break;
				
			case "PRODUCTION_PER_TILE":
				text = getTextUtils ().halfIntToStr (getCurrentTileType ().getDoubleProductionAmountEachTile ());
				break;
				
			case "PRODUCTION_ALL_TILES":
				text = getTextUtils ().halfIntToStr (getCurrentTileType ().getDoubleProductionAmountAllTiles ());
				break;
				
			case "PERCENTAGE_PER_TILE":
				text = new Integer (getCurrentTileType ().getPercentageBonusEachTile ()).toString ();
				break;
				
			case "PERCENTAGE_ALL_TILES":
				text = new Integer (getCurrentTileType ().getPercentageBonusAllTiles ()).toString ();
				break;

			// Dependant on current map feature
			case "MAP_FEATURE":
				final MapFeature mapFeature = getLanguage ().findMapFeature (getCurrentMapFeature ().getMapFeatureID ());
				text = (mapFeature == null) ? getCurrentMapFeature ().getMapFeatureID () : mapFeature.getMapFeatureDescription ();
				break;

			case "MAP_FEATURE_COUNT":
				text = new Integer (getCurrentMapFeature ().getCount ()).toString ();
				break;

			case "PRODUCTION_PER_MAP_FEATURE":
				text = getTextUtils ().halfIntToStr (getCurrentMapFeature ().getDoubleUnmodifiedProductionAmountEachFeature ());
				break;
				
			case "PRODUCTION_ALL_MAP_FEATURES":
				text = getTextUtils ().halfIntToStr (getCurrentMapFeature ().getDoubleUnmodifiedProductionAmountAllFeatures ());
				break;
				
			case "MINERAL_BONUS_FROM_RACE":
				text = new Integer (getCurrentMapFeature ().getRaceMineralBonusMultiplier ()).toString ();
				break;
				
			case "MAP_FEATURE_PRODUCTION_AMOUNT_AFTER_RACE_BONUS":
				text = getTextUtils ().halfIntToStr (getCurrentMapFeature ().getDoubleProductionAmountAfterRacialMultiplier ());
				break;

			case "BUILDING_MINERAL_PERCENTAGE_BONUS":
				text = new Integer (getCurrentMapFeature ().getBuildingMineralPercentageBonus ()).toString ();
				break;

			case "MAP_FEATURE_PRODUCTION_AMOUNT_FINAL":
				text = getTextUtils ().halfIntToStr (getCurrentMapFeature ().getDoubleModifiedProductionAmountAllFeatures ());
				break;
				
			// Dependant on current building
			case "BUILDING_NAME":
				final Building building = getLanguage ().findBuilding (getCurrentBuilding ().getBuildingID ());
				text = (building == null) ? getCurrentBuilding ().getBuildingID () : building.getBuildingName ();
				break;

			case "BUILDING_UNMODIFIED_PRODUCTION":
				text = getTextUtils ().halfIntToStr (getCurrentBuilding ().getDoubleUnmodifiedProductionAmount ());
				break;

			case "RELIGIOUS_PERCENTAGE_BONUS":
				text = new Integer (getCurrentBuilding ().getReligiousBuildingPercentageBonus ()).toString ();
				break;
				
			case "BUILDING_MODIFIED_PRODUCTION":
				text = getTextUtils ().halfIntToStr (getCurrentBuilding ().getDoubleModifiedProductionAmount ());
				break;

			case "RETORT_LIST":
				text = listPickDescriptions (getCurrentBuilding ().getPickIdContributingToReligiousBuildingBonus ());
				break;

			case "BUILDING_PERCENTAGE_VALUE":
				text = new Integer (getCurrentBuilding ().getPercentageBonus ()).toString ();
				break;

			case "BUILDING_CONSUMPTION":
				text = new Integer (getCurrentBuilding ().getConsumptionAmount ()).toString ();
				break;

			// Dependant on current pick type
			case "PICK_TYPE":
				text = getLanguage ().findPickTypeDescription (getCurrentPickType ().getPickTypeID ());
				break;

			case "PICK_COUNT":
				text = new Integer (getCurrentPickType ().getCount ()).toString ();
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