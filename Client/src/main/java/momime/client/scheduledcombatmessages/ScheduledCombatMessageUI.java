package momime.client.scheduledcombatmessages;

/**
 * Defines methods that scheduled combats must provide in order to be able to be sorted into categories.  Combats won't implement
 * this interface directly however, they'll implement ScheduledCombatMessageSimpleUI 
 * which define how the message is drawn onto the scheduled combats scroll.
 */
public interface ScheduledCombatMessageUI
{
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	public ScheduledCombatMessageSortOrder getSortOrder ();
}