package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.HeroItemSlot;
import momime.common.database.Unit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.NumberedHeroItem;

/**
 * Renderer for drawing the hero panels on the items screen, so items can be dragged on/off the item slots
 */
public final class HeroTableCellRenderer extends JPanel implements TableCellRenderer
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (HeroTableCellRenderer.class);
	
	/** XML layout */
	private XmlLayoutContainerEx heroLayout;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;

	/** Multiplayer client */
	private MomClient client;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;

	/** Medium font */
	private Font mediumFont;
	
	/** Background image */
	private BufferedImage background;
	
	/** Size to stretch the hero portraits to */
	private XmlLayoutComponent heroPortraitSize;
	
	/** Label showing portrait*/
	private JLabel heroPortrait;
	
	/** Label showing name */
	private JLabel heroName;
	
	/** Label showing each item */
	private List<JLabel> itemLabels = new ArrayList<JLabel> ();
	
	/**
	 * Loads the background image for the panel and sets up the layout
	 * @throws IOException If there is a problem
	 */
	public final void init () throws IOException
	{
		background = getUtils ().loadImage ("/momime.client.graphics/ui/heroItems/heroItemsGridCell.png");
		
		setLayout (new XmlLayoutManager (getHeroLayout ()));
		heroPortraitSize = getHeroLayout ().findComponent ("frmHeroItemsHeroPortrait");

		heroPortrait = new JLabel ();
		add (heroPortrait, "frmHeroItemsHeroPortrait");
		
		heroName = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		add (heroName, "frmHeroItemsHeroName");
		
		for (int slotNumber = 1; slotNumber <= 3; slotNumber++)
		{
			final JLabel itemLabel = new JLabel ();
			itemLabels.add (itemLabel);
			add (itemLabel, "frmHeroItemsSlot" + slotNumber);
		}
	}
	
	/**
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @return Slot number (0,1,2) at this location within the panel, or -1 if the location is outside the panel or not part of one of the slots
	 */
	public final int getSlotNumberAt (final int x, final int y)
	{
		final int slotNumber;
		final XmlLayoutComponent slot = getHeroLayout ().findComponentAt (x, y);
		if ((slot != null) && (slot.getName ().startsWith ("frmHeroItemsSlot")))
			slotNumber = Integer.parseInt (slot.getName ().substring (slot.getName ().length () - 1)) - 1;
		else
			slotNumber = -1;
		
		return slotNumber;
	}
	
	/**
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @return Whether or not the coordinates fall within the hero portrait
	 */
	public final boolean isWithinHeroPortrait (final int x, final int y)
	{
		final XmlLayoutComponent slot = getHeroLayout ().findComponentAt (x, y);
		return (slot != null) && (slot.getName ().equals ("frmHeroItemsHeroPortrait"));
	}
	
	/**
	 * Show the hero portrait, and either the item slots or the item itself if there is one in the slot
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getTableCellRendererComponent (final JTable table, final Object value,
		final boolean isSelected, final boolean hasFocus, final int row, final int column)
	{
		heroPortrait.setIcon (null);
		heroName.setText (null);
		for (final JLabel itemLabel : itemLabels)
			itemLabel.setIcon (null);
		
		if (value != null)
			try
			{
				// Hero name
				final MemoryUnit unit = (MemoryUnit) value;
				heroName.setText (getUnitClientUtils ().getUnitName (unit, UnitNameType.RACE_UNIT_NAME));
				
				// Hero portrait
				final Unit unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "HeroTableCellRenderer");
				final BufferedImage unitImage = getUtils ().loadImage (unitDef.getHeroPortraitImageFile ());
			
				// Stretch it to the correct size
				final Image resizedImage = unitImage.getScaledInstance
					(heroPortraitSize.getWidth (), heroPortraitSize.getHeight (), Image.SCALE_FAST);
				heroPortrait.setIcon (new ImageIcon (resizedImage));
				
				// Item slots
				int slotNumber = 0;
				for (final HeroItemSlot slot : unitDef.getHeroItemSlot ())
				{
					// Is there an item in this slot?
					NumberedHeroItem item = null;
					if (slotNumber < unit.getHeroItemSlot ().size ())
					{
						final MemoryUnitHeroItemSlot itemContainer = unit.getHeroItemSlot ().get (slotNumber);
						item = itemContainer.getHeroItem ();
					}
						
					// Draw it
					if (slotNumber < itemLabels.size ())
					{
						String itemImageFilename;
						if (item == null)
							itemImageFilename = getClient ().getClientDB ().findHeroItemSlotType
								(slot.getHeroItemSlotTypeID (), "HeroTableCellRenderer").getHeroItemSlotTypeImageFile ();
						else
							itemImageFilename = getClient ().getClientDB ().findHeroItemType
								(item.getHeroItemTypeID (), "HeroTableCellRenderer").getHeroItemTypeImageFile ().get (item.getHeroItemImageNumber ());
						
						itemLabels.get (slotNumber).setIcon (new ImageIcon (getUtils ().doubleSize (getUtils ().loadImage (itemImageFilename))));
					}
					
					slotNumber++;
				}
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		
		return this;
	}

	/**
	 * Paint the panel background
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		g.drawImage (background, 0, 0, null);
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getHeroLayout ()
	{
		return heroLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setHeroLayout (final XmlLayoutContainerEx layout)
	{
		heroLayout = layout;
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
}