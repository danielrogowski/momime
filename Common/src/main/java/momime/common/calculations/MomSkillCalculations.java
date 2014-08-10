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
public interface MomSkillCalculations
{
	/**
	 * @param currentSkill Current casting skill of the wizard
	 * @return How many skill points we need to improve past the specified skill level (In total, i.e. doesn't consider if we've already spent some towards the improvement)
	 */
	public int getSkillPointsRequiredToImproveSkillFrom (final int currentSkill);

	/**
	 * @param castingSkill Desired casting skill of the wizard
	 * @return How many skill points we need to reach the specified casting skill (in total, from absolute zero skill point investment)
	 */
	public int getSkillPointsRequiredForCastingSkill (final int castingSkill);

	/**
	 * @param skillPoints Amount of skill points (i.e. RE10 resource) the wizard has
	 * @return Casting skill we obtain from having the specified number skill points
	 */
	public int getCastingSkillForSkillPoints (final int skillPoints);
}
