package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.messages.v0_9_5.NewTurnMessageOverlandEnchantment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;

/**
 * NTM about an overland enchantment that completed casting (either by us or an opponent)
 */
public final class NewTurnMessageOverlandEnchantmentEx extends NewTurnMessageOverlandEnchantment
	implements NewTurnMessageUI, NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageMusic
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewTurnMessageOverlandEnchantmentEx.class);
	
	/** Controls how overland enchantment images fit inside the mirror */ 
	private final static int IMAGE_MIRROR_X_OFFSET = 9;
	
	/** Controls how overland enchantment images fit inside the mirror */
	private final static int IMAGE_MIRROR_Y_OFFSET = 8;
			
	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_SPELLS;
	}

	/**
	 * @return Image to draw for this NTM, or null to display only text
	 */
	@Override
	public final Image getImage ()
	{
		Image image = null;
		try
		{
			final String imageName = getGraphicsDB ().findSpell (getSpellID (), "NewTurnMessageOverlandEnchantmentEx").getOverlandEnchantmentImageFile ();
			if (imageName != null)
			{
				final BufferedImage spellImage = getUtils ().loadImage (imageName);
				
				// Now that we got the spell image OK, get the coloured mirror for the caster
				final BufferedImage mirrorImage = getPlayerColourImageGenerator ().getOverlandEnchantmentMirror (getCastingPlayerID ());
				final BufferedImage mergedImage = new BufferedImage (mirrorImage.getWidth (), mirrorImage.getHeight (), BufferedImage.TYPE_INT_ARGB);
				final Graphics2D g = mergedImage.createGraphics ();
				try
				{
					g.drawImage (spellImage, IMAGE_MIRROR_X_OFFSET, IMAGE_MIRROR_Y_OFFSET, null);
					g.drawImage (mirrorImage, 0, 0, null);
				}
				finally
				{
					g.dispose ();
				}
				
				// Now half the size of it				
				image = mergedImage.getScaledInstance (mergedImage.getWidth () / 2, mergedImage.getHeight () / 2, Image.SCALE_SMOOTH);
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return image;
	}
	
	/**
	 * @return Name of music file on the classpath to play when this NTM is displayed; null if this message has no music associated
	 */
	@Override
	public final String getMusicResourceName ()
	{
		String music = null;
		try
		{
			music = getGraphicsDB ().findSpell (getSpellID (), "NewTurnMessageOverlandEnchantmentEx").getSpellMusicFile ();
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return music;
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		// Who cast it?
		final PlayerPublicDetails castingPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getCastingPlayerID ());
		
		// Spell name
		final Spell spell = getLanguage ().findSpell (getSpellID ());
		final String spellName = (spell != null) ? spell.getSpellName () : null;
		
		// Now can get the text
		return getLanguage ().findCategoryEntry ("NewTurnMessages",
			((getCastingPlayerID () == getClient ().getOurPlayerID ()) ? "Our" : "Enemy") + 
			((getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN) ? "OverlandEnchantmentLastTurn" : "OverlandEnchantment")).replaceAll
			("SPELL_NAME", (spellName != null) ? spellName : getSpellID ()).replaceAll
			("PLAYER_NAME", (castingPlayer != null) ? castingPlayer.getPlayerDescription ().getPlayerName () : "Player " + getCastingPlayerID ());
	}
	
	/**
	 * @return Font to display the text in
	 */
	@Override
	public final Font getFont ()
	{
		return getSmallFont ();
	}
	
	/**
	 * @return Colour to display the text in
	 */
	@Override
	public final Color getColour ()
	{
		return MomUIConstants.SILVER;
	}
	
	/**
	 * @return Current status of this NTM
	 */
	@Override
	public final NewTurnMessageStatus getStatus ()
	{
		return status;
	}
	
	/**
	 * @param newStatus New status for this NTM
	 */
	@Override
	public final void setStatus (final NewTurnMessageStatus newStatus)
	{
		status = newStatus;
	}

	/**
	 * @return Small font
	 */
	public final Font getSmallFont ()
	{
		return smallFont;
	}

	/**
	 * @param font Small font
	 */
	public final void setSmallFont (final Font font)
	{
		smallFont = font;
	}

	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
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
	 * @return Player colour image generator
	 */
	public final PlayerColourImageGenerator getPlayerColourImageGenerator ()
	{
		return playerColourImageGenerator;
	}

	/**
	 * @param gen Player colour image generator
	 */
	public final void setPlayerColourImageGenerator (final PlayerColourImageGenerator gen)
	{
		playerColourImageGenerator = gen;
	}
}