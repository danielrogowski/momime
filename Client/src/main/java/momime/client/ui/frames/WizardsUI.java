package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.MomException;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.PlayerKnowledgeUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Wizards screen, for checking everybodys' picks and diplomacy
 */
public final class WizardsUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (WizardsUI.class);

	/** XML layout */
	private XmlLayoutContainerEx wizardsLayout;
	
	/** Multiplayer client */
	private MomClient client;

	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Small font */
	private Font smallFont;
	
	/** List of gem images */
	private List<JLabel> gems;
	
	/** List of wizard portrait images */
	private List<JLabel> portraits;

	/** Close action */
	private Action closeAction;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/wizards.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");

		// Actions
		closeAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().setVisible (false);
			}
		};
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getWizardsLayout ()));
		
		contentPane.add (getUtils ().createImageButton (closeAction,
			MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmWizardsClose");
		
		// Create all the gems and portait labels, initially with no image, because the gems get regenerated with the
		// wizard's colour, and the portraits with the wizard's picture
		gems = new ArrayList<JLabel> ();
		portraits = new ArrayList<JLabel> ();
		
		for (int n = 1; n <= 14; n++)
		{
			// Must add portrait first, so it appears in front of the gem
			final JLabel portrait = new JLabel ();
			contentPane.add (portrait, "frmWizardsPortrait" + n);
			portraits.add (portrait);

			final JLabel gem = new JLabel ();
			contentPane.add (gem, "frmWizardsGem" + n);
			gems.add (gem);
		}
		
		updateWizards ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Updates all the gem images, wizard portraits, and how many gems should even be visible
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void updateWizards () throws IOException
	{
		log.trace ("Entering updateWizards");
		
		if (gems != null)
			for (int n = 0; n < 14; n++)
			{
				final JLabel gemLabel = gems.get (n);
				final JLabel portraitLabel = portraits.get (n);
				
				final PlayerPublicDetails player = (n < getClient ().getPlayers ().size ()) ? getClient ().getPlayers ().get (n) : null;
				final MomPersistentPlayerPublicKnowledge pub = (player == null) ? null : (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
				final boolean isWizard = (pub == null) ? false : PlayerKnowledgeUtils.isWizard (pub.getWizardID ());
				
				gemLabel.setVisible (isWizard);
				portraitLabel.setVisible (isWizard);
				
				if (isWizard)
				{
					gemLabel.setIcon (new ImageIcon (getPlayerColourImageGenerator ().getWizardGemImage (player.getPlayerDescription ().getPlayerID ())));

					// Find the wizard's photo
					final BufferedImage unscaledPortrait;
					if (pub.getCustomPhoto () != null)
						unscaledPortrait = ImageIO.read (new ByteArrayInputStream (pub.getCustomPhoto ()));
					else if (pub.getStandardPhotoID () != null)
						unscaledPortrait = getUtils ().loadImage (getGraphicsDB ().findWizard (pub.getStandardPhotoID (), "WizardsUI").getPortraitFile ());
					else
						throw new MomException ("Player ID " + player.getPlayerDescription ().getPlayerID () + " has neither a custom or standard photo");

					final Image scaledPortrait = unscaledPortrait.getScaledInstance (54, 63, Image.SCALE_SMOOTH);
					portraitLabel.setIcon (new ImageIcon (scaledPortrait));
				}
			}
		
		log.trace ("Exiting updateWizards");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		closeAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmWizards", "Close"));
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getWizardsLayout ()
	{
		return wizardsLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setWizardsLayout (final XmlLayoutContainerEx layout)
	{
		wizardsLayout = layout;
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
	 * @return Small font
	 */
	public final Font getSmallFont ()
	{
		return smallFont;
	}

	/**
	 * @param font Small font
	 */
	public final void setSmallFont (final Font font)
	{
		smallFont = font;
	}
}