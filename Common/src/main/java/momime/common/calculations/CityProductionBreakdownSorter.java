package momime.common.calculations;

import java.util.Comparator;

import momime.common.internal.CityProductionBreakdown;

/**
 * Allows lists of production types to be sorted
 */
final class CityProductionBreakdownSorter implements Comparator<CityProductionBreakdown>
{
	/**
	 * @param o1 First production value to compare
	 * @param o2 Second production value to compare
	 * @return Value such that list will be sorted in productionTypeID order
	 */
	@Override
	public final int compare (final CityProductionBreakdown o1, final CityProductionBreakdown o2)
	{
		return o1.getProductionTypeID ().compareTo (o2.getProductionTypeID ());
	}
}