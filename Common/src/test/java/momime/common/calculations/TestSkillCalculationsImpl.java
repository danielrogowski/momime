package momime.common.calculations;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests the calculations in the SkillCalculationsImpl class
 */
public final class TestSkillCalculationsImpl
{
	/**
	 * Tests the getSkillPointsRequiredToImproveSkillFrom method
	 */
	@Test
	public final void testGetSkillPointsRequiredToImproveSkillFrom_Zero ()
	{
		final SkillCalculationsImpl calc = new SkillCalculationsImpl ();
		assertEquals ("Special case for zero", 1, calc.getSkillPointsRequiredToImproveSkillFrom (0));
	}

	/**
	 * Tests the getSkillPointsRequiredToImproveSkillFrom method
	 */
	@Test
	public final void testGetSkillPointsRequiredToImproveSkillFrom_NonZero ()
	{
		final SkillCalculationsImpl calc = new SkillCalculationsImpl ();
		assertEquals ("Should take 2x current skill level to progress to the next casting skill", 24, calc.getSkillPointsRequiredToImproveSkillFrom (12));
	}

	/**
	 * Tests the getSkillPointsRequiredForCastingSkill method
	 */
	@Test
	public final void testGetSkillPointsRequiredForCastingSkill ()
	{
		final SkillCalculationsImpl calc = new SkillCalculationsImpl ();
		
		// This test assumes that getSkillPointsRequiredToImproveSkillFrom has tested OK
		// Totals up how many skill points we need per level using this function, but then tests we can compute the same value in one hit rather than iteratively
		int totalSkillPointsRequired = 0;
		for (int skillLevel = 1; skillLevel <= 100; skillLevel++)
		{
			// -1 is because this is how many skill points we need to progress FROM the previous level to this one
			totalSkillPointsRequired = totalSkillPointsRequired + calc.getSkillPointsRequiredToImproveSkillFrom (skillLevel - 1);
			assertEquals ("Unexpected result for skill level " + skillLevel, totalSkillPointsRequired, calc.getSkillPointsRequiredForCastingSkill (skillLevel));
		}
	}

	/**
	 * Tests the getCastingSkillForSkillPoints method
	 */
	@Test
	public final void testGetCastingSkillForSkillPoints ()
	{
		final SkillCalculationsImpl calc = new SkillCalculationsImpl ();
		
		// This test assumes that getSkillPointsRequiredToImproveSkillFrom has tested OK
		// 10,000 tests up to about casting skill 100
		int totalSkillPointsRequired = 0;
		for (int skillLevel = 0; skillLevel <= 100; skillLevel++)
		{
			final int nextSkillPointsRequired = totalSkillPointsRequired + calc.getSkillPointsRequiredToImproveSkillFrom (skillLevel);

			// So any number of skill points from totalSkillPointsRequired..nextSkillPointsRequired-1 should give us skillLevel as a result
			// nextSkillPointsRequired will push us to the next level so give skillLevel+1 as a result
			for (int skillPoints = totalSkillPointsRequired; skillPoints < nextSkillPointsRequired; skillPoints++)
				assertEquals ("Unexpected result for skill points " + skillPoints, skillLevel, calc.getCastingSkillForSkillPoints (skillPoints));

			totalSkillPointsRequired = nextSkillPointsRequired;
		}
	}
}
