package momime.server.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CombatMapLayerID;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.UnitStatusID;

/**
 * Tests the CombatMapServerUtils class
 */
public final class TestCombatMapServerUtils
{
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
		new CombatMapServerUtilsImpl ().setCombatTileTypeForLayer (tile, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, "C");
		
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
		new CombatMapServerUtilsImpl ().setCombatTileTypeForLayer (tile, CombatMapLayerID.ROAD, "C");
		
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
		unit1.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit1.setCombatSide (UnitCombatSideID.ATTACKER);
		unit1.setCombatHeading (1);
		units.add (unit1);
		
		// Dead
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setOwningPlayerID (3);
		unit2.setStatus (UnitStatusID.DEAD);
		unit2.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit2.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		unit2.setCombatHeading (1);
		units.add (unit2);
		
		// Wrong location
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setOwningPlayerID (3);
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		unit3.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit3.setCombatSide (UnitCombatSideID.ATTACKER);
		unit3.setCombatHeading (1);
		units.add (unit3);
		
		// Wrong player
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setOwningPlayerID (4);
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit4.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit4.setCombatSide (UnitCombatSideID.ATTACKER);
		unit4.setCombatHeading (1);
		units.add (unit4);
		
		// Unit meets critera
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setOwningPlayerID (3);
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit5.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit5.setCombatSide (UnitCombatSideID.ATTACKER);
		unit5.setCombatHeading (1);
		units.add (unit5);

		// Unit in movement stack but not in the combat (land unit sitting in transport during naval combat)
		final MemoryUnit unit6 = new MemoryUnit ();
		unit6.setOwningPlayerID (3);
		unit6.setStatus (UnitStatusID.ALIVE);
		unit6.setCombatLocation (new MapCoordinates3DEx (15, 10, 1));
		unit6.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit6);
		
		// Set up object to test
		final CombatMapServerUtilsImpl utils = new CombatMapServerUtilsImpl ();
		
		// Run test
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 10, 1);
		
		assertEquals (2, utils.countPlayersAliveUnitsAtCombatLocation (3, combatLocation, units));
	}
}