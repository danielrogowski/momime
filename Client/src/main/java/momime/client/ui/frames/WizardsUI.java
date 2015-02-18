package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.PickGfx;
import momime.client.language.database.PickLang;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.utils.TextUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.utils.PlayerKnowledgeUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Wizards screen, for checking everybodys' picks and diplomacy
 */
public final class WizardsUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (WizardsUI.class);

	/** Special inset for books */
	private final static int NO_INSET = 0;
	
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
	
	/** Medium font */
	private Font mediumFont;

	/** Large font */
	private Font largeFont;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Help text scroll */
	private HelpUI helpUI;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** List of gem images */
	private List<JLabel> gems;
	
	/** List of wizard portrait images */
	private List<JLabel> portraits;

	/** Content pane */
	private JPanel contentPane;
	
	/** Close action */
	private Action closeAction;
	
	/** Shelf displaying chosen books */
	private JPanel bookshelf;
	
	/** Images added to draw the books on the shelf */
	private final List<JLabel> bookImages = new ArrayList<JLabel> ();
	
	/** Area listing retorts */
	private JTextArea retorts;
	
	/** Wizard's name */
	private JLabel playerName;
	
	/** Wizard being viewed */
	private PlayerPublicDetails selectedWizard;
	
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
		contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getWizardsLayout ()));
		
		contentPane.add (getUtils ().createImageButton (closeAction,
			MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmWizardsClose");
		
		playerName = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (playerName, "frmWizardsName");
		
		bookshelf = new JPanel (new GridBagLayout ());
		bookshelf.setOpaque (false);
		
		// Force the books to sit on the bottom of the shelf
		bookshelf.add (Box.createRigidArea (new Dimension (0, getWizardsLayout ().findComponent ("frmWizardsBookshelf").getHeight ())));

		contentPane.add (bookshelf, "frmWizardsBookshelf");
		
		retorts = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (retorts, "frmWizardsRetorts");
		
		// Right clicking on specific retorts in the text area brings up help text about them
		retorts.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isRightMouseButton (ev))
				{
					try
					{
						final String pickID = updateRetortsFromPicks (retorts.viewToModel (ev.getPoint ()));
						if (pickID != null)
							getHelpUI ().showPickID (pickID);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			}
		});				
		
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
			
			// Create actions once out here too
			final int wizardNo = n - 1;
			final MouseAdapter displayWizardHandler = new MouseAdapter ()
			{
				@Override
				public final void mouseClicked (final MouseEvent ev)
				{
					try
					{
						selectedWizard = getClient ().getPlayers ().get (wizardNo);
						
						updateBookshelfFromPicks ();
						updateRetortsFromPicks (-1);
						updateWizardName ();
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			};
			
			portrait.addMouseListener (displayWizardHandler);
			gem.addMouseListener (displayWizardHandler);
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
	 * When the selected picks change, update the books on the bookshelf
	 * @throws IOException If there is a problem loading any of the book images
	 */
	private final void updateBookshelfFromPicks () throws IOException
	{
		log.trace ("Entering updateBookshelfFromPicks: " + selectedWizard.getPlayerDescription ().getPlayerID ());

		// Remove all the old books
		for (final JLabel oldBook : bookImages)
			bookshelf.remove (oldBook);
		
		bookImages.clear ();
		
		// Generate new images
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) selectedWizard.getPersistentPlayerPublicKnowledge ();
		int mergedBookshelfGridx = 0;
		
		for (final PlayerPick pick : pub.getPick ())
		{
			// Pick must exist in the graphics XML file, but may not have any image(s)
			final PickGfx pickGfx = getGraphicsDB ().findPick (pick.getPickID (), "WizardsUI.updateBookshelfFromPicks");
			if (pickGfx.getBookImage ().size () > 0)
				for (int n = 0; n < pick.getQuantity (); n++)
				{
					// Choose random image for the pick
					final BufferedImage bookImage = getUtils ().loadImage (pickGfx.chooseRandomBookImageFilename ());
					
					// Add on merged bookshelf
					mergedBookshelfGridx++;
					final JLabel mergedBookshelfImg = getUtils ().createImage (bookImage);
					bookshelf.add (mergedBookshelfImg, getUtils ().createConstraintsNoFill (mergedBookshelfGridx, 0, 1, 1, NO_INSET, GridBagConstraintsNoFill.SOUTH));
					bookImages.add (mergedBookshelfImg);
				}
		}
		
		// Redrawing only the bookshelf isn't enough, because the new books might be smaller than before so only the smaller so
		// bookshelf.validate only redraws the new smaller area and leaves bits of the old books showing
		contentPane.validate ();
		contentPane.repaint ();

		log.trace ("Exiting updateBookshelfFromPicks");
	}
	
	/**
	 * When the selected picks (or language) change, update the retort descriptions
	 * 
	 * @param charIndex Index into the generated text that we want to locate and get in the return param; -1 if we don't care about the return param
	 * @return PickID of the pick at the requested charIndex; -1 if the charIndex is outside of the text or doesn't represent a pick (i.e. is one of the commas)
	 * @throws RecordNotFoundException If one of the picks we have isn't in the graphics XML file
	 */
	private final String updateRetortsFromPicks (final int charIndex) throws RecordNotFoundException
	{
		log.trace ("Entering updateRetortsFromPicks: " + selectedWizard.getPlayerDescription ().getPlayerID ());
		
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) selectedWizard.getPersistentPlayerPublicKnowledge ();
		final StringBuffer desc = new StringBuffer ();
		String result = null;
		
		for (final PlayerPick pick : pub.getPick ())
		{
			// Pick must exist in the graphics XML file, but may not have any image(s)
			if (getGraphicsDB ().findPick (pick.getPickID (), "WizardsUI.updateRetortsFromPicks").getBookImage ().size () == 0)
			{
				if (desc.length () > 0)
					desc.append (", ");
				
				if (pick.getQuantity () > 1)
					desc.append (pick.getQuantity () + "x");
				
				final PickLang pickDesc = getLanguage ().findPick (pick.getPickID ());
				final String thisPickText;
				if (pickDesc == null)
					thisPickText = pick.getPickID ();
				else
					thisPickText = pickDesc.getPickDescriptionSingular ();

				// Does the required index fall within the text for this pick?
				if ((charIndex >= desc.length ()) && (charIndex < desc.length () + thisPickText.length ()))
					result = pick.getPickID ();
				
				// Now add it
				desc.append (thisPickText);
			}
		}
		retorts.setText (getTextUtils ().replaceFinalCommaByAnd (desc.toString ()));

		log.trace ("Exiting updateRetortsFromPicks = " + result);
		return result;
	}	
	
	/**
	 * Updates the name of the currently selected wizard
	 */
	private final void updateWizardName ()
	{
		log.trace ("Entering updateWizardName");
		
		if (selectedWizard == null)
			playerName.setText (null);
		else
			playerName.setText (getWizardClientUtils ().getPlayerName (selectedWizard));
		
		log.trace ("Exiting updateWizardName");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmWizards", "Title"));
		closeAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmWizards", "Close"));
		
		updateWizardName ();
		
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

	/**
	 * @return Medium font
	 */
	public final Font getMediumFont ()
	{
		return mediumFont;
	}

	/**
	 * @param font Medium font
	 */
	public final void setMediumFont (final Font font)
	{
		mediumFont = font;
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
	 * @return Help text scroll
	 */
	public final HelpUI getHelpUI ()
	{
		return helpUI;
	}

	/**
	 * @param ui Help text scroll
	 */
	public final void setHelpUI (final HelpUI ui)
	{
		helpUI = ui;
	}

	/**
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
	}
}