package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.common.database.v0_9_5.UnitHasSkill;
import momime.common.messages.v0_9_5.AvailableUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Renderer for drawing the icon and name of a unit skill
 */
public final class UnitSkillListCellRenderer extends JLabel implements ListCellRenderer<UnitHasSkill>
{
	/** Unique value for serialization */
	private static final long serialVersionUID = -5643821280980837618L;

	/** Class logger */
	private final Log log = LogFactory.getLog (UnitSkillListCellRenderer.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;
	
	/** The unit whose skills we're drawing */
	private AvailableUnit unit;
	
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
	@Override
	public final Component getListCellRendererComponent (final JList<? extends UnitHasSkill> list, final UnitHasSkill value, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		// Look up the name of the skill
		final momime.client.language.database.v0_9_5.UnitSkill skillLang = getLanguage ().findUnitSkill (value.getUnitSkillID ());
		if (skillLang == null)
			setText (value.getUnitSkillID ());
		else
		{
			getUnitStatsReplacer ().setUnit (getUnit ());
			setText (getUnitStatsReplacer ().replaceVariables (skillLang.getUnitSkillDescription ()));
		}
		
		setIcon (null);
		try
		{
			// Look up the image for the skill
			final UnitSkill skillGfx = getGraphicsDB ().findUnitSkill (value.getUnitSkillID (), "UnitSkillListCellRenderer");
			final BufferedImage image = getUtils ().loadImage (skillGfx.getUnitSkillImageFile ());
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
	 * @return The unit whose skills we're drawing
	 */
	public final AvailableUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @param u The unit whose skills we're drawing
	 */
	public final void setUnit (final AvailableUnit u)
	{
		unit = u;
	}
}