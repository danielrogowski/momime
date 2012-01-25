package momime.common.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
	 * Tests the findCombatAreaEffect method with a null map location
	 */
	@Test
	public final void testFindCombatAreaEffect_NullLocation ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		// Add one that matches aside from having a map location
		final MemoryCombatAreaEffect newCaeWithLocation = new MemoryCombatAreaEffect ();
		newCaeWithLocation.setCastingPlayerID (2);
		newCaeWithLocation.setCombatAreaEffectID ("CAE02");
		newCaeWithLocation.setMapLocation (new OverlandMapCoordinates ());
		CAEs.add (newCaeWithLocation);

		assertFalse (MemoryCombatAreaEffectUtils.findCombatAreaEffect (CAEs, null, "CAE02", 2, debugLogger));

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
	 * Tests the findCombatAreaEffect method with a valid map location
	 */
	@Test
	public final void testFindCombatAreaEffect_WithLocation ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		final OverlandMapCoordinates desiredLocation = new OverlandMapCoordinates ();
		desiredLocation.setX (12);
		desiredLocation.setY (22);
		desiredLocation.setPlane (32);

		// Add one that matches aside from not having a map location
		final MemoryCombatAreaEffect newCaeWithLocation = new MemoryCombatAreaEffect ();
		newCaeWithLocation.setCastingPlayerID (2);
		newCaeWithLocation.setCombatAreaEffectID ("CAE02");
		CAEs.add (newCaeWithLocation);

		assertFalse (MemoryCombatAreaEffectUtils.findCombatAreaEffect (CAEs, desiredLocation, "CAE02", 2, debugLogger));

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

		assertTrue (MemoryCombatAreaEffectUtils.findCombatAreaEffect (CAEs, desiredLocation, "CAE02", 2, debugLogger));
	}
}
