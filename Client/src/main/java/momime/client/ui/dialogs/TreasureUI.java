package momime.client.ui.dialogs;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.MapFeatureLang;
import momime.client.language.database.PickLang;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.ShortcutKeyLang;
import momime.client.language.database.SpellLang;
import momime.client.language.database.TileTypeLang;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.MomUIConstants;
import momime.client.utils.TextUtils;
import momime.common.database.PickAndQuantity;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.Shortcut;
import momime.common.messages.MemoryUnit;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.servertoclient.TreasureRewardMessage;
import momime.common.messages.servertoclient.TreasureRewardPrisoner;
import momime.common.utils.UnitUtils;

/**
 * Popup when we walk into a node/lair/tower to say what treasure we found
 */
public final class TreasureUI extends MomClientDialogUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MessageBoxUI.class);

	/** Bullet point prefix for each line of treasure reward */
	private final static String BULLET_POINT = "\u2022 ";
	
	/** XML layout */
	private XmlLayoutContainerEx treasureLayout;

	/** Small font */
	private Font smallFont;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;
	
	/** OK action */
	private Action okAction;
	
	/** Content pane */
	private JPanel contentPane;

	/** Main text area */
	private JTextArea messageText;
	
	/** Details of what was awarded */
	private TreasureRewardMessage treasureReward;

	/** Multiplayer client */
	private MomClient client;
	
	/**
	 * Sets up the dialog once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/treasure.png");
		final BufferedImage sideImage = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/treasureImage.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");
	
		// Actions
		okAction = new LoggingAction ((ev) -> getDialog ().dispose ());
		
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
		contentPane.setLayout (new XmlLayoutManager (getTreasureLayout ()));

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmTreasureButton");
		
		messageText = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		contentPane.add (getUtils ().createTransparentScrollPane (messageText), "frmTreasureText");
		
		// Need to resize the side images
		final XmlLayoutComponent imageSize = getTreasureLayout ().findComponent ("frmTreasureImage");
		contentPane.add (getUtils ().createImage (sideImage.getScaledInstance (imageSize.getWidth (), imageSize.getHeight (), Image.SCALE_SMOOTH)), "frmTreasureImage");
		
		final String filename;
		if (getTreasureReward ().getMapFeatureID () != null)
			filename = getGraphicsDB ().findMapFeature (getTreasureReward ().getMapFeatureID (), "TreasureUI").getMonsterFoundImageFile ();
		else
			filename = getGraphicsDB ().findTileType (getTreasureReward ().getTileTypeID (), "TreasureUI").getMonsterFoundImageFile ();
		
		final BufferedImage lairImage = getUtils ().loadImage (filename);
		contentPane.add (getUtils ().createImage (lairImage.getScaledInstance (imageSize.getWidth (), imageSize.getHeight (), Image.SCALE_SMOOTH)), "frmTreasureLair");
		
		// Lock dialog size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);

		// Shortcut keys
		contentPane.getActionMap ().put (Shortcut.MESSAGE_BOX_CLOSE,	okAction);
		
		log.trace ("Exiting init");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");

		getDialog ().setTitle (getLanguage ().findCategoryEntry ("frmTreasure", "Title"));
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmTreasure", "OK"));

		// Work out text
		final String locationDescription;
		if (getTreasureReward ().getMapFeatureID () != null)
		{
			final MapFeatureLang mapFeature = getLanguage ().findMapFeature (getTreasureReward ().getMapFeatureID ());
			final String mapFeatureDescription = (mapFeature == null) ? null : mapFeature.getMapFeatureDescription ();
			locationDescription = (mapFeatureDescription != null) ? mapFeatureDescription : getTreasureReward ().getMapFeatureID ();
		}
		else
		{
			final TileTypeLang tileType = getLanguage ().findTileType (getTreasureReward ().getTileTypeID ());
			final String tileTypeDescription = (tileType == null) ? null : tileType.getTileTypeDescription ();
			locationDescription = (tileTypeDescription != null) ? tileTypeDescription : getTreasureReward ().getTileTypeID ();
		}
		
		final StringBuilder text = new StringBuilder (getLanguage ().findCategoryEntry ("frmTreasure", "Text").replaceAll ("LOCATION_DESCRIPTION", locationDescription));
		
		if ((getTreasureReward ().getHeroItem ().size () == 0) && (getTreasureReward ().getPick ().size () == 0) && (getTreasureReward ().getResource ().size () == 0) &&
			(getTreasureReward ().getSpellID ().size () == 0) && (getTreasureReward ().getPrisoner ().size () == 0))
			
			text.append (System.lineSeparator () + BULLET_POINT + getLanguage ().findCategoryEntry ("frmTreasure", "Nothing"));
		else
			try
			{
				// Books and retorts
				for (final PickAndQuantity pick : getTreasureReward ().getPick ())
				{
					final PickLang pickLang = getLanguage ().findPick (pick.getPickID ());
					
					if (getGraphicsDB ().findPick (pick.getPickID (), "TreasureUI").getBookImageFile ().size () == 0)
					{
						final String pickDescription = (pickLang == null) ? null : pickLang.getPickDescriptionSingular ();
						text.append (System.lineSeparator () + BULLET_POINT + getLanguage ().findCategoryEntry ("frmTreasure", "Retort").replaceAll
							("PICK_NAME_SINGULAR", (pickDescription != null) ? pickDescription : pick.getPickID ()) + ";");
					}
					else if (pick.getQuantity () == 1)
					{
						final String pickDescription = (pickLang == null) ? null : pickLang.getPickDescriptionSingular ();
						text.append (System.lineSeparator () + BULLET_POINT + "1 " + ((pickDescription != null) ? pickDescription : pick.getPickID ()) + ";");
					}
					else
					{
						final String pickDescription = (pickLang == null) ? null : pickLang.getPickDescriptionPlural ();
						text.append (System.lineSeparator () + BULLET_POINT + pick.getQuantity () + " " + ((pickDescription != null) ? pickDescription : pick.getPickID ()) + ";");
					}
				}
				
				// Prisoners - mention whether they fit or got bumped outside or lost completely
				for (final TreasureRewardPrisoner prisoner : getTreasureReward ().getPrisoner ())
				{
					final MemoryUnit unit = getUnitUtils ().findUnitURN (prisoner.getPrisonerUnitURN (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "TreasureUI");
					
					getUnitStatsReplacer ().setUnit (unit);
					text.append (System.lineSeparator () + BULLET_POINT + getUnitStatsReplacer ().replaceVariables
						(getLanguage ().findCategoryEntry ("frmTreasure", "Prisoner" + prisoner.getUnitAddBumpType ().value ())) + ";");
				}
				
				// Hero items
				for (final NumberedHeroItem item : getTreasureReward ().getHeroItem ())
					text.append (System.lineSeparator () + BULLET_POINT + item.getHeroItemName () + ";");
				
				// Learn spells
				for (final String spellID : getTreasureReward ().getSpellID ())
				{
					final SpellLang spell = getLanguage ().findSpell (spellID);
					final String spellName = (spell == null) ? null : spell.getSpellName ();
					text.append (System.lineSeparator () + BULLET_POINT + getLanguage ().findCategoryEntry ("frmTreasure", "Spell").replaceAll
						("SPELL_NAME", (spellName != null) ? spellName : spellID) + ";");
				}
				
				// Gold and mana
				for (final ProductionTypeAndUndoubledValue resource : getTreasureReward ().getResource ())
				{
					final ProductionTypeLang productionType = getLanguage ().findProductionType (resource.getProductionTypeID ());
					final String productionTypeDescription = (productionType == null) ? null : productionType.getProductionTypeDescription ();
					text.append (System.lineSeparator () + BULLET_POINT + getTextUtils ().intToStrCommas (resource.getUndoubledProductionValue ()) + " " +
						((productionTypeDescription != null) ? productionTypeDescription : resource.getProductionTypeID ()) + ";");
				}
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		
		// Replace final semicolon with a .
		String textString = text.toString ();
		int semicolon = textString.lastIndexOf (';');
		if (semicolon == textString.length () - 1)
			textString = textString.substring (0, textString.length () - 1) + ".";
			
		messageText.setText (textString);
		
		// Shortcut keys
		contentPane.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).clear ();
		for (final Object shortcut : contentPane.getActionMap ().keys ())
			if (shortcut instanceof Shortcut)
			{
				final ShortcutKeyLang shortcutKey = getLanguage ().findShortcutKey ((Shortcut) shortcut);
				if (shortcutKey != null)
				{
					final String keyCode = (shortcutKey.getNormalKey () != null) ? shortcutKey.getNormalKey () : shortcutKey.getVirtualKey ().value ().substring (3);
					log.debug ("Binding \"" + keyCode + "\" to action " + shortcut);
					contentPane.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (keyCode), shortcut);
				}
			}

		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getTreasureLayout ()
	{
		return treasureLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setTreasureLayout (final XmlLayoutContainerEx layout)
	{
		treasureLayout = layout;
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
	 * @return Details of what was awarded
	 */
	public final TreasureRewardMessage getTreasureReward ()
	{
		return treasureReward;
	}

	/**
	 * @param reward Details of what was awarded
	 */
	public final void setTreasureReward (final TreasureRewardMessage reward)
	{
		treasureReward = reward;
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return Variable replacer for outputting skill descriptions
	 */
	public final UnitStatsLanguageVariableReplacer getUnitStatsReplacer ()
	{
		return unitStatsReplacer;
	}

	/**
	 * @param replacer Variable replacer for outputting skill descriptions
	 */
	public final void setUnitStatsReplacer (final UnitStatsLanguageVariableReplacer replacer)
	{
		unitStatsReplacer = replacer;
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
}