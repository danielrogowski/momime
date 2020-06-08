package momime.server.ai;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Tests the AIConstructableUnit class
 */
public final class TestAIConstructableUnit
{
	/**
	 * Tests the compare method by using it to sort a real list of units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCompare () throws Exception
	{
		// One of each kind of test unit
		final AIConstructableUnit first = new AIConstructableUnit (null, null, null, 20, null, false);
		final AIConstructableUnit second = new AIConstructableUnit (null, null, null, 10, null, false);
		final AIConstructableUnit third = new AIConstructableUnit (null, null, null, 8, null, false);
		final AIConstructableUnit fourth = new AIConstructableUnit (null, null, null, 4, null, false);

		// Put units into the list in the wrong order
		final List<AIConstructableUnit> units = new ArrayList<AIConstructableUnit> ();
		units.add (third);
		units.add (second);
		units.add (fourth);
		units.add (first);
		
		// Check results
		Collections.sort (units);
		assertSame (first, units.get (0));
		assertSame (second, units.get (1));
		assertSame (third, units.get (2));
		assertSame (fourth, units.get (3));
	}
}