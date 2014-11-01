package momime.common.database;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CombatMapLayerID;

import org.junit.Test;

/**
 * Tests the CombatMapLayerID enum
 */
public class TestCombatMapLayerID
{
	/**
	 * Tests the enums come out in the right order with the .values () method.
	 * The reason I bother to have a test for this is that the calculateDoubleMovementToEnterCombatTile absolutely relies on the .values () method returning
	 * the layers in the correct order, bottom (terrain) then middle (roads) then top (buildings).
	 * In Delphi enum values have a more obvious ordering since they can be cast to ints, but can't do that here.
	 */
	@Test
	public final void testOrdering ()
	{
		final List<CombatMapLayerID> layers = new ArrayList<CombatMapLayerID> ();
		for (final CombatMapLayerID layer : CombatMapLayerID.values ())
			layers.add (layer);
		
		assertEquals (3, layers.size ());
		assertEquals (CombatMapLayerID.TERRAIN, layers.get (0));
		assertEquals (CombatMapLayerID.ROAD, layers.get (1));
		assertEquals (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, layers.get (2));
	}
}