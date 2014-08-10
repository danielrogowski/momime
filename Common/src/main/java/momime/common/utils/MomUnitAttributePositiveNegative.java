package momime.common.utils;

/**
 * Similarly, we can request that only positive or negative parts of a unit attribute be calculated
 *
 * This is used on the unit info screen to show when stats have been reduced, e.g. by Black Prayer, Vertigo, Warp Reality
 */
public enum MomUnitAttributePositiveNegative
{
	/** Include only positive bonuses, e.g. from Prayer */
	POSITIVE,

	/** Include only negative bonuses, e.g. from Warp Reality */
	NEGATIVE,

	/** Include both positive and negative bonuses (this is all the server ever uses) */
	BOTH;
}
