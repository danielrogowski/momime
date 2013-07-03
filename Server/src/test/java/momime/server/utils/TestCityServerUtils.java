package momime.server.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CityServerUtils class
 */
public final class TestCityServerUtils
{
	/**
	 * Tests the validateCityConstruction method for constructing buildings
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);
		
		// Set up object
		final CityServerUtils utils = new CityServerUtils ();
		utils.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		utils.setCityCalculations (new MomCityCalculations ());

		// Blacksmith - can't yet because we didn't set the city player yet so ANY change is invalid, even one with no prerequisities
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db));

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db));

		cityData.setCityOwnerID (1);
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db));

		cityData.setCityOwnerID (2);
		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db));

		// Can't build a stables without a blacksmith
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL09", sd, db));

		final OverlandMapCoordinatesEx blacksmithLocation = new OverlandMapCoordinatesEx ();
		blacksmithLocation.setX (2);
		blacksmithLocation.setY (2);
		blacksmithLocation.setPlane (0);

		final MemoryBuilding blacksmith = new MemoryBuilding ();
		blacksmith.setBuildingID ("BL08");
		blacksmith.setCityLocation (blacksmithLocation);
		trueMap.getBuilding ().add (blacksmith);

		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL09", sd, db));

		// If we already have a blacksmith, then can't add another one
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db));

		// Can't build a ship wrights' guild without any water within distance 1
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db));

		final OverlandMapTerrainData distance1terrain = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (3).getCell ().get (2).setTerrainData (distance1terrain);
		distance1terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db));

		final OverlandMapTerrainData distance2terrain = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (4).getCell ().get (2).setTerrainData (distance2terrain);
		distance2terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db));

		distance1terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);
		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db));

		// Lizardmen can't build shipwrights' guilds
		cityData.setCityRaceID ("RC07");
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Set up object
		final CityServerUtils utils = new CityServerUtils ();
		utils.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		
		// Barbarian spearmen - city must be correct race
		cityData.setCityRaceID ("RC09");
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN040", sd, db));

		cityData.setCityRaceID ("RC01");
		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN040", sd, db));

		// Can't build any non-normal units (magic spirit)
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN155", sd, db));

		// Building swordsmen requires both a Barracks and Blacksmith
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN041", sd, db));

		final OverlandMapCoordinatesEx barracksLocation = new OverlandMapCoordinatesEx ();
		barracksLocation.setX (2);
		barracksLocation.setY (2);
		barracksLocation.setPlane (0);

		final MemoryBuilding barracks = new MemoryBuilding ();
		barracks.setBuildingID ("BL03");
		barracks.setCityLocation (barracksLocation);
		trueMap.getBuilding ().add (barracks);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN041", sd, db));

		final OverlandMapCoordinatesEx blacksmithLocation = new OverlandMapCoordinatesEx ();
		blacksmithLocation.setX (2);
		blacksmithLocation.setY (2);
		blacksmithLocation.setPlane (0);

		final MemoryBuilding blacksmith = new MemoryBuilding ();
		blacksmith.setBuildingID ("BL08");
		blacksmith.setCityLocation (blacksmithLocation);
		trueMap.getBuilding ().add (blacksmith);

		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN041", sd, db));
	}

	/**
	 * Tests the validateCityConstruction method when the ID requested is neither a valid building or unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_InvalidConstructionProject () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Set up object
		final CityServerUtils utils = new CityServerUtils ();
		
		// Run test
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "X", sd, db));
	}

	/**
	 * Tests the validateCityConstruction method when we have an invalid race
	 * @throws Exception If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testValidateCityConstruction_InvalidRace () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		cityData.setCityRaceID ("X");
		cityData.setCityOwnerID (2);

		// Set up object
		final CityServerUtils utils = new CityServerUtils ();
		utils.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		
		// Run test
		utils.validateCityConstruction (player, trueMap, cityLocation, CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS, sd, db);
	}

	/**
	 * Tests the validateOptionalFarmers method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateOptionalFarmers () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// Set up object
		final CityServerUtils utils = new CityServerUtils ();
		
		//  Can't set yet, even to a zero which is always valid, because we didn't set the city player yet so ANY change is invalid
		assertNotNull (utils.validateOptionalFarmers (player, trueMap, cityLocation, 0, sd, db));

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setMinimumFarmers (1);
		cityData.setNumberOfRebels (2);
		cityData.setCityPopulation (5678);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNotNull (utils.validateOptionalFarmers (player, trueMap, cityLocation, 0, sd, db));

		cityData.setCityOwnerID (1);
		assertNotNull (utils.validateOptionalFarmers (player, trueMap, cityLocation, 0, sd, db));

		cityData.setCityOwnerID (2);
		assertNull (utils.validateOptionalFarmers (player, trueMap, cityLocation, 0, sd, db));

		// Try invalid values
		assertNotNull (utils.validateOptionalFarmers (player, trueMap, cityLocation, -1, sd, db));
		assertNotNull (utils.validateOptionalFarmers (player, trueMap, cityLocation, 3, sd, db));

		// Try valid value
		assertNull (utils.validateOptionalFarmers (player, trueMap, cityLocation, 2, sd, db));
	}
}
