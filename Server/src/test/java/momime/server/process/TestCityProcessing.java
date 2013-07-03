package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.calculations.CalculateCityUnrestBreakdown;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.PendingSaleMessage;
import momime.common.messages.servertoclient.v0_9_4.TaxRateChangedMessage;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.IMomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.IMomServerCityCalculations;
import momime.server.calculations.IMomServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CityProcessing class
 */
public final class TestCityProcessing
{
	/** buildingID for a Granary */
	private final String GRANARY = "BL29";
	
	/** buildingID for a Farmers' Market (which requires a Granary) */
	private final String FARMERS_MARKET = "BL30";

	/** buildingID for a Barracks (which doesn't require a Granary) */
	private final String BARRACKS = "BL03";
	
	/**
	 * Tests the sellBuilding method in the typical situation of selling a building voluntarily in a one-at-a-time turns game
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
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
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setPlane (1);
		cityLocation.setX (20);
		cityLocation.setY (10);
		
		// City data
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);
		tc.setCityData (cityData);
		
		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final IMomServerCityCalculations serverCityCalculations = mock (IMomServerCityCalculations.class);
		final MomCityCalculations cityCalculations = mock (MomCityCalculations.class);
		
		final CityProcessing proc = new CityProcessing ();
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerCityCalculations (serverCityCalculations);
		proc.setCityCalculations (cityCalculations);
		
		// Run test
		final CalculateCityUnrestBreakdown unrest = new CalculateCityUnrestBreakdown (0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 3, null, null); 

		when (memoryBuildingUtils.isBuildingAPrerequisiteFor (GRANARY, BARRACKS, db)).thenReturn (false);
		when (memoryBuildingUtils.goldFromSellingBuilding (db.findBuilding (GRANARY, "testSellBuilding"))).thenReturn (12);
		when (cityCalculations.calculateCityRebels (players, trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (), cityLocation, "TR", db)).thenReturn (unrest);
		
		cityData.setCurrentlyConstructingBuildingOrUnitID (BARRACKS);
		proc.sellBuilding (trueMap, players, cityLocation, GRANARY, false, true, sd, db);
		
		// Check results
		assertEquals (BARRACKS, cityData.getCurrentlyConstructingBuildingOrUnitID ());	// i.e. it didn't change
		assertEquals (GRANARY, tc.getBuildingIdSoldThisTurn ());
		assertEquals (3, cityData.getNumberOfRebels ().intValue ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (trueMap, players, cityLocation, GRANARY, true, sd, db);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, 12);
		verify (serverCityCalculations).calculateCitySizeIDAndMinimumFarmers (players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, sd, db);
		verify (serverCityCalculations).ensureNotTooManyOptionalFarmers (cityData);
		verify (midTurn).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation, sd.getFogOfWarSetting (), false);
	}

	/**
	 * Tests the sellBuilding method when we were forced to sell it (couldn't afford gold maintainence)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_Forced () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
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
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setPlane (1);
		cityLocation.setX (20);
		cityLocation.setY (10);
		
		// City data
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);
		tc.setCityData (cityData);
		
		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final IMomServerCityCalculations serverCityCalculations = mock (IMomServerCityCalculations.class);
		final MomCityCalculations cityCalculations = mock (MomCityCalculations.class);
		
		final CityProcessing proc = new CityProcessing ();
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerCityCalculations (serverCityCalculations);
		proc.setCityCalculations (cityCalculations);
		
		// Run test
		final CalculateCityUnrestBreakdown unrest = new CalculateCityUnrestBreakdown (0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 3, null, null); 

		when (memoryBuildingUtils.isBuildingAPrerequisiteFor (GRANARY, BARRACKS, db)).thenReturn (false);
		when (memoryBuildingUtils.goldFromSellingBuilding (db.findBuilding (GRANARY, "testSellBuilding"))).thenReturn (12);
		when (cityCalculations.calculateCityRebels (players, trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (), cityLocation, "TR", db)).thenReturn (unrest);
		
		cityData.setCurrentlyConstructingBuildingOrUnitID (BARRACKS);
		proc.sellBuilding (trueMap, players, cityLocation, GRANARY, false, false, sd, db);
		
		// Check results
		assertEquals (BARRACKS, cityData.getCurrentlyConstructingBuildingOrUnitID ());	// i.e. it didn't change
		assertNull (tc.getBuildingIdSoldThisTurn ());		// Isn't updated because it was a forced sale
		assertEquals (3, cityData.getNumberOfRebels ().intValue ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (trueMap, players, cityLocation, GRANARY, false, sd, db);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, 12);
		verify (serverCityCalculations).calculateCitySizeIDAndMinimumFarmers (players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, sd, db);
		verify (serverCityCalculations).ensureNotTooManyOptionalFarmers (cityData);
		verify (midTurn).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation, sd.getFogOfWarSetting (), false);
	}

	/**
	 * Tests the sellBuilding method when we're currently trying to construct a building that requires the one being sold
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_ConstructionBasedOn () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
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
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setPlane (1);
		cityLocation.setX (20);
		cityLocation.setY (10);
		
		// City data
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);
		tc.setCityData (cityData);
		
		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final IMomServerCityCalculations serverCityCalculations = mock (IMomServerCityCalculations.class);
		final MomCityCalculations cityCalculations = mock (MomCityCalculations.class);
		
		final CityProcessing proc = new CityProcessing ();
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerCityCalculations (serverCityCalculations);
		proc.setCityCalculations (cityCalculations);
		
		// Run test
		final CalculateCityUnrestBreakdown unrest = new CalculateCityUnrestBreakdown (0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 3, null, null); 

		when (memoryBuildingUtils.isBuildingAPrerequisiteFor (GRANARY, FARMERS_MARKET, db)).thenReturn (true);
		when (memoryBuildingUtils.goldFromSellingBuilding (db.findBuilding (GRANARY, "testSellBuilding"))).thenReturn (12);
		when (cityCalculations.calculateCityRebels (players, trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (), cityLocation, "TR", db)).thenReturn (unrest);
		
		cityData.setCurrentlyConstructingBuildingOrUnitID (FARMERS_MARKET);
		proc.sellBuilding (trueMap, players, cityLocation, GRANARY, false, true, sd, db);
		
		// Check results
		assertEquals (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT, cityData.getCurrentlyConstructingBuildingOrUnitID ());	// Can't build Farmers' Market anymore
		assertEquals (GRANARY, tc.getBuildingIdSoldThisTurn ());
		assertEquals (3, cityData.getNumberOfRebels ().intValue ());
		
		verify (midTurn).destroyBuildingOnServerAndClients (trueMap, players, cityLocation, GRANARY, true, sd, db);
		verify (resourceValueUtils).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, 12);
		verify (serverCityCalculations).calculateCitySizeIDAndMinimumFarmers (players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, sd, db);
		verify (serverCityCalculations).ensureNotTooManyOptionalFarmers (cityData);
		verify (midTurn).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation, sd.getFogOfWarSetting (), false);
	}

	/**
	 * Tests the sellBuilding method when it is handled as a pending sale, i.e. for simultaneous turns games
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSellBuilding_Pending () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// City owner
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
		final PlayerServerDetails cityOwner = new PlayerServerDetails (pd, null, null, null, null);
		
		players.add (cityOwner);
		
		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		cityOwner.setConnection (msgs);
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setPlane (1);
		cityLocation.setX (20);
		cityLocation.setY (10);
		
		// City data
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);
		tc.setCityData (cityData);
		
		// Set up object to test
		final CityProcessing proc = new CityProcessing ();
		
		// Run test
		proc.sellBuilding (trueMap, players, cityLocation, GRANARY, true, true, sd, db);
		
		// Check results
		assertEquals (GRANARY, tc.getBuildingIdSoldThisTurn ());
		assertEquals (1, msgs.getMessages ().size ());
		final PendingSaleMessage msg = (PendingSaleMessage) msgs.getMessages ().get (0);
		assertEquals (cityLocation, msg.getCityLocation ());
		assertEquals (GRANARY, msg.getBuildingID ());
	}
	
	/**
	 * Tests the changeTaxRate method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChangeTaxRate () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
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
		final OverlandMapCoordinatesEx cityLocation1 = new OverlandMapCoordinatesEx ();
		cityLocation1.setX (23);
		cityLocation1.setY (15);
		cityLocation1.setPlane (0);
		
		final OverlandMapCityData cityData1 = new OverlandMapCityData ();
		cityData1.setCityOwnerID (3);
		cityData1.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (15).getCell ().get (23).setCityData (cityData1);
		
		final CalculateCityUnrestBreakdown breakdown1 = new CalculateCityUnrestBreakdown (0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 4, null, null);
		
		// Another of our cities
		final OverlandMapCoordinatesEx cityLocation2 = new OverlandMapCoordinatesEx ();
		cityLocation2.setX (24);
		cityLocation2.setY (15);
		cityLocation2.setPlane (0);
		
		final OverlandMapCityData cityData2 = new OverlandMapCityData ();
		cityData2.setCityOwnerID (3);
		cityData2.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (15).getCell ().get (24).setCityData (cityData2);
		
		final CalculateCityUnrestBreakdown breakdown2 = new CalculateCityUnrestBreakdown (0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 5, null, null);
		
		// Someone else's city
		final OverlandMapCoordinatesEx cityLocation3 = new OverlandMapCoordinatesEx ();
		cityLocation3.setX (25);
		cityLocation3.setY (15);
		cityLocation3.setPlane (0);
		
		final OverlandMapCityData cityData3 = new OverlandMapCityData ();
		cityData3.setCityOwnerID (4);
		cityData3.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (15).getCell ().get (25).setCityData (cityData3);
		
		final CalculateCityUnrestBreakdown breakdown3 = new CalculateCityUnrestBreakdown (0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 6, null, null);

		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		// Set up object to test
		final IMomSessionVariables mom = mock (IMomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		
		final IMomServerResourceCalculations serverResourceCalculations = mock (IMomServerResourceCalculations.class);

		// Have to use anyObject () for location since .equals () doesn't give correct result
		final MomCityCalculations cityCalculations = mock (MomCityCalculations.class);
		when (cityCalculations.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), cityLocation1, "TR03", db)).thenReturn (breakdown1);
		when (cityCalculations.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), cityLocation2, "TR03", db)).thenReturn (breakdown2);
		when (cityCalculations.calculateCityRebels (players, trueTerrain, trueMap.getUnit (), trueMap.getBuilding (), cityLocation3, "TR03", db)).thenReturn (breakdown3);
		
		final IMomServerCityCalculations serverCityCalculations = mock (IMomServerCityCalculations.class);
		final IFogOfWarMidTurnChanges midTurn = mock (IFogOfWarMidTurnChanges.class);
		
		final CityProcessing proc = new CityProcessing ();
		proc.setServerResourceCalculations (serverResourceCalculations);
		proc.setCityCalculations (cityCalculations);
		proc.setServerCityCalculations (serverCityCalculations);
		proc.setFogOfWarMidTurnChanges (midTurn);
		
		// Run test
		proc.changeTaxRate (player, "TR03", mom);

		// Check results
		assertEquals (1, msgs.getMessages ().size ());
		final TaxRateChangedMessage msg = (TaxRateChangedMessage) msgs.getMessages ().get (0);
		assertEquals ("TR03", msg.getTaxRateID ());
		
		assertEquals (4, cityData1.getNumberOfRebels ().intValue ());
		verify (serverCityCalculations, times (1)).ensureNotTooManyOptionalFarmers (cityData1);
		verify (midTurn, times (1)).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation1, sd.getFogOfWarSetting (), false);

		assertEquals (5, cityData2.getNumberOfRebels ().intValue ());
		verify (serverCityCalculations, times (1)).ensureNotTooManyOptionalFarmers (cityData2);
		verify (midTurn, times (1)).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation2, sd.getFogOfWarSetting (), false);

		assertNull (cityData3.getNumberOfRebels ());
		verify (serverCityCalculations, times (0)).ensureNotTooManyOptionalFarmers (cityData3);
		verify (midTurn, times (0)).updatePlayerMemoryOfCity (trueTerrain, players, cityLocation3, sd.getFogOfWarSetting (), false);
	}
}
