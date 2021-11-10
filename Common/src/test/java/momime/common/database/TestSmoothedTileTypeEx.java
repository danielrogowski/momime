package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.random.RandomUtils;

/**
 * Tests the SmoothedTileTypeEx class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSmoothedTileTypeEx
{
	/**
	 * Just to make the test data setup below shorter
	 * @param bitmask Smoothed bitmask to give the tile
	 * @param tileFile Pretend filename for this image
	 * @return Newly created tile
	 */
	private final SmoothedTile createSmoothedTile (final String bitmask, final String tileFile)
	{
		final SmoothedTile tile = new SmoothedTile ();
		tile.setBitmask (bitmask);
		tile.setTileFile (tileFile);
		return tile;
	}
	
	/**
	 * Tests the buildMap method
	 * @throws RecordNotFoundException If a bitmaks that the smoothing system rules say should be in the graphics XML isn't there, i.e. an image is missing
	 */
	@Test
	public final void testBuildMap () throws RecordNotFoundException
	{
		// Set up some dummy smoothed-unsmoothed maps
		final List<String> unsmoothedA = new ArrayList<String> ();
		unsmoothedA.add ("A1");
		unsmoothedA.add ("A2");
		unsmoothedA.add ("A3");
		unsmoothedA.add ("A4");

		final List<String> unsmoothedB = new ArrayList<String> ();
		unsmoothedB.add ("B1");
		unsmoothedB.add ("B2");
		
		final Map<String, List<String>> smoothingSystemBitmasksMap = new HashMap<String, List<String>> ();
		smoothingSystemBitmasksMap.put ("A", unsmoothedA);
		smoothingSystemBitmasksMap.put ("B", unsmoothedB);

		// Set up object to test
		final SmoothedTileTypeEx tt = new SmoothedTileTypeEx ();
		tt.getSmoothedTile ().add (createSmoothedTile ("A", "ImageA"));
		tt.getSmoothedTile ().add (createSmoothedTile ("B", "ImageB1"));
		tt.getSmoothedTile ().add (createSmoothedTile ("B", "ImageB2"));
		
		// Run method
		tt.buildMap (smoothingSystemBitmasksMap);
		
		// Check results
		assertEquals (6, tt.getBitmasksMap ().size ());
		assertEquals (1, tt.getBitmasksMap ().get ("A1").size ());
		assertEquals (1, tt.getBitmasksMap ().get ("A2").size ());
		assertEquals (1, tt.getBitmasksMap ().get ("A3").size ());
		assertEquals (1, tt.getBitmasksMap ().get ("A4").size ());
		assertEquals ("ImageA", tt.getBitmasksMap ().get ("A1").get (0).getTileFile ());
		assertEquals ("ImageA", tt.getBitmasksMap ().get ("A2").get (0).getTileFile ());
		assertEquals ("ImageA", tt.getBitmasksMap ().get ("A3").get (0).getTileFile ());
		assertEquals ("ImageA", tt.getBitmasksMap ().get ("A4").get (0).getTileFile ());

		assertEquals (2, tt.getBitmasksMap ().get ("B1").size ());
		assertEquals (2, tt.getBitmasksMap ().get ("B2").size ());
		assertEquals ("ImageB1", tt.getBitmasksMap ().get ("B1").get (0).getTileFile ());
		assertEquals ("ImageB2", tt.getBitmasksMap ().get ("B1").get (1).getTileFile ());
		
		// Prove same list is reused
		assertSame (tt.getBitmasksMap ().get ("B1"), tt.getBitmasksMap ().get ("B2"));
	}

	/**
	 * Tests the buildMap method when an image is missing from the graphics XML
	 * @throws RecordNotFoundException If a bitmaks that the smoothing system rules say should be in the graphics XML isn't there, i.e. an image is missing
	 */
	@Test
	public final void testBuildMap_MissingImage () throws RecordNotFoundException
	{
		// Set up some dummy smoothed-unsmoothed maps
		final List<String> unsmoothedA = new ArrayList<String> ();
		unsmoothedA.add ("A1");
		unsmoothedA.add ("A2");
		unsmoothedA.add ("A3");
		unsmoothedA.add ("A4");

		final List<String> unsmoothedB = new ArrayList<String> ();
		unsmoothedB.add ("B1");
		unsmoothedB.add ("B2");
		
		final Map<String, List<String>> smoothingSystemBitmasksMap = new HashMap<String, List<String>> ();
		smoothingSystemBitmasksMap.put ("A", unsmoothedA);
		smoothingSystemBitmasksMap.put ("B", unsmoothedB);

		// Set up object to test - note we didn't provide an image for B
		final SmoothedTileTypeEx tt = new SmoothedTileTypeEx ();
		tt.getSmoothedTile ().add (createSmoothedTile ("A", "ImageA"));
		
		// Run method
		assertThrows (RecordNotFoundException.class, () ->
		{
			tt.buildMap (smoothingSystemBitmasksMap);
		});
	}
	
	/**
	 * Tests the getRandomImage method
	 * @throws RecordNotFoundException If this bitmask isn't in the map, i.e. its outside the set of unsmoothed bitmasks derived from the directions+maxValueEachDirection values
	 */
	@Test
	public final void testGetRandomImage () throws RecordNotFoundException
	{
		// List of candidate images
		final List<SmoothedTile> images = new ArrayList<SmoothedTile> ();
		images.add (createSmoothedTile ("A", "ImageA1"));
		images.add (createSmoothedTile ("A", "ImageA2"));
		images.add (createSmoothedTile ("A", "ImageA3"));
		images.add (createSmoothedTile ("A", "ImageA4"));
		
		// Mock random number generator to fix results
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (4)).thenReturn (2);
		
		// Set up object to test
		final SmoothedTileTypeEx tt = new SmoothedTileTypeEx ();
		tt.setRandomUtils (randomUtils);
		tt.getBitmasksMap ().put ("A1", images);
		
		// Run method
		final SmoothedTile tile = tt.getRandomImage ("A1");
		
		// Check results
		assertEquals ("ImageA3", tile.getTileFile ());
	}

	/**
	 * Tests the getRandomImage method when we ask for a bitmask outside of the valid set
	 * @throws RecordNotFoundException If this bitmask isn't in the map, i.e. its outside the set of unsmoothed bitmasks derived from the directions+maxValueEachDirection values
	 */
	@Test
	public final void testGetRandomImage_NotFound () throws RecordNotFoundException
	{
		// List of candidate images
		final List<SmoothedTile> images = new ArrayList<SmoothedTile> ();
		images.add (createSmoothedTile ("A", "ImageA1"));
		images.add (createSmoothedTile ("A", "ImageA2"));
		images.add (createSmoothedTile ("A", "ImageA3"));
		images.add (createSmoothedTile ("A", "ImageA4"));
		
		// Set up object to test
		final SmoothedTileTypeEx tt = new SmoothedTileTypeEx ();
		tt.getBitmasksMap ().put ("A1", images);
		
		// Run method
		assertThrows (RecordNotFoundException.class, () ->
		{
			tt.getRandomImage ("A2");
		});
	}
}