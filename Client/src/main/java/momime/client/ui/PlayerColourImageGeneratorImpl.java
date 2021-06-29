package momime.client.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitEx;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

/**
 * Various items like unit backgrounds and city flags are displayed in the player's colour.
 * This caches the coloured images so that we don't have to recreate a multiplied colour image every time.
 */
public final class PlayerColourImageGeneratorImpl implements PlayerColourImageGenerator
{
	/** Colour backgrounds for each player's units */
	private final Map<Integer, BufferedImage> unitBackgroundImages = new HashMap<Integer, BufferedImage> ();

	/** Colour multiplied flags for each player's cities and units; outer map is the filename and inner map is the playerID */
	private final Map<String, Map<Integer, BufferedImage>> flagImages = new HashMap<String, Map<Integer, BufferedImage>> ();

	/** Overland unit images with correct flag colour draw on; outer map is the unitID and inner map is the playerID */
	private final Map<String, Map<Integer, BufferedImage>> overlandUnitImages = new HashMap<String, Map<Integer, BufferedImage>> ();
	
	/** Colour multiplied node animations for each player */
	private final Map<Integer, List<BufferedImage>> nodeAuraMap = new HashMap<Integer, List<BufferedImage>> ();
	
	/** Colour multiplied flags for each player's mirrors */
	private final Map<Integer, BufferedImage> mirrorImages = new HashMap<Integer, BufferedImage> ();

	/** Colour multiplied gems for each player */
	private final Map<Integer, BufferedImage> wizardGemImages = new HashMap<Integer, BufferedImage> ();

	/** Colour multiplied cracked gems for each player */
	private final Map<Integer, BufferedImage> wizardGemCrackedImages = new HashMap<Integer, BufferedImage> ();
	
	/**
	 * Images (typically of unit figures) that have been modified by either superimposing the player's flag colour, or shading by one or more
	 * unit skills that change a unit's appearance, e.g. Black Sleep or Invisibility, or both.  These then get cached here so we don't have to
	 * generate the modified image over and over again.
	 * 
	 * The key to this is the name of the image, followed a P and the player ID (if applicable) and the colour code(s) that have been applied
	 * to its appearance (if applicable), in alphabetical order, with a : delimiter
	 * e.g. "/momime.client.graphics/units/UN123/d5-stand.png:P1:808080:FFFF50"
	 */
	private final Map<String, BufferedImage> modifiedImages = new HashMap<String, BufferedImage> ();
	
	/** Uncoloured unit background image */
	private BufferedImage unitBackgroundImage;
	
	/** Uncoloured node aura images */
	private List<BufferedImage> nodeAuraImages;
	
	/** Uncoloured mirror image */
	private BufferedImage mirrorImage;

	/** Uncoloured wizard gem image */
	private BufferedImage wizardGemImage;

	/** Uncoloured wizard gem cracked image */
	private BufferedImage wizardGemCrackedImage;
	
	/** Uncoloured friendly zone borders */
	private Map<Integer, BufferedImage> friendlyZoneBorderImages = new HashMap<Integer, BufferedImage> ();
	
	/** Colour multiplied friendly zone borders */
	private Map<String, BufferedImage> colouredFriendlyZoneBorderImages = new HashMap<String, BufferedImage> ();
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/**
	 * @param playerID Unit owner player ID
	 * @return Unit background image in their correct colour; null for the monsters player who has no colour
	 * @throws IOException If there is a problem loading the background image
	 */
	@Override
	public final BufferedImage getUnitBackgroundImage (final int playerID) throws IOException
	{
		if (unitBackgroundImage == null)
			unitBackgroundImage = getUtils ().loadImage ("/momime.client.graphics/ui/overland/unitBackground.png");
		
		return getImage (playerID, unitBackgroundImage, unitBackgroundImages);
	}

	/**
	 * @param flagImageName Filename of flag image to load
	 * @param playerID Player ID
	 * @return Flag image in their correct colour
	 * @throws IOException If there is a problem loading the flag image
	 */
	@Override
	public final BufferedImage getFlagImage (final String flagImageName, final int playerID) throws IOException
	{
		Map<Integer, BufferedImage> imagesOfThisFlag = flagImages.get (flagImageName);
		if (imagesOfThisFlag == null)
		{
			imagesOfThisFlag = new HashMap<Integer, BufferedImage> ();
			flagImages.put (flagImageName, imagesOfThisFlag);
		}
		
		return getImage (playerID, getUtils ().loadImage (flagImageName), imagesOfThisFlag);
	}
	
	/**
	 * @param unitDef Unit to get the image for
	 * @param playerID Player ID
	 * @return Overland image for this unit, with the correct flag colour already drawn on; background square is not included
	 * @throws IOException If there is a problem loading the images
	 */
	@Override
	public final BufferedImage getOverlandUnitImage (final UnitEx unitDef, final int playerID) throws IOException
	{
		BufferedImage image;
		
		// If there's no flag to draw, its just reading the image directly from the normal cache
		if ((unitDef.getUnitOverlandImageFlag () == null) || (unitDef.getFlagOffsetX () == null) || (unitDef.getFlagOffsetY () == null))
			image = getUtils ().loadImage (unitDef.getUnitOverlandImageFile ());
		else
		{
			// See if we've arleady generated it
			Map<Integer, BufferedImage> imagesOfThisUnit = overlandUnitImages.get (unitDef.getUnitID ());
			if (imagesOfThisUnit == null)
			{
				imagesOfThisUnit = new HashMap<Integer, BufferedImage> ();
				overlandUnitImages.put (unitDef.getUnitID (), imagesOfThisUnit);
			}
			
			image = imagesOfThisUnit.get (playerID);
			if (image == null)
			{
				// Generate it - first we need the regular unit image with the flag pixels in green
				final BufferedImage baseImage = getUtils ().loadImage (unitDef.getUnitOverlandImageFile ());
				
				// Copy it as-is
				image = new BufferedImage (baseImage.getWidth (), baseImage.getHeight (), BufferedImage.TYPE_INT_ARGB);
				final Graphics2D g = image.createGraphics ();
				try
				{
					g.drawImage (baseImage, 0, 0, null);
					
					// Now add on the coloured flag
					g.drawImage (getFlagImage (unitDef.getUnitOverlandImageFlag (), playerID), unitDef.getFlagOffsetX (), unitDef.getFlagOffsetY (), null);
				}
				finally
				{
					g.dispose ();
				}
				
				// Add to cache
				imagesOfThisUnit.put (playerID, image);
			}
		}
		
		return image;
	}
	
	/**
	 * @param frameNumber Frame number of the node aura animation
	 * @param playerID Node owner player ID
	 * @return Node aura image in their correct colour
	 * @throws IOException If there is a problem loading the flag image
	 */
	@Override
	public final BufferedImage getNodeAuraImage (final int frameNumber, final int playerID) throws IOException
	{
		if (nodeAuraImages == null)
		{
			nodeAuraImages = new ArrayList<BufferedImage> ();
			for (final AnimationFrame frame : getGraphicsDB ().findAnimation ("NODE_AURA", "getNodeAuraImage").getFrame ())
				nodeAuraImages.add (getUtils ().loadImage (frame.getImageFile ()));
		}
		
		// This is copied from getImage and adapted to work with a list
		final List<BufferedImage> images;
		
		// Use containsKey, because for the monsters player we'll put a null into the map (actually a list of nulls, so this isn't really true here)
		if (nodeAuraMap.containsKey (playerID))
			images = nodeAuraMap.get (playerID);
		else
		{
			// Generate a new list
			images = new ArrayList<BufferedImage> ();

			final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), playerID, "PlayerColourImageGeneratorImpl");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
			
			for (final BufferedImage uncolouredImage : nodeAuraImages)
			{
				final BufferedImage image;
				if (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (pub.getWizardID ()))
					image = null;
				else
					image = getUtils ().multiplyImageByColour (uncolouredImage, Integer.parseInt (trans.getFlagColour (), 16));
				
				images.add (image);
			}
			
			nodeAuraMap.put (playerID, images);
		}
		
		return images.get (frameNumber);
	}
	
	/**
	 * @param playerID Spell owner player ID
	 * @return Mirror image in their correct colour
	 * @throws IOException If there is a problem loading the mirror image
	 */
	@Override
	public final BufferedImage getOverlandEnchantmentMirror (final int playerID) throws IOException
	{
		if (mirrorImage == null)
			mirrorImage = getUtils ().loadImage ("/momime.client.graphics/ui/mirror/mirror.png");

		return getImage (playerID, mirrorImage, mirrorImages);
	}
	
	/**
	 * @param playerID Unit owner player ID
	 * @return Wizard gem background image in their correct colour
	 * @throws IOException If there is a problem loading the background image
	 */
	@Override
	public final BufferedImage getWizardGemImage (final int playerID) throws IOException
	{
		if (wizardGemImage == null)
			wizardGemImage = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/gem.png");

		return getImage (playerID, wizardGemImage, wizardGemImages);
	}

	/**
	 * @param playerID Unit owner player ID
	 * @return Cracked wizard gem background image in their correct colour
	 * @throws IOException If there is a problem loading the background image
	 */
	@Override
	public final BufferedImage getWizardGemCrackedImage (final int playerID) throws IOException
	{
		if (wizardGemCrackedImage == null)
			wizardGemCrackedImage = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/gemCracked.png");

		return getImage (playerID, wizardGemCrackedImage, wizardGemCrackedImages);
	}
	
	/**
	 * @param d Direction of border edge to draw
	 * @param playerID ID of player whose border we are drawing
	 * @return Border edge in player colour
	 * @throws IOException If there is a problem loading the border image
	 */
	@Override
	public final BufferedImage getFriendlyZoneBorderImage (final int d, final int playerID) throws IOException
	{
		// 2nd map is keyed by playerID - direction
		final String key = playerID + "-" + d;
		BufferedImage image = colouredFriendlyZoneBorderImages.get (key);
		if (image == null)
		{
			// Get uncoloured image
			image = friendlyZoneBorderImages.get (d);
			if (image == null)
			{
				image = getUtils ().loadImage ("/momime.client.graphics/overland/friendlyZoneBorder/border-d" + d + ".png");
				friendlyZoneBorderImages.put (d, image);
			}
			
			// Generate coloured image
			final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), playerID, "PlayerColourImageGeneratorImpl");
			final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();

			image = getUtils ().multiplyImageByColour (image, Integer.parseInt (trans.getFlagColour (), 16));
			colouredFriendlyZoneBorderImages.put (key, image);
		}
		
		return image;
	}
	
	/**
	 * This handles the image colouring generically to save repeating the same code
	 * 
	 * @param playerID Player ID
	 * @param uncolouredImage Uncoloured white images
	 * @param map Map containing coloured images that have already been generated
	 * @return Player coloured image; null for the monsters player who has no colour
	 * @throws PlayerNotFoundException If the specified playerID can't be found
	 */
	private final BufferedImage getImage (final int playerID, final BufferedImage uncolouredImage, final Map<Integer, BufferedImage> map)
		throws PlayerNotFoundException
	{
		final BufferedImage image;
		
		// Use containsKey, because for the monsters player we'll put a null into the map
		if (map.containsKey (playerID))
			image = map.get (playerID);
		else
		{
			// Generate a new one
			final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), playerID, "PlayerColourImageGeneratorImpl");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			if (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (pub.getWizardID ()))
				image = null;
			else
			{
				final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
				image = getUtils ().multiplyImageByColour (uncolouredImage, Integer.parseInt (trans.getFlagColour (), 16));
			}
			
			map.put (playerID, image);
		}
		return image;
	}

	/**
	 * Images (typically of unit figures) shaded by one or more unit skills that change a unit's appearance, e.g. Black Sleep or Invisibility
	 * The key to this is the name of the image, followed by the colour codes that have been applied to its appearance, in alphabetical order, with a : delimiter
	 * e.g. "/momime.client.graphics/units/UN123/d5-stand.png:808080:FFFF50"
	 * 
	 * @param imageName Filename of the base colour image
	 * @param playerColourEntireImage If true, the entire primary image is changed to the wizard's flag colour like e.g. the wizard gems
	 * 	or overland enchantment mirrors, in this case any flag values will be ignored; if false, the primary image is kept original colour and
	 * 	only the flag is changed to the wizard's colour and then superimposed onto the primary image
	 * @param flagName Name of flag image, if there is one
	 * @param flagOffsetX X offset to draw flag
	 * @param flagOffsetY Y offset to draw flag
	 * @param playerID Player who owns the unit; if this is not supplied then the flag image will be ignored (if there is one) 
	 * @param shadingColours List of shading colours to apply to the image
	 * @return Image with modified colours
	 * @throws IOException If there is a problem loading the image
	 */
	@Override
	public final BufferedImage getModifiedImage (final String imageName, final boolean playerColourEntireImage,
		final String flagName, final Integer flagOffsetX, final Integer flagOffsetY,
		final Integer playerID, final List<String> shadingColours) throws IOException
	{
		boolean colourEntireImageApplies = (playerColourEntireImage) && (playerID != null);
		boolean flagApplies = (!playerColourEntireImage) && (flagName != null) && (flagOffsetX != null) && (flagOffsetY != null) && (playerID != null);
		final boolean shadingApplies = (shadingColours != null) && (shadingColours.size () > 0);
		
		// Maybe flags and colouring doesn't apply after all for monsters
		PlayerPublicDetails player = null;
		if ((colourEntireImageApplies) || (flagApplies))
		{
			player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), playerID, "getModifiedImage");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			if (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (pub.getWizardID ()))
			{
				colourEntireImageApplies = false;
				flagApplies = false;
			}
		}
		
		// If there are no modifications to make at all, just use the normal image cache
		BufferedImage image;
		if ((!colourEntireImageApplies) && (!flagApplies) && (!shadingApplies))
			image = getUtils ().loadImage (imageName);
		else
		{
			// Generate the entire key - maybe the image already exists in the cache
			final StringBuilder key = new StringBuilder (imageName);
			if (colourEntireImageApplies)
				key.append (":W" + playerID);
			else if (flagApplies)
				key.append (":P" + playerID);

			List<String> sortedColours = null;
			if (shadingApplies)
			{
				sortedColours = shadingColours.stream ().sorted ().collect (Collectors.toList ());
				sortedColours.forEach (s -> key.append (":" + s));
			}
			
			image = modifiedImages.get (key.toString ());
			if (image == null)
			{
				// Generate new image - first deal with the flag colours
				if ((!colourEntireImageApplies) && (!flagApplies))
					image = getUtils ().loadImage (imageName);
				else
				{
					// Need modified image, and its not in the cache.. but maybe only the coloured image or image with flag is in the cache, only missing the extra shading
					final String partialKey = imageName + (colourEntireImageApplies ? ":W" : ":P") + playerID;
					image = modifiedImages.get (partialKey);
					
					if (image == null)
					{
						// Generate a new one
						final BufferedImage baseImage = getUtils ().loadImage (imageName);
						if (colourEntireImageApplies)
						{
							final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
							image = getUtils ().multiplyImageByColour (baseImage, Integer.parseInt (trans.getFlagColour (), 16));
						}
						else
						{
							// Recursive call to get the coloured flag
							final BufferedImage flagImage = getModifiedImage (flagName, true, null, null, null, playerID, null);
							
							image = new BufferedImage (baseImage.getWidth (), baseImage.getHeight (), BufferedImage.TYPE_INT_ARGB);
							final Graphics2D g = image.createGraphics ();
							try
							{
								g.drawImage (baseImage, 0, 0, null);
								g.drawImage (flagImage, flagOffsetX, flagOffsetY, null);
							}
							finally
							{
								g.dispose ();
							}
						}
				
						// Store base image + flag or coloured image, prior to applying shading, in the map
						modifiedImages.put (partialKey, image);
					}
				}
				
				if (shadingApplies)
					for (final String colour : sortedColours)
						if (colour.length () == 8)
							image = getUtils ().multiplyImageByColourAndAlpha (image, (int) Long.parseLong (colour, 16));
						else
							image = getUtils ().multiplyImageByColour (image, Integer.parseInt (colour, 16));
				
				// Store it in the map
				modifiedImages.put (key.toString (), image);
			}
		}
		
		return image;
	}
	
	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
	}
}