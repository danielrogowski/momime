package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonDatabaseLookup;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Building;
import momime.common.database.v0_9_4.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_4.BuildingPrerequisite;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitPrerequisite;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the MemoryBuildingUtils class
 */
public final class TestMemoryBuildingUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

	/**
	 * Lots of methods need a city location, so this is just a helper method to create one
	 * This also helps discourage reusing the same object twice, which is dangerous because doesn't trap accidental use of .equals instead of proper comparison method
	 * @param offset If 0 generates city at standard location; set this to a non-zero value to generate a different location
	 * @return City location
	 */
	private final OverlandMapCoordinates createCityLocation (final int offset)
	{
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (15 + offset);
		cityLocation.setY (10);
		cityLocation.setPlane (1);

		return cityLocation;
	}

	/**
	 * Tests the findBuilding method with an empty list
	 */
	@Test
	public final void testFindBuilding_EmptyList ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		assertFalse (MemoryBuildingUtils.findBuilding (buildingsList, createCityLocation (0), "BL01", debugLogger));
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

		assertFalse (MemoryBuildingUtils.findBuilding (buildingsList, createCityLocation (0), "BL02", debugLogger));
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

		assertFalse (MemoryBuildingUtils.findBuilding (buildingsList, createCityLocation (1), "BL01", debugLogger));
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

		assertTrue (MemoryBuildingUtils.findBuilding (buildingsList, createCityLocation (0), "BL01", debugLogger));
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

		assertTrue (MemoryBuildingUtils.findBuilding (buildingsList, createCityLocation (0), "BL02", debugLogger));
	}

	/**
	 * Tests the destroyBuilding method with an empty list
	 * @throws RecordNotFoundException If we can't find the requested building
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testDestroyBuilding_EmptyList () throws RecordNotFoundException
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		MemoryBuildingUtils.destroyBuilding (buildingsList, createCityLocation (0), "BL01", debugLogger);
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

		MemoryBuildingUtils.destroyBuilding (buildingsList, createCityLocation (0), "BL02", debugLogger);
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

		MemoryBuildingUtils.destroyBuilding (buildingsList, createCityLocation (1), "BL01", debugLogger);
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

		MemoryBuildingUtils.destroyBuilding (buildingsList, createCityLocation (0), "BL01", debugLogger);
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

		MemoryBuildingUtils.destroyBuilding (buildingsList, createCityLocation (0), "BL02", debugLogger);
		assertEquals (1, buildingsList.size ());
		assertEquals ("BL01", buildingsList.get (0).getBuildingID ());
	}

	/**
	 * Tests the findCityWithBuilding method
	 */
	@Test
	public final void testFindCityWithBuilding ()
	{
		final OverlandMapCoordinates buildingLocation = new OverlandMapCoordinates ();
		buildingLocation.setX (2);
		buildingLocation.setY (2);
		buildingLocation.setPlane (0);

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding building = new MemoryBuilding ();
		building.setCityLocation (buildingLocation);
		building.setBuildingID ("BL01");
		buildings.add (building);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// City has the building, but we can't see it
		assertNull (MemoryBuildingUtils.findCityWithBuilding (1, "BL01", map, buildings, debugLogger));

		// Wrong owner
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityPopulation (1);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (2).setCityData (cityData);
		assertNull (MemoryBuildingUtils.findCityWithBuilding (1, "BL01", map, buildings, debugLogger));

		// Right owner
		cityData.setCityOwnerID (1);
		assertEquals (buildingLocation, MemoryBuildingUtils.findCityWithBuilding (1, "BL01", map, buildings, debugLogger));
	}

	/**
	 * Tests the meetsBuildingRequirements method on a building that has no pre-requisites
	 */
	@Test
	public final void testMeetsBuildingRequirements_NoPrerequisites ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		final Building building = new Building ();

		assertTrue (MemoryBuildingUtils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building, debugLogger));
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

		assertFalse (MemoryBuildingUtils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building, debugLogger));
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

		assertFalse (MemoryBuildingUtils.meetsBuildingRequirements (buildingsList, createCityLocation (1), building, debugLogger));
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

		assertTrue (MemoryBuildingUtils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building, debugLogger));
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

		assertFalse (MemoryBuildingUtils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building, debugLogger));
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

		assertTrue (MemoryBuildingUtils.meetsBuildingRequirements (buildingsList, createCityLocation (0), building, debugLogger));
	}

	/**
	 * Tests the meetsUnitRequirements method on a building that has no pre-requisites
	 */
	@Test
	public final void testMeetsUnitRequirements_NoPrerequisites ()
	{
		final List<MemoryBuilding> buildingsList = new ArrayList<MemoryBuilding> ();

		final Unit unit = new Unit ();

		assertTrue (MemoryBuildingUtils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit, debugLogger));
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

		assertFalse (MemoryBuildingUtils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit, debugLogger));
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

		assertFalse (MemoryBuildingUtils.meetsUnitRequirements (buildingsList, createCityLocation (1), unit, debugLogger));
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

		assertTrue (MemoryBuildingUtils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit, debugLogger));
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

		assertFalse (MemoryBuildingUtils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit, debugLogger));
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

		assertTrue (MemoryBuildingUtils.meetsUnitRequirements (buildingsList, createCityLocation (0), unit, debugLogger));
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
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		final BuildingPrerequisite prereq = new BuildingPrerequisite ();
		prereq.setPrerequisiteID ("BL02");
		dbBuildingOne.getBuildingPrerequisite ().add (prereq);
		dbBuildings.add (dbBuildingOne);

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildings.add (dbBuildingTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding memBuilding = new MemoryBuilding ();
			memBuilding.setBuildingID ("BL0" + n);
			memBuilding.setCityLocation (createCityLocation (0));
			memBuildings.add (memBuilding);
		}

		assertNull (MemoryBuildingUtils.doAnyBuildingsDependOn (memBuildings, createCityLocation (0), "BL01", db, debugLogger));
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
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		final BuildingPrerequisite prereq = new BuildingPrerequisite ();
		prereq.setPrerequisiteID ("BL02");
		dbBuildingOne.getBuildingPrerequisite ().add (prereq);
		dbBuildings.add (dbBuildingOne);

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildings.add (dbBuildingTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		for (int n = 1; n <= 2; n++)
		{
			final MemoryBuilding memBuilding = new MemoryBuilding ();
			memBuilding.setBuildingID ("BL0" + n);
			memBuilding.setCityLocation (createCityLocation (0));
			memBuildings.add (memBuilding);
		}

		assertNotNull (MemoryBuildingUtils.doAnyBuildingsDependOn (memBuildings, createCityLocation (0), "BL02", db, debugLogger));
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
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		final BuildingPrerequisite prereq = new BuildingPrerequisite ();
		prereq.setPrerequisiteID ("BL02");
		dbBuildingOne.getBuildingPrerequisite ().add (prereq);
		dbBuildings.add (dbBuildingOne);

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildings.add (dbBuildingTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

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
		assertNull (MemoryBuildingUtils.doAnyBuildingsDependOn (memBuildings, createCityLocation (1), "BL02", db, debugLogger));
	}

	/**
	 * Tests the isBuildingAPrerequisiteFor method
	 */
	@Test
	public final void testIsBuildingAPrerequisiteFor ()
	{
		// Set up dummy XML definitions for couple of building types
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		final BuildingPrerequisite prereq = new BuildingPrerequisite ();
		prereq.setPrerequisiteID ("BL02");
		dbBuildingOne.getBuildingPrerequisite ().add (prereq);
		dbBuildings.add (dbBuildingOne);

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildings.add (dbBuildingTwo);

		// Set up dummy XML definitions for couple of unit types
		final List<Unit> dbUnits = new ArrayList<Unit> ();
		final Unit dbUnitOne = new Unit ();
		dbUnitOne.setUnitID ("UN001");
		final UnitPrerequisite prerequ = new UnitPrerequisite ();
		prerequ.setPrerequisiteID ("BL01");
		dbUnitOne.getUnitPrerequisite ().add (prerequ);
		dbUnits.add (dbUnitOne);

		final Unit dbUnitTwo = new Unit ();
		dbUnitTwo.setUnitID ("UN002");
		dbUnits.add (dbUnitTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, dbUnits, null, null, null, null, dbBuildings, null, null);

		// Building tests
		assertTrue (MemoryBuildingUtils.isBuildingAPrerequisiteFor ("BL02", "BL01", db, debugLogger));
		assertFalse (MemoryBuildingUtils.isBuildingAPrerequisiteFor ("BL01", "BL02", db, debugLogger));

		// Unit tests
		assertTrue (MemoryBuildingUtils.isBuildingAPrerequisiteFor ("BL01", "UN001", db, debugLogger));
		assertFalse (MemoryBuildingUtils.isBuildingAPrerequisiteFor ("BL01", "UN002", db, debugLogger));

		// Test obscure code that doesn't exist
		assertFalse (MemoryBuildingUtils.isBuildingAPrerequisiteFor ("X", "Y", db, debugLogger));
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
		final List<Building> dbBuildings = new ArrayList<Building> ();
		for (int n = 1; n <= 3; n++)
		{
			final Building dbBuilding = new Building ();
			dbBuilding.setBuildingID ("BL0" + n);

			if (n > 1)
				dbBuilding.setBuildingExperience ((n - 2) * 10);

			dbBuildings.add (dbBuilding);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

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
		assertEquals (0, MemoryBuildingUtils.experienceFromBuildings (memBuildings, createCityLocation (0), db, debugLogger));
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
		final List<Building> dbBuildings = new ArrayList<Building> ();
		for (int n = 1; n <= 3; n++)
		{
			final Building dbBuilding = new Building ();
			dbBuilding.setBuildingID ("BL0" + n);
			dbBuilding.setBuildingExperience ((n - 1) * 10);
			dbBuildings.add (dbBuilding);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		assertEquals (0, MemoryBuildingUtils.experienceFromBuildings (memBuildings, createCityLocation (1), db, debugLogger));
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
		final List<Building> dbBuildings = new ArrayList<Building> ();
		for (int n = 1; n <= 3; n++)
		{
			final Building dbBuilding = new Building ();
			dbBuilding.setBuildingID ("BL0" + n);
			dbBuilding.setBuildingExperience ((n - 1) * 10);
			dbBuildings.add (dbBuilding);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		assertEquals (10, MemoryBuildingUtils.experienceFromBuildings (memBuildings, createCityLocation (0), db, debugLogger));
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
		final List<Building> dbBuildings = new ArrayList<Building> ();
		for (int n = 1; n <= 3; n++)
		{
			final Building dbBuilding = new Building ();
			dbBuilding.setBuildingID ("BL0" + n);
			dbBuilding.setBuildingExperience ((n - 1) * 10);
			dbBuildings.add (dbBuilding);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

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
		assertEquals (20, MemoryBuildingUtils.experienceFromBuildings (memBuildings, createCityLocation (0), db, debugLogger));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where the only buildings present do not grant a bonus
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_IrrelevantBuildings () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		dbBuildings.add (dbBuildingOne);

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);
		dbBuildings.add (dbBuildingTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL01");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		assertEquals (0, MemoryBuildingUtils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT01", "RE01", db, debugLogger));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where the only building that grants a bonus is in the wrong location
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_WrongLocation () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		dbBuildings.add (dbBuildingOne);

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);
		dbBuildings.add (dbBuildingTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		assertEquals (0, MemoryBuildingUtils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (1), "PT01", "RE01", db, debugLogger));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where we the population task type is different
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_WrongPopTaskType () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		dbBuildings.add (dbBuildingOne);

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);
		dbBuildings.add (dbBuildingTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		assertEquals (0, MemoryBuildingUtils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT02", "RE01", db, debugLogger));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where we the production type is different
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_WrongProdType () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		dbBuildings.add (dbBuildingOne);

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);
		dbBuildings.add (dbBuildingTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		assertEquals (0, MemoryBuildingUtils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT01", "RE02", db, debugLogger));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where we get a bonus from one building
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_OneBuilding () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		dbBuildings.add (dbBuildingOne);

		final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
		mod.setDoubleAmount (6);
		mod.setPopulationTaskID ("PT01");
		mod.setProductionTypeID ("RE01");

		final Building dbBuildingTwo = new Building ();
		dbBuildingTwo.setBuildingID ("BL02");
		dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);
		dbBuildings.add (dbBuildingTwo);

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

		// Set up list of existing buildings
		final List<MemoryBuilding> memBuildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuilding memBuilding = new MemoryBuilding ();
		memBuilding.setBuildingID ("BL02");
		memBuilding.setCityLocation (createCityLocation (0));
		memBuildings.add (memBuilding);

		// Do test
		assertEquals (6, MemoryBuildingUtils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT01", "RE01", db, debugLogger));
	}

	/**
	 * Tests the totalBonusProductionPerPersonFromBuildings, where we get a bonus from two buildings
	 * @throws RecordNotFoundException If we encounter a building that can't be found in the DB
	 */
	@Test
	public final void testTotalBonusProductionPerPersonFromBuildings_TwoBuildings () throws RecordNotFoundException
	{
		// Set up dummy XML definitions for couple of building types
		// Building BL02 grants a +4 bonus; building BL03 grants a +6 bonus
		final List<Building> dbBuildings = new ArrayList<Building> ();
		final Building dbBuildingOne = new Building ();
		dbBuildingOne.setBuildingID ("BL01");
		dbBuildings.add (dbBuildingOne);

		for (int n = 2; n <= 3; n++)
		{
			final BuildingPopulationProductionModifier mod = new BuildingPopulationProductionModifier ();
			mod.setDoubleAmount (n * 2);
			mod.setPopulationTaskID ("PT01");
			mod.setProductionTypeID ("RE01");

			final Building dbBuildingTwo = new Building ();
			dbBuildingTwo.setBuildingID ("BL0" + n);
			dbBuildingTwo.getBuildingPopulationProductionModifier ().add (mod);
			dbBuildings.add (dbBuildingTwo);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, dbBuildings, null, null);

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
		assertEquals (10, MemoryBuildingUtils.totalBonusProductionPerPersonFromBuildings (memBuildings, createCityLocation (0), "PT01", "RE01", db, debugLogger));
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
		assertEquals (0, MemoryBuildingUtils.findBuildingConsumption (building, "RE05", debugLogger));

		// Production
		assertEquals (0, MemoryBuildingUtils.findBuildingConsumption (building, "RE01", debugLogger));

		// Percentage bonus
		assertEquals (0, MemoryBuildingUtils.findBuildingConsumption (building, "RE03", debugLogger));

		// Population consumption
		assertEquals (0, MemoryBuildingUtils.findBuildingConsumption (building, "RE04", debugLogger));

		// Consumption
		assertEquals (2, MemoryBuildingUtils.findBuildingConsumption (building, "RE02", debugLogger));
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

		MemoryBuildingUtils.findBuildingConsumption (building, "RE02", debugLogger);
	}

	/**
	 * Tests the goldFromSellingBuilding method
	 * @throws RecordNotFoundException If one of the buildings we try to test with can't be found in the test data
	 */
	@Test
	public final void testGoldFromSellingBuilding () throws RecordNotFoundException
	{
		final CommonDatabaseLookup db = GenerateTestData.createDB ();
		assertEquals (100, MemoryBuildingUtils.goldFromSellingBuilding (db.findBuilding (GenerateTestData.ANIMISTS_GUILD, "testGoldFromSellingBuilding")));
		assertEquals (0, MemoryBuildingUtils.goldFromSellingBuilding (db.findBuilding (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, "testGoldFromSellingBuilding")));
	}
}
