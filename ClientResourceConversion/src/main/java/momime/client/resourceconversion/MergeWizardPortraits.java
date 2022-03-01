package momime.client.resourceconversion;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.ndg.graphics.ImageCache;

/**
 * Wizard portraits in WIZARDS.LBX are 109x104
 * Wizard happy/mad portraits in MOODWIZ.LBX are varying sizes around 109-110 x 109-110 and have the circular mirror edge around them
 * Wizards talking in DIPLOMAC.LBX are same 
 *
 * So this tries to sort all this mess out and use the clean base images from WIZARDS.LBX and superimpose only the bits that
 * change so we get all clean images and all the same size
 */
public final class MergeWizardPortraits
{
	/** Location of MoM IME Client project sources */
	private final static String CLIENT_PROJECT_ROOT = "W:\\EclipseHome\\SF\\MoMIME\\Client";
	
	/** Size of area in middle of the image used to line up the 2 image */
	private final static int IDENTIFY_SIZE = 10;
	
	/** Size of square that is copied onto the base image; with corners chopped off */
	private final static int COPY_SIZE = 70;
	
	/** Size of corners ignored */
	private final static int CORNER_SIZE = 20;
	
	/**
	 * @param cleanBaseImage Base image in which we are trying to position the 2nd image
	 * @param baseCircleImage Image that we'll use a portion of and search for it in the 1st image
	 * @return Match location if one was found, null if not found
	 */
	private final Point lineUpImages (final BufferedImage cleanBaseImage, final BufferedImage baseCircleImage)
	{
		// Portion of the circle image to search for
		final int baseCircleImageOffsetX = ((baseCircleImage.getWidth () - IDENTIFY_SIZE) / 2) + 1;		// Ariel's nose has 1 pixel different, this area just happens to work on all
		final int baseCircleImageOffsetY = (baseCircleImage.getHeight () - IDENTIFY_SIZE) / 2;
		
		// Scan all possible search locations
		Point location = null;
		int searchY = 0;
		while ((location == null) && (searchY < cleanBaseImage.getHeight () - IDENTIFY_SIZE))
		{
			int searchX = 0;
			while ((location == null) && (searchX < cleanBaseImage.getWidth () - IDENTIFY_SIZE))
			{
				// Match here?
				boolean match = true;
				int y = 0;
				while ((match) && (y < IDENTIFY_SIZE))
				{
					int x = 0;
					while ((match) && (x < IDENTIFY_SIZE))
					{
						final int c = baseCircleImage.getRGB (baseCircleImageOffsetX + x, baseCircleImageOffsetY + y);
						final int d = cleanBaseImage.getRGB (searchX + x, searchY + y);
						if (c != d)
							match = false;
						
						x++;
					}
					y++;
				}
				
				if (match)
				{
					location = new Point (searchX - baseCircleImageOffsetX, searchY - baseCircleImageOffsetY);
					System.out.println ("   Found match at " + location.x + ", " + location.y);
				}
				
				searchX++;
			}
			searchY++;
		}
		
		if (location == null)
			System.out.println ("   No match found");
		
		return location;
	}
	
	/**
	 * @param cleanBaseImage Base image to copy
	 * @param copyFromImage 2nd image to superimpose a region of onto base image
	 * @param offset Alignment between the two images found by lineUpImages above
	 * @return Generated image
	 */
	private final BufferedImage generateImage (final BufferedImage cleanBaseImage, final BufferedImage copyFromImage, final Point offset)
	{
		// Copy base image
		final BufferedImage dest = new BufferedImage (cleanBaseImage.getWidth (), cleanBaseImage.getHeight (), cleanBaseImage.getType ());
		for (int y = 0; y < cleanBaseImage.getHeight (); y++)
			for (int x = 0; x < cleanBaseImage.getWidth (); x++)
				dest.setRGB (x, y, cleanBaseImage.getRGB (x, y));
		
		// Copy portion of 2nd image over the top
		final int destOffsetX = (cleanBaseImage.getWidth () - COPY_SIZE) / 2; 
		final int destOffsetY = (cleanBaseImage.getHeight () - COPY_SIZE) / 2;
		final int sourceOffsetX = destOffsetX - offset.x; 
		final int sourceOffsetY = destOffsetY - offset.y;
		
		for (int y = 0; y < COPY_SIZE; y++)
			for (int x = 0; x < COPY_SIZE; x++)
				if ((x + y > CORNER_SIZE) && ((COPY_SIZE - x) + y > CORNER_SIZE) && (x + (COPY_SIZE - y) > CORNER_SIZE) &&
					((COPY_SIZE - x) + (COPY_SIZE - y) > CORNER_SIZE))
				{
					final int c = copyFromImage.getRGB (sourceOffsetX + x, sourceOffsetY + y);
					dest.setRGB (destOffsetX + x, destOffsetY + y, c);
				}
		
		return dest;
	}
	
	/**
	 * Runs the conversion
	 * @throws Exception If there is a problem
	 */
	private final void run () throws Exception
	{
		final ImageCache cache = new ImageCache () {

			@Override
			public final InputStream getFileAsInputStream (final String fileName) throws IOException
			{
				final InputStream stream;
				if (fileName.toUpperCase ().endsWith (".NDGARC"))
					stream = new FileInputStream ("W:\\Delphi\\Master of Magic\\New Graphics\\" + fileName);
				else if (fileName.toUpperCase ().endsWith (".LBX"))
					stream = new FileInputStream ("C:\\32 bit Program Files\\DosBox - Master of Magic\\Magic\\" + fileName);
				else
					throw new IOException ("getFileAsInputStream doesn't know how to locate file \"" + fileName + "\"");

				return stream;
			}
		};
		
		for (int w = 0; w <= 13; w++)
		{
			String t = Integer.valueOf (w+1).toString ();
			while (t.length () < 2)
				t = "0" + t;
			
			final BufferedImage cleanBaseImage = cache.findOrLoadImage ("WIZARDS.LBX", w, 0);
			
			// Happy/mad faces
			/* final BufferedImage baseCircleImage = cache.findOrLoadImage ("MOODWIZ.LBX", w, 2);
			
			System.out.println ("WZ" + t + " clean image is " + cleanBaseImage.getWidth () + " x " + cleanBaseImage.getHeight () + ", circle image is " + baseCircleImage.getWidth () + " x " + baseCircleImage.getHeight ());
			final Point offset = lineUpImages (cleanBaseImage, baseCircleImage);
			if (offset != null)
			{
				// Generate new images
				for (int i = 0; i < 2; i++)
				{
					final BufferedImage copyFromImage = cache.findOrLoadImage ("MOODWIZ.LBX", w, i);
					final BufferedImage newImage = generateImage (cleanBaseImage, copyFromImage, offset);
					
					final String suffix = (i == 0) ? "happy" : "mad";
					final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\wizards\\WZ" + t + "-" + suffix + ".png");
					if (!ImageIO.write (newImage, "png", destFile))
						throw new IOException ("Failed to save PNG file");
				}
			} */
			
			// Diplomacy talking aces
			final BufferedImage baseCircleImage = cache.findOrLoadImage ("DIPLOMAC.LBX", 24 + w, 0);
			
			System.out.println ("WZ" + t + " clean image is " + cleanBaseImage.getWidth () + " x " + cleanBaseImage.getHeight () + ", circle image is " + baseCircleImage.getWidth () + " x " + baseCircleImage.getHeight ());
			final Point offset = lineUpImages (cleanBaseImage, baseCircleImage);
			if (offset != null)
			{
				// Generate new images
				for (int i = 0; i < 25; i++)
				{
					final BufferedImage copyFromImage = cache.findOrLoadImage ("DIPLOMAC.LBX", 24 + w, i);
					String s = Integer.valueOf (i+1).toString ();
					while (s.length () < 2)
						s = "0" + s;
					
					final BufferedImage newImage = generateImage (cleanBaseImage, copyFromImage, offset);
					
					final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\wizards\\WZ" + t + "-diplomacy-frame-" + s + ".png");
					if (!ImageIO.write (newImage, "png", destFile))
						throw new IOException ("Failed to save PNG file");
				}
			}
			
			System.out.println ("");
		}
	}
	
	/**
	 * Kicks off the conversion
	 * @param args Command line arguments, ignored
	 */
	public final static void main (final String [] args)
	{
		try
		{
			new MergeWizardPortraits ().run ();
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}
}