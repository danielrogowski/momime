package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
	public final void testOverlandMapCoordinatesEqual_BothNull ()
	{
		assertFalse (CoordinatesUtils.overlandMapCoordinatesEqual (null, null));
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

		assertFalse (CoordinatesUtils.overlandMapCoordinatesEqual (null, coords));
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

		assertFalse (CoordinatesUtils.overlandMapCoordinatesEqual (coords, null));
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

		assertTrue (CoordinatesUtils.overlandMapCoordinatesEqual (coords, coords));
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

		assertTrue (CoordinatesUtils.overlandMapCoordinatesEqual (firstCoords, secondCoords));
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

		assertFalse (CoordinatesUtils.overlandMapCoordinatesEqual (firstCoords, secondCoords));
	}
}
