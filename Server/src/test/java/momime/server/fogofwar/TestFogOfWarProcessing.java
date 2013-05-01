package momime.server.fogofwar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_4.FogOfWarValue;
import momime.common.messages.MemoryCombatAreaEffectUtils;
import momime.common.messages.MemoryGridCellUtils;
import momime.common.messages.MemoryMaintainedSpellUtils;
import momime.common.messages.PlayerPickUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.servertoclient.v0_9_4.UpdateNodeLairTowerUnitIDMessageData;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Plane;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the FogOfWarProcessing class
 */
public final class TestFogOfWarProcessing
{
	/**
	 * Tests the canSee method
	 */
	@Test
	public final void testCanSee ()
	{
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);
		
		// Set up test object
		final FogOfWarProcessing proc = new FogOfWarProcessing ();

		// Never seen, so now seeing it for the first time
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		proc.canSee (fogOfWarArea, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		proc.canSee (fogOfWarArea, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		// Have seen on a previous turn, then lost sight of it, and now seeing it again
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.HAVE_SEEN);
		proc.canSee (fogOfWarArea, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		proc.canSee (fogOfWarArea, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		// Can see, so could see it last turn and still can
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		proc.canSee (fogOfWarArea, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_CAN_STILL_SEE, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		proc.canSee (fogOfWarArea, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_CAN_STILL_SEE, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));
	}

	/**
	 * Tests the canSeeRadius method
	 */
	@Test
	public final void testCanSeeRadius ()
	{
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		// There's 6 FOW states, so put a 7x7 area with the top row clipped off the top of the map
		// The in the leftmost column (which is wrapped to the right edge of the map), put one of each FOW states to test they get modified correctly
		int y = 0;
		for (final FogOfWarStateID state : FogOfWarStateID.values ())
		{
			fogOfWarArea.getPlane ().get (1).getRow ().get (y).getCell ().set (59, state);
			y++;
		}

		// Set up test object
		final FogOfWarProcessing proc = new FogOfWarProcessing ();

		// Run method
		proc.canSeeRadius (fogOfWarArea, sys, 2, 2, 1, 3);

		// Check results of right hand area which is all the same value
		for (int x = 0; x <= 5; x++)
			for (y = 0; y <= 5; y++)
				assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, fogOfWarArea.getPlane ().get (1).getRow ().get (y).getCell ().get (x));

		// Check results of left column
		assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME,				fogOfWarArea.getPlane ().get (1).getRow ().get (0).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT,	fogOfWarArea.getPlane ().get (1).getRow ().get (1).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_CAN_STILL_SEE,								fogOfWarArea.getPlane ().get (1).getRow ().get (2).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME,				fogOfWarArea.getPlane ().get (1).getRow ().get (3).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT,	fogOfWarArea.getPlane ().get (1).getRow ().get (4).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_CAN_STILL_SEE,								fogOfWarArea.getPlane ().get (1).getRow ().get (5).getCell ().get (59));

		// Check no other values got changed
		int count = 0;
		for (int plane = 0; plane < 2; plane++)
			for (y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
					if (fogOfWarArea.getPlane ().get (plane).getRow ().get (y).getCell ().get (x) == FogOfWarStateID.NEVER_SEEN)
						count++;

		assertEquals ((60*40*2)-(7*6), count);
	}

	/**
	 * Tests the markVisibleArea method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws InvalidFormatException If the excel spreadsheet containing the expected results can't be loaded
	 */
	@Test
	public final void testMarkVisibleArea () throws IOException, JAXBException, InvalidFormatException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final UnitUtils unitUtils = new UnitUtils ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);

		// Our cities
		final OverlandMapCityData ourCityOne = new OverlandMapCityData ();
		ourCityOne.setCityOwnerID (2);
		ourCityOne.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (25).getCell ().get (35).setCityData (ourCityOne);

		final OverlandMapCityData ourCityTwo = new OverlandMapCityData ();
		ourCityTwo.setCityOwnerID (2);
		ourCityTwo.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (12).getCell ().get (50).setCityData (ourCityTwo);

		final OverlandMapCityData ourCityThree = new OverlandMapCityData ();
		ourCityThree.setCityOwnerID (2);
		ourCityThree.setCityPopulation (1);
		trueTerrain.getPlane ().get (1).getRow ().get (1).getCell ().get (1).setCityData (ourCityThree);

		// Put buildings in that give cityTwo scouting range 3, and cityThree scouting range 4
		final OverlandMapCoordinates cityWallsLocation = new OverlandMapCoordinates ();
		cityWallsLocation.setX (50);
		cityWallsLocation.setY (12);
		cityWallsLocation.setPlane (0);

		final MemoryBuilding cityWalls = new MemoryBuilding ();
		cityWalls.setBuildingID ("BL35");
		cityWalls.setCityLocation (cityWallsLocation);

		trueMap.getBuilding ().add (cityWalls);

		final OverlandMapCoordinates ourOracleLocation = new OverlandMapCoordinates ();
		ourOracleLocation.setX (1);
		ourOracleLocation.setY (1);
		ourOracleLocation.setPlane (1);

		final MemoryBuilding ourOracle = new MemoryBuilding ();
		ourOracle.setBuildingID ("BL18");
		ourOracle.setCityLocation (ourOracleLocation);

		trueMap.getBuilding ().add (ourOracle);

		// Enemy cities
		final OverlandMapCityData enemyCityOne = new OverlandMapCityData ();
		enemyCityOne.setCityOwnerID (1);
		enemyCityOne.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (5).getCell ().get (5).setCityData (enemyCityOne);

		final OverlandMapCityData enemyCityTwo = new OverlandMapCityData ();
		enemyCityTwo.setCityOwnerID (1);
		enemyCityTwo.setCityPopulation (1);
		trueTerrain.getPlane ().get (1).getRow ().get (32).getCell ().get (54).setCityData (enemyCityTwo);

		// We can see enemy cities that we have a curse on, but having an oracle doesn't increase how much we can see
		final OverlandMapCoordinates curseLocation = new OverlandMapCoordinates ();
		curseLocation.setX (54);
		curseLocation.setY (32);
		curseLocation.setPlane (1);

		final MemoryMaintainedSpell curse = new MemoryMaintainedSpell ();
		curse.setCastingPlayerID (2);
		curse.setCityLocation (curseLocation);
		curse.setSpellID ("SP110");
		curse.setCitySpellEffectID ("SE110");

		trueMap.getMaintainedSpell ().add (curse);

		final OverlandMapCoordinates enemyOracleLocation = new OverlandMapCoordinates ();
		enemyOracleLocation.setX (54);
		enemyOracleLocation.setY (32);
		enemyOracleLocation.setPlane (1);

		final MemoryBuilding enemyOracle = new MemoryBuilding ();
		enemyOracle.setBuildingID ("BL18");
		enemyOracle.setCityLocation (enemyOracleLocation);

		trueMap.getBuilding ().add (enemyOracle);

		// Units - a regular unit, flying unit (sees distance 2) and unit with actual scouting III skill
		final OverlandMapCoordinates unitOneLocation = new OverlandMapCoordinates ();
		unitOneLocation.setX (54);
		unitOneLocation.setY (4);
		unitOneLocation.setPlane (1);

		final MemoryUnit unitOne = unitUtils.createMemoryUnit ("UN105", 1, 0, 0, true, db);
		unitOne.setUnitLocation (unitOneLocation);
		unitOne.setOwningPlayerID (2);

		trueMap.getUnit ().add (unitOne);

		final OverlandMapCoordinates unitTwoLocation = new OverlandMapCoordinates ();
		unitTwoLocation.setX (14);
		unitTwoLocation.setY (34);
		unitTwoLocation.setPlane (1);

		final MemoryUnit unitTwo = unitUtils.createMemoryUnit ("UN067", 2, 0, 0, true, db);
		unitTwo.setUnitLocation (unitTwoLocation);
		unitTwo.setOwningPlayerID (2);

		trueMap.getUnit ().add (unitTwo);

		final OverlandMapCoordinates unitThreeLocation = new OverlandMapCoordinates ();
		unitThreeLocation.setX (44);
		unitThreeLocation.setY (17);
		unitThreeLocation.setPlane (0);

		final MemoryUnit unitThree = unitUtils.createMemoryUnit ("UN005", 3, 0, 0, true, db);
		unitThree.setUnitLocation (unitThreeLocation);
		unitThree.setOwningPlayerID (2);

		trueMap.getUnit ().add (unitThree);

		// Unit in a tower
		for (final Plane plane : db.getPlane ())
		{
			final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
			terrainData.setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_CLEARED_TOWER_OF_WIZARDRY);

			trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (22).getCell ().get (22).setTerrainData (terrainData);
		}

		final OverlandMapCoordinates unitFourLocation = new OverlandMapCoordinates ();
		unitFourLocation.setX (22);
		unitFourLocation.setY (22);
		unitFourLocation.setPlane (0);

		final MemoryUnit unitFour = unitUtils.createMemoryUnit ("UN105", 4, 0, 0, true, db);
		unitFour.setUnitLocation (unitFourLocation);
		unitFour.setOwningPlayerID (2);

		trueMap.getUnit ().add (unitFour);

		// Enemy unit
		final OverlandMapCoordinates unitFiveLocation = new OverlandMapCoordinates ();
		unitFiveLocation.setX (23);
		unitFiveLocation.setY (9);
		unitFiveLocation.setPlane (1);

		final MemoryUnit unitFive = unitUtils.createMemoryUnit ("UN105", 1, 0, 0, true, db);
		unitFive.setUnitLocation (unitFiveLocation);
		unitFive.setOwningPlayerID (1);

		trueMap.getUnit ().add (unitFive);

		// Nature's eye spell
		final OverlandMapCoordinates naturesEyeLocation = new OverlandMapCoordinates ();
		naturesEyeLocation.setX (11);
		naturesEyeLocation.setY (35);
		naturesEyeLocation.setPlane (0);

		final MemoryMaintainedSpell naturesEye = new MemoryMaintainedSpell ();
		naturesEye.setCastingPlayerID (2);
		naturesEye.setCityLocation (naturesEyeLocation);
		naturesEye.setSpellID ("SP012");
		naturesEye.setCitySpellEffectID ("SE012");

		trueMap.getMaintainedSpell ().add (naturesEye);

		// Set up test object
		unitUtils.setPlayerPickUtils (new PlayerPickUtils ());
		unitUtils.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtils ());
		
		final MomServerUnitCalculations serverUnitCalculations = new MomServerUnitCalculations ();
		serverUnitCalculations.setUnitUtils (unitUtils);
		
		final FogOfWarProcessing proc = new FogOfWarProcessing ();
		proc.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtils ());
		proc.setMemoryGridCellUtils (new MemoryGridCellUtils ());
		proc.setServerCityCalculations (new MomServerCityCalculations ());
		proc.setServerUnitCalculations (serverUnitCalculations);

		// Test with no special spells
		priv.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		proc.markVisibleArea (trueMap, player, players, sd, db);

		final Workbook workbook = WorkbookFactory.create (new Object ().getClass ().getResourceAsStream ("/markVisibleArea.xlsx"));
		for (final Plane plane : db.getPlane ())
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				{
					final Cell cell = workbook.getSheetAt (plane.getPlaneNumber ()).getRow (y + 1).getCell (x + 1);

					// The "A" cells mark locations we can only see after we cast Awareness
					if ((cell != null) && (cell.getCellType () != Cell.CELL_TYPE_BLANK) && (!cell.getStringCellValue ().equals ("A")))
						assertEquals (x + "," + y + "," + plane.getPlaneNumber (), FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x));
					else
						assertEquals (x + "," + y + "," + plane.getPlaneNumber (), FogOfWarStateID.NEVER_SEEN, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x));
				}

		// Awareness
		final MemoryMaintainedSpell awareness = new MemoryMaintainedSpell ();
		awareness.setCastingPlayerID (2);
		awareness.setSpellID (ServerDatabaseValues.VALUE_SPELL_ID_AWARENESS);
		trueMap.getMaintainedSpell ().add (awareness);

		priv.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		proc.markVisibleArea (trueMap, player, players, sd, db);

		for (final Plane plane : db.getPlane ())
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				{
					final Cell cell = workbook.getSheetAt (plane.getPlaneNumber ()).getRow (y + 1).getCell (x + 1);
					if ((cell != null) && (cell.getCellType () != Cell.CELL_TYPE_BLANK))
						assertEquals (x + "," + y + "," + plane.getPlaneNumber (), FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x));
					else
						assertEquals (x + "," + y + "," + plane.getPlaneNumber (), FogOfWarStateID.NEVER_SEEN, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x));
				}

		// Nature Awareness
		awareness.setSpellID (ServerDatabaseValues.VALUE_SPELL_ID_NATURE_AWARENESS);

		priv.setFogOfWar (ServerTestData.createFogOfWarArea (sd.getMapSize ()));
		proc.markVisibleArea (trueMap, player, players, sd, db);

		for (final Plane plane : db.getPlane ())
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
					assertEquals (x + "," + y + "," + plane.getPlaneNumber (), FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x));
	}

	/**
	 * Tests the determineVisibleAreaChangedUpdateAction method
	 */
	@Test
	public final void testDetermineVisibleAreaChangedUpdateAction ()
	{
		// Set up test object
		final FogOfWarProcessing proc = new FogOfWarProcessing ();

		// Just go through every possible combination against the expected outcome

		// Never seen - couldn't see before, and we still can't
		// So regardless of what FOW setting we're on, that means there's nothing to do
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.NEVER_SEEN, setting));

		// Have seen - at some prior time we saw it then lost sight of it - and this turn it still didn't come back into view yet
		// So regardless of what FOW setting we're on, that means there's nothing to do
		// Although if we're on "always see once seen" then the fact that we saw the location ages ago still means we've got up to date info on it
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.FORGET));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));

		// Can see - could see it last turn, but this turn we lost sight of it
		// If setting = forget then we need to forget our knowledge of it
		// If setting = remember then we need to remember our knowledge of it, so nothing to do
		// If setting = always see once seen then we're still able to see it and so know that nothing has changed, so still nothing to do
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.CAN_SEE, FogOfWarValue.FORGET));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.CAN_SEE, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));

		// Seeing it for first time
		// So regardless of what FOW setting we're on, we need to add it
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, setting));

		// Seeing after lost sight of it - we saw it once before, lost sight of it for a while, and it just came back into view again
		// If setting = forget then we'd have forgotten about it when it went out of view, so now need to re-add it
		// If setting = remember then maybe it changed while we couldn't see it, so our memory of it may now be incorrect, so need to update it
		// If setting = always see once seen then even when it went out of sight, we could watch changes to it even while it was out of sight, so we know our knowledge of it is already correct
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, FogOfWarValue.FORGET));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));

		// Can still see - could see it last turn, and still can
		// So regardless of what FOW setting we're on, we know we already have the correct values so no update required
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_CAN_STILL_SEE, setting));
	}

	/**
	 * Tests the areCoordinatesIncludedInMessage method
	 */
	@Test
	public final void testAreCoordinatesIncludedInMessage ()
	{
		final List<UpdateNodeLairTowerUnitIDMessageData> coordinateList = new ArrayList<UpdateNodeLairTowerUnitIDMessageData> ();

		// Put 3 sets of coordinates in the list
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
			coords.setX (n);
			coords.setY (n + 1);
			coords.setPlane (n + 2);

			final UpdateNodeLairTowerUnitIDMessageData msgData = new UpdateNodeLairTowerUnitIDMessageData ();
			msgData.setNodeLairTowerLocation (coords);

			coordinateList.add (msgData);
		}

		// Set up test object
		final FogOfWarProcessing proc = new FogOfWarProcessing ();

		// Test some coordinates that are in the list
		final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
		coords.setX (2);
		coords.setY (3);
		coords.setPlane (4);

		assertTrue (proc.areCoordinatesIncludedInMessage (coordinateList, coords));

		// Test some coordinates that aren't in the list
		coords.setX (3);

		assertFalse (proc.areCoordinatesIncludedInMessage (coordinateList, coords));
	}
}
