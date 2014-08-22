package momime.client.ui;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.security.InvalidParameterException;

/**
 * A shape made up of other shapes - needed this to be able to do undecorated frames with holes in them
 */
public final class CompositeShape implements Shape
{
	/** The individual shapes that make up this composite shape */
	private final Shape [] shapes;
	
	/**
	 * @param s The individual shapes that make up this composite shape
	 */
	public CompositeShape (final Shape [] s)
	{
		super ();
		
		if (s.length == 0)
			throw new InvalidParameterException ("CompositeShape: Shapes array must not be empty");
		
		shapes = s;
	}

	/**
	 * @return Rectangle that completely encloses the shape
	 */
	@Override
	public final Rectangle getBounds ()
	{
		final Rectangle result;
		
		final Rectangle r1 = shapes [0].getBounds ();
		if (shapes.length == 1)
			result = r1;
		else
		{
			// Merge the rectangles reported by each of the individual shapes
			int x1 = r1.x;
			int y1 = r1.y;
			int x2 = r1.x + r1.width;
			int y2 = r1.y + r1.height;
		
			boolean first = true;
			for (final Shape shape : shapes)
			{
				if (!first)
				{
					final Rectangle r2 = shape.getBounds ();
					x1 = Math.min (x1, r2.x);
					y1 = Math.min (y1, r2.y);
					x2 = Math.max (x2, r2.x + r2.width);
					y2 = Math.max (y2, r2.y + r2.height);
				}
			
				first = false;
			}
			
			result = new Rectangle (x1, y1, x2-x1, y2-y1);
		}
		
		return result;
	}

	/**
	 * @return High precision rectangle that completely encloses the shape
	 */
	@Override
	public final Rectangle2D getBounds2D ()
	{
		final Rectangle2D result;
		
		final Rectangle2D r1 = shapes [0].getBounds2D ();
		if (shapes.length == 1)
			result = r1;
		else
		{
			// Merge the rectangles reported by each of the individual shapes
			double x1 = r1.getMinX ();
			double y1 = r1.getMinY ();
			double x2 = r1.getMaxX ();
			double y2 = r1.getMaxY ();
		
			boolean first = true;
			for (final Shape shape : shapes)
			{
				if (!first)
				{
					final Rectangle2D r2 = shape.getBounds2D ();
					x1 = Math.min (x1, r2.getMinX ());
					y1 = Math.min (y1, r2.getMinY ());
					x2 = Math.max (x2, r2.getMaxX ());
					y2 = Math.max (y2, r2.getMaxY ());
				}
			
				first = false;
			}
			
			result = new Rectangle2D.Double (x1, y1, x2-x1, y2-y1);
		}
		
		return result;
	}

	/**
     * @param x The X coordinate of the upper-left corner of the specified rectangular area
     * @param y The Y coordinate of the upper-left corner of the specified rectangular area
     * @param w The width of the specified rectangular area
     * @param h The height of the specified rectangular area
	 * @return Whether the specified rectangle intersects with any of the individual shapes
	 */
	@Override
	public final boolean intersects (final double x, final double y, final double w, final double h)
	{
		boolean result = false;
		int n = 0;
		while ((!result) && (n < shapes.length))
		{
			if (shapes [n].intersects (x, y, w, h))
				result = true;
			else
				n++;
		}
		return result;
	}

	/**
     * @param r The rectangular area to test
	 * @return Whether the specified rectangle intersects with any of the individual shapes
	 */
	@Override
	public final boolean intersects (final Rectangle2D r)
	{
		boolean result = false;
		int n = 0;
		while ((!result) && (n < shapes.length))
		{
			if (shapes [n].intersects (r))
				result = true;
			else
				n++;
		}
		return result;
	}

	
	/**
     * @param x The specified X coordinate to be tested
     * @param y The specified Y coordinate to be tested
	 * @return Whether the specified points are contained within any of the individual shapes
	 */
	@Override
	public final boolean contains (final double x, final double y)
	{
		boolean result = false;
		int n = 0;
		while ((!result) && (n < shapes.length))
		{
			if (shapes [n].contains (x, y))
				result = true;
			else
				n++;
		}
		return result;
	}

	/**
     * @param p The specified point to be tested
	 * @return Whether the specified point is contained within any of the individual shapes
	 */
	@Override
	public final boolean contains (final Point2D p)
	{
		boolean result = false;
		int n = 0;
		while ((!result) && (n < shapes.length))
		{
			if (shapes [n].contains (p))
				result = true;
			else
				n++;
		}
		return result;
	}

	/**
     * @param x The X coordinate of the upper-left corner of the specified rectangular area
     * @param y The Y coordinate of the upper-left corner of the specified rectangular area
     * @param w The width of the specified rectangular area
     * @param h The height of the specified rectangular area
	 * @return Whether the specified rectangle is entirely contained within any of the individual shapes
	 */
	@Override
	public final boolean contains (final double x, final double y, final double w, final double h)
	{
		/*
		 * This isn't entirely accurate.  Its possible to manufacture an example of 3 rectangles where the 3rd rectangle
		 * is not contained within either the 1st or 2nd rectangles, but is contained within their composite shape.
		 * So we might return false even when the specified rectangle is contained within the composite shape.
		 * However the contract of the contanis () method allows for implementations to return false if the
		 * calculations are too difficult, so we can get away with it.
		 * 
		 * 1st:
		 * -----------------------------
		 * |                                 |          2nd:
		 * |                            -------------------------------
		 * |                            |    |                              |
		 * |                     -----------------                       |
		 * |             3rd:  |      |    |     |                       |
		 * |                     -----------------                       |
		 * |                            |    |                              |
		 * |                            -------------------------------
		 * |                                 |
		 * -----------------------------
		 */
		boolean result = false;
		int n = 0;
		while ((!result) && (n < shapes.length))
		{
			if (shapes [n].contains (x, y, w, h))
				result = true;
			else
				n++;
		}
		return result;
	}

	/**
     * @param r The rectangular area to test
	 * @return Whether the specified rectangle is entirely contained within any of the individual shapes
	 */
	@Override
	public final boolean contains (final Rectangle2D r)
	{
		// Same comment as method above
		boolean result = false;
		int n = 0;
		while ((!result) && (n < shapes.length))
		{
			if (shapes [n].contains (r))
				result = true;
			else
				n++;
		}
		return result;
	}

	/**
	 * @return Iterator along the points and edges that make up the shape
	 */
	@Override
	public final PathIterator getPathIterator (final AffineTransform at)
	{
		final PathIterator [] paths = new PathIterator [shapes.length];
		
		int n = 0;
		for (final Shape shape : shapes)
		{
			paths [n] = shape.getPathIterator (at);
			n++;
		}
		
		return new CompositeShapePathIterator (paths);
	}

	/**
	 * @return Iterator along the points and edges that make up the shape
	 */
	@Override
	public final PathIterator getPathIterator (final AffineTransform at, final double flatness)
	{
		final PathIterator [] paths = new PathIterator [shapes.length];
		
		int n = 0;
		for (final Shape shape : shapes)
		{
			paths [n] = shape.getPathIterator (at, flatness);
			n++;
		}
		
		return new CompositeShapePathIterator (paths);
	}
	
	/**
	 * Iterates around the composite paths, by iterating around each path in turn.
	 * This assumes the inidividual paths all start with a MOVE and end with a CLOSE, which should always be the case.
	 */
	private final class CompositeShapePathIterator implements PathIterator
	{
		/** The individual paths that make up this composite path */
		private final PathIterator [] paths;

		/** Index into the paths array of the path currently being output */
		private int pathIndex;
		
		/**
		 * @param p The individual paths that make up this composite path
		 */
		public CompositeShapePathIterator (final PathIterator [] p)
		{
			super ();
			
			if (p.length == 0)
				throw new InvalidParameterException ("CompositeShapePathIterator: Paths array must not be empty");
			
			paths = p;
		}
		
		/**
		 * @return Winding rule shared by all the individual paths
		 */
		@Override
		public final int getWindingRule ()
		{
			// I'd rather this wasn't fixed, but Rectangle and Polygon generate different winding rules, so have to use the more generic of the two
			return WIND_EVEN_ODD;
		}

		/**
		 * @return Whether we've iterated around the entire composite path
		 */
		@Override
		public final boolean isDone ()
		{
			return pathIndex >= paths.length;
		}

		/**
		 * 
		 */
		@Override
		public final void next ()
		{
			// Delegate the next () to the current path, then if that path is completed, move on to the next
			if (pathIndex < paths.length)
			{
				paths [pathIndex].next ();
				if (paths [pathIndex].isDone ())
					pathIndex++;
			}
		}

		/**
		 * @param coords Array into which the method will populate the coords of the current segment
		 * @return The type of movement across this segment
		 */
		@Override
		public final int currentSegment (float [] coords)
		{
			if (pathIndex < paths.length)
				return paths [pathIndex].currentSegment (coords);
			else
				return SEG_CLOSE;
		}

		/**
		 * @param coords Array into which the method will populate the coords of the current segment
		 * @return The type of movement across this segment
		 */
		@Override
		public final int currentSegment (double [] coords)
		{
			if (pathIndex < paths.length)
				return paths [pathIndex].currentSegment (coords);
			else
				return SEG_CLOSE;
		}
	}
}