package momime.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

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
		final MapCoordinates3DEx firstCoords = new MapCoordinates3DEx ();
		firstCoords.setX (56);
		firstCoords.setY (0);
		firstCoords.setZ (-12);

		final MapCoordinates3DEx secondCoords = new MapCoordinates3DEx ();
		secondCoords.setX (56);
		secondCoords.setY (0);
		secondCoords.setZ (-12);
		
		final MapCoordinates3DEx thirdCoords = new MapCoordinates3DEx ();
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
		final MapCoordinates2DEx firstCoords = new MapCoordinates2DEx ();
		firstCoords.setX (56);
		firstCoords.setY (0);

		final MapCoordinates2DEx secondCoords = new MapCoordinates2DEx ();
		secondCoords.setX (56);
		secondCoords.setY (0);
		
		final MapCoordinates2DEx thirdCoords = new MapCoordinates2DEx ();
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
