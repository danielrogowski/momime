package momime.client.newturnmessages;

import momime.common.messages.v0_9_5.NewTurnMessageSummonUnit;

/**
 * NTM describing a unit that was added from a summoning spell
 */
public final class NewTurnMessageSummonUnitEx extends NewTurnMessageSummonUnit
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
		return NewTurnMessageSortOrder.SORT_ORDER_SPELLS;
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