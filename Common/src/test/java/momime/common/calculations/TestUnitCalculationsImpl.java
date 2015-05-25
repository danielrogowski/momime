package momime.common.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileBorderBlocksMovementID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.GenerateTestData;
import momime.common.database.MovementRateRule;
import momime.common.database.RangedAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitAttributePositiveNegative;
import momime.common.database.UnitHasSkill;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.utils.CombatMapUtilsImpl;
import momime.common.utils.PlayerPickUtilsImpl;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Tests the UnitCalculationsImpl object
 */
public final class TestUnitCalculationsImpl
{
	/**
	 * Tests the calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort method
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 */
	@Test
	public final void testCalculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort () throws RecordNotFoundException
	{
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setPlayerPickUtils (new PlayerPickUtilsImpl ());
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Picks
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Do basic calc where nothing gives mag weps
		assertEquals (0, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Adamantium next to city, but we can't use it without an alchemists' guild
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID (GenerateTestData.ADAMANTIUM_ORE);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (3).setTerrainData (terrainData);

		assertEquals (0, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Alchemy retort grants us grade 1, but still can't use that adamantium
		final PlayerPick alchemy = new PlayerPick ();
		alchemy.setPickID (CommonDatabaseConstants.RETORT_ID_ALCHEMY);
		alchemy.setQuantity (1);
		picks.add (alchemy);

		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Add the wrong type of building, to prove that it doesn't help
		final MemoryBuilding sagesGuild = new MemoryBuilding ();
		sagesGuild.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		sagesGuild.setBuildingID (GenerateTestData.SAGES_GUILD);
		buildings.add (sagesGuild);

		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Add an alchemists' guild, in the wrong place
		final MemoryBuilding alchemistsGuild = new MemoryBuilding ();
		alchemistsGuild.setCityLocation (new MapCoordinates3DEx (2, 2, 1));
		alchemistsGuild.setBuildingID (GenerateTestData.ALCHEMISTS_GUILD);
		buildings.add (alchemistsGuild);

		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Move it to the right place
		alchemistsGuild.getCityLocation ().setZ (0);
		assertEquals (3, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));
	}
	
	/**
	 * Tests the calculateDoubleMovementToEnterCombatTile method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementToEnterCombatTile () throws Exception
	{
		final CommonDatabase db = GenerateTestData.createDB ();
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setCombatMapUtils (new CombatMapUtilsImpl ());		// Only search routine, easier to use the real one than mock it
		
		// Simplest test of a single grass layer
		final MomCombatTileLayer grass = new MomCombatTileLayer ();
		grass.setLayer (CombatMapLayerID.TERRAIN);
		grass.setCombatTileTypeID ("CTL01");
		
		final MomCombatTile tile = new MomCombatTile ();
		tile.getTileLayer ().add (grass);
		
		assertEquals (2, calc.calculateDoubleMovementToEnterCombatTile (tile, db));
		
		// If its off the map edge, its automatically impassable
		tile.setOffMapEdge (true);
		assertEquals (-1, calc.calculateDoubleMovementToEnterCombatTile (tile, db));
		tile.setOffMapEdge (false);
		
		// Borders with no blocking have no effect
		tile.getBorderID ().add ("CTB03");
		assertEquals (2, calc.calculateDoubleMovementToEnterCombatTile (tile, db));
		
		// Borders with total blocking make tile impassable
		tile.getBorderID ().set (0, "CTB02");
		assertEquals (-1, calc.calculateDoubleMovementToEnterCombatTile (tile, db));

		// Borders with edge blocking have no effect
		tile.getBorderID ().set (0, "CTB01");
		assertEquals (2, calc.calculateDoubleMovementToEnterCombatTile (tile, db));
		
		// Building makes it impassable
		final MomCombatTileLayer building = new MomCombatTileLayer ();
		building.setLayer (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES);
		building.setCombatTileTypeID ("CBL03");
		tile.getTileLayer ().add (building);
		
		assertEquals (-1, calc.calculateDoubleMovementToEnterCombatTile (tile, db));
		
		// Road doesn't prevent it from being impassable
		// Note have intentionally added the layers in the wrong order here - natural order is terrain-road-building
		// but we have terrain-building-road, to prove the routine forces the correct layer priority
		final MomCombatTileLayer road = new MomCombatTileLayer ();
		road.setLayer (CombatMapLayerID.ROAD);
		road.setCombatTileTypeID ("CRL03");
		tile.getTileLayer ().add (road);
		
		assertEquals (-1, calc.calculateDoubleMovementToEnterCombatTile (tile, db));
		
		// Change the building to a rock, which doesn't block movement, now the road works
		tile.getTileLayer ().get (1).setCombatTileTypeID ("CBL01");
		assertEquals (1, calc.calculateDoubleMovementToEnterCombatTile (tile, db));
	}
	
	/**
	 * Tests the calculateFullRangedAttackAmmo method
	 * Its a bit of a dumb test, since its only returning the value straight out of getModifiedSkillValue, but including it to be complete and as a pretest for giveUnitFullRangedAmmoAndMana
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateFullRangedAttackAmmo () throws Exception
	{
		// These are all only used for the mock so doesn't matter if there's anything in them
		final List<UnitHasSkill> skills = new ArrayList<UnitHasSkill> ();
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Test a unit with ammo
		final AvailableUnit unitWithAmmo = new AvailableUnit ();
		when (unitUtils.getModifiedSkillValue (unitWithAmmo, skills, CommonDatabaseConstants.UNIT_SKILL_ID_RANGED_ATTACK_AMMO,
			players, spells, combatAreaEffects, db)).thenReturn (8);
		assertEquals (8, calc.calculateFullRangedAttackAmmo (unitWithAmmo, skills, players, spells, combatAreaEffects, db));
		
		// Test a unit without ammo
		final AvailableUnit unitWithoutAmmo = new AvailableUnit ();
		when (unitUtils.getModifiedSkillValue (unitWithoutAmmo, skills, CommonDatabaseConstants.UNIT_SKILL_ID_RANGED_ATTACK_AMMO,
			players, spells, combatAreaEffects, db)).thenReturn (-1);
		assertEquals (-1, calc.calculateFullRangedAttackAmmo (unitWithoutAmmo, skills, players, spells, combatAreaEffects, db));
	}
	
	/**
	 * Tests the calculateManaTotal method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateManaTotal () throws Exception
	{
		// These are all only used for the mock so doesn't matter if there's anything in them
		final List<UnitHasSkill> skills = new ArrayList<UnitHasSkill> ();
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Test a non-casting unit
		final AvailableUnit nonCaster = new AvailableUnit ();
		when (unitUtils.getModifiedSkillValue (nonCaster, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT,
			players, spells, combatAreaEffects, db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (nonCaster, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO,
			players, spells, combatAreaEffects, db)).thenReturn (-1);
		assertEquals (-1, calc.calculateManaTotal (nonCaster, skills, players, spells, combatAreaEffects, db));

		// Test an archangel
		final AvailableUnit archangel = new AvailableUnit ();
		when (unitUtils.getModifiedSkillValue (archangel, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT,
			players, spells, combatAreaEffects, db)).thenReturn (40);
		when (unitUtils.getModifiedSkillValue (archangel, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO,
			players, spells, combatAreaEffects, db)).thenReturn (-1);
		assertEquals (40, calc.calculateManaTotal (archangel, skills, players, spells, combatAreaEffects, db));

		// Test a low hero, lv 3 caster skill * lv 2 (+1=3) exp * 2½ = 22½
		final ExperienceLevel level2 = new ExperienceLevel ();
		level2.setLevelNumber (2);
		
		final AvailableUnit lowHero = new AvailableUnit ();
		when (unitUtils.getModifiedSkillValue (lowHero, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT,
			players, spells, combatAreaEffects, db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (lowHero, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO,
			players, spells, combatAreaEffects, db)).thenReturn (3);
		when (unitUtils.getExperienceLevel (lowHero, true, players, combatAreaEffects, db)).thenReturn (level2);
		assertEquals (22, calc.calculateManaTotal (lowHero, skills, players, spells, combatAreaEffects, db));
		
		// Test a higher hero, lv 5 caster skill * lv 4 (+1=5) exp * 2½ = 62½, +40 from unit caster skill = 102½
		final ExperienceLevel level4 = new ExperienceLevel ();
		level4.setLevelNumber (4);
		
		final AvailableUnit highHero = new AvailableUnit ();
		when (unitUtils.getModifiedSkillValue (highHero, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT,
			players, spells, combatAreaEffects, db)).thenReturn (40);
		when (unitUtils.getModifiedSkillValue (highHero, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO,
			players, spells, combatAreaEffects, db)).thenReturn (5);
		when (unitUtils.getExperienceLevel (highHero, true, players, combatAreaEffects, db)).thenReturn (level4);
		assertEquals (102, calc.calculateManaTotal (highHero, skills, players, spells, combatAreaEffects, db));
	}
	
	/**
	 * Tests the giveUnitFullRangedAmmoAndMana method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGiveUnitFullRangedAmmoAndMana () throws Exception
	{
		// These are all only used for the mock so doesn't matter if there's anything in them
		final UnitHasSkillMergedList skills = new UnitHasSkillMergedList ();
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Test a unit that has nothing
		final MemoryUnit melee = new MemoryUnit ();
		when (unitUtils.mergeSpellEffectsIntoSkillList (spells, melee)).thenReturn (skills);
		when (unitUtils.getModifiedSkillValue (melee, skills, CommonDatabaseConstants.UNIT_SKILL_ID_RANGED_ATTACK_AMMO,
			players, spells, combatAreaEffects, db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (melee, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT,
			players, spells, combatAreaEffects, db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (melee, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO,
			players, spells, combatAreaEffects, db)).thenReturn (-1);
		calc.giveUnitFullRangedAmmoAndMana (melee, players, spells, combatAreaEffects, db);
		
		assertEquals (-1, melee.getRangedAttackAmmo ());
		assertEquals (-1, melee.getManaRemaining ());

		// Test a unit that has some of each
		final ExperienceLevel level4 = new ExperienceLevel ();
		level4.setLevelNumber (4);

		final MemoryUnit rangedCaster = new MemoryUnit ();
		when (unitUtils.mergeSpellEffectsIntoSkillList (spells, rangedCaster)).thenReturn (skills);
		when (unitUtils.getModifiedSkillValue (rangedCaster, skills, CommonDatabaseConstants.UNIT_SKILL_ID_RANGED_ATTACK_AMMO,
			players, spells, combatAreaEffects, db)).thenReturn (8);
		when (unitUtils.getModifiedSkillValue (rangedCaster, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT,
			players, spells, combatAreaEffects, db)).thenReturn (40);
		when (unitUtils.getModifiedSkillValue (rangedCaster, skills, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO,
			players, spells, combatAreaEffects, db)).thenReturn (5);
		when (unitUtils.getExperienceLevel (rangedCaster, true, players, combatAreaEffects, db)).thenReturn (level4);
		calc.giveUnitFullRangedAmmoAndMana (rangedCaster, players, spells, combatAreaEffects, db);
		
		assertEquals (8, rangedCaster.getRangedAttackAmmo ());
		assertEquals (102, rangedCaster.getManaRemaining ());
	}
	
	/**
	 * Tests the decreaseRangedAttackAmmo method
	 */
	@Test
	public final void testDecreaseRangedAttackAmmo ()
	{
		// Sample unit
		final MemoryUnit unit = new MemoryUnit ();
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Unit has ranged ammo only, e.g. ammo
		unit.setRangedAttackAmmo (8);
		calc.decreaseRangedAttackAmmo (unit);
		assertEquals (7, unit.getRangedAttackAmmo ());
		
		// Unit has both ranged ammo and mana (MoM units are actually cleverly designed to ensure this can never happen)
		unit.setManaRemaining (10);
		calc.decreaseRangedAttackAmmo (unit);
		assertEquals (6, unit.getRangedAttackAmmo ());
		assertEquals (10, unit.getManaRemaining ());
		
		// Unit has only mana
		unit.setRangedAttackAmmo (0);
		calc.decreaseRangedAttackAmmo (unit);
		assertEquals (0, unit.getRangedAttackAmmo ());
		assertEquals (7, unit.getManaRemaining ());
	}
	
	/**
	 * Tests the calculateAliveFigureCount method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateAliveFigureCount () throws Exception
	{
		// These are all only used for the mock so doesn't matter if there's anything in them
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Unit defintion
		final Unit unitDef = new Unit ();
		when (db.findUnit ("A", "calculateAliveFigureCount")).thenReturn (unitDef);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getFullFigureCount (unitDef)).thenReturn (6);
		
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);

		// Unit with 1 HP per figure at full health of 6 figures
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("A");
		when (unitUtils.getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		
		assertEquals (6, calc.calculateAliveFigureCount (unit, players, spells, combatAreaEffects, db));
		
		// Now it takes 2 hits
		unit.setDamageTaken (2);
		assertEquals (4, calc.calculateAliveFigureCount (unit, players, spells, combatAreaEffects, db));

		// Now its dead
		unit.setDamageTaken (6);
		assertEquals (0, calc.calculateAliveFigureCount (unit, players, spells, combatAreaEffects, db));

		// Now its more than dead
		unit.setDamageTaken (9);
		assertEquals (0, calc.calculateAliveFigureCount (unit, players, spells, combatAreaEffects, db));
		
		// Now it has 4 HP per figure, so 6x4=24 total damage
		when (unitUtils.getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);
		assertEquals (4, calc.calculateAliveFigureCount (unit, players, spells, combatAreaEffects, db));
		
		// With 11 dmg taken, there's still only 2 figures dead, since it rounds down
		unit.setDamageTaken (11);
		assertEquals (4, calc.calculateAliveFigureCount (unit, players, spells, combatAreaEffects, db));
		
		// With 12 dmg taken, there's 3 figures dead
		unit.setDamageTaken (12);
		assertEquals (3, calc.calculateAliveFigureCount (unit, players, spells, combatAreaEffects, db));

		// Nearly dead
		unit.setDamageTaken (23);
		assertEquals (1, calc.calculateAliveFigureCount (unit, players, spells, combatAreaEffects, db));
	}
	
	/**
	 * Tests the calculateHitPointsRemainingOfFirstFigure method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateHitPointsRemainingOfFirstFigure () throws Exception
	{
		// These are all only used for the mock so doesn't matter if there's anything in them
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Unit with 1 HP per figure at full health of 6 figures (actually nbr of figures is irrelevant)
		final MemoryUnit unit = new MemoryUnit ();
		when (unitUtils.getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);

		assertEquals (1, calc.calculateHitPointsRemainingOfFirstFigure (unit, players, spells, combatAreaEffects, db));
	
		// Taking a hit makes no difference, now we're just on the same figure, with same HP
		unit.setDamageTaken (1);
		assertEquals (1, calc.calculateHitPointsRemainingOfFirstFigure (unit, players, spells, combatAreaEffects, db));

		// Now it has 4 HP per figure
		when (unitUtils.getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);
		assertEquals (3, calc.calculateHitPointsRemainingOfFirstFigure (unit, players, spells, combatAreaEffects, db));
		
		// Take 2 more hits
		unit.setDamageTaken (3);
		assertEquals (1, calc.calculateHitPointsRemainingOfFirstFigure (unit, players, spells, combatAreaEffects, db));
		
		// 1 more hit and first figure is dead, so second on full HP
		unit.setDamageTaken (4);
		assertEquals (4, calc.calculateHitPointsRemainingOfFirstFigure (unit, players, spells, combatAreaEffects, db));

		// 2 and a quarter figures dead
		unit.setDamageTaken (9);
		assertEquals (3, calc.calculateHitPointsRemainingOfFirstFigure (unit, players, spells, combatAreaEffects, db));

		// 2 and three-quarter figures dead
		unit.setDamageTaken (11);
		assertEquals (1, calc.calculateHitPointsRemainingOfFirstFigure (unit, players, spells, combatAreaEffects, db));
	}
	
	/**
	 * Tests the canMakeRangedAttack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanMakeRangedAttack () throws Exception
	{
		// These are all only used for the mock so doesn't matter if there's anything in them
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Unit without even a ranged attack skill
		final MemoryUnit noRangedAttack = new MemoryUnit ();
		when (unitUtils.getModifiedAttributeValue (noRangedAttack, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (0);
		assertFalse (calc.canMakeRangedAttack (noRangedAttack, players, spells, combatAreaEffects, db));
		
		// Bow with no remaining ammo
		final MemoryUnit outOfAmmo = new MemoryUnit ();
		when (unitUtils.getModifiedAttributeValue (outOfAmmo, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		assertFalse (calc.canMakeRangedAttack (outOfAmmo, players, spells, combatAreaEffects, db));

		// Bow with remaining ammo
		final MemoryUnit hasAmmo = new MemoryUnit ();
		hasAmmo.setRangedAttackAmmo (1);
		when (unitUtils.getModifiedAttributeValue (hasAmmo, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		assertTrue (calc.canMakeRangedAttack (hasAmmo, players, spells, combatAreaEffects, db));
		
		// Ranged attack of unknown type with mana (maybe this should actually be an exception)
		final Unit unknownRATUnitDef = new Unit ();
		unknownRATUnitDef.setUnitID ("A");
		
		final MemoryUnit unknownRAT = new MemoryUnit ();
		unknownRAT.setUnitID (unknownRATUnitDef.getUnitID ());
		unknownRAT.setManaRemaining (3);
		when (unitUtils.getModifiedAttributeValue (unknownRAT, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		when (db.findUnit (unknownRATUnitDef.getUnitID (), "canMakeRangedAttack")).thenReturn (unknownRATUnitDef);
		assertFalse (calc.canMakeRangedAttack (unknownRAT, players, spells, combatAreaEffects, db));

		// Phys ranged attack with mana
		final RangedAttackType physRAT = new RangedAttackType ();
		physRAT.setRangedAttackTypeID ("Y");
		
		final Unit physRATUnitDef = new Unit ();
		physRATUnitDef.setUnitID ("B");
		physRATUnitDef.setRangedAttackType (physRAT.getRangedAttackTypeID ());
		
		final MemoryUnit physRATUnit = new MemoryUnit ();
		physRATUnit.setUnitID (physRATUnitDef.getUnitID ());
		physRATUnit.setManaRemaining (3);
		when (unitUtils.getModifiedAttributeValue (physRATUnit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		when (db.findUnit (physRATUnitDef.getUnitID (), "canMakeRangedAttack")).thenReturn (physRATUnitDef);
		when (db.findRangedAttackType (physRAT.getRangedAttackTypeID (), "canMakeRangedAttack")).thenReturn (physRAT);
		assertFalse (calc.canMakeRangedAttack (physRATUnit, players, spells, combatAreaEffects, db));
		
		// Magic ranged attack with mana
		final RangedAttackType magRAT = new RangedAttackType ();
		magRAT.setRangedAttackTypeID ("Z");
		magRAT.setMagicRealmID ("X");
		
		final Unit magRATUnitDef = new Unit ();
		magRATUnitDef.setUnitID ("C");
		magRATUnitDef.setRangedAttackType (magRAT.getRangedAttackTypeID ());
		
		final MemoryUnit magRATUnit = new MemoryUnit ();
		magRATUnit.setUnitID (magRATUnitDef.getUnitID ());
		magRATUnit.setManaRemaining (3);
		when (unitUtils.getModifiedAttributeValue (magRATUnit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		when (db.findUnit (magRATUnitDef.getUnitID (), "canMakeRangedAttack")).thenReturn (magRATUnitDef);
		when (db.findRangedAttackType (magRAT.getRangedAttackTypeID (), "canMakeRangedAttack")).thenReturn (magRAT);
		assertTrue (calc.canMakeRangedAttack (magRATUnit, players, spells, combatAreaEffects, db));
	}
	
	/**
	 * Tests the listAllSkillsInUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListAllSkillsInUnitStack () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();

		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
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
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final List<MovementRateRule> rules = new ArrayList<MovementRateRule> ();
		for (int n = 1; n <= 2; n++)
		{
			final MovementRateRule pathfindingRule = new MovementRateRule ();
			pathfindingRule.setTileTypeID ("TT0" + n);
			pathfindingRule.setUnitStackSkillID ("US003");
			pathfindingRule.setDoubleMovement (1);
			rules.add (pathfindingRule);
		}
		
		final MovementRateRule flyingRule = new MovementRateRule ();
		flyingRule.setUnitSkillID ("US002");
		flyingRule.setDoubleMovement (2);
		rules.add (flyingRule);

		final MovementRateRule hillsRule = new MovementRateRule ();
		hillsRule.setUnitSkillID ("US001");
		hillsRule.setTileTypeID ("TT01");
		hillsRule.setDoubleMovement (4);
		rules.add (hillsRule);
		
		final MovementRateRule mountainsRule = new MovementRateRule ();
		mountainsRule.setUnitSkillID ("US001");
		mountainsRule.setTileTypeID ("TT02");
		mountainsRule.setDoubleMovement (6);
		rules.add (mountainsRule);
		
		when (db.getMovementRateRule ()).thenReturn (rules);
		
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

		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
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
	 * Tests the okToCrossCombatTileBorder method
	 * @throws RecordNotFoundException If the tile has a combat tile border ID that doesn't exist
	 */
	@Test
	public final void testOkToCrossCombatTileBorder () throws RecordNotFoundException
	{
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// One type of border that doesn't block movement, another that does
		final CombatTileBorder borderA = new CombatTileBorder ();
		borderA.setCombatTileBorderID ("A");
		borderA.setBlocksMovement (CombatTileBorderBlocksMovementID.NO);
		when (db.findCombatTileBorder (borderA.getCombatTileBorderID (), "okToCrossCombatTileBorder")).thenReturn (borderA);

		final CombatTileBorder borderB = new CombatTileBorder ();
		borderB.setCombatTileBorderID ("B");
		borderB.setBlocksMovement (CombatTileBorderBlocksMovementID.CANNOT_CROSS_SPECIFIED_BORDERS);
		when (db.findCombatTileBorder (borderB.getCombatTileBorderID (), "okToCrossCombatTileBorder")).thenReturn (borderB);

		// Combat map
		final CoordinateSystem combatMapCoordinateSystem = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (combatMapCoordinateSystem);

		final MomCombatTile tile = combatMap.getRow ().get (5).getCell ().get (10);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// No border at all
		assertTrue (calc.okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), 10, 5, 1, db));
		
		// Right direction, but a type of border that doesn't block movement
		tile.setBorderDirections ("516");
		tile.getBorderID ().add ("A");
		assertTrue (calc.okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), 10, 5, 1, db));
		
		// Add 2nd border that does block movement
		tile.getBorderID ().add ("B");
		assertFalse (calc.okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), 10, 5, 1, db));
		
		// Test all directions
		tile.setBorderDirections ("34567");
		assertTrue (calc.okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), 10, 5, 1, db));

		tile.setBorderDirections ("8");
		assertFalse (calc.okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), 10, 5, 1, db));

		tile.setBorderDirections ("1");
		assertFalse (calc.okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), 10, 5, 1, db));

		tile.setBorderDirections ("2");
		assertFalse (calc.okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), 10, 5, 1, db));
	}
}