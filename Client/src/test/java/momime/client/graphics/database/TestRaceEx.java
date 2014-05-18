package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import momime.client.graphics.database.v0_9_5.RacePopulationTask;

import org.junit.Test;

/**
 * Tests the RaceEx class
 */
public final class TestRaceEx
{
	/**
	 * Tests the findEntry method
	 */
	@Test
	public final void testFindEntry ()
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
		assertEquals ("Blah2.png", race.findCivilianImageFile ("PT02"));
		assertNull (race.findCivilianImageFile ("PT04"));
	}
}
