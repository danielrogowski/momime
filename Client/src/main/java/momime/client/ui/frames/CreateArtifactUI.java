package momime.client.ui.frames;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.SpellLang;
import momime.client.ui.MomUIConstants;
import momime.client.utils.HeroItemClientUtils;
import momime.client.utils.TextUtils;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.HeroItemType;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

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
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** Spell book */
	private SpellBookUI spellBookUI;

	/** OK action */
	private Action okAction;
	
	/** Cancel action */
	private Action cancelAction;
	
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
	
	/** Edit box to type a name for the item */
	private JTextField itemName;
	
	/** Label showing crafting cost */
	private JLabel craftingCost;

	/** The item creation spell being cast */
	private Spell spell;
	
	/** The spell charges chosen to imbue into the item */
	private Spell spellChargesChosenSpell;
	
	/** Label showing the spell charges chosen to imbue into the item */
	private JLabel spellChargesChosenSpellLabel;
	
	/** The number of spell charges chosen to imbue into the item */
	private int spellChargesChosenCount;
	
	/** The currently selected item type */
	private HeroItemType heroItemType;

	/** The graphics for the currently selected item type */
	private HeroItemTypeGfx heroItemTypeGfx;
	
	/** Index into the available images list for the selected item type */
	private int imageNumber;
	
	/** Background panel that appears when choosing spell charges */
	private JLabel spellChargesBackground;
	
	/** x1 x2 x3 x4 buttons for picking number of spell charges */
	private List<JButton> spellChargesButtons = new ArrayList<JButton> ();
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/createArtifact/background.png");
		final BufferedImage itemTypeButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button62x26Normal.png");
		final BufferedImage itemTypeButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button62x26Pressed.png");
		final BufferedImage leftArrowNormal = getUtils ().loadImage ("/momime.client.graphics/ui/createArtifact/goldArrowLeftNormal.png");
		final BufferedImage leftArrowPressed = getUtils ().loadImage ("/momime.client.graphics/ui/createArtifact/goldArrowLeftPressed.png");
		final BufferedImage rightArrowNormal = getUtils ().loadImage ("/momime.client.graphics/ui/createArtifact/goldArrowRightNormal.png");
		final BufferedImage rightArrowPressed = getUtils ().loadImage ("/momime.client.graphics/ui/createArtifact/goldArrowRightPressed.png");
		final BufferedImage spellChargesBackgroundImage = getUtils ().loadImage ("/momime.client.graphics/ui/createArtifact/spellChargesBackground.png");
		final BufferedImage spellChargesCountNormal = getUtils ().loadImage ("/momime.client.graphics/ui/createArtifact/spellChargesCountNormal.png");
		final BufferedImage spellChargesCountPressed = getUtils ().loadImage ("/momime.client.graphics/ui/createArtifact/spellChargesCountPressed.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x17Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x17Pressed.png");

		// Actions
		final Action previousImageAction = new LoggingAction ((ev) -> updateItemImage (-1));
		final Action nextImageAction = new LoggingAction ((ev) -> updateItemImage (1));
		
		okAction = new LoggingAction ((ev) ->
		{
			final RequestCastSpellMessage msg = new RequestCastSpellMessage ();
			msg.setSpellID (getSpell ().getSpellID ());
			msg.setHeroItem (buildHeroItem ());
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
			
			getFrame ().setVisible (false);
		});
		
		cancelAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
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

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmCreateArtifactOK");

		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmCreateArtifactCancel");
		
		final XmlLayoutComponent itemNameLayout = getCreateArtifactLayout ().findComponent ("frmCreateArtifactName");
		itemName = getUtils ().createTransparentTextField (MomUIConstants.SILVER, getSmallFont (), new Dimension
			(itemNameLayout.getWidth (), itemNameLayout.getHeight ()));
		contentPane.add (itemName, "frmCreateArtifactName");
		
		craftingCost = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (craftingCost, "frmCreateArtifactCraftingCost");
		
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
		
		// Spell charges area - this must be before the spell effect bonuses panel, since the two overlap but we take clicks here in preference
		for (int n = 1; n <= 4; n++)
		{
			final int nn = n;
			final Action spellChargesAction = new LoggingAction ("x" + n, (ev) ->
			{
				// Finally add spell charges to the bonus list
				selectedBonusIDs.add (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
				spellChargesChosenCount = nn;

				spellChargesBackground.setVisible (false);
				spellChargesChosenSpellLabel.setVisible (false);
				
				for (final JButton button : spellChargesButtons)
					button.setVisible (false);
				
				updateBonusColouring ();
				updateCraftingCost ();
				languageChanged ();		// To change the "Spell Charges" text in the selected bonus to say "Spell Name x4"
			});
			
			final JButton spellChargesButton = getUtils ().createImageButton (spellChargesAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN,
				getLargeFont (), spellChargesCountNormal, spellChargesCountPressed, spellChargesCountNormal);
			contentPane.add (spellChargesButton, "frmCreateArtifactSpellCharges" + n);
			spellChargesButtons.add (spellChargesButton);
		}
		
		spellChargesChosenSpellLabel = getUtils ().createLabel (MomUIConstants.DARK_BROWN, getLargeFont ());
		contentPane.add (spellChargesChosenSpellLabel, "frmCreateArtifactSpellChargesName");

		spellChargesBackground = getUtils ().createImage (spellChargesBackgroundImage);
		contentPane.add (spellChargesBackground, "frmCreateArtifactSpellChargesBackground");
		
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
		
		// If we have no spell yet then there's not much else we can reliably do - we'll have to repeat this later anyway
		if (getSpell () != null)
		{
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
			spellChargesBackground.setVisible (false);
			spellChargesChosenSpellLabel.setVisible (false);
			spellChargesChosenSpell = null;
			spellChargesChosenCount = 0;
			
			for (final JButton button : spellChargesButtons)
				button.setVisible (false);
			
			// Set base item name
			itemName.setText (getLanguage ().findHeroItemTypeDescription (heroItemType.getHeroItemTypeID ()));
			
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
						updateCraftingCost ();
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
				final Action bonusAction;
				if (bonus.getHeroItemBonusID ().equals (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES))
					
					// Open spell book back up to pick the spell we want to imbue
					bonusAction = new LoggingAction ((ev) ->
					{
						if (selectedBonusIDs.contains (bonus.getHeroItemBonusID ()))
						{
							selectedBonusIDs.remove (bonus.getHeroItemBonusID ());
							spellChargesBackground.setVisible (false);
							spellChargesChosenSpellLabel.setVisible (false);
							spellChargesChosenSpell = null;
							spellChargesChosenCount = 0;
							updateBonusColouring ();
							updateCraftingCost ();
							languageChanged ();		// Change the unselected text back to the generic text "Spell Charges"
						}
						else
						{
							getSpellBookUI ().setCastType (SpellCastType.SPELL_CHARGES);
							getSpellBookUI ().setVisible (true);
						}
					});
				else
					
					// For any other bonuses, just add directly
					bonusAction = new LoggingAction ((ev) ->
					{
						if (selectedBonusIDs.contains (bonus.getHeroItemBonusID ()))
							selectedBonusIDs.remove (bonus.getHeroItemBonusID ());
						else
							selectedBonusIDs.add (bonus.getHeroItemBonusID ());
						
						updateBonusColouring ();
						updateCraftingCost ();
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
	}
	
	/**
	 * @param chosenSpell Spell chosen from spell book to imbue into this item
	 */
	public final void setSpellCharges (final Spell chosenSpell)
	{
		log.trace ("Entering setSpellCharges: " + chosenSpell.getSpellID ());

		spellChargesChosenSpell = chosenSpell;
		spellChargesBackground.setVisible (true);
		spellChargesChosenSpellLabel.setVisible (true);
		
		// Now ask for whether to put in 1,2,3 or 4 copies of the spell
		for (int n = 0; n < 4; n++)
			spellChargesButtons.get (n).setVisible (n < getClient ().getSessionDescription ().getUnitSetting ().getMaxHeroItemSpellCharges ());
		
		languageChanged ();
		
		log.trace ("Exiting setSpellCharges");
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
		itemImage.setIcon (new ImageIcon (getUtils ().doubleSize (getUtils ().loadImage (heroItemTypeGfx.getHeroItemTypeImageFile ().get (imageNumber)))));
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
				boolean ok = (getClient ().getSessionDescription ().getUnitSetting ().getMaxHeroItemBonuses () == null) ||
					(selectedBonusIDs.size () < getClient ().getSessionDescription ().getUnitSetting ().getMaxHeroItemBonuses ());
				
				final Iterator<HeroItemBonusStat> bonusStatIter = getClient ().getClientDB ().findHeroItemBonus (bonusButton.getKey (), "updateBonusColouring").getHeroItemBonusStat ().iterator ();
				while ((ok) && (bonusStatIter.hasNext ()))
					if (bonusSkillIDs.contains (bonusStatIter.next ().getUnitSkillID ()))
						ok = false;
				
				bonusAction.setEnabled (ok);
				bonusButton.getValue ().setForeground (ok ? MomUIConstants.DULL_GOLD : MomUIConstants.GRAY);
			}				 
		}
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");

		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCreateArtifact", "OK"));
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCreateArtifact", "Cancel"));
		
		if (getSpell () != null)
		{
			// Title
			final SpellLang spellLang = getLanguage ().findSpell (spell.getSpellID ());
			final String spellName = (spellLang == null) ? null : spellLang.getSpellName ();
			getFrame ().setTitle (spellName != null ? spellName : spell.getSpellID ());
		
			// Spell charges
			if (spellChargesChosenSpell == null)
				spellChargesChosenSpellLabel.setText (null);
			else
			{
				final SpellLang chosenSpellLang = getLanguage ().findSpell (spellChargesChosenSpell.getSpellID ());
				final String chosenSpellName = (chosenSpellLang == null) ? null : chosenSpellLang.getSpellName ();
				spellChargesChosenSpellLabel.setText (chosenSpellName != null ? chosenSpellName : spellChargesChosenSpell.getSpellID ());
			}
			
			// Item type buttons
			for (final Entry<String, Action> itemTypeAction : itemTypeActions.entrySet ())
				itemTypeAction.getValue ().putValue (Action.NAME, getLanguage ().findHeroItemTypeDescription (itemTypeAction.getKey ()));
			
			// Item bonus buttons
			for (final Entry<String, Action> itemBonusAction : itemBonusActions.entrySet ())
			{
				// Show fully chosen spell charges as e.g. "Bless x4" other than just the text "Spell Charges"
				final String bonusDescription;
				if ((itemBonusAction.getKey ().equals (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES)) &&
					(spellChargesChosenSpell != null) && (spellChargesChosenCount > 0))
					
					bonusDescription = spellChargesChosenSpellLabel.getText () + " x" + spellChargesChosenCount; 
				else
					bonusDescription = getLanguage ().findHeroItemBonusDescription (itemBonusAction.getKey ());
				
				itemBonusAction.getValue ().putValue (Action.NAME, bonusDescription);
			}
			
			// The "MP" suffix may have changed
			try
			{
				updateCraftingCost ();
			}
			catch (final IOException e)
			{
				log.error (e, e);
			}
		}
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * Updates the label showing the current crafting cost of the item
	 * @throws IOException If there is a problem
	 */
	private final void updateCraftingCost () throws IOException
	{
		log.trace ("Entering updateCraftingCost");
		
		final HeroItem heroItem = buildHeroItem ();
		
		// Base crafting cost
		final ProductionTypeLang mp = getLanguage ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		String mpSuffix = (mp == null) ? null : mp.getProductionTypeSuffix ();
		if (mpSuffix == null)
			mpSuffix = CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA;

		final int baseCost = getHeroItemCalculations ().calculateCraftingCost (heroItem, getClient ().getClientDB ());
		String text = getTextUtils ().intToStrCommas (baseCost) + " " + mpSuffix;
		
		// Do we have artificer or runemaster?
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "updateCraftingCost");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
		
		final int reducedCost = getSpellUtils ().getReducedOverlandCastingCost
			(getSpell (), heroItem, pub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
		
		if (reducedCost < baseCost)
			text = text + " / " + getTextUtils ().intToStrCommas (reducedCost) + " " + mpSuffix;
		
		// Set text
		craftingCost.setText (text);
		
		log.trace ("Exiting updateCraftingCost = " + text);
	}
	
	/**
	 * @return Hero item object built from all the values on the form
	 */
	private final HeroItem buildHeroItem ()
	{
		log.trace ("Entering buildHeroItem");
		
		final HeroItem heroItem = new HeroItem ();
		heroItem.setHeroItemTypeID (heroItemType.getHeroItemTypeID ());
		heroItem.setHeroItemName (itemName.getText ());
		heroItem.setHeroItemImageNumber (imageNumber);
		
		for (final String bonusID : selectedBonusIDs)
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			heroItem.getHeroItemChosenBonus ().add (bonus);
		}
		
		if (selectedBonusIDs.contains (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES)) {
			heroItem.setSpellID (spellChargesChosenSpell.getSpellID ());
			heroItem.setSpellChargeCount (spellChargesChosenCount);
		}

		log.trace ("Exiting buildHeroItem = " + heroItem);
		return heroItem;
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
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return Spell book
	 */
	public final SpellBookUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookUI ui)
	{
		spellBookUI = ui;
	}
}