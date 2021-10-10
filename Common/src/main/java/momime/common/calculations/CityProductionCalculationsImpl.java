package momime.common.calculations;

import java.util.Collections;
import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.PickType;
import momime.common.database.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TaxRate;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PlayerPick;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;

/**
 * Uses the individual calculation methods in CityCalculations to work out the entire production of a city
 */
public final class CityProductionCalculationsImpl implements CityProductionCalculations
{
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/**
	 * @param players Pre-locked players list
	 * @param map Known terrain
	 * @param buildings List of known buildings
	 * @param spells List of known spells
	 * @param cityLocation Location of the city to calculate for; NB. It must be possible to call this on a map location which is not yet a city, so the AI can consider potential sites
	 * @param taxRateID Tax rate to use for the calculation
	 * @param sd Session description
	 * @param includeProductionAndConsumptionFromPopulation Normally true; if false, production and consumption from civilian population will be excluded
	 * 	(This is needed when calculating minimumFarmers, i.e. how many rations does the city produce from buildings and map features only, without considering farmers)
	 * @param calculatePotential Normally false; if true, will consider city size and gold trade bonus to be as they will be after the city is built up
	 * 	(This is typically used in conjunction with includeProductionAndConsumptionFromPopulation=false for the AI to consider the potential value of sites where it may build cities)
	 * @param db Lookup lists built over the XML database
	 * @return List of all productions and consumptions from this city
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final CityProductionBreakdownsEx calculateAllCityProductions (final List<? extends PlayerPublicDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings, final List<MemoryMaintainedSpell> spells,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd, final boolean includeProductionAndConsumptionFromPopulation,
		final boolean calculatePotential, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final MemoryGridCell mc = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final OverlandMapCityData cityData = mc.getCityData ();
		final String raceID = (cityData != null) ? cityData.getCityRaceID () : null;
		final Race cityRace = (raceID != null) ? db.findRace (raceID, "calculateAllCityProductions") : null;

		final Integer cityOwnerID = (cityData != null) ? cityData.getCityOwnerID () : null;
		final PlayerPublicDetails cityOwner = ((players != null) && (cityOwnerID != null)) ? getMultiplayerSessionUtils ().findPlayerWithID (players, cityOwnerID, "calculateAllCityProductions") : null;
		final List<PlayerPick> cityOwnerPicks = (cityOwner != null) ? ((MomPersistentPlayerPublicKnowledge) cityOwner.getPersistentPlayerPublicKnowledge ()).getPick () : null;

		// Set up results object
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();

		// Food production from surrounding tiles
		final CityProductionBreakdown food = getCityCalculations ().listCityFoodProductionFromTerrainTiles (map, cityLocation, sd.getOverlandMapSize (), db);
		productionValues.getProductionType ().add (food);

		// Production % increase from surrounding tiles
		final CityProductionBreakdown production = getCityCalculations ().listCityProductionPercentageBonusesFromTerrainTiles (map, cityLocation, sd.getOverlandMapSize (), db);
		productionValues.getProductionType ().add (production);

		// Deal with people
		if (includeProductionAndConsumptionFromPopulation)
		{
			// Gold from taxes
			final TaxRate taxRate = db.findTaxRate (taxRateID, "calculateAllCityProductions");
			final int taxPayers = (cityData.getCityPopulation () / 1000) - cityData.getNumberOfRebels ();

			// If tax rate set to zero then we're getting no money from taxes, so don't add it
			// Otherwise we get a production entry in the breakdown of zero which produces an error on the client
			if ((taxRate.getDoubleTaxGold () > 0) && (taxPayers > 0))
			{
				final CityProductionBreakdown gold = new CityProductionBreakdown ();
				gold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				gold.setApplicablePopulation (taxPayers);
				gold.setDoubleProductionAmountEachPopulation (taxRate.getDoubleTaxGold ());
				gold.setDoubleProductionAmountAllPopulation (taxPayers * taxRate.getDoubleTaxGold ());
				gold.setDoubleProductionAmount (gold.getDoubleProductionAmountAllPopulation ());

				productionValues.getProductionType ().add (gold);
			}

			// Rations consumption by population
			final int eaters = cityData.getCityPopulation () / 1000;
			if (eaters > 0)
			{
				final CityProductionBreakdown rations = new CityProductionBreakdown ();
				rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
				rations.setApplicablePopulation (eaters);
				rations.setConsumptionAmountEachPopulation (1);
				rations.setConsumptionAmountAllPopulation (eaters);
				rations.setConsumptionAmount (eaters);
				
				productionValues.getProductionType ().add (rations);
			}

			// Production from population
			getCityCalculations ().addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.POPULATION_TASK_ID_FARMER,
				cityData.getMinimumFarmers () + cityData.getOptionalFarmers (), cityLocation, buildings, db);

			getCityCalculations ().addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.POPULATION_TASK_ID_WORKER,
				(cityData.getCityPopulation () / 1000) - cityData.getMinimumFarmers () - cityData.getOptionalFarmers () - cityData.getNumberOfRebels (), cityLocation, buildings, db);

			// With magical races, even the rebels produce power
			getCityCalculations ().addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.POPULATION_TASK_ID_REBEL,
				cityData.getNumberOfRebels (), cityLocation, buildings, db);
		}

		// Production from and Maintenance of buildings
		int doubleTotalFromReligiousBuildings = 0;
		for (final Building thisBuilding : db.getBuilding ())
			
			// If calculatePotential is true, assume we've built everything
			// We only really need to count the granary and farmers' market, but easier just to include everything than to specifically discount these
			if (((calculatePotential) && (!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS))) ||
				((!calculatePotential) && (getMemoryBuildingUtils ().findBuilding (buildings, cityLocation, thisBuilding.getBuildingID ()) != null)))
			{
				if (thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS))
				{
					// Wizard's fortress produces mana according to how many books were chosen at the start of the game...
					for (final PickType thisPickType : db.getPickType ())
						getCityCalculations ().addProductionFromFortressPickType (productionValues, thisPickType, getPlayerPickUtils ().countPicksOfType
							(cityOwnerPicks, thisPickType.getPickTypeID (), true, db));

					// ...and according to which plane it is on
					getCityCalculations ().addProductionFromFortressPlane (productionValues, db.findPlane (cityLocation.getZ (), "calculateAllCityProductions"));
				}

				// Regular building
				// Do not count buildings with a pending sale
				else if (!thisBuilding.getBuildingID ().equals (mc.getBuildingIdSoldThisTurn ()))
					doubleTotalFromReligiousBuildings = doubleTotalFromReligiousBuildings + getCityCalculations ().addProductionAndConsumptionFromBuilding
						(productionValues, thisBuilding, cityOwnerPicks, db);
			}
		
		// Bonuses from spells like Dark Rituals or Prosperity
		if (spells != null)
			for (final MemoryMaintainedSpell spell : spells)
				if (cityLocation.equals (spell.getCityLocation ()))
					getCityCalculations ().addProductionFromSpell (productionValues, spell, doubleTotalFromReligiousBuildings, db);

		// Maintenance cost of city enchantment spells
		// Left this out - its commented out in the Delphi code as well due to the fact that
		// Temples and such produce magic power, but spell maintenance is charged in Mana

		// See if we've got a miners' guild to boost the income from map features
		final CityProductionBreakdown mineralPercentageResult = productionValues.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER);
		final int buildingMineralPercentageBonus = (mineralPercentageResult != null) ? mineralPercentageResult.getPercentageBonus () : 0;
		final int raceMineralBonusMultipler = (cityRace != null) ? cityRace.getMineralBonusMultiplier () : 1;

		// Production from nearby map features
		// Have to do this after buildings, so we can have discovered if we have the miners' guild bonus to map features
		getCityCalculations ().addProductionFromMapFeatures (productionValues, map, cityLocation, sd.getOverlandMapSize (), db, raceMineralBonusMultipler, buildingMineralPercentageBonus);
		
		// Halve and cap food (max city size) production first, because if calculatePotential=true then we need to know the potential max city size before
		// we can calculate the gold trade % cap.
		// Have to do this after map features are added in, since wild game increase max city size.
		getCityCalculations ().halveAddPercentageBonusAndCapProduction (cityOwner, food, sd.getDifficultyLevel (), db);
		
		// Gold trade % from rivers and oceans
		// Have to do this (at least the cap) after map features, since if calculatePotential=true then we need to have included wild game
		// into considering the potential maximum size this city will reach and cap the gold trade % accordingly
		final CityProductionBreakdown gold = productionValues.findOrAddProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		getCityCalculations ().calculateGoldTradeBonus (gold, map, cityLocation, (calculatePotential ? food.getCappedProductionAmount () : null), sd.getOverlandMapSize (), db);

		// Halve production values, using rounding defined in XML file for each production type (consumption values aren't doubled to begin with)
		for (final CityProductionBreakdown thisProduction : productionValues.getProductionType ())
			if (thisProduction != food)
				getCityCalculations ().halveAddPercentageBonusAndCapProduction (cityOwner, thisProduction, sd.getDifficultyLevel (), db);
		
		// Convert production to gold, if set to trade goods
		final String currentlyConstructingBuildingID = (cityData != null) ? cityData.getCurrentlyConstructingBuildingID () : null;
		if ((CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (currentlyConstructingBuildingID)) && (production.getCappedProductionAmount () > 1))
		{
			gold.setConvertFromProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
			gold.setConvertFromProductionAmount (production.getCappedProductionAmount () - production.getConsumptionAmount ());
			gold.setConvertToProductionAmount (gold.getConvertFromProductionAmount () / 2);
		}

		// Sort the list
		Collections.sort (productionValues.getProductionType (), new CityProductionBreakdownSorter ());

		return productionValues;
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
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
	
	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
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
}