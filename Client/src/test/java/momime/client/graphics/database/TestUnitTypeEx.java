package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import momime.client.graphics.database.v0_9_5.ExperienceLevel;

import org.junit.Test;

/**
 * Tests the UnitTypeEx class
 */
public final class TestUnitTypeEx
{
	/**
	 * Tests the findExperienceLevelImageFile method
	 */
	@Test
	public final void testFindExperienceLevelImageFile ()
	{
		// Create some dummy entries
		final UnitTypeEx unitType = new UnitTypeEx ();
		for (int n = 1; n <= 3; n++)
		{
			final ExperienceLevel expLvl = new ExperienceLevel ();
			expLvl.setLevelNumber (n);
			expLvl.setExperienceLevelImageFile ("L" + n + ".png");
			
			unitType.getExperienceLevel ().add (expLvl);
		}
		
		unitType.buildMap ();
		
		// Run tests
		assertEquals ("L2.png", unitType.findExperienceLevelImageFile (2));
		assertNull (unitType.findExperienceLevelImageFile (4));
	}
}