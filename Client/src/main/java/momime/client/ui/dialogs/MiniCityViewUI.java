package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.messages.process.AddBuildingMessageImpl;
import momime.client.messages.process.AddMaintainedSpellMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.panels.CityViewPanel;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.OverlandMapCityData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Mini city screen, used to show spells and random effects.
 * This blocks the rest of the client and processing of all further messages until it is closed.
 */
public final class MiniCityViewUI extends MomClientDialogUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MiniCityViewUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx miniCityViewLayout;
	
	/** Large font */
	private Font largeFont;

	/** Panel where all the buildings are drawn */
	private CityViewPanel cityViewPanel;
	
	/** Multiplayer client */
	private MomClient client;

	/** Music player */
	private AudioPlayer musicPlayer;

	/** Sound effects player */
	private AudioPlayer soundPlayer;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** The city being viewed */
	private MapCoordinates3DEx cityLocation;

	/** City size+name label */
	private JLabel cityNameLabel;
	
	/** Label showing text at the bottom */
	private JLabel textLabel;
	
	/** Spell that we're displaying this popup for; null if we're not displaying a spell */
	private AddMaintainedSpellMessageImpl spellMessage;
	
	/** Building that we're displaying this popup for; null if we're not displaying a building */
	private AddBuildingMessageImpl buildingMessage;

	/** Whether the spell or building has been added yet */
	private boolean added;
	
	/** Whether we've unblocked the message queue */
	private boolean unblocked;
	
	/** Timer to delay the building or spell being added */
	private Timer timer;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getCityLocation ());
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/miniBackground.png");
		
		// Initialize the dialog
		final MiniCityViewUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				try
				{
					getCityViewPanel ().cityViewClosing ();
					getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				
					// If we never added the spell/building then do so now (maybe they clicked to close the windows really fast, before the animation could play out)
					if (!added)
						addSpellOrBuilding ();
					
					// Unblock the message that caused this
					// This somehow seems to get called twice, so protect against that
					if (!unblocked)
					{
						if (getSpellMessage () != null)
							getClient ().finishCustomDurationMessage (getSpellMessage ());
		
						if (getBuildingMessage () != null)
							getClient ().finishCustomDurationMessage (getBuildingMessage ());
						
						unblocked = true;
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = -3994695426286237110L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getMiniCityViewLayout ()));

		cityNameLabel = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (cityNameLabel, "frmMiniCityName");

		getCityViewPanel ().setCityLocation (getCityLocation ());
		contentPane.add (getCityViewPanel (), "frmMiniCityView");
		
		textLabel = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (textLabel, "frmMiniCityText");

		// Make the spell appear after a few seconds
		String spellID = null;
		if (getSpellMessage () != null)
			spellID = getSpellMessage ().getData ().getSpellID ();
		else if (getBuildingMessage () != null)
			spellID = getBuildingMessage ().getData ().getBuildingCreatedFromSpellID ();
		
		if (spellID != null)
			try
			{
				final momime.client.graphics.database.v0_9_5.Spell spellGfx = getGraphicsDB ().findSpell (spellID, "MiniCityViewUI");
				
				// Play the right music
				if (spellGfx.getSpellMusicFile () != null)
					getMusicPlayer ().playThenResume (spellGfx.getSpellMusicFile ());
				
				// If there's no delay, then play the sound effect and add the building right away too
				if ((spellGfx.getSoundAndImageDelay () == null) || (spellGfx.getSoundAndImageDelay () <= 0))
				{
					if (spellGfx.getSpellSoundFile () != null)
						getSoundPlayer ().playAudioFile (spellGfx.getSpellSoundFile ());
					
					addSpellOrBuilding ();
				}
				else
				{
					// Set up a timer to add the spell or building after a while
					timer = new Timer (spellGfx.getSoundAndImageDelay () * 1000, new ActionListener ()
					{
						@Override
						public final void actionPerformed (final ActionEvent ev)
						{
							timer.stop ();
							if (!added)
								try
								{
									if (spellGfx.getSpellSoundFile () != null)
										getSoundPlayer ().playAudioFile (spellGfx.getSpellSoundFile ());
								
									addSpellOrBuilding ();
									
									// The added spell/building might need an additional animation set up for it
									getCityViewPanel ().init ();
									getCityViewPanel ().repaint ();		// In case it isn't an animation, still need to force a repaint
								}
								catch (final Exception e)
								{
									log.error (e, e);
								}
						}
					});
					timer.start ();
				}
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}

		// Lock frame size
		getCityViewPanel ().init ();
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		setCloseOnClick (true);

		log.trace ("Exiting init");
	}
	
	/**
	 * Actually adds the spell or building(s) to the player's memory
	 * @throws IOException If there is a problem
	 */
	private final void addSpellOrBuilding () throws IOException
	{
		log.trace ("Entering addSpellOrBuilding: " + getCityLocation ());
		
		// Spells are easy, can just add the data directly
		if (getSpellMessage () != null)
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().add (getSpellMessage ().getData ());
		
		// May be up to two buildings to add
		if (getBuildingMessage () != null)
		{
			final MemoryBuilding firstBuilding = new MemoryBuilding ();
			firstBuilding.setBuildingID (getBuildingMessage ().getData ().getFirstBuildingID ());
			firstBuilding.setCityLocation (new MapCoordinates3DEx (getCityLocation ()));
			
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ().add (firstBuilding);
			
			if (getBuildingMessage ().getData ().getSecondBuildingID () != null)
			{
				final MemoryBuilding secondBuilding = new MemoryBuilding ();
				secondBuilding.setBuildingID (getBuildingMessage ().getData ().getSecondBuildingID ());
				secondBuilding.setCityLocation (new MapCoordinates3DEx (getCityLocation ()));
				
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ().add (secondBuilding);
			}
		}

		// If we've got a city screen open showing where the spell or building was added, may need to set up animation to display it
		final CityViewUI cityView = getClient ().getCityViews ().get (getCityLocation ().toString ());
		if (cityView != null)
		{
			cityView.cityDataChanged ();
			cityView.spellsChanged ();
		}

		// Addition of a building will alter what we can construct in that city, if we've got the change construction screen open.
		// Potentially that's true for spells too - casting Wall of Stone means we have to take City Walls off the list of what can be built.
		final ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (getCityLocation ().toString ());
		if (changeConstruction != null)
			changeConstruction.updateWhatCanBeConstructed ();
		
		added = true;
		log.trace ("Exiting addSpellOrBuilding");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged: " + getCityLocation ());
		
		// Get details about the city
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		if (cityData != null)
		{
			final String cityName = getLanguage ().findCitySizeName (cityData.getCitySizeID ()).replaceAll ("CITY_NAME", cityData.getCityName ()); 
			cityNameLabel.setText (cityName);
			getDialog ().setTitle (cityName);
		}
		
		// Set the text at the bottom
		String spellID = null;
		Integer castingPlayerID = null;
		if (getSpellMessage () != null)
		{
			spellID = getSpellMessage ().getData ().getSpellID ();
			castingPlayerID = getSpellMessage ().getData ().getCastingPlayerID ();
		}
		else if (getBuildingMessage () != null)
		{
			spellID = getBuildingMessage ().getData ().getBuildingCreatedFromSpellID ();
			castingPlayerID = getBuildingMessage ().getData ().getBuildingCreationSpellCastByPlayerID ();
		}

		String text = null;
		if ((spellID != null) && (castingPlayerID != null))
		{
			final Spell spell = getLanguage ().findSpell (spellID);
			final String spellName = (spell != null) ? spell.getSpellName () : null;
			
			final PlayerPublicDetails castingPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), castingPlayerID);
			
			text = getLanguage ().findCategoryEntry ("SpellCasting", (castingPlayerID == getClient ().getOurPlayerID ()) ? "YouHaveCast" : "SomeoneElseHasCast").replaceAll
				("SPELL_NAME", (spellName != null) ? spellName : spellID).replaceAll
				("PLAYER_NAME", (castingPlayer != null) ? castingPlayer.getPlayerDescription ().getPlayerName () : "Player " + castingPlayerID);
		}
		textLabel.setText (text);

		log.trace ("Exiting languageChanged");
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getMiniCityViewLayout ()
	{
		return miniCityViewLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setMiniCityViewLayout (final XmlLayoutContainerEx layout)
	{
		miniCityViewLayout = layout;
	}
	
	/**
	 * @return Large font
	 */
	public final Font getLargeFont ()
	{
		return largeFont;
	}

	/**
	 * @param font Large font
	 */
	public final void setLargeFont (final Font font)
	{
		largeFont = font;
	}
	
	/**
	 * @return Panel where all the buildings are drawn
	 */
	public final CityViewPanel getCityViewPanel ()
	{
		return cityViewPanel;
	}

	/**
	 * @param pnl Panel where all the buildings are drawn
	 */
	public final void setCityViewPanel (final CityViewPanel pnl)
	{
		cityViewPanel = pnl;
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
	public final AudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final AudioPlayer player)
	{
		musicPlayer = player;
	}
	
	/**
	 * @return Sound effects player
	 */
	public final AudioPlayer getSoundPlayer ()
	{
		return soundPlayer;
	}

	/**
	 * @param player Sound effects player
	 */
	public final void setSoundPlayer (final AudioPlayer player)
	{
		soundPlayer = player;
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
	 * @return The city being viewed
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param loc The city being viewed
	 */
	public final void setCityLocation (final MapCoordinates3DEx loc)
	{
		cityLocation = loc;
	}
	
	/**
	 * @return Spell that we're displaying this popup for; null if we're not displaying a spell
	 */
	public final AddMaintainedSpellMessageImpl getSpellMessage ()
	{
		return spellMessage;
	}

	/**
	 * @param msg Spell that we're displaying this popup for; null if we're not displaying a spell
	 */
	public final void setSpellMessage (final AddMaintainedSpellMessageImpl msg)
	{
		spellMessage = msg;
	}
	
	/**
	 * @return Building that we're displaying this popup for; null if we're not displaying a building
	 */
	public final AddBuildingMessageImpl getBuildingMessage ()
	{
		return buildingMessage;
	}
	
	/**
	 * @param msg Building that we're displaying this popup for; null if we're not displaying a building
	 */
	public final void setBuildingMessage (final AddBuildingMessageImpl msg)
	{
		buildingMessage = msg;
	}
}