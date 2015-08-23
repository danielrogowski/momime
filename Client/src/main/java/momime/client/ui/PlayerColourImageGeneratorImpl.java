package momime.client.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.database.CommonDatabaseConstants;
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

	/** Colour multiplied flags for each player's cities */
	private final Map<Integer, BufferedImage> cityFlagImages = new HashMap<Integer, BufferedImage> ();

	/** Colour multiplied node animations for each player */
	private final Map<Integer, List<BufferedImage>> nodeAuraMap = new HashMap<Integer, List<BufferedImage>> ();
	
	/** Colour multiplied flags for each player's mirrors */
	private final Map<Integer, BufferedImage> mirrorImages = new HashMap<Integer, BufferedImage> ();

	/** Colour multiplied gems for each player */
	private final Map<Integer, BufferedImage> wizardGemImages = new HashMap<Integer, BufferedImage> ();
	
	/** Uncoloured unit background image */
	private BufferedImage unitBackgroundImage;
	
	/** Uncoloured city flag image */
	private BufferedImage cityFlagImage;
	
	/** Uncoloured node aura images */
	private List<BufferedImage> nodeAuraImages;
	
	/** Uncoloured mirror image */
	private BufferedImage mirrorImage;

	/** Uncoloured wizard gem image */
	private BufferedImage wizardGemImage;
	
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
	 * @param playerID City owner player ID
	 * @return City flag image in their correct colour
	 * @throws IOException If there is a problem loading the flag image
	 */
	@Override
	public final BufferedImage getCityFlagImage (final int playerID) throws IOException
	{
		if (cityFlagImage == null)
			cityFlagImage = getUtils ().loadImage ("/momime.client.graphics/overland/cities/cityFlag.png");

		return getImage (playerID, cityFlagImage, cityFlagImages);
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
			for (final String frame : getGraphicsDB ().findAnimation ("NODE_AURA", "getNodeAuraImage").getFrame ())
				nodeAuraImages.add (getUtils ().loadImage (frame));
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