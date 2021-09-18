package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.QueuedSpellsUI;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.NewTurnMessageSpellBlast;

/**
 * NTM about a spell we were casting overland getting blasted / fizzling due to an enemy wizard spell
 */
public final class NewTurnMessageSpellBlastEx extends NewTurnMessageSpellBlast
	implements NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageClickable, NewTurnMessageMusic
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (NewTurnMessageSpellBlastEx.class);
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Queued spells UI */
	private QueuedSpellsUI queuedSpellsUI;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_SPELLS;
	}

	/**
	 * @return Name of music file on the classpath to play when this NTM is displayed; null if this message has no music associated
	 */
	@Override
	public final String getMusicResourceName ()
	{
		return "/momime.client.music/MUSIC_017 - SuppressMagicActivating.mp3";
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
			image = getUtils ().loadImage ("/momime.client.graphics/spells/SP058/newTurnMessagesImage.png");
		}
		catch (final IOException e)
		{
			log.error (e, e);
		}
		
		return image;
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		// Text varies according to the message type
		String text = null;
		try
		{
			final String spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getSpellID (), "NewTurnMessageSpellBlastEx (O)").getSpellName ());
			final String blastedBySpellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getBlastedBySpellID (), "NewTurnMessageSpellBlastEx (B)").getSpellName ());
			final PlayerPublicDetails blastedByPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getBlastedByPlayerID (), "NewTurnMessageSpellBlastEx (P)");
			
			text = getLanguageHolder ().findDescription (getLanguages ().getNewTurnMessages ().getSpellBlast ()).replaceAll
				("SPELL_NAME", spellName).replaceAll
				("BLASTING_SPELL", blastedBySpellName).replaceAll
				("PLAYER_NAME", getWizardClientUtils ().getPlayerName (blastedByPlayer));
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return text;
	}

	/**
	 * Clicking on blasted spells brings up queued spells so we can see what will cast next.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Override
	public final void clicked () throws Exception
	{
		getQueuedSpellsUI ().setVisible (true);
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
	 * @return Wizard client utils
	 */
	public final WizardClientUtils getWizardClientUtils ()
	{
		return wizardClientUtils;
	}

	/**
	 * @param util Wizard client utils
	 */
	public final void setWizardClientUtils (final WizardClientUtils util)
	{
		wizardClientUtils = util;
	}

	/**
	 * @return Queued spells UI
	 */
	public final QueuedSpellsUI getQueuedSpellsUI ()
	{
		return queuedSpellsUI;
	}

	/**
	 * @param ui Queued spells UI
	 */
	public final void setQueuedSpellsUI (final QueuedSpellsUI ui)
	{
		queuedSpellsUI = ui;
	}
}