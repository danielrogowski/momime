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

import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.messages.process.PlayAnimationMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.common.database.AnimationEx;
import momime.common.database.WizardEx;
import momime.common.messages.KnownWizardDetails;

/**
 * Animation when you win the game
 */
public final class WizardWonUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (WizardWonUI.class);
	
	/** Milliseconds between animation frames */
	private final static int TIMER_TICKS_MS = 150;

	/** XML layout */
	private XmlLayoutContainerEx wizardWonLayout;

	/** Large font */
	private Font largeFont;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Music player */
	private AudioPlayer musicPlayer;
	
	/** The wizard who won */
	private KnownWizardDetails winningWizard;
	
	/** Play animation message */
	private PlayAnimationMessageImpl playAnimationMessage;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Line of text */
	private JLabel lineLabel;
	
	/** Line number to display */
	private int lineTextNumber;

	/** Content pane */
	private JPanel contentPane;
	
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
		// Find details about the wizard
		final WizardEx winningWizardDef = (getWinningWizard ().getStandardPhotoID () == null) ? null :
			getClient ().getClientDB ().findWizard (getWinningWizard ().getStandardPhotoID (), "WizardWonUI");
		
		final XmlLayoutComponent handsLayout = getWizardWonLayout ().findComponent ("frmWizardWonHands");
		final Image handsImage = (winningWizardDef == null) ? null : getUtils ().loadImage (winningWizardDef.getWorldHandsImageFile ()).getScaledInstance
			(handsLayout.getWidth (), handsLayout.getHeight (), Image.SCALE_SMOOTH);
		
		final XmlLayoutComponent talkingLayout = getWizardWonLayout ().findComponent ("frmWizardWonTalking");
		final AnimationEx talkingAnim = (winningWizardDef == null) ? null : getClient ().getClientDB ().findAnimation (winningWizardDef.getTalkingAnimation (), "WizardWonUI (T)");
		
		// Static images
		final Image background = getUtils ().loadImage ("/momime.client.graphics/animations/worlds/background.png").getScaledInstance
			(getWizardWonLayout ().getFormWidth (), getWizardWonLayout ().getFormHeight (), Image.SCALE_SMOOTH);
		
		// Animations
		final XmlLayoutComponent worldsLayout = getWizardWonLayout ().findComponent ("frmWizardWonWorlds");
		final AnimationEx worldsAnim = getGraphicsDB ().findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_WON_WORLDS, "WizardWonUI (W)");
		
		final XmlLayoutComponent sparklesLayout = getWizardWonLayout ().findComponent ("frmWizardWonSparkles");
		final AnimationEx sparklesAnim = getGraphicsDB ().findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_WON_SPARKLES, "WizardWonUI (S)");
		
		// Initialize the frame
		final WizardWonUI ui = this;
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
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, null);
				
				// Worlds spin on forever
				final int worldsAnimFrameNumber = (tickNumber < worldsAnim.getFrame ().size ()) ? tickNumber :
					(((tickNumber - 47) % 20) + 47);
				try
				{
					final Image worldsImage = getUtils ().loadImage (worldsAnim.getFrame ().get (worldsAnimFrameNumber).getImageFile ()).getScaledInstance
						(worldsLayout.getWidth (), worldsLayout.getHeight (), Image.SCALE_SMOOTH);
					g.drawImage (worldsImage, worldsLayout.getLeft (), worldsLayout.getTop (), null);
				}
				catch (final IOException e)
				{
					log.error ("Error loading spinning worlds animation frame", e);
				}
				
				// Sparkles only run once
				final int sparklesAnimFrameNumber = tickNumber - 5;
				if ((sparklesAnimFrameNumber >= 0) && (sparklesAnimFrameNumber < sparklesAnim.getFrame ().size ()))
					try
					{
						final Image sparklesImage = getUtils ().loadImage (sparklesAnim.getFrame ().get (sparklesAnimFrameNumber).getImageFile ()).getScaledInstance
							(sparklesLayout.getWidth (), sparklesLayout.getHeight (), Image.SCALE_SMOOTH);
						g.drawImage (sparklesImage, sparklesLayout.getLeft (), sparklesLayout.getTop (), null);
					}
					catch (final IOException e)
					{
						log.error ("Error loading sparkles animation frame", e);
					}
				
				// Hands are there permanently
				if (handsImage != null)
					g.drawImage (handsImage, handsLayout.getLeft (), handsLayout.getTop (), null);
				
				// Loop talking anim permanently
				if (talkingAnim != null)
				{
					final int talkingAnimFrameNumber = tickNumber % talkingAnim.getFrame ().size ();
					try
					{
						final Image talkingImage = getUtils ().loadImage (talkingAnim.getFrame ().get (talkingAnimFrameNumber).getImageFile ()).getScaledInstance
							(talkingLayout.getWidth (), talkingLayout.getHeight (), Image.SCALE_SMOOTH);
						g.drawImage (talkingImage, talkingLayout.getLeft (), talkingLayout.getTop (), null);
					}
					catch (final IOException e)
					{
						log.error ("Error loading talking animation frame", e);
					}
				}
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getWizardWonLayout ()));
		
		lineLabel = getUtils ().createShadowedLabel (MomUIConstants.DARK_RED, MomUIConstants.RED, getLargeFont ());
		lineTextNumber = 0;
		contentPane.add (lineLabel, "frmWizardWonLine");
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		setCloseOnClick (true);
		
		// Start music
		try
		{
			getMusicPlayer ().playThenResume ("/momime.client.music/MUSIC_109 - You win.mp3");
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Start the animation
		timer = new Timer (TIMER_TICKS_MS, (ev) ->
		{
			tickNumber++;
			
			// Update to next line of text
			int lineNumber = tickNumber / 25;
			if ((lineNumber >= 1) && (lineNumber <= 4) && (lineNumber != lineTextNumber)) 
			{
				lineTextNumber = lineNumber;
				languageChanged ();
			}
			
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
		getDialog ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getWizardWonScreen ().getTitle ()));
		
		switch (lineTextNumber)
		{
			case 1: lineLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getWizardWonScreen ().getLine1 ())); break;
			case 2: lineLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getWizardWonScreen ().getLine2 ())); break;
			case 3: lineLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getWizardWonScreen ().getLine3 ())); break;
			case 4: lineLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getWizardWonScreen ().getLine4 ())); break;
		}
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getWizardWonLayout ()
	{
		return wizardWonLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setWizardWonLayout (final XmlLayoutContainerEx layout)
	{
		wizardWonLayout = layout;
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
	 * @return The wizard who won
	 */
	public final KnownWizardDetails getWinningWizard ()
	{
		return winningWizard;
	}

	/**
	 * @param w The wizard who won
	 */
	public final void setWinningWizard (final KnownWizardDetails w)
	{
		winningWizard = w;
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