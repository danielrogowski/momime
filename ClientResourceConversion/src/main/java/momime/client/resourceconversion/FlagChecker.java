package momime.client.resourceconversion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import momime.common.database.Animation;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.MomDatabase;
import momime.common.database.Unit;
import momime.common.database.UnitCombatAction;
import momime.common.database.UnitCombatImage;

/**
 * Looks for the green pixels in original MoM images that should be replaced by the wizard's colour
 */
public final class FlagChecker
{
	/** Folder containing flag images */
	private final static File FLAGS_FOLDER = new File ("W:\\EclipseHome\\SourceForge\\MoMIME\\Client\\src\\main\\resources\\momime.client.graphics\\flags");
	
	/** Location of the flags folder as a resource for the MoM client module */
	private static final String FLAG_RESOURCE_PREFIX = "/momime.client.graphics/flags/";
	
	/** Location of XML to read/update */
	private final static File XML_LOCATION = new File ("W:\\EclipseHome\\SourceForge\\MoMIME\\Server\\src\\external\\resources\\momime.server.database\\Original Master of Magic 1.31 rules.momime.xml");
	
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
	 * @return Correct image name and offsets for flag, or null if the supplied image has no flag colour pixels 
	 * @throws IOException If there is a problem
	 */
	public final FlagCheckResult checkImage (final File imageFile, final String imageName, final String flagName) throws IOException
	{
		FlagCheckResult result = null;
		
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
				{
					System.out.println (imageName + " contains flag pixels starting at (" + left + ", " + top + ") of size " + flagWidth + " x " + flagHeight + " matches existing " + match);
					result = new FlagCheckResult (FLAG_RESOURCE_PREFIX + match, left, top);
				}
				else
				{
					System.out.println (imageName + " contains flag pixels starting at (" + left + ", " + top + ") of size " + flagWidth + " x " + flagHeight + " is new, adding to flags folder named " + flagName);
					ImageIO.write (flagImage, "png", new File (FLAGS_FOLDER, flagName));
					result = new FlagCheckResult (FLAG_RESOURCE_PREFIX + flagName, left, top);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Finds the PNGs files we want to check
	 * @throws Exception If there is a problem
	 */
	public final void run () throws Exception
	{
		// Read XML
		final URL xsdResource = getClass ().getResource (CommonDatabaseConstants.COMMON_XSD_LOCATION);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Schema schema = schemaFactory.newSchema (xsdResource);

		final Unmarshaller unmarshaller = JAXBContext.newInstance (MomDatabase.class).createUnmarshaller ();		
		unmarshaller.setSchema (schema);
		
		final MomDatabase db = (MomDatabase) unmarshaller.unmarshal (XML_LOCATION);
		final Map<String, Unit> unitsMap = db.getUnit ().stream ().collect (Collectors.toMap (u -> u.getUnitID (), u -> u));
		final Map<String, Animation> animsMap = db.getAnimation ().stream ().collect (Collectors.toMap (a -> a.getAnimationID (), a -> a));
		
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

			final String unitID = "UN" + s;
			final Unit unitDef = unitsMap.get (unitID);
			
			final File unitImage = new File ("W:\\EclipseHome\\SourceForge\\MoMIME\\Client\\src\\main\\resources\\momime.client.graphics\\units\\UN" + s + "\\overland.png");
			final FlagCheckResult result = checkImage (unitImage, unitID + "\\overland.png", unitID + ".png");
			
			// Check overland flag details in XML to make sure they're correct
			if (result == null)
			{
				if ((unitDef.getUnitOverlandImageFlag () != null) || (unitDef.getFlagOffsetX () != null) || (unitDef.getFlagOffsetY () != null))
				{
					unitDef.setUnitOverlandImageFlag (null);
					unitDef.setFlagOffsetX (null);
					unitDef.setFlagOffsetY (null);
					System.out.println ("   " + unitID + " blanked out overland flag image");
				}
			}
			else
			{
				if ((unitDef.getUnitOverlandImageFlag () == null) || (unitDef.getFlagOffsetX () == null) || (unitDef.getFlagOffsetY () == null) ||
					(!unitDef.getUnitOverlandImageFlag ().equals (result.getFlagImage ())) || (unitDef.getFlagOffsetX () != result.getFlagOffsetX ()) || (unitDef.getFlagOffsetY () != result.getFlagOffsetY ()))
				{
					unitDef.setUnitOverlandImageFlag (result.getFlagImage ());
					unitDef.setFlagOffsetX (result.getFlagOffsetX ());
					unitDef.setFlagOffsetY (result.getFlagOffsetY ());
					System.out.println ("   " + unitID + " updated overland flag image");
				}
			}
		}
		
		// Check unit combat images
		for (int n = 1; n < 198; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 3)
				s = "0" + s;
			
			final String unitID = "UN" + s;
			final Unit unitDef = unitsMap.get (unitID);
			
			for (int d = 1; d < 8; d++)
				for (String f : new String [] {"stand", "attack", "move-frame1", "move-frame3"})
				{
					final File unitImage = new File ("W:\\EclipseHome\\SourceForge\\MoMIME\\Client\\src\\main\\resources\\momime.client.graphics\\units\\" + unitID +
						"\\d" + d + "-" + f + ".png");
					final FlagCheckResult result = checkImage (unitImage, unitID + "\\d" + d + "-" + f + ".png", unitID + "-d" + d + "-" + f + ".png");

					// Check combat flag details in XML to make sure they're correct
					for (final UnitCombatAction action : unitDef.getUnitCombatAction ())
						for (final UnitCombatImage image : action.getUnitCombatImage ())
							if (image.getDirection () == d)
							{
								// Check static image
								if ((image.getUnitCombatImageFile () != null) && (image.getUnitCombatImageFile ().equals ("/momime.client.graphics/units/" + unitID + "/d" + d + "-" + f + ".png")))
								{
									if (result == null)
									{
										if ((image.getUnitCombatImageFlag () != null) || (image.getFlagOffsetX () != null) || (image.getFlagOffsetY () != null))
										{
											image.setUnitCombatImageFlag (null);
											image.setFlagOffsetX (null);
											image.setFlagOffsetY (null);
											System.out.println ("   " + unitID + " action " + action.getCombatActionID () + " direction " + d + " blanked out combat flag image");
										}
									}
									else
									{
										if ((image.getUnitCombatImageFlag () == null) || (image.getFlagOffsetX () == null) || (image.getFlagOffsetY () == null) ||
											(!image.getUnitCombatImageFlag ().equals (result.getFlagImage ())) || (image.getFlagOffsetX () != result.getFlagOffsetX ()) || (image.getFlagOffsetY () != result.getFlagOffsetY ()))
										{
											image.setUnitCombatImageFlag (result.getFlagImage ());
											image.setFlagOffsetX (result.getFlagOffsetX ());
											image.setFlagOffsetY (result.getFlagOffsetY ());
											System.out.println ("   " + unitID + " action " + action.getCombatActionID () + " direction " + d + " updated combat flag image");
										}
									}
								}
								
								// Check animation
								if (image.getUnitCombatAnimation () != null)
								{
									final Animation anim = animsMap.get (image.getUnitCombatAnimation ());
									
									int frameNumber = 0;
									for (final AnimationFrame frame : anim.getFrame ())
									{
										frameNumber++;
										
										if (frame.getImageFile ().equals ("/momime.client.graphics/units/" + unitID + "/d" + d + "-" + f + ".png"))
										{
											if (result == null)
											{
												if ((frame.getImageFlag () != null) || (frame.getFlagOffsetX () != null) || (frame.getFlagOffsetY () != null))
												{
													frame.setImageFlag (null);
													frame.setFlagOffsetX (null);
													frame.setFlagOffsetY (null);
													System.out.println ("   " + unitID + " action " + action.getCombatActionID () + " direction " + d + " animation " +
														image.getUnitCombatAnimation () + " frame " + frameNumber + " blanked out anim flag image");
												}
											}
											else
											{
												if ((frame.getImageFlag () == null) || (frame.getFlagOffsetX () == null) || (frame.getFlagOffsetY () == null) ||
													(!frame.getImageFlag ().equals (result.getFlagImage ())) || (frame.getFlagOffsetX () != result.getFlagOffsetX ()) || (frame.getFlagOffsetY () != result.getFlagOffsetY ()))
												{
													frame.setImageFlag (result.getFlagImage ());
													frame.setFlagOffsetX (result.getFlagOffsetX ());
													frame.setFlagOffsetY (result.getFlagOffsetY ());
													System.out.println ("   " + unitID + " action " + action.getCombatActionID () + " direction " + d + " animation " +
														image.getUnitCombatAnimation () + " frame " + frameNumber + " updated anim flag image");
												}
											}
										}
									}
								}
							}
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