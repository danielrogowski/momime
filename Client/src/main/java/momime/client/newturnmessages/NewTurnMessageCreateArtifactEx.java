package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.HeroItemsUI;
import momime.common.database.LanguageText;
import momime.common.messages.NewTurnMessageCreateArtifact;

/**
 * NTM describing that we finished crafting a hero item
 */
public final class NewTurnMessageCreateArtifactEx extends NewTurnMessageCreateArtifact
	implements NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageMusic, NewTurnMessageClickable
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (NewTurnMessageCreateArtifactEx.class);

	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Hero items UI */
	private HeroItemsUI heroItemsUI;

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
		// The image is hard coded and not defined in the graphics XML - we just need to half the size of it
		Image image = null;
		try
		{
			final BufferedImage fullSizeImage = getUtils ().loadImage ("/momime.client.graphics/spells/SP211/summon.png");
			image = fullSizeImage.getScaledInstance (fullSizeImage.getWidth () / 2, fullSizeImage.getHeight () / 2, Image.SCALE_SMOOTH);
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
		// Music is defined in the graphics XML however
		String music = null;
		try
		{
			music = getClient ().getClientDB ().findSpell (getSpellID (), "NewTurnMessageCreateArtifactEx").getSpellMusicFile ();
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
		final List<LanguageText> languageText;
		if (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN)
			languageText = getLanguages ().getNewTurnMessages ().getCreateArtifactLastTurn ();
		else
			languageText = getLanguages ().getNewTurnMessages ().getCreateArtifact ();
		
		String text = getLanguageHolder ().findDescription (languageText);		
		return text.replaceAll ("ITEM_NAME", getHeroItemName ());
	}
	
	/**
	 * Show the items screen when clicked on
	 * @throws Exception If there is a problem
	 */
	@Override
	public final void clicked () throws Exception
	{
		getHeroItemsUI ().setVisible (true);
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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
	 * @return Hero items UI
	 */
	public final HeroItemsUI getHeroItemsUI ()
	{
		return heroItemsUI;
	}

	/**
	 * @param ui Hero items UI
	 */
	public final void setHeroItemsUI (final HeroItemsUI ui)
	{
		heroItemsUI = ui;
	}
}