package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import momime.common.messages.v0_9_4.CombatMapCoordinates;

import org.junit.Test;

/**
 * Tests the CombatMapCoordinatesEx class
 */
public final class TestCombatMapCoordinatesEx
{
	/**
	 * Tests the combatMapCoordinatesToString method with a normal param
	 */
	@Test
	public final void testCombatMapCoordinatesToString ()
	{
		final CombatMapCoordinatesEx coords = new CombatMapCoordinatesEx ();
		coords.setX (56);
		coords.setY (0);
		assertEquals ("(56, 0)", coords.toString ());
	}

	/**
	 * Tests the combatMapCoordinatesEqual method against a null
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_Null ()
	{
		final CombatMapCoordinatesEx coords = new CombatMapCoordinatesEx ();
		coords.setX (56);
		coords.setY (0);

		assertFalse (coords.equals (null));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with both being same object
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_SameObject ()
	{
		final CombatMapCoordinatesEx coords = new CombatMapCoordinatesEx ();
		coords.setX (56);
		coords.setY (0);

		assertTrue (coords.equals (coords));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with two equal objects
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_Equal ()
	{
		final CombatMapCoordinatesEx firstCoords = new CombatMapCoordinatesEx ();
		firstCoords.setX (56);
		firstCoords.setY (0);

		final CombatMapCoordinatesEx secondCoords = new CombatMapCoordinatesEx ();
		secondCoords.setX (56);
		secondCoords.setY (0);

		assertTrue (firstCoords.equals (secondCoords)); 
	}

	/**
	 * Tests the combatMapCoordinatesEqual method with two unequal objects
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_Unequal ()
	{
		final CombatMapCoordinatesEx firstCoords = new CombatMapCoordinatesEx ();
		firstCoords.setX (56);
		firstCoords.setY (0);

		final CombatMapCoordinatesEx secondCoords = new CombatMapCoordinatesEx ();
		secondCoords.setX (56);
		secondCoords.setY (10);

		assertFalse (firstCoords.equals (secondCoords));
	}

	/**
	 * Tests the combatMapCoordinatesEqual method when we forgot to use the Ex class
	 */
	@Test
	public final void testCombatMapCoordinatesEqual_DidntUseEx ()
	{
		final CombatMapCoordinatesEx firstCoords = new CombatMapCoordinatesEx ();
		firstCoords.setX (56);
		firstCoords.setY (10);

		final CombatMapCoordinates secondCoords = new CombatMapCoordinates ();
		secondCoords.setX (56);
		secondCoords.setY (10);

		assertFalse (firstCoords.equals (secondCoords));
		assertFalse (secondCoords.equals (firstCoords));
	}
}
