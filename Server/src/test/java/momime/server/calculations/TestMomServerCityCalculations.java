package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.ServerDatabase;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the MomServerCityCalculations class
 */
public final class TestMomServerCityCalculations
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the calculateTotalFoodBonusFromBuildings method with the default XML database
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If the food production values from the XML database aren't multiples of 2
	 */
	@Test
	public final void testCalculateTotalFoodBonusFromBuildings_Valid () throws IOException, JAXBException, MomException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		assertEquals (5, MomServerCityCalculations.calculateTotalFoodBonusFromBuildings (db, debugLogger));
	}

	/**
	 * Tests the calculateTotalFoodBonusFromBuildings method with an edited XML database in order to cause an error
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If the food production values from the XML database aren't multiples of 2
	 * @throws RecordNotFoundException If we can't find the value we want to update
	 */
	@Test(expected=MomException.class)
	public final void testCalculateTotalFoodBonusFromBuildings_Invalid () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Change production to be an odd value, which is invalid
		for (final BuildingPopulationProductionModifier mod : db.findBuilding ("BL30", "testCalculateTotalFoodBonusFromBuildings_Invalid").getBuildingPopulationProductionModifier ())
			if (mod.getProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD))
				mod.setDoubleAmount (mod.getDoubleAmount () + 1);

		assertEquals (5, MomServerCityCalculations.calculateTotalFoodBonusFromBuildings (db, debugLogger));
	}

	/**
	 * Tests the calculateDoubleFarmingRate method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 * @throws MomException If the city's race has no farmers defined or those farmers have no ration production defined
	 */
	@Test
	public final void testCalculateDoubleFarmingRate () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Location
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// Halflings farmers produce 1 extra food
		cityData.setCityRaceID ("RC03");
		assertEquals (6, MomServerCityCalculations.calculateDoubleFarmingRate (map, buildings, cityLocation, db, debugLogger));

		// Normal race (high men) with no bonuses
		cityData.setCityRaceID ("RC05");
		assertEquals (4, MomServerCityCalculations.calculateDoubleFarmingRate (map, buildings, cityLocation, db, debugLogger));

		// Add an irrelevant building
		final OverlandMapCoordinates firstBuildingLocation = new OverlandMapCoordinates ();
		firstBuildingLocation.setX (2);
		firstBuildingLocation.setY (2);
		firstBuildingLocation.setPlane (0);

		final MemoryBuilding firstBuilding = new MemoryBuilding ();
		firstBuilding.setBuildingID ("BL15");		// Sawmill
		firstBuilding.setCityLocation (firstBuildingLocation);

		buildings.add (firstBuilding);
		assertEquals (4, MomServerCityCalculations.calculateDoubleFarmingRate (map, buildings, cityLocation, db, debugLogger));

		// Add an animists' guild in the wrong location
		final OverlandMapCoordinates secondBuildingLocation = new OverlandMapCoordinates ();
		secondBuildingLocation.setX (2);
		secondBuildingLocation.setY (2);
		secondBuildingLocation.setPlane (1);

		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL10");
		secondBuilding.setCityLocation (secondBuildingLocation);

		buildings.add (secondBuilding);
		assertEquals (4, MomServerCityCalculations.calculateDoubleFarmingRate (map, buildings, cityLocation, db, debugLogger));

		// Add an animists' guild in the right location
		final OverlandMapCoordinates thirdBuildingLocation = new OverlandMapCoordinates ();
		thirdBuildingLocation.setX (2);
		thirdBuildingLocation.setY (2);
		thirdBuildingLocation.setPlane (0);

		final MemoryBuilding thirdBuilding = new MemoryBuilding ();
		thirdBuilding.setBuildingID ("BL10");
		thirdBuilding.setCityLocation (thirdBuildingLocation);

		buildings.add (thirdBuilding);
		assertEquals (6, MomServerCityCalculations.calculateDoubleFarmingRate (map, buildings, cityLocation, db, debugLogger));
	}

	/**
	 * Tests the calculateCitySizeIDAndMinimumFarmers method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we can't find the player who owns the city
	 * @throws MomException If any of a number of expected items aren't found in the database
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 */
	@Test
	public final void testCalculateCitySizeIDAndMinimumFarmers () throws IOException, JAXBException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (serverDB, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sd.getMapSize ());

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (new PlayerServerDetails (pd, pub, priv, null, null));

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityRaceID ("RC05");		// High men
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Location
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// Starter size city - with no wild game and no granary, we need 2 farmers to feed the 4 population
		cityData.setCityPopulation (4900);
		MomServerCityCalculations.calculateCitySizeIDAndMinimumFarmers (players, map, buildings, cityLocation, sd, db, debugLogger);
		assertEquals ("CS02", cityData.getCitySizeID ());
		assertEquals (2, cityData.getMinimumFarmers ().intValue ());

		// If we add a granary, that feeds 2 of the population so we need 1 less farmer
		final OverlandMapCoordinates granaryLocation = new OverlandMapCoordinates ();
		granaryLocation.setX (2);
		granaryLocation.setY (2);
		granaryLocation.setPlane (0);

		final MemoryBuilding granary = new MemoryBuilding ();
		granary.setBuildingID ("BL29");
		granary.setCityLocation (granaryLocation);

		buildings.add (granary);

		MomServerCityCalculations.calculateCitySizeIDAndMinimumFarmers (players, map, buildings, cityLocation, sd, db, debugLogger);
		assertEquals ("CS02", cityData.getCitySizeID ());
		assertEquals (1, cityData.getMinimumFarmers ().intValue ());

		// Make the city bigger - now need 3 farmers to feed the 7 population (1 is fed by the granary)
		cityData.setCityPopulation (7500);
		MomServerCityCalculations.calculateCitySizeIDAndMinimumFarmers (players, map, buildings, cityLocation, sd, db, debugLogger);
		assertEquals ("CS03", cityData.getCitySizeID ());
		assertEquals (3, cityData.getMinimumFarmers ().intValue ());

		// Halfling farmers produce more rations - so now we only need 5/3 = 2 farmers
		cityData.setCityRaceID ("RC03");
		MomServerCityCalculations.calculateCitySizeIDAndMinimumFarmers (players, map, buildings, cityLocation, sd, db, debugLogger);
		assertEquals ("CS03", cityData.getCitySizeID ());
		assertEquals (2, cityData.getMinimumFarmers ().intValue ());
	}

	/**
	 * Tests the ensureNotTooManyOptionalFarmers method in valid scenarios
	 * @throws MomException If minimum farmers + rebels > population
	 */
	@Test
	public final void testEnsureNotTooManyOptionalFarmers_Valid () throws MomException
	{
		final OverlandMapCityData city = new OverlandMapCityData ();

		city.setCityPopulation (8678);

		city.setMinimumFarmers (3);
		city.setNumberOfRebels (3);

		// Lower optional farmers
		city.setOptionalFarmers (1);
		MomServerCityCalculations.ensureNotTooManyOptionalFarmers (city, debugLogger);
		assertEquals (1, city.getOptionalFarmers ().intValue ());

		// Exact number of optional farmers
		city.setOptionalFarmers (2);
		MomServerCityCalculations.ensureNotTooManyOptionalFarmers (city, debugLogger);
		assertEquals (2, city.getOptionalFarmers ().intValue ());

		// Too many optional farmers
		city.setOptionalFarmers (3);
		MomServerCityCalculations.ensureNotTooManyOptionalFarmers (city, debugLogger);
		assertEquals (2, city.getOptionalFarmers ().intValue ());

		// Way too many optional farmers
		city.setNumberOfRebels (5);
		MomServerCityCalculations.ensureNotTooManyOptionalFarmers (city, debugLogger);
		assertEquals (0, city.getOptionalFarmers ().intValue ());
	}

	/**
	 * Tests the ensureNotTooManyOptionalFarmers method when the minimum farmers and rebels are set incorrectly
	 * @throws MomException If minimum farmers + rebels > population
	 */
	@Test(expected=MomException.class)
	public final void testEnsureNotTooManyOptionalFarmers_Invalid () throws MomException
	{
		final OverlandMapCityData city = new OverlandMapCityData ();

		city.setCityPopulation (8678);
		city.setMinimumFarmers (3);
		city.setNumberOfRebels (6);
		city.setOptionalFarmers (0);

		MomServerCityCalculations.ensureNotTooManyOptionalFarmers (city, debugLogger);
	}

	/**
	 * Tests the calculateCityScoutingRange cmethod
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If there is a building in the list that cannot be found in the DB
	 */
	@Test
	public final void testCalculateCityScoutingRange () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Location
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// No buildings
		assertEquals (-1, MomServerCityCalculations.calculateCityScoutingRange (buildings, cityLocation, db, debugLogger));

		// City walls in wrong location
		final OverlandMapCoordinates firstBuildingLocation = new OverlandMapCoordinates ();
		firstBuildingLocation.setX (2);
		firstBuildingLocation.setY (3);
		firstBuildingLocation.setPlane (0);

		final MemoryBuilding firstBuilding = new MemoryBuilding ();
		firstBuilding.setBuildingID ("BL35");
		firstBuilding.setCityLocation (firstBuildingLocation);

		buildings.add (firstBuilding);
		assertEquals (-1, MomServerCityCalculations.calculateCityScoutingRange (buildings, cityLocation, db, debugLogger));

		// Irrelevant building in right location
		final OverlandMapCoordinates secondBuildingLocation = new OverlandMapCoordinates ();
		secondBuildingLocation.setX (2);
		secondBuildingLocation.setY (2);
		secondBuildingLocation.setPlane (0);

		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL34");
		secondBuilding.setCityLocation (secondBuildingLocation);

		buildings.add (secondBuilding);
		assertEquals (-1, MomServerCityCalculations.calculateCityScoutingRange (buildings, cityLocation, db, debugLogger));

		// City walls increase to 3
		final OverlandMapCoordinates cityWallsLocation = new OverlandMapCoordinates ();
		cityWallsLocation.setX (2);
		cityWallsLocation.setY (2);
		cityWallsLocation.setPlane (0);

		final MemoryBuilding cityWalls = new MemoryBuilding ();
		cityWalls.setBuildingID ("BL35");
		cityWalls.setCityLocation (cityWallsLocation);

		buildings.add (cityWalls);
		assertEquals (3, MomServerCityCalculations.calculateCityScoutingRange (buildings, cityLocation, db, debugLogger));

		// Oracle increases to 4
		final OverlandMapCoordinates oracleLocation = new OverlandMapCoordinates ();
		oracleLocation.setX (2);
		oracleLocation.setY (2);
		oracleLocation.setPlane (0);

		final MemoryBuilding oracle = new MemoryBuilding ();
		oracle.setBuildingID ("BL18");
		oracle.setCityLocation (oracleLocation);

		buildings.add (oracle);
		assertEquals (4, MomServerCityCalculations.calculateCityScoutingRange (buildings, cityLocation, db, debugLogger));
	}

	/**
	 * Tests the canEventuallyConstructBuilding method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testCanEventuallyConstructBuilding () throws IOException, JAXBException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (serverDB, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Need certain types of terrain in order to be able to construct all building types
		final OverlandMapTerrainData forest = new OverlandMapTerrainData ();
		forest.setTileTypeID (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (19).setTerrainData (forest);

		final OverlandMapTerrainData mountain = new OverlandMapTerrainData ();
		mountain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (20).setTerrainData (mountain);

		final OverlandMapTerrainData ocean = new OverlandMapTerrainData ();
		ocean.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (21).setTerrainData (ocean);

		// Set up city
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (20);
		cityLocation.setY (10);
		cityLocation.setPlane (1);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Orcs can build absolutely everything
		cityData.setCityRaceID ("RC09");
		for (final Building building : db.getBuildings ())
			if ((!building.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS)) &&
				(!building.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE)))

				assertTrue (building.getBuildingID (), MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation, building, sd.getMapSize (), db, debugLogger));

		// Barbarians can't build Universities
		cityData.setCityRaceID ("RC01");
		assertFalse (MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL20", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db, debugLogger));

		// Barbarians can't build Banks, because they can't build Universities
		assertFalse (MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL27", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db, debugLogger));

		// Barbarians can't build Merchants' Guilds, because they can't build Banks, because they can't build Universities
		assertFalse (MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db, debugLogger));

		// Orcs can't build Ship Wrights' Guilds if there's no water
		cityData.setCityRaceID ("RC09");
		ocean.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		assertFalse (MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL12", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db, debugLogger));

		// Orcs can't build Ship Yards if there's no water, because they can't build a Ship Wrights' Guild
		assertFalse (MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL13", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db, debugLogger));

		// Orcs can't build Merchants' Guilds if there's no water, because they can't build a Ship Yard, because they can't build a Ship Wrights' Guild
		assertFalse (MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db, debugLogger));

		// If we got a Ship Wrights' Guild and subsequently the water all dried up then we *can* then construct the other building types
		// (Ok bad example, but similar with Sawmills + forests disappearing is definitely possible)
		final OverlandMapCoordinates shipWrightsGuildLocation = new OverlandMapCoordinates ();
		shipWrightsGuildLocation.setX (20);
		shipWrightsGuildLocation.setY (10);
		shipWrightsGuildLocation.setPlane (1);

		final MemoryBuilding shipWrightsGuild = new MemoryBuilding ();
		shipWrightsGuild.setCityLocation (shipWrightsGuildLocation);
		shipWrightsGuild.setBuildingID ("BL12");

		buildings.add (shipWrightsGuild);

		assertTrue (MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL13", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db, debugLogger));
		assertTrue (MomServerCityCalculations.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db, debugLogger));
	}
}
