package momime.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.client.ui.renderer.CitiesListEntry;
import momime.common.messages.OverlandMapCityData;

/**
 * Tests the CitiesListSorter class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCitiesListSorter
{
	/**
	 * Tests the compare method
	 */
	@Test
	public final void testCompare ()
	{
		// Set up some sample cities
		final OverlandMapCityData city1Data = new OverlandMapCityData ();
		city1Data.setCityName ("Capital");
		city1Data.setCityPopulation (1234);
		final CitiesListEntry city1 = new CitiesListEntry (city1Data, null, true, 0, 0, 0);

		final OverlandMapCityData city2Data = new OverlandMapCityData ();
		city2Data.setCityName ("Zerolle");
		city2Data.setCityPopulation (2345);
		final CitiesListEntry city2 = new CitiesListEntry (city2Data, null, false, 0, 0, 0);

		final OverlandMapCityData city3Data = new OverlandMapCityData ();
		city3Data.setCityName ("Weedy");
		city3Data.setCityPopulation (4567);
		final CitiesListEntry city3 = new CitiesListEntry (city3Data, null, false, 0, 0, 0);
		
		final OverlandMapCityData city4Data = new OverlandMapCityData ();
		city4Data.setCityName ("Ymraag");
		city4Data.setCityPopulation (3456);
		final CitiesListEntry city4 = new CitiesListEntry (city4Data, null, false, 0, 0, 0);
		
		final OverlandMapCityData city5Data = new OverlandMapCityData ();
		city5Data.setCityName ("Xylon");
		city5Data.setCityPopulation (3456);
		final CitiesListEntry city5 = new CitiesListEntry (city5Data, null, false, 0, 0, 0);

		final List<CitiesListEntry> cities = new ArrayList<CitiesListEntry> ();
		cities.add (city1);
		cities.add (city2);
		cities.add (city3);
		cities.add (city4);
		cities.add (city5);
		
		// Run method
		Collections.sort (cities, new CitiesListSorter ());
		
		// Check results
		assertEquals (5, cities.size ());
		assertSame (city1, cities.get (0));
		assertSame (city3, cities.get (1));
		assertSame (city5, cities.get (2));
		assertSame (city4, cities.get (3));
		assertSame (city2, cities.get (4));
	}
}