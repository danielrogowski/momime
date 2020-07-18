package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.WizardGfx;
import momime.client.messages.process.WizardBanishedMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * Animation when a wizard loses a battle at their fortress, showing the enemy wizard blasting them.
 */
public final class WizardBanishedUI extends MomClientDialogUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (WizardBanishedUI.class);

	/** XML layout */
	private XmlLayoutContainerEx wizardBanishedLayout;

	/** Large font */
	private Font largeFont;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;

	/** Music player */
	private AudioPlayer musicPlayer;

	/** Sound effects player */
	private AudioPlayer soundPlayer;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** The wizard who was banished */
	private PlayerPublicDetails banishedWizard;

	/** The wizard (or raiders) who is banishing them */
	private PlayerPublicDetails banishingWizard;
	
	/** If false, the wizard still has at least 1 more city so they can try to cast Spell of Return.  If true, the wizard has no cities left and they are out of the game. */
	private boolean defeated;
	
	/** Graphics data stored about banished wizard */
	private WizardGfx banishedWizardGfx;
	
	/** Graphics data stored about banishing wizard; null if raiders */
	private WizardGfx banishingWizardGfx;
	
	/** Title */
	private JLabel titleLabel;

	/** Content pane */
	private JPanel contentPane;
	
	/** Frame of animation currently drawn */
	private int tickNumber;
	
	/** The message that caused us to display this dialog */
	private WizardBanishedMessageImpl wizardBanishedMessage;

	/** Whether we've unblocked the message queue */
	private boolean unblocked;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Find details about the 2 wizards involved
		// Having a pic and sound effect for the banished wizard is mandatory or we cannot draw anything sensible.
		// Having a pic for the banishing wizard is optional, as there's some mobs shown too so can just show them like raiders if nothing else.
		final MomPersistentPlayerPublicKnowledge banishedWizardPub = (MomPersistentPlayerPublicKnowledge) getBanishedWizard ().getPersistentPlayerPublicKnowledge ();
		banishedWizardGfx = getGraphicsDB ().findWizard (banishedWizardPub.getStandardPhotoID (), "WizardBanishedUI (A)");
		
		final MomPersistentPlayerPublicKnowledge banishingWizardPub = (MomPersistentPlayerPublicKnowledge) getBanishingWizard ().getPersistentPlayerPublicKnowledge ();
		banishingWizardGfx = (banishingWizardPub.getStandardPhotoID () == null) ? null :
			getGraphicsDB ().findWizard (banishingWizardPub.getStandardPhotoID (), "WizardBanishedUI (B)");
		
		final XmlLayoutComponent banishedWizardLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedStanding");
		final Image banishedWizardImage = getUtils ().loadImage (banishedWizardGfx.getStandingImageFile ()).getScaledInstance
			(banishedWizardLayout.getWidth (), banishedWizardLayout.getHeight (), Image.SCALE_SMOOTH);

		final XmlLayoutComponent banishingWizardLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedBanisher");
		final Image banishingWizardImage = (banishingWizardGfx == null) ? null : getUtils ().loadImage (banishingWizardGfx.getBanishingImageFile ()).getScaledInstance
			(banishingWizardLayout.getWidth (), banishingWizardLayout.getHeight (), Image.SCALE_SMOOTH);

		final XmlLayoutComponent banishingWizardsHandLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedBanishersHand");
		final Image banishingWizardsHandImage = (banishingWizardGfx == null) ? null : getUtils ().loadImage (banishingWizardGfx.getBanishingHandImageFile ()).getScaledInstance
			(banishingWizardsHandLayout.getWidth (), banishingWizardsHandLayout.getHeight (), Image.SCALE_SMOOTH);
		
		// Static images
		final Image background = getUtils ().loadImage ("/momime.client.graphics/animations/wizardsLab/wizardsLab.png").getScaledInstance
			(getWizardBanishedLayout ().getFormWidth (), getWizardBanishedLayout ().getFormHeight (), Image.SCALE_SMOOTH);
		
		final XmlLayoutComponent raiderFrontLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedRaiderFront");
		final Image raiderFrontImage = getUtils ().loadImage ("/momime.client.graphics/animations/wizardsLab/raiderFront.png").getScaledInstance
			(raiderFrontLayout.getWidth (), raiderFrontLayout.getHeight (), Image.SCALE_SMOOTH);
		
		final XmlLayoutComponent raiderHeadsLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedRaiderHeads");
		final Image raiderHeadsImage = getUtils ().loadImage ("/momime.client.graphics/animations/wizardsLab/raiderHeads.png").getScaledInstance
			(raiderHeadsLayout.getWidth (), raiderHeadsLayout.getHeight (), Image.SCALE_SMOOTH);
		
		// Animations
		final XmlLayoutComponent singleBlastLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedSingleBlast");
		final AnimationGfx singleBlastAnim = getGraphicsDB ().findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_BANISHED_SINGLE_BLAST, "WizardBanishedUI (C)");

		final XmlLayoutComponent doubleBlastLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedDoubleBlast");
		final AnimationGfx doubleBlastAnim = getGraphicsDB ().findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_BANISHED_DOUBLE_BLAST, "WizardBanishedUI (D)");

		final XmlLayoutComponent evaporatingLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedEvaporating");
		final AnimationGfx evaporatingAnim = getGraphicsDB ().findAnimation (banishedWizardGfx.getEvaporatingAnimation (), "WizardBanishedUI (E)");
		
		// Initialize the frame
		final WizardBanishedUI ui = this;
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
					// This somehow seems to get called twice so protect against that
					if (!unblocked)
					{
						getClient ().finishCustomDurationMessage (getWizardBanishedMessage ());
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
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, null);

				// 7 frames of raiders/wizard walking in
				final int walkingFrame = (tickNumber > 10) ? 6 : (tickNumber - 4);
				
				// Banished wizard
				// If banished by raiders, the banished wizard never actually disappears
				if ((tickNumber <= 22) || (banishingWizardImage == null))
					g.drawImage (banishedWizardImage, banishedWizardLayout.getLeft (), banishedWizardLayout.getTop (), null);
				
				if (tickNumber >= 4)
				{
					// Raider facing forwards
					g.drawImage (raiderFrontImage, (318 - (walkingFrame * 11)) * 2, ((82 - walkingFrame) * 12) / 5, null);
					
					// Banishing wizard
					final int wizardX = (298 - (walkingFrame * 15)) * 2;
					final int wizardY = ((138 - (walkingFrame * 13)) * 12) / 5;
					
					if (banishingWizardImage != null)
					{
						g.drawImage (banishingWizardImage, wizardX, wizardY, null);

						if ((tickNumber >= 11) && (tickNumber <= 14))
							try
							{
								final Image singleBlastImage = getUtils ().loadImage (singleBlastAnim.getFrame ().get (tickNumber - 11)).getScaledInstance
									(singleBlastLayout.getWidth (), singleBlastLayout.getHeight (), Image.SCALE_SMOOTH);
								g.drawImage (singleBlastImage, singleBlastLayout.getLeft (), singleBlastLayout.getTop (), null);
							}
							catch (final IOException e)
							{
								log.error ("Error loading single blast animation frame", e);
							}
	
						if ((tickNumber >= 15) && (tickNumber <= 22))
							try
							{
								final Image doubleBlastImage = getUtils ().loadImage (doubleBlastAnim.getFrame ().get (tickNumber - 15)).getScaledInstance
									(doubleBlastLayout.getWidth (), doubleBlastLayout.getHeight (), Image.SCALE_SMOOTH);
								g.drawImage (doubleBlastImage, doubleBlastLayout.getLeft (), doubleBlastLayout.getTop (), null);
							}
							catch (final IOException e)
							{
								log.error ("Error loading double blast animation frame", e);
							}
						
						if (banishingWizardsHandImage != null)
							g.drawImage (banishingWizardsHandImage, wizardX, wizardY, null);
	
						if ((tickNumber >= 23) && (tickNumber <= 26))
							try
							{
								final Image evaporatingImage = getUtils ().loadImage (evaporatingAnim.getFrame ().get (tickNumber - 23)).getScaledInstance
									(evaporatingLayout.getWidth (), evaporatingLayout.getHeight (), Image.SCALE_SMOOTH);
								g.drawImage (evaporatingImage, evaporatingLayout.getLeft (), evaporatingLayout.getTop (), null);
							}
							catch (final IOException e)
							{
								log.error ("Error loading double blast animation frame", e);
							}
					}
					
					// Back of raiders' heads
					g.drawImage (raiderHeadsImage, (130 - (walkingFrame * 5)) * 2, ((180 - (walkingFrame * 10)) * 12) / 5, null);
				}
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getWizardBanishedLayout ()));
		
		// Only title is set into the layout; all the images are dynamically drawn otherwise the layout manager keeps trying to force them back to default positions
		titleLabel = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (titleLabel, "frmWizardBanishedTitle");
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		setCloseOnClick (true);
		
		// Start music
		try
		{
			getMusicPlayer ().playThenResume ("/momime.client.music/MUSIC_110 - You lose.mp3");
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");

		final String languageEntryID = (isDefeated () ? "Defeated" : "Banished") + "By" + ((banishingWizardGfx != null) ? "Wizard" : "Raiders"); 
		
		final String title = getLanguage ().findCategoryEntry ("frmWizardBanished", languageEntryID).replaceAll
			("BANISHED_WIZARD", getWizardClientUtils ().getPlayerName (banishedWizard)).replaceAll
			("BANISHING_WIZARD", getWizardClientUtils ().getPlayerName (banishingWizard));
		
		getDialog ().setTitle (title);
		titleLabel.setText (title);
		
		log.trace ("Exiting languageChanged");
	}

	/**
	 * @return Number of seconds that the animation takes to display
	 */
	public final double getDuration ()
	{
		return 7;
	}
	
	/**
	 * @return Number of ticks that the duration is divided into
	 */
	public final int getTickCount ()
	{
		return 27;
	}
	
	/**
	 * @param t How many ticks have occurred, from 1..tickCount
	 */
	public final void tick (final int t)
	{
		tickNumber = t;
		contentPane.repaint ();
		
		if ((tickNumber == 15) && (banishedWizardGfx != null) && (banishedWizardGfx.getScreamSoundFile () != null))
			try
			{
				getSoundPlayer ().playAudioFile (banishedWizardGfx.getScreamSoundFile ());
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getWizardBanishedLayout ()
	{
		return wizardBanishedLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setWizardBanishedLayout (final XmlLayoutContainerEx layout)
	{
		wizardBanishedLayout = layout;
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
	 * @return The wizard who was banished
	 */
	public final PlayerPublicDetails getBanishedWizard ()
	{
		return banishedWizard;
	}

	/**
	 * @param w The wizard who was banished
	 */
	public final void setBanishedWizard (final PlayerPublicDetails w)
	{
		banishedWizard = w;
	}

	/**
	 * @return The wizard (or raiders) who is banishing them
	 */
	public final PlayerPublicDetails getBanishingWizard ()
	{
		return banishingWizard;
	}
	
	/**
	 * @param w The wizard (or raiders) who is banishing them
	 */
	public final void setBanishingWizard (final PlayerPublicDetails w)
	{
		banishingWizard = w;
	}

	/**
	 * @return If false, the wizard still has at least 1 more city so they can try to cast Spell of Return.  If true, the wizard has no cities left and they are out of the game.
	 */
	public final boolean isDefeated ()
	{
		return defeated;
	}

	/**
	 * @param d If false, the wizard still has at least 1 more city so they can try to cast Spell of Return.  If true, the wizard has no cities left and they are out of the game.
	 */
	public final void setDefeated (final boolean d)
	{
		defeated = d;
	}

	/**
	 * @return The message that caused us to display this dialog
	 */
	public final WizardBanishedMessageImpl getWizardBanishedMessage ()
	{
		return wizardBanishedMessage;
	}

	/**
	 * @param msg The message that caused us to display this dialog
	 */
	public final void setWizardBanishedMessage (final WizardBanishedMessageImpl msg)
	{
		wizardBanishedMessage = msg;
	}
}