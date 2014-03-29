package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfStrings;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MoveResultsInAttackTypeID;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.MemoryCombatAreaEffectUtilsImpl;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.common.utils.PlayerPickUtilsImpl;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Plane;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the MomServerUnitCalculations class
 */
public final class TestMomServerUnitCalculationsImpl
{
	/**
	 * Tests the calculateUnitScoutingRange class
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitScoutingRange () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (new PlayerServerDetails (pd, ppk, null, null, null));
		
		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		unitUtils.setPlayerPickUtils (new PlayerPickUtilsImpl ());
		unitUtils.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtilsImpl ());
		
		final MomServerUnitCalculationsImpl calc = new MomServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);

		// High men spearmen
		final MemoryUnit highMenSpearmen = unitUtils.createMemoryUnit ("UN105", 1, 0, 0, true, db);
		highMenSpearmen.setOwningPlayerID (2);
		assertEquals (1, calc.calculateUnitScoutingRange (highMenSpearmen, players, spells, combatAreaEffects, db));

		// Draconian spearmen can fly so can see range 2
		final MemoryUnit draconianSpearmen = unitUtils.createMemoryUnit ("UN067", 2, 0, 0, true, db);
		draconianSpearmen.setOwningPlayerID (2);
		assertEquals (2, calc.calculateUnitScoutingRange (draconianSpearmen, players, spells, combatAreaEffects, db));

		// Cast chaos channels flight on the high men spearmen to prove this is the same as if they had the skill naturally
		final MemoryMaintainedSpell ccFlight = new MemoryMaintainedSpell ();
		ccFlight.setUnitURN (1);
		ccFlight.setSpellID ("SP093");
		ccFlight.setUnitSkillID ("SS093C");
		spells.add (ccFlight);

		assertEquals (2, calc.calculateUnitScoutingRange (highMenSpearmen, players, spells, combatAreaEffects, db));

		// Beastmaster hero has Scouting III skill
		final MemoryUnit beastmaster = unitUtils.createMemoryUnit ("UN005", 3, 0, 0, true, db);
		beastmaster.setOwningPlayerID (2);
		assertEquals (3, calc.calculateUnitScoutingRange (beastmaster, players, spells, combatAreaEffects, db));
	}

	/**
	 * Tests the countOurAliveUnitsAtEveryLocation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCountOurAliveUnitsAtEveryLocation () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

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
			final OverlandMapCoordinatesEx u2location = new OverlandMapCoordinatesEx ();
			u2location.setX (20);
			u2location.setY (10);
			u2location.setZ (0);

			final MemoryUnit u2 = new MemoryUnit ();
			u2.setOwningPlayerID (2);
			u2.setStatus (UnitStatusID.ALIVE);
			u2.setUnitLocation (u2location);
			units.add (u2);
		}

		// 4 at second location
		for (int n = 0; n < 4; n++)
		{
			final OverlandMapCoordinatesEx u2location = new OverlandMapCoordinatesEx ();
			u2location.setX (30);
			u2location.setY (20);
			u2location.setZ (1);

			final MemoryUnit u2 = new MemoryUnit ();
			u2.setOwningPlayerID (2);
			u2.setStatus (UnitStatusID.ALIVE);
			u2.setUnitLocation (u2location);
			units.add (u2);
		}

		// Wrong player
		final OverlandMapCoordinatesEx u2location = new OverlandMapCoordinatesEx ();
		u2location.setX (20);
		u2location.setY (10);
		u2location.setZ (0);

		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (3);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (u2location);
		units.add (u2);

		// Null status
		final OverlandMapCoordinatesEx u3location = new OverlandMapCoordinatesEx ();
		u3location.setX (20);
		u3location.setY (10);
		u3location.setZ (0);

		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (2);
		u3.setUnitLocation (u3location);
		units.add (u3);

		// Unit is dead
		final OverlandMapCoordinatesEx u4location = new OverlandMapCoordinatesEx ();
		u4location.setX (20);
		u4location.setY (10);
		u4location.setZ (0);

		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (2);
		u4.setStatus (UnitStatusID.DEAD);
		u4.setUnitLocation (u4location);
		units.add (u4);

		// Set up object to test
		final MomServerUnitCalculationsImpl calc = new MomServerUnitCalculationsImpl ();
		
		// Run test
		final int [] [] [] counts = calc.countOurAliveUnitsAtEveryLocation (2, units, sys, db);

		assertEquals (3, counts [0] [10] [20]);
		assertEquals (4, counts [1] [20] [30]);

		// Reset both the locations we already checked to 0, easier to check the whole array then
		counts [0] [10] [20] = 0;
		counts [1] [20] [30] = 0;
		for (final Plane plane : db.getPlane ())
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
					assertEquals (0, counts [plane.getPlaneNumber ()] [y] [x]);
	}

	/**
	 * Tests the willMovingHereResultInAnAttack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWillMovingHereResultInAnAttack () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Remember this is all operating over a player's memory - so it has to also work where we may know nothing about the location at all, i.e. everything is nulls
		// This is a really key method so there's a ton of test conditions
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);
		final MapVolumeOfStrings nodeLairTowerKnownUnitIDs = ServerTestData.createStringsArea (sys);

		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Set up object to test
		final MomServerUnitCalculationsImpl calc = new MomServerUnitCalculationsImpl ();
		calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		calc.setUnitUtils (new UnitUtilsImpl ());
		
		// Null terrain and city data
		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Terrain data present but tile type and map feature still null
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		map.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Regular tile type
		terrainData.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Node that we haven't scouted yet
		terrainData.setTileTypeID ("TT12");

		assertEquals (MoveResultsInAttackTypeID.SCOUT, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Node that we've previously scouted
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, "UN165");

		assertEquals (MoveResultsInAttackTypeID.SCOUT, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Node that and we've previously cleared
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, "");

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Regular map feature
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, null);
		terrainData.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		terrainData.setMapFeatureID ("MF01");

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Lair that we haven't scouted yet
		terrainData.setMapFeatureID ("MF13");

		assertEquals (MoveResultsInAttackTypeID.SCOUT, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Lair that we've previously scouted
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, "UN165");

		assertEquals (MoveResultsInAttackTypeID.SCOUT, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// You can't have a lair that we've previously cleared, it would have been removed from the map when we cleared it
		// If the lair was empty at the start of the game, then we'd have removed it when we scouted it - you can't choose NOT to enter empty lairs
		// So keeping this here, but really it isn't a valid test scenario
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, "");

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we haven't scouted yet
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, null);
		terrainData.setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);

		assertEquals (MoveResultsInAttackTypeID.SCOUT, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we've previously scouted
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, "UN165");

		assertEquals (MoveResultsInAttackTypeID.SCOUT, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we've previously cleared
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, "");

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we've previously cleared but now occupied by our units
		final OverlandMapCoordinatesEx unitLocation = new OverlandMapCoordinatesEx ();
		unitLocation.setX (20);
		unitLocation.setY (10);
		unitLocation.setZ (0);

		final MemoryUnit unit = new MemoryUnit ();
		unit.setOwningPlayerID (2);
		unit.setUnitLocation (unitLocation);
		unit.setStatus (UnitStatusID.ALIVE);

		units.add (unit);

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we've previously cleared but now occupied by enemy units
		unit.setOwningPlayerID (1);

		assertEquals (MoveResultsInAttackTypeID.YES, calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we haven't scouted yet and we're on Myrror
		unit.setUnitLocation (null);

		final OverlandMapTerrainData myrrorData = new OverlandMapTerrainData ();
		myrrorData.setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (myrrorData);

		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, null);

		assertEquals (MoveResultsInAttackTypeID.SCOUT, calc.willMovingHereResultInAnAttack
			(20, 10, 1, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we've previously scouted and we're on Myrror
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, "UN165");

		assertEquals (MoveResultsInAttackTypeID.SCOUT, calc.willMovingHereResultInAnAttack
			(20, 10, 1, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we've previously cleared and we're on Myrror
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (20, "");

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 1, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we've previously cleared but now occupied by our units and we're on Myrror
		unit.setOwningPlayerID (2);
		unit.setUnitLocation (unitLocation);

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(20, 10, 1, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Tower that we've previously cleared but now occupied by enemy units and we're on Myrror
		unit.setOwningPlayerID (1);

		assertEquals (MoveResultsInAttackTypeID.YES, calc.willMovingHereResultInAnAttack
			(20, 10, 1, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Our city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);

		map.getPlane ().get (0).getRow ().get (10).getCell ().get (30).setCityData (cityData);

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(30, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Enemy city but null population
		cityData.setCityOwnerID (1);

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(30, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Enemy city but zero population
		cityData.setCityPopulation (0);

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(30, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Enemy city
		cityData.setCityPopulation (1);

		assertEquals (MoveResultsInAttackTypeID.YES, calc.willMovingHereResultInAnAttack
			(30, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Our units in open area
		unit.setOwningPlayerID (2);
		unitLocation.setX (40);

		assertEquals (MoveResultsInAttackTypeID.NO, calc.willMovingHereResultInAnAttack
			(40, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));

		// Enemy units in open area
		unit.setOwningPlayerID (1);

		assertEquals (MoveResultsInAttackTypeID.YES, calc.willMovingHereResultInAnAttack
			(40, 10, 0, 2, map, units, nodeLairTowerKnownUnitIDs, db));
	}

	/**
	 * Tests the listAllSkillsInUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListAllSkillsInUnitStack () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final MomServerUnitCalculationsImpl calc = new MomServerUnitCalculationsImpl ();
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Null stack
		assertEquals (0, calc.listAllSkillsInUnitStack (null, spells, db).size ());

		// Single unit with only skills from DB
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		units.add (unitUtils.createMemoryUnit ("UN102", 1, 0, 0, true, db));

		final List<String> longbowmen = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (4, longbowmen.size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, longbowmen.get (0));
		assertEquals ("US132", longbowmen.get (1));
		assertEquals ("US001", longbowmen.get (2));
		assertEquals ("USX01", longbowmen.get (3));

		// Two units with skills only from DB
		units.add (unitUtils.createMemoryUnit ("UN103", 2, 0, 0, true, db));

		final List<String> elvenLords = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (6, elvenLords.size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, elvenLords.get (0));
		assertEquals ("US132", elvenLords.get (1));
		assertEquals ("US001", elvenLords.get (2));
		assertEquals ("USX01", elvenLords.get (3));
		assertEquals ("US028", elvenLords.get (4));
		assertEquals ("US029", elvenLords.get (5));

		// Three units with skills only from DB
		units.add (unitUtils.createMemoryUnit ("UN156", 3, 0, 0, true, db));

		final List<String> hellHounds = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (7, hellHounds.size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, hellHounds.get (0));
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
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, withSpells.get (0));
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
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final MomServerUnitCalculationsImpl calc = new MomServerUnitCalculationsImpl ();
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		calc.setUnitUtils (unitUtils);

		// Regular spearmen unit by itself
		final List<String> unitStackSkills = new ArrayList<String> ();

		final MemoryUnit spearmen = unitUtils.createMemoryUnit ("UN105", 1, 0, 0, true, db);
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (4, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (6, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertNull (calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db));
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (0, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (4, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "FOW", spells, db).intValue ());

		// Boat by itself
		final MemoryUnit trireme = unitUtils.createMemoryUnit ("UN036", 2, 0, 0, true, db);
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db));
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db));
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db));
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db));
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "TT98", spells, db));
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "TT99", spells, db));
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "FOW", spells, db).intValue ());

		// Non-corporeal unit by itself (can't use enchanted road)
		final MemoryUnit magicSpirit = unitUtils.createMemoryUnit ("UN155", 3, 0, 0, true, db);
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "FOW", spells, db).intValue ());

		// Put a unit with path finding in the same stack - doesn't help boats or non-corporeal units
		unitStackSkills.add ("US020");

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertNull (calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db));
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (0, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "FOW", spells, db).intValue ());

		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db));
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db));
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db));
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db));
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "TT98", spells, db));
		assertNull (calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "TT99", spells, db));
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "FOW", spells, db).intValue ());

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "FOW", spells, db).intValue ());

		// Put a unit with wind walking in the same stack - does allow boats to fly, but has no effect on non-corporeal units
		// Also this makes land units worse - they're now flying, so can't use the routes located by the path finder
		unitStackSkills.add ("US023");

		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (0, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "FOW", spells, db).intValue ());

		// Flying boat!  Flying boats become like any other flying unit and so CAN use enchanted road - tested that in the original MoM too
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (0, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "FOW", spells, db).intValue ());

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "FOW", spells, db).intValue ());

		// Cast wraith form onto the spearmen, just to prove the spells list gets included into the unit's skills
		final MemoryMaintainedSpell wraithForm = new MemoryMaintainedSpell ();
		wraithForm.setUnitURN (1);
		wraithForm.setUnitSkillID ("SS181");
		spells.add (wraithForm);

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (spearmen, unitStackSkills, "FOW", spells, db).intValue ());

		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (0, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (trireme, unitStackSkills, "FOW", spells, db).intValue ());

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_HILLS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "TT98", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "TT99", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (magicSpirit, unitStackSkills, "FOW", spells, db).intValue ());
	}

	/**
	 * Tests the calculateDoubleMovementRatesForUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementRatesForUnitStack () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final MomServerUnitCalculationsImpl calc = new MomServerUnitCalculationsImpl ();
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Regular spearmen unit by itself
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		units.add (unitUtils.createMemoryUnit ("UN105", 1, 0, 0, true, db));

		final Map<String, Integer> spearmen = calc.calculateDoubleMovementRatesForUnitStack (units, spells, db);
		assertEquals (15, spearmen.size ());
		assertEquals (6, spearmen.get (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN).intValue ());
		assertEquals (6, spearmen.get (ServerDatabaseValues.VALUE_TILE_TYPE_HILLS).intValue ());
		assertEquals (4, spearmen.get (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST).intValue ());
		assertEquals (2, spearmen.get (CommonDatabaseConstants.VALUE_TILE_TYPE_DESERT).intValue ());
		assertEquals (6, spearmen.get (CommonDatabaseConstants.VALUE_TILE_TYPE_SWAMP).intValue ());
		assertEquals (2, spearmen.get (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS).intValue ());
		assertEquals (4, spearmen.get (ServerDatabaseValues.VALUE_TILE_TYPE_TUNDRA).intValue ());
		assertEquals (4, spearmen.get (ServerDatabaseValues.VALUE_TILE_TYPE_RIVER).intValue ());
		assertEquals (2, spearmen.get ("TT12").intValue ());
		assertEquals (4, spearmen.get ("TT13").intValue ());
		assertEquals (6, spearmen.get ("TT14").intValue ());
		assertEquals (4, spearmen.get (ServerDatabaseValues.VALUE_TILE_TYPE_LANDSIDE_RIVER_MOUTH).intValue ());
		assertEquals (1, spearmen.get ("TT98").intValue ());
		assertEquals (0, spearmen.get ("TT99").intValue ());
		assertEquals (4, spearmen.get ("FOW").intValue ());

		assertNull (spearmen.get (ServerDatabaseValues.VALUE_TILE_TYPE_SHORE));	// Just to prove that Map.get returns null for values not in the map

		// Put a mountaineer (any normal dwarven unit) in the stack, then whole stack gets benefit of the skill
		units.add (unitUtils.createMemoryUnit ("UN077", 2, 0, 0, true, db));

		final Map<String, Integer> mountaineer = calc.calculateDoubleMovementRatesForUnitStack (units, spells, db);
		assertEquals (15, mountaineer.size ());
		assertEquals (2, mountaineer.get (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN).intValue ());
		assertEquals (2, mountaineer.get (ServerDatabaseValues.VALUE_TILE_TYPE_HILLS).intValue ());
		assertEquals (4, mountaineer.get (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST).intValue ());
		assertEquals (2, mountaineer.get (CommonDatabaseConstants.VALUE_TILE_TYPE_DESERT).intValue ());
		assertEquals (6, mountaineer.get (CommonDatabaseConstants.VALUE_TILE_TYPE_SWAMP).intValue ());
		assertEquals (2, mountaineer.get (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS).intValue ());
		assertEquals (4, mountaineer.get (ServerDatabaseValues.VALUE_TILE_TYPE_TUNDRA).intValue ());
		assertEquals (4, mountaineer.get (ServerDatabaseValues.VALUE_TILE_TYPE_RIVER).intValue ());
		assertEquals (2, mountaineer.get ("TT12").intValue ());
		assertEquals (4, mountaineer.get ("TT13").intValue ());
		assertEquals (2, mountaineer.get ("TT14").intValue ());
		assertEquals (4, mountaineer.get (ServerDatabaseValues.VALUE_TILE_TYPE_LANDSIDE_RIVER_MOUTH).intValue ());
		assertEquals (1, mountaineer.get ("TT98").intValue ());
		assertEquals (0, mountaineer.get ("TT99").intValue ());
		assertEquals (4, mountaineer.get ("FOW").intValue ());

		// Put a boat in the stack, then because it doesn't get mountaineer, land units can't go on water... boats can't go on land... so nobody can move anywhere
		// Except amusingly that both the land units and boat think they can try to get through the fog of war
		units.add (unitUtils.createMemoryUnit ("UN036", 3, 0, 0, true, db));

		final Map<String, Integer> trireme = calc.calculateDoubleMovementRatesForUnitStack (units, spells, db);
		assertEquals (1, trireme.size ());
		assertEquals (4, trireme.get ("FOW").intValue ());

		// Cast wind walking on the spearmen, now everybody including the boat can fly
		final MemoryMaintainedSpell windWalkingSpell = new MemoryMaintainedSpell ();
		windWalkingSpell.setUnitURN (1);
		windWalkingSpell.setUnitSkillID ("SS063");
		spells.add (windWalkingSpell);

		final Map<String, Integer> windWalking = calc.calculateDoubleMovementRatesForUnitStack (units, spells, db);
		assertEquals (18, windWalking.size ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN).intValue ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_HILLS).intValue ());
		assertEquals (2, windWalking.get (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST).intValue ());
		assertEquals (2, windWalking.get (CommonDatabaseConstants.VALUE_TILE_TYPE_DESERT).intValue ());
		assertEquals (2, windWalking.get (CommonDatabaseConstants.VALUE_TILE_TYPE_SWAMP).intValue ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS).intValue ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_TUNDRA).intValue ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_SHORE).intValue ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN).intValue ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_RIVER).intValue ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_OCEANSIDE_RIVER_MOUTH).intValue ());
		assertEquals (2, windWalking.get ("TT12").intValue ());
		assertEquals (2, windWalking.get ("TT13").intValue ());
		assertEquals (2, windWalking.get ("TT14").intValue ());
		assertEquals (2, windWalking.get (ServerDatabaseValues.VALUE_TILE_TYPE_LANDSIDE_RIVER_MOUTH).intValue ());
		assertEquals (2, windWalking.get ("TT98").intValue ());
		assertEquals (0, windWalking.get ("TT99").intValue ());
		assertEquals (2, windWalking.get ("FOW").intValue ());
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
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final Workbook workbook = WorkbookFactory.create (new Object ().getClass ().getResourceAsStream ("/calculateOverlandMovementDistances.xlsx"));
		final MapVolumeOfMemoryGridCells terrain = ServerTestData.createOverlandMapFromExcel (sd.getMapSize (), workbook);

		final FogOfWarMemory map = new FogOfWarMemory ();
		map.setMap (terrain);

		// Create other areas
		final int [] [] [] doubleMovementDistances	= new int [db.getPlane ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final int [] [] [] movementDirections			= new int [db.getPlane ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final boolean [] [] [] canMoveToInOneTurn	= new boolean [db.getPlane ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];

		final MoveResultsInAttackTypeID [] [] [] movingHereResultsInAttack =
			new MoveResultsInAttackTypeID [db.getPlane ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];

		final MapVolumeOfStrings nodeLairTowerKnownUnitIDs = ServerTestData.createStringsArea (sd.getMapSize ());

		// Units that are moving - two units of high men spearmen
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();

		for (int n = 1; n <= 2; n++)
		{
			final OverlandMapCoordinatesEx spearmenLocation = new OverlandMapCoordinatesEx ();
			spearmenLocation.setX (20);
			spearmenLocation.setY (10);
			spearmenLocation.setZ (1);

			final MemoryUnit spearmen = unitUtils.createMemoryUnit ("UN105", n, 0, 0, true, db);
			spearmen.setOwningPlayerID (2);
			spearmen.setUnitLocation (spearmenLocation);

			unitStack.add (spearmen);
		}
		map.getUnit ().addAll (unitStack);

		// Set up object to test
		final MomServerUnitCalculationsImpl calc = new MomServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		calc.calculateOverlandMovementDistances (20, 10, 1, 2, map, nodeLairTowerKnownUnitIDs, unitStack,
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

		for (final Plane plane : db.getPlane ())
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				{
					if (canMoveToInOneTurn [plane.getPlaneNumber ()] [y] [x])
						countCanMoveToInOneTurn++;

					if (movingHereResultsInAttack [plane.getPlaneNumber ()] [y] [x] != MoveResultsInAttackTypeID.NO)
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
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final Workbook workbook = WorkbookFactory.create (new Object ().getClass ().getResourceAsStream ("/calculateOverlandMovementDistances.xlsx"));
		final MapVolumeOfMemoryGridCells terrain = ServerTestData.createOverlandMapFromExcel (sd.getMapSize (), workbook);

		final FogOfWarMemory map = new FogOfWarMemory ();
		map.setMap (terrain);

		// Add tower
		for (final Plane plane : db.getPlane ())
			terrain.getPlane ().get (plane.getPlaneNumber ()).getRow ().get (10).getCell ().get (20).getTerrainData ().setMapFeatureID
				(CommonDatabaseConstants.VALUE_FEATURE_CLEARED_TOWER_OF_WIZARDRY);

		// Put 3 nodes on Arcanus - one we haven't scouted, one we have scouted and know its contents, and the last we already cleared
		// The one that we previously cleared we can walk right through and out the other side; the other two we can move onto but not past
		// Nature nodes, so forest, same as there before so we don't alter movement rates - all we alter is that we can't move through them
		final MapVolumeOfStrings nodeLairTowerKnownUnitIDs = ServerTestData.createStringsArea (sd.getMapSize ());
		for (int y = 9; y <= 11; y++)
			terrain.getPlane ().get (0).getRow ().get (y).getCell ().get (18).getTerrainData ().setTileTypeID ("TT13");

		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (9).getCell ().set (18, null);
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (10).getCell ().set (18, "UN165");
		nodeLairTowerKnownUnitIDs.getPlane ().get (0).getRow ().get (11).getCell ().set (18, "");

		// Create other areas
		final int [] [] [] doubleMovementDistances	= new int [db.getPlane ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final int [] [] [] movementDirections			= new int [db.getPlane ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		final boolean [] [] [] canMoveToInOneTurn	= new boolean [db.getPlane ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];

		final MoveResultsInAttackTypeID [] [] [] movingHereResultsInAttack =
			new MoveResultsInAttackTypeID [db.getPlane ().size ()] [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];

		// Units that are moving - two units of high men spearmen
		// To be really precise with the data model and how units plane jump at towers, all units in towers are always set to plane 0, so this test data setup isn't entirely correct
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
		int nextUnitURN = 0;

		for (int n = 1; n <= 2; n++)
		{
			final OverlandMapCoordinatesEx spearmenLocation = new OverlandMapCoordinatesEx ();
			spearmenLocation.setX (20);
			spearmenLocation.setY (10);
			spearmenLocation.setZ (1);

			nextUnitURN++;
			final MemoryUnit spearmen = unitUtils.createMemoryUnit ("UN105", nextUnitURN, 0, 0, true, db);
			spearmen.setOwningPlayerID (2);
			spearmen.setUnitLocation (spearmenLocation);

			unitStack.add (spearmen);
		}
		map.getUnit ().addAll (unitStack);

		// Add 8 of our units in one location, and 8 enemy units in another location, both on Myrror
		// Our units become impassable terrain because we can't fit that many in one map cell; enemy units we can walk onto the tile but not through it
		for (int n = 1; n <= 8; n++)
		{
			final OverlandMapCoordinatesEx ourLocation = new OverlandMapCoordinatesEx ();
			ourLocation.setX (19);
			ourLocation.setY (9);
			ourLocation.setZ (1);

			nextUnitURN++;
			final MemoryUnit our = unitUtils.createMemoryUnit ("UN105", nextUnitURN, 0, 0, true, db);
			our.setOwningPlayerID (2);
			our.setUnitLocation (ourLocation);

			map.getUnit ().add (our);

			final OverlandMapCoordinatesEx theirLocation = new OverlandMapCoordinatesEx ();
			theirLocation.setX (20);
			theirLocation.setY (9);
			theirLocation.setZ (1);

			nextUnitURN++;
			final MemoryUnit their = unitUtils.createMemoryUnit ("UN105", nextUnitURN, 0, 0, true, db);
			their.setOwningPlayerID (1);
			their.setUnitLocation (theirLocation);

			map.getUnit ().add (their);
		}

		// Set up object to test
		final MomServerUnitCalculationsImpl calc = new MomServerUnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		calc.calculateOverlandMovementDistances (20, 10, 1, 2, map, nodeLairTowerKnownUnitIDs, unitStack,
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
		for (final Plane plane : db.getPlane ())
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

		for (final Plane plane : db.getPlane ())
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				{
					if (canMoveToInOneTurn [plane.getPlaneNumber ()] [y] [x])
						countCanMoveToInOneTurn++;

					if (movingHereResultsInAttack [plane.getPlaneNumber ()] [y] [x] != MoveResultsInAttackTypeID.NO)
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
