package momime.client.ui.dialogs;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.actions.LoggingAction;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.audio.MomAudioPlayer;
import momime.client.languages.database.Shortcut;
import momime.client.ui.MomUIConstants;
import momime.client.utils.TextUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.database.CitySize;
import momime.common.database.Event;
import momime.common.database.LanguageText;
import momime.common.database.MapFeatureEx;
import momime.common.messages.servertoclient.RandomEventMessage;

/**
 * Popup when a random event is triggered or stops
 */
public final class RandomEventUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RandomEventUI.class);

	/** XML layout */
	private XmlLayoutContainerEx randomEventLayout;

	/** Small font */
	private Font smallFont;

	/** OK action */
	private Action okAction;
	
	/** Content pane */
	private JPanel contentPane;

	/** Main text area */
	private JTextArea messageText;
	
	/** Details of the random event */
	private RandomEventMessage randomEventMessage;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Music player */
	private MomAudioPlayer musicPlayer;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/**
	 * Sets up the dialog once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/randomEvent.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");
	
		// Actions
		okAction = new LoggingAction ((ev) -> getDialog ().dispose ());
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getRandomEventLayout ()));

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmRandomEventButton");
		
		messageText = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		contentPane.add (getUtils ().createTransparentScrollPane (messageText), "frmRandomEventText");
		
		// Need to resize the side image
		final XmlLayoutComponent imageSize = getRandomEventLayout ().findComponent ("frmRandomEventImage");
		
		final Event eventDef = getClient ().getClientDB ().findEvent (getRandomEventMessage ().getEventID (), "RandomEventUI");
		try
		{
			getMusicPlayer ().playThenResume (eventDef.getEventMusicFile ());
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		final BufferedImage eventImage = getUtils ().loadImage (eventDef.getEventImageFile ());
		contentPane.add (getUtils ().createImage (eventImage.getScaledInstance (imageSize.getWidth (), imageSize.getHeight (), Image.SCALE_SMOOTH)), "frmRandomEventImage");
		
		// Lock dialog size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);

		// Shortcut keys
		contentPane.getActionMap ().put (Shortcut.MESSAGE_BOX_CLOSE, okAction);
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		try
		{
			final Event eventDef = getClient ().getClientDB ().findEvent (getRandomEventMessage ().getEventID (), "RandomEventUI");
			
			getDialog ().setTitle (getLanguageHolder ().findDescription (eventDef.getEventName ()));
			okAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));
			
			// Which text do we need?
			final List<LanguageText> languageText;
			if (getRandomEventMessage ().isEnding ())
				languageText = eventDef.getEventDescriptionEnd ();
			else if (getRandomEventMessage ().getTargetPlayerID () == null)
				languageText = eventDef.getEventDescriptionStart ();
			else if (getRandomEventMessage ().getTargetPlayerID ().equals (getClient ().getOurPlayerID ()))
				languageText = eventDef.getEventDescriptionPlayer ();
			else
				languageText = eventDef.getEventDescriptionEnemy ();
			
			// Look up things we need to replace variables in the text
			String text = getLanguageHolder ().findDescription (languageText);

			if (getRandomEventMessage ().getCitySizeID () != null)
			{
				final CitySize citySize = getClient ().getClientDB ().findCitySize (getRandomEventMessage ().getCitySizeID (), "RandomEventUI");
				text = text.replaceAll ("CITY_SIZE_AND_NAME_INCLUDING_OWNER", getLanguageHolder ().findDescription (citySize.getCitySizeNameIncludingOwner ())).replaceAll
					("CITY_SIZE_AND_NAME", getLanguageHolder ().findDescription (citySize.getCitySizeName ())).replaceAll
					("CITY_NAME", getRandomEventMessage ().getCityName ());
			}
			
			if (getRandomEventMessage ().getTargetPlayerID () != null)
			{
				final PlayerPublicDetails targetPlayer = getMultiplayerSessionUtils ().findPlayerWithID
					(getClient ().getPlayers (), getRandomEventMessage ().getTargetPlayerID (), "RandomEventUI");
				text = text.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (targetPlayer));
			}
			
			if (getRandomEventMessage ().getMapFeatureID () != null)
			{
				final MapFeatureEx mapFeature = getClient ().getClientDB ().findMapFeature (getRandomEventMessage ().getMapFeatureID (), "RandomEventUI");
				text = text.replaceAll ("MAP_FEATURE", getLanguageHolder ().findDescription (mapFeature.getMapFeatureDescription ()));
			}
			
			if (getRandomEventMessage ().getHeroItemName () != null)
				text = text.replaceAll ("ITEM_NAME", getRandomEventMessage ().getHeroItemName ());
			
			if (getRandomEventMessage ().getGoldAmount () != null)
				text = text.replaceAll ("GOLD_AMOUNT", getTextUtils ().intToStrCommas (getRandomEventMessage ().getGoldAmount ()));
			
			if (getRandomEventMessage ().getAttackCitySpellResult () != null)
				text = text.replaceAll ("UNITS_KILLED", Integer.valueOf (getRandomEventMessage ().getAttackCitySpellResult ().getUnitsKilled ()).toString ()).replaceAll
					("BUILDINGS_DESTROYED", Integer.valueOf (getRandomEventMessage ().getAttackCitySpellResult ().getBuildingsDestroyed ()).toString ()).replaceAll
					("POPULATION_KILLED", (getRandomEventMessage ().getAttackCitySpellResult ().getPopulationKilled () == 0 ? "0" :
						(getRandomEventMessage ().getAttackCitySpellResult ().getPopulationKilled () + ",000")));
			
			messageText.setText (text);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Shortcut keys
		getLanguageHolder ().configureShortcutKeys (contentPane);
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getRandomEventLayout ()
	{
		return randomEventLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setRandomEventLayout (final XmlLayoutContainerEx layout)
	{
		randomEventLayout = layout;
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
	 * @return Details of the random event
	 */
	public final RandomEventMessage getRandomEventMessage ()
	{
		return randomEventMessage;
	}

	/**
	 * @param e Details of the random event
	 */
	public final void setRandomEventMessage (final RandomEventMessage e)
	{
		randomEventMessage = e;
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
	 * @return Music player
	 */
	public final MomAudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final MomAudioPlayer player)
	{
		musicPlayer = player;
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
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
	}
}