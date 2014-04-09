package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Building;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_4.BuildingPrerequisite;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitPrerequisite;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.OverlandMapCityData;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Tests the MemoryBuildingUtils class
 */
public final class TestMemoryBuildingUtilsImpl
{
	/**
	 * Lots of methods need a city location, so this is just a helper method to create one
	 * This also helps discourage reusing the same object twice, which is dangerous because doesn't trap accidental use of .equals instead of proper comparison method
	 * @param offset If 0 generates city at standard location; set this to a non-zero value to generate a different location
	 * @return City location
	 */
	private final MapCoordinates3DEx createCityLocation (final int offset)
	{
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx ();
		cityLocation.setX (15 + offset);
		cityLocation.setY (10);
		cityLocation.setZ (1);

		return cityLocation;
	}

	/**
	 * Tests the findBuilding method with an empty list
	 */
	@Test
	public final void testFindBuilding_EmptyList ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.findBuilding (buildingsList, createCityLocation (0), "BL01"));
	}

	/**
	 * Tests the findBuilding method with the wrong building ID
	 */
	@Test
	public final void testFindBuilding_WrongBuildingID ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (createCityLocation (0));
		buildingsList.add (building);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.findBuilding (buildingsList, createCityLocation (0), "BL02"));
	}

	/**
	 * Tests the findBuilding method with the wrong location
	 */
	@Test
	public final void testFindBuilding_WrongLocation ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (createCityLocation (0));
		buildingsList.add (building);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.findBuilding (buildingsList, createCityLocation (1), "BL01"));
	}

	/**
	 * Tests the findBuilding method with only one entry in the list
	 */
	@Test
	public final void testFindBuilding_SingleEntry ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (createCityLocation (0));
		buildingsList.add (building);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.findBuilding (buildingsList, createCityLocation (0), "BL01"));
	}

	/**
	 * Tests the findBuilding method with two entries in the list
	 */
	@Test
	public final void testFindBuilding_TwoEntries ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding building = new MemoryBuilding ();
			building.setBuildingID ("BL0" + n);
			building.setCityLocation (createCityLocation (0));
			buildingsList.add (building);
		}

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.findBuilding (buildingsList, createCityLocation (0), "BL02"));
	}

	/**
	 * Tests the destroyBuilding method with an empty list
	 * @throws RecordNotFoundException If we can't find the requested building
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testDestroyBuilding_EmptyList () throws RecordNotFoundException
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		utils.destroyBuilding (buildingsList, createCityLocation (0), "BL01");
	}

	/**
	 * Tests the destroyBuilding method with the wrong building ID
	 * @throws RecordNotFoundException If we can't find the requested building
	 */
	public final void testDestroyBuilding_WrongBuildingID () throws RecordNotFoundException
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (createCityLocation (0));
		buildingsList.add (building);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		utils.destroyBuilding (buildingsList, createCityLocation (0), "BL02");
	}

	/**
	 * Tests the destroyBuilding method with the wrong location
	 * @throws RecordNotFoundException If we can't find the requested building
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testDestroyBuilding_WrongLocation () throws RecordNotFoundException
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (createCityLocation (0));
		buildingsList.add (building);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		utils.destroyBuilding (buildingsList, createCityLocation (1), "BL01");
	}

	/**
	 * Tests the destroyBuilding method with only one entry in the list
	 * @throws RecordNotFoundException If we can't find the requested building
	 */
	@Test
	public final void testDestroyBuilding_SingleEntry () throws RecordNotFoundException
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (createCityLocation (0));
		buildingsList.add (building);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		utils.destroyBuilding (buildingsList, createCityLocation (0), "BL01");
	}

	/**
	 * Tests the destroyBuilding method with two entries in the list
	 * @throws RecordNotFoundException If we can't find the requested building
	 */
	@Test
	public final void testDestroyBuilding_TwoEntries () throws RecordNotFoundException
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding building = new MemoryBuilding ();
			building.setBuildingID ("BL0" + n);
			building.setCityLocation (createCityLocation (0));
			buildingsList.add (building);
		}

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		utils.destroyBuilding (buildingsList, createCityLocation (0), "BL02");
		assertEquals (1, buildingsList.size ());
		assertEquals ("BL01", buildingsList.get (0).getBuildingID ());
	}

	/**
	 * Tests the findCityWithBuilding method
	 */
	@Test
	public final void testFindCityWithBuilding ()
	{
		final MapCoordinates3DEx buildingLocation = new MapCoordinates3DEx ();
		buildingLocation.setX (2);
		buildingLocation.setY (2);
		buildingLocation.setZ (0);

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding building = new MemoryBuilding ();
		building.setCityLocation (buildingLocation);
		building.setBuildingID ("BL01");
		buildings.add (building);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City has the building, but we can't see it
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertNull (utils.findCityWithBuilding (1, "BL01", map, buildings));

		// Wrong owner
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityPopulation (1);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNull (utils.findCityWithBuilding (1, "BL01", map, buildings));

		// Right owner
		cityData.setCityOwnerID (1);
		assertEquals (buildingLocation, utils.findCityWithBuilding (1, "BL01", map, buildings));
	}

	/**
	 * Tests the meetsBuildingRequirements method on a building that has no pre-requisites
	 */
	@Test
	public final void testMeetsBuildingRequirements_NoPrerequisites ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		final Building building = new Building ();

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building));
	}

	/**
	 * Tests the meetsBuildingRequirements method on a building that has a pre-requisite
	 */
	@Test
	public final void testMeetsBuildingRequirements_MissingPrerequisite ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		final Building building = new Building ();
		final BuildingPrerequisite req = new BuildingPrerequisite ();
		req.setPrerequisiteID ("BL01");
		building.getBuildingPrerequisite ().add (req);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building));
	}

	/**
	 * Tests the meetsBuildingRequirements method on a building that has a pre-requisite which we have in the wrong location
	 */
	@Test
	public final void testMeetsBuildingRequirements_PrerequisiteInWrongLocation ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding mem = new MemoryBuilding ();
		mem.setBuildingID ("BL01");
		mem.setCityLocation (createCityLocation (0));
		buildingsList.add (mem);

		final Building building = new Building ();
		final BuildingPrerequisite req = new BuildingPrerequisite ();
		req.setPrerequisiteID ("BL01");
		building.getBuildingPrerequisite ().add (req);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.meetsBuildingRequirements (buildingsList, createCityLocation (1), building));
	}

	/**
	 * Tests the meetsBuildingRequirements method on a building that has a pre-requisite and we have it
	 */
	@Test
	public final void testMeetsBuildingRequirements_MeetsPrerequisite ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding mem = new MemoryBuilding ();
		mem.setBuildingID ("BL01");
		mem.setCityLocation (createCityLocation (0));
		buildingsList.add (mem);

		final Building building = new Building ();
		final BuildingPrerequisite req = new BuildingPrerequisite ();
		req.setPrerequisiteID ("BL01");
		building.getBuildingPrerequisite ().add (req);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building));
	}

	/**
	 * Tests the meetsBuildingRequirements method on a building that has two pre-requisites but we only have one of them
	 */
	@Test
	public final void testMeetsBuildingRequirements_TwoPrerequisitesOneMissing ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding mem = new MemoryBuilding ();
		mem.setBuildingID ("BL01");
		mem.setCityLocation (createCityLocation (0));
		buildingsList.add (mem);

		final Building building = new Building ();
		for (int n = 1; n <= 2; n++)
		{
			final BuildingPrerequisite req = new BuildingPrerequisite ();
			req.setPrerequisiteID ("BL0" + n);
			building.getBuildingPrerequisite ().add (req);
		}

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building));
	}

	/**
	 * Tests the meetsBuildingRequirements method on a building that has two pre-requisites which we have
	 */
	@Test
	public final void testMeetsBuildingRequirements_MeetsTwoPrerequisites ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding mem = new MemoryBuilding ();
			mem.setBuildingID ("BL0" + n);
			mem.setCityLocation (createCityLocation (0));
			buildingsList.add (mem);
		}

		final Building building = new Building ();
		for (int n = 1; n <= 2; n++)
		{
			final BuildingPrerequisite req = new BuildingPrerequisite ();
			req.setPrerequisiteID ("BL0" + n);
			building.getBuildingPrerequisite ().add (req);
		}

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building));
	}

	/**
	 * Tests the meetsUnitRequirements method on a building that has no pre-requisites
	 */
	@Test
	public final void testMeetsUnitRequirements_NoPrerequisites ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		final Unit unit = new Unit ();

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit));
	}

	/**
	 * Tests the meetsUnitRequirements method on a building that has a pre-requisite
	 */
	@Test
	public final void testMeetsUnitRequirements_MissingPrerequisite ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		final Unit unit = new Unit ();
		final UnitPrerequisite req = new UnitPrerequisite ();
		req.setPrerequisiteID ("BL01");
		unit.getUnitPrerequisite ().add (req);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit));
	}

	/**
	 * Tests the meetsUnitRequirements method on a building that has a pre-requisite which we have in the wrong location
	 */
	@Test
	public final void testMeetsUnitRequirements_PrerequisiteInWrongLocation ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding mem = new MemoryBuilding ();
		mem.setBuildingID ("BL01");
		mem.setCityLocation (createCityLocation (0));
		buildingsList.add (mem);

		final Unit unit = new Unit ();
		final UnitPrerequisite req = new UnitPrerequisite ();
		req.setPrerequisiteID ("BL01");
		unit.getUnitPrerequisite ().add (req);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.meetsUnitRequirements (buildingsList, createCityLocation (1), unit));
	}

	/**
	 * Tests the meetsUnitRequirements method on a building that has a pre-requisite and we have it
	 */
	@Test
	public final void testMeetsUnitRequirements_MeetsPrerequisite ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding mem = new MemoryBuilding ();
		mem.setBuildingID ("BL01");
		mem.setCityLocation (createCityLocation (0));
		buildingsList.add (mem);

		final Unit unit = new Unit ();
		final UnitPrerequisite req = new UnitPrerequisite ();
		req.setPrerequisiteID ("BL01");
		unit.getUnitPrerequisite ().add (req);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit));
	}

	/**
	 * Tests the meetsUnitRequirements method on a building that has two pre-requisites but we only have one of them
	 */
	@Test
	public final void testMeetsUnitRequirements_TwoPrerequisitesOneMissing ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding mem = new MemoryBuilding ();
		mem.setBuildingID ("BL01");
		mem.setCityLocation (createCityLocation (0));
		buildingsList.add (mem);

		final Unit unit = new Unit ();
		for (int n = 1; n <= 2; n++)
		{
			final UnitPrerequisite req = new UnitPrerequisite ();
			req.setPrerequisiteID ("BL0" + n);
			unit.getUnitPrerequisite ().add (req);
		}

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertFalse (utils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit));
	}

	/**
	 * Tests the meetsUnitRequirements method on a building that has two pre-requisites which we have
	 */
	@Test
	public final void testMeetsUnitRequirements_MeetsTwoPrerequisites ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding mem = new MemoryBuilding ();
			mem.setBuildingID ("BL0" + n);
			mem.setCityLocation (createCityLocation (0));
			buildingsList.add (mem);
		}

		final Unit unit = new Unit ();
		for (int n = 1; n <= 2; n++)
		{
			final UnitPrerequisite req = new UnitPrerequisite ();
			req.setPrerequisiteID ("BL0" + n);
			unit.getUnitPrerequisite ().add (req);
		}

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit));
	}

	/**
	 * Tests the doAnyBuildingsDependOn, where we have building BL01 that requires building BL02, and we
	 * want to sell building BL01, so that is fine, because BL02 requires nothing
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testDoAnyBuildingsDependOn_OkToSell () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		final BuildingPrerequisite prereq = new BuildingPrerequisite ();
		prereq.setPrerequisiteID ("BL02");
		dbBuildingOne.getBuildingPrerequisite ().add (prereq);

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding memBuilding = new MemoryBuilding ();
			memBuilding.setBuildingID ("BL0" + n);
			memBuilding.setCityLocation (createCityLocation (0));
			memBuildings.add (memBuilding);
		}

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertNull (utils.doAnyBuildingsDependOn (memBuildings, createCityLocation (0), "BL01", db));
	}

	/**
	 * Tests the doAnyBuildingsDependOn, where we have building BL01 that requires building BL02, and we
	 * want to sell building BL02, so we can't because BL01 still needs it
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testDoAnyBuildingsDependOn_CannotSell () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		final BuildingPrerequisite prereq = new BuildingPrerequisite ();
		prereq.setPrerequisiteID ("BL02");
		dbBuildingOne.getBuildingPrerequisite ().add (prereq);

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding memBuilding = new MemoryBuilding ();
			memBuilding.setBuildingID ("BL0" + n);
			memBuilding.setCityLocation (createCityLocation (0));
			memBuildings.add (memBuilding);
		}

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertNotNull (utils.doAnyBuildingsDependOn (memBuildings, createCityLocation (0), "BL02", db));
	}

	/**
	 * Tests the doAnyBuildingsDependOn, where we have building BL01 that requires building BL02, and we
	 * want to sell building BL02 but in a different location, so should be fine
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testDoAnyBuildingsDependOn_SellInOtherLocation () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		final BuildingPrerequisite prereq = new BuildingPrerequisite ();
		prereq.setPrerequisiteID ("BL02");
		dbBuildingOne.getBuildingPrerequisite ().add (prereq);

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding memBuilding = new MemoryBuilding ();
			memBuilding.setBuildingID ("BL0" + n);
			memBuilding.setCityLocation (createCityLocation (0));
			memBuildings.add (memBuilding);
		}

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertNull (utils.doAnyBuildingsDependOn (memBuildings, createCityLocation (1), "BL02", db));
	}

	/**
	 * Tests the isBuildingAPrerequisiteFor method
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testIsBuildingAPrerequisiteFor () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		final BuildingPrerequisite prereq = new BuildingPrerequisite ();
		prereq.setPrerequisiteID ("BL02");
		dbBuildingOne.getBuildingPrerequisite ().add (prereq);

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");

		// Set up dummy XML definitions for couple of unit types
		final Unit dbUnitOne = new Unit ();
		dbUnitOne.setUnitID ("UN001");
		final UnitPrerequisite prerequ = new UnitPrerequisite ();
		prerequ.setPrerequisiteID ("BL01");
		dbUnitOne.getUnitPrerequisite ().add (prerequ);

		final Unit dbUnitTwo = new Unit ();
		dbUnitTwo.setUnitID ("UN002");

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (dbUnitOne);
		when (db.findUnit (eq ("UN002"), anyString ())).thenReturn (dbUnitTwo);
		
		// Since the DB lookups throw RecordNotFoundException for not founds rather than outputting a null, we have to mock those too
		when (db.findBuilding (eq ("UN001"), anyString ())).thenThrow (new RecordNotFoundException (null, null, null));
		when (db.findBuilding (eq ("UN002"), anyString ())).thenThrow (new RecordNotFoundException (null, null, null));
		when (db.findBuilding (eq ("Y"), anyString ())).thenThrow (new RecordNotFoundException (null, null, null));
		when (db.findUnit (eq ("BL01"), anyString ())).thenThrow (new RecordNotFoundException (null, null, null));
		when (db.findUnit (eq ("BL02"), anyString ())).thenThrow (new RecordNotFoundException (null, null, null));
		when (db.findUnit (eq ("Y"), anyString ())).thenThrow (new RecordNotFoundException (null, null, null));

		// Building tests
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertTrue (utils.isBuildingAPrerequisiteFor ("BL02", "BL01", db));
		assertFalse (utils.isBuildingAPrerequisiteFor ("BL01", "BL02", db));

		// Unit tests
		assertTrue (utils.isBuildingAPrerequisiteFor ("BL01", "UN001", db));
		assertFalse (utils.isBuildingAPrerequisiteFor ("BL01", "UN002", db));

		// Test obscure code that doesn't exist
		assertFalse (utils.isBuildingAPrerequisiteFor ("X", "Y", db));
	}

	/**
	 * Tests the experienceFromBuildings method, where the only buildings present do not grant experience
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testExperienceFromBuildings_IrrelevantBuildings () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		// BL01 grants null exp, BL02 grants 0 exp, BL03 grants 10 exp
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 1; n <= 3; n++)
		{
			final Building dbBuilding = new Building ();
			dbBuilding.setBuildingID ("BL0" + n);

			if (n > 1)
				dbBuilding.setBuildingExperience ((n - 2) * 10);

			when (db.findBuilding (eq (dbBuilding.getBuildingID ()), anyString ())).thenReturn (dbBuilding);
		}

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding memBuilding = new MemoryBuilding ();
			memBuilding.setBuildingID ("BL0" + n);
			memBuilding.setCityLocation (createCityLocation (0));
			memBuildings.add (memBuilding);
		}

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (0, utils.experienceFromBuildings (memBuildings, createCityLocation (0), db));
	}

	/**
	 * Tests the experienceFromBuildings method, where the only buildings that grant experience are in the wrong place
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testExperienceFromBuildings_WrongLocation () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		// BL01 grants no exp, BL02 grants 10 exp, BL03 grants 20 exp
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 1; n <= 3; n++)
		{
			final Building dbBuilding = new Building ();
			dbBuilding.setBuildingID ("BL0" + n);
			dbBuilding.setBuildingExperience ((n - 1) * 10);

			when (db.findBuilding (eq (dbBuilding.getBuildingID ()), anyString ())).thenReturn (dbBuilding);
		}

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (0, utils.experienceFromBuildings (memBuildings, createCityLocation (1), db));
	}

	/**
	 * Tests the experienceFromBuildings method, where we have one building that grants experience
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testExperienceFromBuildings_OneBuilding () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		// BL01 grants no exp, BL02 grants 10 exp, BL03 grants 20 exp
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 1; n <= 3; n++)
		{
			final Building dbBuilding = new Building ();
			dbBuilding.setBuildingID ("BL0" + n);
			dbBuilding.setBuildingExperience ((n - 1) * 10);

			when (db.findBuilding (eq (dbBuilding.getBuildingID ()), anyString ())).thenReturn (dbBuilding);
		}

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (10, utils.experienceFromBuildings (memBuildings, createCityLocation (0), db));
	}

	/**
	 * Tests the experienceFromBuildings method, where we have two buildings that grant experience
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testExperienceFromBuildings_TwoBuildings () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		// BL01 grants no exp, BL02 grants 10 exp, BL03 grants 20 exp
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 1; n <= 3; n++)
		{
			final Building dbBuilding = new Building ();
			dbBuilding.setBuildingID ("BL0" + n);
			dbBuilding.setBuildingExperience ((n - 1) * 10);

			when (db.findBuilding (eq (dbBuilding.getBuildingID ()), anyString ())).thenReturn (dbBuilding);
		}

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		for (int n = 2; n <=3; n++)
		{
			final MemoryBuilding memBuilding = new MemoryBuilding ();
			memBuilding.setBuildingID ("BL0" + n);
			memBuilding.setCityLocation (createCityLocation (0));
			memBuildings.add (memBuilding);
		}

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (20, utils.experienceFromBuildings (memBuildings, createCityLocation (0), db));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where the only buildings present do not grant a bonus
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_IrrelevantBuildings () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL01");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (0, utils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT01", "RE01", db));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where the only building that grants a bonus is in the wrong location
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_WrongLocation () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (0, utils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (1), "PT01", "RE01", db));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where we the population task type is different
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_WrongPopTaskType () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (0, utils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT02", "RE01", db));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where we the production type is different
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_WrongProdType () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (0, utils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT01", "RE02", db));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where we get a bonus from one building
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_OneBuilding () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);
		when (db.findBuilding (eq ("BL02"), anyString ())).thenReturn (dbBuildingTwo);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (6, utils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT01", "RE01", db));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where we get a bonus from two buildings
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_TwoBuildings () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Building BL02 grants a +4 bonus; building BL03 grants a +6 bonus
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (dbBuildingOne);

		for (int n = 2; n <= 3; n++)
		{
			final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
			mod.setDoubleAmount (n * 2);
			mod.setPopulationTaskID ("PT01");
			mod.setProductionTypeID ("RE01");

			final Building dbBuildingTwo = new Building ();
			dbBuildingTwo.setBuildingID ("BL0" + n);
			dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);
			
			when (db.findBuilding (eq (dbBuildingTwo.getBuildingID ()), anyString ())).thenReturn (dbBuildingTwo);
		}

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		for (int n = 2; n <= 3; n++)
		{
			final MemoryBuilding memBuilding = new MemoryBuilding ();
			memBuilding.setBuildingID ("BL0" + n);
			memBuilding.setCityLocation (createCityLocation (0));
			memBuildings.add (memBuilding);
		}

		// Do test
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (10, utils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT01", "RE01", db));
	}

	/**
	 * Tests the findBuildingConsumption method
	 * @throws MomException If we find a building consumption that isn't a multiple of 2
	 */
	@Test
	public final void testFindBuildingConsumption () throws MomException
	{
		final Building building = new Building ();

		final BuildingPopulationProductionModifier production = new BuildingPopulationProductionModifier ();
		production.setProductionTypeID ("RE01");
		production.setDoubleAmount (3);
		building.getBuildingPopulationProductionModifier ().add (production);

		final BuildingPopulationProductionModifier consumption = new BuildingPopulationProductionModifier ();
		consumption.setProductionTypeID ("RE02");
		consumption.setDoubleAmount (-4);
		building.getBuildingPopulationProductionModifier ().add (consumption);

		final BuildingPopulationProductionModifier percentage = new BuildingPopulationProductionModifier ();
		percentage.setProductionTypeID ("RE03");
		percentage.setPercentageBonus (50);
		building.getBuildingPopulationProductionModifier ().add (percentage);

		final BuildingPopulationProductionModifier population = new BuildingPopulationProductionModifier ();
		population.setProductionTypeID ("RE04");
		population.setPopulationTaskID ("PT01");
		population.setDoubleAmount (-4);
		building.getBuildingPopulationProductionModifier ().add (population);

		// Production that isn't even listed
		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (0, utils.findBuildingConsumption (building, "RE05"));

		// Production
		assertEquals (0, utils.findBuildingConsumption (building, "RE01"));

		// Percentage bonus
		assertEquals (0, utils.findBuildingConsumption (building, "RE03"));

		// Population consumption
		assertEquals (0, utils.findBuildingConsumption (building, "RE04"));

		// Consumption
		assertEquals (2, utils.findBuildingConsumption (building, "RE02"));
	}

	/**
	 * Tests the findBuildingConsumption method on an invalid consumption because it isn't a multiple of 2
	 * @throws MomException If we find a building consumption that isn't a multiple of 2
	 */
	@Test(expected=MomException.class)
	public final void testFindBuildingConsumption_Invalid () throws MomException
	{
		final Building building = new Building ();

		final BuildingPopulationProductionModifier consumption = new BuildingPopulationProductionModifier ();
		consumption.setProductionTypeID ("RE02");
		consumption.setDoubleAmount (-5);
		building.getBuildingPopulationProductionModifier ().add (consumption);

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		utils.findBuildingConsumption (building, "RE02");
	}

	/**
	 * Tests the goldFromSellingBuilding method
	 * @throws RecordNotFoundException If one of the buildings we try to test with can't be found in the test data
	 */
	@Test
	public final void testGoldFromSellingBuilding () throws RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();

		final MemoryBuildingUtilsImpl utils = new MemoryBuildingUtilsImpl ();
		assertEquals (100, utils.goldFromSellingBuilding (db.findBuilding (GenerateTestData.ANIMISTS_GUILD, "testGoldFromSellingBuilding")));
		assertEquals (0, utils.goldFromSellingBuilding (db.findBuilding (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, "testGoldFromSellingBuilding")));
	}
}
