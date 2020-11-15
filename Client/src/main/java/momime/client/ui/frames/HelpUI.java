package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsHorizontalFill;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.replacer.SpringExpressionReplacer;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.MomUIConstants;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.common.database.AnimationGfx;
import momime.common.database.CitySpellEffect;
import momime.common.database.CityViewElement;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemSlotType;
import momime.common.database.HeroSlotAllowedItemType;
import momime.common.database.LanguageText;
import momime.common.database.Pick;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSkill;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

/**
 * Scroll that displays help text when we right click on various elements in the game
 */
public final class HelpUI extends MomClientFrameUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (HelpUI.class);

	/** Typical inset used by this layout */
	private final static int NO_INSET = 0;
	
	/** XML layout */
	private XmlLayoutContainerEx newTurnMessagesLayout;
	
	/** Large font */
	private Font largeFont;

	/** Small font */
	private Font smallFont;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Client-side spell utils */
	private SpellClientUtils spellClientUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Replacer for evaluating EL expressions in help text */
	private SpringExpressionReplacer springExpressionReplacer;
	
	/** Title */
	private JLabel title;
	
	/** Image in top left corner */
	private JLabel imageLabel;
	
	/** Intended text to the right of the image (left blank if there's no image) */
	private JTextPane indentedText;
	
	/** Unintended text underneath the image */
	private JTextPane unindentedText;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** Pick ID we're displaying help text about, null if displaying help text about something other than a pick */
	private String pickID;
	
	/** Unit skill ID we're displaying help text about, null if displaying help text about something other than a unit skill */
	private String unitSkillID;

	/** Unit whose skills or attributes we're displaying help text about, null if displaying help text about something other than a unit skill or attribute */
	private ExpandedUnitDetails unit;

	/** Combat area effect ID we're displaying help text about, null if displaying help text about something other than a combat area effect */
	private String combatAreaEffectID;
	
	/** Spell ID we're displaying help text about, null if displaying help text about something other than a spell */
	private String spellID;
	
	/** City spell effect ID we're displaying help text about, null if displaying help text about something other than a city spell effect */
	private String citySpellEffectID;
	
	/** Hero item slot type ID we're displaying help text about, null if displaying help text about something other than a hero item slot */
	private String heroItemSlotTypeID;

	/** Player who owns the spell or city spell effect we're displaying help text about, null if displaying help text about something other than a spell or city spell effect*/
	private PlayerPublicDetails castingPlayer;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/background.png");
		final BufferedImage roller = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/position3-0.png");
		final BufferedImage closeButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/closeButtonNormal.png");
		final BufferedImage closeButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/closeButtonPressed.png");
		
		final int backgroundTop = roller.getHeight () - NewTurnMessagesUI.SCROLL_OVERLAP_TOP;
		final int backgroundLeft = (roller.getWidth () - background.getWidth ()) / 2;
		final int bottomRollerTop = backgroundTop + background.getHeight () - NewTurnMessagesUI.SCROLL_OVERLAP_BOTTOM;
		final int bottomRollerBottom = bottomRollerTop + roller.getHeight ();

		// Actions
		final Action closeAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (background, backgroundLeft, backgroundTop, null);
				
				g.drawImage (roller, 0, 0, null);
				g.drawImage (roller, 0, bottomRollerTop, null);
			}
		};
		
		contentPane.setBackground (Color.BLACK);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getNewTurnMessagesLayout ()));

		contentPane.add (getUtils ().createImageButton (closeAction, null, null, null, closeButtonNormal, closeButtonPressed, closeButtonNormal), "frmNewTurnMessagesClose");
		
		// The area of the scroll that we write in uses a 2x3 GridBagLayout so we can shift the text around according to the
		// size of the image, or if there isn't an image at all then the top and left cells have zero size.
		final JPanel gridPanel = new JPanel (new GridBagLayout ());
		gridPanel.setOpaque (false);
		
		title = getUtils ().createLabel (MomUIConstants.DARK_RED, getLargeFont ());
		gridPanel.add (title, getUtils ().createConstraintsNoFill (0, 0, 2, 1, new Insets (0, 6, 0, 0), GridBagConstraintsNoFill.WEST));
		
		imageLabel = new JLabel ();
		gridPanel.add (imageLabel, getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, 6, 0, 0), GridBagConstraintsNoFill.NORTHWEST));
		
		indentedText = new JTextPane ();
		indentedText.setFont (getSmallFont ());
		indentedText.setForeground (MomUIConstants.DARK_BROWN);
		indentedText.setBackground (MomUIConstants.TRANSPARENT);
		indentedText.setOpaque (false);
		indentedText.setEditable (false);
		gridPanel.add (indentedText, getUtils ().createConstraintsHorizontalFill (1, 1, 1, 1, NO_INSET, GridBagConstraintsHorizontalFill.NORTH));
		
		unindentedText = new JTextPane ();
		unindentedText.setFont (getSmallFont ());
		unindentedText.setForeground (MomUIConstants.DARK_BROWN);
		unindentedText.setBackground (MomUIConstants.TRANSPARENT);
		unindentedText.setContentType ("text/html");
		unindentedText.setOpaque (false);
		unindentedText.setEditable (false);
		
		final JScrollPane unindentedTextScroll = getUtils ().createTransparentScrollPane (unindentedText);
		unindentedTextScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		gridPanel.add (unindentedTextScroll, getUtils ().createConstraintsBothFill (0, 2, 2, 1, NO_INSET));
		
		// Force grid to correct size
		final XmlLayoutComponent gridSize = getNewTurnMessagesLayout ().findComponent ("frmNewTurnMessagesList");
		gridPanel.add (Box.createRigidArea (new Dimension (gridSize.getWidth (), 0)),
			getUtils ().createConstraintsNoFill (0, 3, 2, 1, NO_INSET, GridBagConstraintsNoFill.CENTRE));

		gridPanel.add (Box.createRigidArea (new Dimension (0, gridSize.getHeight ())),
			getUtils ().createConstraintsNoFill (2, 0, 1, 3, NO_INSET, GridBagConstraintsNoFill.CENTRE));
		
		contentPane.add (gridPanel, "frmNewTurnMessagesList");
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);

		// Trying to get the shape of this right is very long and complicated, and impossible to get perfect because the edges are all jagged,
		// so this is just as close as I can get it.  It starts from the top-right corner under the roller and proceeds clockwise.
		
		// Also note the rollers are lopsided, with 7 pixels above them but only 6 below
		getFrame ().setShape (new Polygon
			(new int [] {backgroundLeft + background.getWidth (), backgroundLeft + background.getWidth (),
					
				// Bottom-right roller
				backgroundLeft + background.getWidth () + 2, getNewTurnMessagesLayout ().getFormWidth () - 29, getNewTurnMessagesLayout ().getFormWidth () - 22, getNewTurnMessagesLayout ().getFormWidth () - 16, getNewTurnMessagesLayout ().getFormWidth () - 7, getNewTurnMessagesLayout ().getFormWidth (), getNewTurnMessagesLayout ().getFormWidth (), getNewTurnMessagesLayout ().getFormWidth () - 7, getNewTurnMessagesLayout ().getFormWidth () - 16, getNewTurnMessagesLayout ().getFormWidth () - 22, getNewTurnMessagesLayout ().getFormWidth () - 29, backgroundLeft + background.getWidth () + 2,
				
				// Bottom edge incl. close button
				backgroundLeft + background.getWidth (), 428, 431, 431, 428, 428, 432, 412, 410, 410, 413, 413, 410, backgroundLeft,
					
				// Bottom-left roller
				backgroundLeft - 2, 29, 22, 16, 7, 0, 0, 7, 16, 22, 29, backgroundLeft - 2,
					
				// Left edge
				backgroundLeft, backgroundLeft,
					
				// Top-left roller
				backgroundLeft - 2, 29, 22, 16, 7, 0, 0, 7, 16, 22, 29, backgroundLeft - 2,
				
				// Top edge
				backgroundLeft, backgroundLeft + background.getWidth (),
					
				// Top-right roller
				backgroundLeft + background.getWidth () + 2, getNewTurnMessagesLayout ().getFormWidth () - 29, getNewTurnMessagesLayout ().getFormWidth () - 22, getNewTurnMessagesLayout ().getFormWidth () - 16, getNewTurnMessagesLayout ().getFormWidth () - 7, getNewTurnMessagesLayout ().getFormWidth (), getNewTurnMessagesLayout ().getFormWidth (), getNewTurnMessagesLayout ().getFormWidth () - 7, getNewTurnMessagesLayout ().getFormWidth () - 16, getNewTurnMessagesLayout ().getFormWidth () - 22, getNewTurnMessagesLayout ().getFormWidth () - 29, backgroundLeft + background.getWidth () + 2},
					
			new int [] {backgroundTop, backgroundTop + background.getHeight (),
						
				// Bottom-right roller
				bottomRollerTop, bottomRollerTop, bottomRollerTop + 9, bottomRollerTop + 10, bottomRollerTop + 6, bottomRollerTop + 14, bottomRollerBottom - 14, bottomRollerBottom - 6, bottomRollerBottom - 10, bottomRollerBottom - 9, bottomRollerBottom, bottomRollerBottom,

				// Bottom edge incl. close button
				bottomRollerBottom - 6, bottomRollerBottom - 6, bottomRollerBottom - 3, bottomRollerBottom + 2, bottomRollerBottom + 5, getNewTurnMessagesLayout ().getFormHeight () - 7, getNewTurnMessagesLayout ().getFormHeight (), getNewTurnMessagesLayout ().getFormHeight (), getNewTurnMessagesLayout ().getFormHeight () - 7, bottomRollerBottom + 5, bottomRollerBottom + 2, bottomRollerBottom - 3, bottomRollerBottom - 6, bottomRollerBottom - 6,
				
				// Bottom-left roller
				bottomRollerBottom, bottomRollerBottom, bottomRollerBottom - 9, bottomRollerBottom - 10, bottomRollerBottom - 6, bottomRollerBottom - 14, bottomRollerTop + 14, bottomRollerTop + 6, bottomRollerTop + 10, bottomRollerTop + 9, bottomRollerTop, bottomRollerTop,
					
				// Left edge
				backgroundTop + background.getHeight (), backgroundTop,
					
				// Top-left roller
				roller.getHeight (), roller.getHeight (), roller.getHeight () - 9, roller.getHeight () - 10, roller.getHeight () - 6, roller.getHeight () - 14, 14, 6, 10, 9, 0, 0,

				// Top edge
				7, 7,
				
				// Top-right roller
				0, 0, 9, 10, 6, 14, roller.getHeight () - 14, roller.getHeight () - 6, roller.getHeight () - 10, roller.getHeight () - 9, roller.getHeight (), roller.getHeight ()},
					
			68));
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getTitle ()));
		
		String text = null;		// unindentedText
		try
		{
			if (pickID != null)
			{
				final Pick pick = getClient ().getClientDB ().findPick (pickID, "HelpUI");
				final String pickTitle = getLanguageHolder ().findDescription (pick.getPickDescriptionSingular ());
				final String pickHelpText = getLanguageHolder ().findDescription (pick.getPickHelpText ());
				title.setText (pickTitle);
				indentedText.setText (pickHelpText);
			}
			else if (unitSkillID != null)
			{
				getUnitStatsReplacer ().setUnit (unit);
				
				final UnitSkill unitSkill = getClient ().getClientDB ().findUnitSkill (unitSkillID, "HelpUI");
				final String unitSkillTitle = getLanguageHolder ().findDescription (unitSkill.getUnitSkillDescription ());
				final String unitSkillHelpText = getLanguageHolder ().findDescription (unitSkill.getUnitSkillHelpText ());
				title.setText ((unitSkillTitle != null) ? getUnitStatsReplacer ().replaceVariables (unitSkillTitle) : unitSkillID);
				
				// If the icons are included in the help text, then don't indent it as well (for unit attributes)
				if ((unitSkillHelpText != null) && (unitSkillHelpText.contains ("#{")))
					text = unitSkillHelpText;
				else
				{
					indentedText.setText ((unitSkillHelpText != null) ? getUnitStatsReplacer ().replaceVariables (unitSkillHelpText) : unitSkillID);
				
					// If this unit skill is the result of a spell, show how much upkeep it is costing
					if (unit.isMemoryUnit ())
					{
						final MemoryMaintainedSpell spell = getMemoryMaintainedSpellUtils ().findMaintainedSpell
							(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
							null, null, unit.getUnitURN (), unitSkillID, null, null);
						if (spell != null)
						{
							text = indentedText.getText ();
							
							final Spell spellDef = getClient ().getClientDB ().findSpell (spell.getSpellID (), "HelpUI");
							final PlayerPublicDetails thisPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), spell.getCastingPlayerID (), "HelpUI");
							final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
							indentedText.setText (getSpellClientUtils ().listUpkeepsOfSpell (spellDef, pub.getPick ()));
						}
					}
				}
			}
			else if (citySpellEffectID != null)
			{
				final CitySpellEffect effect = getClient ().getClientDB ().findCitySpellEffect (citySpellEffectID, "HelpUI");
				final String effectTitle = getLanguageHolder ().findDescription (effect.getCitySpellEffectName ());
				final String effectHelpText = getLanguageHolder ().findDescription (effect.getCitySpellEffectHelpText ());
				title.setText (effectTitle);
				text = effectHelpText;
				
				// City spell effects *must* be the result of a spell, so we should already know the spellID and who cast it
				final Spell spellDef = getClient ().getClientDB ().findSpell (spellID, "HelpUI");
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) castingPlayer.getPersistentPlayerPublicKnowledge ();
				indentedText.setText (getSpellClientUtils ().listUpkeepsOfSpell (spellDef, pub.getPick ()));
			}
			else if (combatAreaEffectID != null)
			{
				final CombatAreaEffect cae = getClient ().getClientDB ().findCombatAreaEffect (combatAreaEffectID, "HelpUI");
				final String caeTitle = getLanguageHolder ().findDescription (cae.getCombatAreaEffectDescription ());
				final String caeHelpText = getLanguageHolder ().findDescription (cae.getCombatAreaEffectHelpText ());
				title.setText (caeTitle);
				indentedText.setText (caeHelpText);
			}
			else if (heroItemSlotTypeID != null)
			{
				final StringBuilder description = new StringBuilder ();
				description.append (getLanguageHolder ().findDescription (getLanguages ().getHeroItemInfoScreen ().getItemSlotHelpTextPrefix ()));
				
				// List all the item types that can go into this slot
				final HeroItemSlotType heroItemSlotType = getClient ().getClientDB ().findHeroItemSlotType (heroItemSlotTypeID, "HelpUI");
				title.setText (getLanguageHolder ().findDescription (heroItemSlotType.getSlotTypeDescription ()));

				for (final HeroSlotAllowedItemType allowed : heroItemSlotType.getHeroSlotAllowedItemType ())
					description.append (System.lineSeparator () + "\u2022 " + getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findHeroItemType (allowed.getHeroItemTypeID (), "HelpUI").getHeroItemTypeDescription ()));
				
				indentedText.setText (description.toString ());
			}
			else if (spellID != null)
			{
				final Spell spell = getClient ().getClientDB ().findSpell (spellID, "HelpUI");
				final String spellTitle = getLanguageHolder ().findDescription (spell.getSpellName ());
				final String spellHelpText = getLanguageHolder ().findDescription (spell.getSpellHelpText ());
				title.setText (spellTitle);
				text = spellHelpText;
				
				// Show all the spell stats (research and casting cost and so on) at the top
				final Spell spellDef = getClient ().getClientDB ().findSpell (spellID, "HelpUI");
				final StringBuilder spellStats = new StringBuilder ();
				
				// Spell book section
				if (spellDef.getSpellBookSectionID () != null)
				{
					final String sectionName = getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findSpellBookSection (spellDef.getSpellBookSectionID (), "HelpUI").getSpellBookSectionName ());
					
					spellStats.append (getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellBookSection ()).replaceAll
						("SPELL_BOOK_SECTION", sectionName));
				}
				
				// Research cost
				if (spellDef.getResearchCost () != null)
				{
					final String researchSuffix = getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "HelpUI").getProductionTypeSuffix ());
					if ((castingPlayer == null) || (!castingPlayer.getPlayerDescription ().getPlayerID ().equals (getClient ().getOurPlayerID ())))
						
						// Someone else's spell, so don't show any details about research status
						spellStats.append (System.lineSeparator () + getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellBookResearchCostNotOurs ()).replaceAll
							("RESEARCH_TOTAL", getTextUtils ().intToStrCommas (spellDef.getResearchCost ())).replaceAll
							("PRODUCTION_TYPE", researchSuffix));
					else
					{
						// Our spell - find research status
						final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (), spellID);
						final List<LanguageText> languageText;
						if (researchStatus.getStatus () == SpellResearchStatusID.AVAILABLE)
							languageText = getLanguages ().getHelpScreen ().getSpellBookResearchCostResearched ();
						else if (researchStatus.getRemainingResearchCost () == spellDef.getResearchCost ())
							languageText = getLanguages ().getHelpScreen ().getSpellBookResearchCostNotStarted ();
						else
							languageText = getLanguages ().getHelpScreen ().getSpellBookResearchCostPartial ();

						spellStats.append (System.lineSeparator () + getLanguageHolder ().findDescription (languageText).replaceAll
							("RESEARCH_TOTAL", getTextUtils ().intToStrCommas (spellDef.getResearchCost ())).replaceAll
							("PRODUCTION_TYPE", (researchSuffix != null) ? researchSuffix : CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH).replaceAll
							("RESEARCH_SO_FAR", getTextUtils ().intToStrCommas (spellDef.getResearchCost () - researchStatus.getRemainingResearchCost ())));
					}
				}
				
				// Overland casting cost
				final MomPersistentPlayerPublicKnowledge castingPub = (castingPlayer == null) ? null :
					(MomPersistentPlayerPublicKnowledge) castingPlayer.getPersistentPlayerPublicKnowledge ();

				final String manaSuffix = getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "HelpUI").getProductionTypeSuffix ());
								
				// Item creation spells can be cast overland but have no defined cost - so don't use "spellCanBeCastIn" for this
				if (spellDef.getOverlandCastingCost () != null)
				{
					final int reducedCastingCost;
					if (castingPub == null)
						reducedCastingCost = spellDef.getOverlandCastingCost ();		// No info on caster's picks, so just assume no reduction
					else
						reducedCastingCost = getSpellUtils ().getReducedOverlandCastingCost (spellDef, null, castingPub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
					
					final List<LanguageText> languageText = (spellDef.getOverlandCastingCost () == reducedCastingCost) ?
						getLanguages ().getHelpScreen ().getSpellBookOverlandCostFull () : getLanguages ().getHelpScreen ().getSpellBookOverlandCostReduced ();
					
					spellStats.append (System.lineSeparator () + getLanguageHolder ().findDescription (languageText).replaceAll
						("PRODUCTION_TYPE", (manaSuffix != null) ? manaSuffix : CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA).replaceAll
						("FULL_CASTING_COST", getTextUtils ().intToStrCommas (spellDef.getOverlandCastingCost ())).replaceAll
						("REDUCED_CASTING_COST", getTextUtils ().intToStrCommas (reducedCastingCost)));
				}
				
				// Combat casting cost
				if (getSpellUtils ().spellCanBeCastIn (spellDef, SpellCastType.COMBAT))
				{
					final int reducedCastingCost;
					if (castingPub == null)
						reducedCastingCost = spellDef.getCombatCastingCost ();		// No info on caster's picks, so just assume no reduction
					else
						reducedCastingCost = getSpellUtils ().getReducedCombatCastingCost (spellDef, castingPub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
					
					final List<LanguageText> languageText = (spellDef.getCombatCastingCost () == reducedCastingCost) ?
						getLanguages ().getHelpScreen ().getSpellBookCombatCostFull () : getLanguages ().getHelpScreen ().getSpellBookCombatCostReduced ();
					
					spellStats.append (System.lineSeparator () + getLanguageHolder ().findDescription (languageText).replaceAll
						("PRODUCTION_TYPE", (manaSuffix != null) ? manaSuffix : CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA).replaceAll
						("FULL_CASTING_COST", getTextUtils ().intToStrCommas (spellDef.getCombatCastingCost ())).replaceAll
						("REDUCED_CASTING_COST", getTextUtils ().intToStrCommas (reducedCastingCost)));
				}
				
				// Saving throw modifier?
				if (spellDef.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES)
					spellStats.append (System.lineSeparator () + getSpellClientUtils ().listSavingThrowsOfSpell (spellDef));
				
				// Upkeep
				final String upkeep = getSpellClientUtils ().listUpkeepsOfSpell (spellDef, (castingPub == null) ? null : castingPub.getPick ());
				if (upkeep != null)
					spellStats.append (System.lineSeparator () + upkeep);
				
				indentedText.setText (spellStats.toString ());
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Only show the indentedText if it has some text in it, otherwise it takes up space on help pages like
		// unit attributes which only have unindentedText
		indentedText.setVisible (!"".equals (indentedText.getText ()));
		
		// Convert to HTML
		if (text != null)
		{
			unindentedText.setText ("<html><body>" +
				getSpringExpressionReplacer ().replaceVariables (text.replaceAll ("\\r\\n|\\r|\\n", "<br/>")) +
				"</body></html>");
			
			unindentedText.setCaretPosition (0);		// Scroll to the top
		}
		
		// The text and images resize themselves according to the size of the text, so force everything to reposition and redraw itself
		contentPane.validate ();
		contentPane.repaint ();

		log.trace ("Exiting languageChanged");
	}	

	/**
	 * Clears all identifiers, images and text - ready to show new help text
	 * @throws IOException If a resource cannot be found
	 */
	private final void clear () throws IOException
	{
		log.trace ("Entering clear");

		// Force form to be initialized
		if (!isVisible ())
			setVisible (false);
		
		// Clear fields
		imageLabel.setIcon (null);
		imageLabel.setVisible (false);
		title.setText (null);
		indentedText.setText (null);
		indentedText.setVisible (false);
		unindentedText.setText (null);
		
		// Clear identifiers
		pickID = null;
		unitSkillID = null;
		unit = null;
		spellID = null;
		castingPlayer = null;
		citySpellEffectID = null;
		combatAreaEffectID = null;
		heroItemSlotTypeID = null;

		log.trace ("Exiting clear");
	}
	
	/**
	 * @param id Pick ID to display help text about
	 * @throws IOException If a resource cannot be found
	 */
	public final void showPickID (final String id) throws IOException
	{
		log.trace ("Entering showPickID: " + id);

		clear ();
		pickID = id;
		
		// Look for any images for this pickID
		final Pick pick = getClient ().getClientDB ().findPick (pickID, "showPickID");
		if (pick.getBookImageFile ().size () > 0)
		{
			// Merge the images into one
			int totalWidth = 0;
			int maxHeight = 0;
			for (final String bookImage : pick.getBookImageFile ())
			{
				final BufferedImage image = getUtils ().loadImage (bookImage);
				totalWidth = totalWidth + image.getWidth ();
				maxHeight = Math.max (maxHeight, image.getHeight ());
			}
			
			final BufferedImage mergedImage = new BufferedImage (totalWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = mergedImage.createGraphics ();
			try
			{
				int x = 0;
				for (final String bookImage : pick.getBookImageFile ())
				{
					final BufferedImage image = getUtils ().loadImage (bookImage);
					g.drawImage (image, x, maxHeight - image.getHeight (), null);
					x = x + image.getWidth ();
				}
			}
			finally
			{
				g.dispose ();
			}
			
			imageLabel.setIcon (new ImageIcon (mergedImage));
			imageLabel.setVisible (true);
		}
		
		languageChanged ();
		setVisible (true);

		log.trace ("Exiting showPickID");
	}
	
	/**
	 * @param id Unit Skill ID to display help text about
	 * @param u Unit who owns the skill
	 * @throws IOException If a resource cannot be found
	 */
	public final void showUnitSkillID (final String id, final ExpandedUnitDetails u) throws IOException
	{
		log.trace ("Entering showUnitSkillID: " + id + ", " + u.getUnitID ());

		clear ();
		unitSkillID = id;
		unit = u;

		final BufferedImage image = getUnitClientUtils ().getUnitSkillSingleIcon (unit, unitSkillID);
		if (image != null)
		{
			imageLabel.setIcon (new ImageIcon (image));
			imageLabel.setVisible (true);
		}
		
		languageChanged ();
		setVisible (true);

		log.trace ("Exiting showUnitSkillID");
	}
	
	/**
	 * @param id Combat area effect ID to display help text about
	 * @throws IOException If a resource cannot be found
	 */
	public final void showCombatAreaEffectID (final String id) throws IOException
	{
		log.trace ("Entering showCombatAreaEffectID: " + id);

		clear ();
		combatAreaEffectID = id;

		final CombatAreaEffect cae = getClient ().getClientDB ().findCombatAreaEffect (combatAreaEffectID, "showCombatAreaEffectID");
		imageLabel.setIcon (new ImageIcon (getUtils ().loadImage (cae.getCombatAreaEffectImageFile ())));
		imageLabel.setVisible (true);
		
		languageChanged ();
		setVisible (true);

		log.trace ("Exiting showCombatAreaEffectID");
	}

	/**
	 * @param id City spell effect ID to display help text about
	 * @param aSpellID Spell ID that resulted in this effect
	 * @param player Player who owns the spell
	 * @throws IOException If a resource cannot be found
	 */
	public final void showCitySpellEffectID (final String id, final String aSpellID, final PlayerPublicDetails player) throws IOException
	{
		log.trace ("Entering showCitySpellEffectID: " + id);

		clear ();
		citySpellEffectID = id;
		spellID = aSpellID;
		castingPlayer = player;

		final CityViewElement cityViewElement = getClient ().getClientDB ().findCityViewElementSpellEffect (citySpellEffectID);
		if (cityViewElement != null)
		{
			String imageFilename = null;
			if (cityViewElement.getCityViewAlternativeImageFile () != null)
				imageFilename = cityViewElement.getCityViewAlternativeImageFile ();
			else if (cityViewElement.getCityViewImageFile () != null)
				imageFilename = cityViewElement.getCityViewImageFile ();
			else if (cityViewElement.getCityViewAnimation () != null)
			{
				// Just pick the first animation frame.  Sure it'd be nice to actually have the animations displayed in
				// the help scrolls and anywhere else this is used, but it complicates things enormously having to
				// set up repaint timers, and there's only a handful of effects this actually affects
				// e.g. Dark Rituals, Altar of Battle
				final AnimationGfx anim = getClient ().getClientDB ().findAnimation (cityViewElement.getCityViewAnimation (), "showCitySpellEffectID");
				if (anim.getFrame ().size () > 0)
					imageFilename = anim.getFrame ().get (0);
			}
		
			if (imageFilename != null)
			{
				imageLabel.setIcon (new ImageIcon (getUtils ().loadImage (imageFilename)));
				imageLabel.setVisible (true);
			}
		}
		
		languageChanged ();
		setVisible (true);

		log.trace ("Exiting showCitySpellEffectID");
	}
	
	/**
	 * @param id Spell ID to display help text about
	 * @param player Player who owns the spell
	 * @throws IOException If a resource cannot be found
	 */
	public final void showSpellID (final String id, final PlayerPublicDetails player) throws IOException
	{
		log.trace ("Entering showSpellID: " + id);

		clear ();
		spellID = id;
		castingPlayer = player;

		final Image image = getSpellClientUtils ().findImageForSpell (spellID, (player == null) ? null : player.getPlayerDescription ().getPlayerID ());
		if (image != null)
		{
			imageLabel.setIcon (new ImageIcon (image));
			imageLabel.setVisible (true);
		}
		
		languageChanged ();
		setVisible (true);

		log.trace ("Exiting showUnitSkillID");
	}
	
	/**
	 * @param id Hero item slot type ID to display help text about
	 * @throws IOException If a resource cannot be found
	 */
	public final void showHeroItemSlotTypeID (final String id) throws IOException
	{
		log.trace ("Entering showHeroItemSlotTypeID: " + id);

		clear ();
		heroItemSlotTypeID = id;

		final HeroItemSlotType slotType = getClient ().getClientDB ().findHeroItemSlotType (heroItemSlotTypeID, "showHeroItemSlotTypeID");
		imageLabel.setIcon (new ImageIcon (getUtils ().loadImage (slotType.getHeroItemSlotTypeImageFileWithBackground ())));
		imageLabel.setVisible (true);
		
		languageChanged ();
		setVisible (true);

		log.trace ("Exiting showHeroItemSlotTypeID");
	}

	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getNewTurnMessagesLayout ()
	{
		return newTurnMessagesLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setNewTurnMessagesLayout (final XmlLayoutContainerEx layout)
	{
		newTurnMessagesLayout = layout;
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
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtil MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtil)
	{
		memoryMaintainedSpellUtils = spellUtil;
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
	 * @return Replacer for evaluating EL expressions in help text
	 */
	public final SpringExpressionReplacer getSpringExpressionReplacer ()
	{
		return springExpressionReplacer;
	}

	/**
	 * @param replacer Replacer for evaluating EL expressions in help text
	 */
	public final void setSpringExpressionReplacer (final SpringExpressionReplacer replacer)
	{
		springExpressionReplacer = replacer;
	}
}