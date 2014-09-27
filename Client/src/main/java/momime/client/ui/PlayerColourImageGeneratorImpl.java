package momime.client.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import momime.client.MomClient;
import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;

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

	/** Colour multiplied flags for each player's mirrors */
	private final Map<Integer, BufferedImage> mirrorImages = new HashMap<Integer, BufferedImage> ();
	
	/** Uncoloured unit background image */
	private BufferedImage unitBackgroundImage;
	
	/** Uncoloured city flag image */
	private BufferedImage cityFlagImage;
	
	/** Uncoloured mirror image */
	private BufferedImage mirrorImage;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @param playerID Unit owner player ID
	 * @return Unit background image in their correct colour 
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
	 * @param playerID Spell owner player ID
	 * @return Mirror image in their correct colour 
	 * @throws IOException If there is a problem loading the mirror image
	 */
	@Override
	public final BufferedImage getOverlandEnchantmentMirror (final int playerID) throws IOException
	{
		if (mirrorImage == null)
			mirrorImage = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/mirror.png");

		return getImage (playerID, mirrorImage, mirrorImages);
	}
	
	/**
	 * This handles the image colouring generically to save repeating the same code
	 * 
	 * @param playerID Player ID
	 * @param uncolouredImage Uncoloured white images
	 * @param map Map containing coloured images that have already been generated
	 * @return Player coloured image
	 * @throws PlayerNotFoundException If the specified playerID can't be found
	 */
	private final BufferedImage getImage (final int playerID, final BufferedImage uncolouredImage, final Map<Integer, BufferedImage> map)
		throws PlayerNotFoundException
	{
		BufferedImage image = map.get (playerID);
		if (image == null)
		{
			// Generate a new one
			final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), playerID, "PlayerColourImageGeneratorImpl");
			final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
			
			image = getUtils ().multiplyImageByColour (uncolouredImage, Integer.parseInt (trans.getFlagColour (), 16));
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
}