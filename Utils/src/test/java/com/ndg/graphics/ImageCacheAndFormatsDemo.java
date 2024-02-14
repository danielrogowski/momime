package com.ndg.graphics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.ndg.graphics.lbx.LbxImageReaderSpi;
import com.ndg.graphics.ndgbmp.NdgBmpReaderSpi;
import com.ndg.graphics.ndgbmp.NdgBmpWriterSpi;
import com.ndg.utils.swing.NdgUIUtilsImpl;

/**
 * Tests the image cache, and that it will load in specialised image formats like .ndgbmp files contained in .ndgarc files
 */
public class ImageCacheAndFormatsDemo
{
	/**
	 * Creates the test frame
	 * @param args Command line arguments - ignored
	 */
	public static void main (final String [] args)
	{
		new NdgUIUtilsImpl ().useNimbusLookAndFeel ();

		// Force registration of .ndgbmp and .lbx formats (externally, this would happen via the META-INF/services method in the JAR file)
		IIORegistry.getDefaultInstance ().registerServiceProvider (new LbxImageReaderSpi ());
		IIORegistry.getDefaultInstance ().registerServiceProvider (new NdgBmpReaderSpi ());
		IIORegistry.getDefaultInstance ().registerServiceProvider (new NdgBmpWriterSpi ());

		// Dump out registry
		for (final String thisFormat : ImageIO.getReaderFormatNames ())
			System.out.println ("Can decode " + thisFormat);

		for (final String thisFormat : ImageIO.getWriterFormatNames ())
			System.out.println ("Can encode " + thisFormat);

		// Load in some images
		try
		{
			final ImageCache cache = new ImageCache () {

				@Override
				public final InputStream getFileAsInputStream (final String fileName) throws IOException
				{
					final InputStream stream;
					if (fileName.endsWith (".ndgarc"))
						stream = new FileInputStream ("W:\\Delphi\\Master of Magic\\New Graphics\\" + fileName);
					else if (fileName.endsWith (".LBX"))
						stream = new FileInputStream ("C:\\32 bit Program Files\\DosBox - Master of Magic\\Magic\\" + fileName);
					else
						throw new IOException ("getFileAsInputStream doesn't know how to locate file \"" + fileName + "\"");

					return stream;
				}
			};

			// Show them on a frame
			final JFrame frame = new JFrame ();
			frame.setTitle ("Image Cache and graphics formats test");
			frame.setSize (800, 600);
			frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);

			final JPanel contentPane = new JPanel ();
			contentPane.setLayout (null);

			/*
			 * DECODING TESTS
			 */

			// ndgbmp image inside a .ndgarc file, with transparency (have to do this first so it appears in front)
			final BufferedImage sampleImageWithTransparency = addImage (cache, "MomAdditionalGraphics.ndgarc", 274, 0, 310, 100, contentPane);

			// ndgbmp image inside a .ndgarc file, with no transparency
			final BufferedImage sampleImageNoTransparency = addImage (cache, "MomAdditionalGraphics.ndgarc", 18, 0, 10, 10, contentPane);

			// .lbx image
			addImage (cache, "BACKGRND.LBX", 6, 0, 350, 10, contentPane);

			// Image showing the blackness/transparency in later frames bug
			for (int frameNumber = 0; frameNumber < 8; frameNumber++)
				addImage (cache, "CITYSCAP.LBX", 48, frameNumber, 350 + (frameNumber * 50), 300, contentPane);
			
			// Terrain.lbx has its own special encoding
			for (int frameNumber = 0; frameNumber < 5; frameNumber++)
			{
				addImage (cache, "TERRAIN.LBX", 0, frameNumber + 2, 350 + (frameNumber * 21), 360, contentPane);
				addImage (cache, "TERRAIN.LBX", 0, frameNumber + 30, 350 + (frameNumber * 21), 379, contentPane);
			}

			/*
			 * ENCODING TESTS
			 */

			// Test the encoder on an image with no transparency
			final BufferedImage decodedImageNoTransparency;
			try (final ByteArrayOutputStream sampleEncodeNoTransparency = new ByteArrayOutputStream ())
			{
				if (!ImageIO.write (sampleImageNoTransparency, "ndg bmp", sampleEncodeNoTransparency))
					throw new IOException ("Failed to encode sample .ndgbmp image with no transparency");
				sampleEncodeNoTransparency.close ();

				// Decode the encodes
				decodedImageNoTransparency = ImageIO.read (new ByteArrayInputStream (sampleEncodeNoTransparency.toByteArray ()));
			}
			
			// Test the encoder on an image with transparency
			final BufferedImage decodedImageWithTransparency;
			try (final ByteArrayOutputStream sampleEncodeWithTransparency = new ByteArrayOutputStream ())
			{
				if (!ImageIO.write (sampleImageWithTransparency, "ndg bmp", sampleEncodeWithTransparency))
					throw new IOException ("Failed to encode sample .ndgbmp image with transparency");
				sampleEncodeWithTransparency.close ();

				// Decode the encodes
				decodedImageWithTransparency = ImageIO.read (new ByteArrayInputStream (sampleEncodeWithTransparency.toByteArray ()));
			}

			// Display them
			{
				final JLabel label = new JLabel ();
				label.setIcon (new ImageIcon (decodedImageNoTransparency));
				label.setSize (decodedImageNoTransparency.getWidth (), decodedImageNoTransparency.getHeight ());
				label.setLocation (600, 100);
				contentPane.add (label);
			}

			{
				final JLabel label = new JLabel ();
				label.setIcon (new ImageIcon (decodedImageWithTransparency));
				label.setSize (decodedImageWithTransparency.getWidth (), decodedImageWithTransparency.getHeight ());
				label.setLocation (310, 150);
				contentPane.add (label, 0);		// 0 puts it in front so we can see the transparency
			}

			// Show the frame
			frame.setContentPane (contentPane);
			frame.setVisible (true);
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}

	/**
	 * Loads an image and creates a label to display it
	 * @param cache Cache to use to load the image
     * @param fileName The filename of the image to load
     * @param subFileNumber The sub file number within the file this image was loaded from
     * @param frameNumber The frame number within the sub file this image was loaded from
	 * @param x Location to position the image
	 * @param y Location to position the image
	 * @param contentPane Panel to display the image on
     * @throws IOException If there is a problem reading the specified file
     * @return The loaded image
	 */
	private static BufferedImage addImage (final ImageCache cache, final String fileName, final int subFileNumber, final int frameNumber, final int x, final int y, final JPanel contentPane)
		throws IOException
	{
		final BufferedImage image = cache.findOrLoadImage (fileName, subFileNumber, frameNumber);

		final JLabel label = new JLabel ();
		label.setIcon (new ImageIcon (image));
		label.setSize (image.getWidth (), image.getHeight ());
		label.setLocation (x, y);

		contentPane.add (label);

		return image;
	}
}