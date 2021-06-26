package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.database.UnitEx;
import momime.common.messages.MemoryUnit;

/**
 * Renderer for drawing each row of up to 20 units onto the army list screen
 */
public final class ArmyListCellRenderer implements ListCellRenderer<Entry<MapCoordinates3DEx, List<MemoryUnit>>>
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ArmyListCellRenderer.class);
	
	/** Border between the top of the image and the unit icons */
	public final static int TOP_BORDER = 2;

	/** Border between the left of the image and the first unit icon */
	public final static int LEFT_BORDER = 4;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/** Background image */
	private BufferedImage background;
	
	/** Fixed size for the panel */
	private Dimension panelSize;
	
	/**
	 * Loads the background image for the panel
	 * @throws IOException If there is a problem
	 */
	public final void init () throws IOException
	{
		background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/armyListRow.png");
		panelSize = new Dimension (background.getWidth () * 2, background.getHeight () * 2);
	}
	
	/**
	 * Return this panel to draw itself
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends Entry<MapCoordinates3DEx, List<MemoryUnit>>> list,
		final Entry<MapCoordinates3DEx, List<MemoryUnit>> value, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		// Pre-load the unit images, rather than doing it every redraw
		final List<BufferedImage> unitImages = new ArrayList<BufferedImage> ();
		try
		{		
			for (final MemoryUnit unit : value.getValue ())
			{
				final UnitEx unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "ArmyListCellRenderer");
				unitImages.add (getPlayerColourImageGenerator ().getOverlandUnitImage (unitDef, unit.getOwningPlayerID ()));
			}
		}
		catch (final IOException e)
		{
			log.error (e, e);
		}
		
		// Create the panel
		// NB. We can't just reuse the same panel and put the code in paintComponent, since then it can't access the list of what it needs to draw
		final JPanel panel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				// Draw background
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
				
				// Draw all the units
				int x = LEFT_BORDER;
				for (final BufferedImage unitImage : unitImages)
				{
					g.drawImage (unitImage, x, TOP_BORDER, unitImage.getWidth () * 2, unitImage.getHeight () * 2, null);
					x = x + unitImage.getWidth () * 2;
				}
			}
		};

		panel.setMinimumSize (panelSize);
		panel.setMaximumSize (panelSize);
		panel.setPreferredSize (panelSize);
		
		return panel;
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
	 * @return Player colour image generator
	 */
	public final PlayerColourImageGenerator getPlayerColourImageGenerator ()
	{
		return playerColourImageGenerator;
	}

	/**
	 * @param g Player colour image generator
	 */
	public final void setPlayerColourImageGenerator (final PlayerColourImageGenerator g)
	{
		playerColourImageGenerator= g;
	}
}