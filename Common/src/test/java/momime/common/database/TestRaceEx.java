package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the RaceEx class
 */
@ExtendWith(MockitoExtension.class)
public final class TestRaceEx
{
	/**
	 * Tests the findCivilianImageFile method to look for a population task that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCivilianImageFile_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final RaceEx race = new RaceEx ();
		for (int n = 1; n <= 3; n++)
		{
			final RacePopulationTask task = new RacePopulationTask ();
			task.setPopulationTaskID ("PT0" + n);
			task.setCivilianImageFile ("Blah" + n + ".png");
			
			race.getRacePopulationTask ().add (task);
		}
		
		race.buildMap ();
		
		// Run tests
		assertEquals ("Blah2.png", race.findCivilianImageFile ("PT02", "testFindCivilianImageFile_Exists"));
	}

	/**
	 * Tests the findCivilianImageFile method to look for a population task that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCivilianImageFile_NotExists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final RaceEx race = new RaceEx ();
		for (int n = 1; n <= 3; n++)
		{
			final RacePopulationTask task = new RacePopulationTask ();
			task.setPopulationTaskID ("PT0" + n);
			task.setCivilianImageFile ("Blah" + n + ".png");
			
			race.getRacePopulationTask ().add (task);
		}
		
		race.buildMap ();
		
		// Run tests
		assertThrows (RecordNotFoundException.class, () ->
		{
			race.findCivilianImageFile ("PT04", "testFindCivilianImageFile_NotExists");
		});
	}
	
	/**
	 * Tests the getRaceCannotBuildFullList method
	 */
	@Test
	public final void testGetRaceCannotBuildFullList ()
	{
		// Explicitly include some buildings
		final RaceEx race = new RaceEx ();
		for (int n = 1; n <= 2; n++)
			race.getRaceCannotBuild ().add ("BL0" + n);
		
		// Define some other buildings
		// 1+2 we already explicitly cannot build
		// 3 requires 4+5
		// 5 requires 2
		// 4+6 we can build
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<Building> buildings = new ArrayList<Building> ();
		for (int n = 1; n <= 6; n++)
		{
			final Building building = new Building ();
			building.setBuildingID ("BL0" + n);
			
			if (n == 3)
			{
				building.getBuildingPrerequisite ().add ("BL04");
				building.getBuildingPrerequisite ().add ("BL05");
			}
			else if (n == 5)
				building.getBuildingPrerequisite ().add ("BL02");
			
			buildings.add (building);
		}
		
		when (db.getBuilding ()).thenReturn (buildings);
		
		// Run method
		final List<String> list = race.getRaceCannotBuildFullList (db);
		
		// Check results
		assertEquals (4, list.size ());
		assertEquals ("BL01", list.get (0));
		assertEquals ("BL02", list.get (1));
		assertEquals ("BL05", list.get (2));
		assertEquals ("BL03", list.get (3));
	}
}