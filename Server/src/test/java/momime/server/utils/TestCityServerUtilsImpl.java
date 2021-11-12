package momime.server.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndDoubledValue;
import momime.common.database.RaceEx;
import momime.common.database.RacePopulationTask;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.server.ServerTestData;

/**
 * Tests the CityServerUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCityServerUtilsImpl extends ServerTestData
{
	/**
	 * Tests the findClosestCityTo method with no cities
	 */
	@Test
	public final void testFindClosestCityTo_NoCities ()
	{
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		assertNull (utils.findClosestCityTo (new MapCoordinates3DEx (57, 20, 1), trueTerrain, sys));
	}
	
	/**
	 * Tests the findClosestCityTo method with some cities
	 */
	@Test
	public final void testFindClosestCityTo_Normal ()
	{
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		trueTerrain.getPlane ().get (0).getRow ().get (19).getCell ().get (56).setCityData (new OverlandMapCityData ());		// Very close, but on wrong plane
		trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (47).setCityData (new OverlandMapCityData ());		// Close
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (42).setCityData (new OverlandMapCityData ());		// Far
		trueTerrain.getPlane ().get (1).getRow ().get (19).getCell ().get (2).setCityData (new OverlandMapCityData ());		// Very close, around wrapping edge

		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		assertEquals (5, utils.findClosestCityTo (new MapCoordinates3DEx (57, 20, 1), trueTerrain, sys).intValue ());
	}
	
	/**
	 * Tests the validateCityConstruction method when the ID requested is neither a valid building or unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_InvalidConstructionProject () throws Exception
	{
		// Mock details
		final CommonDatabase db = mock (CommonDatabase.class);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);

		final CityCalculations cityCalc = mock (CityCalculations.class);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a building that is fine (also some tests around incorrect city owner)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building () throws Exception
	{
		// Mock details
		final RaceEx raceDef = new RaceEx ();
		
		final Building blacksmithDef = new Building ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (null);
		when (buildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, blacksmithDef)).thenReturn (true);

		final CityCalculations cityCalc = mock (CityCalculations.class);
		when (cityCalc.buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, blacksmithDef, sys)).thenReturn (true);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		// Blacksmith - can't yet because we didn't set the city player yet so ANY change is invalid, even one with no prerequisities
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));

		cityData.setCityOwnerID (1);
		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));

		cityData.setCityOwnerID (2);
		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a building that we have already
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building_HasAlready () throws Exception
	{
		// Mock details
		final Building blacksmithDef = new Building ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (new MemoryBuilding ());		// <---

		final CityCalculations cityCalc = mock (CityCalculations.class);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a building that our race can't build
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building_RaceCannotBuild () throws Exception
	{
		// Mock details
		final RaceEx raceDef = new RaceEx ();
		raceDef.getRaceCannotBuild ().add ("BL08");		// <---
		
		final Building blacksmithDef = new Building ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (null);

		final CityCalculations cityCalc = mock (CityCalculations.class);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a building that we don't have a pre-requisite building for (e.g. build farmers' market with no granary)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building_MissingPreqBuilding () throws Exception
	{
		// Mock details
		final RaceEx raceDef = new RaceEx ();
		
		final Building blacksmithDef = new Building ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (null);
		when (buildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, blacksmithDef)).thenReturn (false);

		final CityCalculations cityCalc = mock (CityCalculations.class);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a building that we don't have a pre-requisite tile type for (e.g. build a ship wrights' guild with no ocean nearby)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Building_MissingPrereqTile () throws Exception
	{
		// Mock details
		final RaceEx raceDef = new RaceEx ();
		
		final Building blacksmithDef = new Building ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding ("BL08", "validateCityConstruction")).thenReturn (blacksmithDef);
		when (db.findRace ("RC09", "validateCityConstruction")).thenReturn (raceDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuilding (trueMap.getBuilding (), cityLocation, "BL08")).thenReturn (null);
		when (buildingUtils.meetsBuildingRequirements (trueMap.getBuilding (), cityLocation, blacksmithDef)).thenReturn (true);

		final CityCalculations cityCalc = mock (CityCalculations.class);
		when (cityCalc.buildingPassesTileTypeRequirements (trueMap.getMap (), cityLocation, blacksmithDef, sys)).thenReturn (false);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);
		utils.setCityCalculations (cityCalc);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");		// Orcs can build everything
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, "BL08", null, sys, db));
	}

	/**
	 * Tests the validateCityConstruction method for constructing a unit that is fine
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit () throws Exception
	{
		// Mock details
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		unitDef.setUnitRaceID ("RC09");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

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

		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, null, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a unit that is the wrong type (hero or summoned)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit_WrongUnitType () throws Exception
	{
		// Mock details
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("X");		// <---
		unitDef.setUnitRaceID ("RC09");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, null, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a unit that is the wrong race
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit_WrongRace () throws Exception
	{
		// Mock details
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		unitDef.setUnitRaceID ("RC08");		// <---
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Validation checks
		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (buildingUtils);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC09");
		cityData.setCityOwnerID (2);
		trueTerrain.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, null, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a valid unit that has no race (trireme or catapault)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit_NoRace () throws Exception
	{
		// Mock details
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		unitDef.setUnitRaceID (null);		// <---
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

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

		assertNull (utils.validateCityConstruction (player, trueMap, cityLocation, null, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateCityConstruction method for constructing a unit that we don't have a pre-requisite building for (e.g. build swordsmen without a barracks) 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCityConstruction_Unit_MissingPreqBuilding () throws Exception
	{
		// Mock details
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		unitDef.setUnitRaceID ("RC09");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "validateCityConstruction")).thenReturn (unitDef);

		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

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

		assertNotNull (utils.validateCityConstruction (player, trueMap, cityLocation, null, "UN001", sys, db));
	}
	
	/**
	 * Tests the validateOptionalFarmers method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateOptionalFarmers () throws Exception
	{
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

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
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding ("BL32", "totalCostOfBuildingsAtLocation")).thenReturn (buildersHallDef);
		when (db.findBuilding ("BL27", "totalCostOfBuildingsAtLocation")).thenReturn (bankDef);
		when (db.findBuilding (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, "totalCostOfBuildingsAtLocation")).thenReturn (summoningCircleDef);
		
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Buildings list
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuilding buildersHall = new MemoryBuilding ();
		buildersHall.setBuildingID ("BL32");	// costs 60
		buildersHall.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (buildersHall);

		final MemoryBuilding bank = new MemoryBuilding ();
		bank.setBuildingID ("BL27");	// costs 250
		bank.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (bank);

		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setBuildingID (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE);	// no cost defined
		summoningCircle.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		buildings.add (summoningCircle);
		
		final MemoryBuilding bankElsewhere = new MemoryBuilding ();
		bankElsewhere.setBuildingID ("BL27");	// costs 250
		bankElsewhere.setCityLocation (new MapCoordinates3DEx (20, 11, 1));
		buildings.add (bankElsewhere);
		
		// Run method
		assertEquals (310, new CityServerUtilsImpl ().totalCostOfBuildingsAtLocation (cityLocation, buildings, db));
	}
	
	/**
	 * Tests the findCityWithinRadius method
	 */
	@Test
	public final void testFindCityWithinRadius ()
	{
		// Set up map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (sys);
		
		// Place a city
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (new OverlandMapCityData ());
		
		// Set up object to test
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Call method
		assertEquals (new MapCoordinates3DEx (20, 10, 1), utils.findCityWithinRadius (new MapCoordinates3DEx (20, 10, 1), terrain, sys));
		assertEquals (new MapCoordinates3DEx (20, 10, 1), utils.findCityWithinRadius (new MapCoordinates3DEx (22, 11, 1), terrain, sys));
		assertEquals (new MapCoordinates3DEx (20, 10, 1), utils.findCityWithinRadius (new MapCoordinates3DEx (21, 12, 1), terrain, sys));
		assertNull (utils.findCityWithinRadius (new MapCoordinates3DEx (22, 12, 1), terrain, sys));
		assertNull (utils.findCityWithinRadius (new MapCoordinates3DEx (21, 12, 0), terrain, sys));
	}

	/**
	 * Tests the calculateDoubleFarmingRate method on the simple case where there's no modifiers
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleFarmingRate_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeAndDoubledValue rations = new ProductionTypeAndDoubledValue ();
		rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		rations.setDoubledProductionValue (4);
		
		final RacePopulationTask farmer = new RacePopulationTask ();
		farmer.getRacePopulationTaskProduction ().add (rations);
		farmer.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		
		final RaceEx race = new RaceEx ();
		race.getRacePopulationTask ().add (farmer);
		when (db.findRace ("RC01", "calculateDoubleFarmingRate")).thenReturn (race);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// City
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = createOverlandMap (sys);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Call method
		assertEquals (4, utils.calculateDoubleFarmingRate (map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db));
	}

	/**
	 * Tests the calculateDoubleFarmingRate method when the race has no farmers defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleFarmingRate_NoFarmers () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeAndDoubledValue rations = new ProductionTypeAndDoubledValue ();
		rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		rations.setDoubledProductionValue (4);
		
		final RacePopulationTask farmer = new RacePopulationTask ();
		farmer.getRacePopulationTaskProduction ().add (rations);
		farmer.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER);
		
		final RaceEx race = new RaceEx ();
		race.getRacePopulationTask ().add (farmer);
		when (db.findRace ("RC01", "calculateDoubleFarmingRate")).thenReturn (race);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// City
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = createOverlandMap (sys);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Call method
		assertThrows (MomException.class, () ->
		{
			utils.calculateDoubleFarmingRate (map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db);
		});
	}

	/**
	 * Tests the calculateDoubleFarmingRate method when farmers don't have an amount of rations they generate defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleFarmingRate_FarmersDontGenerateRations () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeAndDoubledValue rations = new ProductionTypeAndDoubledValue ();
		rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		rations.setDoubledProductionValue (4);
		
		final RacePopulationTask farmer = new RacePopulationTask ();
		farmer.getRacePopulationTaskProduction ().add (rations);
		farmer.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		
		final RaceEx race = new RaceEx ();
		race.getRacePopulationTask ().add (farmer);
		when (db.findRace ("RC01", "calculateDoubleFarmingRate")).thenReturn (race);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// City
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = createOverlandMap (sys);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Set up object to test
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Call method
		assertThrows (MomException.class, () ->
		{
			utils.calculateDoubleFarmingRate (map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db);
		});
	}

	/**
	 * Tests the calculateDoubleFarmingRate method when we get a bonus per farmer from Animists' Guild
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleFarmingRate_AnimistsGuild () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeAndDoubledValue rations = new ProductionTypeAndDoubledValue ();
		rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		rations.setDoubledProductionValue (4);
		
		final RacePopulationTask farmer = new RacePopulationTask ();
		farmer.getRacePopulationTaskProduction ().add (rations);
		farmer.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		
		final RaceEx race = new RaceEx ();
		race.getRacePopulationTask ().add (farmer);
		when (db.findRace ("RC01", "calculateDoubleFarmingRate")).thenReturn (race);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.totalBonusProductionPerPersonFromBuildings (buildings, new MapCoordinates3DEx (20, 10, 1),
			CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, db)).thenReturn (2);

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// City
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = createOverlandMap (sys);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Set up object to test
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Call method
		assertEquals (6, utils.calculateDoubleFarmingRate (map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db));
	}

	/**
	 * Tests the calculateDoubleFarmingRate method when we get a bonus per farmer from Animists' Guild, but its then all halved by Famine
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleFarmingRate_Famine () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeAndDoubledValue rations = new ProductionTypeAndDoubledValue ();
		rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		rations.setDoubledProductionValue (4);
		
		final RacePopulationTask farmer = new RacePopulationTask ();
		farmer.getRacePopulationTaskProduction ().add (rations);
		farmer.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		
		final RaceEx race = new RaceEx ();
		race.getRacePopulationTask ().add (farmer);
		when (db.findRace ("RC01", "calculateDoubleFarmingRate")).thenReturn (race);

		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.totalBonusProductionPerPersonFromBuildings (buildings, new MapCoordinates3DEx (20, 10, 1),
			CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, db)).thenReturn (2);

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.findMaintainedSpell (spells, null, CommonDatabaseConstants.SPELL_ID_FAMINE, null, null,
			new MapCoordinates3DEx (20, 10, 1), null)).thenReturn (new MemoryMaintainedSpell ());

		// City
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = createOverlandMap (sys);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Set up object to test
		final CityServerUtilsImpl utils = new CityServerUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Call method
		assertEquals (3, utils.calculateDoubleFarmingRate (map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db));
	}
}