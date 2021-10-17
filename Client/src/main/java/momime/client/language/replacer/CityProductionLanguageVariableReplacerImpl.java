package momime.client.language.replacer;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityProductionBreakdownBuilding;
import momime.common.internal.CityProductionBreakdownMapFeature;
import momime.common.internal.CityProductionBreakdownPickType;
import momime.common.internal.CityProductionBreakdownPlane;
import momime.common.internal.CityProductionBreakdownPopulationTask;
import momime.common.internal.CityProductionBreakdownSpell;
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
	
	/** Spell specific breakdown */
	private CityProductionBreakdownSpell currentSpell;
	
	/** Pick type specific breakdown */
	private CityProductionBreakdownPickType currentPickType;
	
	/** Plane specific breakdown */
	private CityProductionBreakdownPlane currentPlane;
	
	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	@Override
	public final String determineVariableValue (final String code) throws RecordNotFoundException
	{
		final String text;
		switch (code)
		{
			case "PRODUCTION_TYPE":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findProductionType
					(getBreakdown ().getProductionTypeID (), "determineVariableValue").getProductionTypeDescription ());
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
				text = getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findBuilding (CommonDatabaseConstants.BUILDING_FORTRESS, "determineVariableValue").getBuildingName ());
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
				
			case "PRODUCTION_AMOUNT_BEFORE_PERCENTAGES":
				text = getTextUtils ().halfIntToStr (getBreakdown ().getDoubleProductionAmountBeforePercentages ());
				break;
				
			case "ROUNDED_PRODUCTION_AMOUNT":
				text = Integer.valueOf (getBreakdown ().getProductionAmountBeforePercentages ()).toString ();
				break;
				
			case "PERCENTAGE_BONUS":
				text = Integer.valueOf (getBreakdown ().getPercentageBonus ()).toString ();
				break;
				
			case "PERCENTAGE_PENALTY":
				text = Integer.valueOf (getBreakdown ().getPercentagePenalty ()).toString ();
				break;
				
			case "PRODUCTION_AMOUNT_FROM_PERCENTAGE_BONUS":
				text = Integer.valueOf (getBreakdown ().getProductionAmountPlusPercentageBonus () - getBreakdown ().getProductionAmountBeforePercentages ()).toString ();
				break;

			case "PRODUCTION_LOSS_FROM_PERCENTAGE_PENALTY":
				text = Integer.valueOf (getBreakdown ().getProductionAmountPlusPercentageBonus () - getBreakdown ().getProductionAmountMinusPercentagePenalty ()).toString ();
				break;
				
			case "PRODUCTION_TOTAL_AFTER_BONUS":
				text = Integer.valueOf (getBreakdown ().getProductionAmountPlusPercentageBonus ()).toString ();
				break;
				
			case "PRODUCTION_TOTAL_AFTER_PENALTY":
				text = Integer.valueOf (getBreakdown ().getProductionAmountMinusPercentagePenalty ()).toString ();
				break;
				
			case "FOOD_FROM_TERRAIN":
				text = getBreakdown ().getFoodProductionFromTerrainTiles ().toString ();
				break;
				
			case "OVERFARMING_AMOUNT":
				text = Integer.valueOf (getBreakdown ().getProductionAmountMinusPercentagePenalty () - getBreakdown ().getFoodProductionFromTerrainTiles ()).toString ();
				break;
				
			case "OVERFARMING_PRODUCTION_GAINED":
				text = Integer.valueOf (getBreakdown ().getProductionAmountAfterOverfarmingPenalty () - getBreakdown ().getFoodProductionFromTerrainTiles ()).toString ();
				break;
				
			case "PRODUCTION_TOTAL_AFTER_OVERFARMING":
				text = getBreakdown ().getProductionAmountAfterOverfarmingPenalty ().toString ();
				break;
				
			case "PRODUCTION_AMOUNT_BASE_TOTAL":
				text = Integer.valueOf (getBreakdown ().getProductionAmountBaseTotal ()).toString ();
				break;
				
			// AI players
			case "AI_PRODUCTION_RATE_MULTIPLIER":
				text = Integer.valueOf (getBreakdown ().getDifficultyLevelMultiplier ()).toString ();
				break;
				
			case "TOTAL_ADJUSTED_FOR_DIFFICULTY_LEVEL":
				text = Integer.valueOf (getBreakdown ().getTotalAdjustedForDifficultyLevel ()).toString ();
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
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findProductionType
					(getBreakdown ().getConvertFromProductionTypeID (), "determineVariableValue").getProductionTypeDescription ());
				break;
				
			case "CONVERT_PRODUCTION_FROM_AMOUNT":
				text = Integer.valueOf (getBreakdown ().getConvertFromProductionAmount ()).toString ();
				break;
				
			case "CONVERT_PRODUCTION_TO_AMOUNT":
				text = Integer.valueOf (getBreakdown ().getConvertToProductionAmount ()).toString ();
				break;
				
			// Dependant on current population task
			case "TASK_NAME_SINGULAR":
				text = getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findPopulationTask (getCurrentPopulationTask ().getPopulationTaskID (), "determineVariableValue").getPopulationTaskSingular ());
				break;
				
			case "TASK_NAME_PLURAL":
				text = getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findPopulationTask (getCurrentPopulationTask ().getPopulationTaskID (), "determineVariableValue").getPopulationTaskPlural ());
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
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findTileType
					(getCurrentTileType ().getTileTypeID (), "determineVariableValue").getTileTypeDescription ());
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
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findMapFeature
					(getCurrentMapFeature ().getMapFeatureID (), "determineVariableValue").getMapFeatureDescription ());
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
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (getCurrentBuilding ().getBuildingID (), "determineVariableValue").getBuildingName ());
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

			case "NEGATED_BY_SPELL_NAME":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getCurrentBuilding ().getNegatedBySpellID (), "determineVariableValue").getSpellName ());
				break;
				
			// Dependant on current spell
			case "SPELL_NAME":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getCurrentSpell ().getSpellID (), "determineVariableValue").getSpellName ());
				break;

			case "SPELL_PRODUCTION":
				text = getTextUtils ().halfIntToStr (getCurrentSpell ().getDoubleProductionAmount ());
				break;
				
			case "SPELL_PERCENTAGE_VALUE":
				text = Integer.valueOf (getCurrentSpell ().getPercentageBonus ()).toString ();
				break;
				
			case "SPELL_PERCENTAGE_PENALTY":
				text = Integer.valueOf (-getCurrentSpell ().getPercentageBonus ()).toString ();
				break;
				
			// Dependant on current pick type
			case "PICK_TYPE":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findPickType
					(getCurrentPickType ().getPickTypeID (), "determineVariableValue").getPickTypeDescriptionSingular ());
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

			// Dependant on current plane
			case "PLANE_NAME":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findPlane
					(getCurrentPlane ().getFortressPlane (), "determineVariableValue").getPlaneDescription ());
				break;
				
			case "FORTRESS_PLANE_PRODUCTION":
				text = getTextUtils ().halfIntToStr (getCurrentPlane ().getDoubleProductionAmountFortressPlane ());
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
	 * @return Spell specific breakdown
	 */
	public final CityProductionBreakdownSpell getCurrentSpell ()
	{
		return currentSpell;
	}

	/**
	 * @param s Spell specific breakdown
	 */
	@Override
	public final void setCurrentSpell (final CityProductionBreakdownSpell s)
	{
		currentSpell = s;
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

	/**
	 * @return Plane specific breakdown
	 */
	public final CityProductionBreakdownPlane getCurrentPlane ()
	{
		return currentPlane;
	}
	
	/**
	 * @param plane Plane specific breakdown
	 */
	@Override
	public final void setCurrentPlane (final CityProductionBreakdownPlane plane)
	{
		currentPlane = plane;
	}
}