package momime.client.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitEx;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.KnownWizardUtils;

/**
 * Various items like unit backgrounds and city flags are displayed in the player's colour.
 * This caches the coloured images so that we don't have to recreate a multiplied colour image every time.
 */
public final class PlayerColourImageGeneratorImpl implements PlayerColourImageGenerator
{
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
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @param unitDef Unit to get the image for
	 * @param playerID Player ID
	 * @return Overland image for this unit, with the correct flag colour already drawn on; background square is not included
	 * @throws IOException If there is a problem loading the images
	 */
	@Override
	public final BufferedImage getOverlandUnitImage (final UnitEx unitDef, final int playerID) throws IOException
	{
		return getModifiedImage (unitDef.getUnitOverlandImageFile (), false,
			unitDef.getUnitOverlandImageFlag (), unitDef.getFlagOffsetX (), unitDef.getFlagOffsetY (),
			playerID, null);
	}
	
	/**
	 * @param frameNumber Frame number of the node aura animation
	 * @param playerID Node owner player ID
	 * @param warped Whether to darken the node aura to show a warped node
	 * @return Node aura image in their correct colour
	 * @throws IOException If there is a problem loading the flag image
	 */
	@Override
	public final BufferedImage getNodeAuraImage (final int frameNumber, final int playerID, final boolean warped) throws IOException
	{
		final String imageName = getGraphicsDB ().findAnimation ("NODE_AURA", "getNodeAuraImage").getFrame ().get (frameNumber).getImageFile ();
		final List<String> shadingColours = warped ? Arrays.asList ("505050") : null;
		
		return getModifiedImage (imageName, true, null, null, null, playerID, shadingColours);
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
		final String imageName = "/momime.client.graphics/overland/friendlyZoneBorder/border-d" + d + ".png";
		return getModifiedImage (imageName, true, null, null, null, playerID, null);
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
		boolean wasMonsters = false;
		if ((colourEntireImageApplies) || (flagApplies))
		{
			player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), playerID, "getModifiedImage");
			
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), playerID, "getModifiedImage");
			
			if (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (wizardDetails.getWizardID ()))
			{
				colourEntireImageApplies = false;
				flagApplies = false;
				wasMonsters = playerColourEntireImage;  // So unit background squares output null, but units themselves are still drawn
			}
		}
		
		// If there are no modifications to make at all, just use the normal image cache
		BufferedImage image;
		if ((!colourEntireImageApplies) && (!flagApplies) && (!shadingApplies))
			image = wasMonsters ? null : getUtils ().loadImage (imageName);
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

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}