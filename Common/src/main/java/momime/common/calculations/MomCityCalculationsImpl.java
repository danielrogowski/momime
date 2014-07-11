package momime.common.calculations;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_5.MapSizeData;
import momime.common.database.v0_9_5.Building;
import momime.common.database.v0_9_5.BuildingRequiresTileType;
import momime.common.database.v0_9_5.MapFeatureProduction;
import momime.common.database.v0_9_5.PickType;
import momime.common.database.v0_9_5.ProductionType;
import momime.common.database.v0_9_5.Race;
import momime.common.database.v0_9_5.RaceUnrest;
import momime.common.database.v0_9_5.RoundingDirectionID;
import momime.common.database.v0_9_5.TaxRate;
import momime.common.database.v0_9_5.TileType;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityGrowthRateBreakdownBuilding;
import momime.common.internal.CityGrowthRateBreakdownDying;
import momime.common.internal.CityGrowthRateBreakdownGrowing;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.CityUnrestBreakdownBuilding;
import momime.common.messages.v0_9_5.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_5.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.OverlandMapTerrainData;
import momime.common.messages.v0_9_5.PlayerPick;
import momime.common.messages.v0_9_5.UnitStatusID;
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
	 * @param map Known terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return % production bonus for a city located at this grid cell
	 * @throws RecordNotFoundException If we encounter a tile type that we cannot find in the cache
	 */
	@Override
	public final int calculateProductionBonus (final MapVolumeOfMemoryGridCells map, final MapCoordinates3DEx cityLocation,
		final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering calculateProductionBonus: " + cityLocation);

		int productionBonus = 0;
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
		
		for (final SquareMapDirection direction : DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
		{
			if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ()))
			{
				final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				if ((terrainData != null) && (terrainData.getTileTypeID () != null))
				{
					final Integer thisBonus = db.findTileType (terrainData.getTileTypeID (), "calculateProductionBonus").getProductionBonus ();
					if (thisBonus != null)
						productionBonus = productionBonus + thisBonus;
				}
			}
		}

		log.trace ("Exiting calculateProductionBonus = " + productionBonus);
		return productionBonus;
	}

	/**
	 * @param map Known terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return % gold bonus for a city located at this grid cell
	 * @throws RecordNotFoundException If we encounter a tile type that we cannot find in the cache
	 */
	@Override
	public final int calculateGoldBonus (final MapVolumeOfMemoryGridCells map, final MapCoordinates3DEx cityLocation,
		final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering calculateGoldBonus: " + cityLocation);

		// Deal with centre square
		int goldBonus = 0;
		final OverlandMapTerrainData centreTile = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getTerrainData ();
		if ((centreTile != null) && (centreTile.getTileTypeID () != null))
		{
			final Integer centreBonus = db.findTileType (centreTile.getTileTypeID (), "calculateGoldBonus").getGoldBonus ();
			if (centreBonus != null)
				goldBonus = centreBonus;
		}

		// Only check adjacent squares if we didn't find a centre square bonus
		int d = 1;
		while ((goldBonus == 0) && (d <= getCoordinateSystemUtils ().getMaxDirection (overlandMapCoordinateSystem.getCoordinateSystemType ())))
		{
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, d))
			{
				// Bonus only applies if adjacent flag is set
				final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				if ((terrainData != null) && (terrainData.getTileTypeID () != null))
				{
					final TileType tileType = db.findTileType (terrainData.getTileTypeID (), "calculateGoldBonus");
					if ((tileType.isGoldBonusSurroundingTiles () != null) && (tileType.isGoldBonusSurroundingTiles ()) && (tileType.getGoldBonus () != null))
						goldBonus = tileType.getGoldBonus ();
				}
			}

			d++;
		}

		log.trace ("Exiting calculateGoldBonus = " + goldBonus);
		return goldBonus;
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
	 * Strategy guide p194
	 * @param map Known terrain
	 * @param cityLocation Location of the city to calculate for
	 * @param sessionDescription Session description
	 * @param includeBonusesFromMapFeatures True to include bonuses from map features (Wild Game), false to just count food harvested from the terrain
	 * @param halveAndCapResult True to halve the result (i.e. return the actual max city size) and cap at the game max city size, false to leave the production values as they are (i.e. return double the actual max city size) and uncapped
	 * @param db Lookup lists built over the XML database
	 * @return Maximum size a city here will grow to, based on knowledge of surrounding terrain, excluding any buildings that will improve it (Granary & Farmers' Market)
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 */
	@Override
	public final int calculateMaxCitySize (final MapVolumeOfMemoryGridCells map,
		final MapCoordinates3DEx cityLocation, final MomSessionDescription sessionDescription, final boolean includeBonusesFromMapFeatures, final boolean halveAndCapResult,
		final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering calculateMaxCitySize: " + cityLocation + ", " +
			sessionDescription.getDifficultyLevel ().getCityMaxSize () + ", " + includeBonusesFromMapFeatures + ", " + halveAndCapResult);

		int maxCitySize = 0;
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
		for (final SquareMapDirection direction : DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
		{
			if (getCoordinateSystemUtils ().move3DCoordinates (sessionDescription.getMapSize (), coords, direction.getDirectionID ()))
			{
				final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();

				// Food from terrain
				if ((terrainData != null) && (terrainData.getTileTypeID () != null))
				{
					final Integer foodFromTileType = db.findTileType (terrainData.getTileTypeID (), "calculateMaxCitySize").getDoubleFood ();
					if (foodFromTileType != null)
						maxCitySize = maxCitySize + foodFromTileType;
				}

				// Food from map feature - have to search through all feature productions looking for the right production type
				if ((includeBonusesFromMapFeatures) && (terrainData != null) && (terrainData.getMapFeatureID () != null))
					for (final MapFeatureProduction thisProduction : db.findMapFeature (terrainData.getMapFeatureID (), "calculateMaxCitySize").getMapFeatureProduction ())
						if (thisProduction.getProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD))
							maxCitySize = maxCitySize + thisProduction.getDoubleAmount ();
			}
		}

		// Database values are double the food value but we must round up, not down - Round up confirmed both by testing + its what the strategy guide says
		// Need to be able to switch the halving off because calculateAllCityProductions () wants the doubled value
		if (halveAndCapResult)
		{
			maxCitySize = (maxCitySize + 1) / 2;

			if (maxCitySize > sessionDescription.getDifficultyLevel ().getCityMaxSize ())
				maxCitySize = sessionDescription.getDifficultyLevel ().getCityMaxSize ();
		}

		log.trace ("Exiting calculateMaxCitySize = " + maxCitySize);
		return maxCitySize;
	}

	/**
	 * Strategy guide p196, however note the example is in contradiction with the formula - from testing I believe the example is right and the formula is supposed to be a -1 not a +1
	 * Death rate is on strategy guide p197
	 *
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @param cityLocation Location of the city to calculate for
	 * @param maxCitySize Maximum city size with all buildings taken into account - i.e. the RE06 output from calculateAllCityProductions () - not the value output from calculateMaxCitySize ()
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
		final MapCoordinates3DEx fortressLocation = getMemoryBuildingUtils ().findCityWithBuilding
			(cityData.getCityOwnerID (), CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, map, buildings);

		if (fortressLocation != null)
		{
			// Find the capital race's unrest value listed under this city's race
			final OverlandMapCityData fortressCityData = map.getPlane ().get (fortressLocation.getZ ()).getRow ().get (fortressLocation.getY ()).getCell ().get (fortressLocation.getX ()).getCityData ();

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
			final PlayerPublicDetails cityOwner = MultiplayerSessionUtils.findPlayerWithID (players, cityData.getCityOwnerID (), "calculateCityRebels");
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
	 * @param players Pre-locked players list
	 * @param map Known terrain
	 * @param buildings List of known buildings
	 * @param cityLocation Location of the city to calculate for
	 * @param taxRateID Tax rate to use for the calculation
	 * @param sd Session description
	 * @param includeProductionAndConsumptionFromPopulation Normally true; if false, production and consumption from civilian population will be excluded
	 * @param db Lookup lists built over the XML database
	 * @param storeBreakdown Whether to store breakdown objects or throw the details away and just keep the results
	 * @return List of all productions and consumptions from this city
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type, map feature, production type or so on that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final CalculateCityProductionResults calculateAllCityProductions (final List<? extends PlayerPublicDetails> players,
		final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final String taxRateID, final MomSessionDescription sd, final boolean includeProductionAndConsumptionFromPopulation,
		final CommonDatabase db, final boolean storeBreakdown)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		log.trace ("Entering calculateAllCityProductions: " + cityLocation);

		final MemoryGridCell mc = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final OverlandMapCityData cityData = mc.getCityData ();
		final Race cityRace = db.findRace (cityData.getCityRaceID (), "calculateAllCityProductions");

		final PlayerPublicDetails cityOwner = MultiplayerSessionUtils.findPlayerWithID (players, cityData.getCityOwnerID (), "calculateAllCityProductions");
		final List<PlayerPick> cityOwnerPicks = ((MomPersistentPlayerPublicKnowledge) cityOwner.getPersistentPlayerPublicKnowledge ()).getPick ();

		// Set up results object, and inject necessary values across into it
		final CalculateCityProductionResultsImplementation productionValues = new CalculateCityProductionResultsImplementation ();
		productionValues.setStoreBreakdown (storeBreakdown);
		productionValues.setMemoryBuildingUtils (getMemoryBuildingUtils ());
		productionValues.setPlayerPickUtils (getPlayerPickUtils ());

		// Food production from surrounding tiles
		productionValues.addProduction (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD, null, null, null, null, null,
			0, 0, 0, 0, 0, 0, 0, calculateMaxCitySize (map, cityLocation, sd, false, false, db));

		// Production % increase from surrounding tiles
		final int terrainProductionBonus = calculateProductionBonus (map, cityLocation, sd.getMapSize (), db);
		if (terrainProductionBonus > 0)
			productionValues.addPercentage (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION, null, terrainProductionBonus);

		// Gold trade % from rivers and oceans
		final int goldTradeBonus = calculateGoldBonus (map, cityLocation, sd.getMapSize (), db);
		if (goldTradeBonus > 0)
		{
			productionValues.addPercentage (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, null, goldTradeBonus);

			// Check if over maximum
			final int maxGoldTradeBonus = (cityData.getCityPopulation () / 1000) * 3;
			if (goldTradeBonus > maxGoldTradeBonus)
			{
				// Enforce cap
				productionValues.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD).setPercentageBonus (maxGoldTradeBonus);
				productionValues.addCap (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, cityData.getCityPopulation (), maxGoldTradeBonus);
			}
		}

		// Deal with people
		if (includeProductionAndConsumptionFromPopulation)
		{
			// Production from population
			productionValues.addProductionFromPopulation (cityRace, CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER,
				cityData.getMinimumFarmers () + cityData.getOptionalFarmers (), cityLocation, buildings, db);

			productionValues.addProductionFromPopulation (cityRace, CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_WORKER,
				(cityData.getCityPopulation () / 1000) - cityData.getMinimumFarmers () - cityData.getOptionalFarmers () - cityData.getNumberOfRebels (), cityLocation, buildings, db);

			// With magical races, even the rebels produce power
			productionValues.addProductionFromPopulation (cityRace, CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_REBEL,
				cityData.getNumberOfRebels (), cityLocation, buildings, db);

			// Gold from taxes
			final TaxRate taxRate = db.findTaxRate (taxRateID, "calculateAllCityProductions");

			// If tax rate set to zero then we're getting no money from taxes, so don't add it
			// Otherwise we get a production entry in the breakdown of zero which produces an error on the client
			if (taxRate.getDoubleTaxGold () > 0)
			{
				final int taxPayers = (cityData.getCityPopulation () / 1000) - cityData.getNumberOfRebels ();

				productionValues.addProduction (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, null, null, null, null, null,
					taxPayers, taxRate.getDoubleTaxGold (), 0,
					0,
					0, 0,
					0, taxPayers * taxRate.getDoubleTaxGold ());
			}
		}

		// Production from and Maintenance of buildings
		for (final MemoryBuilding thisBuilding : buildings)
			if (cityLocation.equals (thisBuilding.getCityLocation ()))
			{
				if (thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS))
				{
					// Wizard's fortress produces mana according to how many books were chosen at the start of the game...
					for (final PickType thisPickType : db.getPickType ())
						productionValues.addProductionFromFortressPickType (thisPickType, getPlayerPickUtils ().countPicksOfType (cityOwnerPicks, thisPickType.getPickTypeID (), true, db));

					// ...and according to which plane it is on
					productionValues.addProductionFromFortressPlane (db.findPlane (cityLocation.getZ (), "calculateAllCityProductions"));
				}

				// Regular building
				// Do not count buildings with a pending sale
				else if (!thisBuilding.getBuildingID ().equals (mc.getBuildingIdSoldThisTurn ()))
					productionValues.addProductionAndConsumptionFromBuilding (db.findBuilding (thisBuilding.getBuildingID (), "calculateAllCityProductions"), cityOwnerPicks, db);
			}

		// Maintenance cost of city enchantment spells
		// Left this out - its commented out in the Delphi code as well due to the fact that
		// Temples and such produce magic power, but spell maintenance is charged in Mana

		// See if we've got a miners' guild to boost the income from map features
		final CalculateCityProductionResult mineralPercentageResult = productionValues.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER);
		final int mineralPercentageBonus;
		if (mineralPercentageResult == null)
			mineralPercentageBonus = 0;
		else
			mineralPercentageBonus = mineralPercentageResult.getPercentageBonus ();

		// Production from nearby map features
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
		for (final SquareMapDirection direction : DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (getCoordinateSystemUtils ().move3DCoordinates (sd.getMapSize (), coords, direction.getDirectionID ()))
			{
				final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				if ((terrainData != null) && (terrainData.getMapFeatureID () != null))
					productionValues.addProductionFromMapFeature (db.findMapFeature (terrainData.getMapFeatureID (), "calculateAllCityProductions"), cityRace.getMineralBonusMultiplier (), mineralPercentageBonus);
			}

		// Rations consumption by population
		if (includeProductionAndConsumptionFromPopulation)
		{
			final int eaters = cityData.getCityPopulation () / 1000;
			if (eaters > 0)
				productionValues.addConsumption (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, null, eaters, eaters);
		}

		// Halve production values, using rounding defined in XML file for each production type (consumption values aren't doubled to begin with)
		for (final CalculateCityProductionResult thisProduction : productionValues.getResults ())
			if (thisProduction.getDoubleProductionAmount () > 0)
			{
				final ProductionType productionType = db.findProductionType (thisProduction.getProductionTypeID (), "calculateAllCityProductions");

				// Perform rounding - if its an exact multiple of 2 then we don't care what type of rounding it is
				RoundingDirectionID roundingDirectionForBreakdown = productionType.getRoundingDirectionID ();
				if (thisProduction.getDoubleProductionAmount () % 2 == 0)
				{
					thisProduction.setBaseProductionAmount (thisProduction.getDoubleProductionAmount () / 2);

					// This is a bit of a cheat so we can still write a non-blank value to the Breakdown to make the Total appear, but without the rounding up/down messages appearing
					roundingDirectionForBreakdown = RoundingDirectionID.MUST_BE_EXACT_MULTIPLE;
				}
				else switch (productionType.getRoundingDirectionID ())
				{
					case ROUND_DOWN:
						thisProduction.setBaseProductionAmount (thisProduction.getDoubleProductionAmount () / 2);
						break;

					case ROUND_UP:
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

				// Add total/rounding details to breakdown, note we re-double the rounded value!
				thisProduction.addRoundingBreakdown (thisProduction.getDoubleProductionAmount (), thisProduction.getBaseProductionAmount () * 2, roundingDirectionForBreakdown);

				// Stop max city size going over the game set maximum
				if ((thisProduction.getProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD)) &&
					(thisProduction.getBaseProductionAmount () > sd.getDifficultyLevel ().getCityMaxSize ()))
				{
					thisProduction.setBaseProductionAmount (sd.getDifficultyLevel ().getCityMaxSize ());
					thisProduction.addCapBreakdown (0, sd.getDifficultyLevel ().getCityMaxSize ());
				}
			}

		// Sort the list
		Collections.sort (productionValues.getResults ());

		log.trace ("Exiting calculateAllCityProductions = " + productionValues);
		return productionValues;
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
		final CalculateCityProductionResults productionValues = calculateAllCityProductions (players, map, buildings, cityLocation, taxRateID, sd,
			includeProductionAndConsumptionFromPopulation, db, false);		// Not interested in breakdown

		final CalculateCityProductionResult singleProductionValue = productionValues.findProductionType (productionTypeID);
		final int netGain;
		if (singleProductionValue == null)
			netGain = 0;
		else
			netGain = singleProductionValue.getModifiedProductionAmount () - singleProductionValue.getConsumptionAmount ();

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
}