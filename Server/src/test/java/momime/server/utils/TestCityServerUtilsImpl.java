package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.v0_9_4.RaceCannotBuild;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.Race;
import momime.server.database.v0_9_4.Unit;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CityServerUtilsImpl class
 */
public final class TestCityServerUtilsImpl
{
	/**
	 * Tests the validateCityConstruction method when the ID requested is neither a valid building or unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_InvalidConstructionProject () throws Exception
	{
		// Mock details
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (false);

		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a building that is fine (also some tests around incorrect city owner)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building () throws Exception
	{
		// Mock details
		final Race raceDef = new Race ();
		
		final Building blacksmithDef = new Building ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (false);
		when (buildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, blacksmithDef)).thenReturn (true);

		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
		when (cityCalc.buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, blacksmithDef, sys)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		// Blacksmith - can't yet because we didn't set the city player yet so ANY change is invalid, even one with no prerequisities
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));

		cityData.setCityOwnerID (1);
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));

		cityData.setCityOwnerID (2);
		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a building that we have already
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building_HasAlready () throws Exception
	{
		// Mock details
		final Race raceDef = new Race ();
		
		final Building blacksmithDef = new Building ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (true);		// <---
		when (buildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, blacksmithDef)).thenReturn (true);

		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
		when (cityCalc.buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, blacksmithDef, sys)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a building that our race can't build
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building_RaceCannotBuild () throws Exception
	{
		// Mock details
		final RaceCannotBuild rcb = new RaceCannotBuild ();
		rcb.setCannotBuildBuildingID ("BL08");
		
		final Race raceDef = new Race ();
		raceDef.getRaceCannotBuild ().add (rcb);		// <---
		
		final Building blacksmithDef = new Building ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (false);
		when (buildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, blacksmithDef)).thenReturn (true);

		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
		when (cityCalc.buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, blacksmithDef, sys)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a building that we don't have a pre-requisite building for (e.g. build farmers' market with no granary)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building_MissingPreqBuilding () throws Exception
	{
		// Mock details
		final Race raceDef = new Race ();
		
		final Building blacksmithDef = new Building ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (false);
		when (buildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, blacksmithDef)).thenReturn (false);

		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
		when (cityCalc.buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, blacksmithDef, sys)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a building that we don't have a pre-requisite tile type for (e.g. build a ship wrights' guild with no ocean nearby)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building_MissingPrereqTile () throws Exception
	{
		// Mock details
		final Race raceDef = new Race ();
		
		final Building blacksmithDef = new Building ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (false);
		when (buildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, blacksmithDef)).thenReturn (true);

		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
		when (cityCalc.buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, blacksmithDef, sys)).thenReturn (false);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a unit that is fine
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit () throws Exception
	{
		// Mock details
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		unitDef.setUnitRaceID ("RC09");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.meetsUnitRequirements (trueMap.getBuilding (), cityLocation, unitDef)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a unit that is the wrong type (hero or summoned)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit_WrongUnitType () throws Exception
	{
		// Mock details
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm ("X");		// <---
		unitDef.setUnitRaceID ("RC09");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.meetsUnitRequirements (trueMap.getBuilding (), cityLocation, unitDef)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a unit that is the wrong race
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit_WrongRace () throws Exception
	{
		// Mock details
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		unitDef.setUnitRaceID ("RC08");		// <---
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.meetsUnitRequirements (trueMap.getBuilding (), cityLocation, unitDef)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a unit that has no race (hero or summoned)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit_NoRace () throws Exception
	{
		// Mock details
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		unitDef.setUnitRaceID (null);		// <---
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.meetsUnitRequirements (trueMap.getBuilding (), cityLocation, unitDef)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a unit that we don't have a pre-requisite building for (e.g. build swordsmen without a barracks) 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit_MissingPreqBuilding () throws Exception
	{
		// Mock details
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		unitDef.setUnitRaceID ("RC09");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);

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
		cityLocation.setZ (0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.meetsUnitRequirements (trueMap.getBuilding (), cityLocation, unitDef)).thenReturn (false);		// <---
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateOptionalFarmers method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateOptionalFarmers () throws Exception
	{
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setZ (0);

		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		
		//  Can't set yet, even to a zero which is always valid, because we didn't set the city player yet so ANY change is invalid
		assertNotNull (utils.validateOptionalFarmers (player, trueTerrain, cityLocation, 0));

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setMinimumFarmers (1);
		cityData.setNumberOfRebels (2);
		cityData.setCityPopulation (5678);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNotNull (utils.validateOptionalFarmers (player, trueTerrain, cityLocation, 0));

		cityData.setCityOwnerID (1);
		assertNotNull (utils.validateOptionalFarmers (player, trueTerrain, cityLocation, 0));

		cityData.setCityOwnerID (2);
		assertNull (utils.validateOptionalFarmers (player, trueTerrain, cityLocation, 0));

		// Try invalid values
		assertNotNull (utils.validateOptionalFarmers (player, trueTerrain, cityLocation, -1));
		assertNotNull (utils.validateOptionalFarmers (player, trueTerrain, cityLocation, 3));

		// Try valid value
		assertNull (utils.validateOptionalFarmers (player, trueTerrain, cityLocation, 2));
	}
	
	/**
	 * Tests the totalCostOfBuildingsAtLocation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testTotalCostOfBuildingsAtLocation () throws Exception
	{
		// Mock some building details
		final Building buildersHallDef = new Building ();
		buildersHallDef.setProductionCost (60);
		
		final Building bankDef = new Building ();
		bankDef.setProductionCost (250);
		
		final Building summoningCircleDef = new Building ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findBuilding ("BL32", "totalCostOfBuildingsAtLocation")).thenReturn (buildersHallDef);
		when (db.findBuilding ("BL27", "totalCostOfBuildingsAtLocation")).thenReturn (bankDef);
		when (db.findBuilding (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, "totalCostOfBuildingsAtLocation")).thenReturn (summoningCircleDef);
		
		// City location
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (20);
		cityLocation.setY (10);
		cityLocation.setZ (1);
		
		// Buildings list
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final OverlandMapCoordinatesEx buildersHallLocation = new OverlandMapCoordinatesEx ();
		buildersHallLocation.setX (20);
		buildersHallLocation.setY (10);
		buildersHallLocation.setZ (1);
		
		final MemoryBuilding buildersHall = new MemoryBuilding ();
		buildersHall.setBuildingID ("BL32");	// costs 60
		buildersHall.setCityLocation (buildersHallLocation);
		buildings.add (buildersHall);

		final OverlandMapCoordinatesEx bankLocation = new OverlandMapCoordinatesEx ();
		bankLocation.setX (20);
		bankLocation.setY (10);
		bankLocation.setZ (1);
		
		final MemoryBuilding bank = new MemoryBuilding ();
		bank.setBuildingID ("BL27");	// costs 250
		bank.setCityLocation (bankLocation);
		buildings.add (bank);

		final OverlandMapCoordinatesEx summoningCircleLocation = new OverlandMapCoordinatesEx ();
		summoningCircleLocation.setX (20);
		summoningCircleLocation.setY (10);
		summoningCircleLocation.setZ (1);
		
		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE);	// no cost defined
		summoningCircle.setCityLocation (summoningCircleLocation);
		buildings.add (summoningCircle);
		
		final OverlandMapCoordinatesEx bankElsewhereLocation = new OverlandMapCoordinatesEx ();
		bankElsewhereLocation.setX (20);
		bankElsewhereLocation.setY (11);
		bankElsewhereLocation.setZ (1);
		
		final MemoryBuilding bankElsewhere = new MemoryBuilding ();
		bankElsewhere.setBuildingID ("BL27");	// costs 250
		bankElsewhere.setCityLocation (bankElsewhereLocation);
		buildings.add (bankElsewhere);
		
		// Run method
		assertEquals (310, new CityServerUtilsImpl ().totalCostOfBuildingsAtLocation (cityLocation, buildings, db));
	}
}
