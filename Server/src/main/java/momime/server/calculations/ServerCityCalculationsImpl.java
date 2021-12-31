package momime.server.calculations;

import java.util.Iterator;
import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.Building;
import momime.common.database.CitySize;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.MomSessionVariables;
import momime.server.utils.CityServerUtils;

/**
 * Server only calculations pertaining to cities, e.g. calculating resources gathered from within the city radius
 */
public final class ServerCityCalculationsImpl implements ServerCityCalculations
{
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;

	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** City production calculations */
	private CityProductionCalculations cityProductionCalculations;
	
	/**
	 * Updates the city size ID and minimum number of farmers
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
	 * @param cityLocation Location of the city to update
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the player who owns the city
	 * @throws MomException If any of a number of expected items aren't found in the database
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 */
	@Override
	public final void calculateCitySizeIDAndMinimumFarmers (final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

		// First work out the Size ID - There should only be one entry in the DB which matches
		boolean found = false;
		final Iterator<CitySize> iter = mom.getServerDB ().getCitySize ().iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final CitySize thisSize = iter.next ();

			// 0 indicates that there is no minimum/maximum
			if (((thisSize.getCitySizeMinimum () == null) || (cityData.getCityPopulation () >= thisSize.getCitySizeMinimum ())) &&
				((thisSize.getCitySizeMaximum () == null) || (cityData.getCityPopulation () <= thisSize.getCitySizeMaximum ())))
			{
				cityData.setCitySizeID (thisSize.getCitySizeID ());
				found = true;
			}
		}

		if (!found)
			throw new MomException ("No city size ID is defined for cities of size " + cityData.getCityPopulation ());

		// Find how many rations the city produces even if it has 0 farmers
		final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityData.getCityOwnerID (), "calculateCitySizeIDAndMinimumFarmers");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();

		final CityProductionBreakdownsEx cityProductions = getCityProductionCalculations ().calculateAllCityProductions (mom.getPlayers (),
			mom.getGeneralServerKnowledge ().getTrueWizardDetails (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
			cityLocation, priv.getTaxRateID (), mom.getSessionDescription (), mom.getGeneralPublicKnowledge ().getConjunctionEventID (), false, false, mom.getServerDB ());
		
		// This is what the wiki calls "Base Food Level"
		final CityProductionBreakdown food = cityProductions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		final int foodProductionFromTerrainTiles = (food == null) ? 0 : food.getProductionAmountPlusPercentageBonus ();
		
		// Which buildings give rations for free?  2 separate totals, for before + after overfarming rule is applied
		final CityProductionBreakdown rations = cityProductions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		final int freeRationsBefore = (rations == null) ? 0 : rations.getProductionAmountMinusPercentagePenalty ();		// If famine in effect, has already been applied here
		final int freeRationsAfter = (rations == null) ? 0 : rations.getProductionAmountToAddAfterPercentages ();			// Famine doesn't apply to after values
		final int population = cityData.getCityPopulation () / 1000;
		final int rationsNeeded = population - freeRationsBefore - freeRationsAfter;

		// See if we need any farmers at all
		if (rationsNeeded <= 0)
			cityData.setMinimumFarmers (0);
		else
		{
			// Get the farming rate for this race.  If famine in effect, this will already have been halved.
			final int doubleFarmingRate = getCityServerUtils ().calculateDoubleFarmingRate (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), cityLocation, mom.getServerDB ());

			// Can work out value assuming every farmer is working at normal efficiency, but that might not be true, so this is optimistic
			int minimumFarmers = ((rationsNeeded * 2) + doubleFarmingRate - 1) / doubleFarmingRate;
			if (minimumFarmers >= population)
				minimumFarmers = population;
			else
			{
				// Check how many rations we'll actually get, taking overfarming rule into account, and keep adding 1 until we run out of civilians or have enough rations
				boolean done = false;
				while (!done)
				{
					int rationsProduced = freeRationsBefore + ((minimumFarmers * doubleFarmingRate) / 2);		// Round down.. only possible to get halves if Famine in effect
					if (rationsProduced > foodProductionFromTerrainTiles)
						rationsProduced = foodProductionFromTerrainTiles + ((rationsProduced - foodProductionFromTerrainTiles) / 2);
					
					rationsProduced = rationsProduced + freeRationsAfter;
					
					if (rationsProduced >= population)
						done = true;
					else
					{
						minimumFarmers++;
						if (minimumFarmers >= population)
							done = true;
					}
				}
			}
			
			cityData.setMinimumFarmers (minimumFarmers);
		}
	}

	/**
	 * Checks that minimum farmers + optional farmers + rebels is not > population
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
	 * @param city City data
	 * @throws MomException If we end up setting optional farmers to negative, which indicates that minimum farmers and/or rebels have been previously calculated incorrectly
	 */
	@Override
	public final void ensureNotTooManyOptionalFarmers (final OverlandMapCityData city)
		throws MomException
	{
		final boolean tooMany = (city.getMinimumFarmers () + city.getOptionalFarmers () + city.getNumberOfRebels () > city.getCityPopulation () / 1000);
		if (tooMany)
		{
			city.setOptionalFarmers ((city.getCityPopulation () / 1000) - city.getMinimumFarmers () - city.getNumberOfRebels ());

			if (city.getOptionalFarmers () < 0)
				throw new MomException ("EnsureNotTooManyOptionalFarmers set number of optional farmers to negative: " +
					city.getCityPopulation () + " population, " + city.getMinimumFarmers () + " minimum farmers, " + city.getNumberOfRebels () + " rebels");
		}
	}

	/**
	 * @param buildings Locked buildings list
	 * @param cityLocation Location of the city to test
	 * @param db Lookup lists built over the XML database
	 * @return -1 if this city has no buildings which give any particular bonus to sight range and so can see the regular resource pattern; 1 or above indicates a longer radius this city can 'see'
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	@Override
	public final int calculateCityScoutingRange (final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final CommonDatabase db) throws RecordNotFoundException
	{
		// Check all buildings at this location
		int result = -1;
		for (final MemoryBuilding thisBuilding : buildings)
			if (cityLocation.equals (thisBuilding.getCityLocation ()))
			{
				final Integer scoutingRange = db.findBuilding (thisBuilding.getBuildingID (), "calculateCityScoutingRange").getBuildingScoutingRange ();

				if (scoutingRange != null)
					result = Math.max (result, scoutingRange);
			}

		return result;
	}

	/**
	 * Checks whether we will eventually (possibly after constructing various other buildings first) be able to construct the specified building
	 * Human players construct buildings one at a time, but the AI routines look a lot further ahead, and so had issues where they AI would want to construct
	 * a Merchants' Guild, which requires a Bank, which requires a University, but oops our race can't build Universities
	 * So this routine is used by the AI to tell it, give up on the idea of ever constructing a Merchants' Guild because you'll never meet one of the lower requirements
	 *
	 * @param map Known terrain
	 * @param buildings List of buildings that have already been constructed
	 * @param cityLocation Location of the city to check
	 * @param building Cache for the building that we want to construct
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return True if the surrounding terrain has one of the tile type options that we need to construct this building
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or one of the buildings involved
	 */
	@Override
	public final boolean canEventuallyConstructBuilding (final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings,
		final MapCoordinates3DEx cityLocation, final Building building,
		final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		// Need to get the city race
		final OverlandMapCityData cityData = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		final Race race = db.findRace (cityData.getCityRaceID (), "canEventuallyConstructBuilding");

		// Check any direct blocks to us constructing this building
		boolean passes = getCityCalculations ().buildingPassesTileTypeRequirements (map, cityLocation, building, overlandMapCoordinateSystem);
		final Iterator<String> iter = race.getRaceCannotBuild ().iterator ();
		while ((passes) && (iter.hasNext ()))
			if (iter.next ().equals (building.getBuildingID ()))
				passes = false;

		// Recursively check whether we can meet the requirements of each prerequisite
		final Iterator<String> recursiveIter = building.getBuildingPrerequisite ().iterator ();
		while ((passes) && (recursiveIter.hasNext ()))
		{
			final Building thisBuilding = db.findBuilding (recursiveIter.next (), "canEventuallyConstructBuilding");

			// Don't check it is we've already got it - its possible, for example, for a sawmill to be built and then us lose the only forest tile, so while
			// we don't have the prerequisites for it anymore, we still have the building
			if (getMemoryBuildingUtils ().findBuilding (buildings, cityLocation, thisBuilding.getBuildingID ()) == null)
				
				// Summoning circle and Wizard fortress have themselves as pre-requisites to stop them ever being built
				if ((building.getBuildingID ().equals (thisBuilding.getBuildingID ())) ||
					(!canEventuallyConstructBuilding (map, buildings, cityLocation, thisBuilding, overlandMapCoordinateSystem, db)))
					
					passes = false;
		}

		return passes;
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
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
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
	 * @return City production calculations
	 */
	public final CityProductionCalculations getCityProductionCalculations ()
	{
		return cityProductionCalculations;
	}

	/**
	 * @param c City production calculations
	 */
	public final void setCityProductionCalculations (final CityProductionCalculations c)
	{
		cityProductionCalculations = c;
	}
}