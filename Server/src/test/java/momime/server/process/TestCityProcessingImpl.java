package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.FogOfWarSetting;
import momime.common.database.OverlandMapSize;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TaxRate;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessagePopulationChange;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitAddBumpTypeID;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.PendingSaleMessage;
import momime.common.messages.servertoclient.TaxRateChangedMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.ai.CityAI;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.BuildingSvr;
import momime.server.database.PlaneSvr;
import momime.server.database.RaceSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.UnitSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

/**
 * Tests the CityProcessingImpl class
 */
public final class TestCityProcessingImpl
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
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final PlaneSvr arcanus = new PlaneSvr ();
		final PlaneSvr myrror = new PlaneSvr ();
		myrror.setPlaneNumber (1);
		
		final List<PlaneSvr> planes = new ArrayList<PlaneSvr> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlanes ()).thenReturn (planes);
		
		final RaceSvr race1 = new RaceSvr ();
		final RaceSvr race2 = new RaceSvr ();
		final RaceSvr race3 = new RaceSvr ();
		final RaceSvr race4 = new RaceSvr ();
		when (db.findRace ("RC01", "createStartingCities")).thenReturn (race1);
		when (db.findRace ("RC02", "createStartingCities")).thenReturn (race2);
		when (db.findRace ("RC03", "createStartingCities")).thenReturn (race3);
		when (db.findRace ("RC04", "createStartingCities")).thenReturn (race4);
		
		// Units free at the start of game for each race
		final List<UnitSvr> units = new ArrayList<UnitSvr> ();
		for (int race = 1; race <= 4; race++)
		{
			final UnitSvr unit = new UnitSvr ();
			unit.setUnitID ("UN00" + race);
			unit.setFreeAtStartCount (2);
			unit.setUnitRaceID ("RC0" + race);
			units.add (unit);
		}
		
		when (db.getUnits ()).thenReturn (units);
		
		// Buildings free at the start of the game
		final BuildingSvr wizardBuilding = new BuildingSvr ();
		wizardBuilding.setBuildingID ("BL01");
		wizardBuilding.setInWizardsStartingCities (true);

		final BuildingSvr smallRaiderBuilding = new BuildingSvr ();
		smallRaiderBuilding.setBuildingID ("BL02");
		smallRaiderBuilding.setInRaidersStartingCitiesWithPopulationAtLeast (2);
		
		final BuildingSvr bigRaiderBuilding = new BuildingSvr ();
		bigRaiderBuilding.setBuildingID ("BL03");
		bigRaiderBuilding.setInRaidersStartingCitiesWithPopulationAtLeast (7);

		final List<BuildingSvr> buildings = new ArrayList<BuildingSvr> ();
		buildings.add (wizardBuilding);
		buildings.add (smallRaiderBuilding);
		buildings.add (bigRaiderBuilding);
		when (db.getBuildings ()).thenReturn (buildings);
		
		// Session description
		final OverlandMapSize sys = ServerTestData.createOverlandMapSize ();
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
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerPick myrran = new PlayerPick ();
		myrran.setPickID ("RT01");
		myrran.setQuantity (1);
		
		final MomPersistentPlayerPublicKnowledge humanPpk = new MomPersistentPlayerPublicKnowledge ();
		humanPpk.setWizardID ("");				// Custom wizard
		humanPpk.getPick ().add (myrran);		// Have to put something in here so that .equals () doesn't consider the human and AI pick lists to be the same
		
		final MomTransientPlayerPrivateKnowledge humanTrans = new MomTransientPlayerPrivateKnowledge ();
		humanTrans.setFirstCityRaceID ("RC01");
		
		final MomPersistentPlayerPrivateKnowledge humanPriv = new MomPersistentPlayerPrivateKnowledge ();
		humanPriv.setTaxRateID ("TR01");
		
		final PlayerDescription humanPd = new PlayerDescription ();
		humanPd.setHuman (true);
		humanPd.setPlayerID (5);
		final PlayerServerDetails humanPlayer = new PlayerServerDetails (humanPd, humanPpk, humanPriv, null, humanTrans);

		final MomTransientPlayerPrivateKnowledge aiTrans = new MomTransientPlayerPrivateKnowledge ();
		aiTrans.setFirstCityRaceID ("RC02");

		final MomPersistentPlayerPrivateKnowledge aiPriv = new MomPersistentPlayerPrivateKnowledge ();
		aiPriv.setTaxRateID ("TR02");
		
		final MomPersistentPlayerPublicKnowledge aiPpk = new MomPersistentPlayerPublicKnowledge ();
		aiPpk.setWizardID ("WZ01");				// Standard wizard
		final PlayerDescription aiPd = new PlayerDescription ();
		aiPd.setHuman (false);
		aiPd.setPlayerID (-1);
		final PlayerServerDetails aiPlayer = new PlayerServerDetails (aiPd, aiPpk, aiPriv, null, aiTrans);

		final MomPersistentPlayerPrivateKnowledge raidersPriv = new MomPersistentPlayerPrivateKnowledge ();
		raidersPriv.setTaxRateID ("TR03");
		
		final MomPersistentPlayerPublicKnowledge raidersPpk = new MomPersistentPlayerPublicKnowledge ();
		raidersPpk.setWizardID (CommonDatabaseConstants.WIZARD_ID_RAIDERS);
		final PlayerDescription raidersPd = new PlayerDescription ();
		raidersPd.setHuman (false);
		raidersPd.setPlayerID (-2);
		final PlayerServerDetails raidersPlayer = new PlayerServerDetails (raidersPd, raidersPpk, raidersPriv, null, null);

		final MomPersistentPlayerPublicKnowledge monstersPpk = new MomPersistentPlayerPublicKnowledge ();
		monstersPpk.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		final PlayerDescription monstersPd = new PlayerDescription ();
		monstersPd.setHuman (false);
		monstersPd.setPlayerID (-3);
		final PlayerServerDetails monstersPlayer = new PlayerServerDetails (monstersPd, monstersPpk, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (humanPlayer);
		players.add (aiPlayer);
		players.add (raidersPlayer);
		players.add (monstersPlayer);
		
		// Human player chose Myrran retort; AI player did not
		final PlayerPickServerUtils playerPickServerUtils = mock (PlayerPickServerUtils.class);
		when (playerPickServerUtils.startingPlaneForWizard (humanPpk.getPick (), db)).thenReturn (1);
		when (playerPickServerUtils.startingPlaneForWizard (aiPpk.getPick (), db)).thenReturn (0);
		
		humanPpk.getPick ().equals (aiPpk.getPick ());
		
		// Raider cities go on random planes, so put 1 on each
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (2)).thenReturn (0, 1);
		
		// Raider cities are random sizes
		when (random.nextInt (10)).thenReturn (3, 5);
		
		// Pick continental race for 1st raider city, and random race for 2nd
		when (random.nextInt (100)).thenReturn (74, 76);
		when (playerPickServerUtils.chooseRandomRaceForPlane (1, db)).thenReturn ("RC04");
		
		// Locations of each starter city
		final MapCoordinates3DEx humanLocation = new MapCoordinates3DEx (25, 15, 1);
		final MapCoordinates3DEx aiLocation = new MapCoordinates3DEx (40, 20, 0);
		final MapCoordinates3DEx raidersMyrrorLocation = new MapCoordinates3DEx (23, 13, 1);
		final MapCoordinates3DEx raidersArcanusLocation = new MapCoordinates3DEx (7, 27, 0);
		
		final CityAI cityAI = mock (CityAI.class);
		when (cityAI.chooseCityLocation (trueTerrain, 1, sd, db)).thenReturn (humanLocation, raidersMyrrorLocation);
		when (cityAI.chooseCityLocation (trueTerrain, 0, sd, db)).thenReturn (aiLocation, raidersArcanusLocation);
		
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
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), humanLocation, "TR01", db)).thenReturn (humanRebels);
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), aiLocation, "TR02", db)).thenReturn (aiRebels);
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), raidersArcanusLocation, "TR03", db)).thenReturn (raidersArcanusRebels);
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), raidersMyrrorLocation, "TR03", db)).thenReturn (raidersMyrrorRebels);
		
		// City names
		when (overlandMapServerUtils.generateCityName (gsk, race1)).thenReturn ("Human city");
		when (overlandMapServerUtils.generateCityName (gsk, race2)).thenReturn ("AI city");
		when (overlandMapServerUtils.generateCityName (gsk, race3)).thenReturn ("Raider city I");
		when (overlandMapServerUtils.generateCityName (gsk, race4)).thenReturn ("Raider city II");
		
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
		
		// Run method
		proc.createStartingCities (players, gsk, sd, db);
		
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
		verify (serverCityCalc, times (4)).calculateCitySizeIDAndMinimumFarmers (eq (players), eq (trueTerrain), eq (trueMap.getBuilding ()),
			any (MapCoordinates3DEx.class), eq (sd), eq (db));
		
		verify (serverCityCalc, times (4)).ensureNotTooManyOptionalFarmers (any (OverlandMapCityData.class));
		
		// Check units were added
		verify (midTurn, times (2)).addUnitOnServerAndClients (gsk, "UN001", humanLocation, humanLocation, null, humanPlayer, UnitStatusID.ALIVE, null, sd, db);
		verify (midTurn, times (2)).addUnitOnServerAndClients (gsk, "UN002", aiLocation, aiLocation, null, aiPlayer, UnitStatusID.ALIVE, null, sd, db);
		verify (midTurn, times (2)).addUnitOnServerAndClients (gsk, "UN003", raidersArcanusLocation, raidersArcanusLocation, null, raidersPlayer, UnitStatusID.ALIVE, null, sd, db);
		verify (midTurn, times (2)).addUnitOnServerAndClients (gsk, "UN004", raidersMyrrorLocation, raidersMyrrorLocation, null, raidersPlayer, UnitStatusID.ALIVE, null, sd, db);
		
		// Check buildings were added.
		// Note players intentionally left as null so that building is only added on the server.
		verify (midTurn).addBuildingOnServerAndClients (gsk, null, humanLocation, "BL01", null, null, null, sd, db);
		verify (midTurn).addBuildingOnServerAndClients (gsk, null, aiLocation, "BL01", null, null, null, sd, db);
		verify (midTurn).addBuildingOnServerAndClients (gsk, null, raidersArcanusLocation, "BL02", null, null, null, sd, db);
		verify (midTurn).addBuildingOnServerAndClients (gsk, null, raidersMyrrorLocation, "BL02", null, null, null, sd, db);
		verify (midTurn).addBuildingOnServerAndClients (gsk, null, raidersMyrrorLocation, "BL03", null, null, null, sd, db);
	}
	
	/**
	 * Tests the growCitiesAndProgressConstructionProjects method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGrowCitiesAndProgressConstructionProjects () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final PlaneSvr arcanus = new PlaneSvr ();
		final PlaneSvr myrror = new PlaneSvr ();
		myrror.setPlaneNumber (1);
		
		final List<PlaneSvr> planes = new ArrayList<PlaneSvr> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlanes ()).thenReturn (planes);
		
		final BuildingSvr building = new BuildingSvr ();
		building.setBuildingID ("BL01");
		building.setProductionCost (1000);
		when (db.findBuilding ("BL01", "growCitiesAndProgressConstructionProjects")).thenReturn (building);
		when (db.findUnit ("BL01", "growCitiesAndProgressConstructionProjects")).thenThrow (new RecordNotFoundException (UnitSvr.class, null, null));
		
		final UnitSvr unit = new UnitSvr ();
		unit.setUnitID ("UN001");
		unit.setProductionCost (100);
		when (db.findBuilding ("UN001", "growCitiesAndProgressConstructionProjects")).thenThrow (new RecordNotFoundException (UnitSvr.class, null, null));
		when (db.findUnit ("UN001", "growCitiesAndProgressConstructionProjects")).thenReturn (unit);				
		
		// Session description
		final OverlandMapSize sys = ServerTestData.createOverlandMapSize ();
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setFogOfWarSetting (fogOfWarSettings);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final MomPersistentPlayerPublicKnowledge humanPpk = new MomPersistentPlayerPublicKnowledge ();
		humanPpk.setWizardID ("");				// Custom wizard

		final MomPersistentPlayerPrivateKnowledge humanPriv = new MomPersistentPlayerPrivateKnowledge ();
		humanPriv.setTaxRateID ("TR01");
		
		final MomTransientPlayerPrivateKnowledge humanTrans = new MomTransientPlayerPrivateKnowledge ();
				
		final PlayerDescription humanPd = new PlayerDescription ();
		humanPd.setHuman (true);
		humanPd.setPlayerID (5);
		final PlayerServerDetails humanPlayer = new PlayerServerDetails (humanPd, humanPpk, humanPriv, null, humanTrans);
		
		final MomPersistentPlayerPublicKnowledge aiPpk = new MomPersistentPlayerPublicKnowledge ();
		aiPpk.setWizardID ("WZ01");				// Standard wizard
		
		final MomPersistentPlayerPrivateKnowledge aiPriv = new MomPersistentPlayerPrivateKnowledge ();
		aiPriv.setTaxRateID ("TR02");
		
		final PlayerDescription aiPd = new PlayerDescription ();
		aiPd.setHuman (false);
		aiPd.setPlayerID (-1);
		final PlayerServerDetails aiPlayer = new PlayerServerDetails (aiPd, aiPpk, aiPriv, null, null);

		final MomPersistentPlayerPublicKnowledge raidersPpk = new MomPersistentPlayerPublicKnowledge ();
		raidersPpk.setWizardID (CommonDatabaseConstants.WIZARD_ID_RAIDERS);
		
		final MomPersistentPlayerPrivateKnowledge raidersPriv = new MomPersistentPlayerPrivateKnowledge ();
		raidersPriv.setTaxRateID ("TR03");
		
		final PlayerDescription raidersPd = new PlayerDescription ();
		raidersPd.setHuman (false);
		raidersPd.setPlayerID (-2);
		final PlayerServerDetails raidersPlayer = new PlayerServerDetails (raidersPd, raidersPpk, raidersPriv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (humanPlayer);
		players.add (aiPlayer);
		players.add (raidersPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, humanPd.getPlayerID (), "growCitiesAndProgressConstructionProjects")).thenReturn (humanPlayer);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, aiPd.getPlayerID (), "growCitiesAndProgressConstructionProjects")).thenReturn (aiPlayer);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, raidersPd.getPlayerID (), "growCitiesAndProgressConstructionProjects")).thenReturn (raidersPlayer);
		
		// Human player is constructing a building, and won't finish it this turn
		final MapCoordinates3DEx humanLocation = new MapCoordinates3DEx (25, 15, 1);

		final MemoryGridCell humanCell = trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		final OverlandMapCityData humanCity = new OverlandMapCityData ();
		humanCity.setCityOwnerID (humanPd.getPlayerID ());
		humanCity.setCityPopulation (4400);
		humanCity.setCurrentlyConstructingBuildingID ("BL01");
		humanCity.setProductionSoFar (500);
		humanCell.setCityData (humanCity);
		final int humanCityMaxSize = 12;
		
		// AI player is constructing a building, and will finish it now
		final MapCoordinates3DEx aiLocation = new MapCoordinates3DEx (40, 20, 0);

		final MemoryGridCell aiCell = trueTerrain.getPlane ().get (0).getRow ().get (20).getCell ().get (40);
		final OverlandMapCityData aiCity = new OverlandMapCityData ();
		aiCity.setCityOwnerID (aiPd.getPlayerID ());
		aiCity.setCityPopulation (5700);
		aiCity.setCurrentlyConstructingBuildingID ("BL01");
		aiCity.setProductionSoFar (500);
		aiCell.setCityData (aiCity);
		final int aiCityMaxSize = 11;
		
		// Raiders player is constructing a unit, and will finish it now 
		final MapCoordinates3DEx raidersLocation = new MapCoordinates3DEx (7, 27, 0);

		final ServerGridCellEx raidersCell = (ServerGridCellEx) trueTerrain.getPlane ().get (0).getRow ().get (27).getCell ().get (7);
		final OverlandMapCityData raidersCity = new OverlandMapCityData ();
		raidersCity.setCityOwnerID (raidersPd.getPlayerID ());
		raidersCity.setCityPopulation (8700);
		raidersCity.setCurrentlyConstructingUnitID ("UN001");
		raidersCity.setProductionSoFar (50);
		final int raidersCityMaxSize = 15;
		
		raidersCell.setRaiderCityAdditionalPopulationCap (9000);
		raidersCell.setCityData (raidersCity);
		
		// City production
		final CityCalculations cityCalc = mock (CityCalculations.class);
		
		final CityProductionBreakdown humanCityMaxSizeContainer = new CityProductionBreakdown ();
		humanCityMaxSizeContainer.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		humanCityMaxSizeContainer.setCappedProductionAmount (humanCityMaxSize);
		final CityProductionBreakdown humanProduction = new CityProductionBreakdown ();
		humanProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		humanProduction.setCappedProductionAmount (150);
		final CityProductionBreakdownsEx humanCityProductions = new CityProductionBreakdownsEx ();
		humanCityProductions.getProductionType ().add (humanCityMaxSizeContainer);
		humanCityProductions.getProductionType ().add (humanProduction);
		when (cityCalc.calculateAllCityProductions (players, trueTerrain, trueMap.getBuilding (), humanLocation, "TR01", sd, true, false, db)).thenReturn (humanCityProductions);

		final CityProductionBreakdown aiCityMaxSizeContainer = new CityProductionBreakdown ();
		aiCityMaxSizeContainer.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		aiCityMaxSizeContainer.setCappedProductionAmount (aiCityMaxSize);
		final CityProductionBreakdown aiProduction = new CityProductionBreakdown ();
		aiProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		aiProduction.setCappedProductionAmount (650);
		final CityProductionBreakdownsEx aiCityProductions = new CityProductionBreakdownsEx ();
		aiCityProductions.getProductionType ().add (aiCityMaxSizeContainer);
		aiCityProductions.getProductionType ().add (aiProduction);
		when (cityCalc.calculateAllCityProductions (players, trueTerrain, trueMap.getBuilding (), aiLocation, "TR02", sd, true, false, db)).thenReturn (aiCityProductions);
		
		final CityProductionBreakdown raidersCityMaxSizeContainer = new CityProductionBreakdown ();
		raidersCityMaxSizeContainer.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		raidersCityMaxSizeContainer.setCappedProductionAmount (raidersCityMaxSize);
		final CityProductionBreakdown raidersProduction = new CityProductionBreakdown ();
		raidersProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		raidersProduction.setCappedProductionAmount (60);
		final CityProductionBreakdownsEx raidersCityProductions = new CityProductionBreakdownsEx ();
		raidersCityProductions.getProductionType ().add (raidersCityMaxSizeContainer);
		raidersCityProductions.getProductionType ().add (raidersProduction);
		when (cityCalc.calculateAllCityProductions (players, trueTerrain, trueMap.getBuilding (), raidersLocation, "TR03", sd, true, false, db)).thenReturn (raidersCityProductions);
		
		// City growth rate
		final CityGrowthRateBreakdown humanGrowthRate = new CityGrowthRateBreakdown ();
		humanGrowthRate.setFinalTotal (650);
		when (cityCalc.calculateCityGrowthRate (trueTerrain, trueMap.getBuilding (), humanLocation, humanCityMaxSize, db)).thenReturn (humanGrowthRate);

		final CityGrowthRateBreakdown aiGrowthRate = new CityGrowthRateBreakdown ();
		aiGrowthRate.setFinalTotal (250);
		when (cityCalc.calculateCityGrowthRate (trueTerrain, trueMap.getBuilding (), aiLocation, aiCityMaxSize, db)).thenReturn (aiGrowthRate);
		
		final CityGrowthRateBreakdown raidersGrowthRate = new CityGrowthRateBreakdown ();
		raidersGrowthRate.setFinalTotal (400);
		when (cityCalc.calculateCityGrowthRate (trueTerrain, trueMap.getBuilding (), raidersLocation, raidersCityMaxSize, db)).thenReturn (raidersGrowthRate);

		// Rebels in each city
		final CityUnrestBreakdown humanRebels = new CityUnrestBreakdown ();
		humanRebels.setFinalTotal (1);
		final CityUnrestBreakdown aiRebels = new CityUnrestBreakdown ();
		aiRebels.setFinalTotal (2);
		final CityUnrestBreakdown raidersRebels = new CityUnrestBreakdown ();
		raidersRebels.setFinalTotal (3);
		
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), humanLocation, "TR01", db)).thenReturn (humanRebels);
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), aiLocation, "TR02", db)).thenReturn (aiRebels);
		when (cityCalc.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), raidersLocation, "TR03", db)).thenReturn (raidersRebels);
		
		// Where the unit built by the raider city will appear
		final UnitAddLocation unitAddLocation = new UnitAddLocation (raidersLocation, UnitAddBumpTypeID.CITY);
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.findNearestLocationWhereUnitCanBeAdded (raidersLocation, "UN001", raidersPd.getPlayerID (), trueMap, sd, db)).thenReturn (unitAddLocation);
		
		// Set up object to test
		final ServerCityCalculations serverCityCalc = mock (ServerCityCalculations.class);
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setCityCalculations (cityCalc);
		proc.setServerCityCalculations (serverCityCalc);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setUnitServerUtils (unitServerUtils);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		proc.growCitiesAndProgressConstructionProjects (0, players, gsk, sd, db);
		
		// Check human city
		assertEquals (4400+650, humanCity.getCityPopulation ());
		assertEquals (1, humanCity.getNumberOfRebels ());
		
		assertEquals (1, humanTrans.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.POPULATION_CHANGE, humanTrans.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessagePopulationChange ntm = (NewTurnMessagePopulationChange) humanTrans.getNewTurnMessage ().get (0);
		assertEquals (humanLocation, ntm.getCityLocation ());
		assertEquals (4400, ntm.getOldPopulation ());
		assertEquals (4400+650, ntm.getNewPopulation ());
		
		assertEquals ("BL01", humanCity.getCurrentlyConstructingBuildingID ());
		assertEquals (650, humanCity.getProductionSoFar ().intValue ());
		
		// Check AI city
		assertEquals (5700+250, aiCity.getCityPopulation ());
		assertEquals (2, aiCity.getNumberOfRebels ());

		assertEquals (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT, aiCity.getCurrentlyConstructingBuildingID ());
		assertEquals (0, aiCity.getProductionSoFar ().intValue ());
		
		verify (midTurn, times (1)).addBuildingOnServerAndClients (gsk, players, aiLocation, "BL01", null, null, null, sd, db);
		
		// Check raiders city
		assertEquals (9000, raidersCity.getCityPopulation ());		// Not 9100, because its over the special Raiders cap
		assertEquals (3, raidersCity.getNumberOfRebels ());

		assertEquals ("UN001", raidersCity.getCurrentlyConstructingUnitID ());
		assertEquals (0, raidersCity.getProductionSoFar ().intValue ());
		
		verify (midTurn, times (1)).addUnitOnServerAndClients (gsk, "UN001", raidersLocation, raidersLocation, null, raidersPlayer, UnitStatusID.ALIVE, players, sd, db);
		
		// Check calcs were done
		verify (serverCityCalc, times (3)).calculateCitySizeIDAndMinimumFarmers (eq (players), eq (trueTerrain), eq (trueMap.getBuilding ()),
			any (MapCoordinates3DEx.class), eq (sd), eq (db));
		
		verify (serverCityCalc, times (3)).ensureNotTooManyOptionalFarmers (any (OverlandMapCityData.class));
		
		// Gets called once per city
		verify (midTurn, times (3)).updatePlayerMemoryOfCity (eq (trueTerrain), eq (players), any (MapCoordinates3DEx.class), eq (fogOfWarSettings));
	}
	
	/**
	 * Tests the sellBuilding method in the typical situation of selling a building voluntarily in a one-at-a-time turns game
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final BuildingSvr granaryDef = new BuildingSvr ();
		when (db.findBuilding (GRANARY, "sellBuilding")).thenReturn (granaryDef);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Overland map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
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
		when (cityCalculations.calculateCityRebels (players, trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (), cityLocation, "TR", db)).thenReturn (unrest);
		
		cityData.setCurrentlyConstructingBuildingID (BARRACKS);
		proc.sellBuilding (trueMap, players, cityLocation, granary.getBuildingURN (), false, true, sd, db);
		
		// Check results
		assertEquals (BARRACKS, cityData.getCurrentlyConstructingBuildingID ());	// i.e. it didn't change
		assertEquals (GRANARY, tc.getBuildingIdSoldThisTurn ());
		assertEquals (3, cityData.getNumberOfRebels ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (trueMap, players, granary.getBuildingURN (), true, sd, db);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 12);
		verify (serverCityCalculations).calculateCitySizeIDAndMinimumFarmers (players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, sd, db);
		verify (serverCityCalculations).ensureNotTooManyOptionalFarmers (cityData);
		verify (midTurn).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation, sd.getFogOfWarSetting ());
	}

	/**
	 * Tests the sellBuilding method when we were forced to sell it (couldn't afford gold maintainence)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_Forced () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final BuildingSvr granaryDef = new BuildingSvr ();
		when (db.findBuilding (GRANARY, "sellBuilding")).thenReturn (granaryDef);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Overland map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
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
		when (cityCalculations.calculateCityRebels (players, trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (), cityLocation, "TR", db)).thenReturn (unrest);
		
		cityData.setCurrentlyConstructingBuildingID (BARRACKS);
		proc.sellBuilding (trueMap, players, cityLocation, granary.getBuildingURN (), false, false, sd, db);
		
		// Check results
		assertEquals (BARRACKS, cityData.getCurrentlyConstructingBuildingID ());	// i.e. it didn't change
		assertNull (tc.getBuildingIdSoldThisTurn ());		// Isn't updated because it was a forced sale
		assertEquals (3, cityData.getNumberOfRebels ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (trueMap, players, granary.getBuildingURN (), false, sd, db);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 12);
		verify (serverCityCalculations).calculateCitySizeIDAndMinimumFarmers (players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, sd, db);
		verify (serverCityCalculations).ensureNotTooManyOptionalFarmers (cityData);
		verify (midTurn).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation, sd.getFogOfWarSetting ());
	}

	/**
	 * Tests the sellBuilding method when we're currently trying to construct a building that requires the one being sold
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_ConstructionBasedOn () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final BuildingSvr granaryDef = new BuildingSvr ();
		when (db.findBuilding (GRANARY, "sellBuilding")).thenReturn (granaryDef);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Overland map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
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
		when (cityCalculations.calculateCityRebels (players, trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (), cityLocation, "TR", db)).thenReturn (unrest);
		
		cityData.setCurrentlyConstructingBuildingID (FARMERS_MARKET);
		proc.sellBuilding (trueMap, players, cityLocation, granary.getBuildingURN (), false, true, sd, db);
		
		// Check results
		assertEquals (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT, cityData.getCurrentlyConstructingBuildingID ());	// Can't build Farmers' Market anymore
		assertEquals (GRANARY, tc.getBuildingIdSoldThisTurn ());
		assertEquals (3, cityData.getNumberOfRebels ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (trueMap, players, granary.getBuildingURN (), true, sd, db);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 12);
		verify (serverCityCalculations).calculateCitySizeIDAndMinimumFarmers (players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, sd, db);
		verify (serverCityCalculations).ensureNotTooManyOptionalFarmers (cityData);
		verify (midTurn).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation, sd.getFogOfWarSetting ());
	}

	/**
	 * Tests the sellBuilding method when it is handled as a pending sale, i.e. for simultaneous turns games
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_Pending () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final BuildingSvr granaryDef = new BuildingSvr ();
		when (db.findBuilding (GRANARY, "sellBuilding")).thenReturn (granaryDef);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Overland map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
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
		
		// Set up object to test
		final CityProcessingImpl proc = new CityProcessingImpl ();
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// Run test
		proc.sellBuilding (trueMap, players, cityLocation, granary.getBuildingURN (), true, true, sd, db);
		
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
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final PlaneSvr arcanus = new PlaneSvr ();
		final PlaneSvr myrror = new PlaneSvr ();
		myrror.setPlaneNumber (1);
		
		final List<PlaneSvr> planes = new ArrayList<PlaneSvr> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlanes ()).thenReturn (planes);
		
		final TaxRate taxRate = new TaxRate ();
		when (db.findTaxRate ("TR03", "changeTaxRate")).thenReturn (taxRate);
		
		// Session description
		final OverlandMapSize sys = ServerTestData.createOverlandMapSize ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		players.add (player);
		
		// One of our cities
		final MapCoordinates3DEx cityLocation1 = new MapCoordinates3DEx (23, 15, 0);
		
		final OverlandMapCityData cityData1 = new OverlandMapCityData ();
		cityData1.setCityOwnerID (3);
		cityData1.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (15).getCell ().get (23).setCityData (cityData1);
		
		final CityUnrestBreakdown breakdown1 = new CityUnrestBreakdown ();
		breakdown1.setFinalTotal (4);
		
		// Another of our cities
		final MapCoordinates3DEx cityLocation2 = new MapCoordinates3DEx (24, 15, 0);
		
		final OverlandMapCityData cityData2 = new OverlandMapCityData ();
		cityData2.setCityOwnerID (3);
		cityData2.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (15).getCell ().get (24).setCityData (cityData2);
		
		final CityUnrestBreakdown breakdown2 = new CityUnrestBreakdown ();
		breakdown2.setFinalTotal (5);
		
		// Someone else's city
		final MapCoordinates3DEx cityLocation3 = new MapCoordinates3DEx (25, 15, 0);
		
		final OverlandMapCityData cityData3 = new OverlandMapCityData ();
		cityData3.setCityOwnerID (4);
		cityData3.setCityPopulation (1);
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
		when (cityCalculations.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), cityLocation1, "TR03", db)).thenReturn (breakdown1);
		when (cityCalculations.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), cityLocation2, "TR03", db)).thenReturn (breakdown2);
		when (cityCalculations.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), cityLocation3, "TR03", db)).thenReturn (breakdown3);
		
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
		verify (serverCityCalculations, times (0)).ensureNotTooManyOptionalFarmers (cityData3);
		verify (midTurn, times (0)).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation3, sd.getFogOfWarSetting ());
	}
}