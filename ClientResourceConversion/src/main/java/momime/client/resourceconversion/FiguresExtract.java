package momime.client.resourceconversion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

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
import momime.common.database.Unit;

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
	 * @param lbxName Name of .LBX file
	 * @param subFileNumber Number of the sub file within the archive
	 * @param frameNumber Number of the frame within the subfile
	 * @param destName Folder and name appropriate to call the saved .png
	 * @param shadowName Name of shadow file - not that we create it here, but need it to update the XML
	 * @return Unit image with original shadow removed
	 * @throws IOException If there is a problem
	 */
	private final BufferedImage convertImage (final MomDatabase db, final String lbxName, final Integer subFileNumber, final int frameNumber, final String destName, final String shadowName)
		throws IOException
	{
		// Read image
		final BufferedImage image = cache.findOrLoadImage (lbxName, subFileNumber, frameNumber);
		
		// Remove the shadows from the original image
		boolean shadowRemoved = false;
		for (int y = 0; y < image.getHeight (); y++)
			for (int x = 0; x < image.getWidth (); x++)
			{
				final int c = image.getRGB (x, y);
				if (c == 0x80000000)		// This is what LbxImageReader generates for shadows
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
			System.out.println (lbxName + ", " + subFileNumber + ", " + frameNumber + " -> " + subFileNumber + "-" + frameNumber + " -> " + destFile + " (existing file was identical so left it alone)");
			updateXml (db, "/momime.client.graphics/" + destName.replace ('\\', '/') + ".png", null, "/momime.client.graphics/" + shadowName.replace ('\\', '/') + ".png");
		}
		else			
		{
			destFile.getParentFile ().mkdirs ();
			if (!ImageIO.write (image, "png", destFile))
				throw new IOException ("Failed to save PNG file");
			
			System.out.println (lbxName + ", " + subFileNumber + ", " + frameNumber + " -> " + subFileNumber + "-" + frameNumber + " -> " + destFile);
			updateXml (db, "/momime.client.graphics/" + destName.replace ('\\', '/') + ".png", "/momime.client.graphics/" + destName.replace ('\\', '/') + "-shadowless.png",
				"/momime.client.graphics/" + shadowName.replace ('\\', '/') + ".png");
		}
		
		return image;
	}
	
	/**
	 * @param image Unit image with original shadow removed
	 * @param destName Folder and name appropriate to call the saved .png
	 * @throws IOException If there is a problem
	 */
	private final void generateShadow (final BufferedImage image, final String destName) throws IOException
	{
		// Make the shadow same size as the original image - CombatUI stretches it out to twice width when it draws it but no need to store it like that
		final BufferedImage shadow = new BufferedImage (image.getWidth (), image.getHeight (), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < shadow.getHeight (); y++)
			for (int x = 0; x < shadow.getWidth (); x++)
			{
				final int c = image.getRGB (x, y);
				
				// 0 is fully transparent; black borders on units usually aren't completely black (see first entry in MOM_PALETTE)
				if ((c != 0) && (c != 0xFF000000) && (c != 0xFF080404))
					shadow.setRGB (x, y, 0x80000000);
			}
		
		final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\" + destName + ".png");
		if (!ImageIO.write (shadow, "png", destFile))
			throw new IOException ("Failed to save shadow PNG file");
	}
	
	/**
	 * @param unitDef Unit definition
	 * @return Standard shadow name if we don't want to generate a shadow; null if we want to use algorithm to generate a shadow 
	 */
	private final String getStandardShadowName (final Unit unitDef)
	{
		final String unitName = unitDef.getUnitName ().get (0).getText ();
		final String shadowName;
		
		// Exception as the one flying hero so doesn't use a standard horse shadow
		if (unitDef.getUnitID ().equals ("UN018"))
			shadowName = null;
		
		else if ((unitName.toLowerCase ().contains ("cavalry")) || (unitName.toLowerCase ().contains ("centaurs")) || (unitName.toLowerCase ().contains ("nightmares")) ||
			(unitName.toLowerCase ().contains ("wolf riders")) || (unitName.toLowerCase ().contains ("elven lords")) || (unitName.toLowerCase ().contains ("paladins")) ||
			(unitName.toLowerCase ().contains ("horse")) || (unitName.toLowerCase ().contains ("hound")) || (unitName.toLowerCase ().contains ("death knights")) ||
			(unitName.toLowerCase ().contains ("unicorns")) || (unitDef.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO)))
			
			shadowName = "narrow";

		else if ((unitName.toLowerCase ().contains ("settlers")) || (unitName.toLowerCase ().contains ("warship")))
			shadowName = "wide";
		
		else if ((unitName.toLowerCase ().contains ("trireme")) || (unitName.toLowerCase ().contains ("galley")) || (unitName.toLowerCase ().contains ("ship")))
			shadowName = "long";
		
		else if ((unitName.toLowerCase ().contains ("catapult")) || (unitName.toLowerCase ().contains ("cannon")))
			shadowName = "square";

		else if ((unitName.toLowerCase ().contains ("basilisk")) || (unitName.toLowerCase ().contains ("hydra")) || (unitName.toLowerCase ().contains ("behemoth")) ||
			(unitName.toLowerCase ().contains ("stag beetle")) || (unitName.toLowerCase ().contains ("dragon turtle")))
			
			shadowName = "huge";

		else if ((unitName.toLowerCase ().contains ("mammoth")))
			shadowName = "fat";

		else if ((unitName.toLowerCase ().contains ("spider")))
			shadowName = "round";
		
		else if ((unitName.toLowerCase ().contains ("nagas")) || (unitName.toLowerCase ().contains ("bear")))
			shadowName = "short";
		
		else
			shadowName = null;
			
		return shadowName;
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
		final Map<String, Unit> unitsMap = db.getUnit().stream ().collect (Collectors.toMap (u -> u.getUnitID (), u -> u));
		
		for (int unitNo = 1; unitNo <= 198; unitNo++)
		{
			String s = Integer.valueOf (unitNo).toString ();
			while (s.length () < 3)
				s = "0" + s;
			
			final String unitID = "UN" + s;
			final Unit unitDef = unitsMap.get (unitID);
			final String standardShadowName = getStandardShadowName (unitDef);
			
			int frameNumber = 0;
			for (final String frameName : new String [] {"move-frame1", "stand", "move-frame3", "attack"})
			{
				for (int d = 1; d <= 8; d++)
				{
					// This gets the images numbered the same as the LBX subfiles, so 0..7 = each direction of unit 1, 8..15 = each direction of unit 2 and so on
					final int figuresIndex = ((unitNo - 1) * 8) + (d - 1);
					final int lbxIndex = (figuresIndex / 120) + 1;
					final int subFileNumber = figuresIndex % 120;
					
					final String lbxName = (lbxIndex < 10) ? ("FIGURES" + lbxIndex + ".LBX") : ("FIGURE" + lbxIndex + ".LBX");
					System.out.println (unitID + " direction " + d + " is in " + lbxName + " sub file " + subFileNumber);
	
					final String imageName = "units\\" + unitID + "\\d" + d + "-" + frameName;
					final String shadowName;
					if (standardShadowName == null)
						shadowName = imageName + "-shadow";
					else
					{
						final int d2 = (d <= 4) ? d : (d - 4);
						shadowName = "shadows\\" + standardShadowName + "-d" + d2;
					}
					
					final BufferedImage image = convertImage (db, lbxName, subFileNumber, frameNumber, imageName, shadowName);
					
					if (standardShadowName == null)
						generateShadow (image, shadowName);
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
