package momime.client.newturnmessages;

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
	/** NTMs about one of our cities completing construction of a a unit or building */
	 SORT_ORDER_CONSTRUCTION_COMPLETED (1, "ConstructionCompletedCategory"),
	
	/** NTMs about us losing units/buildings/spells because we couldn't afford the maintenance to pay for them */
	 SORT_ORDER_LACK_OF_PRODUCTION (2, "LackOfProductionCategory"),
	
	/** NTMs about cities growing across a 1,000 population border */
	 SORT_ORDER_CITY_GROWTH (3, "CityGrowthCategory"),
	
	/** NTMs about cities dying across a 1,000 population border */
	 SORT_ORDER_CITY_DEATH (4, "CityDeathCategory"),
	
	/** NTMs about researching or requiring targetting our spells), or anybody casting an overland enchantment */
	 SORT_ORDER_SPELLS (5, "SpellsCategory"),
	
	/** NTMs about us capturing or losing nodes */
	 SORT_ORDER_NODES (6, "NodesCategory");
	 
	 /** Numeric sort order */
	 private final int sortOrder;
	 
	 /** languageEntryID key for the category heading */
	 private final String languageEntryID;

	 /**
	  * @param aSortOrder Numeric sort order
	  * @param aLanguageEntryID languageEntryID key for the category heading
	  */
	 private NewTurnMessageSortOrder (final int aSortOrder, final String aLanguageEntryID)
	 {
		 sortOrder = aSortOrder;
		 languageEntryID = aLanguageEntryID;
	 }

	 /**
	  * @return Numeric sort order
	  */
	 public final int getSortOrder ()
	 {
		 return sortOrder;
	 }
	 
	 /**
	  * @return languageEntryID key for the category heading
	  */
	 public final String getLanguageEntryID ()
	 {
		 return languageEntryID;
	 }
}