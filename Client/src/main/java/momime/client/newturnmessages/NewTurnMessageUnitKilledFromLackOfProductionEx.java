package momime.client.newturnmessages;

import momime.common.messages.v0_9_5.NewTurnMessageUnitKilledFromLackOfProduction;

/**
 * A unit was killed off because we couldn't afford the rations, gold and/or mana to pay for it
 */
public final class NewTurnMessageUnitKilledFromLackOfProductionEx extends NewTurnMessageUnitKilledFromLackOfProduction
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