package momime.server.calculations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_4.FogOfWarValue;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the MomFogOfWarCalculations class
 */
public final class TestMomFogOfWarCalculationsImpl
{
	/**
	 * Tests the canSeeMidTurn method
	 */
	@Test
	public final void testCanSeeMidTurn ()
	{
		final MomFogOfWarCalculationsImpl calc = new MomFogOfWarCalculationsImpl ();
		
		// If we can see it this turn, then we can see it regardless of FOW setting
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertTrue (calc.canSeeMidTurn (FogOfWarStateID.CAN_SEE, setting));

		// If we've never seen it, then we can't see it regardless of FOW setting
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertFalse (calc.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, setting));

		// If we have seen it but cannot see it this turn, whether we can see it now depends on the setting
		assertTrue (calc.canSeeMidTurn (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));
		assertFalse (calc.canSeeMidTurn (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertFalse (calc.canSeeMidTurn (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.FORGET));
	}

	/**
	 * Tests the canSeeMidTurnOnAnyPlaneIfTower method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeMidTurnOnAnyPlaneIfTower () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		final MomFogOfWarCalculationsImpl calc = new MomFogOfWarCalculationsImpl ();
		calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		
		// Test two locations, one with a tower and one without
		final OverlandMapTerrainData towerData = new OverlandMapTerrainData ();
		towerData.setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		map.getPlane ().get (1).getRow ().get (2).getCell ().get (2).setTerrainData (towerData);

		final OverlandMapCoordinatesEx towerOnMyrror = new OverlandMapCoordinatesEx ();
		towerOnMyrror.setX (2);
		towerOnMyrror.setY (2);
		towerOnMyrror.setPlane (1);

		map.getPlane ().get (1).getRow ().get (2).getCell ().get (3).setTerrainData (new OverlandMapTerrainData ());

		final OverlandMapCoordinatesEx otherLocationOnMyrror = new OverlandMapCoordinatesEx ();
		otherLocationOnMyrror.setX (3);
		otherLocationOnMyrror.setY (2);
		otherLocationOnMyrror.setPlane (1);

		// Never seen location on either plane
		assertFalse (calc.canSeeMidTurnOnAnyPlaneIfTower (towerOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));
		assertFalse (calc.canSeeMidTurnOnAnyPlaneIfTower (otherLocationOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));

		// Can see location on opposite plane
		for (int x = 2; x <=3; x++)
			fogOfWarArea.getPlane ().get (0).getRow ().get (2).getCell ().set (x, FogOfWarStateID.HAVE_SEEN);

		assertTrue (calc.canSeeMidTurnOnAnyPlaneIfTower (towerOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));
		assertFalse (calc.canSeeMidTurnOnAnyPlaneIfTower (otherLocationOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));

		// Depends on FOW setting
		assertFalse (calc.canSeeMidTurnOnAnyPlaneIfTower (towerOnMyrror, FogOfWarValue.REMEMBER_AS_LAST_SEEN, map, fogOfWarArea, db));
		assertFalse (calc.canSeeMidTurnOnAnyPlaneIfTower (otherLocationOnMyrror, FogOfWarValue.REMEMBER_AS_LAST_SEEN, map, fogOfWarArea, db));

		// Can see location on this plane
		for (int x = 2; x <=3; x++)
			fogOfWarArea.getPlane ().get (1).getRow ().get (2).getCell ().set (x, FogOfWarStateID.HAVE_SEEN);

		assertTrue (calc.canSeeMidTurnOnAnyPlaneIfTower (towerOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));
		assertTrue (calc.canSeeMidTurnOnAnyPlaneIfTower (otherLocationOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));
	}
}
