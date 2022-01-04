package momime.server.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.Building;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Plane;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.RaceEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.TaxRate;
import momime.common.database.Unit;
import momime.common.database.UnitEx;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageConstructBuilding;
import momime.common.messages.NewTurnMessageConstructUnit;
import momime.common.messages.NewTurnMessageDestroyBuilding;
import momime.common.messages.NewTurnMessagePopulationChange;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.PendingSaleMessage;
import momime.common.messages.servertoclient.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.TaxRateChangedMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.TreasureRewardMessage;
import momime.common.messages.servertoclient.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.messages.servertoclient.UpdateWizardStateMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.CityAI;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.utils.CityServerUtils;
import momime.server.utils.KnownWizardServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for any significant message processing to do with cities that isn't done in the message implementations
 */
public final class CityProcessingImpl implements CityProcessing
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CityProcessingImpl.class);
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;

	/** Server-only city calculations */
	private ServerCityCalculations serverCityCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Server-only pick utils */
	private PlayerPickServerUtils playerPickServerUtils;
	
	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** AI decisions about cities */
	private CityAI cityAI;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;

	/** Spell queueing methods */
	private SpellQueueing spellQueueing;
	
	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/**
	 * Creates the starting cities for each Wizard and Raiders
	 *
	 * This happens BEFORE we initialize each players' fog of war (of course... without their cities they wouldn't be able to see much of the map!)
	 * and so we don't need to send any messages out to anyone here, whether to add the city itself, buildings or units - just add everything to the true map
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
 	 * @throws MomException If no races are defined for a particular plane
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void createStartingCities (final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// Allocate a race to each continent of land for raider cities
		final MapArea3D<String> continentalRace = getOverlandMapServerUtils ().decideAllContinentalRaces
			(mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());

		// Now create cities for each player
		for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
		{
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
				(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), thisPlayer.getPlayerDescription ().getPlayerID (), "createStartingCities");
			
			// How many cities?
			final int numberOfCities;
			if (getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ()))
				numberOfCities = 1;

			else if (wizardDetails.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS))
				numberOfCities = mom.getSessionDescription ().getOverlandMapSize ().getRaiderCityCount ();

			else
				numberOfCities = 0;	// For monsters

			for (int cityNo = 0; cityNo < numberOfCities; cityNo++)
			{
				final int plane;
				if (getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ()))
					plane = getPlayerPickServerUtils ().startingPlaneForWizard (wizardDetails.getPick (), mom.getServerDB ());
				else
					// Raiders just pick a random plane
					plane = mom.getServerDB ().getPlane ().get (getRandomUtils ().nextInt (mom.getServerDB ().getPlane ().size ())).getPlaneNumber ();

				// Pick location
				final MapCoordinates3DEx cityLocation = getCityAI ().chooseCityLocation (mom.getGeneralServerKnowledge ().getTrueMap (),
					plane, true, mom, "Starter city for \"" + thisPlayer.getPlayerDescription ().getPlayerName () + "\"");
				if (cityLocation == null)
					throw new MomException ("createStartingCities: Can't find starting city location for player \"" + thisPlayer.getPlayerDescription ().getPlayerName () + "\" on plane " + plane);

				final ServerGridCellEx cityCell = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(plane).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
				final OverlandMapCityData city = new OverlandMapCityData ();
				cityCell.setCityData (city);

				// Set the city race and population
				city.setCityOwnerID (thisPlayer.getPlayerDescription ().getPlayerID ());
				city.setOptionalFarmers (0);

				if (getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ()))
				{
					final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) thisPlayer.getTransientPlayerPrivateKnowledge ();

					city.setCityPopulation (mom.getSessionDescription ().getDifficultyLevel ().getWizardCityStartSize () * 1000);
					city.setCityRaceID (priv.getFirstCityRaceID ());
				}
				else
				{
					// Randomize population of raider cities
					city.setCityPopulation ((mom.getSessionDescription ().getDifficultyLevel ().getRaiderCityStartSizeMin () +
						getRandomUtils ().nextInt (mom.getSessionDescription ().getDifficultyLevel ().getRaiderCityStartSizeMax () - mom.getSessionDescription ().getDifficultyLevel ().getRaiderCityStartSizeMin () + 1)) * 1000);

					// Raiders have a special population cap that prevents cities expanding by more than a certain value
					// See strategy guide p426
					cityCell.setRaiderCityAdditionalPopulationCap (city.getCityPopulation () + (mom.getSessionDescription ().getDifficultyLevel ().getRaiderCityGrowthCap () * 1000));

					// Have a good chance of just picking the continental race ID
					if (getRandomUtils ().nextInt (100) < mom.getSessionDescription ().getOverlandMapSize ().getContinentalRaceChance ())
					{
						final String raceID = continentalRace.get (cityLocation);
						if (raceID == null)
							throw new MomException ("createStartingCities: Tried to create Raider city with Continental race, but this tile has no continental race");

						city.setCityRaceID (raceID);
					}
					else
						// Pick totally random race
						city.setCityRaceID (getPlayerPickServerUtils ().chooseRandomRaceForPlane (plane, mom.getServerDB ()));
				}

				// Pick a name for the city
				city.setCityName (getOverlandMapServerUtils ().generateCityName (mom.getGeneralServerKnowledge (), mom.getServerDB ().findRace (city.getCityRaceID (), "createStartingCities")));

				// Cities get a free road, even if it isn't connected to anything yet
				final Plane roadPlane = mom.getServerDB ().findPlane (cityLocation.getZ (), "createStartingCities");
				final String roadTileTypeID = ((roadPlane.isRoadsEnchanted () != null) && (roadPlane.isRoadsEnchanted ())) ?
					CommonDatabaseConstants.TILE_TYPE_ENCHANTED_ROAD : CommonDatabaseConstants.TILE_TYPE_NORMAL_ROAD;
				
				cityCell.getTerrainData ().setRoadTileTypeID (roadTileTypeID);
				
				// Do initial calculations on the city
				getServerCityCalculations ().calculateCitySizeIDAndMinimumFarmers (cityLocation, mom);

				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();

				city.setNumberOfRebels (getCityCalculations ().calculateCityRebels (mom.getGeneralServerKnowledge ().getTrueMap (),
					cityLocation, priv.getTaxRateID (), mom.getServerDB ()).getFinalTotal ());

				getServerCityCalculations ().ensureNotTooManyOptionalFarmers (city);

				// Set default production
				city.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);

				// Add starting buildings
				if (getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ()))
				{
					// Wizards always get the same buildings (this also adds their Fortress & Summoning Circle)
					for (final Building thisBuilding : mom.getServerDB ().getBuilding ())
						if ((thisBuilding.isInWizardsStartingCities () != null) && (thisBuilding.isInWizardsStartingCities ()))
							getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (cityLocation, Arrays.asList (thisBuilding.getBuildingID ()), null, null, false, mom);
				}
				else
				{
					// Raiders buildings' depend on the city size
					final RaceEx race = mom.getServerDB ().findRace (city.getCityRaceID (), "createStartingCities");
					
					for (final Building thisBuilding : mom.getServerDB ().getBuilding ())
						if ((thisBuilding.getInRaidersStartingCitiesWithPopulationAtLeast () != null) &&
							(city.getCityPopulation () >= thisBuilding.getInRaidersStartingCitiesWithPopulationAtLeast () * 1000))
						{
							// Make sure the race of the city is actually allowed this kind of building
							boolean ok = true;
							final Iterator<String> cannotBuildIter = race.getRaceCannotBuild ().iterator ();
							while ((ok) && (cannotBuildIter.hasNext ()))
								if (cannotBuildIter.next ().equals (thisBuilding.getBuildingID ()))
									ok = false;
							
							if (ok)
								getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (cityLocation, Arrays.asList (thisBuilding.getBuildingID ()), null, null, false, mom);
						}
				}

				// Add starting units
				for (final UnitEx thisUnit : mom.getServerDB ().getUnits ())
					if ((thisUnit.getUnitRaceID () != null) && (thisUnit.getFreeAtStartCount () != null) && (thisUnit.getUnitRaceID ().equals (city.getCityRaceID ())))
						for (int freeAtStart = 0; freeAtStart < thisUnit.getFreeAtStartCount (); freeAtStart++)
						{
							final MapCoordinates3DEx unitCoords = new MapCoordinates3DEx (cityLocation);
							getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (thisUnit.getUnitID (), unitCoords, cityLocation, null, null, thisPlayer, UnitStatusID.ALIVE, false, mom);
						}
			}
			
			// Connect roads between starter cities owned by this player
			if (numberOfCities > 1)
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					createStartingRoads (thisPlayer.getPlayerDescription ().getPlayerID (), z, mom);
		}
	}

	/**
	 * Attempts to find all the cells that we need to build a road on in order to join up all cities owned by a particular player on a particular plane.
	 * 
	 * @param playerID Player who owns the cities
	 * @param plane Plane to check cities on
	 * @param maximumSeparation Connect cities who are at most this distance apart; null = connect all cities regardless of how far apart they are
	 * @param fogOfWarMemory Known terrain, buildings, spells and so on
	 * 	When called during map creation to create the initial roads between raider cities, this is the true map; when called for AI players using engineers, this is only what that player knows
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of map cells where we need to add road
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public final List<MapCoordinates3DEx> listMissingRoadCells (final int playerID, final int plane, final Integer maximumSeparation,
		final FogOfWarMemory fogOfWarMemory, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final List<MapCoordinates3DEx> citiesOnThisPlane = new ArrayList<MapCoordinates3DEx> ();
		for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
			for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
			{
				final MemoryGridCell mc = fogOfWarMemory.getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x);
				final OverlandMapCityData cityData = mc.getCityData ();
				if ((cityData != null) && (cityData.getCityOwnerID () == playerID))
					citiesOnThisPlane.add (new MapCoordinates3DEx (x, y, plane));
			}
		
		// Check every pair of cities to see which are close
		final List<MapCoordinates3DEx> missingRoadCells = new ArrayList<MapCoordinates3DEx> ();
		for (int firstCityNumber = 0; firstCityNumber < citiesOnThisPlane.size () - 1; firstCityNumber++)
		{
			final MapCoordinates3DEx firstCityLocation = citiesOnThisPlane.get (firstCityNumber);
			for (int secondCityNumber = firstCityNumber + 1; secondCityNumber < citiesOnThisPlane.size (); secondCityNumber++)
			{
				final MapCoordinates3DEx secondCityLocation = citiesOnThisPlane.get (secondCityNumber);
				if ((maximumSeparation == null) || (getCoordinateSystemUtils ().determineStep2DDistanceBetween
					(mom.getSessionDescription ().getOverlandMapSize (), firstCityLocation, secondCityLocation) <= maximumSeparation))
				{
					final List<MapCoordinates3DEx> newCells = getCityServerUtils ().listMissingRoadCellsBetween (firstCityLocation, secondCityLocation, playerID, fogOfWarMemory, mom);
					for (final MapCoordinates3DEx coords : newCells)
						if (!missingRoadCells.contains (coords))
							missingRoadCells.add (coords);
				}
			}
		}
		
		return missingRoadCells;
	}
	
	/**
	 * Creates all starter roads between raider cities on one plane.
	 * 
	 * @param playerID Player who owns the cities
	 * @param plane Plane to create roads on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	private final void createStartingRoads (final int playerID, final int plane, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final List<MapCoordinates3DEx> missingRoadCells = listMissingRoadCells (playerID, plane, CommonDatabaseConstants.CITY_SEPARATION_TO_GET_STARTER_ROADS,
			mom.getGeneralServerKnowledge ().getTrueMap (), mom);
		if (missingRoadCells.size () > 0)
		{
			final Plane planeDef = mom.getServerDB ().findPlane (plane, "createStartingRoads");
			final String roadTileTypeID = ((planeDef.isRoadsEnchanted () != null) && (planeDef.isRoadsEnchanted ())) ?
				CommonDatabaseConstants.TILE_TYPE_ENCHANTED_ROAD : CommonDatabaseConstants.TILE_TYPE_NORMAL_ROAD;
			
			// This is happening prior to anybody's initial FOW being calculated, so we're fine just to update the trueMap directly and not worry about who can see the change
			for (final MapCoordinates3DEx coords : missingRoadCells)
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setRoadTileTypeID (roadTileTypeID);
		}
	}
	
	/**
	 * All cities progress a little towards construction projects
	 *
	 * @param onlyOnePlayerID If zero, will process grow cities + progress construction for all players; if specified will do so only for the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void progressConstructionProjects (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && ((onlyOnePlayerID == 0) || (onlyOnePlayerID == cityData.getCityOwnerID ())) && (cityData.getCityPopulation () >= 1000))
					{
						final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityData.getCityOwnerID (), "progressConstructionProjects");
						final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();

						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);

						// Use calculated values to determine construction rate
						if ((cityData.getCurrentlyConstructingBuildingID () != null) || (cityData.getCurrentlyConstructingUnitID () != null))
						{
							// Check if we're constructing a building or a unit
							Building building = null;
							Integer productionCost = null;
							if (cityData.getCurrentlyConstructingBuildingID () != null)
							{
								building = mom.getServerDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "progressConstructionProjects");
								productionCost = building.getProductionCost ();
							}

							Unit unit = null;
							if (cityData.getCurrentlyConstructingUnitID () != null)
							{
								unit = mom.getServerDB ().findUnit (cityData.getCurrentlyConstructingUnitID (), "progressConstructionProjects");
								productionCost = unit.getProductionCost ();
							}

							if (productionCost != null)
							{
								final int productionThisTurn = getCityCalculations ().calculateSingleCityProduction (mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
									cityLocation, priv.getTaxRateID (), mom.getSessionDescription (), mom.getGeneralPublicKnowledge ().getConjunctionEventID (), true,
									mom.getServerDB (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
								
								if (productionThisTurn > 0)
								{
									cityData.setProductionSoFar (((cityData.getProductionSoFar () == null) ? 0 : cityData.getProductionSoFar ()) + productionThisTurn);

									// Is it finished?
									if (cityData.getProductionSoFar () >= productionCost)
									{
										// Did we construct a building?
										if (building != null)
										{
											// Current building is now finished
											cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);											
											cityData.setProductionSoFar (0);

											// Show on new turn messages for the player who built it
											if (cityOwner.getPlayerDescription ().isHuman ())
											{
												final NewTurnMessageConstructBuilding completedConstruction = new NewTurnMessageConstructBuilding ();
												completedConstruction.setMsgType (NewTurnMessageTypeID.COMPLETED_BUILDING);
												completedConstruction.setBuildingID (building.getBuildingID ());
												completedConstruction.setCityLocation (cityLocation);
												((MomTransientPlayerPrivateKnowledge) cityOwner.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (completedConstruction);
											}

											// Now actually add the building - this will trigger the messages to be sent to the clients
											getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (cityLocation, Arrays.asList (building.getBuildingID ()), null, null, true, mom);
										}

										// Did we construct a unit?
										else if (unit != null)
										{
											// First see if it was a settler.  Blocked from constructing settlers if city population is below 2,000 or it would return to being an Outpost.
											final boolean isSettler = unit.getUnitHasSkill ().stream ().anyMatch (s -> s.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_CREATE_OUTPOST));
											if ((isSettler) && (cityData.getCityPopulation () < 2000))
												cityData.setProductionSoFar (productionCost);
											else
											{
												cityData.setProductionSoFar (0);
												
												// AI players need to reset construction back to default so they reconsider what to construct next,
												// otherwise they'd construct the same unit forever.
												if (!cityOwner.getPlayerDescription ().isHuman ())
												{
													cityData.setCurrentlyConstructingUnitID (null);
													cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
												}
												
												// Check if the city has space for the unit
												final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
													(cityLocation, unit.getUnitID (), cityData.getCityOwnerID (), mom);
	
												// Show on new turn messages for the player who built it
												if (cityOwner.getPlayerDescription ().isHuman ())
												{
													final NewTurnMessageConstructUnit completedConstruction = new NewTurnMessageConstructUnit ();
													completedConstruction.setMsgType (NewTurnMessageTypeID.COMPLETED_UNIT);
													completedConstruction.setUnitID (unit.getUnitID ());
													completedConstruction.setCityLocation (cityLocation);
													completedConstruction.setUnitAddBumpType (addLocation.getBumpType ());
													((MomTransientPlayerPrivateKnowledge) cityOwner.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (completedConstruction);
												}
	
												// Now actually add the unit
												final MemoryUnit newUnit = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (unit.getUnitID (), addLocation.getUnitLocation (),
													cityLocation, null, null, cityOwner, UnitStatusID.ALIVE, true, mom);
												
												// If the caster has Doom Mastery cast then cast Chaos Channels on the new unit
												if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
													cityData.getCityOwnerID (), CommonDatabaseConstants.SPELL_ID_DOOM_MASTERY, null, null, null, null) != null)
												{
													final Spell chaosChannels = mom.getServerDB ().findSpell (CommonDatabaseConstants.SPELL_ID_CHAOS_CHANNELS, "progressConstructionProjects");
													final List<UnitSpellEffect> unitSpellEffects = getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
														(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), chaosChannels, cityData.getCityOwnerID (), newUnit.getUnitURN ());
													if ((unitSpellEffects != null) && (unitSpellEffects.size () > 0))
														
														getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (cityData.getCityOwnerID (),
															CommonDatabaseConstants.SPELL_ID_CHAOS_CHANNELS, newUnit.getUnitURN (),
															unitSpellEffects.get (getRandomUtils ().nextInt (unitSpellEffects.size ())).getUnitSkillID (), false, null, null, null, true, true, mom);
												}
												
												// If it was a settler, reduce the city population by 1000
												if (isSettler)
													cityData.setCityPopulation (cityData.getCityPopulation () - 1000);
											}
										}
									}
									
									mom.getWorldUpdates ().recalculateCity (cityLocation);
								}
							}
						}
					}
				}
		
		mom.getWorldUpdates ().process (mom);
	}

	/**
	 * All cities grow population a little
	 *
	 * @param onlyOnePlayerID If zero, will process grow cities + progress construction for all players; if specified will do so only for the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void growCities (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final ServerGridCellEx mc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final OverlandMapCityData cityData = mc.getCityData ();
					if ((cityData != null) && ((onlyOnePlayerID == 0) || (onlyOnePlayerID == cityData.getCityOwnerID ())))
					{
						final PlayerServerDetails cityOwnerPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityData.getCityOwnerID (), "growCities");
						final MomPersistentPlayerPrivateKnowledge cityOwnerPriv = (MomPersistentPlayerPrivateKnowledge) cityOwnerPlayer.getPersistentPlayerPrivateKnowledge ();

						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
						
						// Use calculated values to determine population growth
						final int maxCitySize = getCityCalculations ().calculateSingleCityProduction (mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
							cityLocation, cityOwnerPriv.getTaxRateID (), mom.getSessionDescription (), mom.getGeneralPublicKnowledge ().getConjunctionEventID (), true, mom.getServerDB (),
							CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);

						if (cityData.getCityPopulation () >= 1000)
						{
							// Normal city growth rate calculation
							final int cityGrowthRate = getCityCalculations ().calculateCityGrowthRate (mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
								cityLocation, maxCitySize, mom.getSessionDescription ().getDifficultyLevel (), mom.getServerDB ()).getCappedTotal ();
	
							if (cityGrowthRate != 0)
							{
								final int oldPopulation = cityData.getCityPopulation ();
								int newPopulation = oldPopulation + cityGrowthRate;
	
								// Special raiders cap?
								final KnownWizardDetails cityOwnerWizard =  getKnownWizardUtils ().findKnownWizardDetails
									(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), cityData.getCityOwnerID (), "growCities");
								
								if ((mc.getRaiderCityAdditionalPopulationCap () != null) && (mc.getRaiderCityAdditionalPopulationCap () > 0) &&
									(cityOwnerWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)) &&
									(newPopulation > mc.getRaiderCityAdditionalPopulationCap ()))
								{
									newPopulation = mc.getRaiderCityAdditionalPopulationCap ();
									log.debug ("Special raider population cap enforced: " + oldPopulation + " + " + cityGrowthRate + " = " + newPopulation);
								}
	
								cityData.setCityPopulation (newPopulation);
	
								// Show on new turn messages?
								if ((cityOwnerPlayer.getPlayerDescription ().isHuman ()) && ((oldPopulation / 1000) != (newPopulation / 1000)))
								{
									final NewTurnMessagePopulationChange populationChange = new NewTurnMessagePopulationChange ();
									populationChange.setMsgType (NewTurnMessageTypeID.POPULATION_CHANGE);
									populationChange.setCityLocation (cityLocation);
									populationChange.setCityName (cityData.getCityName ());
									populationChange.setOldPopulation (oldPopulation);
									populationChange.setNewPopulation (newPopulation);
									((MomTransientPlayerPrivateKnowledge) cityOwnerPlayer.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (populationChange);
								}
	
								// If we go over a 1,000 boundary the city size ID or number of farmers or rebels may change; if not we still need to update player memory about the city
								mom.getWorldUpdates ().recalculateCity (cityLocation);
							}
						}
						else
						{
							// Outposts have a chance of growing, and a chance of dying too
							final int growthChance = getCityCalculations ().calculateOutpostGrowthChance (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
								mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), cityLocation, maxCitySize,
								mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ()).getTotalChance ();

							final int deathChance = getCityCalculations ().calculateOutpostDeathChance
								(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), cityLocation, mom.getServerDB ()).getTotalChance ();
							
							int outpostGrowthRate = 0;
							if (getRandomUtils ().nextInt (100) < growthChance)
								outpostGrowthRate = outpostGrowthRate + (100 * (getRandomUtils ().nextInt (3) + 1));
							
							if (getRandomUtils ().nextInt (100) < deathChance)
								outpostGrowthRate = outpostGrowthRate - (100 * (getRandomUtils ().nextInt (2) + 1));
							
							if (outpostGrowthRate != 0)
							{
								final int oldPopulation = cityData.getCityPopulation ();
								int newPopulation = Math.min (oldPopulation + outpostGrowthRate, 1000);
								
								if (newPopulation > 0)
									cityData.setCityPopulation (newPopulation);
								else
									razeCity (cityLocation, mom);		// Outposts can die off completely
								
								// Show on new turn messages?
								if ((cityOwnerPlayer.getPlayerDescription ().isHuman ()) && ((newPopulation >= 1000) || (newPopulation <= 0)))
								{
									final NewTurnMessagePopulationChange populationChange = new NewTurnMessagePopulationChange ();
									populationChange.setMsgType (NewTurnMessageTypeID.POPULATION_CHANGE);
									populationChange.setCityLocation (cityLocation);
									populationChange.setCityName (cityData.getCityName ());
									populationChange.setOldPopulation (oldPopulation);
									populationChange.setNewPopulation (newPopulation);
									((MomTransientPlayerPrivateKnowledge) cityOwnerPlayer.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (populationChange);
								}
	
								// If we go over a 1,000 boundary the city size ID or number of farmers or rebels may change; if not we still need to update player memory about the city
								mom.getWorldUpdates ().recalculateCity (cityLocation);
							}
						}
					}
				}
		
		mom.getWorldUpdates ().process (mom);
	}
	
	/**
	 * The method in the FOW class physically removed builidngs from the server and players' memory; this method
	 * deals with all the knock on effects of buildings being sold, such as paying the gold from the sale to the city owner,
	 * making sure the current construction project is still valid, and if the building sale alters unrest or production in any way.
	 *
	 * Does not recalc global production (which will now be reduced from not having to pay the maintenance of the sold building),
	 * this has to be done by the calling routine.
	 * 
	 * @param cityLocation Location of the city to remove the building from
	 * @param buildingURN Which building to remove; this can be null to cancel a pending sale
	 * @param pendingSale If true, building is not sold immediately but merely marked that it will be sold at the end of the turn; used for simultaneous turns games
	 * @param voluntarySale True if building is being sold by the player's choice; false if they are being forced to sell it e.g. due to lack of production
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void sellBuilding (final MapCoordinates3DEx cityLocation, final Integer buildingURN,
		final boolean pendingSale, final boolean voluntarySale, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Building details
		final MemoryBuilding trueBuilding = (buildingURN == null) ? null : getMemoryBuildingUtils ().findBuildingURN
			(buildingURN, mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), "sellBuilding");
		final String buildingID = (trueBuilding == null) ? null : trueBuilding.getBuildingID ();
		
		// City and player details
		final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), tc.getCityData ().getCityOwnerID (), "sellBuilding");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();

		if (pendingSale)
		{
			// Building is not actually sold yet - we're simply marking that we want to sell it at the end of the turn
			// So only the city owner knows about it, we've no production to recalculate or anything else, so this is easy
			if (cityOwner.getPlayerDescription ().isHuman ())
			{
				final PendingSaleMessage msg = new PendingSaleMessage ();
				msg.setCityLocation (cityLocation);
				msg.setBuildingURN (buildingURN);
				cityOwner.getConnection ().sendMessageToClient (msg);
			}

			// Remember on server to sell it at the end of the turn
			tc.setBuildingIdSoldThisTurn (buildingID);
		}
		else
		{
			// Building is actually being destroyed
			// If doing so voluntarily, then record on the server that this player can't sell another building this turn
			if (voluntarySale)
				tc.setBuildingIdSoldThisTurn (buildingID);

			// If selling a building that the current construction project needs, then cancel the current construction project on the city owner's client
			if (((tc.getCityData ().getCurrentlyConstructingBuildingID () != null) &&
					(getMemoryBuildingUtils ().isBuildingAPrerequisiteForBuilding (buildingID, tc.getCityData ().getCurrentlyConstructingBuildingID (), mom.getServerDB ()))) ||
				((tc.getCityData ().getCurrentlyConstructingUnitID () != null) &&
					(getMemoryBuildingUtils ().isBuildingAPrerequisiteForUnit (buildingID, tc.getCityData ().getCurrentlyConstructingUnitID (), mom.getServerDB ()))))
			{
				tc.getCityData ().setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
				tc.getCityData ().setCurrentlyConstructingUnitID (null);
				// We send these lower down on the call to updatePlayerMemoryOfCity ()
			}

			// Actually remove the building, both on the server and on any clients who can see the city
			getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (Arrays.asList (buildingURN), voluntarySale, null, null, null, mom);

			// Give gold from selling it
			final Building building = mom.getServerDB ().findBuilding (buildingID, "sellBuilding");
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD,
				getMemoryBuildingUtils ().goldFromSellingBuilding (building));

			// The sold building might have been producing rations or stemming unrest so had better recalculate everything
			mom.getWorldUpdates ().recalculateCity (cityLocation);
			mom.getWorldUpdates ().process (mom);
		}
	}

	/**
	 * Similar to sellBuilding above, but the building was being destroyed by some spell or other action, so the owner doesn't get gold for it and
	 * the need to be notified about it as well.  Also it handles destroying multiple buildings all at once, potentially in different cities owned by
	 * different players.
	 * 
	 * @param buildingsToDestroy List of buildings to destroy, from server's true list
	 * @param buildingsDestroyedBySpellID The spell that resulted in destroying these building(s), e.g. Earthquake; null if buildings destroyed for any other reason
	 * @param buildingDestructionSpellCastByPlayerID The player who cast the spell that resulted in the destruction of these buildings; null if not from a spell
	 * @param buildingDestructionSpellLocation The location the spell was targeted - need this because it might have destroyed 0 buildings; null if not from a spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void destroyBuildings (final List<MemoryBuilding> buildingsToDestroy,
		final String buildingsDestroyedBySpellID, final Integer buildingDestructionSpellCastByPlayerID, final MapCoordinates3DEx buildingDestructionSpellLocation,
		final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Process each building
		for (final MemoryBuilding destroyBuilding : buildingsToDestroy)
		{
			// City and player details
			final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(destroyBuilding.getCityLocation ().getZ ()).getRow ().get (destroyBuilding.getCityLocation ().getY ()).getCell ().get (destroyBuilding.getCityLocation ().getX ());
			final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), tc.getCityData ().getCityOwnerID (), "destroyBuildings");

			// If selling a building that the current construction project needs, then cancel the current construction project on the city owner's client
			if (((tc.getCityData ().getCurrentlyConstructingBuildingID () != null) &&
					(getMemoryBuildingUtils ().isBuildingAPrerequisiteForBuilding (destroyBuilding.getBuildingID (), tc.getCityData ().getCurrentlyConstructingBuildingID (), mom.getServerDB ()))) ||
				((tc.getCityData ().getCurrentlyConstructingUnitID () != null) &&
					(getMemoryBuildingUtils ().isBuildingAPrerequisiteForUnit (destroyBuilding.getBuildingID (), tc.getCityData ().getCurrentlyConstructingUnitID (), mom.getServerDB ()))))
			{
				// If it is a human player then we need to let them know that this has happened
				if (cityOwner.getPlayerDescription ().isHuman ())
				{
					if (tc.getCityData ().getCurrentlyConstructingBuildingID () != null)
					{
						final NewTurnMessageConstructBuilding abortConstruction = new NewTurnMessageConstructBuilding ();
						abortConstruction.setMsgType (NewTurnMessageTypeID.ABORT_BUILDING);
						abortConstruction.setBuildingID (tc.getCityData ().getCurrentlyConstructingBuildingID ());
						abortConstruction.setCityLocation (destroyBuilding.getCityLocation ());
						((MomTransientPlayerPrivateKnowledge) cityOwner.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (abortConstruction);
					}

					if (tc.getCityData ().getCurrentlyConstructingUnitID () != null)
					{
						final NewTurnMessageConstructUnit abortConstruction = new NewTurnMessageConstructUnit ();
						abortConstruction.setMsgType (NewTurnMessageTypeID.ABORT_UNIT);
						abortConstruction.setUnitID (tc.getCityData ().getCurrentlyConstructingUnitID ());
						abortConstruction.setCityLocation (destroyBuilding.getCityLocation ());
						((MomTransientPlayerPrivateKnowledge) cityOwner.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (abortConstruction);
					}
				}
				
				// Once we've reset construction to default, this can't happen again if more buildings are destroyed in the same city, so no risk of sending abort NTM twice
				tc.getCityData ().setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
				tc.getCityData ().setCurrentlyConstructingUnitID (null);
			}

			// If it is a human player then tell them about the destroyed building
			if ((cityOwner.getPlayerDescription ().isHuman ()) && (buildingsDestroyedBySpellID != null) && (buildingDestructionSpellCastByPlayerID != null))
			{
				final NewTurnMessageDestroyBuilding destroyedBuilding = new NewTurnMessageDestroyBuilding ();
				destroyedBuilding.setMsgType (NewTurnMessageTypeID.DESTROYED_BUILDING);
				destroyedBuilding.setBuildingID (destroyBuilding.getBuildingID ());
				destroyedBuilding.setCityLocation (destroyBuilding.getCityLocation ());
				destroyedBuilding.setDestroyedBySpellID (buildingsDestroyedBySpellID);
				destroyedBuilding.setCastingPlayerID (buildingDestructionSpellCastByPlayerID);
				((MomTransientPlayerPrivateKnowledge) cityOwner.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (destroyedBuilding);
			}
			
			// Recalculate city data later (this is clever enough to ignore adding the same location multiple times)
			mom.getWorldUpdates ().recalculateCity ((MapCoordinates3DEx) destroyBuilding.getCityLocation ());
		}

		// Actually remove the building, both on the server and on any clients who can see the city
		final List<Integer> destroyBuildingURNs = buildingsToDestroy.stream ().map (b -> b.getBuildingURN ()).collect (Collectors.toList ());
		getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (destroyBuildingURNs, false,
			buildingsDestroyedBySpellID, buildingDestructionSpellCastByPlayerID, buildingDestructionSpellLocation, mom);
		
		// Recalculate all affected cities
		mom.getWorldUpdates ().process (mom);
		
		getPlayerMessageProcessing ().sendNewTurnMessages (null, mom.getPlayers (), null);
	}
	
	/**
	 * Changes a player's tax rate, currently only clients can change their tax rate, but the AI should use this method too.
	 * Although this is currently only used by human players, kept it separate from ChangeTaxRateMessageImpl in anticipation of AI players using it
	 * 
	 * @param player Player who is changing their tax rate
	 * @param taxRateID New tax rate
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void changeTaxRate (final PlayerServerDetails player, final String taxRateID, final MomSessionVariables mom)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final TaxRate newTaxRate = mom.getServerDB ().getTaxRate ().stream ().filter (t -> t.getTaxRateID ().equals (taxRateID)).findAny ().orElse (null);
		
		if (newTaxRate == null)
		{
			// Return error
			log.warn ("changeTaxRate: " + player.getPlayerDescription ().getPlayerName () + " tried to set invalid tax rate of \"" + taxRateID + "\"");

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("You tried to set an invalid tax rate!");
			player.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Set on server
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			priv.setTaxRateID (taxRateID);
			
			// Set on client
			if (player.getPlayerDescription ().isHuman ())
			{
				final TaxRateChangedMessage reply = new TaxRateChangedMessage ();
				reply.setTaxRateID (taxRateID);
				player.getConnection ().sendMessageToClient (reply);
			}
			
			// Recalc stats for all cities based on the new tax rate
			final FogOfWarMemory trueMap = mom.getGeneralServerKnowledge ().getTrueMap ();
			for (final Plane plane : mom.getServerDB ().getPlane ())
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					{
						final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
						if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) && (cityData.getCityPopulation () >= 1000))
						{
							final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane.getPlaneNumber ());
							
							cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels (trueMap, cityLocation, taxRateID, mom.getServerDB ()).getFinalTotal ());

							getServerCityCalculations ().ensureNotTooManyOptionalFarmers (cityData);

							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (trueMap.getMap (), mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
						}
					}
			
			// Recalculate all global production based on the new tax rate
			getServerResourceCalculations ().recalculateGlobalProductionValues (player.getPlayerDescription ().getPlayerID (), false, mom);
		}
	}
	
	/**
	 * Changes ownership of a city after it is captured in combat
	 * 
	 * @param cityLocation Location of the city
	 * @param attackingPlayer Player who won the combat, who is taking ownership of the city
	 * @param defendingPlayer Player who lost the combat, and who is losing the city
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void captureCity (final MapCoordinates3DEx cityLocation, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MomSessionVariables mom) throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		
		// Destroy enemy wizards' fortress and/or summoning circle
		final List<Integer> destroyedBuildingURNs = new ArrayList<Integer> ();
		for (final String buildingID : new String [] {CommonDatabaseConstants.BUILDING_FORTRESS, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE})
		{
			final MemoryBuilding destroyedBuilding = getMemoryBuildingUtils ().findBuilding
				(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), cityLocation, buildingID);
			if (destroyedBuilding != null)
				destroyedBuildingURNs.add (destroyedBuilding.getBuildingURN ());
		}
		
		if (destroyedBuildingURNs.size () > 0)
			getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (destroyedBuildingURNs, false, null, null, null, mom);
		
		// Deal with spells cast on the city:
		// 1) Any spells the defender had cast on the city must be enchantments - which unfortunately we don't get - so cancel these
		// 2) Any spells the attacker had cast on the city must be curses - we don't want to curse our own city - so cancel them
		// 3) Any spells a 3rd player (neither the defender nor attacker) had cast on the city must be curses - and I'm sure they'd like to continue cursing the new city owner :D
		// These are executed with the "process" method below
		getFogOfWarMidTurnMultiChanges ().switchOffSpellsInLocationOnServerAndClients
			(cityLocation, attackingPlayer.getPlayerDescription ().getPlayerID (), false, mom);
	
		if (defendingPlayer != null)
			getFogOfWarMidTurnMultiChanges ().switchOffSpellsInLocationOnServerAndClients
				( cityLocation, defendingPlayer.getPlayerDescription ().getPlayerID (), false, mom);

		// Take ownership of the city
		tc.getCityData ().setCityOwnerID (attackingPlayer.getPlayerDescription ().getPlayerID ());
		tc.getCityData ().setProductionSoFar (null);
		tc.getCityData ().setCurrentlyConstructingUnitID (null);
		tc.getCityData ().setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
		
		// AI players generate more resources - which no longer includes rations (old versions did) - but to be on the safe side, recalc everything
		mom.getWorldUpdates ().recalculateCity (cityLocation);
		mom.getWorldUpdates ().process (mom);
	}
	
	/**
	 * Destroys a city after it is taken in combat
	 * 
	 * @param cityLocation Location of the city
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void razeCity (final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		
		// Cancel all spells cast on the city regardless of owner
		getFogOfWarMidTurnMultiChanges ().switchOffSpellsInLocationOnServerAndClients (cityLocation, 0, true, mom);
		
		// Wreck all the buildings
		getFogOfWarMidTurnMultiChanges ().destroyAllBuildingsInLocationOnServerAndClients (cityLocation, mom);
		
		// Wreck the city
		tc.setCityData (null);
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
			cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
		
		// Wreck the road
		tc.getTerrainData ().setRoadTileTypeID (null);
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
			cityLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
	}
	
	/**
	 * Rampaging monsters destroy a city and convert it to a ruin
	 * 
	 * @param cityLocation Location of the city
	 * @param goldInRuin The gold the player lost in the city is kept in the ruin and awarded to whoever captures it
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void ruinCity (final MapCoordinates3DEx cityLocation, final int goldInRuin, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		tc.getTerrainData ().setMapFeatureID (CommonDatabaseConstants.MAP_FEATURE_ID_RUINS);
		tc.setGoldInRuin (goldInRuin);

		// This has the call to updatePlayerMemoryOfTerrain at the end
		razeCity (cityLocation, mom);
	}
	
	/**
	 * Handles when the city housing a wizard's fortress is captured in combat and the wizard gets banished
	 * 
	 * @param attackingPlayer Player who won the combat, who is doing the banishing
	 * @param defendingPlayer Player who lost the combat, who is the one being banished
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void banishWizard (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		getKnownWizardServerUtils ().meetWizard (attackingPlayer.getPlayerDescription ().getPlayerID (), null, false, mom);
		getKnownWizardServerUtils ().meetWizard (defendingPlayer.getPlayerDescription ().getPlayerID (), null, false, mom);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackerPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final KnownWizardDetails defenderWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), defendingPlayer.getPlayerDescription ().getPlayerID (), "banishWizard");
		final KnownWizardDetails attackerWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), defendingPlayer.getPlayerDescription ().getPlayerID (), "banishWizard");
		
		// Do they have another city to try to return to?  Record it on server
		final WizardState wizardState = (getCityServerUtils ().countCities (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
			defendingPlayer.getPlayerDescription ().getPlayerID ()) == 0) ? WizardState.DEFEATED : WizardState.BANISHED;
		
		getKnownWizardServerUtils ().updateWizardState (defendingPlayer.getPlayerDescription ().getPlayerID (), wizardState, mom);
		
		// Update wizardState on client, and this triggers showing the banish animation as well
		final UpdateWizardStateMessage msg = new UpdateWizardStateMessage ();
		msg.setBanishedPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
		msg.setBanishingPlayerID (attackingPlayer.getPlayerDescription ().getPlayerID ());
		msg.setWizardState (wizardState);
		getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);

		// Things that only apply if two wizards
		if ((getPlayerKnowledgeUtils ().isWizard (defenderWizard.getWizardID ())) && (getPlayerKnowledgeUtils ().isWizard (attackerWizard.getWizardID ())))
		{
			// Does the attacker get to steal any spells?
			final List<String> stolenSpellIDs = getSpellProcessing ().stealSpells (defendingPlayer, attackingPlayer,
				mom.getSessionDescription ().getSpellSetting ().getSpellsStolenFromFortress (), mom.getServerDB ());
		
			// Attacker steals half of MP from Wiazard's Fortress
			final int manaSwiped = getResourceValueUtils ().findAmountStoredForProductionType (defendingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA) / 2;
			if (manaSwiped > 0)
			{
				getResourceValueUtils ().addToAmountStored (defendingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -manaSwiped);
				getResourceValueUtils ().addToAmountStored (attackerPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, manaSwiped);
			}
			
			// Need to inform client about stuff they captured?
			if ((attackingPlayer.getPlayerDescription ().isHuman ()) && ((stolenSpellIDs.size () > 0) || (manaSwiped > 0)))
			{
				final TreasureRewardMessage treasureMsg = new TreasureRewardMessage ();
				treasureMsg.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
				treasureMsg.getSpellID ().addAll (stolenSpellIDs);
				
				if (manaSwiped > 0)
				{
					final ProductionTypeAndUndoubledValue treasureManaSwiped = new ProductionTypeAndUndoubledValue ();
					treasureManaSwiped.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
					treasureManaSwiped.setUndoubledProductionValue (manaSwiped);
					
					treasureMsg.getResource ().add (treasureManaSwiped);
				}
				
				attackingPlayer.getConnection ().sendMessageToClient (treasureMsg);
			}
		}
		
		// If a wizard is banished and their summoning circle was with their Fortress then it will have already been removed,
		// but if its somewhere else then we need to remove it too.  When they cast Spell of Return then they get both back.
		if (wizardState == WizardState.BANISHED)
		{
			final MemoryBuilding summoningCircle = getMemoryBuildingUtils ().findCityWithBuilding (defendingPlayer.getPlayerDescription ().getPlayerID (),
				CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
			
			if (summoningCircle != null)
				getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (Arrays.asList (summoningCircle.getBuildingURN ()), false, null, null, null, mom);
		}
		
		// Clean up defeated wizards
		if (wizardState == WizardState.DEFEATED)
		{
			// Remove any CAEs the wizard still had cast
			for (final MemoryCombatAreaEffect defeatedCAE : mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect ())
				if (defeatedCAE.getCastingPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ())
					mom.getWorldUpdates ().removeCombatAreaEffect (defeatedCAE.getCombatAreaEffectURN ());
			
			// Remove any spells the wizard still had cast
			for (final MemoryMaintainedSpell defeatedSpell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
				if (defeatedSpell.getCastingPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ())
					mom.getWorldUpdates ().switchOffSpell (defeatedSpell.getSpellURN ());

			// Remove any units the wizard still had
			for (final MemoryUnit defeatedUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if (defeatedUnit.getOwningPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ())
					mom.getWorldUpdates ().killUnit (defeatedUnit.getUnitURN (), KillUnitActionID.LACK_OF_PRODUCTION);
			
			mom.getWorldUpdates ().process (mom);
			
			// Unown and unwarp any nodes the wizard had captured and volcanoes the wizard raised
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final MemoryGridCell mc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
						final OverlandMapTerrainData terrainData = mc.getTerrainData ();
						boolean terrainUpdated = false;
						if ((terrainData.getNodeOwnerID () != null) && (terrainData.getNodeOwnerID () == defendingPlayer.getPlayerDescription ().getPlayerID ()))
						{
							terrainData.setNodeOwnerID (null);
							terrainData.setWarped (null);
							terrainUpdated = true;
						}

						if ((terrainData.getVolcanoOwnerID () != null) && (terrainData.getVolcanoOwnerID () == defendingPlayer.getPlayerDescription ().getPlayerID ()))
						{
							terrainData.setVolcanoOwnerID (null);
							terrainUpdated = true;
						}
						
						if (terrainUpdated)
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
								new MapCoordinates3DEx (x, y, z), mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
						
						// Remove any outposts
						if ((mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () == defendingPlayer.getPlayerDescription ().getPlayerID ()))
						{
							mc.setCityData (null);
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
								new MapCoordinates3DEx (x, y, z), mom.getSessionDescription ().getFogOfWarSetting ());
						}
					}
			
			// If it was a human player, convert them to AI and possibly end the session
			if (defendingPlayer.getPlayerDescription ().isHuman ())
				mom.updateHumanPlayerToAI (defendingPlayer.getPlayerDescription ().getPlayerID ());
			
			// The attacker may have just defeated the last wizard
			getPlayerMessageProcessing ().checkIfWonGame (mom);
		}
		
		// Its possible by this point that the session doesn't exist, which wipes out the player list
		if (mom.getPlayers ().size () > 0)
		{
			// If wizard was in middle of casting any overland spells then remove them all
			while (defendingPriv.getQueuedSpell ().size () > 0)
			{
				// Remove queued spell on server
				defendingPriv.getQueuedSpell ().remove (0);
	
				// Remove queued spell on client
				if (defendingPlayer.getPlayerDescription ().isHuman ())
				{
					final RemoveQueuedSpellMessage removeSpellMessage = new RemoveQueuedSpellMessage ();
					removeSpellMessage.setQueuedSpellIndex (0);
					defendingPlayer.getConnection ().sendMessageToClient (removeSpellMessage);
				}
			}

			// Make sure any mana spent on removed spells is zeroed out
			if (defendingPriv.getManaSpentOnCastingCurrentSpell () > 0)
			{
				defendingPriv.setManaSpentOnCastingCurrentSpell (0);
				if (defendingPlayer.getPlayerDescription ().isHuman ())
					defendingPlayer.getConnection ().sendMessageToClient (new UpdateManaSpentOnCastingCurrentSpellMessage ());
			}
			
			// If they are only banished, then begin casting spell of return
			if (wizardState == WizardState.BANISHED)
				getSpellQueueing ().queueSpell (defendingPlayer, mom.getPlayers (), CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN, null, null);
		}
	}
	
	/**
	 * If a wizard loses the city where their summoning circle is, but they still have their Wizard's Fortress at a different location,
	 * then this method auto adds their summoning circle back at the same location as their fortress.
	 * 
	 * @param playerID Player who lost their summoning circle 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void moveSummoningCircleToWizardsFortress (final int playerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final MemoryBuilding wizardsFortress = getMemoryBuildingUtils ().findCityWithBuilding (playerID,
			CommonDatabaseConstants.BUILDING_FORTRESS, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
		
		if (wizardsFortress != null)
			getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients ((MapCoordinates3DEx) wizardsFortress.getCityLocation (),
				Arrays.asList (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE), null, null, true, mom);
	}

	/**
	 * Certain buildings require certain tile types to construct them, e.g. a Sawmill can only be constructed if there is a forest tile.
	 * So this method rechecks that city construction is still valid after there's been a change to an overland tile.
	 * 
	 * @param targetLocation Location where terrain was changed
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void recheckCurrentConstructionIsStillValid (final MapCoordinates3DEx targetLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final MapCoordinates3DEx cityLocation = getCityServerUtils ().findCityWithinRadius (targetLocation,
			mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getSessionDescription ().getOverlandMapSize ());
		if (cityLocation != null)
		{
			// City probably isn't owned by the person who cast the spell
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			if (cityData.getCurrentlyConstructingBuildingID () != null)
			{
				final Building buildingDef = mom.getServerDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "targetCorruption");
				if (!getCityCalculations ().buildingPassesTileTypeRequirements (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), cityLocation,
					buildingDef, mom.getSessionDescription ().getOverlandMapSize ()))
				{
					// City can no longer proceed with their current construction project
					cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());

					// If it is a human player then we need to let them know that this has happened
					final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityData.getCityOwnerID (), "targetCorruption");
					if (cityOwner.getPlayerDescription ().isHuman ())
					{
						final NewTurnMessageConstructBuilding abortConstruction = new NewTurnMessageConstructBuilding ();
						abortConstruction.setMsgType (NewTurnMessageTypeID.ABORT_BUILDING);
						abortConstruction.setBuildingID (buildingDef.getBuildingID ());
						abortConstruction.setCityLocation (cityLocation);
						((MomTransientPlayerPrivateKnowledge) cityOwner.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (abortConstruction);
						
						getPlayerMessageProcessing ().sendNewTurnMessages (null, mom.getPlayers (), null);
					}
				}
			}
		}
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
	 * @return Server-only pick utils
	 */
	public final PlayerPickServerUtils getPlayerPickServerUtils ()
	{
		return playerPickServerUtils;
	}

	/**
	 * @param utils Server-only pick utils
	 */
	public final void setPlayerPickServerUtils (final PlayerPickServerUtils utils)
	{
		playerPickServerUtils = utils;
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
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}
	
	/**
	 * @return AI decisions about cities
	 */
	public final CityAI getCityAI ()
	{
		return cityAI;
	}

	/**
	 * @param ai AI decisions about cities
	 */
	public final void setCityAI (final CityAI ai)
	{
		cityAI = ai;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
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
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
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
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnMultiChanges getFogOfWarMidTurnMultiChanges ()
	{
		return fogOfWarMidTurnMultiChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnMultiChanges (final FogOfWarMidTurnMultiChanges obj)
	{
		fogOfWarMidTurnMultiChanges = obj;
	}

	/**
	 * @return Spell queueing methods
	 */
	public final SpellQueueing getSpellQueueing ()
	{
		return spellQueueing;
	}

	/**
	 * @param obj Spell queueing methods
	 */
	public final void setSpellQueueing (final SpellQueueing obj)
	{
		spellQueueing = obj;
	}

	/**
	 * @return Spell processing methods
	 */
	public final SpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final SpellProcessing obj)
	{
		spellProcessing = obj;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
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
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
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

	/**
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
	}
}