package momime.client.ui.dialogs;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.SpellBookSectionLang;
import momime.client.language.database.SpellLang;
import momime.client.newturnmessages.NewTurnMessageSpellEx;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.components.UnitRowDisplayButton;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.UnitSkillTypeID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;

/**
 * Popup that displays each of the units in a particular map cell so we can select one.
 * Used for targetting unit enchantments on the overland map, e.g. Endurance.
 */ 
public final class UnitRowDisplayUI extends MomClientDialogUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitRowDisplayUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx unitRowDisplayLayout;

	/** Multiplayer client */
	private MomClient client;
	
	/** Small font */
	private Font smallFont;
	
	/** Medium font */
	private Font mediumFont;
	
	/** UI component factory */
	private UIComponentFactory uiComponentFactory;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;

	/** Client-side spell utils */
	private SpellClientUtils spellClientUtils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Units to display in the list */
	private List<MemoryUnit> units;

	/** NTM about the spell being targetted */
	private NewTurnMessageSpellEx targetSpell;
	
	/** Label showing prompt text */
	private JLabel title;

	/** Labels showing unit names */
	private List<JLabel> unitNames = new ArrayList<JLabel> ();
	
	/** Cancel action */
	private Action cancelAction;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getUnits ().size ());
		
		// Load images
		final BufferedImage top = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitRowDisplayTop.png");
		final BufferedImage bottom = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitRowDisplayBottom.png");
		
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button49x12redNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button49x12redPressed.png");
		
		// Actions
		cancelAction = new LoggingAction ((ev) -> getDialog ().dispose ());
		
		// Initialize the dialog
		final UnitRowDisplayUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				try
				{
					getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
		
		// Initialize the content pane
		final int formHeight = 29 + (getUnits ().size () * 19); 
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (top, 0, 0, null);
				g.drawImage (bottom, 0, formHeight - bottom.getHeight (), null);
			}
		};
		
		final Dimension panelSize = new Dimension (top.getWidth (), formHeight);
		contentPane.setMinimumSize (panelSize);
		contentPane.setMaximumSize (panelSize);
		contentPane.setPreferredSize (panelSize);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getUnitRowDisplayLayout ()));

		title = getUtils ().createLabel (MomUIConstants.AQUA, getMediumFont ());
		contentPane.add (title, "frmUnitRowDisplayTitle");
		
		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmUnitRowDisplayCancel");
		
		// Create controls for each unit
		final Spell spell = getClient ().getClientDB ().findSpell (getTargetSpell ().getSpellID (), "UnitRowDisplayUI");
		
		int row = 0;
		for (final MemoryUnit unit : getUnits ())
		{
			row++;
			
			// Unit image/button
			final Action selectAction = new LoggingAction ((ev) ->
			{
				// Use common routine to do all the validation
				final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
					(spell, null, getClient ().getOurPlayerID (), null, unit,
					getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
				
				if (validTarget == TargetSpellResult.VALID_TARGET)
				{
					final TargetSpellMessage msg = new TargetSpellMessage ();
					msg.setSpellID (getTargetSpell ().getSpellID ());
					msg.setUnitURN (unit.getUnitURN ());
					getClient ().getServerConnection ().sendMessageToServer (msg);
					
					// Close out this window and the "Target Spell" right hand panel
					getOverlandMapProcessing ().updateMovementRemaining ();
					getDialog ().dispose ();
				}
				else if (validTarget.getUnitLanguageEntryID () != null)
				{
					final SpellLang spellLang = getLanguage ().findSpell (getTargetSpell ().getSpellID ());
					final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
					
					String text = getLanguage ().findCategoryEntry ("SpellTargetting", validTarget.getUnitLanguageEntryID ()).replaceAll
						("SPELL_NAME", (spellName != null) ? spellName : getTargetSpell ().getSpellID ());
					
					// If spell can only be targetted on specific magic realm/lifeform types, the list them
					if (validTarget == TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE)
						text = text + getSpellClientUtils ().listValidMagicRealmLifeformTypeTargetsOfSpell (spell);
					
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setTitleLanguageCategoryID ("SpellTargetting");
					msg.setTitleLanguageEntryID ("Title");
					msg.setText (text);
					msg.setVisible (true);												
				}
			});

			final UnitRowDisplayButton button = getUiComponentFactory ().createUnitRowDisplayButton ();
			button.setAction (selectAction);
			button.setUnit (unit);
			button.init ();
			contentPane.add (button, "frmUnitRowUnit" + row + "Button");
			
			// Unit name
			final JLabel unitName = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
			contentPane.add (unitName, "frmUnitRowUnit" + row + "Name");			
			unitNames.add (unitName);
			
			// There's space on the form for up to 6 unit attributes
			final List<String> unitAttributeIDs = new ArrayList<String> ();
			for (final UnitSkill thisSkill : getClient ().getClientDB ().getUnitSkills ())
				if (getGraphicsDB ().findUnitSkill (thisSkill.getUnitSkillID (), "UnitRowDisplayUI").getUnitSkillTypeID () == UnitSkillTypeID.ATTRIBUTE)
					unitAttributeIDs.add (thisSkill.getUnitSkillID ());
			
			final List<UnitSkillAndValue> mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), unit, getClient ().getClientDB ());
			
			for (int attrNo = 1; attrNo <= 6; attrNo++)
			{
				final String unitAttributeID = unitAttributeIDs.get (attrNo-1);
				final int attrValue = getUnitSkillUtils ().getModifiedSkillValue (unit, mergedSkills, unitAttributeID, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
					null, null, getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
				
				if (attrValue > 0)
				{
					// Show number and icon for this unit attribute
					contentPane.add (getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont (), new Integer (attrValue).toString ()),
						"frmUnitRowUnit" + row + "Attribute" + attrNo + "Value");
					
					final BufferedImage attributeImage = getUnitClientUtils ().getUnitSkillComponentBreakdownIcon (unit, unitAttributeID); 
					if (attributeImage != null)
						contentPane.add (getUtils ().createImage (attributeImage), "frmUnitRowUnit" + row + "Attribute" + attrNo + "Icon");
				}
			}
			
			// There's space on the form for up to 12 unit skills
			int skillNo = 0;
			
			for (final UnitSkillAndValue thisSkill : mergedSkills)
				if (getGraphicsDB ().findUnitSkill (thisSkill.getUnitSkillID (), "UnitRowDisplayUI").getUnitSkillTypeID () != UnitSkillTypeID.ATTRIBUTE)
					if (skillNo < 12)
					{
						final BufferedImage skillImage = getUnitClientUtils ().getUnitSkillSingleIcon (unit, thisSkill.getUnitSkillID ());
						if (skillImage != null)
						{
							skillNo++;
							contentPane.add (getUtils ().createImage (skillImage), "frmUnitRowUnit" + row + "Skill" + skillNo);
						}
					}			
		}
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		
		log.trace ("Exiting init");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged: " + getUnits ().size ());
		
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmUnitRowDisplay", "Cancel"));
		
		try
		{
			// Work out the title prompt to use
			final SpellLang spellLang = getLanguage ().findSpell (getTargetSpell ().getSpellID ());
			final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
			
			final Spell spell = getClient ().getClientDB ().findSpell (getTargetSpell ().getSpellID (), "UnitRowDisplayUI");
			final SpellBookSectionLang section = getLanguage ().findSpellBookSection (spell.getSpellBookSectionID ());
			final String target = (section != null) ? section.getSpellTargetPrompt () : null;
			
			title.setText ((target == null) ? ("Select target of type " + spell.getSpellBookSectionID ()) :
				(target.replaceAll ("SPELL_NAME", (spellName != null) ? spellName : getTargetSpell ().getSpellID ())));
		
			// Unit names
			for (int unitNo = 0; unitNo < getUnits ().size (); unitNo++)
			{
				final MemoryUnit unit = getUnits ().get (unitNo);
				final JLabel unitName = unitNames.get (unitNo);
				
				unitName.setText (getUnitClientUtils ().getUnitName (unit, UnitNameType.RACE_UNIT_NAME));
			}
		}
		catch (final RecordNotFoundException e)
		{
			log.error (e, e);
		}
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getUnitRowDisplayLayout ()
	{
		return unitRowDisplayLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setUnitRowDisplayLayout (final XmlLayoutContainerEx layout)
	{
		unitRowDisplayLayout = layout;
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
	 * @return UI component factory
	 */
	public final UIComponentFactory getUiComponentFactory ()
	{
		return uiComponentFactory;
	}

	/**
	 * @param factory UI component factory
	 */
	public final void setUiComponentFactory (final UIComponentFactory factory)
	{
		uiComponentFactory = factory;
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
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
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
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
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
	 * @return Units to display in the list
	 */
	public final List<MemoryUnit> getUnits ()
	{
		return units;
	}
	
	/**
	 * @param list Units to display in the list
	 */
	public final void setUnits (final List<MemoryUnit> list)
	{
		units = list;
	}

	/**
	 * @return NTM about the spell being targetted
	 */
	public final NewTurnMessageSpellEx getTargetSpell ()
	{
		return targetSpell;
	}
	
	/**
	 * @param msg NTM about the spell being targetted
	 */
	public final void setTargetSpell (final NewTurnMessageSpellEx msg)
	{
		targetSpell = msg;
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
}