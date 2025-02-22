package momime.common.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileBorderBlocksMovementID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.MapFeatureEx;
import momime.common.database.MovementRateRule;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomCombatTile;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;
import momime.common.movement.UnitStack;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;

/**
 * Tests the UnitCalculationsImpl object
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitCalculationsImpl
{
	/**
	 * Tests the resetUnitCombatMovement method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResetUnitCombatMovement () throws Exception
	{
		// Empty mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Other lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Unit A that matches
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);

		final MemoryUnit u1 = new MemoryUnit ();
		u1.setStatus (UnitStatusID.ALIVE);
		u1.setOwningPlayerID (1);
		u1.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u1.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u1.setCombatSide (UnitCombatSideID.ATTACKER);
		u1.setCombatHeading (1);
		fow.getUnit ().add (u1);

		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (u1, null, null, null, players, fow, db)).thenReturn (xu1);
		when (xu1.getMovementSpeed ()).thenReturn (1);
		when (xu1.getControllingPlayerID ()).thenReturn (1);

		// Wrong location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setOwningPlayerID (1);
		u2.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		u2.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u2.setCombatSide (UnitCombatSideID.ATTACKER);
		u2.setCombatHeading (1);
		fow.getUnit ().add (u2);
		
		// No combat position
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setOwningPlayerID (1);
		u3.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u3.setCombatSide (UnitCombatSideID.ATTACKER);
		u3.setCombatHeading (1);
		fow.getUnit ().add (u3);

		// Wrong player
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setStatus (UnitStatusID.ALIVE);
		u4.setOwningPlayerID (2);
		u4.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u4.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u4.setCombatSide (UnitCombatSideID.ATTACKER);
		u4.setCombatHeading (1);
		fow.getUnit ().add (u4);

		final ExpandedUnitDetails xu4 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (u4, null, null, null, players, fow, db)).thenReturn (xu4);
		when (xu4.getControllingPlayerID ()).thenReturn (2);
		
		// Unit B that matches
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setStatus (UnitStatusID.ALIVE);
		u5.setOwningPlayerID (1);
		u5.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u5.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u5.setCombatSide (UnitCombatSideID.ATTACKER);
		u5.setCombatHeading (1);
		fow.getUnit ().add (u5);

		final ExpandedUnitDetails xu5 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (u5, null, null, null, players, fow, db)).thenReturn (xu5);
		when (xu5.getMovementSpeed ()).thenReturn (2);
		when (xu5.getControllingPlayerID ()).thenReturn (1);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setExpandUnitDetails (expand);
		
		// Run method
		final MapCoordinates3DEx loc = new MapCoordinates3DEx (20, 10, 1);
		calc.resetUnitCombatMovement (1, loc, new ArrayList<Integer> (), players, fow, db);

		// Check results
		assertEquals (2, u1.getDoubleCombatMovesLeft ().intValue ());
		assertNull (u2.getDoubleCombatMovesLeft ());
		assertNull (u3.getDoubleCombatMovesLeft ());
		assertNull (u4.getDoubleCombatMovesLeft ());
		assertEquals (4, u5.getDoubleCombatMovesLeft ().intValue ());
	}

	/**
	 * Tests the calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort method
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 */
	@Test
	public final void testCalculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Building otherBuilding = new Building ();
		when (db.findBuilding ("BL01", "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort")).thenReturn (otherBuilding);

		final Building wepGradeBuilding = new Building ();
		wepGradeBuilding.setBuildingMagicWeapons (1);
		when (db.findBuilding ("BL02", "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort")).thenReturn (wepGradeBuilding);
		
		final MapFeatureEx adamantium = new MapFeatureEx ();
		adamantium.setFeatureMagicWeapons (3);
		when (db.findMapFeature ("MF01", "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort")).thenReturn (adamantium);
		
		// Set up object to test
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setPlayerPickUtils (playerPickUtils);
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
		assertEquals (0, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, db));

		// Adamantium next to city, but we can't use it without an alchemists' guild
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF01");
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (3).setTerrainData (terrainData);

		assertEquals (0, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, db));

		// Alchemy retort grants us grade 1, but still can't use that adamantium
		when (playerPickUtils.getHighestWeaponGradeGrantedByPicks (picks, db)).thenReturn (1);
		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, db));

		// Add the wrong type of building, to prove that it doesn't help
		final MemoryBuilding sagesGuild = new MemoryBuilding ();
		sagesGuild.setCityLocation (new MapCoordinates3DEx (2, 2, 0));
		sagesGuild.setBuildingID ("BL01");
		buildings.add (sagesGuild);

		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, db));

		// Add an alchemists' guild, in the wrong place
		final MemoryBuilding alchemistsGuild = new MemoryBuilding ();
		alchemistsGuild.setCityLocation (new MapCoordinates3DEx (2, 2, 1));
		alchemistsGuild.setBuildingID ("BL02");
		buildings.add (alchemistsGuild);

		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, db));

		// Move it to the right place
		alchemistsGuild.getCityLocation ().setZ (0);
		assertEquals (3, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, db));
	}
	
	/**
	 * Tests the giveUnitFullRangedAmmoAndMana method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGiveUnitFullRangedAmmoAndMana () throws Exception
	{
		// Unit definition
		final UnitEx unitDef = new UnitEx ();
		for (final int count : new int [] {4, 6})
		{
			final UnitCanCast fixedSpell = new UnitCanCast ();
			fixedSpell.setNumberOfTimes (count);
			unitDef.getUnitCanCast ().add (fixedSpell);
		}
		
		// Test unit
		final MemoryUnit unit = new MemoryUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getMemoryUnit ()).thenReturn (unit);
		when (xu.getUnitDefinition ()).thenReturn (unitDef);
		when (xu.calculateFullRangedAttackAmmo ()).thenReturn (8);
		when (xu.calculateManaTotal ()).thenReturn (40);

		for (int n = 0; n < 3; n++)
		{
			final MemoryUnitHeroItemSlot slot = new MemoryUnitHeroItemSlot ();
			
			if (n != 1)
			{
				final NumberedHeroItem item = new NumberedHeroItem ();
				if (n == 2)
					item.setSpellChargeCount (3);
				
				slot.setHeroItem (item);
			}
			
			unit.getHeroItemSlot ().add (slot);
		}
		
		// Set up object to test		
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Call method
		calc.giveUnitFullRangedAmmoAndMana (xu);

		// Check results
		verify (xu).setAmmoRemaining (8);
		verify (xu).setManaRemaining (40);
		verifyNoMoreInteractions (xu);

		assertEquals (2, unit.getFixedSpellsRemaining ().size ());
		assertEquals (4, unit.getFixedSpellsRemaining ().get (0).intValue ());
		assertEquals (6, unit.getFixedSpellsRemaining ().get (1).intValue ());
		
		assertEquals (3, unit.getHeroItemSpellChargesRemaining ().size ());
		assertEquals (-1, unit.getHeroItemSpellChargesRemaining ().get (0).intValue ());
		assertEquals (-1, unit.getHeroItemSpellChargesRemaining ().get (1).intValue ());
		assertEquals (3, unit.getHeroItemSpellChargesRemaining ().get (2).intValue ());
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
		unit.setAmmoRemaining (8);
		calc.decreaseRangedAttackAmmo (unit);
		assertEquals (7, unit.getAmmoRemaining ());
		
		// Unit has both ranged ammo and mana (MoM units are actually cleverly designed to ensure this can never happen)
		unit.setManaRemaining (10);
		calc.decreaseRangedAttackAmmo (unit);
		assertEquals (6, unit.getAmmoRemaining ());
		assertEquals (10, unit.getManaRemaining ());
		
		// Unit has only mana
		unit.setAmmoRemaining (0);
		calc.decreaseRangedAttackAmmo (unit);
		assertEquals (0, unit.getAmmoRemaining ());
		assertEquals (7, unit.getManaRemaining ());
	}
	
	/**
	 * Tests the canMakeRangedAttack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanMakeRangedAttack () throws Exception
	{
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Unit without even a ranged attack skill
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);
		when (unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (false);
		assertFalse (calc.canMakeRangedAttack (unit));

		when (unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (true);
		when (unit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (0);
		assertFalse (calc.canMakeRangedAttack (unit));
		
		// Bow with no remaining ammo
		when (unit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (1);
		assertFalse (calc.canMakeRangedAttack (unit));

		// Bow with remaining ammo
		when (unit.getAmmoRemaining ()).thenReturn (1);
		assertTrue (calc.canMakeRangedAttack (unit));
		
		// Ranged attack of unknown type with mana (maybe this should actually be an exception)
		when (unit.getAmmoRemaining ()).thenReturn (0);
		when (unit.getManaRemaining ()).thenReturn (3);
		assertFalse (calc.canMakeRangedAttack (unit));

		// Phys ranged attack with mana
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		when (unit.getRangedAttackType ()).thenReturn (rat);
		assertFalse (calc.canMakeRangedAttack (unit));
		
		// Magic ranged attack with mana
		rat.setMagicRealmID ("X");
		assertTrue (calc.canMakeRangedAttack (unit));
	}
	
	/**
	 * Tests the listAllSkillsInUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListAllSkillsInUnitStack () throws Exception
	{
		// Sample units
		final ExpandedUnitDetails unitOne = mock (ExpandedUnitDetails.class);
		final Set<String> unitOneSkills = new HashSet<String> ();
		unitOneSkills.add ("US001");
		unitOneSkills.add ("US002");
		when (unitOne.listModifiedSkillIDs ()).thenReturn (unitOneSkills);

		final ExpandedUnitDetails unitTwo = mock (ExpandedUnitDetails.class);
		final Set<String> unitTwoSkills = new HashSet<String> ();
		unitTwoSkills.add ("US002");
		unitTwoSkills.add ("US003");
		when (unitTwo.listModifiedSkillIDs ()).thenReturn (unitTwoSkills);

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (mock (UnitUtils.class));
		
		// Null stack
		assertEquals (0, calc.listAllSkillsInUnitStack (null).size ());

		// Single unit
		final List<ExpandedUnitDetails> units = new ArrayList<ExpandedUnitDetails> ();
		units.add (unitOne);

		final Set<String> unitOneResults = calc.listAllSkillsInUnitStack (units);
		assertEquals (2, unitOneResults.size ());
		assertTrue (unitOneResults.contains ("US001"));
		assertTrue (unitOneResults.contains ("US002"));

		// Two units
		units.add (unitTwo);

		final Set<String> unitTwoResults = calc.listAllSkillsInUnitStack (units);
		assertEquals (3, unitTwoResults.size ());
		assertTrue (unitTwoResults.contains ("US001"));
		assertTrue (unitTwoResults.contains ("US002"));
		assertTrue (unitTwoResults.contains ("US003"));
	}

	/**
	 * Tests the calculateDoubleMovementToEnterTileType method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementToEnterTileType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up some movement rate rules to say:
		// 1) Regular units on foot (US001) can move over TT01 at cost of 4 points and TT02 at cost of 6 points
		// 2) Flying (US002) units move over everything at a cost of 2 points, including water (TT03)
		// 3) Units with a pathfinding-like skill (US003) allow their entire stack to move over any land at a cost of 1 point, but not water 
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
		
		// Sample unit
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);
		when (unit.hasModifiedSkill ("US001")).thenReturn (true);
		when (unit.hasModifiedSkill ("US002")).thenReturn (false);

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();

		// Regular walking unit can walk over the two types of land tiles, but not water
		final Set<String> unitStackSkills = new HashSet<String> ();

		assertEquals (4, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT01", db).intValue ());
		assertEquals (6, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT02", db).intValue ());
		assertNull (calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT03", db));
		
		// Stack with a pathfinding unit
		unitStackSkills.add ("US003");

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT01", db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT02", db).intValue ());
		assertNull (calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT03", db));
		
		// Cast flight spell - pathfinding takes preference, with how the demo rules above are ordered
		when (unit.hasModifiedSkill ("US002")).thenReturn (true);

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT01", db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT02", db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT03", db).intValue ());
		
		// Now without the pathfinding to take preference
		unitStackSkills.clear ();
		
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT01", db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT02", db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, null, unitStackSkills, "TT03", db).intValue ());
	}
	
	/**
	 * Tests the isTileTypeImpassable method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsTileTypeImpassable () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up some movement rate rules to say:
		// 1) Regular units on foot (US001) can move over TT01 at cost of 4 points and TT02 at cost of 6 points
		// 2) Flying (US002) units move over everything at a cost of 2 points, including water (TT03)
		// 3) Units with a pathfinding-like skill (US003) allow their entire stack to move over any land at a cost of 1 point, but not water 
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
		
		// Sample unit
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);
		when (unit.hasModifiedSkill ("US001")).thenReturn (true);
		when (unit.hasModifiedSkill ("US002")).thenReturn (false);

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();

		// Regular walking unit can walk over the two types of land tiles, but not water
		final Set<String> unitStackSkills = new HashSet<String> ();

		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT01", db));
		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT02", db));
		assertTrue (calc.isTileTypeImpassable (unit, unitStackSkills, "TT03", db));
		
		// Stack with a pathfinding unit
		unitStackSkills.add ("US003");

		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT01", db));
		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT02", db));
		assertTrue (calc.isTileTypeImpassable (unit, unitStackSkills, "TT03", db));
		
		// Cast flight spell - pathfinding takes preference, with how the demo rules above are ordered
		when (unit.hasModifiedSkill ("US002")).thenReturn (true);

		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT01", db));
		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT02", db));
		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT03", db));
		
		// Now without the pathfinding to take preference
		unitStackSkills.clear ();
		
		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT01", db));
		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT02", db));
		assertFalse (calc.isTileTypeImpassable (unit, unitStackSkills, "TT03", db));
	}
	
	/**
	 * Tests the areAllTerrainTypesPassable method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAreAllTerrainTypesPassable () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// There are 3 tile types, first two a land tiles, third is a sea tile
		final List<TileTypeEx> tileTypes = new ArrayList<TileTypeEx> ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeEx tileType = new TileTypeEx ();
			tileType.setTileTypeID ("TT0" + n);
			tileTypes.add (tileType);
		}
		doReturn (tileTypes).when (db).getTileTypes ();

		// First is a flying skill, second is a walking skill
		final List<MovementRateRule> rules = new ArrayList<MovementRateRule> ();
		for (int n = 1; n <= 3; n++)
		{
			final MovementRateRule flyingRule = new MovementRateRule ();
			flyingRule.setUnitSkillID ("US001");
			flyingRule.setTileTypeID ("TT0" + n);
			flyingRule.setDoubleMovement (1);
			rules.add (flyingRule);
			
			if (n < 3)
			{
				final MovementRateRule walkingRule = new MovementRateRule ();
				walkingRule.setUnitSkillID ("US002");
				walkingRule.setTileTypeID ("TT0" + n);
				walkingRule.setDoubleMovement (1);
				rules.add (walkingRule);
			}
		}
		when (db.getMovementRateRule ()).thenReturn (rules);
		
		// Example unit with each skill
		final ExpandedUnitDetails flyingUnit = mock (ExpandedUnitDetails.class);
		when (flyingUnit.hasModifiedSkill ("US001")).thenReturn (true);

		final ExpandedUnitDetails walkingUnit = mock (ExpandedUnitDetails.class);
		
		// Other lists
		final Set<String> unitStackSkills = new HashSet<String> ();
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Run checks
		assertTrue (calc.areAllTerrainTypesPassable (flyingUnit, unitStackSkills, db));
		assertFalse (calc.areAllTerrainTypesPassable (walkingUnit, unitStackSkills, db));
	}
	
	/**
	 * Tests the createUnitStack method on an empty unit stack
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_Empty () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Only used for the mock so doesn't matter if there's anything in here
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Unit stack
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (mock (UnitUtils.class));
		
		// Run test
		final MomException e = assertThrows (MomException.class, () ->
		{
			calc.createUnitStack (selectedUnits, players, fogOfWarMemory, db);
		});
		
		// Check for the expected error 
		assertEquals ("createUnitStack: Selected units list is empty", e.getMessage ());
	}
	
	/**
	 * Tests the createUnitStack method on a unit stack which is invalid because all the units aren't in the same place
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_DifferentLocations () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx spearmenDef = new UnitEx ();
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();

		// Only used for the mock so doesn't matter if there's anything in here
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Unit stack
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, n));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			spearmen.setUnitID ("UN001");
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);
			when (xuSpearmen.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, n));
			selectedUnits.add (xuSpearmen);
		}

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (mock (UnitUtils.class));
		
		// Run test
		final MomException e = assertThrows (MomException.class, () ->
		{
			calc.createUnitStack (selectedUnits, players, fogOfWarMemory, db);
		});
		
		// Check for the expected error 
		assertEquals ("createUnitStack: All selected units are not in the same starting location, expected (20, 10, 0) but Unit URN 0 is at (20, 10, 1)", e.getMessage ());
	}
	
	/**
	 * Tests the createUnitStack method on a unit stack which is invalid because all the units aren't owned by the same player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_DifferentPlayers () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx spearmenDef = new UnitEx ();
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Only used for the mock so doesn't matter if there's anything in here
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Unit stack
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			spearmen.setUnitID ("UN001");
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			//when (xuSpearmen.getUnit ()).thenReturn (spearmen);
			//when (xuSpearmen.getMemoryUnit ()).thenReturn (spearmen);
			when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);
			when (xuSpearmen.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
			when (xuSpearmen.getOwningPlayerID ()).thenReturn (n + 1);
			//when (xuSpearmen.getUnitURN ()).thenReturn (n + 1);
			selectedUnits.add (xuSpearmen);
		}

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (mock (UnitUtils.class));
		
		// Run test
		final MomException e = assertThrows (MomException.class, () ->
		{
			calc.createUnitStack (selectedUnits, players, fogOfWarMemory, db);
		});
		
		// Check for the expected error 
		assertEquals ("createUnitStack: All selected units are not owned by the same player", e.getMessage ());
	}
	
	/**
	 * Tests the createUnitStack method on a unit stack containing only normal units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx spearmenDef = new UnitEx ();
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Only used for the mock so doesn't matter if there's anything in here
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Unit stack
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			spearmen.setUnitID ("UN001");
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);
			when (xuSpearmen.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
			when (xuSpearmen.getOwningPlayerID ()).thenReturn (1);
			when (xuSpearmen.getUnitURN ()).thenReturn (n + 1);
			selectedUnits.add (xuSpearmen);
		}

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (mock (UnitUtils.class));
		
		// Run test
		final UnitStack unitStack = calc.createUnitStack (selectedUnits, players, fogOfWarMemory, db);
		
		// Check results
		assertEquals (0, unitStack.getTransports ().size ());
		assertEquals (2, unitStack.getUnits ().size ());
		assertEquals (1, unitStack.getUnits ().get (0).getUnitURN ());
		assertEquals (2, unitStack.getUnits ().get (1).getUnitURN ());
	}

	/**
	 * Tests the createUnitStack method on a unit stack containing a trireme and a regular unit, and there's 2 other regular units at the same location
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_Transport () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Have to have a tile type, otherwise all units are treated as being all-terrain and so stay outside transports
		final TileTypeEx tileType = new TileTypeEx ();
		tileType.setTileTypeID ("TT01");

		final List<TileTypeEx> tileTypes = new ArrayList<TileTypeEx> ();
		tileTypes.add (tileType);
		
		doReturn (tileTypes).when (db).getTileTypes ();
		
		// Unit definitions
		final UnitEx spearmenDef = new UnitEx ();
		when (db.findUnit ("UN001", "createUnitStack")).thenReturn (spearmenDef);

		final UnitEx triremeDef = new UnitEx ();
		triremeDef.setTransportCapacity (2);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Only used for the mock so doesn't matter if there's anything in here
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Unit stack
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			spearmen.setUnitID ("UN001");
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			
			if (n == 0)
			{
				when (xuSpearmen.getUnit ()).thenReturn (spearmen);
				when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);
				when (xuSpearmen.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
				when (xuSpearmen.getOwningPlayerID ()).thenReturn (1);
			}
			
			if (n < 2)
				when (xuSpearmen.getUnitURN ()).thenReturn (n + 1);
			
			if (n > 0)
				when (expand.expandUnitDetails (spearmen, null, null, null, players, fogOfWarMemory, db)).thenReturn (xuSpearmen);
			
			fogOfWarMemory.getUnit ().add (spearmen);
			if (n == 0)
				selectedUnits.add (xuSpearmen);
		}

		final MemoryUnit trireme = new MemoryUnit ();
		trireme.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		trireme.setOwningPlayerID (1);
		trireme.setUnitURN (4);
		trireme.setStatus (UnitStatusID.ALIVE);
		trireme.setUnitID ("UN002");
		
		final ExpandedUnitDetails xuTrireme = mock (ExpandedUnitDetails.class);
		when (xuTrireme.getUnit ()).thenReturn (trireme);
		when (xuTrireme.getUnitDefinition ()).thenReturn (triremeDef);
		when (xuTrireme.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
		when (xuTrireme.getOwningPlayerID ()).thenReturn (1);
		when (xuTrireme.getUnitURN ()).thenReturn (4);
		
		fogOfWarMemory.getUnit ().add (trireme);
		selectedUnits.add (xuTrireme);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setExpandUnitDetails (expand);
		
		// Run test
		final UnitStack unitStack = calc.createUnitStack (selectedUnits, players, fogOfWarMemory, db);
		
		// Check results
		assertEquals (1, unitStack.getTransports ().size ());
		assertEquals (4, unitStack.getTransports ().get (0).getUnitURN ());
		
		assertEquals (2, unitStack.getUnits ().size ());
		assertEquals (1, unitStack.getUnits ().get (0).getUnitURN ());
		assertEquals (2, unitStack.getUnits ().get (1).getUnitURN ());
	}
	
	/**
	 * Tests the createUnitStack method on a unit stack containing a trireme and 3 regular units all preselected in the stack, so they don't fit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_NotEnoughSpace () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Have to have a tile type, otherwise all units are treated as being all-terrain and so stay outside transports
		final TileTypeEx tileType = new TileTypeEx ();
		tileType.setTileTypeID ("TT01");

		final List<TileTypeEx> tileTypes = new ArrayList<TileTypeEx> ();
		tileTypes.add (tileType);
		
		doReturn (tileTypes).when (db).getTileTypes ();
		
		// Unit defintions
		final UnitEx spearmenDef = new UnitEx ();

		final UnitEx triremeDef = new UnitEx ();
		triremeDef.setTransportCapacity (2);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Only used for the mock so doesn't matter if there's anything in here
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Unit stack
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			spearmen.setUnitID ("UN001");
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			when (xuSpearmen.getUnitDefinition ()).thenReturn (spearmenDef);
			when (xuSpearmen.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
			when (xuSpearmen.getOwningPlayerID ()).thenReturn (1);
			when (xuSpearmen.getUnitURN ()).thenReturn (n + 1);
			
			//when (expand.expandUnitDetails (spearmen, null, null, null, players, fogOfWarMemory, db)).thenReturn (xuSpearmen);
			
			fogOfWarMemory.getUnit ().add (spearmen);
			selectedUnits.add (xuSpearmen);
		}

		final MemoryUnit trireme = new MemoryUnit ();
		trireme.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		trireme.setOwningPlayerID (1);
		trireme.setUnitURN (4);
		trireme.setStatus (UnitStatusID.ALIVE);
		trireme.setUnitID ("UN002");
		
		final ExpandedUnitDetails xuTrireme = mock (ExpandedUnitDetails.class);
		when (xuTrireme.getUnitDefinition ()).thenReturn (triremeDef);
		when (xuTrireme.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
		when (xuTrireme.getOwningPlayerID ()).thenReturn (1);
		when (xuTrireme.getUnitURN ()).thenReturn (4);
		
		fogOfWarMemory.getUnit ().add (trireme);
		selectedUnits.add (xuTrireme);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setExpandUnitDetails (expand);
		
		// Run test
		final UnitStack unitStack = calc.createUnitStack (selectedUnits, players, fogOfWarMemory, db);
		
		// Check results
		assertEquals (0, unitStack.getTransports ().size ());
		assertEquals (4, unitStack.getUnits ().size ());
		assertEquals (1, unitStack.getUnits ().get (0).getUnitURN ());
		assertEquals (2, unitStack.getUnits ().get (1).getUnitURN ());
		assertEquals (3, unitStack.getUnits ().get (2).getUnitURN ());
		assertEquals (4, unitStack.getUnits ().get (3).getUnitURN ());
	}

	/**
	 * Tests the createUnitStack method when we only select the transport to move, but there's 3 regular units and 3 flying units also all in the same location
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_TransportOnly () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Have to have a tile type, otherwise all units are treated as being all-terrain and so stay outside transports
		final TileTypeEx tileType = new TileTypeEx ();
		tileType.setTileTypeID ("TT01");

		final List<TileTypeEx> tileTypes = new ArrayList<TileTypeEx> ();
		tileTypes.add (tileType);
		
		doReturn (tileTypes).when (db).getTileTypes ();
		
		// Unit definitions
		final UnitEx spearmenDef = new UnitEx ();
		when (db.findUnit ("UN001", "createUnitStack")).thenReturn (spearmenDef);

		final UnitEx triremeDef = new UnitEx ();
		triremeDef.setTransportCapacity (2);

		final UnitEx drakeDef = new UnitEx ();
		when (db.findUnit ("UN003", "createUnitStack")).thenReturn (drakeDef);

		// Movement rate rules, so that the tile type is passable to the flying units but not the spearmen
		final MovementRateRule rule = new MovementRateRule ();
		rule.setTileTypeID ("TT01");
		rule.setUnitSkillID ("US001");
		rule.setDoubleMovement (2);
		
		final List<MovementRateRule> rules = new ArrayList<MovementRateRule> ();
		rules.add (rule);

		when (db.getMovementRateRule ()).thenReturn (rules);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();

		// Only used for the mock so doesn't matter if there's anything in here
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Unit stack
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			spearmen.setUnitID ("UN001");
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			
			if (n < 2)
				when (xuSpearmen.getUnitURN ()).thenReturn (n + 1);
			
			when (expand.expandUnitDetails (spearmen, null, null, null, players, fogOfWarMemory, db)).thenReturn (xuSpearmen);
			
			fogOfWarMemory.getUnit ().add (spearmen);
		}

		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit drake = new MemoryUnit ();
			drake.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			drake.setOwningPlayerID (1);
			drake.setUnitURN (n + 4);
			drake.setStatus (UnitStatusID.ALIVE);
			drake.setUnitID ("UN003");
			
			final ExpandedUnitDetails xuDrake = mock (ExpandedUnitDetails.class);
			when (xuDrake.getUnitURN ()).thenReturn (n + 4);
			when (xuDrake.hasModifiedSkill ("US001")).thenReturn (true);
			
			when (expand.expandUnitDetails (drake, null, null, null, players, fogOfWarMemory, db)).thenReturn (xuDrake);
			
			fogOfWarMemory.getUnit ().add (drake);
		}

		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		
		final MemoryUnit trireme = new MemoryUnit ();
		trireme.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		trireme.setOwningPlayerID (1);
		trireme.setUnitURN (7);
		trireme.setStatus (UnitStatusID.ALIVE);
		trireme.setUnitID ("UN002");
		
		final ExpandedUnitDetails xuTrireme = mock (ExpandedUnitDetails.class);
		when (xuTrireme.getUnit ()).thenReturn (trireme);
		when (xuTrireme.getUnitDefinition ()).thenReturn (triremeDef);
		when (xuTrireme.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
		when (xuTrireme.getOwningPlayerID ()).thenReturn (1);
		when (xuTrireme.getUnitURN ()).thenReturn (7);
		
		fogOfWarMemory.getUnit ().add (trireme);
		selectedUnits.add (xuTrireme);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setExpandUnitDetails (expand);
		
		// Run test
		final UnitStack unitStack = calc.createUnitStack (selectedUnits, players, fogOfWarMemory, db);
		
		// Check results
		assertEquals (1, unitStack.getTransports ().size ());
		assertEquals (7, unitStack.getTransports ().get (0).getUnitURN ());
		
		assertEquals (5, unitStack.getUnits ().size ());
		assertEquals (1, unitStack.getUnits ().get (0).getUnitURN ());
		assertEquals (2, unitStack.getUnits ().get (1).getUnitURN ());
		assertEquals (4, unitStack.getUnits ().get (2).getUnitURN ());
		assertEquals (5, unitStack.getUnits ().get (3).getUnitURN ());
		assertEquals (6, unitStack.getUnits ().get (4).getUnitURN ());
	}
	
	/**
	 * Tests the okToCrossCombatTileBorder method
	 * @throws RecordNotFoundException If the tile has a combat tile border ID that doesn't exist
	 */
	@Test
	public final void testOkToCrossCombatTileBorder () throws RecordNotFoundException
	{
		// Mock database
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

	/**
	 * Tests the willMovingHereResultInAnAttack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWillMovingHereResultInAnAttack () throws Exception
	{
		// Remember this is all operating over a player's memory - so it has to also work where we may know nothing about the location at all, i.e. everything is nulls
		// This is a really key method so there's a ton of test conditions
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (new UnitUtilsImpl ());
		
		// Null terrain and city data
		assertFalse (calc.willMovingHereResultInAnAttack
			(20, 10, 0, 2, map, units));

		// Terrain data present but tile type and map feature still null
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		map.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);

		assertFalse (calc.willMovingHereResultInAnAttack (20, 10, 0, 2, map, units));

		// Regular tile type
		terrainData.setTileTypeID ("TT01");

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
	 * Tests the findPreferredMovementSkillGraphics method when we find a match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindPreferredMovementSkillGraphics_Found () throws Exception
	{
		// Mock database
		final List<UnitSkillEx> skills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("US0" + n);
			
			if ((n >= 2) || (n <= 4))
				skill.setMovementIconImagePreference (5-n);  // So US02,3,4 have preference 3,2,1
			
			skills.add (skill);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitSkills ()).thenReturn (skills);
		
		// Unit to test
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);

		// Give the unit skills US02 and US03
		when (unit.hasModifiedSkill ("US01")).thenReturn (false);
		when (unit.hasModifiedSkill ("US02")).thenReturn (true);
		when (unit.hasModifiedSkill ("US03")).thenReturn (true);
		when (unit.hasModifiedSkill ("US04")).thenReturn (false);
		when (unit.hasModifiedSkill ("US05")).thenReturn (false);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Run test
		assertEquals ("US03", calc.findPreferredMovementSkillGraphics (unit, db).getUnitSkillID ());
	}

	/**
	 * Tests the findPreferredMovementSkillGraphics method when we don't find a match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindPreferredMovementSkillGraphics_NotFound () throws Exception
	{
		// Mock database
		final List<UnitSkillEx> skills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("US0" + n);
			
			if ((n >= 2) || (n <= 4))
				skill.setMovementIconImagePreference (5-n);  // So US02,3,4 have preference 3,2,1
			
			skills.add (skill);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitSkills ()).thenReturn (skills);
		
		// Unit to test
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);

		// Give the unit skills US02 and US03
		when (unit.hasModifiedSkill ("US01")).thenReturn (false);
		when (unit.hasModifiedSkill ("US02")).thenReturn (false);
		when (unit.hasModifiedSkill ("US03")).thenReturn (false);
		when (unit.hasModifiedSkill ("US04")).thenReturn (false);
		when (unit.hasModifiedSkill ("US05")).thenReturn (false);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Run test
		assertThrows (MomException.class, () ->
		{
			calc.findPreferredMovementSkillGraphics (unit, db);
		});
	}
	
	/**
	 * Tests the determineCombatActionID method when a combatActionID is defined 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineCombatActionID_Defined () throws Exception
	{
		// Mock database
		final List<UnitSkillEx> skills = new ArrayList<UnitSkillEx> ();
		final UnitSkillEx skill = new UnitSkillEx ();
		skill.setUnitSkillID ("US01");
		skill.setMovementIconImagePreference (1);
		skill.setStandActionID ("XXX");
		skill.setMoveActionID ("YYY");
		skills.add (skill);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitSkills ()).thenReturn (skills);

		// Unit to test
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);

		// Give the unit skills US01 only
		when (unit.hasModifiedSkill ("US01")).thenReturn (true);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Run test
		assertEquals ("XXX", calc.determineCombatActionID (unit, false, db));
		assertEquals ("YYY", calc.determineCombatActionID (unit, true, db));
	}
	
	/**
	 * Tests the determineCombatActionID method when a combatActionID isn't defined 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineCombatActionID_Undefined () throws Exception
	{
		// Mock database
		final List<UnitSkillEx> skills = new ArrayList<UnitSkillEx> ();
		final UnitSkillEx skill = new UnitSkillEx ();
		skill.setUnitSkillID ("US01");
		skill.setMovementIconImagePreference (1);
		skills.add (skill);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitSkills ()).thenReturn (skills);

		// Unit to test
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);

		// Give the unit skills US01 only
		when (unit.hasModifiedSkill ("US01")).thenReturn (true);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Run test
		assertThrows (MomException.class, () ->
		{
			calc.determineCombatActionID (unit, false, db);
		});
	}
}