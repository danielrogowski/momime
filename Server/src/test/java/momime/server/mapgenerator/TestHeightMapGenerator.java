package momime.server.mapgenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the HeightMapGenerator class
 */
public final class TestHeightMapGenerator
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the nearSingularity method
	 */
	@Test
	public final void testNearSingularity ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setWidth (60);
		sys.setHeight (40);

		final HeightMapGenerator mapGen = new HeightMapGenerator (sys, 0, 0, 0, debugLogger);
		final int dist = 5;

		// Neither wrapping
		assertTrue (mapGen.nearSingularity (0, 0, dist));
		assertTrue (mapGen.nearSingularity (4, 4, dist));
		assertTrue (mapGen.nearSingularity (5, 4, dist));
		assertTrue (mapGen.nearSingularity (4, 5, dist));
		assertFalse (mapGen.nearSingularity (5, 5, dist));
		assertFalse (mapGen.nearSingularity (54, 5, dist));
		assertTrue (mapGen.nearSingularity (55, 5, dist));
		assertFalse (mapGen.nearSingularity (54, 34, dist));
		assertTrue (mapGen.nearSingularity (54, 35, dist));

		// Turn on x-wrapping, like normal MoM map
		sys.setWrapsLeftToRight (true);
		assertTrue (mapGen.nearSingularity (0, 0, dist));
		assertTrue (mapGen.nearSingularity (4, 4, dist));
		assertTrue (mapGen.nearSingularity (5, 4, dist));
		assertFalse (mapGen.nearSingularity (4, 5, dist));
		assertFalse (mapGen.nearSingularity (5, 5, dist));
		assertFalse (mapGen.nearSingularity (54, 5, dist));
		assertFalse (mapGen.nearSingularity (55, 5, dist));
		assertFalse (mapGen.nearSingularity (54, 34, dist));
		assertTrue (mapGen.nearSingularity (54, 35, dist));

		// Turn on both wrapping
		sys.setWrapsTopToBottom (true);
		assertFalse (mapGen.nearSingularity (0, 0, dist));
		assertFalse (mapGen.nearSingularity (4, 4, dist));
		assertFalse (mapGen.nearSingularity (5, 4, dist));
		assertFalse (mapGen.nearSingularity (4, 5, dist));
		assertFalse (mapGen.nearSingularity (5, 5, dist));
		assertFalse (mapGen.nearSingularity (54, 5, dist));
		assertFalse (mapGen.nearSingularity (55, 5, dist));
		assertFalse (mapGen.nearSingularity (54, 34, dist));
		assertFalse (mapGen.nearSingularity (54, 35, dist));
	}

	/**
	 * Tests the setMidPoints method
	 */
	@Test
	public final void testSetMidPoints ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setWidth (6);
		sys.setHeight (6);

		final HeightMapGenerator mapGen = new HeightMapGenerator (sys, 0, 0, 0, debugLogger);

		// So basically test that we can only set each value once
		assertNull (mapGen.getHeightMap () [0] [0]);

		mapGen.setMidPoints (0, 0, 5);
		assertEquals (5, mapGen.getHeightMap () [0] [0].intValue ());

		mapGen.setMidPoints (0, 0, 8);
		assertEquals (5, mapGen.getHeightMap () [0] [0].intValue ());

		mapGen.setMidPoints (1, 0, 8);
		assertEquals (8, mapGen.getHeightMap () [0] [1].intValue ());
	}

	/**
	 * Tests the generateZeroBasedHeightMap method
	 */
	@Test
	public final void testGenerateZeroBasedHeightMap ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setWidth (3);
		sys.setHeight (3);

		// Fill in values
		final HeightMapGenerator mapGen = new HeightMapGenerator (sys, 0, 0, 0, debugLogger);
		mapGen.getHeightMap () [0] [0] = 5;
		mapGen.getHeightMap () [0] [1] = 3;
		mapGen.getHeightMap () [0] [2] = -3;
		mapGen.getHeightMap () [1] [0] = 2;
		mapGen.getHeightMap () [1] [1] = -1;
		mapGen.getHeightMap () [1] [2] = 0;
		mapGen.getHeightMap () [2] [0] = 8;
		mapGen.getHeightMap () [2] [1] = 4;
		mapGen.getHeightMap () [2] [2] = -2;

		// Check values
		mapGen.generateZeroBasedHeightMap ();
		assertEquals (8, mapGen.getZeroBasedHeightMap () [0] [0]);
		assertEquals (6, mapGen.getZeroBasedHeightMap () [0] [1]);
		assertEquals (0, mapGen.getZeroBasedHeightMap () [0] [2]);
		assertEquals (5, mapGen.getZeroBasedHeightMap () [1] [0]);
		assertEquals (2, mapGen.getZeroBasedHeightMap () [1] [1]);
		assertEquals (3, mapGen.getZeroBasedHeightMap () [1] [2]);
		assertEquals (11, mapGen.getZeroBasedHeightMap () [2] [0]);
		assertEquals (7, mapGen.getZeroBasedHeightMap () [2] [1]);
		assertEquals (1, mapGen.getZeroBasedHeightMap () [2] [2]);
	}

	/**
	 * Tests the countTilesAtEachHeight method
	 */
	@Test
	public final void testCountTilesAtEachHeight ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setWidth (6);
		sys.setHeight (6);

		// So this fills the cells with values 0..10
		final HeightMapGenerator mapGen = new HeightMapGenerator (sys, 0, 0, 0, debugLogger);
		for (int x = 0; x < sys.getWidth (); x++)
			for (int y = 0; y < sys.getHeight (); y++)
				mapGen.getZeroBasedHeightMap () [y] [x] = x + y;

		mapGen.countTilesAtEachHeight ();

		assertEquals (11, mapGen.getHeightCounts ().size ());
		assertEquals (1, mapGen.getHeightCounts ().get (0).intValue ());
		assertEquals (2, mapGen.getHeightCounts ().get (1).intValue ());
		assertEquals (3, mapGen.getHeightCounts ().get (2).intValue ());
		assertEquals (4, mapGen.getHeightCounts ().get (3).intValue ());
		assertEquals (5, mapGen.getHeightCounts ().get (4).intValue ());
		assertEquals (6, mapGen.getHeightCounts ().get (5).intValue ());
		assertEquals (5, mapGen.getHeightCounts ().get (6).intValue ());
		assertEquals (4, mapGen.getHeightCounts ().get (7).intValue ());
		assertEquals (3, mapGen.getHeightCounts ().get (8).intValue ());
		assertEquals (2, mapGen.getHeightCounts ().get (9).intValue ());
		assertEquals (1, mapGen.getHeightCounts ().get (10).intValue ());
	}

	/**
	 * Tests the countTilesAboveThreshold method
	 */
	@Test
	public final void testCountTilesAboveThreshold ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		final HeightMapGenerator mapGen = new HeightMapGenerator (sys, 0, 0, 0, debugLogger);

		// So this puts 10 height 0 cells, 11 height 1 cells, 12 height 2 cells, and so on
		for (int n = 0; n <= 10; n++)
			mapGen.getHeightCounts ().add (n + 10);

		assertEquals (105, mapGen.countTilesAboveThreshold (5));
	}

	/**
	 * Tests the countTilesBelowThreshold method
	 */
	@Test
	public final void testCountTilesBelowThreshold ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		final HeightMapGenerator mapGen = new HeightMapGenerator (sys, 0, 0, 0, debugLogger);

		// So this puts 10 height 0 cells, 11 height 1 cells, 12 height 2 cells, and so on
		for (int n = 0; n <= 10; n++)
			mapGen.getHeightCounts ().add (n + 10);

		assertEquals (75, mapGen.countTilesBelowThreshold (5));
	}

	/**
	 * Tests the complete generateHeightMap method
	 * This is really testing fractalLandscapeIteration / generateFractalLandscape but they aren't sensible to test in isolation
	 */
	@Test
	public final void testGenerateHeightMap ()
	{
		// Set this up just like the proper MoM values
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setWrapsLeftToRight (true);

		final HeightMapGenerator mapGen = new HeightMapGenerator (sys, 10, 10, 7, debugLogger);
		mapGen.generateHeightMap ();

		// Not much that can be automatically checked here, we can at least verify that the sum of all the counted heights should equal the number of cells
		assertEquals (sys.getWidth () * sys.getHeight (), mapGen.countTilesAboveThreshold (0));

		// Also check that the entire map is not flat
		if (mapGen.getHeightCounts ().size () == 1)
			fail ("Map is flat");
	}
}
