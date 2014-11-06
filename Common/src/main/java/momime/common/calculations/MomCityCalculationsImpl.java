package momime.common.calculations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.MapSizeData;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.BuildingRequiresTileType;
import momime.common.database.FortressPickTypeProduction;
import momime.common.database.FortressPlaneProduction;
import momime.common.database.MapFeature;
import momime.common.database.MapFeatureProduction;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.ProductionType;
import momime.common.database.Race;
import momime.common.database.RacePopulationTask;
import momime.common.database.RacePopulationTaskProduction;
import momime.common.database.RaceUnrest;
import momime.common.database.TaxRate;
import momime.common.database.TileType;
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
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.operations.BooleanMapAreaOperations2DImpl;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.areas.storage.MapArea2DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Common calculations pertaining to cities, e.g. calculating resources gathered from within the city radius
 */
public final class MomCityCalculationsImpl implements MomCityCalculations
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MomCityCalculationsImpl.class);
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * A list of directions for traversing from a city's coordinates through all the map cells within that city's radius
	 * Note this is different from the Delphi list TRACE_CITY_DIRECTION is in MomMap.pas in that the list here DOES include the tile the city itself is on, the Delphi code (unnecessarily) deals with the centre tile separately
	 * We do not end up back where we started (in either version)
	 */
	public static final SquareMapDirection [] DIRECTIONS_TO_TRAVERSE_CITY_RADIUS = {

		// Go around the 1st ring around the city (radius = 1)
		SquareMapDirection.NORTHWEST, SquareMapDirection.EAST, SquareMapDirection.EAST, SquareMapDirection.SOUTH, SquareMapDirection.SOUTH, SquareMapDirection.WEST, SquareMapDirection.WEST,

		// Jump back to the centre tile before leaving the 1st ring
		SquareMapDirection.NORTHEAST, SquareMapDirection.WEST,

		// Then do the 2nd ring
		SquareMapDirection.WEST, SquareMapDirection.NORTH, SquareMapDirection.NORTHEAST, SquareMapDirection.EAST, SquareMapDirection.EAST, SquareMapDirection.SOUTHEAST,
		SquareMapDirection.SOUTH, SquareMapDirection.SOUTH, SquareMapDirection.SOUTHWEST, SquareMapDirection.WEST, SquareMapDirection.WEST, SquareMapDirection.NORTHWEST};

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
	final CityProductionBreakdown listCityProductionPercentageBonusesFromTerrainTiles (final MapVolumeOfMemoryGridCells map,
		final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering listCityProductionPercentageBonusesFromTerrainTiles: " + cityLocation);

		// First pass - get a list of how many of each tile type are within the city radius
		final Map<String, CityProductionBreakdownTileType> tileTypes = new HashMap<String, CityProductionBreakdownTileType> ();

		final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
		for (final SquareMapDirection direction : DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ()))
			{
				final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				if ((terrainData != null) && (terrainData.getTileTypeID () != null))
				{
					// Is this tile type already listed in the map?
					final CityProductionBreakdownTileType thisTileType = tileTypes.get (terrainData.getTileTypeID ());
					if (thisTileType == null)
					{
						// New tile type
						final CityProductionBreakdownTileType newTileType = new CityProductionBreakdownTileType ();
						newTileType.setTileTypeID (terrainData.getTileTypeID ());
						newTileType.setCount (1);
						tileTypes.put (terrainData.getTileTypeID (), newTileType);
					}
					else
					{
						// Just add to existing counter
						thisTileType.setCount (thisTileType.getCount () + 1);
					}
				}
			}
		
		// Second pass - now check which of those tile types actually produce any production % bonuses
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION);
		
		for (final CityProductionBreakdownTileType thisTileType : tileTypes.values ())
		{
			final Integer percentageBonus = db.findTileType (thisTileType.getTileTypeID (), "listCityProductionPercentageBonusesFromTerrainTiles").getProductionBonus ();
			if (percentageBonus != null)
			{
				thisTileType.setPercentageBonusEachTile (percentageBonus);
				thisTileType.setPercentageBonusAllTiles (percentageBonus * thisTileType.getCount ());
				
				breakdown.getTileTypeProduction ().add (thisTileType);
				breakdown.setPercentageBonus (breakdown.getPercentageBonus () + thisTileType.getPercentageBonusAllTiles ());
			}
		}

		log.trace ("Exiting listCityProductionPercentageBonusesFromTerrainTiles = " + breakdown.getPercentageBonus ());
		return breakdown;
	}

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
	final void calculateGoldTradeBonus (final CityProductionBreakdown gold, final MapVolumeOfMemoryGridCells map, final MapCoordinates3DEx cityLocation,
		final Integer overridePopulation, final CoordinateSystem sys, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering calculateGoldTradeBonus: " + cityLocation);

		// Deal with centre square
		gold.setTradePercentageBonusFromTileType (0);
		final MemoryGridCell centreTile = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final OverlandMapTerrainData centreTerrain = centreTile.getTerrainData (); 
		if ((centreTerrain != null) && (centreTerrain.getTileTypeID () != null))
		{
			final Integer centreBonus = db.findTileType (centreTerrain.getTileTypeID (), "calculateGoldTradeBonus").getGoldBonus ();
			if (centreBonus != null)
				gold.setTradePercentageBonusFromTileType (centreBonus);
		}

		// Only check adjacent squares if we didn't find a centre square bonus
		int d = 1;
		while ((gold.getTradePercentageBonusFromTileType () == 0) && (d <= getCoordinateSystemUtils ().getMaxDirection (sys.getCoordinateSystemType ())))
		{
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			if (getCoordinateSystemUtils ().move3DCoordinates (sys, coords, d))
			{
				// Bonus only applies if adjacent flag is set
				final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				if ((terrainData != null) && (terrainData.getTileTypeID () != null))
				{
					final TileType tileType = db.findTileType (terrainData.getTileTypeID (), "calculateGoldTradeBonus");
					if ((tileType.isGoldBonusSurroundingTiles () != null) && (tileType.isGoldBonusSurroundingTiles ()) && (tileType.getGoldBonus () != null))
						gold.setTradePercentageBonusFromTileType (tileType.getGoldBonus ());
				}
			}

			d++;
		}
		
		// Not yet implemented
		gold.setTradePercentageBonusFromRoads (0);
		
		// Nomad's bonus
		final OverlandMapCityData cityData = centreTile.getCityData ();
		final String raceID = (cityData != null) ? cityData.getCityRaceID () : null;
		final Race race = (raceID != null) ? db.findRace (raceID, "calculateGoldTradeBonus") : null;
		final Integer raceBonus = (race != null) ? race.getGoldTradeBonus () : null;
		gold.setTradePercentageBonusFromRace ((raceBonus != null) ? raceBonus : 0);
		
		// All the 3 parts together
		gold.setTradePercentageBonusUncapped (gold.getTradePercentageBonusFromTileType () +
			gold.getTradePercentageBonusFromRoads () + gold.getTradePercentageBonusFromRace ());
		
		// Deal with cap
		if (overridePopulation != null)
			gold.setTotalPopulation (overridePopulation);
		else
		{
			final Integer cityPopulation = (cityData != null) ? cityData.getCityPopulation () : null;
			gold.setTotalPopulation ((cityPopulation != null) ? cityPopulation / 1000 : 0);
		}
		
		final int maxGoldTradeBonus = gold.getTotalPopulation () * 3;
		gold.setTradePercentageBonusCapped (Math.min (gold.getTradePercentageBonusUncapped (), maxGoldTradeBonus));
		
		// Add to other bonuses
		gold.setPercentageBonus (gold.getPercentageBonus () + gold.getTradePercentageBonusCapped ());
		
		log.trace ("Exiting calculateGoldTradeBonus");
	}

	/**
	 * @param map Known terrain; can use memory map, since we only ever call this for our own cities, and we can always see the terrain surrounding those
	 * @param cityLocation Location of the city to check
	 * @param building Cache for the building that we want to construct
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @return True if the surrounding terrain has one of the tile type options that we need to construct this building
	 */
	@Override
	public final boolean buildingPassesTileTypeRequirements (final MapVolumeOfMemoryGridCells map, final MapCoordinates3DEx cityLocation, final Building building,
		final CoordinateSystem overlandMapCoordinateSystem)
	{
		log.trace ("Entering buildingPassesTileTypeRequirements: " + cityLocation + ", " + building.getBuildingID ());

		// If there are no requirements then we're automatically fine
		final boolean passes;
		if (building.getBuildingRequiresTileType ().size () == 0)
			passes = true;
		else
		{
			// Search for tile type requirements - these are or'd together, so as soon as find any tile that matches any requirement, we're good
			boolean anyRequirementPassed = false;
			final Iterator<BuildingRequiresTileType> iter = building.getBuildingRequiresTileType ().iterator ();

			while ((!anyRequirementPassed) && (iter.hasNext ()))
			{
				final BuildingRequiresTileType thisRequirement = iter.next ();

				// Do we have any of this tile type?
				boolean thisRequirementPasses = false;

				// First check the centre square, we can check this regardless of the defined distance
				// Put requirement first, just in case the area of terrain is unknown to us
				final OverlandMapTerrainData centreTile = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getTerrainData ();
				if ((centreTile != null) && (thisRequirement.getTileTypeID ().equals (centreTile.getTileTypeID ())))
					thisRequirementPasses = true;
				else
				{
					// Depends on distance
					if (thisRequirement.getDistance () == 1)
					{
						// Fan out in all 8 directions from the centre
						int d = 1;
						while ((!thisRequirementPasses) && (d <= getCoordinateSystemUtils ().getMaxDirection (overlandMapCoordinateSystem.getCoordinateSystemType ()))) 
						{
							final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
							if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, d))
							{
								final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
								if ((terrainData != null) && (thisRequirement.getTileTypeID ().equals (terrainData.getTileTypeID ())))
									thisRequirementPasses = true;
							}

							d++;
						}
					}
					else
					{
						// Trace over all 21 city squares
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
						int directionIndex = 0;
						while ((!thisRequirementPasses) && (directionIndex < DIRECTIONS_TO_TRAVERSE_CITY_RADIUS.length))
						{
							if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, DIRECTIONS_TO_TRAVERSE_CITY_RADIUS [directionIndex].getDirectionID ()))
							{
								final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
								if ((terrainData != null) && (thisRequirement.getTileTypeID ().equals (terrainData.getTileTypeID ())))
									thisRequirementPasses = true;
							}

							directionIndex++;
						}
					}
				}

				if (thisRequirementPasses)
					anyRequirementPassed = true;
			}

			passes = anyRequirementPassed;
		}

		log.trace ("Exiting buildingPassesTileTypeRequirements = " + passes);
		return passes;
	}

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
	final CityProductionBreakdown listCityFoodProductionFromTerrainTiles (final MapVolumeOfMemoryGridCells map,
		final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering listCityFoodProductionFromTerrainTiles: " + cityLocation);

		// First pass - get a list of how many of each tile type are within the city radius
		final Map<String, CityProductionBreakdownTileType> tileTypes = new HashMap<String, CityProductionBreakdownTileType> ();

		final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
		for (final SquareMapDirection direction : DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ()))
			{
				final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				if ((terrainData != null) && (terrainData.getTileTypeID () != null))
				{
					// Is this tile type already listed in the map?
					final CityProductionBreakdownTileType thisTileType = tileTypes.get (terrainData.getTileTypeID ());
					if (thisTileType == null)
					{
						// New tile type
						final CityProductionBreakdownTileType newTileType = new CityProductionBreakdownTileType ();
						newTileType.setTileTypeID (terrainData.getTileTypeID ());
						newTileType.setCount (1);
						tileTypes.put (terrainData.getTileTypeID (), newTileType);
					}
					else
					{
						// Just add to existing counter
						thisTileType.setCount (thisTileType.getCount () + 1);
					}
				}
			}
		
		// Second pass - now check which of those tile types actually produce any food
		final CityProductionBreakdown breakdown = new CityProductionBreakdown ();
		breakdown.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
		
		for (final CityProductionBreakdownTileType thisTileType : tileTypes.values ())
		{
			final Integer doubleFoodFromTileType = db.findTileType (thisTileType.getTileTypeID (), "listCityFoodProductionFromTerrainTiles").getDoubleFood ();
			if (doubleFoodFromTileType != null)
			{
				thisTileType.setDoubleProductionAmountEachTile (doubleFoodFromTileType);
				thisTileType.setDoubleProductionAmountAllTiles (doubleFoodFromTileType * thisTileType.getCount ());
				
				breakdown.getTileTypeProduction ().add (thisTileType);
				breakdown.setDoubleProductionAmount (breakdown.getDoubleProductionAmount () + thisTileType.getDoubleProductionAmountAllTiles ());
			}
		}

		log.trace ("Exiting listCityFoodProductionFromTerrainTiles = " + breakdown.getDoubleProductionAmount ());
		return breakdown;
	}

	/**
	 * Strategy guide p196, however note the example is in contradiction with the formula - from testing I believe the example is right and the formula is supposed to be a -1 not a +1
	 * Death rate is on strategy guide p197
	 *
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @param cityLocation Location of the city to calculate for
	 * @param maxCitySize Maximum city size with all buildings taken into account - i.e. the RE06 output from calculateAllCityProductions () or calculateSingleCityProduction ()
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown of all the values used in calculating the growth rate of this city; if the caller doesn't care about the breakdown and just wants the value, just call .getFinalTotal () on the breakdown
	 * @throws RecordNotFoundException If we encounter a race or building that can't be found in the cache
	 */
	@Override
	public final CityGrowthRateBreakdown calculateCityGrowthRate (final MapVolumeOfMemoryGridCells map,
		final List<MemoryBuilding> buildings, final MapCoordinates3DEx cityLocation, final int maxCitySize, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering calculateCityGrowthRate: " + cityLocation);

		final OverlandMapCityData cityData = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		// Start off calculation
		final int currentPopulation = cityData.getCityPopulation ();
		final int maximumPopulation = maxCitySize * 1000;

		// Work out the direction the population is changing in
		final int spaceLeft = maximumPopulation - currentPopulation;
		final CityGrowthRateBreakdown breakdown;

		if (spaceLeft > 0)
		{
			// Growing
			final CityGrowthRateBreakdownGrowing growing = new CityGrowthRateBreakdownGrowing ();

			growing.setBaseGrowthRate (((maxCitySize - (currentPopulation / 1000)) / 2) * 10);	// +0 = -1 from the formula, +1 to make rounding go up
			final Integer racialGrowthModifierInteger = db.findRace (cityData.getCityRaceID (), "calculateCityGrowthRate").getGrowthRateModifier ();
			growing.setRacialGrowthModifier ((racialGrowthModifierInteger == null) ? 0 : racialGrowthModifierInteger);

			growing.setTotalGrowthRate (growing.getBaseGrowthRate () + growing.getRacialGrowthModifier ());

			// Bonuses from buildings
			for (final MemoryBuilding thisBuilding : buildings)
				if (thisBuilding.getCityLocation ().equals (cityLocation))
				{
					final Integer buildingGrowthRateBonus = db.findBuilding (thisBuilding.getBuildingID (), "calculateCityGrowthRate").getGrowthRateBonus ();
					if (buildingGrowthRateBonus != null)
					{
						growing.setTotalGrowthRate (growing.getTotalGrowthRate () + buildingGrowthRateBonus);
						
						final CityGrowthRateBreakdownBuilding breakdownBuilding = new CityGrowthRateBreakdownBuilding ();
						breakdownBuilding.setBuildingID (thisBuilding.getBuildingID ());
						breakdownBuilding.setGrowthRateBonus (buildingGrowthRateBonus);
						growing.getBuildingModifier ().add (breakdownBuilding);
					}
				}

			// Don't allow maximum to go over maximum population
			if (growing.getTotalGrowthRate () > spaceLeft)
				growing.setCappedGrowthRate (spaceLeft);
			else
				growing.setCappedGrowthRate (growing.getTotalGrowthRate ());

			growing.setFinalTotal (growing.getCappedGrowthRate ());
			breakdown = growing;
		}
		else if (spaceLeft < 0)
		{
			// Dying
			final CityGrowthRateBreakdownDying dying = new CityGrowthRateBreakdownDying ();

			// Calculate how many population units we're over
			dying.setBaseDeathRate ((currentPopulation / 1000) - maxCitySize);
			dying.setCityDeathRate (dying.getBaseDeathRate () * 50);
			dying.setFinalTotal (-dying.getCityDeathRate ());

			breakdown = dying;
		}
		else
		{
			// At maximum
			breakdown = new CityGrowthRateBreakdown ();
		}

		// Set common values
		breakdown.setCurrentPopulation (currentPopulation);
		breakdown.setMaximumPopulation (maximumPopulation);

		log.trace ("Exiting calculateCityGrowthRate = " + breakdown.getFinalTotal ());
		return breakdown;
	}

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
	 * @param cityLocation Location of the city to calculate for
	 * @param taxRateID Tax rate to use for the calculation
	 * @param db Lookup lists built over the XML database
	 * @return Breakdown of all the values used in calculating the number of rebels this city should have; if the caller doesn't care about the breakdown and just wants the value, just call .getFinalTotal () on the breakdown
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If any of a number of items cannot be found in the cache
	 */
	@Override
	public final CityUnrestBreakdown calculateCityRebels (final List<? extends PlayerPublicDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units, final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException
	{
		log.trace ("Entering calculateCityRebels: " + cityLocation);

		final MemoryGridCell mc = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final OverlandMapCityData cityData = mc.getCityData ();

		// Create breakdown object to write all the values into
		final CityUnrestBreakdown breakdown = new CityUnrestBreakdown ();
		
		// First get the tax rate
		breakdown.setTaxPercentage (db.findTaxRate (taxRateID, "calculateCityRebels").getTaxUnrestPercentage ());

		// Add on racial unrest percentage
		// To do this, need to find the player's capital race, i.e. the race inhabiting the city where their fortress is
		final MemoryBuilding fortressLocation = getMemoryBuildingUtils ().findCityWithBuilding
			(cityData.getCityOwnerID (), CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, map, buildings);

		if (fortressLocation != null)
		{
			// Find the capital race's unrest value listed under this city's race
			final OverlandMapCityData fortressCityData = map.getPlane ().get (fortressLocation.getCityLocation ().getZ ()).getRow ().get
				(fortressLocation.getCityLocation ().getY ()).getCell ().get (fortressLocation.getCityLocation ().getX ()).getCityData ();

			RaceUnrest raceUnrest = null;
			final Iterator<RaceUnrest> iter = db.findRace (cityData.getCityRaceID (), "calculateCityRebels").getRaceUnrest ().iterator ();
			while ((raceUnrest == null) & (iter.hasNext ()))
			{
				final RaceUnrest thisRaceUnrest = iter.next ();
				if (thisRaceUnrest.getCapitalRaceID ().equals (fortressCityData.getCityRaceID ()))
					raceUnrest = thisRaceUnrest;
			}

			// Fine if the capital race is not listed, this just means there's no unrest
			if (raceUnrest != null)
			{				
				breakdown.setRacialPercentage ((raceUnrest.getUnrestPercentage () == null) ? 0 : raceUnrest.getUnrestPercentage ());
				breakdown.setRacialLiteral ((raceUnrest.getUnrestLiteral () == null) ? 0 : raceUnrest.getUnrestLiteral ());
			}
		}

		// Do calculation, rounding down
		breakdown.setTotalPercentage (breakdown.getTaxPercentage () + breakdown.getRacialPercentage ());
		breakdown.setPopulation (cityData.getCityPopulation () / 1000);
		breakdown.setBaseValue ((breakdown.getPopulation () * breakdown.getTotalPercentage ()) / 100);

		// Count up religious buildings and non-religious buildings separately
		// This is because Divine & Infernal power improve the pacifying effects of religious unrest reduction,
		// but they do not improve the unrest reduction of the Animists' Guild or the Oracle
		int religiousUnrestReduction = 0;
		int nonReligiousUnrestReduction = 0;
		for (final MemoryBuilding thisBuilding : buildings)

			// Make sure its in the right location, and don't count buildings being sold this turn
			if ((thisBuilding.getCityLocation ().equals (cityLocation)) && (!thisBuilding.getBuildingID ().equals (mc.getBuildingIdSoldThisTurn ())))
			{
				final Building building = db.findBuilding (thisBuilding.getBuildingID (), "calculateCityRebels");
				if (building.getBuildingUnrestReduction () != null)
				{
					// Add to which total
					if ((building.isBuildingUnrestReductionImprovedByRetorts () != null) && (building.isBuildingUnrestReductionImprovedByRetorts ()))
						religiousUnrestReduction = religiousUnrestReduction + building.getBuildingUnrestReduction ();
					else
						nonReligiousUnrestReduction = nonReligiousUnrestReduction + building.getBuildingUnrestReduction ();

					// List building in breakdown
					final CityUnrestBreakdownBuilding buildingBreakdown = new CityUnrestBreakdownBuilding ();
					buildingBreakdown.setBuildingID (thisBuilding.getBuildingID ());
					buildingBreakdown.setUnrestReduction (building.getBuildingUnrestReduction ());
					breakdown.getBuildingReducingUnrest ().add (buildingBreakdown);
				}
			}

		// Bump up effect of religious buildings if we have Divine or Infernal Power, rounding down
		if (religiousUnrestReduction > 0)
		{
			// Find the picks of the player who owns this city
			final PlayerPublicDetails cityOwner = getMultiplayerSessionUtils ().findPlayerWithID (players, cityData.getCityOwnerID (), "calculateCityRebels");
			final List<PlayerPick> cityOwnerPicks = ((MomPersistentPlayerPublicKnowledge) cityOwner.getPersistentPlayerPublicKnowledge ()).getPick ();

			breakdown.setReligiousBuildingRetortPercentage (getPlayerPickUtils ().totalReligiousBuildingBonus (cityOwnerPicks, db));
			breakdown.getPickIdContributingToReligiousBuildingBonus ().addAll (getPlayerPickUtils ().pickIdsContributingToReligiousBuildingBonus (cityOwnerPicks, db));

			if (breakdown.getReligiousBuildingRetortPercentage () > 0)
			{
				breakdown.setReligiousBuildingReduction (-religiousUnrestReduction);
				breakdown.setReligiousBuildingRetortValue (-((religiousUnrestReduction * breakdown.getReligiousBuildingRetortPercentage ()) / 100));

				// Actually apply the bonus
				religiousUnrestReduction = religiousUnrestReduction - breakdown.getReligiousBuildingRetortValue ();		// Subtract a -ve, i.e. add to the unrest reduction
			}
		}

		// Subtract pacifying effects of non-summoned units
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (cityLocation.equals (thisUnit.getUnitLocation ())))
			{
				final String unitMagicRealmID = db.findUnit (thisUnit.getUnitID (), "calculateCityRebels").getUnitMagicRealm ();
				if (!db.findUnitMagicRealm (unitMagicRealmID, "calculateCityRebels").getUnitTypeID ().equals (CommonDatabaseConstants.VALUE_UNIT_TYPE_ID_SUMMONED))
					breakdown.setUnitCount (breakdown.getUnitCount () + 1);
			}

		breakdown.setUnitReduction (-(breakdown.getUnitCount () / 2));

		// Total unrest, before applying bounding limits
		breakdown.setBaseTotal (breakdown.getBaseValue () + breakdown.getRacialLiteral () - religiousUnrestReduction - nonReligiousUnrestReduction + breakdown.getUnitReduction ());
		final int boundedTotal;

		if (breakdown.getBaseTotal () < 0)
		{
			boundedTotal = 0;
			breakdown.setForcePositive (true);
		}
		else if (breakdown.getBaseTotal () > breakdown.getPopulation ())
		{
			boundedTotal = breakdown.getPopulation ();
			breakdown.setForceAll (true);
		}
		else
		{
			boundedTotal = breakdown.getBaseTotal ();
		}

		// Rebels can never force the population to starve
		if (cityData.getMinimumFarmers () + boundedTotal > breakdown.getPopulation ())
		{
			breakdown.setMinimumFarmers (cityData.getMinimumFarmers ());
			breakdown.setTotalAfterFarmers (breakdown.getPopulation () - breakdown.getMinimumFarmers ());
			breakdown.setFinalTotal (breakdown.getTotalAfterFarmers ());
		}
		else
		{
			breakdown.setFinalTotal (boundedTotal);
		}

		log.trace ("Entering calculateCityRebels: " + breakdown.getFinalTotal ());
		return breakdown;
	}

	/**
	 * Adds all the productions from a certain number of a particular type of civilian
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param race Race of civilian
	 * @param populationTaskID Task civilian is performing (farmer, worker or rebel)
	 * @param numberDoingTask Number of civilians doing this task
	 * @param cityLocation Location of the city where the civilians are
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	final void addProductionFromPopulation (final CityProductionBreakdownsEx productionValues, final Race race, final String populationTaskID,
		final int numberDoingTask, final MapCoordinates3DEx cityLocation, final List<MemoryBuilding> buildings, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering addProductionFromPopulation: " + race.getRaceID () + ", " + populationTaskID + " x" + numberDoingTask + ", " + cityLocation);
		
		if (numberDoingTask > 0)

			// Find definition for this population task (farmer, worker, rebel) for the city's race
			// It may genuinely not be there - rebels of most races produce nothing so won't even be listed
			for (final RacePopulationTask populationTask : race.getRacePopulationTask ())
				if (populationTask.getPopulationTaskID ().equals (populationTaskID))
					for (final RacePopulationTaskProduction thisProduction : populationTask.getRacePopulationTaskProduction ())
					{
						// Are there are building we have which increase this type of production from this type of population
						// i.e. Animists' guild increasing farmers yield by +1
						final int doubleAmountPerPerson = thisProduction.getDoubleAmount () +
							getMemoryBuildingUtils ().totalBonusProductionPerPersonFromBuildings
								(buildings, cityLocation, populationTaskID, thisProduction.getProductionTypeID (), db);

						// Now add it
						final CityProductionBreakdownPopulationTask taskBreakdown = new CityProductionBreakdownPopulationTask ();
						taskBreakdown.setPopulationTaskID (populationTaskID);
						taskBreakdown.setCount (numberDoingTask);
						taskBreakdown.setDoubleProductionAmountEachPopulation (doubleAmountPerPerson);
						taskBreakdown.setDoubleProductionAmountAllPopulation (numberDoingTask * doubleAmountPerPerson);
						
						final CityProductionBreakdown breakdown = productionValues.findOrAddProductionType (thisProduction.getProductionTypeID ());
						breakdown.getPopulationTaskProduction ().add (taskBreakdown);
						breakdown.setDoubleProductionAmount (breakdown.getDoubleProductionAmount () + taskBreakdown.getDoubleProductionAmountAllPopulation ());
					}

		log.trace ("Exiting addProductionFromPopulation");
	}
	
	/**
	 * Adds on production generated by our fortress according to the number of picks of a particular type we chose at the start of the game
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param pickType Type of picks (spell books or retorts)
	 * @param pickTypeCount The number of the pick we had at the start of the game
	 */
	final void addProductionFromFortressPickType (final CityProductionBreakdownsEx productionValues, final PickType pickType, final int pickTypeCount)
	{
		log.trace ("Entering addProductionFromFortressPickType: " + pickType.getPickTypeID () + " x" + pickTypeCount);

		if (pickTypeCount > 0)
			for (final FortressPickTypeProduction thisProduction : pickType.getFortressPickTypeProduction ())
			{
				final CityProductionBreakdownPickType pickTypeBreakdown = new CityProductionBreakdownPickType ();
				pickTypeBreakdown.setPickTypeID (pickType.getPickTypeID ());
				pickTypeBreakdown.setCount (pickTypeCount);
				pickTypeBreakdown.setDoubleProductionAmountEachPick (thisProduction.getDoubleAmount ());
				pickTypeBreakdown.setDoubleProductionAmountAllPicks (pickTypeCount * thisProduction.getDoubleAmount ());

				final CityProductionBreakdown breakdown = productionValues.findOrAddProductionType (thisProduction.getFortressProductionTypeID ());
				breakdown.getPickTypeProduction ().add (pickTypeBreakdown);
				breakdown.setDoubleProductionAmount (breakdown.getDoubleProductionAmount () + pickTypeBreakdown.getDoubleProductionAmountAllPicks ());
			}

		log.trace ("Exiting addProductionFromFortressPickType");
	}

	/**
	 * Adds on production generated by our fortress being on a particular plane
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param plane Which plane our fortress is on
	 */
	final void addProductionFromFortressPlane (final CityProductionBreakdownsEx productionValues, final Plane plane)
	{
		log.trace ("Entering addProductionFromFortressPlane: " + plane.getPlaneNumber ());

		for (final FortressPlaneProduction thisProduction : plane.getFortressPlaneProduction ())
		{
			final CityProductionBreakdown breakdown = productionValues.findOrAddProductionType (thisProduction.getFortressProductionTypeID ());
			breakdown.setFortressPlane (plane.getPlaneNumber ());
			breakdown.setDoubleProductionAmountFortressPlane (breakdown.getDoubleProductionAmountFortressPlane () + thisProduction.getDoubleAmount ());
			breakdown.setDoubleProductionAmount (breakdown.getDoubleProductionAmount () + thisProduction.getDoubleAmount ());
		}

		log.trace ("Exiting addProductionFromFortressPlane");
	}
	
	/**
	 * Adds all the productions and/or consumptions generated by a particular building
	 * 
	 * @param productionValues Production values running totals to add the production to
	 * @param building The building to calculate for
	 * @param picks The list of spell picks belonging to the player who owns the city that this building is in
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	final void addProductionAndConsumptionFromBuilding (final CityProductionBreakdownsEx productionValues,
		final Building building, final List<PlayerPick> picks, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		log.trace ("Entering addProductionAndConsumptionFromBuilding: " + building.getBuildingID ());

		// Go through each type of production/consumption from this building
		for (final BuildingPopulationProductionModifier thisProduction : building.getBuildingPopulationProductionModifier ())

			// Only pick out production modifiers which come from the building by itself with no effect from the number of population
			// - such as the Library giving +2 research - we don't want modifiers such as the Animsts' Guild giving 1 to each farmer
			if (thisProduction.getPopulationTaskID () == null)
			{
				CityProductionBreakdownBuilding buildingBreakdown = null;
				
				// Just to stop null pointer exceptions below
				if (thisProduction.getDoubleAmount () == null)
				{
				}

				// Production?
				else if (thisProduction.getDoubleAmount () > 0)
				{
					// Bonus from retorts?
					final int totalReligiousBuildingBonus;
					if ((picks != null) && (building.isBuildingUnrestReductionImprovedByRetorts () != null) && (building.isBuildingUnrestReductionImprovedByRetorts ()))
						totalReligiousBuildingBonus = getPlayerPickUtils ().totalReligiousBuildingBonus (picks, db);
					else
						totalReligiousBuildingBonus = 0;

					// Calculate amounts
					final int amountBeforeReligiousBuildingBonus = thisProduction.getDoubleAmount ();
					final int amountAfterReligiousBuildingBonus = amountBeforeReligiousBuildingBonus + ((amountBeforeReligiousBuildingBonus * totalReligiousBuildingBonus) / 100);

					// Add it
					buildingBreakdown = new CityProductionBreakdownBuilding ();
					buildingBreakdown.setReligiousBuildingPercentageBonus (totalReligiousBuildingBonus);
					buildingBreakdown.setDoubleUnmodifiedProductionAmount (amountBeforeReligiousBuildingBonus);
					buildingBreakdown.setDoubleModifiedProductionAmount (amountAfterReligiousBuildingBonus);
					
					if (totalReligiousBuildingBonus > 0)
						buildingBreakdown.getPickIdContributingToReligiousBuildingBonus ().addAll (getPlayerPickUtils ().pickIdsContributingToReligiousBuildingBonus (picks, db));
				}

				// Consumption?
				else if (thisProduction.getDoubleAmount () < 0)
				{
					// Must be an exact multiple of 2
					final int consumption = -thisProduction.getDoubleAmount ();
					if (consumption % 2 != 0)
						throw new MomException ("Building \"" + building.getBuildingID () + "\" has a consumption value for \"" + thisProduction.getProductionTypeID () + "\" that is not an exact multiple of 2");

					buildingBreakdown = new CityProductionBreakdownBuilding ();
					buildingBreakdown.setConsumptionAmount (consumption / 2);
				}

				// Percentage bonus?
				// Can have both (production or consumption) AND percentage bonus, e.g. Marketplace
				if ((thisProduction.getPercentageBonus () != null) && (thisProduction.getPercentageBonus () > 0))
				{
					if (buildingBreakdown == null)
						buildingBreakdown = new CityProductionBreakdownBuilding ();
					
					buildingBreakdown.setPercentageBonus (thisProduction.getPercentageBonus ());
				}
				
				// Did we create anything?
				if (buildingBreakdown != null)
				{
					buildingBreakdown.setBuildingID (building.getBuildingID ());

					final CityProductionBreakdown breakdown = productionValues.findOrAddProductionType (thisProduction.getProductionTypeID ());
					breakdown.getBuildingBreakdown ().add (buildingBreakdown);
					
					// Add whatever we generated above to the grand totals
					breakdown.setDoubleProductionAmount (breakdown.getDoubleProductionAmount () + buildingBreakdown.getDoubleModifiedProductionAmount ());
					breakdown.setConsumptionAmount (breakdown.getConsumptionAmount () + buildingBreakdown.getConsumptionAmount ());
					breakdown.setPercentageBonus (breakdown.getPercentageBonus () + buildingBreakdown.getPercentageBonus ());
				}
			}

		log.trace ("Exiting addProductionAndConsumptionFromBuilding");
	}
	
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
	 */
	final void addProductionFromMapFeatures (final CityProductionBreakdownsEx productionValues, final MapVolumeOfMemoryGridCells map,
		final MapCoordinates3DEx cityLocation, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db,
		final int raceMineralBonusMultipler, final int buildingMineralPercentageBonus) throws RecordNotFoundException
	{
		log.trace ("Entering addProductionFromMapFeatures: " + cityLocation + ", " + raceMineralBonusMultipler + ", " + buildingMineralPercentageBonus);
		
		// First pass - get a list of how many of each map feature are within the city radius
		final Map<String, CityProductionBreakdownMapFeature> mapFeatures = new HashMap<String, CityProductionBreakdownMapFeature> ();

		final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
		for (final SquareMapDirection direction : DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ()))
			{
				final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				if ((terrainData != null) && (terrainData.getMapFeatureID () != null))
				{
					// Is this map feature already listed in the map?
					final CityProductionBreakdownMapFeature thisMapFeature = mapFeatures.get (terrainData.getMapFeatureID ());
					if (thisMapFeature == null)
					{
						// New map feature
						final CityProductionBreakdownMapFeature newMapFeature = new CityProductionBreakdownMapFeature ();
						newMapFeature.setMapFeatureID (terrainData.getMapFeatureID ());
						newMapFeature.setCount (1);
						mapFeatures.put (terrainData.getMapFeatureID (), newMapFeature);
					}
					else
					{
						// Just add to existing counter
						thisMapFeature.setCount (thisMapFeature.getCount () + 1);
					}
				}
			}

		// Second pass - add on the production from each map feature
		for (final CityProductionBreakdownMapFeature thisMapFeature : mapFeatures.values ())
		{
			final MapFeature mapFeature = db.findMapFeature (thisMapFeature.getMapFeatureID (), "addProductionFromMapFeatures");

			// Bonuses apply only to mines, not wild game
			if ((mapFeature.isRaceMineralMultiplerApplies () != null) && (mapFeature.isRaceMineralMultiplerApplies ()))
			{
				thisMapFeature.setRaceMineralBonusMultiplier (raceMineralBonusMultipler);
				thisMapFeature.setBuildingMineralPercentageBonus (buildingMineralPercentageBonus);
			}
			else
			{
				thisMapFeature.setRaceMineralBonusMultiplier (1);
				thisMapFeature.setBuildingMineralPercentageBonus (0);
			}

			// Add on each type of production generated
			for (final MapFeatureProduction thisProduction : mapFeature.getMapFeatureProduction ())
			{
				// Copy the details, in case one map feature generates multiple types of production
				final CityProductionBreakdownMapFeature copyMapFeature = new CityProductionBreakdownMapFeature ();
				copyMapFeature.setMapFeatureID (thisMapFeature.getMapFeatureID ());
				copyMapFeature.setCount (thisMapFeature.getCount ());
				copyMapFeature.setRaceMineralBonusMultiplier (thisMapFeature.getRaceMineralBonusMultiplier ());
				copyMapFeature.setBuildingMineralPercentageBonus (thisMapFeature.getBuildingMineralPercentageBonus ());
				
				// Deal with multipliers
				copyMapFeature.setDoubleUnmodifiedProductionAmountEachFeature (thisProduction.getDoubleAmount ());
				
				copyMapFeature.setDoubleUnmodifiedProductionAmountAllFeatures
					(copyMapFeature.getDoubleUnmodifiedProductionAmountEachFeature () * copyMapFeature.getCount ());
				
				copyMapFeature.setDoubleProductionAmountAfterRacialMultiplier
					(copyMapFeature.getDoubleUnmodifiedProductionAmountAllFeatures () * copyMapFeature.getRaceMineralBonusMultiplier ());
				
				copyMapFeature.setDoubleModifiedProductionAmountAllFeatures (copyMapFeature.getDoubleProductionAmountAfterRacialMultiplier () +
					((copyMapFeature.getDoubleProductionAmountAfterRacialMultiplier () * copyMapFeature.getBuildingMineralPercentageBonus ()) / 100));

				// Add it
				final CityProductionBreakdown breakdown = productionValues.findOrAddProductionType (thisProduction.getProductionTypeID ());
				breakdown.getMapFeatureProduction ().add (copyMapFeature);
				breakdown.setDoubleProductionAmount (breakdown.getDoubleProductionAmount () + copyMapFeature.getDoubleModifiedProductionAmountAllFeatures ());
			}
		}

		log.trace ("Exiting addProductionFromMapFeatures");
	}
	
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
	@Override
	public final CityProductionBreakdownsEx calculateAllCityProductions (final List<? extends PlayerPublicDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd, final boolean includeProductionAndConsumptionFromPopulation,
		final boolean calculatePotential, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		log.trace ("Entering calculateAllCityProductions: " + cityLocation);

		final MemoryGridCell mc = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final OverlandMapCityData cityData = mc.getCityData ();
		final String raceID = (cityData != null) ? cityData.getCityRaceID () : null;
		final Race cityRace = (raceID != null) ? db.findRace (raceID, "calculateAllCityProductions") : null;

		final Integer cityOwnerID = (cityData != null) ? cityData.getCityOwnerID () : null;
		final PlayerPublicDetails cityOwner = (cityOwnerID != null) ? getMultiplayerSessionUtils ().findPlayerWithID (players, cityOwnerID, "calculateAllCityProductions") : null;
		final List<PlayerPick> cityOwnerPicks = (cityOwner != null) ? ((MomPersistentPlayerPublicKnowledge) cityOwner.getPersistentPlayerPublicKnowledge ()).getPick () : null;

		// Set up results object
		final CityProductionBreakdownsEx productionValues = new CityProductionBreakdownsEx ();

		// Food production from surrounding tiles
		final CityProductionBreakdown food = listCityFoodProductionFromTerrainTiles (map, cityLocation, sd.getMapSize (), db);
		productionValues.getProductionType ().add (food);

		// Production % increase from surrounding tiles
		final CityProductionBreakdown production = listCityProductionPercentageBonusesFromTerrainTiles (map, cityLocation, sd.getMapSize (), db);
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
				gold.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
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
				rations.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS);
				rations.setApplicablePopulation (eaters);
				rations.setConsumptionAmountEachPopulation (1);
				rations.setConsumptionAmountAllPopulation (eaters);
				rations.setConsumptionAmount (eaters);
				
				productionValues.getProductionType ().add (rations);
			}

			// Production from population
			addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER,
				cityData.getMinimumFarmers () + cityData.getOptionalFarmers (), cityLocation, buildings, db);

			addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_WORKER,
				(cityData.getCityPopulation () / 1000) - cityData.getMinimumFarmers () - cityData.getOptionalFarmers () - cityData.getNumberOfRebels (), cityLocation, buildings, db);

			// With magical races, even the rebels produce power
			addProductionFromPopulation (productionValues, cityRace, CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_REBEL,
				cityData.getNumberOfRebels (), cityLocation, buildings, db);
		}

		// Production from and Maintenance of buildings
		for (final Building thisBuilding : db.getBuilding ())
			
			// If calculatePotential is true, assume we've built everything
			// We only really need to count the granary and farmers' market, but easier just to include everything than to specifically discount these
			if (((calculatePotential) && (!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS))) ||
				((!calculatePotential) && (getMemoryBuildingUtils ().findBuilding (buildings, cityLocation, thisBuilding.getBuildingID ()) != null)))
			{
				if (thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS))
				{
					// Wizard's fortress produces mana according to how many books were chosen at the start of the game...
					for (final PickType thisPickType : db.getPickType ())
						addProductionFromFortressPickType (productionValues, thisPickType, getPlayerPickUtils ().countPicksOfType (cityOwnerPicks, thisPickType.getPickTypeID (), true, db));

					// ...and according to which plane it is on
					addProductionFromFortressPlane (productionValues, db.findPlane (cityLocation.getZ (), "calculateAllCityProductions"));
				}

				// Regular building
				// Do not count buildings with a pending sale
				else if (!thisBuilding.getBuildingID ().equals (mc.getBuildingIdSoldThisTurn ()))
					addProductionAndConsumptionFromBuilding (productionValues, thisBuilding, cityOwnerPicks, db);
			}

		// Maintenance cost of city enchantment spells
		// Left this out - its commented out in the Delphi code as well due to the fact that
		// Temples and such produce magic power, but spell maintenance is charged in Mana

		// See if we've got a miners' guild to boost the income from map features
		final CityProductionBreakdown mineralPercentageResult = productionValues.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER);
		final int buildingMineralPercentageBonus = (mineralPercentageResult != null) ? mineralPercentageResult.getPercentageBonus () : 0;
		final int raceMineralBonusMultipler = (cityRace != null) ? cityRace.getMineralBonusMultiplier () : 1;

		// Production from nearby map features
		// Have to do this after buildings, so we can have discovered if we have the miners' guild bonus to map features
		addProductionFromMapFeatures (productionValues, map, cityLocation, sd.getMapSize (), db, raceMineralBonusMultipler, buildingMineralPercentageBonus);
		
		// Halve and cap food (max city size) production first, because if calculatePotential=true then we need to know the potential max city size before
		// we can calculate the gold trade % cap.
		// Have to do this after map features are added in, since wild game increase max city size.
		halveAddPercentageBonusAndCapProduction (food, sd.getDifficultyLevel ().getCityMaxSize (), db);
		
		// Gold trade % from rivers and oceans
		// Have to do this (at least the cap) after map features, since if calculatePotential=true then we need to have included wild game
		// into considering the potential maximum size this city will reach and cap the gold trade % accordingly
		calculateGoldTradeBonus (productionValues.findOrAddProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD), map, cityLocation,
			(calculatePotential ? food.getCappedProductionAmount () : null), sd.getMapSize (), db);

		// Halve production values, using rounding defined in XML file for each production type (consumption values aren't doubled to begin with)
		for (final CityProductionBreakdown thisProduction : productionValues.getProductionType ())
			if (thisProduction != food)
				halveAddPercentageBonusAndCapProduction (thisProduction, sd.getDifficultyLevel ().getCityMaxSize (), db);

		// Sort the list
		Collections.sort (productionValues.getProductionType (), new CityProductionBreakdownSorter ());

		log.trace ("Exiting calculateAllCityProductions = " + productionValues);
		return productionValues;
	}
	
	/**
	 * After doubleProductionAmount has been filled in, this deals with halving the value according to the rounding
	 * rules for the production type, adding on any % bonus, and dealing with any overall cap.
	 * 
	 * @param thisProduction Production value to halve, add % bonus and cap
	 * @param cityMaxSize Max city size set in the session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a production type that can't be found in the DB
	 * @throws MomException If we encounter a production value that the DB states should always be an exact multiple of 2, but isn't
	 */
	final void halveAddPercentageBonusAndCapProduction (final CityProductionBreakdown thisProduction, final int cityMaxSize, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		log.trace ("Entering halveAddPercentageBonusAndCapProduction: " + thisProduction.getProductionTypeID () + ", " + thisProduction.getDoubleProductionAmount ());
		
		if (thisProduction.getDoubleProductionAmount () > 0)
		{
			final ProductionType productionType = db.findProductionType (thisProduction.getProductionTypeID (), "halveAddPercentageBonusAndCapProduction");

			// Perform rounding - if its an exact multiple of 2 then we don't care what type of rounding it is
			if (thisProduction.getDoubleProductionAmount () % 2 == 0)
			{
				thisProduction.setBaseProductionAmount (thisProduction.getDoubleProductionAmount () / 2);
			}
			else switch (productionType.getRoundingDirectionID ())
			{
				case ROUND_DOWN:
					thisProduction.setRoundingDirectionID (productionType.getRoundingDirectionID ());
					thisProduction.setBaseProductionAmount (thisProduction.getDoubleProductionAmount () / 2);
					break;

				case ROUND_UP:
					thisProduction.setRoundingDirectionID (productionType.getRoundingDirectionID ());
					thisProduction.setBaseProductionAmount ((thisProduction.getDoubleProductionAmount () + 1) / 2);
					break;

				case MUST_BE_EXACT_MULTIPLE:
					// We've already dealt with the situation where the value is an exact multiple above, so to have reached here it
					// must have been supposed to be an exact multiple but wasn't
					throw new MomException ("calculateAllCityProductions: City calculated a production value for production \"" + thisProduction.getProductionTypeID () +
						"\" which is not a multiple of 2 = " + thisProduction.getDoubleProductionAmount ());

				default:
					throw new MomException ("calculateAllCityProductions: City calculated a production value for production \"" + thisProduction.getProductionTypeID () +
						"\" which has an unknown rounding direction");
			}

			// Add on % bonus
			thisProduction.setModifiedProductionAmount (thisProduction.getBaseProductionAmount () +
				((thisProduction.getBaseProductionAmount () * thisProduction.getPercentageBonus ()) / 100));

			// Stop max city size going over the game set maximum
			final int cap = (thisProduction.getProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD)) ? cityMaxSize : Integer.MAX_VALUE;
			thisProduction.setCappedProductionAmount (Math.min (thisProduction.getModifiedProductionAmount (), cap));
		}

		log.trace ("Exiting halveAddPercentageBonusAndCapProduction = " + thisProduction.getCappedProductionAmount ());
	}

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
	@Override
	public final int calculateSingleCityProduction (final List<? extends PlayerPublicDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd,
		final boolean includeProductionAndConsumptionFromPopulation, final CommonDatabase db, final String productionTypeID)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		log.trace ("Entering calculateSingleCityProduction: " + cityLocation);

		// This is a right pain - ideally we want a cut down routine that scans only for this production type - however the Miners' Guild really
		// buggers that up because it has a different production ID but still might affect the single production type we've asked for (by giving bonuses to map minerals), e.g. Gold
		// So just do this the long way and then throw away all the other results
		final CityProductionBreakdownsEx productionValues = calculateAllCityProductions (players, map, buildings, cityLocation, taxRateID, sd,
			includeProductionAndConsumptionFromPopulation, false, db);		// calculatePotential fixed at false

		final CityProductionBreakdown singleProductionValue = productionValues.findProductionType (productionTypeID);
		final int netGain;
		if (singleProductionValue == null)
			netGain = 0;
		else
			netGain = singleProductionValue.getCappedProductionAmount () - singleProductionValue.getConsumptionAmount ();

		log.trace ("Exiting calculateSingleCityProduction = " + netGain);
		return netGain;
	}

	/**
	 * Blanks the building sold this turn value in every map cell
	 * @param map Should either be the true map or the memory map for the player requested, to ensure that they can see their own cities
	 * @param onlyOnePlayerID If zero, will blank values in cities belonging to all players; if specified will blank values in cities belonging only to the specified player
	 */
	@Override
	public final void blankBuildingsSoldThisTurn (final MapVolumeOfMemoryGridCells map, final int onlyOnePlayerID)
	{
		log.trace ("Entering blankBuildingsSoldThisTurn: Player ID " + onlyOnePlayerID);

		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell mc : row.getCell ())
				{
					if ((mc.getCityData () != null) && (mc.getCityData ().getCityPopulation () > 0) &&
						((onlyOnePlayerID == 0) || (onlyOnePlayerID == mc.getCityData ().getCityOwnerID ())))

						mc.setBuildingIdSoldThisTurn (null);
				}

		log.trace ("Exiting blankBuildingsSoldThisTurn");
	}

	/**
	 * Client uses this for the surveryor and whether settlers have the build button enabled - but note, this only works based
	 * on what we know of the map - its possible we may think we can place a city at a location but actually cannot
	 *
	 * Server uses this during game startup to position all starter cities - for this it runs over the true map, because no
	 * players have any knowledge of the map yet
	 *
	 * @param map Our knowledge of the overland terrain map
	 * @param plane Which plane we want to place a city on
	 * @param mapSize Overland map coordinate system and extended details
	 * @return Map area with areas we know are too close to cities marked
	 */
	@Override
	public final MapArea2D<Boolean> markWithinExistingCityRadius (final MapVolumeOfMemoryGridCells map,
		final int plane, final MapSizeData mapSize)
	{
		log.trace ("Entering markWithinExistingCityRadius: " + plane);

		final MapArea2D<Boolean> result = new MapArea2DArrayListImpl<Boolean> ();
		result.setCoordinateSystem (mapSize);
		
		final BooleanMapAreaOperations2DImpl op = new BooleanMapAreaOperations2DImpl ();
		op.setCoordinateSystemUtils (getCoordinateSystemUtils ());
		op.deselectAll (result);
		
		for (int x = 0; x < mapSize.getWidth (); x++)
			for (int y = 0; y < mapSize.getHeight (); y++)
			{
				final OverlandMapCityData cityData = map.getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getCityData ();
				if ((cityData != null) && (cityData.getCityPopulation () != null) && (cityData.getCityPopulation () > 0))
					op.selectRadius (result, x, y, mapSize.getCitySeparation ());
			}

		log.trace ("Exiting markWithinExistingCityRadius");
		return result;
	}

	/**
	 * @param totalCost Total production cost of the building/unit
	 * @param builtSoFar Amount of production we've put towards it so far
	 * @return Gold to rush buy a particular construction project
	 */
	@Override
	public final int goldToRushBuy (final int totalCost, final int builtSoFar)
	{
		// See p200 in the strategy guide
		final int result;
		if (builtSoFar <= 0)
			result = totalCost * 4;
		
		else if (builtSoFar < totalCost/2)
			result = (totalCost - builtSoFar) * 3;
		
		else
			result = (totalCost - builtSoFar) * 2;
		
		return result;
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
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
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
}