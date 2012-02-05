package momime.server.calculations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_4.FogOfWarValue;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the MomFogOfWarCalculations class
 */
public final class TestMomFogOfWarCalculations
{
	/**
	 * Tests the canSeeMidTurn method
	 */
	@Test
	public final void testCanSeeMidTurn ()
	{
		// If we can see it this turn, then we can see it regardless of FOW setting
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertTrue (MomFogOfWarCalculations.canSeeMidTurn (FogOfWarStateID.CAN_SEE, setting));

		// If we've never seen it, then we can't see it regardless of FOW setting
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertFalse (MomFogOfWarCalculations.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, setting));

		// If we have seen it but cannot see it this turn, whether we can see it now depends on the setting
		assertTrue (MomFogOfWarCalculations.canSeeMidTurn (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));
		assertFalse (MomFogOfWarCalculations.canSeeMidTurn (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertFalse (MomFogOfWarCalculations.canSeeMidTurn (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.FORGET));
	}

	/**
	 * Tests the canSeeMidTurnOnAnyPlaneIfTower method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testCanSeeMidTurnOnAnyPlaneIfTower () throws IOException, JAXBException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		// Test two locations, one with a tower and one without
		final OverlandMapTerrainData towerData = new OverlandMapTerrainData ();
		towerData.setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		map.getPlane ().get (1).getRow ().get (2).getCell ().get (2).setTerrainData (towerData);

		final OverlandMapCoordinates towerOnMyrror = new OverlandMapCoordinates ();
		towerOnMyrror.setX (2);
		towerOnMyrror.setY (2);
		towerOnMyrror.setPlane (1);

		map.getPlane ().get (1).getRow ().get (2).getCell ().get (3).setTerrainData (new OverlandMapTerrainData ());

		final OverlandMapCoordinates otherLocationOnMyrror = new OverlandMapCoordinates ();
		otherLocationOnMyrror.setX (3);
		otherLocationOnMyrror.setY (2);
		otherLocationOnMyrror.setPlane (1);

		// Never seen location on either plane
		assertFalse (MomFogOfWarCalculations.canSeeMidTurnOnAnyPlaneIfTower (towerOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));
		assertFalse (MomFogOfWarCalculations.canSeeMidTurnOnAnyPlaneIfTower (otherLocationOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));

		// Can see location on opposite plane
		for (int x = 2; x <=3; x++)
			fogOfWarArea.getPlane ().get (0).getRow ().get (2).getCell ().set (x, FogOfWarStateID.HAVE_SEEN);

		assertTrue (MomFogOfWarCalculations.canSeeMidTurnOnAnyPlaneIfTower (towerOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));
		assertFalse (MomFogOfWarCalculations.canSeeMidTurnOnAnyPlaneIfTower (otherLocationOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));

		// Depends on FOW setting
		assertFalse (MomFogOfWarCalculations.canSeeMidTurnOnAnyPlaneIfTower (towerOnMyrror, FogOfWarValue.REMEMBER_AS_LAST_SEEN, map, fogOfWarArea, db));
		assertFalse (MomFogOfWarCalculations.canSeeMidTurnOnAnyPlaneIfTower (otherLocationOnMyrror, FogOfWarValue.REMEMBER_AS_LAST_SEEN, map, fogOfWarArea, db));

		// Can see location on this plane
		for (int x = 2; x <=3; x++)
			fogOfWarArea.getPlane ().get (1).getRow ().get (2).getCell ().set (x, FogOfWarStateID.HAVE_SEEN);

		assertTrue (MomFogOfWarCalculations.canSeeMidTurnOnAnyPlaneIfTower (towerOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));
		assertTrue (MomFogOfWarCalculations.canSeeMidTurnOnAnyPlaneIfTower (otherLocationOnMyrror, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN, map, fogOfWarArea, db));
	}
}
