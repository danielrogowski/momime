package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the RaceGfx class
 */
public final class TestRaceGfx
{
	/**
	 * Tests the findCivilianImageFile method to look for a population task that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCivilianImageFile_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final RaceGfx race = new RaceGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final RacePopulationTaskGfx task = new RacePopulationTaskGfx ();
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
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCivilianImageFile_NotExists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final RaceGfx race = new RaceGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final RacePopulationTaskGfx task = new RacePopulationTaskGfx ();
			task.setPopulationTaskID ("PT0" + n);
			task.setCivilianImageFile ("Blah" + n + ".png");
			
			race.getRacePopulationTask ().add (task);
		}
		
		race.buildMap ();
		
		// Run tests
		race.findCivilianImageFile ("PT04", "testFindCivilianImageFile_NotExists");
	}
}