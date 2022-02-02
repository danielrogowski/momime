package momime.client.ui.dialogs;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.utils.SpellClientUtils;
import momime.common.MomException;
import momime.common.database.AnimationEx;
import momime.common.messages.KnownWizardDetails;
import momime.common.utils.KnownWizardUtils;

/**
 * Diplomacy screen for talking to other players (both human and AI)
 */
public final class DiplomacyUI extends MomClientDialogUI
{
	/** XML layout */
	private XmlLayoutContainerEx diplomacyLayout;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Client-side spell utils */
	private SpellClientUtils spellClientUtils;
	
	/** Which wizard we are talking to */
	private int talkingWizardID;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/diplomacy/background.png");
		final Image eyesLeft = getUtils ().doubleSize (getUtils ().loadImage ("/momime.client.graphics/ui/diplomacy/eyes-left-11.png"));
		final Image eyesRight = getUtils ().doubleSize (getUtils ().loadImage ("/momime.client.graphics/ui/diplomacy/eyes-right-11.png"));

		// Need this just to cut the corner off the wizard portraits
		final AnimationEx fadeAnim = getGraphicsDB ().findAnimation (OverlandEnchantmentsUI.MIRROR_ANIM, "DiplomacyUI");
		
		// Wizard portraits are stored exactly from the original LBXes (109x104) but stretched to compensate for original MoM using non-square 320x200 mode pixels (218x250).
		// Note the original MoM doesn't use the main wizard portraits (WIZARDS.LBX) here, it uses a frame of the talking animations (DIPLOMAC.LBX), which fades out
		// the edges of the pic so that it looks more like its in a mirror.  While it would have been easier to do that here, that would have meant no pic for overland
		// enchantments would be available for custom wizard portraits.  So this has to work just from the main wizard portrait.
		final KnownWizardDetails talkingWizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), getTalkingWizardID (), "DiplomacyUI");
		
		final BufferedImage unscaledPortrait;
		if (talkingWizardDetails.getCustomPhoto () != null)
			unscaledPortrait = ImageIO.read (new ByteArrayInputStream (talkingWizardDetails.getCustomPhoto ()));
		else if (talkingWizardDetails.getStandardPhotoID () != null)
			unscaledPortrait = getUtils ().loadImage (getClient ().getClientDB ().findWizard (talkingWizardDetails.getStandardPhotoID (), "DiplomacyUI").getPortraitImageFile ());
		else
			throw new MomException ("Player ID " + getTalkingWizardID () + " has neither a custom or standard photo");
		
		final Image unclippedPortrait = unscaledPortrait.getScaledInstance
			(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH);
		
		// Cut the square corners off the wizard's photo
		final BufferedImage portrait = getSpellClientUtils ().mergeImages (unclippedPortrait, getUtils ().loadImage (fadeAnim.getFrame ().get
			(fadeAnim.getFrame ().size () - 1).getImageFile ()), 0, -5*2);
		
		// Need this to know where to draw the wizard's photo
		final XmlLayoutComponent mirrorLocation = getDiplomacyLayout ().findComponent ("frmDiplomacyMirror");
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel (new XmlLayoutManager (getDiplomacyLayout ()))
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
				g.drawImage (portrait, mirrorLocation.getLeft () + 11*2, mirrorLocation.getTop () + 11*2, null);
			}
		};
		
		// Green/red eyes
		final JLabel eyesLeftLabel = getUtils ().createImage (eyesLeft);
		contentPane.add (eyesLeftLabel, "frmDiplomacyEyesLeft");
		
		final JLabel eyesRightLabel = getUtils ().createImage (eyesRight);
		contentPane.add (eyesRightLabel, "frmDiplomacyEyesRight");
		
		// Lock dialog size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
	}		

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getDialog ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getTitle ()));
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getDiplomacyLayout ()
	{
		return diplomacyLayout;
	}

	/**
	 * @param x XML layout
	 */
	public final void setDiplomacyLayout (final XmlLayoutContainerEx x)
	{
		diplomacyLayout = x;
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
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
	
	/**
	 * @return Client-side spell utils
	 */
	public final SpellClientUtils getSpellClientUtils ()
	{
		return spellClientUtils;
	}

	/**
	 * @param utils Client-side spell utils
	 */
	public final void setSpellClientUtils (final SpellClientUtils utils)
	{
		spellClientUtils = utils;
	}
	
	/**
	 * @return Which wizard we are talking to
	 */
	public final int getTalkingWizardID ()
	{
		return talkingWizardID;
	}

	/**
	 * @param w Which wizard we are talking to
	 */
	public final void setTalkingWizardID (final int w)
	{
		talkingWizardID = w;
	}
}