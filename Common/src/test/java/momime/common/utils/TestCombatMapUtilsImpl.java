package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.MomCombatTileLayer;

import org.junit.Test;

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
}
