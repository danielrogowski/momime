package momime.common.calculations;

import java.util.ArrayList;
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
import momime.common.database.Event;
import momime.common.database.Pick;
import momime.common.database.PickType;
import momime.common.database.RaceEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PlayerPick;
import momime.common.utils.CityProductionUtils;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
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
	
	/** Utils for totalling up city production */
	private CityProductionUtils cityProductionUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @param players Players list
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * @param cityLocation Location of the city to calculate for; NB. It must be possible to call this on a map location which is not yet a city, so the AI can consider potential sites
	 * @param taxRateID Tax rate to use for the calculation
	 * @param sd Session description
	 * @param conjunctionEventID Currently active conjunction, if there is one
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
	public final CityProductionBreakdownsEx calculateAllCityProductions (final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd, final String conjunctionEventID,
		final boolean includeProductionAndConsumptionFromPopulation, final boolean calculatePotential, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// Set up results object
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();

		final MemoryGridCell mc = mem.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		
		// This can legitmately be null, when the AI is scouting possible locations to send settlers to, but no city exists there yet
		final OverlandMapCityData cityData = mc.getCityData ();
		
		final String raceID = (cityData != null) ? cityData.getCityRaceID () : null;
		final RaceEx cityRace = (raceID != null) ? db.findRace (raceID, "calculateAllCityProductions") : null;

		final Integer cityOwnerID = (cityData != null) ? cityData.getCityOwnerID () : null;
		final PlayerPublicDetails cityOwnerPlayer = ((players != null) && (cityOwnerID != null)) ? getMultiplayerSessionUtils ().findPlayerWithID (players, cityOwnerID, "calculateAllCityProductions") : null;
		final KnownWizardDetails cityOwnerWizard = (cityOwnerID != null) ? getKnownWizardUtils ().findKnownWizardDetails (mem.getWizardDetails (), cityOwnerID, "calculateAllCityProductions") : null;
		final List<PlayerPick> cityOwnerPicks = (cityOwnerWizard != null) ? cityOwnerWizard.getPick () : null;

		// Food production from surrounding tiles
		final CityProductionBreakdown food = getCityCalculations ().listCityFoodProductionFromTerrainTiles (mem.getMap (), cityLocation, sd.getOverlandMapSize (), db);
		productionValues.getProductionType ().add (food);

		// Production % increase from surrounding tiles
		final CityProductionBreakdown production = getCityCalculations ().listCityProductionPercentageBonusesFromTerrainTiles
			(mem.getMap (), mem.getMaintainedSpell (), cityLocation, sd.getOverlandMapSize (), db);
		productionValues.getProductionType ().add (production);

		// Deal with people
		if ((includeProductionAndConsumptionFromPopulation) && ((cityData == null) || (cityData.getCityPopulation () >= CommonDatabaseConstants.MIN_CITY_POPULATION)))
		{
			// Gold from taxes
			final CityProductionBreakdown gold = getCityCalculations ().addGoldFromTaxes (cityData, taxRateID, db);
			if (gold != null)
				productionValues.getProductionType ().add (gold);

			// Rations consumption by population
			final CityProductionBreakdown rations = getCityCalculations ().addRationsEatenByPopulation (cityData);
			if (rations != null)
				productionValues.getProductionType ().add (rations);

			// Production from population
			getCityCalculations ().addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.POPULATION_TASK_ID_FARMER,
				cityData.getMinimumFarmers () + cityData.getOptionalFarmers (), cityLocation, mem.getBuilding (), db);

			getCityCalculations ().addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.POPULATION_TASK_ID_WORKER,
				(cityData.getCityPopulation () / 1000) - cityData.getMinimumFarmers () - cityData.getOptionalFarmers () - cityData.getNumberOfRebels (), cityLocation, mem.getBuilding (), db);

			// With magical races, even the rebels produce power
			getCityCalculations ().addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.POPULATION_TASK_ID_REBEL,
				cityData.getNumberOfRebels (), cityLocation, mem.getBuilding (), db);
		}
		
		// See if they get any benefit from religious buildings or if it is nullified
		final MemoryMaintainedSpell evilPresence = getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(mem.getMaintainedSpell (), null, null, null, null, cityLocation, CommonDatabaseConstants.CITY_SPELL_EFFECT_ID_EVIL_PRESENCE);
		final String religiousBuildingsNegatedBySpellID = ((evilPresence != null) && (cityOwnerPicks != null) &&
			(getPlayerPickUtils ().getQuantityOfPick (cityOwnerPicks, CommonDatabaseConstants.PICK_ID_DEATH_BOOK) == 0)) ? evilPresence.getSpellID () : null;
		
		// Production from and Maintenance of buildings (even for outposts, since you can cast Wall of Stone to add a city wall to an outpost, and should be charged maintenance for it)
		int doubleTotalFromReligiousBuildings = 0;
		for (final Building thisBuilding : db.getBuilding ())
			
			// If calculatePotential is true, assume we've built everything
			// We only really need to count the granary and farmers' market, but easier just to include everything than to specifically discount these
			if (((calculatePotential) && (!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS))) ||
				((!calculatePotential) && (getMemoryBuildingUtils ().findBuilding (mem.getBuilding (), cityLocation, thisBuilding.getBuildingID ()) != null)))
			{
				if (thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS))
				{
					// Wizard's fortress produces mana according to how many books were chosen at the start of the game...
					for (final PickType thisPickType : db.getPickType ())
						getCityCalculations ().addProductionFromFortressPickType (productionValues, thisPickType, getPlayerPickUtils ().countPicksOfType
							(cityOwnerPicks, thisPickType.getPickTypeID (), true, db), db);

					// ...and according to which plane it is on
					getCityCalculations ().addProductionFromFortressPlane (productionValues, db.findPlane (cityLocation.getZ (), "calculateAllCityProductions"), db);
				}

				// Regular building
				// Do not count buildings with a pending sale
				else if (!thisBuilding.getBuildingID ().equals (mc.getBuildingIdSoldThisTurn ()))
					doubleTotalFromReligiousBuildings = doubleTotalFromReligiousBuildings + getCityCalculations ().addProductionAndConsumptionFromBuilding
						(productionValues, thisBuilding, religiousBuildingsNegatedBySpellID, cityOwnerPicks, db);
			}
		
		// Religious building amount modified by good/bad moon
		if ((doubleTotalFromReligiousBuildings > 0) && (conjunctionEventID != null))
		{
			final Event conjunctionEvent = db.findEvent (conjunctionEventID, "calculateAllCityProductions");
			if (conjunctionEvent.getEventMagicRealm () != null)
			{
				final Pick magicRealm = db.findPick (conjunctionEvent.getEventMagicRealm (), "calculateAllCityProductions");
				if (!magicRealm.getPickExclusiveFrom ().isEmpty ())		// Ignore node conjunctions
					doubleTotalFromReligiousBuildings = getCityCalculations ().addProductionOrConsumptionFromEvent
						(productionValues, conjunctionEvent, doubleTotalFromReligiousBuildings, cityOwnerPicks, db);
			}
		}
		
		// Bonuses from spells like Dark Rituals or Prosperity
		// Don't process the same spell twice - for example if two different enemy wizards both cast Famine on our city, we don't get -100% food, just the normal -50%
		final List<String> citySpellEffectApplied = new ArrayList<String> ();
		
		for (final MemoryMaintainedSpell spell : mem.getMaintainedSpell ())
			if ((spell.getCitySpellEffectID () != null) && (cityLocation.equals (spell.getCityLocation ())) &&
				(!citySpellEffectApplied.contains (spell.getCitySpellEffectID ())))
			{
				getCityCalculations ().addProductionFromSpell (productionValues, spell, doubleTotalFromReligiousBuildings, db);
				citySpellEffectApplied.add (spell.getCitySpellEffectID ());
			}
	
		// Outposts don't get bonuses from map features like Gold mines
		if ((cityData == null) || (cityData.getCityPopulation () >= CommonDatabaseConstants.MIN_CITY_POPULATION))
		{
			// See if we've got a miners' guild to boost the income from map features
			final CityProductionBreakdown mineralPercentageResult = productionValues.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER);
			final int buildingMineralPercentageBonus = (mineralPercentageResult != null) ? mineralPercentageResult.getPercentageBonus () : 0;
			final int raceMineralBonusMultipler = (cityRace != null) ? cityRace.getMineralBonusMultiplier () : 1;
	
			// Production from nearby map features
			// Have to do this after buildings, so we can have discovered if we have the miners' guild bonus to map features
			getCityCalculations ().addProductionFromMapFeatures (productionValues, mem.getMap (), cityLocation, sd.getOverlandMapSize (),
				db, raceMineralBonusMultipler, buildingMineralPercentageBonus);
		}
		
		// Halve and cap food (max city size) production first, because if calculatePotential=true then we need to know the potential max city size before
		// we can calculate the gold trade % cap.
		// Have to do this after map features are added in, since wild game increase max city size.
		getCityCalculations ().halveAddPercentageBonusAndCapProduction (cityOwnerPlayer, cityOwnerWizard, food, 0, sd.getDifficultyLevel (), db);
		
		// Gold trade % from rivers and oceans
		// Have to do this (at least the cap) after map features, since if calculatePotential=true then we need to have included wild game
		// into considering the potential maximum size this city will reach and cap the gold trade % accordingly
		final CityProductionBreakdown gold = productionValues.findOrAddProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		getCityCalculations ().calculateGoldTradeBonus (gold, mem.getMap (), cityLocation, (calculatePotential ? food.getCappedProductionAmount () : null), sd.getOverlandMapSize (), db);

		// Halve production values, using rounding defined in XML file for each production type (consumption values aren't doubled to begin with)
		for (final CityProductionBreakdown thisProduction : productionValues.getProductionType ())
			if (thisProduction != food)
				getCityCalculations ().halveAddPercentageBonusAndCapProduction (cityOwnerPlayer, cityOwnerWizard, thisProduction, food.getProductionAmountPlusPercentageBonus (),
					sd.getDifficultyLevel (), db);
		
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
	 * @param players Players list
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param taxRateID Tax rate to use for the calculation
	 * @param sd Session description
	 * @param conjunctionEventID Currently active conjunction, if there is one
	 * @param db Lookup lists built over the XML database
	 * @param overrideUnitID Return the production cost of this unit, instead of what's actually being constructed in the city; usually pass null
	 * @return Production cost of whatever is being constructed in the city at the moment; null if constructing Housing or Trade Goods
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final Integer calculateProductionCost (final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd, final String conjunctionEventID, final CommonDatabase db, final String overrideUnitID)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		Integer productionCost = null;
		
		final OverlandMapCityData cityData =  mem.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		if (cityData != null)
		{
			final String buildingID = (overrideUnitID == null) ? cityData.getCurrentlyConstructingBuildingID () : null;
			final String unitID = (overrideUnitID == null) ? cityData.getCurrentlyConstructingUnitID () : overrideUnitID;
			
			if (buildingID != null)
			{
				final Building building = db.findBuilding (buildingID, "calculateProductionCost");
				productionCost = building.getProductionCost ();
			}

			else if (unitID != null)
			{
				final UnitEx unit = db.findUnit (unitID, "calculateProductionCost");
				productionCost = unit.getProductionCost ();
				
				// Only certain units get benefit from Coal and Iron Ore
				if ((productionCost != null) && (unit.isProductionCostCanBeReduced () != null) && (unit.isProductionCostCanBeReduced ()))
				{
					// See if any Coal or Iron Ore nearby
					int unitCostReductionPercentage = getCityCalculations ().calculateSingleCityProduction
						(players, mem, cityLocation, taxRateID, sd, conjunctionEventID, true, db, CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_COST_REDUCTION);
					
					if (unitCostReductionPercentage > 0)
					{
						// Cap at 50%
						if (unitCostReductionPercentage > 50)
							unitCostReductionPercentage = 50;
						
						final int reduction = (productionCost * unitCostReductionPercentage) / 100;
						productionCost = productionCost - reduction;
					}
				}
			}
		}		
		
		return productionCost;
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

	/**
	 * @return Utils for totalling up city production
	 */
	public final CityProductionUtils getCityProductionUtils ()
	{
		return cityProductionUtils;
	}

	/**
	 * @param c Utils for totalling up city production
	 */
	public final void setCityProductionUtils (final CityProductionUtils c)
	{
		cityProductionUtils = c;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}