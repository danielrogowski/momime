package momime.common.calculations;

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
public final class SkillCalculationsImpl implements SkillCalculations
{
	/**
	 * @param currentSkill Current casting skill of the wizard
	 * @return How many skill points we need to improve past the specified skill level (In total, i.e. doesn't consider if we've already spent some towards the improvement)
	 */
	@Override
	public final int getSkillPointsRequiredToImproveSkillFrom (final int currentSkill)
	{
		final int result;
		if (currentSkill <= 0)
			result = 1;
		else
			result = currentSkill * 2;		// Strategy guide p31

		return result;
	}

	/**
	 * @param castingSkill Desired casting skill of the wizard
	 * @return How many skill points we need to reach the specified casting skill (in total, from absolute zero skill point investment)
	 */
	@Override
	public final int getSkillPointsRequiredForCastingSkill (final int castingSkill)
	{
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

		return result;
	}

	/**
	 * @param skillPoints Amount of skill points (i.e. RE10 resource) the wizard has
	 * @return Casting skill we obtain from having the specified number skill points
	 */
	@Override
	public final int getCastingSkillForSkillPoints (final int skillPoints)
	{
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

		return result;
	}
}