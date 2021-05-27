package momime.client.newturnmessages;

import java.util.List;

import momime.client.languages.database.NewTurnMessages;
import momime.common.MomException;
import momime.common.database.LanguageText;

/**
 * These constants define the sort order that messages will appear on the NTM scroll.  Each different sort order has a corresponding
 * title for that category, so after sorting, a category title will be slotted it at each point that the sort order number changes.
 * 
 * These roughly, but not exactly, correspond to the actual subclasses of NewTurnMessageData, but there's no real need
 * for them to match up at all.  e.g. NewTurnMessagePopulationChange generates 2 different sort order numbers/titles depending
 * on whether the population is growing or not, and a lot of different types of message (research, targetting, casting) are all dumped under the 'Spells' heading.
 */
public enum NewTurnMessageSortOrder
{
	/** NTMs about one of our cities completing or aborting construction of a unit or building */
	 SORT_ORDER_CONSTRUCTION (1),
	
	/** NTMs about us losing units/buildings/spells because we couldn't afford the maintenance to pay for them */
	 SORT_ORDER_LACK_OF_PRODUCTION (2),
	
	/** NTMs about cities growing across a 1,000 population border */
	 SORT_ORDER_CITY_GROWTH (3),
	
	/** NTMs about cities dying across a 1,000 population border */
	 SORT_ORDER_CITY_DEATH (4),
	
	/** NTMs about researching or requiring targetting our spells), or anybody casting an overland enchantment */
	 SORT_ORDER_SPELLS (5),
	
	/** NTMs about us capturing or losing nodes */
	 SORT_ORDER_NODES (6),
	
	/** NTMs about units */
	SORT_ORDER_UNITS (7),
	
	/** NTMs about offers */
	SORT_ORDER_OFFERS (8);
	 
	 /** Numeric sort order */
	 private final int sortOrder;

	 /**
	  * @param aSortOrder Numeric sort order
	  */
	 private NewTurnMessageSortOrder (final int aSortOrder)
	 {
		 sortOrder = aSortOrder;
	 }

	 /**
	  * @return Numeric sort order
	  */
	 public final int getSortOrder ()
	 {
		 return sortOrder;
	 }
	 
	 /**
	  * @param lang Data structure holding language text
	  * @return Language text key for the category heading
	  * @throws MomException If the enum value is unknown
	  */
	 public final List<LanguageText> getLanguageText (final NewTurnMessages lang) throws MomException
	 {
		 final List<LanguageText> languageText;
		 switch (this)
		 {
			 case SORT_ORDER_CONSTRUCTION:
				 languageText = lang.getConstructionCategory ();
				 break;
				 
			 case SORT_ORDER_LACK_OF_PRODUCTION:
				 languageText = lang.getLackOfProductionCategory ();
				 break;
				 
			 case SORT_ORDER_CITY_GROWTH:
				 languageText = lang.getCityGrowthCategory ();
				 break;
				 
			 case SORT_ORDER_CITY_DEATH:
				 languageText = lang.getCityDeathCategory ();
				 break;
				 
			 case SORT_ORDER_SPELLS:
				 languageText = lang.getSpellsCategory ();
				 break;
				 
			 case SORT_ORDER_NODES:
				 languageText = lang.getNodesCategory ();
				 break;
				 
			 case SORT_ORDER_UNITS:
				 languageText = lang.getUnitsCategory ();
				 break;
				 
			 case SORT_ORDER_OFFERS:
				 languageText = lang.getOffersCategory ();
				 break;
				 
			default:
				throw new MomException ("NewTurnMessageSortOrder.getLanguageText doesn't know what to do with enum value " + this);
		 }
		 
		 return languageText;
	 }
}