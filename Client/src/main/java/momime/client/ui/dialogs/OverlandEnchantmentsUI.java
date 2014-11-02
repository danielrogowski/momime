package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.messages.process.AddMaintainedSpellMessageImpl;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.frames.MagicSlidersUI;
import momime.common.MomException;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Animation of the swirly mirror when someone casts an overland enchantment
 */
public final class OverlandEnchantmentsUI extends MomClientDialogUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MessageBoxUI.class);
	
	/**
	 * AnimationID for the swirls in the mirror.  I had to tidy up some frames of this, some of the original frames have a black border (the area outside the circle),
	 * some grey, some blue.  Also had to add a 15th frame on the end which is just the solid circle - although it isn't needed to draw the animation itself, it gets used
	 * as a mask to chop the square corners off the wizard portrait so they don't stick out the side of the mirror.
	 */
	final static String MIRROR_ANIM = "OVERLAND_ENCHANTMENTS_MIRROR";
	
	/** Number of animation frames that we pause in between fading shiny-photo and then photo-spell pic */
	private final static int PAUSE_FRAMES = 12;
	
	/** XML layout */
	private XmlLayoutContainerEx overlandEnchantmentsLayout;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Large font */
	private Font largeFont;
	
	/** Music player */
	private AudioPlayer musicPlayer;

	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** The spell being drawn */
	private AddMaintainedSpellMessageImpl addSpellMessage;
	
	/** Text underneath */
	private JLabel spellText;
	
	/** Current frame number to display */
	private int animationFrame = 0;
	
	/** Animation timer */
	private Timer timer;
	
	/** The swirling animation to fade images in and out */
	private AnimationEx fadeAnim;
	
	/** Whether we've unblocked the message queue */
	private boolean unblocked;
	
	/**
	 * Sets up the dialog once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage shiny = getUtils ().loadImage ("/momime.client.graphics/ui/mirror/shiny.png");
		fadeAnim = getGraphicsDB ().findAnimation (MIRROR_ANIM, "OverlandEnchantmentsUI");
		
		// Get the player's colour and face
		final BufferedImage mirror = getPlayerColourImageGenerator ().getOverlandEnchantmentMirror (getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID ());
		
		final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID (), "OverlandEnchantmentsUI");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		
		// Wizard portraits are stored exactly from the original LBXes (109x104) but stretched to compensate for original MoM using non-square 320x200 mode pixels (218x250).
		// Note the original MoM doesn't use the main wizard portraits (WIZARDS.LBX) here, it uses a frame of the talking animations (DIPLOMAC.LBX), which fades out
		// the edges of the pic so that it looks more like its in a mirror.  While it would have been easier to do that here, that would have meant no pic for overland
		// enchantments would be available for custom wizard portraits.  So this has to work just from the main wizard portrait.
		final BufferedImage unscaledPortrait;
		if (pub.getCustomPhoto () != null)
			unscaledPortrait = ImageIO.read (new ByteArrayInputStream (pub.getCustomPhoto ()));
		else if (pub.getStandardPhotoID () != null)
			unscaledPortrait = getUtils ().loadImage (getGraphicsDB ().findWizard (pub.getStandardPhotoID (), "OverlandEnchantmentsUI").getPortraitFile ());
		else
			throw new MomException ("Player ID " + getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID () + " has neither a custom or standard photo");
		
		final Image unclippedPortrait = unscaledPortrait.getScaledInstance
			(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH);
		
		// Cut the square corners off the wizard's photo
		final BufferedImage portrait = mergeImages (unclippedPortrait, getUtils ().loadImage (fadeAnim.getFrame ().get
			(fadeAnim.getFrame ().size () - 1).getFrameImageFile ()), 0, -5*2);
		
		// Get the pic of the spell
		final momime.client.graphics.database.v0_9_5.Spell spellGfx = getGraphicsDB ().findSpell (getAddSpellMessage ().getMaintainedSpell ().getSpellID (), "OverlandEnchantmentsUI");
		final BufferedImage unscaledSpellPic = getUtils ().loadImage (spellGfx.getOverlandEnchantmentImageFile ());
		final Image spellPic = unscaledSpellPic.getScaledInstance (unscaledSpellPic.getWidth () * 2, unscaledSpellPic.getHeight () * 2, Image.SCALE_SMOOTH);

		// Initialize the dialog
		final OverlandEnchantmentsUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				try
				{
					getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				
					// Unblock the message that caused this
					// This somehow seems to get called twice in MiniCityViewUI, so protect against that
					if (!unblocked)
					{
						getClient ().finishCustomDurationMessage (getAddSpellMessage ());
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
				// Paint all the areas outside the mirror black - its way too complicated to try to custom shape this form
				super.paintComponent (g);
				
				// Fade from the shiny empty mirror to the wizard's photo
				try
				{
					if (animationFrame < fadeAnim.getFrame ().size ())
					{
						g.drawImage (shiny, 12*2, 12*2, shiny.getWidth () * 2, shiny.getHeight () * 2, null);
						final int useAnimationFrame = (animationFrame < fadeAnim.getFrame ().size ()) ? animationFrame : (fadeAnim.getFrame ().size () - 1);
						final BufferedImage fadeImage = getUtils ().loadImage (fadeAnim.getFrame ().get (useAnimationFrame).getFrameImageFile ());
						
						// Merge the animation image and the wizard's portrait
						final BufferedImage mergedImage = mergeImages (portrait, fadeImage, 0, 0);						
						g.drawImage (mergedImage, 11*2, 11*2, null);
					}

					// Just the wizard's photo
					else if (animationFrame < fadeAnim.getFrame ().size () + PAUSE_FRAMES)
						g.drawImage (portrait, 11*2, 11*2, null);
					
					// Fade from the wizard's photo to the spell pic
					else
					{
						g.drawImage (portrait, 11*2, 11*2, null);
						final int useAnimationFrame = animationFrame - fadeAnim.getFrame ().size () - PAUSE_FRAMES;

						final BufferedImage fadeImage = getUtils ().loadImage (fadeAnim.getFrame ().get (useAnimationFrame).getFrameImageFile ());
						
						// Merge the animation image and the spell pic
						final BufferedImage mergedImage = mergeImages (spellPic, fadeImage, -1*2, -3*2);						
						g.drawImage (mergedImage, 11*2, 11*2, null);
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
				
				// Always draw the mirror frame last, to cover up whatever has been drawn in the circle
				g.drawImage (mirror, 0, 0, mirror.getWidth () * 2, mirror.getHeight () * 2, null);
			}
		};

		// Set up layout
		contentPane.setBackground (Color.BLACK);
		contentPane.setLayout (new XmlLayoutManager (getOverlandEnchantmentsLayout ()));
		
		// Text label
		final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
		spellText = getUtils ().createLabel (new Color (Integer.parseInt (trans.getFlagColour (), 16)), getLargeFont ());
		contentPane.add (spellText, "frmOverlandEnchantmentsText");

		// Actually add the spell
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().add (getAddSpellMessage ().getMaintainedSpell ());
		getMagicSlidersUI ().spellsChanged ();
		
		// Any music to play?
		try
		{
			if (spellGfx.getSpellMusicFile () != null)
				getMusicPlayer ().playThenResume (spellGfx.getSpellMusicFile ());
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Start the animation.
		// There's 3 stages to the animation (fade from shiny-photo, pause, fade from photo-spell pic) but it would be messy to set up 3 different timers,
		// so instead the 1 timer and frame counter keeps ticking up the whole way through, and the paintComponent method knows which
		// stage of animation it needs to be drawing.
		timer = new Timer ((int) (1000 / fadeAnim.getAnimationSpeed ()), new ActionListener ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				animationFrame++;
				contentPane.repaint ();		// Technically we don't need to do this when we're paused looking at the wizard's photo
		
				if (animationFrame == fadeAnim.getFrame ().size () + PAUSE_FRAMES)
					languageChanged ();
				
				else if (animationFrame+1 >= (fadeAnim.getFrame ().size () * 2) + PAUSE_FRAMES)
					timer.stop ();
			}
		});
		timer.start ();
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		setCloseOnClick (true);

		log.trace ("Exiting init");
	}
	
	/**
	 * @param sourceImage Source image to start from
	 * @param fadeAnimFrame One frame from the fading animation
	 * @param xOffset How much to offset the sourceImage by
	 * @param yOffset How much to offset the sourceImage by
	 * @return Image which will draw only pixels from sourceImage where the matching pixels in fadeAnimFrame are transparent
	 */
	private final BufferedImage mergeImages (final Image sourceImage, final BufferedImage fadeAnimFrame, final int xOffset, final int yOffset)
	{
		final BufferedImage mergedImage = new BufferedImage (fadeAnimFrame.getWidth () * 2, fadeAnimFrame.getHeight () * 2, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = mergedImage.createGraphics ();
		try
		{
			g2.drawImage (sourceImage, xOffset, yOffset, null);
		}
		finally
		{
			g2.dispose ();
		}
		
		for (int x = 0; x < fadeAnimFrame.getWidth (); x++)
			for (int y = 0; y < fadeAnimFrame.getHeight (); y++)
				if (fadeAnimFrame.getRGB (x, y) != 0)
					for (int x2 = 0; x2 < 2; x2++)
						for (int y2 = 0; y2 < 2; y2++)
							mergedImage.setRGB ((x*2) + x2, (y*2) + y2, 0);
		
		return mergedImage;
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		// Spell name
		if (animationFrame >= fadeAnim.getFrame ().size () + PAUSE_FRAMES)
		{
			final Spell spellLang = getLanguage ().findSpell (getAddSpellMessage ().getMaintainedSpell ().getSpellID ());
			final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
			spellText.setText ((spellName != null) ? spellName : getAddSpellMessage ().getMaintainedSpell ().getSpellID ());			
		}
		
		// Us casting
		else if (getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID () == getClient ().getOurPlayerID ())
			spellText.setText (getLanguage ().findCategoryEntry ("SpellCasting", "OurOverlandEnchantment"));
		
		// Someone else casting
		else
		{
			final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID ());
			final String playerName = (player != null) ? player.getPlayerDescription ().getPlayerName () : null;
			spellText.setText (getLanguage ().findCategoryEntry ("SpellCasting", "EnemyOverlandEnchantment").replaceAll
				("PLAYER_NAME", (playerName != null) ? playerName : ("Player " + getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID ())));
		}
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getOverlandEnchantmentsLayout ()
	{
		return overlandEnchantmentsLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setOverlandEnchantmentsLayout (final XmlLayoutContainerEx layout)
	{
		overlandEnchantmentsLayout = layout;
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
	 * @return Player colour image generator
	 */
	public final PlayerColourImageGenerator getPlayerColourImageGenerator ()
	{
		return playerColourImageGenerator;
	}

	/**
	 * @param gen Player colour image generator
	 */
	public final void setPlayerColourImageGenerator (final PlayerColourImageGenerator gen)
	{
		playerColourImageGenerator = gen;
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
	 * @return The spell being drawn
	 */
	public final AddMaintainedSpellMessageImpl getAddSpellMessage ()
	{
		return addSpellMessage;
	}

	/**
	 * @param spl The spell being drawn
	 */
	public final void setAddSpellMessage (final AddMaintainedSpellMessageImpl spl)
	{
		addSpellMessage = spl;
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
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
	}
}