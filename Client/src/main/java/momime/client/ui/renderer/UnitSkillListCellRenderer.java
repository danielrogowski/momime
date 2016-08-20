package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellLang;
import momime.client.language.database.UnitSkillLang;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.panels.UnitSkillOrHeroItemSlot;
import momime.client.utils.UnitClientUtils;
import momime.common.database.CommonDatabaseConstants;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Renderer for drawing the icon and name of a unit skill
 */
public final class UnitSkillListCellRenderer extends JLabel implements ListCellRenderer<UnitSkillOrHeroItemSlot>
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitSkillListCellRenderer.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;

	/** Multiplayer client */
	private MomClient client;

	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** The unit whose skills we're drawing */
	private ExpandedUnitDetails unit;
	
	/**
	 * Sets up the layout of the panel
	 */
	public final void init ()
	{
		// Leave a gap between one icon and the next
		setBorder (BorderFactory.createEmptyBorder (0, 0, 1, 0));
	}
	
	/**
	 * Sets up the image and label to draw the list cell
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends UnitSkillOrHeroItemSlot> list,
		final UnitSkillOrHeroItemSlot value, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		// Items have a fixed name so are easy; slots display no text at all
		if (value.getHeroItem () != null)
			setText (value.getHeroItem ().getHeroItemName ());
		else if (value.getHeroItemSlotTypeID () != null)
			setText (null);
		
		// Ability to cast spells just says e.g. "Doom Bolt Spell"
		else if (value.getSpellID () != null)
		{
			final SpellLang spellLang = getLanguage ().findSpell (value.getSpellID ());
			final String spellName = (spellLang == null) ? null : spellLang.getSpellName ();
			
			setText (getLanguage ().findCategoryEntry ("frmUnitInfo", "UnitCanCast").replaceAll
				("SPELL_NAME", (spellName != null) ? spellName : value.getSpellID ()));
		}
		else
		{
			// Look up the name of the skill
			final UnitSkillLang skillLang = getLanguage ().findUnitSkill (value.getUnitSkillID ());
			if (skillLang == null)
				setText (value.getUnitSkillID ());
			else
			{
				getUnitStatsReplacer ().setUnit (getUnit ());
				String skillText = getUnitStatsReplacer ().replaceVariables (skillLang.getUnitSkillDescription ());
				
				// Show strength of skills, e.g. Fire Breath 2
				if ((value.getUnitSkillValue () != null) && (value.getUnitSkillValue () > 0) &&
					(!value.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)) &&
					(!value.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT)) &&
					(!value.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_RANGED_ATTACK_AMMO)) &&
					(!value.getUnitSkillID ().startsWith ("HS")))	// This is a bit of a hack, but better than listing all hero skills out separately, and the client
																					// doesn't have all the skill rolling data like the "maxOccurrences" value and so on
					
					try
					{
						skillText = skillText + " " + getUnit ().getModifiedSkillValue (value.getUnitSkillID ());
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
				// Trim off the annoying leading space on hero skills like "Armsmaster", which is there in case it needs to put "Super Armsmaster"
				setText (skillText.trim ());
			}
		}
		
		setIcon (null);
		try
		{
			final BufferedImage image;
			if (value.getUnitSkillID () != null)
				image = getUnitClientUtils ().getUnitSkillSingleIcon (getUnit (), value.getUnitSkillID ());
			else if (value.getSpellID () != null)
				image = getUtils ().loadImage (getGraphicsDB ().findSpell (value.getSpellID (), "UnitSkillListCellRenderer").getUnitCanCastImageFile ());
			else if (value.getHeroItemSlotTypeID () != null)
				image = getUtils ().loadImage (getGraphicsDB ().findHeroItemSlotType (value.getHeroItemSlotTypeID (), "UnitSkillListCellRenderer").getHeroItemSlotTypeImageFileWithBackground ());
			else
			{
				// For items, need to superimpose the item image onto a square background
				final HeroItemTypeGfx itemType = getGraphicsDB ().findHeroItemType (value.getHeroItem ().getHeroItemTypeID (), "UnitSkillListCellRenderer");
				
				final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/heroItems/unitSkillsHeroItemBackground.png");
				image = new BufferedImage (background.getWidth (), background.getHeight (), BufferedImage.TYPE_INT_ARGB);
				final Graphics2D g = image.createGraphics ();
				try
				{
					g.drawImage (background, 0, 0, null);
					g.drawImage (getUtils ().loadImage (itemType.getHeroItemTypeImageFile ().get (value.getHeroItem ().getHeroItemImageNumber ())), 0, 0, null);
				}
				finally
				{
					g.dispose ();
				}
			}
			
			setIcon (new ImageIcon (image));
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}

		return this;
	}
	
	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
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
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
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
	 * @return The unit whose skills we're drawing
	 */
	public final ExpandedUnitDetails getUnit ()
	{
		return unit;
	}

	/**
	 * @param u The unit whose skills we're drawing
	 */
	public final void setUnit (final ExpandedUnitDetails u)
	{
		unit = u;
	}
}