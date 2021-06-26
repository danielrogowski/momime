package momime.client.resourceconversion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Looks for the green pixels in original MoM images that should be replaced by the wizard's colour
 */
public final class FlagChecker
{
	/** Folder containing flag images */
	private final static File FLAGS_FOLDER = new File ("W:\\EclipseHome\\SourceForge\\MoMIME\\Client\\src\\main\\resources\\momime.client.graphics\\flags");
	
	/** Green flag pixel colours */
	private final static int GREEN_1 = 0xFF00BC00;
	
	/** Green flag pixel colours */
	private final static int GREEN_2 = 0xFF007C00;
	
	/** Green flag pixel colours */
	private final static int GREEN_3 = 0xFF00A400;
	
	/** List of all green flag pixel colours */
	private final static List<Integer> GREENS = Arrays.asList (GREEN_1, GREEN_2, GREEN_3);

	/** White flag pixel colours */
	private final static int WHITE_1 = 0xFFFFFFFF;
	
	/** White flag pixel colours */
	private final static int WHITE_2 = 0xFFBEBEBE;
	
	/** White flag pixel colours */
	private final static int WHITE_3 = 0xFFE6E6E6;
	
	/**
	 * @param image1 First image to compare
	 * @param image2 Second image to compare
	 * @return Whether images are identical
	 */
	public final boolean imagesMatch (final BufferedImage image1, final BufferedImage image2)
	{
		boolean match = (image1.getWidth () == image2.getWidth ()) && (image1.getHeight () == image2.getHeight ());
		
		int y = 0;
		while ((match) && (y < image1.getHeight ()))
		{
			int x = 0;
			while ((match) && (x < image1.getWidth ()))
			{
				final int colour1 = image1.getRGB (x, y);
				final int colour2 = image2.getRGB (x,  y);
				final int alpha1 = colour1 & 0xFF000000;
				final int alpha2 = colour2 & 0xFF000000;
				
				if (((alpha1 == 0) && (alpha2 == 0)) || (colour1 == colour2))
				{
					// match
				}
				else
					match = false;
				
				x++;
			}
			y++;
		}
		
		return match;
	}
	
	/**
	 * @param image Image to search for
	 * @return Name of existing flag image that this one matches; if matched none then returns null
	 * @throws IOException If there is a problem
	 */
	public final String matchesAnyExistingImage (final BufferedImage image) throws IOException
	{
		String match = null;
		
		final File [] files = FLAGS_FOLDER.listFiles ();
		for (final File file : files)
			if ((match == null) && (!file.isDirectory ()) && (file.getName ().endsWith (".png")))
				try (final FileInputStream in = new FileInputStream (file))
				{
					final BufferedImage testImage = ImageIO.read (in);
					if (imagesMatch (image, testImage))
						match = file.getName ();
				}
		
		return match;
	}
	
	/**
	 * @param imageFile PNG file to check
	 * @param imageName Name of this image, just to display in messages
	 * @param flagName Filename to save the flag as, if it turns out to be new 
	 * @throws IOException If there is a problem
	 */
	public final void checkImage (final File imageFile, final String imageName, final String flagName) throws IOException
	{
		// For some bizarre reason, passing ImageIO.read a file fails, but passing it an input stream works fine
		try (final FileInputStream in = new FileInputStream (imageFile))
		{
			final BufferedImage image = ImageIO.read (in);

			// 1st pass - scan image for green pixels - see if there is any at all
			Integer left = null;
			Integer top = null;
			Integer right = null;
			Integer bottom = null;
			
			for (int y = 0; y < image.getHeight (); y++)
				for (int x = 0; x < image.getWidth (); x++)
					if (GREENS.contains (image.getRGB (x,  y)))
					{
						if ((left == null) || (x < left))
							left = x;

						if ((right == null) || (x > right))
							right = x;

						if ((top == null) || (y < top))
							top = y;

						if ((bottom == null) || (y > bottom))
							bottom = y;
					}
			
			if (left != null)
			{
				final int flagWidth = right - left + 1;
				final int flagHeight = bottom - top + 1;
				
				// Make an image of the flag, converting it to white
				final BufferedImage flagImage = new BufferedImage (flagWidth, flagHeight, BufferedImage.TYPE_INT_ARGB);
				for (int y = 0; y < flagHeight; y++)
					for (int x = 0; x < flagWidth; x++)
					{
						final int srcX = x + left;
						final int srcY = y + top;
						final int colour = image.getRGB (srcX,  srcY);
						
						switch (colour)
						{
							case GREEN_1: flagImage.setRGB (x, y, WHITE_1); break;
							case GREEN_2: flagImage.setRGB (x, y, WHITE_2); break;
							case GREEN_3: flagImage.setRGB (x, y, WHITE_3); break;
							default:
								flagImage.setRGB (x, y, 0);								
						}
					}
				
				// Search for an exact match
				final String match = matchesAnyExistingImage (flagImage);
				if (match != null)
					System.out.println (imageName + " contains flag pixels starting at (" + left + ", " + top + ") of size " + flagWidth + " x " + flagHeight + " matches existing " + match);
				else
				{
					System.out.println (imageName + " contains flag pixels starting at (" + left + ", " + top + ") of size " + flagWidth + " x " + flagHeight + " is new, adding to flags folder named " + flagName);
					ImageIO.write (flagImage, "png", new File (FLAGS_FOLDER, flagName));
				}
			}
		}
	}
	
	/**
	 * Finds the PNGs files we want to check
	 * @throws IOException If there is a problem
	 */
	public final void run () throws IOException
	{
		// Check city images
		final File citiesFolder = new File ("W:\\EclipseHome\\SourceForge\\MoMIME\\Client\\src\\main\\resources\\momime.client.graphics\\overland\\cities");
		
		final File [] files = citiesFolder.listFiles ();
		for (final File file : files)
			if ((!file.isDirectory ()) && (file.getName ().endsWith (".png")))
				checkImage (file, file.getName (), file.getName ());
		
		// Check unit overland images
		for (int n = 1; n < 198; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 3)
				s = "0" + s;
			
			final File unitImage = new File ("W:\\EclipseHome\\SourceForge\\MoMIME\\Client\\src\\main\\resources\\momime.client.graphics\\units\\UN" + s + "\\overland.png");
			checkImage (unitImage, "UN" + s + "\\overland.png", "UN" + s + ".png");
		}
	}
	
	/**
	 * @param args Command line arguments, ignored
	 */
	public final static void main (final String [] args)
	{
		try
		{
			new FlagChecker ().run ();
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}
}