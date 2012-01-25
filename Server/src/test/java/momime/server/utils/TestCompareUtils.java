package momime.server.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * Tests the StringUtils class
 */
public final class TestCompareUtils
{
	/**
	 * Tests the safeStringCompare method
	 */
	@Test
	public final void testSafeStringCompare ()
	{
		assertTrue (CompareUtils.safeStringCompare (null, null));
		assertTrue (CompareUtils.safeStringCompare ("X", "X"));
		assertFalse (CompareUtils.safeStringCompare (null, "X"));
		assertFalse (CompareUtils.safeStringCompare ("X", null));
		assertFalse (CompareUtils.safeStringCompare ("X", "Y"));
	}

	/**
	 * Tests the safeIntegerCompare method
	 */
	@Test
	public final void testSafeIntegerCompare ()
	{
		assertTrue (CompareUtils.safeIntegerCompare (null, null));
		assertTrue (CompareUtils.safeIntegerCompare (2, 2));
		assertFalse (CompareUtils.safeIntegerCompare (null, 2));
		assertFalse (CompareUtils.safeIntegerCompare (2, null));
		assertFalse (CompareUtils.safeIntegerCompare (2, 3));
	}
}
