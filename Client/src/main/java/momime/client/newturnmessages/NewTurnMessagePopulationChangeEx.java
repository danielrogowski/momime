package momime.client.newturnmessages;

import momime.common.messages.v0_9_5.NewTurnMessagePopulationChange;

/**
 * NTM about the population of a city either growing or dying over a 1,000 boundary
 */
public final class NewTurnMessagePopulationChangeEx extends NewTurnMessagePopulationChange
	implements NewTurnMessageUI, NewTurnMessageExpiration
{
	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		final NewTurnMessageSortOrder sortOrder;
		if (getNewPopulation () > getOldPopulation ())
			sortOrder = NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH;
		else
			sortOrder = NewTurnMessageSortOrder.SORT_ORDER_CITY_DEATH;
		
		return sortOrder;
	}

	/**
	 * @return Current status of this NTM
	 */
	@Override
	public final NewTurnMessageStatus getStatus ()
	{
		return status;
	}
	
	/**
	 * @param newStatus New status for this NTM
	 */
	@Override
	public final void setStatus (final NewTurnMessageStatus newStatus)
	{
		status = newStatus;
	}
}