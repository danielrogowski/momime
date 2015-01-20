package momime.client.scheduledcombatmessages;

import java.util.Comparator;

/**
 * Sorts new turn messages according to the numeric sort order of their category
 */
final class ScheduledCombatMessageSorter implements Comparator<ScheduledCombatMessageUI>
{
	/**
	 * @return Int value such that lower numbered sort orders are sorted to the top of the list
	 */
	@Override
	public final int compare (final ScheduledCombatMessageUI o1, final ScheduledCombatMessageUI o2)
	{
		return o1.getSortOrder ().getSortOrder () - o2.getSortOrder ().getSortOrder ();
	}
}