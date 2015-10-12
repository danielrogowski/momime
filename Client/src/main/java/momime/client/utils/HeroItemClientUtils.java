package momime.client.utils;

import java.util.List;

import momime.common.MomException;
import momime.common.database.HeroItemBonus;

/**
 * Client-side utils for dealing with hero items
 */
public interface HeroItemClientUtils
{
	/**
	 * Given a list of attribute bonuses, slots in a null when we swap from a bonus to one attribute to a bonus to another
	 * i.e. so we end up with for example Atk+1 Atk+2 Atk+3 ...gap... Def+1 Def+2 Def+3.
	 * 
	 * This doesn't sort the list - so we're assuming that they're listed in the XML in a sensible order already.
	 * 
	 * @param bonuses List of bonuses to update
	 * @throws MomException If we encounter a bonus in the list that doesn't give a bonus to *any* attribute, or to multiple attributes
	 */
	public void insertGapsBetweenDifferentKindsOfAttributeBonuses (final List<HeroItemBonus> bonuses) throws MomException;

	/**
	 * The attribute bonuses are displayed in 2 columns.  We don't want say Atk+1 and Atk+2 on the bottom of the left list, and Atk+3 on the top of the right list.
	 * So this method will insert nulls as necessary so try to manipulate the list such that a new attribute appears at the top of the right hand list.
	 * 
	 * NB. This method doesn't care anything about the bonuses, it only examines whether they are null or not null.  So it could work on List<Object> instead.
	 * 
	 * @param bonuses List of bonuses to update
	 * @param count The number of bonuses per column
	 * @throws MomException If after shuffling the items, we end up with a list that's too long to display (i.e. is > count * 2); or the left list is so long that we can't split it
	 */
	public void shuffleSplitPoint (final List<HeroItemBonus> bonuses, final int count) throws MomException;
}