package momime.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;

import org.junit.Test;

/**
 * Tests the CompareUtils class
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
	
	/**
	 * Tests the safeOverlandMapCoordinatesCompare method
	 */
	@Test
	public final void testSafeOverlandMapCoordinatesCompare ()
	{
		final OverlandMapCoordinatesEx firstCoords = new OverlandMapCoordinatesEx ();
		firstCoords.setX (56);
		firstCoords.setY (0);
		firstCoords.setZ (-12);

		final OverlandMapCoordinatesEx secondCoords = new OverlandMapCoordinatesEx ();
		secondCoords.setX (56);
		secondCoords.setY (0);
		secondCoords.setZ (-12);
		
		final OverlandMapCoordinatesEx thirdCoords = new OverlandMapCoordinatesEx ();
		thirdCoords.setX (56);
		thirdCoords.setY (10);
		thirdCoords.setZ (-12);
		
		assertTrue (CompareUtils.safeOverlandMapCoordinatesCompare (null, null));
		assertTrue (CompareUtils.safeOverlandMapCoordinatesCompare (firstCoords, firstCoords));
		assertTrue (CompareUtils.safeOverlandMapCoordinatesCompare (firstCoords, secondCoords));
		assertFalse (CompareUtils.safeOverlandMapCoordinatesCompare (null, firstCoords));
		assertFalse (CompareUtils.safeOverlandMapCoordinatesCompare (firstCoords, null));
		assertFalse (CompareUtils.safeOverlandMapCoordinatesCompare (firstCoords, thirdCoords));
	}

	/**
	 * Tests the safeCombatMapCoordinatesCompare method
	 */
	@Test
	public final void testSafeCombatMapCoordinatesCompare ()
	{
		final CombatMapCoordinatesEx firstCoords = new CombatMapCoordinatesEx ();
		firstCoords.setX (56);
		firstCoords.setY (0);

		final CombatMapCoordinatesEx secondCoords = new CombatMapCoordinatesEx ();
		secondCoords.setX (56);
		secondCoords.setY (0);
		
		final CombatMapCoordinatesEx thirdCoords = new CombatMapCoordinatesEx ();
		thirdCoords.setX (56);
		thirdCoords.setY (10);
		
		assertTrue (CompareUtils.safeCombatMapCoordinatesCompare (null, null));
		assertTrue (CompareUtils.safeCombatMapCoordinatesCompare (firstCoords, firstCoords));
		assertTrue (CompareUtils.safeCombatMapCoordinatesCompare (firstCoords, secondCoords));
		assertFalse (CompareUtils.safeCombatMapCoordinatesCompare (null, firstCoords));
		assertFalse (CompareUtils.safeCombatMapCoordinatesCompare (firstCoords, null));
		assertFalse (CompareUtils.safeCombatMapCoordinatesCompare (firstCoords, thirdCoords));
	}
}
