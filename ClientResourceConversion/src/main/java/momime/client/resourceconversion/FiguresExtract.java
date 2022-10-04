package momime.client.resourceconversion;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.ndg.graphics.ImageCache;

/**
 * Extracts images of all the figures from the original LBX files
 */
public final class FiguresExtract
{
	/** Location of MoM IME Client project sources */
	private final static String CLIENT_PROJECT_ROOT = "W:\\EclipseHome\\SF\\MoMIME\\Client";
	
	/** Cache for locating files and handling archives */
	private ImageCache cache;
	
	/**
	 * Converts a single image
	 * 
	 * @param imageFile Name of image file (probably .LBX)
	 * @param subFileNumber Number of the sub file within the archive
	 * @param frameNumber Number of the frame within the subfile
	 * @param destName Folder and name appropriate to call the saved .png
	 * @return Unit image with original shadow removed
	 * @throws IOException If there is a problem
	 */
	private final BufferedImage convertImage (final String imageFile, final Integer subFileNumber, final int frameNumber, final String destName) throws IOException
	{
		// Read image
		final BufferedImage image = cache.findOrLoadImage (imageFile, subFileNumber, frameNumber);
		
		// Remove the shadows from the original image
		boolean shadowRemoved = false;
		for (int y = 0; y < image.getHeight (); y++)
			for (int x = 0; x < image.getWidth (); x++)
			{
				final int c = image.getRGB (x, y);
				if (c == 0x80000000)
				{
					image.setRGB (x, y, 0);
					shadowRemoved = true;
				}
			}

		// See if file already exists; if so see if its identical, in which case no point overwriting it
		boolean identical = false;
		final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\" + destName + (shadowRemoved ? "-shadow-removed" : "") + ".png");
		if (destFile.exists ())
		{
			// This can't read the PNGs if read from a File, but somehow works if read from the classpath
			final String resourceName = "/momime.client.graphics/" + destName.replace ('\\', '/') + ".png";
			final BufferedImage existing = ImageIO.read (getClass ().getResource (resourceName));
			
			identical = (existing.getWidth () == image.getWidth ()) && (existing.getHeight () == image.getHeight ());
			int y = 0;
			while ((identical) && (y < existing.getHeight ()))
			{
				int x = 0;
				while ((identical) && (x < existing.getWidth ()))
				{
					final int c1 = image.getRGB (x, y);
					final int c2 = existing.getRGB (x, y);
					if (c1 != c2)
						identical = false;
					
					x++;
				}
				y++;
			}
		}
		
		// Output image
		if (identical)
			System.out.println (imageFile + ", " + subFileNumber + ", " + frameNumber + " -> " + subFileNumber + "-" + frameNumber + " -> " + destFile + " (existing file was identical so left it alone)");
		else			
		{
			destFile.getParentFile ().mkdirs ();
			if (!ImageIO.write (image, "png", destFile))
				throw new IOException ("Failed to save PNG file");
			
			System.out.println (imageFile + ", " + subFileNumber + ", " + frameNumber + " -> " + subFileNumber + "-" + frameNumber + " -> " + destFile);
		}
		
		return image;
	}
	
	/**
	 * @param x X coordinate within shadow
	 * @param y Y coordinate within shadow
	 * @param width Width of shadow image
	 * @param height Height of shadow image
	 * @return Coordinates to read from original image
	 */
	private final Point2D.Double convertShadowCoordinates (final int x, final int y, final int width, final int height)
	{
		double dx = x - 2;
		double dy = y + 1;

		// Turn it upside down
		dy = height - dy;
		
		// Halve the height of it
		dy = dy * 2;
		
		// Skew it to the right
		dx = dx + dy;
		
		// Now it'll be much too far to the left
		dx = dx - (width / 2);
		
		// Source image is half the size
		dx = dx / 2;
		dy = dy / 2;
		
		return new Point2D.Double (dx, dy);
	}
	
	/**
	 * @param image Image to read from
	 * @param x X coordinate to read
	 * @param y Y coordinate to read
	 * @return 0 if pixel is transparent; 0xFF is pixel is solid
	 */
	private final double readPixel (final BufferedImage image, final int x, final int y)
	{
		final int c;
		
		if ((x < 0) || (y < 0) || (x >= image.getWidth ()) || (y >= image.getHeight ()))
			c = 0;
		else
			c = image.getRGB (x, y);
		
		return (c == 0) ? 0 : 0xFF;
	}
	
	/**
	 * @param image Image to read from
	 * @param x X coordinate to read
	 * @param y Y coordinate to read
	 * @return Value averaged between the 4 neighbouring pixels
	 */
	private final double antialias (final BufferedImage image, final double x, final double y)
	{
		final double c;
		final int intX = (int) Math.floor (x);
		final int intY = (int) Math.floor (y);
		
		if (intX == x)
		{
			if (intY == y)
				c = readPixel (image, intX, intY);
			else
			{
				final double c1 = readPixel (image, intX, intY);
				final double c2 = readPixel (image, intX, intY + 1);
				final double ry = y - intY;
				c = ((1 - ry) * c1) + (ry * c2);
			}
		}
		else
		{
			if (intY == y)
			{
				final double c1 = readPixel (image, intX, intY);
				final double c2 = readPixel (image, intX + 1, intY);
				final double rx = x - intX;
				c = ((1 - rx) * c1) + (rx * c2);
			}
			else
			{
				final double c1 = readPixel (image, intX, intY);
				final double c2 = readPixel (image, intX + 1, intY);
				final double c3 = readPixel (image, intX, intY + 1);
				final double c4 = readPixel (image, intX + 1, intY + 1);
				final double rx = x - intX;
				final double ry = y - intY;
				c = ((1 - rx) * (1 - ry) * c1) + (rx * (1 - ry) * c2) + ((1 - rx) * ry * c3) + (rx * ry * c4);
			}
		}
		
		return c;
	}
	
	/**
	 * @param image Unit image with original shadow removed
	 * @param destName Folder and name appropriate to call the saved .png
	 * @throws IOException If there is a problem
	 */
	private final void generateShadow (final BufferedImage image, final String destName) throws IOException
	{
		// Make the shadow images 2x size, since everything in CombatUI is shown double size anyway, and then can generate smoother looking shadows
		// Also we need to make it significantly larger to accomodate the skews
		final BufferedImage shadow = new BufferedImage (image.getWidth () * 4, image.getHeight (), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < shadow.getHeight (); y++)
			for (int x = 0; x < shadow.getWidth (); x++)
			{
				final Point2D.Double coords = convertShadowCoordinates (x, y, shadow.getWidth (), shadow.getHeight ());
				final double c = antialias (image, coords.getX (), coords.getY ());
				
				// Want 0x80000000 for full shadow
				final int c2 = ((int) (c / 2)) << 24;
				shadow.setRGB (x, y, c2);
			}
		
		final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\" + destName + "-shadow.png");
		if (!ImageIO.write (shadow, "png", destFile))
			throw new IOException ("Failed to save shadow PNG file");
	}
	
	/**
	 * Runs the conversion
	 * @throws Exception If there is a problem
	 */
	private final void run () throws Exception
	{
		// Set up cache the same way as in ImageCacheAndFormatsDemo
		cache = new ImageCache ()
		{
			@Override
			public final InputStream getFileAsInputStream (final String fileName) throws IOException
			{
				final InputStream stream;
				if (fileName.toUpperCase ().endsWith (".LBX"))
					stream = new FileInputStream ("C:\\32 bit Program Files\\DosBox - Main\\Magic\\" + fileName);
				else
					throw new IOException ("getFileAsInputStream doesn't know how to locate file \"" + fileName + "\"");

				return stream;
			}
		};
		
		for (int unitNo = 1; unitNo <= 198; unitNo++)
		{
			String s = Integer.valueOf (unitNo).toString ();
			while (s.length () < 3)
				s = "0" + s;
			
			final String unitID = "UN" + s;
			
			for (int d = 1; d <= 8; d++)
			{
				// This gets the images numbered the same as the LBX subfiles, so 0..7 = each direction of unit 1, 8..15 = each direction of unit 2 and so on
				final int figuresIndex = ((unitNo - 1) * 8) + (d - 1);
				final int lbxIndex = (figuresIndex / 120) + 1;
				final int subFileNumber = figuresIndex % 120;
				
				final String lbxName = (lbxIndex < 10) ? ("FIGURES" + lbxIndex + ".LBX") : ("FIGURE" + lbxIndex + ".LBX");
				System.out.println (unitID + " direction " + d + " is in " + lbxName + " sub file " + subFileNumber);

				int frameNumber = 0;
				for (final String frameName : new String [] {"move-frame1", "stand", "move-frame3", "attack"})
				{
					final String imageName = "units\\" + unitID + "\\d" + d + "-" + frameName;
					final BufferedImage image = convertImage (lbxName, subFileNumber, frameNumber, imageName);
					
					// Only do the 4 easy directions for now
					if ((d - 1) % 2 == 0)
						generateShadow (image, imageName);
					
					frameNumber++;
				}
			}
		}
		
		System.out.println ("All done!");
	} 

	/**
	 * Kicks off the conversion
	 * @param args Command line arguments, ignored
	 */
	public final static void main (final String [] args)
	{
		try
		{
			new FiguresExtract ().run ();
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}
}
