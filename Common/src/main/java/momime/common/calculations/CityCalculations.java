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
import momime.common.database.OverlandMapSize;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
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
	 * @param cityLocation Location of the city to calculate for
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return % production bonus for a city located at this grid cell
	 * @throws RecordNotFoundException If we encounter a tile type that we cannot find in the cache
	 */
	public CityProductionBreakdown listCityProductionPercentageBonusesFromTerrainTiles (final MapVolumeOfMemoryGridCells map,
		final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
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
	 */
	public CityProductionBreakdown listCityFoodProductionFromTerrainTiles (final MapVolumeOfMemoryGridCells map,
		final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * Strategy guide p196, however note the example is in contradiction with the formula - from testing I believe the example is right and the formula is supposed to be a -1 not a +1
	 * Death rate is on strategy guide p197
	 *
	 * @param players Players list
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @param spells Known spells
	 * @param cityLocation Location of the city to calculate for
	 * @param maxCitySize Maximum city size with all buildings taken into account - i.e. the RE06 output from calculateAllCityProductions () or calculateSingleCityProduction ()
	 * @param difficultyLevel Chosen difficulty level, from session description
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown of all the values used in calculating the growth rate of this city; if the caller doesn't care about the breakdown and just wants the value, just call .getFinalTotal () on the breakdown
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a race or building that can't be found in the cache
	 */
	public CityGrowthRateBreakdown calculateCityGrowthRate (final List<? extends PlayerPublicDetails> players, final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings, final List<MemoryMaintainedSpell> spells, final MapCoordinates3DEx cityLocation,
		final int maxCitySize, final DifficultyLevel difficultyLevel, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException;

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
	 * @param players Players list
	 * @param map Known terrain
	 * @param units Known units
	 * @param buildings Known buildings
	 * @param spells Known spells
	 * @param cityLocation Location of the city to calculate for
	 * @param taxRateID Tax rate to use for the calculation
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown of all the values used in calculating the number of rebels this city should have; if the caller doesn't care about the breakdown and just wants the value, just call .getFinalTotal () on the breakdown
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If any of a number of items cannot be found in the cache
	 */
	public CityUnrestBreakdown calculateCityRebels (final List<? extends PlayerPublicDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units, final List<MemoryBuilding> buildings, final List<MemoryMaintainedSpell> spells,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException;

	/**
	 * @param players Pre-locked players list
	 * @param map Known terrain
	 * @param buildings List of known buildings
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
	public CityProductionBreakdownsEx calculateAllCityProductions (final List<? extends PlayerPublicDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd, final boolean includeProductionAndConsumptionFromPopulation,
		final boolean calculatePotential, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException;

	/**
	 * @param players Pre-locked players list
	 * @param map Known terrain
	 * @param buildings List of known buildings
	 * @param cityLocation Location of the city to calculate for
	 * @param taxRateID Tax rate to use for the calculation
	 * @param sd Session description
	 * @param includeProductionAndConsumptionFromPopulation Normally true; if false, production and consumption from civilian population will be excluded
	 * @param db Lookup lists built over the XML database
	 * @param productionTypeID The type of production we want the value for
	 * @return Production value - consumption value for the specified production type
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	public int calculateSingleCityProduction (final List<? extends PlayerPublicDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd,
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