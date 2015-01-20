package momime.client.scheduledcombatmessages;

/**
 * These constants define the sort order that messages will appear on the scheduled messages scroll.  Each different sort order has a corresponding
 * title for that category, so after sorting, a category title will be slotted it at each point that the sort order number changes.
 */
public enum ScheduledCombatMessageSortOrder
{
	/** Combat that another human player is asking us to do with them now */
	 SORT_ORDER_REQUESTING_US_TO_PLAY (1, "RequestingUsToPlayCategory"),
	
	/** Combats against human opponents who are free (that neither side has requested to play yet) */
	 SORT_ORDER_HUMAN_OPPONENT_FREE (2, "HumanOpponentFreeCategory"),
	
	/** Combats against human opponents who are busy (requesting or playing another combat) */
	 SORT_ORDER_HUMAN_OPPONENT_BUSY (3, "HumanOpponentBusyCategory"),
	
	/** Combats against AI opponents */
	 SORT_ORDER_AI_OPPONENT (4, "AIOpponentCategory"),
	
	/** "Combats" where we've already captured the destination cell so there's no combat anymore - we can just advance in freely */
	 SORT_ORDER_WALK_IN_WITHOUT_A_FIGHT (5, "WalkInWithoutAFightCategory"),
	 
	 /** Combats between other players, that we aren't involved in at all */
	 SORT_ORDER_NOT_INVOLVED (6, "NotInvolvedCategory");
	
	 /** Numeric sort order */
	 private final int sortOrder;
	 
	 /** languageEntryID key for the category heading */
	 private final String languageEntryID;

	 /**
	  * @param aSortOrder Numeric sort order
	  * @param aLanguageEntryID languageEntryID key for the category heading
	  */
	 private ScheduledCombatMessageSortOrder (final int aSortOrder, final String aLanguageEntryID)
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