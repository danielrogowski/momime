package momime.client.utils;

import java.util.List;

import momime.common.MomException;
import momime.common.database.HeroItemBonus;

/**
 * Client-side utils for dealing with hero items
 */
public final class HeroItemClientUtilsImpl implements HeroItemClientUtils
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
	@Override
	public final void insertGapsBetweenDifferentKindsOfAttributeBonuses (final List<HeroItemBonus> bonuses) throws MomException
	{
		String lastSkillID = null;
		int index = 0;
		while (index < bonuses.size ())
		{
			final HeroItemBonus bonus = bonuses.get (index);
			
			if (bonus.getHeroItemBonusStat ().size () != 1)
				throw new MomException ("Hero item bonus " + bonus.getHeroItemBonusID () + " looks like an attribute bonus, but gives a bonus to " +
					bonus.getHeroItemBonusStat ().size () + " attributes - expected this to be exactly 1");
			
			final String thisSkillID = bonus.getHeroItemBonusStat ().get (0).getUnitSkillID ();
			
			// If its the first entry, just accept it
			if (lastSkillID == null)
			{
				lastSkillID = thisSkillID;
				index++;
			}
			
			// If its the same, just keep going
			else if (lastSkillID.equals (thisSkillID))
				index++;
			
			// It must be a change in attribute
			else
			{
				lastSkillID = thisSkillID;
				bonuses.add (index, null);
				index = index + 2;
			}
		}
	}

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
	@Override
	public final void shuffleSplitPoint (final List<HeroItemBonus> bonuses, final int count) throws MomException
	{
		// If the list already fits in 1 column, then there's nothing to do
		if (bonuses.size () <= count)
		{}

		// If there's a null at the top of the right hand column, then we can remove it and we're done
		else if (bonuses.get (count) == null)
			bonuses.remove (count);
		
		// Need to manipulate list until we get a null at the bottom of the left list
		else while (bonuses.get (count - 1) != null)
		{
			// Find the lowest null in the left list and add another at the same point
			int index = count - 1;
			while ((index >= 0) && (bonuses.get (index) != null))
				index--;
			
			if (index < 0)
				throw new MomException ("Can't find suitable split point to adjust left hand list of hero item attribute bonuses");
			
			bonuses.add (index, null);
		}
		
		if (bonuses.size () > count * 2)
			throw new MomException ("After all adjustments, list of hero item attribute bonuses spreads over more than 2 columns");
	}
}