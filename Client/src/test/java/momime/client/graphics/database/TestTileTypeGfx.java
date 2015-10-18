package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import momime.client.graphics.database.v0_9_7.TileTypeMiniMap;

/**
 * Tests the TileTypeGfx class
 */
public final class TestTileTypeGfx
{
	/**
	 * Tests the findMiniMapColour method
	 */
	@Test
	public final void testFindMiniMapColour ()
	{
		// Create some dummy entries
		final TileTypeGfx tileType = new TileTypeGfx ();
		
		int n = 0;
		for (final String c : new String [] {"FF0000", "00FF00", "0000FF"})
		{
			n++;
			
			final TileTypeMiniMap miniMap = new TileTypeMiniMap ();
			miniMap.setPlaneNumber (n);
			miniMap.setMiniMapPixelColour (c);
			
			tileType.getTileTypeMiniMap ().add (miniMap);
		}
		
		tileType.buildMap ();

		// Run tests
		assertEquals (0x00FF00, tileType.findMiniMapColour (2).intValue ());
		assertNull (tileType.findMiniMapColour (4));
	}
}