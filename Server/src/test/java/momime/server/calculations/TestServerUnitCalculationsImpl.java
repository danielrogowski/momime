package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.random.RandomUtils;

import momime.common.calculations.UnitCalculations;
import momime.common.calculations.UnitStack;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.PlaneSvr;
import momime.server.database.RangedAttackTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.TileTypeSvr;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.KillUnitActionID;

/**
 * Tests the ServerUnitCalculationsImpl class
 */
public final class TestServerUnitCalculationsImpl extends ServerTestData
{
	/**
	 * Tests the calculateUnitScoutingRange class
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitScoutingRange () throws Exception
	{
		// Mock database
		final UnitSkillSvr flightSkill = new UnitSkillSvr ();
		flightSkill.setUnitSkillScoutingRange (2);
		
		final UnitSkillSvr otherSkill = new UnitSkillSvr ();

		final UnitSkillSvr longSightSkill = new UnitSkillSvr ();
		longSightSkill.setUnitSkillScoutingRange (4);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnitSkill ("US001", "calculateUnitScoutingRange")).thenReturn (flightSkill);
		when (db.findUnitSkill ("US002", "calculateUnitScoutingRange")).thenReturn (otherSkill);
		when (db.findUnitSkill ("US003", "calculateUnitScoutingRange")).thenReturn (longSightSkill);
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();

		// Unit with no skills and no scouting range
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);
		assertEquals (1, calc.calculateUnitScoutingRange (unit, db));
		
		// Unit with Scouting III
		when (unit.hasModifiedSkill (ServerDatabaseValues.UNIT_SKILL_ID_SCOUTING)).thenReturn (true);
		when (unit.getModifiedSkillValue (ServerDatabaseValues.UNIT_SKILL_ID_SCOUTING)).thenReturn (3);
		assertEquals (3, calc.calculateUnitScoutingRange (unit, db));
		
		// Unit with two skills, one which grants Scouting II (like Flight) and one which has nothing at all to do with scouting
		final Set<String> unitSkillIDs = new HashSet<String> ();
		unitSkillIDs.add ("US001");
		unitSkillIDs.add ("US002");
		
		when (unit.listModifiedSkillIDs ()).thenReturn (unitSkillIDs);
		assertEquals (3, calc.calculateUnitScoutingRange (unit, db));
		
		// Unit with a skill which grants Scouting IV
		unitSkillIDs.add ("US003");
		assertEquals (4, calc.calculateUnitScoutingRange (unit, db));
	}

	/**
	 * Tests the countOurAliveUnitsAtEveryLocation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCountOurAliveUnitsAtEveryLocation () throws Exception
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();

		// Null location
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setOwningPlayerID (2);
		u1.setStatus (UnitStatusID.ALIVE);
		units.add (u1);

		// 3 at first location
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit u2 = new MemoryUnit ();
			u2.setOwningPlayerID (2);
			u2.setStatus (UnitStatusID.ALIVE);
			u2.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			units.add (u2);
		}

		// 4 at second location
		for (int n = 0; n < 4; n++)
		{
			final MemoryUnit u2 = new MemoryUnit ();
			u2.setOwningPlayerID (2);
			u2.setStatus (UnitStatusID.ALIVE);
			u2.setUnitLocation (new MapCoordinates3DEx (30, 20, 1));
			units.add (u2);
		}

		// Wrong player
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (3);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		units.add (u2);

		// Null status
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (2);
		u3.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		units.add (u3);

		// Unit is dead
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (2);
		u4.setStatus (UnitStatusID.DEAD);
		u4.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		units.add (u4);

		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		
		// Run test
		final int [] [] [] counts = calc.countOurAliveUnitsAtEveryLocation (2, units, sys);

		assertEquals (3, counts [0] [10] [20]);
		assertEquals (4, counts [1] [20] [30]);

		// Reset both the locations we already checked to 0, easier to check the whole array then
		counts [0] [10] [20] = 0;
		counts [1] [20] [30] = 0;
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
					assertEquals (0, counts [z] [y] [x]);
	}

	/**
	 * Tests the willMovingHereResultInAnAttack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWillMovingHereResultInAnAttack () throws Exception
	{
		// Remember this is all operating over a player's memory - so it has to also work where we may know nothing about the location at all, i.e. everything is nulls
		// This is a really key method so there's a ton of test conditions
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = createOverlandMap (sys);

		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		calc.setUnitUtils (new UnitUtilsImpl ());
		
		// Null terrain and city data
		assertFalse (calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units));

		// Terrain data present but tile type and map feature still null
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		map.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);

		assertFalse (calc.willMovingHereResultInAnAttack (20, 10, 0, 2, map, units));

		// Regular tile type
		terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_MOUNTAIN);

		assertFalse (calc.willMovingHereResultInAnAttack (20, 10, 0, 2, map, units));

		// Tower that we've previously cleared but now occupied by our units
		terrainData.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		final MapCoordinates3DEx unitLocation = new MapCoordinates3DEx (20, 10, 0);

		final MemoryUnit unit = new MemoryUnit ();
		unit.setOwningPlayerID (2);
		unit.setUnitLocation (unitLocation);
		unit.setStatus (UnitStatusID.ALIVE);

		units.add (unit);

		assertFalse (calc.willMovingHereResultInAnAttack (20, 10, 0, 2, map, units));

		// Tower that we've previously cleared but now occupied by enemy units
		unit.setOwningPlayerID (1);

		assertTrue (calc.willMovingHereResultInAnAttack (20, 10, 0, 2, map, units));

		// Tower that we've previously cleared but now occupied by our units and we're on Myrror
		final OverlandMapTerrainData myrrorData = new OverlandMapTerrainData ();
		myrrorData.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (myrrorData);
		
		unit.setOwningPlayerID (2);
		unit.setUnitLocation (unitLocation);

		assertFalse (calc.willMovingHereResultInAnAttack (20, 10, 1, 2, map, units));

		// Tower that we've previously cleared but now occupied by enemy units and we're on Myrror
		unit.setOwningPlayerID (1);

		assertTrue (calc.willMovingHereResultInAnAttack (20, 10, 1, 2, map, units));

		// Our city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);

		map.getPlane ().get (0).getRow ().get (10).getCell ().get (30).setCityData (cityData);

		assertFalse (calc.willMovingHereResultInAnAttack (30, 10, 0, 2, map, units));

		// Enemy city
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (1);

		assertTrue (calc.willMovingHereResultInAnAttack (30, 10, 0, 2, map, units));

		// Our units in open area
		unit.setOwningPlayerID (2);
		unitLocation.setX (40);

		assertFalse (calc.willMovingHereResultInAnAttack (40, 10, 0, 2, map, units));

		// Enemy units in open area
		unit.setOwningPlayerID (1);

		assertTrue (calc.willMovingHereResultInAnAttack (40, 10, 0, 2, map, units));
	}

	/**
	 * Tests the calculateDoubleMovementRatesForUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementRatesForUnitStack () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);		
		
		// All possible tile types
		final List<TileTypeSvr> tileTypes = new ArrayList<TileTypeSvr> ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeSvr thisTileType = new TileTypeSvr ();
			thisTileType.setTileTypeID ("TT0" + n);
			tileTypes.add (thisTileType);
		}
		
		when (db.getTileTypes ()).thenReturn (tileTypes);

		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setUnitCalculations (unitCalc);
		
		// Single unit
		final List<ExpandedUnitDetails> units = new ArrayList<ExpandedUnitDetails> ();
		
		final ExpandedUnitDetails spearmenUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), anySet (), eq ("TT01"), eq (db))).thenReturn (4);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), anySet (), eq ("TT02"), eq (db))).thenReturn (6);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (spearmenUnit), anySet (), eq ("TT03"), eq (db))).thenReturn (null);
		
		units.add (spearmenUnit);

		final Map<String, Integer> spearmen = calc.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, spearmen.size ());
		assertEquals (4, spearmen.get ("TT01").intValue ());
		assertEquals (6, spearmen.get ("TT02").intValue ());
		assertNull (spearmen.get ("TT03"));
		
		// Stacking a faster unit with it makes no difference - it always chooses the slowest movement rate
		final ExpandedUnitDetails flyingUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (flyingUnit), anySet (), any (String.class), eq (db))).thenReturn (2);
		
		units.add (flyingUnit);
		
		final Map<String, Integer> flying = calc.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, flying.size ());
		assertEquals (4, flying.get ("TT01").intValue ());
		assertEquals (6, flying.get ("TT02").intValue ());
		assertNull (flying.get ("TT03"));
		
		// Stack a slower unit
		final ExpandedUnitDetails pathfindingUnit = mock (ExpandedUnitDetails.class);
		when (unitCalc.calculateDoubleMovementToEnterTileType (eq (pathfindingUnit), anySet (), any (String.class), eq (db))).thenReturn (5);
		
		units.add (pathfindingUnit);
		
		final Map<String, Integer> pathfinding = calc.calculateDoubleMovementRatesForUnitStack (units, db);
		assertEquals (2, pathfinding.size ());
		assertEquals (5, pathfinding.get ("TT01").intValue ());
		assertEquals (6, pathfinding.get ("TT02").intValue ());
		assertNull (pathfinding.get ("TT03"));
	}

	/**
	 * Tests the calculateOverlandMovementDistances method when we aren't standing on a tower and there is nothing in our way
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateOverlandMovementDistances_Basic () throws Exception
	{
		final ServerDatabaseEx db = loadServerDatabase ();

		final UnitSvr spearmenDef = new UnitSvr ();
		
		// Create map
		final MomSessionDescription sd = createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		try (final Workbook workbook = WorkbookFactory.create (getClass ().getResourceAsStream ("/calculateOverlandMovementDistances.xlsx")))
		{
			final MapVolumeOfMemoryGridCells terrain = createOverlandMapFromExcel (sd.getOverlandMapSize (), workbook);
		
			final FogOfWarMemory map = new FogOfWarMemory ();
			map.setMap (terrain);
		
			// Create other areas
			final int [] [] [] doubleMovementDistances			= new int [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
			final int [] [] [] movementDirections					= new int [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
			final boolean [] [] [] canMoveToInOneTurn			= new boolean [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
			final boolean [] [] [] movingHereResultsInAttack	= new boolean [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
		
			// Units that are moving - two units of high men spearmen
			final UnitStack unitStack = new UnitStack ();
			final UnitUtils unitUtils = mock (UnitUtils.class);
			final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
			final UnitCalculations unitCalc = mock (UnitCalculations.class);
			final Set<String> unitStackSkills = new HashSet<String> ();
		
			for (int n = 1; n <= 2; n++)
			{
				final UnitSkillAndValue walkingSkill = new UnitSkillAndValue ();
				walkingSkill.setUnitSkillID ("USX01");
				
				final MemoryUnit spearmen = new MemoryUnit ();
				spearmen.setUnitURN (n);
				spearmen.setOwningPlayerID (2);
				spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
				spearmen.getUnitHasSkill ().add (walkingSkill);
				spearmen.setStatus (UnitStatusID.ALIVE);
				spearmen.setUnitID ("UN001");

				final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
				when (unitUtils.expandUnitDetails (spearmen, null, null, null, players, map, db)).thenReturn (xuSpearmen);
				when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);

				map.getUnit ().add (spearmen);
				unitStack.getUnits ().add (xuSpearmen);
				
				// Movement rates for this unit - read these off from the server XML editor
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT01", db)).thenReturn (6);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT02", db)).thenReturn (6);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT03", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT04", db)).thenReturn (2);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT05", db)).thenReturn (6);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT06", db)).thenReturn (2);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT07", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT08", db)).thenReturn (null);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT09", db)).thenReturn (null);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT10", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT11", db)).thenReturn (null);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT12", db)).thenReturn (2);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT13", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT14", db)).thenReturn (6);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT15", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT98", db)).thenReturn (1);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT99", db)).thenReturn (0);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "FOW", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "FOWPARTIAL", db)).thenReturn (null);
			}
			
			// Set up object to test
			final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
			calc.setUnitUtils (unitUtils);
			calc.setUnitCalculations (unitCalc);
			calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
			calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
			
			// Run method
			calc.calculateOverlandMovementDistances (20, 10, 1, 2, map, unitStack,
				2, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, players, sd, db);
		
			// Check canMoveToInOneTurn (see the red marked area on the Excel sheet)
			assertTrue (canMoveToInOneTurn [1] [8] [20]);
			assertTrue (canMoveToInOneTurn [1] [8] [21]);
			assertTrue (canMoveToInOneTurn [1] [8] [22]);
			assertTrue (canMoveToInOneTurn [1] [9] [19]);
			assertTrue (canMoveToInOneTurn [1] [9] [20]);
			assertTrue (canMoveToInOneTurn [1] [9] [21]);
			assertTrue (canMoveToInOneTurn [1] [9] [22]);
			assertTrue (canMoveToInOneTurn [1] [10] [19]);
			assertTrue (canMoveToInOneTurn [1] [10] [20]);
			assertTrue (canMoveToInOneTurn [1] [10] [21]);
			assertTrue (canMoveToInOneTurn [1] [10] [22]);
			assertTrue (canMoveToInOneTurn [1] [11] [21]);
			assertTrue (canMoveToInOneTurn [1] [11] [22]);
			assertTrue (canMoveToInOneTurn [1] [11] [23]);
			assertTrue (canMoveToInOneTurn [1] [12] [21]);
			assertTrue (canMoveToInOneTurn [1] [12] [22]);
			assertTrue (canMoveToInOneTurn [1] [12] [23]);
			assertTrue (canMoveToInOneTurn [1] [13] [20]);
			assertTrue (canMoveToInOneTurn [1] [13] [21]);
			assertTrue (canMoveToInOneTurn [1] [13] [22]);
			assertTrue (canMoveToInOneTurn [1] [13] [23]);
		
			// Check all the movement distances and directions on Myrror
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				{
					// Distances
					final Cell distanceCell = workbook.getSheetAt (2).getRow (y + 1).getCell (x + 1);
					if (distanceCell != null)
					{
						// Impassable
						if (distanceCell.getCellType () == CellType.BLANK)
							assertEquals (x + "," + y, -2, doubleMovementDistances [1] [y] [x]);
						else
						{
							final int doubleMovementDistance = (int) distanceCell.getNumericCellValue ();
							assertEquals ("Distance to " + x + "," + y, doubleMovementDistance, doubleMovementDistances [1] [y] [x]);
						}
					}
		
					// Directions
					final Cell directionCell = workbook.getSheetAt (3).getRow (y + 1).getCell (x + 1);
					final int movementDirection = (int) directionCell.getNumericCellValue ();
					assertEquals ("Direction to " + x + "," + y, movementDirection, movementDirections [1] [y] [x]);
				}
		
			// Perform counts on all the areas to make sure no additional values other than the ones checked above
			int countCanMoveToInOneTurn = 0;
			int countMovingHereResultsInAttack = 0;
			int accessibleTilesDistances = 0;
			int accessibleTilesDirections = 0;
		
			for (final PlaneSvr plane : db.getPlanes ())
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					{
						if (canMoveToInOneTurn [plane.getPlaneNumber ()] [y] [x])
							countCanMoveToInOneTurn++;
		
						if (movingHereResultsInAttack [plane.getPlaneNumber ()] [y] [x])
							countMovingHereResultsInAttack++;
		
						if (doubleMovementDistances [plane.getPlaneNumber ()] [y] [x] >= 0)
							accessibleTilesDistances++;
		
						if (movementDirections [plane.getPlaneNumber ()] [y] [x] > 0)
							accessibleTilesDirections++;
					}
		
			assertEquals (21, countCanMoveToInOneTurn);
			assertEquals (0, countMovingHereResultsInAttack);
			assertEquals ((60*40)-3, accessibleTilesDistances);		// 3 ocean tiles - for distances the cell we start from has a valid value of 0
			assertEquals ((60*40)-4, accessibleTilesDirections);		// 3 ocean tiles plus start position - for directions the cell we start from has invalid value 0
		}
	}

	/**
	 * Tests the calculateOverlandMovementDistances method when we are standing on a tower and there are various obstacles on both planes
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateOverlandMovementDistances_Tower () throws Exception
	{
		final ServerDatabaseEx db = loadServerDatabase ();

		final UnitSvr spearmenDef = new UnitSvr ();
		
		// Create map
		final MomSessionDescription sd = createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		try (final Workbook workbook = WorkbookFactory.create (getClass ().getResourceAsStream ("/calculateOverlandMovementDistances.xlsx")))
		{
			final MapVolumeOfMemoryGridCells terrain = createOverlandMapFromExcel (sd.getOverlandMapSize (), workbook);
	
			final FogOfWarMemory map = new FogOfWarMemory ();
			map.setMap (terrain);
	
			// Add tower
			for (final PlaneSvr plane : db.getPlanes ())
				terrain.getPlane ().get (plane.getPlaneNumber ()).getRow ().get (10).getCell ().get (20).getTerrainData ().setMapFeatureID
					(CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY);
	
			// Put 3 nodes on Arcanus - one we haven't scouted, one we have scouted and know its contents, and the last we already cleared
			// The one that we previously cleared we can walk right through and out the other side; the other two we can move onto but not past
			// Nature nodes, so forest, same as there before so we don't alter movement rates - all we alter is that we can't move through them
			final UnitUtils unitUtils = mock (UnitUtils.class);
			final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
			
			int nextUnitURN = 0;
			for (int y = 9; y <= 11; y++)
			{
				terrain.getPlane ().get (0).getRow ().get (y).getCell ().get (18).getTerrainData ().setTileTypeID ("TT13");
				
				// With removal of scouting, nodes just means enemy units
				if (y < 11)
				{
					nextUnitURN++;
					final MemoryUnit their = new MemoryUnit ();
					their.setUnitURN (nextUnitURN);
					their.setOwningPlayerID (1);
					their.setUnitLocation (new MapCoordinates3DEx (18, y, 0));
					their.setStatus (UnitStatusID.ALIVE);
					their.setUnitID ("UN001");
	
					final ExpandedUnitDetails xuTheir = mock (ExpandedUnitDetails.class);
					when (unitUtils.expandUnitDetails (their, null, null, null, players, map, db)).thenReturn (xuTheir);
					when (xuTheir.getUnitDefinition ()).thenReturn (spearmenDef);
					
					map.getUnit ().add (their);

					when (unitUtils.findFirstAliveEnemyAtLocation (map.getUnit (), 18, y, 0, 2)).thenReturn (their);
				}
			}
	
			// Create other areas
			final int [] [] [] doubleMovementDistances			= new int [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
			final int [] [] [] movementDirections					= new int [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
			final boolean [] [] [] canMoveToInOneTurn			= new boolean [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
			final boolean [] [] [] movingHereResultsInAttack	= new boolean [db.getPlanes ().size ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
	
			// Units that are moving - two units of high men spearmen
			// To be really precise with the data model and how units plane jump at towers, all units in towers are always set to plane 0, so this test data setup isn't entirely correct
			final UnitStack unitStack = new UnitStack ();
			final UnitCalculations unitCalc = mock (UnitCalculations.class);
			final Set<String> unitStackSkills = new HashSet<String> ();
	
			for (int n = 1; n <= 2; n++)
			{
				final UnitSkillAndValue walkingSkill = new UnitSkillAndValue ();
				walkingSkill.setUnitSkillID ("USX01");
	
				nextUnitURN++;
				final MemoryUnit spearmen = new MemoryUnit ();
				spearmen.setUnitURN (nextUnitURN);
				spearmen.setOwningPlayerID (2);
				spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
				spearmen.getUnitHasSkill ().add (walkingSkill);
				spearmen.setStatus (UnitStatusID.ALIVE);
				spearmen.setUnitID ("UN001");
	
				final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
				when (unitUtils.expandUnitDetails (spearmen, null, null, null, players, map, db)).thenReturn (xuSpearmen);
				when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);

				map.getUnit ().add (spearmen);
				unitStack.getUnits ().add (xuSpearmen);
				
				// Movement rates for this unit - read these off from the server XML editor
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT01", db)).thenReturn (6);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT02", db)).thenReturn (6);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT03", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT04", db)).thenReturn (2);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT05", db)).thenReturn (6);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT06", db)).thenReturn (2);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT07", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT08", db)).thenReturn (null);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT09", db)).thenReturn (null);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT10", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT11", db)).thenReturn (null);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT12", db)).thenReturn (2);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT13", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT14", db)).thenReturn (6);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT15", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT98", db)).thenReturn (1);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT99", db)).thenReturn (0);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "FOW", db)).thenReturn (4);
				when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "FOWPARTIAL", db)).thenReturn (null);
			}
	
			// Add 8 of our units in one location, and 8 enemy units in another location, both on Myrror
			// Our units become impassable terrain because we can't fit that many in one map cell; enemy units we can walk onto the tile but not through it
			for (int n = 1; n <= 8; n++)
			{
				nextUnitURN++;
				final MemoryUnit our = new MemoryUnit ();
				our.setUnitURN (nextUnitURN);
				our.setOwningPlayerID (2);
				our.setUnitLocation (new MapCoordinates3DEx (19, 9, 1));
				our.setStatus (UnitStatusID.ALIVE);
				our.setUnitID ("UN001");

				final ExpandedUnitDetails xuOur = mock (ExpandedUnitDetails.class);
				when (unitUtils.expandUnitDetails (our, null, null, null, players, map, db)).thenReturn (xuOur);
				when (xuOur.getUnitDefinition ()).thenReturn (spearmenDef);
				
				map.getUnit ().add (our);
	
				nextUnitURN++;
				final MemoryUnit their = new MemoryUnit ();
				their.setUnitURN (nextUnitURN);
				their.setOwningPlayerID (1);
				their.setUnitLocation (new MapCoordinates3DEx (20, 9, 1));
				their.setStatus (UnitStatusID.ALIVE);
				their.setUnitID ("UN001");
	
				final ExpandedUnitDetails xuTheir = mock (ExpandedUnitDetails.class);
				when (unitUtils.expandUnitDetails (their, null, null, null, players, map, db)).thenReturn (xuTheir);
				when (xuTheir.getUnitDefinition ()).thenReturn (spearmenDef);
				
				map.getUnit ().add (their);
				
				if (n == 1)
					when (unitUtils.findFirstAliveEnemyAtLocation (map.getUnit (), 20, 9, 1, 2)).thenReturn (their);
			}
	
			// Set up object to test
			final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
			calc.setUnitUtils (unitUtils);
			calc.setUnitCalculations (unitCalc);
			calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
			calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
			
			// Run method
			calc.calculateOverlandMovementDistances (20, 10, 1, 2, map, unitStack,
				2, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, players, sd, db);
	
			// Check canMoveToInOneTurn (see the red marked area on the Excel sheet)
			assertTrue (canMoveToInOneTurn [0] [8] [20]);
			assertTrue (canMoveToInOneTurn [0] [8] [21]);
			assertTrue (canMoveToInOneTurn [0] [8] [22]);
			assertTrue (canMoveToInOneTurn [0] [9] [19]);
			assertTrue (canMoveToInOneTurn [0] [9] [20]);
			assertTrue (canMoveToInOneTurn [0] [9] [21]);
			assertTrue (canMoveToInOneTurn [0] [9] [22]);
			assertTrue (canMoveToInOneTurn [0] [10] [19]);
			assertTrue (canMoveToInOneTurn [0] [10] [20]);
			assertTrue (canMoveToInOneTurn [0] [10] [21]);
			assertTrue (canMoveToInOneTurn [0] [10] [22]);
			assertTrue (canMoveToInOneTurn [0] [11] [21]);
			assertTrue (canMoveToInOneTurn [0] [11] [22]);
			assertTrue (canMoveToInOneTurn [0] [11] [23]);
			assertTrue (canMoveToInOneTurn [0] [12] [21]);
			assertTrue (canMoveToInOneTurn [0] [12] [22]);
			assertTrue (canMoveToInOneTurn [0] [12] [23]);
			assertTrue (canMoveToInOneTurn [0] [13] [20]);
			assertTrue (canMoveToInOneTurn [0] [13] [21]);
			assertTrue (canMoveToInOneTurn [0] [13] [22]);
			assertTrue (canMoveToInOneTurn [0] [13] [23]);
	
			assertTrue (canMoveToInOneTurn [1] [8] [20]);
			assertTrue (canMoveToInOneTurn [1] [8] [21]);
			assertTrue (canMoveToInOneTurn [1] [8] [22]);
			// assertTrue (canMoveToInOneTurn [1] [9] [19]);	<-- where stack of 8 units is so we can't move there
			assertTrue (canMoveToInOneTurn [1] [9] [20]);
			assertTrue (canMoveToInOneTurn [1] [9] [21]);
			assertTrue (canMoveToInOneTurn [1] [9] [22]);
			assertTrue (canMoveToInOneTurn [1] [10] [19]);
			assertTrue (canMoveToInOneTurn [1] [10] [20]);
			assertTrue (canMoveToInOneTurn [1] [10] [21]);
			assertTrue (canMoveToInOneTurn [1] [10] [22]);
			assertTrue (canMoveToInOneTurn [1] [11] [21]);
			assertTrue (canMoveToInOneTurn [1] [11] [22]);
			assertTrue (canMoveToInOneTurn [1] [11] [23]);
			assertTrue (canMoveToInOneTurn [1] [12] [21]);
			assertTrue (canMoveToInOneTurn [1] [12] [22]);
			assertTrue (canMoveToInOneTurn [1] [12] [23]);
			assertTrue (canMoveToInOneTurn [1] [13] [20]);
			assertTrue (canMoveToInOneTurn [1] [13] [21]);
			assertTrue (canMoveToInOneTurn [1] [13] [22]);
			assertTrue (canMoveToInOneTurn [1] [13] [23]);
	
			// Check all the movement distances on both planes
			for (final PlaneSvr plane : db.getPlanes ())
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					{
						// Distances
						final Cell distanceCell = workbook.getSheetAt (4 + plane.getPlaneNumber ()).getRow (y + 1).getCell (x + 1);
						if (distanceCell != null)
						{
							// Impassable
							if (distanceCell.getCellType () == CellType.BLANK)
								assertEquals (x + "," + y, -2, doubleMovementDistances [plane.getPlaneNumber ()] [y] [x]);
							else
							{
								final int doubleMovementDistance = (int) distanceCell.getNumericCellValue ();
								assertEquals ("Distance to " + x + "," + y + "," + plane.getPlaneNumber (), doubleMovementDistance, doubleMovementDistances [plane.getPlaneNumber ()] [y] [x]);
							}
						}
					}
	
			// Perform counts on all the areas to make sure no additional values other than the ones checked above
			int countCanMoveToInOneTurn = 0;
			int countMovingHereResultsInAttack = 0;
			int accessibleTilesDistances = 0;
			int accessibleTilesDirections = 0;
	
			for (final PlaneSvr plane : db.getPlanes ())
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					{
						if (canMoveToInOneTurn [plane.getPlaneNumber ()] [y] [x])
							countCanMoveToInOneTurn++;
	
						if (movingHereResultsInAttack [plane.getPlaneNumber ()] [y] [x])
							countMovingHereResultsInAttack++;
	
						if (doubleMovementDistances [plane.getPlaneNumber ()] [y] [x] >= 0)
							accessibleTilesDistances++;
	
						if (movementDirections [plane.getPlaneNumber ()] [y] [x] > 0)
							accessibleTilesDirections++;
					}
	
			assertEquals (41, countCanMoveToInOneTurn);
			assertEquals (3, countMovingHereResultsInAttack);
			assertEquals ((60*40*2)-7, accessibleTilesDistances);		// 3 ocean tiles - for distances the cell we start from has a valid value of 0
			assertEquals ((60*40*2)-9, accessibleTilesDirections);		// 3 ocean tiles plus start position - for directions the cell we start from has invalid value 0
		}
	}

	/**
	 * Tests the recheckTransportCapacity method
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testRecheckTransportCapacity () throws Exception
	{
		// Server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr triremeDef = new UnitSvr ();
		triremeDef.setTransportCapacity (2);
		when (db.findUnit ("UN001", "recheckTransportCapacity")).thenReturn (triremeDef);
		
		final UnitSvr spearmenDef = new UnitSvr ();
		when (db.findUnit ("UN002", "recheckTransportCapacity")).thenReturn (spearmenDef);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting (); 
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (terrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (21).setTerrainData (terrainData);
		
		// Terrain tile
		final MemoryGridCellUtils gridCellUtils = mock (MemoryGridCellUtils.class);
		when (gridCellUtils.convertNullTileTypeToFOW (terrainData, false)).thenReturn ("TT01");

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Unit skills
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		
		final Set<String> unitStackSkills = new HashSet<String> ();
		
		// Units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final MemoryUnit trireme = new MemoryUnit ();
		trireme.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trireme.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
		trireme.setStatus (UnitStatusID.ALIVE);
		trireme.setUnitID ("UN001");
		trireme.setOwningPlayerID (1);
		trueMap.getUnit ().add (trireme);
		
		final ExpandedUnitDetails xuTrireme = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (trireme, null, null, null, players, trueMap, db)).thenReturn (xuTrireme);
		when (xuTrireme.getUnitDefinition ()).thenReturn (triremeDef);
		when (xuTrireme.getMemoryUnit ()).thenReturn (trireme);

		when (unitCalc.calculateDoubleMovementToEnterTileType (xuTrireme, unitStackSkills, "TT01", db)).thenReturn (2);
		
		MemoryUnit killedUnit = null;
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
			spearmen.setStatus (UnitStatusID.ALIVE);
			spearmen.setUnitID ("UN002");
			spearmen.setOwningPlayerID (1);
			trueMap.getUnit ().add (spearmen);
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			when (unitUtils.expandUnitDetails (spearmen, null, null, null, players, trueMap, db)).thenReturn (xuSpearmen);
			when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);
			when (xuSpearmen.getMemoryUnit ()).thenReturn (spearmen);

			when (unitCalc.calculateDoubleMovementToEnterTileType (xuSpearmen, unitStackSkills, "TT01", db)).thenReturn (null);
			
			if (n == 1)
				killedUnit = spearmen;
		}
		
		// Fix random numbers
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (3)).thenReturn (1);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setUnitCalculations (unitCalc);
		calc.setMemoryGridCellUtils (gridCellUtils);
		calc.setRandomUtils (random);
		calc.setFogOfWarMidTurnChanges (midTurn);
		
		// Run method
		calc.recheckTransportCapacity (new MapCoordinates3DEx (20, 10, 1), trueMap, players, fogOfWarSettings, db);
		
		// Check 1 unit of spearmen was killed
		verify (midTurn).killUnitOnServerAndClients (killedUnit, KillUnitActionID.HEALABLE_OVERLAND_DAMAGE, trueMap, players, fogOfWarSettings, db);
	}
	
	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a magic attack
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_Magic () throws Exception
	{
		// RAT
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();
		rat.setMagicRealmID ("A");

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		
		// Run method
		assertEquals (0, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a physical attack, but its close enough to get no penalty
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_Close () throws Exception
	{
		// RAT
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		when (attacker.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 4));
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (6, 4));
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (0, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a physical attack, at short range so we get a small penalty
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_Short () throws Exception
	{
		// RAT
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		when (attacker.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 4));
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (6, 6));
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (1, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a physical attack, at long range so we get a big penalty
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_Long () throws Exception
	{
		// RAT
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		when (attacker.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (0, 7));
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (7, 6));
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (3, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a physical attack, where the long range skill applies
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_LongRange () throws Exception
	{
		// RAT
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		when (attacker.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (0, 7));
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (7, 6));
		
		// We do have the Long Range skill
		when (attacker.hasModifiedSkill (ServerDatabaseValues.UNIT_SKILL_ID_LONG_RANGE)).thenReturn (true);
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (1, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}
}