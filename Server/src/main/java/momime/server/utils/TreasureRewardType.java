package momime.server.utils;

/**
 * Enum covering all possible kinds of treasure reward
 */
enum TreasureRewardType
{
	/** Hero item reward */
	HERO_ITEM (1),
	
	/** Learn a spell reward */
	SPELL (2),
	
	/** Gold coins reward */
	GOLD (1),

	/** Mana crystals reward */
	MANA (1),
	
	/** Special/picks reward, i.e. spell book or retort */
	SPECIAL (3),
	
	/** Prisoner reward, i.e. as per summon hero spell */
	PRISONER (2);

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