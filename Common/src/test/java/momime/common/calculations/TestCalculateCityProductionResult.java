package momime.common.calculations;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Tests the CalculateCityProductionResult class
 */
public final class TestCalculateCityProductionResult
{
	/**
	 * Tests the compareTo method
	 */
	@Test
	public final void testCompareTo ()
	{
		// Build up a test list
		final List<CalculateCityProductionResult> values = new ArrayList<CalculateCityProductionResult> ();
		values.add (new CalculateCityProductionResult ("C"));
		values.add (new CalculateCityProductionResult ("A"));
		values.add (new CalculateCityProductionResult ("D"));
		values.add (new CalculateCityProductionResult ("B"));
		
		// Sort it
		Collections.sort (values);
		
		// Check values were sorted correctly
		assertEquals (4, values.size ());
		assertEquals ("A", values.get (0).getProductionTypeID ());
		assertEquals ("B", values.get (1).getProductionTypeID ());
		assertEquals ("C", values.get (2).getProductionTypeID ());
		assertEquals ("D", values.get (3).getProductionTypeID ());
	}
}
