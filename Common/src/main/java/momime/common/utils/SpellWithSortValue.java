package momime.common.utils;

import momime.common.database.v0_9_5.Spell;

/**
 * SpellUtils.getSortedSpellsInSection needs to sort spells by either their combat, overland or research cost
 * Since we can't modify the Spell generated class itself, need this wrapper around it in order to implement Comparable *
 */
class SpellWithSortValue implements Comparable<SpellWithSortValue>
{
	/** Spell we want to sort */
	final Spell spell;

	/** Value we want to sort by */
	final int sortValue;

	/**
	 * @param aSpell Spell we want to sort
	 * @param aSortValue Value we want to sort by
	 */
	SpellWithSortValue (final Spell aSpell, final int aSortValue)
	{
		spell = aSpell;
		sortValue = aSortValue;
	}

	/**
	 * @param o Spell to compare against
	 * @return 0, positive or negative value in order for the list to sort correctly
	 */
	@Override
	public final int compareTo (final SpellWithSortValue o)
	{
		return sortValue - o.sortValue;
	}
}