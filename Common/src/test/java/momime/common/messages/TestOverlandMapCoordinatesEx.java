package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

import org.junit.Test;

/**
 * Tests the OverlandMapCoordinatesEx class
 */
public final class TestOverlandMapCoordinatesEx
{
	/**
	 * Tests the overlandMapCoordinatesToString method with a normal param
	 */
	@Test
	public final void testOverlandMapCoordinatesToString ()
	{
		final OverlandMapCoordinatesEx coords = new OverlandMapCoordinatesEx ();
		coords.setX (56);
		coords.setY (0);
		coords.setPlane (-12);
		assertEquals ("(56, 0, -12)", coords.toString ());
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method against a null
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_Null ()
	{
		final OverlandMapCoordinatesEx coords = new OverlandMapCoordinatesEx ();
		coords.setX (56);
		coords.setY (0);
		coords.setPlane (-12);

		assertFalse (coords.equals (null));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with both being same object
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_SameObject ()
	{
		final OverlandMapCoordinatesEx coords = new OverlandMapCoordinatesEx ();
		coords.setX (56);
		coords.setY (0);
		coords.setPlane (-12);

		assertTrue (coords.equals (coords));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with two equal objects
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_Equal ()
	{
		final OverlandMapCoordinatesEx firstCoords = new OverlandMapCoordinatesEx ();
		firstCoords.setX (56);
		firstCoords.setY (0);
		firstCoords.setPlane (-12);

		final OverlandMapCoordinatesEx secondCoords = new OverlandMapCoordinatesEx ();
		secondCoords.setX (56);
		secondCoords.setY (0);
		secondCoords.setPlane (-12);

		assertTrue (firstCoords.equals (secondCoords)); 
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method with two unequal objects
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_Unequal ()
	{
		final OverlandMapCoordinatesEx firstCoords = new OverlandMapCoordinatesEx ();
		firstCoords.setX (56);
		firstCoords.setY (0);
		firstCoords.setPlane (-12);

		final OverlandMapCoordinatesEx secondCoords = new OverlandMapCoordinatesEx ();
		secondCoords.setX (56);
		secondCoords.setY (10);
		secondCoords.setPlane (-12);

		assertFalse (firstCoords.equals (secondCoords));
	}

	/**
	 * Tests the overlandMapCoordinatesEqual method when we forgot to use the Ex class
	 */
	@Test
	public final void testOverlandMapCoordinatesEqual_DidntUseEx ()
	{
		final OverlandMapCoordinatesEx firstCoords = new OverlandMapCoordinatesEx ();
		firstCoords.setX (56);
		firstCoords.setY (10);
		firstCoords.setPlane (-12);

		final OverlandMapCoordinates secondCoords = new OverlandMapCoordinates ();
		secondCoords.setX (56);
		secondCoords.setY (10);
		secondCoords.setPlane (-12);

		assertFalse (firstCoords.equals (secondCoords));
		assertFalse (secondCoords.equals (firstCoords));
	}
}
