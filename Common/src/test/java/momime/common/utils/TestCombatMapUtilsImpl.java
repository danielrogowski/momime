package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CombatMapLayerID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.UnitCombatSideID;
import momime.common.messages.UnitStatusID;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CombatMapUtils class
 */
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
	 * Tests the setCombatTileTypeForLayer method where the layer already exists
	 */
	@Test
	public final void testSetCombatTileTypeForLayer_Exists ()
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
		
		// Update tile in the 2nd layer
		new CombatMapUtilsImpl ().setCombatTileTypeForLayer (tile, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, "C");
		
		// Check results
		assertEquals (2, tile.getTileLayer ().size ());
		assertEquals (CombatMapLayerID.TERRAIN, tile.getTileLayer ().get (0).getLayer ());
		assertEquals ("A", tile.getTileLayer ().get (0).getCombatTileTypeID ());
		assertEquals (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, tile.getTileLayer ().get (1).getLayer ());
		assertEquals ("C", tile.getTileLayer ().get (1).getCombatTileTypeID ());
	}

	/**
	 * Tests the setCombatTileTypeForLayer method where the layer doesn't already exists
	 */
	@Test
	public final void testSetCombatTileTypeForLayer_NotExists ()
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
		
		// Update tile in the 3rd layer
		new CombatMapUtilsImpl ().setCombatTileTypeForLayer (tile, CombatMapLayerID.ROAD, "C");
		
		// Check results
		assertEquals (3, tile.getTileLayer ().size ());
		assertEquals (CombatMapLayerID.TERRAIN, tile.getTileLayer ().get (0).getLayer ());
		assertEquals ("A", tile.getTileLayer ().get (0).getCombatTileTypeID ());
		assertEquals (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, tile.getTileLayer ().get (1).getLayer ());
		assertEquals ("B", tile.getTileLayer ().get (1).getCombatTileTypeID ());
		assertEquals (CombatMapLayerID.ROAD, tile.getTileLayer ().get (2).getLayer ());
		assertEquals ("C", tile.getTileLayer ().get (2).getCombatTileTypeID ());
	}
	
	/**
	 * Tests the determinePlayersInCombatFromLocation method
	 * @throws PlayerNotFoundException If we determine the attacking or defending player ID, but that ID then can't be found in the players list
	 */
	@Test
	public final void testDeterminePlayersInCombatFromLocation () throws PlayerNotFoundException
	{
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 10, 1);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final PlayerDescription attackerPd = new PlayerDescription ();
		attackerPd.setHuman (true);
		attackerPd.setPlayerID (3);
		final PlayerPublicDetails attacker = new PlayerPublicDetails (attackerPd, null, null);
		players.add (attacker);

		final PlayerDescription defenderPd = new PlayerDescription ();
		defenderPd.setHuman (false);
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
		units.add (unit1);
		
		// Unit at wrong place
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setUnitLocation (new MapCoordinates3DEx (16, 10, 1));
		unit2.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setOwningPlayerID (attackerPd.getPlayerID ());
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		unit2.setCombatPosition (new MapCoordinates2DEx (0, 0));
		units.add (unit2);
		
		// Doesn't have a location within the combat
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit3.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setOwningPlayerID (attackerPd.getPlayerID ());
		unit3.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit3);

		// Matches, but side isn't set
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit4.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setOwningPlayerID (attackerPd.getPlayerID ());
		unit4.setCombatPosition (new MapCoordinates2DEx (0, 0));
		units.add (unit4);

		// Set up object to test
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Neither player found so far
		final CombatPlayers result1 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players);
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
		units.add (unit5);
		
		final CombatPlayers result2 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players);
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
		units.add (unit6);
		
		final CombatPlayers result3 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players);
		assertSame (attacker, result3.getAttackingPlayer ());
		assertSame (defender, result3.getDefendingPlayer ());
		assertTrue (result3.bothFound ());
	}
	
	/**
	 * Tests the countPlayersAliveUnitsAtCombatLocation method
	 */
	@Test
	public final void testCountPlayersAliveUnitsAtCombatLocation ()
	{
		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		
		// Unit meets critera
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setOwningPlayerID (3);
		unit1.setStatus (UnitStatusID.ALIVE);
		unit1.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		units.add (unit1);
		
		// Dead
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setOwningPlayerID (3);
		unit2.setStatus (UnitStatusID.DEAD);
		unit2.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		units.add (unit2);
		
		// Wrong location
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setOwningPlayerID (3);
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		units.add (unit3);
		
		// Wrong player
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setOwningPlayerID (4);
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		units.add (unit4);
		
		// Unit meets critera
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setOwningPlayerID (3);
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		units.add (unit5);
		
		// Set up object to test
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		
		// Run test
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 10, 1);
		
		assertEquals (2, utils.countPlayersAliveUnitsAtCombatLocation (3, combatLocation, units));
	}
}