package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.SmoothedTile;
import momime.client.graphics.database.v0_9_5.SmoothedTileType;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the TileSetEx class
 */
public final class TestTileSetEx
{
	/**
	 * Tests the deriveAnimationFrameCountAndSpeed method
	 * @throws RecordNotFoundException If one of the tiles refers to an animation that doesn't exist
	 * @throws MomException If the values aren't consistent - every animated tile under one tile set must share the same values; or we find an empty animation
	 */
	@Test
	public final void testDeriveAnimationFrameCountAndSpeed () throws RecordNotFoundException, MomException
	{
		// Create some animations
		final Animation anim1 = new Animation ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);		// Don't need a real frame, we only care how many frames
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);

		final Animation anim2 = new Animation ();
		anim2.setAnimationSpeed (6);
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);
		
		final Animation anim3 = new Animation ();
		anim3.setAnimationSpeed (6);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);

		// Some animation that isn't used by the tileset, so its allowed to be different
		final Animation anim4 = new Animation ();
		anim4.setAnimationSpeed (7);
		anim4.getFrame ().add (null);
		anim4.getFrame ().add (null);
		
		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim2);
		when (db.findAnimation ("C", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim3);
		when (db.findAnimation ("D", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim4);

		// Create some tiles
		final SmoothedTile tile1 = new SmoothedTile ();
		tile1.setTileAnimation ("A");

		final SmoothedTile tile2 = new SmoothedTile ();
		tile2.setTileAnimation ("B");

		final SmoothedTile tile3 = new SmoothedTile ();
		tile3.setTileAnimation ("C");

		final SmoothedTile tile4 = new SmoothedTile ();
		tile4.setTileAnimation ("D");

		final SmoothedTile tile5 = new SmoothedTile ();
		tile5.setTileFile ("blah.png");
		
		// Set up object to test
		final TileSetEx ts = new TileSetEx ();
		
		final SmoothedTileType tt1 = new SmoothedTileType ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);
		tt1.getSmoothedTile ().add (tile5);

		final SmoothedTileType tt2 = new SmoothedTileType ();
		tt2.getSmoothedTile ().add (tile3);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveAnimationFrameCountAndSpeed (db);
		
		// Check results
		assertEquals (6, ts.getAnimationSpeed (), 0.00001);
		assertEquals (3, ts.getAnimationFrameCount ());
	}
	
	/**
	 * Tests the deriveAnimationFrameCountAndSpeed method on a tile set containing only static images
	 * @throws RecordNotFoundException If one of the tiles refers to an animation that doesn't exist
	 * @throws MomException If the values aren't consistent - every animated tile under one tile set must share the same values; or we find an empty animation
	 */
	@Test
	public final void testDeriveAnimationFrameCountAndSpeed_AllImages () throws RecordNotFoundException, MomException
	{
		// Create an animation that doesn't get used
		final Animation anim1 = new Animation ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);
		
		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);

		// Create some tiles
		final SmoothedTile tile1 = new SmoothedTile ();
		tile1.setTileFile ("boo.png");

		final SmoothedTile tile2 = new SmoothedTile ();
		tile2.setTileFile ("yawn.png");

		final SmoothedTile tile3 = new SmoothedTile ();
		tile3.setTileFile ("blah.png");
		
		final SmoothedTile tile4 = new SmoothedTile ();
		tile4.setTileAnimation ("A");
		
		// Set up object to test
		final TileSetEx ts = new TileSetEx ();
		
		final SmoothedTileType tt1 = new SmoothedTileType ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);

		final SmoothedTileType tt2 = new SmoothedTileType ();
		tt2.getSmoothedTile ().add (tile3);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveAnimationFrameCountAndSpeed (db);
		
		// Check results
		assertNull (ts.getAnimationSpeed ());
		assertEquals (1, ts.getAnimationFrameCount ());
	}
	
	/**
	 * Tests the deriveAnimationFrameCountAndSpeed method where one of the animations has no frames
	 * @throws RecordNotFoundException If one of the tiles refers to an animation that doesn't exist
	 * @throws MomException If the values aren't consistent - every animated tile under one tile set must share the same values; or we find an empty animation
	 */
	@Test(expected=MomException.class)
	public final void testDeriveAnimationFrameCountAndSpeed_EmptyAnimation () throws RecordNotFoundException, MomException
	{
		// Create some animations
		final Animation anim1 = new Animation ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);		// Don't need a real frame, we only care how many frames
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);

		// No frames = error
		final Animation anim2 = new Animation ();
		anim2.setAnimationSpeed (6);
		
		final Animation anim3 = new Animation ();
		anim3.setAnimationSpeed (6);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);

		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim2);
		when (db.findAnimation ("C", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim3);

		// Create some tiles
		final SmoothedTile tile1 = new SmoothedTile ();
		tile1.setTileAnimation ("A");

		final SmoothedTile tile2 = new SmoothedTile ();
		tile2.setTileAnimation ("B");

		final SmoothedTile tile3 = new SmoothedTile ();
		tile3.setTileAnimation ("C");

		// Set up object to test
		final TileSetEx ts = new TileSetEx ();
		
		final SmoothedTileType tt1 = new SmoothedTileType ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);

		final SmoothedTileType tt2 = new SmoothedTileType ();
		tt2.getSmoothedTile ().add (tile3);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveAnimationFrameCountAndSpeed (db);
	}
	
	/**
	 * Tests the deriveAnimationFrameCountAndSpeed method where one of the animations has a different number of frames
	 * @throws RecordNotFoundException If one of the tiles refers to an animation that doesn't exist
	 * @throws MomException If the values aren't consistent - every animated tile under one tile set must share the same values; or we find an empty animation
	 */
	@Test(expected=MomException.class)
	public final void testDeriveAnimationFrameCountAndSpeed_InconsistentFrameCount () throws RecordNotFoundException, MomException
	{
		// Create some animations
		final Animation anim1 = new Animation ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);		// Don't need a real frame, we only care how many frames
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);

		final Animation anim2 = new Animation ();
		anim2.setAnimationSpeed (6);
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);		// Missing a frame = error
		
		final Animation anim3 = new Animation ();
		anim3.setAnimationSpeed (6);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);

		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim2);
		when (db.findAnimation ("C", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim3);

		// Create some tiles
		final SmoothedTile tile1 = new SmoothedTile ();
		tile1.setTileAnimation ("A");

		final SmoothedTile tile2 = new SmoothedTile ();
		tile2.setTileAnimation ("B");

		final SmoothedTile tile3 = new SmoothedTile ();
		tile3.setTileAnimation ("C");

		// Set up object to test
		final TileSetEx ts = new TileSetEx ();
		
		final SmoothedTileType tt1 = new SmoothedTileType ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);

		final SmoothedTileType tt2 = new SmoothedTileType ();
		tt2.getSmoothedTile ().add (tile3);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveAnimationFrameCountAndSpeed (db);
	}
	
	/**
	 * Tests the deriveAnimationFrameCountAndSpeed method where one of the animations has a different animation speed
	 * @throws RecordNotFoundException If one of the tiles refers to an animation that doesn't exist
	 * @throws MomException If the values aren't consistent - every animated tile under one tile set must share the same values; or we find an empty animation
	 */
	@Test(expected=MomException.class)
	public final void testDeriveAnimationFrameCountAndSpeed_InconsistentSpeed () throws RecordNotFoundException, MomException
	{
		// Create some animations
		final Animation anim1 = new Animation ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);		// Don't need a real frame, we only care how many frames
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);

		final Animation anim2 = new Animation ();
		anim2.setAnimationSpeed (5);		// Mismatching speed = error
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);
		
		final Animation anim3 = new Animation ();
		anim3.setAnimationSpeed (6);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);

		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim2);
		when (db.findAnimation ("C", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim3);

		// Create some tiles
		final SmoothedTile tile1 = new SmoothedTile ();
		tile1.setTileAnimation ("A");

		final SmoothedTile tile2 = new SmoothedTile ();
		tile2.setTileAnimation ("B");

		final SmoothedTile tile3 = new SmoothedTile ();
		tile3.setTileAnimation ("C");

		// Set up object to test
		final TileSetEx ts = new TileSetEx ();
		
		final SmoothedTileType tt1 = new SmoothedTileType ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);

		final SmoothedTileType tt2 = new SmoothedTileType ();
		tt2.getSmoothedTile ().add (tile3);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveAnimationFrameCountAndSpeed (db);
	}
}
