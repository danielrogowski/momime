package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.TaxRate;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.servertoclient.v0_9_5.PendingSaleMessage;
import momime.common.messages.servertoclient.v0_9_5.TaxRateChangedMessage;
import momime.common.messages.servertoclient.v0_9_5.TextPopupMessage;
import momime.common.messages.servertoclient.v0_9_5.UpdateProductionSoFarMessage;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.NewTurnMessageConstructBuilding;
import momime.common.messages.v0_9_5.NewTurnMessageConstructUnit;
import momime.common.messages.v0_9_5.NewTurnMessagePopulationChange;
import momime.common.messages.v0_9_5.NewTurnMessageTypeID;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.CityAI;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_5.Building;
import momime.server.database.v0_9_5.Plane;
import momime.server.database.v0_9_5.Unit;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;
import momime.server.messages.v0_9_5.ServerGridCell;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

/**
 * Methods for any significant message processing to do with cities that isn't done in the message implementations
 */
public final class CityProcessingImpl implements CityProcessing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (CityProcessingImpl.class);

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private MomCityCalculations cityCalculations;

	/** Server-only city calculations */
	private MomServerCityCalculations serverCityCalculations;
	
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
	private MomServerResourceCalculations serverResourceCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/**
	 * Creates the starting cities for each Wizard and Raiders
	 *
	 * This happens BEFORE we initialize each players' fog of war (of course... without their cities they wouldn't be able to see much of the map!)
	 * and so we don't need to send any messages out to anyone here, whether to add the city itself, buildings or units - just add everything to the true map
	 *
	 * @param players List of players in the session
	 * @param gsk Server knowledge data structure
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
 	 * @throws MomException If no races are defined for a particular plane
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void createStartingCities (final List<PlayerServerDetails> players,
		final MomGeneralServerKnowledge gsk, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering createStartingCities");

		// Allocate a race to each continent of land for raider cities
		final MapArea3D<String> continentalRace = getOverlandMapServerUtils ().decideAllContinentalRaces (gsk.getTrueMap ().getMap (), sd.getMapSize (), db);

		// Now create cities for each player
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();

			// How many cities?
			final int numberOfCities;
			if (PlayerKnowledgeUtils.isWizard (ppk.getWizardID ()))
				numberOfCities = 1;

			else if (ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS))
				numberOfCities = sd.getDifficultyLevel ().getRaiderCityCount ();

			else
				numberOfCities = 0;	// For monsters

			for (int cityNo = 0; cityNo < numberOfCities; cityNo++)
			{
				final int plane;
				if (PlayerKnowledgeUtils.isWizard (ppk.getWizardID ()))
					plane = getPlayerPickServerUtils ().startingPlaneForWizard (ppk.getPick (), db);
				else
					// Raiders just pick a random plane
					plane = db.getPlane ().get (getRandomUtils ().nextInt (db.getPlane ().size ())).getPlaneNumber ();

				// Pick location
				final MapCoordinates3DEx cityLocation = getCityAI ().chooseCityLocation (gsk.getTrueMap ().getMap (), plane, sd, db);
				if (cityLocation == null)
					throw new MomException ("createStartingCities: Can't find starting city location for player \"" + thisPlayer.getPlayerDescription ().getPlayerName () + "\"");

				final ServerGridCell cityCell = (ServerGridCell) gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
				final OverlandMapCityData city = new OverlandMapCityData ();
				cityCell.setCityData (city);

				// Set the city race and population
				city.setCityOwnerID (thisPlayer.getPlayerDescription ().getPlayerID ());
				city.setOptionalFarmers (0);

				if (PlayerKnowledgeUtils.isWizard (ppk.getWizardID ()))
				{
					final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) thisPlayer.getTransientPlayerPrivateKnowledge ();

					city.setCityPopulation (sd.getDifficultyLevel ().getWizardCityStartSize () * 1000);
					city.setCityRaceID (priv.getFirstCityRaceID ());
				}
				else
				{
					// Randomize population of raider cities
					city.setCityPopulation ((sd.getDifficultyLevel ().getRaiderCityStartSizeMin () +
						getRandomUtils ().nextInt (sd.getDifficultyLevel ().getRaiderCityStartSizeMax () - sd.getDifficultyLevel ().getRaiderCityStartSizeMin () + 1)) * 1000);

					// Raiders have a special population cap that prevents cities expanding by more than a certain value
					// See strategy guide p426
					cityCell.setRaiderCityAdditionalPopulationCap (city.getCityPopulation () + (sd.getDifficultyLevel ().getRaiderCityGrowthCap () * 1000));

					// Have a good chance of just picking the continental race ID
					if (getRandomUtils ().nextInt (100) < sd.getMapSize ().getContinentalRaceChance ())
					{
						final String raceID = continentalRace.get (cityLocation);
						if (raceID == null)
							throw new MomException ("createStartingCities: Tried to create Raider city with Continental race, but this tile has no continental race");

						city.setCityRaceID (raceID);
					}
					else
						// Pick totally random race
						city.setCityRaceID (getPlayerPickServerUtils ().chooseRandomRaceForPlane (plane, db));
				}

				// Pick a name for the city
				city.setCityName (getOverlandMapServerUtils ().generateCityName (gsk, db.findRace (city.getCityRaceID (), "createStartingCities")));

				// Do initial calculations on the city
				getServerCityCalculations ().calculateCitySizeIDAndMinimumFarmers (players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding (), cityLocation, sd, db);

				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();

				city.setNumberOfRebels (getCityCalculations ().calculateCityRebels (players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getUnit (), gsk.getTrueMap ().getBuilding (),
					cityLocation, priv.getTaxRateID (), db).getFinalTotal ());

				getServerCityCalculations ().ensureNotTooManyOptionalFarmers (city);

				// Set default production
				city.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);

				// Add starting buildings
				if (PlayerKnowledgeUtils.isWizard (ppk.getWizardID ()))
				{
					// Wizards always get the same buildings (this also adds their Fortress & Summoning Circle)
					for (final Building thisBuilding : db.getBuilding ())
						if ((thisBuilding.isInWizardsStartingCities () != null) && (thisBuilding.isInWizardsStartingCities ()))
						{
							final MemoryBuilding wizardBuilding = new MemoryBuilding ();
							wizardBuilding.setBuildingID (thisBuilding.getBuildingID ());
							wizardBuilding.setCityLocation (new MapCoordinates3DEx (cityLocation));
							gsk.getTrueMap ().getBuilding ().add (wizardBuilding);
						}
				}
				else
				{
					// Raiders buildings' depend on the city size
					for (final Building thisBuilding : db.getBuilding ())
						if ((thisBuilding.getInRaidersStartingCitiesWithPopulationAtLeast () != null) &&
							(city.getCityPopulation () >= thisBuilding.getInRaidersStartingCitiesWithPopulationAtLeast () * 1000))
						{
							final MemoryBuilding raiderBuilding = new MemoryBuilding ();
							raiderBuilding.setBuildingID (thisBuilding.getBuildingID ());
							raiderBuilding.setCityLocation (new MapCoordinates3DEx (cityLocation));
							gsk.getTrueMap ().getBuilding ().add (raiderBuilding);
						}
				}

				// Add starting units
				for (final Unit thisUnit : db.getUnit ())
					if ((thisUnit.getUnitRaceID () != null) && (thisUnit.getFreeAtStartCount () != null) && (thisUnit.getUnitRaceID ().equals (city.getCityRaceID ())))
						for (int freeAtStart = 0; freeAtStart < thisUnit.getFreeAtStartCount (); freeAtStart++)
						{
							final MapCoordinates3DEx unitCoords = new MapCoordinates3DEx (cityLocation);
							getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, thisUnit.getUnitID (), unitCoords, cityLocation, null, thisPlayer, UnitStatusID.ALIVE, null, sd, db);
						}
			}
		}

		log.trace ("Exiting createStartingCities");
	}

	/**
	 * All cities owner grow population a little and progress a little towards construction projects
	 *
	 * @param onlyOnePlayerID If zero, will process grow cities + progress construction for all players; if specified will do so only for the specified player
	 * @param players List of players in this session
	 * @param gsk Server knowledge structure
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void growCitiesAndProgressConstructionProjects (final int onlyOnePlayerID,
		final List<PlayerServerDetails> players, final MomGeneralServerKnowledge gsk,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering growCitiesAndProgressConstructionProjects: Player ID " + onlyOnePlayerID);

		for (final Plane plane : db.getPlane ())
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				{
					final ServerGridCell mc = (ServerGridCell) gsk.getTrueMap ().getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);
					final OverlandMapCityData cityData = mc.getCityData ();
					if ((cityData != null) && (cityData.getCityPopulation () != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityPopulation () > 0) &&
						((onlyOnePlayerID == 0) | (onlyOnePlayerID == cityData.getCityOwnerID ())))
					{
						final PlayerServerDetails cityOwner = MultiplayerSessionServerUtils.findPlayerWithID (players, cityData.getCityOwnerID (), "growCitiesAndProgressConstructionProjects");
						final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();
						final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) cityOwner.getPersistentPlayerPublicKnowledge ();

						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane.getPlaneNumber ());

						final CityProductionBreakdownsEx cityProductions = getCityCalculations ().calculateAllCityProductions
							(players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding (), cityLocation, priv.getTaxRateID (), sd, true, false, db);

						// Use calculated values to determine construction rate
						if ((cityData.getCurrentlyConstructingBuildingID () != null) || (cityData.getCurrentlyConstructingUnitID () != null))
						{
							// Check if we're constructing a building or a unit
							Building building = null;
							Integer productionCost = null;
							if (cityData.getCurrentlyConstructingBuildingID () != null)
							{
								building = db.findBuilding (cityData.getCurrentlyConstructingBuildingID (), "growCitiesAndProgressConstructionProjects");
								productionCost = building.getProductionCost ();
							}

							Unit unit = null;
							if (cityData.getCurrentlyConstructingUnitID () != null)
							{
								unit = db.findUnit (cityData.getCurrentlyConstructingUnitID (), "growCitiesAndProgressConstructionProjects");
								productionCost = unit.getProductionCost ();
							}

							if (productionCost != null)
							{
								final CityProductionBreakdown productionAmount = cityProductions.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION);
								if (productionAmount != null)
								{
									if (mc.getProductionSoFar () == null)
										mc.setProductionSoFar (productionAmount.getCappedProductionAmount ());
									else
										mc.setProductionSoFar (mc.getProductionSoFar () + productionAmount.getCappedProductionAmount ());

									// Is it finished?
									if (mc.getProductionSoFar () > productionCost)
									{
										// Did we construct a building?
										if (building != null)
										{
											// Current building is now finished - set next construction project FIRST
											// This is a leftover from when NTMs showed up instantly, since the mmAddBuilding message would cause the
											// client to immediately show the 'completed construction' window so we needed to send other data that appears in that window first
											cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
											getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (gsk.getTrueMap ().getMap (), players, cityLocation, sd.getFogOfWarSetting (), false);

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
											getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (gsk, players, cityLocation,
												building.getBuildingID (), null, null, null, sd, db);
										}

										// Did we construct a unit?
										else if (unit != null)
										{
											// Check if the city has space for the unit
											final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
												(cityLocation, unit.getUnitID (), cityData.getCityOwnerID (), gsk.getTrueMap (), sd, db);

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
											getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, unit.getUnitID (), addLocation.getUnitLocation (),
												cityLocation, null, cityOwner, UnitStatusID.ALIVE, players, sd, db);
										}

										// Zero production for the next construction project
										mc.setProductionSoFar (0);
									}

									// Send new production so far value to client, whether its an updated value, or zero because we finished the previous building
									if (cityOwner.getPlayerDescription ().isHuman ())
									{
										final UpdateProductionSoFarMessage msg = new UpdateProductionSoFarMessage ();
										msg.setCityLocation (cityLocation);
										msg.setProductionSoFar (mc.getProductionSoFar ());
										cityOwner.getConnection ().sendMessageToClient (msg);
									}
								}
							}
						}

						// Use calculated values to determine population growth
						final CityProductionBreakdown productionAmount = cityProductions.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
						final int maxCitySize;
						if (productionAmount == null)
							maxCitySize = 0;
						else
							maxCitySize = productionAmount.getCappedProductionAmount ();

						final int cityGrowthRate = getCityCalculations ().calculateCityGrowthRate
							(gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding (), cityLocation, maxCitySize, db).getFinalTotal ();

						if (cityGrowthRate != 0)
						{
							final int oldPopulation = cityData.getCityPopulation ();
							int newPopulation = oldPopulation + cityGrowthRate;

							// Special raiders cap?
							if ((mc.getRaiderCityAdditionalPopulationCap () != null) && (mc.getRaiderCityAdditionalPopulationCap () > 0) &&
								(pub.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)) &&
								(newPopulation > mc.getRaiderCityAdditionalPopulationCap ()))
							{
								newPopulation = mc.getRaiderCityAdditionalPopulationCap ();
								log.debug ("Special raider population cap enforced: " + oldPopulation + " + " + cityGrowthRate + " = " + newPopulation);
							}

							cityData.setCityPopulation (newPopulation);

							// Show on new turn messages?
							if ((cityOwner.getPlayerDescription ().isHuman ()) && ((oldPopulation / 1000) != (newPopulation / 1000)))
							{
								final NewTurnMessagePopulationChange populationChange = new NewTurnMessagePopulationChange ();
								populationChange.setMsgType (NewTurnMessageTypeID.POPULATION_CHANGE);
								populationChange.setCityLocation (cityLocation);
								populationChange.setOldPopulation (oldPopulation);
								populationChange.setNewPopulation (newPopulation);
								((MomTransientPlayerPrivateKnowledge) cityOwner.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (populationChange);
							}

							// If we go over a 1,000 boundary the city size ID or number of farmers or rebels may change
							getServerCityCalculations ().calculateCitySizeIDAndMinimumFarmers
								(players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding (), cityLocation, sd, db);

							cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels
								(players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getUnit (), gsk.getTrueMap ().getBuilding (), cityLocation, priv.getTaxRateID (), db).getFinalTotal ());

							getServerCityCalculations ().ensureNotTooManyOptionalFarmers (cityData);

							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (gsk.getTrueMap ().getMap (), players, cityLocation, sd.getFogOfWarSetting (), false);
						}
					}
				}

		log.trace ("Exiting growCitiesAndProgressConstructionProjects");
	}

	/**
	 * The method in the FOW class physically removed builidngs from the server and players' memory; this method
	 * deals with all the knock on effects of buildings being sold, such as paying the gold from the sale to the city owner,
	 * making sure the current construction project is still valid, and if the building sale alters unrest or production in any way.
	 *
	 * Does not recalc global production (which will now be reduced from not having to pay the maintenance of the sold building),
	 * this has to be done by the calling routine.
	 * 
	 * NB. Dephi method was called OkToSellBuilding.
	 *
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param cityLocation Location of the city to remove the building from
	 * @param buildingID Which building to remove; this can be null to cancel a pending sale
	 * @param pendingSale If true, building is not sold immediately but merely marked that it will be sold at the end of the turn; used for simultaneous turns games
	 * @param voluntarySale True if building is being sold by the player's choice; false if they are being forced to sell it e.g. due to lack of production
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void sellBuilding (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx cityLocation, final String buildingID,
		final boolean pendingSale, final boolean voluntarySale,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering sellBuilding: " + cityLocation + ", " + buildingID);

		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final PlayerServerDetails cityOwner = MultiplayerSessionServerUtils.findPlayerWithID (players, tc.getCityData ().getCityOwnerID (), "sellBuilding");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();

		if (pendingSale)
		{
			// Building is not actually sold yet - we're simply marking that we want to sell it at the end of the turn
			// So only the city owner knows about it, we've no production to recalculate or anything else, so this is easy
			if (cityOwner.getPlayerDescription ().isHuman ())
			{
				final PendingSaleMessage msg = new PendingSaleMessage ();
				msg.setCityLocation (cityLocation);
				msg.setBuildingID (buildingID);
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
					(getMemoryBuildingUtils ().isBuildingAPrerequisiteForBuilding (buildingID, tc.getCityData ().getCurrentlyConstructingBuildingID (), db))) ||
				((tc.getCityData ().getCurrentlyConstructingUnitID () != null) &&
					(getMemoryBuildingUtils ().isBuildingAPrerequisiteForUnit (buildingID, tc.getCityData ().getCurrentlyConstructingUnitID (), db))))
			{
				tc.getCityData ().setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
				tc.getCityData ().setCurrentlyConstructingUnitID (null);
				// We send these lower down on the call to updatePlayerMemoryOfCity ()
			}

			// Actually remove the building, both on the server and on any clients who can see the city
			getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (trueMap, players, cityLocation, buildingID, voluntarySale, sd, db);

			// Give gold from selling it
			final Building building = db.findBuilding (buildingID, "sellBuilding");
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD,
				getMemoryBuildingUtils ().goldFromSellingBuilding (building));

			// The sold building might have been producing rations or stemming unrest so had better recalculate everything
			getServerCityCalculations ().calculateCitySizeIDAndMinimumFarmers (players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, sd, db);

			tc.getCityData ().setNumberOfRebels (getCityCalculations ().calculateCityRebels
				(players, trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (), cityLocation, priv.getTaxRateID (), db).getFinalTotal ());

			getServerCityCalculations ().ensureNotTooManyOptionalFarmers (tc.getCityData ());

			// Send the updated city stats to any clients that can see the city
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (trueMap.getMap (), players, cityLocation, sd.getFogOfWarSetting (), false);
		}

		log.trace ("Exiting sellBuilding");
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
		log.trace ("Entering changeTaxRate: Player ID " + player.getPlayerDescription ().getPlayerID () + ", " + taxRateID);
		
		TaxRate newTaxRate = null;
		try
		{
			newTaxRate = mom.getServerDB ().findTaxRate (taxRateID, "changeTaxRate");
		}
		catch (final RecordNotFoundException e)
		{
			// Handled below
		}
		
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
				for (int x = 0; x < mom.getSessionDescription ().getMapSize ().getWidth (); x++)
					for (int y = 0; y < mom.getSessionDescription ().getMapSize ().getHeight (); y++)
					{
						final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
						if (cityData != null)
						{
							if ((cityData.getCityPopulation () != null) && (cityData.getCityOwnerID () != null) &&
								(cityData.getCityPopulation () > 0) && (cityData.getCityOwnerID ().equals (player.getPlayerDescription ().getPlayerID ())))
							{
								final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane.getPlaneNumber ());
								
								cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels 
									(mom.getPlayers (), trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (), cityLocation, taxRateID, mom.getServerDB ()).getFinalTotal ());

								getServerCityCalculations ().ensureNotTooManyOptionalFarmers (cityData);

								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (trueMap.getMap (), mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting (), false);
							}
						}
					}
			
			// Recalculate all global production based on the new tax rate
			getServerResourceCalculations ().recalculateGlobalProductionValues (player.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		log.trace ("Exiting changeTaxRate");
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
	public final MomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final MomServerResourceCalculations calc)
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
}