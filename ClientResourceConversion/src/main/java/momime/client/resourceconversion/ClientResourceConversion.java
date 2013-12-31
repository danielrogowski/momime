package momime.client.resourceconversion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;

import momime.client.graphics.database.v0_9_4.Animation;
import momime.client.graphics.database.v0_9_4.AnimationFrame;
import momime.client.graphics.database.v0_9_4.BookImage;
import momime.client.graphics.database.v0_9_4.CityImage;
import momime.client.graphics.database.v0_9_4.CityViewElement;
import momime.client.graphics.database.v0_9_4.CombatAreaEffect;
import momime.client.graphics.database.v0_9_4.CombatTileBorderImage;
import momime.client.graphics.database.v0_9_4.GraphicsDatabase;
import momime.client.graphics.database.v0_9_4.MapFeature;
import momime.client.graphics.database.v0_9_4.Pick;
import momime.client.graphics.database.v0_9_4.ProductionType;
import momime.client.graphics.database.v0_9_4.ProductionTypeImage;
import momime.client.graphics.database.v0_9_4.Race;
import momime.client.graphics.database.v0_9_4.RacePopulationTask;
import momime.client.graphics.database.v0_9_4.RangedAttackType;
import momime.client.graphics.database.v0_9_4.RangedAttackTypeCombatImage;
import momime.client.graphics.database.v0_9_4.SmoothedTile;
import momime.client.graphics.database.v0_9_4.SmoothedTileType;
import momime.client.graphics.database.v0_9_4.Spell;
import momime.client.graphics.database.v0_9_4.TileSet;
import momime.client.graphics.database.v0_9_4.TileType;
import momime.client.graphics.database.v0_9_4.Unit;
import momime.client.graphics.database.v0_9_4.UnitAttribute;
import momime.client.graphics.database.v0_9_4.UnitCombatAction;
import momime.client.graphics.database.v0_9_4.UnitCombatImage;
import momime.client.graphics.database.v0_9_4.UnitSkill;
import momime.client.graphics.database.v0_9_4.Wizard;

import com.ndg.graphics.ImageCache;

/**
 * This extracts all the required images out of the old .LBX and .NDGARC files and puts them
 * under src/main/resources of the MoMIMEClient project.
 */
public final class ClientResourceConversion
{
	/** Location of MoM IME Client project sources */
	private final static String CLIENT_PROJECT_ROOT = "W:\\EclipseHome\\SourceForge\\MoMIMEClient";
	
	/** Cache for locating files and handling archives */
	private ImageCache cache;
	
	/** Stores existing images that have been exported and what name they were saved as */
	private Map<String, File> exportedImageNames;
	
	/** Graphics DB XML file */
	private GraphicsDatabase xml;

	/**
	 * Converts a single image or animation of images
	 * 
	 * @param imageFile Name of image file (probably .LBX), null if its an animation
	 * @param imageNumber Number of the image within the file, for files that can contain multiple, null if its an animation
	 * @param animationID ID of animation, null if its a single image
	 * @param alreadyExported Whether we expect that this image has already been exported under a different name, due to two entries in the XML pointing at the same file/image number
	 * @param destName Folder and name appropriate to call the saved .png
	 * @throws IOException If there is a problem
	 */
	private final void convertImageOrAnimation (final String imageFile, final Integer imageNumber, final String animationID,
		final boolean alreadyExported, final String destName) throws IOException
	{
		if ((imageFile != null) && (imageNumber != null))
			convertImage (imageFile, imageNumber, alreadyExported, destName);
		
		else if (animationID != null)
		{
			// Find the animation
			Animation anim = null;
			final Iterator<Animation> animIter = xml.getAnimation ().iterator ();
			while ((anim == null) && (animIter.hasNext ()))
			{
				final Animation thisAnim = animIter.next ();
				if (thisAnim.getAnimationID ().equals (animationID))
					anim = thisAnim;
			}
			
			if (anim == null)
				throw new IOException ("AnimationID " + animationID + " not found");
			
			// Process each frame
			int frameNo = 0;
			for (final AnimationFrame frame : anim.getFrame ())
			{
				frameNo++;
				
				// This is a bit of a hack to account that the ranged attack anims run like ABCB and other anims that need special attention
				final boolean useAlreadyExported;
				if (animationID.startsWith ("RAT"))
					useAlreadyExported = (frameNo == 4);
				else if (animationID.equals ("LIFE_RAYS"))
					useAlreadyExported = ((frameNo == 10) || (frameNo == 12) || (alreadyExported));
				else if (animationID.startsWith ("UN"))
					useAlreadyExported = alreadyExported || ((frame.getFrameImageNumber () % 2) == 1);
				else
					useAlreadyExported = alreadyExported;
				
				// Process this frame
				convertImage (frame.getFrameImageFile (), frame.getFrameImageNumber (), useAlreadyExported, destName + "-frame" + frameNo);
			}
		}			
	}
	
	/**
	 * Converts a single image
	 * 
	 * @param imageFile Name of image file (probably .LBX)
	 * @param imageNumber Number of the image within the file, for files that can contain multiple
	 * @param alreadyExported Whether we expect that this image has already been exported under a different name, due to two entries in the XML pointing at the same file/image number
	 * @param destName Folder and name appropriate to call the saved .png
	 * @throws IOException If there is a problem
	 */
	private final void convertImage (final String imageFile, final Integer imageNumber, final boolean alreadyExported, final String destName) throws IOException
	{
		// This is a bit of a hack, the Delphi code numbered all the frames in an .LBX in only a one-index system but
		// really the file is organized in a two-index system, and how we have to convert one to the other depends on which .LBX it is
		final int subFileNumber;
		final int frameNumber;
		if (imageFile.equals ("TERRAIN.LBX"))
		{
			subFileNumber = 0;
			frameNumber = imageNumber;
		}
		else if ((imageFile.startsWith ("FIGURE")) && (imageFile.endsWith (".LBX")))
		{
			subFileNumber = imageNumber / 4;
			frameNumber = imageNumber % 4;
		}
		else if (imageFile.equals ("BACKGRND.LBX"))
		{
			subFileNumber = imageNumber + 3;
			frameNumber = 0;
		}
		else if (imageFile.equals ("CMBMAGIC.LBX"))
		{
			subFileNumber = imageNumber / 4;
			frameNumber = imageNumber % 4;
		}		
		else if (imageFile.equals ("CMBTCITY.LBX"))
		{
			// This one is a pain, and has lots of subimages with varying numbers of frames
			if (imageNumber == 39)
			{
				subFileNumber = 65;
				frameNumber = 0;
			}
			else if ((imageNumber >= 40) && (imageNumber < 48))		// Sorcery node
			{
				subFileNumber = 66;
				frameNumber = imageNumber - 40;
			}
			else if ((imageNumber >= 234) && (imageNumber < 254))		// Arcanus water tiles
			{
				subFileNumber = 109 + ((imageNumber-234) /5);
				frameNumber = (imageNumber-234) % 5;
			}
			else if (imageNumber == 273)
			{
				subFileNumber = 121;
				frameNumber = 0;
			}
			else
			{
				subFileNumber = imageNumber;
				frameNumber = 0;
			}
		}
		else if (imageFile.equals ("CHRIVER.LBX"))
		{
			// This one is a pain, and has lots of subimages with varying numbers of frames
			if (imageNumber < 20)		// Myrror water tiles
			{
				subFileNumber = 12 + (imageNumber/5);
				frameNumber = imageNumber % 5;
			}
			else 	if (imageNumber == 85)
			{
				subFileNumber = 32;
				frameNumber = 0;
			}
			else
			{
				subFileNumber = imageNumber;
				frameNumber = 0;
			}	
		}
		else if (imageFile.equals ("SPECFX.LBX"))
		{
			// This one is a pain, and has lots of subimages with varying numbers of frames
			if (imageNumber < 12)
			{
				subFileNumber = 0;
				frameNumber = imageNumber;
			}
			else if (imageNumber < 18)
			{
				subFileNumber = 1;
				frameNumber = imageNumber - 12;
			}
			else if (imageNumber < 36)
			{
				subFileNumber = 2;
				frameNumber = imageNumber - 18;
			}
			else if ((imageNumber >= 288) && (imageNumber <= 310))		// Overland enchantments
			{
				subFileNumber = imageNumber - 273;
				frameNumber = 0;
			}
			else if (imageNumber == 502)		// Extra overland enchantment that is out of place from the rest
			{
				subFileNumber = 56;
				frameNumber = 0;
			}	
			else
			{
				subFileNumber = imageNumber;
				frameNumber = 0;
			}	
		}
		else if (imageFile.equals ("CMBTFX.LBX"))
		{
			// This one is a pain, and has lots of subimages with varying numbers of frames
			if ((imageNumber >= 188) && (imageNumber < 196))
			{
				subFileNumber = 17;
				frameNumber = imageNumber - 188;
			}
			else if ((imageNumber >= 220) && (imageNumber < 228))
			{
				subFileNumber = 20;
				frameNumber = imageNumber - 220;
			}
			else if ((imageNumber >= 228) && (imageNumber < 238))
			{
				subFileNumber = 21;
				frameNumber = imageNumber - 228;
			}
			else if ((imageNumber >= 238) && (imageNumber < 254))
			{
				subFileNumber = 22;
				frameNumber = imageNumber - 238;
			}
			else
			{
				subFileNumber = imageNumber;
				frameNumber = 0;
			}	
		}
		else if (imageFile.equals ("SPECIAL.LBX"))
		{
			// There are several holes in the SPECIAL.LBX images of unit skills
			if (imageNumber <= 41)
				subFileNumber = imageNumber;
			else if (imageNumber == 42)
				subFileNumber = imageNumber + 1;
			else if (imageNumber <= 48)
				subFileNumber = imageNumber + 2;
			else if (imageNumber == 49)
				subFileNumber = imageNumber + 3;
			else
				subFileNumber = imageNumber + 4;
			
			frameNumber = 0;
		}
		else if (imageFile.equals ("COMPIX.LBX"))
		{
			if ((imageNumber >= 8) && (imageNumber < 19))
				subFileNumber = imageNumber - 3;
			else if ((imageNumber >= 31) && (imageNumber < 89))
				subFileNumber = imageNumber - 8;
			else
				subFileNumber = imageNumber;
			
			frameNumber = 0;
		}
		else if (imageFile.equals ("CITYWALL.LBX"))
		{
			if (imageNumber < 72)
			{
				subFileNumber = imageNumber / 2;
				frameNumber = imageNumber % 2;
			}
			else
			{
				subFileNumber = 36 + ((imageNumber - 72) / 4);
				frameNumber = imageNumber % 4;
			}
		}
		else if (imageFile.equals ("WALLRISE.LBX"))
		{
			if (imageNumber < 112)
			{
				subFileNumber = 36 + (imageNumber / 8);
				frameNumber = imageNumber % 8;
			}
			else
			{
				subFileNumber = 50 + ((imageNumber - 112) / 10);
				frameNumber = (imageNumber - 112) % 10;
			}
		}
		else if (imageFile.equals ("MAPBACK.LBX"))
		{
			if ((imageNumber >= 20) && (imageNumber < 30))		// cities
			{
				subFileNumber = 20 + ((imageNumber - 20) / 5);
				frameNumber = (imageNumber - 20) % 5;
			}
			else if ((imageNumber >= 129) && (imageNumber < 149))		// map feature pics
			{
				subFileNumber = imageNumber - 60;
				frameNumber = 0;
			}
			else if (imageNumber >= 155)
			{
				subFileNumber = imageNumber - 65;
				frameNumber = 0;
			}
			else
			{
				subFileNumber = imageNumber;
				frameNumber = 0;
			}
		}
		else if (imageFile.equals ("CITYSCAP.LBX"))
		{
			// This one is a pain, and has lots of subimages with varying numbers of frames
			if (imageNumber < 5)
			{
				subFileNumber = 0;
				frameNumber = imageNumber;
			}
			else if (imageNumber < 7)
			{
				subFileNumber = imageNumber - 5;
				frameNumber = 0;
			}
			else if (imageNumber < 19)
			{
				subFileNumber = 3 + ((imageNumber - 7) / 6);
				frameNumber = (imageNumber - 7) % 6;
			}
			else if (imageNumber < 24)
			{
				subFileNumber = 5;
				frameNumber = imageNumber - 19;
			}
			else if (imageNumber < 26)
			{
				subFileNumber = imageNumber - 18;
				frameNumber = 0;
			}
			else if (imageNumber < 31)
			{
				subFileNumber = 8;
				frameNumber = imageNumber - 26;
			}
			else if (imageNumber < 34)
			{
				subFileNumber = imageNumber - 22;
				frameNumber = 0;
			}
			else if (imageNumber < 43)
			{
				subFileNumber = 12;
				frameNumber = imageNumber - 34;
			}
			else if (imageNumber < 86)
			{
				// Needs fixing
				subFileNumber = imageNumber;
				frameNumber = 0;
			}
			else if (imageNumber < 95)
			{
				subFileNumber = 40;
				frameNumber = imageNumber - 86;
			}
			else if (imageNumber < 99)
			{
				subFileNumber = imageNumber - 54;
				frameNumber = 0;
			}
			else if (imageNumber < 108)
			{
				subFileNumber = 45;
				frameNumber = imageNumber - 99;
			}
			else if (imageNumber < 109)
			{
				subFileNumber = 46;
				frameNumber = 0;
			}
			else if (imageNumber < 127)
			{
				subFileNumber = 47 + ((imageNumber - 109) / 9);
				frameNumber = (imageNumber - 109) % 9;
			}
			else if (imageNumber < 128)
			{
				subFileNumber = 49;
				frameNumber = 0;
			}
			else if (imageNumber < 146)
			{
				subFileNumber = 50 + ((imageNumber - 128) / 9);
				frameNumber = (imageNumber - 128) % 9;
			}
			else if (imageNumber < 147)
			{
				subFileNumber = 52;
				frameNumber = 0;
			}
			else if (imageNumber < 156)
			{
				subFileNumber = 53;
				frameNumber = imageNumber - 147;
			}
			else if (imageNumber < 158)
			{
				subFileNumber = imageNumber - 102;
				frameNumber = 0;
			}
			else if (imageNumber < 176)
			{
				subFileNumber = 56 + ((imageNumber - 158) / 9);
				frameNumber = (imageNumber - 158) % 9;
			}
			else if (imageNumber < 179)
			{
				subFileNumber = imageNumber - 118;
				frameNumber = 0;
			}
			else if (imageNumber < 188)
			{
				subFileNumber = 61;
				frameNumber = imageNumber - 179;
			}
			else if (imageNumber < 189)
			{
				subFileNumber = 62;
				frameNumber = 0;
			}
			else if (imageNumber < 198)
			{
				subFileNumber = 63;
				frameNumber = imageNumber - 189;
			}
			else if (imageNumber < 210)
			{
				subFileNumber = imageNumber - 134;
				frameNumber = 0;
			}
			else if (imageNumber < 218)
			{
				subFileNumber = 76 + ((imageNumber - 210) / 4);
				frameNumber = (imageNumber - 210) % 4;
			}
			else if (imageNumber < 219)
			{
				subFileNumber = 78;
				frameNumber = 0;
			}
			else if (imageNumber < 227)
			{
				subFileNumber = 79 + ((imageNumber - 219) / 4);
				frameNumber = (imageNumber - 219) % 4;
			}
			else if (imageNumber < 236)
			{
				subFileNumber = 81;
				frameNumber = imageNumber - 227;
			}
			else if (imageNumber < 237)
			{
				subFileNumber = 82;
				frameNumber = 0;
			}
			else if (imageNumber < 264)
			{
				subFileNumber = 83 + ((imageNumber - 237) / 9);
				frameNumber = (imageNumber - 237) % 9;
			}
			else if (imageNumber >= 338)
			{
				subFileNumber = 115 + ((imageNumber - 338) / 6);
				frameNumber = (imageNumber - 338) % 6;
			}
			else if (imageNumber >= 314)
			{
				subFileNumber = imageNumber - 223;
				frameNumber = 0;
			}
			else
			{
				subFileNumber = imageNumber;
				frameNumber = 0;
			}
		}
		else
		{
			subFileNumber = imageNumber;
			frameNumber = 0;
		}
			
		// Save as .png
		final File destFile = new File (CLIENT_PROJECT_ROOT + "\\src\\main\\resources\\momime.client.graphics\\" + destName + ".png");
		if (destFile.exists ())
			throw new IOException ("File " + destFile + " already exists");

		// Did we save this one already?
		final String exportedImageNamesKey = imageFile + "-" + imageNumber;
		final File exportedImageNamesExisting = exportedImageNames.get (exportedImageNamesKey);
		if (alreadyExported)
		{
			// Want it to already have been exported
			if (exportedImageNamesExisting == null)
				throw new IOException ("Expected " + imageFile + ", " + imageNumber + " to have been already saved but it hasn't been");
				
			System.out.println (imageFile + ", " + imageNumber + " -> already exported as " + exportedImageNamesExisting);
		}
		else
		{				
			if (exportedImageNamesExisting != null)
				throw new IOException ("Went to save " + imageFile + ", " + imageNumber + " as " + destFile + ", but it was already saved as " + exportedImageNamesExisting);
			
			exportedImageNames.put (exportedImageNamesKey, destFile);
			
			// Read image
			final BufferedImage image = cache.findOrLoadImage (imageFile, subFileNumber, frameNumber);
			destFile.getParentFile ().mkdirs ();
			if (!ImageIO.write (image, "png", destFile))
				throw new IOException ("Failed to save PNG file");
			
			System.out.println (imageFile + ", " + imageNumber + " -> " + subFileNumber + "-" + frameNumber + " -> " + destFile);
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
				if (fileName.toUpperCase ().endsWith (".NDGARC"))
					stream = new FileInputStream ("W:\\Delphi\\Master of Magic\\New Graphics\\" + fileName);
				else if (fileName.toUpperCase ().endsWith (".LBX"))
					stream = new FileInputStream ("C:\\32 bit Program Files\\DosBox - Master of Magic\\Magic\\" + fileName);
				else
					throw new IOException ("getFileAsInputStream doesn't know how to locate file \"" + fileName + "\"");

				return stream;
			}
		};
		
		// Record names of everything that's been exported, so we avoid saving the identical image with two different names
		// if its referenced twice in the graphics XML
		exportedImageNames = new HashMap<String, File> ();
		
		// Read the graphics XML so we know which images are actually needed - e.g. don't want to
		// convert every single frame in TERRAIN.LBX if they aren't all needed
		final JAXBContext graphicsContext = JAXBContext.newInstance (GraphicsDatabase.class);
		xml = (GraphicsDatabase) graphicsContext.createUnmarshaller ().unmarshal (new File (CLIENT_PROJECT_ROOT +
			"\\src\\external\\resources\\momime.client.graphics.database\\Default.Master of Magic Graphics.xml"));
		
		// Wizards
		for (final Wizard wizard : xml.getWizard ())
			if (wizard.getPortraitFile () != null)
				convertImage (wizard.getPortraitFile (), wizard.getPortraitNumber (), false, "wizards\\" + wizard.getWizardID ());
		
		// Production types
		final Map<String, String> productionTypeNames = new HashMap<String, String> ();
		productionTypeNames.put ("RE01", "rations");
		productionTypeNames.put ("RE02", "production");
		productionTypeNames.put ("RE03", "gold");
		productionTypeNames.put ("RE04", "magicPower");
		productionTypeNames.put ("RE05", "spellResearch");
		productionTypeNames.put ("RE09", "mana");
		
		// Do mana first (since it has 3 images - magicPower only has 2)
		final List<ProductionType> productionTypes = new ArrayList<ProductionType> ();
		for (final ProductionType productionType : xml.getProductionType ())
			if (productionType.getProductionTypeID ().equals ("RE09"))
				productionTypes.add (0, productionType);
			else
				productionTypes.add (productionType);
		
		for (final ProductionType productionType : productionTypes)
		{
			final String productionTypeName = productionTypeNames.get (productionType.getProductionTypeID ());
			
			for (final ProductionTypeImage prodImage : productionType.getProductionTypeImage ())
			{
				final String value = (prodImage.getProductionValue ().equals ("½")) ? "half" : prodImage.getProductionValue ();
				convertImage (prodImage.getProductionImageFile (), prodImage.getProductionImageNumber (), productionType.getProductionTypeID ().equals ("RE04"),
					"production\\" + productionTypeName + "\\" + value);
			}
		}
		
		// Races
		final Map<String, String> raceNames = new HashMap<String, String> ();
		raceNames.put ("RC01", "barbarian");
		raceNames.put ("RC02", "gnoll");
		raceNames.put ("RC03", "halfling");
		raceNames.put ("RC04", "highElf");
		raceNames.put ("RC05", "highMen");
		raceNames.put ("RC06", "klackon");
		raceNames.put ("RC07", "lizardmen");
		raceNames.put ("RC08", "nomad");
		raceNames.put ("RC09", "orc");
		raceNames.put ("RC10", "beastmen");
		raceNames.put ("RC11", "darkElf");
		raceNames.put ("RC12", "draconian");
		raceNames.put ("RC13", "dwarf");
		raceNames.put ("RC14", "troll");

		final Map<String, String> productionTaskNames = new HashMap<String, String> ();
		productionTaskNames.put ("PT01", "farmer");
		productionTaskNames.put ("PT02", "worker");
		productionTaskNames.put ("PT03", "rebel");
		
		for (final Race race : xml.getRace ())
			for (final RacePopulationTask task : race.getRacePopulationTask ())
				convertImage (task.getCivilianImageFile (), task.getCivilianImageNumber (), false,
					"races\\" + raceNames.get (race.getRaceID ()) + "\\" + productionTaskNames.get (task.getPopulationTaskID ()));
		
		// Picks
		final Map<String, String> pickNames = new HashMap<String, String> ();
		pickNames.put ("MB01", "life");
		pickNames.put ("MB02", "death");
		pickNames.put ("MB03", "chaos");
		pickNames.put ("MB04", "nature");
		pickNames.put ("MB05", "sorcery");
		
		for (final Pick pick : xml.getPick ())
		{
			int n = 0;
			for (final BookImage book : pick.getBookImage ())
			{
				n++;
				convertImage (book.getBookImageFile (), book.getBookImageNumber (), false, "picks\\" + pickNames.get (pick.getPickID ()) + "-" + n);
			}
		}
		
		// Ranged attack types
		final Map<String, String> ratNames = new HashMap<String, String> ();
		ratNames.put ("RAT0A", "rock");
		ratNames.put ("RAT14", "arrow");
		ratNames.put ("RAT15", "pebble");
		ratNames.put ("RAT1E", "lightning");
		ratNames.put ("RAT1F", "fireball");
		ratNames.put ("RAT20", "blueBigFatBlast");
		ratNames.put ("RAT21", "purpleBolt");
		ratNames.put ("RAT22", "blueBolt");
		ratNames.put ("RAT23", "blueMediumCross");
		ratNames.put ("RAT24", "purpleDots");
		ratNames.put ("RAT25", "blueTinyCross");
		ratNames.put ("RAT26", "greenBolt");
		
		// Do fireball first, since its the most logical attack to get the fire icon
		final List<RangedAttackType> rats = new ArrayList<RangedAttackType> ();
		for (final RangedAttackType rat : xml.getRangedAttackType ())
			if (rat.getRangedAttackTypeID ().equals ("RAT1F"))
				rats.add (0, rat);
			else
				rats.add (rat);
		
		for (final RangedAttackType rat : rats)
		{
			final String ratName = ratNames.get (rat.getRangedAttackTypeID ());
			convertImage (rat.getUnitDisplayRangedImageFile (), rat.getUnitDisplayRangedImageNumber (),
				(rat.getRangedAttackTypeID ().equals ("RAT15")) || (rat.getRangedAttackTypeID ().equals ("RAT1E")) ||
				(rat.getRangedAttackTypeID ().equals ("RAT21")) ||
				(rat.getRangedAttackTypeID ().equals ("RAT22")) || (rat.getRangedAttackTypeID ().equals ("RAT23")) || 
				(rat.getRangedAttackTypeID ().equals ("RAT24")) ||	(rat.getRangedAttackTypeID ().equals ("RAT25")),		// Rock and pebble share same icon, as do a lot of magic attacks
				"rangedAttacks\\" + ratName + "\\icon");
			
			for (final RangedAttackTypeCombatImage combatImage : rat.getRangedAttackTypeCombatImage ())
				convertImageOrAnimation (combatImage.getRangedAttackTypeCombatImageFile (), combatImage.getRangedAttackTypeCombatImageNumber (),
					combatImage.getRangedAttackTypeCombatAnimation (), false,
					"rangedAttacks\\" + ratName + "\\" + combatImage.getRangedAttackTypeActionID ().name ().toLowerCase () + "-d" + combatImage.getDirection ());
		}
		
		// Unit attributes
		final Map<String, String> unitAttributeNames = new HashMap<String, String> ();
		unitAttributeNames.put ("UA01", "melee");
		unitAttributeNames.put ("UA02", "ranged");
		unitAttributeNames.put ("UA03", "plusToHit");
		unitAttributeNames.put ("UA04", "defence");
		unitAttributeNames.put ("UA05", "resist");
		unitAttributeNames.put ("UA06", "hitPoints");
		unitAttributeNames.put ("UA07", "plusToBlock");

		for (final UnitAttribute attr : xml.getUnitAttribute ())
			convertImage (attr.getAttributeImageFile (), attr.getAttributeImageNumber (), attr.getUnitAttributeID ().equals ("UA02"),	// ranged reuses RAT icon for arrows
				"unitAttributes\\" + unitAttributeNames.get (attr.getUnitAttributeID ()));
		
		// Spells
		for (final Spell spell : xml.getSpell ())
		{
			if ((spell.getOverlandEnchantmentImageFile () != null) && (spell.getOverlandEnchantmentImageNumber () != null))
				convertImage (spell.getOverlandEnchantmentImageFile (), spell.getOverlandEnchantmentImageNumber (), false,
					"spells\\" + spell.getSpellID () + "\\overlandEnchantment");
			
			// Lots of spells share the same anims
			if (spell.getCombatCastAnimation () != null)
				convertImageOrAnimation (null, null, spell.getCombatCastAnimation (),
				(spell.getSpellID ().equals ("SP004")) || (spell.getSpellID ().equals ("SP007")) || (spell.getSpellID ().equals ("SP021")) ||
				(spell.getSpellID ().equals ("SP024")) || (spell.getSpellID ().equals ("SP031")) ||
				(spell.getSpellID ().equals ("SP044")) || (spell.getSpellID ().equals ("SP054")) || (spell.getSpellID ().equals ("SP056")) ||
				(spell.getSpellID ().equals ("SP062")) || (spell.getSpellID ().equals ("SP063")) || (spell.getSpellID ().equals ("SP069")) ||
				(spell.getSpellID ().equals ("SP070")) ||
				(spell.getSpellID ().equals ("SP045")) || (spell.getSpellID ().equals ("SP060")) || (spell.getSpellID ().equals ("SP066")) ||
				(spell.getSpellID ().equals ("SP090")) ||
				(spell.getSpellID ().equals ("SP088")) || (spell.getSpellID ().equals ("SP089")) || (spell.getSpellID ().equals ("SP094")) ||
				(spell.getSpellID ().equals ("SP099")) ||
				(spell.getSpellID ().equals ("SP124")) || (spell.getSpellID ().equals ("SP126")) || (spell.getSpellID ().equals ("SP130")) ||
				(spell.getSpellID ().equals ("SP131")) || (spell.getSpellID ().equals ("SP141")) || (spell.getSpellID ().equals ("SP143")) ||
				(spell.getSpellID ().equals ("SP144")) ||
				(spell.getSpellID ().equals ("SP164")) || (spell.getSpellID ().equals ("SP165")) || (spell.getSpellID ().equals ("SP179")) ||
				(spell.getSpellID ().equals ("SP181")),
				"spells\\" + spell.getSpellID () + "\\cast");
		}
		
		// Overland and combat terrain (tile sets)
		final Map<String, String> tileTypeNames = new HashMap<String, String> ();
		tileTypeNames.put ("TT01", "mountains");
		tileTypeNames.put ("TT02", "hills");
		tileTypeNames.put ("TT03", "forest");
		tileTypeNames.put ("TT04", "desert");
		tileTypeNames.put ("TT05", "swamp");
		tileTypeNames.put ("TT06", "grasslands");
		tileTypeNames.put ("TT07", "tundra");
		tileTypeNames.put ("TT08", "shore");
		tileTypeNames.put ("TT09", "ocean");
		tileTypeNames.put ("TT10", "river");
		tileTypeNames.put ("TT11", "oceansideRiverMouth");
		tileTypeNames.put ("TT12", "sorceryNode");
		tileTypeNames.put ("TT13", "natureNode");
		tileTypeNames.put ("TT14", "chaosNode");
		tileTypeNames.put ("TT15", "landsideRiverMouth");
		tileTypeNames.put ("FOW", "fogOfWar");
		tileTypeNames.put ("FOWPARTIAL", "fogOfWarPartial");

		final Map<String, String> combatTileTypeNames = new HashMap<String, String> ();
		combatTileTypeNames.put ("CRL01", "roadInCity");
		combatTileTypeNames.put ("CRL02", "roadCityEntrance");
		combatTileTypeNames.put ("CBL01", "feature");
		combatTileTypeNames.put ("CTL01", "standard");
		combatTileTypeNames.put ("CTL02", "darkened");
		combatTileTypeNames.put ("CTL03", "ridge");
		combatTileTypeNames.put ("CBL02", "houses");
		combatTileTypeNames.put ("CBL03", "wizardsFortress");
		combatTileTypeNames.put ("CBL04", "towerOfWizardry");
		combatTileTypeNames.put ("CBL05", "cave");
		combatTileTypeNames.put ("CBL06", "keep");
		combatTileTypeNames.put ("CBL07", "ruins");
		combatTileTypeNames.put ("CBL08", "ancientTemple");
		combatTileTypeNames.put ("CBL09", "fallenTemple");
		combatTileTypeNames.put ("CBL10", "sorceryNode");
		combatTileTypeNames.put ("CBL11", "natureNode");
		combatTileTypeNames.put ("CBL12", "chaosNode");
		
		// Units
		// Hero summoning pics are generic, makes more sense not to put them under a specifc unit
		convertImage ("MONSTER.LBX", 45, false, "units\\heroSummonMale");
		convertImage ("MONSTER.LBX", 44, false, "units\\heroSummonFemale");
		
		for (final Unit unit : xml.getUnit ())
		{
			convertImage (unit.getUnitOverlandImageFile (), unit.getUnitOverlandImageNumber (), false, "units\\" + unit.getUnitID () + "\\overland");
			
			if ((unit.getUnitSummonImageFile () != null) && (unit.getUnitSummonImageNumber () != null))
				convertImage (unit.getUnitSummonImageFile (), unit.getUnitSummonImageNumber (),
				(unit.getUnitSummonImageNumber () == 44) || (unit.getUnitSummonImageNumber () == 45),
				"units\\" + unit.getUnitID () + "\\summon");
			
			if ((unit.getHeroPortraitImageFile () != null) && (unit.getHeroPortraitImageNumber () != null))
				convertImage (unit.getHeroPortraitImageFile (), unit.getHeroPortraitImageNumber (), false, "units\\" + unit.getUnitID () + "\\portrait");
			
			for (final UnitCombatAction action : unit.getUnitCombatAction ())
				for (final UnitCombatImage image : action.getUnitCombatImage ())
				{
					final String actionID;
					if (action.getCombatActionID ().equals ("RANGED"))
						actionID = "attack";
					else if (action.getCombatActionID ().equals ("WALK"))
						actionID = "move";
					else
						actionID = action.getCombatActionID ().toLowerCase ();
					
					convertImageOrAnimation (image.getUnitCombatImageFile (), image.getUnitCombatImageNumber (), image.getUnitCombatAnimation (),
						action.getCombatActionID ().equals ("FLY"),
						"units\\" + unit.getUnitID () + "\\d" + image.getDirection () + "-" + actionID);
				}
		}
		
		// Tile sets
		for (final TileSet tileSet : xml.getTileSet ())
		{
			final String tileSetName = tileSet.getTileSetName ().split (" ") [0].toLowerCase () + "\\";
			
			// Have to do Ocean before Shore, so copy the list and reorder
			final List<SmoothedTileType> smoothedTileTypes = new ArrayList<SmoothedTileType> ();
			for (final SmoothedTileType smoothedTileType : tileSet.getSmoothedTileType ())
				if ("TT09".equals (smoothedTileType.getTileTypeID ()))
					smoothedTileTypes.add (0, smoothedTileType);
				else
					smoothedTileTypes.add (smoothedTileType);
			
			// Process them in the new order
			for (final SmoothedTileType smoothedTileType : smoothedTileTypes)
			{
				final String planeName = (smoothedTileType.getPlaneNumber () == null) ? "" :
					((smoothedTileType.getPlaneNumber () == 0) ? "arcanus//" : "myrror//"); 
				
				final String terrainFolder = (smoothedTileType.getSmoothingSystemID ().equals ("FOW")) ? "" : "terrain//";
				
				// Combat maps have dark/etc tile sets inside the overland tileTypeID, so need name for those too
				final String combatTileTypeName = (smoothedTileType.getCombatTileTypeID () == null) ? "" :
					combatTileTypeNames.get (smoothedTileType.getCombatTileTypeID ()) + "\\";
				
				// Count how many of each bitmask there is, because often there's multiple and so the names need a suffix
				final Map<String, Integer> bitmaskRepetitions = new HashMap<String, Integer> ();
				for (final SmoothedTile smoothedTile : smoothedTileType.getSmoothedTile ())
				{
					final Integer count = bitmaskRepetitions.get (smoothedTile.getBitmask ());
					if (count == null)
						bitmaskRepetitions.put (smoothedTile.getBitmask (), 1);
					else
						bitmaskRepetitions.put (smoothedTile.getBitmask (), count + 1);
				}
				
				// Now export them
				final Map<String, Integer> bitmaskCounts = new HashMap<String, Integer> ();
				for (final SmoothedTile smoothedTile : smoothedTileType.getSmoothedTile ())
				{
					final String suffix;
					if (bitmaskRepetitions.get (smoothedTile.getBitmask ()) > 1)
					{
						Integer count = bitmaskCounts.get (smoothedTile.getBitmask ());
						if (count == null)
							count = 0;
						
						count++;
						bitmaskCounts.put (smoothedTile.getBitmask (), count);
						suffix = "" + (char) (count + 96);
					}
					else
						suffix = "";
					
					// Tile type ID is ommitted for default combat tiles, which all look like grass
					final String tileTypeName;
					if (smoothedTileType.getTileTypeID () == null)
					{
						if (smoothedTileType.getPlaneNumber () == null)
							tileTypeName = "";
						else
							tileTypeName = "default\\";
					}
					else
						tileTypeName = tileTypeNames.get (smoothedTileType.getTileTypeID ()) + "\\";
					
					// Do we expect this image to have been saved already because of a previous entry in the graphics XML
					final boolean alreadyExported =
						((smoothedTile.getBitmask ().equals ("NoSmooth")) ||																							// NoSmooth entries always exist elsewhere as either 00000000 or 11111111
						(("TT08".equals (smoothedTileType.getTileTypeID ())) && (smoothedTile.getBitmask ().equals ("00000000"))) ||		// Shore with no grass portions is just an ocean tile
						("TT11".equals (smoothedTileType.getTileTypeID ())) || ("TT15".equals (smoothedTileType.getTileTypeID ())) ||		// Oceanside/Landside River Mouth tiles are just dup shore/ocean/river tiles
						(("TT14".equals (smoothedTileType.getTileTypeID ())) && (tileSet.getTileSetID ().equals ("TS02"))));						// Nodes look like other tiles; e.g. chaos nodes look like mountains on combat map
					
					convertImageOrAnimation (smoothedTile.getTileFile (), smoothedTile.getTileNumber (), smoothedTile.getTileAnimation (), alreadyExported,
						tileSetName + terrainFolder + planeName + tileTypeName + combatTileTypeName + smoothedTile.getBitmask () + suffix);
				}
			}
		}
		
		// Unit skills
		for (final UnitSkill unitSkill : xml.getUnitSkill ())
		{
			// Even the main skill icon is optional, since some passive skills (like "walking") don't have icons
			if ((unitSkill.getUnitSkillImageFile () != null) && (unitSkill.getUnitSkillImageNumber () != null))
				convertImage (unitSkill.getUnitSkillImageFile (), unitSkill.getUnitSkillImageNumber (), false,
					"unitSkills\\" + unitSkill.getUnitSkillID () + "-icon");

			// Movement icons are reused a lot, since there's a number of different e.g. flying skills
			if ((unitSkill.getMovementIconImageFile () != null) && (unitSkill.getMovementIconImageNumber () != null))
				convertImage (unitSkill.getMovementIconImageFile (), unitSkill.getMovementIconImageNumber (),
					(unitSkill.getUnitSkillID ().equals ("USX03")) || (unitSkill.getUnitSkillID ().equals ("SS008")) ||
					(unitSkill.getUnitSkillID ().equals ("US022")) || (unitSkill.getUnitSkillID ().equals ("SS181")) ||
					(unitSkill.getUnitSkillID ().equals ("US023")) || (unitSkill.getUnitSkillID ().equals ("SS056")) ||
					(unitSkill.getUnitSkillID ().equals ("SS063")) || (unitSkill.getUnitSkillID ().equals ("SS093C")),
					"unitSkills\\" + unitSkill.getUnitSkillID () + "-move");
			
			if ((unitSkill.getSampleTileImageFile () != null) && (unitSkill.getSampleTileImageNumber () != null))
				convertImage (unitSkill.getSampleTileImageFile (), unitSkill.getSampleTileImageNumber (), true,	// Should be one of the overland tiles
					"unitSkills\\" + unitSkill.getUnitSkillID () + "-sampleTile");
		}
		
		// CAEs
		for (final CombatAreaEffect cae : xml.getCombatAreaEffect ())
			convertImage (cae.getCombatAreaEffectImageFile (), cae.getCombatAreaEffectImageNumber (), false, "combat\\effects\\" + cae.getCombatAreaEffectID ());
		
		// Tile types
		for (final TileType tileType : xml.getTileType ())
			if ((tileType.getMonsterFoundImageFile () != null) && (tileType.getMonsterFoundImageNumber () != null))
				convertImage (tileType.getMonsterFoundImageFile (),tileType.getMonsterFoundImageNumber (), false, "overland\\tileTypes\\" + tileTypeNames.get (tileType.getTileTypeID ()) + "-scouting");
		
		// Map features
		final Map<String, String> mapFeatureNames = new HashMap<String, String> ();
		mapFeatureNames.put ("MF01", "gems");
		mapFeatureNames.put ("MF02", "quork");
		mapFeatureNames.put ("MF03", "crysx");
		mapFeatureNames.put ("MF04", "iron");
		mapFeatureNames.put ("MF05", "coal");
		mapFeatureNames.put ("MF06", "silver");
		mapFeatureNames.put ("MF07", "gold");
		mapFeatureNames.put ("MF08", "mithril");
		mapFeatureNames.put ("MF09", "adamantium");
		mapFeatureNames.put ("MF10", "wildGame");
		mapFeatureNames.put ("MF11", "nightshade");
		mapFeatureNames.put ("MF12A", "towerOfWizardy-uncleared");
		mapFeatureNames.put ("MF12B", "towerOfWizardy-cleared");
		mapFeatureNames.put ("MF13", "lair");
		mapFeatureNames.put ("MF14", "cave");
		mapFeatureNames.put ("MF15", "keep");
		mapFeatureNames.put ("MF16", "ruins");
		mapFeatureNames.put ("MF17", "dungeon");
		mapFeatureNames.put ("MF18", "ancientTemple");
		mapFeatureNames.put ("MF19", "fallenTemple");

		for (final MapFeature mapFeature : xml.getMapFeature ())
		{
			convertImage (mapFeature.getOverlandMapImageFile (), mapFeature.getOverlandMapImageNumber (),
				(mapFeature.getMapFeatureID ().equals ("MF14")) || (mapFeature.getMapFeatureID ().equals ("MF17")),
				"overland\\mapFeatures\\" + mapFeatureNames.get (mapFeature.getMapFeatureID ()) + "-map");
			
			if ((mapFeature.getMonsterFoundImageFile () != null) && (mapFeature.getMonsterFoundImageNumber () != null))
				convertImage (mapFeature.getMonsterFoundImageFile (), mapFeature.getMonsterFoundImageNumber (), mapFeature.getMapFeatureID ().equals ("MF12B"),
					"overland\\mapFeatures\\" + mapFeatureNames.get (mapFeature.getMapFeatureID ()) + "-scouting");
		}
		
		// Cities on overland map - do output last since has common image with smallest actual town
		final List<CityImage> cities = new ArrayList<CityImage> ();
		for (final CityImage city : xml.getCityImage ())
			if (city.getCitySizeID ().equals ("CS02"))
				cities.add (0, city);
			else
				cities.add (city);
		
		for (final CityImage city : cities)
			convertImage (city.getCityImageFile (), city.getCityImageNumber (), city.getCitySizeID ().equals ("CS01"),
				"overland\\cities\\" + city.getCitySizeID () + ((city.getCityImagePrerequisite ().size () == 0) ? "-noWalls" : "-withWalls"));
		
		// Combat tile borders
		final Map<String, String> combatTileBorderNames = new HashMap<String, String> ();
		combatTileBorderNames.put ("CTB01", "wallOfStone");
		combatTileBorderNames.put ("CTB02", "wallOfStoneCorner");
		combatTileBorderNames.put ("CTB03", "wallOfStoneDoor");
		combatTileBorderNames.put ("CTB04", "wallOfFire");
		combatTileBorderNames.put ("CTB05", "wallOfDarkness");
		
		// Have to process the 68s and 24s last, due to reuse as below
		final List<CombatTileBorderImage> first = new ArrayList<CombatTileBorderImage> ();
		final List<CombatTileBorderImage> last = new ArrayList<CombatTileBorderImage> ();
		for (final CombatTileBorderImage border : xml.getCombatTileBorderImage ())
			if ((border.getDirections ().equals ("68")) || (border.getDirections ().equals ("24")))
				last.add (border);
			else
				first.add (border);

		first.addAll (last);

		for (final CombatTileBorderImage border : first)
		{
			// How many exist with this pattern?
			int count = 0;
			int index = 0;
			for (final CombatTileBorderImage border2 : xml.getCombatTileBorderImage ())
				if ((border2.getCombatTileBorderID ().equals (border.getCombatTileBorderID ())) &&
					(border2.getDirections ().equals (border.getDirections ())))
				{
					count++;
					if (border == border2)
						index = count;
				}
			
			final String suffix = (count == 1) ? "" : "-" + (char) (index + 96);
			
			// Now output them
			final String combatTileBorderName = combatTileBorderNames.get (border.getCombatTileBorderID ());
			
			// Wall of fire and darkness have no particular images for < and > shaped pieces, so reuse the pieces with only a single wall piece drawn
			final boolean reuse = (((border.getCombatTileBorderID ().equals ("CTB04")) || (border.getCombatTileBorderID ().equals ("CTB05"))) &&
				((border.getDirections ().equals ("68")) || (border.getDirections ().equals ("24"))));				
			
			convertImageOrAnimation (border.getStandardFile (), border.getStandardNumber (), border.getStandardAnimation (), reuse,
				"combat\\borders\\" + combatTileBorderName + "-d" + border.getDirections () + "-standard" + suffix);
			
			if ((border.getWreckedFile () != null) && (border.getWreckedNumber () != null))
				convertImage (border.getWreckedFile (), border.getWreckedNumber (), reuse, "combat\\borders\\" + combatTileBorderName + "-d" + border.getDirections () + "-wrecked" + suffix);
			
			if (border.getRaisingAnimation () != null)
				convertImageOrAnimation (null, null, border.getRaisingAnimation (), reuse, "combat\\borders\\" + combatTileBorderName + "-d" + border.getDirections () + "-raising" + suffix);
		}
		
		// City view elements
		for (final CityViewElement cityViewElement : xml.getCityViewElement ())
			if (("L".equals (cityViewElement.getCityViewElementSetID ())) || ("S".equals (cityViewElement.getCityViewElementSetID ())))
			{
				final String landOrSky = (cityViewElement.getCityViewElementSetID ().equals ("L")) ? "landscape" : "sky";
				final String planeName = (cityViewElement.getPlaneNumber () == 0) ? "arcanus" : "myrror";
				final String spell = (cityViewElement.getCitySpellEffectID () == null) ? "" : "-" + cityViewElement.getCitySpellEffectID ();
				final String tileType = (cityViewElement.getTileTypeID () == null) ? "" : "-" + tileTypeNames.get (cityViewElement.getTileTypeID ());
				
				convertImageOrAnimation (cityViewElement.getCityViewImageFile (), cityViewElement.getCityViewImageNumber (), cityViewElement.getCityViewAnimation (),
					"TT14".equals (cityViewElement.getTileTypeID ()),		// since Chaos Nodes reuse the sky from Mountains
					"cityView\\" + landOrSky + "\\" + planeName + spell + tileType);
				
				if ((cityViewElement.getCityViewAlternativeImageFile () != null) && (cityViewElement.getCityViewAlternativeImageNumber () != null))
					convertImage (cityViewElement.getCityViewAlternativeImageFile (), cityViewElement.getCityViewAlternativeImageNumber (),
						(cityViewElement.getPlaneNumber () == 1) && (cityViewElement.getCitySpellEffectID () != null),
						"cityView\\" + landOrSky + "\\" + planeName + spell + tileType + "-mini");
			}
			else if ("R".equals (cityViewElement.getCityViewElementSetID ()))
			{
				final String planeName = (cityViewElement.getPlaneNumber () == 0) ? "arcanus" : "myrror";
				
				convertImageOrAnimation (cityViewElement.getCityViewImageFile (), cityViewElement.getCityViewImageNumber (), cityViewElement.getCityViewAnimation (),
					(cityViewElement.getTileTypeID ().equals ("TT08")) || (cityViewElement.getTileTypeID ().equals ("TT11")) || (cityViewElement.getTileTypeID ().equals ("TT15")),
					"cityView\\water\\" + planeName + "-" + tileTypeNames.get (cityViewElement.getTileTypeID ()));
				
				if ((cityViewElement.getCityViewAlternativeImageFile () != null) && (cityViewElement.getCityViewAlternativeImageNumber () != null))
					convertImage (cityViewElement.getCityViewAlternativeImageFile (), cityViewElement.getCityViewAlternativeImageNumber (),
						false, "cityView\\water\\" + planeName + "-" + tileTypeNames.get (cityViewElement.getTileTypeID ()) + "-mini");
			}
			else if ("W".equals (cityViewElement.getCityViewElementSetID ()))
			{
				final String spell = (cityViewElement.getCitySpellEffectID () == null) ? "" : cityViewElement.getCitySpellEffectID ();
				final String building = (cityViewElement.getBuildingID () == null) ? "" : cityViewElement.getBuildingID ();

				convertImageOrAnimation (cityViewElement.getCityViewImageFile (), cityViewElement.getCityViewImageNumber (), cityViewElement.getCityViewAnimation (),
					false, "cityView\\walls\\" + spell + building);
				
				if ((cityViewElement.getCityViewAlternativeImageFile () != null) && (cityViewElement.getCityViewAlternativeImageNumber () != null))
					convertImage (cityViewElement.getCityViewAlternativeImageFile (), cityViewElement.getCityViewAlternativeImageNumber (),
						false, "cityView\\walls\\" + spell + building + "-mini");
			}
		
			// Otherwise it isn't part of a set, start looking at other elements to categorise it
			else if (cityViewElement.getBuildingID () != null)
			{
				convertImageOrAnimation (cityViewElement.getCityViewImageFile (), cityViewElement.getCityViewImageNumber (), cityViewElement.getCityViewAnimation (),
					false, "cityView\\buildings\\" + cityViewElement.getBuildingID ());

				if ((cityViewElement.getCityViewAlternativeImageFile () != null) && (cityViewElement.getCityViewAlternativeImageNumber () != null))
					convertImage (cityViewElement.getCityViewAlternativeImageFile (), cityViewElement.getCityViewAlternativeImageNumber (),
						false, "cityView\\buildings\\"  + cityViewElement.getBuildingID () + "-mini");
			}

			else if (cityViewElement.getCitySpellEffectID () != null)
			{
				convertImageOrAnimation (cityViewElement.getCityViewImageFile (), cityViewElement.getCityViewImageNumber (), cityViewElement.getCityViewAnimation (),
					false, "cityView\\spellEffects\\" + cityViewElement.getCitySpellEffectID ());

				if ((cityViewElement.getCityViewAlternativeImageFile () != null) && (cityViewElement.getCityViewAlternativeImageNumber () != null))
					convertImage (cityViewElement.getCityViewAlternativeImageFile (), cityViewElement.getCityViewAlternativeImageNumber (),
						false, "cityView\\spellEffects\\"  + cityViewElement.getCitySpellEffectID () + "-mini");
			}
		
			else
			{
				// There's only 1 of these
				convertImageOrAnimation (cityViewElement.getCityViewImageFile (), cityViewElement.getCityViewImageNumber (), cityViewElement.getCityViewAnimation (),
					false, "cityView\\roadWithinCity");

				if ((cityViewElement.getCityViewAlternativeImageFile () != null) && (cityViewElement.getCityViewAlternativeImageNumber () != null))
					convertImage (cityViewElement.getCityViewAlternativeImageFile (), cityViewElement.getCityViewAlternativeImageNumber (),
						false, "cityView\\roadWithinCity-mini");
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
			new ClientResourceConversion ().run ();
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}
}
