package momime.server.utils;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.RaceCannotBuild;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.Race;
import momime.server.database.v0_9_4.Unit;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server side only helper methods for dealing with cities
 */
public final class CityServerUtilsImpl implements CityServerUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CityServerUtilsImpl.class.getName ());
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private MomCityCalculations cityCalculations;
	
	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Server-only city calculations */
	private MomServerCityCalculations serverCityCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Fog of war update methods */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/**
	 * Validates that a building or unit that we want to construct at a particular city is a valid choice
	 *
	 * @param player Player who wants to change construction
	 * @param trueMap True map details
	 * @param cityLocation Location where they want to set the construction project
	 * @param buildingOrUnitID The building or unit that we want to construct
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choices isn't acceptable
	 * @throws RecordNotFoundException If the race inhabiting the city cannot be found
	 */
	@Override
	public final String validateCityConstruction (final PlayerServerDetails player, final FogOfWarMemory trueMap, final OverlandMapCoordinatesEx cityLocation,
		final String buildingOrUnitID, final CoordinateSystem overlandMapCoordinateSystem, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (CityServerUtilsImpl.class.getName (), "validateCityConstruction", new String [] {new Integer (player.getPlayerDescription ().getPlayerID ()).toString (), buildingOrUnitID});

		final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		String msg = null;
		if ((cityData == null) || (cityData.getCityOwnerID () == null) || (!cityData.getCityOwnerID ().equals (player.getPlayerDescription ().getPlayerID ())))
			msg = "You tried to change the construction of a city which isn't yours - change ignored.";
		else
		{
			// Check if we're constructing a building or a unit
			Building building = null;
			try
			{
				building = db.findBuilding (buildingOrUnitID, "validateCityConstruction");
			}
			catch (final RecordNotFoundException e)
			{
				// Ignore, maybe its a unit
			}

			Unit unit = null;
			try
			{
				unit = db.findUnit (buildingOrUnitID, "validateCityConstruction");
			}
			catch (final RecordNotFoundException e)
			{
				// Ignore, maybe its a building
			}

			if (building != null)
			{
				// Check that location doesn't already have that building
				if (getMemoryBuildingUtils ().findBuilding (trueMap.getBuilding (), cityLocation, buildingOrUnitID))
					msg = "The city already has the type of building you're trying to build - change ignored.";
				else
				{
					final Race race = db.findRace (cityData.getCityRaceID (), "validateCityConstruction");

					// Check that the race inhabiting the city can build this building
					boolean cannotBuild = false;
					final Iterator<RaceCannotBuild> cannotBuildIter = race.getRaceCannotBuild ().iterator ();
					while ((!cannotBuild) && (cannotBuildIter.hasNext ()))
						if (cannotBuildIter.next ().getCannotBuildBuildingID ().equals (buildingOrUnitID))
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
				if (!unit.getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL))
					msg = "The unit you're trying to build cannot be built in cities - change ignored.";

				// Check unit is for the correct race
				else if ((unit.getUnitRaceID () == null) || (!unit.getUnitRaceID ().equals (cityData.getCityRaceID ())))
					msg = "This unit you're trying to build doesn't match the race inhabiting the city - change ignored.";

				// Check that we have any necessary pre-requisite buildings e.g. have to have a Barracks and a Blacksmith to be able to build Swordsmen
				else if (!getMemoryBuildingUtils ().meetsUnitRequirements (trueMap.getBuilding (), cityLocation, unit))
					msg = "This city doesn't have the necessary pre-requisite buildings for the unit you're trying to build - change ignored.";
			}
			else
				msg = "The building/unit that you tried to build doesn't exist";
		}

		log.exiting (CityServerUtilsImpl.class.getName (), "validateCityConstruction", msg);
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
	public final String validateOptionalFarmers (final PlayerServerDetails player, final MapVolumeOfMemoryGridCells trueTerrain, final OverlandMapCoordinatesEx cityLocation,
		final int optionalFarmers)
	{
		log.entering (CityServerUtilsImpl.class.getName (), "validateOptionalFarmers", new Integer [] {player.getPlayerDescription ().getPlayerID (), optionalFarmers});

		final OverlandMapCityData cityData = trueTerrain.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		String msg = null;
		if ((cityData == null) || (cityData.getCityOwnerID () == null) || (!cityData.getCityOwnerID ().equals (player.getPlayerDescription ().getPlayerID ())))
			msg = "You tried to change the number of farmers & workers in a city which isn't yours - change ignored.";

		else if ((optionalFarmers < 0) || (optionalFarmers + cityData.getMinimumFarmers () + cityData.getNumberOfRebels () > cityData.getCityPopulation () / 1000))
		{
			log.warning ("Player " + player.getPlayerDescription ().getPlayerID () + " tried to set an invalid number of optional farmers, O" +
				optionalFarmers + " + M" + cityData.getMinimumFarmers () + " +R" + cityData.getNumberOfRebels () + " > " + cityData.getCityPopulation () + "/1000");

			msg = "You tried to change the number of farmers & workers to an invalid amount - change ignored.";
		}

		log.exiting (CityServerUtilsImpl.class.getName (), "validateOptionalFarmers", msg);
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
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (CityServerUtilsImpl.class.getName (), "buildCityFromSettler", settler.getUnitURN ());
		
		// Add the city on the server
		final OverlandMapCoordinatesEx cityLocation = (OverlandMapCoordinatesEx) settler.getUnitLocation ();
		final MemoryGridCell tc = gsk.getTrueMap ().getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final Unit settlerUnit = db.findUnit (settler.getUnitID (), "buildCityFromSettler");
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (player.getPlayerDescription ().getPlayerID ());
		cityData.setCityPopulation (1000);
		cityData.setCityRaceID (settlerUnit.getUnitRaceID ());
		cityData.setCurrentlyConstructingBuildingOrUnitID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
		cityData.setCityName (getOverlandMapServerUtils ().generateCityName (gsk, db.findRace (cityData.getCityRaceID (), "buildCityFromSettler")));
		cityData.setOptionalFarmers (0);
		tc.setCityData (cityData);

		// Now city is created, can do initial calculations on it
		getServerCityCalculations ().calculateCitySizeIDAndMinimumFarmers (players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding (), cityLocation, sd, db);

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels (players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getUnit (), gsk.getTrueMap ().getBuilding (),
			cityLocation, priv.getTaxRateID (), db).getFinalTotal ());

		// Send city to anyone who can see it
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (gsk.getTrueMap ().getMap (), players, cityLocation, sd.getFogOfWarSetting (), true);
		
		// Kill off the settler
		getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (settler, KillUnitActionID.FREE, null, gsk.getTrueMap (), players, sd, db);
		
		// Update our own FOW (the city can see further than the settler could)
		getFogOfWarProcessing ().updateAndSendFogOfWar (gsk.getTrueMap (), player, players, false, "buildCityFromSettler", sd, db);
		
		log.exiting (CityServerUtilsImpl.class.getName (), "buildCityFromSettler");
	}
	
	/**
	 * @param cityLocation City location
	 * @param buildings List of known buildings
	 * @param db Lookup lists built over the XML database
	 * @return Total production cost of all buildings at this location
	 * @throws RecordNotFoundException If one of the buildings can't be found in the db
	 */
	@Override
	public final int totalCostOfBuildingsAtLocation (final OverlandMapCoordinatesEx cityLocation, final List<MemoryBuilding> buildings, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (CityServerUtilsImpl.class.getName (), "totalCostOfBuildingsAtLocation", cityLocation);
		
		int total = 0;
		for (final MemoryBuilding thisBuilding : buildings)
			if (cityLocation.equals (thisBuilding.getCityLocation ()))
			{
				final Building building = db.findBuilding (thisBuilding.getBuildingID (), "totalCostOfBuildingsAtLocation");
				if (building.getProductionCost () != null)
					total = total + building.getProductionCost ();
			}
		
		log.exiting (CityServerUtilsImpl.class.getName (), "totalCostOfBuildingsAtLocation", total);
		return total;
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
	public final MomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final MomCityCalculations calc)
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
	public final MomServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final MomServerCityCalculations calc)
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
}
