package momime.server.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.calculations.UnitCalculations;
import momime.common.calculations.UnitMovement;
import momime.common.calculations.UnitStack;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Plane;
import momime.common.database.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.ServerCityCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Server side only helper methods for dealing with cities
 */
public final class CityServerUtilsImpl implements CityServerUtils
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (CityServerUtilsImpl.class);
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Server-only city calculations */
	private ServerCityCalculations serverCityCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Fog of war update methods */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Methods dealing with unit movement */
	private UnitMovement unitMovement;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * @param location Location we we base our search from
	 * @param map Known terrain
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @return Distance to closest city on the same plane as location; null if there are no cities on this plane yet
	 */
	@Override
	public final Integer findClosestCityTo (final MapCoordinates3DEx location, final MapVolumeOfMemoryGridCells map, final CoordinateSystem overlandMapCoordinateSystem)
	{
		Integer closestDistance = null;
		
		for (int x = 0; x < overlandMapCoordinateSystem.getWidth (); x++)
			for (int y = 0; y < overlandMapCoordinateSystem.getHeight (); y++)
				if (map.getPlane ().get (location.getZ ()).getRow ().get (y).getCell ().get (x).getCityData () != null)
				{
					final int thisDistance = getCoordinateSystemUtils ().determineStep2DDistanceBetween (overlandMapCoordinateSystem, location.getX (), location.getY (), x, y);
					if ((closestDistance == null) || (thisDistance < closestDistance))
						closestDistance = thisDistance;
				}

		return closestDistance;
	}
	
	/**
	 * Validates that a building or unit that we want to construct at a particular city is a valid choice
	 *
	 * @param player Player who wants to change construction
	 * @param trueMap True map details
	 * @param cityLocation Location where they want to set the construction project
	 * @param buildingID The building that we want to construct
	 * @param unitID The unit that we want to construct
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choices isn't acceptable
	 * @throws RecordNotFoundException If the race inhabiting the city cannot be found
	 */
	@Override
	public final String validateCityConstruction (final PlayerServerDetails player, final FogOfWarMemory trueMap, final MapCoordinates3DEx cityLocation,
		final String buildingID, final String unitID, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		String msg = null;
		if ((cityData == null) || (cityData.getCityOwnerID () != player.getPlayerDescription ().getPlayerID ()))
			msg = "You tried to change the construction of a city which isn't yours - change ignored.";
		else
		{
			// Check if we're constructing a building or a unit
			Building building = null;
			if (buildingID != null)
				building = db.findBuilding (buildingID, "validateCityConstruction");

			Unit unit = null;
			if (unitID != null)
				unit = db.findUnit (unitID, "validateCityConstruction");

			if (building != null)
			{
				// Check that location doesn't already have that building
				if (getMemoryBuildingUtils ().findBuilding (trueMap.getBuilding (), cityLocation, buildingID) != null)
					msg = "The city already has the type of building you're trying to build - change ignored.";
				else
				{
					final Race race = db.findRace (cityData.getCityRaceID (), "validateCityConstruction");

					// Check that the race inhabiting the city can build this building
					boolean cannotBuild = false;
					final Iterator<String> cannotBuildIter = race.getRaceCannotBuild ().iterator ();
					while ((!cannotBuild) && (cannotBuildIter.hasNext ()))
						if (cannotBuildIter.next ().equals (buildingID))
							cannotBuild = true;

					if (cannotBuild)
						msg = "The race inhabiting this city cannot build the type of building requested - change ignored.";

					// Check if this building has any pre-requisites e.g. to build a Farmer's Market we have to have a granary
					else if (!getMemoryBuildingUtils ().meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, building))
						msg = "This city doesn't have the necessary pre-requisite buildings for the building you're trying to build - change ignored.";

					// Check if this building can only be built next to an ocean (shoreline) tile i.e. Ship Wrights' Guild
					else if (!getCityCalculations ().buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, building, overlandMapCoordinateSystem))
						msg = "That building can only be built when there is a certain tile type close to the city - change ignored.";
				}
			}
			else if (unit != null)
			{
				// Check that the unit is a normal unit (not hero or summoned)
				if (!unit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL))
					msg = "The unit you're trying to build cannot be built in cities - change ignored.";

				// Check unit is for the correct race
				else if ((unit.getUnitRaceID () != null) && (!unit.getUnitRaceID ().equals (cityData.getCityRaceID ())))
					msg = "This unit you're trying to build doesn't match the race inhabiting the city - change ignored.";

				// Check that we have any necessary pre-requisite buildings e.g. have to have a Barracks and a Blacksmith to be able to build Swordsmen
				else if (!getMemoryBuildingUtils ().meetsUnitRequirements (trueMap.getBuilding (), cityLocation, unit))
					msg = "This city doesn't have the necessary pre-requisite buildings for the unit you're trying to build - change ignored.";
			}
			else
				msg = "The building/unit that you tried to build doesn't exist";
		}

		return msg;
	}

	/**
	 * Validates that a number of optional farmers we want to set a particular city to is a valid choice
	 *
	 * @param player Player who wants to set farmers
	 * @param trueTerrain True terrain details
	 * @param cityLocation Location where they want to set the farmers
	 * @param optionalFarmers The number of optional farmers we want
	 * @return null if choice is acceptable; message to send back to client if choices isn't acceptable
	 */
	@Override
	public final String validateOptionalFarmers (final PlayerServerDetails player, final MapVolumeOfMemoryGridCells trueTerrain, final MapCoordinates3DEx cityLocation,
		final int optionalFarmers)
	{
		final OverlandMapCityData cityData = trueTerrain.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		String msg = null;
		if ((cityData == null) || (cityData.getCityOwnerID () != player.getPlayerDescription ().getPlayerID ()))
			msg = "You tried to change the number of farmers & workers in a city which isn't yours - change ignored.";

		else if ((optionalFarmers < 0) || (optionalFarmers + cityData.getMinimumFarmers () + cityData.getNumberOfRebels () > cityData.getCityPopulation () / 1000))
		{
			log.warn ("Player " + player.getPlayerDescription ().getPlayerID () + " tried to set an invalid number of optional farmers, O" +
				optionalFarmers + " + M" + cityData.getMinimumFarmers () + " +R" + cityData.getNumberOfRebels () + " > " + cityData.getCityPopulation () + "/1000");

			msg = "You tried to change the number of farmers & workers to an invalid amount - change ignored.";
		}

		return msg;
	}

	/**
	 * @param gsk Server knowledge data structure
	 * @param player The player who owns the settler
	 * @param settler The settler being converted into a city
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void buildCityFromSettler (final MomGeneralServerKnowledge gsk, final PlayerServerDetails player, final MemoryUnit settler,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Add the city on the server
		final MapCoordinates3DEx cityLocation = (MapCoordinates3DEx) settler.getUnitLocation ();
		final MemoryGridCell tc = gsk.getTrueMap ().getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final Unit settlerUnit = db.findUnit (settler.getUnitID (), "buildCityFromSettler");
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (player.getPlayerDescription ().getPlayerID ());
		cityData.setCityPopulation (1000);
		cityData.setCityRaceID (settlerUnit.getUnitRaceID ());
		cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
		cityData.setCityName (getOverlandMapServerUtils ().generateCityName (gsk, db.findRace (cityData.getCityRaceID (), "buildCityFromSettler")));
		cityData.setOptionalFarmers (0);
		tc.setCityData (cityData);
		
		// Cities automatically get a road
		final Plane plane = db.findPlane (cityLocation.getZ (), "buildCityFromSettler");
		final String roadTileTypeID = ((plane.isRoadsEnchanted () != null) && (plane.isRoadsEnchanted ())) ?
			CommonDatabaseConstants.TILE_TYPE_ENCHANTED_ROAD : CommonDatabaseConstants.TILE_TYPE_NORMAL_ROAD;

		tc.getTerrainData ().setRoadTileTypeID (roadTileTypeID);

		// Now city is created, can do initial calculations on it
		getServerCityCalculations ().calculateCitySizeIDAndMinimumFarmers (players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding (), cityLocation, sd, db);

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels (players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getUnit (), gsk.getTrueMap ().getBuilding (),
			cityLocation, priv.getTaxRateID (), db).getFinalTotal ());

		// Send city to anyone who can see it
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (gsk.getTrueMap ().getMap (), players, cityLocation, sd.getFogOfWarSetting ());
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (gsk.getTrueMap ().getMap (), players, cityLocation, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
		
		// Kill off the settler
		getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (settler, KillUnitActionID.PERMANENT_DAMAGE, gsk.getTrueMap (), players, sd.getFogOfWarSetting (), db);
		
		// Update our own FOW (the city can see further than the settler could)
		getFogOfWarProcessing ().updateAndSendFogOfWar (gsk.getTrueMap (), player, players, "buildCityFromSettler", sd, db);
	}
	
	/**
	 * @param cityLocation City location
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @return Total production cost of all buildings at this location
	 * @throws RecordNotFoundException If one of the buildings can't be found in the db
	 */
	@Override
	public final int totalCostOfBuildingsAtLocation (final MapCoordinates3DEx cityLocation, final List<MemoryBuilding> buildings, final CommonDatabase db)
		throws RecordNotFoundException
	{
		int total = 0;
		for (final MemoryBuilding thisBuilding : buildings)
			if (cityLocation.equals (thisBuilding.getCityLocation ()))
			{
				final Building building = db.findBuilding (thisBuilding.getBuildingID (), "totalCostOfBuildingsAtLocation");
				if (building.getProductionCost () != null)
					total = total + building.getProductionCost ();
			}
		
		return total;
	}
	
	/**
	 * @param searchLocation Map location to search around
	 * @param trueTerrain Terrain to search
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @return Location of a city that pulls in requested tile as one of its resource locations; null if there is no city closeby
	 */
	@Override
	public final MapCoordinates3DEx findCityWithinRadius (final MapCoordinates3DEx searchLocation, final MapVolumeOfMemoryGridCells trueTerrain,
		final CoordinateSystem overlandMapCoordinateSystem)
	{
		MapCoordinates3DEx found = null;
		
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (searchLocation);
		for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if ((found == null) && (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ())))
				if (trueTerrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getCityData () != null)
					found = coords;
		
		return found;
	}

	/**
	 * @param terrain Terrain to search
	 * @param playerID Player whose cities to look for
	 * @return Number of cities the player has
	 */
	@Override
	public final int countCities (final MapVolumeOfMemoryGridCells terrain, final int playerID)
	{
		int numberOfCities = 0;
		for (final MapAreaOfMemoryGridCells plane : terrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapCityData terrainData = cell.getCityData ();
					if ((terrainData != null) && (terrainData.getCityOwnerID () == playerID))
						numberOfCities++;
				}
		
		return numberOfCities;
	}
	
	/**
	 * Attempts to find all the cells that we need to build a road on in order to join up two cities.  We don't know that its actually possible yet - maybe they're on two different islands.
	 * If we fail to create a road, that's fine, the method just exits with an empty list, it isn't an error.
	 * 
	 * @param firstCityLocation Location of first city
	 * @param secondCityLocation Location of second city
	 * @param playerID Player who owns the cities
	 * @param players List of players in this session
	 * @param fogOfWarMemory Known terrain, buildings, spells and so on
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return List of map cells where we need to add road
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public final List<MapCoordinates3DEx> listMissingRoadCellsBetween (final MapCoordinates3DEx firstCityLocation, final MapCoordinates3DEx secondCityLocation, final int playerID,
		final List<PlayerServerDetails> players, final FogOfWarMemory fogOfWarMemory, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Don't just create a straight line - what's the shortest distance for a basic unit like a spearman to walk from one city to the other, going around mountains for example?
		final int [] [] [] doubleMovementDistances			= new int [sd.getOverlandMapSize ().getDepth ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
		final int [] [] [] movementDirections					= new int [sd.getOverlandMapSize ().getDepth ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
		final boolean [] [] [] canMoveToInOneTurn			= new boolean [sd.getOverlandMapSize ().getDepth ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
		final boolean [] [] [] movingHereResultsInAttack	= new boolean [sd.getOverlandMapSize ().getDepth ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];

		final AvailableUnit dummyUnit = new AvailableUnit ();
		dummyUnit.setUnitID (CommonDatabaseConstants.UNIT_ID_EXAMPLE);
		getUnitUtils ().initializeUnitSkills (dummyUnit, 0, db);		// otherwise it does not even get the "walking" skill
		
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		selectedUnits.add (getUnitUtils ().expandUnitDetails (dummyUnit, null, null, null, players, fogOfWarMemory, db));
		
		final UnitStack unitStack = getUnitCalculations ().createUnitStack (selectedUnits, players, fogOfWarMemory, db);
		
		getUnitMovement ().calculateOverlandMovementDistances (firstCityLocation.getX (), firstCityLocation.getY (), firstCityLocation.getZ (),
			playerID, fogOfWarMemory, unitStack, 0, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, players, sd, db);
		
		final List<MapCoordinates3DEx> missingRoadCells = new ArrayList<MapCoordinates3DEx> ();
		if (doubleMovementDistances [secondCityLocation.getZ ()] [secondCityLocation.getY ()] [secondCityLocation.getX ()] >= 0)
		{
			// Trace route between the two cities
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (secondCityLocation);
			while (!coords.equals (firstCityLocation))
			{
				final int d = getCoordinateSystemUtils ().normalizeDirection (sd.getOverlandMapSize ().getCoordinateSystemType (),
					movementDirections [coords.getZ ()] [coords.getY ()] [coords.getX ()] + 4);
				
				if (!getCoordinateSystemUtils ().move3DCoordinates (sd.getOverlandMapSize (), coords, d))
					throw new MomException ("listMissingRoadCellsBetween: Road tracing moved to a cell off the map");
				
				if (!coords.equals (firstCityLocation))
				{
					final OverlandMapTerrainData terrainData = fogOfWarMemory.getMap ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if ((terrainData != null) && (terrainData.getRoadTileTypeID () == null))
						missingRoadCells.add (new MapCoordinates3DEx (coords));
				}
			}
		}
		
		return missingRoadCells;
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
	 * @return Server-only overland map utils
	 */
	public final OverlandMapServerUtils getOverlandMapServerUtils ()
	{
		return overlandMapServerUtils;
	}
	
	/**
	 * @param utils Server-only overland map utils
	 */
	public final void setOverlandMapServerUtils (final OverlandMapServerUtils utils)
	{
		overlandMapServerUtils = utils;
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
	 * @return Fog of war update methods
	 */
	public final FogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Fog of war update methods
	 */
	public final void setFogOfWarProcessing (final FogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}
	
	/**
	 * @return Methods dealing with unit movement
	 */
	public final UnitMovement getUnitMovement ()
	{
		return unitMovement;
	}

	/**
	 * @param u Methods dealing with unit movement
	 */
	public final void setUnitMovement (final UnitMovement u)
	{
		unitMovement = u;
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
}