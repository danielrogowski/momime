package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.common.database.HeroItemType;
import momime.common.messages.NumberedHeroItem;

/**
 * Renderer for drawing each row of the bank vault (items not assigned to any hero) 
 */
public final class UnassignedHeroItemCellRenderer extends JPanel implements ListCellRenderer<NumberedHeroItem>
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (UnassignedHeroItemCellRenderer.class);
	
	/** XML layout */
	private XmlLayoutContainerEx unassignedHeroItemLayout;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Medium font */
	private Font mediumFont;

	/** Background image */
	private BufferedImage background;
	
	/** Label showing image */
	private JLabel itemImage;
	
	/** Label showing name */
	private JTextArea itemName;
	
	/**
	 * Loads the background image for the panel and sets up the layout
	 * @throws IOException If there is a problem
	 */
	public final void init () throws IOException
	{
		background = getUtils ().loadImage ("/momime.client.graphics/ui/heroItems/heroItemBankRow.png");
		
		setLayout (new XmlLayoutManager (getUnassignedHeroItemLayout ()));
		
		itemImage = new JLabel ();
		add (itemImage, "frmHeroItemsUnassignedImage");
		
		itemName = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getMediumFont ());
		add (itemName, "frmHeroItemsUnassignedName");
	}
	
	/**
	 * Return this panel to draw itself
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends NumberedHeroItem> list,
		final NumberedHeroItem value, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		Image doubleSizeImage = null;
		try
		{
			final HeroItemType itemType = getClient ().getClientDB ().findHeroItemType (value.getHeroItemTypeID (), "UnassignedHeroItemCellRenderer");
			final BufferedImage image = getUtils ().loadImage (itemType.getHeroItemTypeImageFile ().get (value.getHeroItemImageNumber ()));
			doubleSizeImage = getUtils ().doubleSize (image);
		}
		catch (final IOException e)
		{
			log.error (e, e);
		}
		itemImage.setIcon ((doubleSizeImage == null) ? null : new ImageIcon (doubleSizeImage));
		
		itemName.setText (value.getHeroItemName ());
		
		return this;
	}

	/**
	 * Paint the panel background
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getUnassignedHeroItemLayout ()
	{
		return unassignedHeroItemLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setUnassignedHeroItemLayout (final XmlLayoutContainerEx layout)
	{
		unassignedHeroItemLayout = layout;
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