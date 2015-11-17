package momime.server.utils;

/**
 * Enum covering all possible kinds of treasure reward
 */
enum TreasureRewardType
{
	/** Hero item reward */
	HERO_ITEM (5),
	
	/** Learn a spell reward */
	SPELL (3),
	
	/** Gold coins reward */
	GOLD (2),

	/** Mana crystals reward */
	MANA (2),
	
	/** Special/picks reward, i.e. spell book or retort */
	SPECIAL (2),
	
	/** Prisoner reward, i.e. as per summon hero spell */
	PRISONER (1);

	/** Relative chance of receiving each reward, e.g. hero items are most likely to be chosen */
	private final int relativeChance;
	
	/**
	 * @param aRelativeChance Relative chance of receiving each reward, e.g. hero items are most likely to be chosen
	 */
	private TreasureRewardType (final int aRelativeChance)
	{
		relativeChance = aRelativeChance;
	}

	/**
	 * @return Relative chance of receiving each reward, e.g. hero items are most likely to be chosen
	 */
	public final int getRelativeChance ()
	{
		return relativeChance;
	}
}