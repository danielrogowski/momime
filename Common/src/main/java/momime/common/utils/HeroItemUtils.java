package momime.common.utils;

import java.util.List;

import momime.common.messages.NumberedHeroItem;

/**
 * Helper methods for working with hero items
 */
public interface HeroItemUtils
{
	/**
	 * @param heroItemURN Hero Item URN to search for
	 * @param heroItems List of hero items to search through
	 * @return Hero item with requested URN, or null if not found
	 */
	public NumberedHeroItem findHeroItemURN (final int heroItemURN, final List<NumberedHeroItem> heroItems);
}