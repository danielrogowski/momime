package momime.server.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.database.HeroItem;
import momime.common.messages.NumberedHeroItem;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;

/**
 * Server only helper methods for dealing with hero items
 */
public final class HeroItemServerUtilsImpl implements HeroItemServerUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (HeroItemServerUtilsImpl.class);
	
	/**
	 * @param item Hero item stats
	 * @param gsk General server knowledge
	 * @return Hero item with allocated URN
	 */
	@Override
	public final NumberedHeroItem createNumberedHeroItem (final HeroItem item, final MomGeneralServerKnowledgeEx gsk)
	{
		log.trace ("Entering createNumberedHeroItem: " + item.getHeroItemName ());
		
		final NumberedHeroItem numberedItem = new NumberedHeroItem ();
		numberedItem.setHeroItemURN (gsk.getNextFreeHeroItemURN ());
		gsk.setNextFreeHeroItemURN (gsk.getNextFreeHeroItemURN () + 1);

		numberedItem.setHeroItemName (item.getHeroItemName ());
		numberedItem.setHeroItemTypeID (item.getHeroItemTypeID ());
		numberedItem.setHeroItemImageNumber (item.getHeroItemImageNumber ());
		numberedItem.setSpellID (item.getSpellID ());
		numberedItem.setSpellChargeCount (item.getSpellChargeCount ());
		numberedItem.getHeroItemChosenBonus ().addAll (item.getHeroItemChosenBonus ());
		
		log.trace ("Exiting createNumberedHeroItem = " + numberedItem.getHeroItemURN ());
		return numberedItem;
	}
}