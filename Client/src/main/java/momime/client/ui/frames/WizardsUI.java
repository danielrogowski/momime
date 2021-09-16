package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.utils.PlayerPickClientUtils;
import momime.client.utils.TextUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.LanguageText;
import momime.common.database.Pick;
import momime.common.database.ProductionTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.OverlandCastingInfo;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.TargetSpellResult;

/**
 * Wizards screen, for checking everybodys' picks and diplomacy
 */
public final class WizardsUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (WizardsUI.class);

	/** Special inset for books */
	private final static int NO_INSET = 0;
	
	/** How much to scale down the wizard portraits to fit in the gems, as a percent */
	private final static int WIZARD_PORTRAIT_SCALE = 30;
	
	/** Size of wizard portraits to fit in the gems */
	private final static Dimension SCALED_WIZARD_PORTRAIT_SIZE = new Dimension
		((GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width * WIZARD_PORTRAIT_SCALE) / 100,
		 (GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height* WIZARD_PORTRAIT_SCALE) / 100);
	
	/** XML layout */
	private XmlLayoutContainerEx wizardsLayout;
	
	/** Multiplayer client */
	private MomClient client;

	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
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
	
	/** Client-side pick utils */
	private PlayerPickClientUtils playerPickClientUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;

	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** List of gem buttons for each wizard */
	private final List<JButton> wizardButtons = new ArrayList<JButton> ();
	
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
	
	/** Wizard's fame */
	private JLabel fameLabel;
	
	/** Wizard being viewed */
	private PlayerPublicDetails selectedWizard;
	
	/** Portrait of wizard being viewed */
	private JLabel wizardPortrait;
	
	/** Wizards title */
	private JLabel wizardsTitle;
	
	/** List of edit boxes to show spell currently being cast */
	private final List<JLabel> currentlyCasting = new ArrayList<JLabel> ();
	
	/** List of edit boxes to show how much MP has been put into the spells currently being cast */
	private final List<JLabel> currentlyCastingMana = new ArrayList<JLabel> ();
	
	/** List of labels to show spell currently being cast */
	private final List<JLabel> currentlyCastingLabels = new ArrayList<JLabel> ();

	/** List of labels to show how much MP has been put into the spells currently being cast */
	private final List<JLabel> currentlyCastingManaLabels = new ArrayList<JLabel> ();
	
	/** Info about what wizards are casting, gleaned from Detect Magic spell, keyed by playerID */
	private final Map<Integer, OverlandCastingInfo> overlandCastingInfo = new HashMap<Integer, OverlandCastingInfo> ();
	
	/** Which spell is currently being targeted using this screen */
	private Spell targetingSpell;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/wizards.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");
		final BufferedImage detectMagicTextBox = getUtils ().loadImage ("/momime.client.graphics/ui/editBoxes/detectMagicTextBox.png");

		// Actions
		closeAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
		// Initialize the content pane
		contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getWizardsLayout ()));
		
		wizardsTitle = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (wizardsTitle, "frmWizardsTitle");
		
		contentPane.add (getUtils ().createImageButton (closeAction,
			MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmWizardsClose");
		
		playerName = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (playerName, "frmWizardsName");
		
		fameLabel = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (fameLabel, "frmWizardsFame");
		
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
						final String pickID = updateRetortsFromPicks (retorts.viewToModel2D (ev.getPoint ()));
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
		
		// Portrait
		wizardPortrait = new JLabel ();
		contentPane.add (wizardPortrait, "frmWizardsPortrait");
		wizardPortrait.setVisible (false);
		
		// Boxes where we show the spell each wizard is casting
		for (int n = 1; n <= 14; n++)
		{
			final JLabel label = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
			contentPane.add (label, "frmWizardsCastingLabel" + n);
			currentlyCastingLabels.add (label);

			final JLabel box = new JLabel (new ImageIcon (detectMagicTextBox));
			contentPane.add (box, "frmWizardsCasting" + n);
			currentlyCasting.add (box);

			final JLabel manaLabel = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
			contentPane.add (manaLabel, "frmWizardsCastingManaLabel" + n);
			currentlyCastingManaLabels.add (manaLabel);

			final JLabel manaBox = new JLabel (new ImageIcon (detectMagicTextBox));
			contentPane.add (manaBox, "frmWizardsCastingMana" + n);
			currentlyCastingMana.add (manaBox);
		}
		
		updateWizards (true);
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
	}
	
	/**
	 * Updates all the gem images, wizard portraits, and how many gems should even be visible
	 * 
	 * @param calledFromInit Whether this is being called from the init method, in which case we know languageChanged will be called after
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void updateWizards (final boolean calledFromInit) throws IOException
	{
		if (contentPane != null)
		{
			// Remove the old buttons
			for (final JButton button : wizardButtons)
				contentPane.remove (button);
			
			wizardButtons.clear ();
			
			// Do we have detect magic cast or are we targeting spell blast?
			final boolean spellBlast = (getTargetingSpell () != null) && (getTargetingSpell ().getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_BLAST));
			
			final boolean detectMagic = (spellBlast) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPlayerID (), CommonDatabaseConstants.SPELL_ID_DETECT_MAGIC, null, null, null, null) != null);
			
			// Create new buttons
			int n = 0;
			for (final PlayerPublicDetails player : getClient ().getPlayers ())
			{
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
				if (PlayerKnowledgeUtils.isWizard (pub.getWizardID ()))
				{
					final Action wizardAction = new LoggingAction ((ev) ->
					{
						selectedWizard = player;						
						updateBookshelfFromPicks ();
						updateRetortsFromPicks (-1);
						updateWizard ();
						
						if (getTargetingSpell () != null)
						{
							// Is this wizard a valid target?
							final OverlandCastingInfo targetOverlandCastingInfo = overlandCastingInfo.get (player.getPlayerDescription ().getPlayerID ());
							
							final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isWizardValidTargetForSpell (getTargetingSpell (),
								getClient ().getOurPlayerID (), getClient ().getOurPersistentPlayerPrivateKnowledge (),
								player, targetOverlandCastingInfo);
							
							if (validTarget == TargetSpellResult.VALID_TARGET)
							{
								getOverlandMapUI ().targetOverlandPlayerID (player.getPlayerDescription ().getPlayerID ());
								setTargetingSpell (null);
								updateWizards (false);
							}
							else
							{
								String text = getLanguageHolder ().findDescription (getLanguages ().getSpellTargeting ().getWizardLanguageText (validTarget)).replaceAll
									("SPELL_NAME", getLanguageHolder ().findDescription (getTargetingSpell ().getSpellName ())).replaceAll
									("PLAYER_NAME", getWizardClientUtils ().getPlayerName (player));
								
								if ((targetOverlandCastingInfo != null) && (targetOverlandCastingInfo.getSpellID () != null))
									text = text.replaceAll ("BLASTED_NAME", getLanguageHolder ().findDescription
										(getClient ().getClientDB ().findSpell (targetOverlandCastingInfo.getSpellID (), "WizardsUI (T)").getSpellName ()));
								
								final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
								msg.setLanguageTitle (getLanguages ().getSpellTargeting ().getTitle ());
								msg.setText (text);
								msg.setVisible (true);												
							}
						}
					});
					
					final String gemNormalImageName = (pub.getWizardState () == WizardState.ACTIVE) ? "/momime.client.graphics/ui/backgrounds/gem.png" :
						"/momime.client.graphics/ui/backgrounds/gemCracked.png";
					final String gemPressedImageName = "/momime.client.graphics/ui/backgrounds/gemPressed.png";

					BufferedImage wizardNormalImage = getPlayerColourImageGenerator ().getModifiedImage
						(gemNormalImageName, true, null, null, null, player.getPlayerDescription ().getPlayerID (), null);
					BufferedImage wizardPressedImage = getPlayerColourImageGenerator ().getModifiedImage
						(gemPressedImageName, true, null, null, null, player.getPlayerDescription ().getPlayerID (), null);
					
					// Find the wizard's photo
					if (pub.getWizardState () == WizardState.ACTIVE)
					{
						final BufferedImage unscaledPortrait;
						if (pub.getCustomPhoto () != null)
							unscaledPortrait = ImageIO.read (new ByteArrayInputStream (pub.getCustomPhoto ()));
						else if (pub.getStandardPhotoID () != null)
							unscaledPortrait = getUtils ().loadImage (getClient ().getClientDB ().findWizard (pub.getStandardPhotoID (), "WizardsUI").getPortraitImageFile ());
						else
							throw new MomException ("Player ID " + player.getPlayerDescription ().getPlayerID () + " has neither a custom or standard photo");
		
						final Image scaledPortrait = unscaledPortrait.getScaledInstance
							(SCALED_WIZARD_PORTRAIT_SIZE.width, SCALED_WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH);
						
						// Add the wizard portrait onto the gems
						final BufferedImage mergedNormalImage = new BufferedImage
							(wizardNormalImage.getWidth (), wizardNormalImage.getHeight (), BufferedImage.TYPE_INT_ARGB);
						final Graphics2D g = mergedNormalImage.createGraphics ();
						try
						{
							g.drawImage (wizardNormalImage, 0, 0, null);
							g.drawImage (scaledPortrait, (wizardNormalImage.getWidth () - SCALED_WIZARD_PORTRAIT_SIZE.width) / 2,
								(wizardNormalImage.getHeight () - SCALED_WIZARD_PORTRAIT_SIZE.height) / 2, null);
						}
						finally
						{
							g.dispose ();
						}
						
						final BufferedImage mergedPressedImage = new BufferedImage
							(wizardNormalImage.getWidth (), wizardNormalImage.getHeight (), BufferedImage.TYPE_INT_ARGB);
						final Graphics2D g2 = mergedPressedImage.createGraphics ();
						try
						{
							g2.drawImage (wizardPressedImage, 0, 0, null);
							g2.drawImage (scaledPortrait, ((wizardPressedImage.getWidth () - SCALED_WIZARD_PORTRAIT_SIZE.width) / 2) + 1,
								((wizardPressedImage.getHeight () - SCALED_WIZARD_PORTRAIT_SIZE.height) / 2) + 1, null);
						}
						finally
						{
							g2.dispose ();
						}
						
						wizardNormalImage = mergedNormalImage;
						wizardPressedImage = mergedPressedImage;
					}
					
					// Show what the wizard is casting?
					if ((detectMagic) && (pub.getWizardState () == WizardState.ACTIVE))
					{
						currentlyCasting.get (n).setVisible (true);
						currentlyCastingLabels.get (n).setVisible (true);
						
						// Name of spell is set in languageChanged ()
					}
					else
					{
						currentlyCasting.get (n).setVisible (false);
						currentlyCastingLabels.get (n).setVisible (false);
					}
					
					if ((spellBlast) && (pub.getWizardState () == WizardState.ACTIVE))
					{
						currentlyCastingMana.get (n).setVisible (true);
						currentlyCastingManaLabels.get (n).setVisible (true);
						
						// Mana value is set in languageChanged (), because of the MP suffix
					}
					else
					{
						currentlyCastingMana.get (n).setVisible (false);
						currentlyCastingManaLabels.get (n).setVisible (false);
					}
					
					// Finally create the button
					n++;
					final JButton wizardButton = getUtils ().createImageButton (wizardAction, null, null, null, wizardNormalImage, wizardPressedImage, wizardNormalImage);
					wizardButton.setEnabled (pub.getWizardState () == WizardState.ACTIVE);
					
					contentPane.add (wizardButton, "frmWizardsGem" + n);
					wizardButtons.add (wizardButton);
				}
			}
			
			// Hide controls when there's less than 14 wizards
			while (n < 14)
			{
				currentlyCasting.get (n).setVisible (false);
				currentlyCastingLabels.get (n).setVisible (false);
				currentlyCastingMana.get (n).setVisible (false);
				currentlyCastingManaLabels.get (n).setVisible (false);
				n++;
			}
			
			// Need to show names of spells?
			if ((detectMagic) && (!calledFromInit))
				languageChanged ();
			
			contentPane.validate ();
			contentPane.repaint ();
		}
	}
	
	/**
	 * When the selected picks change, update the books on the bookshelf
	 * @throws IOException If there is a problem loading any of the book images
	 */
	private final void updateBookshelfFromPicks () throws IOException
	{
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
			final Pick pickDef = getClient ().getClientDB ().findPick (pick.getPickID (), "WizardsUI.updateBookshelfFromPicks");
			if (pickDef.getBookImageFile ().size () > 0)
				for (int n = 0; n < pick.getQuantity (); n++)
				{
					// Choose random image for the pick
					final BufferedImage bookImage = getUtils ().loadImage (getPlayerPickClientUtils ().chooseRandomBookImageFilename (pickDef));
					
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
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) selectedWizard.getPersistentPlayerPublicKnowledge ();
		final StringBuffer desc = new StringBuffer ();
		String result = null;
		
		for (final PlayerPick pick : pub.getPick ())
		{
			// Pick must exist in the graphics XML file, but may not have any image(s)
			if (getClient ().getClientDB ().findPick (pick.getPickID (), "WizardsUI.updateRetortsFromPicks").getBookImageFile ().size () == 0)
			{
				if (desc.length () > 0)
					desc.append (", ");
				
				if (pick.getQuantity () > 1)
					desc.append (pick.getQuantity () + "x");
				
				final String thisPickText = getLanguageHolder ().findDescription (getClient ().getClientDB ().findPick (pick.getPickID (), "updateRetortsFromPicks").getPickDescriptionSingular ());

				// Does the required index fall within the text for this pick?
				if ((charIndex >= desc.length ()) && (charIndex < desc.length () + thisPickText.length ()))
					result = pick.getPickID ();
				
				// Now add it
				desc.append (thisPickText);
			}
		}
		retorts.setText (getTextUtils ().replaceFinalCommaByAnd (desc.toString ()));

		return result;
	}	
	
	/**
	 * Updates the name of the currently selected wizard
	 */
	private final void updateWizard ()
	{
		wizardPortrait.setVisible (false);
		if (selectedWizard == null)
			playerName.setText (null);
		else
		{
			playerName.setText (getWizardClientUtils ().getPlayerName (selectedWizard));
			
			try
			{
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) selectedWizard.getPersistentPlayerPublicKnowledge ();
				final BufferedImage unscaledPortrait;
				if (pub.getCustomPhoto () != null)
					unscaledPortrait = ImageIO.read (new ByteArrayInputStream (pub.getCustomPhoto ()));
				else if (pub.getStandardPhotoID () != null)
					unscaledPortrait = getUtils ().loadImage (getClient ().getClientDB ().findWizard (pub.getStandardPhotoID (), "WizardsUI").getPortraitImageFile ());
				else
					throw new MomException ("Player ID " + selectedWizard.getPlayerDescription ().getPlayerID () + " has neither a custom or standard photo");
				
				final Image scaledPortrait = unscaledPortrait.getScaledInstance
					(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH);
				
				wizardPortrait.setIcon (new ImageIcon (scaledPortrait));
				wizardPortrait.setVisible (true);
			}
			catch (final IOException e)
			{
				log.error ("Error displaying full size wizard portrait", e);
			}
		}
		
		// Can only see fame of ourselves
		fameLabel.setVisible ((selectedWizard != null) && (selectedWizard.getPlayerDescription ().getPlayerID ().equals (getClient ().getOurPlayerID ())));
		if (fameLabel.isVisible ())
			try
			{
				final ProductionTypeEx fame = getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME, "updateWizard");
				final String fameSuffix = getLanguageHolder ().findDescription (fame.getProductionTypeDescription ());
				
				final int basicFame = getResourceValueUtils ().calculateBasicFame (getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ());
				final int modifiedFame = getResourceValueUtils ().calculateModifiedFame (getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (),
					selectedWizard, getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
				
				if (basicFame == modifiedFame)
					fameLabel.setText (getTextUtils ().intToStrCommas (basicFame) + " " + fameSuffix); 
				else
					fameLabel.setText (getTextUtils ().intToStrCommas (modifiedFame) + " (" + getTextUtils ().intToStrCommas (basicFame) + ") " + fameSuffix); 
			}
			catch (final Exception e)
			{
				log.error (e, e);
				fameLabel.setVisible (false);
			}
	}
	
	/**
	 * @param wizard Wizard whose picks or fame has changed
	 * @throws IOException If there is a problem loading any of the book images
	 * @throws RecordNotFoundException If one of the picks we have isn't in the graphics XML file
	 */
	public final void wizardUpdated (final PlayerPublicDetails wizard)
		throws IOException, RecordNotFoundException
	{
		// If it isn't the wizard we're currently showing then we don't care
		if ((playerName != null) && (wizard == selectedWizard))
		{
			updateBookshelfFromPicks ();
			updateRetortsFromPicks (-1);
			updateWizard ();
		}
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		if (contentPane != null)
		{
			final String title;
			if (getTargetingSpell () != null)
				title = getLanguageHolder ().findDescription (getLanguages ().getWizardsScreen ().getTargetWizard ());
			else
				title = getLanguageHolder ().findDescription (getLanguages ().getWizardsScreen ().getTitle ());
			
			getFrame ().setTitle (title);
			wizardsTitle.setText (title);
			
			closeAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getClose ()));
			
			updateWizard ();
			
			// Do we have detect magic cast or are we targeting spell blast?
			final boolean spellBlast = (getTargetingSpell () != null) && (getTargetingSpell ().getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_BLAST));
			
			final boolean detectMagic = (spellBlast) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPlayerID (), CommonDatabaseConstants.SPELL_ID_DETECT_MAGIC, null, null, null, null) != null);
			
			// Update names of spells, if we can see them because of Detect Magic
			if (detectMagic)
			{
				int n = 0;
				for (final PlayerPublicDetails player : getClient ().getPlayers ())
				{
					final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
					if (PlayerKnowledgeUtils.isWizard (pub.getWizardID ()))
					{
						final OverlandCastingInfo info = getOverlandCastingInfo ().get (player.getPlayerDescription ().getPlayerID ());
						
						// Spell name
						try
						{
							final List<LanguageText> text;
							if ((info == null) || (info.getSpellID () == null))
								text = getLanguages ().getWizardsScreen ().getCastingNothing ();
							else
								text = getClient ().getClientDB ().findSpell (info.getSpellID (), "WizardsUI").getSpellName ();
						
							currentlyCastingLabels.get (n).setText (getLanguageHolder ().findDescription (text));
						}
						catch (final IOException e)
						{
							log.error ("Can't find name of spell to display from Detect Magic for \"" + info.getSpellID () + "\"", e);
						}
						
						// Mana spent
						if (spellBlast)
						{
							if ((info == null) || (info.getManaSpentOnCasting () == null))
								currentlyCastingManaLabels.get (n).setText (null);
							else
								try
								{
									currentlyCastingManaLabels.get (n).setText (getTextUtils ().intToStrCommas (info.getManaSpentOnCasting ()) + " " +
										getLanguageHolder ().findDescription (getClient ().getClientDB ().findProductionType
											(CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "WizardsUI").getProductionTypeSuffix ()));
								}
								catch (final IOException e)
								{
									log.error ("Can't find mana production type to display mana spent for Spell Blast", e);
								}
						}
						
						n++;
					}
				}
			}
		}
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

	/**
	 * @return Client-side pick utils
	 */
	public final PlayerPickClientUtils getPlayerPickClientUtils ()
	{
		return playerPickClientUtils;
	}

	/**
	 * @param util Client-side pick utils
	 */
	public final void setPlayerPickClientUtils (final PlayerPickClientUtils util)
	{
		playerPickClientUtils = util;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
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
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}
	
	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
	
	/**
	 * @return Info about what wizards are casting, gleaned from Detect Magic spell, keyed by playerID
	 */
	public final Map<Integer, OverlandCastingInfo> getOverlandCastingInfo ()
	{
		return overlandCastingInfo;
	}

	/**
	 * @return Which spell is currently being targeted using this screen
	 */
	public final Spell getTargetingSpell ()
	{
		return targetingSpell;
	}
	
	/**
	 * @param s Which spell is currently being targeted using this screen
	 */
	public final void setTargetingSpell (final Spell s)
	{
		if (s != getTargetingSpell ())
		{
			targetingSpell = s;
			languageChanged ();
		}
	}
}