package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}