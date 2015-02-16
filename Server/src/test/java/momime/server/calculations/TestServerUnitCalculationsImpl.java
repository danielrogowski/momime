package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitHasSkill;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.MovementRateRuleSvr;
import momime.server.database.PlaneSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.TileTypeSvr;
import momime.server.database.UnitSkillSvr;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Tests the ServerUnitCalculations class
 */
public final class TestServerUnitCalculationsImpl
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
		
		// Lists
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Unit skills
		final UnitHasSkillMergedList mergedSkills = new UnitHasSkillMergedList ();
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);

		// Unit with no skills and no scouting range
		final MemoryUnit unit = new MemoryUnit ();
		when (unitUtils.mergeSpellEffectsIntoSkillList (spells, unit)).thenReturn (mergedSkills);
		assertEquals (1, calc.calculateUnitScoutingRange (unit, players, spells, combatAreaEffects, db));
		
		// Unit with Scouting III
		when (unitUtils.getModifiedSkillValue (unit, mergedSkills, ServerDatabaseValues.UNIT_SKILL_ID_SCOUTING, players, spells, combatAreaEffects, db)).thenReturn (3);
		assertEquals (3, calc.calculateUnitScoutingRange (unit, players, spells, combatAreaEffects, db));
		
		// Unit with two skills, one which grants Scouting II (like Flight) and one which has nothing at all to do with scouting
		final UnitHasSkill flight = new UnitHasSkill ();
		flight.setUnitSkillID ("US001");
		mergedSkills.add (flight);

		final UnitHasSkill other = new UnitHasSkill ();
		other.setUnitSkillID ("US002");
		mergedSkills.add (other);
		
		assertEquals (3, calc.calculateUnitScoutingRange (unit, players, spells, combatAreaEffects, db));
		
		// Unit with a skill which grants Scouting IV
		final UnitHasSkill longSight = new UnitHasSkill ();
		longSight.setUnitSkillID ("US003");
		mergedSkills.add (longSight);

		assertEquals (4, calc.calculateUnitScoutingRange (unit, players, spells, combatAreaEffects, db));
	}

	/**
	 * Tests the countOurAliveUnitsAtEveryLocation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCountOurAliveUnitsAtEveryLocation () throws Exception
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();

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
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Remember this is all operating over a player's memory - so it has to also work where we may know nothing about the location at all, i.e. everything is nulls
		// This is a really key method so there's a ton of test conditions
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);

		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		calc.setUnitUtils (new UnitUtilsImpl ());
		
		// Null terrain and city data
		assertFalse (calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, db));

		// Terrain data present but tile type and map feature still null
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		map.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);

		assertFalse (calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, db));

		// Regular tile type
		terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_MOUNTAIN);

		assertFalse (calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, db));

		// Tower that we've previously cleared but now occupied by our units
		terrainData.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		final MapCoordinates3DEx unitLocation = new MapCoordinates3DEx (20, 10, 0);

		final MemoryUnit unit = new MemoryUnit ();
		unit.setOwningPlayerID (2);
		unit.setUnitLocation (unitLocation);
		unit.setStatus (UnitStatusID.ALIVE);

		units.add (unit);

		assertFalse (calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, db));

		// Tower that we've previously cleared but now occupied by enemy units
		unit.setOwningPlayerID (1);

		assertTrue (calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, db));

		// Tower that we've previously cleared but now occupied by our units and we're on Myrror
		final OverlandMapTerrainData myrrorData = new OverlandMapTerrainData ();
		myrrorData.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (myrrorData);
		
		unit.setOwningPlayerID (2);
		unit.setUnitLocation (unitLocation);

		assertFalse (calc.willMovingHereResultInAnAttack
			(20, 10, 1, 2, map, units, db));

		// Tower that we've previously cleared but now occupied by enemy units and we're on Myrror
		unit.setOwningPlayerID (1);

		assertTrue (calc.willMovingHereResultInAnAttack
			(20, 10, 1, 2, map, units, db));

		// Our city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);

		map.getPlane ().get (0).getRow ().get (10).getCell ().get (30).setCityData (cityData);

		assertFalse (calc.willMovingHereResultInAnAttack
			(30, 10, 0, 2, map, units, db));

		// Enemy city but null population
		cityData.setCityOwnerID (1);

		assertFalse (calc.willMovingHereResultInAnAttack
			(30, 10, 0, 2, map, units, db));

		// Enemy city but zero population
		cityData.setCityPopulation (0);

		assertFalse (calc.willMovingHereResultInAnAttack
			(30, 10, 0, 2, map, units, db));

		// Enemy city
		cityData.setCityPopulation (1);

		assertTrue (calc.willMovingHereResultInAnAttack
			(30, 10, 0, 2, map, units, db));

		// Our units in open area
		unit.setOwningPlayerID (2);
		unitLocation.setX (40);

		assertFalse (calc.willMovingHereResultInAnAttack
			(40, 10, 0, 2, map, units, db));

		// Enemy units in open area
		unit.setOwningPlayerID (1);

		assertTrue (calc.willMovingHereResultInAnAttack
			(40, 10, 0, 2, map, units, db));
	}

	/**
	 * Tests the listAllSkillsInUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListAllSkillsInUnitStack () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();

		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Null stack
		assertEquals (0, calc.listAllSkillsInUnitStack (null, spells, db).size ());

		// Single unit with only skills from DB
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		final MemoryUnit longbowmenUnit = new MemoryUnit ();
		longbowmenUnit.setUnitURN (1);
		for (final String unitSkillID : new String [] {CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, "US132", "US001", "USX01"})
		{
			final UnitHasSkill unitHasSkill = new UnitHasSkill ();
			unitHasSkill.setUnitSkillID (unitSkillID);
			longbowmenUnit.getUnitHasSkill ().add (unitHasSkill);
		}
		
		units.add (longbowmenUnit);

		final List<String> longbowmen = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (4, longbowmen.size ());
		assertEquals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, longbowmen.get (0));
		assertEquals ("US132", longbowmen.get (1));
		assertEquals ("US001", longbowmen.get (2));
		assertEquals ("USX01", longbowmen.get (3));

		// Two units with skills only from DB
		final MemoryUnit elvenLordsUnit = new MemoryUnit ();
		elvenLordsUnit.setUnitURN (2);
		for (final String unitSkillID : new String [] {CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, "US001", "USX01", "US028", "US029"})
		{
			final UnitHasSkill unitHasSkill = new UnitHasSkill ();
			unitHasSkill.setUnitSkillID (unitSkillID);
			elvenLordsUnit.getUnitHasSkill ().add (unitHasSkill);
		}
		
		units.add (elvenLordsUnit);

		final List<String> elvenLords = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (6, elvenLords.size ());
		assertEquals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, elvenLords.get (0));
		assertEquals ("US132", elvenLords.get (1));
		assertEquals ("US001", elvenLords.get (2));
		assertEquals ("USX01", elvenLords.get (3));
		assertEquals ("US028", elvenLords.get (4));
		assertEquals ("US029", elvenLords.get (5));

		// Three units with skills only from DB
		final MemoryUnit hellHoundsUnit = new MemoryUnit ();
		hellHoundsUnit.setUnitURN (3);
		for (final String unitSkillID : new String [] {"US134", "USX01"})
		{
			final UnitHasSkill unitHasSkill = new UnitHasSkill ();
			unitHasSkill.setUnitSkillID (unitSkillID);
			hellHoundsUnit.getUnitHasSkill ().add (unitHasSkill);
		}
		
		units.add (hellHoundsUnit);

		final List<String> hellHounds = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (7, hellHounds.size ());
		assertEquals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, hellHounds.get (0));
		assertEquals ("US132", hellHounds.get (1));
		assertEquals ("US001", hellHounds.get (2));
		assertEquals ("USX01", hellHounds.get (3));
		assertEquals ("US028", hellHounds.get (4));
		assertEquals ("US029", hellHounds.get (5));
		assertEquals ("US134", hellHounds.get (6));

		// Multiple units with skills both from DB and from spells
		for (int n = 1; n <= 2; n++)
		{
			final MemoryMaintainedSpell endurance = new MemoryMaintainedSpell ();
			endurance.setUnitURN (n);
			endurance.setUnitSkillID ("SS123");
			spells.add (endurance);
		}

		for (int n = 2; n <= 3; n++)
		{
			final MemoryMaintainedSpell flameBlade = new MemoryMaintainedSpell ();
			flameBlade.setUnitURN (n);
			flameBlade.setUnitSkillID ("SS094");
			spells.add (flameBlade);
		}

		// Include a spell on a unit that isn't in the stack
		final MemoryMaintainedSpell heroism = new MemoryMaintainedSpell ();
		heroism.setUnitURN (4);
		heroism.setUnitSkillID ("SS130");
		spells.add (heroism);

		final List<String> withSpells = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (9, withSpells.size ());
		assertEquals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, withSpells.get (0));
		assertEquals ("US132", withSpells.get (1));
		assertEquals ("US001", withSpells.get (2));
		assertEquals ("USX01", withSpells.get (3));
		assertEquals ("SS123", withSpells.get (4));
		assertEquals ("US028", withSpells.get (5));
		assertEquals ("US029", withSpells.get (6));
		assertEquals ("SS094", withSpells.get (7));
		assertEquals ("US134", withSpells.get (8));
	}

	/**
	 * Tests the calculateDoubleMovementToEnterTileType method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementToEnterTileType () throws Exception
	{
		// Set up some movement rate rules to say:
		// 1) Regular units on foot (US001) can move over TT01 at cost of 4 points and TT02 at cost of 6 points
		// 2) Flying (US002) units move over everything at a cost of 2 points, including water (TT03)
		// 3) Units with a pathfinding-like skill (US003) allow their entire stack to move over any land at a cost of 1 point, but not water 
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final List<MovementRateRuleSvr> rules = new ArrayList<MovementRateRuleSvr> ();
		for (int n = 1; n <= 2; n++)
		{
			final MovementRateRuleSvr pathfindingRule = new MovementRateRuleSvr ();
			pathfindingRule.setTileTypeID ("TT0" + n);
			pathfindingRule.setUnitStackSkillID ("US003");
			pathfindingRule.setDoubleMovement (1);
			rules.add (pathfindingRule);
		}
		
		final MovementRateRuleSvr flyingRule = new MovementRateRuleSvr ();
		flyingRule.setUnitSkillID ("US002");
		flyingRule.setDoubleMovement (2);
		rules.add (flyingRule);

		final MovementRateRuleSvr hillsRule = new MovementRateRuleSvr ();
		hillsRule.setUnitSkillID ("US001");
		hillsRule.setTileTypeID ("TT01");
		hillsRule.setDoubleMovement (4);
		rules.add (hillsRule);
		
		final MovementRateRuleSvr mountainsRule = new MovementRateRuleSvr ();
		mountainsRule.setUnitSkillID ("US001");
		mountainsRule.setTileTypeID ("TT02");
		mountainsRule.setDoubleMovement (6);
		rules.add (mountainsRule);
		
		when (db.getMovementRateRules ()).thenReturn (rules);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Sample unit
		final UnitHasSkill movementSkill = new UnitHasSkill ();
		movementSkill.setUnitSkillID ("US001");
		
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.getUnitHasSkill ().add (movementSkill);

		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();

		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);

		// Regular walking unit can walk over the two types of land tiles, but not water
		final List<String> unitStackSkills = new ArrayList<String> ();

		assertEquals (4, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT01", spells, db).intValue ());
		assertEquals (6, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT02", spells, db).intValue ());
		assertNull (calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT03", spells, db));
		
		// Stack with a pathfinding unit
		unitStackSkills.add ("US003");

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT01", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT02", spells, db).intValue ());
		assertNull (calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT03", spells, db));
		
		// Cast flight spell - pathfinding takes preference, with how the demo rules above are ordered
		final MemoryMaintainedSpell flightSpell = new MemoryMaintainedSpell ();
		flightSpell.setUnitSkillID ("US002");
		flightSpell.setUnitURN (1);
		spells.add (flightSpell);

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT01", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT02", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT03", spells, db).intValue ());
		
		// Naturally flying unit
		unitStackSkills.clear ();
		spells.clear ();
		movementSkill.setUnitSkillID ("US002");
		
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT01", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT02", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT03", spells, db).intValue ());
	}

	/**
	 * Tests the calculateDoubleMovementRatesForUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementRatesForUnitStack () throws Exception
	{
		// Set up some movement rate rules to say:
		// 1) Regular units on foot (US001) can move over TT01 at cost of 4 points and TT02 at cost of 6 points
		// 2) Flying (US002) units move over everything at a cost of 2 points, including water (TT03)
		// 3) Units with a pathfinding-like skill (US003) allow their entire stack to move over any land at a cost of 1 point, but not water 
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final List<MovementRateRuleSvr> rules = new ArrayList<MovementRateRuleSvr> ();
		for (int n = 1; n <= 2; n++)
		{
			final MovementRateRuleSvr pathfindingRule = new MovementRateRuleSvr ();
			pathfindingRule.setTileTypeID ("TT0" + n);
			pathfindingRule.setUnitStackSkillID ("US003");
			pathfindingRule.setDoubleMovement (1);
			rules.add (pathfindingRule);
		}
		
		final MovementRateRuleSvr flyingRule = new MovementRateRuleSvr ();
		flyingRule.setUnitSkillID ("US002");
		flyingRule.setDoubleMovement (2);
		rules.add (flyingRule);

		final MovementRateRuleSvr hillsRule = new MovementRateRuleSvr ();
		hillsRule.setUnitSkillID ("US001");
		hillsRule.setTileTypeID ("TT01");
		hillsRule.setDoubleMovement (4);
		rules.add (hillsRule);
		
		final MovementRateRuleSvr mountainsRule = new MovementRateRuleSvr ();
		mountainsRule.setUnitSkillID ("US001");
		mountainsRule.setTileTypeID ("TT02");
		mountainsRule.setDoubleMovement (6);
		rules.add (mountainsRule);
		
		when (db.getMovementRateRules ()).thenReturn (rules);
		
		// All possible tile types
		final List<TileTypeSvr> tileTypes = new ArrayList<TileTypeSvr> ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeSvr thisTileType = new TileTypeSvr ();
			thisTileType.setTileTypeID ("TT0" + n);
			tileTypes.add (thisTileType);
		}
		
		when (db.getTileTypes ()).thenReturn (tileTypes);

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();

		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Regular walking unit can walk over the two types of land tiles, but not water
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		
		final UnitHasSkill spearmenMovementSkill = new UnitHasSkill ();
		spearmenMovementSkill.setUnitSkillID ("US001");
		
		final MemoryUnit spearmenUnit = new MemoryUnit ();
		spearmenUnit.setUnitURN (1);
		spearmenUnit.getUnitHasSkill ().add (spearmenMovementSkill);
		
		units.add (spearmenUnit);

		final Map<String, Integer> spearmen = calc.calculateDoubleMovementRatesForUnitStack (units, spells, db);
		assertEquals (2, spearmen.size ());
		assertEquals (4, spearmen.get ("TT01").intValue ());
		assertEquals (6, spearmen.get ("TT02").intValue ());
		assertNull (spearmen.get ("TT03"));
		
		// Stacking a flying unit with it makes no difference - although it can move over all tile types and faster, it always chooses the slowest movement rate
		final UnitHasSkill flyingMovementSkill = new UnitHasSkill ();
		flyingMovementSkill.setUnitSkillID ("US001");
		
		final MemoryUnit flyingUnit = new MemoryUnit ();
		flyingUnit.setUnitURN (2);
		flyingUnit.getUnitHasSkill ().add (flyingMovementSkill);
		
		units.add (flyingUnit);
		
		final Map<String, Integer> flying = calc.calculateDoubleMovementRatesForUnitStack (units, spells, db);
		assertEquals (2, flying.size ());
		assertEquals (4, flying.get ("TT01").intValue ());
		assertEquals (6, flying.get ("TT02").intValue ());
		assertNull (flying.get ("TT03"));
		
		// Stacking a pathfinding unit reduces the movement rates for the land tile types for all units in the stack down to 1, but still can't move over water
		final UnitHasSkill pathfindingMovementSkill = new UnitHasSkill ();
		pathfindingMovementSkill.setUnitSkillID ("US003");
		
		final MemoryUnit pathfindingUnit = new MemoryUnit ();
		pathfindingUnit.setUnitURN (2);
		pathfindingUnit.getUnitHasSkill ().add (pathfindingMovementSkill);
		
		units.add (pathfindingUnit);
		
		final Map<String, Integer> pathfinding = calc.calculateDoubleMovementRatesForUnitStack (units, spells, db);
		assertEquals (2, pathfinding.size ());
		assertEquals (1, pathfinding.get ("TT01").intValue ());
		assertEquals (1, pathfinding.get ("TT02").intValue ());
		assertNull (pathfinding.get ("TT03"));
	}

	/**
	 * Tests the calculateOverlandMovementDistances method when we aren't standing on a tower and there is nothing in our way
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateOverlandMovementDistances_Basic () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();

		// Create map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final Workbook workbook = WorkbookFactory.create (new Object ().getClass ().getResourceAsStream ("/calculateOverlandMovementDistances.xlsx"));
		final MapVolumeOfMemoryGridCells terrain = ServerTestData.createOverlandMapFromExcel (sd.getMapSize (), workbook);

		final FogOfWarMemory map = new FogOfWarMemory ();
		map.setMap (terrain);

		// Create other areas
		final int [] [] [] doubleMovementDistances			= new int [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final int [] [] [] movementDirections					= new int [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final boolean [] [] [] canMoveToInOneTurn			= new boolean [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final boolean [] [] [] movingHereResultsInAttack	= new boolean [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];

		// Units that are moving - two units of high men spearmen
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();

		for (int n = 1; n <= 2; n++)
		{
			final UnitHasSkill walkingSkill = new UnitHasSkill ();
			walkingSkill.setUnitSkillID ("USX01");
			
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitURN (n);
			spearmen.setOwningPlayerID (2);
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
			spearmen.getUnitHasSkill ().add (walkingSkill);

			unitStack.add (spearmen);
		}
		map.getUnit ().addAll (unitStack);

		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		calc.calculateOverlandMovementDistances (20, 10, 1, 2, map, unitStack,
			2, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, sd, db);

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
		for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
			{
				// Distances
				final Cell distanceCell = workbook.getSheetAt (2).getRow (y + 1).getCell (x + 1);
				if (distanceCell != null)
				{
					// Impassable
					if (distanceCell.getCellType () == Cell.CELL_TYPE_BLANK)
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
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
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

	/**
	 * Tests the calculateOverlandMovementDistances method when we are standing on a tower and there are various obstacles on both planes
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateOverlandMovementDistances_Tower () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();

		// Create map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final Workbook workbook = WorkbookFactory.create (new Object ().getClass ().getResourceAsStream ("/calculateOverlandMovementDistances.xlsx"));
		final MapVolumeOfMemoryGridCells terrain = ServerTestData.createOverlandMapFromExcel (sd.getMapSize (), workbook);

		final FogOfWarMemory map = new FogOfWarMemory ();
		map.setMap (terrain);

		// Add tower
		for (final PlaneSvr plane : db.getPlanes ())
			terrain.getPlane ().get (plane.getPlaneNumber ()).getRow ().get (10).getCell ().get (20).getTerrainData ().setMapFeatureID
				(CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY);

		// Put 3 nodes on Arcanus - one we haven't scouted, one we have scouted and know its contents, and the last we already cleared
		// The one that we previously cleared we can walk right through and out the other side; the other two we can move onto but not past
		// Nature nodes, so forest, same as there before so we don't alter movement rates - all we alter is that we can't move through them
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

				map.getUnit ().add (their);
			}
		}

		// Create other areas
		final int [] [] [] doubleMovementDistances			= new int [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final int [] [] [] movementDirections					= new int [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final boolean [] [] [] canMoveToInOneTurn			= new boolean [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final boolean [] [] [] movingHereResultsInAttack	= new boolean [db.getPlanes ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];

		// Units that are moving - two units of high men spearmen
		// To be really precise with the data model and how units plane jump at towers, all units in towers are always set to plane 0, so this test data setup isn't entirely correct
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();

		for (int n = 1; n <= 2; n++)
		{
			final UnitHasSkill walkingSkill = new UnitHasSkill ();
			walkingSkill.setUnitSkillID ("USX01");

			nextUnitURN++;
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitURN (nextUnitURN);
			spearmen.setOwningPlayerID (2);
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
			spearmen.getUnitHasSkill ().add (walkingSkill);
			spearmen.setStatus (UnitStatusID.ALIVE);

			unitStack.add (spearmen);
		}
		map.getUnit ().addAll (unitStack);

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

			map.getUnit ().add (our);

			nextUnitURN++;
			final MemoryUnit their = new MemoryUnit ();
			their.setUnitURN (nextUnitURN);
			their.setOwningPlayerID (1);
			their.setUnitLocation (new MapCoordinates3DEx (20, 9, 1));
			their.setStatus (UnitStatusID.ALIVE);

			map.getUnit ().add (their);
		}

		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		calc.calculateOverlandMovementDistances (20, 10, 1, 2, map, unitStack,
			2, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, sd, db);

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
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				{
					// Distances
					final Cell distanceCell = workbook.getSheetAt (4 + plane.getPlaneNumber ()).getRow (y + 1).getCell (x + 1);
					if (distanceCell != null)
					{
						// Impassable
						if (distanceCell.getCellType () == Cell.CELL_TYPE_BLANK)
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
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
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