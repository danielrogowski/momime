package momime.common.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileBorderBlocksMovementID;
import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.MapFeature;
import momime.common.database.MovementRateRule;
import momime.common.database.RangedAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.database.Unit;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;
import momime.common.utils.CombatMapUtilsImpl;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;

/**
 * Tests the UnitCalculationsImpl object
 */
public final class TestUnitCalculationsImpl
{
	/**
	 * Tests the resetUnitOverlandMovement method for all players
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResetUnitOverlandMovement_AllPlayers () throws Exception
	{
		// Empty mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Other lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Create units owned by 3 players
		final UnitUtils unitUtils = mock (UnitUtils.class);
		for (int playerID = 1; playerID <= 3; playerID++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setOwningPlayerID (playerID);
			fow.getUnit ().add (spearmen);
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			when (unitUtils.expandUnitDetails (spearmen, null, null, null, players, fow, db)).thenReturn (xuSpearmen);
			when (xuSpearmen.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (1);

			final MemoryUnit hellHounds = new MemoryUnit ();
			hellHounds.setOwningPlayerID (playerID);
			fow.getUnit ().add (hellHounds);

			final ExpandedUnitDetails xuHellHounds = mock (ExpandedUnitDetails.class);
			when (unitUtils.expandUnitDetails (hellHounds, null, null, null, players, fow, db)).thenReturn (xuHellHounds);
			when (xuHellHounds.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		}

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run method
		calc.resetUnitOverlandMovement (0, players, fow, db);

		// Check results
		assertEquals (2, fow.getUnit ().get (0).getDoubleOverlandMovesLeft ());
		assertEquals (4, fow.getUnit ().get (1).getDoubleOverlandMovesLeft ());
		assertEquals (2, fow.getUnit ().get (2).getDoubleOverlandMovesLeft ());
		assertEquals (4, fow.getUnit ().get (3).getDoubleOverlandMovesLeft ());
		assertEquals (2, fow.getUnit ().get (4).getDoubleOverlandMovesLeft ());
		assertEquals (4, fow.getUnit ().get (5).getDoubleOverlandMovesLeft ());
	}

	/**
	 * Tests the resetUnitOverlandMovement method for a single player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResetUnitOverlandMovement_OnePlayer () throws Exception
	{
		// Empty mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Other lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Create units owned by 3 players
		final UnitUtils unitUtils = mock (UnitUtils.class);
		for (int playerID = 1; playerID <= 3; playerID++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setOwningPlayerID (playerID);
			fow.getUnit ().add (spearmen);
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			when (unitUtils.expandUnitDetails (spearmen, null, null, null, players, fow, db)).thenReturn (xuSpearmen);
			when (xuSpearmen.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (1);

			final MemoryUnit hellHounds = new MemoryUnit ();
			hellHounds.setOwningPlayerID (playerID);
			fow.getUnit ().add (hellHounds);

			final ExpandedUnitDetails xuHellHounds = mock (ExpandedUnitDetails.class);
			when (unitUtils.expandUnitDetails (hellHounds, null, null, null, players, fow, db)).thenReturn (xuHellHounds);
			when (xuHellHounds.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		}

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);

		// Run method
		calc.resetUnitOverlandMovement (2, players, fow, db);

		// Check results
		assertEquals (0, fow.getUnit ().get (0).getDoubleOverlandMovesLeft ());
		assertEquals (0, fow.getUnit ().get (1).getDoubleOverlandMovesLeft ());
		assertEquals (2, fow.getUnit ().get (2).getDoubleOverlandMovesLeft ());
		assertEquals (4, fow.getUnit ().get (3).getDoubleOverlandMovesLeft ());
		assertEquals (0, fow.getUnit ().get (4).getDoubleOverlandMovesLeft ());
		assertEquals (0, fow.getUnit ().get (5).getDoubleOverlandMovesLeft ());
	}

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
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final MemoryUnit u1 = new MemoryUnit ();
		u1.setStatus (UnitStatusID.ALIVE);
		u1.setOwningPlayerID (1);
		u1.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u1.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u1.setCombatSide (UnitCombatSideID.ATTACKER);
		u1.setCombatHeading (1);
		fow.getUnit ().add (u1);

		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (u1, null, null, null, players, fow, db)).thenReturn (xu1);
		when (xu1.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (1);

		// Wrong location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setOwningPlayerID (1);
		u2.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		u2.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u2.setCombatSide (UnitCombatSideID.ATTACKER);
		u2.setCombatHeading (1);
		fow.getUnit ().add (u2);

		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (u2, null, null, null, players, fow, db)).thenReturn (xu2);
		when (xu2.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (1);
		
		// No combat position
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setOwningPlayerID (1);
		u3.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u3.setCombatSide (UnitCombatSideID.ATTACKER);
		u3.setCombatHeading (1);
		fow.getUnit ().add (u3);

		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (u3, null, null, null, players, fow, db)).thenReturn (xu3);
		when (xu3.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (1);
		
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
		when (unitUtils.expandUnitDetails (u4, null, null, null, players, fow, db)).thenReturn (xu4);
		when (xu4.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (1);
		
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
		when (unitUtils.expandUnitDetails (u5, null, null, null, players, fow, db)).thenReturn (xu5);
		when (xu5.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (2);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run method
		final MapCoordinates3DEx loc = new MapCoordinates3DEx (20, 10, 1);
		calc.resetUnitCombatMovement (1, loc, players, fow, db);

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
		
		final MapFeature adamantium = new MapFeature ();
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
	 * Tests the calculateDoubleMovementToEnterCombatTile method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoubleMovementToEnterCombatTile () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatTileType tileDef = new CombatTileType ();			// Normal movement
		tileDef.setDoubleMovement (2);
		when (db.findCombatTileType ("CTL01", "calculateDoubleMovementToEnterCombatTile")).thenReturn (tileDef);

		final CombatTileType buildingDef = new CombatTileType ();		// Blocks movement
		buildingDef.setDoubleMovement (-1);
		when (db.findCombatTileType ("CBL03", "calculateDoubleMovementToEnterCombatTile")).thenReturn (buildingDef);

		final CombatTileType rockDef = new CombatTileType ();			// No effect on movement
		when (db.findCombatTileType ("CBL01", "calculateDoubleMovementToEnterCombatTile")).thenReturn (rockDef);

		final CombatTileType roadDef = new CombatTileType ();			// Cheap movement
		roadDef.setDoubleMovement (1);
		when (db.findCombatTileType ("CRL03", "calculateDoubleMovementToEnterCombatTile")).thenReturn (roadDef);
		
		final CombatTileBorder edgeBlockingBorder = new CombatTileBorder ();
		edgeBlockingBorder.setBlocksMovement (CombatTileBorderBlocksMovementID.CANNOT_CROSS_SPECIFIED_BORDERS);
		when (db.findCombatTileBorder ("CTB01", "calculateDoubleMovementToEnterCombatTile")).thenReturn (edgeBlockingBorder);
		
		final CombatTileBorder impassableBorder = new CombatTileBorder ();
		impassableBorder.setBlocksMovement (CombatTileBorderBlocksMovementID.WHOLE_TILE_IMPASSABLE);
		when (db.findCombatTileBorder ("CTB02", "calculateDoubleMovementToEnterCombatTile")).thenReturn (impassableBorder);
		
		final CombatTileBorder passableBorder = new CombatTileBorder ();
		passableBorder.setBlocksMovement (CombatTileBorderBlocksMovementID.NO);
		when (db.findCombatTileBorder ("CTB03", "calculateDoubleMovementToEnterCombatTile")).thenReturn (passableBorder);
		
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
	 * Tests the giveUnitFullRangedAmmoAndMana method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGiveUnitFullRangedAmmoAndMana () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit unitDef = new Unit ();
		for (final int count : new int [] {4, 6})
		{
			final UnitCanCast fixedSpell = new UnitCanCast ();
			fixedSpell.setNumberOfTimes (count);
			unitDef.getUnitCanCast ().add (fixedSpell);
		}

		// These are all only used for the mock so doesn't matter if there's anything in them
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Test unit
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final MemoryUnit unit = new MemoryUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
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
		calc.setUnitUtils (unitUtils);
		
		// Call method
		calc.giveUnitFullRangedAmmoAndMana (unit, players, fow, db);

		// Check results
		assertEquals (8, unit.getAmmoRemaining ());
		assertEquals (40, unit.getManaRemaining ());

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
		final RangedAttackType rat = new RangedAttackType ();
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Units and skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final MemoryUnit unitOne = new MemoryUnit ();
		final UnitHasSkillMergedList unitOneSkills = new UnitHasSkillMergedList ();
		for (final String unitSkillID : new String [] {"US001", "US002"})
		{
			final UnitSkillAndValue unitHasSkill = new UnitSkillAndValue ();
			unitHasSkill.setUnitSkillID (unitSkillID);
			unitOneSkills.add (unitHasSkill);
		}
		when (unitUtils.mergeSpellEffectsIntoSkillList (spells, unitOne, db)).thenReturn (unitOneSkills);

		final MemoryUnit unitTwo = new MemoryUnit ();
		final UnitHasSkillMergedList unitTwoSkills = new UnitHasSkillMergedList ();
		for (final String unitSkillID : new String [] {"US002", "US003"})
		{
			final UnitSkillAndValue unitHasSkill = new UnitSkillAndValue ();
			unitHasSkill.setUnitSkillID (unitSkillID);
			unitTwoSkills.add (unitHasSkill);
		}
		when (unitUtils.mergeSpellEffectsIntoSkillList (spells, unitTwo, db)).thenReturn (unitTwoSkills);

		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Null stack
		assertEquals (0, calc.listAllSkillsInUnitStack (null, spells, db).size ());

		// Single unit
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		units.add (unitOne);

		final List<String> unitOneResults = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (2, unitOneResults.size ());
		assertEquals ("US001", unitOneResults.get (0));
		assertEquals ("US002", unitOneResults.get (1));

		// Two units
		units.add (unitTwo);

		final List<String> unitTwoResults = calc.listAllSkillsInUnitStack (units, spells, db);
		assertEquals (3, unitTwoResults.size ());
		assertEquals ("US001", unitTwoResults.get (0));
		assertEquals ("US002", unitTwoResults.get (1));
		assertEquals ("US003", unitTwoResults.get (2));
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
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Sample unit
		final UnitSkillAndValue movementSkill = new UnitSkillAndValue ();
		movementSkill.setUnitSkillID ("US001");
		
		final UnitHasSkillMergedList movementSkills = new UnitHasSkillMergedList ();
		movementSkills.add (movementSkill);
		
		final MemoryUnit unit = new MemoryUnit ();

		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.mergeSpellEffectsIntoSkillList (spells, unit, db)).thenReturn (movementSkills);
		
		// Set up object to test
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
		final UnitSkillAndValue flightSpellEffect = new UnitSkillAndValue ();
		flightSpellEffect.setUnitSkillID ("US002");
		movementSkills.add (flightSpellEffect);

		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT01", spells, db).intValue ());
		assertEquals (1, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT02", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT03", spells, db).intValue ());
		
		// Now without the pathfinding to take preference
		unitStackSkills.clear ();
		
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT01", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT02", spells, db).intValue ());
		assertEquals (2, calc.calculateDoubleMovementToEnterTileType (unit, unitStackSkills, "TT03", spells, db).intValue ());
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
		final List<TileType> tileTypes = new ArrayList<TileType> ();
		for (int n = 1; n <= 3; n++)
		{
			final TileType tileType = new TileType ();
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
		final UnitSkillAndValue flyingUnitSkill = new UnitSkillAndValue ();
		flyingUnitSkill.setUnitSkillID ("US001");
		
		final AvailableUnit flyingUnit = new AvailableUnit ();
		flyingUnit.getUnitHasSkill ().add (flyingUnitSkill);

		final UnitSkillAndValue walkingUnitSkill = new UnitSkillAndValue ();
		walkingUnitSkill.setUnitSkillID ("US002");
		
		final AvailableUnit walkingUnit = new AvailableUnit ();
		walkingUnit.getUnitHasSkill ().add (walkingUnitSkill);
		
		// Other lists
		final List<String> unitStackSkills = new ArrayList<String> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		
		// Run checks
		assertTrue (calc.areAllTerrainTypesPassable (flyingUnit, unitStackSkills, spells, db));
		assertFalse (calc.areAllTerrainTypesPassable (walkingUnit, unitStackSkills, spells, db));
	}
	
	/**
	 * Tests the createUnitStack method on an empty unit stack
	 * @throws RecordNotFoundException If we can't find the definitions for any of the units at the location
	 * @throws MomException If selectedUnits is empty, all the units aren't at the same location, or all the units don't have the same owner 
	 */
	@Test(expected=MomException.class)
	public final void testCreateUnitStack_Empty () throws RecordNotFoundException, MomException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Unit stack
		final List<MemoryUnit> selectedUnits = new ArrayList<MemoryUnit> ();
		
		// Skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		calc.createUnitStack (selectedUnits, fogOfWarMemory, db);
	}
	
	/**
	 * Tests the createUnitStack method on a unit stack which is invalid because all the units aren't in the same place
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testCreateUnitStack_DifferentLocations () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit spearmenDef = new Unit ();
		when (db.findUnit ("UN001", "createUnitStack")).thenReturn (spearmenDef);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Unit stack
		final List<MemoryUnit> selectedUnits = new ArrayList<MemoryUnit> ();
		
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID ("UN001");
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, n));
			selectedUnits.add (spearmen);
		}

		// Skills
		final UnitHasSkillMergedList skills = new UnitHasSkillMergedList ();
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.mergeSpellEffectsIntoSkillList (anyListOf (MemoryMaintainedSpell.class), any (MemoryUnit.class), eq (db))).thenReturn (skills);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		calc.createUnitStack (selectedUnits, fogOfWarMemory, db);
	}
	
	/**
	 * Tests the createUnitStack method on a unit stack which is invalid because all the units aren't owned by the same player
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testCreateUnitStack_DifferentPlayers () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit spearmenDef = new Unit ();
		when (db.findUnit ("UN001", "createUnitStack")).thenReturn (spearmenDef);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Unit stack
		final List<MemoryUnit> selectedUnits = new ArrayList<MemoryUnit> ();
		
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID ("UN001");
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (n + 1);
			selectedUnits.add (spearmen);
		}

		// Skills
		final UnitHasSkillMergedList skills = new UnitHasSkillMergedList ();
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.mergeSpellEffectsIntoSkillList (anyListOf (MemoryMaintainedSpell.class), any (MemoryUnit.class), eq (db))).thenReturn (skills);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		calc.createUnitStack (selectedUnits, fogOfWarMemory, db);
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
		
		final Unit spearmenDef = new Unit ();
		when (db.findUnit ("UN001", "createUnitStack")).thenReturn (spearmenDef);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Unit stack
		final List<MemoryUnit> selectedUnits = new ArrayList<MemoryUnit> ();
		
		for (int n = 0; n < 2; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID ("UN001");
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			selectedUnits.add (spearmen);
		}

		// Skills
		final UnitHasSkillMergedList skills = new UnitHasSkillMergedList ();
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.mergeSpellEffectsIntoSkillList (anyListOf (MemoryMaintainedSpell.class), any (MemoryUnit.class), eq (db))).thenReturn (skills);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final UnitStack unitStack = calc.createUnitStack (selectedUnits, fogOfWarMemory, db);
		
		// Check results
		assertEquals (0, unitStack.getTransports ().size ());
		assertEquals (2, unitStack.getUnits ().size ());
		assertEquals (1, unitStack.getUnits ().get (0).getUnitURN ());
		assertEquals (2, unitStack.getUnits ().get (1).getUnitURN ());
	}

	/**
	 * Tests the createUnitStack method on a unit stack containing a trieme and a regular unit, and there's 2 other regular units at the same location
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_Transport () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Have to have a tile type, otherwise all units are treated as being all-terrain and so stay outside transports
		final TileType tileType = new TileType ();
		tileType.setTileTypeID ("TT01");

		final List<TileType> tileTypes = new ArrayList<TileType> ();
		tileTypes.add (tileType);
		
		doReturn (tileTypes).when (db).getTileTypes ();
		
		// Unit definitions
		final Unit spearmenDef = new Unit ();
		when (db.findUnit ("UN001", "createUnitStack")).thenReturn (spearmenDef);

		final Unit triemeDef = new Unit ();
		triemeDef.setTransportCapacity (2);
		when (db.findUnit ("UN002", "createUnitStack")).thenReturn (triemeDef);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Unit stack
		final List<MemoryUnit> selectedUnits = new ArrayList<MemoryUnit> ();
		
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID ("UN001");
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			
			fogOfWarMemory.getUnit ().add (spearmen);
			if (n == 0)
				selectedUnits.add (spearmen);
		}

		final MemoryUnit trieme = new MemoryUnit ();
		trieme.setUnitID ("UN002");
		trieme.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		trieme.setOwningPlayerID (1);
		trieme.setUnitURN (4);
		trieme.setStatus (UnitStatusID.ALIVE);
		
		fogOfWarMemory.getUnit ().add (trieme);
		selectedUnits.add (trieme);
		
		// Skills
		final UnitHasSkillMergedList skills = new UnitHasSkillMergedList ();
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.mergeSpellEffectsIntoSkillList (anyListOf (MemoryMaintainedSpell.class), any (MemoryUnit.class), eq (db))).thenReturn (skills);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final UnitStack unitStack = calc.createUnitStack (selectedUnits, fogOfWarMemory, db);
		
		// Check results
		assertEquals (1, unitStack.getTransports ().size ());
		assertEquals (4, unitStack.getTransports ().get (0).getUnitURN ());
		
		assertEquals (2, unitStack.getUnits ().size ());
		assertEquals (1, unitStack.getUnits ().get (0).getUnitURN ());
		assertEquals (2, unitStack.getUnits ().get (1).getUnitURN ());
	}
	
	/**
	 * Tests the createUnitStack method on a unit stack containing a trieme and 3 regular units all preselected in the stack, so they don't fit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateUnitStack_NotEnoughSpace () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Have to have a tile type, otherwise all units are treated as being all-terrain and so stay outside transports
		final TileType tileType = new TileType ();
		tileType.setTileTypeID ("TT01");

		final List<TileType> tileTypes = new ArrayList<TileType> ();
		tileTypes.add (tileType);
		
		doReturn (tileTypes).when (db).getTileTypes ();
		
		// Unit defintions
		final Unit spearmenDef = new Unit ();
		when (db.findUnit ("UN001", "createUnitStack")).thenReturn (spearmenDef);

		final Unit triemeDef = new Unit ();
		triemeDef.setTransportCapacity (2);
		when (db.findUnit ("UN002", "createUnitStack")).thenReturn (triemeDef);
		
		// Player's memory
		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		
		// Unit stack
		final List<MemoryUnit> selectedUnits = new ArrayList<MemoryUnit> ();
		
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID ("UN001");
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			
			fogOfWarMemory.getUnit ().add (spearmen);
			selectedUnits.add (spearmen);
		}

		final MemoryUnit trieme = new MemoryUnit ();
		trieme.setUnitID ("UN002");
		trieme.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		trieme.setOwningPlayerID (1);
		trieme.setUnitURN (4);
		trieme.setStatus (UnitStatusID.ALIVE);
		
		fogOfWarMemory.getUnit ().add (trieme);
		selectedUnits.add (trieme);
		
		// Skills
		final UnitHasSkillMergedList skills = new UnitHasSkillMergedList ();
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.mergeSpellEffectsIntoSkillList (anyListOf (MemoryMaintainedSpell.class), any (MemoryUnit.class), eq (db))).thenReturn (skills);
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final UnitStack unitStack = calc.createUnitStack (selectedUnits, fogOfWarMemory, db);
		
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
		final TileType tileType = new TileType ();
		tileType.setTileTypeID ("TT01");

		final List<TileType> tileTypes = new ArrayList<TileType> ();
		tileTypes.add (tileType);
		
		doReturn (tileTypes).when (db).getTileTypes ();
		
		// Unit definitions
		final Unit spearmenDef = new Unit ();
		when (db.findUnit ("UN001", "createUnitStack")).thenReturn (spearmenDef);

		final Unit triemeDef = new Unit ();
		triemeDef.setTransportCapacity (2);
		when (db.findUnit ("UN002", "createUnitStack")).thenReturn (triemeDef);

		final Unit drakeDef = new Unit ();
		when (db.findUnit ("UN003", "createUnitStack")).thenReturn (drakeDef);

		// Have to mock skill list merging
		final UnitHasSkillMergedList noSkills = new UnitHasSkillMergedList ();

		final UnitSkillAndValue drakesFly = new UnitSkillAndValue ();
		drakesFly.setUnitSkillID ("US001");
		
		final UnitHasSkillMergedList yesSkills = new UnitHasSkillMergedList ();
		yesSkills.add (drakesFly);
		
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
		
		// Unit stack
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final List<MemoryUnit> selectedUnits = new ArrayList<MemoryUnit> ();
		
		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID ("UN001");
			spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			spearmen.setOwningPlayerID (1);
			spearmen.setUnitURN (n + 1);
			spearmen.setStatus (UnitStatusID.ALIVE);
			
			fogOfWarMemory.getUnit ().add (spearmen);

			when (unitUtils.mergeSpellEffectsIntoSkillList (fogOfWarMemory.getMaintainedSpell (), spearmen, db)).thenReturn (noSkills);
		}

		for (int n = 0; n < 3; n++)
		{
			final MemoryUnit drake = new MemoryUnit ();
			drake.setUnitID ("UN003");
			drake.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
			drake.setOwningPlayerID (1);
			drake.setUnitURN (n + 4);
			drake.setStatus (UnitStatusID.ALIVE);
			
			fogOfWarMemory.getUnit ().add (drake);
			
			when (unitUtils.mergeSpellEffectsIntoSkillList (fogOfWarMemory.getMaintainedSpell (), drake, db)).thenReturn (yesSkills);
		}

		final MemoryUnit trieme = new MemoryUnit ();
		trieme.setUnitID ("UN002");
		trieme.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		trieme.setOwningPlayerID (1);
		trieme.setUnitURN (7);
		trieme.setStatus (UnitStatusID.ALIVE);
		
		fogOfWarMemory.getUnit ().add (trieme);
		selectedUnits.add (trieme);
		
		when (unitUtils.mergeSpellEffectsIntoSkillList (fogOfWarMemory.getMaintainedSpell (), trieme, db)).thenReturn (noSkills);		
		
		// Set up object to test
		final UnitCalculationsImpl calc = new UnitCalculationsImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final UnitStack unitStack = calc.createUnitStack (selectedUnits, fogOfWarMemory, db);
		
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
}