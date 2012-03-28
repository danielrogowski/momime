package momime.common.messages;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

import org.junit.Test;

/**
 * Tests the MemoryCombatAreaEffectUtils class
 */
public final class TestMemoryCombatAreaEffectUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

	/**
	 * Tests the findCombatAreaEffect method with a player specific, but a null map location (i.e. an overland enchantment spell)
	 */
	@Test
	public final void testFindCombatAreaEffect_PlayerButNullLocation ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCastingPlayerID (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		assertTrue (MemoryCombatAreaEffectUtils.findCombatAreaEffect (CAEs, null, "CAE02", 2, debugLogger));
	}

	/**
	 * Tests the findCombatAreaEffect method with a specified player and map location (i.e. a localized combat spell, like prayer)
	 */
	@Test
	public final void testFindCombatAreaEffect_WithPlayerAndLocation ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCastingPlayerID (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);

			final OverlandMapCoordinates thisLocation = new OverlandMapCoordinates ();
			thisLocation.setX (10 + n);
			thisLocation.setY (20 + n);
			thisLocation.setPlane (30 + n);

			newCAE.setMapLocation (thisLocation);
			CAEs.add (newCAE);
		}

		final OverlandMapCoordinates desiredLocation = new OverlandMapCoordinates ();
		desiredLocation.setX (12);
		desiredLocation.setY (22);
		desiredLocation.setPlane (32);

		assertTrue (MemoryCombatAreaEffectUtils.findCombatAreaEffect (CAEs, desiredLocation, "CAE02", 2, debugLogger));
	}

	/**
	 * Tests the findCombatAreaEffect method with no player and no map location (don't think this actually applies to anything in game)
	 */
	@Test
	public final void testFindCombatAreaEffect_NullPlayerAndLocation ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		assertTrue (MemoryCombatAreaEffectUtils.findCombatAreaEffect (CAEs, null, "CAE02", null, debugLogger));
	}

	/**
	 * Tests the findCombatAreaEffect method with a valid map location
	 */
	@Test
	public final void testFindCombatAreaEffect_LocationButNullPlayer ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectID ("CAE0" + n);

			final OverlandMapCoordinates thisLocation = new OverlandMapCoordinates ();
			thisLocation.setX (10 + n);
			thisLocation.setY (20 + n);
			thisLocation.setPlane (30 + n);

			newCAE.setMapLocation (thisLocation);
			CAEs.add (newCAE);
		}

		final OverlandMapCoordinates desiredLocation = new OverlandMapCoordinates ();
		desiredLocation.setX (12);
		desiredLocation.setY (22);
		desiredLocation.setPlane (32);

		assertTrue (MemoryCombatAreaEffectUtils.findCombatAreaEffect (CAEs, desiredLocation, "CAE02", null, debugLogger));
	}

	/**
	 * Tests the cancelCombatAreaEffect method with a player specific, but a null map location (i.e. an overland enchantment spell)
	 * @throws RecordNotFoundException If the CAE doesn't exist
	 */
	@Test
	public final void testCancelCombatAreaEffect_PlayerButNullLocation () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCastingPlayerID (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		MemoryCombatAreaEffectUtils.cancelCombatAreaEffect (CAEs, null, "CAE02", 2, debugLogger);
		assertEquals (2, CAEs.size ());
		assertEquals ("CAE01", CAEs.get (0).getCombatAreaEffectID ());
		assertEquals ("CAE03", CAEs.get (1).getCombatAreaEffectID ());
	}

	/**
	 * Tests the cancelCombatAreaEffect method with a specified player and map location (i.e. a localized combat spell, like prayer)
	 * @throws RecordNotFoundException If the CAE doesn't exist
	 */
	@Test
	public final void testCancelCombatAreaEffect_WithPlayerAndLocation () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCastingPlayerID (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);

			final OverlandMapCoordinates thisLocation = new OverlandMapCoordinates ();
			thisLocation.setX (10 + n);
			thisLocation.setY (20 + n);
			thisLocation.setPlane (30 + n);

			newCAE.setMapLocation (thisLocation);
			CAEs.add (newCAE);
		}

		final OverlandMapCoordinates desiredLocation = new OverlandMapCoordinates ();
		desiredLocation.setX (12);
		desiredLocation.setY (22);
		desiredLocation.setPlane (32);

		MemoryCombatAreaEffectUtils.cancelCombatAreaEffect (CAEs, desiredLocation, "CAE02", 2, debugLogger);
		assertEquals (2, CAEs.size ());
		assertEquals ("CAE01", CAEs.get (0).getCombatAreaEffectID ());
		assertEquals ("CAE03", CAEs.get (1).getCombatAreaEffectID ());
	}

	/**
	 * Tests the cancelCombatAreaEffect method with no player and no map location (don't think this actually applies to anything in game)
	 * @throws RecordNotFoundException If the CAE doesn't exist
	 */
	@Test
	public final void testCancelCombatAreaEffect_NullPlayerAndLocation () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		MemoryCombatAreaEffectUtils.cancelCombatAreaEffect (CAEs, null, "CAE02", null, debugLogger);
		assertEquals (2, CAEs.size ());
		assertEquals ("CAE01", CAEs.get (0).getCombatAreaEffectID ());
		assertEquals ("CAE03", CAEs.get (1).getCombatAreaEffectID ());
	}

	/**
	 * Tests the cancelCombatAreaEffect method with a valid map location
	 * @throws RecordNotFoundException If the CAE doesn't exist
	 */
	@Test
	public final void testCancelCombatAreaEffect_LocationButNullPlayer () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectID ("CAE0" + n);

			final OverlandMapCoordinates thisLocation = new OverlandMapCoordinates ();
			thisLocation.setX (10 + n);
			thisLocation.setY (20 + n);
			thisLocation.setPlane (30 + n);

			newCAE.setMapLocation (thisLocation);
			CAEs.add (newCAE);
		}

		final OverlandMapCoordinates desiredLocation = new OverlandMapCoordinates ();
		desiredLocation.setX (12);
		desiredLocation.setY (22);
		desiredLocation.setPlane (32);

		MemoryCombatAreaEffectUtils.cancelCombatAreaEffect (CAEs, desiredLocation, "CAE02", null, debugLogger);
		assertEquals (2, CAEs.size ());
		assertEquals ("CAE01", CAEs.get (0).getCombatAreaEffectID ());
		assertEquals ("CAE03", CAEs.get (1).getCombatAreaEffectID ());
	}

	/**
	 * Tests the cancelCombatAreaEffect method with a CAE that doesn't exist
	 * @throws RecordNotFoundException If the CAE doesn't exist
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testCancelCombatAreaEffect_NotExists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		MemoryCombatAreaEffectUtils.cancelCombatAreaEffect (CAEs, null, "CAE04", null, debugLogger);
	}
}
