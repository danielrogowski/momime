package momime.client.resourceconversion;

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
	 * @throws IOException If there is a problem
	 */
	private final void convertImage (final String imageFile, final Integer subFileNumber, final int frameNumber, final String destName) throws IOException
	{
		// Read image
		final BufferedImage image = cache.findOrLoadImage (imageFile, subFileNumber, frameNumber);
		
		// See if file already exists
		boolean identical = false;
		
		final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\" + destName + ".png");
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
	}
	
	/**
	 * Runs the conversion
	 * @throws Exception If there is a problem
	 */
	private final void run () throws Exception
	{
		// Set up cache the same way as in ImageCacheAndFormatsDemo
		cache = new ImageCache () {

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
					convertImage (lbxName, subFileNumber, frameNumber, imageName);
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
