package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.messages.MemoryMaintainedSpell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Renderer for drawing overland enchantments spell onto the magic sliders screen
 */
public final class MemoryMaintainedSpellTableCellRenderer extends JLabel implements TableCellRenderer
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MemoryMaintainedSpellTableCellRenderer.class);
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;

	/**
	 * Output mirror in caster wizard's colour, and overland enchantment image
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getTableCellRendererComponent (final JTable table, final Object value,
		final boolean isSelected, final boolean hasFocus, final int row, final int column)
	{
		setIcon (null);
		if (value != null)
			try
			{
				final MemoryMaintainedSpell spell = (MemoryMaintainedSpell) value;	
				final String imageName = getGraphicsDB ().findSpell (spell.getSpellID (), "MemoryMaintainedSpellTableCellRenderer").getOverlandEnchantmentImageFile ();
				if (imageName != null)
				{
					final BufferedImage spellImage = getUtils ().loadImage (imageName);
					
					// Now that we got the spell image OK, get the coloured mirror for the caster
					final BufferedImage mirrorImage = getPlayerColourImageGenerator ().getOverlandEnchantmentMirror (spell.getCastingPlayerID ());
					final BufferedImage mergedImage = new BufferedImage (mirrorImage.getWidth (), mirrorImage.getHeight (), BufferedImage.TYPE_INT_ARGB);
					final Graphics2D g = mergedImage.createGraphics ();
					try
					{
						g.drawImage (spellImage, GraphicsDatabaseConstants.IMAGE_MIRROR_X_OFFSET, GraphicsDatabaseConstants.IMAGE_MIRROR_Y_OFFSET, null);
						g.drawImage (mirrorImage, 0, 0, null);
					}
					finally
					{
						g.dispose ();
					}
					
					setIcon (new ImageIcon (mergedImage));
				}
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		
		return this;
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
}