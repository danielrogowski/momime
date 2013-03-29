package momime.server.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
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
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the validateCityConstruction method for constructing buildings
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testValidateCityConstruction_Building () throws IOException, JAXBException
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
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// Blacksmith - can't yet because we didn't set the city player yet so ANY change is invalid, even one with no prerequisities
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db, debugLogger));

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db, debugLogger));

		cityData.setCityOwnerID (1);
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db, debugLogger));

		cityData.setCityOwnerID (2);
		assertNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db, debugLogger));

		// Can't build a stables without a blacksmith
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL09", sd, db, debugLogger));

		final OverlandMapCoordinates blacksmithLocation = new OverlandMapCoordinates ();
		blacksmithLocation.setX (2);
		blacksmithLocation.setY (2);
		blacksmithLocation.setPlane (0);

		final MemoryBuilding blacksmith = new MemoryBuilding ();
		blacksmith.setBuildingID ("BL08");
		blacksmith.setCityLocation (blacksmithLocation);
		trueMap.getBuilding ().add (blacksmith);

		assertNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL09", sd, db, debugLogger));

		// If we already have a blacksmith, then can't add another one
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sd, db, debugLogger));

		// Can't build a ship wrights' guild without any water within distance 1
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db, debugLogger));

		final OverlandMapTerrainData distance1terrain = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (3).getCell ().get (2).setTerrainData (distance1terrain);
		distance1terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db, debugLogger));

		final OverlandMapTerrainData distance2terrain = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (4).getCell ().get (2).setTerrainData (distance2terrain);
		distance2terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db, debugLogger));

		distance1terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);
		assertNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db, debugLogger));

		// Lizardmen can't build shipwrights' guilds
		cityData.setCityRaceID ("RC07");
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "BL12", sd, db, debugLogger));
	}

	/**
	 * Tests the validateCityConstruction method for constructing units
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testValidateCityConstruction_Unit () throws IOException, JAXBException
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
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Barbarian spearmen - city must be correct race
		cityData.setCityRaceID ("RC09");
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "UN040", sd, db, debugLogger));

		cityData.setCityRaceID ("RC01");
		assertNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "UN040", sd, db, debugLogger));

		// Can't build any non-normal units (magic spirit)
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "UN155", sd, db, debugLogger));

		// Building swordsmen requires both a Barracks and Blacksmith
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "UN041", sd, db, debugLogger));

		final OverlandMapCoordinates barracksLocation = new OverlandMapCoordinates ();
		barracksLocation.setX (2);
		barracksLocation.setY (2);
		barracksLocation.setPlane (0);

		final MemoryBuilding barracks = new MemoryBuilding ();
		barracks.setBuildingID ("BL03");
		barracks.setCityLocation (barracksLocation);
		trueMap.getBuilding ().add (barracks);

		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "UN041", sd, db, debugLogger));

		final OverlandMapCoordinates blacksmithLocation = new OverlandMapCoordinates ();
		blacksmithLocation.setX (2);
		blacksmithLocation.setY (2);
		blacksmithLocation.setPlane (0);

		final MemoryBuilding blacksmith = new MemoryBuilding ();
		blacksmith.setBuildingID ("BL08");
		blacksmith.setCityLocation (blacksmithLocation);
		trueMap.getBuilding ().add (blacksmith);

		assertNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "UN041", sd, db, debugLogger));
	}

	/**
	 * Tests the validateCityConstruction method when the ID requested is neither a valid building or unit
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testValidateCityConstruction_InvalidConstructionProject () throws IOException, JAXBException
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
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Run test
		assertNotNull (CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, "X", sd, db, debugLogger));
	}

	/**
	 * Tests the validateCityConstruction method when we have an invalid race
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testValidateCityConstruction_InvalidRace () throws IOException, JAXBException
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
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		cityData.setCityRaceID ("X");
		cityData.setCityOwnerID (2);

		// Run test
		CityServerUtils.validateCityConstruction (player, trueMap, cityLocation, CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS, sd, db, debugLogger);
	}

	/**
	 * Tests the validateOptionalFarmers method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testValidateOptionalFarmers () throws IOException, JAXBException
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
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		//  Can't set yet, even to a zero which is always valid, because we didn't set the city player yet so ANY change is invalid
		assertNotNull (CityServerUtils.validateOptionalFarmers (player, trueMap, cityLocation, 0, sd, db, debugLogger));

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setMinimumFarmers (1);
		cityData.setNumberOfRebels (2);
		cityData.setCityPopulation (5678);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNotNull (CityServerUtils.validateOptionalFarmers (player, trueMap, cityLocation, 0, sd, db, debugLogger));

		cityData.setCityOwnerID (1);
		assertNotNull (CityServerUtils.validateOptionalFarmers (player, trueMap, cityLocation, 0, sd, db, debugLogger));

		cityData.setCityOwnerID (2);
		assertNull (CityServerUtils.validateOptionalFarmers (player, trueMap, cityLocation, 0, sd, db, debugLogger));

		// Try invalid values
		assertNotNull (CityServerUtils.validateOptionalFarmers (player, trueMap, cityLocation, -1, sd, db, debugLogger));
		assertNotNull (CityServerUtils.validateOptionalFarmers (player, trueMap, cityLocation, 3, sd, db, debugLogger));

		// Try valid value
		assertNull (CityServerUtils.validateOptionalFarmers (player, trueMap, cityLocation, 2, sd, db, debugLogger));
	}
}
