package momime.client.newturnmessages;

import java.util.Comparator;

/**
 * Sorts new turn messages according to the numeric sort order of their category
 */
final class NewTurnMessageSorter implements Comparator<NewTurnMessageUI>
{
	/**
	 * @return Int value such that lower numbered sort orders are sorted to the top of the list
	 */
	@Override
	public final int compare (final NewTurnMessageUI o1, final NewTurnMessageUI o2)
	{
		return o1.getSortOrder ().getSortOrder () - o2.getSortOrder ().getSortOrder ();
	}
}