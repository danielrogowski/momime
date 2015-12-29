package momime.client.utils;

import java.util.Comparator;

import momime.client.ui.renderer.CitiesListEntry;

/**
 * Sorts cities in the order they should appear on the cities list.
 * 1) Our capital always appears at the top
 * 2) Then sorted by population
 * 3) Then sorted by name
 */
public final class CitiesListSorter implements Comparator<CitiesListEntry>
{
	/**
	 * @param c1 First city to compare
	 * @param c2 Second city to compare
	 * @return Value suitable to sort cities into order
	 */
	@Override
	public final int compare (final CitiesListEntry c1, final CitiesListEntry c2)
	{
		final int result;
		
		// Capital always appears at the top
		if ((c1.isCapital ()) && (!c2.isCapital ()))
			result = -1;
		else if ((c2.isCapital ()) && (!c1.isCapital ()))
			result = 1;
		
		// Sort by population
		else if (c1.getCityPopulation () != c2.getCityPopulation ())
			result = c2.getCityPopulation () - c1.getCityPopulation ();
		
		// Sort by city name
		else
			result = c1.getCityName ().compareTo (c2.getCityName ());
		
		return result;
	}
}