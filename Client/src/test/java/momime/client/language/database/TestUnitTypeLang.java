package momime.client.language.database;

import static org.junit.Assert.assertEquals;
import momime.client.language.database.v0_9_5.ExperienceLevel;

import org.junit.Test;

/**
 * Tests the UnitTypeEx class
 */
public final class TestUnitTypeLang
{
	/**
	 * Tests the findExperienceLevelName method
	 */
	@Test
	public final void testFindExperienceLevelName ()
	{
		// Create some dummy entries
		final UnitTypeLang unitType = new UnitTypeLang ();
		for (int n = 1; n <= 3; n++)
		{
			final ExperienceLevel expLvl = new ExperienceLevel ();
			expLvl.setLevelNumber (n);
			expLvl.setExperienceLevelName ("L" + n);
			
			unitType.getExperienceLevel ().add (expLvl);
		}
		
		unitType.buildMap ();
		
		// Run tests
		assertEquals ("L2", unitType.findExperienceLevelName (2));
		assertEquals ("4", unitType.findExperienceLevelName (4));
	}
}