package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;

import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.UnitStatusID;

/**
 * Tests the CombatMapUtils class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCombatMapUtilsImpl
{
	/**
	 * Tests the getCombatTileTypeForLayer method
	 */
	@Test
	public final void testGetCombatTileTypeForLayer ()
	{
		// Set up tile with 2 layers in it already
		final MomCombatTile tile = new MomCombatTile ();
		
		final MomCombatTileLayer layer1 = new MomCombatTileLayer ();
		layer1.setLayer (CombatMapLayerID.TERRAIN);
		layer1.setCombatTileTypeID ("A");
		tile.getTileLayer ().add (layer1);

		final MomCombatTileLayer layer2 = new MomCombatTileLayer ();
		layer2.setLayer (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES);
		layer2.setCombatTileTypeID ("B");
		tile.getTileLayer ().add (layer2);
		
		// Check all the layers
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		assertEquals ("A", utils.getCombatTileTypeForLayer (tile, CombatMapLayerID.TERRAIN));
		assertEquals ("B", utils.getCombatTileTypeForLayer (tile, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES));
		assertNull (utils.getCombatTileTypeForLayer (tile, CombatMapLayerID.ROAD));
	}

	/**
	 * Tests the determinePlayersInCombatFromLocation method
	 * @throws PlayerNotFoundException If we determine the attacking or defending player ID, but that ID then can't be found in the players list
	 */
	@Test
	public final void testDeterminePlayersInCombatFromLocation () throws PlayerNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 10, 1);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final PlayerDescription attackerPd = new PlayerDescription ();
		attackerPd.setPlayerType (PlayerType.HUMAN);
		attackerPd.setPlayerID (3);
		final PlayerPublicDetails attacker = new PlayerPublicDetails (attackerPd, null, null);
		players.add (attacker);

		final PlayerDescription defenderPd = new PlayerDescription ();
		defenderPd.setPlayerType (PlayerType.AI);
		defenderPd.setPlayerID (-1);
		final PlayerPublicDetails defender = new PlayerPublicDetails (defenderPd, null, null);
		players.add (defender);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, attackerPd.getPlayerID (), "determinePlayersInCombatFromLocation-A")).thenReturn (attacker);
		when (multiplayerSessionUtils.findPlayerWithID (players, defenderPd.getPlayerID (), "determinePlayersInCombatFromLocation-D")).thenReturn (defender);
		
		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		
		// Dead unit
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit1.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit1.setStatus (UnitStatusID.DEAD);
		unit1.setOwningPlayerID (attackerPd.getPlayerID ());
		unit1.setCombatSide (UnitCombatSideID.ATTACKER);
		unit1.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit1.setCombatHeading (1);
		units.add (unit1);
		
		// Unit at wrong place
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setUnitLocation (new MapCoordinates3DEx (16, 10, 1));
		unit2.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setOwningPlayerID (attackerPd.getPlayerID ());
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		unit2.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit2.setCombatHeading (1);
		units.add (unit2);
		
		// Doesn't have a location within the combat
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit3.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setOwningPlayerID (attackerPd.getPlayerID ());
		unit3.setCombatSide (UnitCombatSideID.ATTACKER);
		unit3.setCombatHeading (1);
		units.add (unit3);

		// Matches, but side isn't set
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit4.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setOwningPlayerID (attackerPd.getPlayerID ());
		unit4.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit4.setCombatHeading (1);
		units.add (unit4);

		// Set up object to test
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Neither player found so far
		final CombatPlayers result1 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players, db);
		assertNull (result1.getAttackingPlayer ());
		assertNull (result1.getDefendingPlayer ());
		assertFalse (result1.bothFound ());
		
		// Defending unit
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit5.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setOwningPlayerID (defenderPd.getPlayerID ());
		unit5.setCombatSide (UnitCombatSideID.DEFENDER);
		unit5.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit5.setCombatHeading (1);
		units.add (unit5);
		
		final CombatPlayers result2 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players, db);
		assertNull (result2.getAttackingPlayer ());
		assertSame (defender, result2.getDefendingPlayer ());
		assertFalse (result2.bothFound ());

		// Attacking unit - note its attacking from an adjacent location, but still gets picked up
		final MemoryUnit unit6 = new MemoryUnit ();
		unit6.setUnitLocation (new MapCoordinates3DEx (16, 10, 1));
		unit6.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit6.setStatus (UnitStatusID.ALIVE);
		unit6.setOwningPlayerID (attackerPd.getPlayerID ());
		unit6.setCombatSide (UnitCombatSideID.ATTACKER);
		unit6.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit6.setCombatHeading (1);
		units.add (unit6);
		
		final CombatPlayers result3 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players, db);
		assertSame (attacker, result3.getAttackingPlayer ());
		assertSame (defender, result3.getDefendingPlayer ());
		assertTrue (result3.bothFound ());
	}
	
	/**
	 * Tests the isWithinCityWalls method when the combat tile is within city walls
	 */
	@Test
	public final void testIsWithinCityWalls ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getCityWallsBuildingID ()).thenReturn ("BL01");
		
		final CombatTileType combatTileType = new CombatTileType ();
		combatTileType.setCombatTileTypeID ("CTT02");
		combatTileType.setInsideCity (true);
		when (db.getCombatTileType ()).thenReturn (Arrays.asList (combatTileType));
		
		// There are city walls in this combat
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (20, 10, 1), "BL01")).thenReturn (new MemoryBuilding ());
		
		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile tile = combatMap.getRow ().get (8).getCell ().get (5);
		
		int n = 0;
		for (final CombatMapLayerID layer : CombatMapLayerID.values ())
		{
			n++;
			final MomCombatTileLayer tileLayer = new MomCombatTileLayer ();
			tileLayer.setLayer (layer);
			tileLayer.setCombatTileTypeID ("CTT0" + n);
			tile.getTileLayer ().add (tileLayer);
		}
		
		// Set up object to test		
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);

		// Run method
		assertTrue (utils.isWithinCityWalls (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (5, 8), combatMap, buildings, db));
	}

	/**
	 * Tests the isWithinCityWalls method when there aren't even any city walls at the location
	 */
	@Test
	public final void testIsWithinCityWalls_NoCityWallsHere ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getCityWallsBuildingID ()).thenReturn ("BL01");
		
		// There aren't any city walls in this combat
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object to test		
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);

		// Run method
		assertFalse (utils.isWithinCityWalls (new MapCoordinates3DEx (20, 10, 1), null, null, buildings, db));
	}

	/**
	 * Tests the isWithinCityWalls method when the combat has city walls, but the location is outside of them
	 */
	@Test
	public final void testIsWithinCityWalls_OutsideCityWalls ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getCityWallsBuildingID ()).thenReturn ("BL01");
		
		final CombatTileType combatTileType = new CombatTileType ();
		combatTileType.setCombatTileTypeID ("CTT04");
		combatTileType.setInsideCity (true);
		when (db.getCombatTileType ()).thenReturn (Arrays.asList (combatTileType));
		
		// There are city walls in this combat
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (20, 10, 1), "BL01")).thenReturn (new MemoryBuilding ());
		
		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile tile = combatMap.getRow ().get (8).getCell ().get (5);
		
		int n = 0;
		for (final CombatMapLayerID layer : CombatMapLayerID.values ())
		{
			n++;
			final MomCombatTileLayer tileLayer = new MomCombatTileLayer ();
			tileLayer.setLayer (layer);
			tileLayer.setCombatTileTypeID ("CTT0" + n);
			tile.getTileLayer ().add (tileLayer);
		}
		
		// Set up object to test		
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);

		// Run method
		assertFalse (utils.isWithinCityWalls (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (5, 8), combatMap, buildings, db));
	}
	
	/**
	 * Tests the isWithinWallOfDarkness method when the combat tile is within a wall of darkness
	 */
	@Test
	public final void testIsWithinWallOfDarkness ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatTileType combatTileType = new CombatTileType ();
		combatTileType.setCombatTileTypeID ("CTT02");
		combatTileType.setInsideCity (true);
		when (db.getCombatTileType ()).thenReturn (Arrays.asList (combatTileType));
		
		// There is a wall of darkness in this combat
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		when (memoryMaintainedSpellUtils.findMaintainedSpell (spells, null, CommonDatabaseConstants.SPELL_ID_WALL_OF_DARKNESS, null, null,
			new MapCoordinates3DEx (20, 10, 1), null)).thenReturn (new MemoryMaintainedSpell ());
		
		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile tile = combatMap.getRow ().get (8).getCell ().get (5);
		
		int n = 0;
		for (final CombatMapLayerID layer : CombatMapLayerID.values ())
		{
			n++;
			final MomCombatTileLayer tileLayer = new MomCombatTileLayer ();
			tileLayer.setLayer (layer);
			tileLayer.setCombatTileTypeID ("CTT0" + n);
			tile.getTileLayer ().add (tileLayer);
		}
		
		// Set up object to test		
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);

		// Run method
		assertTrue (utils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (5, 8), combatMap, spells, db));
	}
	
	/**
	 * Tests the isWithinWallOfDarkness method when there isn't even a wall of darkness at the location
	 */
	@Test
	public final void testIsWithinWallOfDarkness_NoWallOfDarknessHere ()
	{
		// There isn't a wall of darkness in this combat
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		// Set up object to test		
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);

		// Run method
		assertFalse (utils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), null, null, spells, null));
	}

	/**
	 * Tests the isWithinWallOfDarkness method when the combat has a wall of darkness, but the location is outside of it
	 */
	@Test
	public final void testIsWithinWallOfDarkness_OutsideWallOfDarkness ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatTileType combatTileType = new CombatTileType ();
		combatTileType.setCombatTileTypeID ("CTT04");
		combatTileType.setInsideCity (true);
		when (db.getCombatTileType ()).thenReturn (Arrays.asList (combatTileType));
		
		// There is a wall of darkness in this combat
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		when (memoryMaintainedSpellUtils.findMaintainedSpell (spells, null, CommonDatabaseConstants.SPELL_ID_WALL_OF_DARKNESS, null, null,
			new MapCoordinates3DEx (20, 10, 1), null)).thenReturn (new MemoryMaintainedSpell ());
		
		// Combat map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = GenerateTestData.createCombatMap (sys);
		final MomCombatTile tile = combatMap.getRow ().get (8).getCell ().get (5);
		
		int n = 0;
		for (final CombatMapLayerID layer : CombatMapLayerID.values ())
		{
			n++;
			final MomCombatTileLayer tileLayer = new MomCombatTileLayer ();
			tileLayer.setLayer (layer);
			tileLayer.setCombatTileTypeID ("CTT0" + n);
			tile.getTileLayer ().add (tileLayer);
		}
		
		// Set up object to test		
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);

		// Run method
		assertFalse (utils.isWithinWallOfDarkness (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates2DEx (5, 8), combatMap, spells, db));
	}
}