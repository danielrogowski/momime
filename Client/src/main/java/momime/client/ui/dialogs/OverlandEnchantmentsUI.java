package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.messages.process.AddOrUpdateMaintainedSpellMessageImpl;
import momime.client.messages.process.ShowSpellAnimationMessageImpl;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.WizardsUI;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.database.AnimationEx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.MemoryMaintainedSpellUtils;

/**
 * Animation of the swirly mirror when someone casts an overland enchantment
 */
public final class OverlandEnchantmentsUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (OverlandEnchantmentsUI.class);
	
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
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** The spell being drawn, for overland enchantments */
	private AddOrUpdateMaintainedSpellMessageImpl addSpellMessage;

	/** The spell being drawn, for global attack spells */
	private ShowSpellAnimationMessageImpl showSpellAnimationMessage;
	
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
		// Load images
		final BufferedImage shiny = getUtils ().loadImage ("/momime.client.graphics/ui/mirror/shiny.png");
		fadeAnim = getGraphicsDB ().findAnimation (MIRROR_ANIM, "OverlandEnchantmentsUI");
		
		// Get the player's colour and face
		final int castingPlayerID = (getAddSpellMessage () != null) ? getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID () :
			getShowSpellAnimationMessage ().getCastingPlayerID ();
		
		final BufferedImage mirror = getPlayerColourImageGenerator ().getModifiedImage (GraphicsDatabaseConstants.OVERLAND_ENCHANTMENTS_MIRROR,
			true, null, null, null, castingPlayerID, null);
		
		final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), castingPlayerID, "OverlandEnchantmentsUI");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		
		// Wizard portraits are stored exactly from the original LBXes (109x104) but stretched to compensate for original MoM using non-square 320x200 mode pixels (218x250).
		// Note the original MoM doesn't use the main wizard portraits (WIZARDS.LBX) here, it uses a frame of the talking animations (DIPLOMAC.LBX), which fades out
		// the edges of the pic so that it looks more like its in a mirror.  While it would have been easier to do that here, that would have meant no pic for overland
		// enchantments would be available for custom wizard portraits.  So this has to work just from the main wizard portrait.
		final BufferedImage unscaledPortrait;
		if (pub.getCustomPhoto () != null)
			unscaledPortrait = ImageIO.read (new ByteArrayInputStream (pub.getCustomPhoto ()));
		else if (pub.getStandardPhotoID () != null)
			unscaledPortrait = getUtils ().loadImage (getClient ().getClientDB ().findWizard (pub.getStandardPhotoID (), "OverlandEnchantmentsUI").getPortraitImageFile ());
		else
			throw new MomException ("Player ID " + castingPlayerID + " has neither a custom or standard photo");
		
		final Image unclippedPortrait = unscaledPortrait.getScaledInstance
			(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH);
		
		// Cut the square corners off the wizard's photo
		final BufferedImage portrait = mergeImages (unclippedPortrait, getUtils ().loadImage (fadeAnim.getFrame ().get
			(fadeAnim.getFrame ().size () - 1).getImageFile ()), 0, -5*2);
		
		// Get the pic of the spell
		final String spellID = (getAddSpellMessage () != null) ? getAddSpellMessage ().getMaintainedSpell ().getSpellID () :
			getShowSpellAnimationMessage ().getSpellID ();
		
		final Spell spell = getClient ().getClientDB ().findSpell (spellID, "OverlandEnchantmentsUI");
		final BufferedImage unscaledSpellPic = getUtils ().loadImage (spell.getOverlandEnchantmentImageFile ());
		final Image spellPic = unscaledSpellPic.getScaledInstance (unscaledSpellPic.getWidth () * 2, unscaledSpellPic.getHeight () * 2, Image.SCALE_SMOOTH);

		// Initialize the dialog
		final OverlandEnchantmentsUI ui = this;
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
						if (getAddSpellMessage () != null)
							getClient ().finishCustomDurationMessage (getAddSpellMessage ());
						
						else if (getShowSpellAnimationMessage () != null)
							getClient ().finishCustomDurationMessage (getShowSpellAnimationMessage ());
						
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
				// Paint all the areas outside the mirror black - its way too complicated to try to custom shape this form
				super.paintComponent (g);
				
				// Fade from the shiny empty mirror to the wizard's photo
				try
				{
					if (animationFrame < fadeAnim.getFrame ().size ())
					{
						g.drawImage (shiny, 12*2, 12*2, shiny.getWidth () * 2, shiny.getHeight () * 2, null);
						final int useAnimationFrame = (animationFrame < fadeAnim.getFrame ().size ()) ? animationFrame : (fadeAnim.getFrame ().size () - 1);
						final BufferedImage fadeImage = getUtils ().loadImage (fadeAnim.getFrame ().get (useAnimationFrame).getImageFile ());
						
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

						final BufferedImage fadeImage = getUtils ().loadImage (fadeAnim.getFrame ().get (useAnimationFrame).getImageFile ());
						
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

		// Updates are only relevant if actually adding a spell; if animation message then its just the animation and nothing else
		if (getAddSpellMessage () != null)
		{
			// Add or update the spell
			final MemoryMaintainedSpell existingSpell = getMemoryMaintainedSpellUtils ().findSpellURN
				(getAddSpellMessage ().getMaintainedSpell ().getSpellURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ());
			
			if (existingSpell == null)
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().add (getAddSpellMessage ().getMaintainedSpell ());
			else
				existingSpell.setCastingPlayerID (castingPlayerID);
						
			// Update other screens
			getMagicSlidersUI ().spellsChanged ();
			
			// Update fame if cast Just Cause
			if (spellID.equals (CommonDatabaseConstants.SPELL_ID_JUST_CAUSE))
				getWizardsUI ().wizardUpdated (player);
		}
		
		// If spell binding, use special music for it
		Spell musicSpell = spell;
		if ((getAddSpellMessage () != null) && (!getAddSpellMessage ().isNewlyCast ()))
			for (final Spell spellBinding : getClient ().getClientDB ().getSpell ())
				if ((spellBinding.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS) &&
					(spellBinding.getOverlandMaxDamage () == null) && (spellBinding.getCombatMaxDamage () == null))
					
					musicSpell = spellBinding;
		
		// Any music to play?
		try
		{
			if (musicSpell.getSpellMusicFile () != null)
				getMusicPlayer ().playThenResume (musicSpell.getSpellMusicFile ());
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Start the animation.
		// There's 3 stages to the animation (fade from shiny-photo, pause, fade from photo-spell pic) but it would be messy to set up 3 different timers,
		// so instead the 1 timer and frame counter keeps ticking up the whole way through, and the paintComponent method knows which
		// stage of animation it needs to be drawing.
		timer = new Timer ((int) (1000 / fadeAnim.getAnimationSpeed ()), (ev) ->
		{
			animationFrame++;
			contentPane.repaint ();		// Technically we don't need to do this when we're paused looking at the wizard's photo
	
			if (animationFrame == fadeAnim.getFrame ().size () + PAUSE_FRAMES)
				languageChanged ();
			
			else if (animationFrame+1 >= (fadeAnim.getFrame ().size () * 2) + PAUSE_FRAMES)
				timer.stop ();
		});
		timer.start ();
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		setCloseOnClick (true);
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
		// Spell name
		if (animationFrame >= fadeAnim.getFrame ().size () + PAUSE_FRAMES)
		{
			try
			{
				final String spellID = (getAddSpellMessage () != null) ? getAddSpellMessage ().getMaintainedSpell ().getSpellID () :
					getShowSpellAnimationMessage ().getSpellID ();
				
				spellText.setText (getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findSpell (spellID, "OverlandEnchantmentsUI").getSpellName ()));
			}
			catch (final RecordNotFoundException e)
			{
				log.error (e, e);
			}
		}
		else
		{
			final int castingPlayerID = (getAddSpellMessage () != null) ? getAddSpellMessage ().getMaintainedSpell ().getCastingPlayerID () :
				getShowSpellAnimationMessage ().getCastingPlayerID ();
		
			// Us binding
			if ((getAddSpellMessage () != null) && (!getAddSpellMessage ().isNewlyCast ()) && (castingPlayerID == getClient ().getOurPlayerID ()))
				spellText.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getBindingOurOverlandEnchantment ()));
			
			// Us casting
			else if (castingPlayerID == getClient ().getOurPlayerID ())
				spellText.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getOurOverlandEnchantment ()));
			
			// Someone else binding or casting
			else
			{
				final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), castingPlayerID);
				final String playerName = (player != null) ? getWizardClientUtils ().getPlayerName (player) : null;
				
				if ((getAddSpellMessage () != null) && (!getAddSpellMessage ().isNewlyCast ()))
					spellText.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getBindingEnemyOverlandEnchantment ()).replaceAll
						("PLAYER_NAME", (playerName != null) ? playerName : ("Player " + castingPlayerID)));
				else
					spellText.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getEnemyOverlandEnchantment ()).replaceAll
						("PLAYER_NAME", (playerName != null) ? playerName : ("Player " + castingPlayerID)));
			}
		}
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
	 * @return The spell being drawn, for overland enchantments
	 */
	public final AddOrUpdateMaintainedSpellMessageImpl getAddSpellMessage ()
	{
		return addSpellMessage;
	}

	/**
	 * @param spl The spell being drawn, for overland enchantments
	 */
	public final void setAddSpellMessage (final AddOrUpdateMaintainedSpellMessageImpl spl)
	{
		addSpellMessage = spl;
	}

	/**
	 * @return The spell being drawn, for global attack spells
	 */
	public final ShowSpellAnimationMessageImpl getShowSpellAnimationMessage ()
	{
		return showSpellAnimationMessage;
	}

	/**
	 * @param m The spell being drawn, for global attack spells
	 */
	public final void setShowSpellAnimationMessage (final ShowSpellAnimationMessageImpl m)
	{
		showSpellAnimationMessage = m;
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}

	/**
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
	}
}