package momime.server.worldupdates;

import java.util.Comparator;

/**
 * Comparator for sorting updates so that e.g. all city recalculations are put to the end of the list
 */
final class WorldUpdateComparator implements Comparator<WorldUpdate>
{
	/**
	 * @param o1 First object to compare
	 * @param o2 Second object to compare
	 * @return Value to sort objects into the correct order
	 */
	@Override
	public final int compare (final WorldUpdate o1, final WorldUpdate o2)
	{
		return o1.getKindOfWorldUpdate ().getSortOrder () - o2.getKindOfWorldUpdate ().getSortOrder (); 
	}
}