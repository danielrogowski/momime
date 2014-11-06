package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryCombatAreaEffect;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Tests the MemoryCombatAreaEffectUtils class
 */
public final class TestMemoryCombatAreaEffectUtilsImpl
{
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
			newCAE.setCombatAreaEffectURN (n);
			newCAE.setCastingPlayerID (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffect (CAEs, null, "CAE02", 2).getCombatAreaEffectURN ());
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
			newCAE.setCombatAreaEffectURN (n);
			newCAE.setCastingPlayerID (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);

			newCAE.setMapLocation (new MapCoordinates3DEx (10 + n, 20 + n, 30 + n));
			CAEs.add (newCAE);
		}

		final MapCoordinates3DEx desiredLocation = new MapCoordinates3DEx (12, 22, 32);
		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffect (CAEs, desiredLocation, "CAE02", 2).getCombatAreaEffectURN ());
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
			newCAE.setCombatAreaEffectURN (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffect (CAEs, null, "CAE02", null).getCombatAreaEffectURN ());
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
			newCAE.setCombatAreaEffectURN (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			newCAE.setMapLocation (new MapCoordinates3DEx (10 + n, 20 + n, 30 + n));
			CAEs.add (newCAE);
		}

		final MapCoordinates3DEx desiredLocation = new MapCoordinates3DEx (12, 22, 32);
		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffect (CAEs, desiredLocation, "CAE02", null).getCombatAreaEffectURN ());
	}

	/**
	 * Tests the findCombatAreaEffectURN method on a combatAreaEffect that does exist
	 * @throws RecordNotFoundException If combatAreaEffect with requested URN is not found
	 */
	@Test
	public final void testFindCombatAreaEffectURN_Exists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectURN (n);
			combatAreaEffects.add (combatAreaEffect);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffectURN (2, combatAreaEffects).getCombatAreaEffectURN ());
		assertEquals (2, utils.findCombatAreaEffectURN (2, combatAreaEffects, "testFindCombatAreaEffectURN_Exists").getCombatAreaEffectURN ());
	}

	/**
	 * Tests the findCombatAreaEffectURN method on a combatAreaEffect that doesn't exist
	 * @throws RecordNotFoundException If combatAreaEffect with requested URN is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatAreaEffectURN_NotExists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectURN (n);
			combatAreaEffects.add (combatAreaEffect);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertNull (utils.findCombatAreaEffectURN (4, combatAreaEffects));
		utils.findCombatAreaEffectURN (4, combatAreaEffects, "testFindCombatAreaEffectURN_NotExists");
	}

	/**
	 * Tests the removeCombatAreaEffectURN method on a combatAreaEffect that does exist
	 * @throws RecordNotFoundException If combatAreaEffect with requested URN is not found
	 */
	@Test
	public final void testRemoveCombatAreaEffectURN_Exists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectURN (n);
			combatAreaEffects.add (combatAreaEffect);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		utils.removeCombatAreaEffectURN (2, combatAreaEffects);
		assertEquals (2, combatAreaEffects.size ());
		assertEquals (1, combatAreaEffects.get (0).getCombatAreaEffectURN ());
		assertEquals (3, combatAreaEffects.get (1).getCombatAreaEffectURN ());
	}

	/**
	 * Tests the removeCombatAreaEffectURN method on a combatAreaEffect that doesn't exist
	 * @throws RecordNotFoundException If combatAreaEffect with requested URN is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testRemoveCombatAreaEffectURN_NotExists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectURN (n);
			combatAreaEffects.add (combatAreaEffect);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		utils.removeCombatAreaEffectURN (4, combatAreaEffects);
	}
}