package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.audio.MomAudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.messages.process.UpdateWizardStateMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;
import momime.common.database.LanguageText;
import momime.common.database.WizardEx;
import momime.common.messages.KnownWizardDetails;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Animation when a wizard loses a battle at their fortress, showing the enemy wizard blasting them.
 */
public final class WizardBanishedUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (WizardBanishedUI.class);
	
	/** Number of animation frames */
	private final static int TICK_COUNT = 27;
	
	/** Number of seconds to display the animation over */
	private final static double DURATION = 7;

	/** XML layout */
	private XmlLayoutContainerEx wizardBanishedLayout;

	/** Large font */
	private Font largeFont;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;

	/** Music player */
	private MomAudioPlayer musicPlayer;

	/** Sound effects player */
	private MomAudioPlayer soundPlayer;
	
	/** The wizard who was banished */
	private PlayerPublicDetails banishedWizard;

	/** The wizard who was banished */
	private KnownWizardDetails banishedWizardDetails;
	
	/** The wizard (or raiders) who is banishing them; can be null */
	private PlayerPublicDetails banishingWizard;
	
	/** The wizard (or raiders) who is banishing them; can be null */
	private KnownWizardDetails banishingWizardDetails;
	
	/** If false, the wizard still has at least 1 more city so they can try to cast Spell of Return.  If true, the wizard has no cities left and they are out of the game. */
	private boolean defeated;

	/** Update wizard state message */
	private UpdateWizardStateMessageImpl updateWizardStateMessage;
	
	/** Multiplayer client */
	private MomClient client;

	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Graphics data stored about banished wizard */
	private WizardEx banishedWizardDef;
	
	/** Graphics data stored about banishing wizard; null if raiders */
	private WizardEx banishingWizardDef;
	
	/** Title */
	private JLabel titleLabel;

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
		// Find details about the 2 wizards involved
		// Having a pic and sound effect for the banished wizard is mandatory or we cannot draw anything sensible.
		// Having a pic for the banishing wizard is optional, as there's some mobs shown too so can just show them like raiders if nothing else.
		banishedWizardDef = getClient ().getClientDB ().findWizard (getBanishedWizardDetails ().getStandardPhotoID (), "WizardBanishedUI (A)");
		
		banishingWizardDef = null;
		if ((getBanishingWizardDetails () != null) && (getBanishingWizardDetails ().getStandardPhotoID () != null))
			banishingWizardDef = getClient ().getClientDB ().findWizard (getBanishingWizardDetails ().getStandardPhotoID (), "WizardBanishedUI (B)");
		
		// Raiders do have a standardPhotoID, but no images
		if ((banishingWizardDef != null) && (banishingWizardDef.getBanishingImageFile () == null))
			banishingWizardDef = null;
		
		final XmlLayoutComponent banishedWizardLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedStanding");
		final Image banishedWizardImage = getUtils ().loadImage (banishedWizardDef.getStandingImageFile ()).getScaledInstance
			(banishedWizardLayout.getWidth (), banishedWizardLayout.getHeight (), Image.SCALE_SMOOTH);

		final XmlLayoutComponent banishingWizardLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedBanisher");
		final Image banishingWizardImage = (banishingWizardDef == null) ? null : getUtils ().loadImage (banishingWizardDef.getBanishingImageFile ()).getScaledInstance
			(banishingWizardLayout.getWidth (), banishingWizardLayout.getHeight (), Image.SCALE_SMOOTH);

		final XmlLayoutComponent banishingWizardsHandLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedBanishersHand");
		final Image banishingWizardsHandImage = (banishingWizardDef == null) ? null : getUtils ().loadImage (banishingWizardDef.getBanishingHandImageFile ()).getScaledInstance
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
		final AnimationEx singleBlastAnim = getGraphicsDB ().findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_BANISHED_SINGLE_BLAST, "WizardBanishedUI (C)");

		final XmlLayoutComponent doubleBlastLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedDoubleBlast");
		final AnimationEx doubleBlastAnim = getGraphicsDB ().findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_BANISHED_DOUBLE_BLAST, "WizardBanishedUI (D)");

		final XmlLayoutComponent evaporatingLayout = getWizardBanishedLayout ().findComponent ("frmWizardBanishedEvaporating");
		final AnimationEx evaporatingAnim = getClient ().getClientDB ().findAnimation (banishedWizardDef.getEvaporatingAnimation (), "WizardBanishedUI (E)");
		
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
					// This somehow seems to get called twice in MiniCityViewUI, so protect against that
					if (!unblocked)
					{
						getClient ().finishCustomDurationMessage (getUpdateWizardStateMessage ());
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
								final Image singleBlastImage = getUtils ().loadImage (singleBlastAnim.getFrame ().get (tickNumber - 11).getImageFile ()).getScaledInstance
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
								final Image doubleBlastImage = getUtils ().loadImage (doubleBlastAnim.getFrame ().get (tickNumber - 15).getImageFile ()).getScaledInstance
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
								final Image evaporatingImage = getUtils ().loadImage (evaporatingAnim.getFrame ().get (tickNumber - 23).getImageFile ()).getScaledInstance
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
		
		// Start the animation
		timer = new Timer ((int) (1000.0 * DURATION / TICK_COUNT), (ev) ->
		{
			tickNumber++;
			contentPane.repaint ();
			
			if ((tickNumber == 15) && (banishedWizardDef != null) && (banishedWizardDef.getScreamSoundFile () != null))
				try
				{
					getSoundPlayer ().playAudioFile (banishedWizardDef.getScreamSoundFile ());
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			
			if (tickNumber >= TICK_COUNT)
				timer.stop ();
		});
		timer.start ();
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		final List<LanguageText> languageText;
		if (getBanishingWizardDetails () == null)
			languageText = isDefeated () ? getLanguages ().getWizardBanishedScreen ().getDefeatedByNobody () : getLanguages ().getWizardBanishedScreen ().getBanishedByNobody ();
		else if (getPlayerKnowledgeUtils ().isWizard (getBanishingWizardDetails ().getWizardID ()))
			languageText = isDefeated () ? getLanguages ().getWizardBanishedScreen ().getDefeatedByWizard () : getLanguages ().getWizardBanishedScreen ().getBanishedByWizard ();
		else
			languageText = isDefeated () ? getLanguages ().getWizardBanishedScreen ().getDefeatedByRaiders () : getLanguages ().getWizardBanishedScreen ().getBanishedByRaiders ();
		
		String title = getLanguageHolder ().findDescription (languageText).replaceAll
			("BANISHED_WIZARD", getWizardClientUtils ().getPlayerName (getBanishedWizard ()));
		
		if (getBanishingWizard () != null)
			title = title.replaceAll
				("BANISHING_WIZARD", getWizardClientUtils ().getPlayerName (getBanishingWizard ()));
		
		getDialog ().setTitle (title);
		titleLabel.setText (title);
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
	 * @return The wizard who was banished
	 */
	public final KnownWizardDetails getBanishedWizardDetails ()
	{
		return banishedWizardDetails;
	}

	/**
	 * @param w The wizard who was banished
	 */
	public final void setBanishedWizardDetails (final KnownWizardDetails w)
	{
		banishedWizardDetails = w;
	}
	
	/**
	 * @return The wizard (or raiders) who is banishing them; can be null
	 */
	public final PlayerPublicDetails getBanishingWizard ()
	{
		return banishingWizard;
	}
	
	/**
	 * @param w The wizard (or raiders) who is banishing them; can be null
	 */
	public final void setBanishingWizard (final PlayerPublicDetails w)
	{
		banishingWizard = w;
	}

	/**
	 * @return The wizard (or raiders) who is banishing them; can be null
	 */
	public final KnownWizardDetails getBanishingWizardDetails ()
	{
		return banishingWizardDetails;
	}

	/**
	 * @param w The wizard (or raiders) who is banishing them; can be null
	 */
	public final void setBanishingWizardDetails (final KnownWizardDetails w)
	{
		banishingWizardDetails = w;
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
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}
}