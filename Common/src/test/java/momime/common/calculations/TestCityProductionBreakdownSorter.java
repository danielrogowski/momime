package momime.common.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import momime.common.internal.CityProductionBreakdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the CityProductionBreakdownSorter class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCityProductionBreakdownSorter
{
	/**
	 * Tests the compare method
	 */
	@Test
	public final void testCompare ()
	{
		// Build up a test list
		final List<CityProductionBreakdown> values = new ArrayList<CityProductionBreakdown> ();
		for (final String s : new String [] {"C", "A", "D", "B"})
		{
			final CityProductionBreakdown value = new CityProductionBreakdown ();
			value.setProductionTypeID (s);
			values.add (value);
		}
		
		// Sort it
		Collections.sort (values, new CityProductionBreakdownSorter ());
		
		// Check values were sorted correctly
		assertEquals (4, values.size ());
		assertEquals ("A", values.get (0).getProductionTypeID ());
		assertEquals ("B", values.get (1).getProductionTypeID ());
		assertEquals ("C", values.get (2).getProductionTypeID ());
		assertEquals ("D", values.get (3).getProductionTypeID ());
	}
}