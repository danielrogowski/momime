package momime.client.newturnmessages;

import momime.common.messages.v0_9_5.NewTurnMessageBuildingSoldFromLackOfProduction;

/**
 * A building was sold off because we couldn't afford the gold to maintain it
 */
public final class NewTurnMessageBuildingSoldFromLackOfProductionEx extends NewTurnMessageBuildingSoldFromLackOfProduction
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
		return NewTurnMessageSortOrder.SORT_ORDER_LACK_OF_PRODUCTION;
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