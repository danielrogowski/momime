package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.messages.NumberedHeroItem;

/**
 * Helper methods for working with hero items
 */
public final class HeroItemUtilsImpl implements HeroItemUtils
{
	/**
	 * @param heroItemURN Hero Item URN to search for
	 * @param heroItems List of hero items to search through
	 * @return Hero item with requested URN, or null if not found
	 */
	@Override
	public final NumberedHeroItem findHeroItemURN (final int heroItemURN, final List<NumberedHeroItem> heroItems)
	{
		NumberedHeroItem found = null;
		
		final Iterator<NumberedHeroItem> iter = heroItems.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final NumberedHeroItem thisItem = iter.next ();
			if (thisItem.getHeroItemURN () == heroItemURN)
				found = thisItem;
		}

		return found;
	}
}