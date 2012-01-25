package momime.server.mapgenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.server.utils.RandomUtils;

import com.ndg.map.CoordinateSystem;

/**
 * Generates plasma fractal height maps, that are used as the basis of both the overland and combat map generators
 */
final class HeightMapGenerator
{
	/** Logger to write to debug text file when the debug log is enabled */
	private final Logger debugLogger;

	/** Coordinate system we are generating a height map for */
	private final CoordinateSystem coordinateSystem;

	/** Width of each "zone" - small values produce more random jagged landscape, larger zones produce more gradual height changes */
	private final int zoneWidth;

	/** Height of each "zone" - small values produce more random jagged landscape, larger zones produce more gradual height changes */
	private final int zoneHeight;

	/** Number of rows from non-wrapping edges were we may place tundra */
	private final int numberOfRowsFromMapEdgeWhereTundraCanAppear;

	/** Generated height map */
	private final Integer [] [] heightMap;

	/** Height map with minimum value on each plane adjusted to zero */
	private final int [] [] zeroBasedHeightMap;

	/** Number of occurrences of each height on each plane - outer list contains the two planes; inner list is the list of heights */
	private final List<Integer> heightCounts;

	/**
	 * Creates all of the objects leaving them unfilled, must call the generateHeightMap () method to actually do the work
	 * This allows the unit tests to create the map generator objects without actually running it
	 *
	 * @param aCoordinateSystem Coordinate system we are generating a height map for
	 * @param aZoneWidth Width of each "zone" - small values produce more random jagged landscape, larger zones produce more gradual height changes
	 * @param aZoneHeight Height of each "zone" - small values produce more random jagged landscape, larger zones produce more gradual height changes
	 * @param aNumberOfRowsFromMapEdgeWhereTundraCanAppear Number of rows from non-wrapping edges were we may place tundra
	 * @param aDebugLogger Logger to write to debug text file when the debug log is enabled
	 */
	HeightMapGenerator (final CoordinateSystem aCoordinateSystem, final int aZoneWidth, final int aZoneHeight,
		final int aNumberOfRowsFromMapEdgeWhereTundraCanAppear, final Logger aDebugLogger)
	{
		super ();

		debugLogger = aDebugLogger;
		coordinateSystem = aCoordinateSystem;
		zoneWidth = aZoneWidth;
		zoneHeight = aZoneHeight;
		numberOfRowsFromMapEdgeWhereTundraCanAppear = aNumberOfRowsFromMapEdgeWhereTundraCanAppear;

		heightMap = new Integer [coordinateSystem.getHeight ()] [coordinateSystem.getWidth ()];
		zeroBasedHeightMap = new int [coordinateSystem.getHeight ()] [coordinateSystem.getWidth ()];
		heightCounts = new ArrayList<Integer> ();
	}

	/**
	 * Generates the height map
	 */
	final void generateHeightMap ()
	{
		debugLogger.entering (HeightMapGenerator.class.getName (), "generateHeightMap",
			coordinateSystem.getWidth () + " x " + coordinateSystem.getHeight () + ", " + numberOfRowsFromMapEdgeWhereTundraCanAppear);

		// Generate height-based scenery
		generateFractalLandscape ();
		generateZeroBasedHeightMap ();
		countTilesAtEachHeight ();

		debugLogger.exiting (HeightMapGenerator.class.getName (), "generateHeightMap");
	}

	/**
	 * Combat map generation shares this, hence it being static, package private and taking all values an inputs rather than from the map generator object
	 *
	 * @param x X coordinate to test
	 * @param y Y coordinate to test
	 * @param dist Distance away from non-wrapping edges to return "true" for
	 * @return True if the requested coordinates near a non-wrapping edge
	 */
	final boolean nearSingularity (final int x, final int y, final int dist)
	{
		return (((!coordinateSystem.isWrapsLeftToRight ()) && ((x < dist) || (x >= coordinateSystem.getWidth () - dist))) ||
					((!coordinateSystem.isWrapsTopToBottom ()) && ((y < dist) || (y >= coordinateSystem.getHeight () - dist))));
	}

	/**
	 * Sets the value only if not previously set (is null)
	 * Combat map generation shares this, hence it being static and taking all values an inputs rather than from the map generator object
	 *
	 * @param x X coordinate to set
	 * @param y Y coordinate to set
	 * @param value Value to set
	 */
	final void setMidPoints (final int x, final int y, final int value)
	{
		if (heightMap [y] [x] == null)
			heightMap [y] [x] = value;
	}

	/**
	 * Performs one iteraction of the fractal landscape algorithm
	 * Given 4 corner points, chooses values for the 4 midpoints and centrepoint
	 * This was originally based on the routine "gen5rec" in mapgen.c from FreeCiv.
	 * The method in FreeCiv has since been moved to height_map.c.
	 * Both of these are in the "Master of Magic\Reference code" folder.
	 *
	 * Combat map generation shares this, hence it being static and taking all values an inputs rather than from the map generator object
	 *
	 * @param step Used to scale the variation in the height
	 * @param x0 Left edge of the area
	 * @param y0 Top edge of the area
	 * @param x1 Right edge of the area
	 * @param y1 Bottom edge of the area
	 */
	private final void fractalLandscapeIteration (final int step, final int x0, final int y0, final int x1, final int y1)
	{
		// All x and y values are native
		if (((y1 - y0 <= 0) || (x1 - x0 <= 0)) || ((y1 - y0 == 1) && (x1 - x0 == 1)))
		{
			// Do nothing
		}
		else
		{
			//  To wrap correctly
			final int x1wrap;
			if (x1 == coordinateSystem.getWidth ())
				x1wrap = 0;
			else
				x1wrap = x1;

			final int y1wrap;
			if (y1 == coordinateSystem.getHeight ())
				y1wrap = 0;
			else
				y1wrap = y1;

			// Read the current corner values from the height map
			// These should always be reading existing values so should never be null
			final int val00 = heightMap [y0] [x0];
			final int val01 = heightMap [y1wrap] [x0];
			final int val10 = heightMap [y0] [x1wrap];
			final int val11 = heightMap [y1wrap] [x1wrap];

			// Find mid points
			final int xmid = (x0 + x1) / 2;
			final int ymid = (y0 + y1) / 2;

			// Set midpoints of sides to avg of side's vertices plus a random factor
			// Unset points are null, don't reset if set
			setMidPoints (xmid,		y0,		((val00 + val10) / 2) + RandomUtils.getGenerator ().nextInt (step) - (step / 2));
			setMidPoints (xmid,		y1wrap,	((val01 + val11) / 2) + RandomUtils.getGenerator ().nextInt (step) - (step / 2));
			setMidPoints (x0,		ymid,		((val00 + val01) / 2) + RandomUtils.getGenerator ().nextInt (step) - (step / 2));
			setMidPoints (x1wrap,	ymid,		((val10 + val11) / 2) + RandomUtils.getGenerator ().nextInt (step) - (step / 2));

			// Set middle to average of midpoints plus a random factor, if not set
			setMidPoints (xmid, ymid, ((val00 + val01 + val10 + val11) / 4) + RandomUtils.getGenerator ().nextInt (step) - (step / 2));

			// Now call recursively on the four subrectangles
			final int newStep = (2 * step) / 3;
			fractalLandscapeIteration (newStep, x0,		y0,	xmid,	ymid);
			fractalLandscapeIteration (newStep, x0,		ymid,	xmid,	y1);
			fractalLandscapeIteration (newStep, xmid,	y0,	x1,	ymid);
			fractalLandscapeIteration (newStep, xmid,	ymid,	x1,	y1);
		}
	}

	/**
	 * Generates a landscape height map via a plasma fractal
	 * This was originally based on the routine "mapgenerator5" in mapgen.c from FreeCiv.
	 * The method in FreeCiv has since been renamed to "make_pseudofractal1_hmap" and moved to height_map.c.
	 * Both of these are in the "Master of Magic\Reference code" folder.
	 */
	private final void generateFractalLandscape ()
	{
		debugLogger.entering (HeightMapGenerator.class.getName (), "generateFractalLandscape");

		// Session description contains zone width/height - we need the number of zones
		final int xBlocks = (int) Math.round (coordinateSystem.getWidth () / (double) zoneWidth);		// a.k.a. zonesHorizontally
		final int yBlocks = (int) Math.round (coordinateSystem.getHeight () / (double) zoneHeight);	// a.k.a. zonesVertically

		// If the edge wraps, the last block ends with the first point, so there's the same number of blocks and dividing points
		// If the edge doesn't wrap, we need a point on the right/bottom of it as well to end the last block
		final int xPoints;
		if (coordinateSystem.isWrapsLeftToRight ())
			xPoints = xBlocks;
		else
			xPoints = xBlocks + 1;

		final int yPoints;
		if (coordinateSystem.isWrapsTopToBottom ())
			yPoints = yBlocks;
		else
			yPoints = yBlocks + 1;

		// If the edge wraps, the last pretend block will be the first pixel off the edge of the map.
		// If the edge doesn't wrap, the last block is the actual last pixel
		final int xMax;
		if (coordinateSystem.isWrapsLeftToRight ())
			xMax = coordinateSystem.getWidth ();
		else
			xMax = coordinateSystem.getWidth () - 1;

		final int yMax;
		if (coordinateSystem.isWrapsTopToBottom ())
			yMax = coordinateSystem.getHeight ();
		else
			yMax = coordinateSystem.getHeight () - 1;

		// Just need something > log(max(xsize, ysize)) for the recursion???
		final int step = coordinateSystem.getWidth () + coordinateSystem.getHeight ();

		// Edges are avoided more strongly as this increases
		final int avoidEdge = step / 2;

		// Now generate a plasma fractal to use as the height map
		// Set initial points
		for (int xn = 0; xn < xPoints; xn++)
			for (int yn = 0; yn < yPoints; yn++)
			{
				final int x = (xn * xMax) / xBlocks;
				final int y = (yn * yMax) / yBlocks;

				// Randomize initial point
				int thisHeight = RandomUtils.getGenerator ().nextInt (2 * step) - step;

				// Avoid edges (topological singularities)
				if (nearSingularity (x, y, 7))		// cannot find actual value for CITY_MAP_RADIUS
					thisHeight = thisHeight - avoidEdge;

				// Separate poles and avoid too much land at poles
				// This is basically repeating the above... but its how the MoM IME map generator has been since 0.1 and it gives nice results so I kept it as is
				if (nearSingularity (x, y, numberOfRowsFromMapEdgeWhereTundraCanAppear))
					thisHeight = thisHeight - avoidEdge;

				// Set it
				heightMap [y] [x] = thisHeight;
			}

		// Calculate recursively on each block
		for (int xn = 0; xn < xBlocks; xn++)
			for (int yn = 0; yn < yBlocks; yn++)
				fractalLandscapeIteration (step, (xn * xMax) / xBlocks, (yn * yMax) / yBlocks, ((xn + 1) * xMax) / xBlocks, ((yn + 1) * yMax) / yBlocks);

		debugLogger.exiting (HeightMapGenerator.class.getName (), "generateFractalLandscape");
	}

	/**
	 * Raises the height map so that the lowest point of land is guaranteed to be at height 0
	 */
	final void generateZeroBasedHeightMap ()
	{
		debugLogger.entering (HeightMapGenerator.class.getName (), "generateZeroBasedHeightMap");

		// Find the minimum height
		int minimumHeight = heightMap [0] [0];
		for (int x = 0; x < coordinateSystem.getWidth (); x++)
			for (int y = 0; y < coordinateSystem.getHeight (); y++)
			{
				final int thisHeight = heightMap [y] [x];
				if (thisHeight < minimumHeight)
					minimumHeight = thisHeight;
			}

		// Copy + adjust the height map
		for (int x = 0; x < coordinateSystem.getWidth (); x++)
			for (int y = 0; y < coordinateSystem.getHeight (); y++)
				zeroBasedHeightMap [y] [x] = heightMap [y] [x] - minimumHeight;

		debugLogger.exiting (HeightMapGenerator.class.getName (), "generateZeroBasedHeightMap");
	}

	/**
	 * Counts how many tiles there are of each particular height
	 */
	final void countTilesAtEachHeight ()
	{
		debugLogger.entering (HeightMapGenerator.class.getName (), "countTilesAtEachHeight");

		// This relies on the fact that there are no entries in the height map with negative values
		// so by starting at zero and working up, we've guaranteed to evenually hit every tile on the map
		int tilesDone = 0;
		while (tilesDone < coordinateSystem.getWidth () * coordinateSystem.getHeight ())
		{
			// Count tiles with this height
			int count = 0;
			for (int x = 0; x < coordinateSystem.getWidth (); x++)
				for (int y = 0; y < coordinateSystem.getHeight (); y++)
					if (zeroBasedHeightMap [y] [x] == heightCounts.size ())
						count++;

			// Add to the list
			heightCounts.add (new Integer (count));
			tilesDone = tilesDone + count;
		}

		debugLogger.exiting (HeightMapGenerator.class.getName (), "countTilesAtEachHeight");
	}

	/**
	 * @param threshold Height to test
	 * @return Number of tiles whose height is at least threshold
	 */
	final int countTilesAboveThreshold (final int threshold)
	{
		int count = 0;
		for (int index = threshold; index < heightCounts.size (); index++)
			count = count + heightCounts.get (index);

		return count;
	}

	/**
	 * @param threshold Height to test
	 * @return Number of tiles whose height is at most threshold
	 */
	final int countTilesBelowThreshold (final int threshold)
	{
		int count = 0;
		for (int index = 0; index <= threshold; index++)
			count = count + heightCounts.get (index);

		return count;
	}

	/**
	 * @return Coordinate system we are generating a height map for
	 */
	final CoordinateSystem getCoordinateSystem ()
	{
		return coordinateSystem;
	}

	/**
	 * @return Generated height map
	 */
	final Integer [] [] getHeightMap ()
	{
		return heightMap;
	}

	/**
	 * @return Height map with minimum value on each plane adjusted to zero
	 */
	final int [] [] getZeroBasedHeightMap ()
	{
		return zeroBasedHeightMap;
	}

	/**
	 * @return Number of occurrences of each height on each plane - outer list contains the two planes; inner list is the list of heights
	 */
	final List<Integer> getHeightCounts ()
	{
		return heightCounts;
	}
}
