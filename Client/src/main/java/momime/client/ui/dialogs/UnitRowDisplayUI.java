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

import com.ndg.utils.swing.actions.LoggingAction;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.components.UnitRowDisplayButton;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.LanguageText;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSection;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillTypeID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellTargetingUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;

/**
 * Popup that displays each of the units in a particular map cell so we can select one.
 * Used for targeting unit enchantments on the overland map, e.g. Endurance.
 */ 
public final class UnitRowDisplayUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (UnitRowDisplayUI.class);
	
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

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;

	/** Client-side spell utils */
	private SpellClientUtils spellClientUtils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Combat UI */
	private CombatUI combatUI;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Units to display in the list */
	private List<MemoryUnit> units;

	/** The spell being targeted */
	private String targetSpellID;
	
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
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
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
		final Spell spell = getClient ().getClientDB ().findSpell (getTargetSpellID (), "UnitRowDisplayUI");
		
		int row = 0;
		for (final MemoryUnit unit : getUnits ())
		{
			row++;
			
			// Unit image/button
			final Action selectAction = new LoggingAction ((ev) ->
			{
				// If its a raise dead-type spell in combat then we've picked the unit, now pick where to bring it back
				if ((spell.getResurrectedHealthPercentage () != null) && (getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)))
				{
					// We've picked the unit, now pick where to bring it back
					getCombatUI ().setSpellBeingTargeted (spell);
					getCombatUI ().setUnitBeingRaised (unit);
					getDialog ().dispose ();
				}
				else
				{
					// Its a normal unit enchantment being cast on the overland map
					// Use common routine to do all the validation
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, spell.getSpellRealm (),
						getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					final TargetSpellResult validTarget = getSpellTargetingUtils ().isUnitValidTargetForSpell
						(spell, null, null, null, getClient ().getOurPlayerID (), null, null, xu, true,
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar (), getClient ().getPlayers (), getClient ().getClientDB ());
					
					if (validTarget == TargetSpellResult.VALID_TARGET)
					{
						final TargetSpellMessage msg = new TargetSpellMessage ();
						msg.setSpellID (getTargetSpellID ());
						msg.setOverlandTargetUnitURN (unit.getUnitURN ());
						getClient ().getServerConnection ().sendMessageToServer (msg);
						
						// Close out this window and the "Target Spell" right hand panel
						getOverlandMapProcessing ().updateMovementRemaining ();
						getDialog ().dispose ();
					}
					else
					{
						final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
						
						String text = getLanguageHolder ().findDescription (getLanguages ().getSpellTargeting ().getUnitLanguageText (validTarget)).replaceAll
							("SPELL_NAME", (spellName != null) ? spellName : getTargetSpellID ());
						
						// If spell can only be targeted on specific magic realm/lifeform types, the list them
						if (validTarget == TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE)
							text = text + getSpellClientUtils ().listValidMagicRealmLifeformTypeTargetsOfSpell (spell);
						
						final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
						msg.setLanguageTitle (getLanguages ().getSpellTargeting ().getTitle ());
						msg.setText (text);
						msg.setVisible (true);												
					}
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
			for (final UnitSkillEx thisSkill : getClient ().getClientDB ().getUnitSkills ())
				if (thisSkill.getUnitSkillTypeID () == UnitSkillTypeID.ATTRIBUTE)
					unitAttributeIDs.add (thisSkill.getUnitSkillID ());

			final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, spell.getSpellRealm (),
				getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
			
			for (int attrNo = 1; attrNo <= 6; attrNo++)
			{
				final String unitAttributeID = unitAttributeIDs.get (attrNo-1);
				if (xu.hasModifiedSkill (unitAttributeID))
				{
					final int attrValue = xu.getModifiedSkillValue (unitAttributeID);
					
					// Show number and icon for this unit attribute
					contentPane.add (getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont (), Integer.valueOf (attrValue).toString ()),
						"frmUnitRowUnit" + row + "Attribute" + attrNo + "Value");
					
					final BufferedImage attributeImage = getUnitClientUtils ().getUnitSkillComponentBreakdownIcon (xu, unitAttributeID); 
					if (attributeImage != null)
						contentPane.add (getUtils ().createImage (attributeImage), "frmUnitRowUnit" + row + "Attribute" + attrNo + "Icon");
				}
			}
			
			// There's space on the form for up to 12 unit skills
			int skillNo = 0;
			
			for (final String thisSkillID : xu.listModifiedSkillIDs ())
				if (getClient ().getClientDB ().findUnitSkill (thisSkillID, "UnitRowDisplayUI").getUnitSkillTypeID () != UnitSkillTypeID.ATTRIBUTE)
					if (skillNo < 12)
					{
						final BufferedImage skillImage = getUnitClientUtils ().getUnitSkillSingleIcon (xu, thisSkillID);
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
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		cancelAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getCancel ()));
		
		try
		{
			// Work out the title prompt to use
			final Spell spell = getClient ().getClientDB ().findSpell (getTargetSpellID (), "UnitRowDisplayUI");
			if (spell.getResurrectedHealthPercentage () != null)
			{
				// Its a raise dead-type spell being cast in combat
				final List<LanguageText> languageText = ((spell.isResurrectEnemyUnits () != null) && (spell.isResurrectEnemyUnits ())) ?
					getLanguages ().getUnitRowDisplayScreen ().getTitleRaiseEither () : getLanguages ().getUnitRowDisplayScreen ().getTitleRaiseOwn ();
				
				title.setText (getLanguageHolder ().findDescription (languageText));
			}
			else
			{
				// Its a normal unit enchantment being cast on the overland map
				final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
				
				final SpellBookSection section = getClient ().getClientDB ().findSpellBookSection (spell.getSpellBookSectionID (), "UnitRowDisplayUI");
				final String target = getLanguageHolder ().findDescription (section.getSpellTargetPrompt ());
				
				title.setText (target.replaceAll ("SPELL_NAME", spellName));
			}
		
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
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
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
	 * @return Methods that determine whether something is a valid target for a spell
	 */
	public final SpellTargetingUtils getSpellTargetingUtils ()
	{
		return spellTargetingUtils;
	}

	/**
	 * @param s Methods that determine whether something is a valid target for a spell
	 */
	public final void setSpellTargetingUtils (final SpellTargetingUtils s)
	{
		spellTargetingUtils = s;
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
	 * @return The spell being targeted
	 */
	public final String getTargetSpellID ()
	{
		return targetSpellID;
	}
	
	/**
	 * @param spellID The spell being targeted
	 */
	public final void setTargetSpellID (final String spellID)
	{
		targetSpellID = spellID;
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
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
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
}