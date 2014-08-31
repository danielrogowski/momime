package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.client.graphics.database.v0_9_5.ExperienceLevel;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the UnitTypeEx class
 */
public final class TestUnitTypeEx
{
	/**
	 * Tests the findExperienceLevelImageFile method to look for an experience level that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindExperienceLevelImageFile_Exists () throws RecordNotFoundException
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
		assertEquals ("L2.png", unitType.findExperienceLevelImageFile (2, "testFindExperienceLevelImageFile_Exists"));
	}

	/**
	 * Tests the findExperienceLevelImageFile method to look for an experience level that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindExperienceLevelImageFile_NotExists () throws RecordNotFoundException
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
		unitType.findExperienceLevelImageFile (4, "testFindExperienceLevelImageFile_NotExists");
	}
}