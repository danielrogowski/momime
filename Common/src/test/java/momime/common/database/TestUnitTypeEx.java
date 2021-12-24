package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the UnitTypeex class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitTypeEx
{
	/**
	 * Tests the findExperienceLevel method on a level that exists
	 */
	@Test
	public final void testFindExperienceLevel_Exists ()
	{
		final UnitTypeEx unitType = new UnitTypeEx ();
		for (int n = 0; n <= 2; n++)
		{
			final ExperienceLevel level = new ExperienceLevel ();
			level.setLevelNumber (n);
			unitType.getExperienceLevel ().add (level);
		}

		assertEquals (2, unitType.findExperienceLevel (2).getLevelNumber ());
	}

	/**
	 * Tests the findExperienceLevel method on a level that doesn't exist
	 */
	@Test
	public final void testFindExperienceLevel_NotExists ()
	{
		final UnitTypeEx unitType = new UnitTypeEx ();
		for (int n = 0; n <= 2; n++)
		{
			final ExperienceLevel level = new ExperienceLevel ();
			level.setLevelNumber (n);
			unitType.getExperienceLevel ().add (level);
		}

		assertNull (unitType.findExperienceLevel (3));
	}
}