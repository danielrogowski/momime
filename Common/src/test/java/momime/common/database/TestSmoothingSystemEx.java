package momime.common.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import momime.common.MomException;

import org.junit.Test;

/**
 * Tests the SmoothingSystemEx class
 */
public final class TestSmoothingSystemEx
{
	/**
	 * Tests the listUnsmoothedBitmasks method on numbers with max value 0 (i.e. fixed)
	 */
	@Test
	public final void testListUnsmoothedBitmasks_MaxValue0 ()
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		ss.setMaxValueEachDirection (0);
		
		// Run method
		final List<String> bitmasks = ss.listUnsmoothedBitmasks (8);
		
		// Check results
		assertEquals (1, bitmasks.size ());
		assertEquals ("00000000", bitmasks.get (0));
	}

	/**
	 * Tests the listUnsmoothedBitmasks method on numbers with max value 1 (i.e. binary)
	 */
	@Test
	public final void testListUnsmoothedBitmasks_MaxValue1 ()
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		ss.setMaxValueEachDirection (1);
		
		// Run method
		final List<String> bitmasks = ss.listUnsmoothedBitmasks (4);
		
		// Check results
		assertEquals (16, bitmasks.size ());
		assertEquals ("0000", bitmasks.get (0));
		assertEquals ("0001", bitmasks.get (1));
		assertEquals ("0010", bitmasks.get (2));
		assertEquals ("0011", bitmasks.get (3));
		assertEquals ("0100", bitmasks.get (4));
		assertEquals ("0101", bitmasks.get (5));
		assertEquals ("0110", bitmasks.get (6));
		assertEquals ("0111", bitmasks.get (7));
		assertEquals ("1000", bitmasks.get (8));
		assertEquals ("1001", bitmasks.get (9));
		assertEquals ("1010", bitmasks.get (10));
		assertEquals ("1011", bitmasks.get (11));
		assertEquals ("1100", bitmasks.get (12));
		assertEquals ("1101", bitmasks.get (13));
		assertEquals ("1110", bitmasks.get (14));
		assertEquals ("1111", bitmasks.get (15));
	}

	/**
	 * Tests the listUnsmoothedBitmasks method on numbers with max value 2
	 */
	@Test
	public final void testListUnsmoothedBitmasks_MaxValue2 ()
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		ss.setMaxValueEachDirection (2);
		
		// Run method
		final List<String> bitmasks = ss.listUnsmoothedBitmasks (2);
		
		// Check results
		assertEquals (9, bitmasks.size ());
		assertEquals ("00", bitmasks.get (0));
		assertEquals ("01", bitmasks.get (1));
		assertEquals ("02", bitmasks.get (2));
		assertEquals ("10", bitmasks.get (3));
		assertEquals ("11", bitmasks.get (4));
		assertEquals ("12", bitmasks.get (5));
		assertEquals ("20", bitmasks.get (6));
		assertEquals ("21", bitmasks.get (7));
		assertEquals ("22", bitmasks.get (8));
	}
	
	/**
	 * Tests the smoothingReductionConditionMatches method
	 * @throws MomException If one input value is null but another is non-null
	 */
	@Test
	public final void testSmoothingReductionConditionMatches () throws MomException
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		
		// Run tests
		assertTrue (ss.smoothingReductionConditionMatches ("10111000", null, null, null));
		
		assertFalse (ss.smoothingReductionConditionMatches ("10111000", "1357", 2, "1"));
		assertTrue (ss.smoothingReductionConditionMatches ("10111000", "1357", 3, "1"));
		assertFalse (ss.smoothingReductionConditionMatches ("10111000", "1357", 4, "1"));

		assertFalse (ss.smoothingReductionConditionMatches ("10201021", "1357", 4, "1"));
		assertTrue (ss.smoothingReductionConditionMatches ("10201021", "1357", 4, "12"));
	}

	/**
	 * Tests the smoothingReductionConditionMatches method with a mixture of null and non-null inputs
	 * @throws MomException If one input value is null but another is non-null
	 */
	@Test(expected=MomException.class)
	public final void testSmoothingReductionConditionMatches_NullMixture () throws MomException
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		
		// Run tests
		ss.smoothingReductionConditionMatches ("10111000", "1357", null, "1");
	}
	
	/**
	 * Tests the applySmoothingReductionReplacement method
	 * @throws MomException If one input value is null but the other is non-null
	 */
	@Test
	public final void testApplySmoothingReductionReplacement () throws MomException
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		
		// Run tests
		assertEquals ("12345678", ss.applySmoothingReductionReplacement ("12345678", null, null));
		
		assertEquals ("12395678", ss.applySmoothingReductionReplacement ("12345678", 4, 9));
		assertEquals ("92345678", ss.applySmoothingReductionReplacement ("12345678", 1, 9));
		assertEquals ("12345679", ss.applySmoothingReductionReplacement ("12345678", 8, 9));
	}

	/**
	 * Tests the applySmoothingReductionReplacement method with a mixture of null and non-null inputs
	 * @throws MomException If one input value is null but the other is non-null
	 */
	@Test(expected=MomException.class)
	public final void testApplySmoothingReductionReplacement_NullMixture () throws MomException
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		
		// Run tests
		ss.applySmoothingReductionReplacement ("12345678", 4, null);
	}
	
	/**
	 * Tests the applySmoothingReductionRules method
	 * @throws MomException If there are invalid rules defined, i.e. with a mixture of null and non-null condition/set rules
	 */
	@Test
	public final void testApplySmoothingReductionRules () throws MomException
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		
		// Do a test with a single rule, 10101010 -> 10201010
		final SmoothingReduction rule1 = new SmoothingReduction ();
		rule1.setDirection1 ("1357");
		rule1.setRepetitions1 (4);
		rule1.setValue1 ("1");
		rule1.setSetDirection1 (3);
		rule1.setSetValue1 (2);
		ss.getSmoothingReduction ().add (rule1);
		assertEquals ("10201010", ss.applySmoothingReductionRules ("10101010"));
		
		// Do with two rules in series, the second which has two conditions and sets two different things, 10201010 -> 10201022
		final SmoothingReduction rule2 = new SmoothingReduction ();
		rule2.setDirection1 ("1357");
		rule2.setRepetitions1 (4);
		rule2.setValue1 ("12");
		rule2.setDirection2 ("2468");
		rule2.setRepetitions2 (4);
		rule2.setValue2 ("0");
		rule2.setSetDirection1 (7);
		rule2.setSetValue1 (2);
		rule2.setSetDirection2 (8);
		rule2.setSetValue2 (2);
		ss.getSmoothingReduction ().add (rule2);
		assertEquals ("10201022", ss.applySmoothingReductionRules ("10101010"));
		
		// Add a third rule where one condition matches but the other doesn't
		final SmoothingReduction rule3 = new SmoothingReduction ();
		rule3.setDirection1 ("1357");
		rule3.setRepetitions1 (4);
		rule3.setValue1 ("12");
		rule3.setDirection2 ("2468");
		rule3.setRepetitions2 (4);
		rule3.setValue2 ("0");
		rule3.setSetDirection1 (1);
		rule3.setSetValue1 (9);
		ss.getSmoothingReduction ().add (rule3);
		assertEquals ("10201022", ss.applySmoothingReductionRules ("10101010"));
	}

	/**
	 * Tests the applySmoothingReductionRules method where one of the rules has a mixture of null and non-null inputs
	 * @throws MomException If there are invalid rules defined, i.e. with a mixture of null and non-null condition/set rules
	 */
	@Test(expected=MomException.class)
	public final void testApplySmoothingReductionRules_NullMixture () throws MomException
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		
		// Do a test with a single rule, 10101010 -> 10201010
		final SmoothingReduction rule1 = new SmoothingReduction ();
		rule1.setDirection1 ("1357");
		rule1.setRepetitions1 (4);
		rule1.setValue1 ("1");
		rule1.setSetDirection1 (3);
		rule1.setSetValue1 (2);
		rule1.setRepetitions2 (4);	// Break it		
		ss.getSmoothingReduction ().add (rule1);
		
		ss.applySmoothingReductionRules ("10101010");
	}
	
	/**
	 * Tests the buildMap method
	 * @throws MomException If there are invalid rules defined, i.e. with a mixture of null and non-null condition/set rules
	 */
	@Test
	public final void testBuildMap () throws MomException
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		ss.setMaxValueEachDirection (1);
		
		// Do cornering rules for something actually realistic, e.g. 10100000 -> 11100000
		// This is the actual SS161 rule set from the graphics XML file
		final SmoothingReduction rule1 = new SmoothingReduction ();
		rule1.setDirection1 ("13");
		rule1.setRepetitions1 (2);
		rule1.setValue1 ("1");
		rule1.setSetDirection1 (2);
		rule1.setSetValue1 (1);
		ss.getSmoothingReduction ().add (rule1);

		final SmoothingReduction rule2 = new SmoothingReduction ();
		rule2.setDirection1 ("35");
		rule2.setRepetitions1 (2);
		rule2.setValue1 ("1");
		rule2.setSetDirection1 (4);
		rule2.setSetValue1 (1);
		ss.getSmoothingReduction ().add (rule2);

		final SmoothingReduction rule3 = new SmoothingReduction ();
		rule3.setDirection1 ("57");
		rule3.setRepetitions1 (2);
		rule3.setValue1 ("1");
		rule3.setSetDirection1 (6);
		rule3.setSetValue1 (1);
		ss.getSmoothingReduction ().add (rule3);

		final SmoothingReduction rule4 = new SmoothingReduction ();
		rule4.setDirection1 ("71");
		rule4.setRepetitions1 (2);
		rule4.setValue1 ("1");
		rule4.setSetDirection1 (8);
		rule4.setSetValue1 (1);
		ss.getSmoothingReduction ().add (rule4);
		
		// Run test
		ss.buildMap (8);
		
		// Should have less than 256 smoothed bitmasks
		assertEquals (161, ss.getBitmasksMap ().size ());
		
		// And exactly 256 unsmoothed bitmasks
		int count = 0;
		for (final List<String> unsmoothedList : ss.getBitmasksMap ().values ())
			count = count + unsmoothedList.size ();
		
		assertEquals (256, count);
	}

	/**
	 * Tests the buildMap method
	 * @throws MomException If there are invalid rules defined, i.e. with a mixture of null and non-null condition/set rules
	 */
	@Test(expected=MomException.class)
	public final void testBuildMap_NullMixture () throws MomException
	{
		// Set up object to test
		final SmoothingSystemEx ss = new SmoothingSystemEx ();
		ss.setMaxValueEachDirection (1);
		
		// Do a test with a single rule, 10101010 -> 10201010
		final SmoothingReduction rule1 = new SmoothingReduction ();
		rule1.setDirection1 ("1357");
		rule1.setRepetitions1 (4);
		rule1.setValue1 ("1");
		rule1.setSetDirection1 (3);
		rule1.setSetValue1 (2);
		rule1.setRepetitions2 (4);	// Break it		
		ss.getSmoothingReduction ().add (rule1);
		
		// Run test
		ss.buildMap (8);
	}
}