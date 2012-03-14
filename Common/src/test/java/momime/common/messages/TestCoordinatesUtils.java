package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import momime.common.messages.v0_9_4.CombatMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

import org.junit.Test;

/**
 * Tests the CoordinatesUtils class
 */
public final class TestCoordinatesUtils
{
	/**
	 * Tests the overlandMapCoordinatesToString method with a null param
	 */
	@Test
	public final void testOverlandMapCoordinatesToString_Null ()
	{
		assertEquals ("(null)", CoordinatesUtils.overlandMapCoordinatesToString (null));
	}

	/**
	 * Tests the overlandMapCoordinatesToString method with a normal param
	 */
	@Test
	public final void testOverlandMapCoordinatesToString_Normal ()
	{
		final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
		coords.setX (56);
		coords.setY (0);
		coords.setPlane (-12);
		assertEquals ("(56, 0, -12)", CoordinatesUtils.overlandMapCoordinatesToString (coords));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with both params null
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_BothNullFalse ()
	{
		assertFalse (CoordinatesUtils.overlandMapCoordinatesEqual (null, null, false));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with both params null
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_BothNullTrue ()
	{
		assertTrue (CoordinatesUtils.overlandMapCoordinatesEqual (null, null, true));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with the first param null
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_FirstNull ()
	{
		final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
		coords.setX (56);
		coords.setY (0);
		coords.setPlane (-12);

		assertFalse (CoordinatesUtils.overlandMapCoordinatesEqual (null, coords, false));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with the second param null
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_SecondNull ()
	{
		final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
		coords.setX (56);
		coords.setY (0);
		coords.setPlane (-12);

		assertFalse (CoordinatesUtils.overlandMapCoordinatesEqual (coords, null, false));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with both objects equal
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_SameObject ()
	{
		final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
		coords.setX (56);
		coords.setY (0);
		coords.setPlane (-12);

		assertTrue (CoordinatesUtils.overlandMapCoordinatesEqual (coords, coords, false));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with two equal objects
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_Equal ()
	{
		final OverlandMapCoordinates firstCoords = new OverlandMapCoordinates ();
		firstCoords.setX (56);
		firstCoords.setY (0);
		firstCoords.setPlane (-12);

		final OverlandMapCoordinates secondCoords = new OverlandMapCoordinates ();
		secondCoords.setX (56);
		secondCoords.setY (0);
		secondCoords.setPlane (-12);

		assertTrue (CoordinatesUtils.overlandMapCoordinatesEqual (firstCoords, secondCoords, false));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with two unequal objects
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_Unequal ()
	{
		final OverlandMapCoordinates firstCoords = new OverlandMapCoordinates ();
		firstCoords.setX (56);
		firstCoords.setY (0);
		firstCoords.setPlane (-12);

		final OverlandMapCoordinates secondCoords = new OverlandMapCoordinates ();
		secondCoords.setX (56);
		secondCoords.setY (10);
		secondCoords.setPlane (-12);

		assertFalse (CoordinatesUtils.overlandMapCoordinatesEqual (firstCoords, secondCoords, false));
	}

	/**
	 * Tests the combatMapCoordinatesToString method with a null param
	 */
	@Test
	public final void testCombatMapCoordinatesToString_Null ()
	{
		assertEquals ("(null)", CoordinatesUtils.combatMapCoordinatesToString (null));
	}

	/**
	 * Tests the combatMapCoordinatesToString method with a normal param
	 */
	@Test
	public final void testCombatMapCoordinatesToString_Normal ()
	{
		final CombatMapCoordinates coords = new CombatMapCoordinates ();
		coords.setX (56);
		coords.setY (0);
		assertEquals ("(56, 0)", CoordinatesUtils.combatMapCoordinatesToString (coords));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with both params null
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_BothNullFalse ()
	{
		assertFalse (CoordinatesUtils.combatMapCoordinatesEqual (null, null, false));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with both params null
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_BothNullTrue ()
	{
		assertTrue (CoordinatesUtils.combatMapCoordinatesEqual (null, null, true));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with the first param null
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_FirstNull ()
	{
		final CombatMapCoordinates coords = new CombatMapCoordinates ();
		coords.setX (56);
		coords.setY (0);

		assertFalse (CoordinatesUtils.combatMapCoordinatesEqual (null, coords, false));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with the second param null
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_SecondNull ()
	{
		final CombatMapCoordinates coords = new CombatMapCoordinates ();
		coords.setX (56);
		coords.setY (0);

		assertFalse (CoordinatesUtils.combatMapCoordinatesEqual (coords, null, false));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with both objects equal
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_SameObject ()
	{
		final CombatMapCoordinates coords = new CombatMapCoordinates ();
		coords.setX (56);
		coords.setY (0);

		assertTrue (CoordinatesUtils.combatMapCoordinatesEqual (coords, coords, false));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with two equal objects
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_Equal ()
	{
		final CombatMapCoordinates firstCoords = new CombatMapCoordinates ();
		firstCoords.setX (56);
		firstCoords.setY (0);

		final CombatMapCoordinates secondCoords = new CombatMapCoordinates ();
		secondCoords.setX (56);
		secondCoords.setY (0);

		assertTrue (CoordinatesUtils.combatMapCoordinatesEqual (firstCoords, secondCoords, false));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with two unequal objects
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_Unequal ()
	{
		final CombatMapCoordinates firstCoords = new CombatMapCoordinates ();
		firstCoords.setX (56);
		firstCoords.setY (0);

		final CombatMapCoordinates secondCoords = new CombatMapCoordinates ();
		secondCoords.setX (56);
		secondCoords.setY (10);

		assertFalse (CoordinatesUtils.combatMapCoordinatesEqual (firstCoords, secondCoords, false));
	}
}
