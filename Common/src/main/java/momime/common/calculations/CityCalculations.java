package momime.common.calculations;

import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.DifficultyLevel;
import momime.common.database.Event;
import momime.common.database.OverlandMapSize;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.OutpostDeathChanceBreakdown;
import momime.common.internal.OutpostGrowthChanceBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PlayerPick;
import momime.common.messages.servertoclient.RenderCityData;

/**
 * Common calculations pertaining to cities, e.g. calculating resources gathered from within the city radius
 */
public interface CityCalculations
{
	/**
	 * This used to be called calculateProductionBonus when it only returned an int.
	 * 
	 * @param map Known terrain
	 * @param spells Known spells
	 * @param cityLocation Location of the city to calculate for
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return % production bonus for a city located at this grid cell
	 * @throws RecordNotFoundException If we encounter a tile type that we cannot find in the cache
	 */
	public CityProductionBreakdown listCityProductionPercentageBonusesFromTerrainTiles (final MapVolumeOfMemoryGridCells map,
		final List<MemoryMaintainedSpell> spells, final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * Strategy guide p210.  Note this must generate % values even if the city produces no gold, or if there is no city at all,
	 * since the AI uses this to consider the potential value of sites it is considering building cities at. 
	 * 
	 * @param gold Gold production breakdown
	 * @param map Known terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param overridePopulation If null uses the actual city population for the gold trade % bonus cap; if filled in will override and use this value instead, in 1000s
	 * @param sys Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a tile type or race that we cannot find in the cache
	 */
	public void calculateGoldTradeBonus (final CityProductionBreakdown gold, final MapVolumeOfMemoryGridCells map, final MapCoordinates3DEx cityLocation,
		final Integer overridePopulation, final CoordinateSystem sys, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * @param map Known terrain; can use memory map, since we only ever call this for our own cities, and we can always see the terrain surrounding those
	 * @param cityLocation Location of the city to check
	 * @param building Cache for the building that we want to construct
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @return True if the surrounding terrain has one of the tile type options that we need to construct this building
	 */
	public boolean buildingPassesTileTypeRequirements (final MapVolumeOfMemoryGridCells map, final MapCoordinates3DEx cityLocation, final Building building,
		final CoordinateSystem overlandMapCoordinateSystem);

	/**
	 * Strategy guide p194.  This used to be called calculateMaxCitySize when it only returned an int.
	 * 
	 * @param map Known terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown count of the food production from each of the surrounding tiles
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 * @throws MomException If there is a problem totalling up the production values
	 */
	public CityProductionBreakdown listCityFoodProductionFromTerrainTiles (final MapVolumeOfMemoryGridCells map,
		final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Strategy guide p196, however note the example is in contradiction with the formula - from testing I believe the example is right and the formula is supposed to be a -1 not a +1
	 * Death rate is on strategy guide p197
	 *
	 * @param players Players list
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param maxCitySize Maximum city size with all buildings taken into account - i.e. the RE06 output from calculateAllCityProductions () or calculateSingleCityProduction ()
	 * @param difficultyLevel Chosen difficulty level, from session description
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown of all the values used in calculating the growth rate of this city; if the caller doesn't care about the breakdown and just wants the value, just call .getFinalTotal () on the breakdown
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a race or building that can't be found in the cache
	 */
	public CityGrowthRateBreakdown calculateCityGrowthRate (final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem,
		final MapCoordinates3DEx cityLocation, final int maxCitySize, final DifficultyLevel difficultyLevel, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException;

	/**
	 * Calculates the percentage chance of an outpost growing
	 * 
	 * @param map Known terrain
	 * @param spells Known spells
	 * @param cityLocation Location of the city to calculate for
	 * @param maxCitySize Maximum city size with all buildings taken into account - i.e. the RE06 output from calculateAllCityProductions () or calculateSingleCityProduction ()
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown of all the values used in calculating the chance of this outpost growing
	 * @throws RecordNotFoundException If we encounter a race, building or city spell effect that can't be found
	 */
	public OutpostGrowthChanceBreakdown calculateOutpostGrowthChance (final MapVolumeOfMemoryGridCells map, final List<MemoryMaintainedSpell> spells,
		final MapCoordinates3DEx cityLocation, final int maxCitySize, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Calculates the percentage chance of an outpost shrinking
	 * 
	 * @param spells Known spells
	 * @param cityLocation Location of the city to calculate for
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown of all the values used in calculating the chance of this outpost growing
	 * @throws RecordNotFoundException If we encounter a spell that can't be found
	 */
	public OutpostDeathChanceBreakdown calculateOutpostDeathChance (final List<MemoryMaintainedSpell> spells,
		final MapCoordinates3DEx cityLocation, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * Strategy guide p191
	 *
	 * After updating city population, must call the routines in this sequence:
	 * 1) City size ID & Minimum farmers
	 * 2) Rebels
	 * 3) Check not too many optional farmers
	 *
	 * For changing the tax rate (or anything else that might alter the number of rebels, e.g. units moving in/out), just need to call the last two
	 *
	 * This is called on the TrueMap to update the values there, then the calling routine checks each player's Fog of War to see
	 * if they can see the city, and if so then sends them the updated values
	 *
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param taxRateID Tax rate to use for the calculation
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown of all the values used in calculating the number of rebels this city should have; if the caller doesn't care about the breakdown and just wants the value, just call .getFinalTotal () on the breakdown
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If any of a number of items cannot be found in the cache
	 */
	public CityUnrestBreakdown calculateCityRebels (final FogOfWarMemory mem, final MapCoordinates3DEx cityLocation, final String taxRateID, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException;

	/**
	 * Works out how much gold the civilians in a city generate according to the tax rate chosen.
	 * 
	 * @param cityData City to generate taxes for
	 * @param taxRateID Chosen tax rate
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown detailing gold from taxes, or null if not generating any
	 * @throws RecordNotFoundException If we can't find the tax rate or production type in the DB
	 * @throws MomException If there is a problem totalling up the production values
	 */
	public CityProductionBreakdown addGoldFromTaxes (final OverlandMapCityData cityData, final String taxRateID, final CommonDatabase db)
		throws RecordNotFoundException, MomException;

	/**
	 * Works out how many rations the civilians in a city are eating.
	 * 
	 * @param cityData City to generate consumption for
	 * @return Breakdown detailing rations eaten, or null if not generating any
	 */
	public CityProductionBreakdown addRationsEatenByPopulation (final OverlandMapCityData cityData);
		
	/**
	 * Adds all the productions from a certain number of a particular type of civilian.  This is the amount of rations+production+magic power
	 * each civilian generates, depending on whether they're a farmer/worker/rebel.  Civilians generating gold (taxes) are handled separately,
	 * as are civilians eating rations.
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param race Race of civilian
	 * @param populationTaskID Task civilian is performing (farmer, worker or rebel)
	 * @param numberDoingTask Number of civilians doing this task
	 * @param cityLocation Location of the city where the civilians are
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 * @throws MomException If there is a problem totalling up the production values
	 */
	public void addProductionFromPopulation (final CityProductionBreakdownsEx productionValues, final Race race, final String populationTaskID,
		final int numberDoingTask, final MapCoordinates3DEx cityLocation, final List<MemoryBuilding> buildings, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Adds on production generated by our fortress according to the number of picks of a particular type we chose at the start of the game
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param pickType Type of picks (spell books or retorts)
	 * @param pickTypeCount The number of the pick we had at the start of the game
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the productionTypeID can't be found in the database
	 * @throws MomException If there is a problem totalling up the production values
	 */
	public void addProductionFromFortressPickType (final CityProductionBreakdownsEx productionValues,
		final PickType pickType, final int pickTypeCount, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Adds on production generated by our fortress being on a particular plane
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param plane Which plane our fortress is on
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the productionTypeID can't be found in the database
	 * @throws MomException If there is a problem totalling up the production values
	 */
	public void addProductionFromFortressPlane (final CityProductionBreakdownsEx productionValues, final Plane plane, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Adds all the productions and/or consumptions generated by a particular building
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param buildingDef The building to calculate for
	 * @param religiousBuildingsNegatedBySpellID Set to spellID of Evil Presence if it is cast on this city and we don't have any death books, otherwise null
	 * @param picks The list of spell picks belonging to the player who owns the city that this building is in
	 * @param db Lookup lists built over the XML database
	 * @return Production (magic power) from religious buildings
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public int addProductionAndConsumptionFromBuilding (final CityProductionBreakdownsEx productionValues,
		final Building buildingDef, final String religiousBuildingsNegatedBySpellID, final List<PlayerPick> picks, final CommonDatabase db)
		throws MomException, RecordNotFoundException;
	
	/**
	 * Adds all the productions generated by a particular spell / city spell effect
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param spell The spell to calculate for
	 * @param doubleTotalFromReligiousBuildings Double magic power value generated from religious buildings, including bonuses from Divine/Infernal Power
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the definition for the spell can't be found in the db
	 * @throws MomException If there is a problem totalling up the production values
	 */
	public void addProductionFromSpell (final CityProductionBreakdownsEx productionValues, final MemoryMaintainedSpell spell,
		final int doubleTotalFromReligiousBuildings, final CommonDatabase db)
		throws RecordNotFoundException, MomException;

	/**
	 * Adds the production/consumption from good/bad moons
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param event Good/bad moon random event
	 * @param doubleTotalFromReligiousBuildings Double magic power value generated from religious buildings, including bonuses from Divine/Infernal Power
	 * @param picks The list of spell picks belonging to the player who owns the city that this building is in
	 * @param db Lookup lists built over the XML database
	 * @return Updated doubleTotalFromReligiousBuildings value
	 * @throws RecordNotFoundException If the definition for the spell can't be found in the db
	 * @throws MomException If there is a problem totalling up the production values
	 */
	public int addProductionOrConsumptionFromEvent (final CityProductionBreakdownsEx productionValues, final Event event,
		final int doubleTotalFromReligiousBuildings, final List<PlayerPick> picks, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * Adds on production generated from a particular map feature
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param map Known terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @param raceMineralBonusMultipler The amount our race multiplies mineral bonuses by (2 for Dwarves, 1 for everyone else)
	 * @param buildingMineralPercentageBonus The % bonus buildings give to mineral bonuses (50% if we have a Miners' Guild)
	 * @throws RecordNotFoundException If we encounter a map feature that we cannot find in the cache
	 * @throws MomException If there is a problem totalling up the production values
	 */
	public void addProductionFromMapFeatures (final CityProductionBreakdownsEx productionValues, final MapVolumeOfMemoryGridCells map,
		final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db,
		final int raceMineralBonusMultipler, final int buildingMineralPercentageBonus)
		throws RecordNotFoundException, MomException;
	
	/**
	 * After doubleProductionAmount has been filled in, this deals with halving the value according to the rounding
	 * rules for the production type, adding on any % bonus, and dealing with any overall cap.
	 * 
	 * @param cityOwnerPlayer Player who owns the city, which may be null if we're just evaluating a potential location for a new city
	 * @param cityOwnerWizard Wizard who owns the city, which may be null if we're just evaluating a potential location for a new city
	 * @param thisProduction Production value to halve, add % bonus and cap
	 * @param foodProductionFromTerrainTiles Amount of food (max city size) production collected up from terrain tiles around the city
	 * @param difficultyLevel Difficulty level settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a production type that can't be found in the DB
	 * @throws MomException If we encounter a production value that the DB states should always be an exact multiple of 2, but isn't
	 */
	public void halveAddPercentageBonusAndCapProduction (final PlayerPublicDetails cityOwnerPlayer, final KnownWizardDetails cityOwnerWizard,
		final CityProductionBreakdown thisProduction, final int foodProductionFromTerrainTiles, final DifficultyLevel difficultyLevel, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * @param players Players list
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param taxRateID Tax rate to use for the calculation
	 * @param sd Session description
	 * @param conjunctionEventID Currently active conjunction, if there is one
	 * @param includeProductionAndConsumptionFromPopulation Normally true; if false, production and consumption from civilian population will be excluded
	 * @param db Lookup lists built over the XML database
	 * @param productionTypeID The type of production we want the value for
	 * @return Production value - consumption value for the specified production type
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	public int calculateSingleCityProduction (final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd, final String conjunctionEventID,
		final boolean includeProductionAndConsumptionFromPopulation, final CommonDatabase db, final String productionTypeID)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;

	/**
	 * Blanks the building sold this turn value in every map cell
	 * @param map Should either be the true map or the memory map for the player requested, to ensure that they can see their own cities
	 * @param onlyOnePlayerID If zero, will blank values in cities belonging to all players; if specified will blank values in cities belonging only to the specified player
	 */
	public void blankBuildingsSoldThisTurn (final MapVolumeOfMemoryGridCells map, final int onlyOnePlayerID);

	/**
	 * Client uses this for the surveryor and whether settlers have the build button enabled - but note, this only works based
	 * on what we know of the map - its possible we may think we can place a city at a location but actually cannot
	 *
	 * Server uses this during game startup to position all starter cities - for this it runs over the true map, because no
	 * players have any knowledge of the map yet
	 *
	 * @param map1 First set of data about the overland terrain map
	 * @param map2 Second set of data about the overland terrain map (optional)
	 * @param plane Which plane we want to place a city on
	 * @param overlandMapSize Overland map coordinate system and extended details
	 * @return Map area with areas we know are too close to cities marked
	 */
	public MapArea2D<Boolean> markWithinExistingCityRadius (final MapVolumeOfMemoryGridCells map1, final MapVolumeOfMemoryGridCells map2,
		final int plane, final OverlandMapSize overlandMapSize);

	/**
	 * @param totalCost Total production cost of the building/unit
	 * @param builtSoFar Amount of production we've put towards it so far
	 * @return Gold to rush buy a particular construction project
	 */
	public int goldToRushBuy (final int totalCost, final int builtSoFar);

	/**
	 * @param cityLocation City location
	 * @param map Known terrain
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @return List of all units that the player can choose between to construct at the city
	 */
	public List<UnitEx> listUnitsCityCanConstruct (final MapCoordinates3DEx cityLocation,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings, final CommonDatabase db);
	
	/**
	 * Collects together all the data necessary to render CityViewPanel, so we must have sufficient into to be able to look at all the
	 * CityViewElement records in the graphics XML and determine whether to draw each one.
	 *  
	 * @param cityLocation City location
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param mem Player's knowledge about the city and surrounding terrain
	 * @return Data necessary to render CityViewPanel
	 */
	public RenderCityData buildRenderCityData (final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final FogOfWarMemory mem);
}