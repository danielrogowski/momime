package momime.server.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;

import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.OverlandMapSize;
import momime.common.database.Plane;
import momime.common.database.RaceEx;
import momime.common.database.TaxRate;
import momime.common.database.UnitEx;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageConstructBuilding;
import momime.common.messages.NewTurnMessageConstructUnit;
import momime.common.messages.NewTurnMessagePopulationChange;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitAddBumpTypeID;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.PendingSaleMessage;
import momime.common.messages.servertoclient.TaxRateChangedMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.ai.CityAI;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;
import momime.server.worldupdates.WorldUpdates;

/**
 * Tests the CityProcessingImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCityProcessingImpl extends ServerTestData
{
	/** buildingID for a Granary */
	private final String GRANARY = "BL29";
	
	/** buildingID for a Farmers' Market (which requires a Granary) */
	private final String FARMERS_MARKET = "BL30";

	/** buildingID for a Barracks (which doesn't require a Granary) */
	private final String BARRACKS = "BL03";
	
	/**
	 * Tests the createStartingCities method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateStartingCities () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlane ()).thenReturn (planes);
	
		when (db.findPlane (0, "createStartingCities")).thenReturn (arcanus);
		when (db.findPlane (1, "createStartingCities")).thenReturn (myrror);
		
		final RaceEx race1 = new RaceEx ();
		final RaceEx race2 = new RaceEx ();
		final RaceEx race3 = new RaceEx ();
		final RaceEx race4 = new RaceEx ();
		when (db.findRace ("RC01", "createStartingCities")).thenReturn (race1);
		when (db.findRace ("RC02", "createStartingCities")).thenReturn (race2);
		when (db.findRace ("RC03", "createStartingCities")).thenReturn (race3);
		when (db.findRace ("RC04", "createStartingCities")).thenReturn (race4);
		
		// Units free at the start of game for each race
		final List<UnitEx> units = new ArrayList<UnitEx> ();
		for (int race = 1; race <= 4; race++)
		{
			final UnitEx unit = new UnitEx ();
			unit.setUnitID ("UN00" + race);
			unit.setFreeAtStartCount (2);
			unit.setUnitRaceID ("RC0" + race);
			units.add (unit);
		}
		
		when (db.getUnits ()).thenReturn (units);
		
		// Buildings free at the start of the game
		final Building wizardBuilding = new Building ();
		wizardBuilding.setBuildingID ("BL01");
		wizardBuilding.setInWizardsStartingCities (true);

		final Building smallRaiderBuilding = new Building ();
		smallRaiderBuilding.setBuildingID ("BL02");
		smallRaiderBuilding.setInRaidersStartingCitiesWithPopulationAtLeast (2);
		
		final Building bigRaiderBuilding = new Building ();
		bigRaiderBuilding.setBuildingID ("BL03");
		bigRaiderBuilding.setInRaidersStartingCitiesWithPopulationAtLeast (7);

		final List<Building> buildings = new ArrayList<Building> ();
		buildings.add (wizardBuilding);
		buildings.add (smallRaiderBuilding);
		buildings.add (bigRaiderBuilding);
		when (db.getBuilding ()).thenReturn (buildings);
		
		// Session description
		final OverlandMapSize sys = createOverlandMapSize ();
		sys.setRaiderCityCount (2);

		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setWizardCityStartSize (4);
		difficultyLevel.setRaiderCityStartSizeMin (2);
		difficultyLevel.setRaiderCityStartSizeMax (11);
		difficultyLevel.setRaiderCityGrowthCap (8);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setDifficultyLevel (difficultyLevel);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		for (final MapAreaOfMemoryGridCells plane : trueTerrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					cell.setTerrainData (new OverlandMapTerrainData ());
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerPick myrran = new PlayerPick ();
		myrran.setPickID ("RT01");
		myrran.setQuantity (1);
		
		final KnownWizardDetails humanWizard = new KnownWizardDetails ();
		humanWizard.setWizardID (null);			// Custom wizard
		humanWizard.getPick ().add (myrran);		// Have to put something in here so that .equals () doesn't consider the human and AI pick lists to be the same
		
		final MomTransientPlayerPrivateKnowledge humanTrans = new MomTransientPlayerPrivateKnowledge ();
		humanTrans.setFirstCityRaceID ("RC01");
		
		final MomPersistentPlayerPrivateKnowledge humanPriv = new MomPersistentPlayerPrivateKnowledge ();
		humanPriv.setTaxRateID ("TR01");
		
		final PlayerDescription humanPd = new PlayerDescription ();
		humanPd.setPlayerType (PlayerType.HUMAN);
		humanPd.setPlayerID (5);
		humanPd.setPlayerName ("Human player");
		final PlayerServerDetails humanPlayer = new PlayerServerDetails (humanPd, null, humanPriv, null, humanTrans);

		final MomTransientPlayerPrivateKnowledge aiTrans = new MomTransientPlayerPrivateKnowledge ();
		aiTrans.setFirstCityRaceID ("RC02");

		final MomPersistentPlayerPrivateKnowledge aiPriv = new MomPersistentPlayerPrivateKnowledge ();
		aiPriv.setTaxRateID ("TR02");
		
		final KnownWizardDetails aiWizard = new KnownWizardDetails ();
		aiWizard.setWizardID ("WZ01");				// Standard wizard
		final PlayerDescription aiPd = new PlayerDescription ();
		aiPd.setPlayerType (PlayerType.AI);
		aiPd.setPlayerID (-1);
		aiPd.setPlayerName ("AI player");
		final PlayerServerDetails aiPlayer = new PlayerServerDetails (aiPd, null, aiPriv, null, aiTrans);

		final MomPersistentPlayerPrivateKnowledge raidersPriv = new MomPersistentPlayerPrivateKnowledge ();
		raidersPriv.setTaxRateID ("TR03");
		
		final KnownWizardDetails raidersWizard = new KnownWizardDetails ();
		raidersWizard.setWizardID (CommonDatabaseConstants.WIZARD_ID_RAIDERS);
		final PlayerDescription raidersPd = new PlayerDescription ();
		raidersPd.setPlayerType (PlayerType.AI);
		raidersPd.setPlayerID (-2);
		raidersPd.setPlayerName ("Raiders");
		final PlayerServerDetails raidersPlayer = new PlayerServerDetails (raidersPd, null, raidersPriv, null, null);

		final KnownWizardDetails monstersWizard = new KnownWizardDetails ();
		monstersWizard.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		final PlayerDescription monstersPd = new PlayerDescription ();
		monstersPd.setPlayerType (PlayerType.AI);
		monstersPd.setPlayerID (-3);
		monstersPd.setPlayerName ("Monsters");
		final PlayerServerDetails monstersPlayer = new PlayerServerDetails (monstersPd, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (humanPlayer);
		players.add (aiPlayer);
		players.add (raidersPlayer);
		players.add (monstersPlayer);
		
		// Wizard		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), humanPd.getPlayerID (), "createStartingCities")).thenReturn (humanWizard);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), aiPd.getPlayerID (), "createStartingCities")).thenReturn (aiWizard);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), raidersPd.getPlayerID (), "createStartingCities")).thenReturn (raidersWizard);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), monstersPd.getPlayerID (), "createStartingCities")).thenReturn (monstersWizard);
		
		// Who are wizards, raiders, monsters
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard (null)).thenReturn (true);		// custom wizard
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (true);
		when (playerKnowledgeUtils.isWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS)).thenReturn (false);
		when (playerKnowledgeUtils.isWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS)).thenReturn (false);
		
		// Human player chose Myrran retort; AI player did not
		final PlayerPickServerUtils playerPickServerUtils = mock (PlayerPickServerUtils.class);
		when (playerPickServerUtils.startingPlaneForWizard (humanWizard.getPick (), db)).thenReturn (1);
		when (playerPickServerUtils.startingPlaneForWizard (aiWizard.getPick (), db)).thenReturn (0);
		
		// Raider cities go on random planes, so put 1 on each
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (2)).thenReturn (0, 1);
		
		// Raider cities are random sizes
		when (random.nextInt (10)).thenReturn (3, 5);
		
		// Pick continental race for 1st raider city, and random race for 2nd
		when (random.nextInt (100)).thenReturn (74, 76);
		when (playerPickServerUtils.chooseRandomRaceForPlane (1, db)).thenReturn ("RC04");
		
		// Locations of each starter city
		final MomSessionVariables mom = mock (MomSessionVariables.class);

		final MapCoordinates3DEx humanLocation = new MapCoordinates3DEx (25, 15, 1);
		final MapCoordinates3DEx aiLocation = new MapCoordinates3DEx (40, 20, 0);
		final MapCoordinates3DEx raidersMyrrorLocation = new MapCoordinates3DEx (23, 13, 1);
		final MapCoordinates3DEx raidersArcanusLocation = new MapCoordinates3DEx (7, 27, 0);
		
		final CityAI cityAI = mock (CityAI.class);
		when (cityAI.chooseCityLocation (trueMap, 1, true, mom, "Starter city for \"Human player\"")).thenReturn (humanLocation);
		when (cityAI.chooseCityLocation (trueMap, 1, true, mom, "Starter city for \"Raiders\"")).thenReturn (raidersMyrrorLocation);
		when (cityAI.chooseCityLocation (trueMap, 0, true, mom, "Starter city for \"AI player\"")).thenReturn (aiLocation);
		when (cityAI.chooseCityLocation (trueMap, 0, true, mom, "Starter city for \"Raiders\"")).thenReturn (raidersArcanusLocation);
		
		// Race for each starter city - set these ALL to be the same race; then can test the % choice for this
		final MapArea3D<String> continentalRace = new MapArea3DArrayListImpl<String> ();
		continentalRace.setCoordinateSystem (sys);
		for (int x = 0; x < sys.getWidth (); x++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int z = 0; z < sys.getDepth (); z++)
					continentalRace.set (x, y, z, "RC03");
		
		final OverlandMapServerUtils overlandMapServerUtils = mock (OverlandMapServerUtils.class);
		when (overlandMapServerUtils.decideAllContinentalRaces (trueTerrain, sys, db)).thenReturn (continentalRace);
		
		// Rebels in each city
		final CityUnrestBreakdown humanRebels = new CityUnrestBreakdown ();
		humanRebels.setFinalTotal (1);
		final CityUnrestBreakdown aiRebels = new CityUnrestBreakdown ();
		aiRebels.setFinalTotal (2);
		final CityUnrestBreakdown raidersArcanusRebels = new CityUnrestBreakdown ();
		raidersArcanusRebels.setFinalTotal (3);
		final CityUnrestBreakdown raidersMyrrorRebels = new CityUnrestBreakdown ();
		raidersMyrrorRebels.setFinalTotal (4);
		
		final CityCalculations cityCalc = mock (CityCalculations.class);
		when (cityCalc.calculateCityRebels (trueMap, humanLocation, "TR01", db)).thenReturn (humanRebels);
		when (cityCalc.calculateCityRebels (trueMap, aiLocation, "TR02", db)).thenReturn (aiRebels);
		when (cityCalc.calculateCityRebels (trueMap, raidersArcanusLocation, "TR03", db)).thenReturn (raidersArcanusRebels);
		when (cityCalc.calculateCityRebels (trueMap, raidersMyrrorLocation, "TR03", db)).thenReturn (raidersMyrrorRebels);
		
		// City names
		when (overlandMapServerUtils.generateCityName (gsk, race1)).thenReturn ("Human city");
		when (overlandMapServerUtils.generateCityName (gsk, race2)).thenReturn ("AI city");
		when (overlandMapServerUtils.generateCityName (gsk, race3)).thenReturn ("Raider city I");
		when (overlandMapServerUtils.generateCityName (gsk, race4)).thenReturn ("Raider city II");
		
		// Session variables
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final ServerCityCalculations serverCityCalc = mock (ServerCityCalculations.class);
		
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setServerCityCalculations (serverCityCalc);
		proc.setCityCalculations (cityCalc);
		proc.setOverlandMapServerUtils (overlandMapServerUtils);
		proc.setPlayerPickServerUtils (playerPickServerUtils);
		proc.setCityAI (cityAI);
		proc.setRandomUtils (random);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		proc.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		proc.createStartingCities (mom);
		
		// Check human city
		final ServerGridCellEx humanCell = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		final OverlandMapCityData humanCity = humanCell.getCityData ();
		assertNotNull (humanCity);
		assertEquals (humanPd.getPlayerID ().intValue (), humanCity.getCityOwnerID ());
		assertEquals (0, humanCity.getOptionalFarmers ());
		assertEquals (4000, humanCity.getCityPopulation ());
		assertNull (humanCell.getRaiderCityAdditionalPopulationCap ());
		assertEquals ("RC01", humanCity.getCityRaceID ());
		assertEquals ("Human city", humanCity.getCityName ());
		assertEquals (1, humanCity.getNumberOfRebels ());
		assertEquals (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT, humanCity.getCurrentlyConstructingBuildingID ());
		assertNull (humanCity.getCurrentlyConstructingUnitID ());
		
		// Check AI city
		final ServerGridCellEx aiCell = (ServerGridCellEx) trueTerrain.getPlane ().get (0).getRow ().get (20).getCell ().get (40);
		final OverlandMapCityData aiCity = aiCell.getCityData ();
		assertNotNull (aiCity);
		assertEquals (aiPd.getPlayerID ().intValue (), aiCity.getCityOwnerID ());
		assertEquals (0, aiCity.getOptionalFarmers ());
		assertEquals (4000, aiCity.getCityPopulation ());
		assertNull (aiCell.getRaiderCityAdditionalPopulationCap ());
		assertEquals ("RC02", aiCity.getCityRaceID ());
		assertEquals ("AI city", aiCity.getCityName ());
		assertEquals (2, aiCity.getNumberOfRebels ());
		assertEquals (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT, aiCity.getCurrentlyConstructingBuildingID ());
		assertNull (aiCity.getCurrentlyConstructingUnitID ());
		
		// Check raiders arcanus city
		final ServerGridCellEx raidersArcanusCell = (ServerGridCellEx) trueTerrain.getPlane ().get (0).getRow ().get (27).getCell ().get (7);
		final OverlandMapCityData raidersArcanusCity = raidersArcanusCell.getCityData ();
		assertNotNull (raidersArcanusCity);
		assertEquals (raidersPd.getPlayerID ().intValue (), raidersArcanusCity.getCityOwnerID ());
		assertEquals (0, raidersArcanusCity.getOptionalFarmers ());
		assertEquals (5000, raidersArcanusCity.getCityPopulation ());
		assertEquals (13000, raidersArcanusCell.getRaiderCityAdditionalPopulationCap ().intValue ());
		assertEquals ("RC03", raidersArcanusCity.getCityRaceID ());
		assertEquals ("Raider city I", raidersArcanusCity.getCityName ());
		assertEquals (3, raidersArcanusCity.getNumberOfRebels ());
		assertEquals (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT, raidersArcanusCity.getCurrentlyConstructingBuildingID ());
		assertNull (raidersArcanusCity.getCurrentlyConstructingUnitID ());
		
		// Check raiders myrror city		
		final ServerGridCellEx raidersMyrrorCell = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (13).getCell ().get (23);
		final OverlandMapCityData raidersMyrrorCity = raidersMyrrorCell.getCityData ();
		assertNotNull (raidersMyrrorCity);
		assertEquals (raidersPd.getPlayerID ().intValue (), raidersMyrrorCity.getCityOwnerID ());
		assertEquals (0, raidersMyrrorCity.getOptionalFarmers ());
		assertEquals (7000, raidersMyrrorCity.getCityPopulation ());
		assertEquals (15000, raidersMyrrorCell.getRaiderCityAdditionalPopulationCap ().intValue ());
		assertEquals ("RC04", raidersMyrrorCity.getCityRaceID ());
		assertEquals ("Raider city II", raidersMyrrorCity.getCityName ());
		assertEquals (4, raidersMyrrorCity.getNumberOfRebels ());
		assertEquals (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT, raidersMyrrorCity.getCurrentlyConstructingBuildingID ());
		assertNull (raidersMyrrorCity.getCurrentlyConstructingUnitID ());
		
		// Check no other cities got created
		int count = 0;
		for (final MapAreaOfMemoryGridCells plane : trueTerrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					if (cell.getCityData () != null)
						count++;
		
		assertEquals (4, count);
		
		// Check calcs were done
		verify (serverCityCalc, times (4)).calculateCitySizeIDAndMinimumFarmers (any (MapCoordinates3DEx.class), eq (mom));		
		verify (serverCityCalc, times (4)).ensureNotTooManyOptionalFarmers (any (OverlandMapCityData.class));
		
		// Check units were added
		verify (midTurn, times (2)).addUnitOnServerAndClients ("UN001", humanLocation, humanLocation, null, null,
			humanPlayer, UnitStatusID.ALIVE, false, mom);
		verify (midTurn, times (2)).addUnitOnServerAndClients ("UN002", aiLocation, aiLocation, null, null,
			aiPlayer, UnitStatusID.ALIVE, false, mom);
		verify (midTurn, times (2)).addUnitOnServerAndClients ("UN003", raidersArcanusLocation, raidersArcanusLocation, null, null,
			raidersPlayer, UnitStatusID.ALIVE, false, mom);
		verify (midTurn, times (2)).addUnitOnServerAndClients ("UN004", raidersMyrrorLocation, raidersMyrrorLocation, null, null,
			raidersPlayer, UnitStatusID.ALIVE, false, mom);
		
		// Check buildings were added.
		// Note players intentionally left as null so that building is only added on the server.
		verify (midTurn).addBuildingOnServerAndClients (humanLocation, Arrays.asList ("BL01"), null, null, false, mom);
		verify (midTurn).addBuildingOnServerAndClients (aiLocation, Arrays.asList ("BL01"), null, null, false, mom);
		verify (midTurn).addBuildingOnServerAndClients (raidersArcanusLocation, Arrays.asList ("BL02"), null, null, false, mom);
		verify (midTurn).addBuildingOnServerAndClients (raidersMyrrorLocation, Arrays.asList ("BL02"), null, null, false, mom);
		verify (midTurn).addBuildingOnServerAndClients (raidersMyrrorLocation, Arrays.asList ("BL03"), null, null, false, mom);
		
		verifyNoMoreInteractions (serverCityCalc);
		verifyNoMoreInteractions (midTurn);
	}
	
	/**
	 * Tests the progressConstructionProjects method when we didn't finish the building we're constructing
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressConstructionProjects_Building_Unfinished () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize sys = createOverlandMapSize ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (null, null, priv, null, null);
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, 1, "progressConstructionProjects")).thenReturn (cityOwner);		
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		cityData.setCurrentlyConstructingBuildingID ("BL01");
		cityData.setProductionSoFar (500);
		cityData.setCityPopulation (1000);
		
		trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25).setCityData (cityData);
		
		// Production each turn
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		when (cityCalculations.calculateSingleCityProduction (players, trueMap,
			new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, true, db, CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION)).thenReturn (100);
		
		final CityProductionCalculations cityProductionCalculations = mock (CityProductionCalculations.class);
		when (cityProductionCalculations.calculateProductionCost (players, priv.getFogOfWarMemory (), new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, db, null)).thenReturn (1000);
		
		// No event
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setCityCalculations (cityCalculations);
		proc.setCityProductionCalculations (cityProductionCalculations);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		proc.progressConstructionProjects (0, mom);
		
		// Check results
		assertEquals (600, cityData.getProductionSoFar ());
		
		verify (wu).recalculateCity (new MapCoordinates3DEx (25, 15, 1));
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (wu);
		verifyNoMoreInteractions (cityCalculations);
	}
	
	/**
	 * Tests the progressConstructionProjects method when we didn't finish the unit we're constructing
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressConstructionProjects_Unit_Unfinished () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize sys = createOverlandMapSize ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (null, null, priv, null, null);
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, 1, "progressConstructionProjects")).thenReturn (cityOwner);		
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		cityData.setCurrentlyConstructingUnitID ("UN001");
		cityData.setProductionSoFar (500);
		cityData.setCityPopulation (1000);
		
		trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25).setCityData (cityData);
		
		// Production each turn
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		when (cityCalculations.calculateSingleCityProduction (players, trueMap,
			new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, true, db, CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION)).thenReturn (100);
		
		final CityProductionCalculations cityProductionCalculations = mock (CityProductionCalculations.class);
		when (cityProductionCalculations.calculateProductionCost (players, priv.getFogOfWarMemory (), new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, db, null)).thenReturn (1000);
		
		// No event
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setCityCalculations (cityCalculations);
		proc.setCityProductionCalculations (cityProductionCalculations);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		proc.progressConstructionProjects (0, mom);
		
		// Check results
		assertEquals (600, cityData.getProductionSoFar ());
		
		verify (wu).recalculateCity (new MapCoordinates3DEx (25, 15, 1));
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (wu);
		verifyNoMoreInteractions (cityCalculations);
	}
	
	/**
	 * Tests the progressConstructionProjects method when we finish the building we're constructing
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressConstructionProjects_Building_Finished () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Building building = new Building ();
		building.setBuildingID ("BL01");
		building.setProductionCost (1000);
		when (db.findBuilding ("BL01", "progressConstructionProjects")).thenReturn (building);
		
		// Session description
		final OverlandMapSize sys = createOverlandMapSize ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerType (PlayerType.HUMAN);

		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (pd, null, priv, null, trans);
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, 1, "progressConstructionProjects")).thenReturn (cityOwner);		
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		cityData.setCurrentlyConstructingBuildingID ("BL01");
		cityData.setProductionSoFar (950);
		cityData.setCityPopulation (1000);
		
		trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25).setCityData (cityData);
		
		// Production each turn
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		when (cityCalculations.calculateSingleCityProduction (players, trueMap,
			new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, true, db, CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION)).thenReturn (100);
		
		final CityProductionCalculations cityProductionCalculations = mock (CityProductionCalculations.class);
		when (cityProductionCalculations.calculateProductionCost (players, priv.getFogOfWarMemory (), new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, db, null)).thenReturn (1000);
		
		// No event
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setCityCalculations (cityCalculations);
		proc.setCityProductionCalculations (cityProductionCalculations);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		
		// Run method
		proc.progressConstructionProjects (0, mom);
		
		// Check results
		assertEquals (0, cityData.getProductionSoFar ());
		
		assertEquals (1, trans.getNewTurnMessage ().size ());
		assertSame (NewTurnMessageConstructBuilding.class, trans.getNewTurnMessage ().get (0).getClass ());
		final NewTurnMessageConstructBuilding ntm = (NewTurnMessageConstructBuilding) trans.getNewTurnMessage ().get (0);
		assertEquals (NewTurnMessageTypeID.COMPLETED_BUILDING, ntm.getMsgType ());
		assertEquals ("BL01", ntm.getBuildingID ());
		assertEquals (new MapCoordinates3DEx (25, 15, 1), ntm.getCityLocation ());

		verify (midTurn).addBuildingOnServerAndClients (new MapCoordinates3DEx (25, 15, 1), Arrays.asList ("BL01"), null, null, true, mom);
		
		verify (wu).recalculateCity (new MapCoordinates3DEx (25, 15, 1));
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (wu);
		verifyNoMoreInteractions (midTurn);
		verifyNoMoreInteractions (cityCalculations);
	}
	
	/**
	 * Tests the progressConstructionProjects method when we finish the unit we're constructing
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressConstructionProjects_Unit_Finished () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unit = new UnitEx ();
		unit.setUnitID ("UN001");
		unit.setProductionCost (1000);
		when (db.findUnit ("UN001", "progressConstructionProjects")).thenReturn (unit);				
		
		// Session description
		final OverlandMapSize sys = createOverlandMapSize ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerType (PlayerType.HUMAN);
		
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (pd, null, priv, null, trans);
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, 1, "progressConstructionProjects")).thenReturn (cityOwner);		
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		cityData.setCurrentlyConstructingUnitID ("UN001");
		cityData.setProductionSoFar (950);
		cityData.setCityPopulation (1000);
		
		trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25).setCityData (cityData);

		// No event
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Production each turn
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		when (cityCalculations.calculateSingleCityProduction (players, trueMap,
			new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, true, db, CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION)).thenReturn (100);
		
		final CityProductionCalculations cityProductionCalculations = mock (CityProductionCalculations.class);
		when (cityProductionCalculations.calculateProductionCost (players, priv.getFogOfWarMemory (), new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, db, null)).thenReturn (1000);
		
		// Where the unit will appear
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.findNearestLocationWhereUnitCanBeAdded (new MapCoordinates3DEx (25, 15, 1), "UN001", 1, mom)).thenReturn
			(new UnitAddLocation (new MapCoordinates3DEx (26, 15, 1), UnitAddBumpTypeID.BUMPED));
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);

		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setCityCalculations (cityCalculations);
		proc.setCityProductionCalculations (cityProductionCalculations);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setUnitServerUtils (unitServerUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Run method
		proc.progressConstructionProjects (0, mom);
		
		// Check results
		assertEquals (0, cityData.getProductionSoFar ());
		assertEquals ("UN001", cityData.getCurrentlyConstructingUnitID ());
		
		assertEquals (1, trans.getNewTurnMessage ().size ());
		assertSame (NewTurnMessageConstructUnit.class, trans.getNewTurnMessage ().get (0).getClass ());
		final NewTurnMessageConstructUnit ntm = (NewTurnMessageConstructUnit) trans.getNewTurnMessage ().get (0);
		assertEquals (NewTurnMessageTypeID.COMPLETED_UNIT, ntm.getMsgType ());
		assertEquals ("UN001", ntm.getUnitID ());
		assertEquals (new MapCoordinates3DEx (25, 15, 1), ntm.getCityLocation ());

		verify (midTurn).addUnitOnServerAndClients ("UN001", new MapCoordinates3DEx (26, 15, 1), new MapCoordinates3DEx (25, 15, 1), null, null,
			cityOwner, UnitStatusID.ALIVE, true, mom);
		
		verify (wu).recalculateCity (new MapCoordinates3DEx (25, 15, 1));
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (wu);
		verifyNoMoreInteractions (midTurn);
		verifyNoMoreInteractions (cityCalculations);
	}
	
	/**
	 * Tests the growCities method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGrowCities () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final OverlandMapSize sys = createOverlandMapSize ();
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setDifficultyLevel (difficultyLevel);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerType (PlayerType.HUMAN);
		pd.setPlayerID (1);
		
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");
		
		final PlayerServerDetails cityOwnerPlayer = new PlayerServerDetails (pd, null, priv, null, trans);
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd.getPlayerID (), "growCities")).thenReturn (cityOwnerPlayer);
		
		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), pd.getPlayerID (), "growCities")).thenReturn (wizardDetails);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (4950);
		
		trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25).setCityData (cityData);
		
		// Max city size
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		when (cityCalculations.calculateSingleCityProduction (players, trueMap,
			new MapCoordinates3DEx (25, 15, 1), "TR01", sd, null, true, db, CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD)).thenReturn (9000);
		
		// Growth rate
		final CityGrowthRateBreakdown growthRate = new CityGrowthRateBreakdown ();
		growthRate.setCappedTotal (100);
		
		when (cityCalculations.calculateCityGrowthRate (players, trueMap,
			new MapCoordinates3DEx (25, 15, 1), 9000, difficultyLevel, db)).thenReturn (growthRate);
		
		// No event
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getWorldUpdates ()).thenReturn (wu);

		// Set up object to test
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setCityCalculations (cityCalculations);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		proc.growCities (1, mom);
		
		// Check results
		assertEquals (5050, cityData.getCityPopulation ());
		
		assertEquals (1, trans.getNewTurnMessage ().size ());
		assertSame (NewTurnMessagePopulationChange.class, trans.getNewTurnMessage ().get (0).getClass ());
		final NewTurnMessagePopulationChange ntm = (NewTurnMessagePopulationChange) trans.getNewTurnMessage ().get (0);
		assertEquals (NewTurnMessageTypeID.POPULATION_CHANGE, ntm.getMsgType ());
		assertEquals (new MapCoordinates3DEx (25, 15, 1), ntm.getCityLocation ());
		assertEquals (4950, ntm.getOldPopulation ());
		assertEquals (5050, ntm.getNewPopulation ());
		
		verify (wu).recalculateCity (new MapCoordinates3DEx (25, 15, 1));
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (wu);
		verifyNoMoreInteractions (cityCalculations);
	}
	
	/**
	 * Tests the sellBuilding method in the typical situation of selling a building voluntarily in a one-at-a-time turns game
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Building granaryDef = new Building ();
		when (db.findBuilding (GRANARY, "sellBuilding")).thenReturn (granaryDef);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setPlayerType (PlayerType.HUMAN);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR");
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (pd, null, priv, null, null);
		
		players.add (cityOwner);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd.getPlayerID (), "sellBuilding")).thenReturn (cityOwner);
		
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// City data
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);
		tc.setCityData (cityData);
		
		// Building being sold
		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingURN (3);
		granary.setBuildingID (GRANARY);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuildingURN (granary.getBuildingURN (), trueMap.getBuilding (), "sellBuilding")).thenReturn (granary);
		
		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final ServerCityCalculations serverCityCalculations = mock (ServerCityCalculations.class);
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerCityCalculations (serverCityCalculations);
		proc.setCityCalculations (cityCalculations);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		final CityUnrestBreakdown unrest = new CityUnrestBreakdown ();
		unrest.setFinalTotal (3);

		when (memoryBuildingUtils.isBuildingAPrerequisiteForBuilding (GRANARY, BARRACKS, db)).thenReturn (false);
		when (memoryBuildingUtils.goldFromSellingBuilding (granaryDef)).thenReturn (12);
		
		cityData.setCurrentlyConstructingBuildingID (BARRACKS);
		proc.sellBuilding (cityLocation, granary.getBuildingURN (), false, true, mom);
		
		// Check results
		assertEquals (BARRACKS, cityData.getCurrentlyConstructingBuildingID ());	// i.e. it didn't change
		assertEquals (GRANARY, tc.getBuildingIdSoldThisTurn ());
		//assertEquals (3, cityData.getNumberOfRebels ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (Arrays.asList (granary.getBuildingURN ()), true, null, null, null, mom);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 12);
		verify (wu).recalculateCity (cityLocation);
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (serverCityCalculations);
		verifyNoMoreInteractions (resourceValueUtils);
		verifyNoMoreInteractions (midTurn);
		verifyNoMoreInteractions (wu);
	}

	/**
	 * Tests the sellBuilding method when we were forced to sell it (couldn't afford gold maintenance)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_Forced () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Building granaryDef = new Building ();
		when (db.findBuilding (GRANARY, "sellBuilding")).thenReturn (granaryDef);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setPlayerType (PlayerType.HUMAN);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR");
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (pd, null, priv, null, null);
		
		players.add (cityOwner);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd.getPlayerID (), "sellBuilding")).thenReturn (cityOwner);
		
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// City data
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);
		tc.setCityData (cityData);

		// Building being sold
		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingURN (3);
		granary.setBuildingID (GRANARY);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuildingURN (granary.getBuildingURN (), trueMap.getBuilding (), "sellBuilding")).thenReturn (granary);
		
		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final ServerCityCalculations serverCityCalculations = mock (ServerCityCalculations.class);
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerCityCalculations (serverCityCalculations);
		proc.setCityCalculations (cityCalculations);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		final CityUnrestBreakdown unrest = new CityUnrestBreakdown ();
		unrest.setFinalTotal (3);

		when (memoryBuildingUtils.isBuildingAPrerequisiteForBuilding (GRANARY, BARRACKS, db)).thenReturn (false);
		when (memoryBuildingUtils.goldFromSellingBuilding (granaryDef)).thenReturn (12);
		
		cityData.setCurrentlyConstructingBuildingID (BARRACKS);
		proc.sellBuilding (cityLocation, granary.getBuildingURN (), false, false, mom);
		
		// Check results
		assertEquals (BARRACKS, cityData.getCurrentlyConstructingBuildingID ());	// i.e. it didn't change
		assertNull (tc.getBuildingIdSoldThisTurn ());		// Isn't updated because it was a forced sale
		//assertEquals (3, cityData.getNumberOfRebels ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (Arrays.asList (granary.getBuildingURN ()), false, null, null, null, mom);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 12);
		verify (wu).recalculateCity (cityLocation);
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (serverCityCalculations);
		verifyNoMoreInteractions (resourceValueUtils);
		verifyNoMoreInteractions (midTurn);
		verifyNoMoreInteractions (wu);
	}

	/**
	 * Tests the sellBuilding method when we're currently trying to construct a building that requires the one being sold
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_ConstructionBasedOn () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Building granaryDef = new Building ();
		when (db.findBuilding (GRANARY, "sellBuilding")).thenReturn (granaryDef);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setPlayerType (PlayerType.HUMAN);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR");
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (pd, null, priv, null, null);
		
		players.add (cityOwner);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd.getPlayerID (), "sellBuilding")).thenReturn (cityOwner);
		
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// City data
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);
		tc.setCityData (cityData);
		
		// Building being sold
		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingURN (3);
		granary.setBuildingID (GRANARY);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuildingURN (granary.getBuildingURN (), trueMap.getBuilding (), "sellBuilding")).thenReturn (granary);
		
		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final ServerCityCalculations serverCityCalculations = mock (ServerCityCalculations.class);
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerCityCalculations (serverCityCalculations);
		proc.setCityCalculations (cityCalculations);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
				
		// Run test
		final CityUnrestBreakdown unrest = new CityUnrestBreakdown ();
		unrest.setFinalTotal (3);

		when (memoryBuildingUtils.isBuildingAPrerequisiteForBuilding (GRANARY, FARMERS_MARKET, db)).thenReturn (true);
		when (memoryBuildingUtils.goldFromSellingBuilding (granaryDef)).thenReturn (12);
		
		cityData.setCurrentlyConstructingBuildingID (FARMERS_MARKET);
		proc.sellBuilding (cityLocation, granary.getBuildingURN (), false, true, mom);
		
		// Check results
		assertEquals (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT, cityData.getCurrentlyConstructingBuildingID ());	// Can't build Farmers' Market anymore
		assertEquals (GRANARY, tc.getBuildingIdSoldThisTurn ());
		//assertEquals (3, cityData.getNumberOfRebels ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (Arrays.asList (granary.getBuildingURN ()), true, null, null, null, mom);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 12);
		verify (wu).recalculateCity (cityLocation);
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (serverCityCalculations);
		verifyNoMoreInteractions (resourceValueUtils);
		verifyNoMoreInteractions (midTurn);
		verifyNoMoreInteractions (wu);
	}

	/**
	 * Tests the sellBuilding method when it is handled as a pending sale, i.e. for simultaneous turns games
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_Pending () throws Exception
	{
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setPlayerType (PlayerType.HUMAN);
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (pd, null, null, null, null);
		
		players.add (cityOwner);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd.getPlayerID (), "sellBuilding")).thenReturn (cityOwner);
		
		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		cityOwner.setConnection (msgs);
		
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// City data
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);
		tc.setCityData (cityData);
		
		// Building being sold
		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingURN (3);
		granary.setBuildingID (GRANARY);

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuildingURN (granary.getBuildingURN (), trueMap.getBuilding (), "sellBuilding")).thenReturn (granary);
		
		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Set up object to test
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// Run test
		proc.sellBuilding ( cityLocation, granary.getBuildingURN (), true, true, mom);
		
		// Check results
		assertEquals (GRANARY, tc.getBuildingIdSoldThisTurn ());
		assertEquals (1, msgs.getMessages ().size ());
		final PendingSaleMessage msg = (PendingSaleMessage) msgs.getMessages ().get (0);
		assertEquals (cityLocation, msg.getCityLocation ());
		assertEquals (granary.getBuildingURN (), msg.getBuildingURN ().intValue ());
	}
	
	/**
	 * Tests the changeTaxRate method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChangeTaxRate () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlane ()).thenReturn (planes);
		
		final TaxRate taxRate = new TaxRate ();
		taxRate.setTaxRateID ("TR03");
		
		when (db.getTaxRate ()).thenReturn (Arrays.asList (taxRate));
		
		// Session description
		final OverlandMapSize sys = createOverlandMapSize ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setPlayerType (PlayerType.HUMAN);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		players.add (player);
		
		// One of our cities
		final MapCoordinates3DEx cityLocation1 = new MapCoordinates3DEx (23, 15, 0);
		
		final OverlandMapCityData cityData1 = new OverlandMapCityData ();
		cityData1.setCityOwnerID (3);
		cityData1.setCityPopulation (1000);
		trueTerrain.getPlane ().get (0).getRow ().get (15).getCell ().get (23).setCityData (cityData1);
		
		final CityUnrestBreakdown breakdown1 = new CityUnrestBreakdown ();
		breakdown1.setFinalTotal (4);
		
		// Another of our cities
		final MapCoordinates3DEx cityLocation2 = new MapCoordinates3DEx (24, 15, 0);
		
		final OverlandMapCityData cityData2 = new OverlandMapCityData ();
		cityData2.setCityOwnerID (3);
		cityData2.setCityPopulation (1000);
		trueTerrain.getPlane ().get (0).getRow ().get (15).getCell ().get (24).setCityData (cityData2);
		
		final CityUnrestBreakdown breakdown2 = new CityUnrestBreakdown ();
		breakdown2.setFinalTotal (5);
		
		// Someone else's city
		final OverlandMapCityData cityData3 = new OverlandMapCityData ();
		cityData3.setCityOwnerID (4);
		cityData3.setCityPopulation (1000);
		trueTerrain.getPlane ().get (0).getRow ().get (15).getCell ().get (25).setCityData (cityData3);
		
		final CityUnrestBreakdown breakdown3 = new CityUnrestBreakdown ();
		breakdown3.setFinalTotal (6);

		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		// Set up object to test
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);

		// Have to use anyObject () for location since .equals () doesn't give correct result
		final CityCalculations cityCalculations = mock (CityCalculations.class);
		when (cityCalculations.calculateCityRebels (trueMap, cityLocation1, "TR03", db)).thenReturn (breakdown1);
		when (cityCalculations.calculateCityRebels (trueMap, cityLocation2, "TR03", db)).thenReturn (breakdown2);
		
		final ServerCityCalculations serverCityCalculations = mock (ServerCityCalculations.class);
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setServerResourceCalculations (serverResourceCalculations);
		proc.setCityCalculations (cityCalculations);
		proc.setServerCityCalculations (serverCityCalculations);
		proc.setFogOfWarMidTurnChanges (midTurn);
		
		// Run test
		proc.changeTaxRate (player, "TR03", mom);

		// Check results
		assertEquals (1, msgs.getMessages ().size ());
		assertEquals (TaxRateChangedMessage.class.getName (), msgs.getMessages ().get (0).getClass ().getName ());
		final TaxRateChangedMessage msg = (TaxRateChangedMessage) msgs.getMessages ().get (0);
		assertEquals ("TR03", msg.getTaxRateID ());
		
		assertEquals (4, cityData1.getNumberOfRebels ());
		verify (serverCityCalculations, times (1)).ensureNotTooManyOptionalFarmers (cityData1);
		verify (midTurn, times (1)).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation1, sd.getFogOfWarSetting ());

		assertEquals (5, cityData2.getNumberOfRebels ());
		verify (serverCityCalculations, times (1)).ensureNotTooManyOptionalFarmers (cityData2);
		verify (midTurn, times (1)).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation2, sd.getFogOfWarSetting ());

		assertEquals (0, cityData3.getNumberOfRebels ());
		
		verifyNoMoreInteractions (serverCityCalculations);
		verifyNoMoreInteractions (midTurn);
	}
}