package momime.server.utils;

import momime.common.database.HeroItem;
import momime.common.messages.NumberedHeroItem;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;

/**
 * Server only helper methods for dealing with hero items
 */
public interface HeroItemServerUtils
{
	/**
	 * @param item Hero item stats
	 * @param gsk General server knowledge
	 * @return Hero item with allocated URN
	 */
	public NumberedHeroItem createNumberedHeroItem (final HeroItem item, final MomGeneralServerKnowledgeEx gsk);
}