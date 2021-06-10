package momime.client.ui.dialogs;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.messages.process.PlayAnimationMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;
import momime.common.database.WizardEx;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.WizardState;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Animation of portal opening and wizards getting balled up and sucked into it when someone finishes casting the Spell of Mastery
 */
public final class SpellOfMasteryEndUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SpellOfMasteryEndUI.class);

	/** Milliseconds between animation frames */
	private final static int TIMER_TICKS_MS = 150;

	/** XML layout */
	private XmlLayoutContainerEx spellOfMasteryEndLayout;

	/** Large font */
	private Font largeFont;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Music player */
	private AudioPlayer musicPlayer;

	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** The wizard who completed casting Spell of Mastery */
	private PlayerPublicDetails castingWizard;
	
	/** Animations of the wizards who are being banished by the spell */
	private List<AnimationEx> banishedWizardAnims = new ArrayList<AnimationEx> ();
	
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
		// Find details about the wizards involved
		// Total up the number of "ball" frames while we're at it
		int totalFrameCount = 6 + 6;  // 6 of portal opening, 6 of portal closing
		
		final MomPersistentPlayerPublicKnowledge castingWizardPub = (MomPersistentPlayerPublicKnowledge) getCastingWizard ().getPersistentPlayerPublicKnowledge ();
		final WizardEx castingWizardDef = (castingWizardPub.getStandardPhotoID () == null) ? null :
			getClient ().getClientDB ().findWizard (castingWizardPub.getStandardPhotoID (), "SpellOfMasteryEndUI (C)");

		for (final PlayerPublicDetails player : getClient ().getPlayers ())
			if (player != getCastingWizard ())
			{
				final MomPersistentPlayerPublicKnowledge banishedWizardPub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
				if ((PlayerKnowledgeUtils.isWizard (banishedWizardPub.getWizardID ())) && (banishedWizardPub.getWizardState () != WizardState.DEFEATED) &&
					(banishedWizardPub.getStandardPhotoID () != null))
				{
					final WizardEx banishedWizardDef = getClient ().getClientDB ().findWizard (banishedWizardPub.getStandardPhotoID (), "SpellOfMasteryEndUI (B)");
					final AnimationEx banishedWizardAnim = getClient ().getClientDB ().findAnimation (banishedWizardDef.getBallAnimation (), "SpellOfMasteryEndUI (B)");
					banishedWizardAnims.add (banishedWizardAnim);
					
					totalFrameCount = totalFrameCount + banishedWizardAnim.getFrame ().size ();
				}
			}
		
		// Animations
		final XmlLayoutComponent chantingWizardLayout = getSpellOfMasteryEndLayout ().findComponent ("frmSpellOfMasteryEndChanting");
		final AnimationEx chantingAnim = (castingWizardDef == null) ? null :
			getClient ().getClientDB ().findAnimation (castingWizardDef.getChantingAnimation (), "SpellOfMasteryEndUI (C)");

		final XmlLayoutComponent portalLayout = getSpellOfMasteryEndLayout ().findComponent ("frmSpellOfMasteryEndPortal");
		final AnimationEx portalAnim = getGraphicsDB ().findAnimation (GraphicsDatabaseConstants.ANIM_SPELL_OF_MASTERY_PORTAL, "SpellOfMasteryEndUI");
		
		// Static images
		final Image background = getUtils ().loadImage ("/momime.client.graphics/animations/wizardsLab/wizardsLab.png").getScaledInstance
			(getSpellOfMasteryEndLayout ().getFormWidth (), getSpellOfMasteryEndLayout ().getFormHeight (), Image.SCALE_SMOOTH);
		
		// Initialize the frame
		final SpellOfMasteryEndUI ui = this;
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
					}
					
					// Stop animation timer
					if ((timer != null) && (timer.isRunning ()))
						timer.stop ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
		
		// Initialize the content pane
		final int totalTickCount = totalFrameCount;
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, null);
				
				// Casting wizard waves arms in an endless loop, at half the frame rate
				if (chantingAnim != null)
					try
					{
						final Image image = getUtils ().loadImage (chantingAnim.getFrame ().get ((tickNumber/2) % chantingAnim.getFrame ().size ())).getScaledInstance
							(chantingWizardLayout.getWidth (), chantingWizardLayout.getHeight (), Image.SCALE_SMOOTH);
						g.drawImage (image, chantingWizardLayout.getLeft (), chantingWizardLayout.getTop (), null);
					}
					catch (final IOException e)
					{
						log.error ("Error loading Spell of Mastery End chanting animation frame", e);
					}
				
				// Portal anim
				try
				{
					final int portalFrameNumber;
					if (tickNumber < 6)
						portalFrameNumber = tickNumber;
					else if (tickNumber >= totalTickCount - 6)
						portalFrameNumber = tickNumber - totalTickCount + portalAnim.getFrame ().size ();
					else
						portalFrameNumber = 6 + ((tickNumber - 6) % 10);
					
					final Image image = getUtils ().loadImage (portalAnim.getFrame ().get (portalFrameNumber)).getScaledInstance
						(portalLayout.getWidth (), portalLayout.getHeight (), Image.SCALE_SMOOTH);
					g.drawImage (image, portalLayout.getLeft (), portalLayout.getTop (), null);
				}
				catch (final IOException e)
				{
					log.error ("Error loading Spell of Mastery End chanting portal frame", e);
				}
				
				// Ball anim
				if ((tickNumber >= 6) && (tickNumber < totalTickCount - 6))
					try
					{
						int ballTickNumber = tickNumber - 6;
						int ballNo = 0;
						AnimationEx ballAnim = banishedWizardAnims.get (ballNo);
						while (ballTickNumber >= ballAnim.getFrame ().size ())
						{
							ballTickNumber = ballTickNumber - ballAnim.getFrame ().size ();
							ballNo++;
							ballAnim = banishedWizardAnims.get (ballNo);
						}
						
						final Image image = getUtils ().loadImage (ballAnim.getFrame ().get (ballTickNumber)).getScaledInstance
							(getSpellOfMasteryEndLayout ().getFormWidth (), getSpellOfMasteryEndLayout ().getFormHeight (), Image.SCALE_SMOOTH);
						g.drawImage (image, 0, 0, null);
					}
					catch (final IOException e)
					{
						log.error ("Error loading Spell of Mastery End ball animation frame", e);
					}
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getSpellOfMasteryEndLayout ()));
		
		lineLabel = getUtils ().createShadowedLabel (MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (lineLabel, "frmSpellOfMasteryEndLine");
		
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
			if (tickNumber >= totalTickCount)
				getDialog ().dispose ();
			else
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
		getDialog ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getSpellOfMasteryEndScreen ().getTitle ()));
		
		lineLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellOfMasteryEndScreen ().getLine ()).replaceAll
			("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getCastingWizard ())));
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getSpellOfMasteryEndLayout ()
	{
		return spellOfMasteryEndLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setSpellOfMasteryEndLayout (final XmlLayoutContainerEx layout)
	{
		spellOfMasteryEndLayout = layout;
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
	 * @return The wizard who completed casting Spell of Mastery
	 */
	public final PlayerPublicDetails getCastingWizard ()
	{
		return castingWizard;
	}

	/**
	 * @param w The wizard who completed casting Spell of Mastery
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