package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RacePopulationTask;
import momime.common.database.RacePopulationTaskProduction;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_5.Building;
import momime.server.database.v0_9_5.CitySize;
import momime.server.database.v0_9_5.Race;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the MomServerCityCalculations class
 */
public final class TestMomServerCityCalculationsImpl
{
	/**
	 * Tests the calculateDoubleFarmingRate method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleFarmingRate () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Set up object to test
		final MomServerCityCalculationsImpl calc = new MomServerCityCalculationsImpl ();
		calc.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		// Halflings farmers produce 1 extra food
		cityData.setCityRaceID ("RC03");
		assertEquals (6, calc.calculateDoubleFarmingRate (map, buildings, cityLocation, db));

		// Normal race (high men) with no bonuses
		cityData.setCityRaceID ("RC05");
		assertEquals (4, calc.calculateDoubleFarmingRate (map, buildings, cityLocation, db));

		// Add an irrelevant building
		final MemoryBuilding firstBuilding = new MemoryBuilding ();
		firstBuilding.setBuildingID ("BL15");		// Sawmill
		firstBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		buildings.add (firstBuilding);
		assertEquals (4, calc.calculateDoubleFarmingRate (map, buildings, cityLocation, db));

		// Add an animists' guild in the wrong location
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL10");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 1));

		buildings.add (secondBuilding);
		assertEquals (4, calc.calculateDoubleFarmingRate (map, buildings, cityLocation, db));

		// Add an animists' guild in the right location
		final MemoryBuilding thirdBuilding = new MemoryBuilding ();
		thirdBuilding.setBuildingID ("BL10");
		thirdBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		buildings.add (thirdBuilding);
		assertEquals (6, calc.calculateDoubleFarmingRate (map, buildings, cityLocation, db));
	}

	/**
	 * Tests the calculateCitySizeIDAndMinimumFarmers method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCitySizeIDAndMinimumFarmers () throws Exception
	{
		// Mock database
		final CitySize smallCity = new CitySize ();
		smallCity.setCitySizeID ("CS01");
		smallCity.setCitySizeMaximum (3999);
		
		final CitySize mediumCity = new CitySize ();
		mediumCity.setCitySizeID ("CS02");
		mediumCity.setCitySizeMinimum (4000);
		mediumCity.setCitySizeMaximum (6999);
		
		final CitySize largeCity = new CitySize ();
		largeCity.setCitySizeID ("CS03");
		largeCity.setCitySizeMinimum (7000);
		
		final List<CitySize> citySizes = new ArrayList<CitySize> ();
		citySizes.add (smallCity);
		citySizes.add (mediumCity);
		citySizes.add (largeCity);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getCitySize ()).thenReturn (citySizes);
		
		final RacePopulationTaskProduction highMenRations = new RacePopulationTaskProduction ();
		highMenRations.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS);
		highMenRations.setDoubleAmount (4);
		
		final RacePopulationTask highMenFarmers = new RacePopulationTask ();
		highMenFarmers.getRacePopulationTaskProduction ().add (highMenRations);
		highMenFarmers.setPopulationTaskID (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER);
		
		final Race highMen = new Race ();
		highMen.getRacePopulationTask ().add (highMenFarmers);
		when (db.findRace ("RC05", "calculateDoubleFarmingRate")).thenReturn (highMen);

		final RacePopulationTaskProduction halflingRations = new RacePopulationTaskProduction ();
		halflingRations.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS);
		halflingRations.setDoubleAmount (6);
		
		final RacePopulationTask halflingFarmers = new RacePopulationTask ();
		halflingFarmers.getRacePopulationTaskProduction ().add (halflingRations);
		halflingFarmers.setPopulationTaskID (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER);
		
		final Race halfling = new Race ();
		halfling.getRacePopulationTask ().add (halflingFarmers);
		when (db.findRace ("RC03", "calculateDoubleFarmingRate")).thenReturn (halfling);

		// Session description
		final MapSizeData mapSizeData = ServerTestData.createMapSizeData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSizeData);
		
		// Overland map
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (mapSizeData);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Building utils
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR04");

		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, pd.getPlayerID (), "calculateCitySizeIDAndMinimumFarmers")).thenReturn (player);

		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityRaceID ("RC05");		// High men
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		
		// Rations from city
		final CityCalculations cityCalc = mock (CityCalculations.class);

		// Set up object to test
		final MomServerCityCalculationsImpl calc = new MomServerCityCalculationsImpl ();
		calc.setCityCalculations (cityCalc);
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Starter size city - with no wild game and no granary, we need 2 farmers to feed the 4 population
		cityData.setCityPopulation (4900);
		calc.calculateCitySizeIDAndMinimumFarmers (players, map, buildings, new MapCoordinates3DEx (2, 2, 0), sd, db);
		assertEquals ("CS02", cityData.getCitySizeID ());
		assertEquals (2, cityData.getMinimumFarmers ().intValue ());

		// If we add a granary, that feeds 2 of the population so we need 1 less farmer
		when (cityCalc.calculateSingleCityProduction (players, map, buildings, new MapCoordinates3DEx (2, 2, 0),
			"TR04", sd, false, db, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS)).thenReturn (2);

		calc.calculateCitySizeIDAndMinimumFarmers (players, map, buildings, new MapCoordinates3DEx (2, 2, 0), sd, db);
		assertEquals ("CS02", cityData.getCitySizeID ());
		assertEquals (1, cityData.getMinimumFarmers ().intValue ());

		// Make the city bigger - now need 3 farmers to feed the 7 population (1 is fed by the granary)
		cityData.setCityPopulation (7500);
		calc.calculateCitySizeIDAndMinimumFarmers (players, map, buildings, new MapCoordinates3DEx (2, 2, 0), sd, db);
		assertEquals ("CS03", cityData.getCitySizeID ());
		assertEquals (3, cityData.getMinimumFarmers ().intValue ());

		// Halfling farmers produce more rations - so now we only need 5/3 = 2 farmers
		cityData.setCityRaceID ("RC03");
		calc.calculateCitySizeIDAndMinimumFarmers (players, map, buildings, new MapCoordinates3DEx (2, 2, 0), sd, db);
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

		// Set up object to test
		final MomServerCityCalculationsImpl calc = new MomServerCityCalculationsImpl ();
		
		// Lower optional farmers
		city.setOptionalFarmers (1);
		calc.ensureNotTooManyOptionalFarmers (city);
		assertEquals (1, city.getOptionalFarmers ().intValue ());

		// Exact number of optional farmers
		city.setOptionalFarmers (2);
		calc.ensureNotTooManyOptionalFarmers (city);
		assertEquals (2, city.getOptionalFarmers ().intValue ());

		// Too many optional farmers
		city.setOptionalFarmers (3);
		calc.ensureNotTooManyOptionalFarmers (city);
		assertEquals (2, city.getOptionalFarmers ().intValue ());

		// Way too many optional farmers
		city.setNumberOfRebels (5);
		calc.ensureNotTooManyOptionalFarmers (city);
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

		final MomServerCityCalculationsImpl calc = new MomServerCityCalculationsImpl ();
		calc.ensureNotTooManyOptionalFarmers (city);
	}

	/**
	 * Tests the calculateCityScoutingRange cmethod
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityScoutingRange () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Set up object to test
		final MomServerCityCalculationsImpl calc = new MomServerCityCalculationsImpl ();
		
		// No buildings
		assertEquals (-1, calc.calculateCityScoutingRange (buildings, cityLocation, db));

		// City walls in wrong location
		final MemoryBuilding firstBuilding = new MemoryBuilding ();
		firstBuilding.setBuildingID ("BL35");
		firstBuilding.setCityLocation (new MapCoordinates3DEx (2, 3, 0));

		buildings.add (firstBuilding);
		assertEquals (-1, calc.calculateCityScoutingRange (buildings, cityLocation, db));

		// Irrelevant building in right location
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL34");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		buildings.add (secondBuilding);
		assertEquals (-1, calc.calculateCityScoutingRange (buildings, cityLocation, db));

		// City walls increase to 3
		final MemoryBuilding cityWalls = new MemoryBuilding ();
		cityWalls.setBuildingID ("BL35");
		cityWalls.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		buildings.add (cityWalls);
		assertEquals (3, calc.calculateCityScoutingRange (buildings, cityLocation, db));

		// Oracle increases to 4
		final MemoryBuilding oracle = new MemoryBuilding ();
		oracle.setBuildingID ("BL18");
		oracle.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		buildings.add (oracle);
		assertEquals (4, calc.calculateCityScoutingRange (buildings, cityLocation, db));
	}

	/**
	 * Tests the canEventuallyConstructBuilding method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanEventuallyConstructBuilding () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
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
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Set up object to test
		final CityCalculationsImpl cityCalc = new CityCalculationsImpl ();
		cityCalc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		final MomServerCityCalculationsImpl calc = new MomServerCityCalculationsImpl ();
		calc.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		calc.setCityCalculations (cityCalc);
		
		// Orcs can build absolutely everything
		cityData.setCityRaceID ("RC09");
		for (final Building building : db.getBuilding ())
			if ((!building.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS)) &&
				(!building.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE)))

				assertTrue (building.getBuildingID (), calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation, building, sd.getMapSize (), db));

		// Barbarians can't build Universities
		cityData.setCityRaceID ("RC01");
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL20", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db));

		// Barbarians can't build Banks, because they can't build Universities
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL27", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db));

		// Barbarians can't build Merchants' Guilds, because they can't build Banks, because they can't build Universities
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db));

		// Orcs can't build Ship Wrights' Guilds if there's no water
		cityData.setCityRaceID ("RC09");
		ocean.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL12", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db));

		// Orcs can't build Ship Yards if there's no water, because they can't build a Ship Wrights' Guild
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL13", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db));

		// Orcs can't build Merchants' Guilds if there's no water, because they can't build a Ship Yard, because they can't build a Ship Wrights' Guild
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db));

		// If we got a Ship Wrights' Guild and subsequently the water all dried up then we *can* then construct the other building types
		// (Ok bad example, but similar with Sawmills + forests disappearing is definitely possible)
		final MemoryBuilding shipWrightsGuild = new MemoryBuilding ();
		shipWrightsGuild.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		shipWrightsGuild.setBuildingID ("BL12");

		buildings.add (shipWrightsGuild);

		assertTrue (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL13", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db));
		assertTrue (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getMapSize (), db));
	}
}