package momime.server.fogofwar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import momime.common.database.newgame.v0_9_4.FogOfWarValue;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.server.ServerTestData;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the FogOfWarMidTurnChanges class
 */
public final class TestFogOfWarMidTurnChanges
{
	/**
	 * Tests the canSeeCombatAreaEffectMidTurn method on a global CAE
	 */
	@Test
	public final void testCanSeeCombatAreaEffectMidTurn_Global ()
	{
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		// CAE
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();

		// Can see this regardless of settings or visible area
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertTrue (FogOfWarMidTurnChanges.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, setting));
	}

	/**
	 * Tests the canSeeCombatAreaEffectMidTurn method on a localized CAE
	 */
	@Test
	public final void testCanSeeCombatAreaEffectMidTurn_Localized ()
	{
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		// CAE
		final OverlandMapCoordinates caeLocation = new OverlandMapCoordinates ();
		caeLocation.setX (20);
		caeLocation.setY (10);
		caeLocation.setPlane (1);

		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		cae.setMapLocation (caeLocation);

		// If we've never seen the location, then we can't see the CAE regardless of FOW setting
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertFalse (FogOfWarMidTurnChanges.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, setting));

		// Have seen it previously
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.HAVE_SEEN);
		assertFalse (FogOfWarMidTurnChanges.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, FogOfWarValue.FORGET));
		assertFalse (FogOfWarMidTurnChanges.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertTrue (FogOfWarMidTurnChanges.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));

		// Can see it now, regardless of FOW setting
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertTrue (FogOfWarMidTurnChanges.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, setting));
	}
}
