package momime.client.resourceconversion;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.graphics.ImageCache;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.MomDatabase;

/**
 * Extracts images of all the figures from the original LBX files
 */
public final class FiguresExtract
{
	/** Location of MoM IME Client project sources */
	private final static String CLIENT_PROJECT_ROOT = "W:\\EclipseHome\\SF\\MoMIME\\Client";
	
	/** Location of XML to read/update */
	private final static File XML_LOCATION = new File ("W:\\EclipseHome\\SF\\MoMIME\\Server\\src\\external\\resources\\momime.server.database\\Original Master of Magic 1.31 rules.momime.xml");
	
	/** Cache for locating files and handling archives */
	private ImageCache cache;
	
	/**
	 * @param db Database to update
	 * @param baseFilename Base filename - the original image converted as-is from the LBXes, which is what we expect to find already present in the database
	 * @param shadowlessFilename Shadowless filename if we generated one; null means the base image had no shadow pixels in it
	 * @param shadowFilename Shadow filename
	 */
	private final void updateXml (final MomDatabase db, final String baseFilename, final String shadowlessFilename, final String shadowFilename)
	{
		db.getUnit ().forEach (u -> u.getUnitCombatAction ().forEach (a -> a.getUnitCombatImage ().stream ().filter (i -> baseFilename.equals (i.getUnitCombatImageFile ())).forEach (i ->
		{
			i.setUnitCombatShadowlessImageFile (shadowlessFilename);
			i.setUnitCombatShadowImageFile (shadowFilename);
		})));
		
		db.getAnimation ().forEach (a -> a.getFrame ().stream ().filter (f -> baseFilename.equals (f.getImageFile ())).forEach (f ->
		{
			f.setShadowlessImageFile (shadowlessFilename);
			f.setShadowImageFile (shadowFilename);
		}));
	}
	
	/**
	 * Converts a single image
	 * 
	 * @param db Database to update
	 * @param imageFile Name of image file (probably .LBX)
	 * @param subFileNumber Number of the sub file within the archive
	 * @param frameNumber Number of the frame within the subfile
	 * @param destName Folder and name appropriate to call the saved .png
	 * @return Unit image with original shadow removed
	 * @throws IOException If there is a problem
	 */
	private final BufferedImage convertImage (final MomDatabase db, final String imageFile, final Integer subFileNumber, final int frameNumber, final String destName) throws IOException
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
		final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\" + destName + (shadowRemoved ? "-shadowless" : "") + ".png");
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
		{
			System.out.println (imageFile + ", " + subFileNumber + ", " + frameNumber + " -> " + subFileNumber + "-" + frameNumber + " -> " + destFile + " (existing file was identical so left it alone)");
			updateXml (db, "/momime.client.graphics/" + destName.replace ('\\', '/') + ".png", null, "/momime.client.graphics/" + destName.replace ('\\', '/') + "-shadow.png");
		}
		else			
		{
			destFile.getParentFile ().mkdirs ();
			if (!ImageIO.write (image, "png", destFile))
				throw new IOException ("Failed to save PNG file");
			
			System.out.println (imageFile + ", " + subFileNumber + ", " + frameNumber + " -> " + subFileNumber + "-" + frameNumber + " -> " + destFile);
			updateXml (db, "/momime.client.graphics/" + destName.replace ('\\', '/') + ".png", "/momime.client.graphics/" + destName.replace ('\\', '/') + "-shadowless.png",
				"/momime.client.graphics/" + destName.replace ('\\', '/') + "-shadow.png");
		}
		
		return image;
	}
	
	/**
	 * @param x X coordinate within shadow
	 * @param y Y coordinate within shadow
	 * @param height Height of shadow image
	 * @param d Direction the unit is facing
	 * @return Coordinates to read from original image
	 */
	private final Point2D.Double convertShadowCoordinates (final int x, final int y, final int height, final int d)
	{
		double dx = x + 2;
		double dy = y + 1;

		// Doing this manually in PSP, would start with the image and perform operations to produce the shadow image
		// But here we're starting with shadow coordinates, and need to work backwards to the coordinates of the original image, so have to do all the operations in reverse
		
		// Extra processing for diagonal directions
		if (d % 2 == 0)
		{
			// Skew it up/down
			final double skew = ((d == 2) || (d == 6)) ? 0.4 : -0.4;
			dy = dy + (dx * skew);
			
			if (skew > 0)
				dy = dy - (height / 2);
			
			// Make it a bit narrower
			dx = dx * 4d / 3d;
		}

		// Skew it to the right
		dx = dx - (dy * 2);
		
		// Halve the height of it
		dy = dy * 2;
		
		// Turn it upside down
		dy = height - dy;
		
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
	 * @param d Direction the unit is facing
	 * @throws IOException If there is a problem
	 */
	private final void generateShadow (final BufferedImage image, final String destName, final int d) throws IOException
	{
		// Make the shadow images 2x size, since everything in CombatUI is shown double size anyway, and then can generate smoother looking shadows
		// Also we need to make it significantly larger to accomodate the skews
		final BufferedImage shadow = new BufferedImage (image.getWidth () * 4, (image.getHeight () * 11) / 5, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < shadow.getHeight (); y++)
			for (int x = 0; x < shadow.getWidth (); x++)
			{
				final Point2D.Double coords = convertShadowCoordinates (x, y, image.getHeight () * 2, d);
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
		
		// Read XML
		final URL xsdResource = getClass ().getResource (CommonDatabaseConstants.COMMON_XSD_LOCATION);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Schema schema = schemaFactory.newSchema (xsdResource);

		final Unmarshaller unmarshaller = JAXBContext.newInstance (MomDatabase.class).createUnmarshaller ();		
		unmarshaller.setSchema (schema);
		
		final MomDatabase db = (MomDatabase) unmarshaller.unmarshal (XML_LOCATION);

		for (int unitNo = 1; unitNo <= 198; unitNo++)
		{
			String s = Integer.valueOf (unitNo).toString ();
			while (s.length () < 3)
				s = "0" + s;
			
			final String unitID = "UN" + s;
			
			int frameNumber = 0;
			for (final String frameName : new String [] {"move-frame1", "stand", "move-frame3", "attack"})
			{
				// Do the 4 easy directions first, and put the shadowless images in a map
				final Map<Integer, BufferedImage> images = new HashMap<Integer, BufferedImage> ();
				for (int d = 1; d <= 8; d = d + 2)
				{
					// This gets the images numbered the same as the LBX subfiles, so 0..7 = each direction of unit 1, 8..15 = each direction of unit 2 and so on
					final int figuresIndex = ((unitNo - 1) * 8) + (d - 1);
					final int lbxIndex = (figuresIndex / 120) + 1;
					final int subFileNumber = figuresIndex % 120;
					
					final String lbxName = (lbxIndex < 10) ? ("FIGURES" + lbxIndex + ".LBX") : ("FIGURE" + lbxIndex + ".LBX");
					System.out.println (unitID + " direction " + d + " is in " + lbxName + " sub file " + subFileNumber);
	
					final String imageName = "units\\" + unitID + "\\d" + d + "-" + frameName;
					final BufferedImage image = convertImage (db, lbxName, subFileNumber, frameNumber, imageName);
					images.put (d, image);
					
					generateShadow (image, imageName, d);
				}
				
				// Then do the 4 diagonals
				for (int d = 2; d <= 8; d = d + 2)
				{
					// Still have to call convertImage to generate a shadowless main image, even though we then don't use it for generating the shadow
					final int figuresIndex = ((unitNo - 1) * 8) + (d - 1);
					final int lbxIndex = (figuresIndex / 120) + 1;
					final int subFileNumber = figuresIndex % 120;
					
					final String lbxName = (lbxIndex < 10) ? ("FIGURES" + lbxIndex + ".LBX") : ("FIGURE" + lbxIndex + ".LBX");
					System.out.println (unitID + " direction " + d + " is in " + lbxName + " sub file " + subFileNumber);
	
					final String imageName = "units\\" + unitID + "\\d" + d + "-" + frameName;
					convertImage (db, lbxName, subFileNumber, frameNumber, imageName);
					
					// Use the image from the unit facing sideways to generate the shadow from
					final int d2 = (d <= 4) ? 3 : 7;
					final BufferedImage image = images.get (d2);
					generateShadow (image, imageName, d);
				}
				
				frameNumber++;
			}
		}
		
		// Save XML back out
		final Marshaller marshaller = JAXBContext.newInstance (MomDatabase.class).createMarshaller ();		
		marshaller.setSchema (schema);
		marshaller.setProperty (Marshaller.JAXB_FORMATTED_OUTPUT, true);
		
		marshaller.marshal (db, XML_LOCATION);
		
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
