package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import momime.common.database.ExperienceLevel;
import momime.common.database.UnitType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the UnitTypeUtils class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitTypeUtils
{
	/**
	 * Tests the findExperienceLevel method on a level that exists
	 */
	@Test
	public final void testFindExperienceLevel_Exists ()
	{
		final UnitType unitType = new UnitType ();
		for (int n = 0; n <= 2; n++)
		{
			final ExperienceLevel level = new ExperienceLevel ();
			level.setLevelNumber (n);
			unitType.getExperienceLevel ().add (level);
		}

		assertEquals (2, UnitTypeUtils.findExperienceLevel (unitType, 2).getLevelNumber ());
	}

	/**
	 * Tests the findExperienceLevel method on a level that doesn't exist
	 */
	@Test
	public final void testFindExperienceLevel_NotExists ()
	{
		final UnitType unitType = new UnitType ();
		for (int n = 0; n <= 2; n++)
		{
			final ExperienceLevel level = new ExperienceLevel ();
			level.setLevelNumber (n);
			unitType.getExperienceLevel ().add (level);
		}

		assertNull (UnitTypeUtils.findExperienceLevel (unitType, 3));
	}
}