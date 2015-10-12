package momime.client.ui.frames;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.language.database.SpellLang;
import momime.client.ui.MomUIConstants;
import momime.client.utils.HeroItemClientUtils;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemType;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * UI for designing hero items
 */
public final class CreateArtifactUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (QueuedSpellsUI.class);

	/** Number of rows of buttons of attribute bonuses */
	private final static int ATTRIBUTE_BONUS_ROWS = 16;
	
	/** XML layout */
	private XmlLayoutContainerEx createArtifactLayout;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Small font */
	private Font smallFont;
	
	/** Large font */
	private Font largeFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Hero item calculations */
	private HeroItemCalculations heroItemCalculations;
	
	/** Client-side hero item utils */
	private HeroItemClientUtils heroItemClientUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** Dynamically created item type actions */
	private final Map<String, Action> itemTypeActions = new HashMap<String, Action> ();

	/** Dynamically created item type buttons */
	private final Map<String, JButton> itemTypeButtons = new HashMap<String, JButton> ();

	/** Dynamically created item bonus actions */
	private final Map<String, Action> itemBonusActions = new HashMap<String, Action> ();

	/** Dynamically created item bonus buttons */
	private final Map<String, JButton> itemBonusButtons = new HashMap<String, JButton> ();
	
	/** Panel containing all the spell effect bonuses */
	private JPanel spellEffectBonusesPanel; 
	
	/** List of currently selected bonuses */
	private final List<String> selectedBonusIDs = new ArrayList<String> ();
	
	/** Image of the item being made */
	private JLabel itemImage;

	/** The item creation spell being cast */
	private Spell spell;
	
	/** The currently selected item type */
	private HeroItemType heroItemType;

	/** The graphics for the currently selected item type */
	private HeroItemTypeGfx heroItemTypeGfx;
	
	/** Index into the available images list for the selected item type */
	private int imageNumber;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/createArtifact.png");
		final BufferedImage itemTypeButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button62x26Normal.png");
		final BufferedImage itemTypeButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button62x26Pressed.png");
		final BufferedImage leftArrowNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/goldArrowLeftNormal.png");
		final BufferedImage leftArrowPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/goldArrowLeftPressed.png");
		final BufferedImage rightArrowNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/goldArrowRightNormal.png");
		final BufferedImage rightArrowPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/goldArrowRightPressed.png");

		// Actions
		final Action previousImageAction = new LoggingAction ((ev) -> updateItemImage (-1));
		final Action nextImageAction = new LoggingAction ((ev) -> updateItemImage (1));
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};

		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getCreateArtifactLayout ()));
		
		itemImage = new JLabel ();
		contentPane.add (itemImage, "frmCreateArtifactImage");
		
		contentPane.add (getUtils ().createImageButton (previousImageAction, null, null, null, leftArrowNormal, leftArrowPressed, leftArrowNormal), "frmCreateArtifactImagePrevious");
		contentPane.add (getUtils ().createImageButton (nextImageAction, null, null, null, rightArrowNormal, rightArrowPressed, rightArrowNormal), "frmCreateArtifactImageNext");
		
		// Item type buttons
		int itemTypeNumber = 0;
		for (final HeroItemType itemType : getClient ().getClientDB ().getHeroItemType ())
		{
			itemTypeNumber++;
			
			final Action itemTypeAction = new LoggingAction ((ev) -> selectItemType (itemType));
			
			final JButton itemTypeButton = getUtils ().createImageButton (itemTypeAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
				itemTypeButtonNormal, itemTypeButtonPressed, itemTypeButtonNormal);
			contentPane.add (itemTypeButton, "frmCreateArtifactItemType" + itemTypeNumber);
			
			itemTypeActions.put (itemType.getHeroItemTypeID (), itemTypeAction);
			itemTypeButtons.put (itemType.getHeroItemTypeID (), itemTypeButton);
		}
		
		// Spell effect bonuses panel
		final JPanel spellEffectBonusesContainer = new JPanel (new BorderLayout ());		// This is to make the buttons take up minimum space
		spellEffectBonusesContainer.setOpaque (false);
		
		spellEffectBonusesPanel = new JPanel (new GridLayout (0, 1, 0, 0));
		spellEffectBonusesPanel.setOpaque (false);
		spellEffectBonusesContainer.add (spellEffectBonusesPanel, BorderLayout.NORTH);
		
		final JScrollPane spellEffectsListScroll = getUtils ().createTransparentScrollPane (spellEffectBonusesContainer);
		spellEffectsListScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (spellEffectsListScroll, "frmCreateArtifactSpellEffectBonuses");
		
		// Lock frame size
		selectItemType (getClient ().getClientDB ().getHeroItemType ().get (0));		// Pick Sword by default
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);

		log.trace ("Exiting init");
	}
	
	/**
	 * @param newItemType Type of item we now wish to make
	 * @throws IOException If we can't find the new item type in the graphics XML, we find it but it has no image(s) defined, or there's a problem loading the first image
	 */
	private final void selectItemType (final HeroItemType newItemType) throws IOException
	{
		heroItemType = newItemType;
		
		// Clear old dynamically created controls
		for (final JButton itemBonusButton : itemBonusButtons.values ())
		{
			// Don't know which it will be in - just try both
			contentPane.remove (itemBonusButton);
			spellEffectBonusesPanel.remove (itemBonusButton);
		}
		
		itemBonusButtons.clear ();
		itemBonusActions.clear ();
		selectedBonusIDs.clear ();
		
		// Light up the relevant item type button gold
		for (final Entry<String, JButton> itemTypeButton : itemTypeButtons.entrySet ())
			itemTypeButton.getValue ().setForeground
				(itemTypeButton.getKey ().equals (heroItemType.getHeroItemTypeID ()) ? MomUIConstants.GOLD : MomUIConstants.DARK_BROWN);
		
		// Update the image
		heroItemTypeGfx = getGraphicsDB ().findHeroItemType (heroItemType.getHeroItemTypeID (), "selectItemType");
		if (heroItemTypeGfx.getHeroItemTypeImageFile ().size () == 0)
			throw new IOException ("Hero item type " + heroItemType.getHeroItemTypeID () + " exists in graphics XML but has no image(s) defined"); 
				
		imageNumber = 0;
		updateItemImage (0);
		
		// Find what bonuses are applicable to this item type and the picks we have
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "selectItemType");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
		
		final List<HeroItemBonus> attributeBonuses = new ArrayList<HeroItemBonus> ();
		final List<HeroItemBonus> spellEffectBonuses = new ArrayList<HeroItemBonus> ();
		
		for (final HeroItemTypeAllowedBonus allowedBonus : heroItemType.getHeroItemTypeAllowedBonus ())
			if (getHeroItemCalculations ().haveRequiredBooksForBonus (allowedBonus.getHeroItemBonusID (), pub.getPick (), getClient ().getClientDB ()))
			{
				final HeroItemBonus bonus = getClient ().getClientDB ().findHeroItemBonus (allowedBonus.getHeroItemBonusID (), "selectItemType");
				
				// Limit bonuses available for Enchant Item, but check for spell max = 0 first - so we allow Spell Charges for Create Artifact
				if ((spell.getHeroItemBonusMaximumCraftingCost () == 0) ||
					((bonus.getBonusCraftingCost () != null) && (bonus.getBonusCraftingCost () <= spell.getHeroItemBonusMaximumCraftingCost ())))
				{
					// This a pretty cheesy way to determine which list the bonus should go in, but it works for now;
					// it also gets "Spell Charges" correctly in the correct right hand list which splitting according to bonuses
					// that have or don't have any pre-requisites would get wrong.
					if (bonus.isCraftingCostMultiplierApplies ())
						attributeBonuses.add (bonus);
					else
						spellEffectBonuses.add (bonus);
				}
			}
		
		// Insert spaces between bonuses to the same attribute, i.e. so there's a space left between the "+ attack"s and the "+ defence"s and so on
		getHeroItemClientUtils ().insertGapsBetweenDifferentKindsOfAttributeBonuses (attributeBonuses);
		getHeroItemClientUtils ().shuffleSplitPoint (attributeBonuses, ATTRIBUTE_BONUS_ROWS);
		
		// Create buttons for the attribute bonuses
		int buttonNo = 0;
		for (final HeroItemBonus bonus : attributeBonuses)
		{
			buttonNo++;
			if (bonus != null)
			{
				final Action bonusAction = new LoggingAction ((ev) ->
				{
					if (selectedBonusIDs.contains (bonus.getHeroItemBonusID ()))
						selectedBonusIDs.remove (bonus.getHeroItemBonusID ());
					else
						selectedBonusIDs.add (bonus.getHeroItemBonusID ());
					
					updateBonusColouring ();
				});
				
				final JButton bonusButton = getUtils ().createTextOnlyButton (bonusAction, MomUIConstants.DULL_GOLD, getLargeFont ());
				contentPane.add (bonusButton, "frmCreateArtifactAttributeBonus" + buttonNo);

				itemBonusActions.put (bonus.getHeroItemBonusID (), bonusAction);
				itemBonusButtons.put (bonus.getHeroItemBonusID (), bonusButton);
			}
		}
		
		// Create buttons for the spell effect bonuses
		for (final HeroItemBonus bonus : spellEffectBonuses)
		{
			final Action bonusAction = new LoggingAction ((ev) ->
			{
				if (selectedBonusIDs.contains (bonus.getHeroItemBonusID ()))
					selectedBonusIDs.remove (bonus.getHeroItemBonusID ());
				else
					selectedBonusIDs.add (bonus.getHeroItemBonusID ());
				
				updateBonusColouring ();
			});
			
			final JButton bonusButton = getUtils ().createTextOnlyButton (bonusAction, MomUIConstants.DULL_GOLD, getLargeFont ());
			bonusButton.setHorizontalAlignment (SwingConstants.LEFT);
			spellEffectBonusesPanel.add (bonusButton);

			itemBonusActions.put (bonus.getHeroItemBonusID (), bonusAction);
			itemBonusButtons.put (bonus.getHeroItemBonusID (), bonusButton);
		}
		
		languageChanged ();
		contentPane.revalidate ();
		contentPane.repaint ();
	}
	
	/**
	 * @param changeBy Amount to change the image by; set to +1/-1 for the image selection buttons
	 * @throws IOException If there is a problem loading the new image
	 */
	private final void updateItemImage (final int changeBy) throws IOException
	{
		// Update image number
		imageNumber = imageNumber + changeBy;
		while (imageNumber < 0)
			imageNumber = imageNumber + heroItemTypeGfx.getHeroItemTypeImageFile ().size ();

		while (imageNumber >= heroItemTypeGfx.getHeroItemTypeImageFile ().size ())
			imageNumber = imageNumber - heroItemTypeGfx.getHeroItemTypeImageFile ().size ();
		
		// Update icon
		itemImage.setIcon (new ImageIcon (doubleSize (getUtils ().loadImage (heroItemTypeGfx.getHeroItemTypeImageFile ().get (imageNumber)))));
	}
	
	/**
	 * Updates the colours of the bonus buttons to show which are selected, deselected, or unavailable
	 * @throws RecordNotFoundException If we have a skill selected that can't be found in the DB
	 */
	private final void updateBonusColouring () throws RecordNotFoundException
	{
		// First go through listing the attributes our selected bonuses are granting a bonus to
		final List<String> bonusSkillIDs = new ArrayList<String> ();
		for (final String bonusID : selectedBonusIDs)
			for (final UnitSkillAndValue bonusStat : getClient ().getClientDB ().findHeroItemBonus (bonusID, "updateBonusColouring").getHeroItemBonusStat ())
				bonusSkillIDs.add (bonusStat.getUnitSkillID ());
		
		// Now can go through all the buttons, highlighting any that are selected, dulling any that aren't selected,
		// and greying out any that give a bonus to an attribute that we've already picked - we can't pick both Atk+1 and Atk+2
		for (final Entry<String, JButton> bonusButton : itemBonusButtons.entrySet ())
		{
			final Action bonusAction = bonusButton.getValue ().getAction ();
			if (selectedBonusIDs.contains (bonusButton.getKey ()))
			{
				bonusAction.setEnabled (true);
				bonusButton.getValue ().setForeground (MomUIConstants.GOLD);				
			}
			else
			{
				// Typically there's only 1 bonus stat, so don't bother using an iterator
				boolean ok = true;
				for (final UnitSkillAndValue bonusStat : getClient ().getClientDB ().findHeroItemBonus (bonusButton.getKey (), "updateBonusColouring").getHeroItemBonusStat ())
					if (bonusSkillIDs.contains (bonusStat.getUnitSkillID ()))
						ok = false;
				
				bonusAction.setEnabled (ok);
				bonusButton.getValue ().setForeground (ok ? MomUIConstants.DULL_GOLD : MomUIConstants.GRAY);
			}				 
		}
	}

	/**
	 * @param source Source image
	 * @return Double sized image
	 */
	private final Image doubleSize (final BufferedImage source)
	{
		return source.getScaledInstance (source.getWidth () * 2, source.getHeight () * 2, Image.SCALE_FAST);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		final SpellLang spellLang = getLanguage ().findSpell (spell.getSpellID ());
		final String spellName = (spellLang == null) ? null : spellLang.getSpellName ();
		getFrame ().setTitle (spellName != null ? spellName : spell.getSpellID ());
		
		// Item type buttons
		for (final Entry<String, Action> itemTypeAction : itemTypeActions.entrySet ())
			itemTypeAction.getValue ().putValue (Action.NAME, getLanguage ().findHeroItemTypeDescription (itemTypeAction.getKey ()));
		
		// Item bonus buttons
		for (final Entry<String, Action> itemBonusAction : itemBonusActions.entrySet ())
			itemBonusAction.getValue ().putValue (Action.NAME, getLanguage ().findHeroItemBonusDescription (itemBonusAction.getKey ()));
		
		log.trace ("Exiting languageChanged");
	}

	/**
	 * @return The item creation spell being cast
	 */
	public final Spell getSpell ()
	{
		return spell;
	}

	/**
	 * @param s The item creation spell being cast
	 * @throws IOException If we can't find the new item type in the graphics XML, we find it but it has no image(s) defined, or there's a problem loading the first image
	 */
	public final void setSpell (final Spell s) throws IOException
	{
		spell = s;
		
		// If the form is already displayed and we're simply switching which spell is being cast, then update the form.
		// Do this by reselecting the item type - since some bonuses will need to appear/be removed depending on how the cost limit changed.
		if (itemImage != null)
			selectItemType (heroItemType);
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCreateArtifactLayout ()
	{
		return createArtifactLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCreateArtifactLayout (final XmlLayoutContainerEx layout)
	{
		createArtifactLayout = layout;
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
	 * @return Hero item calculations
	 */
	public final HeroItemCalculations getHeroItemCalculations ()
	{
		return heroItemCalculations;
	}

	/**
	 * @param calc Hero item calculations
	 */
	public final void setHeroItemCalculations (final HeroItemCalculations calc)
	{
		heroItemCalculations = calc;
	}

	/**
	 * @return Client-side hero item utils
	 */
	public final HeroItemClientUtils getHeroItemClientUtils ()
	{
		return heroItemClientUtils;
	}

	/**
	 * @param util Client-side hero item utils
	 */
	public final void setHeroItemClientUtils (final HeroItemClientUtils util)
	{
		heroItemClientUtils = util;
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
}