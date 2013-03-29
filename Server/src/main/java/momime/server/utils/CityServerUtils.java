package momime.server.utils;

import java.util.Iterator;
import java.util.logging.Logger;

import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.RaceCannotBuild;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.Race;
import momime.server.database.v0_9_4.Unit;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Server side only helper methods for dealing with cities
 */
public final class CityServerUtils
{
	/**
	 * Validates that a building or unit that we want to construct at a particular city is a valid choice
	 *
	 * @param player Player who wants to change construction
	 * @param trueMap True map details
	 * @param cityLocation Location where they want to set the construction project
	 * @param buildingOrUnitID The building or unit that we want to construct
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return null if choice is acceptable; message to send back to client if choices isn't acceptable
	 * @throws RecordNotFoundException If the race inhabiting the city cannot be found
	 */
	public static final String validateCityConstruction (final PlayerServerDetails player, final FogOfWarMemory trueMap, final OverlandMapCoordinates cityLocation,
		final String buildingOrUnitID, final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (CityServerUtils.class.getName (), "validateCityConstruction", new String [] {new Integer (player.getPlayerDescription ().getPlayerID ()).toString (), buildingOrUnitID});

		final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getPlane ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

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
				if (MemoryBuildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, buildingOrUnitID, debugLogger))
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
					else if (!MemoryBuildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, building, debugLogger))
						msg = "This city doesn't have the necessary pre-requisite buildings for the building you're trying to build - change ignored.";

					// Check if this building can only be built next to an ocean (shoreline) tile i.e. Ship Wrights' Guild
					else if (!MomCityCalculations.buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, building, sd.getMapSize (), debugLogger))
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
				else if (!MemoryBuildingUtils.meetsUnitRequirements (trueMap.getBuilding (), cityLocation, unit, debugLogger))
					msg = "This city doesn't have the necessary pre-requisite buildings for the unit you're trying to build - change ignored.";
			}
			else
				msg = "The building/unit that you tried to build doesn't exist";
		}

		debugLogger.exiting (CityServerUtils.class.getName (), "validateCityConstruction", msg);
		return msg;
	}

	/**
	 * Validates that a number of optional farmers we want to set a particular city to is a valid choice
	 *
	 * @param player Player who wants to set farmers
	 * @param trueMap True map details
	 * @param cityLocation Location where they want to set the farmers
	 * @param optionalFarmers The number of optional farmers we want
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return null if choice is acceptable; message to send back to client if choices isn't acceptable
	 */
	public static final String validateOptionalFarmers (final PlayerServerDetails player, final FogOfWarMemory trueMap, final OverlandMapCoordinates cityLocation,
		final int optionalFarmers, final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
	{
		debugLogger.entering (CityServerUtils.class.getName (), "validateOptionalFarmers", new Integer [] {player.getPlayerDescription ().getPlayerID (), optionalFarmers});

		final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getPlane ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		String msg = null;
		if ((cityData == null) || (cityData.getCityOwnerID () == null) || (!cityData.getCityOwnerID ().equals (player.getPlayerDescription ().getPlayerID ())))
			msg = "You tried to change the number of farmers & workers in a city which isn't yours - change ignored.";

		else if ((optionalFarmers < 0) || (optionalFarmers + cityData.getMinimumFarmers () + cityData.getNumberOfRebels () > cityData.getCityPopulation () / 1000))
		{
			debugLogger.warning ("Player " + player.getPlayerDescription ().getPlayerID () + " tried to set an invalid number of optional farmers, O" +
				optionalFarmers + " + M" + cityData.getMinimumFarmers () + " +R" + cityData.getNumberOfRebels () + " > " + cityData.getCityPopulation () + "/1000");

			msg = "You tried to change the number of farmers & workers to an invalid amount - change ignored.";
		}

		debugLogger.exiting (CityServerUtils.class.getName (), "validateOptionalFarmers", msg);
		return msg;
	}

	/**
	 * Prevent instantiation
	 */
	private CityServerUtils ()
	{
	}
}
