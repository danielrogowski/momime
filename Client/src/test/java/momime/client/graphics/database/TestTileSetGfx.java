package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;

/**
 * Tests the TileSetGfx class
 */
public final class TestTileSetGfx
{
	/**
	 * Tests the deriveAnimationFrameCountAndSpeed method when all values are consistent
	 * @throws RecordNotFoundException If one of the tiles refers to an animation that doesn't exist
	 * @throws MomException If the values aren't consistent - every animated tile under one tile set must share the same values; or we find an empty animation
	 */
	@Test
	public final void testDeriveAnimationFrameCountAndSpeed_Consistent () throws RecordNotFoundException, MomException
	{
		// Create some animations
		final AnimationGfx anim1 = new AnimationGfx ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);		// Don't need a real frame, we only care how many frames
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);

		final AnimationGfx anim2 = new AnimationGfx ();
		anim2.setAnimationSpeed (6);
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);
		
		final AnimationGfx anim3 = new AnimationGfx ();
		anim3.setAnimationSpeed (6);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);

		// Some animation that isn't used by the tileset, so its allowed to be different
		final AnimationGfx anim4 = new AnimationGfx ();
		anim4.setAnimationSpeed (7);
		anim4.getFrame ().add (null);
		anim4.getFrame ().add (null);
		
		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim2);
		when (db.findAnimation ("C", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim3);
		when (db.findAnimation ("D", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim4);

		// Create some tiles
		final SmoothedTileGfx tile1 = new SmoothedTileGfx ();
		tile1.setTileAnimation ("A");

		final SmoothedTileGfx tile2 = new SmoothedTileGfx ();
		tile2.setTileAnimation ("B");

		final SmoothedTileGfx tile3 = new SmoothedTileGfx ();
		tile3.setTileAnimation ("C");

		final SmoothedTileGfx tile4 = new SmoothedTileGfx ();
		tile4.setTileAnimation ("D");

		final SmoothedTileGfx tile5 = new SmoothedTileGfx ();
		tile5.setTileFile ("blah.png");
		
		// Set up object to test
		final TileSetGfx ts = new TileSetGfx ();
		
		final SmoothedTileTypeGfx tt1 = new SmoothedTileTypeGfx ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);
		tt1.getSmoothedTile ().add (tile5);

		final SmoothedTileTypeGfx tt2 = new SmoothedTileTypeGfx ();
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
		final AnimationGfx anim1 = new AnimationGfx ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);
		
		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);

		// Create some tiles
		final SmoothedTileGfx tile1 = new SmoothedTileGfx ();
		tile1.setTileFile ("boo.png");

		final SmoothedTileGfx tile2 = new SmoothedTileGfx ();
		tile2.setTileFile ("yawn.png");

		final SmoothedTileGfx tile3 = new SmoothedTileGfx ();
		tile3.setTileFile ("blah.png");
		
		final SmoothedTileGfx tile4 = new SmoothedTileGfx ();
		tile4.setTileAnimation ("A");
		
		// Set up object to test
		final TileSetGfx ts = new TileSetGfx ();
		
		final SmoothedTileTypeGfx tt1 = new SmoothedTileTypeGfx ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);

		final SmoothedTileTypeGfx tt2 = new SmoothedTileTypeGfx ();
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
	 * Tests the deriveAnimationFrameCountAndSpeed method where one of the animations has a different number of frames
	 * @throws RecordNotFoundException If one of the tiles refers to an animation that doesn't exist
	 * @throws MomException If the values aren't consistent - every animated tile under one tile set must share the same values; or we find an empty animation
	 */
	@Test(expected=MomException.class)
	public final void testDeriveAnimationFrameCountAndSpeed_InconsistentFrameCount () throws RecordNotFoundException, MomException
	{
		// Create some animations
		final AnimationGfx anim1 = new AnimationGfx ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);		// Don't need a real frame, we only care how many frames
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);

		final AnimationGfx anim2 = new AnimationGfx ();
		anim2.setAnimationSpeed (6);
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);		// Missing a frame = error
		
		final AnimationGfx anim3 = new AnimationGfx ();
		anim3.setAnimationSpeed (6);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);

		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim2);
		when (db.findAnimation ("C", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim3);

		// Create some tiles
		final SmoothedTileGfx tile1 = new SmoothedTileGfx ();
		tile1.setTileAnimation ("A");

		final SmoothedTileGfx tile2 = new SmoothedTileGfx ();
		tile2.setTileAnimation ("B");

		final SmoothedTileGfx tile3 = new SmoothedTileGfx ();
		tile3.setTileAnimation ("C");

		// Set up object to test
		final TileSetGfx ts = new TileSetGfx ();
		
		final SmoothedTileTypeGfx tt1 = new SmoothedTileTypeGfx ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);

		final SmoothedTileTypeGfx tt2 = new SmoothedTileTypeGfx ();
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
		final AnimationGfx anim1 = new AnimationGfx ();
		anim1.setAnimationSpeed (6);
		anim1.getFrame ().add (null);		// Don't need a real frame, we only care how many frames
		anim1.getFrame ().add (null);
		anim1.getFrame ().add (null);

		final AnimationGfx anim2 = new AnimationGfx ();
		anim2.setAnimationSpeed (5);		// Mismatching speed = error
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);
		anim2.getFrame ().add (null);
		
		final AnimationGfx anim3 = new AnimationGfx ();
		anim3.setAnimationSpeed (6);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);
		anim3.getFrame ().add (null);

		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim2);
		when (db.findAnimation ("C", "deriveAnimationFrameCountAndSpeed")).thenReturn (anim3);

		// Create some tiles
		final SmoothedTileGfx tile1 = new SmoothedTileGfx ();
		tile1.setTileAnimation ("A");

		final SmoothedTileGfx tile2 = new SmoothedTileGfx ();
		tile2.setTileAnimation ("B");

		final SmoothedTileGfx tile3 = new SmoothedTileGfx ();
		tile3.setTileAnimation ("C");

		// Set up object to test
		final TileSetGfx ts = new TileSetGfx ();
		
		final SmoothedTileTypeGfx tt1 = new SmoothedTileTypeGfx ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile2);

		final SmoothedTileTypeGfx tt2 = new SmoothedTileTypeGfx ();
		tt2.getSmoothedTile ().add (tile3);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveAnimationFrameCountAndSpeed (db);
	}
	
	/**
	 * Tests the deriveTileWidthAndHeight on a tile set that includes a tile that is neither an image nor animation
	 * @throws IOException If there is a problem loading any of the images, or we fail the consistency checks
	 */
	@Test(expected=MomException.class)
	public final void testDeriveTileWidthAndHeight_TileNeitherImageNorAnimation () throws IOException
	{
		// Create some animations
		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);

		// Create some tiles
		final SmoothedTileGfx tile1 = new SmoothedTileGfx ();
		
		// Set up object to test
		final TileSetGfx ts = new TileSetGfx ();

		final SmoothedTileTypeGfx tt1 = new SmoothedTileTypeGfx ();
		tt1.getSmoothedTile ().add (tile1);
		
		ts.getSmoothedTileType ().add (tt1);
		
		// Run method
		ts.deriveTileWidthAndHeight (db);
	}

	/**
	 * Tests the deriveTileWidthAndHeight method when all values are consistent
	 * @throws IOException If there is a problem loading any of the images, or we fail the consistency checks
	 */
	@Test
	public final void testDeriveTileWidthAndHeight_Consistent () throws IOException
	{
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage image1 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("TileImage1")).thenReturn (image1);

		final BufferedImage image2 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("TileImage2")).thenReturn (image2);
		
		// Create some animations
		final AnimationGfx anim1 = new AnimationGfx ();
		anim1.animationWidth = 10;
		anim1.animationHeight = 5;

		final AnimationGfx anim2 = new AnimationGfx ();
		anim2.animationWidth = 10;
		anim2.animationHeight = 5;

		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveTileWidthAndHeight")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveTileWidthAndHeight")).thenReturn (anim2);
		
		// Create some tiles
		final SmoothedTileGfx tile1 = new SmoothedTileGfx ();
		tile1.setTileFile ("TileImage1");

		final SmoothedTileGfx tile2 = new SmoothedTileGfx ();
		tile2.setTileFile ("TileImage2");

		final SmoothedTileGfx tile3 = new SmoothedTileGfx ();
		tile3.setTileAnimation ("A");

		final SmoothedTileGfx tile4 = new SmoothedTileGfx ();
		tile4.setTileAnimation ("B");
		
		// Set up object to test
		final TileSetGfx ts = new TileSetGfx ();
		ts.setUtils (utils);

		final SmoothedTileTypeGfx tt1 = new SmoothedTileTypeGfx ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile3);
		
		final SmoothedTileTypeGfx tt2 = new SmoothedTileTypeGfx ();
		tt2.getSmoothedTile ().add (tile2);
		tt2.getSmoothedTile ().add (tile4);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveTileWidthAndHeight (db);
	}
	
	/**
	 * Tests the deriveTileWidthAndHeight method when one image doesn't match the rest
	 * @throws IOException If there is a problem loading any of the images, or we fail the consistency checks
	 */
	@Test(expected=MomException.class)
	public final void testDeriveTileWidthAndHeight_InconsistentImage () throws IOException
	{
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage image1 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("TileImage1")).thenReturn (image1);

		final BufferedImage image2 = new BufferedImage (10, 6, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("TileImage2")).thenReturn (image2);
		
		// Create some animations
		final AnimationGfx anim1 = new AnimationGfx ();
		anim1.animationWidth = 10;
		anim1.animationHeight = 5;

		final AnimationGfx anim2 = new AnimationGfx ();
		anim2.animationWidth = 10;
		anim2.animationHeight = 5;

		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveTileWidthAndHeight")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveTileWidthAndHeight")).thenReturn (anim2);
		
		// Create some tiles
		final SmoothedTileGfx tile1 = new SmoothedTileGfx ();
		tile1.setTileFile ("TileImage1");

		final SmoothedTileGfx tile2 = new SmoothedTileGfx ();
		tile2.setTileFile ("TileImage2");

		final SmoothedTileGfx tile3 = new SmoothedTileGfx ();
		tile3.setTileAnimation ("A");

		final SmoothedTileGfx tile4 = new SmoothedTileGfx ();
		tile4.setTileAnimation ("B");
		
		// Set up object to test
		final TileSetGfx ts = new TileSetGfx ();
		ts.setUtils (utils);

		final SmoothedTileTypeGfx tt1 = new SmoothedTileTypeGfx ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile3);
		
		final SmoothedTileTypeGfx tt2 = new SmoothedTileTypeGfx ();
		tt2.getSmoothedTile ().add (tile2);
		tt2.getSmoothedTile ().add (tile4);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveTileWidthAndHeight (db);
	}
	
	/**
	 * Tests the deriveTileWidthAndHeight method when one animation doesn't match the rest
	 * @throws IOException If there is a problem loading any of the images, or we fail the consistency checks
	 */
	@Test(expected=MomException.class)
	public final void testDeriveTileWidthAndHeight_InconsistentAnimation () throws IOException
	{
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage image1 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("TileImage1")).thenReturn (image1);

		final BufferedImage image2 = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("TileImage2")).thenReturn (image2);
		
		// Create some animations
		final AnimationGfx anim1 = new AnimationGfx ();
		anim1.animationWidth = 10;
		anim1.animationHeight = 5;

		final AnimationGfx anim2 = new AnimationGfx ();
		anim2.animationWidth = 10;
		anim2.animationHeight = 6;

		final GraphicsDatabaseEx db = mock (GraphicsDatabaseEx.class);
		when (db.findAnimation ("A", "deriveTileWidthAndHeight")).thenReturn (anim1);
		when (db.findAnimation ("B", "deriveTileWidthAndHeight")).thenReturn (anim2);
		
		// Create some tiles
		final SmoothedTileGfx tile1 = new SmoothedTileGfx ();
		tile1.setTileFile ("TileImage1");

		final SmoothedTileGfx tile2 = new SmoothedTileGfx ();
		tile2.setTileFile ("TileImage2");

		final SmoothedTileGfx tile3 = new SmoothedTileGfx ();
		tile3.setTileAnimation ("A");

		final SmoothedTileGfx tile4 = new SmoothedTileGfx ();
		tile4.setTileAnimation ("B");
		
		// Set up object to test
		final TileSetGfx ts = new TileSetGfx ();
		ts.setUtils (utils);

		final SmoothedTileTypeGfx tt1 = new SmoothedTileTypeGfx ();
		tt1.getSmoothedTile ().add (tile1);
		tt1.getSmoothedTile ().add (tile3);
		
		final SmoothedTileTypeGfx tt2 = new SmoothedTileTypeGfx ();
		tt2.getSmoothedTile ().add (tile2);
		tt2.getSmoothedTile ().add (tile4);
		
		ts.getSmoothedTileType ().add (tt1);
		ts.getSmoothedTileType ().add (tt2);
		
		// Run method
		ts.deriveTileWidthAndHeight (db);
	}
	
	/**
	 * Tests the findSmoothingSystem method to find a smoothing system ID that does exist
	 * @throws MomException If there is an error in buildMaps
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindSmoothingSystem_Exists () throws MomException, RecordNotFoundException
	{
		final TileSetGfx db = new TileSetGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final SmoothingSystemGfx newSmoothingSystem = new SmoothingSystemGfx ();
			newSmoothingSystem.setSmoothingSystemID ("MB0" + n);
			db.getSmoothingSystem ().add (newSmoothingSystem);
		}

		db.buildMaps ();

		assertEquals ("MB02", db.findSmoothingSystem ("MB02", "testFindSmoothingSystem_Exists").getSmoothingSystemID ());
	}

	/**
	 * Tests the findSmoothingSystem method to find a smoothing system ID that doesn't exist
	 * @throws MomException If there is an error in buildMaps
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSmoothingSystem_NotExists () throws MomException, RecordNotFoundException
	{
		final TileSetGfx db = new TileSetGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final SmoothingSystemGfx newSmoothingSystem = new SmoothingSystemGfx ();
			newSmoothingSystem.setSmoothingSystemID ("MB0" + n);
			db.getSmoothingSystem ().add (newSmoothingSystem);
		}

		db.buildMaps ();

		db.findSmoothingSystem ("MB04", "testFindSmoothingSystem_NotExists");
	}

	/**
	 * Just to make repeatedly creating these in the below tests eaiser
	 * @param overlandMapTileTypeID Overland map tile type ID to give to the new tile type 
	 * @param planeNumber Plane number to give to the new tile type
	 * @param combatTileTypeID Combat map tile type ID to give to the new tile type
	 * @return Newly created smoothed tile type
	 */
	private final SmoothedTileTypeGfx createTileType (final String overlandMapTileTypeID, final Integer planeNumber, final String combatTileTypeID)
	{
		final SmoothedTileTypeGfx tileType = new SmoothedTileTypeGfx ();
		tileType.setTileTypeID (overlandMapTileTypeID);
		tileType.setPlaneNumber (planeNumber);
		tileType.setCombatTileTypeID (combatTileTypeID);
		return tileType;
	}
	
	/**
	 * Tests the findSmoothedTileType method looking for tile types that do exist
	 * @throws RecordNotFoundException If no matching tile type is found
	 */
	@Test
	public final void testFindSmoothedTileType_Exists () throws RecordNotFoundException
	{
		// Set up some dummy tile types
		final SmoothedTileTypeGfx tileType1 = createTileType ("TT01", 0, null);
		final SmoothedTileTypeGfx tileType2 = createTileType ("TT02", null, null);
		final SmoothedTileTypeGfx tileType3 = createTileType (null, 1, null);

		final SmoothedTileTypeGfx tileType4 = createTileType ("TT01", 0, "XX01");
		final SmoothedTileTypeGfx tileType5 = createTileType ("TT02", null, "XX01");
		final SmoothedTileTypeGfx tileType6 = createTileType (null, 1, "XX01");
		
		final TileSetGfx tileSet = new TileSetGfx ();
		tileSet.getSmoothedTileType ().add (tileType1);
		tileSet.getSmoothedTileType ().add (tileType2);
		tileSet.getSmoothedTileType ().add (tileType3);
		tileSet.getSmoothedTileType ().add (tileType4);
		tileSet.getSmoothedTileType ().add (tileType5);
		tileSet.getSmoothedTileType ().add (tileType6);
		
		// Verify some searches work
		assertSame (tileType1, tileSet.findSmoothedTileType ("TT01", 0, null));
		assertSame (tileType2, tileSet.findSmoothedTileType ("TT02", 1, null));
		assertSame (tileType3, tileSet.findSmoothedTileType ("TT03", 1, null));

		assertSame (tileType4, tileSet.findSmoothedTileType ("TT01", 0, "XX01"));
		assertSame (tileType5, tileSet.findSmoothedTileType ("TT02", 1, "XX01"));
		assertSame (tileType6, tileSet.findSmoothedTileType ("TT03", 1, "XX01"));		
	}

	/**
	 * Tests the findSmoothedTileType method looking for a tile type that doesn't exist
	 * @throws RecordNotFoundException If no matching tile type is found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSmoothedTileType_NotExists () throws RecordNotFoundException
	{
		// Set up some dummy tile types
		final SmoothedTileTypeGfx tileType1 = createTileType ("TT01", 0, null);
		final SmoothedTileTypeGfx tileType2 = createTileType ("TT02", null, null);
		final SmoothedTileTypeGfx tileType3 = createTileType (null, 1, null);

		final TileSetGfx tileSet = new TileSetGfx ();
		tileSet.getSmoothedTileType ().add (tileType1);
		tileSet.getSmoothedTileType ().add (tileType2);
		tileSet.getSmoothedTileType ().add (tileType3);
		
		// Verify exception gets thrown
		tileSet.findSmoothedTileType ("TT03", 0, null);
	}
}