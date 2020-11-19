package momime.client.ui.dialogs;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.languages.database.Shortcut;
import momime.client.ui.MomUIConstants;
import momime.client.utils.TextUtils;
import momime.common.database.LanguageText;
import momime.common.database.Pick;
import momime.common.database.PickAndQuantity;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.messages.MemoryUnit;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.servertoclient.TreasureRewardMessage;
import momime.common.messages.servertoclient.TreasureRewardPrisoner;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;

/**
 * Popup when we walk into a node/lair/tower to say what treasure we found
 */
public final class TreasureUI extends MomClientDialogUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MessageBoxUI.class);

	/** Bullet point prefix for each line of treasure reward */
	private final static String BULLET_POINT = "\u2022 ";
	
	/** XML layout */
	private XmlLayoutContainerEx treasureLayout;

	/** Small font */
	private Font smallFont;
	
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
			filename = getClient ().getClientDB ().findMapFeature (getTreasureReward ().getMapFeatureID (), "TreasureUI").getMonsterFoundImageFile ();
		else if (getTreasureReward ().getTileTypeID () != null)
			filename = getClient ().getClientDB ().findTileType (getTreasureReward ().getTileTypeID (), "TreasureUI").getMonsterFoundImageFile ();
		else
			filename = getClient ().getClientDB ().findBuilding (getTreasureReward ().getBuildingID (), "TreasureUI").getMonsterFoundImageFile ();
		
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

		getDialog ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getTreasureScreen ().getTitle ()));
		okAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));

		// Work out text
		try
		{
			final String locationDescription;
			if (getTreasureReward ().getMapFeatureID () != null)
				locationDescription = getLanguageHolder ().findDescription (getClient ().getClientDB ().findMapFeature (getTreasureReward ().getMapFeatureID (), "TreasureUI").getMapFeatureDescription ());
			else if (getTreasureReward ().getTileTypeID () != null)
				locationDescription = getLanguageHolder ().findDescription (getClient ().getClientDB ().findTileType (getTreasureReward ().getTileTypeID (), "TreasureUI").getTileTypeDescription ());
			else
				locationDescription = getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (getTreasureReward ().getBuildingID (), "TreasureUI").getBuildingName ());
			
			final StringBuilder text = new StringBuilder (getLanguageHolder ().findDescription
				(getLanguages ().getTreasureScreen ().getText ()).replaceAll ("LOCATION_DESCRIPTION", locationDescription));
			
			if ((getTreasureReward ().getHeroItem ().size () == 0) && (getTreasureReward ().getPick ().size () == 0) && (getTreasureReward ().getResource ().size () == 0) &&
				(getTreasureReward ().getSpellID ().size () == 0) && (getTreasureReward ().getPrisoner ().size () == 0))
				
				text.append (System.lineSeparator () + BULLET_POINT + getLanguageHolder ().findDescription (getLanguages ().getTreasureScreen ().getNothing ()));
			else
			{
				// Books and retorts
				for (final PickAndQuantity pick : getTreasureReward ().getPick ())
				{
					final Pick pickDef = getClient ().getClientDB ().findPick (pick.getPickID (), "TreasureUI");
					
					if (pickDef.getBookImageFile ().size () == 0)
					{
						final String pickDescription = getLanguageHolder ().findDescription (pickDef.getPickDescriptionSingular ());
						text.append (System.lineSeparator () + BULLET_POINT + getLanguageHolder ().findDescription (getLanguages ().getTreasureScreen ().getRetort ()).replaceAll
							("PICK_NAME_SINGULAR", (pickDescription != null) ? pickDescription : pick.getPickID ()) + ";");
					}
					else if (pick.getQuantity () == 1)
					{
						final String pickDescription = getLanguageHolder ().findDescription (pickDef.getPickDescriptionSingular ());
						text.append (System.lineSeparator () + BULLET_POINT + "1 " + ((pickDescription != null) ? pickDescription : pick.getPickID ()) + ";");
					}
					else
					{
						final String pickDescription = getLanguageHolder ().findDescription (pickDef.getPickDescriptionPlural ());
						text.append (System.lineSeparator () + BULLET_POINT + pick.getQuantity () + " " + ((pickDescription != null) ? pickDescription : pick.getPickID ()) + ";");
					}
				}
				
				// Prisoners - mention whether they fit or got bumped outside or lost completely
				for (final TreasureRewardPrisoner prisoner : getTreasureReward ().getPrisoner ())
				{
					final List<LanguageText> languageText;
					switch (prisoner.getUnitAddBumpType ())
					{
						case BUMPED:
							languageText = getLanguages ().getTreasureScreen ().getPrisonerBumped ();
							break;
						case NO_ROOM:
							languageText = getLanguages ().getTreasureScreen ().getPrisonerEscaped ();
							break;
						default:
							languageText = getLanguages ().getTreasureScreen ().getPrisoner ();
					}
					
					String prisonerText = getLanguageHolder ().findDescription (languageText) + ";";
					
					final MemoryUnit mu = getUnitUtils ().findUnitURN (prisoner.getPrisonerUnitURN (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
					if (mu == null)
						prisonerText = prisonerText.replaceAll ("A_UNIT_NAME", "A hero");
					else
					{
						final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (mu, null, null, null,
							getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
						
						getUnitStatsReplacer ().setUnit (xu);
						prisonerText = getUnitStatsReplacer ().replaceVariables (prisonerText);
					}
					text.append (System.lineSeparator () + BULLET_POINT + prisonerText);
				}
				
				// Hero items
				for (final NumberedHeroItem item : getTreasureReward ().getHeroItem ())
					text.append (System.lineSeparator () + BULLET_POINT + item.getHeroItemName () + ";");
				
				// Learn spells
				for (final String spellID : getTreasureReward ().getSpellID ())
				{
					final String spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (spellID, "TreasureUI").getSpellName ());
					
					text.append (System.lineSeparator () + BULLET_POINT + getLanguageHolder ().findDescription (getLanguages ().getTreasureScreen ().getSpell ()).replaceAll
						("SPELL_NAME", (spellName != null) ? spellName : spellID) + ";");
				}
				
				// Gold and mana
				for (final ProductionTypeAndUndoubledValue resource : getTreasureReward ().getResource ())
				{
					final String productionTypeDescription = getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findProductionType (resource.getProductionTypeID (), "TreasureUI").getProductionTypeDescription ());
					text.append (System.lineSeparator () + BULLET_POINT + getTextUtils ().intToStrCommas (resource.getUndoubledProductionValue ()) + " " +
						((productionTypeDescription != null) ? productionTypeDescription : resource.getProductionTypeID ()) + ";");
				}
			}
			
			// Replace final semicolon with a .
			String textString = text.toString ();
			int semicolon = textString.lastIndexOf (';');
			if (semicolon == textString.length () - 1)
				textString = textString.substring (0, textString.length () - 1) + ".";
				
			messageText.setText (textString);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Shortcut keys
		getLanguageHolder ().configureShortcutKeys (contentPane);

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