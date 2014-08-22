package momime.client.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.junit.Test;

/**
 * Tests the CompositeShape class
 */
public final class TestCompositeShape
{
	/**
	 * Tests the getBounds method
	 */
	@Test
	public final void testGetBounds ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});
		
		// Run method
		final Rectangle r = shape.getBounds ();
		
		// Check results
		assertEquals (0, r.x);
		assertEquals (0, r.y);
		assertEquals (40, r.width);
		assertEquals (25, r.height);
	}

	/**
	 * Tests the getBounds2D method
	 */
	@Test
	public final void testGetBounds2D ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});
		
		// Run method
		final Rectangle2D r = shape.getBounds2D ();
		
		// Check results
		assertEquals (0, r.getMinX (), 0);
		assertEquals (0, r.getMinY (), 0);
		assertEquals (40, r.getMaxX (), 0);
		assertEquals (25, r.getMaxY (), 0);
	}

	/**
	 * Tests the intersects method, using coords
	 */
	@Test
	public final void testIntersects_Coords ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});
		
		// Check some points
		assertTrue (shape.intersects (5, 5, 1, 1));
		assertFalse (shape.intersects (35, 5, 1, 1));
		assertTrue (shape.intersects (35, 20, 1, 1));
		assertFalse (shape.intersects (5, 20, 1, 1));
		
		assertTrue (shape.intersects (27, 17, 1, 1));
		assertFalse (shape.intersects (32, 13, 1, 1));
		assertFalse (shape.intersects (23, 22, 1, 1));
		
		// These are here to demonstrate the difference between intersects and contains
		assertTrue (shape.intersects (28, 5, 4, 2));
		assertTrue (shape.intersects (23, 22, 4, 2));
	}

	/**
	 * Tests the intersects method, using rectangles
	 */
	@Test
	public final void testIntersects_Rectangles ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});
		
		// Check some points
		assertTrue (shape.intersects (new Rectangle2D.Double (5, 5, 1, 1)));
		assertFalse (shape.intersects (new Rectangle2D.Double (35, 5, 1, 1)));
		assertTrue (shape.intersects (new Rectangle2D.Double (35, 20, 1, 1)));
		assertFalse (shape.intersects (new Rectangle2D.Double (5, 20, 1, 1)));
		
		assertTrue (shape.intersects (new Rectangle2D.Double (27, 17, 1, 1)));
		assertFalse (shape.intersects (new Rectangle2D.Double (32, 13, 1, 1)));
		assertFalse (shape.intersects (new Rectangle2D.Double (23, 22, 1, 1)));

		// These are here to demonstrate the difference between intersects and contains
		assertTrue (shape.intersects (new Rectangle2D.Double (28, 5, 4, 2)));
		assertTrue (shape.intersects (new Rectangle2D.Double (23, 22, 4, 2)));
	}
	
	/**
	 * Tests the contains method, using point coords
	 */
	@Test
	public final void testContains_PointCoords ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});
		
		// Check some points
		assertTrue (shape.contains (5, 5));
		assertFalse (shape.contains (35, 5));
		assertTrue (shape.contains (35, 20));
		assertFalse (shape.contains (5, 20));
		
		assertTrue (shape.contains (27, 17));
		assertFalse (shape.contains (32, 13));
		assertFalse (shape.contains (23, 22));
	}

	/**
	 * Tests the contains method, using points
	 */
	@Test
	public final void testContains_Points ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});
		
		// Check some points
		assertTrue (shape.contains (new Point2D.Double (5, 5)));
		assertFalse (shape.contains (new Point2D.Double (35, 5)));
		assertTrue (shape.contains (new Point2D.Double (35, 20)));
		assertFalse (shape.contains (new Point2D.Double (5, 20)));
		
		assertTrue (shape.contains (new Point2D.Double (27, 17)));
		assertFalse (shape.contains (new Point2D.Double (32, 13)));
		assertFalse (shape.contains (new Point2D.Double (23, 22)));
	}

	/**
	 * Tests the contains method, using rectangle coords
	 */
	@Test
	public final void testContains_RectangleCoords ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});
		
		// Check some points
		assertTrue (shape.contains (5, 5, 1, 1));
		assertFalse (shape.contains (35, 5, 1, 1));
		assertTrue (shape.contains (35, 20, 1, 1));
		assertFalse (shape.contains (5, 20, 1, 1));
		
		assertTrue (shape.contains (27, 17, 1, 1));
		assertFalse (shape.contains (32, 13, 1, 1));
		assertFalse (shape.contains (23, 22, 1, 1));
		
		// These are here to demonstrate the difference between contains and contains
		assertFalse (shape.contains (28, 5, 4, 2));
		assertFalse (shape.contains (23, 22, 4, 2));
	}

	/**
	 * Tests the contains method, using rectangles
	 */
	@Test
	public final void testContains_Rectangles ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});
		
		// Check some points
		assertTrue (shape.contains (new Rectangle2D.Double (5, 5, 1, 1)));
		assertFalse (shape.contains (new Rectangle2D.Double (35, 5, 1, 1)));
		assertTrue (shape.contains (new Rectangle2D.Double (35, 20, 1, 1)));
		assertFalse (shape.contains (new Rectangle2D.Double (5, 20, 1, 1)));
		
		assertTrue (shape.contains (new Rectangle2D.Double (27, 17, 1, 1)));
		assertFalse (shape.contains (new Rectangle2D.Double (32, 13, 1, 1)));
		assertFalse (shape.contains (new Rectangle2D.Double (23, 22, 1, 1)));

		// These are here to demonstrate the difference between contains and contains
		assertFalse (shape.contains (new Rectangle2D.Double (28, 5, 4, 2)));
		assertFalse (shape.contains (new Rectangle2D.Double (23, 22, 4, 2)));
	}
	
	/**
	 * Tests the getPathIterator method
	 */
	@Test
	public final void testGetPathIterator ()
	{
		// Set up sample shape with a 30x20 rectangle and 15x10 rectangle overlapping in a 5x5 area
		final CompositeShape shape = new CompositeShape (new Shape []
			{new Rectangle (0, 0, 30, 20), new Rectangle (25, 15, 15, 10)});

		// Run method
		final PathIterator iter = shape.getPathIterator (null);
		final double [] coords = new double [6];
		
		// Check points around the 1st rectangle
		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_MOVETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 0);
		assertEquals (0, coords [1], 0);
		iter.next ();

		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_LINETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 30);
		assertEquals (0, coords [1], 0);
		iter.next ();

		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_LINETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 30);
		assertEquals (0, coords [1], 20);
		iter.next ();

		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_LINETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 0);
		assertEquals (0, coords [1], 20);
		iter.next ();

		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_LINETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 0);
		assertEquals (0, coords [1], 0);
		iter.next ();
		
		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_CLOSE, iter.currentSegment (coords));
		iter.next ();

		// Check points around the 2nd rectangle
		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_MOVETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 25);
		assertEquals (0, coords [1], 15);
		iter.next ();

		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_LINETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 40);
		assertEquals (0, coords [1], 15);
		iter.next ();

		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_LINETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 40);
		assertEquals (0, coords [1], 25);
		iter.next ();

		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_LINETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 25);
		assertEquals (0, coords [1], 25);
		iter.next ();

		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_LINETO, iter.currentSegment (coords));
		assertEquals (0, coords [0], 25);
		assertEquals (0, coords [1], 15);
		iter.next ();
		
		assertFalse (iter.isDone ());
		assertEquals (PathIterator.SEG_CLOSE, iter.currentSegment (coords));
		iter.next ();
		
		// Now it should be done
		assertTrue (iter.isDone ());
	}
}