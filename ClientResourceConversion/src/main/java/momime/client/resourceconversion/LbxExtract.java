package momime.client.resourceconversion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.ndg.graphics.ImageCache;

/**
 * Extracts images out of old .LBX and .NDGARC files and puts them
 * under src/main/resources of the MoMIMEClient project.
 */
public final class LbxExtract
{
	/** Location of MoM IME Client project sources */
	private final static String CLIENT_PROJECT_ROOT = "W:\\EclipseHome\\SourceForge\\MoMIMEClient";
	
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
		// Save as .png
		final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\" + destName + ".png");
		if (destFile.exists ())
			throw new IOException ("File " + destFile + " already exists");

		// Read image
		final BufferedImage image = cache.findOrLoadImage (imageFile, subFileNumber, frameNumber);
		destFile.getParentFile ().mkdirs ();
		if (!ImageIO.write (image, "png", destFile))
			throw new IOException ("Failed to save PNG file");
			
		System.out.println (imageFile + ", " + subFileNumber + ", " + frameNumber + " -> " + subFileNumber + "-" + frameNumber + " -> " + destFile);
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
				if (fileName.toUpperCase ().endsWith (".NDGARC"))
					stream = new FileInputStream ("W:\\Delphi\\Master of Magic\\New Graphics\\" + fileName);
				else if (fileName.toUpperCase ().endsWith (".LBX"))
					stream = new FileInputStream ("C:\\32 bit Program Files\\DosBox - Master of Magic\\Magic\\" + fileName);
				else
					throw new IOException ("getFileAsInputStream doesn't know how to locate file \"" + fileName + "\"");

				return stream;
			}
		};
		
		// Extract from an LBX
		//for (int n = 0; n < 20; n++)
		// convertImage ("MAINSCRN.LBX", 0, n, "ui\\mainMenu\\title-frame" + (n < 9 ? "0" : "") + (n+1));

		convertImage ("NEWGAME.LBX", 54, 0, "ui\\newGame\\divider");
		
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
			new LbxExtract ().run ();
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}
}
