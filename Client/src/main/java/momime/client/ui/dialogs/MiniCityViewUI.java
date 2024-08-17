package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.random.RandomUtils;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.audio.MomAudioPlayer;
import momime.client.messages.process.AddBuildingMessageImpl;
import momime.client.messages.process.AddOrUpdateMaintainedSpellMessageImpl;
import momime.client.messages.process.DestroyBuildingMessageImpl;
import momime.client.messages.process.UpdateWizardStateMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.panels.CityViewPanel;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.LanguageText;
import momime.common.database.Spell;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.servertoclient.RenderCityData;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Mini city screen, used to show spells and random effects.
 * This blocks the rest of the client and processing of all further messages until it is closed.
 */
public final class MiniCityViewUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MiniCityViewUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx miniCityViewLayout;
	
	/** Large font */
	private Font largeFont;

	/** Panel where all the buildings are drawn */
	private CityViewPanel cityViewPanel;
	
	/** Multiplayer client */
	private MomClient client;

	/** Music player */
	private MomAudioPlayer musicPlayer;

	/** Sound effects player */
	private MomAudioPlayer soundPlayer;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** The city being viewed, note this is optional and will be null when displaying Spell of Return animation */
	private MapCoordinates3DEx cityLocation;

	/** Details about the city to draw; the caller has to build this and pass it in */
	private RenderCityData renderCityData;
	
	/** City size+name label */
	private JLabel cityNameLabel;
	
	/** Label showing text at the bottom */
	private JLabel textLabel;
	
	/** Spell that we're displaying this popup for; null if we're not displaying a spell */
	private AddOrUpdateMaintainedSpellMessageImpl addSpellMessage;
	
	/** Building that we're displaying this popup for; null if we're not displaying a building */
	private AddBuildingMessageImpl addBuildingMessage;

	/** Building that we're showing being destroying; null if we're not destroying a building */
	private DestroyBuildingMessageImpl destroyBuildingMessage;

	/** Update wizard state message */
	private UpdateWizardStateMessageImpl updateWizardStateMessage;
	
	/** Whether the spell or building has been added yet */
	private boolean added;
	
	/** Whether we've unblocked the message queue */
	private boolean unblocked;
	
	/** Timer to delay the building or spell being added */
	private Timer timer;
	
	/** Timer for Earthquake effect */
	private Timer earthquakeTimer;
	
	/** Tick counter for earthquake effect */
	private int earthquakeTicks;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/miniBackground.png");
		
		// Initialize the dialog
		final MiniCityViewUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				if (timer != null)
					timer.stop ();

				if (earthquakeTimer != null)
					earthquakeTimer.stop ();
				
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
						if (getAddSpellMessage () != null)
							getClient ().finishCustomDurationMessage (getAddSpellMessage ());
		
						if (getAddBuildingMessage () != null)
							getClient ().finishCustomDurationMessage (getAddBuildingMessage ());
						
						if (getDestroyBuildingMessage () != null)
							getClient ().finishCustomDurationMessage (getDestroyBuildingMessage ());
						
						if (getUpdateWizardStateMessage () != null)
							getClient ().finishCustomDurationMessage (getUpdateWizardStateMessage ());
						
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
		getCityViewPanel ().setRenderCityData (getRenderCityData ());
		contentPane.add (getCityViewPanel (), "frmMiniCityView");
		
		textLabel = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (textLabel, "frmMiniCityText");

		// Make the spell appear after a few seconds
		String spellID = null;
		if (getAddSpellMessage () != null)
			spellID = getAddSpellMessage ().getMaintainedSpell ().getSpellID ();
		else if (getAddBuildingMessage () != null)
			spellID = getAddBuildingMessage ().getBuildingsCreatedFromSpellID ();
		else if (getDestroyBuildingMessage () != null)
			spellID = getDestroyBuildingMessage ().getBuildingsDestroyedBySpellID ();
		else if (getUpdateWizardStateMessage () != null)
			spellID = CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN; 
		
		if (spellID != null)
			try
			{
				final Spell spell = getClient ().getClientDB ().findSpell (spellID, "MiniCityViewUI");
				
				// Play the right music
				if (spell.getSpellMusicFile () != null)
					getMusicPlayer ().playThenResume (spell.getSpellMusicFile ());
				
				// If there's no delay, then play the sound effect and add the building right away too
				if ((spell.getSoundAndImageDelay () == null) || (spell.getSoundAndImageDelay () <= 0))
				{
					if (spell.getSpellSoundFile () != null)
						getSoundPlayer ().playAudioFile (spell.getSpellSoundFile ());
					
					addSpellOrBuilding ();
				}
				else
				{
					// Set up a timer to add the spell or building after a while
					timer = new Timer (spell.getSoundAndImageDelay () * 1000, (ev) ->
					{
						timer.stop ();
						if (!added)
							try
							{
								if (spell.getSpellSoundFile () != null)
									getSoundPlayer ().playAudioFile (spell.getSpellSoundFile ());
							
								addSpellOrBuilding ();
								
								// The added spell/building might need an additional animation set up for it
								getCityViewPanel ().init ();
								getCityViewPanel ().repaint ();		// In case it isn't an animation, still need to force a repaint
								
								// Start Earthquake shake effect
								if (spell.getAttackSpellOverlandTarget () == AttackSpellTargetID.ALL_UNITS_AND_BUILDINGS)
									earthquakeAnimation ();
							}
							catch (final Exception e)
							{
								log.error (e, e);
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
	}
	
	/**
	 * Actually adds the spell or building(s) to the player's memory
	 * @throws IOException If there is a problem
	 */
	private final void addSpellOrBuilding () throws IOException
	{
		// Spells are easy, can just add the data directly
		if (getAddSpellMessage () != null)
		{
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().add (getAddSpellMessage ().getMaintainedSpell ());
			
			if ((getAddSpellMessage ().getMaintainedSpell ().getCitySpellEffectID () != null) &&
				(!getRenderCityData ().getCitySpellEffectID ().contains (getAddSpellMessage ().getMaintainedSpell ().getCitySpellEffectID ())))
				
				getRenderCityData ().getCitySpellEffectID ().add (getAddSpellMessage ().getMaintainedSpell ().getCitySpellEffectID ());
		}
		
		// May be up to two buildings to add
		if (getAddBuildingMessage () != null)
		{
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ().addAll (getAddBuildingMessage ().getBuilding ());

			for (final MemoryBuilding thisBuilding : getAddBuildingMessage ().getBuilding ())
				if (!getRenderCityData ().getBuildingID ().contains (thisBuilding.getBuildingID ()))
					getRenderCityData ().getBuildingID ().add (thisBuilding.getBuildingID ());
		}
		
		// Don't really add the buildings, just show them in the display
		if (getUpdateWizardStateMessage () != null)
		{
			if (!getRenderCityData ().getBuildingID ().contains (CommonDatabaseConstants.BUILDING_FORTRESS))
				getRenderCityData ().getBuildingID ().add (CommonDatabaseConstants.BUILDING_FORTRESS);

			if (!getRenderCityData ().getBuildingID ().contains (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE))
				getRenderCityData ().getBuildingID ().add (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE);
		}
		
		// Destroy buildings
		if (getDestroyBuildingMessage () != null)
			for (final Integer buildingURN : getDestroyBuildingMessage ().getBuildingURN ())
			{
				final MemoryBuilding building = getMemoryBuildingUtils ().findBuildingURN
					(buildingURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), "MiniCityViewUI (d)");
				
				getMemoryBuildingUtils ().removeBuildingURN (buildingURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ());
				
				getRenderCityData ().getBuildingID ().remove (building.getBuildingID ());
				getRenderCityData ().getRubbleBuildingID ().add (building.getBuildingID ());
			}
		
		if (getCityLocation () != null)
		{
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
		}
		
		added = true;
	}
	
	/**
	 * Starts animation jiggling the dialog box around
	 */
	private final void earthquakeAnimation ()
	{
		final Point dialogStartLocation = getDialog ().getLocation ();
		earthquakeTicks = 0;
		
		earthquakeTimer = new Timer (25, (ev) ->
		{
			// Shake very little at start, build up to maximum in the middle, then ease off again
			final int range = (earthquakeTicks < 50) ? earthquakeTicks : (100 - earthquakeTicks);
			if (range > 0)
				getDialog ().setLocation (dialogStartLocation.x + getRandomUtils ().nextInt ((range * 2) + 1) - range,
					dialogStartLocation.y + getRandomUtils ().nextInt ((range * 2) + 1) - range);
			
			earthquakeTicks++;
			if (earthquakeTicks >= 100)
				earthquakeTimer.stop ();
		});
		earthquakeTimer.start ();
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Get details about the city
		if (getRenderCityData () != null)
			try
			{
				String cityName = getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findCitySize (getRenderCityData ().getCitySizeID (), "MiniCityViewUI").getCitySizeNameIncludingOwner ());
				cityName = cityName.replaceAll ("CITY_NAME", getRenderCityData ().getCityName ());
				
				final PlayerPublicDetails cityOwner = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getRenderCityData ().getCityOwnerID ());
				if (cityOwner != null)
					cityName = cityName.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (cityOwner));
				
				cityNameLabel.setText (cityName);
				getDialog ().setTitle (cityName);
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		
		// Set the text at the bottom
		// Use the name of the specific effect in preference to the generic spell name, so Spell Ward says e.g. "You have completed casting Chaos Ward"
		String spellID = null;
		String citySpellEffectID = null;
		Integer castingPlayerID = null;
		if (getAddSpellMessage () != null)
		{
			citySpellEffectID = getAddSpellMessage ().getMaintainedSpell ().getCitySpellEffectID ();
			castingPlayerID = getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID ();
		}
		else if (getAddBuildingMessage () != null)
		{
			spellID = getAddBuildingMessage ().getBuildingsCreatedFromSpellID ();
			castingPlayerID = getAddBuildingMessage ().getBuildingCreationSpellCastByPlayerID ();
		}
		else if (getDestroyBuildingMessage () != null)
		{
			spellID = getDestroyBuildingMessage ().getBuildingsDestroyedBySpellID ();
			castingPlayerID = getDestroyBuildingMessage ().getBuildingDestructionSpellCastByPlayerID ();
		}
		else if (getUpdateWizardStateMessage () != null)
		{
			spellID = CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN;
			castingPlayerID = getRenderCityData ().getCityOwnerID ();
		}

		try
		{
			String text = null;
			if (((spellID != null) || (citySpellEffectID != null)) && (castingPlayerID != null))
			{
				final String useSpellName;
				if (citySpellEffectID != null)
					useSpellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findCitySpellEffect (citySpellEffectID, "MiniCityViewUI").getCitySpellEffectName ());
				else
					useSpellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (spellID, "MiniCityViewUI").getSpellName ());
				
				final PlayerPublicDetails castingPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), castingPlayerID);
				
				final List<LanguageText> languageText = (castingPlayerID.equals (getClient ().getOurPlayerID ())) ? 
					getLanguages ().getSpellCasting ().getYouHaveCast () : getLanguages ().getSpellCasting ().getSomeoneElseHasCast ();
					
				text = getLanguageHolder ().findDescription (languageText).replaceAll
					("SPELL_NAME", useSpellName).replaceAll
					("PLAYER_NAME", (castingPlayer != null) ? getWizardClientUtils ().getPlayerName (castingPlayer) : "Player " + castingPlayerID);
			}
			textLabel.setText (text);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
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
	 * @return Sound effects player
	 */
	public final MomAudioPlayer getSoundPlayer ()
	{
		return soundPlayer;
	}

	/**
	 * @param player Sound effects player
	 */
	public final void setSoundPlayer (final MomAudioPlayer player)
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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param mbu Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils mbu)
	{
		memoryBuildingUtils = mbu;
	}
	
	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
	
	/**
	 * @return The city being viewed, note this is optional and will be null when displaying Spell of Return animation
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param loc The city being viewed, note this is optional and will be null when displaying Spell of Return animation
	 */
	public final void setCityLocation (final MapCoordinates3DEx loc)
	{
		cityLocation = loc;
	}
	
	/**
	 * @return Details about the city to draw; the caller has to build this and pass it in
	 */
	public final RenderCityData getRenderCityData ()
	{
		return renderCityData;
	}

	/**
	 * @param r Details about the city to draw; the caller has to build this and pass it in
	 */
	public final void setRenderCityData (final RenderCityData r)
	{
		renderCityData = r;
	}

	/**
	 * @return Spell that we're displaying this popup for; null if we're not displaying a spell
	 */
	public final AddOrUpdateMaintainedSpellMessageImpl getAddSpellMessage ()
	{
		return addSpellMessage;
	}

	/**
	 * @param msg Spell that we're displaying this popup for; null if we're not displaying a spell
	 */
	public final void setAddSpellMessage (final AddOrUpdateMaintainedSpellMessageImpl msg)
	{
		addSpellMessage = msg;
	}
	
	/**
	 * @return Building that we're displaying this popup for; null if we're not displaying a building
	 */
	public final AddBuildingMessageImpl getAddBuildingMessage ()
	{
		return addBuildingMessage;
	}
	
	/**
	 * @param msg Building that we're displaying this popup for; null if we're not displaying a building
	 */
	public final void setAddBuildingMessage (final AddBuildingMessageImpl msg)
	{
		addBuildingMessage = msg;
	}

	/**
	 * @return Building that we're showing being destroying; null if we're not destroying a building
	 */
	public final DestroyBuildingMessageImpl getDestroyBuildingMessage ()
	{
		return destroyBuildingMessage;
	}

	/**
	 * @param msg Building that we're showing being destroying; null if we're not destroying a building
	 */
	public final void setDestroyBuildingMessage (final DestroyBuildingMessageImpl msg)
	{
		destroyBuildingMessage = msg;
	}

	/**
	 * @return Update wizard state message
	 */
	public final UpdateWizardStateMessageImpl getUpdateWizardStateMessage ()
	{
		return updateWizardStateMessage;
	}

	/**
	 * @param msg Update wizard state message
	 */
	public final void setUpdateWizardStateMessage (final UpdateWizardStateMessageImpl msg)
	{
		updateWizardStateMessage = msg;
	}
}