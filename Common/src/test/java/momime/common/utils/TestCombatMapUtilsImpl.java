package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.MomCombatTileLayer;
import momime.common.messages.v0_9_4.UnitCombatSideID;
import momime.common.messages.v0_9_4.UnitStatusID;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CombatMapUtils class
 */
public final class TestCombatMapUtilsImpl
{
	/**
	 * Lots of methods need a location, so this is just a helper method to create one
	 * This also helps discourage reusing the same object twice, which is dangerous because doesn't trap accidental use of .equals instead of proper comparison method
	 * @param offset If 0 generates city at standard location; set this to a non-zero value to generate a different location
	 * @return City location
	 */
	private final OverlandMapCoordinatesEx createLocation (final int offset)
	{
		final OverlandMapCoordinatesEx cityLocation = new OverlandMapCoordinatesEx ();
		cityLocation.setX (15 + offset);
		cityLocation.setY (10);
		cityLocation.setPlane (1);

		return cityLocation;
	}

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
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		
		// Combat location
		final OverlandMapCoordinatesEx combatLocation = createLocation (0);
		
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
		
		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		
		// Dead unit
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitLocation (createLocation (0));
		unit1.setCombatLocation (createLocation (0));
		unit1.setStatus (UnitStatusID.DEAD);
		unit1.setOwningPlayerID (attackerPd.getPlayerID ());
		unit1.setCombatSide (UnitCombatSideID.ATTACKER);
		unit1.setCombatPosition (new CombatMapCoordinatesEx ());
		units.add (unit1);
		
		// Unit at wrong place
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setUnitLocation (createLocation (1));
		unit2.setCombatLocation (createLocation (1));
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setOwningPlayerID (attackerPd.getPlayerID ());
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		unit2.setCombatPosition (new CombatMapCoordinatesEx ());
		units.add (unit2);
		
		// Doesn't have a location within the combat
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setUnitLocation (createLocation (0));
		unit3.setCombatLocation (createLocation (0));
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setOwningPlayerID (attackerPd.getPlayerID ());
		unit3.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit3);

		// Matches, but side isn't set
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setUnitLocation (createLocation (0));
		unit4.setCombatLocation (createLocation (0));
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setOwningPlayerID (attackerPd.getPlayerID ());
		unit4.setCombatPosition (new CombatMapCoordinatesEx ());
		units.add (unit4);
		
		// Neither player found so far
		final CombatPlayers result1 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players);
		assertNull (result1.getAttackingPlayer ());
		assertNull (result1.getDefendingPlayer ());
		assertFalse (result1.bothFound ());
		
		// Defending unit
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setUnitLocation (createLocation (0));
		unit5.setCombatLocation (createLocation (0));
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setOwningPlayerID (defenderPd.getPlayerID ());
		unit5.setCombatSide (UnitCombatSideID.DEFENDER);
		unit5.setCombatPosition (new CombatMapCoordinatesEx ());
		units.add (unit5);
		
		final CombatPlayers result2 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players);
		assertNull (result2.getAttackingPlayer ());
		assertSame (defender, result2.getDefendingPlayer ());
		assertFalse (result2.bothFound ());

		// Attacking unit - note its attacking from an adjacent location, but still gets picked up
		final MemoryUnit unit6 = new MemoryUnit ();
		unit6.setUnitLocation (createLocation (1));
		unit6.setCombatLocation (createLocation (0));
		unit6.setStatus (UnitStatusID.ALIVE);
		unit6.setOwningPlayerID (attackerPd.getPlayerID ());
		unit6.setCombatSide (UnitCombatSideID.ATTACKER);
		unit6.setCombatPosition (new CombatMapCoordinatesEx ());
		units.add (unit6);
		
		final CombatPlayers result3 = utils.determinePlayersInCombatFromLocation (combatLocation, units, players);
		assertSame (attacker, result3.getAttackingPlayer ());
		assertSame (defender, result3.getDefendingPlayer ());
		assertTrue (result3.bothFound ());
	}
}
