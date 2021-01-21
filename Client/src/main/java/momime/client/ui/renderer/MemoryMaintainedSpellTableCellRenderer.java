package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.messages.MemoryMaintainedSpell;

/**
 * Renderer for drawing overland enchantments spell onto the magic sliders screen
 */
public final class MemoryMaintainedSpellTableCellRenderer extends JLabel implements TableCellRenderer
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MemoryMaintainedSpellTableCellRenderer.class);
	
	/** Multiplayer client */
	private MomClient client;
	
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
				final String imageName = getClient ().getClientDB ().findSpell (spell.getSpellID (), "MemoryMaintainedSpellTableCellRenderer").getOverlandEnchantmentImageFile ();
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