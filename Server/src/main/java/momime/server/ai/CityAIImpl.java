package momime.server.ai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;
import com.ndg.random.WeightedChoicesImpl;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.database.AiBuildingTypeID;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Plane;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TaxRate;
import momime.common.database.Unit;
import momime.common.database.Wizard;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerCityCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.process.CityProcessing;
import momime.server.utils.CityServerUtils;

/**
 * Methods for AI players making decisions about where to place cities and what to build in them
 */
public final class CityAIImpl implements CityAI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CityAIImpl.class);
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;

	/** Server-only city calculations */
	private ServerCityCalculations serverCityCalculations;

	/** Random number generator */
	private RandomUtils randomUtils;

	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/**
	 * @param cityLocation Where to consider putting a city
	 * @param avoidOtherCities Whether to avoid putting this city close to any existing cities (regardless of who owns them); used for placing starter cities but not when AI builds new ones
	 * @param enforceMinimumQuality Whether to avoid returning data about cities that are too small to be useful; so usually true, but false if we want to evalulate even terrible cities
	 * @param knownMap Known terrain
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return null if enforceMinimumQuality = true and a city here is too small to be useful; otherwise an estimate of how good a city here is/will be
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final Integer evaluateCityQuality (final MapCoordinates3DEx cityLocation, final boolean avoidOtherCities, final boolean enforceMinimumQuality,
		final MapVolumeOfMemoryGridCells knownMap, final MomSessionDescription sd, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		Integer thisCityQuality;
		
		final CityProductionBreakdownsEx productions = getCityCalculations ().calculateAllCityProductions (null, knownMap, null, cityLocation, null, sd, false, true, db);
		final CityProductionBreakdown foodProduction = productions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		final int maxCitySize = (foodProduction != null) ? foodProduction.getCappedProductionAmount () : 0;
		
		// If city will be so small as to not be useful then just discount it; remember granary adds +2 and farmers' market adds +3 to this, so really minimum size is 10
		if ((enforceMinimumQuality) && (maxCitySize < 5))
			thisCityQuality = null;
		else
		{
			final CityProductionBreakdown productionProduction = productions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
			final CityProductionBreakdown goldProduction = productions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
			
			final int productionPercentageBonus = (productionProduction != null) ? productionProduction.getPercentageBonus () : 0;
			final int goldTradePercentageBonus = (goldProduction != null) ? goldProduction.getTradePercentageBonusCapped () : 0;
	
			// Add on how good production and gold bonuses are
			thisCityQuality = (maxCitySize * 10) +			// Typically 5-25 -> 50-250
				goldTradePercentageBonus +					// Typically 0-30
				productionPercentageBonus;					// Typically 0-80 usefully
	
			// Improve the estimate according to nearby map features e.g. always stick cities next to adamantium!
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if (getCoordinateSystemUtils ().move3DCoordinates (sd.getOverlandMapSize (), coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData checkFeatureData = knownMap.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if ((checkFeatureData != null) && (checkFeatureData.getMapFeatureID () != null))
					{
						final Integer featureCityQualityEstimate = db.findMapFeature (checkFeatureData.getMapFeatureID (), "chooseCityLocation").getCityQualityEstimate ();
						if (featureCityQualityEstimate != null)
							thisCityQuality = thisCityQuality + featureCityQualityEstimate;
					}
				}
			
			// Avoid placing start cities near other cities, especially don't put wizards near each other
			if (avoidOtherCities)
			{
				// What's the closest existing city to these coordinates?
				Integer closestDistance = getCityServerUtils ().findClosestCityTo (cityLocation, knownMap, sd.getOverlandMapSize ());
				if (closestDistance != null)
					thisCityQuality = thisCityQuality + (closestDistance * 2);		// Maximum would be 40 apart (north-south of map) x2 = 80
			}
		}
		
		return thisCityQuality;
	}
	
	/**
	 * NB. We don't always know the race of the city we're positioning, when positioning raiders at the start of the game their
	 * race will most likely be the race chosen for the continent we decide to put the city on, i.e. we have to pick position first, race second
	 *
	 * @param knownMap Known terrain
	 * @param trueMap True map, just used to ensure we don't put a city too closed to another city that we cannot see
	 * @param plane Plane to place a city on
	 * @param avoidOtherCities Whether to avoid putting this city close to any existing cities (regardless of who owns them); used for placing starter cities but not when AI builds new ones
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param purpose What this city is being placed for, just for debug message
	 * @return Best possible location to put a new city, or null if there's no space left for any new cities on this plane
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final MapCoordinates3DEx chooseCityLocation (final MapVolumeOfMemoryGridCells knownMap, final MapVolumeOfMemoryGridCells trueMap,
		final int plane, final boolean avoidOtherCities, final MomSessionDescription sd, final CommonDatabase db, final String purpose)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// Mark off all places within 3 squares of an existing city, i.e. those spaces we can't put a new city
		final MapArea2D<Boolean> withinExistingCityRadius = getCityCalculations ().markWithinExistingCityRadius (trueMap, knownMap, plane, sd.getOverlandMapSize ());

		// Now consider every map location as a possible location for a new city
		MapCoordinates3DEx bestLocation = null;
		int bestCityQuality = -1;

		for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
			{
				// Can we build a city here?
				final OverlandMapTerrainData terrainData = knownMap.getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ();

				if ((terrainData != null) && (!withinExistingCityRadius.get (x, y)))
				{
					final Boolean canBuildCityOnThisTerrain = db.findTileType (terrainData.getTileTypeID (), "chooseCityLocation").isCanBuildCity ();
					final Boolean canBuildCityOnThisFeature = (terrainData.getMapFeatureID () == null) ? true : db.findMapFeature (terrainData.getMapFeatureID (), "chooseCityLocation").isCanBuildCity ();

					if ((canBuildCityOnThisTerrain != null) && (canBuildCityOnThisTerrain) &&
						(canBuildCityOnThisFeature != null) && (canBuildCityOnThisFeature))
					{
						// How good will this city be?
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane);
						final Integer thisCityQuality = evaluateCityQuality (cityLocation, avoidOtherCities, true, knownMap, sd, db); 

						// Is it the best so far?
						if ((thisCityQuality != null) && ((bestLocation == null) || (thisCityQuality > bestCityQuality)))
						{
							bestLocation = cityLocation;
							bestCityQuality = thisCityQuality;
						}
					}
				}
			}
		
		log.debug ("AI chose city location " + bestLocation + " with quality " + bestCityQuality + " for " + purpose);
		return bestLocation;
	}

	/**
	 * Finds workers in cities to convert to optional farmers
	 *
	 * @param doubleRationsNeeded 2x number of rations that we still need to find optional farmers to help produce
	 * @param tradeGoods If true will only consider cities that are building trade goods; if false will only consider cities that are building something other than trade goods
	 * @param trueMap True map details
	 * @param player Player who we want to convert workers into farmers for
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @return Adjusted value of doubleRationsNeeded
	 * @throws RecordNotFoundException If there is a building that cannot be found in the DB
	 * @throws MomException If a city's race has no farmers defined or those farmers have no ration production defined
	 */
	final int findWorkersToConvertToFarmers (final int doubleRationsNeeded, final boolean tradeGoods, final FogOfWarMemory trueMap,
		final PlayerServerDetails player, final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, MomException
	{
		// Build a list of all the workers, by finding all the cities and adding the coordinates of the city to the list the number
		// of times for how many workers there are in the city that we could convert to farmers
		final List<MapCoordinates3DEx> workerCoordinates = new ArrayList<MapCoordinates3DEx> ();

		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))

						if (((tradeGoods) && (CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))) ||
							((!tradeGoods) && (!CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))))
						{
							final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane.getPlaneNumber ());

							final int numberOfWorkers = (cityData.getCityPopulation () / 1000) - cityData.getMinimumFarmers () - cityData.getNumberOfRebels ();
							for (int workerNo = 0; workerNo < numberOfWorkers; workerNo++)
								workerCoordinates.add (cityLocation);
						}
				}

		log.debug ("findWorkersToConvertToFarmers: Found " + workerCoordinates.size () + " workers available to convert");

		// Now pick workers at random from the list to convert to farmers
		// In this way, we tend to favour putting farmers in larger cities that can spare the population more
		int modifiedDoubleRationsNeeded = doubleRationsNeeded;
		while ((modifiedDoubleRationsNeeded > 0) && (workerCoordinates.size () > 0))
		{
			final int workerNo = getRandomUtils ().nextInt (workerCoordinates.size ());
			final MapCoordinates3DEx cityLocation = workerCoordinates.get (workerNo);
			workerCoordinates.remove (workerNo);

			final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

			// Add 1 optional farmer in this city
			cityData.setOptionalFarmers (cityData.getOptionalFarmers () + 1);
			modifiedDoubleRationsNeeded = modifiedDoubleRationsNeeded - getServerCityCalculations ().calculateDoubleFarmingRate (trueMap.getMap (), trueMap.getBuilding (), cityLocation, db);
		}

		return modifiedDoubleRationsNeeded;
	}

	/**
	 * Sets the number of optional farmers optimally in every city owned by one player
	 *
	 * @param trueMap True map details
	 * @param players List of players in the session
	 * @param player Player who we want to reset the number of optional farmers for
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws RecordNotFoundException If we encounter a unitID that doesn't exist
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void setOptionalFarmersInAllCities (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final PlayerServerDetails player, final CommonDatabase db, final MomSessionDescription sd)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// First find how many rations our current army needs
		int rationsNeeded = 0;
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
			{
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null, players, trueMap, db);
				rationsNeeded = rationsNeeded + xu.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
			}

		log.debug ("setOptionalFarmersInAllCities: Armies require " + rationsNeeded + " rations");

		// Then take off how many rations cities are producing even if they have zero optional farmers set
		// e.g. a size 1 city with a granary next to a wild game resource will produce +3 rations even with no farmers,
		// or a size 1 city with no resources must be a farmer, but he only eats 1 of the 2 rations he produces so this also gives +1
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
					{
						cityData.setOptionalFarmers (0);

						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane.getPlaneNumber ());

						rationsNeeded = rationsNeeded - getCityCalculations ().calculateSingleCityProduction (players, trueMap.getMap (),
							trueMap.getBuilding (), cityLocation, priv.getTaxRateID (), sd, true, db,
							CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
					}
				}

		log.debug ("setOptionalFarmersInAllCities: Armies require " + rationsNeeded + " rations after taking off what is provided by setting 0 optional farmers in all cities");

		// Farming rate for each farmer is doubled
		int doubleRationsNeeded = rationsNeeded * 2;
		log.debug ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after doubling");

		// Use farmers from cities producing trade goods first
		if (doubleRationsNeeded > 0)
			doubleRationsNeeded = findWorkersToConvertToFarmers (doubleRationsNeeded, true, trueMap, player, db, sd);

		log.debug ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after using up trade goods cities");

		if (doubleRationsNeeded > 0)
			doubleRationsNeeded = findWorkersToConvertToFarmers (doubleRationsNeeded, false, trueMap, player, db, sd);

		log.debug ("setOptionalFarmersInAllCities: Armies require " + doubleRationsNeeded + "/2 after using up other production cities");

		// Update each player's memorised view of this city with the new number of optional farmers, if they can see it
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane.getPlaneNumber ());

						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (trueMap.getMap (), players, cityLocation, sd.getFogOfWarSetting ());
					}
				}
	}

	/**
	 * AI player decides what to build in this city
	 *
	 * @param wizard Which wizard the AI player is controlling
	 * @param cityLocation Location of the city
	 * @param cityData True info on the city, so it can be updated
	 * @param numberOfCities Number of cities we own
	 * @param isUnitFactory Is this one of our unit factories? (i.e. one of our cities that can construct the best units we can currently make?)
	 * @param constructableHere Map of all units we could choose to construct, broken down by unit type
	 * @param wantedUnitTypes List of unit types we have a need to build 
	 * @param needForNewUnitsMod Modifier to add/substract to base "need for new units" value from XML DB
	 * @param knownTerrain Known overland terrain
	 * @param knownBuildings Known list of buildings
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 */
	@Override
	public final void decideWhatToBuild (final Wizard wizard, final MapCoordinates3DEx cityLocation, final OverlandMapCityData cityData,
		final int numberOfCities, final boolean isUnitFactory, final int needForNewUnitsMod, Map<AIUnitType, List<AIConstructableUnit>> constructableHere,
		final List<AIUnitType> wantedUnitTypes, final MapVolumeOfMemoryGridCells knownTerrain, final List<MemoryBuilding> knownBuildings,
		final MomSessionDescription sd, final CommonDatabase db) throws RecordNotFoundException
	{
		log.debug ("AI Player ID " + cityData.getCityOwnerID () + " deciding what to construct in " + (isUnitFactory ? "unit factory " : "city ") + cityLocation +
			(isUnitFactory ? ", needMod = " + needForNewUnitsMod : ""));
		
		// Always build a Granary - Marketplace - Farmer's Market first, regardless of anything else
		boolean decided = tryToConstructBuildingOfType (cityLocation, cityData, AiBuildingTypeID.GROWTH, knownTerrain, knownBuildings, sd, db);
		if (!decided)
		{
			// After that, depends on type of city and choose somewhat randomly
			final WeightedChoicesImpl<AICityConstructionType> choices = new WeightedChoicesImpl<AICityConstructionType> ();
			choices.setRandomUtils (getRandomUtils ());
			
			boolean anyBuildingsLeftToBuild = false;
			final Iterator<Building> iter = db.getBuilding ().iterator ();
			while ((!anyBuildingsLeftToBuild) && (iter.hasNext ()))
			{
				final Building building = iter.next ();
				
				if ((!building.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_HOUSING)) && (!building.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_TRADE_GOODS)) &&
					(getMemoryBuildingUtils ().findBuilding (knownBuildings, cityLocation, building.getBuildingID ()) == null) &&
					(getServerCityCalculations ().canEventuallyConstructBuilding (knownTerrain, knownBuildings, cityLocation, building, sd.getOverlandMapSize (), db)))
				{
					anyBuildingsLeftToBuild = true;
					log.debug ("AI Player ID " + cityData.getCityOwnerID () + " could eventually construct building " + building.getBuildingID () + " at " + cityLocation);
				}
			}			
			
			final StringBuilder debugChoices = new StringBuilder ();
			
			if ((wizard.getBuildingChance () > 0) && (anyBuildingsLeftToBuild))
			{
				choices.add (wizard.getBuildingChance (), AICityConstructionType.BUILDING);
				debugChoices.append (wizard.getBuildingChance () + ":Building");
			}
			
			// or we have only 1 city, in which case its our unit factory by definition
			final int needForNewUnits = wizard.getBaseNeedForNewUnits () + needForNewUnitsMod;
			if ((isUnitFactory) && (needForNewUnits > 0) && (constructableHere.containsKey (AIUnitType.COMBAT_UNIT)))
			{
				choices.add (needForNewUnits, AICityConstructionType.COMBAT_UNIT);
				
				if (debugChoices.length () > 0)
					debugChoices.append (", ");
				
				debugChoices.append (needForNewUnits + ":Unit");
			}
			
			// Don't waste our good unit factory building settlers if there's somewhere else that can do it
			if ((wizard.getSettlersChance () > 0) && (wantedUnitTypes != null) && (wantedUnitTypes.contains (AIUnitType.BUILD_CITY)) &&
				(constructableHere.containsKey (AIUnitType.BUILD_CITY)) && ((numberOfCities == 1) || (!isUnitFactory)))
			{
				choices.add (wizard.getSettlersChance (), AICityConstructionType.SETTLER);
				
				if (debugChoices.length () > 0)
					debugChoices.append (", ");

				debugChoices.append (wizard.getSettlersChance () + ":Settler");
			}
			
			// Engineers are very similar to settlers
			if ((wizard.getEngineersChance () > 0) && (wantedUnitTypes != null) && (wantedUnitTypes.contains (AIUnitType.BUILD_ROAD)) &&
				(constructableHere.containsKey (AIUnitType.BUILD_ROAD)) && ((numberOfCities == 1) || (!isUnitFactory)))
				{
					choices.add (wizard.getEngineersChance (), AICityConstructionType.ENGINEER);
					
					if (debugChoices.length () > 0)
						debugChoices.append (", ");

					debugChoices.append (wizard.getEngineersChance () + ":Engineer");
				}
			
			// Make random choice
			// It is possible to get null here (literally nothing to build) if its not a unit factory, nowhere new to build cities, and we have every building already made
			final AICityConstructionType constructionType = choices.nextWeightedValue ();
			log.debug ("AI Player ID " + cityData.getCityOwnerID () + " choices for construction types at " + cityLocation + " are " + debugChoices + ": chose " + constructionType);
			if (constructionType != null)
				switch (constructionType)
				{
					case BUILDING:
						// Set list of building priorities depending if this is a unit factory or not
						final List<AiBuildingTypeID> buildingTypes = new ArrayList<AiBuildingTypeID> ();
						if (isUnitFactory)
						{
							buildingTypes.add (AiBuildingTypeID.UNITS);		// Doesn't mean build units, it means "buildings that make units better", e.g. Fighters' Guild
							buildingTypes.add (AiBuildingTypeID.PRODUCTION);
							buildingTypes.add (AiBuildingTypeID.RESEARCH);
							buildingTypes.add (AiBuildingTypeID.UNREST_AND_MAGIC_POWER);
							buildingTypes.add (AiBuildingTypeID.NAVAL);
							buildingTypes.add (AiBuildingTypeID.GOLD);
							buildingTypes.add (AiBuildingTypeID.UNREST_WITHOUT_MAGIC_POWER);
							buildingTypes.add (AiBuildingTypeID.DEFENCE);
						}
						else
						{
							buildingTypes.add (AiBuildingTypeID.PRODUCTION);
							buildingTypes.add (AiBuildingTypeID.RESEARCH);
							buildingTypes.add (AiBuildingTypeID.UNREST_AND_MAGIC_POWER);
							buildingTypes.add (AiBuildingTypeID.GOLD);
							buildingTypes.add (AiBuildingTypeID.UNITS);
							buildingTypes.add (AiBuildingTypeID.NAVAL);
							buildingTypes.add (AiBuildingTypeID.UNREST_WITHOUT_MAGIC_POWER);
							buildingTypes.add (AiBuildingTypeID.DEFENCE);
						}
				
						final Iterator<AiBuildingTypeID> buildingTypesIter = buildingTypes.iterator ();
						while ((!decided) && (buildingTypesIter.hasNext ()))
						{
							final AiBuildingTypeID buildingType = buildingTypesIter.next ();
				
							if (tryToConstructBuildingOfType (cityLocation, cityData, buildingType, knownTerrain, knownBuildings, sd, db))
								decided = true;
						}
						break;
						
					case COMBAT_UNIT:
					{
						// The list has already been filtered to ensure it lists only units we have the necessary pre-requisite buildings to construct
						// and only units we can afford the maintenance of, so we can literally just pick one without worrying about it
						final List<AIConstructableUnit> matchingUnits = constructableHere.get (AIUnitType.COMBAT_UNIT);
						cityData.setCurrentlyConstructingBuildingID (null);
						cityData.setCurrentlyConstructingUnitID (matchingUnits.get (getRandomUtils ().nextInt (matchingUnits.size ())).getUnit ().getUnitID ());
						decided = true;
						break;
					}

					case SETTLER:
					{
						final List<AIConstructableUnit> matchingUnits = constructableHere.get (AIUnitType.BUILD_CITY);
						cityData.setCurrentlyConstructingBuildingID (null);
						cityData.setCurrentlyConstructingUnitID (matchingUnits.get (getRandomUtils ().nextInt (matchingUnits.size ())).getUnit ().getUnitID ());
						decided = true;
						break;
					}
					
					case ENGINEER:
					{
						final List<AIConstructableUnit> matchingUnits = constructableHere.get (AIUnitType.BUILD_ROAD);
						cityData.setCurrentlyConstructingBuildingID (null);
						cityData.setCurrentlyConstructingUnitID (matchingUnits.get (getRandomUtils ().nextInt (matchingUnits.size ())).getUnit ().getUnitID ());
						decided = true;
						break;
					}
				}
		}
		
		// If still found nothing to build, then resort to Trade Gooods
		if (!decided)
		{
			cityData.setCurrentlyConstructingBuildingID (CommonDatabaseConstants.BUILDING_TRADE_GOODS);
			cityData.setCurrentlyConstructingUnitID (null);
		}

		log.debug ("AI Player ID " + cityData.getCityOwnerID () + " set city at " + cityLocation + " to construct " +
			cityData.getCurrentlyConstructingBuildingID () + "/" + cityData.getCurrentlyConstructingUnitID ());
	}
	
	/**
	 * @param cityLocation Location of the city
	 * @param cityData True info on the city, so it can be updated
	 * @param buildingType Type of building we want to try to construct
	 * @param knownTerrain Known overland terrain
	 * @param knownBuildings Known list of buildings
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return Whether we found a suitable building to construct or not
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 */
	final boolean tryToConstructBuildingOfType (final MapCoordinates3DEx cityLocation, final OverlandMapCityData cityData, final AiBuildingTypeID buildingType,
		final MapVolumeOfMemoryGridCells knownTerrain, final List<MemoryBuilding> knownBuildings,
		 final MomSessionDescription sd, final CommonDatabase db) throws RecordNotFoundException
	{
		boolean decided = false;

		// List out all buildings, except those that:
		// a) are of the wrong type
		// b) we already have, or
		// c) our race cannot build
		// Keep buildings in the list even if we don't yet have the pre-requisites necessary to build them
		final List<Building> buildingOptions = new ArrayList<Building> ();
		for (final Building building : db.getBuilding ())
			if ((building.getAiBuildingTypeID () == buildingType) && (getMemoryBuildingUtils ().findBuilding (knownBuildings, cityLocation, building.getBuildingID ()) == null) &&
				(getServerCityCalculations ().canEventuallyConstructBuilding (knownTerrain, knownBuildings, cityLocation, building, sd.getOverlandMapSize (), db)))

				buildingOptions.add (building);

		// Now step through the list of possible buildings until we find one we have all the pre-requisites for
		// If we find one that we DON'T have all the pre-requisites for, then add the pre-requisites that we don't have but can build onto the end of the list
		// In this way we can decide that we want e.g. a Wizards' Guild and this repeating loop will work out all the buildings that we need to build first in order to get it

		// Use an index so we can add to the tail of the list as we go along
		int buildingIndex = 0;
		while ((!decided) && (buildingIndex < buildingOptions.size ()))
		{
			final Building thisBuilding = buildingOptions.get (buildingIndex);

			if (getMemoryBuildingUtils ().meetsBuildingRequirements (knownBuildings, cityLocation, thisBuilding))
			{
				// Got one we can decide upon
				cityData.setCurrentlyConstructingBuildingID (thisBuilding.getBuildingID ());
				cityData.setCurrentlyConstructingUnitID (null);
				decided = true;
			}
			else
			{
				// We want this, but we need to build something else first - find out what
				// Note we don't need to check whether each prerequisite can itself be constructed, or for the right tile types, because
				// canEventuallyConstructBuilding () above already checked all this over the entire prerequisite tree, so all we need to do is add them
				for (final String prereq : thisBuilding.getBuildingPrerequisite ())
				{
					final Building buildingPrereq = db.findBuilding (prereq, "decideWhatToBuild");
					if ((!buildingOptions.contains (buildingPrereq)) && (getMemoryBuildingUtils ().findBuilding (knownBuildings, cityLocation, buildingPrereq.getBuildingID ()) == null))
						buildingOptions.add (buildingPrereq);
				}

				buildingIndex++;
			}
		}
		
		return decided;
	}
	
	/**
	 * AI player tests every tax rate and chooses the best one
	 * 
	 * @param player Player we want to pick tax rate for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void decideTaxRate (final PlayerServerDetails player, final MomSessionVariables mom)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		String bestTaxRateID = null;
		Integer bestValue = null;
		
		for (final TaxRate taxRate : mom.getServerDB ().getTaxRate ())
		{
			getCityProcessing ().changeTaxRate (player, taxRate.getTaxRateID (), mom);
			
			final int goldPerTurn = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
			
			// Factor in how many rebels we have, otherwise the AI tends to pick really high tax rates which generate the most money but cripple production
			int numberOfRebels = 0;
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final OverlandMapCityData cityData = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
						if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
							numberOfRebels = numberOfRebels + cityData.getNumberOfRebels ();
					}
			
			final int rebelPenalty = numberOfRebels * 3;
			final int totalValue = goldPerTurn - rebelPenalty;
			
			log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " would generate " + goldPerTurn + " gold this turn - 3*" + numberOfRebels + " rebels = " + totalValue + " with tax rate ID " + taxRate.getTaxRateID ());
			
			if ((bestValue == null) || (totalValue > bestValue))
			{
				bestTaxRateID = taxRate.getTaxRateID ();
				bestValue = totalValue;
			}
		}
		
		if (bestTaxRateID != null)
		{
			log.debug ("AI player ID " + player.getPlayerDescription () + " choosing tax rate ID " + bestTaxRateID);
			getCityProcessing ().changeTaxRate (player, bestTaxRateID, mom);
		}
	}
	
	/**
	 * AI checks cities to see if they want to rush buy anything
	 * 
	 * @param player Player we want to check for rush buying
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If there is a problem with city calculations 
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void checkForRushBuying (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		final int goldPerTurn = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		final int goldStored = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		
		if ((goldPerTurn >= 2) && (goldStored >= 50))
		{
			// Search for cities where we could afford to rush buy whatever is being constructed, and find the one with the lowest production per turn
			Integer lowestProductionPerTurn = null;
			MapCoordinates3DEx bestCityLocation = null;
			Integer bestRushBuyCost = null;
			Integer bestProductionCost = null;
			
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final OverlandMapCityData cityData = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
						if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
						{
							final Integer productionCost;
							if (cityData.getCurrentlyConstructingBuildingID () != null)
							{
								final Building buildingDef = mom.getServerDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "checkForRushBuying");
								productionCost = buildingDef.getProductionCost ();
							}
							else if (cityData.getCurrentlyConstructingUnitID () != null)
							{
								final Unit unitDef = mom.getServerDB ().findUnit (cityData.getCurrentlyConstructingUnitID (), "checkForRushBuying");
								productionCost = unitDef.getProductionCost ();
							}
							else
								productionCost = null;
							
							// Does it even have a production cost?  (Can't rush buy Trade Goods)
							if (productionCost != null)
							{
								final int productionSoFar = (cityData.getProductionSoFar () == null) ? 0 : cityData.getProductionSoFar ();
								final int rushBuyCost = getCityCalculations ().goldToRushBuy (productionCost, productionSoFar);
								
								if (goldStored > rushBuyCost + 20)
								{
									// Found something we could rush buy - now how much production does this city generate by itself?
									final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
									final int thisProductionPerTurn = getCityCalculations ().calculateSingleCityProduction (mom.getPlayers (), priv.getFogOfWarMemory ().getMap (),
										priv.getFogOfWarMemory ().getBuilding (), cityLocation, priv.getTaxRateID (), mom.getSessionDescription (), true, mom.getServerDB (),
										CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
									
									if ((lowestProductionPerTurn == null) || (thisProductionPerTurn < lowestProductionPerTurn))
									{
										lowestProductionPerTurn = thisProductionPerTurn;
										bestCityLocation = cityLocation;
										bestRushBuyCost = rushBuyCost;
										bestProductionCost = productionCost;
									}
								}
							}
						}
					}
			
			// Did we find anywhere?
			if (bestCityLocation == null)
				log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " decided not to rush buy anything");
			else
			{
				log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " decided to rush buy project at " + bestCityLocation + " costing " + bestRushBuyCost + " gold");
				
				// Same as what RushBuyMessageImpl does for human players
				getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -bestRushBuyCost);

				final OverlandMapCityData cityData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
					(bestCityLocation.getZ ()).getRow ().get (bestCityLocation.getY ()).getCell ().get (bestCityLocation.getX ()).getCityData ();
				
				cityData.setProductionSoFar (bestProductionCost);

				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getPlayers (), bestCityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
			}
		}
	}
	
	/**
	 * @param playerID Player we want cities for
	 * @param plane Which plane we want cities on
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @return List of coordinates of all our cities
	 */
	@Override
	public final List<MapCoordinates2DEx> listOurCitiesOnPlane (final int playerID, final int plane, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys)
	{
		final List<MapCoordinates2DEx> list = new ArrayList<MapCoordinates2DEx> ();

		for (int y = 0; y < sys.getHeight (); y++)
			for (int x = 0; x < sys.getWidth (); x++)
			{
				final OverlandMapCityData cityData = terrain.getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getCityData ();
				if ((cityData != null) && (cityData.getCityOwnerID () == playerID))
					list.add (new MapCoordinates2DEx (x, y));
			}
		
		return list;
	}
	
	/**
	 * @param x X coordinate of location to search from
	 * @param y Y coordinate of location to search from
	 * @param ourCitiesOnPlane List output from listOurCitiesOnPlane
	 * @param sys Overland map coordinate system
	 * @return Distance to closest city; 0 if we have no cities on this plane or we are right on top of one
	 */
	@Override
	public final int findDistanceToClosestCity (final int x, final int y, final List<MapCoordinates2DEx> ourCitiesOnPlane, final CoordinateSystem sys)
	{
		Integer closestDistance = null;
		for (final MapCoordinates2DEx cityLocation : ourCitiesOnPlane)
		{
			final int thisDistance = getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, x, y, cityLocation.getX (), cityLocation.getY ());
			if ((closestDistance == null) || (thisDistance < closestDistance))
				closestDistance = thisDistance;
		}
		
		return (closestDistance == null) ? 0 : closestDistance;
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
	
	/**
	 * @return MemoryBuilding utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
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
	 * @return Server-only city calculations
	 */
	public final ServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final ServerCityCalculations calc)
	{
		serverCityCalculations = calc;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
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
	 * @return Server-only city utils
	 */
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
	}

	/**
	 * @return City processing methods
	 */
	public final CityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final CityProcessing obj)
	{
		cityProcessing = obj;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}
}