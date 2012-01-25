package momime.common.calculations;

import java.util.logging.Logger;

/**
 * Calculations for dealing with casting skill
 *
 * Naming convention here is:
 *
 * CastingSkill is the wizard's actual casting skill value
 *
 * SkillPoints is how much magic power the wizard had to channel
 *    into skill in order to attain their casting skill, i.e.
 *    this is the amount of stored RE10 production the wizard has
 */
public final class MomSkillCalculations
{
	/**
	 * @param currentSkill Current casting skill of the wizard
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return How many skill points we need to improve past the specified skill level (In total, i.e. doesn't consider if we've already spent some towards the improvement)
	 */
	public static final int getSkillPointsRequiredToImproveSkillFrom (final int currentSkill, final Logger debugLogger)
	{
		debugLogger.entering (MomSkillCalculations.class.getName (), "getSkillPointsRequiredToImproveSkillFrom", currentSkill);

		final int result;
		if (currentSkill <= 0)
			result = 1;
		else
			result = currentSkill * 2;		// Strategy guide p31

		debugLogger.exiting (MomSkillCalculations.class.getName(), "getSkillPointsRequiredToImproveSkillFrom", result);
		return result;
	}

	/**
	 * @param castingSkill Desired casting skill of the wizard
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return How many skill points we need to reach the specified casting skill (in total, from absolute zero skill point investment)
	 */
	public static final int getSkillPointsRequiredForCastingSkill (final int castingSkill, final Logger debugLogger)
	{
		debugLogger.entering (MomSkillCalculations.class.getName(), "getSkillPointsRequiredForCastingSkill", castingSkill);

	    /*
	     * To get from casting skill (x) to casting skill (x + 1) we need
	     * (2x) more skill points, which works out as below...
	     *
	     * Formula works for all values except zero
	     */
		final int result;
		if (castingSkill <= 0)
			result = 0;
		else
			result = (castingSkill * castingSkill) - castingSkill + 1;

		debugLogger.exiting (MomSkillCalculations.class.getName(), "getSkillPointsRequiredForCastingSkill", result);
		return result;
	}

	/**
	 * @param skillPoints Amount of skill points (i.e. RE10 resource) the wizard has
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Casting skill we obtain from having the specified number skill points
	 */
	public static final int getCastingSkillForSkillPoints (final int skillPoints, final Logger debugLogger)
	{
		debugLogger.entering (MomSkillCalculations.class.getName(), "getCastingSkillForSkillPoints", skillPoints);

		/*
		 * Odd formula but it works
		 * Old delphi formula was:
		 *
		 * x := Round (Int (Sqrt (SkillPoints)));
		 * Result := Round (Int (Sqrt (x + SkillPoints)));
		 */
		final int roundedSquareRootOfSkillPoints = (int) Math.sqrt (skillPoints);
		final int skillPointsPlusRoundedSquareRootOfSkillPoints = skillPoints + roundedSquareRootOfSkillPoints;
		final int result = (int) Math.sqrt (skillPointsPlusRoundedSquareRootOfSkillPoints);

		debugLogger.exiting (MomSkillCalculations.class.getName(), "getCastingSkillForSkillPoints", result);
		return result;
	}
}
