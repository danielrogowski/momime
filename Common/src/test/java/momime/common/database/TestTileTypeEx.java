package momime.common.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests the TileTypeEx class
 */
public final class TestTileTypeEx
{
	/**
	 * Tests the findMiniMapColour method
	 */
	@Test
	public final void testFindMiniMapColour ()
	{
		// Create some dummy entries
		final TileTypeEx tileType = new TileTypeEx ();
		
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