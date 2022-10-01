package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.messages.NumberedHeroItem;

/**
 * Tests the CompareUtils class
 */
@ExtendWith(MockitoExtension.class)
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
	 * Tests the safeBooleanCompare method
	 */
	@Test
	public final void testSafeBooleanCompare ()
	{
		assertTrue (CompareUtils.safeBooleanCompare (null, null));
		assertTrue (CompareUtils.safeBooleanCompare (true, true));
		assertFalse (CompareUtils.safeBooleanCompare (null, true));
		assertFalse (CompareUtils.safeBooleanCompare (false, null));
		assertFalse (CompareUtils.safeBooleanCompare (false, true));
	}
	
	/**
	 * Tests the safeOverlandMapCoordinatesCompare method
	 */
	@Test
	public final void testSafeOverlandMapCoordinatesCompare ()
	{
		final MapCoordinates3DEx firstCoords = new MapCoordinates3DEx (56, 0, -12);
		final MapCoordinates3DEx secondCoords = new MapCoordinates3DEx (56, 0, -12);
		final MapCoordinates3DEx thirdCoords = new MapCoordinates3DEx (56, 10, -12);
		
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
		final MapCoordinates2DEx firstCoords = new MapCoordinates2DEx (56, 0);
		final MapCoordinates2DEx secondCoords = new MapCoordinates2DEx (56, 0);
		final MapCoordinates2DEx thirdCoords = new MapCoordinates2DEx (56, 10);
		
		assertTrue (CompareUtils.safeCombatMapCoordinatesCompare (null, null));
		assertTrue (CompareUtils.safeCombatMapCoordinatesCompare (firstCoords, firstCoords));
		assertTrue (CompareUtils.safeCombatMapCoordinatesCompare (firstCoords, secondCoords));
		assertFalse (CompareUtils.safeCombatMapCoordinatesCompare (null, firstCoords));
		assertFalse (CompareUtils.safeCombatMapCoordinatesCompare (firstCoords, null));
		assertFalse (CompareUtils.safeCombatMapCoordinatesCompare (firstCoords, thirdCoords));
	}
	
	/**
	 * Tests the safeNumberedHeroItemCompare method
	 */
	@Test
	public final void testSafeNumberedHeroItemCompare ()
	{
		final NumberedHeroItem firstItem = new NumberedHeroItem ();
		firstItem.setHeroItemURN (1);
		
		final NumberedHeroItem secondItem = new NumberedHeroItem ();
		secondItem.setHeroItemURN (1);
		
		final NumberedHeroItem thirdItem = new NumberedHeroItem ();
		thirdItem.setHeroItemURN (2);
		
		assertTrue (CompareUtils.safeNumberedHeroItemCompare (null, null));
		assertTrue (CompareUtils.safeNumberedHeroItemCompare (firstItem, firstItem));
		assertTrue (CompareUtils.safeNumberedHeroItemCompare (firstItem, secondItem));
		assertFalse (CompareUtils.safeNumberedHeroItemCompare (null, firstItem));
		assertFalse (CompareUtils.safeNumberedHeroItemCompare (firstItem, null));
		assertFalse (CompareUtils.safeNumberedHeroItemCompare (firstItem, thirdItem));
	}
}
