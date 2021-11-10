package momime.common.utils;

/**
 * Output from addSkillBonus method to describe whether the bonus was added, and if not, why not.
 * Nothing actually needs this, it just helps the unit tests verify that the method is making the right decisions for the right reasons.
 */
enum AddSkillBonusResult
{
	/** Trying to give +attack to settlers or +defence to phantom warriors */
	UNIT_DOES_NOT_HAVE_SKILL,
	
	/**
	 * ADD_FIXED or ADD_DIVISOR are handled in one method, LOCK, DIVIDE and MULTIPLY are handled in another, so this is returned when we call
	 * one of the methods with a type of adjustment that it doesn't deal with
	 */
	INCORRECT_TYPE_OF_ADJUSTMENT,
	
	/** Skill or magic realm of incoming attack is unknown (or more likely there is no attack, and we're calculating unit stats for another reason) so bonus can't apply */
	NO_INFO_ABOUT_INCOMING_ATTACK,
	
	/** Skill bonus only applies in combat, and we aren't in combat */
	NOT_IN_COMBAT,
	
	/** Skill bonus only applies to specific magic realms, like True Light giving +1 to Life creatures */
	WRONG_MAGIC_REALM_LIFEFORM_TYPE_ID,
	
	/** Skill bonus only applies to specific RATs, like Flame Blade will only affect bows/slings, not magic attacks or boulders */
	WRONG_RANGED_ATTACK_TYPE,
	
	/** Bonus only triggers against certain attack skills, like Holy Weapon gives +1 to hit, but only with melee attacks not ranged attacks */
	WRONG_ATTACK_SKILL,

	/** Bonus only triggers against attacks associated with certain magic realms, like Bless giving +3 defence against Death and Chaos attacks only */
	WRONG_ATTACK_MAGIC_REALM,
	
	/** All preconditions of the bonus were met and so it was applied (even if it was zero) */
	APPLIES;
}