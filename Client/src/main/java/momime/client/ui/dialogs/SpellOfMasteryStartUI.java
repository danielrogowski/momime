package momime.client.ui.dialogs;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.audio.MomAudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.messages.process.PlayAnimationMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;

/**
 * Animation of yellow cloud opening above fortress when someone starts casting the Spell of Mastery
 */
public final class SpellOfMasteryStartUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (WizardWonUI.class);
	
	/** Milliseconds between animation frames */
	private final static int TIMER_TICKS_MS = 150;

	/** XML layout */
	private XmlLayoutContainerEx spellOfMasteryStartLayout;

	/** Large font */
	private Font largeFont;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Music player */
	private MomAudioPlayer musicPlayer;

	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** The wizard who started casting Spell of Mastery */
	private PlayerPublicDetails castingWizard;
	
	/** Play animation message */
	private PlayAnimationMessageImpl playAnimationMessage;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Line of text */
	private JLabel lineLabel;
	
	/** Frame of animation currently drawn */
	private int tickNumber;
	
	/** Whether we've unblocked the message queue */
	private boolean unblocked;
	
	/** Animation timer */
	private Timer timer;

	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		final AnimationEx anim = getGraphicsDB ().findAnimation (GraphicsDatabaseConstants.ANIM_SPELL_OF_MASTERY_START, "SpellOfMasteryStartUI");
		
		// Initialize the frame
		final SpellOfMasteryStartUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				try
				{
					getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				
					// Unblock the message that caused this
					// This somehow seems to get called twice in MiniCityViewUI, so protect against that
					if (!unblocked)
					{
						getClient ().finishCustomDurationMessage (getPlayAnimationMessage ());
						unblocked = true;

						// Stop animation timer
						if ((timer != null) && (timer.isRunning ()))
							timer.stop ();
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
				// Last 11 frames loop forever
				int frameNumber = tickNumber;
				if (frameNumber >= anim.getFrame ().size ())
					frameNumber = ((tickNumber - anim.getFrame ().size ()) % 11) + 49;
					
				try
				{
					final Image image = getUtils ().loadImage (anim.getFrame ().get (frameNumber).getImageFile ()).getScaledInstance
						(spellOfMasteryStartLayout.getFormWidth (), spellOfMasteryStartLayout.getFormHeight (), Image.SCALE_SMOOTH);
					g.drawImage (image, 0, 0, null);
				}
				catch (final IOException e)
				{
					log.error ("Error loading Spell of Mastery Start animation frame", e);
				}
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getSpellOfMasteryStartLayout ()));
		
		lineLabel = getUtils ().createShadowedLabel (MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (lineLabel, "frmSpellOfMasteryStartLine");
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		setCloseOnClick (true);
		
		// Start music
		try
		{
			getMusicPlayer ().playThenResume ("/momime.client.music/MUSIC_012 - SpellOfMasteryStartAndEnd.mp3");
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Start the animation
		timer = new Timer (TIMER_TICKS_MS, (ev) ->
		{
			tickNumber++;
			contentPane.repaint ();
		});
		timer.start ();
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getDialog ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getSpellOfMasteryStartScreen ().getTitle ()));
		
		lineLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellOfMasteryStartScreen ().getLine ()).replaceAll
			("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getCastingWizard ())));
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getSpellOfMasteryStartLayout ()
	{
		return spellOfMasteryStartLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setSpellOfMasteryStartLayout (final XmlLayoutContainerEx layout)
	{
		spellOfMasteryStartLayout = layout;
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
	 * @return The wizard who started casting Spell of Mastery
	 */
	public final PlayerPublicDetails getCastingWizard ()
	{
		return castingWizard;
	}

	/**
	 * @param w The wizard who started casting Spell of Mastery
	 */
	public final void setCastingWizard (final PlayerPublicDetails w)
	{
		castingWizard = w;
	}
	
	/**
	 * @return Play animation message
	 */
	public final PlayAnimationMessageImpl getPlayAnimationMessage ()
	{
		return playAnimationMessage;
	}

	/**
	 * @param msg Play animation message
	 */
	public final void setPlayAnimationMessage (final PlayAnimationMessageImpl msg)
	{
		playAnimationMessage = msg;
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
}